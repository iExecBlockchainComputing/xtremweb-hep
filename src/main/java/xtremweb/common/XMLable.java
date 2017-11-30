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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;


import xtremweb.communications.URI;

/**
 * Created: Sept 15th, 2005<br />
 * This class implements XML un/marschalling
 *
 * @author Oleg Lodygensky
 * @since RPCXW-v5
 */

public abstract class XMLable {

	private static final Logger logger = Logger.getLogger(XMLable.class);

	/**
	 * This contains the version attribute name
	 *
	 * @since 5.8.0
	 */
	private static final String VERSIONATTRIBUTE = "version";
	/**
	 * This contains the version attribute value. This is set to null by default
	 * But this contains version as received in a message and this may contain
	 * another version : the sender one.
	 *
	 * @since 5.8.0
	 */
	private Version currentVersion = null;

	/**
	 * This sets version to current one.
	 *
	 * @since 5.8.0
	 * @see CommonVersion#getCurrent()
	 * @see #currentVersion
	 */
	public final void setCurrentVersion() {
		currentVersion = CommonVersion.getCurrent();
	}

	/**
	 * This sets version to null
	 *
	 * @since 5.8.0
	 * @see #currentVersion
	 */
	public void resetCurrentVersion() {
		currentVersion = null;
	}

	/**
	 * This sets version to the provided one
	 *
	 * @since 5.8.0
	 * @see CommonVersion#getCurrent()
	 * @see #currentVersion
	 */
	public void setCurrentVersion(final Version v) {
		currentVersion = v;
	}

	/**
	 * This retrieves this object version
	 *
	 * @since 5.8.0
	 * @see CommonVersion#getCurrent()
	 * @see #currentVersion
	 */
	public Version getCurrentVersion() {
		return currentVersion;
	}

	/**
	 * This stores attribute values, if any
	 *
	 * @since 5.8.0
	 */
	protected Object[] values;

	/**
	 * @return the values
	 */
	public Object[] getValues() {
		return values;
	}

	/**
	 * This retrieves the value given its index
	 *
	 * @param i
	 *            is the index
	 * @return the value at index i
	 */
	public Object getValueAt(final int i) {
		return values[i];
	}

	/**
	 * This sets the value at the given index
	 *
	 * @param i
	 *            is the index
	 * @param v
	 *            is the value
	 */
	public void setValueAt(final int i, final Object v) {
		values[i] = v;
	}

	/**
	 * This contains the standard XML header (&lt;?xml version='1.0'
	 * encoding='UTF-8'?&gt;)
	 */
	public static final String XMLHEADER = "<?xml version='1.0' encoding='UTF-8'?>";

	/**
	 * This is the XML root tag. This contains "xwhep"
	 */
	public static final String ROOTTAG = "xwhep";
	/**
	 * This is to ensure backward compatibility since prior to 5.8.0 ROOTTAG was
	 * not used.
	 * <ul>
	 * <li>fromXml(InputStream) sets it to false
	 * <li>xmlElementStart(String, String, String, Attributes) sets it to true
	 * if ROOTTAG is encountered
	 * <li>xmlElementStop(String, String, String) correctly stop parsing
	 * independently of the version of the sender
	 * </ul>
	 *
	 * @see #xmlElementStart(String, String, String, Attributes)
	 * @since 5.9.0
	 */
	private boolean rootTagFound;

	private final InputStream input;

	/**
	 * This calls xmlRootElement(false)
	 *
	 * @see #xmlRootElement(boolean)
	 * @since 5.8.0
	 */
	public final String openXmlRootElement() {
		return xmlRootElement(true);
	}

	/**
	 * This calls xmlRootElement(true)
	 *
	 * @see #xmlRootElement(boolean)
	 * @since 5.8.0
	 */
	public final String closeXmlRootElement() {
		return xmlRootElement(false);
	}

	protected static final String emptyString = new String("");

	/**
	 * This creates a string contains opening or closing root tag, depending on
	 * close param. This set the 'version' root tag attribute to
	 * CommonVersion#getCurrent()
	 *
	 * @param open
	 *            contains true to open the xml tag, false to close it
	 * @return a String containing the XML tag
	 * @see CommonVersion#getCurrent()
	 * @since 5.8.0
	 */
	public final String xmlRootElement(final boolean open) {
		if (currentVersion == null) {
			return emptyString;
		}
		if (open) {
			return "<" + ROOTTAG + " " + VERSIONATTRIBUTE + "=\"" + CommonVersion.getCurrent().toString() + "\">";
		}
		return "</" + ROOTTAG + ">";
	}

	/**
	 * This is the XML tag
	 */
	private String XMLTAG;

	public String getOpenTag() {
		return "<" + XMLTAG + ">";
	}

	/**
	 * 
	 * @param uri is the object uri
	 * @param mandat (since 11.0.0) is a user login for which a command could have a mandate 
	 * @return
	 */
	public String getOpenTag(final URI uri, final String mandat) {
		if (uri == null) {
			return getOpenTag();
		}
		final String dlattr = mandat == null ? "" : " mandatinglogin = \"" + mandat + "\"";
		return "<" + XMLTAG + " uri=\"" + uri.toString() + "\"" + dlattr + ">";
	}

