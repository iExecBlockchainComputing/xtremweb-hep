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
 * Date : Mar 25th, 2005 Project : RPCXW / RPCXW-C File : CallbackRpc.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky </a>
 * @version
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Vector;
import org.apache.log4j.Logger;

import xtremweb.common.*;
import xtremweb.communications.CommClient;
import xtremweb.communications.URI;
import xtremweb.services.rpc.Packet;
import xtremweb.services.rpc.rpcdefs;

/**
 * This class forwards RCP calls on UDP through XtremWeb.
 */
public class Callback {

	private static final Logger logger = Logger.getLogger(Callback.class);

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Communication channel
	 */
	private CommClient comm;

	/**
	 * This stores client configuration
	 */
	private final XWConfigurator config;

	/**
	 * This is the program name to launch
	 */
	private String appName;
	/**
	 * This is this client UID
	 */
	private UID clientUID;
	/**
	 * This is this client session UID
	 */
	private UID sessionUID;

	/**
	 * This aims to display some time stamps
	 */
	MileStone mileStone;

	/**
	 * This is the file name packet that RPCXW-S forwards to the SunRPC server
	 * We write content from SunRPC client to this file; RPCXW-S reads and
	 * forwards it to the SunRPC server
	 */
	public static final String FILEIN = "packet-in.bin";

	/**
	 * This is the file name packet that RPCXW-S received from the SunRPC server
	 * RPCXW-S writes this file and we must forward the content to the SunRPC
	 * client
	 */
	public static final String FILEOUT = "packet-out.bin";

	/**
	 * This is the server identifier<br />
	 * If forwarding to XtremWeb this is the worker UID String representation
	 * <br />
	 * If piping (i.e. testing) this is the server host name
	 */
	private String serverId;
	/**
	 * If forwarding to XtremWeb this is the worker UID
	 */
	private UID serverUID;

	/**
	 * This is the RPC host to forward client requests to The RPC server is
	 * supposed to run on local host
	 */
	private InetAddress serverHost = null;

	/**
	 * @return the serverHost
	 */
	public InetAddress getServerHost() {
		return serverHost;
	}

	/**
	 * @param serverHost
	 *            the serverHost to set
	 */
	public void setServerHost(final InetAddress serverHost) {
		this.serverHost = serverHost;
	}

	/**
	 * This is the server name This is used to change packet credentials If
	 * forwarding to XtremWeb this is the worker name If piping (i.e. testing)
	 * this is the server host name
	 */
	private String serverName = null;

	/**
	 * @return the serverName
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * @param serverName
	 *            the serverName to set
	 */
	public void setServerName(final String serverName) {
		this.serverName = serverName;
	}

	/**
	 * This stores XtremWeb job uid
	 */
	private final Vector uids;

	/**
	 * This is the transfert buffer size
	 */
	public static final int BUFSIZE = 8192;

	/**
	 * This aims to simulate a response time (default is 0) This is supposed to
	 * contain seconds
	 */
	private int sleep_delay = 0;

	/**
	 * This is only needed if piping to another host If piping to local host,
	 * the port is automatically extracted from portmapper
	 */
	private int nfsPort;

	/**
	 * @return the nfsPort
	 */
	public int getNfsPort() {
		return nfsPort;
	}

	/**
	 * This is only needed if piping to another host If piping to local host,
	 * the port is automatically extracted from portmapper
	 */
	private int mountPort;

	/**
	 * @return the mountPort
	 */
	public int getMountPort() {
		return mountPort;
	}

