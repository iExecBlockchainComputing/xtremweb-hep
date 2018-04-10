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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

import java.io.*;
import java.security.InvalidKeyException;
import java.sql.ResultSet;

/**
 * This class describes a row of the envs SQL table.
 * Please note that an envelope is always public
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 * @since 13.0.0
 */
public final class EnvelopeInterface extends Table {

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.App
	 *
	 */
	private static final String ENVTABLENAME = "envs";

	/**
	 * This the application name length as defined in DB
	 */
	private static final int ENVNAMELENGTH = 100;

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "envelope";

	/**
	 * This enumerates this interface columns. The base enumerations are defined
	 * in TableColumns.
	 */
	public enum Columns implements XWBaseColumn {
        /**
         * This is the column index of the name
         */
        NAME {
            /**
             * This creates an object from String representation for this column
             * value This cleans the parameter to ensure SQL compliance
             *
             * @param v
             *            the String representation
             * @return a Boolean representing the column value
             */
            @Override
            public String fromString(final String v) {
                return v.replaceAll("[\\n\\s\'\"]+", "_");
            }
        },
        /**
         * This is the column index of the ID which must be unic in DB
         */
        ENVID {
            /**
             * This creates an object from String representation for this column
             * value This cleans the parameter to ensure SQL compliance
             *
             * @param v
             *            the String representation
             * @return a Boolean representing the column value
             */
            @Override
            public Integer fromString(final String v) {
                return Integer.valueOf(v);
            }
        },
        /**
         * This is the column index of the max wall clock time; this is in seconds
         */
		MAXWALLCLOCKTIME {
			/**
			 * This creates an object from String representation for this column
			 * value This cleans the parameter to ensure SQL compliance
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
                return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the max free mass storage. This is in bytes
		 */
		MAXFREEMASSSTORAGE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
		/**
		 * This is the column index of the max file size. This is in bytes
		 */
		MAXFILESIZE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
		/**
		 * This is the column index of the max memory. This is in bytes
		 */
		MAXMEMORY{
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
		/**
		 * This is the column index of the max CPU speed. This is in percentage
		 * @link{https://docs.docker.com/engine/reference/run/#cpu-period-constraint}
		 */
		MAXCPUSPEED {
			/**
			 * This creates an object from String representation for this column
			 * value
			 * @param v the String representation
			 * @return an Float representing the column value
			 */
			@Override
            public Float fromString(final String v) {
                return Float.valueOf(v);
            }
		};
		/**
		 * This is the index based on ordinal so that the first value is
		 * TableColumns + 1
		 *
		 * @see Enum#ordinal()
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
		 * This creates a new object from String for the given column This must
		 * be overridden by enum which value is not a String
		 *
		 * @param v the String representation
		 * @return v
		 * @throws Exception is thrown on instantiation error
		 */
		@Override
		public Object fromString(final String v) throws Exception {
			return v;
		}

		/**
		 * This creates a new object from the digen SQL result set
		 *
		 * @param rs the SQL result set
		 * @return the object representing the column
		 * @throws Exception is thrown on instantiation error
		 */
		public final Object fromResultSet(final ResultSet rs) throws Exception {
			return this.fromString(rs.getString(this.toString()));
		}

		/**
		 * This retrieves an Columns from its integer value
		 *
		 * @param v is the integer value of the Columns
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
	 * This is the default constructor. Access rights are set to public
	 */
	public EnvelopeInterface() {

		super(THISTAG, ENVTABLENAME);
		setAttributeLength(ENUMSIZE);
		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(),
				Columns.NAME.getOrdinal(),
				Columns.ENVID.getOrdinal() });
		setMaxCpuSpeed(XWTools.DEFAULTCPUSPEED);
		setMaxFileSize(XWTools.MAXFILESIZE);
		setMaxFreeMassStorage(XWTools.MAXDISKSIZE);
		setMaxWallClockTime(XWTools.DEFAULTWALLCLOCKTIME);
		setMaxMemory(XWTools.MAXRAMSIZE);
	}

