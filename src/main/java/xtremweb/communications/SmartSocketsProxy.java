package xtremweb.communications;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import javax.net.SocketFactory;

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

import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;

import org.apache.log4j.Logger;


/**
 * This class implements a SmartSockets proxy. In server mode, this listen from
 * SmartSockets port and forwards to local port. To do so this registers to a
 * SmartSockets hub, listens from a VirtualSocket and forwards received stream
 * to a given port on localhost. This mode can be used by the XWHEP worker when
 * running a server like job (listening to a port) so that such a job can be
 * reached from XWHEP client or workers. This mode may also be used by the XWHEP
 * client to receive connections from workers.
 *
 * In client mode, this is the contrary. This listens from a local port and
 * forwards to a server through SmartSockets connections. This mode can be used
 * by the XWHEP client to connect to a server like job running on a XWHEP
 * worker. This mode may also be used by the XWHEP worker to connect to client.
 *
 * @author Oleg Lodygensky
 * @since 8.0.0
 */
public final class SmartSocketsProxy extends Thread {

	private static final Logger logger = Logger.getLogger(SmartSocketsProxy.class);

	/**
	 * This class implements the reading thread
	 */
	private class ProxyThread extends Thread {
		/**
		 * This is the incoming connection socket
		 */
		private final Socket socket;
		/**
		 * This is the incoming connection socket
		 */
		private final VirtualSocket vsocket;
		/**
		 * This is the incoming connection input stream
		 */
		private DataInputStream inputStream;
		/**
		 * This is the outgoing connection output stream
		 */
		private DataOutputStream outputStream;

		/**
		 * This is the constructor
		 *
		 * @param n
		 *            is this thread name
		 * @param s
		 *            is the incoming connection socket
		 * @param in
		 *            is the incoming connection input stream
		 * @param out
		 *            is the incoming connection output stream
		 * @throws IOException
		 *             on general I/O error
		 * @throws UnknownHostException
		 *             on connection error
		 */
		private ProxyThread(final String n, final Socket s, final DataInputStream in, final DataOutputStream out)
				throws UnknownHostException, IOException {
			super(n);
			this.socket = s;
			this.vsocket = null;
			this.inputStream = in;
			this.outputStream = out;
		}

		/**
		 * This is the constructor
		 *
		 * @param n
		 *            is this thread name
		 * @param vs
		 *            is the incoming connection socket
		 * @param in
		 *            is the incoming connection input stream
		 * @param out
		 *            is the incoming connection output stream
		 * @throws IOException
		 *             on general I/O error
		 * @throws UnknownHostException
		 *             on connection error
		 */
		private ProxyThread(final String n, final VirtualSocket vs, final DataInputStream in,
				final DataOutputStream out) throws UnknownHostException, IOException {
			super(n);
			this.socket = null;
			this.vsocket = vs;
			this.inputStream = in;
			this.outputStream = out;
		}

		/**
		 * This tests if the incoming connection socket is closed
		 *
		 * @return incoming socket.isClised()
		 */
		private boolean isClosed() {
			if (socket != null) {
				logger.trace("socket is closed " + socket.isClosed());
				return socket.isClosed();
			}
			if (vsocket != null) {
				logger.trace("vsocket is closed " + vsocket.isClosed());
				return vsocket.isClosed();
			}
			return true;
		}

		/**
		 * This is the main loop
		 */
		@Override
		public void run() {
			if ((socket == null) && (vsocket == null)) {
				logger.error("initialization error : socket and vsocket are null!!!");
			}

			final byte[] buffer = new byte[XWTools.PACKETSIZE];
			long start;
			long last;
			final long timeout = Long.parseLong(XWPropertyDefs.SOTIMEOUT.defaultValue());
			{
				final Date now = new Date();
				start = now.getTime();
			}
			while (!isClosed()) {
				try {
					int n;
					do {
						n = inputStream.read(buffer);
						if (n != -1) {
							logger.debug("received = " + n);
							outputStream.write(buffer, 0, n);
							outputStream.flush();
							logger.debug("forwarded = " + n);
							{
								final Date now = new Date();
								start = now.getTime();
							}
						}
					} while (n > 0);

					final Date now = new Date();
					last = now.getTime();
					if ((last - start) > timeout) {
						throw new TimeoutException("Inactivity since " + (last - start));
					}
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
					break;
				}
			}
			try {
				inputStream.close();
			} catch (final Exception e) {
			}
			inputStream = null;
			try {
				outputStream.flush();
				outputStream.close();
			} catch (final Exception e) {
			}
			outputStream = null;
		}
	}

