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

// Activator.java
//
// Created: Thu Mar 21 2002.

import org.apache.log4j.Logger;


import xtremweb.common.MileStone;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWReturnCode;

/**
 * Subclasses of the <CODE>Activator</CODE> class can be used to control the
 * activity of the worker. The name of the class to be used must be given in the
 * config file with the key <CODE>activator.class</CODE>. <BR>
 * If the activator object implements the <CODE>Runable</CODE> interface, the
 * worker will launch a new thread to run it.
 *
 * @author Samuel Heriard.
 *
 */

public abstract class Activator {

	public static final int CPU_ACTIVITY = 1;
	public static final int NETWORK_ACTIVITY = 1 << 1;
	public static final int DISK_ACTIVITY = 1 << 2;
	public static final int ALL_ACTIVITY = ~0;

	private int activityMask;

	private static final Logger logger = Logger.getLogger(Activator.class);

	/**
	 * This aims to display some time stamps
	 */
	private MileStone mileStone;

	/**
	 * @return the mileStone
	 */
	public MileStone getMileStone() {
		return mileStone;
	}

	/**
	 * This is the config properties.
	 */
	private XWConfigurator config;

	/**
	 * @param config
	 *            the config to set
	 */
	public void setConfig(final XWConfigurator config) {
		this.config = config;
	}

	/**
	 * @return the config
	 */
	public XWConfigurator getConfig() {
		return config;
	}

	/**
	 * This contains this activator parameter string
	 */
	private String params;

	public Activator() {

		try {
			mileStone = new MileStone(getClass());
		} catch (final Exception e) {
		}
		params = null;
	}

	/**
	 * The <CODE>initialize</CODE> method is invoked after the instantiation of
	 * the <CODE>Activator</CODE> object.
	 *
	 * @param c
	 *            a <CODE>Properties</CODE> object that can be used to configure
	 *            the activator. For now the global config of the worker is
	 *            passed.
	 * @exception Exception
	 *                is thrown on initialization error, depending on the
	 *                Acivator implementation
	 */
	public void initialize(final XWConfigurator c) {
		config = c;
	}

	/**
	 * This sets this activator parameters.
	 */
	public void setParams(final String p) {
		params = p;
	}

	/**
	 * This returns this activator parameters.
	 *
	 * @return a String containing this activator parameters
	 */
	public String getParams() {
		return params;
	}

	/**
	 * This does nothing
	 *
	 * @see CpuActivator#raz ()
	 */
	public void raz() {
	}

	/**
	 * Change the activity mask
	 *
	 * @param mask
	 *            new activity mask
	 */

	protected void setMask(final int mask) {
		activityMask = mask;
	}

	/**
	 * @return the actvity mask : <CODE>getMask()&XXX_ACTIVITY == 0</CODE> means
	 *         no XXX activity allowed.
	 */
	public int getMask() {
		return activityMask;
	}

	public final int getMask(final int filter) {
		return filter & getMask();
	}

	/**
	 * Check if activity is allowed.
	 *
	 * @param mask
	 *            : mask of activity to test
	 * @return true if all the activities specified are allowed
	 */
	public final boolean allowed(final int mask) {
		return ((~getMask() & mask) == 0);
	}

	/**
	 * Check if activity is allowed.
	 *
	 * @param mask
	 *            : mask of activity to test
	 * @return true if one of the activities specified is allowed
	 */
	public final boolean anyAllowed(final int mask) {
		return (getMask(mask) != 0);
	}

	/**
	 * wait for a bit in the activity mask to become 0
	 *
	 * @param filter
	 *            : watch only bits not nul in <CODE>filter</CODE>
	 * @return : the mask of watched bits that have changed
	 */
	public int waitForSuspend(final int filter) throws InterruptedException {
		final int inactive = ~getMask() & filter;
		if (inactive != 0) {
			return 0;
		} else {
			return this.waitForEvent(filter);
		}
	}

	/**
	 * wait for a bit in the activity mask to be 1
	 *
	 * @param filter
	 *            watch only bits not nul in <CODE>filter</CODE>
	 * @return the mask of watched bits that have changed (0 if an activity is
	 *         already allowed)
	 */
	public int waitForAllow(final int filter) throws InterruptedException {
		final int active = getMask() & filter;
		if (active != 0) {
			return 0;
		} else {
			return this.waitForEvent(filter);
		}
	}

	/**
	 * Wait for any change in activity mask.
	 *
	 * This method wait for the activator object to be notified until the
	 * activityMask changes.
	 *
	 * Subclasses can either override this method or let it unchanged and
	 * implement a <CODE>run</CODE> method to modify the activity mask and
	 * notify itself in a separate thread.
	 *
	 * @param mask
	 *            set the event to wait for
	 * @return : the mask of watched bits that have changed
	 */
	public int waitForEvent(final int mask) throws InterruptedException {
		final int oldm = getMask(mask);
		int newm = oldm;

		while (oldm == newm) {
			try {
				synchronized (this) {
					this.wait();
					notifyAll();
				}
			} catch (final IllegalMonitorStateException e) {
				logger.error("unrecoverable exception" + e);
				System.exit(XWReturnCode.FATAL.ordinal());
			}
			newm = getMask(mask);
		}
		// oldm xor newm, c'est quoi xor en java ?
		return (oldm | newm) & ~(oldm & newm);
	}
}
