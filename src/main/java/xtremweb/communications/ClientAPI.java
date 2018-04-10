/*
 * Copyrights     : CNRS
 * Author         : Oleg Lodygensky
 * Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
 * Web            : http://www.xtremweb-hep.org
 *
 *      This file is part of XtremWeb-HEP.
 *
 *    XtremWeb-HEP is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    XtremWeb-HEP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General License for more details.
 *
 *    You should have received a copy of the GNU General License
 *    along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package xtremweb.communications;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import org.xml.sax.SAXException;

import xtremweb.common.HostInterface;
import xtremweb.common.StatusEnum;
import xtremweb.common.Table;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.common.WorkerParameters;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLVector;
import xtremweb.security.XWAccessRights;

/**
 * This interface defines client communications facilities. This replaces
 * This interface defines client communications facilities. This replaces
 * RMIOutputInterface.java
 *
 * @author Oleg Lodygensky
 * @since RPCXW
 */
interface ClientAPI {
	/**
	 * This disconnects this client from server : all sessions jobs are removed
	 */
	void disconnect() throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This disconnects this client from server : all sessions jobs are removed
	 */
	void disconnect(XMLRPCCommandDisconnect command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This sends an XMLRPC command to be executed on server side and closes
	 * communication channel
	 */
	void sendCommand(XMLRPCCommand cmd)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This set access rights to an object
	 *
	 * @since 5.8.0
	 */
	void chmod(UID uid, XWAccessRights r)
			throws SAXException, InvalidKeyException, AccessControlException, IOException, URISyntaxException;

	/**
	 * This retrieves an object from server
	 */
	Table get(UID uid) throws InvalidKeyException, AccessControlException, IOException, ClassNotFoundException,
			SAXException, URISyntaxException;

	/**
	 * This retrieves an object from server
	 */
	Table get(XMLRPCCommandGet command) throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This sends (creates or updates) an object definition
	 *
	 * @since 5.0.0
	 */
	void send(XMLRPCCommandSend command)
			throws InvalidKeyException, AccessControlException, IOException, ClassNotFoundException, SAXException;

	/**
	 * This sends an object
	 *
	 * @since 8.0.0
	 */
	void send(Table app) throws InvalidKeyException, AccessControlException, IOException, ClassNotFoundException,
			SAXException, URISyntaxException;

	/**
	 * This retrieves all applications from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getApps()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all applications from server, up to MAXDBREQUESTLIMIT
	 *
	 * @param command
	 *            is the XMLRPC command to use
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getApps(XMLRPCCommandGetApps command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This removes an object definition from server
	 *
	 * @param uri
	 *            is the URI of the object to remove
	 */
	void remove(URI uri) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This retrieves all datas from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getDatas()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all datas from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getDatas(XMLRPCCommandGetDatas command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This uploads a data to server
	 *
	 * @param command
	 *            is the upload command to send to server
	 * @param content
	 *            is the file containing data content to upload
	 */
	void uploadData(XMLRPCCommandUploadData command, File content)
			throws InvalidKeyException, AccessControlException, IOException, URISyntaxException;

	/**
	 * This downloads a data from server
	 *
	 * @param uid
	 *            is the UID the data to download
	 * @param content
	 *            represents a File to store downloaded data
	 */
	void downloadData(UID uid, File content)
			throws InvalidKeyException, AccessControlException, IOException, URISyntaxException;

	/**
	 * This downloads a data from server
	 *
	 * @param command
	 *            is the download command to send to server
	 * @param content
	 *            represents a File to store downloaded data
	 */
	void downloadData(XMLRPCCommandDownloadData command, File content)
			throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This retrieves all envelopes from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
	 * @since 13.0.0
	 * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getEnvelopes()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all works from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
	 * @since 13.0.0
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getEnvelopes(XMLRPCCommandGetEnvelopes command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all groups from server for the client, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getGroups()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all groups from server for the client, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getGroups(XMLRPCCommandGetGroups command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all works for the given group, up to MAXDBREQUESTLIMIT
	 *
	 * @param groupuid
	 *            is the group UID to retreive works for
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getGroupWorks(UID groupuid)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all works for the given group, up to MAXDBREQUESTLIMIT
	 *
	 * @param uri
	 *            is the URI to connect to ; its path must contains the UID of
	 *            the group to retreive works for
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getGroupWorks(URI uri) throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all works for the given group, up to MAXDBREQUESTLIMIT
	 *
	 * @param command
	 *            is the command to send to server
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getGroupWorks(XMLRPCCommandGetGroupWorks command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all workers from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getHosts()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all workers from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getHosts(XMLRPCCommandGetHosts command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * Set workers active flag.
	 *
	 * @param uid
	 *            is the worker uid
	 * @param flag
	 *            is the active flag
	 */
	void activateHost(UID uid, boolean flag)
			throws InvalidKeyException, AccessControlException, IOException, URISyntaxException;

	/**
	 * Set workers active flag.
	 *
	 * @param command
	 *            is the command to send to server
	 */
	void activateHost(XMLRPCCommandActivateHost command)
			throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * Set workers running parameters.
	 *
	 * @param nbWorkers
	 *            is the expected number of workers to activate.
	 * @param p
	 *            is the worker parameters
	 * @return the number of activated workers, -1 on error
	 */
	int setWorkersParameters(int nbWorkers, WorkerParameters p)
			throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * Get workers running parameters.
	 *
	 * @return worker parameters
	 */
	WorkerParameters getWorkersParameters() throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This set the expected number of workers
	 *
	 * @return the number of activated workers, -1 on error
	 */
	int setWorkersNb(int nb) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This retrieves all sessions from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getSessions()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all sessions from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getSessions(XMLRPCCommandGetSessions command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all works for the given session, up to MAXDBREQUESTLIMIT
	 *
	 * @param session
	 *            is the session UID to retreive works for
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getSessionWorks(UID session)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all works for the given session, up to MAXDBREQUESTLIMIT
	 *
	 * @param command
	 *            is the command to send to server
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getSessionWorks(XMLRPCCommandGetSessionWorks command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all tasks from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getTasks()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all tasks from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getTasks(XMLRPCCommandGetTasks command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all traces from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getTraces()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all traces from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getTraces(XMLRPCCommandGetTraces command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves traces from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getTraces(Date since, Date before)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all usergroups from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getUserGroups()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all usergroups from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getUserGroups(XMLRPCCommandGetUserGroups command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves an user from server
	 */
	UserInterface getUser(String login)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves an user from server
	 */
	UserInterface getUser(XMLRPCCommandGetUserByLogin command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all users from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getUsers()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all users from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getUsers(XMLRPCCommandGetUsers command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves the next work to compute
	 */
	WorkInterface workRequest(HostInterface h)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves the next work to compute
	 */
	WorkInterface workRequest(XMLRPCCommandWorkRequest command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves all works from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getWorks()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves all works from server, up to MAXDBREQUESTLIMIT
	 *
	 * @return a Vector of UIDs
     * @see xtremweb.common.XWTools#MAXDBREQUESTLIMIT
	 */
	XMLVector getWorks(XMLRPCCommandGetWorks command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This retrieves a work from its external ID (external scheduler, blockchain transaction)
	 * @return a WorkInterface
	 * @since 11.1.0
	 */
	public Table getWorkByExternalId(final String extId, final boolean bypass)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves a work from its external ID (external scheduler, blockchain transaction)
	 * @return a WorkInterface
	 * @since 11.1.0
	 */
	public Table getWorkByExternalId(final XMLRPCCommandGetWorkByExternalId command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;
	/**
	 * This retrieves a work from its external ID (external scheduler, blockchain transaction)
	 * @param command
	 *            is the command to send to server to retreive user
	 * @param bypass
	 *            if true user is downloaded from server even if already in
	 *            cache if false, user is only downloaded if not already in
	 *            cache
	 */
	public Table getWorkByExternalId(final XMLRPCCommandGetWorkByExternalId command, final boolean bypass)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;
	/**
	 * This retrieves an application from its name
	 * @return a AppkInterface
	 * @since 12.2.9
	 */
	public Table getAppByName(final String name, final boolean bypass)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This retrieves an application from its name
	 * @return a AppkInterface
	 * @since 12.2.9
	 */
	public Table getAppByName(final XMLRPCCommandGetAppByName command)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;
	/**
	 * This retrieves an application from its name
	 * @return a AppkInterface
	 * @since 12.2.9
	 */
	public Table getAppByName(final XMLRPCCommandGetAppByName command, final boolean bypass)
			throws InvalidKeyException, AccessControlException, IOException, SAXException;

	/**
	 * This removes a set of jobs from server
	 *
	 * @param uids
	 *            is a Vector of UID of the works to delete
	 */
	void removeWorks(Collection<URI> uids) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This broadcasts a new work to all workers
	 *
	 * @param work
	 *            defines the work to broadcast
	 */
	void broadcast(WorkInterface work)
			throws InvalidKeyException, AccessControlException, IOException, URISyntaxException;

	/**
	 * This broadcasts a new work to all workers
	 *
	 * @param uri
	 *            is the URI to connect to ; its path must contains the UID of
	 *            the work to broadcast
	 * @since 4.2.0
	 */
	void broadcast(URI uri) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This broadcasts a new work to all workers
	 *
	 * @param uid
	 *            is the uid of the work to braodcast
	 * @since 4.2.0
	 */
	void broadcast(UID uid) throws InvalidKeyException, AccessControlException, IOException, URISyntaxException;

	/**
	 * This broadcasts a new work to all workers
	 *
	 * @param command
	 *            is the broadcast command to send
	 */
	void broadcast(XMLRPCCommandBroadcastWork command) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This method waits for works to complete, with a time out
	 * If time out is reached, this returns
	 *
	 * @param works
	 *            a Vector of UID
	 * @param t
	 *            is the time out to wait, in seconds
	 * @return a Vector of the UID of the completed works within timeout, if any
	 * @exception InvalidKeyException
	 *                , AccessControlException on connection error
	 */
	void getCompletedWorks(Collection<UID> works, long t) throws InvalidKeyException, AccessControlException,
			IOException, ClassNotFoundException, SAXException, URISyntaxException;

	/**
	 * This method waits(for ever) until all works are completed
	 *
	 * @param works
	 *            a Vector of UID
	 * @exception InvalidKeyException
	 *                , AccessControlException on connection error
	 */
	void waitForCompletedWorks(Collection<UID> works) throws InvalidKeyException, AccessControlException, IOException,
			ClassNotFoundException, SAXException, URISyntaxException;

	/**
	 * This method waits until all works are completed, with a time out
	 *
	 * @param works
	 *            a Vector of UID
	 * @param timeOut
	 *            is the time out to wait, in seconds
	 * @exception InvalidKeyException
	 *                , AccessControlException on connection error
	 * @exception InterruptedException
	 *                if time out reached
	 * @throws IOException
	 */
	void waitForCompletedWorks(Collection<UID> works, long timeOut) throws InvalidKeyException, AccessControlException,
			InterruptedException, ClassNotFoundException, SAXException, URISyntaxException, IOException;

	/**
	 * This method waits until the work is completed
	 *
	 * @param uid
	 *            is the UID of the expected work
	 * @return the found work
	 * @exception InvalidKeyException
	 *                , AccessControlException on connection error
	 * @throws InterruptedException
	 */
	WorkInterface waitForCompletedWork(UID uid) throws InvalidKeyException, AccessControlException, IOException,
			ClassNotFoundException, SAXException, URISyntaxException, InterruptedException;

	/**
	 * This method waits until the work is completed, with a time out
	 *
	 * @param uid
	 *            is the UID of the expected work
	 * @param timeOut
	 *            is the time out
	 * @return the found work
	 * @exception InvalidKeyException
	 *                , AccessControlException on connection error
	 * @exception InterruptedException
	 *                if time out reached
	 * @throws IOException
	 */
	WorkInterface waitForCompletedWork(UID uid, long timeOut) throws InvalidKeyException, AccessControlException,
			InterruptedException, ClassNotFoundException, SAXException, URISyntaxException, IOException;

	/**
	 * This waits until a work has the given status
	 *
	 * @param status
	 *            is the status to wait for
	 * @param uid
	 *            is the uid of the expected work
	 * @param timeOut
	 *            is the time out
	 * @exception InvalidKeyException
	 *                , AccessControlException on connection error
	 * @exception InterruptedException
	 *                on time out error
	 * @throws IOException
	 */
	WorkInterface waitForWork(StatusEnum status, UID uid, long timeOut)
			throws InvalidKeyException, AccessControlException, InterruptedException, ClassNotFoundException,
			SAXException, URISyntaxException, IOException;

	/**
	 * This retrieves the knwon SmartSockets hub address
	 *
	 * @since 8.0.0
	 */
	XMLHashtable getHubAddress()
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This checks the provided work accordingly to the server status
	 */
	XMLHashtable workAlive(UID workUID)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/**
	 * This synchronizes with the server
	 */
	XMLHashtable workAlive(Hashtable params)
			throws InvalidKeyException, AccessControlException, IOException, SAXException, URISyntaxException;

	/*
	 * Tracer
	 */
	void tactivityMonitor(long start, long end, byte[] file)
			throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * Get trusted addresses
	 *
	 * @return a String containing trused ip addresses separated by a white
	 *         space.
	 */
	String getTrustedAddresses() throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * Add a trusted address
	 *
	 * @param ip
	 *            new trusted IP
	 */
	void addTrustedAddress(String ip) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * Remove a trusted address
	 *
	 * @param ip
	 *            trusted IP to remove
	 */
	void removeTrustedAddress(String ip) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * Set workers trace flag.
	 *
	 * @param hosts
	 *            is a hashtable which contains host name as key and their
	 *            dedicated trace flag as value.
	 */
	void traceWorkers(Hashtable hosts) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This reads a file from socket
	 *
	 * @param f
	 *            is the file to store received bytes
	 */
	void readFile(File f) throws InvalidKeyException, AccessControlException, IOException;

	/**
	 * This writes a file to socket
	 *
	 * @param f
	 *            is the file to write
	 */
	void writeFile(File f) throws InvalidKeyException, AccessControlException, IOException;

}
