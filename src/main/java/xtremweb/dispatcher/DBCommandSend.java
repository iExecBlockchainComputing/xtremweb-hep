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

import java.io.IOException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import xtremweb.common.CommCallback;
import xtremweb.common.XMLable;
import xtremweb.communications.IdRpc;
import xtremweb.communications.XMLRPCCommand;

/**
 * @author Oleg Lodygensky
 * @since 11.5.0
 */

public abstract class DBCommandSend extends DBCommand implements CommCallback {

	protected DBInterface dbInterface;
	protected XMLRPCCommand rpc;

	public static DBCommandSend newCommand(final IdRpc rpc) throws IOException, ClassCastException {
		switch(rpc) {
		case SENDAPP:
			return new DBCommandSendApp();
		case SENDDATA:
			return new DBCommandSendData();
		case SENDGROUP:
			return new DBCommandSendGroup();
		case SENDSESSION:
			return new DBCommandSendSession();
		case SENDUSER:
			return new DBCommandSendUser();
		case SENDUSERGROUP:
			return new DBCommandSendUsergroup();
		case SENDWORK:
			return new DBCommandSendWork();
		case SENDCATEGORY:
			return new DBCommandSendCategory();
		}

		throw new ClassCastException("invalid RPC " + rpc);
	}

	public DBCommandSend() throws IOException {
		super();
	}
	public DBCommandSend(final DBInterface dbi) throws IOException {
		super();
		dbInterface = dbi;
	}
	public void setDBInterface(final DBInterface dbi) {
		dbInterface = dbi;
		System.out.println("DBCommandSend.setDBInterface " + dbInterface);
	}
	public abstract XMLable exec(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException;
}
