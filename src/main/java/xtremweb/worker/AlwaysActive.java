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

package xtremweb.worker;

/**
 *  AlwaysActive.java<br />
 *  This activator never suspends the worker; the worker is alays allowed to compute<br />
 *  Created on Fri Mar 22 2002.<br />
 *
 * @author heriard
 */

import xtremweb.common.XWConfigurator;
import org.apache.log4j.Logger;

import xtremweb.common.XWReturnCode;

public class AlwaysActive extends Activator {

	private static final Logger logger = Logger.getLogger(AlwaysActive.class);

	/** This is the default and only constructor */
	public AlwaysActive() {
		super();
	}

	@Override
	public void initialize(final XWConfigurator c) {
		super.initialize(c);
		setMask(ALL_ACTIVITY);
	}

	/**
	 * This returns null
	 *
	 * @return always null
	 */
	@Override
	public String getParams() {
		return null;
	}

	/**
	 * This waits for ever since this class is the <b>AlwaysActive</b> activator
	 * ;)
	 *
	 * @param filter
	 *            not used
	 * @return never
	 * @see Activator#waitForAllow(int)
	 * @exception InterruptedException
	 *                is never thrown
	 */
	@Override
	public final int waitForSuspend(final int filter) throws InterruptedException {

		while (true) {
			getMileStone().println("waitForSuspend");
			try {
				synchronized (this) {
					this.wait();
					notifyAll();
				}
			} catch (final IllegalMonitorStateException e) {
				logger.error("unrecoverable exception" + e);
				System.exit(XWReturnCode.FATAL.ordinal());
			}
		}
	}

	/**
	 * This waits nothing and returns immediatly since this class is the
	 * <b>AlwaysActive</b> activator ;)
	 *
	 * @param filter
	 *            is not used
	 * @return always 0
	 * @see Activator#waitForAllow(int)
	 * @exception InterruptedException
	 *                is never thrown
	 */
	@Override
	public final int waitForAllow(final int filter) throws InterruptedException {
		getMileStone().println("waitForAllow");
		return 0;
	}
}
