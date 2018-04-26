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
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import xtremweb.common.XMLable;
import xtremweb.communications.XMLRPCCommand;
import xtremweb.communications.XMLRPCCommandSendApp;

/**
 * @author Oleg Lodygensky
 * @since 11.0.0
 */

public final class DBCommandSendApp extends DBCommandSend {

	public DBCommandSendApp() throws IOException {
		super();
	}

	public DBCommandSendApp(final DBInterface dbi) throws IOException {
		super(dbi);
	}

	public XMLable exec(final XMLRPCCommandSendApp command)
			throws IOException, InvalidKeyException, AccessControlException {

		try {
			mileStone.println("<sendapp>");
			dbInterface.addApp(command);
			dbInterface.updateAppsPool();
		} catch (URISyntaxException e) {
			mileStone.println("<error msg =\"" + e.getMessage() + " \"/>");
		}

		mileStone.println("</sendapp>");
		return null;
	}


	@Override
	public XMLable exec(final XMLRPCCommand cmd) throws IOException, InvalidKeyException, AccessControlException {
		return this.exec((XMLRPCCommandSendApp)cmd);
	}
}
