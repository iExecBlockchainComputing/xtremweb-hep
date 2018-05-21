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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import java.text.ParseException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.communications.URI;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * AppInterface.java
 *
 * Created: Feb 19th, 2002
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

/**
 * This class describes a row of the apps SQL table.
 */
public final class AppInterface extends Table {

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.App
	 *
	 * @since 9.0.0
	 */
	public static final String APPTABLENAME = "apps";

	/**
	 * This the application name length as defined in DB
	 */
	public static final int APPNAMELENGTH = 100;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "app";

	/**
	 * This enumerates this interface columns. The base enumerations are defined
	 * in TableColumns.
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is the price is the author expects a remuneration for its application
		 * This is typically used through Blockchain
		 * @since 13.1.0
		 */
		PRICE {
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
		/**
		 * This is the column index of the application name, this is a unique
		 * index in the DB so that client can refer application with its name
		 * which is more user friendly than UID ;)
		 */
		NAME {
			/**
			 * This creates an object from String representation for this column
			 * value This cleans the parameter to ensure SQL compliance
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 */
			@Override
			public String fromString(final String v) {
				return v.replaceAll("[\\n\\s\'\"]+", "");
			}
		},
		/**
		 * This is the column index of the flag which tells whether this
		 * application is a service<br />
		 * A service is a java code inserted in the platform at compile time
		 * <br />
		 * At boot time, the server inserts its embedded services into
		 * database
		 *
		 * @since RPCXW
		 */
		ISSERVICE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 */
			@Override
			public Boolean fromString(final String v) {
				return Boolean.valueOf(v);
			}
		},
		/**
		 * This is the column index of the application type
		 *
		 * @since 8.0.0 (FG)
		 * @see AppTypeEnum
		 */
		TYPE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an ApplicationType representing the column value
			 */
			@Override
			public AppTypeEnum fromString(final String v) {
				return AppTypeEnum.valueOf(v.toUpperCase());
			}
		},
		/**
		 * This is the column index of the minimal free mass storage needed by
		 * the application. This is in Mb
		 *
		 * @since 9.0.0
		 */
		MINFREEMASSSTORAGE {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Long fromString(final String v) {
				return Long.valueOf(v);
			}
		},
		/**
		 * This is the average execution time; this is calculated as finished
		 * jobs return
		 */
		AVGEXECTIME {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the minimum memory needed to run job for
		 * this application. This is in Kb
		 */
		MINMEMORY {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the minimum CPU spped needed to run job
		 * for this application. This is in MHz
		 */
		MINCPUSPEED {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the number of executed jobs for this
		 * application
		 */
		NBJOBS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the pending job counter; this is updated
		 * on job submission
		 *
		 * @since 7.0.0
		 */
		PENDINGJOBS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the running job counter this is updated
		 * on succesfull worker request
		 *
		 * @since 7.0.0
		 */
		RUNNINGJOBS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the error job counter; this is updated on
		 * job error
		 *
		 * @since 7.0.0
		 */
		ERRORJOBS {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 */
			@Override
			public Integer fromString(final String v) {
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the application web page, if any
		 *
		 * @since 7.0.0
		 */
		WEBPAGE,
		/**
		 * This is the column index of the needed packages list. This is
		 * optional. This is a comma separated list of name packages. This is
		 * used by the scheduler. This must match worker SHAREDPACKAGES.
		 *
		 * @see xtremweb.database.SQLRequestWorkRequest
		 * @see HostInterface.Columns#SHAREDPACKAGES
		 * @since 8.0.0 (FG)
		 */
		NEEDEDPACKAGES,
		/**
		 * This is the column index of the environment variables This is
		 * optional This is a comma separated tuple NAME=VALUE[,NAME=VALUE]*
		 *
		 * @since 8.0.0 (FG)
		 */
		ENVVARS,
		/**
		 * This is the column index of the default stdin, if any. If this is
		 * set, any job with no stdin defined receives this
		 *
		 * @see WorkInterface.Columns#STDINURI
		 */
		DEFAULTSTDINURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the base dirin, if any. If this is set,
		 * all jobs receive this. If set, this is installed **after**
		 * defaultdirin or works.dirin to ensure those last do not override any
		 * file contained in basedirin
		 *
		 * @see WorkInterface.Columns#DIRINURI
		 */
		BASEDIRINURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the default dirin, if any. If this is
		 * set, any job with no dirin defined receives this This is installed
		 * before basedirin to ensure this does not override any of the
		 * basedirin files
		 *
		 * @see WorkInterface.Columns#DIRINURI
		 */
		DEFAULTDIRINURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux ix86 library, if any. This is a
		 * data URI.<br />
		 * I know this is ugly in DB point of view. I should rather create a
		 * data relation, but I'm affraid such a solution would overload the
		 * platform
		 */
		LDLINUX_IX86URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux x86_64 library, if any.
		 *
		 * @since 7.2.0
		 */
		LDLINUX_X86_64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux amd64 library, if any.
		 */
		LDLINUX_AMD64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux arm library
		 * @since 11.5.0
		 */
		LDLINUX_ARM32URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux arm library
		 * @since 11.5.0
		 */
		LDLINUX_ARM64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux intel itanuim library, if any.
		 *
		 * @since 7.0.0
		 */
		LDLINUX_IA64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux ppc library, if any.
		 */
		LDLINUX_PPCURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Mac OS X ix86 library, if any.
		 */
		LDMACOS_IX86URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Mac OS X x86_64 library, if any.
		 */
		LDMACOS_X86_64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Mac OS X ppc library, if any.
		 */
		LDMACOS_PPCURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Win32 ix86 library, if any.
		 */
		LDWIN32_IX86URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Win32 AMD64 library, if any.
		 */
		LDWIN32_AMD64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Win32 X86_64 library, if any.
		 */
		LDWIN32_X86_64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the SOLARIS alpha library, if any.
		 */
		LDSOLARIS_ALPHAURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the SOLARIS alpha library, if any.
		 */
		LDSOLARIS_SPARCURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux ix86 binary, if any.
		 */
		LINUX_IX86URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux amd64 binary, if any.
		 */
		LINUX_AMD64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux arm64 binary
		 * @since 11.5.0
		 */
		LINUX_ARM64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux arm32 binary
		 * @since 11.5.0
		 */
		LINUX_ARM32URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux x86_64 binary, if any.
		 *
		 * @since 7.2.0
		 */
		LINUX_X86_64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux intel itanium binary, if any.
		 *
		 * @since 7.0.0
		 */
		LINUX_IA64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the linux ppc binary, if any.
		 */
		LINUX_PPCURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Mac OS X ix86 binary, if any.
		 */
		MACOS_IX86URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Mac OS X x86_64 binary, if any.
		 */
		MACOS_X86_64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Mac OS X ppc binary, if any.
		 */
		MACOS_PPCURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Win32 ix86 binary, if any.
		 */
		WIN32_IX86URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Win32 amd64 binary, if any.
		 */
		WIN32_AMD64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Win32 X86_64 binary, if any.
		 */
		WIN32_X86_64URI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the Java binary, if any.
		 */
		JAVAURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the SOLARIS alpha binary, if any.
		 */
		SOLARIS_ALPHAURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		},
		/**
		 * This is the column index of the SOLARIS alpha binary, if any.
		 */
		SOLARIS_SPARCURI {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an URI representing the column value
			 * @throws URISyntaxException
			 *             is thrown on instantiation error
			 */
			@Override
			public URI fromString(final String v) throws URISyntaxException {
				return new URI(v);
			}
		};
		/**
		 * This is the index based on ordinal so that the first value is
		 * TableColumns + 1
		 *
		 * @see Enum#ordinal()
		 * @since 8.2.0
		 */
		private int ord;

		/**
		 * This constructor sets the ord member as ord = this.ordinal +
		 * TableColumns.SIZE
		 *
		 * @since 8.2.0
		 */
		Columns() {
			ord = this.ordinal() + TableColumns.SIZE;
		}

		/**
		 * This retrieves the index based ordinal
		 *
		 * @return the index based ordinal
		 * @since 8.2.0
		 */
		@Override
		public int getOrdinal() {
			return ord;
		}

		/**
		 * This creates a new object from String for the given column This must
		 * be overridden by enum which value is not a String
		 *
		 * @param v
		 *            the String representation
		 * @return v
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		@Override
		public Object fromString(final String v) throws Exception {
			return v;
		}

		/**
		 * This creates a new object from the digen SQL result set
		 *
		 * @param rs
		 *            the SQL result set
		 * @return the object representing the column
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		public final Object fromResultSet(final ResultSet rs) throws Exception {
			return this.fromString(rs.getString(this.toString()));
		}

		/**
		 * This retrieves an Columns from its integer value
		 *
		 * @param v
		 *            is the integer value of the Columns
		 * @return an Columns
		 */
		public static XWBaseColumn fromInt(final int v) throws IndexOutOfBoundsException {
			try {
				return TableColumns.fromInt(v);
			} catch (final Exception e) {
			}
			for (final Columns c : Columns.values()) {
				if (c.getOrdinal() == v) {
					return c;
				}
			}
			throw new IndexOutOfBoundsException(("unvalid Columns value ") + v);
		}
	}

	/**
	 * This is the size, including TableColumns
	 */
	private static final int ENUMSIZE = Columns.values().length;

	/**
	 * This is the default constructor
	 */
	public AppInterface() {

		super(THISTAG, APPTABLENAME);

		setAttributeLength(ENUMSIZE);

		setService(false);
		setType(AppTypeEnum.DEPLOYABLE);
		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.NAME.getOrdinal() });
	}

	/**
	 * This constructs a new object providing its primary key value
	 *
	 * @param uid
	 *            is this new object UID
	 */
	public AppInterface(final UID uid) throws IOException {
		this();
		setUID(uid);
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 */
	public AppInterface(final SQLRequest r) {
		this();
		setRequest(r);
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException on error
	 */
	public AppInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This retrieves column label from enum Columns. This takes cares of this
	 * version. If this version is null, this version is prior to 5.8.0. Before
	 * 5.8.0, OWNERUID and ACCESSRIGHTS did not exist. Then this returns null
	 * for these two columns.
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @since 5.8.0
	 * @return null if((version == null) &amp;&amp; ((i ==
	 *         MACOS_X86_64URI.ordinal()) || (c ==
	 *         LDMACOS_X86_64URI.ordinal()))); column label otherwise
	 */
	@Override
	public String getColumnLabel(final int i) throws IndexOutOfBoundsException {
		try {
			return TableColumns.fromInt(i).toString();
		} catch (final Exception e) {
		}
		return Columns.fromInt(i).toString();
	}
	/**
	 * This fills columns from DB
	 *
	 * @since 9.0.0
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException on error
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setUID((UID) TableColumns.UID.fromResultSet(rs));
			setOwner((UID) TableColumns.OWNERUID.fromResultSet(rs));
			setAccessRights((XWAccessRights) TableColumns.ACCESSRIGHTS.fromResultSet(rs));
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}

		try {
			setWebPage((URL) Columns.WEBPAGE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setJava((URI) Columns.JAVAURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setWin32_ix86((URI) Columns.WIN32_IX86URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setWin32_amd64((URI) Columns.WIN32_AMD64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setWin32_x86_64((URI) Columns.WIN32_X86_64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_ix86((URI) Columns.LINUX_IX86URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_amd64((URI) Columns.LINUX_AMD64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_arm64((URI) Columns.LINUX_ARM64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_arm32((URI) Columns.LINUX_ARM32URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_x86_64((URI) Columns.LINUX_X86_64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_ia64((URI) Columns.LINUX_IA64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLinux_ppc((URI) Columns.LINUX_PPCURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setMacos_ix86((URI) Columns.MACOS_IX86URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setMacos_x86_64((URI) Columns.MACOS_X86_64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setMacos_ppc((URI) Columns.MACOS_PPCURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setSolaris_sparc((URI) Columns.SOLARIS_SPARCURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setSolaris_alpha((URI) Columns.SOLARIS_ALPHAURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDWin32_ix86((URI) Columns.LDWIN32_IX86URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDWin32_amd64((URI) Columns.LDWIN32_AMD64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDWin32_x86_64((URI) Columns.LDWIN32_X86_64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_ix86((URI) Columns.LDLINUX_IX86URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_amd64((URI) Columns.LDLINUX_AMD64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_arm64((URI) Columns.LDLINUX_ARM64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_arm32((URI) Columns.LDLINUX_ARM32URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_x86_64((URI) Columns.LDLINUX_X86_64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_ia64((URI) Columns.LDLINUX_IA64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDLinux_ppc((URI) Columns.LDLINUX_PPCURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDMacos_ix86((URI) Columns.LDMACOS_IX86URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDMacos_x86_64((URI) Columns.LDMACOS_X86_64URI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDMacos_ppc((URI) Columns.LDMACOS_PPCURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDSolaris_sparc((URI) Columns.LDSOLARIS_SPARCURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLDSolaris_alpha((URI) Columns.LDSOLARIS_ALPHAURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setDefaultStdin((URI) Columns.DEFAULTSTDINURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setDefaultDirin((URI) Columns.DEFAULTDIRINURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setBaseDirin((URI) Columns.BASEDIRINURI.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setNbJobs((Integer) Columns.NBJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setPendingJobs((Integer) Columns.PENDINGJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setRunningJobs((Integer) Columns.RUNNINGJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setErrorJobs((Integer) Columns.ERRORJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setAvgExecTime((Integer) Columns.AVGEXECTIME.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setMinMemory((Integer) Columns.MINMEMORY.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setMinCpuSpeed((Integer) Columns.MINCPUSPEED.fromResultSet(rs));
		} catch (final Exception e) {
		}
        try {
            setMinFreeMassStorage((Long) Columns.MINFREEMASSSTORAGE.fromResultSet(rs));
        } catch (final Exception e) {
        }
        try {
            setPrice((Long) Columns.PRICE.fromResultSet(rs));
        } catch (final Exception e) {
        }
		try {
			setEnvVars((String) Columns.ENVVARS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setNeededPackages((String) Columns.NEEDEDPACKAGES.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setName((String) Columns.NAME.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setService((Boolean) Columns.ISSERVICE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setType((AppTypeEnum) Columns.TYPE.fromResultSet(rs));
		} catch (final Exception e) {
		}

		setDirty(false);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public AppInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #AppInterface(InputStream)
	 */
	public AppInterface(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 * @see XMLReader#read(InputStream)
	 */
	public AppInterface(final InputStream input) throws IOException, SAXException {
		this();
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			getLogger().exception(e);
		}
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @see Table#fromXml(Attributes)
	 */
	public AppInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This updates this object from interface.
	 *
	 * @since 9.0.0
	 */
	@Override
	public void updateInterface(final Table appitf) throws IOException {
		final AppInterface itf = (AppInterface) appitf;
		if (itf.getName() != null) {
			setName(itf.getName());
		}
		setService(itf.isService());
		setAccessRights(itf.getAccessRights());
		setMinMemory(itf.getMinMemory());
		setMinCpuSpeed(itf.getMinCpuSpeed());
		setMinFreeMassStorage(itf.getMinFreeMassStorage());
		setPrice(itf.getPrice());
		setEnvVars(itf.getEnvVars());
		setNeededPackages(itf.getNeededPackages());
		setType(itf.getType());
		setDefaultStdin(itf.getDefaultStdin());
		setBaseDirin(itf.getBaseDirin());
		setDefaultDirin(itf.getDefaultDirin());
	}

	/**
	 * This tests an attribute
	 *
	 * @return true if this is attribute is set, false otherwise
	 */
	public boolean isService() {
		try {
			return ((Boolean) getValue(Columns.ISSERVICE)).booleanValue();
		} catch (final Exception e) {
			setService(false);
			return false;
		}
	}

	/**
	 * This retrieves the web page
	 *
	 * @return the URL of the web page
	 * @since 7.0.0
	 */
	public URL getWebPage() {
		return (URL) getValue(Columns.WEBPAGE);
	}

	/**
	 * This retrieves the URI to get application binary accordingly to OS and
	 * CPU. If JAVA binary is set, java binary URI is returned independently of
	 * OS and CPU
	 *
	 * @param cpu
	 *            is the cpu for the expected binary
	 * @param os
	 *            is the os for the expected binary
	 * @return binary URI for the given OS and CPU; otherwise null if no binary
	 *         available the the given OS and CPU
	 */
	public URI getBinary(final CPUEnum cpu, final OSEnum os) {

		if (getJava() != null) {
			return getJava();
		}
		if ((os == null) || (cpu == null)) {
			return null;
		}
		switch (os) {
		case LINUX:
			switch (cpu) {
			case IX86:
				return getLinuxIX86();
			case PPC:
				return getLinuxPpc();
			case AMD64:
				return getLinuxAmd64();
			case ARM64:
				return getLinuxArm64();
			case ARM32:
				return getLinuxArm32();
			case X86_64:
				return getLinuxX8664();
			case IA64:
				return getLinuxIA64();
			default:
				break;
			}
			break;
		case WIN32:
			switch (cpu) {
			case IX86:
				return getWin32IX86();
			case AMD64:
				return getWin32Amd64();
			case X86_64:
				return getWin32X8664();
			default:
				break;
			}
			break;
		case MACOSX:
			switch (cpu) {
			case IX86:
				return getMacosIX86();
			case X86_64:
				return getMacosX8664();
			case PPC:
				return getMacosPpc();
			default:
				break;
			}
			break;
		case SOLARIS:
			switch (cpu) {
			case SPARC:
				return getSolarisSparc();
			case ALPHA:
				return getSolarisAlpha();
			default:
				break;
			}
			break;
		default:
			break;
		}
		return null;
	}

	/**
	 * This retrieves the URI to get application binary accordingly to OS and
	 * CPU. This does **not** test JAVAURI
	 *
	 * @param cpu
	 *            is the cpu for the expected binary
	 * @param os
	 *            is the os for the expected binary
	 * @return binary URI for the given OS and CPU; otherwise null if no binary
	 *         available the the given OS and CPU
	 * @since 5.8.0
	 */
	public static String getBinaryField(final CPUEnum cpu, final OSEnum os) {

		if ((os == null) || (cpu == null)) {
			return null;
		}
		switch (os) {
		case LINUX:
			switch (cpu) {
			case IX86:
				return Columns.LINUX_IX86URI.toString();
			case PPC:
				return Columns.LINUX_PPCURI.toString();
			case AMD64:
				return Columns.LINUX_AMD64URI.toString();
			case ARM64:
				return Columns.LINUX_ARM64URI.toString();
			case ARM32:
				return Columns.LINUX_ARM32URI.toString();
			case X86_64:
				return Columns.LINUX_X86_64URI.toString();
			case IA64:
				return Columns.LINUX_IA64URI.toString();
			default:
				break;
			}
			break;
		case WIN32:
			switch (cpu) {
			case IX86:
				return Columns.WIN32_IX86URI.toString();
			case AMD64:
				return Columns.WIN32_AMD64URI.toString();
			case X86_64:
				return Columns.WIN32_X86_64URI.toString();
			default:
				break;
			}
			break;
		case MACOSX:
			switch (cpu) {
			case IX86:
				return Columns.MACOS_IX86URI.toString();
			case X86_64:
				return Columns.MACOS_X86_64URI.toString();
			case PPC:
				return Columns.MACOS_PPCURI.toString();
			default:
				break;
			}
			break;
		case SOLARIS:
			switch (cpu) {
			case SPARC:
				return Columns.SOLARIS_SPARCURI.toString();
			case ALPHA:
				return Columns.SOLARIS_ALPHAURI.toString();
			default:
				break;
			}
			break;
		default:
			break;
		}
		return null;
	}

	/**
	 * This retrieves the URI to get application library accordingly to OS ane
	 * CPU
	 *
	 * @param cpu
	 *            is the cpu for the expected binary
	 * @param os
	 *            is the os for the expected binary
	 * @return library URI or null
	 */
	public URI getLibrary(final CPUEnum cpu, final OSEnum os) {

		if ((os == null) || (cpu == null)) {
			return null;
		}
		switch (os) {
		case LINUX:
			switch (cpu) {
			case IX86:
				return getLDLinuxIX86();
			case PPC:
				return getLDLinuxPpc();
			case AMD64:
				return getLDLinuxAmd64();
			case X86_64:
				return getLDLinuxX8664();
			case IA64:
				return getLDLinuxIA64();
			default:
				break;
			}
			break;
		case WIN32:
			switch (cpu) {
			case IX86:
				return getLDWin32IX86();
			case AMD64:
				return getLDWin32Amd64();
			case X86_64:
				return getLDWin32X8664();
			default:
				break;
			}
			break;
		case MACOSX:
			switch (cpu) {
			case IX86:
				return getLDMacosIX86();
			case X86_64:
				return getLDMacosX8664();
			case PPC:
				return getLDMacosPpc();
			default:
				break;
			}
			break;
		case SOLARIS:
			switch (cpu) {
			case SPARC:
				return getLDSolarisSparc();
			case ALPHA:
				return getLDSolarisAlpha();
			default:
				break;
			}
			break;
		default:
			break;
		}
		return null;
	}

	/**
	 * This retrieves the URI to get application binary for linux ix86
	 *
	 * @return a data URI
	 */
	public URI getLinuxIX86() {
		return (URI) getValue(Columns.LINUX_IX86URI);
	}

	/**
	 * This retrieves the URI to get application binary for linux amd64
	 * @return a data URI
	 */
	public URI getLinuxAmd64() {
		return (URI) getValue(Columns.LINUX_AMD64URI);
	}
	/**
	 * This retrieves the URI to get application binary for linux arm64
	 * @return a data URI
	 * @since 11.5.0
	 */
	public URI getLinuxArm64() {
		return (URI) getValue(Columns.LINUX_ARM64URI);
	}
	/**
	 * This retrieves the URI to get application binary for linux arm32
	 * @return a data URI
	 * @since 11.5.0
	 */
	public URI getLinuxArm32() {
		return (URI) getValue(Columns.LINUX_ARM32URI);
	}
	/**
	 * This retrieves the URI to get application binary for linux x86 64
	 *
	 * @return a data URI
	 * @since 7.2.0
	 */
	public URI getLinuxX8664() {
		return (URI) getValue(Columns.LINUX_X86_64URI);
	}

	/**
	 * This retrieves the URI to get application binary for linux amd64
	 *
	 * @return a data URI
	 * @since 7.0.0
	 */
	public URI getLinuxIA64() {
		return (URI) getValue(Columns.LINUX_IA64URI);
	}

	/**
	 * This retrieves the URI to get application binary for linux ppc
	 *
	 * @return a data URI
	 */
	public URI getLinuxPpc() {
		return (URI) getValue(Columns.LINUX_PPCURI);
	}

	/**
	 * This retrieves the URI to get application binary for win32 ix86
	 *
	 * @return a data URI
	 */
	public URI getWin32IX86() {
		return (URI) getValue(Columns.WIN32_IX86URI);
	}

	/**
	 * This retrieves the URI to get application binary for win32 amd64
	 *
	 * @return a data URI
	 */
	public URI getWin32Amd64() {
		return (URI) getValue(Columns.WIN32_AMD64URI);
	}

	/**
	 * This retrieves the URI to get application binary for win32 x86_64
	 *
	 * @return a data URI
	 * @since 6.0.0
	 */
	public URI getWin32X8664() {
		return (URI) getValue(Columns.WIN32_X86_64URI);
	}

	/**
	 * This retrieves the URI to get application binary for mac os ix86
	 *
	 * @return a data URI
	 */
	public URI getMacosIX86() {
		return (URI) getValue(Columns.MACOS_IX86URI);
	}

	/**
	 * This retrieves the URI to get application binary for mac os x86_64
	 *
	 * @return a data URI
	 * @since 5.8.0
	 */
	public URI getMacosX8664() {
		return (URI) getValue(Columns.MACOS_X86_64URI);
	}

	/**
	 * This retrieves the URI to get application binary for mac os ppc
	 *
	 * @return a data URI
	 */
	public URI getMacosPpc() {
		return (URI) getValue(Columns.MACOS_PPCURI);
	}

	/**
	 * This retrieves the URI to get application binary for java
	 *
	 * @return a data URI
	 */
	public URI getJava() {
		return (URI) getValue(Columns.JAVAURI);
	}

	/**
	 * This retrieves the URI to get application binary for solaris sparc
	 *
	 * @return a data URI
	 */
	public URI getSolarisSparc() {
		return (URI) getValue(Columns.SOLARIS_SPARCURI);
	}

	/**
	 * This retrieves the URI to get application binary for solaris alpha
	 *
	 * @return a data URI
	 */
	public URI getSolarisAlpha() {
		return (URI) getValue(Columns.SOLARIS_ALPHAURI);
	}

	/**
	 * This retrieves the URI to get application library for linux ix86
	 *
	 * @return a data URI
	 */
	public URI getLDLinuxIX86() {
		return (URI) getValue(Columns.LDLINUX_IX86URI);
	}
	/**
	 * This retrieves the URI to get application library for linux amd64
	 * @return a data URI
	 */
	public URI getLDLinuxAmd64() {
		return (URI) getValue(Columns.LDLINUX_AMD64URI);
	}
	/**
	 * This retrieves the URI to get application library for linux arm64
	 * @return a data URI
	 * @since 11.5.0
	 */
	public URI getLDLinuxArm64() {
		return (URI) getValue(Columns.LDLINUX_ARM64URI);
	}
	/**
	 * This retrieves the URI to get application library for linux arm32
	 * @return a data URI
	 * @since 11.5.0
	 */
	public URI getLDLinuxArm32() {
		return (URI) getValue(Columns.LDLINUX_ARM32URI);
	}
	/**
	 * This retrieves the URI to get application library for linux x86 64
	 *
	 * @return a data URI
	 * @since 7.2.0
	 */
	public URI getLDLinuxX8664() {
		return (URI) getValue(Columns.LDLINUX_X86_64URI);
	}

	/**
	 * This retrieves the URI to get application library for linux amd64
	 *
	 * @return a data URI
	 * @since 7.0.0
	 */
	public URI getLDLinuxIA64() {
		return (URI) getValue(Columns.LDLINUX_IA64URI);
	}

	/**
	 * This retrieves the URI to get application library for linux ppc
	 *
	 * @return a data URI
	 */
	public URI getLDLinuxPpc() {
		return (URI) getValue(Columns.LDLINUX_PPCURI);
	}

	/**
	 * This retrieves the URI to get application library for win32 ix86
	 *
	 * @return a data URI
	 */
	public URI getLDWin32IX86() {
		return (URI) getValue(Columns.LDWIN32_IX86URI);
	}

	/**
	 * This retrieves the URI to get application library for win32 amd64
	 *
	 * @return a data URI
	 */
	public URI getLDWin32Amd64() {
		return (URI) getValue(Columns.LDWIN32_AMD64URI);
	}

	/**
	 * This retrieves the URI to get application library for win32 X86_64
	 *
	 * @return a data URI
	 * @since 6.0.0
	 */
	public URI getLDWin32X8664() {
		return (URI) getValue(Columns.LDWIN32_X86_64URI);
	}

	/**
	 * This retrieves the URI to get application library for mac os ix86
	 *
	 * @return a data URI
	 */
	public URI getLDMacosIX86() {
		return (URI) getValue(Columns.LDMACOS_IX86URI);
	}

	/**
	 * This retrieves the URI to get application library for mac os x86_64
	 *
	 * @return a data URI
	 */
	public URI getLDMacosX8664() {
		return (URI) getValue(Columns.LDMACOS_X86_64URI);
	}

	/**
	 * This retrieves the URI to get application library for mac os ppc
	 *
	 * @return a data URI
	 */
	public URI getLDMacosPpc() {
		return (URI) getValue(Columns.LDMACOS_PPCURI);
	}

	/**
	 * This retrieves the URI to get application library for solaris sparc
	 *
	 * @return a data URI
	 */
	public URI getLDSolarisSparc() {
		return (URI) getValue(Columns.LDSOLARIS_SPARCURI);
	}

	/**
	 * This retrieves the URI to get application library for solaris alpha
	 *
	 * @return a data URI
	 */
	public URI getLDSolarisAlpha() {
		return (URI) getValue(Columns.LDSOLARIS_ALPHAURI);
	}

	/**
	 * This get the number of jobs; if not set it is forced to 0
	 *
	 * @return nbjobs or 0 if not set
	 */
	public int getNbJobs() {
		final Integer ret = (Integer) getValue(Columns.NBJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setNbJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of pending jobs<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 * @since 7.0.0
	 */
	public int getPendingJobs() {
		final Integer ret = (Integer) getValue(Columns.PENDINGJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setPendingJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of running jobs<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 * @since 7.0.0
	 */
	public int getRunningJobs() {
		final Integer ret = (Integer) getValue(Columns.RUNNINGJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setRunningJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of erroneus jobs<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 * @since 7.0.0
	 */
	public int getErrorJobs() {
		final Integer ret = (Integer) getValue(Columns.ERRORJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setErrorJobs(0);
		return 0;
	}

	/**
	 * This get the average execution time; if not set it is forced to 0
	 *
	 * @return exec average time or 0 if not set
	 */
	public int getAvgExecTime() {
		final Integer ret = (Integer) getValue(Columns.AVGEXECTIME);
		if (ret != null) {
			return ret.intValue();
		}
		setAvgExecTime(0);
		return 0;
	}

	/**
	 * This retrieves the mimimum RAM needed for this application; if not set it
	 * is forced to 0 This is ni Kb
	 *
	 * @return the mimimum RAM needed for this application in Kb
	 */
	public int getMinMemory() {
		final Integer ret = (Integer) getValue(Columns.MINMEMORY);
		if (ret != null) {
			return ret.intValue();
		}
		setMinMemory(0);
		return 0;
	}

	/**
	 * This retrieves the mimimal CPU speed for this application; if not set it
	 * is forced to 0 This is in MHz
	 *
	 * @return the mimimal CPU speed for this application in MHz
	 */
	public int getMinCpuSpeed() {
		final Integer ret = (Integer) getValue(Columns.MINCPUSPEED);
		if (ret != null) {
			return ret.intValue();
		}
		setMinCpuSpeed(0);
		return 0;
	}

    /**
     * This retrieves the minimum RAM for this application. If not set it is
     * forced to 0
     *
     * @return the minimal disk space needed for this application in Mb
     * @since 9.0.5
     */
    public Long getMinFreeMassStorage() {
        final Long ret = (Long) getValue(Columns.MINFREEMASSSTORAGE);
        if (ret != null) {
            return ret.longValue();
        }
        setMinFreeMassStorage(0);
        return 0L;
    }
    /**
     * This retrieves the price for this application
     *
     * @return the price
     * @since 13.1.0
     */
    public Long getPrice() {
        final Long ret = (Long) getValue(Columns.PRICE);
        if (ret != null) {
            return ret.longValue();
        }
        return 0L;
    }
    /**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 * @since 8.0.0 (FG)
	 */
	public String getEnvVars() {
		try {
			return (String) getValue(Columns.ENVVARS);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 * @since 8.0.0 (FG)
	 */
	public String getNeededPackages() {
		try {
			return (String) getValue(Columns.NEEDEDPACKAGES);
		} catch (final Exception e) {
		}
		return null;
	}
	/**
	 * This gets this application name
	 *
	 * @return this application name
	 */
	public String getName() {
		try {
			return (String) getValue(Columns.NAME);
		} catch (final Exception e) {
		}
		return null;
	}
	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public URI getDefaultStdin() {
		return (URI) getValue(Columns.DEFAULTSTDINURI);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public URI getBaseDirin() {
		return (URI) getValue(Columns.BASEDIRINURI);
	}
	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public URI getDefaultDirin() {
		return (URI) getValue(Columns.DEFAULTDIRINURI);
	}

	/**
	 * This retrieves if this application can be run with local binary on worker
	 * side (a binary that is not downloaded by the worker)
	 *
	 * @return this attribute
	 * @since 8.0.0 (FG)
	 */
	public AppTypeEnum getType() {
		try {
			return (AppTypeEnum) getValue(Columns.TYPE);
		} catch (final Exception e) {
		}
		setType(AppTypeEnum.NONE);
		return AppTypeEnum.NONE;
	}
    /**
     * This retrieves launch script
     * @return launch script or null
     * @see AppTypeEnum#getLaunchScriptName()
     * @since 13.0.0
     */
    public String getLaunchScript() throws AccessControlException {
        return getType().getLaunchScriptName();
    }
    /**
     * This retrieves unload script
     * @return unload script or null
     * @see AppTypeEnum#getUnloadScriptName()
     * @since 13.0.0
     */
    public String getUnloadScript() throws AccessControlException {
        return getType().getUnloadScriptName();
    }
	/**
	 * This sets parameter value; this is called from
	 * TableInterface#fromXml(Attributes)
	 *
	 * @param attribute
	 *            is the name of the attribute to set
	 * @param v
	 *            is the new attribute value
	 * @return true if value has changed, false otherwise
	 * @see Table#fromXml(Attributes)
	 */
	@Override
	public final boolean setValue(final String attribute, final Object v) {
		final String uppercaseAttribute = attribute.toUpperCase();
		try {
			return setValue(TableColumns.valueOf(uppercaseAttribute), v);
		} catch (final Exception e) {
			return setValue(Columns.valueOf(uppercaseAttribute), v);
		}
	}

	/**
	 * This sets the URI to retrieve application binary for linux ix86
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setBinary(final CPUEnum cpu, final OSEnum os, final URI v) throws ParseException {

		if (os == null) {
			throw new ParseException("unknown OS", 0);
		}
		if ((cpu == null) && (os != OSEnum.JAVA)) {
			throw new ParseException("unknown CPU", 0);
		}
		switch (os) {
		case LINUX:
			switch (cpu) {
			case IX86:
				return setLinux_ix86(v);
			case PPC:
				return setLinux_ppc(v);
			case AMD64:
				return setLinux_amd64(v);
			case ARM64:
				return setLinux_arm64(v);
			case ARM32:
				return setLinux_arm32(v);
			case X86_64:
				return setLinux_x86_64(v);
			case IA64:
				return setLinux_ia64(v);
			default:
				break;
			}
			break;
		case WIN32:
			switch (cpu) {
			case IX86:
				return setWin32_ix86(v);
			case AMD64:
				return setWin32_amd64(v);
			case X86_64:
				return setWin32_x86_64(v);
			default:
				break;
			}
			break;
		case MACOSX:
			switch (cpu) {
			case IX86:
				return setMacos_ix86(v);
			case X86_64:
				return setMacos_x86_64(v);
			case PPC:
				return setMacos_ppc(v);
			default:
				break;
			}
			break;
		case SOLARIS:
			switch (cpu) {
			case SPARC:
				return setSolaris_sparc(v);
			case ALPHA:
				return setSolaris_alpha(v);
			default:
				break;
			}
			break;
		case JAVA:
			return setJava(v);
		default:
			break;
		}
		throw new ParseException("invalid OS or CPU type", 0);
	}

	/**
	 * This sets the URI to retrieve application library accordingly to
	 * parameters
	 *
	 * @param cpu
	 *            is the cpu type
	 * @param os
	 *            is the OS type
	 * @param v
	 *            is the library URI
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLibrary(final CPUEnum cpu, final OSEnum os, final URI v) throws ParseException {

		if (os == null) {
			throw new ParseException("unknown OS", 0);
		}
		if ((cpu == null) && (os != OSEnum.JAVA)) {
			throw new ParseException("unknown CPU", 0);
		}
		switch (os) {
		case LINUX:
			switch (cpu) {
			case IX86:
				return setLDLinux_ix86(v);
			case PPC:
				return setLDLinux_ppc(v);
			case AMD64:
				return setLDLinux_amd64(v);
			case ARM64:
				return setLDLinux_arm64(v);
			case ARM32:
				return setLDLinux_arm32(v);
			case X86_64:
				return setLDLinux_x86_64(v);
			case IA64:
				return setLDLinux_ia64(v);
			default:
				break;
			}
			break;
		case WIN32:
			switch (cpu) {
			case IX86:
				return setLDWin32_ix86(v);
			case AMD64:
				return setLDWin32_amd64(v);
			case X86_64:
				return setLDWin32_x86_64(v);
			default:
				break;
			}
			break;
		case MACOSX:
			switch (cpu) {
			case IX86:
				return setLDMacos_ix86(v);
			case X86_64:
				return setLDMacos_x86_64(v);
			case PPC:
				return setLDMacos_ppc(v);
			default:
				break;
			}
			break;
		case SOLARIS:
			switch (cpu) {
			case SPARC:
				return setLDSolaris_sparc(v);
			case ALPHA:
				return setLDSolaris_alpha(v);
			default:
				break;
			}
			break;
		default:
			break;
		}
		throw new ParseException("invalid OS or CPU type", 0);
	}

	/**
	 * This sets the URI to retrieve application binary for linux ix86
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLinux_ix86(final URI v) {
		return setValue(Columns.LINUX_IX86URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux amd64
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLinux_amd64(final URI v) {
		return setValue(Columns.LINUX_AMD64URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux arm64
	 * @return true if value has changed, false otherwise
	 * @since 11.5.0
	 */
	public boolean setLinux_arm64(final URI v) {
		return setValue(Columns.LINUX_ARM64URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux arm32
	 * @return true if value has changed, false otherwise
	 * @since 11.5.0
	 */
	public boolean setLinux_arm32(final URI v) {
		return setValue(Columns.LINUX_ARM32URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux x86 64
	 *
	 * @return true if value has changed, false otherwise
	 * @since 7.2.0
	 */
	public boolean setLinux_x86_64(final URI v) {
		return setValue(Columns.LINUX_X86_64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for linux amd64
	 *
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setLinux_ia64(final URI v) {
		return setValue(Columns.LINUX_IA64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for linux ppc
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLinux_ppc(final URI v) {
		return setValue(Columns.LINUX_PPCURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for win32 ix32
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setWin32_ix86(final URI v) {
		return setValue(Columns.WIN32_IX86URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for win32 amd64
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setWin32_amd64(final URI v) {
		return setValue(Columns.WIN32_AMD64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for win32 x86_64
	 *
	 * @return true if value has changed, false otherwise
	 * @since 6.0.0
	 */
	public boolean setWin32_x86_64(final URI v) {
		return setValue(Columns.WIN32_X86_64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for mac os ix86
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMacos_ix86(final URI v) {
		return setValue(Columns.MACOS_IX86URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for mac os x86_64
	 *
	 * @return true if value has changed, false otherwise
	 * @since 5.8.0
	 */
	public boolean setMacos_x86_64(final URI v) {
		return setValue(Columns.MACOS_X86_64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for mac os ppc
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMacos_ppc(final URI v) {
		return setValue(Columns.MACOS_PPCURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for java
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setJava(final URI v) {
		return setValue(Columns.JAVAURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for solaris sparc
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setSolaris_sparc(final URI v) {
		return setValue(Columns.SOLARIS_SPARCURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for solaris alpha
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setSolaris_alpha(final URI v) {
		return setValue(Columns.SOLARIS_ALPHAURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for linux ix86
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDLinux_ix86(final URI v) {
		return setValue(Columns.LDLINUX_IX86URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux amd64
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDLinux_amd64(final URI v) {
		return setValue(Columns.LDLINUX_AMD64URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux arm64
	 * @return true if value has changed, false otherwise
	 * @since 11.5.0
	 */
	public boolean setLDLinux_arm64(final URI v) {
		return setValue(Columns.LDLINUX_ARM64URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux arm32
	 * @return true if value has changed, false otherwise
	 * @since 11.5.0
	 */
	public boolean setLDLinux_arm32(final URI v) {
		return setValue(Columns.LDLINUX_ARM32URI, v);
	}
	/**
	 * This sets the URI to retrieve application binary for linux x86 64
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDLinux_x86_64(final URI v) {
		return setValue(Columns.LDLINUX_X86_64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for linux intel itanium
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDLinux_ia64(final URI v) {
		return setValue(Columns.LDLINUX_IA64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for linux ppc
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDLinux_ppc(final URI v) {
		return setValue(Columns.LDLINUX_PPCURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for win32 ix32
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDWin32_ix86(final URI v) {
		return setValue(Columns.LDWIN32_IX86URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for win32 amd64
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDWin32_amd64(final URI v) {
		return setValue(Columns.LDWIN32_AMD64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for win32 x86_64
	 *
	 * @return true if value has changed, false otherwise
	 * @since 6.0.0
	 */
	public boolean setLDWin32_x86_64(final URI v) {
		return setValue(Columns.LDWIN32_X86_64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for mac os ix86
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDMacos_ix86(final URI v) {
		return setValue(Columns.LDMACOS_IX86URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for mac os x86_64
	 *
	 * @return true if value has changed, false otherwise
	 * @since 5.8.0
	 */
	public boolean setLDMacos_x86_64(final URI v) {
		return setValue(Columns.LDMACOS_X86_64URI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for mac os ppc
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDMacos_ppc(final URI v) {
		return setValue(Columns.LDMACOS_PPCURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for solaris sparc
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDSolaris_sparc(final URI v) {
		return setValue(Columns.LDSOLARIS_SPARCURI, v);
	}

	/**
	 * This sets the URI to retrieve application binary for solaris alpha
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setLDSolaris_alpha(final URI v) {
		return setValue(Columns.LDSOLARIS_ALPHAURI, v);
	}

	/**
	 * This sets this application web page
	 *
	 * @param v
	 *            is the URL
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setWebPage(final URL v) {
		return setValue(Columns.WEBPAGE, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setNbJobs(final int v) {
		return setValue(Columns.NBJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This sets amount of pending jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setPendingJobs(final int v) {
		return setValue(Columns.PENDINGJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This sets amount of running jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setRunningJobs(final int v) {
		return setValue(Columns.RUNNINGJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This sets amout of erroneus jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setErrorJobs(final int v) {
		return setValue(Columns.ERRORJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This increments the amount of executed jobs for this application
	 */
	public void incNbJobs() {
		setNbJobs(getNbJobs() + 1);
	}

	/**
	 * This increments the amount of pending jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incPendingJobs() {
		setPendingJobs(getPendingJobs() + 1);
	}

	/**
	 * This increments the amount of running jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incRunningJobs() {
		setRunningJobs(getRunningJobs() + 1);
	}

	/**
	 * This increments the amount of erroneus jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incErrorJobs() {
		setErrorJobs(getErrorJobs() + 1);
	}

	/**
	 * This decrements the amount of pending jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void decPendingJobs() {
		setPendingJobs(getPendingJobs() - 1);
	}

	/**
	 * This decrements the amount of running jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void decRunningJobs() {
		setRunningJobs(getRunningJobs() - 1);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setAvgExecTime(final int v) {
		return setValue(Columns.AVGEXECTIME, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This increments NbJobs and recalculates avg exec time
	 *
	 * @param v
	 *            is the last execution time
	 */
	public void incAvgExecTime(final int v) {
		int nbJobs = getNbJobs();
		int avg = getAvgExecTime();
		int value = v;
		if (value <= 0) {
			value = 0;
		}
		if (avg <= 0) {
			avg = 0;
		}
		if (nbJobs <= 0) {
			nbJobs = 0;
		}
		final int total = (avg * nbJobs) + value;
		setAvgExecTime(total / (nbJobs + 1));
		incNbJobs();
	}

	/**
	 * This set the minimal RAM needed for this application
	 *
	 * @param v
	 *            is the minimal RAM needed for this application in Kb
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMinMemory(final Integer v) {
		return setMinMemory(v.intValue());
	}

	/**
	 * This sets the minimal amount of RAM this work needs, in Kb. Provided
	 * value must be positive and can not exceed XWPropertyDefs.MAXRAMSPACE.
	 * If(v > XWPropertyDefs.MAXDISKSPACE) v is forced to
	 * XWPropertyDefs.MAXDISKSPACE. If(v < 0) v is forced to 0.
	 *
	 * @param v
	 *            is the minimal amount of RAM this work needs in Kb.
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setMinMemory(final int v) {
		try {
			return setValue(Columns.MINMEMORY, Integer.valueOf(v < 0 ? 0 : v));
		} catch (final Exception e) {
			return setValue(Columns.MINMEMORY, Integer.valueOf(0));
		}
	}

	/**
	 * This sets the mimimal CPU speed for this application
	 *
	 * @param v
	 *            is the mimimal CPU speed for this application, in MHz
	 * @return true if value has changed, false otherwise
	 */
	public boolean setMinCpuSpeed(final int v) {
		return setValue(Columns.MINCPUSPEED, Integer.valueOf(v < 0 ? 0 : v));
	}

    /**
     * This sets the minimal disk space for this application
     *
     * @param v
     *            is the minimal amount of disk space this work needs in Mb.
     * @return true if value has changed, false otherwise
     * @since 9.0.5
     */
    public boolean setMinFreeMassStorage(final long v) {
        try {
            return setValue(Columns.MINFREEMASSSTORAGE, Long.valueOf(v < 0L ? 0L : v));
        } catch (final Exception e) {
            return setValue(Columns.MINFREEMASSSTORAGE, 0L);
        }
    }
    /**
     * This sets the price
     *
     * @param v is the price
     * @return true if value has changed, false otherwise
     * @since 13.1.0
     */
    public boolean setPrice(final long v) {
        try {
            return setValue(Columns.PRICE, Long.valueOf(v < 0L ? 0L : v));
        } catch (final Exception e) {
            return setValue(Columns.PRICE, 0L);
        }
    }

	/**
	 * This sets this application name
	 * @return true if value has changed, false otherwise
	 */
	public boolean setName(final String v) {
		String value = v;
		if ((value != null) && (value.length() > APPNAMELENGTH)) {
			value = value.substring(0, APPNAMELENGTH - 1);
			getLogger().warn("Name too long; truncated to " + value);
		}
		return setValue(Columns.NAME, value == null ? null : value);
	}
	/**
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0 (FG)
	 */
	public boolean setEnvVars(final String v) {
		return setValue(Columns.ENVVARS, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0 (FG)
	 */
	public boolean setNeededPackages(final String v) {
		return setValue(Columns.NEEDEDPACKAGES, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setService(final boolean v) {
		return setValue(Columns.ISSERVICE, Boolean.valueOf(v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 8.0.0 (FG)
	 */
	public boolean setType(final AppTypeEnum v) {
		if (v == null) {
			return false;
		}
		return setValue(Columns.TYPE, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setDefaultStdin(final URI v) {
		return setValue(Columns.DEFAULTSTDINURI, v);
	}

	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setBaseDirin(final URI v) {
		return setValue(Columns.BASEDIRINURI, v);
	}
	/**
	 * @return true if value has changed, false otherwise
	 */
	public boolean setDefaultDirin(final URI v) {
		return setValue(Columns.DEFAULTDIRINURI, v);
	}

	/**
	 * This change name to "DEL_" + uid and set ISDELETED flag to TRUE Because
	 * name is a DB constraint and one may want to insert and delete and then
	 * reinsert an app with the same name. And delete does not remove row from
	 * DB : it simply set ISDELETED flag to true
	 */
	@Override
	public void delete() throws IOException {
		setName("DEL_ " + getUID().toString());
		update(false);
		super.delete();
	}

	/**
	 * This is for testing only Without any argument, this dumps a AppInterface
	 * object. If the first argument is an XML file containing a description of
	 * a AppInterface, this creates a AppInterface from XML description and
	 * dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.AppInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			LoggerLevel logLevel = LoggerLevel.DEBUG;
			try {
				logLevel = LoggerLevel.valueOf(System.getProperty(XWPropertyDefs.LOGGERLEVEL.toString()));
			} catch (final Exception e) {
			}
			final AppInterface itf = new AppInterface();
			itf.setUID(UID.getMyUid());
			if (argv.length > 0) {
				try {
					final XMLReader reader = new XMLReader(itf);
					reader.read(new FileInputStream(argv[0]));
				} catch (final XMLEndParseException e) {
				}
			}
			itf.setLoggerLevel(logLevel);
			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			final Logger logger = new Logger();
			logger.exception(
					"Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.AppInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
