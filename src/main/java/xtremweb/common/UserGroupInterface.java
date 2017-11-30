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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * UserGroupInterface.java
 *
 * Created: Feb 19th, 2002
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @since v1r3-rc7
 * @version %I%, %G%
 */

/**
 * This class describes a row of the works SQL table.
 */
public final class UserGroupInterface extends xtremweb.common.Table {

	private static final Logger logger = Logger.getLogger(UserGroupInterface.class);

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.UserGroup
	 *
	 * @since 9.0.0
	 */
	public static final String TABLENAME = ("usergroups");
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "usergroup";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is the column index of the application web page, if any
		 *
		 * @since 7.0.0
		 */
		WEBPAGE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URL representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public URL fromString(final String v) {
				try {
					return new URL(v);
				} catch (final MalformedURLException e) {
					return null;
				}
			}
		},
		/**
		 * This defines if this user group can be a "project". Any user group
		 * can be a "project" except worker user group and administrator user
		 * group.
		 */
		PROJECT {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public Boolean fromString(final String v) throws URISyntaxException {
				return new Boolean(v);
			}
		},
		/**
		 * This is the column index of the user group label
		 */
		LABEL {
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
		 * @return v
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		@Override
		public Object fromString(final String v) throws Exception {
			return v;
		}

		/**
		 * This creates a new object from SQL result set
		 *
		 * @param rs
		 *            is the SQL result set
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
	 * This retrieves column label from enum Columns. This takes cares of this
	 * version. If this version is null, this version is prior to 5.8.0. Before
	 * 5.8.0, OWNERUID and ACCESSRIGHTS did not exist. Then this returns null
	 * for these two values.
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @since 5.8.0
	 * @return null if((version == null) &amp;&amp; ((i == OWNERUID.ordinal())
	 *         || (c == ACCESSRIGHTS.ordinal()))); column label otherwise
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
	 * This is the default constructor
	 */
	public UserGroupInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);

		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.LABEL.getOrdinal() });
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException
	 */
	public UserGroupInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
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
			setLabel((String) Columns.LABEL.fromResultSet(rs));
			try {
				setWebPage((URL) Columns.WEBPAGE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setProject((Boolean) Columns.PROJECT.fromResultSet(rs));
			} catch (final Exception e) {
				setProject(true);
			}
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
		setDirty(false);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public UserGroupInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #UserGroupInterface(InputStream)
	 */
	public UserGroupInterface(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This creates a new object that will be retreived with a complex SQL
	 * request
	 *
	 * @since 5.8.0
	 */
	public UserGroupInterface(final SQLRequest r) {
		this();
		setRequest(r);
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
	public UserGroupInterface(final InputStream input) throws IOException, SAXException {
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
	public UserGroupInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This is the default constructor
	 */
	public UserGroupInterface(final UID uid) {
		this();
		setUID(uid);
	}

	/**
	 * This updates this object from interface.
	 */
	@Override
	public void updateInterface(final Table gitf) throws IOException {
		final UserGroupInterface itf = (UserGroupInterface) gitf;
		if (itf.getAccessRights() != null) {
			setAccessRights(itf.getAccessRights());
		}
		if (itf.getOwner() != null) {
			setOwner(itf.getOwner());
		}
		if (itf.getLabel() != null) {
			setLabel(itf.getLabel());
		}
		setProject(itf.isProject());
	}

	/**
	 * This retrieves the web page
	 *
	 * @return the URL of the web page
	 * @since 7.0.0
	 */
	public URL getWebPage() {
		return (URL) getValue(Columns.WEBPAGE);
	}

	/**
	 * This retrieves is this can be a "project"
	 *
	 * @return UID
	 * @exception IOException
	 *                is thrown is attribute is not set
	 */
	public boolean isProject() {
		try {
			return ((Boolean) getValue(Columns.PROJECT)).booleanValue();
		} catch (final Exception e) {
			setProject(true);
			return true;
		}
	}

	/**
	 * This retrieves this group label, if any
	 */
	public String getLabel() {
		return (String) getValue(Columns.LABEL);
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
		final String A = attribute.toUpperCase();
		try {
			return setValue(TableColumns.valueOf(A), v);
		} catch (final Exception e) {
			return setValue(Columns.valueOf(A), v);
		}
	}

	/**
	 * This sets this user group web page
	 *
	 * @param v
	 *            is the URL
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setWebPage(final URL v) {
		return setValue(Columns.WEBPAGE, v);
	}

	/**
	 * This sets the label
	 *
	 * @return true if value has changed
	 */
	public boolean setLabel(final String v) {
		return setValue(Columns.LABEL, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since XWHEP 1.0.0
	 */
	public boolean setProject(final boolean v) {
		Boolean b = new Boolean(v);
		final boolean ret = setValue(Columns.PROJECT, b);
		b = null;
		return ret;
	}

	/**
	 * This change label to "DEL_" + uid and set ISDELETED flag to TRUE Because
	 * label is a DB constraint and one may want to insert and delete and then
	 * reinsert it with the same login. And delete does not remove row from DB :
	 * it simply set ISDELETED flag to true
	 */
	@Override
	public void delete() throws IOException {
		setLabel("DEL_ " + getUID().toString());
		update();
		super.delete();
	}

	/**
	 * This is for testing only. Without any argument, this dumps a
	 * UserGroupInterface object. If the first argument is an XML file
	 * containing a description of a UserGroupInterface, this creates a
	 * UserGroupInterface from XML description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.UserGroupInterface
	 * [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final UserGroupInterface itf = new UserGroupInterface();
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
					+ " xtremweb.common.UserGroupInterface [anXMLDescriptionFile]", e);
		}
	}
}
