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
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import java.util.Date;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * TaskInterface.java
 *
 * Created: Feb 19th, 2002
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

/**
 * This class describes a row of the tasks SQL table.
 */
public final class TaskInterface extends xtremweb.common.Table {

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.Task
	 *
	 * @since 9.0.0
	 */
	public static final String TABLENAME = ("tasks");

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "task";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is the reward for the computing resource
		 * This is typically used through Blockchain
		 * @since 13.1.0
		 */
		PRICE {
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
		/**
		 * This is the column index of the referenced work UID
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
		 * This is the column index of the running worker UID
		 */
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
		 * This is the column index of the status
		 */
		STATUS {
			/**
			 * This creates a new UID from string
			 */
			@Override
			public StatusEnum fromString(final String v) {
				return StatusEnum.valueOf(v);
			}
		},
		/**
		 * This is the column index of the insertion date
		 */
		INSERTIONDATE {
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
		 * This is the column index of the last start date
		 */
		LASTSTARTDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		/**
		 * This is the column index of the alive count
		 */
		ALIVECOUNT {
			@Override
			public Integer fromString(final String v) {
				return new Integer(v);
			}
		},
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
		 * This is the column index of the removal date
		 */
		REMOVALDATE {
			@Override
			public Date fromString(final String v) {
				return XWTools.getSQLDateTime(v);
			}
		},
		/**
		 * This is the column index of the duration
		 */
		DURATION {
			@Override
			public Long fromString(final String v) {
				return new Long(v);
			}
		};

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
	public TaskInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);
		setStatus(StatusEnum.UNAVAILABLE);
		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(
				new int[] { TableColumns.UID.getOrdinal(), Columns.WORKUID.getOrdinal(), Columns.STATUS.getOrdinal() });
	}

