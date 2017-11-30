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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * XMLRPCHashtable.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class (un)marshall hashtable This contents tuples (key, value)
 *
 * This may content :
 * <ul>
 * <li>any object that a constructor like object(String) (e.g Integer objects
 * have Integer(String) constructor)
 * <li>hashtables
 * <li>vectors
 * </ul>
 */
public final class XMLHashtable extends XMLValue {

	private static final Logger logger = Logger.getLogger(XMLHashtable.class);

	public static final String THISTAG = "XMLHashtable";

	/**
	 * This is the size columns index
	 *
	 * @see xtremweb.common.XMLable#columns
	 */
	private static final int SIZEIDX = 0;
	/**
	 * This is this hashtable size
	 */
	private int size;
	/**
	 * This count nested elements since this hashtable may content hashtables
	 * and vectors as elements. This is incremented on each new hashtable or
	 * vector, and decremented on each vector or hashtable endings.<br />
	 *
	 * This is needed since SAX API reads input sequentially
	 */
	private int nested = 0;
	/**
	 * This is the tuples index
	 *
	 * @see #tuples
	 */
	private int currentIndex;
	/**
	 * This stores hashtable values, if any
	 */
	private XMLtuple[] tuples;

	/**
	 */
	public XMLHashtable() {
		this((Hashtable) null);
	}

	/**
	 * @param h
	 *            is the hashtable to (un)marshal
	 */
	public XMLHashtable(final Hashtable h) {
		super(THISTAG, 0);
		size = 0;

		setEmpty(true);

		setAttributeLength(SIZEIDX);
		setColumns();
		setColumnAt(SIZEIDX, "SIZE");

		size = 0;
		currentIndex = 0;

		if (h == null) {
			return;
		}

		setEmpty(false);

		size = h.size();

		tuples = new XMLtuple[size];

		Enumeration myenum = h.keys();

		for (int i = 0; myenum.hasMoreElements(); i++) {

			final Object k = myenum.nextElement();

			if (k == null) {
				continue;
			}

			tuples[i] = new XMLtuple(k, h.get(k));
		}
		myenum = null;
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
	public XMLHashtable(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
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
	public XMLHashtable(final DataInputStream input) throws IOException, SAXException {
		this(new Hashtable());
		setEmpty(false);
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			logger.error("Caught exception: ", e);
		}
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
	public XMLHashtable(final Attributes attrs) {
		this(new Hashtable());
		setEmpty(false);
		fromXml(attrs);
	}

	/**
	 * This is called by the GC; this calls clear();
	 *
	 * @since 5.8.0
	 * @see #clear()
	 */
	@Override
	protected void finalize() {
		clear();
		super.finalize();
	}

	/**
	 * This clears this hashtable
	 *
	 * @since 5.8.0
	 */
	@Override
	protected void clear() {
		if (tuples == null) {
			return;
		}

		for (int i = 0; i < size; i++) {
			tuples[i].clear();
			tuples[i] = null;
		}
		tuples = null;
		size = 0;
		nested = 0;
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		final StringBuilder ret = new StringBuilder(
				"<" + getXMLTag() + " " + getColumnLabel(SIZEIDX) + "=\"" + size + "\" >");

		for (int i = 0; i < size; i++) {
			ret.append(tuples[i].toXml());
		}
		ret.append("</" + getXMLTag() + ">");

		return ret.toString();
	}

	/**
	 * This writes this object XML representation to output stream
	 *
	 * @param o
	 *            is the output stream to write to
	 */
	@Override
	public void toXml(final DataOutputStream o) throws IOException {

		final String strHead = "<" + getXMLTag() + " " + getColumnLabel(SIZEIDX) + "=\"" + size + "\" >";
		o.write(strHead.getBytes(XWTools.UTF8));
		for (int i = 0; i < size; i++) {
			tuples[i].toXml(o);
		}
		final String strTail = "</" + getXMLTag() + ">";
		o.write(strTail.getBytes(XWTools.UTF8));
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

		size = 0;

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);

			logger.trace("     attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\"");

			if (attribute.compareToIgnoreCase(getColumnLabel(SIZEIDX)) == 0) {
				size = Integer.parseInt(value);
			}
		}
		if (size > 0) {
			tuples = new XMLtuple[size];
		}
		currentIndex = 0;
	}

