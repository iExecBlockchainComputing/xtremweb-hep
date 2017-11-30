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

//  MouseKbdActivator.java
//
//  Created by heriard on Wed Apr 3 2002.

import xtremweb.archdep.ArchDepFactory;
import xtremweb.archdep.XWInterrupts;
import org.apache.log4j.Logger;


/**
 * <CODE>MouseKbdActivator</CODE> activator for that monitors keyboard and mouse
 * activity. This activator use XWInterrupts to monitor activity.
 *
 * @author Samuel Heriard
 *
 */

public class MouseKbdActivator extends PollingActivator {

	private static final Logger logger = Logger.getLogger(MouseKbdActivator.class);

	/** interrupt counters */
	private int lastKey = 0;
	private int lastMouse = 0;
	private boolean initialized = false;

	private int remains = getActivationDelay();
	private final XWInterrupts irq = ArchDepFactory.xwinterrupts();

	/**
	 * This is the default contructor This initializes IRQ counter
	 */
	public MouseKbdActivator() {
		super();
		initialized = irq.initialize();
		remains = getActivationDelay();
		if (remains <= 0) {
			remains = 60000;
		}
		logger.debug("MouseKbdActivator " + initialized + getActivationDelay() + " " + remains);
	}

	/** return true if the user is active */
	private boolean isActive() {
		int newKey;
		int newMouse;
		boolean differ = false;

		// TODO(shd): remove static reference to Worker.config
		// (move hasKey and hasMouse to XWInterrupts)

		if (Worker.getConfig().hasKeyboard()) {
			newKey = irq.readKey();
			differ = differ || (newKey != lastKey);
			logger.debug("isactive() key " + newKey + " " + lastKey + " " + differ);
			lastKey = newKey;
		}

		if (Worker.getConfig().hasMouse()) {
			newMouse = irq.readMouse();
			differ = differ || (newMouse != lastMouse);
			logger.debug("isactive() mouse " + newMouse + " " + lastMouse + " " + differ);
			lastMouse = newMouse;
		}

		return differ;
	}

	/**
	 * This tells whether the worker can start computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work can start computing
	 */
	@Override
	protected boolean canStart() {

		if (this.isActive()) {
			remains = getActivationDelay();
		} else {
			remains -= getWaitingProbeInterval();
		}
		logger.info("activation in " + remains + " ms");

		return (remains <= 0);

	}

	/**
	 * This tells whether the worker must stop computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work must stop computing
	 */
	@Override
	protected boolean mustStop() {
		final boolean ret = isActive();
		if (ret) {
			remains = getActivationDelay();
			lastKey--;
		}
		return ret;
	}

}
