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

import org.apache.log4j.Logger;

import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;

/**
 * <p>
 * Abtract class for implementing polling activators
 * </p>
 *
 * <p>
 * <code>PollingActivator</code> contains methods common to activators that
 * polls the system to see if the worker is allowed to compute<br>
 * Subclasses needs to implement the <code>isActive</code>method. This method is
 * called every <code>waitingProbeInterval</code> milliseconds when the worker
 * is sleeping. If it has returned false for </code>activationDelay</code> ms,
 * then the worker is awaken and <code>isActive</code> is called every
 * <code>workingProbeInterval</code> ms to see if the worker should be stopped.
 * </p>
 *
 * Created May 30 2002 (from MouseKbdActivator.java)
 *
 * @author Samuel Heriard
 */

public abstract class PollingActivator extends Activator {

	private static final Logger logger = Logger.getLogger(PollingActivator.class);

	/** milliseconds to wait between each probe when the worker is active */
	private long workingProbeInterval = 1000;
	/** milliseconds to wait between each probe when waiting for activation */
	private long waitingProbeInterval = 10000;
	/**
	 * number of millisecond of inactivity to wait before allowing the worker to
	 * start this can be set with the property activator.ua.delay (give the time
	 * in seconds)
	 */
	private int activationDelay = 0;

	/**
	 * This tells whether the worker can start computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work can start computing
	 */
	protected abstract boolean canStart();

	/**
	 * This tells whether the worker must stop computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work must stop computing
	 */
	protected abstract boolean mustStop();

	/**
	 * This initializes the activator accordingly to the config file
	 *
	 * @param c
	 *            is the Properties read from file
	 * @exception Exception
	 *                is thrown on initialization error, depending on the
	 *                Acivator implementation
	 */
	@Override
	public void initialize(final XWConfigurator c) {

		super.initialize(c);

		try {
			if (getConfig().getProperty(XWPropertyDefs.ACTIVATORDELAY) != null) {
				setActivationDelay(60000 * Integer.parseInt(getConfig().getProperty(XWPropertyDefs.ACTIVATORDELAY)));
			}
		} catch (final NumberFormatException e) {
			logger.warn("'activator.poll.delay' is not an integer ("
					+ getConfig().getProperty(XWPropertyDefs.ACTIVATORDELAY) + ")");
		}

		if (getActivationDelay() <= 0) {
			setActivationDelay(60000);
		}

		logger.debug("PollingActivator::initilize() " + getActivationDelay());

		setMask(~CPU_ACTIVITY);
	}

	/**
	 * This wait for an event
	 *
	 * @param mask
	 *            is the event mask to wait for
	 */
	@Override
	public int waitForEvent(final int mask) throws InterruptedException {
		try {
			if ((getMask() & CPU_ACTIVITY) != 0) {

				// xtremweb running
				logger.warn("PollingActivator : running");

				while (this.mustStop() == false) {
					Thread.sleep(getWorkingProbeInterval());
				}

				setMask(getMask() - CPU_ACTIVITY);
				logger.debug("PollingActivator (end running) : " + Integer.toHexString(getMask()));

			} else {

				// xtremweb not running
				logger.warn("PollingActivator : sleeping");

				Thread.sleep(getWaitingProbeInterval());
				while (this.canStart() == false) {
					logger.debug("PollingActivator : sleeping " + getWaitingProbeInterval());
					Thread.sleep(getWaitingProbeInterval());
				}

				setMask(getMask() | CPU_ACTIVITY);
				logger.debug("PollingActivator (end sleeping) : " + Integer.toHexString(getMask()));
			}
		} catch (final InterruptedException e) {
			logger.info("PollingActivator#waitForEvent() : interrupted");
			throw e;
		}
		return CPU_ACTIVITY;
	}

	/**
	 * @return the activationDelay
	 */
	public int getActivationDelay() {
		return activationDelay;
	}

	/**
	 * @param activationDelay
	 *            the activationDelay to set
	 */
	public void setActivationDelay(final int activationDelay) {
		this.activationDelay = activationDelay;
	}

	/**
	 * @return the waitingProbeInterval
	 */
	public long getWaitingProbeInterval() {
		return waitingProbeInterval;
	}

	/**
	 * @param waitingProbeInterval
	 *            the waitingProbeInterval to set
	 */
	public void setWaitingProbeInterval(final long waitingProbeInterval) {
		this.waitingProbeInterval = waitingProbeInterval;
	}

	/**
	 * @return the workingProbeInterval
	 */
	public long getWorkingProbeInterval() {
		return workingProbeInterval;
	}

	/**
	 * @param workingProbeInterval
	 *            the workingProbeInterval to set
	 */
	public void setWorkingProbeInterval(final long workingProbeInterval) {
		this.workingProbeInterval = workingProbeInterval;
	}

}
