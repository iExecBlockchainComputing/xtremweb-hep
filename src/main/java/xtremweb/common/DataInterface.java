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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.communications.URI;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * DataInterface.java
 *
 * Created: 19 juillet 2006
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

public final class DataInterface extends Table {

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.Data
	 *
	 * @since 9.0.0
	 */
	public static final String DATATABLENAME = "datas";

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "data";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is the column index of the name if any.. This should considered
		 * as an alias.
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
		 * This is the column index of the links (how many uses this data)..
		 */
		LINKS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public Integer fromString(final String v) {
				return new Integer(v);
			}
		},
		/**
		 * This is the column index of the creation date
		 */
		INSERTIONDATE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return a Date representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		/**
		 * This is the column index of the OS version; this is a foreign key to
		 * "executables" table
		 *
		 * @since 9.0.0
		 */
		OSVERSION {
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
		 * This is the column index of the status
		 *
		 * @see StatusEnum
		 */
		STATUS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an XWStatus representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public StatusEnum fromString(final String v) {
				return StatusEnum.valueOf(v.toUpperCase());
			}
		},
		/**
		 * This is the column index of the data type
		 *
		 * @see xtremweb.common.DataType
		 */
		TYPE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return a DataType representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public DataTypeEnum fromString(final String v) {
				return DataTypeEnum.valueOf(v.toUpperCase());
			}
		},
		/**
		 * This is the column index of the CPU (needed for executables)
		 */
		CPU {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an XWCPUs representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public CPUEnum fromString(final String v) {
				return CPUEnum.valueOf(v.toUpperCase());
			}
		},
		/**
		 * This is the column index of the OS
		 */
		OS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an XWOses representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public OSEnum fromString(final String v) {
				return OSEnum.valueOf(v.toUpperCase());
			}
		},
		/**
		 * This is the column index of the size (in bytes)
		 */
		SIZE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return a Long representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public Long fromString(final String v) {
				return new Long(v);
			}
		},
		/**
		 * This is the column index of the SHASUM
		 */
		SHASUM,
		/**
		 * This is the column index of the URI
		 */
		URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the flag to tell whether this is sent to
		 * client
		 */
		SENDTOCLIENT {
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
		 * This is the column index of the referenced work UID for data driven
		 * scheduling
		 *
		 * @since 10.0.0
		 */
		WORKUID {
			/**
			 * This creates a new UID from string
			 */
			@Override
			public UID fromString(final String v) {
				return new UID(v);
			}
		},
		/**
		 * This is the column index of the package. This is optional. This is
		 * used by the scheduler. This must match worker SHAREDDATAS.
		 *
		 * @see xtremweb.database.SQLRequestWorkRequest
		 * @see HostInterface.Columns#SHAREDDATAS
		 * @since 10.0.0
		 */
		PACKAGE,
		/**
		 * This is the column index of the flag to tell whether this is a server
		 * local work or a replicated workd
		 */
		REPLICATED {
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
		 * This creates a new object from SQL restul set
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
	 * This is the default constructor
	 */
	public DataInterface() {

		super(THISTAG, DATATABLENAME);
		setAttributeLength(ENUMSIZE);
		setAccessRights(XWAccessRights.DEFAULT);
		setReplicated(false);
		setSendToClient(false);
		setStatus(StatusEnum.NONE);
		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(
				new int[] { TableColumns.UID.getOrdinal(), Columns.NAME.getOrdinal(), Columns.STATUS.getOrdinal() });
	}

	/**
	 * This constructs a new object providing its primary key value
	 *
	 * @param uid
	 *            is this new object UID
	 */
	public DataInterface(final UID uid) throws IOException {
		this();
		setUID(uid);
	}

	/**
	 * This creates a new object that will be retreived with a complex SQL
	 * request
	 *
	 * @since 5.8.0
	 */
	public DataInterface(final SQLRequest r) {
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
	public DataInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This retrieves column label from enum Columns. This takes cares of this
	 * version. Version 5.8.0 introduces no change in data definition so this
	 * works as it used to in prior versions
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @since 5.8.0
	 * @return column label as in versions prior to 5.8.0, since 5.8.0
	 *         introduces no change in data definition
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

			try {
				setURI((URI) Columns.URI.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setWork((UID) Columns.WORKUID.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setPackage((String) Columns.PACKAGE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setName((String) Columns.NAME.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setLinks(((Integer) Columns.LINKS.fromResultSet(rs)).intValue());
			} catch (final Exception e) {
				setLinks(0);
			}
			try {
				setStatus((StatusEnum) Columns.STATUS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setSize((Long) Columns.SIZE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setType((DataTypeEnum) Columns.TYPE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setOs((OSEnum) Columns.OS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setCpu((CPUEnum) Columns.CPU.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setShasum((String) Columns.SHASUM.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setReplicated((Boolean) Columns.REPLICATED.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setSendToClient((Boolean) Columns.SENDTOCLIENT.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setInsertionDate((Date) Columns.INSERTIONDATE.fromResultSet(rs));
			} catch (final Exception e) {
			}
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
		setDirty(false);
	}

	/**
	 * This calls this(StreamIO.stream(input))
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public DataInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #DataInterface(InputStream)
	 */
	public DataInterface(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @see XMLReader#read(InputStream)
	 * @throws SAXException
	 *             on XML error
	 */
	public DataInterface(final InputStream input) throws IOException, SAXException {
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
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @see Table#fromXml(Attributes)
	 * @throws IOException
	 *             on XML error
	 */
	public DataInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This sets this objects attributes from provided interface This does not
	 * insert/update this object into DB Updated attributes: access rights;
	 * name; type; cpu; os; status; size; shasum
	 *
	 * @since 9.0.0
	 */
	@Override
	public void updateInterface(final Table dataitf) throws IOException {

		final DataInterface itf = (DataInterface) dataitf;

		setAccessRights(itf.getAccessRights());
		setName(itf.getName());
		setType(itf.getType());
		if (getType() == DataTypeEnum.X509) {
			setAccessRights(XWAccessRights.USERALL);
		}
		setCpu(itf.getCpu());
		setWork(itf.getWork());
		setPackage(itf.getPackage());
		setOs(itf.getOs());
		setSize(itf.getSize());
		setShasum(itf.getShasum());
	}

	/**
	 * Please see {@link Type#toString(boolean, boolean, boolean)} Since 9.0.0,
	 * STATUS is replaced by STATUSID and TYPE by DATATYPEID. If shortOutput is
	 * true, they are converted to more human readable formats (enumeration
	 * labels)
	 *
	 * @since 9.0.0
	 */
	@Override
	public String toString(final boolean csv, final boolean shortOutput, final boolean hex) {

		try {
			int max = getMaxAttribute();
			final boolean shortDescription = (shortIndexes != null) && (shortOutput);

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
	 * This retrieves the URI
	 *
	 * @return this attribute, or null if not set
	 */
	public URI getURI() {
		return (URI) getValue(Columns.URI);
	}

	/**
	 * This retrieves the work reference for data driven scheduling
	 *
	 * @return this attribute, or null if not set
	 * @exception IOException
	 *                is thrown is attribute is not well formed
	 * @since 10.0.0
	 */
	public UID getWork() throws IOException {
		return (UID) getValue(Columns.WORKUID);
	}

	/**
	 * This retrieves the data package
	 *
	 * @return this attribute, or null if not set
	 * @since 10.0.0
	 */
	public String getPackage() {
		try {
			return (String) getValue(Columns.PACKAGE);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the creation date
	 *
	 * @return this attribute
	 * @exception IOException
	 *                is thrown is attribute is nor set, neither well formed
	 */
	public java.util.Date getInsertionDate() throws IOException {
		try {
			return (java.util.Date) getValue(Columns.INSERTIONDATE);
		} catch (final Exception e) {
			throw new IOException("DataInterface#getOwner() : attribute not set");
		}
	}

	/**
	 * This retrieves the number of links to this data
	 *
	 * @return this attribute
	 */
	public int getLinks() {
		try {
			return ((Integer) getValue(Columns.LINKS)).intValue();
		} catch (final Exception e) {
			setLinks(0);
			return 0;
		}
	}

	/**
	 * This retrieves this data size in bytes
	 *
	 * @return this attribute
	 */
	public Long getSize() {
		try {
			return ((Long) getValue(Columns.SIZE)).longValue();
		} catch (final Exception e) {
		}
		setSize(0);
		return 0L;
	}

	/**
	 * This retrieves this data type
	 *
	 * @return this attribute
	 */
	public DataTypeEnum getType() {
		try {
			return (DataTypeEnum) getValue(Columns.TYPE);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		setType(DataTypeEnum.NONE);
		return DataTypeEnum.NONE;
	}

	/**
	 * This retrieves this data CPU type
	 *
	 * @return this attribute or -1 if not set
	 */
	public CPUEnum getCpu() {
		try {
			return (CPUEnum) getValue(Columns.CPU);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves this data OS type
	 *
	 * @return this attribute or -1 if not set
	 */
	public OSEnum getOs() {
		try {
			return (OSEnum) getValue(Columns.OS);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the name
	 *
	 * @return this attribute or null if not set
	 */
	public String getName() {
		try {
			return (String) getValue(Columns.NAME);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the SHASUM
	 *
	 * @return this attribute or null if not set
	 */
	public String getShasum() {
		try {
			return (String) getValue(Columns.SHASUM);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves whether this data is replicated. This attr is forced to
	 * true, if not set
	 *
	 * @return this attribute
	 */
	public boolean isReplicated() {
		try {
			final Boolean ret = (Boolean) getValue(Columns.REPLICATED);
			return ret.booleanValue();
		} catch (final Exception e) {
			setReplicated(true);
			return true;
		}
	}

	/**
	 * This gets an attribute. This attr is forced to false if not set
	 *
	 * @return this attribute
	 */
	public boolean isSendToClient() {
		try {
			final Boolean ret = (Boolean) getValue(Columns.SENDTOCLIENT);
			return ret.booleanValue();
		} catch (final Exception e) {
			setSendToClient(false);
			return false;
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
	 * This set the URI where to get the stdin
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setURI(final URI v) {
		return setValue(Columns.URI, v);
	}

	/**
	 * This sets the referenced work for data driven scheduling
	 *
	 * @since 10.0.0
	 */
	public boolean setWork(final UID v) {
		return setValue(Columns.WORKUID, v);
	}

	/**
	 * This sets the data package
	 *
	 * @return true if value has changed, false otherwise
	 * @since 10.0.0
	 */
	public boolean setPackage(final String v) {
		return setValue(Columns.PACKAGE, v);
	}

	/**
	 * This sets the name
	 *
	 * @return true is value has changed
	 */
	public boolean setName(final String v) {
		return setValue(Columns.NAME, v == null ? null : v);
	}

	/**
	 * This sets the SHASUM
	 *
	 * @return true is value has changed
	 */
	public boolean setShasum(final String v) {
		return setValue(Columns.SHASUM, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setSendToClient(final boolean v) {
		final Boolean b = new Boolean(v);
		final boolean ret = setValue(Columns.SENDTOCLIENT, b);
		return ret;
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setReplicated(final boolean v) {
		final Boolean b = new Boolean(v);
		final boolean ret = setValue(Columns.REPLICATED, b);
		return ret;
	}

	/**
	 * This sets this data type
	 *
	 * @return true is value has changed
	 * @since 9.0.0
	 */
	public boolean setType(final DataTypeEnum v) {
		return setValue(Columns.TYPE, v);
	}

	/**
	 * This sets this data OS
	 *
	 * @return true is value has changed
	 */
	public boolean setOs(final OSEnum v) {
		return setValue(Columns.OS, v);
	}

	/**
	 * This sets this data CPU
	 *
	 * @return true is value has changed
	 */
	public boolean setCpu(final CPUEnum v) {
		return setValue(Columns.CPU, v);
	}

	/**
	 * This sets this data size
	 *
	 * @param v
	 *            is the data size in bytes
	 * @return true is value has changed
	 */
	public boolean setSize(final long v) {
		final Long l = new Long(v);
		return  setValue(Columns.SIZE, l);
	}

	/**
	 * This retrieves the status id
	 *
	 * @exception IOException
	 *                is thrown if attribute is not set
	 * @return this attribute
	 * @since 9.0.0
	 */
	public StatusEnum getStatus() {
		try {
			return (StatusEnum) getValue(Columns.STATUS);
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * This calls setStatusId(v.ordinal())
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setStatus(final StatusEnum v) throws IndexOutOfBoundsException {
		return setValue(Columns.STATUS, v);
	}

	/**
	 * This sets the number of links to this data
	 *
	 * @return true is value has changed
	 */
	public boolean setLinks(final int v) {
		int value = v;
		if (value < 0) {
			value = 0;
		}
		final Integer i = new Integer(value);
		final boolean ret = setValue(Columns.LINKS, i);
		return ret;
	}

	/**
	 * This increments the number of links to this data
	 *
	 * @return the new number of links
	 */
	public int incLinks() {
		setLinks(getLinks() + 1);
		return getLinks();
	}

	/**
	 * This decrements the number of links to this data
	 *
	 * @return the new number of links
	 */
	public int decLinks() {
		setLinks(getLinks() - 1);
		return getLinks();
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setInsertionDate(final Date v) {
		return setValue(Columns.INSERTIONDATE, v);
	}

	/**
	 * This return the data path
	 *
	 * @return the data path
	 */
	public String getPathName() throws IOException {
		return getPath().getAbsolutePath();
	}

	/**
	 * This return the data path
	 *
	 * @return the data path
	 */
	public File getPath() throws IOException {
		return new File(getDirName(), getUID().toString());
	}

	/**
	 * This return the data directory name
	 *
	 * @return the directory name
	 */
	public String getDirName() throws IOException {
		return getDir().getAbsolutePath();
	}

	/**
	 * This return the data directory name
	 *
	 * @return the directory name
	 */
	public File getDir() throws IOException {

		return XWTools.createDir(System.getProperty(XWPropertyDefs.HOMEDIR.toString()), getUID());
	}

	/**
	 * This deletes this data from the database table as well as associated
	 * files
	 */
	@Override
	public void delete() throws IOException {
		decLinks();
		if (getLinks() > 0) {
			update(false);
		} else {
			final File path = getPath();
			if (path != null) {
				path.delete();
			}
			final File rep = getDir();
			if (rep != null) {
				rep.delete();
			}
			super.delete();
		}
	}

	/**
	 * This is for testing only Without any argument, this dumps a DataInterface
	 * object. If the first argument is an XML file containing a description of
	 * a DataInterface, this creates a DataInterface from XML description and
	 * dumps it. Usage : java -cp xtremweb.jar xtremweb.common.DataInterface
	 * [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final DataInterface itf = new DataInterface();
			itf.setUID(UID.getMyUid());
			if (argv.length > 0) {
				try {
					final XMLReader reader = new XMLReader(itf);
					reader.read(new FileInputStream(argv[0]));
				} catch (final XMLEndParseException e) {
				}
			}
			itf.setLoggerLevel(LoggerLevel.DEBUG);
			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			final Logger logger = new Logger();
			logger.exception(
					"Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.DataInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
