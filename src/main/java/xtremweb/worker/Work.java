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

package xtremweb.worker;

/**
 * Work.java
 *
 *
 * Created: Mon Jun  4 15:28:06 2001
 *
 * @author <a href="mailto:fedak@lri.fr">Gilles Fedak</a>
 */

import java.io.File;
import java.io.IOException;

import xtremweb.common.StatusEnum;
import xtremweb.common.WorkInterface;
import xtremweb.common.XWTools;

/**
 * This class describes a work to compute as managed by the worker
 */
public final class Work extends WorkInterface {

	/**
	 * This is the work description file name
	 */
	public static final String workDescName = "workDesc";

	/**
	 * This is the directory where the work is stored which name is <tmpDir>/XW.
	 * <userName>.<hostName>/works/<UID>/ This directory contains :
	 * <li>workDesc file (a text file describing the work) so that work can be
	 * resumed (see variable workDescName above);
	 * <li>scratch, the directory where the work is run so that it finds its I/O
	 * files and directories structure as eventually provided in its files;
	 * <li>the result file if computation run successfully which name is UID.zip
	 */
	private File scratchDir;

	/** The scratch dir name */
	private final String SCRATCHNAME = "scratch";

	/**
	 * This is this work data package name (worker shared data used by the data
	 * driven scheduler)
	 *
	 * @since 10.0.0
	 */
	private String dataPackageName;

	/**
	 * This creates a new work, providing its interface. This is typically
	 * called as a new work is provided by the server. This writes all necessary
	 * informations to disk, so that this work could be recreated, if necessary
	 *
	 * @see #Work(WorkInterface, boolean)
	 * @param job
	 *            describes this work
	 * @exception IOException
	 *                is thrown on disk I/O error
	 */
	public Work(final WorkInterface job) throws IOException {
		this(job, true);
	}

	/**
	 * This creates a new work writing, or not, on disk. This is needed on I/O
	 * problems so that we can send a ERROR message to the dispatcher
	 *
	 * @see PoolWork#addWork(WorkInterface)
	 * @see CommManager#run()
	 * @param job
	 *            is this work informations
	 * @param dir
	 *            tells to check directory structure or not
	 * @since XtremWeb 1.3.12
	 * @exception IOException
	 *                is thrown on disk I/O error
	 */
	public Work(final WorkInterface job, final boolean dir) throws IOException {

		super(job);

        contributeV = "0";
        contributeR = "0";
        contributeS = "0";
		dataPackageName = null;
		revealCalls = 0;
		contribCalls = 0;
		if (dir) {
			prepareDir();
		} else {
			clean();
		}
	}

	/**
	 * This creates needed directory structure accordingly to package, if any
	 *
	 * @throws IOException
	 */
	public void prepareDir() throws IOException {
		final String extension = "" + getUID().toString() + "_cwd";
		final File root = dataPackageName != null ? Worker.getConfig().getDataPackageDir(dataPackageName)
				: Worker.getConfig().getWorksDir();
		getLogger().debug("prepareDir packageName = " + dataPackageName + " ; " + root);
		if (dataPackageName == null) {
			setScratchDir(new File(root, extension));
		} else {
			setScratchDir(root);
		}
	}

	/**
	 * This creates needed directory structure
	 *
	 * @param v
	 *            is the base path
	 * @throws IOException
	 */
	private synchronized void setScratchDir(final File v) throws IOException {
		if (dataPackageName == null) {
			scratchDir = new File(v, SCRATCHNAME);
		} else {
			scratchDir = v;
		}
		getLogger().config("SCRATCHDIR = " + scratchDir);
		XWTools.checkDir(scratchDir);
		notifyAll();
	}

	/**
	 * This return the scratch directory
	 *
	 * @return
	 */
	public File getScratchDir() {
		return scratchDir;
	}

	/**
	 * This calls clean(true) to force cleaning
	 *
	 * @see #clean(boolean)
	 */
	public void clean() {
		clean(scratchDir, true);
	}

