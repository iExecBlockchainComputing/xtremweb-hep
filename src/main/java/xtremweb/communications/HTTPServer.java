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

package xtremweb.communications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.commons.httpclient.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.apache.log4j.Logger;


import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWReturnCode;
import xtremweb.common.XWRole;

/**
 * <p>
 * This class implements a generic HTTP server<br />
 * This instanciates a new CommHandler on each new connection.
 * </p>
 * <p>
 * This is subject to replace WebServer so that all our servers extend
 * CommServer
 * </p>
 * <p>
 * This uses org.eclipse.jetty.Server where everything is done.<br />
 * </p>
 *
 * <p>
 * Created: March 30th, 2006
 * </p>
 *
 * @see CommServer
 * @author Oleg Lodygensky
 * @since RPCXW
 */

public class HTTPServer extends CommServer {

	private static final Logger logger = Logger.getLogger(HTTPServer.class);

	/**
	 * This is the Jetty HTTP server
	 */
	private final Server httpServer;

	/**
	 * This only calls super()
	 */
	public HTTPServer() {
		super("HTTPServer");
		httpServer = new Server();
	}

	/**
	 * This initializes communications
	 *
	 * @see CommServer#initComm(XWConfigurator, Handler)
	 */
	@Override
	public void initComm(final XWConfigurator prop, final Handler handler) throws RemoteException {

		setPort(Connection.HTTPPORT.defaultPortValue());

		if (prop.getRole() == XWRole.WORKER) {
			setPort(Connection.HTTPWORKERPORT.defaultPortValue());
		}
		try {
			final int httpPort = (prop.getRole() == XWRole.WORKER
					? Integer.parseInt(prop.getProperty(Connection.HTTPPORT.toString()))
					: Integer.parseInt(prop.getProperty(Connection.HTTPWORKERPORT.toString())));

			setPort(httpPort);

			final File keyFile = prop.getKeyStoreFile();

			if ((keyFile == null) || (!keyFile.exists()) || (prop.getRole() == XWRole.WORKER)) {

				logger.warn("unsecured communications : not using SSL");

				final HttpConfiguration httpConfig = new HttpConfiguration();
				httpConfig.setSecureScheme("https");
				httpConfig.setSecurePort(8443);
				httpConfig.setOutputBufferSize(32768);

				final ServerConnector http = new ServerConnector(httpServer, new HttpConnectionFactory(httpConfig));
				http.setPort(getPort());
				http.setIdleTimeout(30000);
				httpServer.setConnectors(new Connector[] { http });
			} else {
				initSecuredLayer(prop);
			}

			setHandler(handler);
			final HandlerCollection handlers = new HandlerCollection();
			httpServer.setHandler(handlers);
			this.addHandler("/", getHandler());
			Runtime.getRuntime().addShutdownHook(new Thread(getName() + "Cleaner") {
				@Override
				public void run() {
					cleanup();
				}
			});

			logger.info("started, listening on port : " + getPort());
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			logger.error(getName() + ": could not listen on port " + getPort() + " : " + e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}
	}

	/**
	 * This initializes the secured layer
	 * 
	 * @param prop
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	private void initSecuredLayer(final XWConfigurator prop)
			throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
		final String password = prop.getProperty(XWPropertyDefs.SSLKEYPASSWORD);
		final String passphrase = prop.getProperty(XWPropertyDefs.SSLKEYPASSPHRASE);
		final File fstore = File.createTempFile("xwcacert", null);
		try (final FileOutputStream sstore = new FileOutputStream(fstore)) {
			final int httpsPort = Integer.parseInt(prop.getProperty(Connection.HTTPSPORT.toString()));
			setPort(httpsPort);

			fstore.deleteOnExit();
			final KeyStore store = prop.getKeyStore();
			store.store(sstore, password.toCharArray());
			logger.debug("HTTPS keystore = " + fstore.getCanonicalPath());
			final SslContextFactory sslContextFactory = new SslContextFactory(fstore.getCanonicalPath());
			sslContextFactory.setKeyStorePassword(password);
			sslContextFactory.setKeyManagerPassword(passphrase);
			sslContextFactory.setTrustStore(store);
			sslContextFactory.setTrustStorePassword(password);
			sslContextFactory.setWantClientAuth(true);

			final HttpConfiguration http_config = new HttpConfiguration();
			http_config.setSecureScheme("https");
			http_config.setSecurePort(getPort());
			http_config.setOutputBufferSize(32768);

			final HttpConfiguration https_config = new HttpConfiguration(http_config);
			final SecureRequestCustomizer src = new SecureRequestCustomizer();
			src.setStsMaxAge(2000);
			src.setStsIncludeSubDomains(true);
			https_config.addCustomizer(src);

			final ServerConnector https = new ServerConnector(httpServer,
					new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
					new HttpConnectionFactory(https_config));
			https.setPort(getPort());
			https.setIdleTimeout(500000);

			httpServer.setConnectors(new Connector[] { https });

		}
	}

	/**
	 * This adds an handler
	 *
	 * @param h
	 *            is the handler to add
	 */
	public void addHandler(final Handler h) {
		try {
			logger.debug("addHandler " + h.getClass());
			final HandlerCollection handlers = (HandlerCollection) httpServer.getHandler();
			handlers.addHandler(h);
		} catch (final Exception e) {
			logger.error("Caught exception, Can't add handler", e);
		}
	}

	/**
	 * This adds an resource handler which will answer from HTML files
	 *
	 * @param contextPath
	 *            is the path of the url accessed
	 * @param h
	 *            is the handler to add
	 */
	public void addHandler(final String contextPath, final Handler h) {
		final ContextHandler context = new ContextHandler();
		context.setContextPath(contextPath);
		context.setHandler(h);
		addHandler(context);
	}

	/**
	 * This adds an resource handler which will answer from HTML files
	 *
	 * @param contextPath
	 *            is the path of the url accessed
	 * @param lpath
	 *            is the local path where to find HTML files
	 */
	public void addHandler(final String contextPath, final String lpath) {
		final ContextHandler context = new ContextHandler();
		context.setContextPath(contextPath);
		context.setResourceBase(lpath);
		context.setHandler(new ResourceHandler());
		addHandler(context);
	}

	/**
	 * This adds an resource handler which will answer from HTML files
	 *
	 * @param contextPath
	 *            is the path of the url accessed
	 * @param path
	 *            is the local path where to find HTML files
	 */
	public void addHandler(final String contextPath, final File path) {
		final ContextHandler context = new ContextHandler();
		context.setContextPath(contextPath);
		context.setResourceBase(path.getAbsolutePath());
		context.setHandler(new ResourceHandler());
		addHandler(context);
	}

	/**
	 * This calls httpServer#start()
	 */
	@Override
	public void run() {

		try {
			httpServer.start();
			logger.info("started, listening on port : " + getPort());
			httpServer.join();
		} catch (final Exception e) {
			logger.error("Caught exception, can't start HTTP server : " + getPort(), e);
		}
	}

	/**
	 * This is called on program termination (CTRL+C) This deletes session from
	 * server
	 */
	protected void cleanup() {
		try {
			logger.debug("cleanup");
			httpServer.getHandler().stop();
		} catch (final Exception e) {
			logger.error("can't clean up");
		}
	}

}
