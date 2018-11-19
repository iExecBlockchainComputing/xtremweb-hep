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

import java.io.File;
import java.io.FileNotFoundException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This defines XtremWeb application type.
 *
 * Created: 16 novembre 2011
 *
 * @author <a href="mailto: lodygens at lal in2p3 fr">Oleg Lodygensky</a>
 * @since 8.0.0
 */

public enum AppTypeEnum {

	NONE,
	/**
	 * This denotes an application where workers must download executable This
	 * denotes application as known until XWHEP 7
	 */
	DEPLOYABLE,
	/**
	 * This denotes an application where workers don't download binary because
	 * the application is supposed to be pre-installed on workers. All following
	 * values in this enum represent shared application.
	 */
	SHARED,
	/**
	 * On July 3rd, 2017, this is our 2nd shared application: docker
	 * @since 14.0.0
	 */
	PDO,
	/**
	 * On July 3rd, 2017, this is our 2nd shared application: docker
	 * @since 11.0.0
	 */
	DOCKER,
	/**
	 * On Dec 2nd, 2011, this denotes our 1st shared application. This denotes
	 * VirtualBox as shared application
	 */
	VIRTUALBOX ;

	public static final AppTypeEnum LAST = VIRTUALBOX;
	public static final int SIZE = LAST.ordinal() + 1;

	/**
	 * This retrieves an OS from its ordinal value
	 *
	 * @exception IllegalArgumentException
	 *                is thrown if v is not a valid ordinal value
	 */
	public static AppTypeEnum fromInt(final int v) throws IllegalArgumentException {
		for (final AppTypeEnum i : AppTypeEnum.values()) {
			if (i.ordinal() == v) {
				return i;
			}
		}
		throw new IllegalArgumentException("unvalid XWOSes value " + v);
	}

	/**
	 * This retrieves this string representation
	 *
	 * @return a array containing this string representation
	 */
	public static String[] getLabels() {
		final String[] labels = new String[SIZE];
		for (final AppTypeEnum c : AppTypeEnum.values()) {
			labels[c.ordinal()] = c.toString();
		}
		return labels;
	}

	/**
	 * This retrieves application launch script name.
	 * Since 13.0.0 applications do not have launchscript any more
	 * @return application binary path if available
	 * @since 13.0.0
	 */
	final public String getLaunchScriptName() {
		return XWTools.LAUNCHSCRIPTHEADER + this.toString().toLowerCase() + XWTools.LAUNCHSCRIPTTRAILER;
	}
	/**
	 * This retrieves application unlaunch script name.
	 * Since 13.0.0 applications do not have unloadscript any more
	 * @return application unload script name
	 * @since 13.0.0
	 */
	final public String getUnloadScriptName() {
		return XWTools.UNLOADSCRIPTHEADER + this.toString().toLowerCase() + XWTools.UNLOADSCRIPTTRAILER;
	}
	/**
	 * This retrieves application default command line arguments
	 * @return an empty string
	 * @since 12.1.0
	 */
	public String getStartCommandLineArgs() {
		return "";
	}
    /**
     * This retrieves command line arguments to mount a volume
     * @param pwd represents the path to mount; this should be overridden
     * @return an empty string
     * @since 12.1.0
     */
    public String getMountVolumeCommandLine(final File pwd) {
        return "";
    }
    /**
     * This retrieves command line arguments to use PWD; this should be overridden
     * @param pwd represents the path of the present working directory
     * @return an empty string
     * @since 12.2.8
     */
    public String getDefaultWorkingDirectoryCommandLine(final File pwd) {
        return "";
    }

	/**
	 * This is for debug purposes
	 *
	 * @param argv
	 */
	public static void main(final String[] argv) {
		for (final AppTypeEnum i : AppTypeEnum.values()) {
			System.out.println(i.toString());
		}
	}

}
