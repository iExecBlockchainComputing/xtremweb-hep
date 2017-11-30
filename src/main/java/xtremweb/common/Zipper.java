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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;


/**
 * Zipper.java This class zip/unzip files
 *
 * If the same object is used to unzip and then zip, it may be configured so
 * that only new or modified files are zipped. (used by the worker to improve
 * bandwidth usage)
 *
 * Created: 5 octobre 2002
 *
 * @author Oleg Lodygensky
 */

public class Zipper {
	static final int BUFFERSIZE = 2048;

	private static final Logger logger = Logger.getLogger(Zipper.class);

	protected static final String NAME = "ZIPPER";

	/**
	 * This is the ZIP archive file name used to create/read archive
	 */
	private String fileName = null;
	/**
	 * This contains files list. This is filled as archive is unzipped. This is
	 * used to exclude uncessary files from compression. The idea is to be able
	 * to unzip, do some work and then only zip modified and new files.
	 */
	private Map filesList = null;
	/**
	 * This contains directory name to zip.<BR>
	 * It is removed from zip entry names because we want to be able to extract
	 * them everywhere.
	 */
	private String scratch;
	/**
	 * This tells whether or not to use <CODE>filesList</CODE> <BR>
	 * This is set to true by default, to force zip creation. Set it to false to
	 * zip differences only.
	 */
	private boolean creation;

	private final byte[] buffer = new byte[1024];

	/**
	 * This is the default constructor.
	 */
	public Zipper() {
		creation = true;
	}

	/**
	 * This is a constructor.
	 *
	 * @param fn
	 *            is the filename to be used
	 */
	public Zipper(final String fn) {
		this();
		fileName = fn;
	}

	public void setFileName(final String fn) {
		if (fn == null) {
			return;
		}

		fileName = fn;

		if (!fileName.startsWith(File.separator) && !fileName.startsWith(".") && (fileName.charAt(1) != ':')) {// win32
			// specific

			fileName = "." + File.separator + fileName;
		}
		logger.trace("seFileName " + fileName);
	}

	public String getFileName() {
		return fileName;
	}

	/**
	 * This sets the file list
	 *
	 * @see #filesList
	 */
	public void resetFilesList() {
		filesList = new HashMap();
	}

	/**
	 * This sets the file list
	 *
	 * @see #filesList
	 */
	public void setFilesList(final Map fl) {
		filesList = fl;
	}

	/**
	 * This sets the file list from the provided file If file is a directory the
	 * full content is put into filesLis
	 *
	 * @see #filesList
	 */
	public void setFilesList(final File root) {

		if (!root.exists()) {
			return;
		}

		if (!root.isDirectory()) {
			filesList.put(root.getName(), new Long(root.lastModified()));
			return;
		}

		final String[] list = root.list();

		for (int i = 0; i < list.length; i++) {
			final File file = new File(root, list[i]);
			if (file.isDirectory()) {
				setFilesList(file);
			} else {
				filesList.put(file.getName(), new Long(file.lastModified()));
			}
		}
	}

	public Map getFilesList() {
		return filesList;
	}

	public void setCreation(final boolean r) {
		creation = r;
	}

	public boolean getCreation() {
		return creation;
	}

	/**
	 * This tests whether file needs to be compressed in zip file
	 *
	 * @param file
	 *            is the file name to be included
	 */
	private boolean generate(final String dir, final String file) {
		if (creation) {
			return true;
		}
		if (filesList == null) {
			return true;
		}
		if (!filesList.containsKey(file)) {
			return true;
		}
		return (((Long) filesList.get(file)).longValue() < (new File(dir + File.separator + file)).lastModified());
	}

