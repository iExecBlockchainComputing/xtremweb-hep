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

package xtremweb.services.rpc;

/**
 * Date    : Mar 25th, 2005
 * Project : RPCXW / RPCXW-C
 * File    : CallbackPipe.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 * @since RPCXW
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.apache.log4j.Logger;

import xtremweb.archdep.ArchDepFactory;
import xtremweb.common.MileStone;
import xtremweb.common.XWReturnCode;

/**
 * This class forwards RCP on UDP.
 *
 * @see xtremweb.archdep.PortMapperItf
 */
public class rpc implements Interface {

	private static final Logger logger = Logger.getLogger(rpc.class);

	/**
	 * This aims to display some time printlns
	 */
	private final MileStone mileStone;
	/**
	 * This is the RPC port to forward client requests to This RPC server port
	 * must be determined at runtime
	 */
	private int rpcServerPort = 0;
	/**
	 * This is the RPC host name to forward client requests to The RPC server is
	 * supposed to run on local host
	 */
	private String hostName;
	/**
	 * This is the RPC host to forward client requests to The RPC server is
	 * supposed to run on local host
	 */
	private InetAddress serverHost = null;
	/**
	 * This is the transfered buffer size
	 */
	private static final int BUFSIZE = 8192;
	/**
	 * This is the results
	 */
	private byte[] datas;
	/**
	 * This is the user id
	 */
	private final int userID;
	/**
	 * This is the group id
	 */
	private final int groupID;

	/**
	 * This contructs a new object, retreiving this host name and this
	 * application user and group ID<br />
	 */
	public rpc() {

		datas = null;

		try {
			serverHost = InetAddress.getLocalHost();
			hostName = serverHost.getHostName();
		} catch (final Exception e) {
			logger.error("InetAddress.getLocalHost() : " + e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}
		groupID = ArchDepFactory.xwutil().getGid();
		logger.debug("GID = " + groupID);
		userID = ArchDepFactory.xwutil().getUid();
		logger.debug("UID = " + userID);

		mileStone = new MileStone(getClass());
	}

	/**
	 * This implements the Interface exec() method
	 *
	 * @see xtremweb.services.Interface
	 */
	@Override
	public int exec(final String cmdLine, final byte[] stdin, final byte[] dirin) {

		logger.debug("rpc.exec " + cmdLine + " " + stdin + " " + dirin);

		int ret = -1;
		if (cmdLine == null) {
			logger.error("no param found!!!");
			return ret;
		}
		if (dirin == null) {
			logger.error("no packet found!!!");
			return ret;
		}

		try {
			if (cmdLine.indexOf("--udp") != -1) {
				ret = udp(dirin);
			} else {
				logger.error("unknwon command line : " + cmdLine);
				System.exit(XWReturnCode.FATAL.ordinal());
			}
		} catch (final Exception e) {
			ret = -1;
			logger.error("Caught exception: ", e);
		}

		return ret;
	}

	/**
	 * This implements the Interface getResult() method by returning the
	 * <code>datas</code> member
	 *
	 * @see xtremweb.services.Interface
	 * @see #datas
	 */
	@Override
	public byte[] getResult() {
		return datas;
	}

	/**
	 * This connects RPC clients to RPC servers with a simple pipe. This is
	 * called on UDP connections.<br />
	 *
	 * It first determines RPC server to connect to, thanks to client RPC
	 * message, forwards packet to RPC and waits for an answer
	 *
	 * @param newDatas
	 *            is the packet to forward to RPC
	 * @return 0 on success, 1 on error (couldn't determine RPC service)
	 * @see xtremweb.archdep.PortMapperItf
	 */
	protected int udp(final byte[] newDatas) throws Exception {

		mileStone.println("XW RPC service started");

		logger.debug("Callback length 00 = " + newDatas.length);

		final Packet request = new Packet(newDatas, newDatas.length, hostName, userID,
				groupID);

		mileStone.println("XW RPC service packet ready");

		logger.debug("Callback length 01 = " + request.getBuffer().length);

		final int prog = request.getProg();
		final int version = request.getVersion();
		String proc = null;

		switch (prog) {
		case rpcdefs.RPC_NFS:
			proc = rpcdefs.NFSPROC_TEXT[request.getProc()];
			break;
		case rpcdefs.RPC_MOUNT:
			proc = rpcdefs.MOUNTPROC_TEXT[request.getProc()];
			break;
		}

		rpcServerPort = ArchDepFactory.portMap().getudpport(prog, version);
		if (rpcServerPort <= 0) {
			logger.error("can't retreive RPC port");
			return 1;
		}

		logger.debug("prog " + prog + " version " + version);

		try (final DatagramSocket serverSocket = new DatagramSocket()) {

			logger.debug("newDatas.length = " + newDatas.length);
			logger.debug("request.getLength() = " + request.getLength());
			final DatagramPacket serverPacket = new DatagramPacket(request.getBuffer(), request.getLength(), serverHost,
					rpcServerPort);

			logger.debug("writing to " + hostName + ":" + rpcServerPort);

			mileStone.println(proc + " sending");

			serverSocket.send(serverPacket);

			mileStone.println(proc + " waiting result");

			final byte[] buf = new byte[BUFSIZE];
			serverPacket.setData(buf);
			logger.debug("waiting answer from " + hostName + ":" + rpcServerPort);

			serverSocket.receive(serverPacket);

			mileStone.println(proc + " got result");

			final int nbBytes = serverPacket.getLength();
			logger.debug("nbBytes = " + nbBytes);

			datas = new byte[nbBytes];
			logger.debug("datas.length = " + datas.length);

			System.arraycopy(serverPacket.getData(), 0, datas, 0, nbBytes);

			mileStone.println(proc + " executed");
		}
		return 0;
	}

}
