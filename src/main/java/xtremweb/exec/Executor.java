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

package xtremweb.exec;

/**
 * Executor.java
 * Launch and execute process
 *
 * Created: Sun Mar 28 21:48:40 2004
 *
 * @author <a href="mailto:fedak@lri.fr">Gilles Fedak</a>
 * @version 1.0
 *
 * @author Oleg Lodygensky
 * @version 8.2.0
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import xtremweb.common.Logger;
import xtremweb.common.LoggerLevel;

public class Executor {

	private Logger logger;
	/**
	 * This contains environment variables
	 *
	 * @since 8.0.0 (FG)
	 */
	private String[] envvars = null;
	/**
	 * This is the command line This includes the process command line and its
	 * options
	 */
	private String commandLine = null;
	/**
	 * This is the process execution directory
	 */
	private String executionDirectory = null;

	/** This is the process standard input */
	private InputStream stdin = null;
	/** This is the process standard output */
	private OutputStream stdout = null;
	/** This is the process standard error */
	private OutputStream stderr = null;

	/** This is the managed process */
	private Process process = null;

	/** This is the execution Runtime */
	private final Runtime machine = Runtime.getRuntime();

	/** This stores the process running flag */
	private boolean isRunning = false;
	/** block siez in bytes */
	private final int BLOCK_SIZE = 65500;

	/**
	 * This is the default timer period in milliseconds; default value is 250
	 */
	private final int SLEEP_UNIT = 250;
	/**
	 * This is the timer period in millisec
	 */
	private long sleep_delay;
	/**
	 * This is the wallclock time in seconds. The job is killed when call
	 * clocktime is reached. If wall clocktime < 0, this means don't use.
	 *
	 * @since 8.2.0
	 */
	private long maxWallClockTime;
	/**
	 * This is the start computing date in seconds
	 *
	 * @since 8.2.0
	 */
	private long startDate;
	/**
	 * This is a timer; it helps to periodically check process status and to
	 * flush I/O
	 */
	private final Timer timer = new Timer();
	private boolean wallclockTimeReached;

	public void setLoggerLevel(final LoggerLevel l) {
		getLogger().setLoggerLevel(l);
	}

	public Executor() {
		setLogger(new Logger(this));
		maxWallClockTime = -1;
		wallclockTimeReached = false;
		startDate = 0;
	}

	/**
	 * This constructor sets execution directory and I/O to null
	 *
	 * @param cmd
	 *            is the command to execute
	 * @param ev
	 *            contains the env variables
	 */
	public Executor(final String cmd, final String[] ev) {
		this(cmd);
		setEnvVars(ev);
	}

	/**
	 * This constructor sets execution directory and I/O to null
	 *
	 * @param cmd
	 *            is the command to execute
	 */
	public Executor(final String cmd) {
		this(cmd, (String) null);
	}

	/**
	 * This constructor sets I/O to the standard input and output
	 *
	 * @param cmd
	 *            is the command to execute
	 * @param dir
	 *            is the execution directory
	 */
	public Executor(final String cmd, final String dir) {
		this(cmd, dir, System.in, System.out, System.err);
	}

	/**
	 * This constructor sets sleep delay to SLEEP_UNIT
	 *
	 * @see #Executor(String, String, InputStream, OutputStream, OutputStream,
	 *      long)
	 */
	public Executor(final String cmd, final String dir, final InputStream in, final OutputStream out,
			final OutputStream err) {
		this(cmd, dir, in, out, err, -1);
	}

	/**
	 * This constructor sets I/O to null If delay == 0 , sleep delay set to 5
	 *
	 * @param cmd
	 *            is the command to execute
	 * @param dir
	 *            is the execution directory
	 * @param in
	 *            is the process input
	 * @param out
	 *            is the process output
	 * @param err
	 *            is the process output error
	 * @param delay
	 *            is the sleep delay
	 * @see #executionDirectory
	 * @see #stdin
	 * @see #stdout
	 * @see #stderr
	 */
	public Executor(final String cmd, final String dir, final InputStream in, final OutputStream out,
			final OutputStream err, final long delay) {
		this();
		setCommandLine(cmd);
		executionDirectory = dir;
		stdin = in;
		stdout = out;
		stderr = err;
		setDelay(delay);
	}

	/**
	 * This constructor sets I/O to null If delay == 0 , sleep delay set to 5
	 *
	 * @param cmd
	 *            is the command to execute
	 * @param env
	 *            contains environment variables
	 * @param dir
	 *            is the execution directory
	 * @param in
	 *            is the process input
	 * @param out
	 *            is the process output
	 * @param err
	 *            is the process output error
	 * @param delay
	 *            is the sleep delay
	 * @see #executionDirectory
	 * @see #stdin
	 * @see #stdout
	 * @see #stderr
	 * @since 8.0.0 (FG)
	 */
	public Executor(final String cmd, final String[] env, final String dir, final InputStream in,
			final OutputStream out, final OutputStream err, final long delay) {
		this();
		setCommandLine(cmd);
		envvars = env.clone();
		executionDirectory = dir;
		stdin = in;
		stdout = out;
		stderr = err;
		setDelay(delay);
	}

	/**
	 * This sets the environment variables
	 *
	 * @since 8.0.0 (FG)
	 */
	public final void setEnvVars(final String[] v) {
		envvars = v.clone();
	}

	/**
	 * This sets and corrects the sleep delay
	 */
	public final long setDelay(final long delay) {
		sleep_delay = delay;
		if ((sleep_delay < 0) || (sleep_delay > SLEEP_UNIT)) {
			sleep_delay = SLEEP_UNIT;
		} else if (sleep_delay < 5) {
			sleep_delay = 5;
		}
		getLogger().info("Executor#setDelay() = " + sleep_delay);
		return sleep_delay;
	}

	/**
	 * This sets the wall clock time
	 *
	 * @param w
	 *            is the new wall clock time in seconds
	 * @since 8.2.0
	 * @see #maxWallClockTime
	 */
	public void setMaxWallClockTime(final long w) {
		maxWallClockTime = w * 1000;
	}
	public long getMaxWallClockTime() {
		return maxWallClockTime;
	}

	/**
	 * This retreives an attribute (as the name says)
	 *
	 * @return the expected attribute
	 */
	public String getCmdLine() {
		return getCommandLine();
	}

	/**
	 * This retreives an attribute (as the name says)
	 *
	 * @return the expected attribute
	 */
	public String getExecDir() {
		return executionDirectory;
	}

	/**
	 * This sets an attribute (as the name says)
	 *
	 * @param v
	 *            is the new attribute value
	 */
	public void setCmdLine(final String v) {
		setCommandLine(v);
	}

	/**
	 * This sets an attribute (as the name says)
	 *
	 * @param v
	 *            is the new attribute value
	 */
	public void setExecDir(final String v) {
		executionDirectory = v;
	}

	/**
	 * This stops the process, if any
	 *
	 * @exception ExecutorLaunchException
	 *                is thrown on error (no running process)
	 */
	public synchronized void stop() throws ExecutorLaunchException {

		getLogger().debug("stop process");

		clean();

		try {
			process.destroy();
			isRunning = false;
			process = null;
		} catch (final Throwable e) {
			throw new ExecutorLaunchException("Can't stop process [ " + getCmdLine() + "  ]: " + e);
		}
	}

	/**
	 * This closes handles stdin, stdout and stderr. This cancels the timer.
	 */
	private void closeHandles() {

		getLogger().finest("closeHandles()");

		try {
			stdout.flush();
		} catch (final Throwable t) {
		}
		try {
			stdout.close();
		} catch (final Throwable t) {
		}
		stdout = null;
		try {
			stdin.close();
		} catch (final Throwable t) {
		}
		stdin = null;
		try {
			stderr.flush();
		} catch (final Throwable t) {
		}
		try {
			stderr.close();
		} catch (final Throwable t) {
		}
		stderr = null;

		timer.cancel();
	}

	/**
	 * This starts the process using Runtime.exec () It instanciates a timer to
	 * check process state and periodically flush the I/O
	 */
	public void start() throws ExecutorLaunchException {

		getLogger().debug("start process : " + getCommandLine());

		File dir = null;

		try {
			if (executionDirectory != null) {
				dir = new File(executionDirectory);
			}

			startDate = new Date().getTime();

			process = machine.exec(getCommandLine(), envvars, dir);

			if (stdin == null) {
				process.getOutputStream().flush();
				process.getOutputStream().close();
			}
			isRunning = true;
		} catch (final Exception e) {
			throw new ExecutorLaunchException("Can't start process  [" + getCommandLine() + "] : " + e);
		} finally {
			dir = null;
		}

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				flushPipe();
				checkWallClock();
			}
		}, 0, // run now
				sleep_delay);
	}

	/**
	 * This tells whether the process is running or not
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * This checks for process terminason and flushes the I/O
	 */
	private void flushPipe() {
		int returnCode = -1;
		try {
			if (process != null) {
				returnCode = process.exitValue();
			}
			isRunning = false;
			timer.cancel();
		} catch (final IllegalThreadStateException ie) {
		}

		if (process == null) {
			return;
		}

		try {
			int piped;
			if (stdin != null) {
				try {
					if (stdin.available() > 0) {
						getLogger().finest("stdin.available() = " + stdin.available());
					}
					piped = pipe(stdin, process.getOutputStream(), false);
					if (piped > 0) {
						getLogger().finest("pipe(stdin)  = " + piped);
					}
					if (stdin.available() <= 0) {
						stdin.close();
						process.getOutputStream().close();
						stdin = null;
					}
				} catch (final Exception e) {
					getLogger().exception("Executor (" + getCommandLine() + ") : stdin error", e);
					try {
						stdin.close();
					} catch (final Exception e1) {
					}

					try {
						if ((process != null) && (process.getOutputStream() != null)) {
							process.getOutputStream().close();
						}
					} catch (final Exception e1) {
					}
					stdin = null;
				}
			}
			if (process.getInputStream() != null) {
				try {
					if (process.getInputStream().available() > 0) {
						getLogger().finest(
								"process.getInputStream().available() = " + process.getInputStream().available());
					}

					piped = pipe(process.getInputStream(), stdout, false);
					if (piped > 0) {
						getLogger().finest("pipe(stdout) = " + piped);
					}
				} catch (final Exception e) {
					getLogger().exception("Executor (" + getCommandLine() + ") : input stream error", e);
				}
			}
			if (process.getErrorStream() != null) {
				try {
					if (process.getErrorStream().available() > 0) {
						getLogger().finest(
								"process.getErrorStream().available() = " + process.getErrorStream().available());
					}
					piped = pipe(process.getErrorStream(), stderr, false);
					if (piped > 0) {
						getLogger().finest("pipe(stderr) = " + piped);
					}
				} catch (final Exception e) {
					getLogger().exception("Executor (" + getCommandLine() + ") : err stream error", e);
				}
			}
		} catch (final Exception e) {
			getLogger().exception(e);
		}
	}

	/**
	 * This waits until the process exits
	 *
	 * @return the process return code
	 * @throws ExecutorWallClockTimeException
     *             is thrown if wall clock time reached
	 */
	public int waitFor() throws ExecutorWallClockTimeException {
		int returnCode = 0;

		for (isRunning = true; isRunning;) {
			try {
				Thread.sleep(sleep_delay);
			} catch (final InterruptedException e) {
			}

			try {
				if (process != null) {
					returnCode = process.exitValue();
				}
				isRunning = false;
			} catch (final IllegalThreadStateException ie) {
			}
		}

		if (wallclockTimeReached == true) {
			throw new ExecutorWallClockTimeException();
		}

		clean();

		return (returnCode);
	}

	/**
	 * This flushed pipes, closes handles and cancels timer
	 *
	 * @since 8.2.0
	 */
	private void clean() {
		flushPipe();
		closeHandles();
		timer.cancel();
	}

	/**
	 * This checks wall clock time and terminates the current process, if it is
	 * reached
	 *
	 * @since 8.2.0
	 */
	private void checkWallClock() {
		if ((maxWallClockTime > 0) && ((new Date().getTime() - startDate) > maxWallClockTime)) {
			try {
				stop();
			} catch (final ExecutorLaunchException e) {
				getLogger().exception("checkWallClock", e);
			} finally {
				wallclockTimeReached = true;
				isRunning = false;
			}
		}
	}

	/**
	 * This start the process and wait until the process exits
	 *
	 * @return the process exit code
	 * @throws InterruptedException
	 *             is thrown if wall clock time reached
	 */
	public int startAndWait() throws ExecutorLaunchException, ExecutorWallClockTimeException  {
		start();
		return waitFor();
	}

	/**
	 * This pipes from one stream to other
	 *
	 * @param in
	 *            is the input stream to read from; if null this method returns
	 *            immediatly
	 * @param out
	 *            is output stream to write to; if null nothing is written
	 * @param isBlocking
	 *            tells whether I/O are blocking or not
	 */
	private int pipe(final InputStream in, final OutputStream out, final boolean isBlocking) throws IOException {
		int nread;
		int navailable;
		int total = 0;

		if (in == null) {
			return -1;
		}

		synchronized (in) {

			final byte[] buf = new byte[BLOCK_SIZE];

			while (((navailable = (isBlocking ? BLOCK_SIZE : in.available())) > 0)
					&& ((nread = in.read(buf, 0, Math.min(buf.length, navailable))) >= 0)) {

				if (out != null) {
					out.write(buf, 0, nread);
				} else {
					getLogger().error("Executor#pipe : out is null ?!?!");
				}
				total += nread;
			}

		}

		if (out != null) {
			out.flush();
		}

		return total;
	}

	/**
	 *
	 */
	protected static String join(final String tab[], final String sep) {
		String result = "";
		if (tab == null) {
			return null;
		}
		if (tab.length == 0) {
			return "";
		}
		for (int i = 0; i < (tab.length - 1); i++) {
			result += (tab[i] + sep);
		}
		return result + tab[tab.length - 1];
	}

	/**
	 * This is the standard main () method This is only for test purposes
	 */
	public static void main(final String[] args) {

		String command = args[0];
		String stdinName = null;

		if (args.length > 1) {
			command += " " + args[1];
		}
		if (args.length > 2) {
			stdinName = args[2];
		}
		try (final FileOutputStream stdout = new FileOutputStream(new File("stdout.txt"));
				final FileOutputStream stderr = new FileOutputStream(new File("stderr.txt"))) {
			final Executor exec = new Executor(command, ".",
					stdinName == null ? null : new FileInputStream(new File(stdinName)), stdout, stderr);
			exec.getLogger().setLoggerLevel(LoggerLevel.DEBUG);
			exec.startAndWait();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @param logger
	 *            the logger to set
	 */
	public void setLogger(final Logger logger) {
		this.logger = logger;
	}

	/**
	 * @return the commandLine
	 */
	public String getCommandLine() {
		return commandLine;
	}

	/**
	 * @param commandLine
	 *            the commandLine to set
	 */
	public void setCommandLine(final String commandLine) {
		this.commandLine = commandLine;
	}

}
