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
import java.util.Date;

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
			 */
			@Override
			public StatusEnum fromString(final String v) {
				return StatusEnum.valueOf(v.toUpperCase());
			}
		},
		/**
         * The index as returned by actuatorService.createMarketOrder
         */
        MARKETORDERIDX {
            @Override
            public Long fromString(final String v) {
                return Long.valueOf(v);
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
         * Trust level for this order.
         * This defines ExpectedWorkers as : trust / 10
         * @see #setTrust(long)
         */
		TRUST {
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
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
		},
		/**
		 * This is the column index of the arrival date
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
        /**
         * This is the column index of the contributing date
         */
        CONTRIBUTINGDATE {
            @Override
            public Date fromString(final String v) {
                return XWTools.getSQLDateTime(v);
            }
        },
		/**
		 * This is the column index of the revealing date
		 */
		REVEALINGDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		/**
		 * This is the column index of the completed date
		 */
		COMPLETEDDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
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
     * EXPECTEDWORKERS is forced to 10
	 */
	public MarketOrderInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);
		setArrivalDate();
		setAccessRights(XWAccessRights.USERALL);
        setVolume(1L);
        setNbWorkers(0);
        setRemaining(getVolume());
        setStatus(StatusEnum.WAITING);
        setTrust(0L);
        setMarketOrderIdx(0);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.MARKETORDERIDX.getOrdinal(), Columns.DIRECTION.getOrdinal(), Columns.STATUS.getOrdinal() });
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
            setMarketOrderIdx((Long) Columns.MARKETORDERIDX.fromResultSet(rs));
        } catch (final Exception e) {
        }
        try {
            setNbWorkers((Long) Columns.NBWORKERS.fromResultSet(rs));
        } catch (final Exception e) {
        }
        try {
            setVolume((Long) Columns.VOLUME.fromResultSet(rs));
        } catch (final Exception e) {
        }
        try {
            setRemaining((Long) Columns.REMAINING.fromResultSet(rs));
        } catch (final Exception e) {
        }
		try {
			setArrivalDate((Date) Columns.ARRIVALDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setCompletedDate((Date) Columns.COMPLETEDDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setRevealingDate((Date) Columns.REVEALINGDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
        try {
            setContributingDate((Date) Columns.CONTRIBUTINGDATE.fromResultSet(rs));
        } catch (final Exception e) {
        }
        try {
            setStartDate((Date) Columns.STARTDATE.fromResultSet(rs));
        } catch (final Exception e) {
        }
        try {
            setUID((UID) TableColumns.UID.fromResultSet(rs));
            setOwner((UID) TableColumns.OWNERUID.fromResultSet(rs));
            setAccessRights((XWAccessRights) TableColumns.ACCESSRIGHTS.fromResultSet(rs));
            setCategoryId((Long) Columns.CATEGORYID.fromResultSet(rs));
            setExpectedWorkers((Long) Columns.EXPECTEDWORKERS.fromResultSet(rs));
			setDirection((MarketOrderDirectionEnum) Columns.DIRECTION.fromResultSet(rs));
			setStatus((StatusEnum) Columns.STATUS.fromResultSet(rs));
            setPrice((Long) Columns.PRICE.fromResultSet(rs));
            setTrust((Long) Columns.TRUST.fromResultSet(rs));
            setWorkerPoolAddr((String) Columns.WORKERPOOLADDR.fromResultSet(rs));
            setWorkerPoolOwnerAddr((String) Columns.WORKERPOOLOWNERADDR.fromResultSet(rs));
            setDirty(false);
        } catch (final Exception e) {
            getLogger().exception(e);
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
        setOwner(marketOrderInterface.getOwner());
        setAccessRights(marketOrderInterface.getAccessRights());
        setDirection(marketOrderInterface.getDirection());
        setStatus(marketOrderInterface.getStatus());
        setCategoryId(marketOrderInterface.getCategoryId());
        setMarketOrderIdx(marketOrderInterface.getMarketOrderIdx());
		setArrivalDate(marketOrderInterface.getArrivalDate());
		setCompletedDate(marketOrderInterface.getCompletedDate());
		setRevealingDate(marketOrderInterface.getRevealingDate());
        setContributingDate(marketOrderInterface.getContributingDate());
        setStartDate(marketOrderInterface.getStartDate());
		setExpectedWorkers(marketOrderInterface.getExpectedWorkers());
 		setNbWorkers(marketOrderInterface.getNbWorkers());
		setTrust(marketOrderInterface.getTrust());
        setPrice(marketOrderInterface.getPrice());
        setVolume(marketOrderInterface.getVolume());
        setRemaining(marketOrderInterface.getRemaining());
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
	 * This retrieves market status
	 *
	 * @return the market status
	 */
	public StatusEnum getStatus() {
		return (StatusEnum) getValue(Columns.STATUS);
	}
    /**
     * This retrieves the market order index
     *
     * @return this attribute, or null if not set
     */
    public Long getMarketOrderIdx()  {
        try {
            return (Long) getValue(Columns.MARKETORDERIDX);
        } catch (final NullPointerException e) {
            return null;
        }
    }
    /**
     * This retrieves the amount of needed worker to safely reach the trust
     *
     * @return this attribute, or null if not set
     */
    public long getExpectedWorkers() {
        try {
            return ((Long) getValue(Columns.EXPECTEDWORKERS)).longValue();
        } catch (final Exception e) {
            return 0L;
        }
    }
    /**
     * This retrieves the amount of booked worker to reach the trust
     *
     * @return this attribute, or 0 if not set
     */
    public long getNbWorkers()  {
        try {
            return ((Long) getValue(Columns.NBWORKERS)).longValue();
        } catch (final Exception e) {
            return 0;
        }
    }
	/**
	 * This retrieves the trust value
	 *
	 * @return this attribute, or 0 if not set
	 */
	public long getTrust()  {
		try {
			return ((Long) getValue(Columns.TRUST)).longValue();
		} catch (final Exception e) {
			return 0;
		}
	}
	/**
	 * This retrieves the price value
	 * This is named "value" in smart contract
	 *
	 * @return this attribute, or 0 if not set
	 */
	public long getPrice()  {
		try {
			return ((Long) getValue(Columns.PRICE)).longValue();
		} catch (final Exception e) {
			return 0;
		}
	}
	/**
	 * This retrieves the volume
	 *
	 * @return this attribute, or 0 if not set
	 */
	public long getVolume()  {
		try {
			return ((Long) getValue(Columns.VOLUME)).longValue();
		} catch (final Exception e) {
			return 0;
		}
	}
	/**
	 * This retrieves the remaining
	 *
	 * @return this attribute, or 0 if not set
	 */
	public long getRemaining()  {
		try {
			return ((Long) getValue(Columns.REMAINING)).longValue();
		} catch (final Exception e) {
			return 0;
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
	 * This retrieves the submission date
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getArrivalDate() {
		return (Date) getValue(Columns.ARRIVALDATE);
	}
	/**
	 * This retrieves the completion date
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getCompletedDate() {
		return (Date) getValue(Columns.COMPLETEDDATE);
	}
    /**
     * This retrieves the contributing date
     *
     * @return this attribute, or null if not set
     */
    public Date getContributingDate() {
        return (Date) getValue(Columns.CONTRIBUTINGDATE);
    }
    /**
     * This retrieves the start date
     *
     * @return this attribute, or null if not set
     */
    public Date getStartDate() {
        return (Date) getValue(Columns.STARTDATE);
    }
	/**
	 * This retrieves the revealing date
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getRevealingDate() {
		return (Date) getValue(Columns.REVEALINGDATE);
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
	 * This tests if this can be closed (aka removed)
	 * @return true if status is WAITING, AVAILABLE, COMPLETED, ERROR; false otherwise
	 */
	public boolean canBeClosed() {
		final StatusEnum status = getStatus();
		switch (status) {
			case WAITING:
			case AVAILABLE:
			case COMPLETED:
			case ERROR:
				return true;
			default:
			return false;
		}
	}
    /**
     * This sets market status
     * @param s is the market order status
     * @return true if value has changed, false otherwise
     */
    public boolean setStatus(final StatusEnum s) {
        return setValue(Columns.STATUS, s);
    }
    /**
     * This sets this market order status to running
     * @return true if value has changed, false otherwise
     */
    public boolean setRunning(final HostInterface host) {
        if(host == null)
            return false;
        host.setRunning();
        setStartDate();
        return setValue(Columns.STATUS, StatusEnum.RUNNING);
    }
    /**
     * This sets this market order status to contributing
     * @return true if value has changed, false otherwise
     */
    public boolean setContributing() {
    	setContributingDate();
        return setValue(Columns.STATUS, StatusEnum.CONTRIBUTING);
    }
    /**
     * This sets this market order status to contributed
     * @return true if value has changed, false otherwise
     */
    public boolean setContributed() {
        return setValue(Columns.STATUS, StatusEnum.CONTRIBUTED);
    }
	/**
	 * This sets this market order status to revealing
	 * @return true if value has changed, false otherwise
	 */
	public boolean setRevealing() {
		setRevealingDate();
		return setValue(Columns.STATUS, StatusEnum.REVEALING);
	}
	/**
	 * This sets this market order status to finalizing
	 * @return true if value has changed, false otherwise
	 */
	public boolean setFinalizing() {
		return setValue(Columns.STATUS, StatusEnum.FINALIZING);
	}
    /**
     * This sets this market order status to completed
     * @return true if value has changed, false otherwise
     */
    public boolean setCompleted() {
    	setCompletedDate();
        return setValue(Columns.STATUS, StatusEnum.COMPLETED);
    }
    /**
     * This sets this market order status to pending  (it has been bought)
     * This decrements the volume by one
     * @return true if value has changed, false otherwise
     */
    public boolean setPending() {
        return setValue(Columns.STATUS, StatusEnum.PENDING);
    }
    /**
     * This sets this market order status to available (it has been registered on the BC)
     * This decrements the volume by one
     * @return true if value has changed, false otherwise
	 * @see #setMarketOrderIdx(long)
     */
    public boolean setAvailable() {
        return setValue(Columns.STATUS, StatusEnum.AVAILABLE);
    }
    /**
     * This sets this market order status to waiting (not enough workers)
     * @return true if value has changed, false otherwise
     */
    public boolean setWaiting() {
        return setValue(Columns.STATUS, StatusEnum.WAITING);
    }
    /**
     * This sets this market order status to error
     * @return true if value has changed, false otherwise
     */
    public boolean setError() {
        return setValue(Columns.STATUS, StatusEnum.ERROR);
    }
    /**
     * This sets the category id
     * @param c is the category id
     * @return true if value has changed, false otherwise
     */
    public boolean setCategoryId(final long c)  {
        return setValue(Columns.CATEGORYID, Long.valueOf(c));
    }
    /**
     * This sets the market order index and marks this as AVAILABLE
     * @param c is the category id
     * @return true if value has changed, false otherwise
     */
    public boolean setMarketOrderIdx(final long c)  {
        if (c != 0L)
            setAvailable();
        return setValue(Columns.MARKETORDERIDX, Long.valueOf(c));
    }
    /**
     * This sets the amount of needed workers to safely reach the trust
     * @param e is the amount of needed workers
     * @return true if value has changed, false otherwise
     */
    private boolean setExpectedWorkers(final long e)  {
        return setValue(Columns.EXPECTEDWORKERS, Long.valueOf(e < 0 ? 0 : e));
    }
	/**
	 * This increments the amount of needed workers to safely reach the trust.
	 * @return true
	 */
    public boolean incExpectedWorkers()  {
		Thread.currentThread().dumpStack();
        return addExpectedWorkers(1);
    }

    /**
     * This adds nbWorkers to the amount of needed workers to safely reach the trust.
     * @param nbWorkers is the amount of workers to add
     * @return true if value has changed, false otherwise
     */
    public boolean addExpectedWorkers(final long nbWorkers)  {
        if (nbWorkers < 0) {
            return false;
        }
        return setExpectedWorkers(getExpectedWorkers() + nbWorkers);
    }
    /**
     * This sets the amount of booked workers to reach the trust
     * @param n is the amount of booked workers
     * @return true if value has changed, false otherwise
     */
    public boolean setNbWorkers(final long n)  {
        return setValue(Columns.NBWORKERS, Long.valueOf(n));
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
     * This marks the provided host as participating in this market order
     * and increments the amount of booked workers to reach the trust
     * @param host is the participating host
     * @return true
     */
    public boolean addWorker(final HostInterface host)  {
        if(host == null)
            return false;
        host.setMarketOrderUid(getUID());
        if(getMarketOrderIdx() > 0) {
            // this is necessary if need to reschedule a lost work
            host.setPending();
        }
        incNbWorkers();
        return true;
    }
    /**
     * This removes the provided host from this market order
     * and decrements the amount of booked workers to reach the trust
     * @param host is the participating host
     * @return true
     */
    public boolean removeWorker(final HostInterface host)  {
        if(host == null) {
			return false;
		}
		final UID hostMoUid = host.getMarketOrderUid();
		if((hostMoUid == null) || (!hostMoUid.equals(this.getUID()))) {
			return false;
		}
		host.leaveMarketOrder();
        decNbWorkers();
        return true;
    }
    /**
     * This checks is this market order can be created on the blockchain
     * @return true if getExpectedWorkers() <= getNbWorkers() && getMarketOrderIdx() <= 0
     */
    public boolean canBeCreated()  {
        return getStatus() != StatusEnum.ERROR &&
				getExpectedWorkers() <= getNbWorkers()
                && getMarketOrderIdx() <= 0;
    }
	/**
	 * This sets the trust value and expectedWortkers as well
	 * @param t is the trust
     * @return true if value has changed, false otherwise
     * @see Columns#TRUST
	 */
	public boolean setTrust(final long t)  {
	    long trust = t < 10 ? 10 : t;
	    if (t > 100) trust = 100;
		long workers = trust / 10;

		setExpectedWorkers(workers);

        return setValue(Columns.TRUST, trust);
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
	 * This decrements the remaining by one
     * @return true if value has changed, false otherwise
	 */
	public boolean decRemaining()  {
		return setRemaining(getRemaining() - 1);
	}
	/**
	 * This sets the volume
	 * @param v is the volume
	 * @return true if value has changed, false otherwise
	 */
	public boolean setVolume(final Long v) {
//		return setValue(Columns.VOLUME, v < 0 ? 0 : v);
		return setValue(Columns.VOLUME, 1);
	}
	/**
	 * This sets the remaining
	 * @param r is the remaining
     * @return true if value has changed, false otherwise
	 */
	public boolean setRemaining(final Long r)  {
		return setValue(Columns.REMAINING, r < 0 ? 0 : r);
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
	 * This set the submission date
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setArrivalDate() {
		return setArrivalDate(new java.util.Date());
	}
	/**
	 * This set the submission date
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setArrivalDate(final Date v) {
		return setValue(Columns.ARRIVALDATE, v);
	}
	/**
	 * This set the completion date
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setCompletedDate() {
		return setCompletedDate(new java.util.Date());
	}
	/**
	 * This set the completion date
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setCompletedDate(final Date v) {
		return setValue(Columns.COMPLETEDDATE, v);
	}
    /**
     * This set the start date
     *
     * @return true if value has changed, false otherwise
     */
    public final boolean setStartDate() {
        return setStartDate(new java.util.Date());
    }
    /**
     * This set the start date
     *
     * @return true if value has changed, false otherwise
     */
    public final boolean setStartDate(final Date v) {
        return setValue(Columns.STARTDATE, v);
    }
    /**
     * This set the contributing date
     *
     * @return true if value has changed, false otherwise
     */
    public final boolean setContributingDate() {
        return setContributingDate(new java.util.Date());
    }
    /**
     * This set the contributing date
     *
     * @return true if value has changed, false otherwise
     */
    public final boolean setContributingDate(final Date v) {
        return setValue(Columns.CONTRIBUTINGDATE, v);
    }
	/**
	 * This set the revealing date
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setRevealingDate() {
		return setRevealingDate(new java.util.Date());
	}
	/**
	 * This set the revealing date
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setRevealingDate(final Date v) {
		return setValue(Columns.REVEALINGDATE, v);
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
