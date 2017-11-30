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
 * Copyright 1999 Hannes Wallnoefer
 * Implements a XML-RPC client. See http://www.xmlrpc.com/
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;



/**
 * A multithreaded, reusable XML-RPC client object. This version uses a
 * homegrown HTTP client which can be quite a bit faster than
 * java.net.URLConnection, especially when used with XmlRpc.setKeepAlive(true).
 */

public final class XwXmlRpcClient extends XmlRpcClient {

	private static final Logger logger = Logger.getLogger(XwXmlRpcClient.class);

	/**
	 * Construct a XML-RPC client with this URL.
	 */
	public XwXmlRpcClient(final URL url) {
		super(url);
	}

	/**
	 * Construct a XML-RPC client for the URL represented by this String.
	 */
	public XwXmlRpcClient(final String url) throws MalformedURLException {
		super(url);
	}

	/**
	 * Construct a XML-RPC client for the URL represented by this String.
	 */
	public XwXmlRpcClient(final String socketType, final String url) throws MalformedURLException {
		super(socketType, url);
	}

	/**
	 * Construct a XML-RPC client for the specified hostname and port.
	 */
	public XwXmlRpcClient(final String hostname, final int port) throws MalformedURLException {
		super(hostname, port);
	}

	/**
	 * Construct a XML-RPC client for the specified hostname and port.
	 */
	public XwXmlRpcClient(final String socketType, final String hostname, final int port) throws MalformedURLException {
		super(socketType, hostname, port);
	}

	/**
	 * Generate an XML-RPC request and send it to the server. Parse the result
	 * and return the corresponding Java object.
	 *
	 * @exception XmlRpcException
	 *                : If the remote host returned a fault message.
	 * @exception IOException
	 *                : If the call could not be made because of lower level
	 *                problems.
	 */
	@Override
	public Object execute(final String method, final Vector params) throws XmlRpcException, IOException {
		final Worker worker = getWorker();
		try {
			final Object retval = worker.execute(method, params);
			return retval;
		} finally {
			if ((workers < 50) && !worker.fault) {
				pool.push(worker);
			} else {
				workers -= 1;
			}
		}
	}

	private final Stack pool = new Stack();
	private int workers = 0;

	private final Worker getWorker() throws IOException {
		try {
			return (Worker) pool.pop();
		} catch (final EmptyStackException x) {
			if (workers < 100) {
				workers += 1;
				return new Worker();
			}
			throw new IOException("XML-RPC System overload");
		}
	}

	class Worker extends XmlRpc {

		private boolean fault;
		private Object result = null;
		private HttpClient client = null;
		private StringBuffer strbuf;

		public Worker() {
			super();
		}

		public Object execute(final String method, final Vector params) throws XmlRpcException, IOException {
			final long now = System.currentTimeMillis();
			fault = false;

			logger.debug("XwXmlRpcClient::execute() params.len = " + params.size());

			try {
				if (strbuf == null) {
					strbuf = new StringBuffer();
				} else {
					strbuf.setLength(0);
				}
				final XmlWriter writer = new XmlWriter(strbuf);
				writeRequest(writer, method, params);
				final byte[] request = writer.getBytes();

				if (client == null) {
					client = new HttpClient(getSocketType(), getUrl());
				}
				logger.debug("XwXmlRpcClient::execute() request = " + request.length);

				client.write(request);

				final InputStream in = client.getInputStream();

				parse(in);

				if (!client.keepalive) {
					client.closeConnection();
				}
				logger.debug("result = " + result);

				if (getErrorLevel() == FATAL) {
					throw new Exception(getErrorMsg());
				}
			} catch (final IOException iox) {
				logger.debug("XwXmlRpcClient::execute() IOException");
				throw iox;

			} catch (final Exception x) {
				logger.error("Caught exception: ", x);
				String msg = x.getMessage();
				if ((msg == null) || (msg.length() == 0)) {
					msg = x.toString();
				}
				throw new IOException(msg);
			}

			if (fault) {
				logger.debug("XwXmlRpcClient::execute() fault");

				XmlRpcException exception = null;
				try {
					final Hashtable f = (Hashtable) result;
					final String faultString = (String) f.get("faultString");
					final int faultCode = Integer.parseInt(f.get("faultCode").toString());
					exception = new XmlRpcException(faultCode, faultString.trim());
				} catch (final Exception x) {
					logger.debug("XwXmlRpcClient::execute() fault exception");

					throw new XmlRpcException(0, "Server returned an invalid fault response.");
				}
				throw exception;
			}
			logger.debug("Spent " + (System.currentTimeMillis() - now) + " millis in request");

			return result;
		}

