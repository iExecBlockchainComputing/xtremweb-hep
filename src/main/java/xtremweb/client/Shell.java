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

package xtremweb.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.text.ParseException;

import org.apache.log4j.Logger;


import org.xml.sax.SAXException;

import xtremweb.common.StreamIO;
import xtremweb.common.XWReturnCode;
import xtremweb.communications.Connection;
import xtremweb.communications.SmartSocketsProxy;
import xtremweb.communications.XMLRPCCommand;

/**
 * This class implements a simple TCP server that waits for incoming
 * connections. This is not multi threaded and can manage one connection at a
 * time only. <br />
 * Incoming connection must send a valid XMLRPCCommand <br />
 * Created: Sept 14th, 2010
 *
 * @author Oleg Lodygensky
 * @since 7.0.0
 */

public class Shell extends Thread {

	private static final Logger logger = Logger.getLogger(Shell.class);

	/**
	 * This is this thread name
	 */
	private static final String NAME = "Shell";
	/**
	 * This is the secured socket server
	 */
	private ServerSocket socketServer = null;

	private int port;
	private Client client;

	/**
	 * This constructs a new instance
	 *
	 * @param label
	 *            is this thread label
	 */
	public Shell(final String label) {
		super(label);
		port = -1;
	}

	/**
	 * This constructs a new instance
	 */
	public Shell() {
		this(NAME);
	}

	/**
	 * This initializes communications
	 */
	public void initComm(final Client c) {

		client = c;

		try {
			final String proptxt = System.getProperty(Connection.XMLRPCPORT.toString());
			if (proptxt != null) {
				port = Integer.parseInt(proptxt.trim());
			}

			socketServer = new ServerSocket(port);

			Runtime.getRuntime().addShutdownHook(new Thread(getName() + "Cleaner") {
				@Override
				public void run() {
					cleanup();
				}
			});
		} catch (final Exception e) {
			logger.error(getName() + ": could not listen on port " + port + " : " + e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}
	}

	/**
	 * This indefinitely waits for incoming connections<br />
	 */
	@Override
	public void run() {

		logger.info("started, listening on port : " + port);

		while (true) {

			try (Socket socket = socketServer.accept()) {
				process(socket);
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}

	}

	/**
	 * This reads from socket input stream
	 */
	private void process(final Socket socket) throws ParseException, IOException {

		try (final StreamIO io = new StreamIO(new DataOutputStream(socket.getOutputStream()),
				new DataInputStream(socket.getInputStream()), socket.getSendBufferSize(), false);
				PrintStream printStream = new PrintStream(socket.getOutputStream());) {
			client.setPrintStream(printStream);

			final XMLRPCCommand cmd = XMLRPCCommand.newCommand(io);
			if (cmd.getUser() == null) {
				printStream.println("ERROR : user must be set");
			} else {
				client.sendCommand(cmd, true);
			}

		} catch (final ClassNotFoundException | InvalidKeyException | AccessControlException | InstantiationException
				| SAXException e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This is called on program termination (CTRL+C) This deletes session from
	 * server
	 */
	protected void cleanup() {
		try {
			logger.debug("cleanup");
			socketServer.close();
		} catch (final Exception e) {
			logger.error("can't clean up", e);
		}
	}

	/*
	 * This is for testing only <br /> Usage : java xtremweb.worker.ThreadProxy
	 * <server address> <port>
	 */
	public static void main(final String[] args) throws Exception {
		SmartSocketsProxy proxy = null;
		try {
			proxy = new SmartSocketsProxy(args[0], args[1], Integer.parseInt(args[2]), Boolean.parseBoolean(args[3]));
		} catch (final ArrayIndexOutOfBoundsException e) {
			System.out.println(
					"Usage : java xtremweb.worker.ThreadProxy <hub address> <server address> <port> <true|false>");
			System.exit(1);
		}
		proxy.start();
	}
}
