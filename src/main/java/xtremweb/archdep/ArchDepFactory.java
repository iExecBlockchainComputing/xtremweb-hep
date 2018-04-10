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

//  ArchDepFactory.java
//  Created : Mon Mar 25 2002.

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import xtremweb.common.*;

/**
 * Provider of architecture dependent classes.
 *
 * <p>
 * <code>ArchDepFactory</code> is the key class of the package, it handles
 * Instantiation of interface implementations, and loading of JNI libraries
 * </p>
 *
 * @author Samuel H&eacute;riard
 *
 */

public class ArchDepFactory {

	private final Logger logger;

	private static final String JAVALIBPATH="java.library.path";

	/** Mapping between interface and implementations */
	private final Map<String, Map<OSEnum, String>> ifmap;

	/**
	 * Instances of architecture dependent classes, indexed by the interface
	 * name
	 */
	private final Map<String, Object> uniqueInstances;

	/** unique instance of the class */
	private static ArchDepFactory instance = new ArchDepFactory();

	/**
	 * This is the default constructor This creates all needed maps to retrieve
	 * Libraries accordingly to the OS
	 */
    protected ArchDepFactory() {

		logger = new Logger(this);
		logger.setLoggerLevel(LoggerLevel.FINEST);

		final Map<OSEnum, String> mapUtil = new Hashtable<OSEnum, String>(10);
		final Map<OSEnum, String> mapTracer = new Hashtable<OSEnum, String>(10);
		final Map<OSEnum, String> mapInterrupts = new Hashtable<OSEnum, String>(10);
		final Map<OSEnum, String> mapExec = new Hashtable<OSEnum, String>(10);
		final Map<OSEnum, String> mapPortmap = new Hashtable<OSEnum, String>(10);

		mapUtil.put(OSEnum.NONE, "xtremweb.archdep.XWUtilDummy");
		mapUtil.put(OSEnum.LINUX, "xtremweb.archdep.XWUtilLinux");
		mapUtil.put(OSEnum.SOLARIS, "xtremweb.archdep.XWUtilSolaris");
		mapUtil.put(OSEnum.WIN32, "xtremweb.archdep.XWUtilWin32");
		mapUtil.put(OSEnum.MACOSX, "xtremweb.archdep.XWUtilMacOSX");

		mapTracer.put(OSEnum.NONE, "xtremweb.archdep.XWTracerImpl");

		mapInterrupts.put(OSEnum.LINUX, "xtremweb.archdep.XWInterruptsLinux");
		mapInterrupts.put(OSEnum.WIN32, "xtremweb.archdep.XWInterruptsWin32");

		mapExec.put(OSEnum.NONE, "xtremweb.archdep.XWExecPureJava");
		mapExec.put(OSEnum.LINUX, "xtremweb.archdep.XWExecPureJava");

		mapPortmap.put(OSEnum.NONE, "xtremweb.archdep.PortMapper");
		mapPortmap.put(OSEnum.LINUX, "xtremweb.archdep.PortMapper");

		ifmap = new Hashtable<String, Map<OSEnum, String>>(10);
		ifmap.put("xtremweb.archdep.XWUtil", mapUtil);
		ifmap.put("xtremweb.archdep.XWTracerNative", mapTracer);
		ifmap.put("xtremweb.archdep.XWInterrupts", mapInterrupts);
		ifmap.put("xtremweb.archdep.XWExec", mapExec);
		ifmap.put("xtremweb.archdep.PortMapperItf", mapPortmap);

		uniqueInstances = new Hashtable<String, Object>(10);

		// force libraries loading at startup
		final StringBuilder loadingMessage = new StringBuilder();
		final String[] librairies = { "XWUtil", "XWInterrupts", "XwTracer", "XWExecJNI", "PortMapper" };
		for (int i = 0; i < librairies.length; i++) {
			loadingMessage.append(librairies[i] + ":" + (loadLibrary(librairies[i]) ? "Loaded; " : "Missing; "));
		}
		logger.info(loadingMessage.toString());
	}

