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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;


import xtremweb.common.Base64;

/**
 * A multithreaded, reusable XML-RPC client object. Use this if you need a
 * full-grown HTTP client (e.g. for Proxy and Cookies support). If you don't
 * need that, <code>XmlRpcClientLite</code> may work better for you.
 */
public class XmlRpcClient implements XmlRpcHandler {

	private URL url;
	private String auth;
	private String socketType;
	private static final Logger logger = Logger.getLogger(XmlRpcClient.class);

	/**
	 * Construct a XML-RPC client with this URL.
	 */
	public XmlRpcClient(final URL url) {
		this.setUrl(url);
	}

	/**
	 * Construct a XML-RPC client for the URL represented by this String.
	 */
	public XmlRpcClient(final String url) throws MalformedURLException {
		this(new URL(url));
	}

	/**
	 * Construct a XML-RPC client for the URL represented by this String.
	 */
	public XmlRpcClient(final String socketType, final String url) throws MalformedURLException {
		this(new URL(url));
		this.setSocketType(socketType);
	}

	/**
	 * Construct a XML-RPC client for the specified hostname and port.
	 */
	public XmlRpcClient(final String hostname, final int port) throws MalformedURLException {
		this("http://" + hostname + ":" + port + "/RPC2");
	}

	/**
	 * Construct a XML-RPC client for the specified hostname and port.
	 */
	public XmlRpcClient(final String socketType, final String hostname, final int port) throws MalformedURLException {
		this("http://" + hostname + ":" + port + "/RPC2");
		this.setSocketType(socketType);
	}

	/**
	 * Sets Authentication for this client. This will be sent as Basic
	 * Authentication header to the server as described in
	 * <a href="http://www.ietf.org/rfc/rfc2617.txt" >http://www.ietf.org/rfc/
	 * rfc2617.txt</a>.
	 */
	public void setBasicAuthentication(final String user, final String password) {
		if ((user == null) || (password == null)) {
			setAuth(null);
		} else {
			final char[] basicAuth = Base64.encode((user + ":" + password).getBytes());
			setAuth(new String(basicAuth).trim());
		}
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
		private StringBuffer strbuf;

		public Worker() throws IOException {
			super();
		}

		public Object execute(final String method, final Vector params) throws XmlRpcException, IOException {
			fault = false;
			final long now = System.currentTimeMillis();

			final byte[] bInput = new byte[300];

			try {
				final ByteArrayOutputStream bout = new ByteArrayOutputStream();

				if (strbuf == null) {
					strbuf = new StringBuffer();
				} else {
					strbuf.setLength(0);
				}
				final XmlWriter writer = new XmlWriter(strbuf);
				writeRequest(writer, method, params);
				final byte[] request = writer.getBytes();

				final URLConnection con = getUrl().openConnection();
				con.setDoInput(true);
				con.setDoOutput(true);
				con.setUseCaches(false);
				con.setAllowUserInteraction(false);
				con.setRequestProperty("Content-Length", Integer.toString(request.length));
				con.setRequestProperty("Content-Type", "text/xml");
				if (getAuth() != null) {
					con.setRequestProperty("Authorization", "Basic " + getAuth());
				}
				final OutputStream out = con.getOutputStream();
				out.write(request);
				out.flush();

				final String sRequest = new String(request);
				logger.debug("\nXML method call\n" + sRequest + "\n");

				final InputStream in = con.getInputStream();

				logger.debug("\nXML method response");
				final BufferedInputStream bIn = new BufferedInputStream(in);

				bIn.mark(300);
				final int num = bIn.read(bInput);
				final String sInput = new String(bInput);
				logger.debug(sInput);
				bIn.reset();

				parse(bIn);

			} catch (final Exception x) {
				x.printStackTrace();
				throw new IOException(x.getMessage());
			}
			if (fault) {
				XmlRpcException exception = null;
				try {
					final Hashtable f = (Hashtable) result;
					final String faultString = (String) f.get("faultString");
					final int faultCode = Integer.parseInt(f.get("faultCode").toString());
					exception = new XmlRpcException(faultCode, faultString.trim());
				} catch (final Exception x) {
					throw new XmlRpcException(0, "Invalid fault response");
				}
				throw exception;
			}
			logger.debug("Spent " + (System.currentTimeMillis() - now) + " in request");

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

	} // end of inner class Worker

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
			final XmlRpcClient client = new XmlRpcClient(url);
			try {
				client.execute(method, v);
			} catch (final Exception ex) {
				System.err.println("Error: " + ex.getMessage());
			}
		} catch (final Exception x) {
			System.err.println(x);
			System.err.println("Usage: java helma.xmlrpc.XmlRpcClient <url> <method> <arg> ....");
		}
	}

	/**
	 * @return the socketType
	 */
	public String getSocketType() {
		return socketType;
	}

	/**
	 * @param socketType
	 *            the socketType to set
	 */
	public void setSocketType(final String socketType) {
		this.socketType = socketType;
	}

	/**
	 * @return the url
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(final URL url) {
		this.url = url;
	}

	/**
	 * @return the auth
	 */
	public String getAuth() {
		return auth;
	}

	/**
	 * @param auth
	 *            the auth to set
	 */
	public void setAuth(final String auth) {
		this.auth = auth;
	}
}
