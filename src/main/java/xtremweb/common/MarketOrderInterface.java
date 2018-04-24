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
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.ResultSet;

/**
 * @author Oleg Lodygensky
 * @since 13.1.0
 */

/**
 * This class describes a row of the works SQL table.
 */
public final class MarketOrderInterface extends Table {

	public static final String TABLENAME = "marketorders";
	public static final String THISTAG = "marketorder";

	public enum Columns implements XWBaseColumn {
        /**
         * ADK/BID ?
         * @see MarketOrderDirectionEnum
         */
		DIRECTION {
			@Override
			public MarketOrderDirectionEnum fromString(final String v) {
				return MarketOrderDirectionEnum.valueOf(v.toUpperCase());
			}
		},
        /**
         * The category for this order
         */
        CATEGORYID {
            @Override
            public Long fromString(final String v) {
                return Long.valueOf(v);
            }
        },
        /**
         * How many workers needed to safely ensure the trust
         */
        EXPECTEDWORKERS{
            @Override
            public Long fromString(final String v) {
                return Long.valueOf(v);
            }
        },
        /**
         * How many workers already booked to ensure the trust
         */
        NBWORKERS {
            @Override
            public Long fromString(final String v) {
                return Long.valueOf(v);
            }
        },
        /**
         * Trust level for this order
         */
		TRUST {
			@Override
			public Float fromString(final String v) {
				return Float.valueOf(v);
			}
		},
        /**
         * Depending on the direction this may be the cost; in smart contract this is named 'value'.
         * We can't name this 'value' because seteValue() and getValue() are the general purpose methods
         * in xtremweb.common.Table
         */
		PRICE {
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
        /**
         * How many of this order can be proposed
         */
		VOLUME {
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
        /**
         * How many of this order still not sold
         */
		REMAINING {
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
        /**
         * This is the worker pool addr
         */
		WORKERPOOLADDR {
			@Override
			public String fromString(final String v) {
				return v;
			}
		},
        /**
         * This is the schdeuler wallet addr
         */
		WORKERPOOLOWNERADDR {
			@Override
			public String fromString(final String v) {
				return v;
			}
		};

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
		 * This is the index based on ordinal so that the first value is
		 * TableColumns + 1
		 *
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
			for (final MarketOrderInterface.Columns c : MarketOrderInterface.Columns.values()) {
				if (c.getOrdinal() == v) {
					return c;
				}
			}
			throw new IndexOutOfBoundsException(("unvalid Columns value ") + v);
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
	}

	/**
	 * This is the size, including TableColumns
	 */
	private static final int ENUMSIZE = Columns.values().length;

	/**
	 * This is the default constructor
     * TRUST is forced to 0.5
     * EXPECTEDWORKERS is forced to 10
	 */
	public MarketOrderInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);

		setAccessRights(XWAccessRights.USERALL);
		setTrust(0.7f);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.CATEGORYID.getOrdinal(), Columns.DIRECTION.getOrdinal() });
	}

	/**
	 * This creates a new object that will be retreived with a complex SQL
	 * request
	 */
	public MarketOrderInterface(final SQLRequest r) {
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
	public MarketOrderInterface(final ResultSet rs) throws IOException {
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
            setCategoryId((Long) Columns.CATEGORYID.fromResultSet(rs));
            setExpectedWorkers((Long) Columns.EXPECTEDWORKERS.fromResultSet(rs));
            setNbWorkers((Long) Columns.NBWORKERS.fromResultSet(rs));
			setDirection((MarketOrderDirectionEnum) Columns.DIRECTION.fromResultSet(rs));
			setPrice((Long) Columns.PRICE.fromResultSet(rs));
			setTrust((Float) Columns.TRUST.fromResultSet(rs));
			setVolume((Long) Columns.VOLUME.fromResultSet(rs));
			setRemaining((Long) Columns.REMAINING.fromResultSet(rs));
			setWorkerPoolAddr((String) Columns.WORKERPOOLADDR.fromResultSet(rs));
			setWorkerPoolOwnerAddr((String) Columns.WORKERPOOLOWNERADDR.fromResultSet(rs));
			setDirty(false);
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
	public MarketOrderInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 */
	public MarketOrderInterface(final File f) throws IOException, SAXException {
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
	public MarketOrderInterface(final InputStream input) throws IOException, SAXException {
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
	public MarketOrderInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This is the default constructor
	 */
	public MarketOrderInterface(final UID uid) {
		this();
		setUID(uid);
	}

	/**
	 * This updates this object from interface.
	 */
	@Override
	public void updateInterface(final Table itf) throws IOException {
		final MarketOrderInterface marketOrderInterface = (MarketOrderInterface) itf;
		if (marketOrderInterface.getOwner() != null) {
			setOwner(marketOrderInterface.getOwner());
		}
		if (marketOrderInterface.getAccessRights() != null) {
			setAccessRights(marketOrderInterface.getAccessRights());
		}
		if (marketOrderInterface.getDirection() != null) {
			setDirection(marketOrderInterface.getDirection());
		}
        if (marketOrderInterface.getCategoryId() != null) {
            setCategoryId(marketOrderInterface.getCategoryId());
        }
        if (marketOrderInterface.getExpectedWorkers() != null) {
            setExpectedWorkers(marketOrderInterface.getExpectedWorkers());
        }
        if (marketOrderInterface.getNbWorkers() != null) {
            setNbWorkers(marketOrderInterface.getNbWorkers());
        }
		if (marketOrderInterface.getTrust() != null) {
			setTrust(marketOrderInterface.getTrust());
		}
		if (marketOrderInterface.getPrice() != null) {
			setPrice(marketOrderInterface.getPrice());
		}
		if (marketOrderInterface.getVolume() != null) {
			setVolume(marketOrderInterface.getVolume());
		}
		if (marketOrderInterface.getRemaining() != null) {
			setRemaining(marketOrderInterface.getRemaining());
		}
		if (marketOrderInterface.getWorkerPoolAddr() != null) {
			setWorkerPoolAddr(marketOrderInterface.getWorkerPoolAddr());
		}
		if (marketOrderInterface.getWorkerPoolOwnerAddr() != null) {
			setWorkerPoolOwnerAddr(marketOrderInterface.getWorkerPoolOwnerAddr());
		}

	}

	/**
	 * This retrieves market order direction
	 *
	 * @return the market order direction
	 */
	public MarketOrderDirectionEnum getDirection() {
		return (MarketOrderDirectionEnum) getValue(Columns.DIRECTION);
	}
    /**
     * This retrieves the category id
     *
     * @return this attribute, or null if not set
     * @exception IOException
     *                is thrown is attribute is nor well formed
     */
    public Long getCategoryId() throws IOException {
        try {
            return (Long) getValue(Columns.CATEGORYID);
        } catch (final NullPointerException e) {
            return null;
        }
    }
    /**
     * This retrieves the amount of needed worker to safely reach the trust
     *
     * @return this attribute, or null if not set
     */
    public Long getExpectedWorkers() {
        try {
            return (Long) getValue(Columns.EXPECTEDWORKERS);
        } catch (final Exception e) {
            return null;
        }
    }
    /**
     * This retrieves the amount of booked worker to reach the trust
     *
     * @return this attribute, or null if not set
     */
    public Long getNbWorkers()  {
        try {
            return (Long) getValue(Columns.NBWORKERS);
        } catch (final Exception e) {
            return null;
        }
    }
	/**
	 * This retrieves the trust value
	 *
	 * @return this attribute, or null if not set
	 */
	public Float getTrust()  {
		try {
			return (Float) getValue(Columns.TRUST);
		} catch (final Exception e) {
			return null;
		}
	}
	/**
	 * This retrieves the price value
	 * This is named "value" in smart contract
	 *
	 * @return this attribute, or null if not set
	 */
	public Long getPrice()  {
		try {
			return (Long) getValue(Columns.PRICE);
		} catch (final Exception e) {
			return null;
		}
	}
	/**
	 * This retrieves the volume
	 *
	 * @return this attribute, or null if not set
	 */
	public Long getVolume()  {
		try {
			return (Long) getValue(Columns.VOLUME);
		} catch (final Exception e) {
			return null;
		}
	}
	/**
	 * This retrieves the remaining
	 *
	 * @return this attribute, or null if not set
	 */
	public Long getRemaining()  {
		try {
			return (Long) getValue(Columns.REMAINING);
		} catch (final Exception e) {
			return null;
		}
	}
	/**
	 * This retrieves the worker pool address
	 *
	 * @return this attribute, or null if not set
	 */
	public String getWorkerPoolAddr() {
		try {
			return (String) getValue(Columns.WORKERPOOLADDR);
		} catch (final Exception e) {
			return null;
		}
	}
	/**
	 * This retrieves the worker pool owner address
	 *
	 * @return this attribute, or null if not set
	 */
	public String getWorkerPoolOwnerAddr() {
		try {
			return (String) getValue(Columns.WORKERPOOLOWNERADDR);
		} catch (final Exception e) {
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
	 * This sets market order direction
	 * @param d is the market order direction
     * @return true if value has changed, false otherwise
	 */
	public boolean setDirection(final MarketOrderDirectionEnum d) {
		return setValue(Columns.DIRECTION, d);
	}
    /**
     * This sets the category id
     * @param c is the category id
     * @return true if value has changed, false otherwise
     */
    public boolean setCategoryId(final Long c)  {
        return setValue(Columns.CATEGORYID, c);
    }
    /**
     * This sets the amount of needed workers to safely reach the trust
     * @param e is the amount of needed workers
     * @return true if value has changed, false otherwise
     */
    public boolean setExpectedWorkers(final Long e)  {
        return setValue(Columns.EXPECTEDWORKERS, e);
    }
    /**
     * This sets the amount of booked workers to reach the trust
     * @param n is the amount of booked workers
     * @return true if value has changed, false otherwise
     */
    public boolean setNbWorkers(final Long n)  {

        setValue(Columns.NBWORKERS, n);
        return setVolume(getExpectedWorkers() / getNbWorkers());
    }
    /**
     * This increments the amount of booked workers to reach the trust
     * @return true if value has changed, false otherwise
     */
    public boolean incNbWorkers()  {
        return setNbWorkers(getNbWorkers() + 1);
    }
    /**
     * This decrements the amount of booked workers to reach the trust
     * @return true if value has changed, false otherwise
     */
    public boolean decNbWorkers()  {
        return setNbWorkers(getNbWorkers() - 1);
    }
	/**
	 * This sets the trust value
	 * @param t is the trust
     * @return true if value has changed, false otherwise
	 */
	public boolean setTrust(final Float t)  {
		return setValue(Columns.TRUST, t);
	}
	/**
	 * This sets the price value
	 * @param p is the price
     * @return true if value has changed, false otherwise
	 */
	public boolean setPrice(final Long p)  {
		return setValue(Columns.PRICE, p);
	}
	/**
	 * This sets the volume
	 * @param v is the volume
     * @return true if value has changed, false otherwise
	 */
	public boolean setVolume(final Long v)  {
		return setValue(Columns.VOLUME, v);
	}
	/**
	 * This sets the remaining
	 * @param r is the remaining
     * @return true if value has changed, false otherwise
	 */
	public boolean setRemaining(final Long r)  {
		return setValue(Columns.REMAINING, r);
	}
	/**
	 * This sets the worker pool address
	 * @param addr is the worker pool address
     * @return true if value has changed, false otherwise
	 */
	public boolean setWorkerPoolAddr(final String addr)  {
		return setValue(Columns.WORKERPOOLADDR, addr);
	}
	/**
	 * This sets the worker pool owner address
	 * @param addr is the worker pool owner address
     * @return true if value has changed, false otherwise
	 */
	public boolean setWorkerPoolOwnerAddr(final String addr)  {
		return setValue(Columns.WORKERPOOLOWNERADDR, addr);
	}

	/**
	 * This is for testing only Without any argument, this dumps a
	 * MarketOrderInterface object. If the first argument is an XML file containing a
	 * description of a MarketOrderInterface, this creates a MarketOrderInterface from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.MarketOrderInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final MarketOrderInterface itf = new MarketOrderInterface();
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
			logger.exception("Usage : java -cp " + XWTools.JARFILENAME
					+ " xtremweb.common.MarketOrderInterface [anXMLDescriptionFile]", e);
		}
	}
}