	/**
	 * This constructor reads its definition from a String provided as argument
	 * where each field is separated by a '\t' character.
	 *
	 * Such a String has typically been created with an SQL command like : $>
	 * mysql -e "select * from tasks" > aTextFile
	 *
	 *
	 * @param data
	 *            is a String containing this object definition where each field
	 *            is separated by a tab, a space or a comma character
	 */
	public TaskInterface(final String data) {

		this();

		final StringTokenizer tokenizer = new StringTokenizer(data, "\t ,");

		for (int attr = FIRST_ATTRIBUTE; tokenizer.hasMoreTokens(); attr++) {
			final String elem = tokenizer.nextToken();
			setValue(Columns.fromInt(attr), elem);
		}
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException
	 */
	public TaskInterface(final ResultSet rs) throws IOException {
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
			setWork((UID) Columns.WORKUID.fromResultSet(rs));
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
		try {
			setPrice((Long) Columns.PRICE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setAliveCount((Integer) Columns.ALIVECOUNT.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setDuration((Long) Columns.DURATION.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setHost((UID) Columns.HOSTUID.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setStatus((StatusEnum) Columns.STATUS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setStartDate((Date) Columns.STARTDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLastStartDate((Date) Columns.LASTSTARTDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLastAliveDate((Date) Columns.LASTALIVE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setRemovalDate((Date) Columns.REMOVALDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setInsertionDate((Date) Columns.INSERTIONDATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		setDirty(false);
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #TaskInterface(InputStream)
	 */
	public TaskInterface(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This creates an object that will be retrieve with the complex SQL request
	 *
	 * @see xtremweb.common.TaskInterface#TaskInterface(ResultSet)
	 * @since 9.0.0
	 */
	public TaskInterface(final SQLRequest r) throws IOException {
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
	public TaskInterface(final InputStream input) throws IOException, SAXException {
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
	public TaskInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This is the constructor; it inserts new task into DB
	 *
	 * @param w
	 *            is the work the task is created for
	 */
	public TaskInterface(final WorkInterface w) throws IOException {

		this();
		UID uid = new UID();
		setUID(uid);
		uid = null;
		setOwner(w.getOwner());
		setWork(w.getUID());
		setStatus(w.getStatus());
		setAccessRights(w.getAccessRights());
		setInsertionDate();
		try {
			insert();
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
	}

	/**
	 * This updates this object from interface.
	 */
	@Override
	public void updateInterface(final Table titf) throws IOException {
		final TaskInterface itf = (TaskInterface) titf;
		setOwner(itf.getOwner());
		setWork(itf.getWork());
		setAccessRights(itf.getAccessRights());
		setDuration(itf.getDuration());
		setStatus(itf.getStatus());
		setAliveCount(itf.getAliveCount());
		setHost(itf.getHost());
		setInsertionDate(itf.getInsertionDate());
		setStartDate(itf.getStartDate());
		setLastStartDate(itf.getLastStartDate());
		setLastAliveDate(itf.getLastAlive());
		setRemovalDate(itf.getRemovalDate());
		setPrice(itf.getPrice());
	}

	/**
	 * This sets parameter
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean incAliveCount() {
		Integer i = null;
		if (getValue(Columns.ALIVECOUNT) == null) {
			i = new Integer(1);
			setValue(Columns.ALIVECOUNT, i);
		} else {
			int count = ((Integer) getValue(Columns.ALIVECOUNT)).intValue();
			i = new Integer(count++);
			setValue(Columns.ALIVECOUNT, i);
		}
		i = null;
		return true;
	}

	/**
	 * This retrieves the job duration<br />
	 * If this attr is not set, it os forced to 0
	 *
	 * @return this attribute or 0 if not set
	 */
	public long getDuration() {
		final Long ret = (Long) getValue(Columns.DURATION);
		if (ret != null) {
			return ret.longValue();
		}
		setDuration(0L);
		return 0;
	}

	/**
	 * This returns XWStatus.fromInt(getValue(Columns.STATUSID))
	 *
	 * @return the status
	 */
	public StatusEnum getStatus() {
		try {
			return (StatusEnum) getValue(Columns.STATUS);
		} catch (final Exception e) {
			setStatus(StatusEnum.NONE);
			return StatusEnum.NONE;
		}
	}
	/**
	 * This retrieves the price for this application
	 *
	 * @return the price
	 * @since 13.1.0
	 */
	public Long getPrice() {
		final Long ret = (Long) getValue(Columns.PRICE);
		if (ret != null) {
			return ret.longValue();
		}
		return 0L;
	}
	/**
	 * This reterives the alive counter<br />
	 * If this attr is not set, it os forced to 0
	 *
	 * @return this attribute or 0 if not set
	 */
	public int getAliveCount() {
		final Integer ret = (Integer) getValue(Columns.ALIVECOUNT);
		if (ret != null) {
			return ret.intValue();
		}
		setAliveCount(0);
		return 0;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 * @exception IOException
	 *                is thrown is attribute is not well formed
	 */
	public UID getHost() {
		try {
			return (UID) getValue(Columns.HOSTUID);
		} catch (final NullPointerException e) {
			return null;
		}
	}

	/**
	 * This retrieves the referenced work
	 *
	 * @return this attribute, or null if not set
	 * @exception IOException
	 *                is thrown is attribute is not well formed
	 * @since 8.0.0
	 */
	public UID getWork() throws IOException {
		return (UID) getValue(Columns.WORKUID);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getInsertionDate() {
		return (Date) getValue(Columns.INSERTIONDATE);
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
	public Date getLastStartDate() {
		return (Date) getValue(Columns.LASTSTARTDATE);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getLastAlive() {
		return (Date) getValue(Columns.LASTALIVE);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public Date getRemovalDate() {
		return (Date) getValue(Columns.REMOVALDATE);
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
	 * This sets the duration
	 *
	 * @return true if value has changed
	 */
	public boolean setDuration(final long v) {
		Long i = new Long(v);
		final boolean ret = setValue(Columns.DURATION, i);
		i = null;
		return ret;
	}

	/**
	 * This calls setStatusId(v.ordinal())
	 *
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setStatus(final StatusEnum v) throws ArrayIndexOutOfBoundsException {
		return setValue(Columns.STATUS, v);
	}

	/**
	 * This tests task status
	 *
	 * @param v
	 *            the value to test; must be one of the values defined in
	 *            XWStatus.java.
	 * @return a boolean.
	 * @see xtremweb.common.StatusEnum
	 */
	public boolean testStatus(final StatusEnum v) {
		try {
			return (getStatus() == v);
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This sets the task status as running on a worker.
	 */
	public void setRunning() {
		Thread.currentThread().dumpStack();
		setStatus(StatusEnum.RUNNING);
	}

	/**
	 * This tests whether task is running on a worker.
	 *
	 * @return a boolean.
	 */
	public boolean isRunning() {
		return testStatus(StatusEnum.RUNNING);
	}

	/**
	 * This tests whether task is supposed to be processed by a worker
	 *
	 * @return a boolean.
	 * @since 13.1.0
	 */
	public boolean isUnderProcess() {
		return isRunning()
				|| isDataRequest()
				|| isResultRequest()
				|| hasContributed()
				|| isContributing()
				|| isRevealing();
	}
	/**
	 * This sets the task status as pending for a worker.
	 */
	public void setPending() {
		setStatus(StatusEnum.PENDING);
	}
	/**
	 * This marks this as ready to reveal contribution
	 * i.eg: the worker will send the contribution proof
	 * @since 13.1.0
	 */
	public void setRevealing() {
		setStatus(StatusEnum.REVEALING);
	}
	/**
	 * This checks if this has contributed
	 * i.eg: the worker sent the contribution proposal
	 * @since 13.1.0
	 */
	public boolean hasContributed() {
		return getStatus() == StatusEnum.CONTRIBUTED;
	}
	/**
	 * This checks if this work contribution must be sent
	 * @since 13.1.0
	 */
	public boolean isContributing() {
		return getStatus() == StatusEnum.CONTRIBUTING;
	}
	/**
	 * This marks this work as ready to reveal contribution, if this.getWorkOrderId() != null
	 * @since 13.1.0
	 */
	public boolean isRevealing() {
		return getStatus() == StatusEnum.REVEALING;
	}
	/**
	 * This marks this as contribution
	 * i.eg: the worker sent the contribution proposal
	 * @since 13.1.0
	 */
	public void setContributed() {
		setStatus(StatusEnum.CONTRIBUTED);
	}

	public void setContributing() {
		setStatus(StatusEnum.CONTRIBUTING);
	}

	/**
	 * This tests whether task is pending for a worker.
	 *
	 * @return a boolean.
	 */
	public boolean isPending() {
		return testStatus(StatusEnum.PENDING);
	}

	/**
	 * This sets the task status as completed by a worker.
	 */
	public void setCompleted() {
		Thread.currentThread().dumpStack();
		setStatus(StatusEnum.COMPLETED);
		setRemovalDate();
	}

	/**
	 * This tests whether task is completed by worker.
	 *
	 * @return a boolean.
	 */
	public boolean isCompleted() {
		return testStatus(StatusEnum.COMPLETED);
	}

	/**
	 * This marks this work as still waiting for result
	 *
	 * @since 7.5.0
	 */
	public void setDataRequest() {
		setStatus(StatusEnum.DATAREQUEST);
	}

	/**
	 * This marks this work as waiting for intermediate results
	 *
	 * @since 8.2.0
	 */
	public void setResultRequest() {
		setStatus(StatusEnum.RESULTREQUEST);
	}

	/**
	 * This tests if this work is still waiting for result
	 *
	 * @since 7.5.0
	 */
	public boolean isDataRequest() {
		return testStatus(StatusEnum.DATAREQUEST);
	}

	/**
	 * This tests if this work is waiting for intermediate results
	 *
	 * @since 8.2.0
	 */
	public boolean isResultRequest() {
		return testStatus(StatusEnum.RESULTREQUEST);
	}

	/**
	 * This sets the task status as unable to run on worker.
	 */
	public void setError() {
		Thread.currentThread().dumpStack();
		setStatus(StatusEnum.ERROR);
		setRemovalDate();
	}

	/**
	 * This tests whether task generated any error
	 *
	 * @return a boolean.
	 */
	public boolean isError() {
		return testStatus(StatusEnum.ERROR);
	}

	/**
	 * This sets the task to FAILED
	 * @since 13.0.0
	 */
	public void setFailed() {
		setStatus(StatusEnum.FAILED);
		setRemovalDate();
	}

	/**
	 * This tests is this task is FAILED
	 * @since 13.0.0
	 * @return a boolean.
	 */
	public boolean isFailed() {
		return testStatus(StatusEnum.FAILED);
	}


	public boolean setAliveCount(final int v) {
		Integer i = new Integer(v);
		final boolean ret = setValue(Columns.ALIVECOUNT, i);
		i = null;
		return ret;
	}

	/**
	 * This sets the worker that runs this task
	 */
	public boolean setHost(final UID v) {
		return setValue(Columns.HOSTUID, v);
	}

	/**
	 * This sets the referenced work
	 *
	 * @since 8.0.0
	 */
	public boolean setWork(final UID v) {
		return setValue(Columns.WORKUID, v);
	}

	/**
	 * This sets the date when this task has been created
	 * 
	 * @since 10.5.1
	 */
	public boolean setInsertionDate() {
		final Date date = new Date();
		return setInsertionDate(date);
	}

	/**
	 * This sets the date when this task has been created
	 */
	public boolean setInsertionDate(final Date v) {
		return setValue(Columns.INSERTIONDATE, v);
	}

	/**
	 * This sets the starting date of the first run
	 */
	public boolean setStartDate(final Date v) {
		return setValue(Columns.STARTDATE, v);
	}

	/**
	 * This sets the starting date of the last run
	 */
	public boolean setLastStartDate(final Date v) {
		return setValue(Columns.LASTSTARTDATE, v);
	}

	/**
	 * This sets the last alive signal date of the current run
	 */
	public boolean setLastAliveDate(final Date v) {
		return setValue(Columns.LASTALIVE, v);
	}

	/**
	 * This sets the removal date
	 * 
	 * @since 10.5.1
	 */
	public boolean setRemovalDate() {
		final Date date = new Date();
		return setRemovalDate(date);
	}

	/**
	 * This sets the removal date
	 */
	public boolean setRemovalDate(final Date v) {
		return setValue(Columns.REMOVALDATE, v);
	}

	/**
	 * This set removal date
	 */
	public void remove() {

		long duration;
		final java.util.Date removaldate = new java.util.Date();
		final java.util.Date laststartdate = getLastStartDate();

		setRemovalDate(removaldate);

		if (laststartdate != null) {
			duration = (removaldate.getTime() - laststartdate.getTime()) / 1000;
			setDuration(duration);
		}
	}

	/**
	 * Assign a task to a worker
	 *
	 * @param worker
	 *            is the worker UID
	 */
	public void setRunningBy(final UID worker) {

		if ((isUnderProcess()) || (getHost() != null)) {
			getLogger().debug("" + getUID() + " already under process : " + getStatus() + ", " +
					(getHost() == null  ? "null" : getHost().getMyUid()));
			return;
		}

		final java.util.Date newDate = new java.util.Date(System.currentTimeMillis());

		setHost(worker);
		setLastAliveDate(newDate);

		if (getStartDate() == null) {
			setStartDate(newDate);
		}
		setLastStartDate(newDate);
		setRunning();
	}

	/**
	 * Update due to alive signal Since RPC-V : we must ckeck the task is not
	 * running on another worker; in such a case, the current worker we are
	 * computing the alive signal for, has to be asked to stop computing.
	 *
	 * @param worker
	 *            is the worker UID
	 * @return true on success(since RPC-V); which covers two cases: 1) the
	 *         computing worker is the expected one; 2) no computing worker is
	 *         asigned to that task certainly because the server has crashed and
	 *         relaunched since it had provided the task to the signalling
	 *         worker false on error(since RPC-V); the worker is not the
	 *         expected one! the signalling worker has to be asked to stop
	 *         computing
	 */
	public boolean setAlive(final UID worker) {

		if (getHost() == null) {
			setRunningBy(worker);
		}

		if (worker.equals(getHost()) == false) {
			return false;
		}

		setLastAliveDate(new java.util.Date(System.currentTimeMillis()));

		incAliveCount();

		return true;
	}
	/**
	 * This sets the price
	 *
	 * @param v is the price
	 * @return true if value has changed, false otherwise
	 * @since 13.1.0
	 */
	public boolean setPrice(final long v) {
		try {
			return setValue(Columns.PRICE, Long.valueOf(v < 0L ? 0L : v));
		} catch (final Exception e) {
			return setValue(Columns.PRICE, 0L);
		}
	}

	/**
	 * This is for testing only. Without any argument, this dumps a
	 * TaskInterface object. If the first argument is an XML file containing a
	 * description of a TaskInterface, this creates a TaskInterface from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.TaskInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final TaskInterface itf = new TaskInterface();
			itf.setUID(UID.getMyUid());
			itf.setLoggerLevel(LoggerLevel.DEBUG);
			if (argv.length > 0) {
				final XMLReader reader = new XMLReader(itf);
				reader.read(new FileInputStream(argv[0]));
			}
			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			final Logger logger = new Logger();
			logger.exception(
					"Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.TaskInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
