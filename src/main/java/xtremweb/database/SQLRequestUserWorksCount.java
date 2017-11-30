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
import xtremweb.common.TableColumns;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;

/**
 * This is a decorator pattern aiming to implement SQL requests used to retrieve
 * objects from DB. This implements the SQL request to retrieve works count for
 * the given user and the given work status.<br />
 * This retrieves results from SQL statement:<br />
 *
 * <pre>
 *   SELECT users.uid, users.login,count(*) AS workscount
 *   FROM works, users
 *   WHERE (status='&lt;theGivenStatus&gt;' OR ISNULL(status))
 *     AND active='true'
 *     AND works.useruid=users.uid
 *     AND works.isdeleted = 'false'
 *   GROUP BY LOGIN
 *   LIMIT 1000
 * </pre>
 *
 * @author <A HREF="mailto:lodygens /at\ lal.in2p3.fr">Oleg Lodygensky </A>
 * @since 5.8.0
 */
public class SQLRequestUserWorksCount extends Table {

	private static final Logger logger = Logger.getLogger(SQLRequestUserWorksCount.class);

	private static final String TABLENAME = WorkInterface.TABLENAME;
	/**
	 * This contains table names for the FROM part of the SQL request. There
	 * must not be a space after the comma
	 *
	 * @see DBConnPool#rowTableNames(String)
	 */
	private static final String TABLENAMES = WorkInterface.TABLENAME + " as " + SQLRequest.MAINTABLEALIAS + ","
			+ UserInterface.TABLENAME;
	private static final String SELECTIONROW = "users.uid, users.login,count(*) as workscount";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns {
		UID, WORKSCOUNT, LOGIN
	};

	private UID userUID;
	private int worksCount;
	private String login;
	private StatusEnum status;

	/**
	 * This sets status of the work to retrieve to WAITING
	 */
	public SQLRequestUserWorksCount() {
		super(TABLENAME, TABLENAME);
		worksCount = 0;
		login = null;
		status = StatusEnum.WAITING;
	}

	/**
	 * This constructor sets the status to the given one
	 *
	 * @param s
	 *            is the status of the works to retrieve from DB
	 */
	public SQLRequestUserWorksCount(final StatusEnum s) {
		this();
		status = s;
	}

	/**
	 * This constructor instanciates an object from data read from an SQL table
	 *
	 * @see #fill(ResultSet)
	 */
	public SQLRequestUserWorksCount(final ResultSet rs) throws IOException {
		super(TABLENAME, TABLENAME);
		fill(rs);
	}

	/**
	 * This aims to retrieve "GROUP BY" SQL statement. This returns null and
	 * should be overriden
	 *
	 * @return null
	 * @since 5.8.0
	 */
	@Override
	public String groupBy() throws IOException {
		return Columns.LOGIN.toString();
	}

	/**
	 * This retrieves "FROM" SQL statement table names
	 *
	 * @return TABLENAMES
	 * @since 5.8.0
	 */
	@Override
	public String fromTableNames() {
		return TABLENAMES;
	}

	/**
	 * This aims to retrieve rows for the "SELECT" SQL statement.
	 *
	 * @return "users.uid, users.login,count(*)"
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
			setUID(new UID(rs.getString(Columns.UID.toString())));
		} catch (final Exception e) {
			throw new IOException("Can't find UID from result set");
		}
		try {
			setWorksCount(rs.getInt(Columns.WORKSCOUNT.toString()));
		} catch (final Exception e) {
			logger.warn("can't find works count from result set");
			setWorksCount(0);
		}
		try {
			setLogin(rs.getString(Columns.LOGIN.toString()));
		} catch (final Exception e) {
			throw new IOException("Can't find user login from result set");
		}
	}

	/**
	 * This retrieves this SQL criteria
	 *
	 * @return a String containing SQL criteria
	 */
	@Override
	public String criteria() throws IOException {

		return SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.STATUS + "='" + status + "' AND "
				+ SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.ACTIVE + "='" + Boolean.toString(true)
				+ "' AND " + SQLRequest.MAINTABLEALIAS + "." + TableColumns.OWNERUID + "=" + UserInterface.TABLENAME
				+ "." + TableColumns.UID;
	}

	/**
	 * This retrieves user login
	 */
	public void setLogin(final String l) throws IOException {
		login = l;
	}

	/**
	 * This set the works count
	 */
	public void setWorksCount(final int c) throws IOException {
		worksCount = c;
	}

	/**
	 * This retrieves the user logn
	 *
	 * @return the user login
	 */
	public String getLogin() throws IOException {
		return login;
	}

	/**
	 * This retrieves the works count
	 *
	 * @return the works count
	 */
	public int getWorksCount() throws IOException {
		return worksCount;
	}

	@Override
	public boolean setValue(final String attribute, final Object v) throws IllegalArgumentException {
		return false;
	}

	@Override
	public void updateInterface(final Table t) throws IOException {
	}
}
