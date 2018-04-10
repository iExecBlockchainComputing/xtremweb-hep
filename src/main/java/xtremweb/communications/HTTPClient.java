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
 * This class implements HTTP client part to connect to the dispatcher<br>
 *
 * Created: Oct 5th, 2007
 * @author Oleg Lodygensky
 * @since XWHEP 1.0.0
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.security.KeyStore;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
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
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;

public class HTTPClient extends CommClient {

	public class AuthSSLProtocolSocketFactory implements SecureProtocolSocketFactory {

		private final TCPClient tcpClient;
		private final String uriScheme;

		public AuthSSLProtocolSocketFactory(final String scheme, final KeyStore ks, final String passwd) {
			super();
			uriScheme = scheme;
			tcpClient = new TCPClient();
		}

		private URI newURI(final String host, final int port) throws URISyntaxException {
			return new URI(uriScheme + "://" + host + ":" + port);
		}

		/**
		 * Attempts to get a new socket connection to the given host within the
		 * given time limit.
		 * <p>
		 * To circumvent the limitations of older JREs that do not support
		 * connect timeout a controller thread is executed. The controller
		 * thread attempts to create a new socket within the given limit of
		 * time. If socket constructor does not return until the timeout
		 * expires, the controller terminates and throws an
		 * {@link ConnectTimeoutException}
		 * </p>
		 *
		 * @param host
		 *            the host name/IP
		 * @param port
		 *            the port on the host
		 * @param localAddress
		 *            the local host name/IP to bind the socket to
		 * @param localPort
		 *            the port on the local machine
		 * @param params
		 *            {@link HttpConnectionParams Http connection parameters}
		 *
		 * @return Socket a new socket
		 *
		 * @throws IOException
		 *             if an I/O error occurs while creating the socket
		 * @throws UnknownHostException
		 *             if the IP address of the host cannot be determined
		 */
		@Override
		public Socket createSocket(final String host, final int port, final InetAddress localAddress,
				final int localPort, final HttpConnectionParams params)
				throws IOException, UnknownHostException, ConnectTimeoutException {
			try {
				tcpClient.open(newURI(host, port));
			} catch (final URISyntaxException e) {
				throw new IOException(e.getMessage());
			}
			return tcpClient.getSocket();
		}

		/**
		 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
		 */
		@Override
		public Socket createSocket(final String host, final int port, final InetAddress clientHost,
				final int clientPort) throws IOException, UnknownHostException {
			try {
				tcpClient.open(newURI(host, port));
			} catch (final URISyntaxException e) {
				throw new IOException(e.getMessage());
			}
			return tcpClient.getSocket();
		}

		/**
		 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
		 */
		@Override
		public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
			try {
				tcpClient.open(newURI(host, port));
			} catch (final URISyntaxException e) {
				throw new IOException(e.getMessage());
			}
			return tcpClient.getSocket();
		}

