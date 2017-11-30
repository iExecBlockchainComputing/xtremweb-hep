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

package xtremweb.database;

import java.io.IOException;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import xtremweb.common.StatusEnum;
import xtremweb.common.Table;
import xtremweb.common.UID;

/**
 * This is a decorator pattern aiming to implement SQL requests used to retrieve
 * objects from DB. This implements the SQL request to retrieve PENDING and
 * RUNNING works count for all users and also for the given user.<br />
 * This retrieves results from SQL statement:
 *
 * <pre>
 *   select login,
 *          uid as theuid,
 *          (select count(*) from works
 *           where works.isdeleted='false'
 *   	  and works.owneruid=theuid
 *   	  and status='WAITING') as twaitings,
 *          (select count(*) from works
 *           where works.isdeleted='false'
 *   	  and works.owneruid=theuid
 *   	  and status='PENDING') as tpendings,
 *          (select count(*) from works
 *           where works.isdeleted='false'
 *   	  and works.owneruid=theuid
 *   	  and status='RUNNING') as trunnings,
 *          (select count(*) from works
 *           where works.isdeleted='false'
 *   	  and works.owneruid=theuid
 *   	  and status='ERROR') as terrors,
 *          (select count(*) from works
 *           where works.isdeleted='false'
 *   	  and works.owneruid=theuid
 *   	  and status='COMPLETED') as tcompleteds
 *   from users
 *   where users.isdeleted='false'
 *   group by users.uid;
 * </pre>
 *
 * @author <A HREF="mailto:lodygens /at\ lal.in2p3.fr">Oleg Lodygensky </A>
 * @since 5.8.0
 */
public class SQLRequestUserWorksCountAll extends Table {

	private static final Logger logger = Logger.getLogger(SQLRequestUserWorksCountAll.class);

	private static final String TABLENAME = "users";
	private static final String SELECTIONROW = "login," + "uid as theuid, " + "   (select count(*) from works "
			+ "where works.isdeleted='false'" + "	  and works.owneruid=theuid"
			+ "	  and status='WAITING') as waitings," + "   (select count(*) from works "
			+ "where works.isdeleted='false'" + "	  and works.owneruid=theuid"
			+ "	  and status='PENDING') as pendings," + "   (select count(*) from works "
			+ "where works.isdeleted='false'" + "	  and works.owneruid=theuid"
			+ "	  and status='RUNNING') as runnings," + "   (select count(*) from works "
			+ "where works.isdeleted='false'" + "	  and works.owneruid=theuid"
			+ "	  and status='ERROR') as errors," + "   (select count(*) from works "
			+ "    where works.isdeleted='false'" + "	  and works.owneruid=theuid"
			+ "	  and status='COMPLETED') as completeds";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns {
		THEUID, LOGIN, WAITINGS, PENDINGS, RUNNINGS, ERRORS, COMPLETEDS
	};

	private UID userUID;
	private String login;

	private int waitings;
	private int pendings;
	private int runnings;
	private int errors;
	private int completeds;

	private StatusEnum status;

	/**
	 * This is the default constructor It creates a new object which is **not**
	 * written in the database. This sets status of the work to retrieve to
	 * WAITING
	 */
	public SQLRequestUserWorksCountAll() {
		super(TABLENAME, TABLENAME);
		waitings = 0;
		pendings = 0;
		runnings = 0;
		errors = 0;
		completeds = 0;
		login = null;
		userUID = null;
		status = StatusEnum.WAITING;
	}

	/**
	 * This is the default constructor It creates a new object which is **not**
	 * written in the database
	 *
	 * @param s
	 *            is the status of the works to retrieve from DB
	 */
	public SQLRequestUserWorksCountAll(final StatusEnum s) {
		this();
		status = s;
	}

	/**
	 * This constructor instanciates an object from data read from an SQL table
	 *
	 * @see #fill(ResultSet)
	 */
	public SQLRequestUserWorksCountAll(final ResultSet rs) throws IOException {
		super(TABLENAME, TABLENAME);
		fill(rs);
	}

