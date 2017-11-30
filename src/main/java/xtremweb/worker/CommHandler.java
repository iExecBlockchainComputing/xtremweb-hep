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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;

import org.apache.log4j.Logger;


import xtremweb.common.AppInterface;
import xtremweb.common.DataInterface;
import xtremweb.common.GroupInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.MileStone;
import xtremweb.common.SessionInterface;
import xtremweb.common.Table;
import xtremweb.common.TraceInterface;
import xtremweb.common.UID;
import xtremweb.common.UserGroupInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.common.WorkerParameters;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLVector;
import xtremweb.common.XMLable;
import xtremweb.communications.CommServer;
import xtremweb.communications.IdRpc;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommand;
import xtremweb.communications.XMLRPCCommandActivateHost;
import xtremweb.communications.XMLRPCCommandDownloadData;
import xtremweb.communications.XMLRPCCommandGet;
import xtremweb.communications.XMLRPCCommandGetGroupWorks;
import xtremweb.communications.XMLRPCCommandRemove;
import xtremweb.communications.XMLRPCCommandUploadData;
import xtremweb.communications.XMLRPCCommandWorkAliveByUID;

/**
 * CommHandler.java This Class launches several communication handler over
 * different "media" like RMI, UDP, SSL, custom, For now uses only RMI
 *
 * Created: Sun Jul 9 17:39:06 2000
 *
 * @author Gilles Fedak
 * @version %I% %G%
 */

public abstract class CommHandler extends Thread implements xtremweb.communications.CommHandler {

	private static final Logger logger = Logger.getLogger(CommHandler.class);

	/**
	 * This is the server that received incoming communication This is used to
	 * push this communication handler back to server pool
	 */
	private CommServer commServer;

	@Override
	public void setCommServer(final CommServer s) {
		commServer = s;
	}

	/**
	 * This aims to display some time stamps
	 */
	private final MileStone mileStone;

	/**
	 * This tells whether results recovery should be tested.
	 *
	 * @since v1r2-rc0 (RPC-V)
	 */
	private final boolean TESTRESULTRECOVERY = false;

	/**
	 * This is the test results recovery counter. It is only used if <CODE>
	 * TESTRESULTRECOVERY</CODE> is true.
	 *
	 * @since v1r2-rc0 (RPC-V)
	 */
	private final int resultTry = 0;
	/** 10 sec */
	private final int tracesResultDelay = 10000;
	/** 60 * tracesResultDelay = 10 mn */
	private final int tracesSendResultDelay = 60;

	/**
	 * This is the client host name; for debug purposes only
	 */
	private String remoteName;
	/**
	 * This is the client IP addr; for debug purposes only
	 */
	private String remoteIP;
	/**
	 * This is the client port; for debug purposes only
	 */
	private int remotePort;

