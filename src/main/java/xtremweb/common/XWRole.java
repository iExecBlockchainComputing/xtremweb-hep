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

package xtremweb.common;

import org.apache.log4j.Logger;


/**
 * XWRole.java
 *
 * Created: Mar 8th, 2007
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

/**
 * This class describes xtremweb roles
 */

public enum XWRole {

	/** This defines unknown role */
	UNKNOWN {
		@Override
		public String className() {
			return null;
		}
	},
	/** This defines worker role */
	WORKER {
		@Override
		public String className() {
			return xtremweb.worker.Worker.class.getName();
		}
	},
	/** This defines client role */
	CLIENT {
		@Override
		public String className() {
			return xtremweb.client.Client.class.getName();
		}
	},
	/** This defines dispatcher (aka server) role */
	SERVER {
		@Override
		public String className() {
			return xtremweb.dispatcher.Dispatcher.class.getName();
		}
	};

	/**
	 * This defines the current role. This is initialized to UNKNOWN
	 */
	private static XWRole myRole = UNKNOWN;

	/**
	 * This setS the current role to worker
	 */
	public static void setRole(final XWRole r) {
		if (getMyRole() != UNKNOWN) {
			final Logger logger = Logger.getLogger(XWRole.class);
			logger.error("setRole : role redefined ?! (was " + getMyRole() + ")");
		}
		setMyRole(r);
	}

	/**
	 * This setS the current role to worker
	 */
	public static void setWorker() {
		setRole(WORKER);
	}

	/**
	 * This sets the current role to dispatcher
	 */
	public static void setDispatcher() {
		setRole(SERVER);
	}

	/**
	 * This sets the current role to client
	 */
	public static void setClient() {
		setRole(CLIENT);
	}

	/**
	 * This tests whether role is worker
	 */
	public static boolean isWorker() {
		return (getMyRole() == WORKER);
	}

	/**
	 * This tests whether role is dispatcher
	 */
	public static boolean isDispatcher() {
		return (getMyRole() == SERVER);
	}

	/**
	 * This tests whether role is client
	 */
	public static boolean isClient() {
		return (getMyRole() == CLIENT);
	}

	/**
	 * This converts this enum to a String.
	 *
	 * @return a string containing boolean value
	 */
	public abstract String className();

	/**
	 * @return the myRole
	 */
	public static XWRole getMyRole() {
		return myRole;
	}

	/**
	 * @param myRole
	 *            the myRole to set
	 */
	public static void setMyRole(final XWRole myRole) {
		XWRole.myRole = myRole;
	}
}
