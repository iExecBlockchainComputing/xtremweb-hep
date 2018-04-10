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

/**
 * This class helps to define some constants This is used to define Works, Tasks
 * and results status
 *
 * <br />
 * <br />
 * Job state graph <code>
 *                        Waiting
 *                           |
 *                           v
 *     +----------------> Pending <----------------------+
 *     ^                     |                           ^
 *     |                     v                           |
 *     |                  Running <--- ResultRequest -> Lost
 *     |                     |               ^           ^
 *     |                     v               |           |
 *     |                     |               |           |
 *     |                Replicating          |           |
 *     |                     |               |           |
 *     |                     v               |           |
 *  Aborted <----+-----------+---------------+-----------+-----------+
 *               |           |               |           |           |
 *               v           v               v           |           v
 *             Error    Completed <-- DataRequest -------+         Failed
 *
 * </code>
 *
 * Any update in this enum must be reflected in src/scripts/db-maintenance: -
 * xwhep-core-tables-create-tables.sql.in -
 * xwhep-core-tables-create-views-for-sessions-and-groups.sql -
 * xwhep-core-tables-from-8-create-new-tables-columns-fk.sql
 *
 */
public enum StatusEnum {

	/**
	 * This is no status
	 */
	NONE,
	/**
	 * This is any status
	 */
	ANY,
	/**
	 * This tells work has not been inserted in scheduling queue yet
	 */
	WAITING,
	/**
	 * This tells work has been inserted in scheduling queue and is waiting for
	 * computation
	 */
	PENDING,
	/**
	 * This tells this work is being computed
	 */
	RUNNING,
	/**
	 * This denotes a middleware error
	 */
	ERROR,
	/**
	 * This denotes a completed job
	 */
	COMPLETED,
	/**
	 * This tells work has been canceled by worker local activation policy
	 */
	ABORTED,
	/**
	 * This denotes a lost job (the worker does not send heart beat signal)
	 */
	LOST,
	/**
	 * This denotes a job still waiting for its result to be (re)uploaded from
	 * worker. If results are retrieved after 3 Alive signals, the job status
	 * passes to PENDING to be recomputed
	 */
	DATAREQUEST,
	/**
	 * This is used to ask the computing resource to send results "as is", for a
	 * job which is running The computing resource send a archive of the job
	 * working directory
	 *
	 * @since 8.2.0
	 */
	RESULTREQUEST,
	/**
	 * This tells this object is available
	 */
	AVAILABLE,
	/**
	 * This tells this object is not available
	 */
	UNAVAILABLE,
	/**
	 * This tells work has not reached expected replication yet
	 *
	 * @since 10.2.0
	 */
	REPLICATING,
	/**
	 * This denotes an user error
     * e.g. : the work does not comply to its envelope parameters
	 * @since 13.0.0
	 */
	FAILED;

	public static final StatusEnum LAST = REPLICATING;
	public static final int SIZE = LAST.ordinal() + 1;

	/**
	 * This retrieves a status from its integer value
	 *
	 * @param v
	 *            is the integer value of the status
	 * @return a StatusEnum
	 */
	public static StatusEnum fromInt(final int v) throws IndexOutOfBoundsException {
		for (final StatusEnum c : StatusEnum.values()) {
			if (c.ordinal() == v) {
				return c;
			}
		}
		throw new IndexOutOfBoundsException("unvalid status " + v);
	}

	/**
	 * This array stores enum as string
	 */
	private static String[] LABELS = null;

	/**
	 * This retreives this enum string representation
	 *
	 * @return a array containing this enum string representation
	 */
	public static String[] getLabels() {
		if (LABELS != null) {
			return LABELS;
		}
		LABELS = new String[SIZE];
		for (final StatusEnum c : StatusEnum.values()) {
			LABELS[c.ordinal()] = c.toString();
		}
		return LABELS;
	}

	/**
	 * This dumps enums to stdout
	 */
	public static void main(final String[] argv) {
		for (final StatusEnum i : StatusEnum.values()) {
			System.out.println(i.toString());
		}
	}
}