	/**
	 * This unzip file which file name has been provided. 27 avril 2006 : this
	 * uses ZipFile; some troubles have been reported which are (hopefully)
	 * solved with unzipNew()
	 *
	 * @param outDir
	 *            is the directory to explode zip file to
	 *
	 *            Since RPCXW : this may represent a non zipped file (see zip
	 *            ()) if there was a single 'little' file to zip
	 * @return true if correctly unzipped, false otherwise
	 * @see #zip(String[])
	 * @exception IOException
	 *                is thrown on I/O error
	 * @exception NullPointerException
	 *                is thrown if this fileName is not set
	 */
	public boolean unzip(final String outDir) throws IOException {

		if (fileName == null) {
			throw new IOException("file name not set");
		}

		filesList = new HashMap();

		final File outputDir = new File(outDir);
		XWTools.checkDir(outputDir);

		try {
			logger.debug("Unzipping : " + fileName + " to " + outDir);
			final File file = new File(fileName);
			if (!file.exists()) {
				logger.warn(fileName + " does not exist");
			}
			try (final ZipFile zipFile = new ZipFile(file)) {
				final Enumeration entries = zipFile.entries();
				while (entries.hasMoreElements()) {

					final ZipEntry entry = (ZipEntry) entries.nextElement();

					final InputStream in = zipFile.getInputStream(entry);
					final File f = new File(outDir, entry.getName());
					XWTools.checkDir(f.getParent());
					// amazing : I had such a case...
					// because a file name contained a french accentuation
					// :(
					if ((in == null) || (entry.isDirectory())) {
						logger.trace("Unzipping " + fileName + " : " + entry.getName() + " is not unzipped ...");
						continue;
					}

					try (final FileOutputStream out = new FileOutputStream(f)) {

						int len;
						while ((len = in.read(buffer)) >= 0) {
							out.write(buffer, 0, len);
						}
					}
					in.close();

					filesList.put(entry.getName(), new Long(f.lastModified()));
					logger.trace("Extracted file: " + entry.getName());
				}

				return true;
			}

		} catch (final ZipException z) {
			logger.error("Caught exception, not a zip file ? trying unzipNew", z);
			return unzipNew(outDir);
		}
	}

