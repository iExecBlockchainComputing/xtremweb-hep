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

package xtremweb.exec;

/**
 * JavaByteCodeExecutor.java
 *
 *
 * Created: Sat Feb 26 10:07:34 2005
 *
 * @author <a href="mailto:fedak@lri.fr">Gilles Fedak</a>
 * @version 1.0
 */

import java.io.File;
import org.apache.log4j.Logger;


public class JavaByteCodeExecutor extends Executor {

	private static final Logger logger = Logger.getLogger(JavaByteCodeExecutor.class);
	private final String jarNameOrClassName;
	private final String args;
	private final boolean isJar;
	private String jvm;
	private final String classPath;

	protected JavaByteCodeExecutor(final String cP, final String jNoC, final String argv, final boolean iJ)
			throws ExecutorLaunchException {
		jarNameOrClassName = jNoC;
		classPath = cP;
		args = argv;
		isJar = iJ;
		setJVM();
		setCmdLine();
	} // JavaByteCodeExecutor constructor

	private void setJVM() throws ExecutorLaunchException {
		try {
			jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		} catch (final Exception e) {
			logger.debug("Can't acces to property java.home");
			throw new ExecutorLaunchException();
		} // end of try-catch
	}

	private void setCmdLine() {
		// set the vm
		setCommandLine(jvm);

		// set the classpath
		if (classPath != null) {
			setCommandLine(getCommandLine() + (" -classpath " + classPath + " "));
		}

		// set hte jar switch jar
		if (isJar) {
			setCommandLine(getCommandLine() + " -jar ");
		}

		// set the class or jar name
		setCommandLine(getCommandLine() + jarNameOrClassName);

		// set the args
		if (args != null) {
			setCommandLine(getCommandLine() + (" " + args + " "));
		}
	}

} // JavaByteCodeExecutor
