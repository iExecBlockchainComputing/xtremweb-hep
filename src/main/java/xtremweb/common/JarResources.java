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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;


/**
 * JarResources: JarResources maps all resources included in a Zip or Jar file.
 * Additionaly, it provides a method to extract one as a blob.
 */
public final class JarResources {

	private static final Logger logger = Logger.getLogger(JarResources.class);

	// jar resource mapping tables
	private final Hashtable htSizes = new Hashtable();
	private final Hashtable htJarContents = new Hashtable();

	// a jar file
	private final String jarFileName;

	// The main class name
	private String mainClassName;

	/**
	 * creates a JarResources. It extracts all resources from a Jar into an
	 * internal hashtable, keyed by resource names.
	 *
	 * @param jarFileName
	 *            a jar or zip file
	 */
	public JarResources(final String jarFileName) {
		this.jarFileName = jarFileName;
		init();
	}

	/**
	 * Extracts a jar resource as a blob.
	 *
	 * @param name
	 *            a resource name.
	 */
	public byte[] getResource(final String name) {
		return (byte[]) htJarContents.get(name);
	}

	public String getMainClassName() {
		return mainClassName;
	}

	/** initializes internal hash tables with Jar file resources. */
	private void init() {
		try (final JarFile jf = new JarFile(jarFileName);
				final ZipFile zf = new ZipFile(jarFileName);
				final FileInputStream fis = new FileInputStream(jarFileName);
				final BufferedInputStream bis = new BufferedInputStream(fis);
				final ZipInputStream zis = new ZipInputStream(bis)) {
			// gets the main class name
			mainClassName = jf.getManifest().getMainAttributes().getValue("Main-Class");

			// extracts just sizes only.

			final Enumeration e = zf.entries();
			while (e.hasMoreElements()) {
				final ZipEntry ze = (ZipEntry) e.nextElement();

				logger.debug(dumpZipEntry(ze));

				htSizes.put(ze.getName(), new Integer((int) ze.getSize()));
			}
			// extract resources and put them into the hashtable.
			ZipEntry ze = null;
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					continue;
				}

				logger.debug("ze.getName()=" + ze.getName() + "," + "getSize()=" + ze.getSize());

				int size = (int) ze.getSize();
				// -1 means unknown size.
				if (size == -1) {
					size = ((Integer) htSizes.get(ze.getName())).intValue();
				}

				final byte[] b = new byte[size];
				int rb = 0;
				int chunk = 0;
				while ((size - rb) > 0) {
					chunk = zis.read(b, rb, size - rb);
					if (chunk == -1) {
						break;
					}
					rb += chunk;
				}

				// add to internal resource hashtable
				htJarContents.put(ze.getName(), b);

				logger.debug(ze.getName() + "  rb=" + rb + ",size=" + size + ",csize=" + ze.getCompressedSize());
			}
			zis.close();
		} catch (final NullPointerException e) {
			logger.debug("done.");
		} catch (final FileNotFoundException e) {
			logger.error("Caught exception: ", e);
		} catch (final IOException e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * Dumps a zip entry into a string.
	 *
	 * @param ze
	 *            a ZipEntry
	 */
	private String dumpZipEntry(final ZipEntry ze) {
		String sb = new String();
		if (ze.isDirectory()) {
			sb += "d ";
		} else {
			sb += "f ";
		}

		if (ze.getMethod() == ZipEntry.STORED) {
			sb += "stored   ";
		} else {
			sb += "deflated ";
		}

		sb += ze.getName();
		sb += "\t";
		sb += "" + ze.getSize();
		if (ze.getMethod() == ZipEntry.DEFLATED) {
			sb += "/" + ze.getCompressedSize();
		}

		return sb;
	}

} // End of JarResources class.
