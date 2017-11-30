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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.common.UID;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;

/**
 * This class describes an encapsulation of java.net.URI to ensure XtremWeb is
 * formed like :
 * <code>Connection.XWSCHEME + "://" + serverName + "/" + UID</code>
 *
 * @author Oleg Lodygensky
 * @since 2.0.0
 */
public class URI extends XMLable {

	private static final Logger logger = Logger.getLogger(URI.class);

	// static {
	// AtticProtocol.registerAttic();
	// }

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "uri";
	/**
	 * This is the URI
	 */
	private java.net.URI uri;
	/**
	 * This is the URI column index
	 *
	 * @see XMLable#columns
	 */
	private static final int URIIDX = FIRST_ATTRIBUTE;

	/**
	 * This tests if this contains a null UID If this does not contain an XWHEP
	 * URI (i.e. an HTTP one), this returns fale
	 *
	 * @see UID#isNull()
	 * @return UID#isNull() if this contains an UID, false otherwise
	 */
	public boolean isNull() {
		try {
			final UID uid = getUID();
			if (uid != null) {
				return uid.isNull();
			}
			return false;
		} catch(final Exception e) {
			return false;
		}
	}
	/**
	 * This constructs a new empty object
	 */
	private URI() {
		super(THISTAG, URIIDX);
		this.uri = null;
		setColumnAt(URIIDX, "URI");
		uri = null;
	}

	/**
	 * This constructs a new URI from string
	 *
	 * @param value
	 *            is the URI String representation
	 * @exception IOException
	 *                is thrown if parameter does not represents an UID
	 */
	public URI(final String value) throws URISyntaxException {
		this();
		fromString(value);
		checkHostAndScheme();
	}

	/**
	 * This constructs a new URI from XML attributes
	 *
	 * @since 7.5.0
	 */
	public URI(final Attributes attrs) {
		this();
		fromXml(attrs);
	}

	/**
	 * This calls this(server, -1, uid)
	 *
	 * @param server
	 *            is the xtremweb server name
	 * @param uid
	 *            is the referenced object uid
	 * @see #URI(String, int, UID)
	 */
	public URI(final String server, final UID uid) throws URISyntaxException {
		this(server, -1, uid);
		checkHostAndScheme();
	}

	/**
	 * This constructs a new URI
	 *
	 * @param server
	 *            is the xtremweb server name
	 * @param port
	 *            is the xtremweb server port
	 * @param uid
	 *            is the referenced object uid
	 * @since 5.9.0
	 */
	public URI(final String server, final int port, final UID uid) throws URISyntaxException {
		this();
		uri = new java.net.URI(Connection.xwScheme() + Connection.getSchemeSeparator() + server
				+ (port > 0 ? ":" + port : "") + (uid != null ? "/" + uid.toString() : ""));
		uri.normalize();
		checkHostAndScheme();
	}

	/**
	 * This constructs a new object by receiving XML representation from input
	 * stream
	 *
	 * @param in
	 *            is the input stream
	 * @exception IOException
	 *                is thrown on XML parsing error
	 * @throws URISyntaxException 
	 */
	public URI(final DataInputStream in) throws IOException, SAXException, URISyntaxException {
		this();
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(in);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
		checkHostAndScheme();
	}
	/**
	 * This checks the presence of server and scheme
	 * @throws URISyntaxException if server or scheme is null
	 */
	private void checkHostAndScheme() throws URISyntaxException {
		if (uri == null) {
			return;
		}
		if (!isFile() && ((getScheme() == null) || (getHost() == null))) {
			throw new URISyntaxException(uri.toString(), "host or scheme not set");
		}
	}

	/**
	 * This test equality
	 */
	@Override
	public boolean equals(final Object uri2) {
		if ((uri2 == null) || (uri == null) || !(uri2 instanceof URI)) {
			return false;
		}
		return (uri2.toString().compareTo(uri.toString()) == 0);
	}

	/**
	 * This return this object hash code.
	 */
	@Override
	public int hashCode() {
		if (uri != null) {
			return uri.hashCode();
		} else {
			return UID.NULLUID.hashCode();
		}
	}

	/**
	 * This returns this URI's UID, or null
	 */
	public UID getUID() {
		if (getPath() == null) {
			return null;
		}
		final String uidStr = getPath().substring(1, getPath().length());
		return new UID(uidStr);
	}

	/**
	 * This returns the host part of this URI
	 */
	public String getHost() {
		if(uri != null) {
			return uri.getHost();
		}
		return null;
	}

	/**
	 * This returns the port part of this URI
	 */
	public int getPort() {
		if(uri != null) {
			return uri.getPort();
		}
		return -1;
	}
	/**
	 * This returns the scheme part of this URI
	 */
	public String getScheme() {
		if(uri != null) {
			return uri.getScheme();
		}
		return null;
	}

