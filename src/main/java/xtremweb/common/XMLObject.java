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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.InvalidKeyException;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * XMLObject.java
 *
 * Created: March 22nd, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class (un)marshal java.lang.Object to/from XML Objects must be single
 * ones; nor a Vector, neither Hashtable etc.
 */
public class XMLObject extends XMLable {

	private static final Logger logger = Logger.getLogger(XMLObject.class);

	public static final String XMLTYPE = "type";
	public static final String XMLVALUE = "value";

	/**
	 * This stores this object type
	 */
	private Class type;
	/**
	 * This stores this object value
	 */
	private Object value;
	/**
	 * This defines an empty object by default (i.e. an XML object like
	 * "<XMLObject ... />") If false this object is not empty (i.e. an XML
	 * object like "<XMLObject> ... </XMLObject >")
	 */
	private boolean empty;

	/**
	 */
	protected XMLObject() {
		super("XMLOBJECT", 1);

		empty = true;
		value = null;
	}

	/**
	 * This calls XMLable(String, int)
	 *
	 * @see XMLable#XMLable(String, int)
	 */
	protected XMLObject(final String tag, final int last) {
		super(tag, last);
		empty = true;
	}

	/**
	 */
	protected XMLObject(final Object v) {
		super("XMLOBJECT", 1);

		value = v;
		empty = true;

		type = v.getClass();
		value = v;
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
	protected XMLObject(final DataInputStream input) throws IOException, SAXException {
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This constructs a new object from XML attributes
	 */
	protected XMLObject(final Attributes attrs) throws IOException {
		this();
		fromXml(attrs);
	}

	/**
	 * This retrieve this object value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(final Object value) {
		this.value = value;
	}

	/**
	 * @param empty
	 *            the empty to set
	 */
	public void setEmpty(final boolean empty) {
		this.empty = empty;
	}

	/**
	 * @return the empty
	 */
	public boolean isEmpty() {
		return empty;
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
	}

	/**
	 * This clears this hashtable
	 *
	 * @since 5.8.0
	 */
	@Override
	protected void clear() {
		type = null;
		value = null;
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		final StringBuilder ret = new StringBuilder("<" + getXMLTag() + " ");
		if (empty) {
			ret.append(XMLTYPE + "=\"" + type.getName() + "\" " + XMLVALUE + "=\"" + value.toString() + "\" />");
		} else {
			ret.append(
					XMLTYPE + "=\"" + type.getName() + "\">" + ((XMLObject) value).toXml() + "</" + getXMLTag() + ">");
		}
		return ret.toString();
	}

	/**
	 * This always throws an exception since XMLObject has no attributes
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public void fromXml(final Attributes attrs) {

		try {
			for (int a = 0; a < attrs.getLength(); a++) {
				final String attribute = attrs.getQName(a);
				final String v = attrs.getValue(a);

				if (attribute.compareToIgnoreCase(XMLTYPE) == 0) {
					type = Class.forName(v);
				} else if (attribute.compareToIgnoreCase(XMLVALUE) == 0) {
					final Constructor constructor = type.getConstructor(String.class);
					if (constructor != null) {
						final Object[] args = new Object[1];
						args[0] = v;
						logger.trace("type = " + type + " ; constructor = " + constructor);
						try {
							value = constructor.newInstance(args);
							logger.trace("value = " + value);
						} catch (final Exception e) {
							logger.error("Caught exception: ", e);
							value = null;
						}
					}

					if (value == null) {
						logger.warn("constructor not found : " + type + "(String)");
					}
				}
			}
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
	}

	@Override
	public String toString() {
		return type.toString() + " " + value.toString();
	}

	@Override
	public String toString(boolean csv) {
		return toString();
	}
}
