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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * XMLRPCtuple.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class (un)marshall tuple(key, value)
 */
public final class XMLtuple extends XMLable {

	private static final Logger logger = Logger.getLogger(XMLtuple.class);
	public static final String THISTAG = "XMLTUPLE";

	/**
	 * This stores the key, if any
	 */
	private XMLKey key;
	/**
	 * This stores the value
	 */
	private XMLValue value;

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
	 * This clears this object
	 *
	 * @since 5.8.0
	 */
	@Override
	protected void clear() {
		if (key != null) {
			key.clear();
		}
		key = null;
		if (value != null) {
			value.clear();
		}
		value = null;
	}

	/**
	 * This default constructor sets key and value to null
	 */
	public XMLtuple() {
		super(THISTAG, -1);

		key = null;
		value = null;
	}

	/**
	 * This constructor sets key and value
	 */
	public XMLtuple(final XMLKey k, final XMLValue v) {
		this();
		key = k;
		value = v;
	}

	/**
	 * This constructor sets key and value from Objects
	 *
	 * @param k
	 *            is the tuple key; a new XMLKey is created from this param
	 * @param v
	 *            is the tuple value. If v is an Hashtable, a new XMLHashtable
	 *            is created as value. If v is a Vector, a new XMLVector is
	 *            created as value Otherwise, a new XMLValue is created as
	 *            value.
	 *
	 */
	public XMLtuple(final Object k, final Object v) {
		this();
		key = new XMLKey(k);
		if (v instanceof Hashtable) {
			value = new XMLHashtable((Hashtable) v);
		} else if (v instanceof Vector) {
			value = new XMLVector((Vector) v);
		} else {
			value = new XMLValue(v);
		}
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
	public XMLtuple(final DataInputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This constructs a new object from XML attributes
	 */
	public XMLtuple(final Attributes attrs) {
		this();
		fromXml(attrs);
	}

	/**
	 * This does nothing since this has no attribute
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public void fromXml(final Attributes attrs) {
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		String ret = "<" + getXMLTag() + ">";

		if (key != null) {
			ret += key.toXml();
		}
		if (value != null) {
			ret += value.toXml();
		}

		ret += "</" + getXMLTag() + ">";

		return ret;
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

		if (qname.compareToIgnoreCase(getXMLTag()) == 0) {
			fromXml(attrs);
			return;
		} else {
			if ((qname.compareToIgnoreCase(XMLKey.THISTAG) == 0) && (key == null)) {

				logger.trace("new " + qname);
				key = new XMLKey(attrs);
				return;
			}
		}

		if (value == null) {
			logger.trace("new " + qname);
			if (qname.compareToIgnoreCase(XMLValue.THISTAG) == 0) {
				value = new XMLValue(attrs);
			} else if (qname.compareToIgnoreCase(XMLHashtable.THISTAG) == 0) {
				value = new XMLHashtable(attrs);
			} else if (qname.compareToIgnoreCase(XMLVector.THISTAG) == 0) {
				value = new XMLVector(attrs);
			}
		} else {
			value.xmlElementStart(uri, tag, qname, attrs);
		}
	}

	/**
	 * @see xtremweb.common.XMLObject#xmlElementStop(String, String, String)
	 */
	@Override
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		logger.trace("xmlElementStop (" + qname + ")");

		if (qname.compareToIgnoreCase(XMLKey.THISTAG) == 0) {
			key.xmlElementStop(uri, tag, qname);
		} else {
			value.xmlElementStop(uri, tag, qname);
		}

		super.xmlElementStop(uri, tag, qname);
	}

	/**
	 * this returns XML string representation
	 */
	@Override
	public String toString() {
		return toXml();
	}

	/**
	 * This calls toString()
	 *
	 * @param csv
	 *            is never used
	 * @see Table#toString(boolean)
	 */
	@Override
	public String toString(final boolean csv) {
		return toString();
	}

	/**
	 * This retrieves this tuple key
	 *
	 * @return this tuple key XML representation
	 */
	public XMLKey getXMLKey() {
		return key;
	}

	/**
	 * This retrieves this tuple value
	 *
	 * @return this tuple value XML representation
	 */
	public XMLValue getXMLValue() {
		return value;
	}

	/**
	 * This retreives this tuple key
	 *
	 * @return this tuple key
	 */
	public Object getKey() {
		return key.getValue();
	}

	/**
	 * This retreives this tuple value
	 *
	 * @return this tuple value
	 */
	public Object getValue() {
		return value.getValue();
	}

	/**
	 * This sets this tuple key
	 *
	 * @param k
	 *            is the new key
	 */
	public void setKey(final Object k) {
		key = new XMLKey(k);
	}

	/**
	 * This sets this tuple value
	 *
	 * @param v
	 *            is the new value
	 */
	public void setValue(final Object v) {
		value = new XMLValue(v);
	}

	/**
	 * This sets this tuple key
	 *
	 * @param k
	 *            is the new key
	 */
	public void setKey(final XMLKey k) {
		key = k;
	}

	/**
	 * This sets this tuple value
	 *
	 * @param v
	 *            is the new value
	 */
	public void setValue(final XMLValue v) {
		value = v;
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
			XMLtuple tuple = new XMLtuple(new Integer(1), new String("un"));

			if (argv.length == 1) {
				try (final FileInputStream fis = new FileInputStream(argv[0])) {
					final DataInputStream dis = new DataInputStream(fis);
					tuple = new XMLtuple(dis);
				} finally {
				}
			}

			System.out.println(tuple.toXml());
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
