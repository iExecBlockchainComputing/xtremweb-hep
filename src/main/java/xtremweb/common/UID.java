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
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This class describes an extension of java.rmi.server.UID so that we can
 * create a new UID from a String representation.<br />
 * This is typically needed to re-create an UID from its string value stored in
 * database
 */
public final class UID extends XMLable {

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "uid";
	/**
	 * This defines the null UID so that we can differentiate a null value
	 * (where value is not defined) to a empty value where value is defined to
	 * be empty<br />
	 * This is especially useful to force works.STDIN to null even if
	 * apps.STDIN is defined
	 */
	public static final UID NULLUID = newUID("00000000-0000-0000-0000-000000000000");
	/**
	 * This is the NULLUID label. This aims to ease client command line : to
	 * provide a null UID, the user only needs to provide the string "NULLUID"
	 * instead of the string "00000000-0000-0000-0000-000000000000"
	 */
	public static final String NULLUID_LABEL = "NULLUID";

	/**
	 * Tests if the argument is equal to NULLUID
	 *
	 * @param anUid
	 *            is the uid to test
	 * @return true if argument is equal to NULLUID
	 * @see #NULLUID
	 */
	public static boolean isNull(final UID anUid) {
		return NULLUID.equals(anUid);
	}

	/**
	 * Tests if this UID is null
	 *
	 * @see #isNull(UID)
	 */
	public boolean isNull() {
		return isNull(getMyUid());
	}

	/**
	 * This defines a new default UID at boot time
	 */
	private static UID myUid = new UID();

	/**
	 * @param m
	 *            the new value of myUid
	 */
	public static void setMyUid(final UID m) {
		myUid = m;
	}

	/**
	 * This is the unic identifier
	 */
	private UUID uuid;
	/**
	 * This is the UID column index
	 *
	 * @see XMLable#columns
	 */
	private static final int UID = FIRST_ATTRIBUTE;

	/**
	 * This constructs a new object, instantiating a new java.rmi.serverUID
	 */
	public UID() {
		super(THISTAG, UID);
		this.uuid = UUID.randomUUID();
		setColumnAt(UID, "UID");
	}

	/**
	 * This constructs a new instance from a UID String representation
	 *
	 * @param value
	 *            is the UID String representation
	 * @exception IllegalArgumentException
	 *                is thrown if parameter does not represents an UID
	 */
	private static UID newUID(final String value) throws IllegalArgumentException {
		return new UID(value);
	}

	/**
	 * This constructs a new instance from a UID String representation
	 *
	 * @param value
	 *            is the UID String representation
	 * @exception IllegalArgumentException
	 *                is thrown if parameter does not represents an UID
	 */
	public UID(final String value) throws IllegalArgumentException {
		fromString(value);
	}

	/**
	 * This constructs a new object by receiving XML representation from input
	 * stream
	 *
	 * @param in
	 *            is the input stream
	 * @exception IOException
	 *                is thrown on XML parsing error
	 */
	public UID(final DataInputStream in) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(in);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This compares this UID to provided one
	 *
	 * @param uid2
	 *            is the object to compare to; this params must be an Object and
	 *            not an UID to be correctly called if UID is used as key in
	 *            hashtable, for instance.
	 * @return false if uid2 is null or if uid2 differs from this
	 */
	@Override
	public boolean equals(final Object uid2) {
		if ((uid2 == null) || !(uid2 instanceof UID)) {
			return false;
		}
		boolean ret = false;
		final String uid2str = uid2.toString();
		final String thisstr = this.toString();
		ret = (uid2str.compareTo(thisstr) == 0);
		return ret;
	}

	/**
	 * This return this objet hash code.
	 *
	 * @return uid.hashCode() if uid is not null, NULLUID.hashCode() otherwise
	 * @see #uuid
	 * @see #NULLUID
	 */
	@Override
	public int hashCode() {
		if (uuid != null) {
			return uuid.hashCode();
		} else {
			return NULLUID.hashCode();
		}
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
			return uuid.toString();
		} catch (final NullPointerException e) {
		}
		return null;
	}

	/**
	 * This retrieves an UID from String representation<br />
	 * This uses java.rmi.server.UID#read(DataInput) method which algorithm is
	 * like:
	 * <ol>
	 * <li>DataInput.readInt()
	 * <li>DataInput.readLong()
	 * <li>DataInput.readShort()
	 * </ol>
	 *
	 * @param value
	 *            is the UID String representation
	 * @exception IllegalArgumentException
	 *                is thrown if parameter does not represents an UID
	 */
	public void fromString(final String value) throws IllegalArgumentException {

		if (value == null) {
			throw new IllegalArgumentException("value is null");
		}
		final String v = value.trim();
		if (v.compareToIgnoreCase(NULLUID_LABEL) == 0) {
			this.uuid = UUID.fromString(NULLUID.toString());
		} else {
			this.uuid = UUID.fromString(v);
		}
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
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}

		final Logger logger = getLogger();
		logger.finest("UID nb attributes  : " + attrs.getLength());

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);
			logger.finest("UID  ##  attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\"");

			if (attribute.compareToIgnoreCase(getColumnLabel(UID)) == 0) {
				try {
					fromString(value);
				} catch (final Exception e) {
					logger.exception(e);
				}
			}
		}
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
		}
	}

	/**
	 * This is for testing only
	 */
	public static void main(final String[] argv) {

		final Hashtable<UID, String> cache = new Hashtable<>();
		final Logger logger = new Logger();
		final UID org = new UID();
		UID copy = null;
		try {
			copy = new UID(org.toString());
		} catch (final Exception e) {
			logger.exception(e);
		}
		logger.info("copy.equals(org)) = " + copy.equals(org));

		cache.put(org, "this is our UID");

		for (int i = 0; i < 10; i++) {
			final UID uid = new UID();
			final String str = "str" + i;
			cache.put(uid, str);
		}

		final Enumeration<UID> enums = cache.keys();
		while (enums.hasMoreElements()) {
			final UID uid = enums.nextElement();
			logger.info("cache.get(" + uid + ")) = " + cache.get(uid));
		}
		logger.info("cache.get(copy) should be the same as cache.get(org)\n");
		logger.info("cache.get(org) = " + cache.get(org));
		logger.info("cache.get(copy) = " + cache.get(copy));
		logger.info("org = " + org);
		logger.info("org.hasCode = " + org.hashCode());
        String address="0xd881fab27d8b25dcf33b4fa93ace8cacdc5aff3d";
		logger.info("from " + address + " = " + new UID(UUID.nameUUIDFromBytes(address.getBytes()).toString()).toString());
	}

	/**
	 * This retrieves myUid
	 *
	 * @return myUid
	 */
	public static UID getMyUid() {
		return myUid;
	}
}
