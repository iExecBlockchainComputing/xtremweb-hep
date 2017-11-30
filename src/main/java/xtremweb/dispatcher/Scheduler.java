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

import xtremweb.common.HostInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;

import java.io.IOException;
import java.util.Collection;

/**
 * This is the abstract class that defines XtremWeb scheduler main methods set.
 * <br />
 * Since XWHEP 5.8.0 this abstract class defines two responsabilities
 * <ul>
 * <li>retreive job from DB (this is the retreive() method)
 * <li>select a job for a worker (this is the select() method)
 * </ul>
 */
public abstract class Scheduler {

	protected Scheduler() { }

	/**
	 * This retreives waiting jobs from DB
	 *
	 * @return null if no work available; a MobileWork otherwise
	 * @since 5.8.0
	 */
	public abstract Collection<WorkInterface> retrieve() throws IOException;

	/**
	 * This tries to find a job that matches the given host and host owner. The
	 * found job must:
	 * <ul>
	 * <li>match the host definition (arch, OS etc)
	 * <li>the host owner must have the right to execute the job
	 * </ul>
	 *
	 * @param host
	 *            is the worker definition
	 * @param user
	 *            is the worker identity
	 * @return a Work matching host; null if no work matches this host -or no
	 *         pending work- found
	 */
	public abstract WorkInterface select(HostInterface host, UserInterface user) throws IOException;

}
