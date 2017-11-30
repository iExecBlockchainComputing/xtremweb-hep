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
 * XML-RPC base class. See http://www.xmlrpc.com/
 */

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import xtremweb.common.Base64;
import xtremweb.common.XWTools;

/**
 * This abstract base class provides basic capabilities for XML-RPC, like
 * parsing of parameters or encoding Java objects into XML-RPC format. Any XML
 * parser with a <a href=http://www.megginson.com/SAX/>SAX</a> interface can be
 * used.
 * <p>
 * XmlRpcServer and XmlRpcClient are the classes that actually implement an
 * XML-RCP server and client.
 *
 * @see XmlRpcServer
 * @see XmlRpcClient
 */

public abstract class XmlRpc extends DefaultHandler {

	private static final Logger logger = Logger.getLogger(XmlRpc.class);

	public static final String version = "helma XML-RPC 1.0";

	private String methodName;

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(final String m) {
		methodName = m;
	}

	private static Class parserClass;
	private static Hashtable saxDrivers = new Hashtable();
	static {
		saxDrivers.put("minml", "uk.co.wilson.xml.MinML");
		saxDrivers.put("xp", "com.jclark.xml.sax.Driver");
		saxDrivers.put("ibm1", "com.ibm.xml.parser.SAXDriver");
		saxDrivers.put("ibm2", "com.ibm.xml.parsers.SAXParser");
		saxDrivers.put("aelfred", "com.microstar.xml.SAXDriver");
		saxDrivers.put("oracle1", "oracle.xml.parser.XMLParser");
		saxDrivers.put("oracle2", "oracle.xml.parser.v2.SAXParser");
		saxDrivers.put("openxml", "org.openxml.parser.XMLSAXParser");
	}

	private Stack values;
	private Value currentValue;

	public static final Formatter dateformat = new Formatter();

	private StringBuffer cdata;
	private boolean readCdata;

	public static final int STRING = 0;
	public static final int INTEGER = 1;
	public static final int BOOLEAN = 2;
	public static final int DOUBLE = 3;
	public static final int DATE = 4;
	public static final int BASE64 = 5;
	public static final int STRUCT = 6;
	public static final int ARRAY = 7;
	public static final int NIL = 8;

	private int errorLevel;

	/**
	 * @return the errorLevel
	 */
	public int getErrorLevel() {
		return errorLevel;
	}

	/**
	 * @param errorLevel
	 *            the errorLevel to set
	 */
	public void setErrorLevel(final int errorLevel) {
		this.errorLevel = errorLevel;
	}

	private String errorMsg;

