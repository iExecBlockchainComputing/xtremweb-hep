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

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import xtremweb.common.Logger;
import xtremweb.common.UID;
import xtremweb.common.XWPropertyDefs;

/**
 * The <CODE>ThreadLaunch</CODE> class determines whether worker should compute
 * or not.
 *
 * @see Activator
 */

public final class ThreadLaunch extends Thread {

	private final Logger logger;

	/**
	 * This
	 */
	private static ThreadLaunch instance = null;

	/**
	 * This is the thread associated to the activator
	 */
	private Thread activatorThread;
	/**
	 * This is the policy activator which defines the activation policy (detects
	 * whether the worker can compute)
	 */
	private Activator activator = null;

	/**
	 * This retrieves the actual activator
	 */
	public Activator getActivator() {
		return activator;
	}

	/**
	 * This retrieves the actual activator name
	 */
	public String getActivatorName() {
		return activator.getClass().getName();
	}

	/**
	 * This sets the activator
	 */
	public void setActivator(final Activator act) {
		activator = act;
	}

	/**
	 * This contains the information reported by the activator. This is true if
	 * the worker is not available and must wait before it can compute. This is
	 * set to false if the worker can compute. This is sent to the dispatcher
	 * through the alive signal
	 */
	private boolean unavailable;

	private final Vector<ThreadWork> threadWorkPool;

	/**
	 * This is the default constructor
	 */
	public ThreadLaunch() throws InterruptedException, InstantiationException {

		super("ThreadLaunch");

		logger = new Logger(this);

		unavailable = true;

		threadWorkPool = new Vector<>();

		activatorThread = null;
		setupActivator();

		if (getInstance() == null) {
			setInstance(this);
		}
	}

	private boolean canRun = true;

	public void terminate() {
		canRun = false;
		this.interrupt();
	}

	/**
	 * This resets the activator, if any.
	 */
	public void raz() {
		if (activator != null) {
			activator.raz();
		}
	}

	public void wakeup() {
		for (final Iterator<ThreadWork> it = threadWorkPool.iterator(); it.hasNext();) {
			final ThreadWork threadWork = it.next();
			threadWork.wakeup();
		}
	}

	/**
	 * This is the main loop It waits for computation to be allowed, accordingly
	 * to activity policy and resume any suspended jobs; it then waits until
	 * computation is not allowed and suspend any running jobs. And it loops for
	 * ever.
	 */
	@Override
	public void run() {

		while (canRun) {

			try {

				activator.waitForAllow(Activator.CPU_ACTIVITY);
				logger.info("wait for allow");
				raz();

				//
				// we ask new jobs here only (see constructor comments)
				//
				for (int i = CommManager.getInstance().getPoolWork().getSize(); i < Worker.getConfig()
						.getWorkPoolSize(); i++) {
					CommManager.getInstance().workRequest();
				}

				synchronized (this) {
					unavailable = false;
					logger.info("allowed!");
					notify();
				}

				checkThreadPoolSanity();

				for (final Iterator<ThreadWork> it = threadWorkPool.iterator(); it.hasNext();) {
					ThreadWork threadWork = it.next();
					threadWork.resumeProcess();
					threadWork = null;
				}

				try {
					Thread.sleep(Long.parseLong(Worker.getConfig().getProperty(XWPropertyDefs.TIMEOUT)));
				} catch (final InterruptedException ie) {
					logger.info("interrupted");
				}

				logger.debug("wait for suspend");
				raz();
				activator.waitForSuspend(Activator.CPU_ACTIVITY);

				synchronized (this) {
					unavailable = true;
					logger.info("not allowed!");
					notify();
				}

				for (final Iterator<ThreadWork> it = threadWorkPool.iterator(); it.hasNext();) {
					ThreadWork threadWork = it.next();
					if (threadWork != null) {
						threadWork.suspendProcess();
					}
					threadWork = null;
				}
			} catch (final InterruptedException e) {
				logger.info("interrupted " + e.getMessage());
				unavailable = true;
			} catch (final RuntimeException e) {
				logger.exception("run time exception", e);
			}
		}
	}

	/**
	 * This tells whether this worker is available. This is deprecated and
	 * should not be used any longer.
	 *
	 * @return always false
	 * @deprecated
	 * @see #available()
	 */
	@Deprecated
	public boolean allowToCompute() {
		return false;
	}

	/**
	 * This tells whether this worker is available
	 *
	 * @return true if this worker is allowed to compute, accordingly to its
	 *         local activation policy
	 * @since 1.3.12
	 */
	public boolean available() {
		return !unavailable;
	}

