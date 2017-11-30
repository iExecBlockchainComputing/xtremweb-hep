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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * GroupInterface.java
 *
 * Created: Feb 19th, 2002
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

/**
 * This class describes a row of the works SQL table.
 */
public final class GroupInterface extends xtremweb.common.Table {

	private static final Logger logger = Logger.getLogger(GroupInterface.class);

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.Group
	 *
	 * @since 9.0.0
	 */
	public static final String TABLENAME = "groups";

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "group";

	public enum Columns implements XWBaseColumn {

		/**
		 * This is the column index of the group name
		 */
		NAME {
			/**
			 * This creates an object from String representation for this column
			 * value This cleans the parameter to ensure SQL compliance
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public String fromString(final String v) {
				final String val = v;
				return val.replaceAll("[\\n\\s\'\"]+", "_");
			}
		},
		/**
		 * This is the column index of the UID of the associated session, if any
		 * <br />
		 */
		SESSIONUID {
			@Override
			public UID fromString(final String v) throws URISyntaxException {
				return new UID(v);
			}
		};

		/**
		 * This is the index based on ordinal so that the first value is
		 * TableColumns + 1
		 *
		 * @see xtremweb.common#TableColumns
		 * @see Enum#ordinal()
		 * @since 8.2.0
		 */
		private int ord;

		/**
		 * This constructor sets the ord member as ord = this.ordinal +
		 * TableColumns.SIZE
		 *
		 * @since 8.2.0
		 */
		Columns() {
			ord = this.ordinal() + TableColumns.SIZE;
		}

		/**
		 * This retrieves the index based ordinal
		 *
		 * @return the index based ordinal
		 * @since 8.2.0
		 */
		@Override
		public int getOrdinal() {
			return ord;
		}

		/**
		 * This creates a new object from String for the given column
		 *
		 * @param v
		 *            the String representation
		 * @return the object representing the column
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		@Override
		public Object fromString(final String v) throws Exception {
			throw new IOException("fromString() not implemented : " + this);
		}

		/**
		 * This creates a new object from SQL result set
		 *
		 * @param rs
		 *            is the SQL restul set
		 * @return the object representing the column
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		public final Object fromResultSet(final ResultSet rs) throws Exception {
			return this.fromString(rs.getString(this.toString()));
		}

		/**
		 * This retrieves an Columns from its integer value
		 *
		 * @param v
		 *            is the integer value of the Columns
		 * @return an Columns
		 */
		public static XWBaseColumn fromInt(final int v) throws IndexOutOfBoundsException {
			try {
				return TableColumns.fromInt(v);
			} catch (final Exception e) {
			}
			for (final Columns c : Columns.values()) {
				if (c.getOrdinal() == v) {
					return c;
				}
			}
			throw new IndexOutOfBoundsException(("unvalid Columns value ") + v);
		}
	}

	/**
	 * This is the size, including TableColumns
	 */
	private static final int ENUMSIZE = Columns.values().length;

	/**
	 * This is the default constructor
	 */
	public GroupInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);

		setAccessRights(XWAccessRights.USERALL);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.NAME.getOrdinal() });
	}

	/**
	 * This creates a new object that will be retreived with a complex SQL
	 * request
	 *
	 * @since 9.0.0
	 */
	public GroupInterface(final SQLRequest r) {
		this();
		setRequest(r);
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException
	 */
	public GroupInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This retrieves column label from enum Columns. This takes cares of this
	 * version. If this version is null, this version is prior to 5.8.0. Before
	 * 5.8.0, OWNERUID was known as CLIENTUID and ACCESSRIGHTS did not exist.
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @since 5.8.0
	 * @return null if((version == null) &amp;&amp; (i ==
	 *         ACCESSRIGHTS.ordinal())); "CLIENTUID" if((version == null)
	 *         &amp;&amp; (i == OWNERUID.ordinal())); column label otherwise
	 */
	@Override
	public String getColumnLabel(final int i) throws IndexOutOfBoundsException {
		try {
			return TableColumns.fromInt(i).toString();
		} catch (final Exception e) {
		}
		return Columns.fromInt(i).toString();
	}
	/**
	 * This fills columns from DB
	 *
	 * @since 9.0.0
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {
		try {
			setUID((UID) TableColumns.UID.fromResultSet(rs));
			setOwner((UID) TableColumns.OWNERUID.fromResultSet(rs));
			setAccessRights((XWAccessRights) TableColumns.ACCESSRIGHTS.fromResultSet(rs));
			setName((String) Columns.NAME.fromResultSet(rs));
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
		try {
			setSession((UID) Columns.SESSIONUID.fromResultSet(rs));
		} catch (final Exception e) {
		}
		setDirty(false);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public GroupInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #GroupInterface(InputStream)
	 */
	public GroupInterface(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @see XMLReader#read(InputStream)
	 * @throws IOException
	 *             on XML error
	 */
	public GroupInterface(final InputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @see Table#fromXml(Attributes)
	 * @throws IOException
	 *             on XML error
	 */
	public GroupInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This is the default constructor
	 */
	public GroupInterface(final UID uid) {
		this();
		setUID(uid);
	}

	/**
	 * This updates this object from interface.
	 *
	 * @since 9.0.0
	 */
	@Override
	public void updateInterface(final Table gitf) throws IOException {
		final GroupInterface itf = (GroupInterface) gitf;
		if (itf.getName() != null) {
			setName(itf.getName());
		}
		if (itf.getOwner() != null) {
			setOwner(itf.getOwner());
		}
		if (itf.getAccessRights() != null) {
			setAccessRights(itf.getAccessRights());
		}
		if (itf.getSession() != null) {
			setSession(itf.getSession());
		}
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getName() {
		return (String) getValue(Columns.NAME);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 * @exception IOException
	 *                is thrown is attribute is nor well formed
	 */
	public UID getSession() throws IOException {
		try {
			return (UID) getValue(Columns.SESSIONUID);
		} catch (final NullPointerException e) {
			return null;
		}
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
	 * @see Table#fromXml(Attributes)
	 */
	@Override
	public final boolean setValue(final String attribute, final Object v) throws IllegalArgumentException {
		final String uppercaseAttr = attribute.toUpperCase();
		try {
			return setValue(TableColumns.valueOf(uppercaseAttr), v);
		} catch (final Exception e) {
			return setValue(Columns.valueOf(uppercaseAttr), v);
		}
	}

	/**
	 * This sets the name
	 *
	 * @return true if name has changed, false otherwise
	 */
	public boolean setName(final String v) {
		return setValue(Columns.NAME, v);
	}

	/**
	 * This sets the session
	 *
	 * @return true if session has changed, false otherwise
	 */
	public boolean setSession(final UID v) {
		return setValue(Columns.SESSIONUID, v);
	}

	/**
	 * This is for testing only Without any argument, this dumps a
	 * GroupInterface object. If the first argument is an XML file containing a
	 * description of a GroupInterface, this creates a GroupInterface from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.GroupInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final GroupInterface itf = new GroupInterface();
			itf.setUID(UID.getMyUid());
			if (argv.length > 0) {
				try {
					final XMLReader reader = new XMLReader(itf);
					reader.read(new FileInputStream(argv[0]));
				} catch (final XMLEndParseException e) {
				}
			}
			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			logger.error("Caught exception, Usage : java -cp " + XWTools.JARFILENAME
					+ " xtremweb.common.GroupInterface [anXMLDescriptionFile]", e);
		}
	}
}
