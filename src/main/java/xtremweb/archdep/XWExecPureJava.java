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

package xtremweb.archdep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import xtremweb.worker.StreamPiper;

public class XWExecPureJava implements XWExec {

	private static final Logger logger = Logger.getLogger(XWExecPureJava.class);
	private Process workProcess = null;
	private boolean isRunning = false;

	private Thread outThread = null;
	private Thread errThread = null;
	private Thread inThread = null;
	private OutputStream stdoutFile;
	private OutputStream stderrFile;
	private InputStream stdinFile = null;

	public XWExecPureJava() {
		init();
	}

	@Override
	public final void init() {
		isRunning = false;
	}

	// Launch the execution on a separate process
	@Override
	public boolean exec(final String[] args, final String stdin, final String stdout, final String stderr,
			final String workingDir) {
		if (isRunning) {
			logger.error("Cannot exec: a process already running");
			return false;
		}

		logger.debug("Exec " + args[0]);

		try {
			stdoutFile = new FileOutputStream(new File(workingDir, stdout));
			stderrFile = new FileOutputStream(new File(workingDir, stderr));
		} catch (final Throwable e) {
			logger.error("executeNativeJob: can't create " + stdout + " and " + stderr);
			e.printStackTrace();
			return false;
		} // end of try-catch

		try {
			workProcess = Runtime.getRuntime().exec(args, null, new File(workingDir));
		} catch (final Exception e) {
			logger.error("ThreadLaunch in executeNativeJob: cannot spawn a new process" + e);
			return false;
		} // end of try-catch

		logger.debug("apres exec");
		final StreamPiper outPiper = new StreamPiper(workProcess.getInputStream(), stdoutFile);
		final StreamPiper errPiper = new StreamPiper(workProcess.getErrorStream(), stderrFile);

		StreamPiper inPiper;
		outThread = new Thread(outPiper);
		errThread = new Thread(errPiper);
		inThread = null;
		outThread.start();
		errThread.start();

		if ((workingDir != null) && (stdin != null)) {
			final File inputFile = new File(workingDir, stdin);

			try {
				logger.debug("stdin :" + inputFile.getCanonicalPath());
				stdinFile = new FileInputStream(inputFile);
			} catch (final Exception e) {
				logger.error("executeNativeJob: cannot open stdin file" + e);
				kill();
				return false;
			} // end of try-catch

			inPiper = new StreamPiper(stdinFile, workProcess.getOutputStream());
			inThread = new Thread(inPiper);
			inThread.start();
		}
		logger.debug("isRunning true");
		isRunning = true;
		return true;
	}

	// Stop the curant execution
	@Override
	public boolean kill() {
		logger.debug("kill process");
		workProcess.destroy();
		isRunning = false;
		try {
			if (outThread != null) {
				stdoutFile.close();
				outThread.join();
			}
			if (errThread != null) {
				stderrFile.close();
				errThread.join();
			}
			if (inThread != null) {
				stdinFile.close();
				inThread.join();
			}
		} catch (final Exception e) {
			logger.error("ThreadLaunch::executeNativeJob() : finalization error " + e);
			workProcess.destroy();
			return false;
		} // end of try-catch

		return true;
	}

	@Override
	public boolean destroy() {
		return kill();
	}

	// Wait for the end of the current execution
	@Override
	public int waitFor() {
		int res;
		try {
			res = workProcess.waitFor();
			isRunning = false;
			kill();
			return res;
		} catch (final Exception e) {
			logger.error("run binary app failed: " + e);
			e.printStackTrace();
			isRunning = true;
			return -1;
		}
	}

	// Suspend the current execution
	@Override
	public boolean suspend() {
		return false;
	}

	// Re-activate the current execution
	@Override
	public boolean activate() {
		return false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}
}