	/**
	 * ArchDepFactory singleton
	 *
	 * @return the unique instance of ArchdepFactory
	 */
	public static ArchDepFactory getInstance() {
		return instance;
	}

	/**
	 * This retrieves the resource name of the JNI library Such names are
	 * composed as follow: "platforms/worker/"<os>"-"<cpu>"/jni/"<libname>".jni"
	 *
	 * @param lib
	 *            name of the library
	 * @return the name of the resource containing this library. The data can
	 *         later be retrieved using <code>getResource</code> or
	 *         <code>getResourceAsStream</code>
	 */
	protected static String mapLibraryName(final String lib) {

		final Version v = Version.currentVersion;
		final OSEnum os = OSEnum.getOs();
		final CPUEnum cpu = CPUEnum.getCpu();

		return "platforms/worker/" +
				os.toString().toLowerCase() + "-" + cpu.toString().toLowerCase() +
				"/jni/" + lib + ".jni";
//		return lib + ".jni." + v.full() + "." + os + "-" + cpu;
	}

	/**
	 * Loads a JNI library
	 *
	 * <p>
	 * <code>loadLibrary</code> uses <code>mapLibraryName</code> to find the
	 * actual name of the library, then it tries to load the library from the
	 * cache directory, or from the classloader if it's not already in the
	 * cache. The architecture names of the architecture chain are tested from
	 * the most specific to the most generic
	 * </p>
	 *
	 * @param s
	 *            name of the library
	 * @see #mapLibraryName(String)
	 */
	protected final boolean loadLibrary(final String s) {
		boolean loaded = false;

		try {
			final String libResName = mapLibraryName(s);

			logger.finest("libResName  = " + libResName);
			logger.finest("ArchDepFactory::loadLibrary (" + libResName + ") CACHEDIR = "
					+ System.getProperty(XWPropertyDefs.CACHEDIR.toString()));

			final File f = new File(System.getProperty(XWPropertyDefs.CACHEDIR.toString()), libResName);
			f.deleteOnExit();
            XWTools.checkDir(f.getParent());
			logger.finest("Copying " + libResName + " to " + f.getAbsolutePath());

			String libpath = System.getProperty(JAVALIBPATH);

			if (libpath == null) {
				libpath = "";
			}

			if ((System.getProperty(XWPropertyDefs.CACHEDIR.toString()) != null)
					&& (libpath.indexOf(System.getProperty(XWPropertyDefs.CACHEDIR.toString())) == -1)) {
				libpath = libpath.concat(File.pathSeparator + "." + File.pathSeparator
						+ System.getProperty(XWPropertyDefs.CACHEDIR.toString()) + File.pathSeparator
						+ f.getParentFile().getAbsolutePath());
			}

			System.setProperty(JAVALIBPATH, libpath);
			logger.finest("java.library.path = " + System.getProperty(JAVALIBPATH));

			if (f.exists()) {
				f.delete();
			}

			final String resname = libResName;
			writeRes(resname, f);

			if (f.exists()) {
				try {
					System.load(f.getAbsolutePath());
				} catch (final UnsatisfiedLinkError ule) {
					ule.printStackTrace();
					System.loadLibrary(libResName);
				}

				logger.finest("Loaded jni library : " + s);
				loaded = true;
			}

			if (!loaded) {
				logger.finest("Not loaded jni library : " + s);
			}

		} catch (final Throwable e) {
			e.printStackTrace();
			logger.warn(" can't load " + s + " : " + e);
		}
		return loaded;
	}

	private void writeRes(final String resName, final File f) throws IOException {
		
		try (final FileOutputStream lf = new FileOutputStream(f);
				final InputStream ls = getClass().getClassLoader().getResourceAsStream(resName)){

		    if (ls != null) logger.finest(resName + " bytes : " + ls.available());

                if ((ls != null) && (ls.available() > 0)) {
				final byte[] buf = new byte[1024];
				for (int n = ls.read(buf); n > 0; n = ls.read(buf)) {
					lf.write(buf, 0, n);
				}
			} else {
				logger.finest("Archive does not contains " + resName);
			}
		}
	}

