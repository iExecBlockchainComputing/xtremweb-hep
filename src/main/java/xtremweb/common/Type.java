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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * This class defines methods for type tables (AppTypes, DataType etc.). Type
 * tables differ from interface ones (AppInterface, DataInterface etc.) as they
 * don't have columns defined in TableColumns.
 *
 * <br />
 * <br />
 * Created: Apr 14th, 2014<br />
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @since 9.0.0
 */
public abstract class Type extends XMLable {

	/**
	 * This is the table name
	 */
	protected String tableName;

	public String tableName() {
		return tableName;
	}

	/**
	 * This is the tag name
	 */
	protected String tagName;
	/**
	 * This tells whether object has changed since last DB I/O This was in
	 * xtremweb.dispatcher.TableRow until 9.0.0
	 *
	 * @since 9.0.0
	 */
	protected boolean dirty;
	/**
	 * This define default rows for "SELECT" SQL statement : "*" This was in
	 * xtremweb.dispatcher.TableRow until 9.0.0
	 *
	 * @since 9.0.0
	 */
	public static final String DEFAULTSELECTIONROW = "*";
	/**
	 * This helps to use complex SQL request This was in
	 * xtremweb.dispatcher.TableRow until 9.0.0
	 *
	 * @since 9.0.0
	 */
	private SQLRequest request;

	/**
	 * This is used in SQL statements
	 *
	 * @see xtremweb.common.XWTools#QUOTE
	 */
	public static final String QUOTE = XWTools.QUOTE;

	/**
	 * This is the primary key index in arrays columns and values<br />
	 * Since RPCXW, this KEYINDEX is not the index of the primary key as defined
	 * in DB, but the index of the UID. Since RPCXW, UID is always the first
	 * element in arrays columns and values.
	 *
	 * @see XMLable#columns
	 * @see #values
	 */
	public static final int KEYINDEX = 0;
	/**
	 * This contains column indexes to be used by short description
	 *
	 * @since 7.0.0
	 * @see #toString(boolean, boolean)
	 */
	protected int[] shortIndexes;

	/**
	 * @return the shortIndexes
	 */
	public int[] getShortIndexes() {
		return shortIndexes;
	}

	/**
	 * @param shortIndexes
	 *            the shortIndexes to set
	 */
	public void setShortIndexes(final int[] shortIndexes) {
		this.shortIndexes = shortIndexes.clone();
	}

	/**
	 * This default constructor sets all attributes to null
	 */
	public Type() {
		super();
		tagName = null;
		tableName = null;
		shortIndexes = null;
		setRequest(null);
		setDirty(true);
	}

	/**
	 * This constructor sets the table name as provided
	 *
	 * @param n
	 *            is this tag name
	 * @param t
	 *            is this table name
	 * @see #Type()
	 */
	public Type(final String n, final String t) {
		this();
		tagName = n;
		if (tagName != null) {
			setXMLTag(n);
		}
		tableName = t;
	}

	/**
	 * This fills columns from DB
	 *
	 * @since 9.0.0
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException
	 */
	abstract public void fill(ResultSet rs) throws IOException;

	/**
	 * This sets parameter with the right object type; this reads the value
	 * string representation and instantiates the value with the right object
	 * type.
	 *
	 * @param column
	 *            is the column to set
	 * @param val
	 *            is a String representation of the new attribute value
	 * @return true if value has changed, false otherwise
	 */
	protected final boolean setValue(final XWBaseColumn column, final Object val) {

		Object value = null;
		try {
			if (val != null) {
				value = column.fromString(val.toString().trim());
			}
		} catch (final Exception e) {
		}
		return setValue(column.getOrdinal(), value);
	}

