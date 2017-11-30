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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocket;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;


import xtremweb.common.BytePacket;
import xtremweb.common.MD5;
import xtremweb.common.OSEnum;
import xtremweb.common.StreamIO;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;
import xtremweb.common.XMLWriter;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;
import xtremweb.communications.XMLRPCCommand;
import xtremweb.security.PEMPublicKeyValidator;

/**
 * This handles incoming communications through TCP<br>
 * This answers request from TCPClient
 *
 * Created: August 2005
 *
 * @see xtremweb.communications.TCPClient
 * @author Oleg Lodygensky
 * @version RPCXW
 */

public class TCPHandler extends xtremweb.dispatcher.CommHandler {

	private static final Logger logger = Logger.getLogger(TCPHandler.class);
	private SSLSocket sslSocket = null;
	private Socket socket = null;
	private StreamIO io;
	private XMLWriter writer;

	public static final String NAME = "TCPHandler";

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public TCPHandler() {
		super(NAME);
		socket = null;
		sslSocket = null;
	}

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public TCPHandler(final String n, final XWConfigurator context) {
		super(n, context);
		socket = null;
		sslSocket = null;
	}

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public TCPHandler(final XWConfigurator context) {
		this(NAME + Math.random(), context);
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 */
	@Override
	public synchronized void resetSockets() throws RemoteException {
		this.socket = null;
		this.sslSocket = null;
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 */
	@Override
	public synchronized void setSocket(final SSLSocket s) throws RemoteException {
		if (s == null) {
			throw new RemoteException("setSocket_ssl() s is null???");
		}
		if (sslSocket != null) {
			throw new RemoteException("setSocket_ssl() sslSocket is not null???");
		}
		sslSocket = s;
		socket = null;
		paramSocket(sslSocket);
		notifyAll();
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 */
	@Override
	public synchronized void setSocket(final Socket s) throws RemoteException {
		if (s == null) {
			notifyAll();
			throw new RemoteException("setSocket() s is null???");
		}
		if (socket != null) {
			notifyAll();
			throw new RemoteException("setSocket() socket is not null???");
		}
		socket = s;
		sslSocket = null;
		paramSocket(socket);
		notifyAll();
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 */
	private void paramSocket(final Socket s) throws RemoteException {

		setRemoteName(s.getInetAddress().getHostName());
		setRemoteIP(s.getInetAddress().getHostAddress());
		setRemotePort(s.getPort());

		final XWConfigurator config = getConfig();

		try {
			s.setSoTimeout(config.getInt(XWPropertyDefs.SOTIMEOUT));
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}

		final boolean net = Boolean.parseBoolean(config.getProperty(XWPropertyDefs.OPTIMIZENETWORK));
		if ((net) && !OSEnum.getOs().isMacosx()) {
			try {
				s.setSoLinger(false, 0); // don't wait on close
				s.setTcpNoDelay(true); // don't wait to send
				s.setTrafficClass(0x08); // maximize throughput
				s.setKeepAlive(false); // don't keep alive
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}
	}

	/**
	 * This throws an exception since setPacket() is dedicated to UDP comms
	 *
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramSocket s, final DatagramPacket p) throws RemoteException {
		throw new RemoteException("TCPHandler#setPacket() TCP can't set packet");
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramChannel c, final SocketAddress r, final BytePacket p) throws RemoteException {
		throw new RemoteException("TCPHandler#setPacket() TCP can't set packet");
	}

	/**
	 * This writes an object to output channel
	 */
	@Override
	protected void write(final XMLable cmd) throws IOException {

		try {
			mileStone("<write>");
			writer.writeWithTags(cmd);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			final String str = e.toString();
			mileStone("<error method='write' msg='" + e.getMessage() + "' />");
			throw new IOException(str);
		} finally {
			mileStone("</write>");
		}
	}

	/**
	 * This writes a file to output channel
	 *
	 * @see xtremweb.common.StreamIO#writeFile(File)
	 * @param f
	 *            is the file to send
	 */
	@Override
	public synchronized void writeFile(final File f) throws IOException {
		try {
			mileStone("<writeFile file='" + f + "'>");
			io.writeFile(f);
		} finally {
			mileStone("</writeFile>");
		}
	}

	/**
	 * This reads a file from socket This is typically needed after a
	 * workRequest to get stdin and/or dirin files
	 *
	 * @param f
	 *            is the file to store received bytes
	 */
	@Override
	public synchronized void readFile(final File f) throws IOException {
		try {
			mileStone("<readFile file='" + f + "'>");
			io.readFile(f);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			final String str = e.getMessage();
			mileStone("<error method='readFile' msg='" + e.getMessage() + "' />");
			throw new IOException(str);
		} finally {
			mileStone("</readFile>");
			notifyAll();
		}
	}

	/**
	 * This waits for socket
	 */
	private synchronized void waitSocket() {

		while ((socket == null) && (sslSocket == null)) {
			try {
				logger.debug("TCPHandler is waiting");
				this.wait();
				logger.debug("TCPHandler woken up");
			} catch (final InterruptedException e) {
			}
		}
		notifyAll();
	}

	/**
	 * This is the main loop; this is called by java.lang.Thread#start() This
	 * executes one command form communication channel and exists
	 *
	 * @see CommHandler#run(XMLRPCCommand)
	 */
	@Override
	public void run() {

		XMLRPCCommand cmd;
		final boolean nio = Boolean.parseBoolean(getConfig().getProperty(XWPropertyDefs.JAVANIO));

		while (true) {

			waitSocket();

			Socket theSocket = null;
			try {
				theSocket = sslSocket;
				if (theSocket == null) {
					theSocket = socket;
				}
				final DataOutputStream os = new DataOutputStream(theSocket.getOutputStream());
				final DataInputStream is = new DataInputStream(theSocket.getInputStream());
				io = new StreamIO(os, is, theSocket.getSendBufferSize(), nio);
				writer = new XMLWriter(os);

				int nbmessages = 0;

				do {
					cmd = XMLRPCCommand.newCommand(io);
					final UserInterface client = cmd.getUser();
					if (client == null) {
						throw new IOException("cmd.client is null");
					}
					final boolean challenging = client.isChallenging();

					if (challenging) {
						X500Principal principal = null;
						final UID newuid = new UID();
						UserInterface user = null;
						X509Certificate cert = null;

						try {
							try {
								final BufferedInputStream input = new BufferedInputStream(is);
								input.mark(255);
								final ObjectInputStream ois = new ObjectInputStream(input);
								cert = (X509Certificate) ois.readObject();
								//
								// first authenticate, to read all from client
								// and let him a chance to get an answer
								//
								PEMPublicKeyValidator.authenticate(cert, is);
								if (Dispatcher.getConfig().getAdminUid() == null) {
									throw new AccessControlException("Server config error : admin.uid is not set");
								}
								if (Dispatcher.getProxyValidator() == null) {
									throw new AccessControlException(
											"Server config error : server can't validate certificate");
								}
								Dispatcher.getProxyValidator().validate(cert);
							} catch (final Exception e) {
								throw new AccessControlException("Certificate challenge error : " + e.getMessage());
							}
							logger.debug("cert.getIssuerX500Principal().getName() "
									+ cert.getIssuerX500Principal().getName());

							principal = cert.getSubjectX500Principal();
							final String subjectName = principal.getName();
							principal = cert.getIssuerX500Principal();
							final String issuerName = principal.getName();
							final String loginName = subjectName + "_" + issuerName;
							principal = null;
							final String random = loginName + Math.random();
							final byte[] strb = random.getBytes();
							final MD5 md5 = new MD5(strb);
							final String md5hex = md5.asHex();

							// login may be
							// truncated; see UserIntergace.USERLOGINLENGTH
							client.setLogin(loginName);

							final DBInterface db = DBInterface.getInstance();
							if (client.getUID() != null) {
								user = db.user(client.getUID());
							}
							if (user == null) {
								user = db
										.user(UserInterface.Columns.LOGIN.toString() + "= '" + client.getLogin() + "'");
							}
							if (user == null) {
								client.setUID(newuid);
								client.setLogin(loginName);
								client.setPassword(md5hex);
								client.setOwner(Dispatcher.getConfig().getAdminUid());
								client.setRights(UserRightEnum.STANDARD_USER);
								//
								// this inserts user in DB
								//
								user = new UserInterface(client);
							}
							cmd.getUser().setLogin(user.getLogin());
							cmd.getUser().setPassword(user.getPassword());
							cmd.getUser().setUID(user.getUID());
						} finally {
							user = null;
							cert = null;
							principal = null;
						}
					}
					// next will close the communication channel
					super.run(cmd);
					if (nbmessages++ > XWTools.MAXMESSAGES) {
						warn("Enough messages (" + getId() + ")");
						cmd = null;
					}

				} while (cmd != null);

			} catch (final Exception e) {
				logger.debug(remoteAddresse() + " end of communication : " + e.getMessage());
			} finally {
				close();
				this.socket = null;
				this.sslSocket = null;
				theSocket = null;
				getCommServer().pushConnection(this);
			}
		}
	}

	/**
	 * This cleans and closes communications
	 */
	@Override
	public void close() {
		try {
			mileStone("<close>");
			if (io != null) {
				io.close();
			}
			if ((socket != null) && !socket.isClosed()) {
				socket.close();
			}
			if ((sslSocket != null) && !sslSocket.isClosed()) {
				sslSocket.close();
			}
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			mileStone("<error method='close' msg='" + e.getMessage() + "' />");
			logger.error("Caught exception, close error", e);
		} finally {
			io = null;
			writer = null;
			socket = null;
			sslSocket = null;
			resetRemoteName();
			resetRemoteIP();
			resetRemotePort();
			mileStone("</close>");
		}
	}
}