		/**
		 * Called when the return value has been parsed.
		 */
		@Override
		void objectParsed(final Object what) {
			result = what;
		}

		/**
		 * Generate an XML-RPC request from a method name and a parameter
		 * vector.
		 */
		void writeRequest(final XmlWriter writer, final String method, final Vector params) throws IOException {
			writer.startElement("methodCall");

			writer.startElement("methodName");
			writer.write(method);
			writer.endElement("methodName");

			writer.startElement("params");
			final int l = params.size();
			for (int i = 0; i < l; i++) {
				writer.startElement("param");
				writeObject(params.elementAt(i), writer);
				writer.endElement("param");
			}
			writer.endElement("params");
			writer.endElement("methodCall");
		}

		/**
		 * Overrides method in XmlRpc to handle fault repsonses.
		 */
		@Override
		public void startElement(final String name, final Attributes atts) throws SAXException {
			if ("fault".equals(name)) {
				fault = true;
			} else {
				super.startElement(name, atts);
			}
		}
	}

	/**
	 * A replacement for java.net.URLConnection, which seems very slow on MS
	 * Java
	 */
	class HttpClient {

		private final String hostname;
		private final String host;
		private int port;
		private String uri;
		private Socket socket = null;
		private SSLSocket sslSocket = null;
		private BufferedOutputStream output;
		private BufferedInputStream input;
		private boolean keepalive;
		private boolean fresh;
		private static final boolean debug = false;

		public HttpClient(final String socketType, final URL url) throws IOException {
			hostname = url.getHost();
			port = url.getPort();
			if (port < 1) {
				port = 80;
			}
			uri = url.getFile();
			if ((uri == null) || "".equals(uri)) {
				uri = "/";
			}
			host = port == 80 ? hostname : hostname + ":" + port;
			initConnection(socketType);
		}

		protected void initConnection(final String socketType) throws IOException {
			logger.debug("HttpClient::initConnection()");
			fresh = true;

			if (socketType.equals("SSL")) {
				SSLSocketFactory factory = null;
				try {

					SSLContext ctx;
					KeyManagerFactory kmf;
					KeyStore ks;
					TrustManagerFactory tmf;
					final char[] passphrase = XmlRpc.getPassPhrase().toCharArray();

					ctx = SSLContext.getInstance("TLS");
					kmf = KeyManagerFactory.getInstance("SunX509");
					tmf = TrustManagerFactory.getInstance("SunX509");
					ks = KeyStore.getInstance("JKS");

					logger.debug("HttpClient::initConnection() key file =  " + XmlRpc.getKeyStore());
					ks.load(new FileInputStream(XmlRpc.getKeyStore()), passphrase);

					kmf.init(ks, passphrase);
					tmf.init(ks);

					ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

					factory = ctx.getSocketFactory();

					sslSocket = (SSLSocket) factory.createSocket(hostname, port);

					sslSocket.startHandshake();
					output = new BufferedOutputStream(sslSocket.getOutputStream());
					input = new BufferedInputStream(sslSocket.getInputStream());
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
				}
			} else {
				socket = new Socket(hostname, port);
				output = new BufferedOutputStream(socket.getOutputStream());
				input = new BufferedInputStream(socket.getInputStream());
			}
		}