	/**
	 * This is called to decode XML elements. This increments nested on each
	 * "<XMLHashtable>" or "<XMLVector>"
	 *
	 * @see #nested
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

		logger.trace("xmlElementStart (" + qname + ") " + "size = " + size + "  currentIndex = " + currentIndex
				+ "  nested = " + nested);

		if ((size == 0) && (qname.compareToIgnoreCase(getXMLTag()) == 0)) {
			logger.trace("" + qname + ".fromXml(attrs)");
			fromXml(attrs);
		} else {

			if (size <= 0) {
				throw new SAXException("size <= 0 ?!?!");
			}

			if ((qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0)
					|| (qname.compareToIgnoreCase(XMLVector.THISTAG) == 0)) {

				nested++;
			}

			if (tuples[currentIndex] == null) {
				logger.trace("tuples[" + currentIndex + "]  = new XMLtuple(" + qname + ")");
				tuples[currentIndex] = new XMLtuple(attrs);
			}

			//
			// XMLtuple manages XMLKey and XMLValue
			//
			logger.trace("tuples[" + currentIndex + "].xmlElementStart(" + qname + ")");
			tuples[currentIndex].xmlElementStart(uri, tag, qname, attrs);
		}
	}

	/**
	 * This decrements nested on each "</XMLHashtable>" or "</XMLVector>".<br />
	 * This increment currentIndex is nested is 0; otherwise this calls
	 * tuples[currentIndex].xmlElementStop()
	 *
	 * @see #nested
	 * @see #currentIndex
	 * @see xtremweb.common.XMLObject#xmlElementStop(String, String, String)
	 */
	@Override
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		if (currentIndex < size) {
			tuples[currentIndex].xmlElementStop(uri, tag, qname);
		}

		if ((qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0)
				|| (qname.compareToIgnoreCase(XMLVector.THISTAG) == 0)) {

			nested--;
		}

		if ((qname.compareToIgnoreCase(XMLtuple.THISTAG) == 0) && (nested <= 0)) {
			currentIndex++;
		}

		logger.trace("xmlElementStop (" + qname + ") " + "size = " + size + "  currentIndex = " + currentIndex
				+ "  nested = " + nested);

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
	 * This retreives this content
	 *
	 * @return an hashtable
	 */
	@Override
	public Object getValue() {
		return getHashtable();
	}

	/**
	 * This retreives this content
	 *
	 * @return an hashtable
	 */
	public Hashtable getHashtable() {

		final Hashtable ret = new Hashtable(size);

		for (int i = 0; i < size; i++) {

			final Object k = tuples[i].getKey();
			final Object v = tuples[i].getValue();

			ret.put(k, v);
		}

		return ret;
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
			final Collection v = new Vector();
			v.add(new String("a string in vector"));
			v.add(new Integer(100));
			v.add(new Boolean("true"));
			final Hashtable h = new Hashtable();
			final Hashtable h2 = new Hashtable();
			h.put(new Integer(1), new String("un"));
			h.put(new String("deux"), new Integer(2));
			h.put(new String("a vector"), v);
			h.put(new String("a null UID"), UID.NULLUID);
			h.put(new String("a false boolean"), new Boolean("false"));
			h2.put(new Integer(10), new String("dix"));
			h2.put(new String("dix"), new Integer(10));
			h2.put(new String("a vector"), v);
			h.put(new String("an hashtable"), h2);
			XMLHashtable xmlh = new XMLHashtable(h);

			if (argv.length == 1) {
				try (FileInputStream fis = new FileInputStream(argv[0])) {
					xmlh = new XMLHashtable(new DataInputStream(fis));
				} finally {
				}
			}

			System.out.println(xmlh.toXml());

			final Hashtable ret = xmlh.getHashtable();
			System.out.println(ret);
			final Enumeration myenum = ret.keys();
			for (; myenum.hasMoreElements();) {

				final Object k = myenum.nextElement();

				if (k == null) {
					continue;
				}

				logger.debug("(" + k.toString() + "," + ret.get(k) + ") " + ret.get(k).getClass());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
