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
 * CommQueue.java
 *
 *
 * Created: Thu Nov 15 14:39:07 2001
 *
 * @author <a href="mailto: "Gilles Fedak</a>
 */

/**
 * This abstract class defines message container : a class that holds messages
 * to compute
 */
abstract public class CommQueue {

	/**
	 * This defines maximum number of simultaneous messages. Default value is 2
	 */
	public static final int MAX_COMMEVENT_INPROGRESS = 2;

	/** This insert a work request in message queue */
	abstract public void workRequest();

	/** This inserts a send result in message queue */
	abstract public void sendResult(Work w);

	/**
	 * This inserts a send work in message queue
	 *
	 * @since 8.3.0
	 */
	abstract public void sendWork(Work w);

	/**
	 * This retreives the next message from queue
	 *
	 * @return the next available message, or null if none
	 */
	abstract public CommEvent getCommEvent();

	/**
	 * This removes the message from queue
	 *
	 * @param ce
	 *            is the message to remove
	 */
	abstract public void removeCommEvent(CommEvent ce);

	/**
	 * This returns the number of events in queue
	 *
	 * @return the number of events in queue
	 */
	abstract public int size();

	/**
	 * This returns the number of events of the given type in queue
	 *
	 * @param type
	 *            is the type of the event
	 * @return the number of events of the given type in queue
	 */
	abstract public int size(int type);
}
