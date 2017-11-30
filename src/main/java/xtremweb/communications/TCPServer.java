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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.eclipse.jetty.server.Handler;

import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWReturnCode;

/**
 * <p>
 * This class implements a generic TCP server<br />
 * This instanciates a new CommHandler on each new connection.
 * </p>
 *
 * <p>
 * Created: Jun 7th, 2005
 * </p>
 *
 * @see CommHandler
 * @author Oleg Lodygensky
 * @since RPCXW
 */

public class TCPServer extends CommServer {

	private static final Logger logger = Logger.getLogger(TCPServer.class);

	/**
	 * This is this thread name if client auth is expected
	 *
	 * @since 7.4.0
	 */
	private static final String SNAME = "TCPSServer";
	/**
	 * This is this thread name
	 */
	private static final String NAME = "TCPServer";
	/**
	 * This is the secured socket server
	 */
	private SSLServerSocket sslSocketServer = null;
	/**
	 * This is the NIO server
	 */
	private ServerSocketChannel nioSocketServer = null;
	/**
	 * This is the NIO socket selector
	 */
	private Selector acceptSelector = null;
	/**
	 * This is the NIO socket key
	 */
	private SelectionKey acceptKey = null;
	/**
	 * This tells whether to use NIO or not This contains value is read from
	 * config file
	 */
	private boolean nio;
	/**
	 * This tells whether client authentication is expected. If true, this
	 * server will listen on ConnectionTPCS. If false, this server will listen
	 * on ConnectionTPC.
	 *
	 * @since 7.4.0
	 * @see #Connection#TCP
	 * @see #Connection#TCPS
	 */
	private boolean needClientAuthentication;

	/**
	 * This constructs a new instance
	 *
	 * @param label
	 *            is this thread label
	 */
	protected TCPServer(final String label) {
		super(label);
		nio = true;
		needClientAuthentication = false;
	}

	/**
	 * This constructs a new instance. Depending on provided parameter, this
	 * server will listen on different ports.
	 *
	 * @param needAuth
	 *            is true if client authentication is expected
	 * @since 7.4.0
	 * @see #needClientAuthentication
	 */
	public TCPServer(final boolean needAuth) {
		this(NAME);
		needClientAuthentication = needAuth;
		if (needClientAuthentication) {
			setName(SNAME);
		}
	}

	/**
	 * This calls this(false)
	 *
	 * @see #TCPServer(boolean)
	 */
	public TCPServer() {
		this(false);
	}

	/**
	 * This initializes communications
	 *
	 * @see CommServer#initComm(XWConfigurator, Handler)
	 */
	@Override
	public void initComm(final XWConfigurator prop, final Handler handler) throws RemoteException {

		super.initComm(prop, handler);

		setPort(Connection.TCPPORT.defaultPortValue());

		try {
			if (needClientAuthentication) {
				setPort(prop.getPort(Connection.TCPSPORT));
			} else {
				setPort(prop.getPort(Connection.TCPPORT));
			}

			nio = prop.nio();

			setName(NAME + (nio ? "NIO" : ""));

			final File keyFile = prop.getKeyStoreFile();

			if ((keyFile == null) || !keyFile.exists()) {
				logger.warn("unsecured communications : not using SSL");
				if (nio) {
					nioSocketServer = ServerSocketChannel.open();
					nioSocketServer.configureBlocking(false);
					nioSocketServer.socket().bind(new InetSocketAddress(getPort()));
					acceptSelector = SelectorProvider.provider().openSelector();
					acceptKey = nioSocketServer.register(acceptSelector, SelectionKey.OP_ACCEPT);
				} else {
					sslSocketServer = (SSLServerSocket) new ServerSocket(getPort());
				}
			} else {
				try {
					final SSLContext sslContext = SSLContext.getInstance("SSLv3");
					sslContext.init(prop.getKeyManagerFactory().getKeyManagers(), null, null);

					final SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
					sslSocketServer = (SSLServerSocket) factory.createServerSocket(getPort());

					sslSocketServer.setNeedClientAuth(needClientAuthentication);

					nio = false;
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
					logger.error("Can't init SSL : " + e.toString());
					System.exit(XWReturnCode.FATAL.ordinal());
				}
			}

			Runtime.getRuntime().addShutdownHook(new Thread(getName() + "Cleaner") {
				@Override
				public void run() {
					cleanup();
				}
			});
		} catch (final Exception e) {
			logger.error(getName() + ": could not listen on port " + getPort() + " : " + e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}
	}

	/**
	 * This indefinitly waits for incoming connections<br />
	 * This uses the CommHandler to handle connections
	 *
	 * @see CommServer#handler
	 */
	@Override
	public void run() {

		logger.info("started, listening on port : " + getPort());
		int loop = 0;

		while (true) {

			try {
				if ((++loop % 1000) == 0) {
					loop = 0;
					System.gc();
				}
				if (nio) {
					int keysAdded = 0;
					while ((keysAdded = acceptSelector.select()) > 0) {

						// Someone is ready for I/O, get the ready keys
						final Set readyKeys = acceptSelector.selectedKeys();
						final Iterator i = readyKeys.iterator();

						// Walk through the ready keys collection and process
						// date requests.
						while (i.hasNext()) {
							final SelectionKey sk = (SelectionKey) i.next();
							i.remove();
							// The key indexes into the selector so you
							// can retrieve the socket that's ready for I/O
							final ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();

							final CommHandler h = popConnection();
							final Socket socket = nextReady.accept().socket();
							setRemoteName(socket.getInetAddress().getHostName());
							setRemoteIP(socket.getInetAddress().getHostAddress());
							setRemotePort(socket.getPort());
							socket.setSoTimeout(getConfig().getInt(XWPropertyDefs.SOTIMEOUT));
							h.setCommServer(this);
							h.setSocket(socket);
							h.start();
						}
					}
				} else {
					SSLSocket socket = null;

					logger.debug(msgWithRemoteAddresse("Connection management : accepting (" + getId() + ")"));
					try {
						socket = (SSLSocket) sslSocketServer.accept();
						socket.setSoTimeout(getConfig().getInt(XWPropertyDefs.SOTIMEOUT));
						setRemoteName(socket.getInetAddress().getHostName());
						setRemoteIP(socket.getInetAddress().getHostAddress());
						setRemotePort(socket.getPort());
					} catch (final Exception e) {
						setRemoteName(null);
						setRemoteIP(null);
						logger.error("Caught exception: ", e);
						break;
					}
					CommHandler h = popConnection();
					try {
						h.setCommServer(this);
						h.setSocket(socket);
					} catch (final Exception e) {
						logger.error(msgWithRemoteAddresse("new connection exception"));
						logger.error("Caught exception: ", e);
						socket.close();
						h.resetSockets();
						pushConnection(h);
					} finally {
						setRemoteName(null);
						setRemoteIP(null);
						socket = null;
						h = null;
					}
				}
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}

	}

	/**
	 * This is called on program termination (CTRL+C) This deletes session from
	 * server
	 */
	protected void cleanup() {
		try {
			logger.debug("cleanup");
			if (nio) {
				nioSocketServer.close();
			} else {
				sslSocketServer.close();
			}
		} catch (final Exception e) {
			logger.error("can't clean up");
		}
	}

}