	/**
	 * This aims to retrieve "GROUP BY" SQL statement. This returns null and
	 * should be overridden
	 *
	 * @return "login"
	 * @since 5.8.0
	 */
	@Override
	public String groupBy() throws IOException {
		return Columns.THEUID.toString();
	}

	/**
	 * This aims to retrieve rows for the "SELECT" SQL statement.
	 *
	 * @return rows for the SELECT SQL statement
	 * @since 5.8.0
	 */
	@Override
	public String rowSelection() throws IOException {
		return SELECTIONROW;
	}

	/**
	 * This fills this object with data from DB
	 *
	 * @param rs
	 *            is a ResultSet read from DB
	 */
	@Override
	public final void fill(final ResultSet rs) throws IOException {
		try {
			setUID(new UID(rs.getString(Columns.THEUID.toString())));
		} catch (final Exception e) {
			throw new IOException("Can't find UID from result set");
		}
		try {
			setLogin(rs.getString(Columns.LOGIN.toString()));
		} catch (final Exception e) {
			throw new IOException("Can't find user login from result set");
		}
		try {
			setWaitings(rs.getInt(Columns.WAITINGS.toString()));
		} catch (final Exception e) {
			logger.warn("can't find waitingsfrom result set");
			setWaitings(0);
		}
		try {
			setPendings(rs.getInt(Columns.PENDINGS.toString()));
		} catch (final Exception e) {
			logger.warn("can't find pendingsfrom result set");
			setPendings(0);
		}
		try {
			setRunnings(rs.getInt(Columns.RUNNINGS.toString()));
		} catch (final Exception e) {
			logger.warn("can't find runningsfrom result set");
			setRunnings(0);
		}
		try {
			setCompleteds(rs.getInt(Columns.COMPLETEDS.toString()));
		} catch (final Exception e) {
			logger.warn("can't find completedsfrom result set");
			setCompleteds(0);
		}
		try {
			setErrors(rs.getInt(Columns.ERRORS.toString()));
		} catch (final Exception e) {
			logger.warn("can't find errorsfrom result set");
			setErrors(0);
		}
	}

	/**
	 * This sets the user login
	 */
	protected void setLogin(final String l) throws IOException {
		login = l;
	}

	/**
	 * This retrieves the user login
	 *
	 * @return the user login
	 */
	public String getLogin() throws IOException {
		return login;
	}

	/**
	 * This sets waiting count
	 */
	protected void setWaitings(final int c) throws IOException {
		waitings = c;
	}

	/**
	 * This retrieve waiting count
	 *
	 * @return waiting amount
	 */
	public int waitings() throws IOException {
		return waitings;
	}

	/**
	 * This sets running count
	 */
	private void setPendings(final int c) throws IOException {
		pendings = c;
	}

	/**
	 * This retrieve pending count
	 *
	 * @return pending count
	 */
	public int pendings() throws IOException {
		return pendings;
	}

	/**
	 * This sets running count
	 */
	private void setRunnings(final int c) throws IOException {
		runnings = c;
	}

	/**
	 * This retrieve running count
	 *
	 * @return running count
	 */
	public int runnings() throws IOException {
		return runnings;
	}

	/**
	 * This sets completed count
	 */
	private void setCompleteds(final int c) throws IOException {
		completeds = c;
	}

	/**
	 * This retrieve completed count
	 *
	 * @return completed count
	 */
	public int completeds() throws IOException {
		return completeds;
	}

	/**
	 * This sets error count
	 */
	private void setErrors(final int c) throws IOException {
		errors = c;
	}

	/**
	 * This retrieve error count
	 *
	 * @return error count
	 */
	public int errors() throws IOException {
		return errors;
	}

	@Override
	public boolean setValue(final String attribute, final Object v) throws IllegalArgumentException {
		return false;
	}

	@Override
	public void updateInterface(final Table t) throws IOException {
	}
}