	protected CommHandler(final String name) {
		super(name);
		mileStone = new MileStone(getClass());
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	public void handle(final String target, final HttpServletRequest request, final HttpServletResponse response,
			final int dispatch) throws IOException, ServletException {
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void setServer(final Server server) {
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public Server getServer() {
		return null;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isFailed() {
		return true;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isRunning() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarted() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarting() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isStopped() {
		return true;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStopping() {
		return false;
	}

	@Override
	public void removeLifeCycleListener(final Listener l) {

	}

	@Override
	public void addLifeCycleListener(final Listener l) {

	}

	/**
	 * This write an object to communication channel
	 */
	protected abstract void write(XMLable obj);

	/**
	 * This write an object to communication channel
	 */
	protected abstract void writeFile(File f) throws IOException;

	/**
	 * This write an object to communication channel
	 */
	protected abstract void readFile(File f) throws IOException;

	/**
	 * This retreives an application from server
	 */
	public Table get(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("get() is not implemented");
	}

	/**
	 * This retreives an object from server
	 *
	 * @since 5.8.0
	 */
	public Table get(final UserInterface client, final URI uri)
			throws RemoteException, InvalidKeyException, AccessControlException {
		return get(client, uri.getUID());
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 */
	@Override
	public synchronized void resetSockets() throws RemoteException {
		logger.error("resetSockets not implemented");
	}

	/**
	 * This is called by run() from inherited classes.
	 *
	 * @param command
	 *            is the command received from communication channel
	 */
	protected void run(final XMLRPCCommand command) {

		try {

			final IdRpc idRpc = command.getIdRpc();
			XMLable result = null;

			logger.debug("IDRPC " + idRpc.toString());

			switch (idRpc) {
			case DISCONNECT:
				disconnect(command.getUser());
				break;
			case SHUTDOWN:
				shutDown(command.getUser(), command.getHost());
				break;

			case REMOVE:
				remove(command.getUser(), ((XMLRPCCommandRemove) command).getURI().getUID());
				break;
			case GET:
				result = getApp(command.getUser(), ((XMLRPCCommandGet) command).getURI().getUID());
				break;
			case GETAPPS:
				result = getApps(command.getUser());
				break;
			case SENDAPP:
				sendApp(command.getUser(), (AppInterface) command.getParameter());
				break;
			case GETDATAS:
				result = getDatas(command.getUser());
				break;
			case SENDDATA:
				sendData(command.getUser(), (DataInterface) command.getParameter());
				break;
			case UPLOADDATA:
				uploadData(command.getUser(), ((XMLRPCCommandUploadData) command).getURI().getUID());
				break;
			case DOWNLOADDATA:
				downloadData(command.getUser(), ((XMLRPCCommandDownloadData) command).getURI().getUID());
				break;

			case GETGROUPS:
				result = getGroups(command.getUser());
				break;
			case GETGROUPWORKS:
				result = getGroupWorks(command.getUser(), ((XMLRPCCommandGetGroupWorks) command).getURI().getUID());
				break;
			case SENDGROUP:
				sendGroup(command.getUser(), (GroupInterface) command.getParameter());
				break;
			case ACTIVATEHOST:
				activateWorker(command.getUser(), ((XMLRPCCommandActivateHost) command).getURI().getUID(),
						((XMLRPCCommandActivateHost) command).getActivation());
				break;
			case GETHOSTS:
				result = getWorkers(command.getUser());
				break;
			case GETSESSIONS:
				result = getSessions(command.getUser());
				break;
			case SENDSESSION:
				sendSession(command.getUser(), (SessionInterface) command.getParameter());
				break;
			case GETTRACES:
				result = getTraces(command.getUser());
				break;
			case SENDTRACE:
				sendTrace(command.getUser(), (TraceInterface) command.getParameter());
				break;
			case SENDUSER:
				sendUser(command.getUser(), (UserInterface) command.getParameter());
				break;
			case GETUSERS:
				result = getUsers(command.getUser());
				break;
			case GETUSERGROUPS:
				result = getUserGroups(command.getUser());
				break;
			case SENDUSERGROUP:
				sendUserGroup(command.getUser(), (UserGroupInterface) command.getParameter());
				break;
			case GETWORKS:
				result = getWorks(command.getUser());
				break;
			case SENDWORK:
				sendWork(command.getUser(), command.getHost(), (WorkInterface) command.getParameter());
				break;
			case BROADCASTWORK:
				broadcast(command.getUser(), (WorkInterface) command.getParameter());
				break;
			case WORKREQUEST:
				result = workRequest(command.getUser(), command.getHost());
				break;
			case WORKALIVEBYUID:
				result = new XMLHashtable(workAlive(command.getUser(), command.getHost(),
						((XMLRPCCommandWorkAliveByUID) command).getURI().getUID()));
				break;
			case WORKALIVE:
				final Hashtable hash = ((XMLHashtable) command.getParameter()).getHashtable();
				result = new XMLHashtable(workAlive(command.getUser(), command.getHost(), hash));
				break;
			case PING:
				break;

			default:
				logger.error("BAD Id: " + idRpc);
				break;
			}

			if (result != null) {
				logger.debug("answer " + result.toXml());
				write(result);
			} else {
				logger.debug("no answer");
			}
		} catch (final EOFException e) {
		} catch (final SocketException e) {
			logger.debug(e.toString());
		} catch (final Exception e) {
			logger.error("Caught exception, Cannot get io socket", e);
		} finally {
			close();
		}

	}

	/**
	 * Set workers running parameters.
	 *
	 * @param client
	 *            is the client identifier
	 * @param nbWorkers
	 *            is the expected number of workers to activate.
	 * @param p
	 *            are the parameters to set
	 * @return the number of activated workers, -1 on error
	 */
	public int setWorkersParameters(final UserInterface client, final int nbWorkers, final WorkerParameters p)
			throws RemoteException {

		throw new RemoteException("worker.CommHandler#setWorkersParameters() is not implemented");
	}

	/**
	 * Get workers running parameters.
	 *
	 * @return workers parameters
	 */
	public WorkerParameters getWorkersParameters(final UserInterface client) {
		return null;
	}

	/**
	 * Call to the scheduler to select a work
	 *
	 * @return a Description of the Work the server has to complete
	 */
	public synchronized WorkInterface workRequest(final UserInterface _user, final HostInterface _host)
			throws RemoteException {
		notifyAll();
		throw new RemoteException("worker.CommHandler#workRequest() is not implemented");
	}

	/**
	 * This is sent by the worker for each job it has. This is not called if the
	 * worker has no job.
	 *
	 * This gets current job UID from worker. This sends a boolean back to
	 * worker wich tells whether to stop computing this jobs TRUE to continue
	 * and FALSE to stop
	 *
	 * @param uri
	 *            is the URI od the computing job on worker side
	 * @return a hashtable containing this job status so that worker continue or
	 *         stop computing it
	 * @since 5.8.0
	 */
	public Hashtable workAlive(final UserInterface _user, final HostInterface _host, final URI uri)
			throws RemoteException, InvalidKeyException {
		return workAlive(_user, _host, uri.getUID());
	}

	/**
	 * This is sent by the worker for each job it has. This is not called if the
	 * worker has no job.
	 *
	 * This gets current job UID from worker. This sends a boolean back to
	 * worker wich tells whether to stop computing this jobs TRUE to continue
	 * and FALSE to stop
	 *
	 * @param jobUID
	 *            is the UID od the computing job on worker side
	 * @return a hashtable containing this job status so that worker continue or
	 *         stop computing it
	 */
	public synchronized Hashtable workAlive(final UserInterface _user, final HostInterface _host, final UID jobUID)
			throws RemoteException {

		notifyAll();
		throw new RemoteException("worker.CommHandler#workAlive() is not implemented");
	}

	/**
	 * This is send by worker to tell it is still connected. Workers always try
	 * to sent this signal.
	 *
	 * This gets some informations from worker:
	 * <ul>
	 * <li>current stored job results UIDs, if any
	 * </ul>
	 *
	 * This sends some parameters back to worker:
	 * <ul>
	 * <li>traces, a boolean, to collect traces (or not);
	 * <li>tracesSendResultDelay, an integer, contains traces results transfert
	 * to coordinator periodicity;
	 * <li>tracesResultDelay, an integer, contains traces collection
	 * periodicity.
	 * <li>savedTasks, a Vector, contains tasks saved by coordinator that worker
	 * can delete from local disk (since v1r2-rc0/RPC-V) savedTasks also
	 * contains tasks that the coordinator is not able to save for any reason.
	 * <li>resultsExpected, a Vector, contains tasks the coordinator wants the
	 * worker sends again results for (since v1r2-rc0/RPC-V)
	 * </ul>
	 *
	 * @param _user
	 *            defines the calling client
	 * @param _host
	 *            defines the calling host
	 * @param rmiParams
	 *            is a Hashtable containing the worker local results, if any
	 * @return a hashtable containing new worker parameters.
	 */
	public synchronized Hashtable workAlive(final UserInterface _user, final HostInterface _host,
			final Hashtable rmiParams) throws RemoteException {

		notifyAll();
		throw new RemoteException("worker.CommHandler#workAlive(Hashtable) is not implemented");
	}

	/* Tracer */

	public synchronized void tactivityMonitor(final HostInterface host, final String start, final String end,
			final byte[] file) {
		logger.error("worker.CommHandler#tactivityMonitor() is not implemented");
		notifyAll();
	}

	/**
	 * This disconnects this client from server
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 */
	public void disconnect(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#disconnect() is not implemented");
	}

	/**
	 */
	public void shutDown(final UserInterface client, final HostInterface host) throws RemoteException {
		throw new RemoteException("worker.CommHandler#shutDown() is not implemented");
	}

	/**
	 * This creates or updates an application on server side This calls
	 * DBInterface::addApplication() to check whether client has the right to do
	 * so.
	 */
	public void sendApp(final UserInterface client, final AppInterface mapp) throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendApp() is not implemented");
	}

	/**
	 * This retreives an application from server
	 */
	public AppInterface getApp(final UserInterface client, final String name) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getApp() is not implemented");
	}

	/**
	 * This retreives an application from server
	 */
	public AppInterface getApp(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getApp() is not implemented");
	}

	/**
	 * This retreives all applications from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getApps(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#remove() is not implemented");
	}

	/**
	 * This removes an application from server
	 *
	 * @param client
	 *            defines the client
	 * @param uid
	 *            is the UID of the application to remove
	 */
	public void remove(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#remove() is not implemented");
	}

	/**
	 * This removes an application from server
	 *
	 * @param client
	 *            defines the client
	 * @param uri
	 *            is the URI of the object to remove
	 * @since 5.8.0
	 */
	public void remove(final UserInterface client, final URI uri)
			throws IOException, InvalidKeyException, AccessControlException {
		remove(client, uri.getUID());
	}

	/**
	 * This creates or updates data on server side
	 *
	 * @param client
	 *            is the caller attributes
	 * @param data
	 *            is the data to create
	 */
	public void sendData(final UserInterface client, final DataInterface data) throws RemoteException {

		throw new RemoteException("worker.CommHandler#sendData() is not implemented");
	}

	/**
	 * This retreives a data from server
	 */
	public DataInterface getData(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getData() is not implemented");
	}

	/**
	 * This retreives a data from server
	 */
	public XMLVector getDatas(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getDatas() is not implemented");
	}

	/**
	 * This uploads a data to server<br />
	 * Data must be defined on server side (i.e. sendData() must be called
	 * first)
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uid
	 *            is the UID of the data to upload
	 * @see #sendData(UserInterface, DataInterface)
	 */
	public long uploadData(final UserInterface client, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {

		throw new RemoteException("worker.CommHandler#uploadData() is not implemented");
	}

	/**
	 * This uploads a data to server<br />
	 * Data must be defined on server side (i.e. sendData() must be called
	 * first)
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uri
	 *            is the URI of the data to upload
	 * @see #sendData(UserInterface, DataInterface)
	 * @return the size of the uploaded data
	 * @since 5.8.0
	 */
	public long uploadData(final UserInterface client, final URI uri)
			throws IOException, InvalidKeyException, AccessControlException {
		return uploadData(client, uri.getUID());
	}

	/**
	 * This downloads a data from server
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uri
	 *            is the URI of the data to download
	 * @since 5.8.0
	 */
	public long downloadData(final UserInterface client, final URI uri)
			throws IOException, InvalidKeyException, AccessControlException {
		return downloadData(client, uri.getUID());
	}

	/**
	 * This downloads a data from server
	 *
	 * @param client
	 *            is the caller attributes
	 * @param data
	 *            is the UID of the data to remove
	 */
	public long downloadData(final UserInterface client, final UID data)
			throws IOException, InvalidKeyException, AccessControlException {

		throw new RemoteException("worker.CommHandler#downloadData() is not implemented");
	}

	/**
	 * This creates or updates a group on server side
	 *
	 * @param client
	 *            contains user id/password
	 * @param group
	 *            is the group to send
	 */
	public void sendGroup(final UserInterface client, final GroupInterface group) throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendGroup() is not implemented");
	}

	/**
	 * This retreives an group from server
	 */
	public GroupInterface getGroup(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getGroup() is not implemented");
	}

	/**
	 * This retreives this client groups
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 */
	public XMLVector getGroups(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getGroups() is not implemented");
	}

	/**
	 * This retreives all works for the given group
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 * @param uri
	 *            is the URI of the group to retreive works for
	 * @since 5.8.0
	 */
	public XMLVector getGroupWorks(final UserInterface client, final URI uri)
			throws IOException, InvalidKeyException, AccessControlException {
		return getGroupWorks(client, uri.getUID());
	}

	/**
	 * This retreives all works for the given group
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 * @param group
	 *            is the group UID to retreive works for
	 */
	public XMLVector getGroupWorks(final UserInterface client, final UID group) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getGroupWorks() is not implemented");
	}

	/**
	 * This creates or updates an session on server side
	 */
	public void sendSession(final UserInterface client, final SessionInterface session) throws RemoteException {

		throw new RemoteException("worker.CommHandler#getSession() is not implemented");
	}

	/**
	 * This retreives an session from server
	 */
	public SessionInterface getSession(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("TCPHandler::getSession() TCP not implemented yet");
	}

	/**
	 * This retreives all sessions from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getSessions(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getSessions() is not implemented");
	}

	/**
	 * This retreives all works for the given session
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 * @param session
	 *            is the session UID to retreive works for
	 */
	public XMLVector getSessionWorks(final UserInterface client, final UID session) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getSessionWorks() is not implemented");
	}

	/**
	 * This creates or updates an work on server side This calls
	 * DBInterface#insertWork()
	 */
	public void sendWork(final UserInterface client, final HostInterface worker, final WorkInterface work)
			throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendWork() is not implemented");
	}

	/**
	 * @deprecated since 1.9.0 this is deprecated ; sendWork() should be used
	 *             instead stdin and dirin must be sent using sendData
	 * @see #sendWork(UserInterface, HostInterface, WorkInterface)
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Deprecated
	public WorkInterface submit(final UserInterface client, final WorkInterface job) throws RemoteException {
		throw new RemoteException("worker.CommHandler#submit() is not implemented");
	}

	/**
	 * This retreives an work from server
	 */
	public WorkInterface getWork(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getWork() is not implemented");
	}

	/**
	 * This retreives an work from server, including all associated files
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uid
	 *            is the work UID
	 * @return a WorkInterface object
	 */
	public WorkInterface loadWork(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#loadWork() is not implemented");
	}

	/**
	 * This retreives all works from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getWorks(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getWorks() is not implemented");
	}

	/**
	 * This broadcasts a new work to all workers
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 * @param work
	 *            defines the work to broadcast
	 * @return a Vector of String containing submitted work UIDs
	 */
	public XMLVector broadcast(final UserInterface client, final WorkInterface work) throws RemoteException {
		throw new RemoteException("worker.CommHandler#broadcast() is not implemented");
	}

	/**
	 * This always throws an exception
	 *
	 * @deprecated since 1.9.0
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Deprecated
	public DataInterface getResult(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getResult() is deprecated");
	}

	/**
	 * This always throws an exception
	 *
	 * @deprecated since 1.9.0
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Deprecated
	public void sendResult(final UserInterface client, final DataInterface result) throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendResult() is deprecated");
	}

	/**
	 * This creates or updates an worker on server side
	 */
	public void sendWorker(final UserInterface client, final HostInterface worker) throws RemoteException {
		throw new RemoteException("TCPHandler::sendWorker TCP not implemented yet");
	}

	/**
	 * This retreives a worker from server
	 */
	public HostInterface getWorker(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getWorker() is not implemented");
	}

	/**
	 * This retreives all workers from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getWorkers(final UserInterface client) {
		return null;
	}

	/**
	 * Set worker active flag.
	 *
	 * @param client
	 *            contains client parameters
	 * @param uri
	 *            is the worker uri
	 * @param flag
	 *            is the active flag
	 * @since 5.8.0
	 */
	public void activateWorker(final UserInterface client, final URI uri, final boolean flag) throws RemoteException {
		activateWorker(client, uri.getUID(), flag);
	}

	/**
	 * Set worker active flag.
	 *
	 * @param client
	 *            contains client parameters
	 * @param uid
	 *            is the worker uid
	 * @param flag
	 *            is the active flag
	 */
	public void activateWorker(final UserInterface client, final UID uid, final boolean flag) throws RemoteException {
		throw new RemoteException("worker.CommHandler#activateWorker() is not implemented");
	}

	/**
	 * This set the expected number of workers
	 *
	 * @param client
	 *            contains client parameters
	 * @return the number of activated workers, -1 on error
	 */
	public int setWorkersNb(final UserInterface client, final int nb) throws RemoteException {
		throw new RemoteException("worker.CommHandler#setWorkersNb() is not implemented");
	}

	/*
	 * Tracer
	 */
	public void tactivityMonitor(final HostInterface host, final long start, final long end, final byte[] file)
			throws RemoteException {
		tactivityMonitor(host, new Long(start).toString(), new Long(end).toString(), file);
	}

	/**
	 * This adds a new user.
	 *
	 * @param client
	 *            contains client parameters
	 * @param user
	 *            describes new user informations
	 */
	public void sendUser(final UserInterface client, final UserInterface user) throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendUser() is not implemented");
	}

	/**
	 * Get traces files path.
	 *
	 * @return a string containing traces files path.
	 */
	public String getTracesPath() throws RemoteException {
		throw new RemoteException("worker.CommHandler#getTracesPath() is not implemented");
	}

	/**
	 * This creates or updates an usergroup on server side
	 */
	public void sendUserGroup(final UserInterface client, final UserGroupInterface group) throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendUserGroup() is not implemented");
	}