	/**
	 * This unzip file which file name has been provided.<br />
	 * 27 avril 2006 : this uses ZipInputStream since some troubles have been
	 * reported with unzip().
	 *
	 * @param outDir
	 *            is the directory to explode zip file to
	 *
	 *            Since RPCXW : this may represent a non zipped file (see zip
	 *            ()) if there was a single 'little' file to zip
	 * @return true if correctly unzipped, false otherwise
	 * @see #zip(String[])
	 * @exception IOException
	 *                is thrown on I/O error
	 * @exception NullPointerException
	 *                is thrown if this fileName is not set
	 */
	private boolean unzipNew(final String outDir) throws IOException {

		boolean ret = false;

		if (fileName == null) {
			throw new IOException("file name not set");
		}

		final File outputDir = new File(outDir);
		XWTools.checkDir(outputDir);

		filesList = new HashMap();

		BufferedOutputStream dest;
		ZipInputStream zis = null;
		ZipEntry entry;

		try (final FileInputStream fis = new FileInputStream(fileName)) {
			zis = new ZipInputStream(new BufferedInputStream(fis));
		} catch (final Exception z) {
			logger.error("Caught exception, definitely not a zip file ", z);
			return false;
		}

		while ((entry = zis.getNextEntry()) != null) {

			ret = true;

			int count;
			final byte[] data = new byte[BUFFERSIZE];

			FileOutputStream fos = null;
			try {
				final File f = new File(outDir, entry.getName());
				XWTools.checkDir(f.getParent());
				if (entry.isDirectory()) {
					throw new IOException();
				}
				fos = new FileOutputStream(f);
			} catch (final Exception e) {
				logger.error("Caught exception, Unzipping " + fileName + " : " + entry.getName() + " is not unzipped ...", e);
				continue;
			}

			dest = new BufferedOutputStream(fos, BUFFERSIZE);
			while ((count = zis.read(data, 0, BUFFERSIZE)) != -1) {
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
			logger.trace("Extracted entry : " + entry.getName());
		}

		zis.close();

		return ret;

	}

	/**
	 * This calls zip(inputs, true)
	 *
	 * @see #zip(String[], boolean)
	 */
	public boolean zip(final String[] inputs) throws IOException {
		return zip(inputs, true);
	}

	/**
	 * This zip files/directories provided as arguments. Accordingly to
	 * filesList, some files may not be zipped if they have previously been
	 * extracted from this zip file and not modified.
	 *
	 * If optimize is true we may not zip at all if inputs has only one file and
	 * if this file size is less than util.LONGFILESIZE, since zipping would
	 * then only be waste of CPU time. In such a case this.getFileName() returns
	 * the name of the unic file.
	 *
	 * @param inputs
	 *            is the directories/files list to compress
	 * @param optimize
	 *            tells to optimize zipping; if false this always zips
	 *            accordingly to filesList
	 * @return true if something has been zipped, false otherwise
	 * @since RPCXW
	 * @see #filesList
	 */
	public boolean zip(final String[] inputs, final boolean optimize) throws IOException {

		if ((optimize) && (inputs.length == 1)) {
			logger.trace("Zipper     inputs[0] = " + inputs[0]);
			final File inputFile = new File(inputs[0]);
			if (!inputFile.isDirectory()) {
				if (inputFile.length() < XWTools.LONGFILESIZE) {
					fileName = inputs[0];
					logger.trace("not necessary to zip = " + fileName + " (" + inputFile.length() + ")");
					return false;
				}
			} else {
				final String[] files = inputFile.list();
				switch (files.length) {
				case 0:
					fileName = null;
					logger.trace("not necessary to zip; no file found");
					return false;

				case 1:
					final File f2 = new File(inputFile, files[0]);
					if (!f2.isDirectory() && (f2.length() < XWTools.LONGFILESIZE)) {
						fileName = inputFile.getCanonicalPath() + File.separator + files[0];

						logger.trace("not necessary to zip file = " + fileName);
						return false;
					}
					break;
				}
			}
		}

		boolean atleastone = false;

		try (final FileOutputStream fos = new FileOutputStream(new File(fileName));
				final ZipOutputStream zipFile = new ZipOutputStream(fos)) {
			logger.debug("zipping to " + fileName);

			for (int i = 0; i < inputs.length; i++) {
				logger.trace("*** " + inputs[i]);
				scratch = inputs[i];
				atleastone = zip(zipFile, inputs[i]);
			}
		} catch (final ZipException ze) {
			if (atleastone) {
				throw ze;
			}
			fileName = null;
			return false;
		}
		return true;

	}

	/**
	 * This zip file/directory provided as argument. This is recursivly called.
	 * <BR>
	 * Zipping is done using <CODE>filesList</CODE> to test whether input has to
	 * be compressed or not.
	 *
	 * @param zipfile
	 *            is the file to add entry to
	 * @param input
	 *            is the current entry to add to zip file
	 * @return true if something has been zipped, false otherwise
	 */
	private boolean zip(final ZipOutputStream zipfile, final String input) throws IOException {

		if ((input == null) || (input.length() == 0)) {
			return false;
		}

		final File inputFile = new File(input);
		String[] tmpFiles;
		int loop;
		boolean atleastone = false;

		if (!inputFile.isDirectory()) {
			tmpFiles = new String[1];
			tmpFiles[0] = input;
		} else {
			tmpFiles = inputFile.list();
		}

		if (tmpFiles == null) {
			logger.debug("Zipping " + input + " nothing to do");
			return false;
		}

		logger.trace("Zipping " + input + " " + tmpFiles.length + " entries");

		if (tmpFiles.length == 0) {
			final int nLen = scratch.length();
			final String withoutScratch = input.substring(nLen);
			String entryName;

			if (withoutScratch.length() == 0) {
				entryName = input;
			} else {
				entryName = withoutScratch + "/" + input;
			}

			if (entryName.startsWith("/") || (entryName.indexOf("../") > 0)) {
				final int lastslash = entryName.lastIndexOf('/');
				if (lastslash < 0) {
					logger.warn("Invalid entry name " + entryName + "; ignoring");
					return false;
				}
				try {
					entryName = entryName.substring(lastslash + 1);
				} catch (final IndexOutOfBoundsException e) {
					logger.error("Caught exception: ", e);
					return false;
				}
				final String header = entryName.substring(0, lastslash);
				logger.warn("Invalid entry name " + entryName + "; removing header '" + header + "'");
			}
			logger.trace("Zipping put entry " + entryName);
			zipfile.putNextEntry(new ZipEntry(entryName + "/"));
			zipfile.closeEntry();
			return false;
		}

		for (loop = 0; loop < tmpFiles.length; loop++) {

			if (fileName.compareTo(input + "/" + tmpFiles[loop]) == 0) {
				continue;
			}

			if (generate(input, tmpFiles[loop])) {

				final int nLen = scratch.length();
				String withoutScratch = null;
				try {
					withoutScratch = input.substring(nLen + 1);
				} catch (final StringIndexOutOfBoundsException e) {
					withoutScratch = input.substring(nLen);
				}
				String entryName;

				if (withoutScratch.length() == 0) {
					entryName = tmpFiles[loop];
				} else {
					entryName = withoutScratch + "/" + tmpFiles[loop];
				}

				logger.trace("Zipping scratch = " + scratch + " withoutScratch = " + withoutScratch + " entryName = "
						+ entryName);

				if (entryName.startsWith("/") || (entryName.indexOf("../") > 0)) {
					final int lastslash = entryName.lastIndexOf('/');
					if (lastslash < 0) {
						logger.warn("Invalid entry name " + entryName + "; ignoring");
						return false;
					}
					String header = entryName.substring(0, lastslash);
					try {
						entryName = entryName.substring(lastslash + 1);
					} catch (final IndexOutOfBoundsException e) {
						logger.error("Caught exception: ", e);
						return false;
					}
					logger.warn("Invalid entry name " + entryName + "; removing header '" + header + "'");
					header = null;
				}

				File fileIn;

				if (inputFile.isDirectory()) {
					fileIn = new File(input + "/" + tmpFiles[loop]);
					logger.trace("Zipping directory : " + entryName + "(" + input + "/" + tmpFiles[loop] + ", "
							+ fileIn.length() + ")");
				} else {
					fileIn = new File(tmpFiles[loop]);
					logger.trace("Zipping file : " + entryName + "(" + tmpFiles[loop] + ", " + fileIn.length() + ")");
				}

				logger.trace("Zipping put entry " + entryName);
				if (!fileIn.isDirectory()) {

					zipfile.putNextEntry(new ZipEntry(entryName));
					final FileInputStream dataIn = new FileInputStream(fileIn);

					int len;

					while ((len = dataIn.read(buffer)) >= 0) {
						zipfile.write(buffer, 0, len);
					}

					dataIn.close();
				} else {
					zip(zipfile, input + "/" + tmpFiles[loop]);
					zipfile.putNextEntry(new ZipEntry(entryName + "/"));
				}

				zipfile.closeEntry();

				atleastone = true;

			} else {
				logger.trace("Zipping is skipping : " + input + "/" + tmpFiles[loop]);
			}

		}

		return atleastone;

	}

	/**
	 * This tests whether the provided argument is a zip file
	 */
	public static boolean test(final String fileName) throws IOException {

		final File testDir = new File(System.getProperty("java.io.tmpdir"),
				"XWHEP.Zipper.Test." + System.currentTimeMillis());
		XWTools.checkDir(testDir);
		final Zipper zipper = new Zipper();
		zipper.setCreation(false);
		zipper.setFileName(fileName);
		final boolean ret = zipper.unzip(testDir.getCanonicalPath());
		XWTools.deleteDir(testDir);
		return ret;
	}

	/**
	 * This main method is for test purposes only
	 * 
	 * @throws IOException
	 */
	public static void main(final String[] argv) throws IOException {
		Zipper.test(argv[0]);
	}
}