	/**
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * @param errorMsg
	 *            the errorMsg to set
	 */
	public void setErrorMsg(final String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public static final int NONE = 0;
	public static final int RECOVERABLE = 1;
	public static final int FATAL = 2;

	private static boolean keepalive = false;

	private static String keyStoreFile;
	private static String passPhrase;

	public static final String types[] = { "String", "Integer", "Boolean", "Double", "Date", "Base64", "Struct",
			"Array", "Nil" };

	private String encoding = "ISO8859_1";

	/**
	 * Set the encoding of the XML. This should be the name of a Java encoding
	 * contained in the encodings Hashtable.
	 */
	public void setEncoding(final String enc) {
		encoding = enc;
	}

	/**
	 * Get the encoding of the XML. This should be the name of a Java encoding
	 * contained in the encodings Hashtable.
	 */
	public String getEncoding() {
		return encodings.getProperty(encoding, encoding);
	}

	private static Properties encodings = new Properties();
	static {
		encodings.put("UTF8", "UTF-8");
		encodings.put("ISO8859_1", "ISO-8859-1");
	}

	/**
	 * Set the SAX Parser to be used. The argument can either be the full class
	 * name or a user friendly shortcut if the parser is known to this class.
	 * The parsers that can currently be set by shortcut are listed in the main
	 * documentation page. If you are using another parser please send me the
	 * name of the SAX driver and I'll include it in a future release. If
	 * setDriver() is never called then the System property "sax.driver" is
	 * consulted. If that is not defined the driver defaults to OpenXML.
	 */
	public static void setDriver(final String driver) throws ClassNotFoundException {
		String parserClassName = null;
		try {
			parserClassName = (String) saxDrivers.get(driver);
			if (parserClassName == null) {
				parserClassName = driver;
			}
			parserClass = Class.forName(parserClassName);
		} catch (final ClassNotFoundException x) {
			throw new ClassNotFoundException("SAX driver not found: " + parserClassName);
		}
	}

	/**
	 * Set the SAX Parser to be used by directly passing the Class object.
	 */
	public static void setDriver(final Class driver) {
		parserClass = driver;
	}

	/**
	 * Switch HTTP keepalive on/off.
	 */
	public static void setKeepAlive(final boolean val) {
		keepalive = val;
	}

	/**
	 * get current HTTP keepalive mode.
	 */
	public static boolean getKeepAlive() {
		return keepalive;
	}

	/**
	 * set the name of the keyStoreFile
	 */
	public static void setKeyStore(final String name) {
		keyStoreFile = name;
	}

	/**
	 * get the name of the keyStoreFile
	 */
	public static String getKeyStore() {
		return keyStoreFile;
	}

	/**
	 * set the pass phrase
	 */
	public static void setPassPhrase(final String name) {
		passPhrase = name;
	}

	/**
	 * get the pass phrase
	 */
	public static String getPassPhrase() {
		return passPhrase;
	}

	/**
	 * Parse the input stream. For each root level object, method
	 * <code>objectParsed</code> is called.
	 */
	synchronized void parse(final InputStream is) throws Exception {

		errorLevel = NONE;
		errorMsg = null;
		values = new Stack();
		if (cdata == null) {
			cdata = new StringBuffer(128);
		} else {
			cdata.setLength(0);
		}
		readCdata = false;
		currentValue = null;

		final long now = System.currentTimeMillis();
		if (parserClass == null) {
			setDriver(System.getProperty("sax.driver", "org.openxml.parser.XMLSAXParser"));
		}

		final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(is, this);
		notifyAll();
	}

	/**
	 * Writes the XML representation of a supported Java object to the XML
	 * writer.
	 */
	void writeObject(final Object what, final XmlWriter writer) {
		writer.startElement("value");
		if (what == null) {
			writer.emptyElement("nil");
		} else if (what instanceof String) {
			writer.chardata(what.toString());
		} else if (what instanceof Integer) {
			writer.startElement("int");
			writer.write(what.toString());
			writer.endElement("int");
		} else if (what instanceof Boolean) {
			writer.startElement("boolean");
			writer.write(((Boolean) what).booleanValue() ? "1" : "0");
			writer.endElement("boolean");
		} else if ((what instanceof Double) || (what instanceof Float)) {
			writer.startElement("double");
			writer.write(what.toString());
			writer.endElement("double");
		} else if (what instanceof Date) {
			writer.startElement("dateTime.iso8601");
			final Date d = (Date) what;
			writer.write(dateformat.format(d));
			writer.endElement("dateTime.iso8601");
		} else if (what instanceof byte[]) {
			writer.startElement("base64");
			writer.write(Base64.encode((byte[]) what));
			writer.endElement("base64");
		} else if (what instanceof Vector) {
			writer.startElement("array");
			writer.startElement("data");
			final Vector v = (Vector) what;
			final int l2 = v.size();
			for (int i2 = 0; i2 < l2; i2++) {
				writeObject(v.elementAt(i2), writer);
			}
			writer.endElement("data");
			writer.endElement("array");
		} else if (what instanceof Hashtable) {
			writer.startElement("struct");
			final Hashtable h = (Hashtable) what;
			for (final Enumeration e = h.keys(); e.hasMoreElements();) {
				final String nextkey = (String) e.nextElement();
				final Object nextval = h.get(nextkey);
				writer.startElement("member");
				writer.startElement("name");
				writer.write(nextkey);
				writer.endElement("name");
				writeObject(nextval, writer);
				writer.endElement("member");
			}
			writer.endElement("struct");
		} else {
			throw new RuntimeException("unsupported Java type: " + what.getClass());
		}
		writer.endElement("value");
	}

	/**
	 * This method is called when a root level object has been parsed.
	 */
	abstract void objectParsed(Object what);

	/**
	 * Method called by SAX driver.
	 */
	@Override
	public void characters(final char ch[], final int start, final int length) throws SAXException {
		if (!readCdata) {
			return;
		}
		cdata.append(ch, start, length);
	}

	/**
	 * Method called by SAX driver.
	 */
	public void endElement(final String name) throws SAXException {

		logger.debug("endElement: " + name);
		if ((currentValue != null) && readCdata) {
			currentValue.characterData(cdata.toString());
			cdata.setLength(0);
			readCdata = false;
		}

		if ("value".equals(name)) {
			final int depth = values.size();
			if ((depth < 2) || (values.elementAt(depth - 2).hashCode() != STRUCT)) {
				final Value v = currentValue;
				values.pop();
				if (depth < 2) {
					objectParsed(v.value);
					currentValue = null;
				} else {
					currentValue = (Value) values.peek();
					currentValue.endElement(v);
				}
			}
		}

		if ("member".equals(name)) {
			final Value v = currentValue;
			values.pop();
			currentValue = (Value) values.peek();
			currentValue.endElement(v);
		} else if ("methodName".equals(name)) {
			methodName = cdata.toString();
			cdata.setLength(0);
			readCdata = false;
		}
	}

	/**
	 * Method called by SAX driver.
	 */
	public void startElement(final String name, final Attributes atts) throws SAXException {

		logger.debug("startElement: " + name);

		if ("value".equals(name)) {
			final Value v = new Value();
			values.push(v);
			currentValue = v;
			cdata.setLength(0);
			readCdata = true;
		} else if ("methodName".equals(name)) {
			cdata.setLength(0);
			readCdata = true;
		} else if ("name".equals(name)) {
			cdata.setLength(0);
			readCdata = true;
		} else if ("string".equals(name)) {
			cdata.setLength(0);
			readCdata = true;
		} else if ("i4".equals(name) || "int".equals(name)) {
			currentValue.setType(INTEGER);
			cdata.setLength(0);
			readCdata = true;
		} else if ("boolean".equals(name)) {
			currentValue.setType(BOOLEAN);
			cdata.setLength(0);
			readCdata = true;
		} else if ("double".equals(name)) {
			currentValue.setType(DOUBLE);
			cdata.setLength(0);
			readCdata = true;
		} else if ("dateTime.iso8601".equals(name)) {
			currentValue.setType(DATE);
			cdata.setLength(0);
			readCdata = true;
		} else if ("base64".equals(name)) {
			currentValue.setType(BASE64);
			cdata.setLength(0);
			readCdata = true;
		} else if ("struct".equals(name)) {
			currentValue.setType(STRUCT);
		} else if ("array".equals(name)) {
			currentValue.setType(ARRAY);
		} else if ("nil".equals(name)) {
			currentValue.setType(NIL);
		}
	}

	@Override
	public void error(final SAXParseException e) throws SAXException {
		System.err.println("Error parsing XML: " + e);
		errorLevel = RECOVERABLE;
		errorMsg = e.toString();
	}

	@Override
	public void fatalError(final SAXParseException e) throws SAXException {
		System.err.println("Fatal error parsing XML: " + e);
		errorLevel = FATAL;
		errorMsg = e.toString();
	}

	/**
	 * This represents an XML-RPC Value while the request is being parsed.
	 */
	class Value {

		private int type;
		private Object value;
		private String nextMemberName;

		private Hashtable struct;
		private Vector array;

		/**
		 * Constructor.
		 */
		public Value() {
			this.type = STRING;
		}

		/**
		 * Notification that a new child element has been parsed.
		 */
		public void endElement(final Value child) {
			if (type == ARRAY) {
				array.addElement(child.value);
			} else if (type == STRUCT) {
				struct.put(nextMemberName, child.value);
			}
		}

		/**
		 * Set the type of this value. If it's a container, create the
		 * corresponding java container.
		 */
		public void setType(final int type) {
			this.type = type;
			if (type == ARRAY) {
				value = array = new Vector();
			}
			if (type == STRUCT) {
				value = struct = new Hashtable();
			}
		}

		/**
		 * Set the character data for the element and interpret it according to
		 * the element type
		 */
		public void characterData(final String cdata) {
			switch (type) {
			case INTEGER:
				value = new Integer(cdata.trim());
				break;
			case BOOLEAN:
				value = new Boolean("1".equals(cdata.trim()));
				break;
			case DOUBLE:
				value = new Double(cdata.trim());
				break;
			case DATE:
				try {
					value = dateformat.parse(cdata.trim());
				} catch (final ParseException p) {
					throw new RuntimeException(p.getMessage());
				}
				break;
			case BASE64:
				value = Base64.decode(cdata.getBytes());
				break;
			case STRING:
				value = cdata;
				break;
			case STRUCT:
				nextMemberName = cdata;
				break;
			}
		}

		@Override
		public int hashCode() {
			return type;
		}

		@Override
		public String toString() {
			return (types[type] + " element " + value);
		}
	}

	class XmlWriter {

		private final StringBuffer buf;
		private final String enc;

		public XmlWriter(final StringBuffer buf) {
			this(buf, encoding);
		}

		public XmlWriter(final StringBuffer buf, final String enc) {
			this.buf = buf;
			this.enc = enc;
			final String encName = encodings.getProperty(enc, enc);
			buf.append("<?xml version=\"1.0\" encoding=\"" + encName + "\"?>");
		}

		public void startElement(final String elem) {
			buf.append("<");
			buf.append(elem);
			buf.append(">");
		}

		public void endElement(final String elem) {
			buf.append("</");
			buf.append(elem);
			buf.append(">");
		}

		public void emptyElement(final String elem) {
			buf.append("<");
			buf.append(elem);
			buf.append("/>");
		}

		public void chardata(final String text) {
			final int l = text.length();
			for (int i = 0; i < l; i++) {
				final char c = text.charAt(i);
				switch (c) {
				case '<':
					buf.append("&lt;");
					break;
				case '&':
					buf.append("&amp;");
					break;
				default:
					buf.append(c);
				}
			}
		}

		public void write(final char[] text) {
			buf.append(text);
		}

		public void write(final String text) {
			buf.append(text);
		}

		@Override
		public String toString() {
			return buf.toString();
		}

		public byte[] getBytes() throws UnsupportedEncodingException {
			return buf.toString().getBytes(enc);
		}

	}

}

class Formatter {

	public Formatter() {
	}

	public synchronized String format(final Date d) {
		return XWTools.getSQLDateTime(d);
	}

	public synchronized Date parse(final String s) throws ParseException {
		return XWTools.getSQLDateTime(s);
	}
}
