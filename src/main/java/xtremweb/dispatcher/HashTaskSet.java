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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import xtremweb.common.AppInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.StatusEnum;
import xtremweb.common.Table;
import xtremweb.common.TaskInterface;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;

/**
 * HashTaskSet.java
 *
 * Created: Wed Aug 23 14:17:05 2000
 *
 * @author Gilles Fedak
 * @version %I% %G%
 */

public class HashTaskSet extends TaskSet {

	public HashTaskSet() {
		super();
	}

	/**
	 * This retrieves WAITING jobs and set status to PENDING This retrieves
	 * associated tasks, if any, and set their status to ERROR
	 */
	@Override
	protected void refill() {
		final DBInterface db = DBInterface.getInstance();
		final Date now = new Date();
		try {
			final Vector<Table> rows = new Vector<>();

			final Collection<WorkInterface> works = Dispatcher.getScheduler().retrieve();
			if (works == null) {
				getLogger().debug("refill works is null");
				return;
			}

			getLogger().debug("refill size = " + works.size());

			for (final Iterator<WorkInterface> worksEnum = works.iterator(); worksEnum.hasNext();) {

				try {
					final WorkInterface theWork = worksEnum.next();
					if (theWork == null) {
						continue;
					}

					getLogger().debug("refill = " + theWork.getUID());

					theWork.setPending();

					final Collection<TaskInterface> tasks = db.tasks(theWork);
					if (tasks != null) {
						for (final Iterator<TaskInterface> tasksEnum = tasks.iterator(); tasksEnum.hasNext();) {
							final TaskInterface theTask = tasksEnum.next();

							theTask.setError();
							theTask.setRemovalDate(now);
							rows.add(theTask);
						}
					}

					rows.add(theWork);
					db.update(rows);
				} catch (final Exception e) {
					getLogger().exception(e);
				}
			}
		} catch (final Exception e) {
			getLogger().exception(e);
		}
	}

	/**
	 * This checks if given task is lost. A task is lost if (status == RUNNIG)
	 * || (status == DATAREQUEST) || (status == RESULTREQUEST) and if alive
	 * signal not received after 3 alive periods. If lost, the task is set to
	 * ERROR and a new PENDING task is created.
	 */
	private void detectAbortedTask(final TaskInterface theTask) {

		final DBInterface db = DBInterface.getInstance();
		final Date now = new Date();
		try {
			if ((theTask == null) || (theTask.getLastAlive() == null)) {
				return;
			}

			final int delay = (int) (System.currentTimeMillis() - theTask.getLastAlive().getTime());

			int aliveTimeOut = Integer
					.parseInt(Dispatcher.getConfig().getProperty(XWPropertyDefs.ALIVETIMEOUT.toString()));
			aliveTimeOut *= 1000;

			if ((theTask.isRunning() || theTask.isDataRequest() || theTask.isResultRequest())
					&& (delay > aliveTimeOut)) {

				final WorkInterface theWork = db.work(theTask.getWork());
				if (theWork == null) {
					getLogger().warn("No work found for task ; deleting " + theTask.getUID());
					theTask.delete();
					return;
				}

				theWork.lost(XWTools.getLocalHostName());

				switch (theTask.getStatus()) {
					case RUNNING:
					case DATAREQUEST:
					case CONTRIBUTED:
					case CONTRIBUTING:
					case REVEALING:
					case RESULTREQUEST:
					theWork.setErrorMsg("rescheduled : worker lost");
					break;
				}

				theTask.setError();
				theTask.setRemovalDate(now);

				final Vector<Table> rows = new Vector<>();

				final UID hostUID = theTask.getHost();
				if (hostUID != null) {
					final HostInterface theHost = db.host(hostUID);
					if (theHost != null) {
						theHost.decRunningJobs();
						theHost.leaveMarketOrder();
						rows.add(theHost);
					}
				}
				final UID ownerUID = theWork.getOwner();
				if (ownerUID != null) {
					final UserInterface theUser = db.user(ownerUID);
					if (theUser != null) {
						theUser.decRunningJobs();
						rows.add(theUser);
					}
				}
				final UID appUID = theWork.getApplication();
				if (appUID != null) {
					final AppInterface theApp = db.app(appUID);
					if (theApp != null) {
						theApp.decRunningJobs();
						rows.add(theApp);
					}
				}

				rows.add(theWork);
				rows.add(theTask);
				db.update(rows);
			}
		} catch (final Exception e) {
			getLogger().exception("detecAbortedTasks_unitary : can't set tasks lost", e);
		}
	}

	/**
	 * This are the status we must monitor to detect lost tasks
	 *
	 * @since 8.2.0
	 */
	private enum abortedStatus {
		RUNNING(StatusEnum.RUNNING), DATAREQUEST(StatusEnum.DATAREQUEST), RESULTREQUEST(StatusEnum.RESULTREQUEST);

		private final StatusEnum status;

		private abortedStatus(final StatusEnum s) {
			status = s;
		}

		public StatusEnum getStatus() {
			return status;
		}
	};

	/**
	 * This retrieves RUNNING or DATAREQUEST or RESULTREQUEST tasks and calls
	 * detectAbortedTask(Task) for each
	 *
	 * @see #detectAbortedTask(TaskInterface)
	 */
	@Override
	protected void detectAbortedTasks() {
		final DBInterface db = DBInterface.getInstance();
		for (final abortedStatus s : abortedStatus.values()) {

			try {
				final Collection<TaskInterface> tasks = db.tasks(s.getStatus());
				getLogger().debug("detectAbortedTasks " + s + " = " + (tasks == null ? "null" : tasks.size()));
				if (tasks != null) {
					for (final Iterator<TaskInterface> enumeration = tasks.iterator(); enumeration.hasNext();) {
						final TaskInterface theTask = enumeration.next();
						if (theTask == null) {
							continue;
						}
						detectAbortedTask(theTask);
					}
				}
			} catch (final Exception e) {
				getLogger().exception(e);
			}
		}
	}

	/**
	 * This converts this object to string
	 */
	@Override
	public String toString() {

		String s = "";
		try {
			final Collection<WorkInterface> works = DBInterface.getInstance().works();
			if (works == null) {
				return null;
			}
			for (final Iterator<WorkInterface> enumeration = works.iterator(); enumeration.hasNext();) {
				final WorkInterface theWork = enumeration.next();
				s += theWork.toString() + "\n";
			}
			return s;
		} catch (final Exception e) {
			s = null;
		}
		return s;
	}

}