	/**
	 * This retreives an user group from server, if allowed
	 *
	 * @param client
	 *            defines the client calling this method
	 * @param uid
	 *            is the group UID to retreive
	 * @return an UserGroupInterface object on success, null otherwise(group
	 *         does not exist, user rights...)
	 */
	public UserGroupInterface getUserGroup(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getUserGroup() is not implemented");
	}

	/**
	 * This retreives all usergroups from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getUserGroups(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getUserGroups() is not implemented");
	}

	/**
	 * This retreives an user from server
	 *
	 * @param client
	 *            defines the client calling this method
	 * @param uid
	 *            is the UID of the user to retreive
	 */
	public UserInterface getUser(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getUser() is not implemented");
	}

	/**
	 * This retreives an user from server
	 */
	public UserInterface getUser(final UserInterface client, final String login) throws RemoteException {
		throw new RemoteException("TCPHandler::getUser TCP not implemented yet");
	}

	/**
	 * This retreives all users from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getUsers(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getUsers() is not implemented");
	}

	/**
	 * This creates a Mobile Work filled with worker software. It is used to
	 * update worker as
	 *
	 * @param user
	 *            defines the client
	 * @param host
	 *            defines the host
	 * @return a filled mobileWork
	 */
	public DataInterface getWorkerBin(final UserInterface user, final HostInterface host) throws RemoteException {
		throw new RemoteException("getWorkerBin is not implemented");
	}

