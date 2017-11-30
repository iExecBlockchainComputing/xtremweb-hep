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
import xtremweb.common.WorkInterface;

/**
 * This is a decorator pattern aiming to implement SQL requests used to retreive
 * objects from DB. This implements the SQL request to retreive works count with
 * the given status from DB.<br />
 * This retrieves results from SQL statement:
 *
 * <pre>
 *   SELECT count(*) as count
 *   FROM works
 *   WHERE status="&lt;aStatus&gt;"
 *     AND works.isdeleted="false"
 *   LIMIT 1000
 * </pre>
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr">Oleg Lodygensky </a>
 * @since 5.8.0
 */
public class SQLRequestWorksCount extends WorkInterface {

	private static final Logger logger = Logger.getLogger(SQLRequestWorksCount.class);

	private static final String SELECTIONROW = "count(*) as " + Columns.COUNT;

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns {
		COUNT
	};

	private int count;

	private StatusEnum status;

	/**
	 * This sets status of the work to retrieve to WAITING
	 */
	public SQLRequestWorksCount() {
		super();
		count = 0;
		status = StatusEnum.WAITING;
	}

	/**
	 * This sets the status to the given one
	 *
	 * @param s
	 *            is the status of the works to retreive from DB
	 */
	public SQLRequestWorksCount(final StatusEnum s) {
		this();
		status = s;
	}

	/**
	 * This calls fills(r)
	 *
	 * @see WorkInterface#fill(ResultSet)
	 */
	public SQLRequestWorksCount(final ResultSet r) throws IOException {
		this();
		fill(r);
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
			setCount(rs.getInt(Columns.COUNT.toString()));
		} catch (final Exception e) {
			logger.warn("can't find waitingsfrom result set");
			setCount(0);
		}
	}

	/**
	 * This sets the count member
	 */
	public void setCount(final int c) throws IOException {
		count = c;
	}

	/**
	 * This gets parameter
	 *
	 * @return the expected parameter
	 */
	public int getCount() throws IOException {
		return count;
	}
}
