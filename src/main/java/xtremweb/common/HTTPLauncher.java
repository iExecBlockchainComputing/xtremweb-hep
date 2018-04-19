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

package xtremweb.common;

import com.iexec.common.ethereum.CommonConfiguration;
import com.iexec.worker.ethereum.CommonConfigurationGetter;
import com.iexec.worker.ethereum.IexecWorkerLibrary;
import xtremweb.communications.CommClient;
import xtremweb.exec.Executor;
import xtremweb.worker.WorkerPocoWatcherImpl;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This launches XtremWeb from its JAR file. This first retrieve URL from config
 * file, if any. If the config file contains no URL, this contructs a new URL
 * with the server found in the config file.
 * <p>
 * URL must be like "http://aServer/aFile.jar. Default URL is
 * "http://aServer/XWHEP/download/xtremweb.jar"
 */
public final class HTTPLauncher {

    private static final int SLEEPDELAY = 2500;
    private XWConfigurator config;

    /**
     * Creates a new <code>HTTPLauncher</code> instance.
     *
     * @param a a <code>String[]</code> value : command line arguments which
     *          specificies the config file name
     * @throws IOException
     */
    private HTTPLauncher(final String[] a) throws IOException {

        final String[] argv = a.clone();
        CommandLineParser args = null;
        try {
            args = new CommandLineParser(argv);
        } catch (final Exception e) {
            XWTools.fatal("xtremweb.upgrade.HTTPLauncher " + e);
        }

        try {
            config = (XWConfigurator) args.getOption(CommandLineOptions.CONFIG);
        } catch (final Exception e) {
            XWTools.fatal("can retreive config file");
        }

        final Logger logger = new Logger();

        URL url = null;
        try {
            url = config.launcherURL();
            if ((url.getPath() == null) || (url.getPath().length() == 0)) {
                url = new URL(url.toString() + "/XWHEP/download/" + XWTools.JARFILENAME);
            }
        } catch (final MalformedURLException e) {
            logger.warn("Invalid launcher URL : " + url);
            url = null;
        }

        Version serverVersion = null;
        File jarFile = null;
        final File rootDir = config.getConfigFile().getParentFile().getParentFile();
        File libDir = new File(rootDir, "lib");
        if (!libDir.exists()) {
            libDir = config.getTmpDir();
        }
        File binDir = new File(rootDir, "bin");
        if (!binDir.exists()) {
            binDir = config.getTmpDir();
        }

        final Version version = Version.currentVersion;
        while (true) {
            logger.debug("launcherURL = " + url);
            logger.debug("Current version : " + version.toString());

            boolean upgrade = false;

            try {
                serverVersion = commClient().version();
                logger.debug("Server version : " + serverVersion);

                if (serverVersion != null) {

                    final File newJarFile = new File(libDir, XWTools.JARFILENAME + "-" + serverVersion);

                    if (!newJarFile.exists()
                            && !serverVersion.toString().equals(version.toString())) {
                        logger.info("Server  version : " + serverVersion);
                        logger.info("**********  **********  **********");
                        logger.info("We must upgrade");
                        logger.info("**********  **********  **********");
                        upgrade = true;
                        jarFile = null;
                        jarFile = newJarFile;
                    }

                    if (newJarFile.exists()) {
                        jarFile = newJarFile;
                    }
                }
            } catch (final SSLHandshakeException e) {
                logger.fatal("SSL error (maybe we have received a new keystore: relaunch is then necessary) : " + e);
            } catch (final Exception e) {
                upgrade = false;
                logger.exception(e);
            }

            if ((upgrade) && (url != null)) {
                logger.info("Downloading xwhep JAR file");
                try (final StreamIO io = new StreamIO(null, new DataInputStream(url.openStream()), false)) {
                    logger.debug("" + jarFile + ".exists() = " + jarFile.exists());

                    io.readFileContent(jarFile);
                } catch (final FileNotFoundException e) {
                    logger.fatal("Can't download " + XWTools.JARFILENAME + " : " + e);
                } catch (final Exception e) {
                    logger.exception(e);
                    logger.warn("Can't download " + XWTools.JARFILENAME + "; using default : " + e.toString());
                }
            } else {
                logger.info("Not downloading xwhep JAR file");
            }

            Executor exec = null;
            String tmpPath = null;
            String jarFilePath = null;
            String keystorePath = null;
            String configPath = null;
            String xwcp = null;
            String javacp = System.getProperty("java.class.path");
            final StringBuilder javaCmd = new StringBuilder("java ");

            try {
                logger.debug("00 libDir = " + libDir.getAbsolutePath());
                if (config.getProperty(XWPropertyDefs.XWCP) != null) {
                    libDir = new File(config.getProperty(XWPropertyDefs.XWCP));
                    if (libDir.isFile()) {
                        libDir = libDir.getParentFile();
                    }
                }
                logger.config("libDir = " + libDir.getAbsolutePath());

                if ((jarFile == null) || !jarFile.exists()) {
                    jarFile = new File(libDir, XWTools.JARFILENAME);
                }

                logger.config("jarFile = " + jarFile.getAbsolutePath());
                Thread.sleep(SLEEPDELAY);

                tmpPath = config.getTmpDir().getAbsolutePath();
                jarFilePath = jarFile.getAbsolutePath();
                keystorePath = config.getProperty(XWPropertyDefs.SSLKEYSTORE);
                configPath = config.getProperty(XWPropertyDefs.CONFIGFILE);
                xwcp = config.getProperty(XWPropertyDefs.XWCP);
                if (tmpPath.endsWith("/") || tmpPath.endsWith("\\")) {
                    tmpPath = tmpPath.substring(0, tmpPath.length() - 1);
                }
                if (jarFilePath.endsWith("/") || jarFilePath.endsWith("\\")) {
                    jarFilePath = jarFilePath.substring(0, jarFilePath.length() - 1);
                }
                if (keystorePath.endsWith("/") || keystorePath.endsWith("\\")) {
                    keystorePath = keystorePath.substring(0, keystorePath.length() - 1);
                }
                if (configPath.endsWith("/") || configPath.endsWith("\\")) {
                    configPath = configPath.substring(0, configPath.length() - 1);
                }
                if (xwcp.endsWith("/") || xwcp.endsWith("\\")) {
                    xwcp = xwcp.substring(0, xwcp.length() - 1);
                }

                if (OSEnum.getOs().isWin32()) {
                    tmpPath = "\"" + tmpPath + "\"";
                    jarFilePath = "\"" + jarFilePath + "\"";
                    keystorePath = "\"" + keystorePath + "\"";
                    configPath = "\"" + configPath + "\"";
                    xwcp = "\"" + xwcp + "\"";
                }

                final String hwmem = System.getProperty(XWPropertyDefs.HWMEM.toString()) == "" ? ""
                        : " -D" + XWPropertyDefs.HWMEM + "=" + System.getProperty(XWPropertyDefs.HWMEM.toString());
                final String log4jconf = System.getProperty(XWPropertyDefs.LOG4JCONFIGFILE.propertyName()) == null ?
                        "" : " -D" + XWPropertyDefs.LOG4JCONFIGFILE.propertyName() + "=" + System.getProperty(XWPropertyDefs.LOG4JCONFIGFILE.propertyName());
                final String loglevel = System.getProperty(XWPropertyDefs.LOGGERLEVEL.propertyName()) == null ?
                        "" : " -D" + XWPropertyDefs.LOGGERLEVEL.propertyName() + "=" + System.getProperty(XWPropertyDefs.LOGGERLEVEL.propertyName());

                final StringBuilder javaOpts = new StringBuilder(" -D" + XWPropertyDefs.CACHEDIR.propertyName() + "=" + tmpPath
                        + log4jconf + loglevel
                        + " -D" + XWPropertyDefs.JAVALIBPATH.propertyName() + "=" + tmpPath + hwmem
                        + " -D" + XWPropertyDefs.XWCP.propertyName() + "=" + xwcp
                        + " -D" + XWPropertyDefs.JAVAKEYSTORE.propertyName() + "=" + keystorePath
                        + " -cp " + jarFilePath + File.pathSeparator + (javacp != null ? javacp : "")
                        + " xtremweb.worker.Worker "
                        + " --xwconfig " + configPath);

                if (OSEnum.getOs().isWin32()) {
                    javaCmd.append(" -Xrs ");
                }

                final String serveurOpt = " -server ";
                final String cmd = javaCmd.toString() + serveurOpt + javaOpts.toString();

                logger.config("Executing " + cmd);
                final FileInputStream in = null;
                exec = new Executor(cmd, binDir.getAbsolutePath(), in, System.out, System.err,
                        Long.parseLong(config.getProperty(XWPropertyDefs.TIMEOUT)));
                int rc = exec.startAndWait();
                XWReturnCode returnCode = XWReturnCode.fromInt(rc);
                logger.config("returnCode = " + returnCode + " (" + rc + ")");

                if (returnCode == XWReturnCode.RESTART) {
                    continue;
                }

                if (returnCode != XWReturnCode.SUCCESS) {

                    final String cmd1 = javaCmd.toString() + javaOpts.toString();

                    logger.config("Trying to launch the worker without \"" + serveurOpt + "\" java option : " + cmd1);
                    exec = new Executor(cmd1, binDir.getAbsolutePath(), in, System.out, System.err,
                            Long.parseLong(config.getProperty(XWPropertyDefs.TIMEOUT)));
                    rc = exec.startAndWait();
                    returnCode = XWReturnCode.fromInt(rc);
                }

                if (returnCode == XWReturnCode.RESTART) {
                    continue;
                }

                if (returnCode != XWReturnCode.SUCCESS) {
                    XWTools.fatal("We can't launch the worker : return code = " + returnCode + " (" + rc + ")"
                            + "\n(maybe URL launcher is not set properly or does not point to server version...)"
                            + "\n(maybe config file is corrupted...)");
                }
            } catch (final Exception e) {
                logger.exception(e);
                logger.error(e.toString());
            } finally {
                if (exec != null) {
                    try {
                        logger.info("Stopping process");
                        exec.stop();
                    } catch (final Exception e) {
                    }
                }
                exec = null;
                tmpPath = null;
                jarFilePath = null;
                keystorePath = null;
                configPath = null;
                xwcp = null;
                javacp = null;
            }
        }
    }

    public static void main(final String[] argv) {
        try {
            new HTTPLauncher(argv);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * This retrieves the default communication client and initializes it
     *
     * @return the default comm client
     */
    private CommClient commClient() throws ConnectException {

        CommClient commClient = null;
        try {
            commClient = config.defaultCommClient();
        } catch (final Exception e) {
            throw new ConnectException(e.toString());
        }
        return commClient;
    }
}