	/**
	 * This creates or updates an trace on server side
	 */
	public void sendTrace(final UserInterface client, final TraceInterface trace) throws RemoteException {
		throw new RemoteException("worker.CommHandler#sendTrace() is not implemented");
	}

	/**
	 * This retreives an trace from server
	 */
	public TraceInterface getTrace(final UserInterface client, final UID uid) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getTrace() is not implemented");
	}

	/**
	 * This retreives all traces from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLVector getTraces(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getTraces() is not implemented");
	}

	/**
	 * Get all known traces.
	 *
	 * @return an vector of TraceInterface
	 */
	public XMLVector getTraces(final UserInterface client, final Date since, final Date before) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getTraces() is not implemented");
	}

	/**
	 * Get the path of traces files.
	 *
	 * @return a string describing path to traces files.
	 */
	public String getTracesPath(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getTracesPath() is not implemented");
	}

	/**
	 * Get trusted addresses
	 *
	 * @return a string containing trused ip addresses separated by a white
	 *         space.
	 */
	public String getTrustedAddresses(final UserInterface client) throws RemoteException {
		throw new RemoteException("worker.CommHandler#getTrustedAddresses() is not implemented");
	}

	/**
	 * Add a trusted address
	 *
	 * @param ip
	 *            new trusted IP
	 */
	public void addTrustedAddress(final UserInterface client, final String ip) throws RemoteException {
		throw new RemoteException("worker.CommHandler#addTrustedAddresses() is not implemented");
	}

	/**
	 * Remove a trusted address
	 *
	 * @param ip
	 *            trusted IP to remove
	 */
	public void removeTrustedAddress(final UserInterface client, final String ip) throws RemoteException {
		throw new RemoteException("worker.CommHandler#removeTrustedAddresses() is not implemented");
	}

	/**
	 * Set workers trace flag.
	 *
	 * @param hosts
	 *            is a hashtable which contains host name as key and their
	 *            dedicated trace flag as value.
	 */
	public void traceWorkers(final UserInterface client, final Hashtable hosts) throws RemoteException {
		throw new RemoteException("worker.CommHandler#traceWorkers() is not implemented");
	}

}