	/**
	 * Maps a interface to its implementation
	 *
	 * @param ifname
	 *            of the interface
	 * @return the class implementing this interface
	 * @exception ArchDepException
	 *                if the implementation does not exists or can't be loaded
	 * @see #getUniqueInstance(String)
	 */
	public Class getClassForInterface(final String ifname) throws ArchDepException {

		String implclass = null;
		final Map<OSEnum, String> map = ifmap.get(ifname);
		final ArchDepException error = new ArchDepException("No implementation found for interface : " + ifname);
		if (map == null) {
			throw error;
		}

		logger.finest("ifname = " + ifname + "  OS = " + OSEnum.getOs());
		implclass = map.get(OSEnum.getOs());

		if (implclass == null) {
			implclass = map.get(OSEnum.NONE);
		}

		if (implclass == null) {
			throw error;
		}

		try {
			final Class result = Class.forName(implclass);
			return result;
		} catch (final ClassNotFoundException e) {
			logger.debug(e.getMessage());
			throw new ArchDepException(
					"Unable to load implementation for interface " + ifname + " : " + e.getMessage());
		}
	}

	/**
	 * Gets an instance of an implementation of an given interface.
	 *
	 * @param ifname
	 *            name of the interface
	 * @return a cached instance of the implementation of <code>ifname</code>.
	 *         Only one such instance will be created.
	 */
	public Object getUniqueInstance(final String ifname) {
		Object obj = uniqueInstances.get(ifname);
		if (obj == null) {
			try {
				final Class iface = Class.forName(ifname);
				obj = getClassForInterface(ifname).newInstance();
				if (!iface.isInstance(obj)) {
					throw new ArchDepException(
							"Invalid implementation : " + obj.getClass() + " does not implements " + ifname);
				}
				uniqueInstances.put(ifname, obj);
			} catch (final Exception e) {
				logger.debug(e.getMessage());
				obj = null;
				logger.error("Can't get implementation for " + ifname + " : " + e.getMessage());
			}
		}
		return obj;
	}

	/**
	 * Quick access to <code>XWUtil</code>
	 *
	 * @return an <code>XWUtil</code> instance or <code>null</code> if the
	 *         implementation could not be loaded
	 * @see #getUniqueInstance(String)
	 */
	public static XWUtil xwutil() {
		return (XWUtil) instance.getUniqueInstance("xtremweb.archdep.XWUtil");
	}

	/** Quick access to <code>XWTracerNative</code> */
	public static XWTracerNative xwtracer() {
		return (XWTracerNative) instance.getUniqueInstance("xtremweb.archdep.XWTracerNative");
	}

	/** Quick access to <code>XWInterrupts<code> */
	public static XWInterrupts xwinterrupts() {
		return (XWInterrupts) instance.getUniqueInstance("xtremweb.archdep.XWInterrupts");
	}

	/** Quick access to <code>XWExec</code> */
	public static XWExec xwexec() {
		try {
			return (XWExec) instance.getClassForInterface("xtremweb.archdep.XWExec").newInstance();
		} catch (final Exception e) {
			return null;
		}
	}

	/** Quick access to <code>PortMapper</code> */
	public static PortMapper portMap() {
		try {
			return (PortMapper) instance.getUniqueInstance("xtremweb.archdep.PortMapperItf");
		} catch (final Exception e) {
			System.err.println(e.toString());
			return null;
		}
	}

	public static void main(final String[] args) {

		if (ArchDepFactory.xwutil() == null) {
			System.out.println("Can't load xwutil library");
			System.exit(1);
		}

		try {
			System.out.println("speed proc = " + ArchDepFactory.xwutil().getSpeedProc());
		} catch (final UnsatisfiedLinkError e) {
		}
		try {
			System.out.println("num proc = " + Runtime.getRuntime().availableProcessors());
		} catch (final UnsatisfiedLinkError e) {
		}
		try {
			System.out.println("proc model = " + ArchDepFactory.xwutil().getProcModel());
		} catch (final UnsatisfiedLinkError e) {
		}
		try {
			System.out.println("total mem = " + ArchDepFactory.xwutil().getTotalMem());
		} catch (final UnsatisfiedLinkError e) {
		}
	}
}