	public String getCloseTag() {
		return "</" + XMLTAG + ">";
	}

	public String getXMLTag() {
		return XMLTAG;
	}

	public void setXMLTag(final String t) {
		XMLTAG = t;
	}

	/**
	 * This tells whether to dump null attribute values
	 */
	private boolean DUMPNULLS = false;

	/**
	 * This checks DUMPNULLS
	 *
	 * @return DUMPNULLS
	 * @see #DUMPNULLS
	 */
	public boolean isDUMPNULLS() {
		return DUMPNULLS;
	}

	/**
	 * This sets DUMPNULLS
	 *
	 * @param d
	 *            the value to set DUMPNULLS
	 * @see #DUMPNULLS
	 */
	public void setDUMPNULLS(final boolean d) {
		DUMPNULLS = d;
	}

	/**
	 * This is used in XML representation to describe null attributes
	 */
	protected static final String NULLVALUE = "NULL";

	/**
	 * This stores the attributes names
	 */
	private String[] columns = null;

	/**
	 * This retrieves column label from columns. This method used to be in
	 * XMLable only and not in inherited classes. But since 5.8.0, we want to be
	 * able to retrieve column labels accordingly to versions. A typical
	 * scenario would be : a distributed part (client, worker) connect to server
	 * and this part runs a different middleware version than the server. <br />
	 * Since 5.8.0:
	 * <ul>
	 * <li>the version is in XML root element
	 * <li>if version is null, this object version is prior to 5.8.0
	 * <li>this returns column label accordinlgy to this object version
	 * <li>this may return null if a column does not exist in this object
	 * version.
	 * <ul>
	 *
	 * @param i
	 *            is an index for array columns
	 * @exception IndexOutOfBoundsException
	 *                is thrown if i &lt; 0 or i &gt; LAST_ATTRIBUTE
	 * @see #columns
	 * @see #xmlRootElement(boolean)
	 * @see #currentVersion
	 * @return the column label or null if column does not exist in current
	 *         object version
	 */
	public String getColumnLabel(final int i) {
		return columns[i];
	}

	public void setColumnAt(final int i, final String v) {
		columns[i] = v;
	}

	public void setColumns(final String[] c) {
		columns = c.clone();
	}

	public void setColumns() {
		columns = new String[maxAttribute];
	}

	/**
	 * This returns columns
	 *
	 * @see #columns
	 */
	public String[] columns() {
		return columns;
	}

	/**
	 * This is the index of the first element
	 *
	 * @see #columns
	 */
	protected static final int FIRST_ATTRIBUTE = 0;
	/**
	 * This is the index of the last element
	 *
	 * @see #columns
	 */
	protected int lastAttribute;
	/**
	 * This is the number of element
	 *
	 * @see #columns
	 */
	protected int maxAttribute;

	/**
	 * @return the lAST_ATTRIBUTE
	 */
	public int getLastAttribute() {
		return lastAttribute;
	}

	/**
	 * This calls setAttributeSize(s + TableColumns.SIZE). This is called by
	 * interfaces (AppInterface...)
	 *
	 * @param l
	 *            is the attribute length
	 * @see #setAttributeSize(int)
	 * @since 9.0.0
	 */
	public void setAttributeLength(final int l) {
		setAttributeSize(l + TableColumns.SIZE);
	}

	/**
	 * This sets maxAttribute to s and lastAttribute to (maxAttribute - 1).
	 * Finally, this sets values to new Object[maxAttribute]. This is called by
	 * types (AppType...)
	 *
	 * @param s
	 *            is the attribute size
	 * @see #maxAttribute
	 * @see #values
	 * @since 9.0.0
	 */
	protected void setAttributeSize(final int s) {
		maxAttribute = s;
		lastAttribute = maxAttribute - 1;
		values = new Object[maxAttribute];
	}

	/**
	 * @return the lAST_ATTRIBUTE
	 */
	public int getMaxAttribute() {
		return maxAttribute;
	}

	/**
	 * This is called by the GC; this calls clear();
	 *
	 * @throws Throwable
	 *
	 * @since 5.8.0
	 * @see #clear()
	 */
	@Override
	protected void finalize() throws Throwable {
		clear();
		super.finalize();
	}

	/**
	 * This clears this object
	 *
	 * @since 5.8.0
	 */
	protected void clear() {
		if (values == null) {
			return;
		}

		for (int i = FIRST_ATTRIBUTE; i < maxAttribute; i++) {
			values[i] = null;
		}
		values = null;
	}

	/**
	 * This default constructor sets version to current version
	 */
	protected XMLable() {
		input = null;
		values = null;
		XMLTAG = null;
		rootTagFound = true;
		setCurrentVersion();
		lastAttribute = 0;
		maxAttribute = lastAttribute + 1;
	}

