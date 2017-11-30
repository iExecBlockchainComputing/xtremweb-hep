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
 * CommPool.java
 *
 *
 * Created: Sat Jun  9 13:55:12 2001
 *
 * @author <a href="mailto: fedak@lri.fr> "Gilles Fedak</a>
 */

import xtremweb.communications.IdRpc;

import java.util.Collection;
import java.util.Stack;
import java.util.Vector;

public class CommStack extends CommQueue {

	private final Stack<CommEvent> poolQueue;
	private final Collection<CommEvent> commEventInProgress;
	private int size;

	public CommStack() {
		size = 0;
		poolQueue = new Stack<>();
		commEventInProgress = new Vector<>(MAX_COMMEVENT_INPROGRESS);
	}

	@Override
	public void workRequest() {
		size++;
		poolQueue.push(new CommEvent(IdRpc.WORKREQUEST));
	}

	@Override
	public void sendResult(final Work w) {
		size++;
		poolQueue.push(new CommEvent(IdRpc.UPLOADDATA, w));
	}

	/**
	 * @since 8.3.0
	 */
	@Override
	public void sendWork(final Work w) {
		size++;
		poolQueue.push(new CommEvent(IdRpc.SENDWORK, w));
	}

	@Override
	public CommEvent getCommEvent() {
		if ((poolQueue.isEmpty()) || (commEventInProgress.size() > MAX_COMMEVENT_INPROGRESS)) {
			return null;
		}

		size--;
		final CommEvent ce = poolQueue.pop();
		commEventInProgress.add(ce);
		return ce;
	}

	@Override
	public void removeCommEvent(final CommEvent ce) {
		commEventInProgress.remove(ce);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int size(final int type) {
		return size;
	}

}
