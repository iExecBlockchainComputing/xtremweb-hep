package xtremweb.communications;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;


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
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;

/**
 * This is a general purpose class, independent from XWHEP. This class
 * implements a socket proxy. This listens from incoming socket IP:port and
 * forwards to output IP:port. This may be useful, in conjunction to IP aliases,
 * to pretend there's a remote host listening.
 *
 * @author Oleg Lodygensky
 * @since 8.3.0
 */
public final class SocketProxy extends Thread {

	private static final Logger logger = Logger.getLogger(SocketProxy.class);

	/**
	 * This class implements the reading thread
	 */
	private class ProxyThread extends Thread {
		/**
		 * This defines the thread label
		 */
		private static final String DEFAULT_LABEL = "ProxyThread";

		/**
		 * This is the incoming socket
		 */
		private final Socket inSocket;
		/**
		 * This is the outgoing socket
		 */
		private final Socket outSocket;
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
			this.inSocket = s;
			this.outSocket = s;
			this.inputStream = in;
			this.outputStream = out;
		}

		/**
		 * This is the constructor
		 *
		 * @param n
		 *            is this thread name
		 * @param out
		 *            is the incoming connection output stream
		 * @throws IOException
		 *             on general I/O error
		 * @throws UnknownHostException
		 *             on connection error
		 */
		private ProxyThread(final String n, final DataInputStream in, final DataOutputStream out)
				throws UnknownHostException, IOException {
			super(n);
			this.inSocket = null;
			this.outSocket = null;
			this.inputStream = in;
			this.outputStream = out;
		}

		/**
		 * This tests if the incoming connection socket is closed
		 *
		 * @return incoming socket.isClised()
		 */
		private boolean isClosed() {
			if (inSocket != null) {
				if (inSocket.isClosed()) {
					logger.trace("insocket is closed");
				}
				return inSocket.isClosed();
			}
			if (outSocket != null) {
				if (outSocket.isClosed()) {
					logger.trace("outsocket is closed");
				}
				return outSocket.isClosed();
			}
			return true;
		}

		/**
		 * This is the main loop
		 */
		@Override
		public void run() {
			if ((inSocket == null) && (outSocket == null)) {
				logger.error("initialization error : sockets are null!!!");
			}

			final byte[] buffer = new byte[XWTools.PACKETSIZE];
			Date now = null;
			long start = -1;
			long last = -1;
			final long timeout = Long.parseLong(XWPropertyDefs.SOTIMEOUT.defaultValue());
			now = new Date();
			start = now.getTime();
			now = null;
			while (!isClosed()) {
				try {
					int n = 0;
					do {
						n = inputStream.read(buffer);
						if (n != -1) {
							outputStream.write(buffer, 0, n);
							outputStream.flush();
							logger.trace("forwarded = " + n);
							now = new Date();
							start = now.getTime();
							now = null;
						}
					} while (n > 0);
					now = new Date();
					last = now.getTime();
					now = null;
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
	 * This defines the thread label
	 */
	private static final String DEFAULT_LABEL = "SocketProxy";
	/**
	 * This defines the reader thread label
	 */
	private static final String READER_LABEL = DEFAULT_LABEL + "_reader";
	/**
	 * This defines the writer thread label
	 */
	private static final String WRITER_LABEL = DEFAULT_LABEL + "_writer";
	/**
	 * This is this thread name header
	 */
	public static final String NAME = "ThreadProxy";
	/**
	 * This tells if this proxy acts as server or as client.
	 */
	private final ServerSocket server;
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
	 * This tells if this proxy must continue to work
	 */
	private boolean continuer;
	/**
	 * This is the output address to forward
	 */
	private InetAddress outputAddr;
	/**
	 * This is the input port to forward
	 */
	private final int outputPort;

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
	 * mode this listen from a socket VirtualSocket and forwards to local port.
	 * On client mode this listen from local port and forwards to a socket
	 * VirtualSocket.
	 *
	 * @param inputIPAddr
	 *            is the socket hub address
	 * @param inPort
	 *            is the input port
	 * @param outputIPAddr
	 *            is the socket hub address
	 * @param outPort
	 *            is the output port
	 * @throws MalformedAddressException
	 *             if hubAddr is not correct
	 * @throws InitializationException
	 *             on initialization error
	 * @throws IOException
	 *             on I/O error
	 */
	public SocketProxy(final String inputIPAddr, final int inPort, final String outputIPAddr, final int outPort)
			throws IOException {

		super(DEFAULT_LABEL);

		InetAddress inputAddr = InetAddress.getLocalHost();
		if (inputIPAddr != null) {
			inputAddr = InetAddress.getByName(inputIPAddr);
		}
		this.server = new ServerSocket(inPort, 0, inputAddr);

		outputAddr = InetAddress.getLocalHost();
		if (outputIPAddr != null) {
			outputAddr = InetAddress.getByName(outputIPAddr);
		}
		this.outputPort = outPort;

		this.continuer = true;
		logger.info("Started on " + inputAddr + ":" + inPort + " => " + outputAddr + ":" + outputPort);
	}

	/**
	 * This calls server() if server == true, client() otherwise
	 */
	@Override
	public void run() {
		logger.debug("Starting socket proxy");

		while (!mustStop()) {
			logger.debug("Waiting for incoming connections");

			try (final Socket incoming = server.accept();
					final Socket outputSocket = new Socket(outputAddr, outputPort);
					final DataInputStream outgoingIn = new DataInputStream(outputSocket.getInputStream());
					final DataOutputStream outgoingOut = new DataOutputStream(outputSocket.getOutputStream());
					final DataInputStream incomingIn = new DataInputStream(incoming.getInputStream());
					final DataOutputStream incomingOut = new DataOutputStream(incoming.getOutputStream())) {

				logger.debug("Incoming connection");

				final ProxyThread reader = new ProxyThread(READER_LABEL, incoming, incomingIn, outgoingOut);
				reader.start();

				final ProxyThread writer = new ProxyThread(WRITER_LABEL, outputSocket, outgoingIn, incomingOut);
				writer.start();

				logger.debug("Started");
			} catch (final IOException e) {
				logger.error("Caught exception, socket proxy error", e);
			}
		}
	}

	/**
	 * This is for testing only Usage : java xtremweb.worker.ThreadProxy <hub
	 * address> <server address> <port> <true|false>
	 */
	public static void main(final String[] args) throws Exception {
		SocketProxy proxy = null;
		try {
			proxy = new SocketProxy(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
		} catch (final ArrayIndexOutOfBoundsException e) {
			System.out.println(
					"Usage : java -cp xtremweb.jar xtremweb.worker.SocketProxy <incoming IP address> <incoming port> <outgoing IP address> <outgoing port>");
			System.exit(1);
		}
		proxy.start();
	}
}