	/**
	 * This sets parameter with the given date value; this reads the value
	 * string representation and instantiates the date value.
	 *
	 * @param column
	 *            is the column to set
	 * @param val
	 *            is a String representation of the new attribute value
	 * @return true if value has changed, false otherwise
	 * @since 8.2.0
	 * @see XWTools#getSQLDateTime(Date)
	 */
	protected final boolean setValue(final XWBaseColumn column, final Date val) {

		Object value = null;
		try {
			if (val != null) {
				value = column.fromString(XWTools.getSQLDateTime(val));
			}
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		return setValue(column.getOrdinal(), value);
	}

	/**
	 * This sets parameter value; this is called from
	 * TableInterface#fromXml(Attributes)
	 *
	 * @param attribute
	 *            is the name of the attribute to set
	 * @param v
	 *            is the new attribute value
	 * @return true if value has changed, false otherwise
	 * @see Type#fromXml(Attributes)
	 */
	public abstract boolean setValue(String attribute, Object v) throws IllegalArgumentException;

	/**
	 * This writes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		try {
			final StringBuilder ret = new StringBuilder(getOpenTag());
			for (int i = FIRST_ATTRIBUTE; i < getMaxAttribute(); i++) {

				final String attrLabel = getColumnLabel(i).toLowerCase();
				if (attrLabel == null) {
					continue;
				}

				final Object value = getValueAt(i);
				if (value != null) {
					ret.append("<" + attrLabel + ">");
					if (value instanceof java.util.Date) {
						ret.append(XWTools.getSQLDateTime((java.util.Date) value));
					} else if (value instanceof XWAccessRights) {
						ret.append(((XWAccessRights) value).toHexString());
					} else {
						ret.append(value.toString());
					}
					ret.append("</" + attrLabel + ">");
				} else {
					if (isDUMPNULLS()) {
						ret.append("<" + attrLabel + "></" + attrLabel + ">");
					}
				}
			}
			ret.append(getCloseTag());
			return ret.toString();
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		return null;
	}

	/**
	 * This writes this object to a String an HTML table
	 *
	 * @return a String containing this object definition as HTML
	 */
	public String toHtml() {

		try {
			final StringBuilder ret = new StringBuilder("<table>");
			for (int i = FIRST_ATTRIBUTE; i < getMaxAttribute(); i++) {

				if (getColumnLabel(i) == null) {
					continue;
				}

				final Object value = getValueAt(i);
				if (value != null) {
					ret.append("<tr><td class='param''>" + getColumnLabel(i).toLowerCase() + "</td><td class='value'>");
					if (value instanceof java.util.Date) {
						ret.append(XWTools.getSQLDateTime((java.util.Date) value));
					} else {
						ret.append(value.toString());
					}
					ret.append("</td></tr>");
				}
			}
			ret.append("</table>");
			return ret.toString();
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves attributes from XML representation. Since 5.8.0, this does
	 * not throw an exception if attribute is unknown but displays a warning
	 * message to allow connection among different versions
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @throws IOException
	 *             on XML error
	 * @see #toXml()
	 * @see #setValue(String, Object)
	 * @deprecated since 9.0.0 columns are not stored as XML attributes any
	 *             more, but as XML entities. This is kept to be compliant with
	 *             8.x and below versions
	 */
	@Deprecated
	@Override
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			String value = attrs.getValue(a);

			getLogger()
					.finest("Type  ##  attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\"");

			if (value.compareToIgnoreCase(NULLVALUE) == 0) {
				value = null;
			}

			try {
				setValue(attribute, value);
			} catch (final IllegalArgumentException e) {
				getLogger().exception(
						getClass().getCanonicalName() + " : '" + attribute
								+ "' is not a valid attribute (you should use 'xwversion' to check your client version)",
						e);
			}
		}
	}

	/**
	 * This is called by XML parser on XML element open tag This checks the
	 * qname attribute which must be the XML qualified name of either this
	 * object or of a column of this object
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
			getLogger().finest(ioe.toString());
		}

		getLogger().finest("Type#xmlElementStart(" + uri + ", " + tag + ", " + qname + ") " + attrs.getLength());

		if (qname.compareToIgnoreCase(getXMLTag()) == 0) {
			fromXml(attrs);
		} else {
			for (int i = FIRST_ATTRIBUTE; i < getMaxAttribute(); i++) {
				final String attrLabel = getColumnLabel(i).toLowerCase();
				if (attrLabel == null) {
					continue;
				}
				if (qname.compareToIgnoreCase(attrLabel) == 0) {
					return;
				}
			}
			throw new SAXException("Type  invalid qname : " + qname);
		}
	}

	/**
	 * This is called by XML parser on XML element close tag. This sets value
	 * retrieve by
	 *
	 * @see XMLable#characters(char[], int, int)
	 * @see XMLReader#read(InputStream)
	 * @exception SAXException
	 *                on XML error, or SAXException(XMLEndParseException()) to
	 *                force stop parsing
	 * @since 9.0.0
	 */
	@Override
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		super.xmlElementStop(uri, tag, qname);

		getLogger().finest("Type#xmlElementStop " + uri + ", " + tag + ", " + qname + " = " + getCurrentValue());
		try {
			setValue(qname, getCurrentValue());
		} catch (final IllegalArgumentException e) {
		} finally {
			resetCurrentValue();
		}
	}

