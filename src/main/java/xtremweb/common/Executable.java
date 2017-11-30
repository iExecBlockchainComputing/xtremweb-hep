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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;


import xtremweb.communications.URI;
import xtremweb.database.SQLRequest;

/**
 * AppTypeInterface.java
 *
 * Created: Mar 31st, 2014
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 * @since 9.0.0
 */

public final class Executable extends Type {

	private static final Logger logger = Logger.getLogger(Executable.class);

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.App
	 */
	public static final String TABLENAME = "executables";

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "executable";

	/**
	 * This enumerates this interface columns. The base enumerations are defined
	 * in TableColumns.
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is the application UID
		 */
		APPUID {
			@Override
			public UID fromString(final String v) {
				return new UID(v);
			}
		},
		/**
		 * This is the data UID
		 */
		DATAUID {
			@Override
			public UID fromString(final String v) {
				return new UID(v);
			}
		},
		/**
		 * This is the data URI
		 */
		DATAURI {
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the OS name for this executable
		 */
		OSNAME {
			@Override
			public String fromString(final String v) {
				final String val = v;
				return val.replaceAll("[\\n\\s\'\"]+", "_");
			}
		},
		/**
		 * This is the OS version for this executable
		 */
		OSVERSION {
			@Override
			public String fromString(final String v) {
				final String val = v;
				return val.replaceAll("[\\n\\s\'\"]+", "_");
			}
		},
		/**
		 * This is the CPU name for this executable
		 */
		CPUTYPENAME {
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
		 */
		private int ord;

		/**
		 * This constructor sets the ord member as ord = this.ordinal
		 */
		Columns() {
			ord = this.ordinal();
		}

		/**
		 * This retrieves the index based ordinal
		 *
		 * @return the index based ordinal
		 */
		@Override
		public int getOrdinal() {
			return ord;
		}

		/**
		 * This creates a new object from String for the given column This must
		 * be overridden by enum which value is not a String
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
		 * This creates a new object from the digen SQL result set
		 *
		 * @param rs
		 *            the SQL result set
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
			for (final Columns c : Columns.values()) {
				if (c.getOrdinal() == v) {
					return c;
				}
			}
			throw new IndexOutOfBoundsException(("unvalid Columns value ") + v);
		}
	}

	/**
	 * This is the size
	 */
	private static final int ENUMSIZE = Columns.values().length;

	/**
	 * This is the default constructor
	 */
	public Executable() {
		super(THISTAG, TABLENAME);
		setAttributeSize(ENUMSIZE);
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 */
	public Executable(final SQLRequest r) {
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
	public Executable(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public Executable(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #Executable(InputStream)
	 */
	public Executable(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 * @see XMLReader#read(InputStream)
	 */
	public Executable(final InputStream input) throws IOException, SAXException {
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
	public Executable(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This fills columns from DB
	 *
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setApp((UID) Columns.APPUID.fromResultSet(rs));
			setData((UID) Columns.DATAUID.fromResultSet(rs));
			setOsName((String) Columns.OSNAME.fromResultSet(rs));
			setOsVersion((String) Columns.OSVERSION.fromResultSet(rs));
			setCpuTypeName((String) Columns.CPUTYPENAME.fromResultSet(rs));
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}

		setDirty(false);
	}

	/**
	 * This retrieves column label from enum Columns. This takes cares of this
	 * version. If this version is null, this version is prior to 5.8.0. Before
	 * 5.8.0, OWNERUID and ACCESSRIGHTS did not exist. Then this returns null
	 * for these two columns.
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @return null if((version == null) &amp;&amp; ((i ==
	 *         MACOS_X86_64URI.ordinal()) || (c ==
	 *         LDMACOS_X86_64URI.ordinal()))); column label otherwise
	 */
	@Override
	public String getColumnLabel(final int i) throws IndexOutOfBoundsException {
		return Columns.fromInt(i).toString();
	}

	/**
	 * This retrieves the app UID
	 *
	 * @return this attribute, or null if not set
	 */
	public String getApp() {
		try {
			return (String) getValue(Columns.APPUID);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the data UID
	 *
	 * @return this attribute, or null if not set
	 */
	public String getData() {
		try {
			return (String) getValue(Columns.DATAUID);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the CPU type name
	 *
	 * @return this attribute, or null if not set
	 */
	public String getCpuTypeName() {
		try {
			return (String) getValue(Columns.CPUTYPENAME);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the OS name
	 *
	 * @return this attribute, or null if not set
	 */
	public String getOsName() {
		try {
			return (String) getValue(Columns.OSNAME);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the OS version
	 *
	 * @return this attribute, or null if not set
	 */
	public String getOsVersion() {
		try {
			return (String) getValue(Columns.OSVERSION);
		} catch (final Exception e) {
		}
		return null;
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
		return setValue(Columns.valueOf(uppercaseAttr), v);
	}

	/**
	 * This sets the CPU type name
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setCpuTypeName(final String v) {
		return setValue(Columns.CPUTYPENAME, v);
	}

	/**
	 * This sets the OS name
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setOsName(final String v) {
		return setValue(Columns.OSNAME, v);
	}

	/**
	 * This sets the OS name
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setOsVersion(final String v) {
		return setValue(Columns.OSVERSION, v);
	}

	/**
	 * This sets the app UID
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setApp(final UID v) {
		return setValue(Columns.APPUID, v);
	}

	/**
	 * This sets the data UID
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setData(final UID v) {
		return setValue(Columns.DATAUID, v);
	}

	/**
	 * This is for testing only Without any argument, this dumps a AppInterface
	 * object. If the first argument is an XML file containing a description of
	 * a AppInterface, this creates a AppInterface from XML description and
	 * dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.AppInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final Executable itf = new Executable();
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
			logger.error("Caught exception, Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.AppInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
