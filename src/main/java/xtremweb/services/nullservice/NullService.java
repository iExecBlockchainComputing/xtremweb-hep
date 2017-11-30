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

package xtremweb.services.nullservice;

/**
 * Date    : Nov 20th, 2005 <br />
 * File    : NullService.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 * @since RPCXW
 */

import xtremweb.common.MileStone;

/**
 * This class does nothing. This is only an XtremWeb service test
 */
public class NullService implements Interface {

	/**
	 * This aims to display some time stamps
	 */
	MileStone mileStone;

	/**
	 * This constructs a new object
	 */
	public NullService() {
		mileStone = new MileStone(getClass());
	}

	/**
	 * This implements the Interface exec() method<br />
	 * This does nothing
	 *
	 * @return always 0
	 */
	@Override
	public int exec(final String cmdLine, final byte[] stdin, final byte[] dirin) {
		mileStone.println("XW NullService started");
		mileStone.println("XW NullService done");
		return 0;
	}

	/**
	 * This implements the Interface getResult() method
	 *
	 * @see xtremweb.services.Interface
	 * @return always null
	 */
	@Override
	public byte[] getResult() {
		return null;
	}
}