	/**
	 * This calls toString(false)
	 *
	 * @see #toString(boolean)
	 */
	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * This calls toString(csv, false)
	 *
	 * @see #toString(boolean, boolean)
	 */
	@Override
	public String toString(final boolean csv) {
		return toString(csv, false);
	}

	/**
	 * This retrieves the effective index, depending of shortIndexes value.
	 *
	 * @return i, if shortIndexes == null; shortIndexes[i] otherwise
	 * @param i
	 *            is the index to retrieve
	 * @since 7.0.0
	 */
	public int getIndex(final int i, final boolean shortDescription) {
		int ret = i;
		if (shortIndexes == null) {
			return i;
		}
		if ((i < shortIndexes.length) && (shortDescription)) {
			ret = shortIndexes[i];
		}
		return ret;
	}

	/**
	 * This calls toString(csv, shortOutput, false)
	 *
	 * @see #toString(boolean,boolean,boolean)
	 */
	@Override
	public String toString(final boolean csv, final boolean shortOutput) {
		return toString(csv, shortOutput, false);
	}

	/**
	 * This calls toString(csv, shortOutput, true)
	 *
	 * @see #toString(boolean,boolean,boolean)
	 */
	@Override
	public String toHexString(final boolean csv, final boolean shortOutput) {
		return toString(csv, shortOutput, true);
	}

	/**
	 * This returns a string representation of this object, in the form
	 * column='value',column='value',... if csv is false or in the form
	 * 'value','value',... if csv is true. If shortIndexes == null or
	 * shortOutput == false, all attributes are included in the description.
	 * Otherwise this returns a short description only, containing attributes
	 * which indexes are in shortIndexes.
	 *
	 * @param csv
	 *            tells whether CSV format is expected
	 * @param shortOutput
	 *            if true, this generates a short description only
	 * @param hex
	 *            if true, integers are dumped as hexadecimal; decimal if false
	 * @return a String representation of this interface
	 */
	public String toString(final boolean csv, final boolean shortOutput, final boolean hex) {

		try {
			int max = getMaxAttribute();
			final boolean shortDescription = ((shortIndexes != null) && (shortOutput));

			if (shortDescription) {
				max = shortIndexes.length;
			}

			final StringBuilder ret = new StringBuilder();
			for (int i = FIRST_ATTRIBUTE; i < max; i++) {

				final int index = getIndex(i, shortOutput);

				if (getColumnLabel(index) == null) {
					continue;
				}

				if (index != FIRST_ATTRIBUTE) {
					ret.append(",");
				}

				final Object value = getValueAt(index);

				if (value != null) {
					if (value.getClass() == XWAccessRights.class) {
						String theValue = value.toString();
						if (hex) {
							theValue = ((XWAccessRights) value).toHexString();
						}
						if (csv) {
							ret.append(" " + theValue);
						} else {
							ret.append(" " + getColumnLabel(index) + "=" + theValue);
						}
					} else if (value.getClass() != java.util.Date.class) {
						if (csv) {
							ret.append(" " + QUOTE + value.toString() + QUOTE);
						} else {
							ret.append(" " + getColumnLabel(index) + "=" + QUOTE + value.toString() + QUOTE);
						}
					} else {
						final java.util.Date date = (java.util.Date) value;
						if (csv) {
							ret.append(" " + QUOTE + XWTools.getSQLDateTime(date) + QUOTE);
						} else {
							ret.append(
									" " + getColumnLabel(index) + "=" + QUOTE + XWTools.getSQLDateTime(date) + QUOTE);
						}
					}
				} else if (!csv) {
					ret.append(" " + getColumnLabel(index) + "=" + NULLVALUE);
				} else {
					ret.append(NULLVALUE);
				}
			}
			return ret.toString();
		} catch (final Exception e) {
			getLogger().exception(e);
		}

		return null;
	}