	/**
	 * This constructor sets XMLTAG, LAST_ATTRIBUTE and MAX_ATTRIBUTE
	 *
	 * @param tag
	 *            is this object XML tag
	 * @param last
	 *            is this object LAST_ATTRIBUTE
	 * @see #XMLTAG
	 * @see #lastAttribute
	 * @see #maxAttribute
	 */
	protected XMLable(final String tag, final int last) {
		this();
		XMLTAG = tag;
		setAttributeLength(last);
		setColumns();
	}

	/**
	 * This retrieves this object String representation
	 *
	 * @return this object String representation
	 */
	@Override
	public abstract String toString();

	/**
	 * This retrieves this object String representation where XWAccessRights are
	 * in hexadecimal This must be overridden; default is to return toString()
	 *
	 * @return toString()
	 */
	public String toHexString() {
		return toString();
	}

	/**
	 * This retrieves this object String representation where XWAccessRights are
	 * in hexadecimal This must be overridden; default is to return
	 * toString(csv)
	 *
	 * @return toString(csv)
	 */
	public String toHexString(final boolean csv) {
		return toString(csv);
	}

	/**
	 * This retrieves this object String representation where XWAccessRights are
	 * in hexadecimal This must be overridden; default is to return
	 * toString(csv, shortoutput)
	 *
	 * @return toString(csv, shortoutput)
	 */
	public String toHexString(final boolean csv, final boolean shortoutput) {
		return toString(csv, shortoutput);
	}

	/**
	 * This retrieves this object XML tag
	 *
	 * @return this object XML tag
	 */
	public String xmlTag() {
		return XMLTAG;
	}

	/**
	 * This retrieves this object XML representation
	 *
	 * @return this object XML representation
	 * @throws IOException
	 */
	public abstract String toXml();

	/**
	 * This writes this object XML representation to output stream
	 *
	 * @param o
	 *            is the output stream
	 */
	public void toXml(final DataOutputStream o) throws IOException {

		final String xml = toXml();
		o.write(xml.getBytes(XWTools.UTF8));
	}

	/**
	 * This returns a string representation of this object, in the form
	 * column='value',column='value',... in csv is false or in the form
	 * 'value','value',... if csv is true
	 *
	 * @param csv
	 *            tells whether CSV format is expected
	 */
	public abstract String toString(final boolean csv);

	/**
	 * This calls this#toString(csv). This should be overwritten by inheriting
	 * classes.
	 *
	 * @param csv
	 *            tells whether CSV format is expected
	 * @param shortOutput
	 *            is not used
	 */
	public String toString(final boolean csv, final boolean shortOutput) {
		return toString(csv);
	}

	/**
	 * This retrieves attributes from XML attributes.
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	public abstract void fromXml(final Attributes attrs);

	/**
	 * This stores the current XML element value
	 *
	 * @since 9.0.0
	 */
	private String currentValue;

	/**
	 * @return the current value
	 * @see #currentValue
	 * @since 9.0.0
	 */
	public String getCurrentValue() {
		return currentValue;
	}

	/**
	 * This sets currentValue to null
	 *
	 * @see #currentValue
	 * @since 9.0.0
	 */
	public void resetCurrentValue() {
		currentValue = null;
	}

	/**
	 * This retrieves XML element value
	 *
	 * @since 9.0.0
	 * @see XMLReader#read(InputStream)
	 */
	final public void characters(final char[] ch, final int start, final int length) throws SAXException {
		currentValue = new String(ch.clone(), start, length);
	}

	/**
	 * This decodes XML elements. Since 5.8.0, this method is not abstract any
	 * more to retrieve VERSION attribute
	 *
	 * @see XMLReader#read(InputStream)
	 */
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		if (qname.compareToIgnoreCase(ROOTTAG) == 0) {

			logger.trace("XMLable#xmlElementStart(" + uri + ", " + tag + ", " + qname + ") " + attrs.getLength());

			rootTagFound = true;

			for (int a = 0; a < attrs.getLength(); a++) {
				final String attribute = attrs.getQName(a);
				final String value = attrs.getValue(a);

				if (attribute.compareToIgnoreCase(VERSIONATTRIBUTE) == 0) {
					logger.trace("XMLable#xmlElementStart() set current version " + value);
					currentVersion = new Version(value);
				}
			}
		} else {
			throw new SAXException("XMLable not a " + ROOTTAG + " element (" + qname + ")");
		}
	}

	/**
	 * This is called on XML element close tag. This stops parsing if qname ==
	 * ROOTTAG. This is to ensure that we stop waiting incoming bytes since we
	 * don't close communication channel because we want to answer by the same
	 * channel
	 *
	 * @see XMLReader#read(InputStream)
	 * @exception SAXException
	 *                on XML error, or SAXException(XMLEndParseException()) to
	 *                force stop parsing
	 */
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		if (((qname.compareToIgnoreCase(ROOTTAG) == 0) && (rootTagFound))
				|| ((qname.compareToIgnoreCase(XMLTAG) == 0) && !rootTagFound)) {
			throw new XMLEndParseException(qname);
		}
	}
}
