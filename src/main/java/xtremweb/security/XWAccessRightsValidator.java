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
package xtremweb.security;

import java.io.IOException;
import java.security.AccessControlException;

import org.apache.log4j.Logger;


import xtremweb.common.Table;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;

/**
 * This grants access to objects
 *
 * Created: Oct 14, 2014
 *
 * @since 9.0.7
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

public class XWAccessRightsValidator {

	private static final Logger logger = Logger.getLogger(XWAccessRightsValidator.class);

	/**
	 * This is the row to protect
	 */
	private final Table row;

	public XWAccessRightsValidator(final Table r) {
		row = r;
	}

	/**
	 * This tests user access rights
	 *
	 * @param user
	 *            is the user to check right for
	 * @param rights
	 *            is the rights to test
	 * @return true if the user is this owner and access rights are defined
	 */
	final private boolean checkUserRights(final UserInterface user, final XWAccessRights rights)
			throws AccessControlException, IOException {

		if ((row == null) || (user == null)) {
			logger.trace("XWAccessRights#checkUserRights() user is null");
			return false;
		}
		final UID thisUid = row.getUID();
		final UID thisOwner = row.getOwner();
		final UID userUid = user.getUID();
		if ((thisOwner.equals(userUid)) || (userUid.equals(thisUid))) {
			logger.trace("XWAccessRights#checkUserRights() : owner equals user " + row.getAccessRights() + " & " + rights
					+ " & " + XWAccessRights.USERALL + " = "
					+ ((row.getAccessRights().value() & rights.value() & XWAccessRights.USERALL.value()) != 0));
			return ((row.getAccessRights().value() & rights.value() & XWAccessRights.USERALL.value()) != 0);
		}
		logger.trace("XWAccessRights#checkUserRights() user and owner differ : returns false");
		return false;
	}

	/**
	 * This tests user group access rights
	 *
	 * @param ownerGroup
	 *            is the owner group UID
	 * @param userGroup
	 *            is the group UID of the user who try to access
	 * @param rights
	 *            is the rights to test
	 * @return true if the user belongs to this owner group and group has access
	 *         rights
	 */
	final private boolean checkGroupRights(final UID ownerGroup, final UID userGroup, final XWAccessRights rights)
			throws AccessControlException, IOException {

		if (ownerGroup != null) {
			if ((userGroup == null) || (ownerGroup.equals(userGroup) == false)) {
				logger.trace("XWAccessRights#checkGroupRights() : returns false");
				return false;
			}
		}

		final String f = String.format("XWAccessRights#checkGroupRights(%s) : %x & %x & %x = %s", row.getUID(), row.getAccessRights().value(),
				rights.value(), XWAccessRights.GROUPALL.value(),
				"" + ((row.getAccessRights().value() & rights.value() & XWAccessRights.GROUPALL.value()) != 0));
		logger.trace(f);
		return ((row.getAccessRights().value() & rights.value() & XWAccessRights.GROUPALL.value()) != 0);
	}

	/**
	 * This tests others access rights
	 *
	 * @param rights
	 *            is the rights to test
	 * @return true if others have access rights
	 */
	final private boolean checkOthersRights(final XWAccessRights rights) throws AccessControlException, IOException {

		final String f = String.format("XWAccessRights#checkOtherRights(%s) %x & %x & %x = %s ", row.getUID(), row.getAccessRights().value(),
				rights.value(),
				XWAccessRights.OTHERALL.value(),
				"" + ((row.getAccessRights().value() & rights.value() & XWAccessRights.OTHERALL.value()) != 0));
		logger.trace(f);

		return ((row.getAccessRights().value() & rights.value() & XWAccessRights.OTHERALL.value()) != 0);
	}

	/**
	 * This tests access rights
	 *
	 * @param user
	 *            is the user who try to read
	 * @param ownerGroup
	 *            is the group of the owner of this object
	 * @return userCanRead(user) || groupCanRead(ownerGroup, userGroup) ||
	 *         otherCanRead()
	 */
	public final boolean canRead(final UserInterface user, final UID ownerGroup)
			throws AccessControlException, IOException {
		if (user == null) {
			return false;
		}
		final UID userGroup = user.getGroup();
		final boolean readable = userCanRead(user) || groupCanRead(ownerGroup, userGroup) || otherCanRead();
		final boolean stickyBitAccess = ((user.getRights().doesEqual(UserRightEnum.VWORKER_USER)) 
				&& ((row.getAccessRights().value() & XWAccessRights.STICKYBIT_INT) == XWAccessRights.STICKYBIT_INT));
		final String f = String.format("XWAccessRights#canRead(%s) : %b || %b", row.getUID(), readable, stickyBitAccess);
		logger.trace(f);
		return readable || stickyBitAccess;
	}

	/**
	 * This tests access rights
	 *
	 * @param user
	 *            is the user who try to read
	 * @param ownerGroup
	 *            is the group of the owner of this object
	 * @return userCanWrite(user) || groupCanWrite(ownerGroup, userGroup) ||
	 *         otherCanWrite()
	 */
	public final boolean canWrite(final UserInterface user, final UID ownerGroup)
			throws AccessControlException, IOException {
		if (user == null) {
			return false;
		}
		final UID userGroup = user.getGroup();
		final boolean stickyBitAccess = ((user.getRights().doesEqual(UserRightEnum.VWORKER_USER)) 
				&& ((row.getAccessRights().value() & XWAccessRights.STICKYBIT_INT) == XWAccessRights.STICKYBIT_INT));
		final boolean writable = userCanWrite(user) || groupCanWrite(ownerGroup, userGroup) || otherCanWrite();
		final String f = String.format("XWAccessRights#canWrite(%s) : %b || %b", row.getUID(), writable, stickyBitAccess);
		logger.trace(f);
		return writable || stickyBitAccess;
	}

	/**
	 * This tests access rights
	 *
	 * @param user
	 *            is the user who try to read
	 * @param ownerGroup
	 *            is the group of the owner of this object
	 * @return userCanExec(user) || groupCanExec(ownerGroup, userGroup) ||
	 *         otherCanExec()
	 */
	public final boolean canExec(final UserInterface user, final UID ownerGroup)
			throws AccessControlException, IOException {
		if (user == null) {
			return false;
		}
		final UID userGroup = user.getGroup();
		final boolean executable = userCanExec(user) || groupCanExec(ownerGroup, userGroup) || otherCanExec();
		final boolean stickyBitAccess = ((user.getRights().doesEqual(UserRightEnum.VWORKER_USER)) 
				&& ((row.getAccessRights().value() & XWAccessRights.STICKYBIT_INT) == XWAccessRights.STICKYBIT_INT));
		final String f = String.format("XWAccessRights#canExec(%s) : %b || %b", row.getUID(), executable, stickyBitAccess);
		logger.trace(f);
		return executable || stickyBitAccess;
	}
	/**
	 * This tests if user can read
	 *
	 * @param user
	 *            is the UID of the user who try to read
	 * @return true if the user is this owner and acces rights are defined
	 */
	private final boolean userCanRead(final UserInterface user) throws AccessControlException, IOException {
		return checkUserRights(user, XWAccessRights.USERREAD);
	}

	/**
	 * This tests user group access rights
	 *
	 * @param ownerGroup
	 *            is the owner group UID
	 * @param userGroup
	 *            is the group UID of the user who try to access
	 * @return true if the user belongs to this owner group and group has access
	 *         rights
	 */
	private final boolean groupCanRead(final UID ownerGroup, final UID userGroup)
			throws AccessControlException, IOException {
		return checkGroupRights(ownerGroup, userGroup, XWAccessRights.GROUPREAD);
	}

	/**
	 * This tests other access rights
	 *
	 * @return true if others have access rights
	 */
	private final boolean otherCanRead() throws AccessControlException, IOException {
		return checkOthersRights(XWAccessRights.OTHERREAD);
	}

	/**
	 * This tests if user can write
	 *
	 * @param user
	 *            is the UID of the user who try to write
	 * @return true if the user is this owner and acces rights are defined
	 */
	private final boolean userCanWrite(final UserInterface user) throws AccessControlException, IOException {
		return checkUserRights(user, XWAccessRights.USERWRITE);
	}

	/**
	 * This tests user group access rights
	 *
	 * @param ownerGroup
	 *            is the owner group UID
	 * @param userGroup
	 *            is the group UID of the user who try to write
	 * @return true if the user belongs to this owner group and group has access
	 *         rights
	 */
	private final boolean groupCanWrite(final UID ownerGroup, final UID userGroup)
			throws AccessControlException, IOException {
		return checkGroupRights(ownerGroup, userGroup, XWAccessRights.GROUPWRITE);
	}

	/**
	 * This tests other access rights
	 *
	 * @return true if others have access rights
	 */
	private final boolean otherCanWrite() throws AccessControlException, IOException {
		return checkOthersRights(XWAccessRights.OTHERWRITE);
	}

	/**
	 * This tests if user can exec
	 *
	 * @param user
	 *            is the UID of the user who try to exec
	 * @return true if the user is this owner and acces rights are defined
	 */
	private final boolean userCanExec(final UserInterface user) throws AccessControlException, IOException {
		return checkUserRights(user, XWAccessRights.USEREXEC);
	}

	/**
	 * This tests user group access rights
	 *
	 * @param ownerGroup
	 *            is the owner group UID
	 * @param userGroup
	 *            is the group UID of the user who try to exec
	 * @return true if the user belongs to this owner group and group has access
	 *         rights
	 */
	private final boolean groupCanExec(final UID ownerGroup, final UID userGroup)
			throws AccessControlException, IOException {
		return checkGroupRights(ownerGroup, userGroup, XWAccessRights.GROUPEXEC);
	}

	/**
	 * This tests other access rights
	 *
	 * @return true if others have access rights
	 */
	private final boolean otherCanExec() throws AccessControlException, IOException {
		return checkOthersRights(XWAccessRights.OTHEREXEC);
	}

}
