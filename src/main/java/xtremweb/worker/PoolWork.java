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
 * PoolWork.java
 *
 *
 * Created: Sat Jun 02 14:29:49 2001
 *
 * @author <a href="mailto: fedak@lri.fr">Gilles Fedak</a>
 */

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import xtremweb.common.Logger;
import xtremweb.common.StatusEnum;
import xtremweb.common.UID;
import xtremweb.common.WorkInterface;
import xtremweb.common.XWTools;

/**
 * This class describes works managed by worker as a vector
 */
public class PoolWork {

	private final Logger logger;

	/** This is the vector of works (ready to be or beeing computed). */
	private final Hashtable<UID, Work> poolWorks;

	/**
	 * This stores works being saved.
	 *
	 * @since v1r2-rc0 (RPC-V)
	 */
    private final Hashtable<UID, Work> completedWorks;

	/**
	 * This is the default constructor. This erases any old data.
	 */
	public PoolWork() {

		logger = new Logger(this);

		poolWorks = new Hashtable<>();
        completedWorks = new Hashtable<>();

		try {
			final File worksDir = Worker.getConfig().getWorksDir();
			XWTools.deleteDir(worksDir);
			XWTools.checkDir(worksDir);
		} catch (final IOException e) {
			logger.exception(e);
			logger.fatal("unrecoverable exception " + e);
		}
	}

	/**
	 * This tests whether pool is full
	 *
	 * @return true is pool is full, false otherwise
	 */
	public synchronized boolean isFull() {
		printSize("PoolWork#isFull");
		final boolean ret = (poolWorks.size() >= Worker.getConfig().getWorkPoolSize());
		notifyAll();
		return ret;
	}

	private int lastSize = -1;

	private void printSize(final String msg) {
		if (lastSize != poolWorks.size()) {
			logger.debug(msg + " size = " + poolWorks.size() + " MAX = " + Worker.getConfig().getWorkPoolSize());
			lastSize = poolWorks.size();
		}
	}

	/**
	 * This calculates pool size
	 *
	 * @return an integer containing the pool size
	 */
	public synchronized int getSize() {
		final int ret = poolWorks.size();
		notifyAll();
		return ret;
	}

	/**
	 * This stores provided work to the list of computing ones; i.e. the list of
	 * works being computed by the worker. This is typically called as a new
	 * work is provided by the server
	 *
	 * @see CommManager#run()
	 * @param mw
	 *            is the description of the work to create
	 * @return the new inserted Work from the MobileWork
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public synchronized Work addWork(final WorkInterface mw) throws IOException {

		Work work;

		logger.debug("PoolWork::addWork(" + mw.getUID() + ")");
		work = new Work(mw);
		final Date d = new Date();
		work.setReadyDate(d);
		work.setDataReadyDate(null);
		work.setCompStartDate(null);
		work.setCompEndDate(null);
		poolWorks.put(work.getUID(), work);
		printSize("addWork");

		notifyAll();
		return work;
	}

	/**
	 * This stores the given work to the list of completed ones; i.e. the list of
	 * finished works which results are being saved to the coordinator This is
	 * called when provided work computation is finished and results can then
	 * been sent to the server.
	 *
	 * @param w
	 *            is the work to save
	 * @see #removeWork(UID)
	 * @since v1r2-rc0 (RPC-V)
	 */
	public synchronized void saveCompletedWork(final Work w) {

		final UID uid = w.getUID();
		if(uid == null) {
			logger.error("saveCompletedWork can't find uid???");
			notifyAll();
			return;
		}
		logger.debug("PoolWork::saveCompletedWork(" + uid + ") = " + w.toXml());
		if (completedWorks.get(uid) != null) {
			// this may happen on communication failure
			// then, work has already been saved, and we retry to upload result
			logger.debug(uid.toString() + " already saved");
			Thread.currentThread().dumpStack();
			if (poolWorks.remove(uid) != null) {
				logger.error(uid.toString() + " still in poolWorks???");
			}
			notifyAll();
			return;
		}

		completedWorks.put(uid, w);
		if (poolWorks.remove(uid) == null) {
			logger.error("saveCompletedWork can't remove element");
		}
		CommManager.getInstance().workRequest();
		notifyAll();
	}

	/**
	 * This retrieves completed works
	 *
	 * @return a vector containing the saving works
	 * @since v1r2-rc0(RPC-V)
	 */
    public synchronized Hashtable<UID, Work> getCompletedWorks() {
        return completedWorks;
    }

    /**
     * This retrieves all works
     *
     * @return a vector containing the saving being computed
     * @since v1r2-rc0(RPC-V)
     */
    public synchronized Hashtable<UID, Work> getAllRunningWorks() {
        return getWorks(true);
    }
    /**
     * This retrieves alive works
     *
     * @return a vector containing the saving being computed
     * @since v1r2-rc0(RPC-V)
     */
    public synchronized Hashtable<UID, Work> getAliveWorks() {
        return getWorks(false);
    }