	/**
	 * This thread writes
	 */
	private ProxyThread reader;
	/**
	 * This thread reads
	 */
	private ProxyThread writer;
	/**
	 * This is this thread name header
	 */
	public static final String NAME = "ThreadProxy";
	/**
	 * This is the proxy thread name
	 */
	private final String proxyName;
	/**
	 * This is the reader thread name
	 */
	private final String readerName;
	/**
	 * This is the writer thread name
	 */
	private final String writerName;
	/**
	 * This tells if this proxy acts as server or as client.
	 */
	private final boolean server;
	/**
	 * This is the port this proxy listens on client mode
	 */
	private int listenPort;

	/**
	 * This retrieve the port this proxy is listening to. This is only set for
	 * "client" proxy, aiming to forward connections to a remote server
	 * (typically running on client side).
	 *
	 * @return the port number, or -1 if not set
	 */
	public int getListenPort() {
		return listenPort;
	}

	/**
	 * This contains the connection properties
	 */
	private static HashMap<String, Object> connectProperties;
	/**
	 * This port this proxy is forwarding to
	 */
	private int forwardPort;

	/**
	 * forward port setter
	 */
	public int getForwardPort() {
		return forwardPort;
	}

	/**
	 * This is the virtual server socket
	 */
	private VirtualServerSocket vServerSocket;
	/**
	 * This is the virtual socket factory
	 */
	private final VirtualSocketFactory vSocketFactory;
	/**
	 * This is the virtual server socket
	 */
	private ServerSocket socketServer;
	/**
	 * This is the SmartSockets address of the server like application running
	 * on worker side.
	 */
	private VirtualSocketAddress vServerAddress;
	/**
	 * This is the local address.
	 */
	private SocketAddress localAddress;

	/**
	 * This retrieves the local address this is listening to
	 *
	 * @return this local address
	 */
	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	/**
	 * This retrieves the local address this is listening to
	 *
	 * @return this local address
	 */
	public SocketAddress getRemoteAddress() {
		return vServerAddress;
	}

	/**
	 * This tells if this proxy must continue to work
	 */
	private boolean continuer;

	/**
	 * This tells if this thread should stop
	 *
	 * @return true if this thread should exit its main loop in run() method
	 */
	private synchronized boolean mustStop() {
		return continuer == false;
	}

	/**
	 * This sets this thread main loop test
	 *
	 * @param continuer
	 *            is true to tell this thread to exit its main loop
	 */
	public synchronized void setContinuer(final boolean continuer) {
		this.continuer = continuer;
	}

	/**
	 * This constructs a new proxy either on server or client mode. On server
	 * mode this listen from a SmartSockets VirtualSocket and forwards to local
	 * port. On client mode this listen from local port and forwards to a
	 * SmartSockets VirtualSocket.
	 *
	 * @param hubAddr
	 *            is the SmartSockets hub address
	 * @param serverAddr
	 *            is the SmartSockets server address; used on client mode only
	 * @param fport
	 *            on server mode, this is the port to forward to; on client mode
	 *            this is the port to listen to
	 * @param server
	 *            determines the behavior. If true, this proxy acts as server;
	 *            this proxy acts as client otherwise
	 * @throws MalformedAddressException
	 *             if hubAddr is not correct
	 * @throws InitializationException
	 *             on initialization error
	 * @throws IOException
	 *             on I/O error
	 */
	public SmartSocketsProxy(final String hubAddr, final String serverAddr, final int fport, final boolean server)
			throws MalformedAddressException, InitializationException, IOException {

		super(NAME + (server == true ? "Server" : "Client"));
		this.continuer = true;
		proxyName = NAME + (server == true ? "Server" : "Client");
		readerName = proxyName + "Reader";
		writerName = proxyName + "Writer";
		this.server = server;
		connectProperties = new HashMap<>();
		System.setProperty(Connection.HUBPNAME, hubAddr);
		vSocketFactory = VirtualSocketFactory.getDefaultSocketFactory();
		if (server) {
			listenPort = -1;
			forwardPort = fport;
			vServerSocket = vSocketFactory.createServerSocket(0, 0, connectProperties);
			localAddress = vServerSocket.getLocalSocketAddress();
			logger.debug(proxyName + "; forward port = " + forwardPort);
		} else {
			vServerAddress = new VirtualSocketAddress(serverAddr);
			forwardPort = -1;
			listenPort = fport;
			socketServer = new ServerSocket(listenPort);
			vServerSocket = null;
			localAddress = socketServer.getLocalSocketAddress();
			logger.debug(proxyName + "; listen port = " + listenPort);
		}
	}

