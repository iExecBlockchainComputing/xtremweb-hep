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
import java.util.Date;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.communications.URI;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * TraceInterface.java
 *
 * Created: Feb 19th, 2002
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

/**
 * This class describes a row of the traces SQL table.
 */
public final class TraceInterface extends xtremweb.common.Table {

	private static final Logger logger = Logger.getLogger(TraceInterface.class);

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.Trace
	 *
	 * @since 9.0.0
	 */
	public static final String TABLENAME = ("traces");
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "trace";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns implements XWBaseColumn {
		HOSTUID {
			/**
			 * This creates a new UID from string
			 */
			@Override
			public UID fromString(final String v) {
				return new UID(v);
			}
		},
		/**
		 * This is the column index of this access rights if any
		 *
		 * @since 5.8.0
		 */
		ARRIVALDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		/**
		 * This is the column index of the start date
		 */
		STARTDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		ENDDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		FILE {
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
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
	public TraceInterface() {

		super(THISTAG, TABLENAME);
		setAttributeLength(ENUMSIZE);
		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal() });
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException
	 */
	public TraceInterface(final ResultSet rs) throws IOException {
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

			try {
				setHost((UID) Columns.HOSTUID.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setFile((URI) Columns.FILE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setStartDate((Date) Columns.STARTDATE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setEndDate((Date) Columns.ENDDATE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setArrivalDate((Date) Columns.ARRIVALDATE.fromResultSet(rs));
			} catch (final Exception e) {
			}
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public TraceInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #TraceInterface(InputStream)
	 */
	public TraceInterface(final File f) throws IOException, SAXException {
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
	public TraceInterface(final InputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 *
	 * @since 9.0.0
	 */
	public TraceInterface(final SQLRequest r) {
		this();
		setRequest(r);
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
	public TraceInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This updates this object from interface.
	 */
	@Override
	public void updateInterface(final Table titf) throws IOException {
		final TraceInterface itf = (TraceInterface) titf;
		setOwner(itf.getOwner());
		setAccessRights(itf.getAccessRights());
		setArrivalDate(itf.getArrivalDate());
		setStartDate(itf.getStartDate());
		setEndDate(itf.getEndDate());
		setFile(itf.getFile());
		setHost(itf.getHost());
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public UID getHost() {
		return (UID) getValue(Columns.HOSTUID);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getArrivalDate() {
		return (Date) getValue(Columns.ARRIVALDATE);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getStartDate() {
		return (Date) getValue(Columns.STARTDATE);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getEndDate() {
		return (Date) getValue(Columns.ENDDATE);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public URI getFile() {
		return (URI) getValue(Columns.FILE);
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
	 * @return true if value has changed, false otherwise
	 */
	public boolean setHost(final UID v) {
		return setValue(Columns.HOSTUID, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setArrivalDate(final Date v) {
		return setValue(Columns.ARRIVALDATE, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setStartDate(final Date v) {
		return setValue(Columns.STARTDATE, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setEndDate(final Date v) {
		return setValue(Columns.ENDDATE, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setFile(final URI v) {
		return setValue(Columns.FILE, v);
	}

	/**
	 * This is for testing only. Without any argument, this dumps a
	 * TraceInterface object. If the first argument is an XML file containing a
	 * description of a TraceInterface, this creates a TraceInterface from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.TraceInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final TraceInterface itf = new TraceInterface();
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
			logger.error("Caught exception, Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.TaskInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
