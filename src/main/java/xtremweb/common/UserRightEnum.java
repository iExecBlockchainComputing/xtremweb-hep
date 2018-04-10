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
 * UserRights.java
 *
 * Created: Nov 3rd, 2003
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

/**
 * This class describes static variables that define user right levels
 */

public enum UserRightEnum {

	/**
	 * no right
	 */
	NONE,
	/**
	 * submit/update job
	 */
	INSERTJOB,
	/**
	 * retrieve job
	 */
	GETJOB,
	/**
	 * send/update data
	 */
	INSERTDATA,
	/**
	 * retrieve data
	 */
	GETDATA,
	/**
	 * retrieve group
	 */
	GETGROUP,
	/**
	 * retrieve session
	 */
	GETSESSION,
	/**
	 * retrieve host
	 */
	GETHOST,
	/**
	 * retrieve application
	 */
	GETAPP,
	/**
	 * retrieve user
	 */
	GETUSER,
	/**
	 * retrieve envelope
	 * @since 13.0.0
	 */
	GETENVELOPE,
	/**
	 * This allows user rights delegation to workers e.g. workers must be able
	 * to update works and change works status (among others)
	 */
	UPDATEWORK,
	/**
	 * this is the user rights needed by workers
	 */
	WORKER_USER,
	/**
	 * This denotes virtualized worker user rights. This aims to allow access
	 * objects that has the sticky bit.
	 *
	 * @since 8.0.0
	 * @see xtremweb.security.XWAccessRights#STICKYBIT
	 */
	VWORKER_USER,
	/**
	 * broadcast a job to all known worker
	 */
	BROADCAST,
	/**
	 * retrieve jobs list
	 */
	LISTJOB,
	/**
	 * delete job
	 */
	DELETEJOB,
	/**
	 * retrieve datas list
	 */
	LISTDATA,
	/**
	 * delete data
	 */
	DELETEDATA,
	/**
	 * retrieve groups list
	 */
	LISTGROUP,
	/**
	 * send/update group
	 */
	INSERTGROUP,
	/**
	 * delete group
	 */
	DELETEGROUP,
	/**
	 * retrieve sessions list
	 */
	LISTSESSION,
	/**
	 * send/update session
	 */
	INSERTSESSION,
	/**
	 * delete session
	 */
	DELETESESSION,
	/**
	 * retrieve hosts list
	 */
	LISTHOST,
	/**
	 * Retrieve users list
	 */
	LISTUSER,
	/**
	 * retrieve user groups list
	 */
	LISTUSERGROUP,
	/**
	 * retrieve user group
	 */
	GETUSERGROUP,
	/**
	 * send/update app
	 */
	INSERTAPP,
	/**
	 * delete app
	 */
	DELETEAPP,
	/**
	 * retrieve applications list
	 */
	LISTAPP,
	/**
	 * retrieve envelopes list
	 * @since 13.0.0
	 */
	LISTENVELOPE,
	/**
	 * this is the default user right
	 */
	STANDARD_USER,
	/**
	 * this is a advanced rights : send/update a user
	 */
	INSERTUSER,
	/**
	 * this is a advanced rights : delete user
	 */
	DELETEUSER,
	/**
	 * this is the advanced right
	 */
	ADVANCED_USER,
	/**
	 * This permits to mandate : a user can be mandated to do something for someone else
	 * @since 11.0.0
	 */
	MANDATED_USER,
	/**
	 * this is a privileged right : send/update host
	 */
	INSERTHOST,
	/**
	 * this is a privileged right : send/update envelope
	 * @sonce 13.0.0
	 */
	INSERTENVELOPE,
	/**
	 * this is a privileged right : delete host
	 */
	DELETEHOST,
	/**
	 * this is a privileged right : send/update user group
	 */
	INSERTUSERGROUP,
	/**
	 * this is a privileged right : delete user group
	 */
	DELETEUSERGROUP,
	/**
	 * all rights
	 */
	SUPER_USER;

	public static final UserRightEnum LAST = SUPER_USER;
	public static final int SIZE = LAST.ordinal() + 1;

	/**
	 * This tests whether this is higher than the provided parameter
	 *
	 * @param c
	 *            is the value to compare
	 * @return true if this is higher than the parameter, false otherwise
	 */
	public boolean higherThan(final UserRightEnum c) {
		return this.ordinal() > c.ordinal();
	}

	/**
	 * This tests whether this is higher or equal than the provided parameter
	 *
	 * @param c
	 *            is the value to compare
	 * @return true if this is higher or equal than the parameter, false
	 *         otherwise
	 */
	public boolean higherOrEquals(final UserRightEnum c) {
		return this.ordinal() >= c.ordinal();
	}

	/**
	 * This tests whether this is lower than the provided parameter
	 *
	 * @param c
	 *            is the value to compare
	 * @return true if this is lower than the parameter, false otherwise
	 */
	public boolean lowerThan(final UserRightEnum c) {
		return this.ordinal() < c.ordinal();
	}

	/**
	 * This tests whether this is lower or equal than the provided parameter
	 *
	 * @param c
	 *            is the value to compare
	 * @return true if this is lower or equal than the parameter, false
	 *         otherwise
	 */
	public boolean lowerOrEquals(final UserRightEnum c) {
		return this.ordinal() <= c.ordinal();
	}

	/**
	 * This tests whether this equals the provided parameter
	 *
	 * @param c
	 *            is the value to compare
	 * @return true if this equals the parameter, false otherwise
	 */
	public boolean doesEqual(final UserRightEnum c) {
		return this.ordinal() == c.ordinal();
	}
	
	/**
	 * This tests whether this represents a worker right
	 * @return true if so
	 * since 11.3.0
	 */
	public boolean isWorker() {
		return this.doesEqual(WORKER_USER) || this.doesEqual(UserRightEnum.VWORKER_USER);
	}

	/**
	 * This retrieves an Columns from its integer value
	 *
	 * @param v
	 *            is the integer value of the Columns
	 * @return an Columns
	 */
	public static UserRightEnum fromInt(final int v) throws IndexOutOfBoundsException {
		for (final UserRightEnum c : UserRightEnum.values()) {
			if (c.ordinal() == v) {
				return c;
			}
		}
		throw new IndexOutOfBoundsException("unvalid status " + v);
	}

	public static void main(final String[] argv) {
		for (final UserRightEnum r : UserRightEnum.values()) {
			System.out.println(r.toString() + " = " + r.ordinal());
		}
	}

}