		/**
		 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
		 */
		@Override
		public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose)
				throws IOException, UnknownHostException {
			try {
				tcpClient.open(newURI(host, port));
			} catch (final URISyntaxException e) {
				throw new IOException(e.getMessage());
			}
			return tcpClient.getSocket();
		}
	}

	private boolean nio;

	private HttpClient httpClient;
	private PostMethod post;

	/**
	 * This is the default constructor; this only calls super()
	 */
	public HTTPClient() {
		super();
		httpClient = null;
	}

	/**
	 * This retreives this client port number
	 *
	 * @since 5.9.0
	 */
	@Override
	public int getPort() {
		if (getConfig() == null) {
			return -1;
		}
		return getConfig().getPort(Connection.HTTPPORT);
	}

	/**
	 * This does nothing; everything is done in write()
	 *
	 * @throws IOException
	 * @see #write(XMLRPCCommand)
	 */
	@Override
	protected void open(URI uri) throws UnknownHostException, NoRouteToHostException, SSLHandshakeException,
			SocketTimeoutException, IOException {

		try {
			String serverName = XWTools.getHostName(uri.getHost());
			int serverPort = uri.getPort();

			httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

			Protocol xwssl = null;
			final XWConfigurator config = getConfig();

			final KeyStore keyStore = config.getKeyStore();

			if (keyStore != null) {
				if (serverPort == -1) {
					serverPort = config.getPort(Connection.HTTPSPORT);
				}
				xwssl = new Protocol(uri.getScheme(), new AuthSSLProtocolSocketFactory(uri.getScheme(), keyStore,
						config.getProperty(XWPropertyDefs.SSLKEYPASSWORD)), serverPort);
				Protocol.registerProtocol(uri.getScheme(), xwssl);
			} else {
				getLogger().warn("unsecured communications : not using SSL");
				if (serverPort == -1) {
					serverPort = config.getPort(Connection.HTTPPORT);
				}
			}

			final String proxyName = config.getProperty(XWPropertyDefs.PROXYSERVER);
			if ((proxyName != null) && (proxyName.trim().length() > 0)) {
				serverName = XWTools.getHostName(proxyName);
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
				final StringBuilder struri2 = new StringBuilder(uri.getScheme() + Connection.getSchemeSeparator() + serverName);
				if (serverPort > 0) {
					struri2.append(":" + serverPort);
				}
				if (uri.getPath() != null) {
					struri2.append("/" + uri.getPath());
				}
				uri2 = new URI(struri2.toString());
			} catch (final Exception e) {
				uri2 = uri;
			}
			uri = uri2;
			uri2 = null;

			mileStone("<open uri=\"" + uri + "\">");
			httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			final HostConfiguration hConfig = new HostConfiguration();
			if (xwssl == null) {
				hConfig.setHost(serverName, serverPort);
			} else {
				hConfig.setHost(serverName, serverPort, xwssl);
			}
			httpClient.setHostConfiguration(hConfig);

			nio = false;

			if (config.getBoolean(XWPropertyDefs.OPTIMIZENETWORK) && (OSEnum.getOs().isMacosx() == false)) {
				final HttpConnectionManagerParams params = httpClient.getHttpConnectionManager().getParams();
				params.setLinger(0); // don't wait on close
				params.setTcpNoDelay(true); // don't wait to send
			}

			final HttpConnection connection = httpClient.getHttpConnectionManager()
					.getConnection(httpClient.getHostConfiguration());

			connection.open();
		} catch (final Exception e) {
			getLogger().exception(e);
			mileStone("<error method='open' msg='" + e.getMessage() + "' />");
			throw new IOException("HTTPClient : open failed " + e.toString());
		} finally {
			mileStone("</open>");
		}
	}

	/**
	 * This closes communication channel
	 */
	@Override
	public void close() {

		mileStone("<close>");
		post.releaseConnection();
		final HttpConnection connection = httpClient.getHttpConnectionManager()
				.getConnection(httpClient.getHostConfiguration());
		connection.close();

		mileStone("</close>");
	}

	/**
	 * This sends a command to server
	 *
	 * @param cmd
	 *            is the command to send
	 */
	@Override
	protected void write(final XMLRPCCommand cmd) throws IOException {

		mileStone("<write cmd='" + cmd.getIdRpc() + "'>");
		cmd.setMandatingLogin(getConfig().getMandate());
		post = new PostMethod(httpClient.getHostConfiguration().getHostURL());
		post.addParameter(XWPostParams.XMLDESC.toString(), cmd.toXml());
		httpClient.executeMethod(post);

		mileStone("</write>");
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected Table newTableInterface() throws InvalidKeyException, AccessControlException, IOException, SAXException {

		return super.newTableInterface(post.getResponseBodyAsStream());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected AppInterface newAppInterface()
			throws InvalidKeyException, AccessControlException, IOException, SAXException {
		return super.newAppInterface(post.getResponseBodyAsStream());
	}

	/**
	 * This creates an object from channel
	 */
	@Override
	protected DataInterface newDataInterface()
			throws InvalidKeyException, AccessControlException, IOException, SAXException {
		return super.newDataInterface(post.getResponseBodyAsStream());
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
		return super.newGroupInterface(post.getResponseBodyAsStream());
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
		return super.newHostInterface(post.getResponseBodyAsStream());
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
		return super.newSessionInterface(post.getResponseBodyAsStream());
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
		return super.newTaskInterface(post.getResponseBodyAsStream());
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
		return super.newTraceInterface(post.getResponseBodyAsStream());
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
		return super.newUserInterface(post.getResponseBodyAsStream());
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
		return super.newUserGroupInterface(post.getResponseBodyAsStream());
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
		return super.newWorkInterface(post.getResponseBodyAsStream());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected XMLVector newXMLVector() throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newXMLVector(post.getResponseBodyAsStream());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected Version newXMLVersion() throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return super.newXMLVersion(post.getResponseBodyAsStream());
	}

	/**
	 * This creates an object from channel
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	protected XMLHashtable newXMLHashtable()
			throws InvalidKeyException, AccessControlException, IOException, SAXException {
		return super.newXMLHashtable(post.getResponseBodyAsStream());
	}

	/**
	 * This uploads a data content to server
	 *
	 * @param command
	 *            is the command to send to server
	 * @param content
	 *            represents a File to get data to upload
	 * @since XWHEP 1.0.0
	 */
	@Override
	public void uploadData(final XMLRPCCommandUploadData command, final File content) throws IOException {

		try {
			mileStone("<uploadData>");

			open(command.getURI());

			command.setUser(getConfig().getUser());

			mileStone("Uploading " + command.toXml());

			post = new PostMethod(httpClient.getHostConfiguration().getHostURL());
			final Part[] parts = { new StringPart(XWPostParams.XMLDESC.toString(), command.toXml()),
					new FilePart(XWPostParams.XWUPLOAD.toString(), content) };

			post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

			final int status = httpClient.executeMethod(post);
			if (status != HttpStatus.SC_OK) {
				throw new IOException("can't upload data status = " + status);
			}
		} catch (final Exception e) {
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			getLogger().error("Upload error " + command.getURI() + " " + e);
		} finally {
			post.releaseConnection();
			close();
			mileStone("</uploadData>");
		}
	}

	/**
	 * This always throws an IOException. Please use uploadData instead
	 *
	 * @see #uploadData(XMLRPCCommandUploadData, File)
	 * @deprecated
	 */
	@Deprecated
	@Override
	public void writeFile(final File f) throws IOException {
		throw new IOException("HTTPClient#writeFile() is not implemented; please use HTTPClient#uploadData()");
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

		try (final DataInputStream inputStream = new DataInputStream(post.getResponseBodyAsStream());
				final StreamIO io = new StreamIO(null, inputStream, nio);) {
			mileStone("<readFile>");
			io.readFile(f, XWPostParams.MAXUPLOADSIZE);
		} catch (final Exception e) {
			getLogger().exception(e);
			mileStone("<error method='readFile' msg='" + e.getMessage() + "' />");
			throw new IOException(e.toString());
		} finally {
			mileStone("</readFile>");
		}
	}
}
