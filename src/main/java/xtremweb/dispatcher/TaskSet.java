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

package xtremweb.dispatcher;

import org.apache.log4j.Logger;

import xtremweb.common.XWPropertyDefs;

/**
 * The <CODE>TaskSet</CODE> is an abstract class which defines the minimum
 * needed methods set to manage XtremWeb tasks.
 */

public abstract class TaskSet extends Thread {
	private static final Logger logger = Logger.getLogger(TaskSet.class);
	/**
	 * This stores the tasks set status
	 *
	 * @see #isReady()
	 */
	private boolean ready = false;

	/**
	 *
	 */
	public TaskSet() {
		super("TaskSet");
	}

	/**
	 * This tells whether the task set is ready The taskset is ready when tasks
	 * have been read from DB, ckecked and up to dated to a corret status
	 *
	 * @return true if tasks set is ready
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * This retreives WAITING jobs from DB, creates a Task iin DB and set their
	 * status to PENDING
	 */
	protected abstract void refill();

	/**
	 * This detects lost jobs and set their status to PENDING for rescheduling.
	 * A job is lost after 3 alive periods whithout heart beat signal from
	 * worker
	 */
	protected abstract void detectAbortedTasks();

	/**
	 * This is the main method. This is an infinite loop that detects aborted
	 * tasks and retreive WAITING jobs The loop then sleeps for ALIVE seconds
	 *
	 * @see xtremweb.common.XWPropertyDefs#ALIVEPERIOD
	 */
	@Override
	public void run() {
		try {
			int timeout = Integer.parseInt(Dispatcher.getConfig().getProperty(XWPropertyDefs.ALIVEPERIOD));
			timeout *= 1000;

			logger.info("TaskSet started (" + timeout + ")");
			while (true) {
				detectAbortedTasks();
				refill();
				ready = true;

				sleep(timeout);
			}
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
	}
}
