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

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import xtremweb.common.AppInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.Table;
import xtremweb.common.TableColumns;
import xtremweb.common.TaskInterface;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommandWorkRequest;
import xtremweb.database.DBConnPoolThread;
import xtremweb.database.SQLRequest;
import xtremweb.database.SQLRequestWorkRequest;
import xtremweb.database.SQLRequestWorkRequestDataDriven;

/**
 * This implements a scheduler which selects a job for the expected worker. This
 * helps to broadcast as well as to use the WorkInterface#expextedhost field.
 * This also helps to check whether the identity (user definition) under which
 * the worker is running, has the right to execute the job.
 *
 * A private worker can execute public, group and private jobs of the worker
 * owner itself.<br />
 * A group worker belongs to an user group and can execute public and group jobs
 * of the worker owner group.<br />
 * A public worker can execute public jobs only.
 *
 * @see xtremweb.common.WorkInterface
 * @since RPCXW
 */

public class MatchingScheduler extends SimpleScheduler {

	/**
	 * This constructor only calls its parent constructor
	 */
	public MatchingScheduler() {
		super();
	}

	/**
	 * This retrieves a waiting work for the requesting host using
	 * SQLRequestWorkRequest. This first updates work, otherwise scheduler may
	 * return the same work several times. Then this can safelly update a vector
	 * of rows
	 *
	 * @return a Work matching host; null if no work matches this host -or no
	 *         pending work- found
	 * @exception IOException
	 *                is thrown on error
	 * @see SQLRequestWorkRequest
	 */
	@Override
	public synchronized WorkInterface select(final XMLRPCCommandWorkRequest command) throws IOException {

		final HostInterface host = command.getHost();
		final UserInterface user = command.getUser();

		if ((host == null) || (user == null)) {
			notify();
			throw new IOException("MatchingScheduler#select() param error");
		}

		getMileStone().println("<select>");

		IOException ioe = null;
		WorkInterface theWork = null;
		TaskInterface theTask = null;

		final DBInterface db = DBInterface.getInstance();

		try {
			String moreCriterias = null;
			final SQLRequestWorkRequest workRequest = new SQLRequestWorkRequest(host, user);
			final WorkInterface workSelection = new WorkInterface(workRequest);

			final URI jobId = host.getJobId();
			final URI batchId = host.getBatchId();

			if (jobId != null) {
				final UID uid = jobId.getUID();
				if (uid != null) {
                    moreCriterias = SQLRequest.MAINTABLEALIAS + "." + TableColumns.UID + "='" + uid + "'";
				}
			} else if (batchId != null) {
				final UID uid = batchId.getUID();
				if (uid != null) {
                    moreCriterias = SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.GROUPUID + "='" + uid + "'";
				}
			}

			getLogger().debug("host      = " + host.toXml());
			getLogger().debug("criterias = " + moreCriterias);
			theWork = db.selectOne(workSelection, moreCriterias);

			if(theWork == null) {
				final SQLRequestWorkRequestDataDriven dataDrivenWorkRequest = new SQLRequestWorkRequestDataDriven(host, user);
				final WorkInterface dataDrivenWorkSelection = new WorkInterface(dataDrivenWorkRequest);
				theWork = db.selectOne(dataDrivenWorkSelection, moreCriterias);
			}

			if (theWork != null) {
                final Collection<Table> rows = new Vector<>();
                final UID theAppUID = theWork.getApplication();
				final UID theWorkOwnerUID = theWork.getOwner();
				final AppInterface theApp = db.app(user, theAppUID);
				final UserInterface theWorkOwner = db.user(theWorkOwnerUID);
				theApp.decPendingJobs();
				theApp.incRunningJobs();
				theWorkOwner.decPendingJobs();
				theWorkOwner.incRunningJobs();
				host.incRunningJobs();
				theWork.setRunning();
				theTask = new TaskInterface(theWork);
				theWork.setRunning();
				theTask.setRunningBy(host.getUID());

				//
				// 20 juin 2011
				// We must first update work, otherwise scheduler may return
				// the same work several times.
				// Then we can update all others
				//
				db.putToCache(theWork);
				DBConnPoolThread.getInstance().update(theWork, null, false);

				rows.add(host);
				rows.add(theTask);
				rows.add(theApp);
				rows.add(theWorkOwner);
				db.update(rows);
			}
		} catch (final Exception e) {
			getLogger().exception(e);
			ioe = new IOException(e.toString());
			if (theWork != null) {
				theWork.setError("sched error " + e);
				db.update(theWork);
			}
			if (theTask != null) {
				theTask.setError();
				final Date now = new Date();
				theTask.setRemovalDate(now);
				db.update(theTask);
			}
		} finally {
			theTask = null;

			getMileStone().println("</select>");
			notify();

			if (ioe != null) {
				theWork = null;
				getMileStone().println("<error msg=" + ioe.toString() + " /></select>");
				throw ioe;
			}
		}


		return theWork;

	}
}
