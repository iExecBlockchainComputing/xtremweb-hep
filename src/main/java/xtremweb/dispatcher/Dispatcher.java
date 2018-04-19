/*
 * Copyrights     : CNRS
 * Author         : Oleg Lodygensky
 * Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
 * Web            : http://www.xtremweb-hep.org
 *
 *      This file is part of XtremWeb-HEP.
 *
 *    XtremWeb-HEP is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    XtremWeb-HEP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package xtremweb.dispatcher;

import com.iexec.common.contracts.generated.IexecHub;
import com.iexec.common.ethereum.CommonConfiguration;
import com.iexec.common.ethereum.IexecConfigurationService;
import com.iexec.common.ethereum.Web3jService;
import com.iexec.scheduler.actuator.ActuatorService;
import com.iexec.scheduler.ethereum.IexecSchedulerLibrary;
import com.iexec.common.workerpool.WorkerPoolConfig;

import xtremweb.common.*;
import xtremweb.communications.AccessLogger;
import xtremweb.communications.HTTPServer;
import xtremweb.communications.TCPServer;
import xtremweb.security.PEMPublicKeyValidator;
import xtremweb.security.X509ProxyValidator;

import java.io.IOException;
import java.util.*;

import org.eclipse.jetty.server.session.SessionHandler;

import static xtremweb.common.XWPropertyDefs.BLOCKCHAINETHENABLED;

/**
 * Dispatcher Implementation
 * The Dispatcher is responsible for delivering task to servers
 *
 * @author V. Neri, G. Fedak
 */

/**
 * The <CODE>Dispatcher</CODE> is the XtremWeb Dispatcher main class.
 */

public class Dispatcher {

    /**
     * The TaskSet and TaskManager
     */
    private static TaskSet tset;
    /**
     * Database Interface
     */
    private static DBInterface db;
    /**
     * Scheduling policy
     */
    private static Scheduler scheduler;
    /**
     * This aims to validate an X509 certificate against certificate paths
     *
     * @since 7.0.0
     */
    private static X509ProxyValidator proxyValidator;
    /**
     * This aims to challenge client connections
     *
     * @since 7.5.0
     */
    private static PEMPublicKeyValidator certValidator;
    /**
     * Timer
     */
    private static Timer timer;
    /**
     * Config as read from config file
     */
    private static XWConfigurator config;
    private final Logger logger;
    /**
     * This stores and tests the command line parameters
     */
    private CommandLineParser args;

    /**
     * This is the default constructor
     */
    public Dispatcher(final String a[]) {
        final String[] argv = a.clone();
        logger = new Logger(this);
        try {
            args = new CommandLineParser(argv);
        } catch (final Exception e) {
            logger.fatal("Command line error " + e);
        }

        XWRole.setDispatcher();

        try {
            setConfig((XWConfigurator) args.getOption(CommandLineOptions.CONFIG));
        } catch (final Exception e) {
            logger.fatal("can retreive config file");
        }
    }

    /**
     * Main function
     */
    public static void main(final String args[]) {

        try {
            new Dispatcher(args).go();
        } catch (final Exception e) {
            System.err.println(e.toString());
        }
    }

    public static void shutdown() {
        db.unlockWorks(XWTools.getLocalHostName());
        System.exit(XWReturnCode.SUCCESS.ordinal());
    }

    /**
     * @return the config
     */
    public static XWConfigurator getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public static void setConfig(final XWConfigurator config) {
        Dispatcher.config = config;
    }

    /**
     * @return the scheduler
     */
    public static Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * @param scheduler the scheduler to set
     */
    public static void setScheduler(final Scheduler scheduler) {
        Dispatcher.scheduler = scheduler;
    }

    /**
     * @return the proxyValidator
     */
    public static X509ProxyValidator getProxyValidator() {
        return proxyValidator;
    }

    /**
     * @param proxyValidator the proxyValidator to set
     */
    public static void setProxyValidator(final X509ProxyValidator proxyValidator) {
        Dispatcher.proxyValidator = proxyValidator;
    }

