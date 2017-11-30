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

import org.apache.log4j.Logger;


/**
 * ThreadLock.java This Thread Launch the screen saver
 *
 * Created: Thu Jun 29 17:47:11 2000
 *
 * @author Gilles Fedak
 */

public class ThreadLock extends Thread {

	private static final Logger logger = Logger.getLogger(ThreadLock.class);
	private Process xlockProcess;

	ThreadLock() {
		super("ThreadLock");
	}

	@Override
	public void run() {

		logger.info("ThreadLock: started");

		final String screensaverPath = Worker.getConfig().getProperty("activator.screensaver.path");
		if (screensaverPath == null) {
			logger.warn("ScreenSaver not found in the config file (key activator.screensaver.path)");
			return;
		}
		try {
			final Runtime machine = java.lang.Runtime.getRuntime();
			final String[] cmd = { "/bin/sh", "-c", screensaverPath };
			xlockProcess = machine.exec(cmd);
		} catch (final Exception e) {
			logger.error("erreur: " + e.getMessage());
		}

		// Wait until end of computation
		while (!ThreadLaunch.getInstance().available()) {
			yield();
		}

		xlockProcess.destroy();
	}
}
