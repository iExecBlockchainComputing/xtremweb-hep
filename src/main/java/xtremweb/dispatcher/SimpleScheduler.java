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

import org.apache.log4j.Logger;


import xtremweb.common.HostInterface;
import xtremweb.common.MileStone;
import xtremweb.common.StatusEnum;
import xtremweb.common.TaskInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.database.SQLRequestWorkRequest;

/**
 * This is a simple scheduler
 *
 * @author Gille Fedak
 */

public class SimpleScheduler extends Scheduler {

	private static final Logger logger = Logger.getLogger(SimpleScheduler.class);

	/**
	 * This aims to display some time stamps
	 */
	private MileStone mileStone;

	/**
	 * This does nothing
	 */
	public SimpleScheduler() {
		super();
		setMileStone(new MileStone(this.getClass()));
	}

	/**
	 * This retrieves waiting jobs from DB in FIFO mode
	 *
	 * @return null if no work available; a vector of works otherwise
	 * @since 5.8.0
	 * @see DBInterface#works(StatusEnum)
	 */
	@Override
	public Collection<WorkInterface> retrieve() throws IOException {
		return DBInterface.getInstance().works(StatusEnum.WAITING);
	}

	/**
	 * Get the first task that is pending
	 *
	 * @param host
	 *            is the worker definition
	 * @param user
	 *            is not used here (see MatchingScheduler)
	 * @return a Work matching host; null if no work matches this host -or no
	 *         pending work- found
	 * @exception IOException
	 *                is thrown on error
	 */
	@Override
	public synchronized WorkInterface select(final HostInterface host, final UserInterface user) throws IOException {
		final DBInterface db = DBInterface.getInstance();

		if (host == null) {
			throw new IOException("MatchingScheduler#select() host is null");
		}
		getMileStone().println("<select>");

		WorkInterface theWork = new WorkInterface(new SQLRequestWorkRequest(host, user));
		theWork = db.select(theWork);

		if (theWork == null) {
			getMileStone().println("</select>");
			logger.info("SimpleScheduler#select() can't find any work");
			return null;
		}

		// insert associated task
		final TaskInterface theTask = new TaskInterface(theWork);
		theWork.setRunning();
		theTask.setRunningBy(host.getUID());
		db.update(theWork);
		db.update(theTask);

		getMileStone().println("</select>");

		theWork = null;

		return theWork;
	}

	/**
	 * @return the mileStone
	 */
	public MileStone getMileStone() {
		return mileStone;
	}

	/**
	 * @param mileStone
	 *            the mileStone to set
	 */
	public final void setMileStone(final MileStone mileStone) {
		this.mileStone = mileStone;
	}
}