	/**
	 * This returns a vector of running works
	 */
	public Vector<Work> runningWorks() {

		final Vector<Work> ret = new Vector<>();

		for (final Iterator<ThreadWork> it = threadWorkPool.iterator(); it.hasNext();) {
			final ThreadWork threadWork = it.next();
			final Work work = threadWork.getWork();
			if ((threadWork != null) && (work != null) && (work.isRunning() == true)) {
				ret.add(work);
			}
		}

		return ret;
	}

	/**
	 * This calls getThreadByWorkUid(w.getUID())
	 *
	 * @param w
	 *            is the work we want to retrieve the managing ThreadWork
	 * @see #getThreadByWorkUid(UID)
	 */
	public ThreadWork getThreadByWork(final Work w) {
		return getThreadByWorkUid(w.getUID());
	}

	/**
	 * This returns the ThreadWork executing the work which UID is provided
	 *
	 * @param uid
	 *            is the work identifier
	 * @return the ThreadWork managing to the work which uid is provided, or
	 *         null if not found
	 * @since 8.2.0
	 */
	public ThreadWork getThreadByWorkUid(final UID uid) {

		if (uid == null) {
			return null;
		}

		for (final Iterator<ThreadWork> it = threadWorkPool.iterator(); it.hasNext();) {
			final ThreadWork threadWork = it.next();
			if ((threadWork != null) && (threadWork.getWork() != null)
					&& threadWork.getWork().getUID().equals(uid)) {
				return threadWork;
			}
		}
		logger.error("getThreadByWorkUid() can't find work " + uid);
		return null;
	}

	/**
	 * Verify the right number of threads
	 */
	private void checkThreadPoolSanity() {

		final int threadsToCreate = Worker.getConfig().getWorkPoolSize() - threadWorkPool.size();

		for (int i = 0; i < threadsToCreate; i++) {
			final ThreadWork threadWork = new ThreadWork();
			threadWork.setDaemon(true);
			threadWork.setPriority(Thread.MIN_PRIORITY);
			threadWork.start();
			threadWorkPool.addElement(threadWork);
		}
	}

	/**
	 * This instantiates a new activator as defined in config file
	 *
	 * @since XWHEP 1.0.0
	 * @see #activator
	 * @see #setupActivator(String)
	 */
	private void setupActivator() throws InstantiationException {
		setupActivator(Worker.getConfig().getProperty(XWPropertyDefs.ACTIVATORCLASS));
	}

	/**
	 * This instantiates a new activator
	 *
	 * @param activatorClassName
	 *            is the activator class name to instantiate
	 * @since XWHEP 1.0.0
	 * @see #activator
	 * @see #initActivator()
	 */
	public void setupActivator(final String activatorClassName) throws InstantiationException {
		try {
			if (activatorThread != null) {
				activatorThread.wait();
			}
			activator = null;
			final Class actClass = Class.forName(activatorClassName);
			Worker.getConfig().setProperty(XWPropertyDefs.ACTIVATORCLASS, activatorClassName);
			activator = (Activator) actClass.newInstance();
			initActivator();
		} catch (final InterruptedException e) {
			throw new InstantiationException("Error while instanciating activator " + activatorClassName + e);
		} catch (final IllegalAccessException e) {
			throw new InstantiationException("Error while instanciating activator " + activatorClassName + e);
		} catch (final ClassNotFoundException e) {
			throw new InstantiationException("Error while instanciating activator " + activatorClassName + e);
		}
	}

	/**
	 * This initializes the activator and its associated thread
	 *
	 * @since XWHEP 1.0.0
	 * @see #activator
	 * @see #activatorThread
	 */
	private void initActivator() throws InstantiationException {
		try {
			activator.initialize(Worker.getConfig());
			if (activator instanceof Runnable) {
				if (activatorThread != null) {
					activatorThread.notify();
					activatorThread.interrupt();
				} else {
					activatorThread = new Thread((Runnable) activator);
					activatorThread.setDaemon(true);
					activatorThread.start();
				}
			}

			this.interrupt();

			logger.debug("Activator = " + activator.getClass().getName());
		} catch (final Exception e) {
			logger.exception(e);
			throw new InstantiationException(
					"Error while instanciating activator " + activator.getClass().getName() + " " + e);
		}
	}

	/**
	 * @return the instance
	 */
	public static ThreadLaunch getInstance() {
		return instance;
	}

	/**
	 * @param instance
	 *            the instance to set
	 */
	public static void setInstance(final ThreadLaunch instance) {
		ThreadLaunch.instance = instance;
	}
}
