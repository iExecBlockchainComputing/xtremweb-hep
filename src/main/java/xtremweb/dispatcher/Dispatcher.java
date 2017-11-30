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

import org.eclipse.jetty.server.session.SessionHandler;
import xtremweb.common.*;
import xtremweb.communications.AccessLogger;
import xtremweb.communications.HTTPServer;
import xtremweb.communications.TCPServer;
import xtremweb.security.PEMPublicKeyValidator;
import xtremweb.security.X509ProxyValidator;

import org.apache.log4j.Logger;


import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;

/**
 * Dispatcher Implementation
 * The Dispatcher is responsible for delivering task to servers
 * @author V. Neri, G. Fedak
 */

/**
 * The <CODE>Dispatcher</CODE> is the XtremWeb Dispatcher main class.
 */

public class Dispatcher {

	private static final Logger logger = Logger.getLogger(Dispatcher.class);

	/**
	 * The TaskSet and TaskManager
	 */
	private static TaskSet tset;

	/**
	 * Database Interface
	 */
	private static DBInterface db;

	/** Scheduling policy */
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

	/** Timer */
	private static Timer timer;

	/**
	 * This stores and tests the command line parameters
	 */
	private CommandLineParser args;
	/**
	 * Config as read from config file
	 */
	private static XWConfigurator config;

	/**
	 * This is the default constructor
	 */
	public Dispatcher(final String a[]) {
		final String[] argv = a.clone();
		try {
			args = new CommandLineParser(argv);
		} catch (final Exception e) {
			logger.error("Command line error " + e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}

		XWRole.setDispatcher();

		try {
			setConfig((XWConfigurator) args.getOption(CommandLineOptions.CONFIG));
		} catch (final Exception e) {
			logger.error("can retreive config file");
			System.exit(XWReturnCode.FATAL.ordinal());
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

	/**
	 * Main function
	 */
	public void go() throws Exception {

		timer = new Timer();

		db = new DBInterface(config);

		tset = new HashTaskSet();
		try {
			setScheduler(
					(Scheduler) (Class.forName(getConfig().getProperty(XWPropertyDefs.SCHEDULERCLASS)).newInstance()));
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}

		try {
			setProxyValidator(new X509ProxyValidator());
		} catch (final Exception e) {
			setProxyValidator(null);
			logger.error("Caught exception, Can't create ProxyValidator", e);
		}

		try {
			getProxyValidator().setCACertificateEntries(getConfig().getKeyStore());
		} catch (final Exception e) {
			logger.error("Caught exception, Can't add CA certificates to keystore", e);
		}

		try {
			certValidator = new PEMPublicKeyValidator();
		} catch (final Exception e) {
			certValidator = null;
			logger.error("Caught exception, Can't create PEMPublicKeyValidator", e);
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

			@SuppressWarnings("unused")
			final AccessLogger accessLogger = new AccessLogger(getConfig().getFile(XWPropertyDefs.HOMEDIR),
					XWTools.getLocalHostName());

			final TCPServer tcpServer = new TCPServer();
			tcpServer.initComm(getConfig(), new TCPHandler(getConfig()));
			tcpServer.start();

			logger.warn("UDP server not started");

			if (getConfig().http()) {
				final HTTPServer httpServer = new HTTPServer();
				// httpServer.initComm(getConfig(), new
				// HTTPHandler(getConfig()));

				// Handler needs HTTP session to handle openid call returns
				final SessionHandler main_sessionHandler = new SessionHandler();
				main_sessionHandler.setHandler(new HTTPHandler(getConfig()));
				httpServer.initComm(getConfig(), main_sessionHandler);
				httpServer.addHandler(HTTPHandler.PATH, main_sessionHandler);

				// OpenId Handler needs HTTP session
				// final SessionHandler opid_sessionHandler = new
				// SessionHandler();
				// final HTTPOpenIdHandler oh = new HTTPOpenIdHandler();
				// HTTPOpenIdHandler.setCACertificateEntries(getConfig().getKeyStore());
				// opid_sessionHandler.setHandler(oh);
				// httpServer.addHandler(HTTPOpenIdHandler.handlerPath,
				// opid_sessionHandler);

				// OAuth Handler needs HTTP session
				final SessionHandler oauth_sessionHandler = new SessionHandler();
				final HTTPOAuthHandler oah = new HTTPOAuthHandler();
				oauth_sessionHandler.setHandler(oah);
				httpServer.addHandler(HTTPOAuthHandler.handlerPath, oauth_sessionHandler);

				// JWT Handler needs HTTP session
				final SessionHandler jwt_sessionHandler = new SessionHandler();
				final HTTPJWTEthereumAuthHandler jwth = new HTTPJWTEthereumAuthHandler();
				jwt_sessionHandler.setHandler(jwth);
				httpServer.addHandler(HTTPJWTEthereumAuthHandler.handlerPath, jwt_sessionHandler);

				// StatsHandler does not need HTTP session
				httpServer.addHandler(HTTPStatsHandler.PATH, new HTTPStatsHandler());
				httpServer.start();
			}
		} catch (final Exception e) {
			logger.error("Caught exception, Dispatcher main(): " + e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}

		final Collection<String> services = XWTools.split(getConfig().getProperty(XWPropertyDefs.SERVICES));
		if (services != null) {
			for (final Iterator<String> iter = services.iterator(); iter.hasNext();) {
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

		logger.info("XWHEP Dispatcher(" + CommonVersion.getCurrent().full() + ") started [" + new Date() + "]");
		logger.info("DB vendor  = " + getConfig().getProperty(XWPropertyDefs.DBVENDOR));
		logger.info("mileStone  = " + getConfig().getProperty(XWPropertyDefs.MILESTONES));
		logger.info("Time out   = " + getConfig().getProperty(XWPropertyDefs.TIMEOUT));
		logger.info("Disk opt'd = " + getConfig().getProperty(XWPropertyDefs.OPTIMIZEDISK));
		logger.info("Net  opt'd = " + getConfig().getProperty(XWPropertyDefs.OPTIMIZENETWORK));
		logger.info("NIO        = " + getConfig().getProperty(XWPropertyDefs.JAVANIO));
		getConfig().dump(System.out, "XWHEP Dispatcher started ");
	}

	public static void shutdown() {
		db.unlockWorks(XWTools.getLocalHostName());
		System.exit(XWReturnCode.SUCCESS.ordinal());
	}

	@Override
	public void finalize() {
		db.unlockWorks(XWTools.getLocalHostName());
	}

	/**
	 * @return the config
	 */
	public static XWConfigurator getConfig() {
		return config;
	}

	/**
	 * @param config
	 *            the config to set
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
	 * @param scheduler
	 *            the scheduler to set
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
	 * @param proxyValidator
	 *            the proxyValidator to set
	 */
	public static void setProxyValidator(final X509ProxyValidator proxyValidator) {
		Dispatcher.proxyValidator = proxyValidator;
	}
}