    /**
     * This retrieves works
     * @param anything if false this does not retrieve non alive works
     * @return
     */
    public synchronized Hashtable<UID, Work> getWorks(final boolean anything) {

        if(anything) {
            logger.debug("PoolWork::getWorks() works size = " + poolWorks.size());
            return poolWorks;
        }

        final Hashtable<UID, Work> worksAlive = new Hashtable<>();
        final Enumeration<Work> poolWorksEnum = poolWorks.elements();

        while (poolWorksEnum.hasMoreElements()) {
            final Work w = poolWorksEnum.nextElement();
            logger.debug("PoolWork::getAliveWorks() running work " + w.getUID() + " is " + w.getStatus());
            if (w.isAlive()) {
                worksAlive.put(w.getUID(), w);
            }
        }

        logger.debug("PoolWork::getWorks() alive works size = " + worksAlive.size());
        return worksAlive;
    }

	/**
	 * This retrieves a completed work
	 *
	 * @param uid
	 *            is the task UID to retrieve
	 * @return the saving work or null if not found
	 * @since v1r2-rc0(RPC-V)
	 */
	public synchronized Work getCompletedWork(final UID uid) {

		if(uid == null)
			return null;

		final Work ret = completedWorks.get(uid);
		logger.debug("PoolWork::getCompletedWorks() : work " + (ret == null ? "not" : "") + " found " + uid);
		notifyAll();
		return ret;
	}

	/**
	 * This saves a non completed work
	 *
	 * @param w is the revealed work
	 * @since 13.1.0
	 */
	public synchronized void saveWorkUnderProcess(final Work w) {

		final UID uid = w.getUID();
		if(uid == null) {
			logger.error("saveWorkUnderProcess can't find uid???");
			notifyAll();
			return;
		}

		final Work ret = poolWorks.remove(uid);
		logger.debug("PoolWork::saveWorkUnderProcess(" + uid + ") : work " + (ret == null ? "not" : "") + " found in savings");
		logger.debug("PoolWork::saveWorkUnderProcess(" + uid + ") = " + w.toXml());
        poolWorks.put(uid, w);
		notifyAll();
	}

    public synchronized Hashtable<UID, Work> getWorksUnderProcess() {
        return poolWorks;
    }

    public synchronized Work getWorkUnderProcess(final UID uid) {

        if(uid == null)
            return null;

        final Work ret = poolWorks.get(uid);
        logger.debug("PoolWork::getWorkUnderProcess() : work " + (ret == null ? "not" : "") + " found " + uid);
        notifyAll();
        return ret;
    }

    /**
	 * This removes provided work from saving list. This is called when work
	 * results have been successfully saved by the server, or when the work is
	 * in unrecoverable error state.
	 *
	 * @param uid is the task UID to remove
	 */
	public synchronized void removeWork(final UID uid) {

		final Work ret = completedWorks.remove(uid);
		logger.debug("PoolWork::removeWork(" + uid + ") : work " + (ret == null ? "not" : "") + " found in savings");
        Thread.currentThread().dumpStack();

		if (ret != null) {
			ret.clean(false);
		}
		notifyAll();
	}

	/**
	 * This removes provided work from all list This is called when job has been
	 * killed.
	 *
	 * @since 5.7.7
	 * @param uid
	 *            is the task UID to remove
	 */
	public synchronized void removeKilledWork(final UID uid) {

		Work ret = completedWorks.remove(uid);
		logger.debug("PoolWork::removeWork(" + uid + ") : work " + (ret == null ? "not" : "") + " found in savings");
        Thread.currentThread().dumpStack();
		//
		// maybe job has been killed then it has not been saved
		//
		ret = poolWorks.remove(uid);
		logger.debug("PoolWork::removeWork(" + uid + ") : work " + (ret == null ? "not" : "") + " found in runnings");
		if (ret != null) {
			ret.clean();
		}
		notifyAll();
	}

	/**
	 * This retrieves a work being computing
	 *
	 * @param uid
	 *            is the work UID to retrieve
	 * @return the saving work or null if not found
	 * @since 8.2.0
	 */
//	public synchronized Work getAliveWorks(final UID uid) {
//		return poolWorks.get(uid);
//	}

	/**
	 * This retrieves the next available work to compute This returns only when
	 * a work to compute is found!
	 *
	 * @return next available work to compute if any
	 */
	public synchronized Work getNextWorkToCompute() {

		final Enumeration<Work> theEnumeration = poolWorks.elements();

		while (theEnumeration.hasMoreElements()) {

			final Work w = theEnumeration.nextElement();
			if (w.isPending()) {
				w.setStatus(StatusEnum.RUNNING);
				return w;
			}
		}

		return null;
	}

	@Override
	public synchronized String toString() {

		String workString = "";
		final Enumeration<Work> theEnumeration = poolWorks.elements();

		while (theEnumeration.hasMoreElements()) {

			final Work w = theEnumeration.nextElement();

			try {
				workString += " Work : " + w.getUID() + " " + w.getStatus().toString() + "\n";
			} catch (final Exception e) {
			}

		}

		notifyAll();
		return workString;
	}

}// PoolWork