	/**
	 * This constructs a new object providing its primary key value
	 *
	 * @param uid
	 *            is this new object UID
	 */
	public EnvelopeInterface(final UID uid) throws IOException {
		this();
		setUID(uid);
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 */
	public EnvelopeInterface(final SQLRequest r) {
		this();
		setRequest(r);
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException on refill error
	 * @see #fill(ResultSet)
	 */
	public EnvelopeInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}
	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public EnvelopeInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}
	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #EnvelopeInterface(InputStream)
	 */
	public EnvelopeInterface(final File f) throws IOException, SAXException {
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
	public EnvelopeInterface(final InputStream input) throws IOException, SAXException {
		this();
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			getLogger().exception(e);
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
	public EnvelopeInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This fills columns from DB. Access rights are set to public
	 *
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException on data inconsistency
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setUID((UID) TableColumns.UID.fromResultSet(rs));
            setOwner((UID) TableColumns.OWNERUID.fromResultSet(rs));
            setAccessRights(XWAccessRights.DEFAULT);
			setName((String) Columns.NAME.fromResultSet(rs));
            setEnvId((Integer) Columns.ENVID.fromResultSet(rs));
			setMaxMemory((Long) Columns.MAXMEMORY.fromResultSet(rs));
			setMaxCpuSpeed((Float) Columns.MAXCPUSPEED.fromResultSet(rs));
			setMaxFreeMassStorage((Long) Columns.MAXFREEMASSSTORAGE.fromResultSet(rs));
			setMaxFileSize((Long) Columns.MAXFILESIZE.fromResultSet(rs));
			setMaxWallClockTime((Integer) Columns.MAXWALLCLOCKTIME.fromResultSet(rs));
		} catch (final Exception e) {
			getLogger().exception(e);
			throw new IOException(e.toString());
		}
		try {
			setErrorMsg( (String)TableColumns.ERRORMSG.fromResultSet(rs));
		} catch (final Exception e) {
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
	 * @return column label
	 */
	@Override
	public String getColumnLabel(final int i) throws IndexOutOfBoundsException {
        try {
            return TableColumns.fromInt(i).toString();
        } catch (final Exception e) {
            return Columns.fromInt(i).toString();
        }
    }
	/**
	 * This updates this object from interface. Access rights are set to public
	 */
	@Override
	public void updateInterface(final Table appitf) throws IOException {
		final EnvelopeInterface itf = (EnvelopeInterface) appitf;
		if (itf.getName() != null) {
			setName(itf.getName());
		}
		setAccessRights(XWAccessRights.DEFAULT);
		setMaxMemory(itf.getMaxMemory());
		setMaxCpuSpeed(itf.getMaxCpuSpeed());
		setMaxFreeMassStorage(itf.getMaxFreeMassStorage());
		setMaxFileSize(itf.getMaxFileSize());
		setMaxWallClockTime(itf.getMaxWallClockTime());
		setEnvId(itf.getEnvId());
	}

	/**
	 * This retrieves the max RAM needed for this envelope
	 *
	 * @return the max RAM needed for this envelope
	 * @exception IOException if value not defined
	 */
	public long getMaxMemory() throws IOException {
		final Long ret = (Long) getValue(Columns.MAXMEMORY);
		if (ret != null) {
			return ret.longValue();
		}
		throw new IOException("" + getEnvId() + " : no max memory");
	}

	/**
	 * This retrieves the max CPU speed for this envelope
	 *
	 * @return the max CPU speed
	 * @exception IOException if value not defined
	 */
	public float getMaxCpuSpeed() throws IOException {
		final Float ret = (Float) getValue(Columns.MAXCPUSPEED);
		if (ret != null) {
			return ret.floatValue();
		}
		throw new IOException("" + getEnvId() + " : no max cpu speed");
	}

	/**
	 * This retrieves the maximum amount of disk for this envelope
	 *
	 * @return the max disk space, in bytes
	 * @exception IOException if value not defined
	 */
	public long getMaxFreeMassStorage() throws IOException {
		final Long ret = (Long) getValue(Columns.MAXFREEMASSSTORAGE);
		if (ret != null) {
			return ret.longValue();
		}
		throw new IOException("" + getEnvId() + " : no max free max storage");
	}
	/**
	 * This retrieves the max file size for this envelope
	 *
	 * @return the max file size, in bytes
	 * @exception IOException if value not defined
	 */
	public long getMaxFileSize() throws IOException {
		final Long ret = (Long) getValue(Columns.MAXFILESIZE);
		if (ret != null) {
			return ret.longValue();
		}
		throw new IOException("" + getEnvId() + " : no max file size");
	}

	/**
	 * This retrieves the maximum wall clock time for this envelope
	 *
	 * @return the max wall clock time, in seconds
	 * @exception IOException if value not defined
	 */
	public int getMaxWallClockTime() throws IOException {
		final Integer ret = (Integer) getValue(Columns.MAXWALLCLOCKTIME);
		if (ret != null) {
			return ret.intValue();
		}
		throw new IOException("" + getEnvId() + " : no max wall clock time");
	}

    /**
     * This gets this envelope name
     *
     * @return the name, or null if not set
     */
    public String getName() {
        try {
            return (String) getValue(Columns.NAME);
        } catch (final Exception e) {
        }
        return null;
    }
    /**
     * This gets this envelope name
     *
     * @return the name, or -1 if not set
     */
    public int getEnvId() {
    	final Integer ret = (Integer) getValue(Columns.ENVID);
    	if (ret != null) {
    		return ret.intValue();
    	}
        return -1;
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
	public final boolean setValue(final String attribute, final Object v) {
		final String uppercaseAttribute = attribute.toUpperCase();
		try {
			return setValue(TableColumns.valueOf(uppercaseAttribute), v);
		} catch (final Exception e) {
			return setValue(Columns.valueOf(uppercaseAttribute), v);
		}
	}

	/**
	 * This sets the maximum amount of RAM for this envelope
	 *
	 * @param v is the max amount of RAM in bytes
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setMaxMemory(final long v) {
		try {
			return setValue(Columns.MAXMEMORY, Long.valueOf(v < 0L ? 0L : v));
		} catch (final Exception e) {
		}
		return false;
	}

	/**
	 * This sets the max CPU speed for this envelope
	 *
	 * @param v is the max CPU speed in percentage
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMaxCpuSpeed(final float v) {
	    final float value = v > 1.0f ? 1.0f : v;
		return setValue(Columns.MAXCPUSPEED, Float.valueOf(value < 0.0f ? 0.5f : value));
	}

	/**
	 * This sets the max disk space for this envelope
	 *
	 * @param v is the max amount of disk space in bytes
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMaxFreeMassStorage(final long v) {
		try {
			return setValue(Columns.MAXFREEMASSSTORAGE, Long.valueOf(v < 0L ? 0L : v));
		} catch (final Exception e) {
		}
		return false;
	}
	/**
	 * This sets the max file size for this envelope
	 *
	 * @param v is the max file size, in bytes
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMaxFileSize(final long v) {
		try {
			return setValue(Columns.MAXFILESIZE, Long.valueOf(v < 0L ? 0L : v));
		} catch (final Exception e) {
		}
		return false;
	}
    /**
     * This sets the max computing time
     *
     * @param v is the max computing time, in seconds
     * @return true if value has changed, false otherwise
     */
    public boolean setMaxWallClockTime(final int v) {
        try {
            return setValue(Columns.MAXWALLCLOCKTIME, Integer.valueOf(v < 0 ? 0 : v));
        } catch (final Exception e) {
        }
        return false;
    }
    /**
     * This sets the max disk space for this envelope
     *
     * @param v is the max amount of disk space
     * @return true if value has changed, false otherwise
     */
    public boolean setEnvId(final int v) {
        try {
            return setValue(Columns.ENVID, Integer.valueOf(v < 0 ? 0 : v));
        } catch (final Exception e) {
        }
        return false;
    }
	/**
	 * This set this envelope name; name is eventually truncated to ENVNAMELENGTH
	 * @return true if value has changed, false otherwise
	 * @see #ENVNAMELENGTH
	 */
	public boolean setName(final String v) {
		String value = v;
		if ((value != null) && (value.length() > ENVNAMELENGTH)) {
			value = value.substring(0, ENVNAMELENGTH - 1);
			getLogger().warn("Name too long; truncated to " + value);
		}
		return setValue(Columns.NAME, value == null ? null : value);
	}

	/**
	 * This is for testing only Without any argument, this dumps a EnvelopeInterface
	 * object. If the first argument is an XML file containing a description of
	 * a EnvelopeInterface, this creates a EnvelopeInterface from XML description and
	 * dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.EnvelopeInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			LoggerLevel logLevel = LoggerLevel.DEBUG;
			try {
				logLevel = LoggerLevel.valueOf(System.getProperty(XWPropertyDefs.LOGGERLEVEL.toString()));
			} catch (final Exception e) {
			}
			final EnvelopeInterface itf = new EnvelopeInterface();
			itf.setUID(UID.getMyUid());
			if (argv.length > 0) {
				try {
					final XMLReader reader = new XMLReader(itf);
					reader.read(new FileInputStream(argv[0]));
				} catch (final XMLEndParseException e) {
				}
			}
			itf.setLoggerLevel(logLevel);
			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			final Logger logger = new Logger();
			logger.exception(
					"Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.EnvelopeInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
