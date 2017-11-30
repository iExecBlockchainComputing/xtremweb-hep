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

package xtremweb.communications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.apache.log4j.Logger;


import xtremweb.common.CommonVersion;
import xtremweb.common.XWTools;

/**
 * This class aims to store access log "a la" Apache httpd so that we can parse
 * access log with "standard" tools such as webalizer
 * (http://www.mrunix.net/webalizer/)
 *
 * Format:
 * {@code $RHOST - $user [dd/MM/yyyy:HH:mm:ss Z] "GET $file $PROTOCOL/$VERSION" $STATUS $SIZERETURNED "-" "XW/$VERSION ($OS)"}
 *
 * This class create a log file per day; is automatically creates a new log file
 * on each new day.<br />
 * Log files are named $server-YYYY-MM-DD.log
 *
 * @author Oleg Lodygensky
 * @since XWHEP 1.0.0
 *
 *        See
 *        <a href="http://httpd.apache.org/docs/1.3/mod/mod_log_common.html">
 *        Apache log file format</a>
 *
 */
public class AccessLogger {

	private static final Logger logger = Logger.getLogger(AccessLogger.class);

	/**
	 * This helps to format date : the format is "yyyy-MM-dd HH:mm:ss"
	 */
	public static final SimpleDateFormat logDateFormat = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]", Locale.US);
	/**
	 * This formats date for log file name as : "-yyyy-MM-dd.log"
	 */
	private static final SimpleDateFormat logFileNameFormat = new SimpleDateFormat("-yyyy-MM-dd", Locale.US);
	/**
	 * This defines the stream to write access log to
	 */
	private PrintStream out;
	/**
	 * This is the directory where log files are stored
	 */
	private File logPath;
	/**
	 * This is the log date
	 */
	private static Calendar logDate;
	/**
	 * This is the server name to log access for
	 */
	private String server;
	/**
	 * This stores XWHEP version as string
	 */
	private final String version = CommonVersion.getCurrent().rev();
	/**
	 * This is the directory name where access logs are stored
	 */
	private static final String DIRNAME = "xwAccessLogs";
	/**
	 *
	 */
	private static AccessLogger instance = null;

	/**
	 * @return the instance
	 */
	public static AccessLogger getInstance() {
		return instance;
	}

	/**
	 * @param instance
	 *            the instance to set
	 */
	public static void setInstance(final AccessLogger instance) {
		AccessLogger.instance = instance;
	}

	/**
	 * This constructs a new instance and prepare the output directory to store
	 * log files On output directory error, logPath is set to null and out is
	 * set to System.out
	 *
	 * @param root
	 *            is the path where log files are stored
	 * @param s
	 *            is the server name to log access for
	 */
	public AccessLogger(final File root, final String s) throws IOException {
		if (getInstance() != null) {
			return;
		}

		try {
			logPath = root;
			server = s;
			out = null;
			logDate = Calendar.getInstance();
			try {
				final File rootFile = new File(root, DIRNAME);
				XWTools.checkDir(rootFile);
			} catch (final IOException ioe) {
				logPath = null;
				out = System.out;
				logger.error("Caught exception: ", ioe);
			}
			setInstance(this);
		} finally {
		}
	}

	/**
	 * This return the current log file name
	 *
	 * @return a String containing the current log file name
	 */
	public String getCurrentLogFileName() {
		return server + logFileNameFormat.format(logDate.getTime()) + ".log";
	}

	/**
	 * This checks whether we should change log file; such changes occur for
	 * each new day. If logPath is null, this does nothing
	 */
	private void checkLogFile() throws IOException {

		if (logPath == null) {
			return;
		}

		try {
			final Calendar currentDate = Calendar.getInstance();

			if ((out == null) || (currentDate.get(Calendar.YEAR) != logDate.get(Calendar.YEAR))
					|| (currentDate.get(Calendar.MONTH) != logDate.get(Calendar.MONTH))
					|| (currentDate.get(Calendar.DAY_OF_MONTH) != logDate.get(Calendar.DAY_OF_MONTH))) {

				if (out != null) {
					out.flush();
					out.close();
				}

				logDate = currentDate;
				final FileOutputStream fos = new FileOutputStream(new File(logPath, getCurrentLogFileName()), true);
				out = new PrintStream(fos);
			}
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
	}

	/**
	 * This prints an entry to out stream
	 *
	 * @param fileAccessed
	 *            is the accessed file path
	 * @param user
	 *            is the user name
	 * @param proto
	 *            is the communication protocol
	 * @param status
	 *            is the request status
	 * @param returnSize
	 *            is the size of the answer
	 * @param rHost
	 *            is the name of the remote host
	 * @param ros
	 *            is the OS of the remote host
	 * @see #out
	 */
	public synchronized void println(final String fileAccessed, final String user, final String proto, final int status,
			final long returnSize, final String rHost, final String ros, final IdRpc idRpc) throws IOException {
		try {
			checkLogFile();
		} catch (final IOException e) {
			notify();
			throw e;
		}

		out.print(rHost + " " + this.server + " - " + user + " " + logDateFormat.format(new Date()) + " \"" + idRpc
				+ " " + fileAccessed + " " + proto + "/" + version + "\" " + status + " " + returnSize + " \"-\" \"XW/"
				+ version + " (" + ros + ")\"");
		out.println();

		notify();
	}

	public static void main(final String[] args) throws IOException {
		final AccessLogger accessLog = new AccessLogger(new File("."), "xtremweb-access-test");

		for (int i = 0; i < 100; i++) {
			accessLog.println("/toto", "user0", "HTTP/1.1", 200, 512, "toto.com", "WIN32", IdRpc.GET);
		}
		for (int i = 0; i < 100; i++) {
			accessLog.println("/tata", "user1", "TCP/1.1", 200, 1024, "tata.com", "MACOSX", IdRpc.GETAPPS);
		}
		for (int i = 0; i < 100; i++) {
			accessLog.println("/titi", "user2", "UDP/0.9", 200, 1024, "titi.com", "WIN32", IdRpc.DISCONNECT);
		}

	}
}
