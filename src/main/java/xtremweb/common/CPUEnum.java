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

/**
 * XWCPUs.java<br />
 *
 * This defines XtremWeb compatible CPUs<br />
 *
 * Created: 29 janvier 2007 ChangeLog - 6 janvier 2014 : introducing ARM
 *
 * @since 1.9.0
 *
 * @author <a href="mailto: lodygens *a**t* lal.in2p3.fr">Oleg Lodygensky</a>
 * @version %I% %G%
 */

public enum CPUEnum {

	NONE, IX86, X86_64, IA64, PPC, SPARC, ALPHA, AMD64, ARM32, ARM64, ARM;

	public static final CPUEnum LAST = ARM;
	public static final int SIZE = LAST.ordinal() + 1;

	public static CPUEnum fromInt(final int v) throws IllegalArgumentException {
		for (final CPUEnum i : CPUEnum.values()) {
			if (i.ordinal() == v) {
				return i;
			}
		}
		throw new IllegalArgumentException("unvalid XWCPUs value " + v);
	}

	/**
	 * Get Hardware name If this architecture is not supported by XtremWeb, this
	 * forces the program to immediately stop
	 *
	 * @see #getCpuName(String)
	 */
	public static String getCpuName() {
		try {
			return getCpuName(System.getProperty("os.arch"));
		} catch (final Exception e) {
			XWTools.fatal(e.toString());
		}
		return null;
	}

	/**
	 * This forces architecture name to predefined values to avoid confusion
	 *
	 * @param archName
	 *            the architecture name
	 * @exception ClassNotFound
	 *                exception is thrown if archName is not supported by
	 *                XtremWeb
	 */
	public static String getCpuName(final String archName) throws IllegalArgumentException {

		if ((archName.compareToIgnoreCase("i86") == 0) || (archName.compareToIgnoreCase("x86") == 0)
				|| (archName.compareToIgnoreCase("ix86") == 0) || (archName.compareToIgnoreCase("i386") == 0)
				|| (archName.compareToIgnoreCase("x386") == 0) || (archName.compareToIgnoreCase("ix386") == 0)
				|| (archName.compareToIgnoreCase("i486") == 0) || (archName.compareToIgnoreCase("x486") == 0)
				|| (archName.compareToIgnoreCase("ix486") == 0) || (archName.compareToIgnoreCase("i586") == 0)
				|| (archName.compareToIgnoreCase("x586") == 0) || (archName.compareToIgnoreCase("ix586") == 0)
				|| (archName.compareToIgnoreCase("i686") == 0) || (archName.compareToIgnoreCase("x686") == 0)
				|| (archName.compareToIgnoreCase("ix686") == 0)) {
			return IX86.toString();
		}

		return valueOf(archName.toUpperCase()).toString();
	}

	/**
	 * Get Hardware type If this architecture is not supported by XtremWeb, this
	 * forces the program to immediately stop
	 *
	 * @see #getCpu(String)
	 */
	public static CPUEnum getCpu() {
		try {
			return getCpu(System.getProperty("os.arch"));
		} catch (final Exception e) {
			XWTools.fatal(e.toString());
		}
		return null;
	}

	/**
	 * This forces architecture name to predefined values to avoid confusion
	 *
	 * @param archName
	 *            the architecture name
	 * @return architecture type
	 */
	public static CPUEnum getCpu(final String archName) throws IllegalArgumentException {
		return valueOf(getCpuName(archName));
	}

	/**
	 * This retrieves this enum string representation
	 *
	 * @return a array containing this enum string representation
	 */
	public static String[] getLabels() {
		final String[] labels = new String[SIZE];
		for (final CPUEnum c : CPUEnum.values()) {
			labels[c.ordinal()] = c.toString();
		}
		return labels;
	}

	public static void main(final String[] argv) {
		for (final CPUEnum i : CPUEnum.values()) {
			System.out.println(i.toString());
		}
	}

}
