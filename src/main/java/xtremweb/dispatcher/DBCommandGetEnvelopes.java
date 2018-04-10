/*
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

package xtremweb.dispatcher;

import xtremweb.common.*;
import xtremweb.communications.XMLRPCCommand;
import xtremweb.communications.XMLRPCCommandGetWorks;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Vector;

/**
 * @author Oleg Lodygensky
 * @since 13.0.0
 */

public final class DBCommandGetEnvelopes extends DBCommand implements CommCallback {

	private DBInterface dbInterface;
	/**
	 * This instantiates a DBConnPoolThread, update application pools and set
	 * default SQLRequest attributes
	 *
	 * @see xtremweb.database.DBConnPoolThread
	 * @see xtremweb.database.SQLRequest#setDbName(String)
	 * @see xtremweb.database.SQLRequest#setHsqldb(boolean)
	 */
	public DBCommandGetEnvelopes(final DBInterface dbi) throws IOException {
		super();
		dbInterface = dbi;
	}

	/**
	 * This is called by client to disconnect from server This deletes the
	 * client sessions
	 *
	 * @param command is the received command
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public XMLable exec(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		mileStone.println("<getenvelopes>");
		final Vector<UID> ret = (Vector<UID>) dbInterface.getAllEnvelopes(command);
		XMLVector v = null;
		if (ret != null) {
			v = new XMLVector(ret);
		}
		mileStone.println("</getenvelopes>");

		return v;
	}
}