	/**
	 * This is the default constructor.
	 *
	 * @param c
	 *            is the configuration
	 */
	protected Callback(final String argv[], final XWConfigurator c) {

		config = c;
		serverId = null;
		serverUID = null;
		comm = null;
		uids = new Vector();
		nfsPort = -1;
		mountPort = -1;
		serverName = null;
		appName = "xtremweb.services.rpc.rpc";
		mileStone = new MileStone(getClass());

		parse(argv);

		final boolean test = (this.getClass().getName().compareTo("xtremweb.rpcd.client.CallbackPipe") != 0);

		if (test == true) {

			// serverUID is already set in case of
			// instanciation from xtremweb.worker.Worker
			try {
				comm = (CommClient) Class.forName(config.getProperty(XWPropertyDefs.COMMLAYER)).newInstance();
				CommClient.setConfig(config);

				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						cleanup();
					}
				});
			} catch (final Exception e) {
				logger.error("Can't init comm :  " + e);
				System.exit(XWReturnCode.FATAL.ordinal());
			}

			try {
				if (serverId != null) {
					serverUID = new UID(serverId);
				} else {
					// 14 nov 2005 : to ease deployment, we now take
					// the first worker found as NFS server
					logger.warn("Looking for any server");
					final Vector<XMLValue> workers = (Vector<XMLValue>) comm.getHosts().getXmlValues();
					if ((workers == null) || (workers.size() < 1)) {
						logger.error("can't find any server");
						System.exit(XWReturnCode.FATAL.ordinal());
					}
					serverUID = (UID) ((XMLValue) workers.get(0)).getValue();
				}
				final HostInterface host = (HostInterface) comm.get(serverUID);
				if (host == null) {
					logger.error("can't find server " + serverId);
					System.exit(XWReturnCode.FATAL.ordinal());
				}
				serverName = host.getName();
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
				System.exit(XWReturnCode.FATAL.ordinal());
			}

			if ((comm == null) || (serverUID == null)) {
				logger.error("Can't init comm");
				System.exit(XWReturnCode.FATAL.ordinal());
			}

			new Cleaner(config, this).start();

			//
			// we must now determine app UID
			// since Sept 19th, 2005 :
			// submitting to XW needs app UID and not app name
			//
			try {
				final AppInterface app = comm.getApp(appName);
				app.getUID();

				final SessionInterface session = new SessionInterface();
				clientUID = new UID();
				sessionUID = new UID();
				session.setUID(sessionUID);
				session.setOwner(clientUID);
				session.setName("RPCXWC");
				comm.send(session);
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
				logger.error("can't retreive app " + appName);
				System.exit(XWReturnCode.FATAL.ordinal());
			}
		}

		logger.info("Server UID       : " + serverUID);
		logger.info("Server name      : " + serverName);
		logger.info("Application      : " + appName);
		logger.debug("mount port = " + mountPort);
		logger.debug("nfs   port = " + nfsPort);
	}

	/**
	 * This parses command line arguments
	 */
	public final void parse(final String argv[]) {

		int i = 0;

		while (i < argv.length) {

			if (argv[i].toLowerCase().compareTo("-s") == 0) {
				sleep_delay = new Integer(argv[++i]).intValue();
			} else if (argv[i].toLowerCase().compareTo("--server") == 0) {
				serverId = argv[++i];
			} else if (argv[i].toLowerCase().compareTo("--name") == 0) {
				serverName = argv[++i];
			} else if (argv[i].toLowerCase().compareTo("--app") == 0) {
				appName = argv[++i];
			} else if (argv[i].toLowerCase().compareTo("--nfsport") == 0) {
				nfsPort = new Integer(argv[++i]).intValue();
			} else if (argv[i].toLowerCase().compareTo("--mountport") == 0) {
				mountPort = new Integer(argv[++i]).intValue();
			}

			i++;
		}
	}

	/**
	 * This connects RPC client to RPC server This transmits RPC client requests
	 * to RPC server and forwards RPC server answers back to RPC client
	 *
	 * It first determines RPC server to connect to, thanks to client RPC
	 * message.
	 *
	 * @param clientPacket
	 *            is the client datagram packet
	 */
	protected void udp(final DatagramSocket clientSocket, final DatagramPacket clientPacket) throws Exception {

		mileStone.println("new packet received");

		logger.debug("Callback received.length = " + clientPacket.getLength());

		Packet request = null;
		try {
			request = new Packet(clientPacket.getData(), clientPacket.getLength(), serverName);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			throw e;
		}
		final byte[] newDatas = request.getBuffer();

		logger.debug("Callback length 01 = " + newDatas.length);

		byte[] datas = null;
		final int prog = request.getProg();
		final int version = request.getVersion();
		final int method = request.getProc();
		String proc = null;

		switch (prog) {
		case rpcdefs.RPC_NFS:
			proc = rpcdefs.NFSPROC_TEXT[method];
			break;
		case rpcdefs.RPC_MOUNT:
			proc = rpcdefs.MOUNTPROC_TEXT[method];
			break;
		}

		datas = xwForward(proc, newDatas, "" + prog + " " + version + " --udp", request.getLength());

		if (datas == null) {
			logger.error("!?!?!?!?!?!?!?!?! datas is null !?!?!?!?!?!?!?!?!");
			return;
		}

		clientPacket.setData(datas);

		logger.debug("forwarding answer " + clientPacket.getLength());

		clientSocket.send(clientPacket);

		mileStone.println("packet answered");
	}

	/**
	 * This is for debug purposes only<br />
	 * This sleeps sleep_delay seconds
	 *
	 * @see #sleep_delay
	 */
	protected void sleep() {

		try {
			logger.info("sleeping " + sleep_delay + " s");
			Thread.sleep(sleep_delay * 1000);
		} catch (final Exception e) {
		}
	}

	/**
	 * This retreives the list of computed jobs This is typically needed by the
	 * Cleaner
	 *
	 * @see xtremweb.rpcd.client.Cleaner
	 */
	public synchronized Vector getJobs() {
		Vector ret = null;
		synchronized (uids) {
			ret = (Vector) uids.clone();
			uids.clear();
		}
		return ret;
	}

	/**
	 * This forwards the packet to XtremWeb.<br />
	 * This is typically called if serverId is set
	 *
	 * @param proc
	 *            is the RPC proc name
	 * @param newDatas
	 *            is the packet to send
	 * @param params
	 *            is the prog parameters
	 * @param len
	 *            is the packet length (i.e. <SunRPC Prog Num> <SunRPC Version
	 *            Num> [--udp])
	 */
	protected byte[] xwForward(final String proc, final byte[] newDatas, final String params, final int len)
			throws Exception {

		throw new Exception("Callback::xwForward not implemented");
	}

	/**
	 * This is called on program termination (CTRL+C) This deletes session from
	 * server
	 */
	private void cleanup() {
		try {
			comm.remove(new URI(config.getCurrentDispatcher(), sessionUID));
		} catch (final Exception e) {
			logger.error("can't clean up");
		}
	}
}
