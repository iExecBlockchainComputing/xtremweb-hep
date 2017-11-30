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
 * File    : rpcudp.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.apache.log4j.Logger;

import xtremweb.common.XWConfigurator;
import xtremweb.communications.Connection;

/**
 * This implements the RPC Proxy for the UDP protocol<br />
 *
 * RPC message structure on UDP :
 * <ul>
 * <li>[0] 4 bytes : one 32 bits integer : XID
 * <li>[1] 4 bytes : one 32 bits integer : MSG type (call, answer etc.)
 * <li>[2] 4 bytes : one 32 bits integer : RPC version
 * <li>[3] 4 bytes : one 32 bits integer : the prog number (expected to auto
 * detect port)
 * <li>[4] 4 bytes : one 32 bits integer : the prog version (expected to auto
 * detect port)
 * <li>[5] 4 bytes : one 32 bits integer : the method number
 * <li>[6] 4 bytes : one 32 bits integer : Credential flavour (1 = AUTH_UNIX)
 * <li>[7] 4 bytes : one 32 bits integer : Credential length
 * <li>[8] 4 bytes : one 32 bits integer : Credential stamp
 * <li>[9] 4 bytes : one 32 bits integer : Credential machine name length
 * <li>some bytes : Credential machine name
 * <li>some bytes : name padding byte to (name = X * 4 bytes)
 * <li>4 bytes : one 32 bits integer : UID user id
 * <li>4 bytes : one 32 bits integer : GID group id
 * <li>some more bytes follow
 * </ul>
 * Under UDP : [3] and [4] to determine RPC port
 *
 */
public class rpcudp extends rpc {

	private static final Logger logger = Logger.getLogger(rpcudp.class);

	/**
	 * This is the Socket to accept client requests
	 */
	private DatagramSocket clientSocket = null;

	private final byte[] buf;

	/**
	 * This is the only constructor
	 *
	 * @param argv
	 *            is the command line
	 * @param c
	 *            is the XtremWeb config
	 */
	public rpcudp(final String[] argv, final XWConfigurator c) {

		super("UDP", argv, c);
		buf = new byte[getBUFSIZE()];
	}

	/**
	 * This creates a new ServerSocket and listen to
	 */
	@Override
	public void run() {

		final int port = config.getPort(Connection.SUNRPCPORT);
		try {
			clientSocket = new DatagramSocket(port);
			System.out.println("XtremWeb for SunRPC listening on UDP port: " + clientSocket.getLocalPort());
		} catch (final IOException e) {
			logger.warn("Could not listen on UDP port " + port + ". xtremweb.rpcd.client.rpcudp stops now.");
			throw new IllegalThreadStateException("Could not listen on UDP port " + port);
		}

		while (true) {

			try {
				logger.debug("accepting...");

				// byte[] buf = new byte [BUFSIZE];
				final DatagramPacket packet = new DatagramPacket(buf, buf.length);
				clientSocket.receive(packet);

				logger.debug("accepted...");

				/*
				 * byte[] datas = packet.getData (); int [] integers =
				 * io.bytes2integers (datas, datas.length); int prog = integers
				 * [3];
				 *
				 * logger.logger.debug ("prog " + prog);
				 *
				 * if (piped != null) piped.udp (clientSocket, packet,
				 * integers); else { callback.udp(clientSocket, packet,
				 * integers);
				 */
				if (getPiped() != null) {
					getPiped().udp(clientSocket, packet);
				} else {
					getCallback().udp(clientSocket, packet);
					/*
					 * switch (prog) { case rpcdefs.RPC_NFS: // nfsd = new
					 * CallbackNfsd (argv, config); nfsd.udp (clientSocket,
					 * packet, integers); break; case rpcdefs.RPC_MOUNT: //
					 * mountd = new CallbackMountd (argv, config); mountd.udp
					 * (clientSocket, packet, integers); break; }
					 */
				}
			} catch (final Exception e) {
				logger.error("Caught exception, connect() exception " + e);
			}
		}
	}

	/**
	 * This is the standard main function for debug purposes
	 */
	public static void main(final String[] argv) {

		rpcudp client;

		client = new rpcudp(argv, null);
		client.run();
	}

}// rpcudp
