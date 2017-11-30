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

package xtremweb.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * XMLRPCVector.java
 *
 * Created: Nov 28th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class (un)marshall vector. This contents values.
 *
 * This may content :
 * <ul>
 * <li>any object that a constructor like object(String) (e.g Integer objects
 * have Integer(String) constructor)
 * <li>hashtables
 * <li>vectors
 * </ul>
 */
public final class XMLVector extends XMLValue {

	private static final Logger logger = Logger.getLogger(XMLVector.class);

	public static final String THISTAG = "XMLVector";

	/**
	 * This is the size columns index
	 *
	 * @see xtremweb.common.XMLable#columns
	 */
	private final int SIZE = 0;
	/**
	 * This is this vector size
	 */
	private int expectedSize;

	public int size() {
		return expectedSize;
	}

	/**
	 * This count nested elements since this vector may content hashtables and
	 * vectors as elements. This is incremented on each new hashtable or vector,
	 * and decremented on each vector or hashtable endings.<br />
	 *
	 * This is needed since SAX API reads input sequentially
	 */
	private int nested = 0;
	/**
	 * This is the values index
	 */
	private int currentIndex;

	/**
	 * This is called by the GC; this calls clear();
	 *
	 * @since 5.8.0
	 * @see #clear()
	 */
	@Override
	protected void finalize() {
		clear();
	}

	/**
	 * This clears this vector
	 *
	 * @since 5.8.0
	 */
	@Override
	protected void clear() {
		if (getValue() != null) {
			((Vector<XMLValue>) getValue()).clear();
		}
		super.clear();
	}

	/**
	 */
	public XMLVector() {
		this((Vector) null);
	}

