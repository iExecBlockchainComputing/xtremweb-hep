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

package xtremweb.communications;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import xtremweb.common.XWReturnCode;

public class XWCommException extends Exception {

	private final XMLRPCResult result;

	public XWCommException(final XMLRPCResult r) {
		result = r;
	}

	public void authentication() throws InvalidKeyException {
		if (result.getReturnCode() == XWReturnCode.AUTHENTICATION) {
			throw new InvalidKeyException(result.getMessage());
		}
	}

	public void authorization() throws AccessControlException {
		if (result.getReturnCode() == XWReturnCode.AUTHORIZATION) {
			throw new AccessControlException(result.getMessage());
		}
	}

	public void disk() throws IOException {
		if (result.getReturnCode() == XWReturnCode.DISK) {
			throw new IOException(result.getMessage());
		}
	}

	public void wallClockTime() throws IOException {
		if (result.getReturnCode() == XWReturnCode.WALLCLOCKTIME) {
			throw new IOException(result.getMessage());
		}
	}

	@Override
	public String toString() {
		return result.toString();
	}
}