	/**
	 * This calls toHexString(false)
	 *
	 * @see #toHexString(boolean)
	 */
	@Override
	public String toHexString() {
		return toHexString(false);
	}

	/**
	 * This calls toHexString(csv, false)
	 *
	 * @see #toHexString(boolean, boolean)
	 */
	@Override
	public String toHexString(final boolean csv) {
		return toHexString(csv, false);
	}

	/**
	 * This returns a vector representation of this object<br />
	 * This is usefull for client GUI
	 *
	 * @return a Vector representation, or an empty Vector on error
	 * @see xtremweb.client.gui.TableModel#getRows()
	 */
	public Vector toVector() {

		final Vector ret = new Vector(getMaxAttribute());

		try {

			for (int i = FIRST_ATTRIBUTE; i < getMaxAttribute(); i++) {

				if (getColumnLabel(i) == null) {
					continue;
				}

				if (getValue(i) != null) {
					ret.add(getValue(i));
				} else {
					ret.add((""));
				}
			}
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		return ret;
	}

	/**
	 * This returns a string representation of all columns of this object, in
	 * the form column,column,... so that it can be used in SQL query
	 */
	final public String getColumns() {

		try {
			final StringBuilder ret = new StringBuilder();
			for (int i = FIRST_ATTRIBUTE; i < getMaxAttribute(); i++) {

				if (getColumnLabel(i) == null) {
					getLogger().error("getColumnLabel(" + i + ") = null?!?!");
					continue;
				}

				if (i != FIRST_ATTRIBUTE) {
					ret.append(",");
				}
				ret.append(getColumnLabel(i));
			}
			return ret.toString();
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This calls columns(shortOutput, false)
	 *
	 * @see #columns(boolean, boolean)
	 */
	final public String[] columns(final boolean shortOutput) {
		return columns(shortOutput, false);
	}

	/**
	 * This calls columns(shortOutput, true)
	 *
	 * @see #columns(boolean, boolean)
	 */
	final public String[] notnullcolumns(final boolean shortOutput) {
		return columns(shortOutput, true);
	}

	/**
	 * This returns an array of string of columns of this object, so that it can
	 * be used in GUI. This uses shortIndexes, if set. If shortIndexes == null
	 * or shortOutput == false, all attributes are included in the returned
	 * array
	 *
	 * @since 7.0.0
	 * @return an array of String containing column names
	 * @param shortOutput
	 *            if true, this generates a short description only
	 * @param notnull
	 *            if false, all columns are returned; if true, null columns are
	 *            not returned
	 */
	final public String[] columns(final boolean shortOutput, final boolean notnull) {

		try {

			int max = getMaxAttribute();
			final boolean shortDescription = ((shortIndexes != null) && (shortOutput));

			if (shortDescription) {
				max = shortIndexes.length;
			}

			final String[] ret = new String[max];

			for (int i = FIRST_ATTRIBUTE; i < max; i++) {

				final int index = getIndex(i, shortOutput);

				final String label = getColumnLabel(index);
				if ((label == null) || (notnull && (getValue(index) == null))) {
					continue;
				}

				ret[i] = label;
			}
			return ret;
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		return null;
	}

	/**
	 * This returns a string representation of this columns value object, in the
	 * form value,value,... so that it can be used in SQL query
	 */
	final public String valuesToString() {

		try {
			final StringBuilder ret = new StringBuilder();
			for (int i = FIRST_ATTRIBUTE; i < getMaxAttribute(); i++) {

				if (i != FIRST_ATTRIBUTE) {
					ret.append(",");
				}

				final Object value = getValueAt(i);
				if (value != null) {
					if (value.getClass() == XWAccessRights.class) {
						ret.append(value.toString());
					} else if (value.getClass() != java.util.Date.class) {
						ret.append(QUOTE + value + QUOTE);
					} else {
						final java.util.Date date = (java.util.Date) value;
						ret.append(QUOTE + XWTools.getSQLDateTime(date) + QUOTE);
					}
				} else {
					ret.append("NULL");
				}
			}
			return ret.toString();
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This creates the needed SQL query string to select work
	 *
	 * @return a String containing the needed SQL query conditions
	 */
	public String criteria() throws IOException {

		if (getRequest() != null) {
			return getRequest().getFullCriterias();
		}

		if (getColumnLabel(KEYINDEX) == null) {
			getLogger().error("\n\n\nWe got a huuuge problem, folks");
			getLogger().fatal(this.getClass().getName() + "#crietrias() : getColumnLabel(KEYINDEX) == null");
		}
		final Object value = getValueAt(KEYINDEX);
		if (value != null) {
			return (getColumnLabel(KEYINDEX) + "=" + QUOTE + value.toString() + QUOTE);
		}
		return null;
	}

	/**
	 * This sets parameter
	 *
	 * @param index
	 *            is the index of the attribute to set
	 * @param v
	 *            is the new attribute value
	 * @return true if value has changed, false otherwise
	 */
	final public boolean setValue(final int index, final Object v) {

		if ((index > getLastAttribute()) || (index < KEYINDEX)) {
			getLogger().error("\n\n\nWe got a huuuge problem, folks");
			getLogger().fatal(this.getClass().getName() + "#setValue() : invalid index " + index + " ("
					+ FIRST_ATTRIBUTE + ", " + getLastAttribute() + ")");
		}

		boolean change = false;

		try {
			final Object value = getValueAt(index);
			if (value == null) {
				change = (v != null);
			} else if (v == null) {
				change = true;
			} else {
				change = (value.equals(v) != true);
			}

			if (change) {
				setValueAt(index, v);
			}
		} catch (final Exception e) {
			getLogger().error("\n\n\nWe got a huuuge problem, folks");
			getLogger().fatal(this.getClass().getName() + "#setValue() " + e);
		}

		if (change == true) {
			setDirty(true);
		}

		return change;
	}

	/**
	 * This retrieves a parameter
	 *
	 * @param c
	 *            is the column of the parameter to retrieve
	 * @return the expected value
	 */
	public final Object getValue(final XWBaseColumn c) {
		return getValue(c.getOrdinal());
	}

	/**
	 * This retrieves a parameter
	 *
	 * @param index
	 *            is the attribute index to set
	 * @return the value of the attribute index
	 */
	public final Object getValue(final int index) {

		if ((index > getLastAttribute()) || (index < FIRST_ATTRIBUTE)) {
			getLogger().error("\n\n\nWe got a huuuge problem, folks");
			Thread.currentThread();
			Thread.dumpStack();
			getLogger().fatal(this.getClass().getName() + "#getValue() : invalid index " + index + " ("
					+ FIRST_ATTRIBUTE + ", " + getLastAttribute() + ")");
		}
		try {
			return getValueAt(index);
		} catch (final Exception e) {
			getLogger().error("\n\n\nWe got a huuuge problem, folks");
			Thread.currentThread();
			Thread.dumpStack();
			getLogger().fatal(this.getClass().getName() + "#getValue() " + e);
		}
		return null;
	}

	/**
	 * This constructs a new XMLRPCCommand object
	 *
	 * @param io
	 *            is stream handler to read XML representation from
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	public static Type newType(final StreamIO io)
			throws ClassNotFoundException, IOException, InvalidKeyException, AccessControlException {
		return newType(io.input());
	}

	/**
	 * This reads a new XMLRPCCommand object from input stream
	 *
	 * @param in
	 *            is the input stream
	 * @param itf
	 *            is the interface to read
	 * @throws IOException
	 *             on I/O error
	 * @throws InvalidKeyException
	 *             on authentication or authorization error
	 * @throws SAXException
	 *             on XML exception error
	 * @return the read interface
	 */
	private static Type readType(final BufferedInputStream input, final Type itf)
			throws InvalidKeyException, SAXException, IOException {
		try (final XMLReader reader = new XMLReader(itf)) {
			reader.read(input);
			return itf;
		}
	}

	/**
	 * This constructs a new XMLRPCCommand object. This first checks the opening
	 * tag and then instantiate the right object accordingly to the opening tag.
	 *
	 * @param in
	 *            is the input stream to read interface from
	 * @throws IOException
	 *             on I/O error
	 * @throws InvalidKeyException
	 *             on authentication or authorization error
	 */
	public static Type newType(final InputStream in) throws IOException, InvalidKeyException {

		final BufferedInputStream input = new BufferedInputStream(in);
		input.mark(XWTools.BUFFEREND);

		try {
			final Type ret = new AppType();
			return readType(input, ret);
		} catch (final SAXException e) {
		}
		throw new IOException("Unable to create new Interface from input stream");
	}

	/**
	 * This returns table name as needed by SQL requests; the table name is also
	 * aliased as SQLRequest.MAINTABLEALIAS
	 *
	 * @return the aliased main table name
	 * @see SQLRequest#MAINTABLEALIAS
	 * @see #tagName
	 * @since 5.8.0
	 */
	protected String aliasedTableName() {
		return tableName + " as " + SQLRequest.MAINTABLEALIAS;
	}

	/**
	 * This retrieves "FROM" SQL statement table names
	 *
	 * @return if request != null, this returns request.fromTableNames(); else
	 *         this returns aliasedTableName()
	 * @since 5.8.0
	 * @see SQLRequest#fromTableNames()
	 * @see #aliasedTableName()
	 */
	public String fromTableNames() {
		if (getRequest() != null) {
			return getRequest().fromTableNames();
		}
		return aliasedTableName();
	}

	/**
	 * This aims to retrieve rows for the "SELECT" SQL statement.
	 *
	 * @return DEFAULTSELECTIONROW
	 * @since 5.8.0
	 */
	public String rowSelection() throws IOException {
		if (getRequest() != null) {
			return getRequest().rowSelection();
		}
		return DEFAULTSELECTIONROW;
	}

	/**
	 * This aims to retreive "GROUP BY" SQL statement. This returns null and
	 * should be overriden
	 *
	 * @return null
	 * @since 5.8.0
	 */
	public String groupBy() throws IOException {
		return null;
	}

	/**
	 * @return the dirty
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * @param dirty
	 *            the dirty to set
	 */
	public final void setDirty(final boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * @return the request
	 */
	public SQLRequest getRequest() {
		return request;
	}

	/**
	 * @param request
	 *            the request to set
	 */
	public final void setRequest(final SQLRequest request) {
		this.request = request;
	}

	/**
	 * This is for testing only.<br />
	 * This creates a XMLRPCCommand from given XML String representation of any
	 * XMLRPCCommand descendant<br />
	 * argv[0] must contain an XML representation.<br />
	 * The object is finally dumped
	 */
	public static void main(final String[] argv) {
		try {
			final Type itf = Type.newType(new ByteArrayInputStream(argv[0].getBytes(XWTools.UTF8)));
			System.out.println(itf.openXmlRootElement() + itf.toXml() + itf.closeXmlRootElement());
		} catch (final Exception e) {
			final Logger logger = new Logger();
			logger.exception("Usage : java -cp " + XWTools.JARFILENAME
					+ " xtremweb.common.TableInterface [anXMLDescriptionFile]", e);
		}
	}
}
