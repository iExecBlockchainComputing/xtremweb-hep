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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLHandshakeException;

import xtremweb.communications.CommClient;
import xtremweb.exec.Executor;

/**
 * This launches XtremWeb from its JAR file. This first retreive URL from config
 * file, if any. If the config file contains no URL, this contructs a new URL
 * with the server found in the config file.
 *
 * URL must be like "http://aServer/aFile.jar. Default URL is
 * "http://aServer/XWHEP/download/xtremweb.jar"
 */
public final class HTTPLauncher {

	private static final Logger logger = Logger.getLogger(HTTPLauncher.class);
	private static final int SLEEPDELAY = 2500;
	private XWConfigurator config;

	/**
	 * This retreives the default comm client and initializes it
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

	/**
	 * Creates a new <code>HTTPLauncher</code> instance.
	 *
	 * @param argv
	 *            a <code>String[]</code> value : command line arguments which
	 *            specificies the config file name
	 */
	private HTTPLauncher(final String[] a) {
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
			libDir = new File(System.getProperty(XWPropertyDefs.TMPDIR.toString()));
		}
		File binDir = new File(rootDir, "bin");
		if (!binDir.exists()) {
			binDir = new File(System.getProperty(XWPropertyDefs.TMPDIR.toString()));
		}

		while (true) {
			logger.debug("launcherURL = " + url);
			logger.debug("Current version : " + CommonVersion.getCurrent());

			boolean upgrade = false;

			try {
				serverVersion = commClient().version();
				logger.debug("Server version : " + serverVersion);

				if (serverVersion != null) {

					final File newJarFile = new File(libDir, XWTools.JARFILENAME + "-" + serverVersion);

					if (!newJarFile.exists()
							&& !serverVersion.toString().equals(CommonVersion.getCurrent().toString())) {
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
				logger.error("SSL error (maybe we have received a new keystore: relaunch is then necessary) : " + e);
				System.exit(XWReturnCode.FATAL.ordinal());
			} catch (final Exception e) {
				upgrade = false;
				logger.error("Caught exception: ", e);
			}

			if ((upgrade) && (url != null)) {
				logger.info("Downloading xwhep JAR file");
				try (StreamIO io = new StreamIO(null, new DataInputStream(url.openStream()), false)) {
					logger.debug("" + jarFile + ".exists() = " + jarFile.exists());

					io.readFileContent(jarFile);
					io.close();
				} catch (final FileNotFoundException e) {
					logger.error("Can't download " + XWTools.JARFILENAME + " : " + e);
					System.exit(XWReturnCode.FATAL.ordinal());
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
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
			String javaCmd = "java ";

			try {
				logger.debug("00 libDir = " + libDir.getCanonicalPath());
				if (config.getProperty(XWPropertyDefs.XWCP) != null) {
					libDir = new File(config.getProperty(XWPropertyDefs.XWCP));
					if (libDir.isFile()) {
						libDir = libDir.getParentFile();
					}
				}
				logger.info("libDir = " + libDir.getCanonicalPath());

				if ((jarFile == null) || !jarFile.exists()) {
					jarFile = new File(libDir, XWTools.JARFILENAME);
				}

				logger.info("jarFile = " + jarFile.getCanonicalPath());
				Thread.sleep(SLEEPDELAY);

				tmpPath = config.getProperty(XWPropertyDefs.TMPDIR);
				jarFilePath = jarFile.getCanonicalPath();
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
				final String javaOpts = " -Dxtremweb.cache=" + tmpPath + " -Djava.library.path=" + tmpPath + hwmem
						+ " -Dxtremweb.cp=" + xwcp + " -Djavax.net.ssl.trustStore=" + keystorePath + " -cp "
						+ jarFilePath + File.pathSeparator + (javacp != null ? javacp : "") + " xtremweb.worker.Worker "
						+ " --xwconfig " + configPath;

				if (OSEnum.getOs().isWin32()) {
					javaCmd += " -Xrs ";
				}

				final String serveurOpt = " -server ";
				final String cmd = javaCmd + serveurOpt + javaOpts;

				logger.info("Executing " + cmd);
				final FileInputStream in = null;
				exec = new Executor(cmd, binDir.getCanonicalPath(), in, System.out, System.err,
						Long.parseLong(config.getProperty(XWPropertyDefs.TIMEOUT)));
				int rc = exec.startAndWait();
				XWReturnCode returnCode = XWReturnCode.fromInt(rc);
				logger.info("returnCode = " + returnCode + " (" + rc + ")");

				if (returnCode == XWReturnCode.RESTART) {
					continue;
				}

				if (returnCode != XWReturnCode.SUCCESS) {

					final String cmd1 = javaCmd + javaOpts;

					logger.info("Trying to launch the worker without \"" + serveurOpt + "\" java option : " + cmd1);
					exec = new Executor(cmd1, binDir.getCanonicalPath(), in, System.out, System.err,
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
				logger.error("Caught exception: ", e);
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
		new HTTPLauncher(argv);
	}
}
