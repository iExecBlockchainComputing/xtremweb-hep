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

	public static final int SIZE = CPUEnum.values().length;

	// this should be uppercase
	public static final String[] ARM_ARCH_ARRAY = {"ARM", "AARCH"};
	public static final String[] IX86_ARCH_ARRAY = {"I86", "X86", "IX86", "I386", "X386", "IX386", "I486", "X486", "IX486",
		"I586", "X586", "IX586", "I686", "X686", "IX686"};


	public static CPUEnum fromInt(final int i) throws IllegalArgumentException {
		if (0 <= i && i < SIZE) return CPUEnum.values()[i];
		throw new IllegalArgumentException("unvalid XWCPUs value " + i);
	}

	/**
	 * Get Hardware type If this architecture is not supported by XtremWeb, this
	 * forces the program to stop immediately
	 *
	 * @return host's architecture type
	 */
	public static CPUEnum getCpu() {
		try {
			String archName = System.getProperty("os.arch").toUpperCase();

			if (Arrays.asList(IX86_ARCH_ARRAY).contains(archName)) return IX86;

			if (Arrays.stream(ARM_ARCH_ARRAY).parallel().anyMatch(archName::contains))
				return valueOf("ARM" + System.getProperty("sun.arch.data.model")); // ARM32 or ARM64

			return valueOf(archName);

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
	 * @exception IllegalArgumentException
	 *                exception is thrown if archName is not supported by XtremWeb
	 */
	public static CPUEnum getCpu(final String archName) throws IllegalArgumentException {

		if (Arrays.asList(IX86_ARCH_ARRAY).contains(archName.toUpperCase()))
			return IX86;

		return valueOf(archName.toUpperCase());
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