	/**
	 * This calls clean(scratchDir, force)
	 *
	 * @see #clean(File, boolean)
	 */
	public void clean(final boolean force) {
		if ((force == false) && (hasPackage() == true)) {
			getLogger().debug("don't clean " + force + ", " + hasPackage());
			return;
		}
		clean(scratchDir, force);
		try {
			scratchDir.delete();
			getLogger().debug("cleaning deleted " + scratchDir);
		} catch (final Exception e) {
			getLogger().exception("Work clean can't delete scratchdir", e);
		}
		try {
			scratchDir.getParentFile().delete();
			getLogger().debug("cleaning deleted " + scratchDir.getParent());
		} catch (final Exception e) {
			getLogger().exception("Work clean can't delete scratchdir parent", e);
		}
	}

	/**
	 * This erases provided file/directory from disk, except if dataPacakgeName
	 * is set
	 *
	 * @param dirWork
	 *            is the directory to clean
	 * @param force
	 *            is true to remove provided file/directory, even if
	 *            dataPackageName is set
	 * @see #dataPackageName
	 */
	private synchronized void clean(final File dirWork, final boolean force) {
		if (dirWork == null) {
			notifyAll();
			return;
		}
		if ((force == false) && (hasPackage() == true)) {
			getLogger().debug("do not clean " + force + ", " + hasPackage());
			return;
		}

		getLogger().debug("cleaning = " + dirWork);
		try {
			XWTools.deleteDir(dirWork);
		} catch (final Exception e) {
			getLogger().exception("Work clean error", e);
		}
		notifyAll();
	}

	/**
	 * This sets this work package name (worker shared data used by the data
	 * driven scheduler)
	 *
	 * @param pkg
	 *            is this work package name
	 * @throws IOException
	 * @since 10.0.0
	 */
	public void setDataPackage(final String pkg) throws IOException {
		clean(false);
		dataPackageName = pkg;
		prepareDir();
	}

	/**
	 * This retrieves this work package name (worker shared data used by the
	 * data driven scheduler)
	 *
	 * @since 10.0.0
	 */
	public String getDataPackage() {
		return dataPackageName;
	}

	/**
	 * This checks if this work has a package name
	 *
	 * @since 10.0.0
	 * @return true if packageName attribute is set; false otherwise
	 */
	public boolean hasPackage() {
		return (dataPackageName != null) && (dataPackageName.length() > 0);
	}

	public String getScratchDirName() throws IOException {
		return scratchDir.getAbsolutePath();
	}

	/**
     * This used by the SGX enclave challenge. This is optional ; default is "0"
     */
    private String contributeV;
    public String getContributeV(){
        return contributeV;
    }
    public void setContributeV(final String v) {
        contributeV = v;
    }
    /**
     * This used by the SGX enclave challenge. This is optional ; default is "0"
     */
    private String contributeR;
    public String getContributeR(){
        return contributeR;
    }
    public void setContributeR(final String r) {
        contributeR = r;
    }
    /**
     * This used by the SGX enclave challenge. This is optional ; default is "0"
     */
    private String contributeS;
    public String getContributeS(){
        return contributeS;
    }
    public void setContributeS(final String s) {
        contributeS = s;
    }

    private String hiddenh2r;
	public void setHiddenH2r(final String h2r) {
		hiddenh2r = h2r;
	}
	public String getHiddenH2r() {
		return hiddenh2r;
	}

	private int revealCalls;
	public void incRevealCalls() {
		revealCalls++;
	}
	public int getRevealCalls() {
		return revealCalls;
	}
	public boolean stopTryingRevealCall() {
		return revealCalls > 3;
	}
	private int contribCalls;
	public void incContribCalls() {
		contribCalls++;
	}
	public int getContribCalls() {
		return contribCalls;
	}
	public boolean stopTryingContribCall() {
		return contribCalls > 3;
	}
}
