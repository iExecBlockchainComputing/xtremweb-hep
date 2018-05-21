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
 * This class describes a row of the hosts SQL table.
 * HostsInterface.java
 *
 * Created: Feb 19th, 2002
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */
public final class HostInterface extends Table {

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.Host
	 *
	 * @since 9.0.0
	 */
	public static final String TABLENAME = "hosts";

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "host";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns implements XWBaseColumn {

		/**
		 * This is the worker wallet addr
		 * @since 13.1.0
		 */
		ETHWALLETADDR {
			@Override
			public String fromString(final String v) {
				return v;
			}
		},
		/**
		 * This is the worker pool addr this host belongs to
		 * @since 13.1.0
		 */
		WORKERPOOLADDR {
			@Override
			public String fromString(final String v) {
				return v;
			}
		},
        /**
         * This is the market order for this worker
         * @since 13.1.0
         */
        MARKETORDERUID {
            @Override
            public UID fromString(final String v) {
                return new UID(v);
            }
        },
        /**
         * This is the column index of the contribution flag
         * which tells if this worker has already contributed to the current marketorder
         * @sine 13.1.0
         */
        HASCONTRIBUTED {
            @Override
            public Boolean fromString(final String v) {
                return Boolean.valueOf(v);
            }
        },
		/**
		 * This is a job URI; this is for SpeQuLoS (EDGI/JRA2). If this is set,
		 * the worker will receive this job in priority, if available, and
		 * according to the match making - CPU, OS... This is has a higher
		 * priority than batchid
		 *
		 * @since 7.2.0
		 */
		JOBID {
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is a job group URI; this is for SpeQuLoS (EDGI/JRA2). If this is
		 * set, the worker will receive a job from this group in priority, if
		 * any, and according to the match making - CPU, OS... This is has a
		 * lower priority than jobid
		 *
		 * @since 7.2.0
		 */
		BATCHID {
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the host name
		 */
		NAME,
		/**
		 * This is the column index of the percentage of CPU usable by the worker
		 *
		 * @since 8.0.0
		 */
		CPULOAD {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the connections amount
		 */
		NBCONNECTIONS {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the IP address as provided by worker
		 * itself This may be a NATed IP address
		 *
		 * @since 2.0.0
		 */
		NATEDIPADDR,
		/**
		 * This sets the IP address obtained at connexion time. This is set by
		 * server at connexion time This may be different from NATed IP
		 */
		IPADDR,
		/**
		 * This is the column index of the MAC address
		 */
		HWADDR,
		/**
		 * This is the column index of the time zone
		 */
		TIMEZONE,
		/**
		 * This is the column index of the average ping to server
		 *
		 * @since 2.0.0
		 */
		AVGPING {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the ping amount to server
		 *
		 * @since 2.0.0
		 */
		NBPING {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the upload bandwidth usage (in Mb/s)
		 *
		 * @since 2.0.0
		 */
		UPLOADBANDWIDTH {
			@Override
			public Float fromString(final String v) {
				return new Float(v);
			}
		},
		/**
		 * This is the column index of the download bandwidth usage (in Mb/s)
		 *
		 * @since 2.0.0
		 */
		DOWNLOADBANDWIDTH {
			@Override
			public Float fromString(final String v) {
				return new Float(v);
			}
		},
		/**
		 * This is the column index of the OS name
		 */
		OS {
			@Override
			public OSEnum fromString(final String v) {
				return OSEnum.valueOf(v.trim());
			}
		},
		/**
		 * OS version
		 *
		 * @since XWHEP 6.0.0
		 */
		OSVERSION,
		/**
		 * Java version
		 *
		 * @since XWHEP 6.0.0
		 */
		JAVAVERSION,
		/**
		 * Java data model (32 or 64 bits)
		 *
		 * @since XWHEP 6.0.0
		 */
		JAVADATAMODEL {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the pool work size
		 *
		 * @since 7.0.0
		 */
		POOLWORKSIZE {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the cpu type
		 */
		CPUTYPE {
			@Override
			public CPUEnum fromString(final String v) {
				return CPUEnum.valueOf(v.trim());
			}
		},
		/**
		 * This is the column index of the cpu amount
		 */
		CPUNB {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the cpu model
		 */
		CPUMODEL,
		/**
		 * This is the column index of the cpu speed. This is in Mhz
		 */
		CPUSPEED {
			@Override
			public Integer fromString(final String v) {
				return new Integer(v);
			}
		},
		/**
		 * This is the column index of the total memory. This is in Kb
		 */
		TOTALMEM {
			@Override
			public Long fromString(final String v) {
				return new Long(v);
			}
		},
		/**
		 * This is the column index of the total available memory, according to
		 * the resource owner policy. This is in Kb.
		 *
		 * @since 9.1.0
		 */
		AVAILABLEMEM {
			@Override
			public Integer fromString(final String v) {
				return new Integer(v);
			}
		},
		/**
		 * This is the column index of the total swap. This is in Mb
		 */
		TOTALSWAP {
			@Override
			public Long fromString(final String v) {
				return new Long(v);
			}
		},
		/**
		 * This is the column index of the tmp partition total space. This is in
		 * Mb
		 */
		TOTALTMP {
			@Override
			public Long fromString(final String v) {
				return new Long(v);
			}
		},
		/**
		 * This is the column index of the free space in tmp partition. This is
		 * in Mb
		 */
		FREETMP {
			@Override
			public Long fromString(final String v) {
				return new Long(v);
			}
		},
		/**
		 * This is the column index uf the project. This defines the project
		 * this worker wants to participate. This is a usergroup name. This
		 * override owner's usergroup, if any.
		 */
		PROJECT,
		/**
		 * This is the column index of the last alive date
		 */
		LASTALIVE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		/**
		 * This is the column index of the active flag<br />
		 * Any erroneus worker is set inactive
		 */
		ACTIVE {
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * This flag tells whether this host accepts to run jobs that listen for
		 * incoming connections
		 *
		 * @since 8.0.0
		 */
		INCOMINGCONNECTIONS {
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * This is the column index of the host availability flag accordingly to
		 * worker local policy<br />
		 * This is refreshed on <code>alive</code> signal
		 */
		AVAILABLE {
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * This is the column index of the host availability flag accordingly to
		 * worker local policy<br />
		 * This is refreshed on <code>alive</code> signal
		 */
		PILOTJOB {
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * This is the column index of the service grid identifier, if any
		 *
		 * @since 7.0.0
		 */
		SGID {
			@Override
			public String fromString(final String v) {
				return v.replaceAll("[\\n\\s\'\"]+", "_");
			}
		},
		/**
		 * This is the column index of the host timeout
		 */
		TIMEOUT {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the trace flag which says to collect
		 * traces or not
		 */
		TRACES {
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * This is the column index of the average execution time
		 */
		AVGEXECTIME {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the time shift
		 */
		TIMESHIFT {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This attribute tells whether worker accept application binary If
		 * false worker accepts only services
		 */
		ACCEPTBIN {
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * comma separated list of shared application types
		 *
		 * @see AppTypeEnum
		 * @since 8.0.0 (FG)
		 */
		SHAREDAPPS,
		/**
		 * comma separated list of shared data URIs
		 *
		 * @since 8.0.0 (FG)
		 */
		SHAREDDATAS,
		/**
		 * comma separated list of shared packages names
		 *
		 * @since 8.0.0 (FG)
		 */
		SHAREDPACKAGES,
		/**
		 * This is the worker software version
		 */
		VERSION,
		/**
		 * This is the column index of the pending job counter; this is updated
		 * on job submission
		 *
		 * @since 7.0.0
		 */
		PENDINGJOBS {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the running job counter this is updated
		 * on successfully worker request
		 *
		 * @since 7.0.0
		 */
		RUNNINGJOBS {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the error job counter; this is updated on
		 * job error
		 *
		 * @since 7.0.0
		 */
		ERRORJOBS {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the compued jobs amount
		 */
		NBJOBS {
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		};
		/**
		 * This is the index based on ordinal so that the first value is
		 * TableColumns + 1
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
	 * 5.8.0, ACCESSRIGHTS did not exist. Then this returns null for this
	 * column.
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @since 5.8.0
	 * @return null if((version == null) &amp;&amp; (i ==
	 *         ACCESSRIGHTS.ordinal())); column label otherwise
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
	public HostInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);

        setAcceptBin(false);
		setActive(true);
		setAvailable(true);
		setIncomingConnections(false);
		setPilotJob(false);
		setTracing(false);
		setAccessRights(XWAccessRights.DEFAULT);
		setOsVersion(XWPropertyDefs.OSVERSION.defaultValue());
		setJavaVersion(XWPropertyDefs.JAVAVERSION.defaultValue());
		setJavaDataModel(Integer.parseInt(XWPropertyDefs.JAVADATAMODEL.defaultValue()));
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.NAME.getOrdinal() });
		setContribution(false);
		setDirty(true);
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException is thrown on error
	 */
	public HostInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This fills columns from DB
	 *
	 * @since 9.0.0
	 * @param rs
	 *            is the SQL data set
	 * @exception IOException is thrown on error
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setUID((UID) TableColumns.UID.fromResultSet(rs));
			setOwner((UID) TableColumns.OWNERUID.fromResultSet(rs));
			setAccessRights((XWAccessRights) TableColumns.ACCESSRIGHTS.fromResultSet(rs));

			try {
				setNbConnections((Integer) Columns.NBCONNECTIONS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setEthWalletAddr((String) Columns.ETHWALLETADDR.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setWorkerPoolAddr((String) Columns.WORKERPOOLADDR.fromResultSet(rs));
			} catch (final Exception e) {
			}
            try {
                setMarketOrderUid((UID) Columns.MARKETORDERUID.fromResultSet(rs));
            } catch (final Exception e) {
            }
            try {
                setContribution((Boolean) Columns.HASCONTRIBUTED.fromResultSet(rs));
            } catch (final Exception e) {
            }
            try {
                setCpuLoad((Integer) Columns.CPULOAD.fromResultSet(rs));
            } catch (final Exception e) {
            }
			try {
				setNbPing((Integer) Columns.NBPING.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setAvgPing((Integer) Columns.AVGPING.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setPoolWorkSize((Integer) Columns.POOLWORKSIZE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setCpuNb((Integer) Columns.CPUNB.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setCpuSpeed((Integer) Columns.CPUSPEED.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTotalMem((Long) Columns.TOTALMEM.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setAvailableMem((Integer) Columns.AVAILABLEMEM.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTotalSwap((Long) Columns.TOTALSWAP.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTotalTmp((Long) Columns.TOTALTMP.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setFreeTmp((Long) Columns.FREETMP.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setAvgExecTime((Integer) Columns.AVGEXECTIME.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTimeShift((Integer) Columns.TIMESHIFT.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setNbJobs((Integer) Columns.NBJOBS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setPendingJobs((Integer) Columns.PENDINGJOBS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setRunningJobs((Integer) Columns.RUNNINGJOBS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setErrorJobs((Integer) Columns.ERRORJOBS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTimeOut((Integer) Columns.TIMEOUT.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setVersion((String) Columns.VERSION.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setJobId((URI) Columns.JOBID.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setBatchId((URI) Columns.BATCHID.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setName((String) Columns.NAME.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setIPAddr((String) Columns.IPADDR.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setNatedIPAddr((String) Columns.NATEDIPADDR.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setHWAddr((String) Columns.HWADDR.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTimeZone((String) Columns.TIMEZONE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setOs((OSEnum) Columns.OS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setOsVersion((String) Columns.OSVERSION.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setJavaVersion((String) Columns.JAVAVERSION.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setSharedApps((String) Columns.SHAREDAPPS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setSharedPackages((String) Columns.SHAREDPACKAGES.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setSharedDatas((String) Columns.SHAREDDATAS.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setJavaDataModel((Integer) Columns.JAVADATAMODEL.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setCpu((CPUEnum) Columns.CPUTYPE.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setCpuModel((String) Columns.CPUMODEL.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setProject((String) Columns.PROJECT.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setUploadBandwidth((Float) Columns.UPLOADBANDWIDTH.fromResultSet(rs));
			} catch (final Exception e) {
				setUploadBandwidth(0);
			}
			try {
				setDownloadBandwidth((Float) Columns.DOWNLOADBANDWIDTH.fromResultSet(rs));
			} catch (final Exception e) {
				setDownloadBandwidth(0);
			}
			try {
				setAcceptBin((Boolean) Columns.ACCEPTBIN.fromResultSet(rs));
			} catch (final Exception e) {
				setAcceptBin(false);
			}
			try {
				setActive((Boolean) Columns.ACTIVE.fromResultSet(rs));
			} catch (final Exception e) {
				setActive(false);
			}
			try {
				setAvailable((Boolean) Columns.AVAILABLE.fromResultSet(rs));
			} catch (final Exception e) {
				setAvailable(false);
			}
			try {
				setIncomingConnections((Boolean) Columns.INCOMINGCONNECTIONS.fromResultSet(rs));
			} catch (final Exception e) {
				setIncomingConnections(false);
			}
			try {
				setPilotJob((Boolean) Columns.PILOTJOB.fromResultSet(rs));
			} catch (final Exception e) {
				setPilotJob(false);
			}
			try {
				setSgId((String) Columns.SGID.fromResultSet(rs));
			} catch (final Exception e) {
			}
			try {
				setTracing((Boolean) Columns.TRACES.fromResultSet(rs));
			} catch (final Exception e) {
				setTracing(false);
			}

			try {
				setLastAlive((Date) Columns.LASTALIVE.fromResultSet(rs));
			} catch (final Exception e) {
			}
		} catch (final Exception e) {
			getLogger().exception(e);
			throw new IOException(e.toString());
		}
		setDirty(false);
	}

	/**
	 * This constructs a new object from XML attributes
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @see Table#fromXml(Attributes)
	 */
	public HostInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This creates a new object that will be retrieved with the given SQL
	 * request
	 *
	 * @param r
	 *            is the SQL request to retrieve host
	 * @since 9.0.0
	 */
	public HostInterface(final SQLRequest r) {
		this();
		setRequest(r);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public HostInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #HostInterface(InputStream)
	 */
	public HostInterface(final File f) throws IOException, SAXException {
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
	public HostInterface(final InputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This updates this object from interface.
	 */
	@Override
	public void updateInterface(final Table hitf) throws IOException {

		final HostInterface itf = (HostInterface) hitf;

		setProject(getProject());
		setAccessRights(itf.getAccessRights());
		setOwner(itf.getOwner());
		setEthWalletAddr(itf.getEthWalletAddr());
		setWorkerPoolAddr(itf.getWorkerPoolAddr());
        setMarketOrderUid(itf.getMarketOrderUid());
        setContribution(itf.hasContributed());
		setAvgExecTime(itf.getAvgExecTime());
		setNbJobs(itf.getNbJobs());
		setPendingJobs(itf.getPendingJobs());
		setRunningJobs(itf.getRunningJobs());
		setErrorJobs(itf.getErrorJobs());
		setTimeOut(itf.getTimeOut());
		setCpuLoad(itf.getCpuLoad());
		setNbConnections(itf.getNbConnections());
		setNbPing(itf.getNbPing());
		setAvgPing(itf.getAvgPing());
		setPoolWorkSize(itf.getPoolWorkSize());
		setCpuNb(itf.getCpuNb());
		setCpuSpeed(itf.getCpuSpeed());
		setTotalMem(itf.getTotalMem());
		setAvailableMem(itf.getAvailableMem());
		setTotalSwap(itf.getTotalSwap());
		setTotalTmp(itf.getTotalTmp());
		setFreeTmp(itf.getFreeTmp());
		setName(itf.getName());
		setIPAddr(itf.getIPAddr());
		setNatedIPAddr(itf.getNatedIPAddr());
		setHWAddr(itf.getHWAddr());
		setTimeZone(itf.getTimeZone());
		setOs(itf.getOs());
		setOsVersion(itf.getOsVersion());
		setJavaVersion(itf.getJavaVersion());
		setJavaDataModel(itf.getJavaDataModel());
		setCpu(itf.getCpu());
		setVersion(itf.getVersion());
		setSharedApps(itf.getSharedApps());
		setSharedDatas(itf.getSharedDatas());
		setSharedPackages(itf.getSharedPackages());
		setCpuModel(itf.getCpuModel());
		setLastAlive(itf.getLastAlive());
		setAvailable(itf.isAvailable());
		setIncomingConnections(itf.incomingConnections());
		setPilotJob(itf.isPilotJob());
		setTracing(itf.isTracing());
		setSgId(itf.getSgId());
		setPoolWorkSize(itf.getPoolWorkSize());
		setJobId(itf.getJobId());
		setBatchId(itf.getBatchId());
		setUploadBandwidth(itf.getUploadBandwidth());
		setDownloadBandwidth(itf.getDownloadBandwidth());
	}

	/**
	 * This retrieves the job URI this worker expects, if any
	 *
	 * @return the value of the attribute, or null if not set
	 * @since 7.2.0
	 */
	public URI getJobId() {
		return (URI) getValue(Columns.JOBID);
	}

	/**
	 * This retrieves the group job URI this worker expects, if any
	 *
	 * @return the value of the attribute, or null if not set
	 * @since 7.2.0
	 */
	public URI getBatchId() {
		return (URI) getValue(Columns.BATCHID);
	}

	/**
	 * This gets an attribute
	 *
	 * @return the value of the attribute, or null if not set
	 */
	public Date getLastAlive() {
		return (Date) getValue(Columns.LASTALIVE);
	}

	/**
	 * This retrieves the upload bandwidth usage
	 *
	 * @return the value of the attribute
	 * @since 2.0.0
	 */
	public float getUploadBandwidth() {
		try {
			return ((Float) getValue(Columns.UPLOADBANDWIDTH)).floatValue();
		} catch (final Exception e) {
		}
		setUploadBandwidth(0);
		return 0;
	}
	/**
	 * This retrieves the worker eth wallet addr
	 *
	 * @return this attribute, or null if not set
	 * @since 13.1.0
	 */
	public String getEthWalletAddr() {
		try {
			return (String) getValue(Columns.ETHWALLETADDR);
		} catch (final Exception e) {
			return null;
		}
	}
	/**
	 * This retrieves the worker pool addr this host belongs to
	 *
	 * @return this attribute, or null if not set
	 * @since 13.1.0
	 */
	public String getWorkerPoolAddr() {
		try {
			return (String) getValue(Columns.WORKERPOOLADDR);
		} catch (final Exception e) {
			return null;
		}
	}
    /**
     * This checks if this worker has already contributed to the current market order
     *
     * @return this attribute, or true if not set
     * @since 13.1.0
     */
    public boolean hasContributed() {
        try {
            return ((Boolean) getValue(Columns.HASCONTRIBUTED)).booleanValue();
        } catch (final Exception e) {
            return true;
        }
    }
    /**
     * This retrieves the market order for this worker
     *
     * @return this attribute, or null if not set
     * @since 13.1.0
     */
    public UID getMarketOrderUid() {
        try {
            return (UID) getValue(Columns.MARKETORDERUID);
        } catch (final Exception e) {
            return null;
        }
    }
    /**
     * This checks if this worker can contribute to a market order
     *
     * @return (getMarketOrderUid() == null) && (getEthWalletAddr() != null) && (getWorkerPoolAddr() != null)
     * @since 13.1.0
     */
    public boolean canContribute() {
        try {
            return (!hasContributed() &&
					(getMarketOrderUid() == null) &&
					(getEthWalletAddr() != null)
					&& (getWorkerPoolAddr() != null));
        } catch (final Exception e) {
            return false;
        }
    }
	/**
	 * This retrieves the download bandwidth usage
	 *
	 * @return the value of the attribute
	 * @since 2.0.0
	 */
	public float getDownloadBandwidth() {
		try {
			return ((Float) getValue(Columns.DOWNLOADBANDWIDTH)).floatValue();
		} catch (final Exception e) {
		}
		setDownloadBandwidth(0);
		return 0;
	}

	/**
	 * This tests whether this host is eligible for computing. A host is
	 * deactivated if it has sent a job as ERROR. This can also be (de)activated
	 * on client request. If this attribute is not set it is forced to true.
	 *
	 * @return true if active, false otherwise
	 */
	public boolean isActive() {
		try {
			return ((Boolean) getValue(Columns.ACTIVE)).booleanValue();
		} catch (final Exception e) {
		}
		setActive(true);
		return true;
	}

	/**
	 * This tests whether this host accept binary application This attribute is
	 * forced to false if not set.
	 *
	 * @return true if binary accepted , false otherwise
	 */
	public boolean acceptBin() {
		try {
			return ((Boolean) getValue(Columns.ACCEPTBIN)).booleanValue();
		} catch (final Exception e) {
		}
		setAcceptBin(false);
		return false;
	}

	/**
	 * This retrieves this host local policy. This is set on alive signal. If
	 * this attribute is not set it is forced to false
	 *
	 * @return true if available, false otherwise
	 */
	public boolean isAvailable() {
		try {
			return ((Boolean) getValue(Columns.AVAILABLE)).booleanValue();
		} catch (final Exception e) {
		}
		setAvailable(false);
		return false;
	}

	/**
	 * This tells whether this host accepts to run jobs that listen for incoming
	 * connections. If this attribute is not set it is forced to false
	 *
	 * @return true if this host accept incoming connections, false otherwise
	 * @since 8.0.0
	 */
	public boolean incomingConnections() {
		try {
			return ((Boolean) getValue(Columns.INCOMINGCONNECTIONS)).booleanValue();
		} catch (final Exception e) {
		}
		setIncomingConnections(false);
		return false;
	}

	/**
	 * This test if this host is a pilot job (run on a SG resource). If this
	 * attribute is not set it is forced to false
	 *
	 * @return true if this host is pilot job, false otherwise
	 */
	public boolean isPilotJob() {
		try {
			return ((Boolean) getValue(Columns.PILOTJOB)).booleanValue();
		} catch (final Exception e) {
		}
		setPilotJob(false);
		return false;
	}

	/**
	 * This tests if this host collects traces. If this attribute is not set it
	 * is forced to false
	 *
	 * @return true if collecting traces, false otherwise
	 */
	public boolean isTracing() {
		try {
			return ((Boolean) getValue(Columns.TRACES)).booleanValue();
		} catch (final Exception e) {
		}
		setTracing(false);
		return false;
	}

	/**
	 * This retrieves the amount of computed jobs. If this attribute is not set
	 * it is forced to 0.
	 *
	 * @return the value of this attribute
	 */
	public int getNbJobs() {
		try {
			return ((Integer) getValue(Columns.NBJOBS)).intValue();
		} catch (final Exception e) {
		}
		setNbJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of pending jobs for this user. If this attribute is
	 * not set it is forced to 0.
	 *
	 * @return the value of this attribute
	 * @since 7.0.0
	 */
	public int getPendingJobs() {
		try {
			return ((Integer) getValue(Columns.PENDINGJOBS)).intValue();
		} catch (final Exception e) {
		}
		setPendingJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of running jobs on this host. If this attribute is
	 * not set it is forced to 0.
	 *
	 * @return the value of this attribute
	 * @since 7.0.0
	 */
	public int getRunningJobs() {
		try {
			return ((Integer) getValue(Columns.RUNNINGJOBS)).intValue();
		} catch (final Exception e) {
		}
		setRunningJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of error jobs for this host. If this attribute is
	 * not set it is forced to 0.
	 *
	 * @return the value of this attribute
	 * @since 7.0.0
	 */
	public int getErrorJobs() {
		try {
			return ((Integer) getValue(Columns.ERRORJOBS)).intValue();
		} catch (final Exception e) {
		}
		setErrorJobs(0);
		return 0;
	}

	/**
	 * This retrieves the timeout. If this attribute is not set it is forced to
	 * its default value.
	 *
	 * @return the value of this attribute
	 * @see XWPropertyDefs#TIMEOUT
	 */
	public int getTimeOut() {
		try {
			return ((Integer) getValue(Columns.TIMEOUT)).intValue();
		} catch (final Exception e) {
		}
		setTimeOut(Integer.parseInt(XWPropertyDefs.TIMEOUT.defaultValue()));
		return Integer.parseInt(XWPropertyDefs.TIMEOUT.defaultValue());
	}

	/**
	 * This tells whether the worker tries to pool as fast as possible
	 *
	 * @return true is timeout is 0, false otherwise
	 */
	public boolean isRealTime() {
		return (getTimeOut() == 0);
	}

	/**
	 * This retrieves the time shift. If this attribute is not set it is forced
	 * to 0.
	 *
	 * @return the value of this attribute
	 */
	public int getTimeShift() {
		try {
			return ((Integer) getValue(Columns.TIMESHIFT)).intValue();
		} catch (final Exception e) {
		}
		setTimeShift(0);
		return 0;
	}

	/**
	 * This get the average execution time. If this attribute is not set it is
	 * forced to 0.
	 *
	 * @return the value of this attribute
	 */
	public int getAvgExecTime() {
		try {
			return ((Integer) getValue(Columns.AVGEXECTIME)).intValue();
		} catch (final Exception e) {
		}
		setAvgExecTime(0);
		return 0;
	}

	/**
	 * This retrieves the percentage of CPU usable by the worker If this
	 * attribute is not set it is forced to its default value.
	 *
	 * @return the value of this attribute
	 * @see XWPropertyDefs#CPULOAD
	 * @since 8.0.0
	 */
	public int getCpuLoad() {
		try {
			return ((Integer) getValue(Columns.CPULOAD)).intValue();
		} catch (final Exception e) {
		}
		final int def = Integer.parseInt(XWPropertyDefs.CPULOAD.defaultValue());
		setCpuLoad(def);
		return def;
	}

	/**
	 * This retrieves the amount of connections for this host. If this attribute
	 * is not set it is reseted to 0.
	 *
	 * @return the value of this attribute
	 */
	public int getNbConnections() {
		try {
			return ((Integer) getValue(Columns.NBCONNECTIONS)).intValue();
		} catch (final Exception e) {
		}
		setNbConnections(0);
		return 0;
	}

	/**
	 * This retrieves the amount of ping. If this attribute is not set it is
	 * reseted to 0.
	 *
	 * @return the value of this attribute
	 * @since 2.0.0
	 */
	public int getNbPing() {
		try {
			return ((Integer) getValue(Columns.NBPING)).intValue();
		} catch (final Exception e) {
		}
		setNbPing(0);
		return 0;
	}

	/**
	 * This retrieves the ping average. If this attribute is not set it is
	 * reseted to 0.
	 *
	 * @return the value of this attribute
	 * @since 2.0.0
	 */
	public int getAvgPing() {
		try {
			return ((Integer) getValue(Columns.AVGPING)).intValue();
		} catch (final Exception e) {
		}
		setAvgPing(0);
		return 0;
	}

	/**
	 * This gets the amount of simultaneous jobs. If this attribute is not set
	 * it is reseted to 1
	 *
	 * @return the value of this attribute
	 * @since 7.0.0
	 */
	public int getPoolWorkSize() {
		try {
			return ((Integer) getValue(Columns.POOLWORKSIZE)).intValue();
		} catch (final Exception e) {
		}
		setPoolWorkSize(1);
		return 1;
	}

	/**
	 * This retrieves the amount of cores (not only CPU, but cores) If this
	 * attribute is not set it is reseted to 1
	 *
	 * @return the value of this attribute
	 */
	public int getCpuNb() {
		try {
			return ((Integer) getValue(Columns.CPUNB)).intValue();
		} catch (final Exception e) {
		}
		setCpuNb(1);
		return 1;
	}

	/**
	 * This retrieves CPU speed in MHz If this attribute is not set it is
	 * reseted to 0 This is in Mhz
	 *
	 * @return the value of this attribute
	 */
	public int getCpuSpeed() {
		try {
			return ((Integer) getValue(Columns.CPUSPEED)).intValue();
		} catch (final Exception e) {
		}
		setCpuSpeed(0);
		return 0;
	}

	/**
	 * This retrieves the total amount of RAM. If this attribute is not set it
	 * is reseted to 0. This is in Kb.
	 *
	 * @return the total amount of RAM in Kb
	 */
	public long getTotalMem() {
		try {
			return ((Long) getValue(Columns.TOTALMEM)).longValue();
		} catch (final Exception e) {
		}
		setTotalMem(0L);
		return 0L;
	}

	/**
	 * This retrieves the total amount of available RAM, according to the
	 * resource owner policy. If this attribute is not set it is reseted to 0.
	 * This is in Kb.
	 *
	 * @since 9.1.0
	 * @return the total amount of RAM in Kb
	 */
	public int getAvailableMem() {
		try {
			return ((Integer) getValue(Columns.AVAILABLEMEM)).intValue();
		} catch (final Exception e) {
		}
		setAvailableMem(0);
		return 0;
	}

	/**
	 * This retrieves the amount of swap space. If this attribute is not set it
	 * is reseted to 0. This is in Mb
	 *
	 * @return the total amount of swap in Mb
	 */
	public long getTotalSwap() {
		try {
			return ((Long) getValue(Columns.TOTALSWAP)).longValue();
		} catch (final Exception e) {
		}
		setTotalSwap(0L);
		return 0L;
	}

	/**
	 * This gets the total space in temporary disk. If this attribute is not set
	 * it is reseted to 0. This is in Mb
	 *
	 * @return the total disk space in Mb
	 * @since 7.0.0
	 */
	public long getTotalTmp() {
		try {
			return ((Long) getValue(Columns.TOTALTMP)).longValue();
		} catch (final Exception e) {
		}
		setTotalTmp(0L);
		return 0l;
	}

	/**
	 * This retrieves the free space in temporary disk. If this attribute is not
	 * set it is reseted to 0. This is in Mb
	 *
	 * @return the total amount of free disk space in Mb
	 * @since 7.0.0
	 */
	public long getFreeTmp() {
		try {
			return ((Long) getValue(Columns.FREETMP)).longValue();
		} catch (final Exception e) {
		}
		setFreeTmp(0L);
		return 0L;
	}

	/**
	 * This retrieves the host name
	 *
	 * @return this attribute, or null if not set
	 */
	public String getName() {
		try {
			return (String) getValue(Columns.NAME);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the IP address as provided by worker itself. This may be a
	 * NATed IP
	 *
	 * @return this attribute, or null if not set
	 * @since 2.0.0
	 */
	public String getNatedIPAddr() {
		try {
			return (String) getValue(Columns.NATEDIPADDR);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the IP address obtained at connection time. This is set by
	 * server at connection time. This is the public IP address. This may be
	 * different from NATed IP.
	 *
	 * @return the public IP address, or null if not set
	 * @since 2.0.0
	 */
	public String getIPAddr() {
		try {
			return (String) getValue(Columns.IPADDR);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the MAC addresss
	 *
	 * @return this attribute, or null if not set
	 */
	public String getHWAddr() {
		try {
			return (String) getValue(Columns.HWADDR);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the time zone
	 *
	 * @return this attribute, or null if not set
	 */
	public String getTimeZone() {
		try {
			return (String) getValue(Columns.TIMEZONE);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the operating system
	 *
	 * @return this attribute, or null if not set
	 */
	public OSEnum getOs() {
		return (OSEnum) getValue(Columns.OS);
	}

	/**
	 * This retrieves the operating system version
	 *
	 * @return this attribute, or null if not set
	 * @since 6.0.0
	 */
	public String getOsVersion() {
		return (String) getValue(Columns.OSVERSION);
	}

	/**
	 * This retrieves the java version
	 *
	 * @return this attribute, or null if not set
	 * @since 6.0.0
	 */
	public String getJavaVersion() {
		return (String) getValue(Columns.JAVAVERSION);
	}

	/**
	 * This retrieves the java data model (32 or 64 bits)
	 *
	 * @return this attribute, or null if not set
	 * @since 6.0.0
	 */
	public int getJavaDataModel() {
		return ((Integer) getValue(Columns.JAVADATAMODEL)).intValue();
	}

	/**
	 * This retrieves the CPU
	 *
	 * @return this attribute, or null if not set
	 */
	public CPUEnum getCpu() {
		return (CPUEnum) getValue(Columns.CPUTYPE);
	}

	/**
	 * This retrieves the CPU model
	 *
	 * @return this attribute, or null if not set
	 */
	public String getCpuModel() {
		try {
			return (String) getValue(Columns.CPUMODEL);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the XWHEP middleware version
	 *
	 * @return this attribute, or null if not set
	 */
	public String getVersion() {
		try {
			return (String) getValue(Columns.VERSION);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves shared applications
	 *
	 * @return this attribute, or null if not set
	 * @since 8.0.0
	 */
	public String getSharedApps() {
		try {
			return (String) getValue(Columns.SHAREDAPPS);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves shared data
	 *
	 * @return this attribute, or null if not set
	 * @since 8.0.0
	 */
	public String getSharedDatas() {
		try {
			return (String) getValue(Columns.SHAREDDATAS);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves shared packages
	 *
	 * @return this attribute, or null if not set
	 * @since 8.0.0
	 */
	public String getSharedPackages() {
		try {
			return (String) getValue(Columns.SHAREDPACKAGES);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves project, if any. Project being the group of users this
	 * host works exclusively for.
	 *
	 * @return this attribute, or null if not set
	 */
	public String getProject() {
		try {
			return (String) getValue(Columns.PROJECT);
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
		try {
			return setValue(TableColumns.valueOf(uppercaseAttr), v);
		} catch (final Exception e) {
			return setValue(Columns.valueOf(uppercaseAttr), v);
		}
	}

	/**
	 * This sets the URI of the job id
	 *
	 * @return true if value has changed, false otherwise
	 * @since 7.2.0
	 */
	public boolean setJobId(final URI v) {
		return setValue(Columns.JOBID, v);
	}

	/**
	 * This sets the URI of the group job id
	 *
	 * @return true if value has changed, false otherwise
	 * @since 7.2.0
	 */
	public boolean setBatchId(final URI v) {
		return setValue(Columns.BATCHID, v);
	}

	/**
	 * This sets the percentage of CPU usable by the worker
	 *
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0
	 */
	public boolean setCpuLoad(final int v) {
		return setValue(Columns.CPULOAD, Integer.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setNbConnections(final int v) {
		return setValue(Columns.NBCONNECTIONS, Integer.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 2.0.0
	 */
	public boolean setNbPing(final int v) {
		return setValue(Columns.NBPING, Integer.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 2.0.0
	 */
	public boolean setAvgPing(final int v) {
		return setValue(Columns.AVGPING, Integer.valueOf(v));
	}

	/**
	 * This increments NbPing and recalculates avg ping
	 *
	 * @param v
	 *            is the last ping
	 * @since 2.0.0
	 */
	public void incAvgPing(final int v) {

		int nbpings = getNbPing();
		int avg = getAvgPing();

		if (v < 0) {
			return;
		}
		if (avg < 0) {
			avg = 0;
		}
		if (nbpings < 0) {
			nbpings = 0;
		}

		final float total = (avg * nbpings) + v;

		setAvgPing((int) (total / (nbpings + 1)));

		setNbPing(nbpings + 1);
	}

	/**
	 * This calculates the upload bandwidth usage in Mb/s, providing the
	 * transfert delay
	 *
	 * @param transfert
	 *            is the delay needed to upload data content
	 * @param size
	 *            is the transfert size in bytes
	 * @since 2.0.0
	 */
	public void setUploadBandwidth(final long transfert, final long size) throws IOException {
		final long s = size / (1024 * 1024);
		final float b = s / transfert;
		setUploadBandwidth(b);
	}

	/**
	 * This sets this data upload bandwidth usage (in Mb/s)
	 *
	 * @return true is value has changed
	 * @since 2.0.0
	 */
	public boolean setUploadBandwidth(final float v) {
		return setValue(Columns.UPLOADBANDWIDTH, Float.valueOf(v));
	}

	/**
	 * This calculates the download bandwidth usage in Mb/s, providing the
	 * transfer delay
	 *
	 * @param transfert
	 *            is the delay needed to download data content
	 * @param size
	 *            is the transfer size in bytes
	 * @since 2.0.0
	 */
	public void setDownloadBandwidth(final long transfert, final long size) throws IOException {
		final long s = size / (1024 * 1024);
		final float b = s / transfert;
		setDownloadBandwidth(b);
	}

	/**
	 * This sets this data download bandwidth usage (in Mb/s)
	 *
	 * @return true is value has changed
	 * @since 2.0.0
	 */
	public boolean setDownloadBandwidth(final float v) {
		return setValue(Columns.DOWNLOADBANDWIDTH, Float.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setNbJobs(final int v) {
		return setValue(Columns.NBJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This sets amount of pending jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setPendingJobs(final int v) {
		return setValue(Columns.PENDINGJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This sets amout of running jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setRunningJobs(final int v) {
		return setValue(Columns.RUNNINGJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This sets amount of erroneus jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setErrorJobs(final int v) {
		return setValue(Columns.ERRORJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This increments the amount of executed jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incNbJobs() {
		setNbJobs(getNbJobs() + 1);
	}

	/**
	 * This increments the amount of pending jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incPendingJobs() {
		setPendingJobs(getPendingJobs() + 1);
	}

	/**
	 * This increments the amount of running jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incRunningJobs() {
		setRunningJobs(getRunningJobs() + 1);
	}

	/**
	 * This increments the amount of erroneus jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incErrorJobs() {
		setErrorJobs(getErrorJobs() + 1);
	}

	/**
	 * This decrements the amount of pending jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void decPendingJobs() {
		setPendingJobs(getPendingJobs() - 1);
	}

	/**
	 * This decrements the amount of running jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void decRunningJobs() {
		setRunningJobs(getRunningJobs() - 1);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setTimeOut(final int v) {
		final Integer i = Integer.valueOf(v);
		final boolean ret = setValue(Columns.TIMEOUT, i);
		return ret;
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setTimeShift(final int v) {
		final Integer i = Integer.valueOf(v);
		final boolean ret = setValue(Columns.TIMESHIFT, i);
		return ret;
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setAvgExecTime(final int v) {
		Integer i = null;
		if (v < 0) {
			i = new Integer(0);
		} else {
			i = Integer.valueOf(v);
		}
		final boolean ret = setValue(Columns.AVGEXECTIME, i);
		i = null;
		return ret;
	}

	/**
	 * This increments NbJobs and recalculates avg exec time
	 *
	 * @param v
	 *            is the last execution time
	 */
	public void incAvgExecTime(final int v) {
		int nbJobs = getNbJobs();
		int avg = getAvgExecTime();

		int w = v;
		if (w < 0) {
			w = 0;
		}
		if (avg < 0) {
			avg = 0;
		}
		if (nbJobs < 0) {
			nbJobs = 0;
		}

		final long total = (avg * nbJobs) + w;
		setAvgExecTime((int) (total / (nbJobs + 1)));
		incNbJobs();
	}

	/**
	 * This set the amount of simultaneous jobs
	 *
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setPoolWorkSize(final int v) {
		return setValue(Columns.POOLWORKSIZE, Integer.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setCpuNb(final int v) {
		return setValue(Columns.CPUNB, Integer.valueOf(v));
	}

	/**
	 * This sets this host CPU speed
	 *
	 * @param v
	 *            is the CPU speed in MHz
	 * @return true if value has changed, false otherwise
	 */
	public boolean setCpuSpeed(final int v) {
		return setValue(Columns.CPUSPEED, Integer.valueOf(v));
	}

	/**
	 * This sets the amount of RAM
	 *
	 * @param v
	 *            is the total amount of RAM in Kb
	 * @return true if value has changed, false otherwise
	 */
	public boolean setTotalMem(final long v) {
		return setValue(Columns.TOTALMEM, Long.valueOf(v));
	}

	/**
	 * This sets the minimal amount of available RAM this host give to a job, in
	 * Kb.
	 *
	 * @param v
	 *            is the minimal amount of RAM this work needs in Kb.
	 * @return true if value has changed, false otherwise
	 * @since 9.1.0
	 */
	public final boolean setAvailableMem(final long v) {
		try {
			return setValue(Columns.AVAILABLEMEM, Long.valueOf(v < 0L ? 0L : v));
		} catch (final Exception e) {
			return setValue(Columns.AVAILABLEMEM, Long.valueOf(v));
		}
	}

	/**
	 * @param v
	 *            is in Mb
	 * @return true if value has changed, false otherwise
	 */
	public boolean setTotalSwap(final long v) {
		return setValue(Columns.TOTALSWAP, Long.valueOf(v));
	}

	/**
	 * This sets total space in tmp disk partition
	 *
	 * @param v
	 *            is in Mb
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setTotalTmp(final long v) {
		return setValue(Columns.TOTALTMP, Long.valueOf(v));
	}

	/**
	 * This sets free space in tmp disk partition
	 *
	 * @param v
	 *            is in Mb
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setFreeTmp(final long v) {
		return setValue(Columns.FREETMP, Long.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setName(final String v) {
		return setValue(Columns.NAME, v == null ? null : v);
	}

	/**
	 * This retrieves the IP address as provided by worker itself. This may be a
	 * NATed IP
	 *
	 * @return true if value has changed, false otherwise
	 * @since 2.0.0
	 */
	public boolean setNatedIPAddr(final String v) {
		return setValue(Columns.NATEDIPADDR, v == null ? null : v);
	}

	/**
	 * This retrieves the IP address obtained at connexion time. This is set by
	 * server at connexion time. This may be different from NATed IP.
	 *
	 * @return the expected parameter
	 * @since 2.0.0
	 */
	public boolean setIPAddr(final String v) {
		return setValue(Columns.IPADDR, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setHWAddr(final String v) {
		return setValue(Columns.HWADDR, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setTimeZone(final String v) {
		return setValue(Columns.TIMEZONE, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setOs(final OSEnum v) {
		return setValue(Columns.OS, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 6.0.0
	 */
	public boolean setOsVersion(final String v) {
		return setValue(Columns.OSVERSION, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 6.0.0
	 */
	public boolean setJavaVersion(final String v) {
		return setValue(Columns.JAVAVERSION, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 6.0.0
	 */
	public boolean setJavaDataModel(final int v) {
		return setValue(Columns.JAVADATAMODEL, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setCpu(final CPUEnum v) {
		return setValue(Columns.CPUTYPE, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setCpuModel(final String v) {
		return setValue(Columns.CPUMODEL, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setVersion(final String v) {
		return setValue(Columns.VERSION, v == null ? null : v);
	}

	/**
	 * This sets this host Shared applications
	 *
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0 (FG)
	 */
	public boolean setSharedApps(final String v) {
		return setValue(Columns.SHAREDAPPS, v == null ? null : v);
	}

	/**
	 * This sets this host shared data
	 *
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0 (FG)
	 */
	public boolean setSharedDatas(final String v) {
		return setValue(Columns.SHAREDDATAS, v == null ? null : v);
	}

	/**
	 * This sets this host shared packages
	 *
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0 (FG)
	 */
	public boolean setSharedPackages(final String v) {
		return setValue(Columns.SHAREDPACKAGES, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setProject(final String v) {
		return setValue(Columns.PROJECT, v == null ? null : v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLastAlive(final Date v) {
		return setValue(Columns.LASTALIVE, v);
	}

	/**
	 * This sets this host as(in)active; i.e. an active host is selectable for
	 * jobs. A host is inactivated if it has sent a job as ERROR. This can also
	 * be(in)activated on client request.
	 *
	 * @return true if this is attribute value has changed
	 */
	public boolean setActive(final boolean v) {
		return setValue(Columns.ACTIVE, Boolean.valueOf(v));
	}

	/**
	 * This sets this host as able to accept binary application
	 *
	 * @return true if this is attribute value has changed
	 */
	public boolean setAcceptBin(final boolean v) {
		return setValue(Columns.ACCEPTBIN, Boolean.valueOf(v));
	}

	/**
	 * This sets the eth wallet address
	 * @param addr is the eth wallet address
	 * @return true if value has changed, false otherwise
	 * @since 13.1.0
	 */
	public boolean setEthWalletAddr(final String addr)  {
		return setValue(Columns.ETHWALLETADDR, addr);
	}
	/**
	 * This sets the worker pool address this host belongs to
	 * @param addr is the worker pool address
	 * @return true if value has changed, false otherwise
	 * @since 13.1.0
	 */
	public boolean setWorkerPoolAddr(final String addr)  {
		return setValue(Columns.WORKERPOOLADDR, addr);
	}
    /**
     * This leaves the current market order
     * @since 13.1.0
     */
    public void leaveMarketOrder()  {
        setValue(Columns.MARKETORDERUID, null);
    }
    /**
     * This leaves the current market order
     * @since 13.1.0
     */
    public void leaveMarketOrder(final MarketOrderInterface marketOrder)  {
        if(marketOrder != null)
            marketOrder.removeWorker(this);
    }
    /**
     * This sets the market order uid
     * @param uid is the market order uid
     * @return true if value has changed, false otherwise
     * @since 13.1.0
     */
    public boolean setMarketOrderUid(final UID uid)  {
        if(uid == null) {
            setContribution(false);
        }
        return setValue(Columns.MARKETORDERUID, uid);
    }
    /**
     * This sets the contribution flag
     * @param c is the contribution flag
     * @return true if value has changed, false otherwise
     * @since 13.1.0
     */
    public boolean setContribution(final boolean c)  {
        return setValue(Columns.HASCONTRIBUTED, c);
    }
	/**
	 * This sets this host as(un)available accordingly to its local policy This
	 * is set on alive signal.
	 *
	 * @return true if this is attribute value has changed
	 */
	public boolean setAvailable(final boolean v) {
		return setValue(Columns.AVAILABLE, Boolean.valueOf(v));
	}

	/**
	 * This increments nbconnections If not set, this sets it to 0
	 */
	public void incNbConnections() {
		setNbConnections(getNbConnections() + 1);
	}

	/**
	 * This sets this host capability to run jobs that listen for incoming
	 * connections
	 *
	 * @return true if this is attribute value has changed
	 * @since 8.0.0
	 */
	public boolean setIncomingConnections(final boolean v) {
		return setValue(Columns.INCOMINGCONNECTIONS, Boolean.valueOf(v));
	}

	/**
	 * This sets this host as a pilot job
	 *
	 * @return true if this is attribute value has changed
	 */
	public boolean setPilotJob(final boolean v) {
		return setValue(Columns.PILOTJOB, Boolean.valueOf(v));
	}

	/**
	 * This set the service grid identifier
	 *
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setSgId(final String v) {
		return setValue(Columns.SGID, v);
	}

	/**
	 * This retrieves the service grid id
	 *
	 * @return the service grid identifier, or null if not set
	 * @since 7.0.0
	 */
	public String getSgId() {
		return (String) getValue(Columns.SGID);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setTracing(final boolean v) {
		return setValue(Columns.TRACES, Boolean.valueOf(v));
	}

	/**
	 * This is for testing only Without any argument, this dumps a HostInterface
	 * object. If the first argument is an XML file containing a description of
	 * a HostInterface, this creates a HostInterface from XML description and
	 * dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.HostInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final HostInterface itf = new HostInterface();
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
					"Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.HostInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
