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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;


//
//  XWUtilLinux.java
//  Samuel Heriard
//
//  Linux impl. of XWUtil

public class XWUtilSolaris extends XWUtilDummy {

	private static final Logger logger = Logger.getLogger(XWUtilSolaris.class);
	private final Runtime machine = Runtime.getRuntime();

	@Override
	public int getSpeedProc() {
		String valStr = null;
		Process workProcess;

		File tempFile = null;
		try {
			tempFile = File.createTempFile("speedProc", "txt");
		} catch (final Exception e) {
			logger.error("Cannot create temp file");
			return -1;
		}

		if (!tempFile.exists()) {
			tempFile.delete();
		}

		try {
			workProcess = machine.exec("/usr/sbin/psrinfo -v > " + tempFile.getCanonicalPath());
		} catch (final IOException e) {
			logger.error("XWUtilSolaris : cannot spawn a new process" + e);
			return -1;
		}

		try {
			workProcess.waitFor();
		} catch (final InterruptedException e) {
			logger.error("Caught exception: ThreadLaunch in executeNativeJob: cannot wait for the end of the job ?!?", e);
			return -1;
		}

		try (final BufferedReader bufferFile = new BufferedReader(new FileReader(tempFile))){

			String l = "";
			while ((l != null) && (valStr == null)) {
				// each processor section has
				// operates at xxx MHz
				final String operates = new String("operates at ");
				final int idx = l.indexOf(operates);
				if (idx != -1) {
					valStr = l.substring(idx + operates.length() + 1, l.indexOf("MHz") - 1);
					break;
				}
				l = bufferFile.readLine();
			}
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			return 0;
		}

		if (valStr != null) {
			return Integer.parseInt(valStr);
		}
		return 0;
	}

	@Override
	public String getProcModel() {
		return new String("");
	}

}
