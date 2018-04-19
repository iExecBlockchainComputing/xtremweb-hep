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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import xtremweb.common.XWTools;
import xtremweb.exec.Executor;
import xtremweb.exec.ExecutorLaunchException;

//  XWUtilMacOSX.java
//
//  Created: 28 fevrier 2007

/**
 * <p>
 * Mac OS X implementation of <code>XWUtil</code>
 * </p>
 *
 * @author Oleg Lodygensky
 */
public class XWUtilMacOSX extends XWUtilImpl {

	public static String LABEL_PROCMODEL = "machdep.cpu.brand_string";
	public static String LABEL_SPEEDPROC = "hw.cpufrequency";
	public static String LABEL_TOTALMEM = "hw.memsize";
	public XWUtilMacOSX() {
	}

	/**
	 * in Kb
	 */
	@Override
	public long getTotalMem() {
		try {
			return Long.parseLong(callsysctl(LABEL_TOTALMEM)) / XWTools.ONEKILOBYTES;
		}
		catch(final Exception e) {
            e.printStackTrace();

            return 0;
		}
	}

	/**
	 * in MHz
	 */
	@Override
	public int getSpeedProc() {
		try {
			return (int)(Long.parseLong(callsysctl(LABEL_SPEEDPROC)) / 1000000);
		}
		catch(final Exception e) {
		    e.printStackTrace();
			return 0;
		}
	}

	@Override
	public String getProcModel() {
		return callsysctl(LABEL_PROCMODEL);
	}

	/**
	 * This call "sysctl -a" and search the given key in the output
	 * @param key is the key to find
	 * @return
	 */
	private String callsysctl(final String key) {

		System.out.println("XWUtilMacOSX callsysctl(" + key + ")");

		try (final FileOutputStream out = new FileOutputStream(new File(".", XWTools.STDOUT));
			 final FileOutputStream err = new FileOutputStream(new File(".", XWTools.STDERR))) {

			final Executor exec = new Executor("sysctl -a", (String)null, null, out, err);
			
			if (exec.startAndWait() != 0) {
				return "";
			}
		} catch (Exception e1) {
			new File(".", XWTools.STDOUT).delete();
			new File(".", XWTools.STDERR).delete();
			return "";
		}

		try (final BufferedReader bufferFile = new BufferedReader(new FileReader(new File(".", XWTools.STDOUT)))) {
			String l = "";
			while (l != null) {
				if (l.indexOf(key) != -1) {
                    System.out.println("found " + key + " in " + l);
					final int start = l.indexOf(':') + 1;
					if (start != -1) {
						return l.substring(start).trim();
					}
				}
				l = bufferFile.readLine();
			}
		} catch (final IOException e) {
			return "";
		} finally {
			new File(".", XWTools.STDOUT).delete();
			new File(".", XWTools.STDERR).delete();
		}
		return "";
	}

	public  static void main(String[] args) {
		XWUtilMacOSX util = new XWUtilMacOSX();
		System.out.println("procModel= " + util.getProcModel());
		System.out.println("speedProc = " + util.getSpeedProc());
		System.out.println("totalMem = " + util.getTotalMem());
	}
}