	/**
	 * This returns the path part of this URI
	 */
	public String getPath() {
		if(uri != null) {
			return uri.getPath();
		}
		return null;
	}

	/**
	 * This check whether scheme is file one
	 *
	 * @return true if scheme is file one
	 * @see xtremweb.communications.Connection#fileScheme()
	 */
	public boolean isFile() {
		if (getScheme() == null) {
			return false;
		}
		return (getScheme() == null ? false : getScheme().compareToIgnoreCase(Connection.fileScheme()) == 0);
	}

	/**
	 * This check whether scheme is XtremWeb one
	 *
	 * @return true if scheme is XtremWeb one
	 * @see xtremweb.communications.Connection#xwScheme()
	 */
	public boolean isXtremWeb() {
		return (getScheme() == null ? false : getScheme().compareToIgnoreCase(Connection.xwScheme()) == 0);
	}

	/**
	 * This check whether scheme is attic one
	 *
	 * @return true if scheme is attic one
	 * @see xtremweb.communications.Connection#atticScheme()
	 */
	public boolean isAttic() {
		if(uri == null) {
			return false;
		}
		try {
			final URL url = new URL(uri.toString());
			return (url.getProtocol().compareToIgnoreCase(Connection.atticScheme()) == 0);
		} catch (final Exception e) {
		}
		return false;
	}

	/**
	 * This check whether scheme is http one
	 *
	 * @return true if scheme is http one
	 * @see xtremweb.communications.Connection#httpScheme()
	 */
	public boolean isHttp() {
		if(uri == null) {
			return false;
		}
		try {
			final URL url = new URL(uri.toString());
			return (url.getProtocol().compareToIgnoreCase(Connection.httpScheme()) == 0);
		} catch (final Exception e) {
		}
		return false;
	}

	/**
	 * This check whether scheme is http one
	 *
	 * @return true if scheme is http one
	 * @see xtremweb.communications.Connection#httpScheme()
	 * @since 7.3.2
	 */
	public boolean isHttps() {
		if(uri == null) {
			return false;
		}
		try {
			final URL url = new URL(uri.toString());
			return (url.getProtocol().compareToIgnoreCase(Connection.httpsScheme()) == 0);
		} catch (final Exception e) {
		}
		return false;
	}

	/**
	 * This calls toString(false)
	 */
	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * This calls java.util.UUID.toString()
	 *
	 * @param csv
	 *            is never used
	 */
	@Override
	public String toString(final boolean csv) {
		try {
			final String ret = uri.toString().replaceAll("&amp;", "&");
			return ret.replaceAll("&", "&amp;");
		} catch (final NullPointerException e) {
		}
		return null;
	}

	/**
	 * This retrieves an URI from String representation.
	 *
	 * @param value
	 *            is the URI String representation
	 * @exception IOException
	 *                is thrown if parameter does not represents an URI
	 */
	public final void fromString(final String value) throws URISyntaxException {
		if (value == null) {
			throw new URISyntaxException("string is null", "");
		}
		final String v = value.replaceAll("&amp;", "&");
		uri = new java.net.URI(v.replaceAll("&", "&amp;"));
		uri.normalize();
	}

	/**
	 * This retrieves this object XML representation
	 *
	 * @return this object XML representation
	 */
	@Override
	public String toXml() {
		return getOpenTag() + toString() + getCloseTag();
	}

	/**
	 * This retrieves attributes from XML attributes
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public final void fromXml(final Attributes attrs) {
		if (attrs == null) {
			return;
		}

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);
			logger.trace("     attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\""
					+ "getColumnLabel(URI) = " + getColumnLabel(URIIDX));

			if (attribute.compareToIgnoreCase(getColumnLabel(URIIDX)) == 0) {
				try {
					fromString(value);
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
				}
			}
		}
	}

	/**
	 * This is called by the XML parser
	 *
	 * @see xtremweb.common.XMLReader#read(java.io.InputStream)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {
		try {
			super.xmlElementStart(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		if (qname.compareToIgnoreCase(getXMLTag()) == 0) {
			fromXml(attrs);
		}
	}

	/**
	 * This is for debug purposes only
	 */
	public static void main(final String[] argv) {
		try {
			URI uri = new URI(argv[0]);
			System.out.println("uri = " + uri);
			if (uri.isFile() == true) {
				System.out.println(uri.getPath() + " exists = " + new File(uri.getPath()).exists());
			}
			try {
				uri.checkHostAndScheme();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			final Hashtable<URI, String> cache = new Hashtable<>();

			for (int i = 0; i < 10; i++) {
				uri = new URI("http://server/" + i);
				cache.put(uri, uri.toString());
			}

			final Enumeration<URI> enums = cache.keys();
			while (enums.hasMoreElements()) {
				uri = enums.nextElement();
				System.out.println("cache.get(" + uri + ")) = " + cache.get(uri));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