	/**
	 * This calls server() if server == true, client() otherwise
	 */
	@Override
	public void run() {
		if (server) {
			server();
		} else {
			client();
		}
	}

	/**
	 * This loops until mustStop() == false on listening from SmartSockets port
	 * and forwarding to local port. This is typically run on XWHEP worker side
	 */
	public void server() {

		logger.debug("Starting SmartSockets proxy server");

		logger.info("Created server on " + localAddress);

		while (!mustStop()) {
			logger.debug("Server waiting for connections");

			try {
				final VirtualSocket incoming = vServerSocket.accept();
				final Socket outgoing = SocketFactory.getDefault().createSocket(XWTools.getLocalHostName(),
						forwardPort);

				try (final DataInputStream incomingIn = new DataInputStream(incoming.getInputStream());
						final DataOutputStream incomingOut = new DataOutputStream(incoming.getOutputStream());
						final DataInputStream outgoingIn = new DataInputStream(outgoing.getInputStream());
						final DataOutputStream outgoingOut = new DataOutputStream(outgoing.getOutputStream());) {

					logger.debug(
							"Incoming connection; forwarding to " + XWTools.getLocalHostName() + ":" + forwardPort);

					reader = new ProxyThread(readerName, incoming, incomingIn, outgoingOut);
					reader.start();

					writer = new ProxyThread(writerName, outgoing, outgoingIn, incomingOut);
					writer.start();

					logger.debug("Started");
				}
			} catch (final IOException e) {
				logger.error("Caught exception, SmartSockets server proxy", e);
			}
		}
	}

	/**
	 * This loops until mustStop() == false on listening from local port and
	 * forwarding to SmartSockets port This is typically run on XWHEP client
	 * side
	 */
	public void client() {

		logger.debug("Starting SmartSockets proxy client");

		logger.info("Created Client on " + localAddress);

		while (!mustStop()) {
			logger.debug("Client waiting for connections");

			try {
				final VirtualSocket outgoing = vSocketFactory.createClientSocket(vServerAddress,
						Integer.parseInt(System.getProperty(XWPropertyDefs.SOTIMEOUT.toString())), connectProperties);

				try (final Socket incoming = socketServer.accept();
						final DataInputStream incomingIn = new DataInputStream(incoming.getInputStream());
						final DataOutputStream incomingOut = new DataOutputStream(incoming.getOutputStream());
						final DataInputStream outgoingIn = new DataInputStream(outgoing.getInputStream());
						final DataOutputStream outgoingOut = new DataOutputStream(outgoing.getOutputStream());) {

					logger.debug("Incoming connection");

					reader = new ProxyThread(readerName, incoming, incomingIn, outgoingOut);
					reader.start();

					writer = new ProxyThread(writerName, outgoing, outgoingIn, incomingOut);
					writer.start();

					logger.debug("Started");
				}
			} catch (final IOException e) {
				logger.error("Caught exception, SmartSockets client proxy", e);
			}
		}
	}

	/**
	 * This is for testing only Usage : java xtremweb.worker.ThreadProxy <hub
	 * address> <server address> <port> <true|false>
	 */
	public static void main(final String[] args) throws Exception {
		SmartSocketsProxy proxy = null;
		try {
			proxy = new SmartSocketsProxy(args[0], args[1], Integer.parseInt(args[2]), Boolean.parseBoolean(args[3]));
		} catch (final ArrayIndexOutOfBoundsException e) {
			System.out.println(
					"Usage : java -cp xtremweb.jar xtremweb.worker.SmartSocketsProxy <hub address> <server address> <port> <true|false>");
			System.exit(1);
		}
		proxy.start();
	}
}
