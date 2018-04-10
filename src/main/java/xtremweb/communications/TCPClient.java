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

/**
 * This class implements the TCP client part to connect to the dispatcher<br>
 *
 * @see xtremweb.dispatcher.TCPHandler
 * Created: Jun 2nd, 2005
 * @author Oleg Lodygensky
 * @since RPCXW
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.xml.sax.SAXException;

import xtremweb.common.AppInterface;
import xtremweb.common.DataInterface;
import xtremweb.common.GroupInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.OSEnum;
import xtremweb.common.SessionInterface;
import xtremweb.common.StreamIO;
import xtremweb.common.Table;
import xtremweb.common.TaskInterface;
import xtremweb.common.TraceInterface;
import xtremweb.common.UserGroupInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.Version;
import xtremweb.common.WorkInterface;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLVector;
import xtremweb.common.XMLWriter;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;

public class TCPClient extends CommClient {

	/**
	 * This is the NIO socket channel
	 */
	private SocketChannel nioSocket = null;
	/**
	 * This is the socket
	 */
	private Socket socket = null;

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * This is implements I/O over the socket
	 */
	private StreamIO io;
	private XMLWriter writer;
	/**
	 * This is true if using Java NIO
	 */
	private boolean nio;

	/**
	 * This is the default constructor; this only calls super()
	 */
	public TCPClient() {
		super();
	}

	/**
	 * This retrieves this client port number
	 *
	 * @since 5.9.0
	 */
	@Override
	public int getPort() {
		if (getConfig() == null) {
			return -1;
		}
		return getConfig().getPort(Connection.TCPPORT);
	}

	/**
	 * This opens connection to server
	 *
	 * @param uri
	 *            is the URI to reach
	 */
	@Override
	protected void open(URI uri)
			throws IOException, UnknownHostException, NoRouteToHostException, SSLHandshakeException, ConnectException {

		if (isOpened()) {
			return;
		}
		mileStone("<open uri=\"" + uri + "\">");
		final XWConfigurator config = getConfig();
		try {
			nio = config.nio();

			String serverName = null;
			final String proxyName = config.getProperty(XWPropertyDefs.PROXYSERVER);
			if ((proxyName != null) && (proxyName.trim().length() > 0)) {
				serverName = proxyName;
			} else {
				serverName = XWTools.getHostName(uri.getHost());
			}

			int serverPort = uri.getPort();
			if (serverPort == -1) {
				serverPort = config.getPort(Connection.TCPPORT);
			}

			final String porttxt = config.getProperty(XWPropertyDefs.PROXYPORT);
			if ((porttxt != null) && (porttxt.trim().length() > 0)) {
				final int proxyPort = config.getPort(Connection.PROXYPORT);
				if (proxyPort > 0) {
					serverPort = proxyPort;
				}
			}

			URI uri2 = null;
			try {
				uri2 = new URI(serverName, serverPort, uri.getUID());
			} catch (final Exception e) {
				uri2 = uri;
			}
			uri = uri2;

			final File keyFile = config.getKeyStoreFile();

			int nbopens = 0;
			for (nbopens = 0; nbopens < getSocketRetries(); nbopens++) {
				try {
					if ((keyFile == null) || (!keyFile.exists())) {
						getLogger().warn("unsecured communications : not using SSL");

						if (nio) {
							nioSocket = SocketChannel.open();
							nioSocket.configureBlocking(true);

							nioSocket.connect(new InetSocketAddress(serverName, serverPort));

							while (!nioSocket.finishConnect()) {
								getLogger().info("still connecting");
							}
							socket = nioSocket.socket();
						} else {
							socket = new Socket(serverName, serverPort);
						}
						socket.setSoTimeout(config.getInt(XWPropertyDefs.SOTIMEOUT));
					} else {
						final SocketFactory socketFactory = SSLSocketFactory.getDefault();
						socket = socketFactory.createSocket(serverName, serverPort);
						socket.setSoTimeout(config.getInt(XWPropertyDefs.SOTIMEOUT));
						((SSLSocket) socket).startHandshake();
						nio = false;
					}
				} catch(SSLHandshakeException e) {
					throw e;
				} catch (final IOException e) {
					getLogger().exception("open", e);
					try {
						Thread.sleep(1000);
					} catch (final Exception es) {
					}
					continue;
				}
				getLogger().debug("*************************************** Socket opened");
				break;
			}

			if (nbopens == getSocketRetries()) {
				throw new ConnectException("unsuccessfully tried to open " + nbopens + " times");
			}

			// mac os x don't like that :(
			if (config.getBoolean(XWPropertyDefs.OPTIMIZENETWORK) && (!OSEnum.getOs().isMacosx())) {
				socket.setSoLinger(false, 0); // don't wait on close
				socket.setTcpNoDelay(true); // don't wait to send
				socket.setTrafficClass(0x08); // maximize throughput
				socket.setKeepAlive(false); // don't keep alive
			}

			io = new StreamIO(new DataOutputStream(socket.getOutputStream()),
					new DataInputStream(socket.getInputStream()), socket.getSendBufferSize(), nio);
			writer = new XMLWriter(io.output());

			setOpened(true);
		} catch (final IOException e) {
			getLogger().exception(e);
			mileStone("<error method='open' msg='" + e.getMessage() + "' />");
			throw e;
		} finally {
			mileStone("</open>");
		}
	}

	/**
	 * This closes communication channel is util.MAXMESSAGES is reached or if
	 * autoClose is true
	 *
	 * @see xtremweb.common.XWTools#MAXMESSAGES
	 * @see CommClient#autoClose
	 */
	@Override
	public void close() {

		boolean forceClose = false;
		if ((getNbMessages() % XWTools.MAXMESSAGES) == 0) {
			getLogger().debug("Enough messages : closing socket");
			forceClose = true;
			setNbMessages(0);
		}
		if ((!isAutoClose()) && (!forceClose)) {
			getLogger().debug("not closing");
			return;
		}

		try {
			mileStone("<close>");
			if (io != null) {
				io.close();
			}
			if (socket != null) {
				socket.close();
			}

		} catch (final IOException e) {
			getLogger().exception(e);
		} finally {
			setOpened(false);
			io = null;
			writer = null;
			socket = null;
			mileStone("</close>");
		}
	}

	/**
	 * this writes a command
	 *
	 * @param cmd
	 *            is the command to write
	 * @throws IOException
	 *             is thrown on communication error
	 */
	@Override
	protected void write(final XMLRPCCommand cmd) throws IOException {
		IOException ioe = null;
		try {
			mileStone("<write idrpc='" + cmd.getIdRpc() + "'>");

			if (getConfig().getPrivateKey() != null) {
				cmd.getUser().setChallenging(true);
			}

			try {
				cmd.setMandatingLogin(getConfig().getMandate());
				getLogger().finest("TCPClient#write writing " + cmd.toXml());
				writer.writeWithTags(cmd);
			} catch (final IOException brokenpipe) {
				// Oct 26th, 2011 : the socket may have been closed;
				// we try a 2nd time and then give up
				// For example : to send data , the client open the connection,
				// then may compress the data and finally send the data;
				// compression takes some time, and the server may have
				// closed the socket
				getLogger().exception(brokenpipe);
				final boolean ac = isAutoClose();
				setAutoClose(true);
				close();
				setAutoClose(ac);
				open(cmd.getURI());
				writer.writeWithTags(cmd);
			}

			if (getConfig().getPrivateKey() != null) {
				try {
					io.writeObject(getConfig().getPublicKey().getCertificate());
					getConfig().getPrivateKey().sendAuthentication(io.output());
				} catch (final Exception e) {
					throw new IOException(e.toString());
				}
			}
		} catch (final IOException e) {
			getLogger().exception(e);
			mileStone("<error method='write' msg='" + e.getMessage() + "' />");
			ioe = e;
		} finally {
			mileStone("</write>");
			if (ioe != null) {
				throw ioe;
			}
		}
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected Table newTableInterface() throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newTableInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected AppInterface newAppInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newAppInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected DataInterface newDataInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newDataInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected GroupInterface newGroupInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newGroupInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected HostInterface newHostInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newHostInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected SessionInterface newSessionInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newSessionInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected TaskInterface newTaskInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newTaskInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected TraceInterface newTraceInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newTraceInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected UserInterface newUserInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newUserInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected UserGroupInterface newUserGroupInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newUserGroupInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected WorkInterface newWorkInterface()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newWorkInterface(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected XMLVector newXMLVector() throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newXMLVector(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected Version newXMLVersion() throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newXMLVersion(io.input());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected XMLHashtable newXMLHashtable()
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newXMLHashtable(io.input());
	}

	/**
	 * This writes a file to socket
	 *
	 * @param f is the file to send
	 */
	@Override
	public void writeFile(final File f) throws IOException {
		mileStone("<writeFile file='" + f + "'>");
		io.writeFile(f, XWPostParams.MAXUPLOADSIZE);
		mileStone("</writeFile>");
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
		mileStone("<readFile file='" + f + "'>");
		io.readFile(f, XWPostParams.MAXUPLOADSIZE);
		mileStone("</readFile>");
	}

}
