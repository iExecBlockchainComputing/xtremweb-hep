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

package xtremweb.communications;

import xtremweb.common.XWTools;

/**
 * This enumerates HTTP POST parameter names. This is used by the HTTPClient.
 *
 * You can also open your favorite web browser and do
 * http://yourXWServer:4325/IDRPC?XMLDESC=
 * <getusers><user login="yourlogin" password="yourpassword" /></getuser> where
 * IDRPC is one of IdRpc and XMLDESC is a the string "XMLDESC"
 *
 * @see xtremweb.communications.IdRpc
 * @since XWHEP 1.0.0
 * @author Oleg Lodygensky
 */

public enum XWPostParams {

	/**
	 * This is the name of the parameter containing the external authentication
	 * operator
	 *
	 * @since 8.2.0
	 */
	AUTH_OPERATOR,
	/**
	 * This is the name of the parameter containing the private random string
	 *
	 * @since 8.2.0
	 */
	AUTH_NONCE,
	/**
	 * This is the name of the parameter containing the email address
	 *
	 * @since 8.2.0
	 */
	AUTH_EMAIL,
	/**
	 * This is the name of the parameter containing the identity
	 *
	 * @since 8.2.0
	 */
	AUTH_IDENTITY,
	/**
	 * This is the name of the parameter containing the code set by the external
	 * authentication operator
	 *
	 * @since 8.2.2
	 */
	AUTH_CODE {
		@Override
		public String toString() {
			return "code";
		}
	},
	/**
	 * @since 12.2.9
	 */
	NOREDIRECT,
	/**
	 * This is the name of the parameter containing the random string generated
	 * when invoking the external authentication operator
	 *
	 * @since 8.2.2
	 */
	AUTH_STATE {
		@Override
		public String toString() {
			return "state";
		}
	},
	/**
	 * This is used to pass parameter to the embedded web server. This is the
	 * name of the parameter which contains the file to upload
	 */
	XWUPLOAD,
	/**
	 * This is the name of the parameter which contains an XMLable object XML
	 * description
	 *
	 * @since 8.0.2
	 */
	XMLDESC,
	/**
	 * This is the parameter for the given command (e.g. AR value for chmod)
	 *
	 * @since 8.2.
	 */
	PARAMETER,
	/**
	 * This is the UID of the data to upload
	 *
	 * @since 8.0.2
	 */
	DATAUID,
	/**
	 * This is the name of a file to upload
	 *
	 * @since 8.0.2
	 */
	DATAFILE,
	/**
	 * This is the shasum of a data to upload
	 *
	 * @since 8.0.2
	 */
	DATASHASUM,
	/**
	 * This is the size of a data to upload
	 *
	 * @since 8.0.2
	 */
	DATASIZE,
	/**
	 * This is used to pass parameter through the alive signal. This param is
	 * sent from workers to server and contains a vector of results worker still
	 * have on its local FS
	 *
	 * @since 5.8.0
	 */
	JOBRESULTS,
	/**
	 * This is used to pass parameter through the alive signal. This param is
	 * sent from server to workers and contains server version.
	 *
	 * @since 5.8.0
	 */
	CURRENTVERSION,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains a vector of UID of completed job
	 * the worker can then erase the local copy of the result it still have in
	 * its local FS.
	 *
	 * @since 5.8.0
	 */
	FINISHEDTASKS,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains a vector of UID of job that the
	 * server has not been able to results for. The worker should then re-upload
	 * results.
	 *
	 * @since 5.8.0
	 */
	RESULTEXPECTEDS,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains server address the worker should
	 * connect to
	 *
	 * @since 5.8.0
	 */
	NEWSERVER,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains the URI of the keystore
	 * containing the server public key.
	 *
	 * @since 5.9.0
	 */
	KEYSTOREURI,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains a boolean telling the worker to
	 * collect traces or not
	 *
	 * @since 5.8.0
	 */
	TRACES,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains the period of trace uploads
	 *
	 * @since 5.8.0
	 */
	TRACESSENDRESULTDELAY,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains the period of trace collections
	 *
	 * @since 5.8.0
	 */
	TRACESRESULTDELAY,
	/**
	 * This is used to pass parameter through the alive signal This param is
	 * sent from server to workers and contains the period of the alive signal.
	 *
	 * @since 5.8.0
	 */
	ALIVEPERIOD,
	/**
	 * This is the mandating parameter
	 *
	 * @since 11.0.0
	 */
	XWMANDATINGLOGIN,
	/**
	 * This is the login parameter from the login formulaire
	 *
	 * @since 10.2.0
	 */
	XWLOGIN,
	/**
	 * This is the passowrd parameter from the login formulaire
	 *
	 * @since 10.2.0
	 */
	XWPASSWD;

	/**
	 * @see xtremweb.common.XWTools#MAXFILESIZE
	 */
	public static final long MAXUPLOADSIZE = XWTools.MAXFILESIZE;
}