		protected void closeConnection() {
			try {
				if (getSocketType().equals("SSL")) {
					sslSocket.close();
				} else {
					socket.close();
				}
			} catch (final Exception ignore) {
			}
		}

		public void write(final byte[] request) throws IOException {
			logger.debug("XwXmlRpcClient::HttpClient::write()");
			try {
				output.write(("POST " + uri + " HTTP/1.0\r\n").getBytes());
				output.write(("User-Agent: " + XmlRpc.version + "\r\n").getBytes());
				output.write(("Host: " + host + "\r\n").getBytes());
				if (XmlRpc.getKeepAlive()) {
					output.write("Connection: Keep-Alive\r\n".getBytes());
				}
				output.write("Content-Type: text/xml\r\n".getBytes());
				if (getAuth() != null) {
					output.write(("Authorization: Basic " + getAuth() + "\r\n").getBytes());
				}
				output.write(("Content-Length: " + request.length).getBytes());
				output.write("\r\n\r\n".getBytes());
				output.write(request);
				output.flush();
				fresh = false;
			} catch (final IOException iox) {
				if (!fresh) {
					initConnection(getSocketType());
					write(request);
				} else {
					throw (iox);
				}
			}
		}

		public InputStream getInputStream() throws IOException {
			String line = readLine();
			logger.debug(line);

			int contentLength = -1;
			try {
				final StringTokenizer tokens = new StringTokenizer(line);
				final String httpversion = tokens.nextToken();
				final String statusCode = tokens.nextToken();
				final String statusMsg = tokens.nextToken("\n\r");
				keepalive = XmlRpc.getKeepAlive() && "HTTP/1.1".equals(httpversion);
				if (!"200".equals(statusCode)) {
					throw new IOException("Unexpected Response from Server: " + statusMsg);
				}
			} catch (final IOException iox) {
				throw iox;
			} catch (final Exception x) {
				logger.error("Caught exception: ", x);
				throw new IOException("Server returned invalid Response.");
			}
			do {
				line = readLine();
				if (line != null) {
					logger.debug(line);
					line = line.toLowerCase();
					if (line.startsWith("content-length:")) {
						contentLength = Integer.parseInt(line.substring(15).trim());
					}
					if (line.startsWith("connection:")) {
						keepalive = XmlRpc.getKeepAlive() && (line.indexOf("keep-alive") > -1);
					}
				}
			} while ((line != null) && !line.equals(""));
			return new ServerInputStream(input, contentLength);
		}

		private byte[] buffer;

		private String readLine() throws IOException {
			if (buffer == null) {
				buffer = new byte[512];
			}
			int next;
			int count = 0;
			while (true) {
				next = input.read();
				if ((next < 0) || (next == '\n')) {
					break;
				}
				if (next != '\r') {
					buffer[count++] = (byte) next;
				}
				if (count >= 512) {
					throw new IOException("HTTP Header too long");
				}
			}
			return new String(buffer, 0, count);
		}

		@Override
		protected void finalize() throws Throwable {
			closeConnection();
		}

	}

	/**
	 * Just for testing.
	 */
	public static void main(final String args[]) throws Exception {
		try {
			final String url = args[0];
			final String method = args[1];
			final Vector v = new Vector();
			for (int i = 2; i < args.length; i++) {
				try {
					v.addElement(new Integer(Integer.parseInt(args[i])));
				} catch (final NumberFormatException nfx) {
					v.addElement(args[i]);
				}
			}
			final XmlRpcClient client = new XwXmlRpcClient(url);
			try {
				client.execute(method, v);
			} catch (final Exception ex) {
				System.err.println("Error: " + ex.getMessage());
			}
		} catch (final Exception x) {
			System.err.println(x);
			System.err.println("Usage: java helma.xmlrpc.XmlRpcClient <url> <method> <arg> ....");
			System.err.println("Arguments are sent as integers or strings.");
		}
	}

}
