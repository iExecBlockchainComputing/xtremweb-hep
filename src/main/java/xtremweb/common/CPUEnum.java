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

import java.util.Arrays;

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

	NONE, ALL, IX86, X86_64, IA64, PPC, SPARC, ALPHA, AMD64, ARM32, ARM64;

	public static final CPUEnum LAST = ARM64;
	public static final int SIZE = LAST.ordinal() + 1;

	public static CPUEnum fromInt(final int v) throws IllegalArgumentException {
		if (0 <= v && v < SIZE) return CPUEnum.values()[v];
		throw new IllegalArgumentException("unvalid XWCPUs value " + v);
	}

	/**
	 * Get Hardware type If this architecture is not supported by XtremWeb, this
	 * forces the program to immediately stop
	 *
	 * @see #getCpu(String)
	 */
	public static CPUEnum getCpu() {
		try {

			String archName = System.getProperty("os.arch").toLowerCase();

			final String[] ixArchArray = {"i86", "x86", "ix86", "i386", "x386", "ix386", "i486", "x486", "ix486",
					"i586", "x586", "ix586", "i686", "x686", "ix686"};
			if (Arrays.asList(ixArchArray).contains(archName)) return IX86;

			final String[] armArchArray = {"arm", "aarch64" , "arm32", "arm64"};
			if (Arrays.asList(armArchArray).contains(archName.toLowerCase()))
				return valueOf(("ARM" + System.getProperty("sun.arch.data.model")).toUpperCase()); // ARM32 or ARM64

			return valueOf(archName.toUpperCase());

		} catch (final Exception e) {
			XWTools.fatal(e.toString());
			return null;
		}
	}

	/**
	 * This forces architecture name to predefined values to avoid confusion
	 *
	 * @param archName
	 *            the architecture name
	 * @return architecture type
	 */
	public static CPUEnum getCpu(final String archName) throws IllegalArgumentException {
		try {
			return valueOf(archName.toUpperCase());
		} catch (final Exception e) {
			XWTools.fatal(e.toString());
			return null;
		}

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
