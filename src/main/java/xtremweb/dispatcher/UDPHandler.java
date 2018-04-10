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

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.rmi.RemoteException;

import javax.net.ssl.SSLSocket;

import xtremweb.common.BytePacket;
import xtremweb.common.OSEnum;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.communications.CommHandler;
import xtremweb.communications.XMLRPCCommand;

/**
 * This handles incoming communications through UDP<br>
 * This answers request from UDPClient
 *
 * Created: August 2005
 *
 * @see xtremweb.communications.UDPClient
 * @author Oleg Lodygensky
 * @version RPCXW
 */

public class UDPHandler extends xtremweb.dispatcher.CommHandler {

	/**
	 * This aims to send datagram packets back to client
	 */
	private DatagramSocket serverSocket = null;
	/**
	 * This is the datagram packet used for I/O
	 */
	private DatagramPacket serverPacket;

	private boolean answerSent;

	/**
	 * This manages the receiving packet byte array
	 */
	private BytePacket packetIn;
	/**
	 * This manages the sending packet byte array
	 */
	private BytePacket packetOut;

	public static final String NAME = "UDPHandler";

	/**
	 * This is the default constructor which only calls super("UDPHandler")
	 */
	public UDPHandler() {
		super(NAME);
	}

	/**
	 * This is the default constructor which only calls super("UDPHandler")
	 */
	public UDPHandler(final XWConfigurator context) {
		this(NAME, context);
	}

	/**
	 * This is the default constructor which only calls super("UDPHandler")
	 */
	public UDPHandler(final String n, final XWConfigurator context) {
		super(n, context);
		packetIn = new BytePacket();
		packetOut = new BytePacket();

		answerSent = false;
	}

	/**
	 * This throws an exception since setSocket() is dedicated to TCP comms
	 *
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to TCP
	 *                comms
	 */
	@Override
	public void setSocket(final SSLSocket s) throws RemoteException {
		throw new RemoteException("setSocket is not implemented for UDP comms");
	}

	/**
	 * This throws an exception since setSocket() is dedicated to TCP comms
	 *
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to TCP
	 *                comms
	 */
	@Override
	public void setSocket(final Socket s) throws RemoteException {
		throw new RemoteException("setSocket is not implemented for UDP comms");
	}

	/**
	 */
	public void setPacket(final DatagramSocket s, final DatagramPacket p) throws RemoteException {
		serverPacket = p;
		packetIn.setData(p.getData());
		serverSocket = s;

		final boolean net = getConfig().getBoolean(XWPropertyDefs.OPTIMIZENETWORK);
		// mac os x don't like that :(
		if (net && !OSEnum.getOs().isMacosx()) {
			try {
				serverSocket.setTrafficClass(0x08); // maximize throughput
			} catch (final Exception e) {
				getLogger().warn(e.toString());
			}
		}
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramChannel c, final SocketAddress r, final BytePacket p) throws RemoteException {
		throw new RemoteException("UDPHandler#setPacket() UDP can't set packet");
	}

	/**
	 * This first packs the packet, then sends it
	 *
	 * @see xtremweb.common.BytePacket#pack()
	 */
	protected synchronized void send() throws RemoteException {
		if (answerSent) {
			notifyAll();
			return;
		}

		try {
			packetOut.pack();
			serverPacket.setData(packetOut.getData());
			serverSocket.send(serverPacket);

			answerSent = true;
		} catch (final Exception e) {
			notifyAll();
			throw new RemoteException(e.toString());
		}
		notifyAll();
	}

	/**
	 * @see xtremweb.communications.CommHandler#close()
	 */
	@Override
	public void close() {
		getLogger().info("UDPHandler#close() does nothing (this is normal)");
	}

	/**
	 * This sends the object
	 */
	@Override
	protected void write(final XMLable obj) throws IOException {
		try {
			packetOut.putObject(obj);
			send();
		} catch (final Exception e) {
			getLogger().exception(e);
			throw new IOException(e.toString());
		}
	}

	/**
	 * This writes a file to socket
	 *
	 * @param f
	 *            is the file to send
	 */
	@Override
	public void writeFile(final File f) throws IOException {
		throw new IOException("UDP does not implement writeFile");
	}

	/**
	 * This reads a file from socket This is typically needed after a
	 * workRequest to get stdin and/or dirin files
	 *
	 * @param f
	 *            is the file to store received bytes
	 */
	@Override
	public void readFile(final File f) throws IOException {
		throw new IOException("UDP does not implement readFile");
	}

	/**
	 * This is the main loop; this is called by java.lang.Thread#start() This
	 * executes one command form communication channel and exists
	 */
	@Override
	public void run() {

		try {
			packetOut.reset();
			answerSent = false;
			final XMLRPCCommand cmd = XMLRPCCommand.newCommand(packetIn.getString());

			final InetSocketAddress socket = (InetSocketAddress) (serverPacket.getSocketAddress());
			cmd.setRemoteName(socket.getHostName());
			cmd.setRemoteIP(socket.getAddress().getHostAddress());
			cmd.setRemotePort(socket.getPort());

			super.run(cmd);
		} catch (final Exception e) {
			getLogger().exception(e);
		}
	}
}