	/**
	 * @param v
	 *            is the vector to (un)marshal
	 */
	public XMLVector(final Vector v) {
		super(THISTAG, 0);

		setEmpty(false);
		setAttributeLength(SIZE);
		setColumns();
		setColumnAt(SIZE, "SIZE");

		expectedSize = 0;
		currentIndex = 0;

		if (v == null) {
			return;
		}

		expectedSize = v.size();

		setValue(new Vector<XMLValue>());

		Enumeration myenum = v.elements();

		for (; myenum.hasMoreElements();) {

			final Object obj = myenum.nextElement();

			if (obj == null) {
				continue;
			}
			final Vector<XMLValue> value = (Vector<XMLValue>) getValue();
			if (obj instanceof Hashtable) {
				value.add(new XMLHashtable((Hashtable) obj));
			} else if (obj instanceof Vector) {
				value.add(new XMLVector((Vector) obj));
			} else {
				value.add(new XMLValue(obj));
			}
		}
		myenum = null;
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 */
	public XMLVector(final DataInputStream input) throws IOException, SAXException {
		this(new Vector());
		setEmpty(false);
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This constructs a new object from XML attributes received from an input
	 * string
	 *
	 * @param input
	 *            is the input string
	 * @throws IOException
	 *             on XML error
	 */
	public XMLVector(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param attrs
	 *            contains attributes read from XML description
	 * @throws IOException
	 *             on XML error
	 */
	public XMLVector(final Attributes attrs) {
		this(new Vector());
		setEmpty(false);
		fromXml(attrs);
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {
		final Object value = getValue();
		if (value == null) {
			return "<" + getXMLTag() + " " + getColumnLabel(SIZE) + "=\"0\" ></" + getXMLTag() + ">";
		}

		String ret = "<" + getXMLTag() + " " + getColumnLabel(SIZE) + "=\"" + ((Vector<XMLValue>) value).size()
				+ "\" >";

		Enumeration<XMLValue> myenum = ((Vector<XMLValue>) value).elements();

		for (; myenum.hasMoreElements();) {
			final XMLValue v = myenum.nextElement();
			ret += v.toXml();
		}

		ret += "</" + getXMLTag() + ">";

		myenum = null;

		return ret;
	}

	/**
	 * This writes this object XML representation to output stream
	 *
	 * @param o
	 *            is the output stream to write to
	 */
	@Override
	public void toXml(final DataOutputStream o) throws IOException {

		String ret;
		byte[] strb = null;
		final Object value = getValue();
		if (value == null) {
			ret = "<" + getXMLTag() + " " + getColumnLabel(SIZE) + "=\"0\" ></" + getXMLTag() + ">";
			strb = ret.getBytes(XWTools.UTF8);
			o.write(strb);
			strb = null;
			return;
		}
		ret = "<" + getXMLTag() + " " + getColumnLabel(SIZE) + "=\"" + ((Vector<XMLValue>) value).size() + "\" >";
		strb = ret.getBytes(XWTools.UTF8);
		o.write(strb);
		strb = null;

		Enumeration<XMLValue> myenum = ((Vector<XMLValue>) value).elements();

		for (; myenum.hasMoreElements();) {
			final XMLValue v = myenum.nextElement();
			v.toXml(o);
		}
		ret = "</" + getXMLTag() + ">";
		strb = ret.getBytes(XWTools.UTF8);
		o.write(strb);
		strb = null;
		myenum = null;
	}

	/**
	 * This
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}

		expectedSize = 0;

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);
			if (attribute.compareToIgnoreCase(getColumnLabel(SIZE)) == 0) {
				expectedSize = new Integer(value).intValue();
			}
		}

		final Object value = getValue();
		if (value != null) {
			((Vector<XMLValue>) value).clear();
		}
		setValue(new Vector<XMLValue>());
		currentIndex = 0;
	}

	/**
	 * This is called to decode XML elements
	 *
	 * @see XMLReader#read(InputStream)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			super.xmlElementStart(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		final Object value = getValue();
		logger.trace("xmlElementStart (" + qname + ") " + "expectedSize = " + expectedSize + "  values.size = "
				+ (value != null ? ((Vector<XMLValue>) value).size() : 0) + "  currentIndex = " + currentIndex
				+ "  nested = " + nested);

		if (((value == null) || (((Vector<XMLValue>) value).size() == 0))
				&& (qname.compareToIgnoreCase(getXMLTag()) == 0)) {
			logger.trace("" + qname + ".fromXml(attrs)");
			fromXml(attrs);
		} else {

			if (expectedSize <= 0) {
				throw new SAXException("expectedSize <= 0 ?!?!");
			}

			if ((qname.compareToIgnoreCase(XMLVector.THISTAG) == 0)
					|| (qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0)) {

				nested++;
			}

			if (currentIndex >= ((Vector<XMLValue>) value).size()) {
				logger.trace("values[" + currentIndex + "]  = new XMValue(" + qname + ")");
				if (qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0) {
					((Vector<XMLValue>) value).add(new XMLHashtable(attrs));
				} else if (qname.compareToIgnoreCase(XMLVector.THISTAG) == 0) {
					((Vector<XMLValue>) value).add(new XMLVector(attrs));
				} else if (qname.compareToIgnoreCase(XMLValue.THISTAG) == 0) {
					((Vector<XMLValue>) value).add(new XMLValue(attrs));
				}
			} else {
				logger.debug("values[" + currentIndex + "].xmlElementStart(" + qname + ")");
				((Vector<XMLValue>) value).elementAt(currentIndex).xmlElementStart(uri, tag, qname, attrs);
			}
		}
	}

	/**
	 * This increment currentIndex on "</XML>" closing tag.
	 *
	 * @see #currentIndex
	 * @see xtremweb.common.XMLObject#xmlElementStop(String, String, String)
	 */
	@Override
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		final Object value = getValue();
		if (currentIndex < ((Vector<XMLValue>) value).size()) {
			((Vector<XMLValue>) value).elementAt(currentIndex).xmlElementStop(uri, tag, qname);
		}

		if ((qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0)
				|| (qname.compareToIgnoreCase(XMLVector.THISTAG) == 0)) {

			nested--;
		}

		if ((qname.compareToIgnoreCase(XMLValue.THISTAG) == 0) || (qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0)
				|| (qname.compareToIgnoreCase(XMLVector.THISTAG) == 0)) {

			if (nested <= 0) {
				currentIndex++;
			}
		}

		logger.trace("xmlElementStop (" + qname + ") " + "expectedSize = " + expectedSize + "  currentIndex = "
				+ currentIndex + "  nested = " + nested);

		super.xmlElementStop(uri, tag, qname);
	}

	/**
	 * This retreives this object String representation
	 *
	 * @return this object XML String representation
	 */
	@Override
	public String toString() {
		return toXml();
	}

	/**
	 * This returns a clone of value
	 *
	 * @see XMLObject#value
	 */
	public Vector<XMLValue> getXmlValues() {
		return (Vector<XMLValue>) ((Vector<XMLValue>) getValue()).clone();
	}

	/**
	 * This is for testing only.<br />
	 * If argv[0] is empty this creates a dummy XML representation<br />
	 * Otherwise, argv[0] may content an XML file name containing an XML
	 * representation to read. <br />
	 * <br />
	 * The dummy or read representation is finally dumped
	 */
	public static void main(final String[] argv) {
		try {
			final Vector v = new Vector();
			final Vector v2 = new Vector();
			v.add(new String("un"));
			v.add(new Integer(2));
			v.add(UID.NULLUID);
			v2.add(new String("dix"));
			v2.add(new String("cent"));
			v.add(v2);
			final Hashtable h = new Hashtable();
			h.put(new Integer(1), new String("un"));
			h.put(new String("deux"), new Integer(2));
			v.add(h);
			XMLVector xmlv = new XMLVector(v);

			if (argv.length == 1) {
				xmlv = new XMLVector(new DataInputStream(new FileInputStream(argv[0])));
			}

			System.out.println(xmlv.toXml());

			final Vector<XMLValue> ret = xmlv.getXmlValues();
			System.out.println(ret);
			final Enumeration<XMLValue> myenum = ret.elements();
			for (; myenum.hasMoreElements();) {

				final Object k = myenum.nextElement().getValue();

				if (k == null) {
					continue;
				}

				logger.debug("[" + k + "] " + k.getClass());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