    /**
     * Main function
     */
    public void go() throws Exception {

        timer = new Timer();

        try {
            db = new DBInterface(config);
        } catch (Exception e) {
            e.printStackTrace();
        }

        tset = new HashTaskSet();
        try {
            setScheduler(
                    (Scheduler) (Class.forName(config.getProperty(XWPropertyDefs.SCHEDULERCLASS)).newInstance()));
        } catch (final Exception e) {
            logger.exception(e);
            logger.fatal(e.toString());
        }

        try {
            setProxyValidator(new X509ProxyValidator());
        } catch (final Exception e) {
            setProxyValidator(null);
            logger.exception("Can't create ProxyValidator", e);
        }

        try {
            getProxyValidator().setCACertificateEntries(config.getKeyStore());
        } catch (final Exception e) {
            logger.exception("Can't add CA certificates to keystore", e);
        }

        try {
            certValidator = new PEMPublicKeyValidator();
        } catch (final Exception e) {
            certValidator = null;
            logger.exception("Can't create PEMPublicKeyValidator", e);
        }

        tset.start();
        try {
            while (!tset.isReady()) {
                Thread.sleep(1000);
                logger.info("still waiting task set...");
            }
        } catch (final Exception e) {
            logger.error("exception while waiting task set...");
        }

        try {

            @SuppressWarnings("unused") final AccessLogger accessLogger = new AccessLogger(config.getFile(XWPropertyDefs.HOMEDIR),
                    XWTools.getLocalHostName());

            final TCPServer tcpServer = new TCPServer();
            tcpServer.initComm(config, new TCPHandler(config));
            tcpServer.start();

            logger.warn("UDP server not started");

            if (config.http()) {
                final HTTPServer httpServer = new HTTPServer();
                // httpServer.initComm(config, new
                // HTTPHandler(config));

                // Handler needs HTTP session to handle openid call returns
                final SessionHandler main_sessionHandler = new SessionHandler();
                main_sessionHandler.setHandler(new HTTPHandler(config));
                httpServer.initComm(config, main_sessionHandler);
                httpServer.addHandler(HTTPHandler.PATH, main_sessionHandler);

                // OpenId Handler needs HTTP session
                // final SessionHandler opid_sessionHandler = new
                // SessionHandler();
                // final HTTPOpenIdHandler oh = new HTTPOpenIdHandler();
                // HTTPOpenIdHandler.setCACertificateEntries(config.getKeyStore());
                // opid_sessionHandler.setHandler(oh);
                // httpServer.addHandler(HTTPOpenIdHandler.handlerPath,
                // opid_sessionHandler);

                // OAuth Handler needs HTTP session
                final SessionHandler oauth_sessionHandler = new SessionHandler();
                final HTTPOAuthHandler oah = new HTTPOAuthHandler();
                oauth_sessionHandler.setHandler(oah);
                httpServer.addHandler(HTTPOAuthHandler.handlerPath, oauth_sessionHandler);

                if ((config.getProperty(XWPropertyDefs.JWTETHSECRET) != null) &&
                        (config.getProperty(XWPropertyDefs.JWTETHISSUER) != null)) {
                    // JWT Handler needs HTTP session
                    final SessionHandler jwt_sessionHandler = new SessionHandler();
                    final HTTPJWTEthereumAuthHandler jwth = new HTTPJWTEthereumAuthHandler();
                    jwt_sessionHandler.setHandler(jwth);
                    httpServer.addHandler(HTTPJWTEthereumAuthHandler.handlerPath, jwt_sessionHandler);
                }
                // StatsHandler does not need HTTP session
                httpServer.addHandler(HTTPStatsHandler.PATH, new HTTPStatsHandler());
                httpServer.start();
            }
        } catch (final Exception e) {
            logger.exception(e);
            logger.fatal("Dispatcher main(): " + e);
        }

        final Collection<String> services = XWTools.split(config.getProperty(XWPropertyDefs.SERVICES));
        if (services != null) {
            for (final Iterator<String> iter = services.iterator(); iter.hasNext(); ) {
                try {
                    final String s = iter.next();
                    logger.debug(s);
                    db.insertService(s);
                } catch (final Exception e) {
                    logger.warn("Unable to start service : " + e);
                }
                db.updateAppsPool();
            }
        }

        if (config.getBoolean(BLOCKCHAINETHENABLED) == true) {
            try {
                IexecSchedulerLibrary.initialize(config.getConfigFile().getParentFile().getAbsolutePath() + "/iexec-scheduler.yml");
                boolean ethNodeRunning = Web3jService.getInstance().getWeb3j().web3ClientVersion().send().getWeb3ClientVersion() != null;
                if (ethNodeRunning) {
                    SchedulerPocoWatcherImpl schedulerPocoWatcher = new SchedulerPocoWatcherImpl();
                } else {
                    throw new IOException("Unable to connect to ETH node");
                }
                config.blockchainServices = true;
            } catch (final Exception e) {
                logger.exception("Can't access to blockchain services", e);
            }

            final ActuatorService actuatorService = ActuatorService.getInstance();
            final List<IexecHub.CreateCategoryEventResponse> categories =
                    actuatorService != null ?
                            actuatorService.getCategories() :
                            null;
            if (categories != null) {
                for (final Iterator<IexecHub.CreateCategoryEventResponse> iter = categories.iterator(); iter.hasNext(); ) {
                    try {
                        final IexecHub.CreateCategoryEventResponse category = iter.next();
                        logger.debug(category.catid + " " + category.name);
                        db.insertCategory(category);
                    } catch (final Exception e) {
                        logger.warn("Unable to start service : " + e);
                    }
                }
            }
        }

        logger.info("XWHEP Dispatcher(" + Version.currentVersion + ") started [" + new Date() + "]");
        logger.info("DB vendor       = " + config.getProperty(XWPropertyDefs.DBVENDOR));
        logger.info("mileStone       = " + config.getProperty(XWPropertyDefs.MILESTONES));
        logger.info("Time out        = " + config.getProperty(XWPropertyDefs.TIMEOUT));
        logger.info("Disk opt'd      = " + config.getProperty(XWPropertyDefs.OPTIMIZEDISK));
        logger.info("Net  opt'd      = " + config.getProperty(XWPropertyDefs.OPTIMIZENETWORK));
        logger.info("NIO             = " + config.getProperty(XWPropertyDefs.JAVANIO));
        config.dump(System.out, "XWHEP Dispatcher started ");
    }

    @Override
    public void finalize() {
        db.unlockWorks(XWTools.getLocalHostName());
    }
}
