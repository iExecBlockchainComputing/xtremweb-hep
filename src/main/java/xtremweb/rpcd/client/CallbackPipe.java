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

package xtremweb.rpcd.client;

/**
 * Date    : Mar 25th, 2005
 * Project : RPCXW / RPCXW-C
 * File    : CallbackPipe.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import org.apache.log4j.Logger;

import xtremweb.archdep.ArchDepFactory;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWReturnCode;
import xtremweb.services.rpc.Packet;
import xtremweb.services.rpc.rpcdefs;

/**
 * This class pipes RCP callback on UDP
 */
public class CallbackPipe extends Callback {

	private static final Logger logger = Logger.getLogger(CallbackPipe.class);

	/**
	 * This is the RPC port to forward client requests to This RPC server port
	 * must be determined at runtime
	 */
	protected int rpcServerPort = 0;

	/**
	 * This is the default consructor. This does nothing
	 *
	 * @param c
	 *            is the XtremWeb config
	 */
	public CallbackPipe(final String argv[], final XWConfigurator c) {
		super(argv, c);

		// let preload library now
		@SuppressWarnings("unused")
		final int rpcServerPort = ArchDepFactory.portMap().getudpport(0, 0);
		final String sn = getServerName();
		try {
			setServerHost(InetAddress.getByName(sn));
		} catch (final Exception e) {
			final Logger logger = getLogger();
			logger.error("Caught exception, InetAddress.getByName (" + sn + ")", e);
			logger.error("Please use '--name' option");
			System.exit(XWReturnCode.FATAL.ordinal());
		}
	}

	/**
	 * This connects RPC clients to RPC servers with a simple pipe. This is
	 * called on UDP connections.
	 *
	 * It first determines RPC server to connect to, thanks to client RPC
	 * message.
	 *
	 * @param clientPacket
	 *            is the client datagram packet
	 */
	@Override
	protected void udp(final DatagramSocket clientSocket, final DatagramPacket clientPacket) throws Exception {

		final Logger logger = getLogger();
		final String serverName = getServerName();
		final Packet request = new Packet(clientPacket.getData(), clientPacket.getLength(), serverName);

		String proc = null;
		final int prog = request.getProg();
		final int version = request.getVersion();

		switch (prog) {
		case rpcdefs.RPC_NFS:
			proc = rpcdefs.NFSPROC_TEXT[request.getProc()];
			break;
		case rpcdefs.RPC_MOUNT:
			proc = rpcdefs.MOUNTPROC_TEXT[request.getProc()];
			break;
		}

		logger.debug("CallbackMountd;" + proc + " received;" + new Date().getTime());

		final byte[] newDatas = request.getBuffer();
		byte[] datas = null;

		rpcServerPort = ArchDepFactory.portMap().getudpport(prog, version);

		switch (prog) {
		case rpcdefs.RPC_NFS:
			if (getNfsPort() != -1) {
				rpcServerPort = getNfsPort();
			}
			break;
		case rpcdefs.RPC_MOUNT:
			if (getMountPort() != -1) {
				rpcServerPort = getMountPort();
			}
			break;
		}

		logger.debug("CallbackPipe;" + proc + " forwarding;" + new Date().getTime());

		logger.info("prog " + prog + " version " + version);

		try (final DatagramSocket serverSocket = new DatagramSocket()) {

			logger.info("newDatas.length = " + newDatas.length);
			logger.info("request.getLength() = " + request.getLength());
			final InetAddress serverHost = getServerHost();
			final DatagramPacket serverPacket = new DatagramPacket(newDatas, request.getLength(), serverHost,
					rpcServerPort);

			logger.info("writing to " + serverName + ":" + rpcServerPort);

			//
			// Sending the job
			//

			logger.debug("RPCXW;" + proc + " sending;" + new Date().getTime());

			serverSocket.send(serverPacket);

			logger.debug("RPCXW;" + proc + " sent;" + new Date().getTime());

			final byte[] buf = new byte[BUFSIZE];
			serverPacket.setData(buf);
			logger.info("waiting answer from " + serverName + ":" + rpcServerPort);

			serverSocket.receive(serverPacket);

			logger.debug("RPCXW;" + proc + " got result;" + new Date().getTime());

			final int nbBytes = serverPacket.getLength();

			logger.debug("CallbackPipe;" + proc + " forwarded;" + new Date().getTime());

			datas = new byte[nbBytes];
			System.arraycopy(serverPacket.getData(), 0, datas, 0, nbBytes);

			clientPacket.setData(datas);
			clientSocket.send(clientPacket);

			logger.debug("CallbackPipe;" + proc + " answered;" + new Date().getTime());
		}
	}
}
