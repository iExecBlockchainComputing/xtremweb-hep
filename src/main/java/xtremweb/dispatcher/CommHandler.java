/**
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

package xtremweb.dispatcher;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.apache.log4j.Logger;

import xtremweb.common.AppInterface;
import xtremweb.common.CommonVersion;
import xtremweb.common.DataInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.MileStone;
import xtremweb.common.StatusEnum;
import xtremweb.common.TaskInterface;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;
import xtremweb.common.Version;
import xtremweb.common.WorkInterface;
import xtremweb.common.WorkerParameters;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLValue;
import xtremweb.common.XMLVector;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWReturnCode;
import xtremweb.communications.AccessLogger;
import xtremweb.communications.CommServer;
import xtremweb.communications.Connection;
import xtremweb.communications.IdRpc;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommand;
import xtremweb.communications.XMLRPCCommandGet;
import xtremweb.communications.XMLRPCCommandGetWorkByExternalId;
import xtremweb.communications.XMLRPCCommandWorkAliveByUID;
import xtremweb.communications.XMLRPCResult;
import xtremweb.communications.XWPostParams;

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
	private final String URINOTSET = "uri not set";


	/**
	 * This aims to display some time stamps
	 */
	private final MileStone mileStone;

	protected void mileStone(final String msg) {
		mileStone.println(msgWithRemoteAddresse(msg));
	}

	/** 10 sec */
	private int tracesResultDelay = 10000;
	/**
	 * equals 60 60 * tracesResultDelay = 10 mn
	 */
	private int tracesSendResultDelay = 60;

	private static final XMLVector NOANSWER = new XMLVector(new Vector());
	private static final Version CURRENTVERSION = CommonVersion.getCurrent();
	private static final String CURRENTVERSIONSTRING = CURRENTVERSION.toString();

	/**
	 * This is the server that received incoming communication This is used to
	 * push this communication handler back to server pool
	 */
	private CommServer commServer;

	/**
	 * This retrieves the communication server
	 *
	 * @return the commServer
	 */
	public CommServer getCommServer() {
		return commServer;
	}

	/**
	 * This sets the communication server
	 *
	 * @param s
	 *            the commServer to set
	 */
	@Override
	public void setCommServer(final CommServer s) {
		commServer = s;
	}

	/**
	 * This is the remote host name
	 */
	private String remoteName;

	/**
	 * This resets remote host name
	 *
	 * @since 8.2.0
	 */
	public void resetRemoteName() {
		remoteName = null;
	}

	/**
	 * This retrieves remote host name
	 *
	 * @return the remoteName
	 */
	public String getRemoteName() {
		return remoteName;
	}

	/**
	 * This sets the remote host name
	 *
	 * @param remoteName
	 *            the remoteName to set
	 */
	public void setRemoteName(final String remoteName) {
		this.remoteName = remoteName;
	}

	/**
	 * This is the client IP address
	 */
	private String remoteIP;

	/**
	 * This resets the remote host IP address
	 *
	 * @since 8.2.0
	 */
	public void resetRemoteIP() {
		remoteIP = null;
	}

	/**
	 * This retrieves the remote host IP address
	 *
	 * @return the remoteIP
	 */
	public String getRemoteIP() {
		return remoteIP;
	}

	/**
	 * This sets the remote host IP address
	 *
	 * @param remoteIP
	 *            the remoteIP to set
	 */
	public void setRemoteIP(final String remoteIP) {
		this.remoteIP = remoteIP;
	}

	/**
	 * This is the remote host port
	 */
	private int remotePort;

	/**
	 * This retrives the remote host port
	 *
	 * @return the remotePort
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * This sets the remote host port
	 *
	 * @param remotePort
	 *            the remotePort to set
	 */
	public void setRemotePort(final int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * This resets the remote host port
	 *
	 * @since 8.2.0
	 */
	public void resetRemotePort() {
		remotePort = -1;
	}

	/**
	 * This has been constructed from config file
	 */
	private XWConfigurator config;

	/**
	 * @return the config
	 */
	public XWConfigurator getConfig() {
		return config;
	}

	/**
	 * This is the running semaphore
	 */
	private final boolean isRunning;

	/**
	 * constructor
	 */
	protected CommHandler(final String name) {
		super(name);
		logger.debug(name);
		mileStone = new MileStone(xtremweb.dispatcher.CommHandler.class);
		config = Dispatcher.getConfig();
		isRunning = false;
	}

	protected CommHandler(final String name, final XWConfigurator c) {
		this(name);
		config = c;
	}

	public String remoteAddresse() {
		return "{" + remoteName + "/" + remoteIP + ":" + remotePort + "}";
	}

	protected String msgWithRemoteAddresse(final String msg) {
		return remoteAddresse() + " : " + msg;
	}

	/**
	 * This prefixes the finest message with the remote host name and port
	 */
	public void finest(final String msg) {
		logger.trace(msgWithRemoteAddresse(msg));
	}

	/**
	 * This prefixes the debug message with the remote host name and port
	 */
	public void debug(final String msg) {
		logger.debug(msgWithRemoteAddresse(msg));
	}

	/**
	 * This prefixes the info message with the remote host name and port
	 */
	public void info(final String msg) {
		logger.info(msgWithRemoteAddresse(msg));
	}

	/**
	 * This prefixes the warning message with the remote host name and port
	 */
	public void warn(final String msg) {
		logger.warn(msgWithRemoteAddresse(msg));
	}

	/**
	 * This prefixes the error message with the remote host name and port
	 */
	public void error(final String msg) {
		logger.error(msgWithRemoteAddresse(msg));
	}

	/**
	 * This prefixes the error message with the remote host name and port
	 */
	public void error(final Exception e) {
		logger.error(msgWithRemoteAddresse(""), e);
	}

	/**
	 * This prefixes the error message with the remote host name and port
	 */
	public void error(final String msg, final Exception e) {
		logger.error(msgWithRemoteAddresse(msg), e);
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest _request,
			final HttpServletResponse _response) throws IOException, ServletException {
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void setServer(final Server server) {
		warn("" + this + " CommHandler#serServer(server = " + server + ") does nothing ");
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
		return isRunning;
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

	@Override
	public void removeLifeCycleListener(final Listener l) {

	}

	@Override
	public void addLifeCycleListener(final Listener l) {

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

	/**
	 * This write an object to output channel
	 */
	protected abstract void write(XMLable obj) throws IOException;

	/**
	 * This writes a file to output channel
	 */
	protected abstract void writeFile(File f) throws IOException;

	/**
	 * This read a file from input channel
	 */
	protected abstract void readFile(File f) throws IOException;

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 */
	@Override
	public synchronized void resetSockets() throws IOException, InvalidKeyException, AccessControlException {
		logger.error("resetSockets not implemented");
	}

	/**
	 * This is called by run() from inherited classes. This do not close
	 * communication channel. This pushes connection back to commserver channel
	 * stack
	 *
	 * @param command
	 *            is the command received from communication channel
	 */
	protected void run(final XMLRPCCommand command) {

		if (command == null) {
			error("CMD is null");
			return;
		}

		IdRpc idRpc = null;

		int accessStatus = 200;
		final int accessAnswerSize = 0;
		XMLable result = null;
		long resultSize = 0;

		try {

			idRpc = command.getIdRpc();

			final String newConnection = "<newConnection IDRPC='" + idRpc.toString() + "'>";
			debug(msgWithRemoteAddresse(newConnection) + " " + command.toXml());
			mileStone(newConnection);

			switch (idRpc) {
			case DISCONNECT:
				disconnect(command);
				result = NOANSWER;
				break;
			case SHUTDOWN:
				shutDown(command);
				result = NOANSWER;
				break;
			case REMOVE:
				remove(command);
				result = NOANSWER;
				break;
			case VERSION:
				result = CURRENTVERSION;
				break;
			case GET:
				result = get((XMLRPCCommandGet)command);
				break;
			case GETWORKBYEXTERNALID:
				result = get((XMLRPCCommandGetWorkByExternalId)command);
				break;
			case GETTASK: {
				result = getTask(command);
				break;
			}
			case GETAPPS:
				result = getApps(command);
				break;
			case SEND:
				if (command.getParameter() == null) {
					throw new IOException("Can't retrieve command parameter");
				}
				try {
					sendWork(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendData(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendGroup(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendSession(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendApp(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendUser(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendUserGroup(command);
					break;
				} catch (final ClassCastException e) {
				}
				try {
					sendTrace(command);
					break;
				} catch (final ClassCastException e) {
				}
				throw new IOException("Insertion error: bad object type");
			case SENDAPP:
				sendApp(command);
				break;
			case GETDATAS:
				result = getDatas(command);
				break;
			case SENDDATA:
				sendData(command);
				break;
			case UPLOADDATA: {
				resultSize = uploadData(command);
				result = NOANSWER;
				break;
			}
			case DOWNLOADDATA: {
				resultSize = downloadData(command);
				result = NOANSWER;
				break;
			}
			case GETGROUPS:
				result = getGroups(command);
				break;
			case GETGROUPWORKS: {
				result = getGroupWorks(command);
				break;
			}
			case SENDGROUP:
				sendGroup(command);
				break;
			case ACTIVATEHOST: {
				activateWorker(command);
				break;
			}
			case CHMOD: {
				chmod(command);
				break;
			}
			case GETHUBADDR:
				Hashtable<String, String> reth = getHubAddr(command);
				result = new XMLHashtable(reth);
				reth = null;
				break;
			case GETHOSTS:
				result = getWorkers(command);
				break;
			case GETSESSIONS:
				result = getSessions(command);
				break;
			case SENDSESSION:
				sendSession(command);
				break;
			case GETTASKS:
				result = getTasks(command);
				break;
			case GETTRACES:
				result = getTraces(command);
				break;
			case SENDTRACE:
				sendTrace(command);
				break;
			case SENDUSER:
				sendUser(command);
				break;
			case GETUSERBYLOGIN:
				result = getUser(command);
				break;
			case GETUSERS:
				result = getUsers(command);
				break;
			case GETUSERGROUPS:
				result = getUserGroups(command);
				break;
			case SENDUSERGROUP:
				sendUserGroup(command);
				break;
			case GETWORKS:
				result = getWorks(command);
				break;
			case SENDWORK: {
				sendWork(command);
				break;
			}
			case BROADCASTWORK: {
				broadcast(command);
				result = NOANSWER;
				break;
			}
			case WORKREQUEST: {
				result = workRequest(command);
				break;
			}
			case WORKALIVEBYUID: {
				final Hashtable hash = workAlive(command);
				result = new XMLHashtable(hash);
				break;
			}
			case WORKALIVE:
				// here is a big hack to force "old" worker to update
				if (command.getCurrentVersion() == null) {
					warn("Forcing worker to update " + command.getHost().getUID() + " (" + command.getHost().getName()
							+ ")");
					final Hashtable h = new Hashtable();
					h.put("currentversion", CURRENTVERSIONSTRING);
					result = new XMLHashtable(h);
				} else {
					final Hashtable h2 = ((XMLHashtable) command.getParameter()).getHashtable();
					Hashtable ret = workAlive(command, h2);
					result = new XMLHashtable(ret);
					ret = null;
				}
				break;
			case PING:
				result = NOANSWER;
				break;

			default:
				error("BAD Id: " + idRpc);
				accessStatus = 404;
				break;
			}
		} catch (final EOFException e) {
			error(e);
		} catch (final SocketException e) {
			debug(e.toString());
		} catch (final IOException e) {
			error("Disk access error " + e);
			result = new XMLRPCResult(XWReturnCode.DISK, e.getMessage());
		} catch (final InvalidKeyException e) {
			error("Right access error " + e);
			result = new XMLRPCResult(XWReturnCode.AUTHENTICATION, e.getMessage());
			accessStatus = 401;
		} catch (final AccessControlException e) {
			error("Right access error " + e);
			result = new XMLRPCResult(XWReturnCode.AUTHORIZATION, e.getMessage());
			accessStatus = 403;
		} catch (final Exception e) {
			error("Cannot get io socket ", e);
		} finally {
			if (result == null) {
				result = NOANSWER;
			}
			String resultxml = result.toXml();
			resultxml = resultxml.substring(0, Math.min(resultxml.length(), 150));
			try {
				if (command.getCurrentVersion() == null) {
					result.resetCurrentVersion();
				} else {
					if (result.getCurrentVersion() == null) {
						result.setCurrentVersion();
					}
				}

				// if (idRpc != IdRpc.SEND) {
				debug("answer (" + idRpc.toString() + ") " + resultxml);
				// }
				write(result);
			} catch (final Exception e) {
				logger.error("Caught exception, Can't write result", e);
			} finally {
				resultxml = null;
			}
			mileStone("</newConnection>");
		}

		try {
			String accessProto = null;
			if (this instanceof TCPHandler) {
				accessProto = "TCP";
			}
			if (this instanceof UDPHandler) {
				accessProto = "UDP";
			}
			if (this instanceof HTTPHandler) {
				accessProto = "HTTP";
			}

			final URI accessuri = command.getURI();
			if (accessuri != null) {
				final String accessPath = accessuri.toString();
				final UserInterface user = command.getUser();
				final String login = user != null ? user.getLogin() : "-";
				String os = null;
				try {
					os = command.getHost().getOs().toString();
				} catch (final Exception e) {
					os = "-";
				}
				AccessLogger.getInstance().println(accessPath, login, accessProto, accessStatus,
						result != NOANSWER ? result.toXml().length() : resultSize, remoteName, os, idRpc);
			}
		} catch (final Exception e) {
			error(e);
		}

		result = null;
		idRpc = null;

	}

//	/**
//	 * Set workers running parameters.
//	 *
//	 * @param client
//	 *            is the client identifier
//	 * @param nbWorkers
//	 *            is the expected number of workers to activate.
//	 * @param p
//	 *            are the parameters to set
//	 * @return the number of activated workers, -1 on error
//	 */
//	private synchronized int setWorkersParameters(final UserInterface client, final int nbWorkers,
//			final WorkerParameters p) throws IOException, InvalidKeyException, AccessControlException {
//
//		mileStone("<setWorkersParameters>");
//
//		final int ret = DBInterface.getInstance().activateWorkers(client, nbWorkers);
//		tracesSendResultDelay = p.getSendResultDelay();
//		tracesResultDelay = p.getResultDelay();
//
//		mileStone("</setWorkersParameters");
//
//		notifyAll();
//		return ret;
//	}

	/**
	 * Get workers running parameters.
	 *
	 * @return workers parameters
	 */
	private WorkerParameters getWorkersParameters(final UserInterface client) {

		final WorkerParameters params = new WorkerParameters();
		params.setSendResultDelay(tracesSendResultDelay);
		params.setResultDelay(tracesResultDelay);
		return params;
	}

	/**
	 * Call to the scheduler to select a work
	 *
	 * @return a Description of the Work the server has to complete
	 */
	protected synchronized WorkInterface workRequest(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final HostInterface _host = command.getHost();

		mileStone("<workRequest host=" + (_host != null ? _host.getName() : "null") + ">");
		try {
			final UserInterface user = DBInterface.getInstance().checkClient(command, UserRightEnum.GETJOB);

			_host.setIPAddr(remoteIP);
			final HostInterface host = DBInterface.getInstance().hostRegister(user, _host);

			if (host == null) {
				throw new IOException("can't register host");
			}
			if (host.isActive() && host.getVersion().equals(CURRENTVERSIONSTRING)) {
				return Dispatcher.getScheduler().select(host, user);
			}
		} catch (InvalidKeyException e) {
			logger.error("Caught exception, workRequest error", e);
			throw e;
		} catch (final Exception e) {
			logger.error("Caught exception, workRequest error", e);
			throw new IOException(e);
		} finally {
			mileStone("</workRequest>");
			notifyAll();
		}
		debug("worker " + _host.getName() + " gets nothing");
		return null;
	}

	/**
	 */
	private void shutDown(final XMLRPCCommand cmd)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("shutdown not implemented");
//		shutDown(user, command.getHost());
		// mileStone("<shutdown>");
		// DBInterface.getInstance().shutDown(client, host);
		// mileStone("</shutdown>");
	}


	/**
	 * This is sent by the worker for each job it has. This is not called if the
	 * worker has no job.
	 *
	 * This gets current job UID from worker. This sends a boolean back to
	 * worker which tells whether to stop computing this jobs TRUE to continue
	 * and FALSE to stop
	 *
	 * @param jobUID
	 *            is the UID od the computing job on worker side
	 * @return a hashtable containing this job status so that worker continue or
	 *         stop computing it
	 */
	public Hashtable workAlive(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final HostInterface _host = command.getHost();
		if (_host == null) {
			throw new IOException("host is not set");
		}

		boolean keepWorking = false;
		final UID jobUID = ((XMLRPCCommandWorkAliveByUID) command).getURI().getUID();

		mileStone("<workAlive host=" + (_host != null ? _host.getName() : "null") + ">");
		final UserInterface theClient = DBInterface.getInstance().checkClient(command, UserRightEnum.GETJOB);

		_host.setIPAddr(remoteIP);
		final HostInterface theHost = DBInterface.getInstance().hostRegister(theClient, _host);

		if (theHost == null) {
			throw new IOException("workAlive : can't find host ");
		}
		final boolean isActive = _host.isActive();

		logger.debug("retrieving current computing job");
		// theHost = DBInterface.getInstance().host(_host.getUID());
		final WorkInterface theWork = DBInterface.getInstance().work(theClient, jobUID);
		final TaskInterface theTask = DBInterface.getInstance().task(theWork, theHost);

		if (theTask != null) {
			if (!isActive) {
				theWork.unlockWork();
				theTask.setError();
				keepWorking = false;
			} else {
				try {
					keepWorking = theTask.setAlive(_host.getUID());
				} catch (final Exception e) {
					error(e);
				}
			}
			theTask.update();
		} else {
			logger.trace("work is not in the dispatcher pool. Send back 'abort'");
			keepWorking = false;
		}

		if (!keepWorking) {
			warn("workAlive(" + _host.getName() + "," + jobUID + ") stopping!");
		}
		final Hashtable result = new Hashtable();
		result.put("keepWorking", new Boolean(keepWorking));
		mileStone("</workAlive>");
		return result;

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
	 * <li>the SmartSockets hub address
	 * <li>traces, a boolean, to collect traces (or not);
	 * <li>tracesSendResultDelay, an integer, contains traces results transfert
	 * to coordinator periodicity;
	 * <li>tracesResultDelay, an integer, contains traces collection
	 * periodicity.
	 * <li>savedTasks, a Vector, contains works saved by coordinator that worker
	 * can delete from local disk savedTasks also contains works that the
	 * coordinator is not able to save for any reason.
	 * <li>resultsExpected, a Vector, contains tasks the coordinator wants the
	 * worker sends again results for
	 * <li>this server current version
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
	public Hashtable workAlive(final XMLRPCCommand command, final Hashtable rmiParams)
			throws IOException, InvalidKeyException, AccessControlException {

		HostInterface _host = command.getHost();
		mileStone("<workAlive host=" + (_host != null ? _host.getName() : "null") + ">");
		final UserInterface theClient = DBInterface.getInstance().checkClient(command, UserRightEnum.GETJOB);

		_host.setIPAddr(remoteIP);
		final HostInterface theHost = DBInterface.getInstance().hostRegister(theClient, _host);

		if (theHost == null) {
			throw new IOException("workAlive : can't find host ");
		}
		final Hashtable ret = new Hashtable();
		final boolean isTracing = theHost.isTracing();
		final boolean isActive = theHost.isActive();

		ret.put(Connection.HUBPNAME, System.getProperty(XWPropertyDefs.SMARTSOCKETSHUBADDR.toString()));
		ret.put(XWPostParams.TRACES.toString(), new Boolean(isTracing));
		ret.put(XWPostParams.TRACESSENDRESULTDELAY.toString(), new Integer(tracesSendResultDelay));
		ret.put(XWPostParams.TRACESRESULTDELAY.toString(), new Integer(tracesResultDelay));
		ret.put(XWPostParams.CURRENTVERSION.toString(), CURRENTVERSIONSTRING);

		logger.trace("retrieve saved tasks so that the worker cleans its local copy");
		final Vector<UID> finishedTasks = new Vector<>();
		final Vector<UID> resultsVector = new Vector<>();

		try {
			//
			// It finally checks whether worker should erase some results it
			// still
			// owns.
			// This may happen if worker has some deleted job results on its
			// disk.
			// (i.e. if client has submitted a job which has been dispatched and
			// computed on a worker while the client finally decided to delete
			// that
			// job)
			//
			final Vector jobResults = (Vector) rmiParams.get(XWPostParams.JOBRESULTS.toString());

			if (jobResults != null) {

				debug("workAlive (" + _host.getName() + ") : jobResults.size () = " + jobResults.size());

				final Iterator<XMLValue> vIterator = jobResults.iterator();

				if (vIterator != null) {
					while (vIterator.hasNext()) {

						final XMLValue v = vIterator.next();
						final UID workUID = (UID) v.getValue();

						if (workUID == null) {
							continue;
						}
						final WorkInterface w = DBInterface.getInstance().work(theClient, workUID);
						if (w == null) {
							// we don't know that job, remove result on worker
							// side
							// (maybe it has been removed by user)
							debug("workAlive (" + _host.getName() + ") : worker must stop " + workUID);
							finishedTasks.add(workUID);
						} else {
							final URI resultURI = w.getResult();
							final StatusEnum workStatus = w.getStatus();

							debug("resultURI = " + resultURI);
							if ((resultURI != null) && (resultURI.isXtremWeb())) {
								final UID resultUID = resultURI.getUID();
								final DataInterface workResult = DBInterface.getInstance().data(resultUID);
								debug("workResult (" + resultURI + ") = " + workResult);
								if (workResult != null) {
									final StatusEnum resultStatus = workResult.getStatus();
									if ((workStatus == StatusEnum.DATAREQUEST)
											|| (resultStatus == StatusEnum.DATAREQUEST)) {
										warn("workAlive (" + _host.getName() + ") : reasking result for " + workUID);
										resultsVector.add(workUID);
									}
								}
							}

							switch (workStatus) {
							case ERROR:
							case COMPLETED:
								debug("workAlive (" + _host.getName() + ") : worker can delete " + resultURI);
								finishedTasks.add(workUID);
								break;
							}
						}
					}
				}
			}

			ret.put(XWPostParams.FINISHEDTASKS.toString(), finishedTasks);

			ret.put(XWPostParams.RESULTEXPECTEDS.toString(), resultsVector);

			UID hostuid = theHost.getUID();
			String newServer = DBInterface.getInstance().getServer(hostuid);
			hostuid = null;

			if (newServer != null) {
				info("workAlive (" + theHost.getName() + ") : new server = " + newServer);
				ret.put(XWPostParams.NEWSERVER.toString(), newServer);
			}
			newServer = null;

			Integer aliveperiod = new Integer(config.getProperty(XWPropertyDefs.ALIVEPERIOD));
			debug("alivePeriod = " + aliveperiod);
			ret.put(XWPostParams.ALIVEPERIOD.toString(), aliveperiod);
			aliveperiod = null;
		} catch (final IOException e) {
			error(e);
		} finally {
			if (theHost != null) {
				try {
					theHost.update();
				} catch (final Exception e) {
					error("cant update host " + e);
				}
			}
		}

		String newKeystoreUriStr = config.getProperty(XWPropertyDefs.KEYSTOREURI);
		debug("\n\n\nnewKeystoreUriStr =  " + newKeystoreUriStr);
		if (newKeystoreUriStr != null) {
			ret.put(XWPostParams.KEYSTOREURI.toString(), newKeystoreUriStr);
		}
		newKeystoreUriStr = null;

		mileStone("</workAlive>");
		return ret;

	}

	/** Tracer */
	private void tactivityMonitor(final HostInterface host, final long start, final long end, final byte[] file)
			throws IOException, InvalidKeyException, AccessControlException {

		tactivityMonitor(host, new Long(start).toString(), new Long(end).toString(), file);
	}

	private synchronized void tactivityMonitor(final HostInterface host, final String start, final String end,
			final byte[] file) throws IOException, InvalidKeyException, AccessControlException {
		final long startValue = (new Long(start)).longValue();
		final long endValue = (new Long(end)).longValue();

		DBInterface.getInstance().writeStatFile(host, startValue, endValue, file);
		notifyAll();
	}

	/**
	 * This disconnects a client from server
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 */
	protected void disconnect(final XMLRPCCommand cmd) throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandDisconnect disconnect = new DBCommandDisconnect(DBInterface.getInstance()); 
		disconnect.exec(cmd);
	}

	/**
	 * This creates or updates an application on server side This calls
	 * DBInterface::addApplication() to check whether client has the right to do
	 * so.
	 *
	 * @see DBInterface#addApp(UserInterface, AppInterface)
	 */
	protected void sendApp(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandSendApp dbc = new DBCommandSendApp(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This retrieves an object from server
	 */
	protected XMLable get(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandGet dbc = new DBCommandGet(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves an object from server
	 */
	protected XMLable get(final XMLRPCCommandGet command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandGet dbc = new DBCommandGet(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves an object from server
	 */
	protected XMLable get(final XMLRPCCommandGetWorkByExternalId command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandGetWorkByExternalId dbc = new DBCommandGetWorkByExternalId(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves a task from server On Apr, 2012, we introduce GETTASK
	 * message to ease client usage and help to retrieve task from work uid This
	 * aims to retrieve task from either its UID **or** its WORKUID
	 */
	public XMLable getTask(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandGetTask dbc = new DBCommandGetTask(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves all applications from server
	 *
	 * @return a vector of UIDs
	 */
	protected XMLable getApps(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetApps dbc = new DBCommandGetApps(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This changes access rights of the given object with the given access
	 * rights modifier
	 *
	 * @param client
	 *            is the requesting client
	 * @param uri
	 *            is the URI of the object to change the access rights for
	 * @param modifier
	 *            is the access rights modifier
	 * @see xtremweb.security.XWAccessRights#chmod(String)
	 * @since 5.8.0
	 */
	protected void chmod(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandChmod dbc = new DBCommandChmod(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This retrieves the known SmartSockets hub address
	 *
	 * @param _user
	 *            is the caller credentials
	 * @return a hashtable containing the SmartSockets hub address
	 * @since 8.0.0
	 */
	public Hashtable<String, String> getHubAddr(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		mileStone("<getHubAddr>");
		DBInterface.getInstance().checkClient(command, UserRightEnum.GETJOB);

		final Hashtable<String, String> ret = new Hashtable<>();
		logger.debug("hub addr = " + System.getProperty(XWPropertyDefs.SMARTSOCKETSHUBADDR.toString()));
		ret.put(Connection.HUBPNAME, System.getProperty(XWPropertyDefs.SMARTSOCKETSHUBADDR.toString()));
		mileStone("</getHubAddr>");
		return ret;
	}

	/**
	 * This removes an application from server
	 *
	 * @param client
	 *            defines the client
	 * @param uid
	 *            is the UID of the object to remove
	 */
	protected void remove(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandRemove remove = new DBCommandRemove(DBInterface.getInstance()); 
		remove.exec(command);
	}

	/**
	 * This creates or updates data on server side
	 *
	 * @param client
	 *            is the caller attributes
	 * @param data
	 *            is the data to create
	 */
	protected void sendData(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandSendData dbc = new DBCommandSendData(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This retrieves a data from server
	 */
	public XMLable getDatas(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetDatas dbc = new DBCommandGetDatas(DBInterface.getInstance()); 
		return dbc.exec(command);
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
	 * @return the size of the uploaded data
	 */
	public long uploadData(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		long ret = 0;

		mileStone("<uploadData>");
		DataInterface theData = null;
		File dFile = null;
		StatusEnum dataStatus = StatusEnum.AVAILABLE;
		UID uid = command.getURI().getUID();

		try {
			theData = (DataInterface)get(command);
			if (theData == null) {
				throw new IOException("uploadData(" + uid + ") data not found");
			}
			dFile = theData.getPath();
			readFile(dFile);
		} catch (final InvalidKeyException e) {
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			throw e;
		} catch (final AccessControlException e) {
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			throw e;
		} catch (final IOException ioe) {
			mileStone("<error method='uploadData' msg='" + ioe.getMessage() + "' />");
			if (dFile != null) {
				dFile.delete();
			}
			logger.error("Caught exception: ", ioe);
			dataStatus = StatusEnum.DATAREQUEST;
			throw new IOException("uploadData(" + uid + ") IOerror on server side");
		} catch (final Exception e) {
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			logger.error("Caught exception: ", e);
			dataStatus = StatusEnum.ERROR;
			throw new IOException(e.getMessage());
		} finally {
			try {
				if (theData != null) {
					theData.setStatus(dataStatus);
					theData.update();
				}
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
			if (dFile != null) {
				ret = dFile.length();
			}
			theData = null;
			dFile = null;
			mileStone("</uploadData>");
		}
		return ret;
	}

	/**
	 * This downloads a data from server
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uid
	 *            is the UID of the data to download
	 */
	public long downloadData(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		long ret = 0;
		DataInterface theData = null;
		UID uid = command.getURI().getUID();

		mileStone("<downloadData>");
		try {
			theData = (DataInterface)get(command);
			if (theData == null) {
				throw new IOException("downloadData(" + uid + ") data not found");
			}

			theData = DBInterface.getInstance().data(uid);

			if ((theData.getSize() > 0) && (theData.getMD5() == null)) {
				error("downloadData setstatus ERROR : size=" + theData.getSize() + ", md5=" + theData.getMD5());
				theData.setStatus(StatusEnum.ERROR);
				theData.update();
				mileStone("<error method='downloadData' msg='MD5 error' />");
				mileStone("</downloadData>");
				throw new IOException("downloadData(" + uid + ") MD5 should not be null");
			}

			final File dFile = theData.getPath();
			writeFile(dFile);
			ret = 0;
			if (dFile != null) {
				ret = dFile.length();
			} else {
				throw new IOException("downloadData(" + uid + ") file not found");
			}
			if (theData != null) {
				theData.update();
			} else {
				throw new IOException("downloadData(" + uid + ") data error");
			}
		} catch (final InvalidKeyException e) {
			mileStone("<error method='downloadData' msg='" + e.getMessage() + "' />");
			throw e;
		} catch (final AccessControlException e) {
			mileStone("<error method='downloadData' msg='" + e.getMessage() + "' />");
			throw e;
		} catch (final Exception e) {
			mileStone("<error method='downloadData' msg='" + e.getMessage() + "' />");
			error(e);
			if (theData != null) {
				error("downloadData setstatus ERROR : " + e);
				theData.setStatus(StatusEnum.ERROR);
				theData.update();
			}
			throw new IOException(e.toString());
		} finally {
			theData = null;
			mileStone("</downloadData>");
		}
		return ret;
	}

	/**
	 * This creates or updates a group on server side
	 *
	 * @param client
	 *            contains user id/password
	 * @param group
	 *            is the group to send
	 */
	public void sendGroup(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandSendGroup dbc = new DBCommandSendGroup(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This retrieves this client groups
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 */
	protected XMLable getGroups(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetGroups dbc = new DBCommandGetGroups(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves all works for the given group
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 * @param group
	 *            is the group UID to retrieve works for
	 */
	protected XMLable getGroupWorks(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetGroupWorks dbc = new DBCommandGetGroupWorks(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This creates or updates an session on server side
	 */
	public void sendSession(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		
		final DBCommandSendSession dbc = new DBCommandSendSession(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This retrieves all sessions from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLable getSessions(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetSessions dbc = new DBCommandGetSessions(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves all works for the given session
	 *
	 * @param client
	 *            defines this client attributes, such as user ID, password etc.
	 * @param session
	 *            is the session UID to retrieve works for
	 */
	public XMLVector getSessionWorks(final UserInterface client, final UID session)
			throws IOException, InvalidKeyException, AccessControlException {

		try {
			mileStone("<getSessionWorks>");
			final Vector<UID> ret = (Vector<UID>) DBInterface.getInstance().getSessionJobs(client, session);
			XMLVector v = null;
			if (ret != null) {
				v = new XMLVector(ret);
			}
			return v;
		} finally {
			mileStone("</getSessionWorks>");
		}
	}

	/**
	 * This creates or updates an work on server side This calls
	 * DBInterface#insertWork()
	 *
	 * @see DBInterface#addWork(XMLRPCCommand)
	 */
	protected void sendWork(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandSendWork dbc = new DBCommandSendWork(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * @deprecated since 1.9.0 this is deprecated ; sendWork() should be used
	 *             instead stdin and dirin must be sent using sendData
	 * @see #sendWork(UserInterface, HostInterface, WorkInterface)
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Deprecated
	public void submit(final UserInterface client, final WorkInterface job)
			throws IOException, InvalidKeyException, AccessControlException {
	}

	/**
	 * This retrieves an work from server, including all associated files
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uid
	 *            is the work UID
	 * @return a WorkInterface object
	 */
	public WorkInterface loadWork(final UserInterface client, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("TCPHandler::loadWork() not implemented yet");
	}

	/**
	 * This retrieves all works from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLable getWorks(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetWorks dbc = new DBCommandGetWorks(DBInterface.getInstance()); 
		return dbc.exec(command);
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
	public XMLable broadcast(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandBroadcast dbc = new DBCommandBroadcast(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This always throws an exception
	 *
	 * @deprecated since 1.9.0
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Deprecated
	public DataInterface getResult(final UserInterface client, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("GETRESULT is deprecated; please use GETDATA");
	}

	/**
	 * This always throws an exception
	 *
	 * @deprecated since 1.9.0
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Deprecated
	public void sendResult(final UserInterface client, final DataInterface result)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("SENDRESULT is deprecated; please use SENDDATA");
	}

	/**
	 * This creates or updates an worker on server side
	 */
	protected void sendWorker(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("TCPHandler::sendWorker TCP not implemented yet");
	}

	/**
	 * This retrieves all workers from server
	 *
	 * @return a vector of UIDs
	 */
	protected XMLable getWorkers(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetHosts dbc = new DBCommandGetHosts(DBInterface.getInstance()); 
		return dbc.exec(command);
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
	protected void activateWorker(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandActivateWorker dbc = new DBCommandActivateWorker(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This set the expected number of workers
	 *
	 * @param client
	 *            contains client parameters
	 * @return the number of activated workers, -1 on error
	 */
//	public int setWorkersNb(final UserInterface client, final int nb)
//			throws IOException, InvalidKeyException, AccessControlException {
//		return DBInterface.getInstance().activateWorkers(client, nb);
//	}

	/**
	 * This adds a new user.
	 *
	 * @param client
	 *            contains client parameters
	 * @param user
	 *            describes new user informations
	 */
	protected void sendUser(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final DBCommandSendUser dbc = new DBCommandSendUser(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This adds/updates an user group. This forces group.project to true
	 */
	public void sendUserGroup(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandSendUser dbc = new DBCommandSendUser(DBInterface.getInstance()); 
		dbc.exec(command);
	}

	/**
	 * This retrieves all usergroups from server
	 *
	 * @return a vector of UIDs
	 */
	public XMLable getUserGroups(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetUserGroups dbc = new DBCommandGetUserGroups(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves an user from server
	 */
	public XMLable getUser(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetUser dbc = new DBCommandGetUser(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves all users from server
	 *
	 * @return a vector of UIDs
	 */
	protected XMLable getUsers(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetUsers dbc = new DBCommandGetUsers(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This retrieves all tasks from server
	 *
	 * @return a vector of UIDs
	 */
	protected XMLable getTasks(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final DBCommandGetTasks dbc = new DBCommandGetTasks(DBInterface.getInstance()); 
		return dbc.exec(command);
	}

	/**
	 * This is not implemented
	 */
	public void sendTrace(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("sendTrace not implemented yet");
	}

	/**
	 * This retrieves all traces from server
	 *
	 * @return a vector of UIDs
	 */
	protected XMLable getTraces(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("getTraces not implemented yet");
	}

	/**
	 * Get all known traces.
	 *
	 * @return an vector of TraceInterface
	 */
	public XMLVector getTraces(final UserInterface client, final Date since, final Date before)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("getTraces not implemented yet");
	}

	/**
	 * Get traces files path.
	 *
	 * @return a string containing traces files path.
	 */
	public String getTracesPath() throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("getTracesPath not implemented yet");
	}

	/**
	 * Get the path of traces files.
	 *
	 * @return a string describing path to traces files.
	 */
	public String getTracesPath(final UserInterface client)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("getTracesPath not implemented yet");
	}

	/**
	 * Get trusted addresses
	 *
	 * @return a string containing trused ip addresses separated by a white
	 *         space.
	 */
	public String getTrustedAddresses(final UserInterface client)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("getTrustedAddress is not implemented");
	}

	/**
	 * Add a trusted address
	 *
	 * @param ip
	 *            new trusted IP
	 */
	public void addTrustedAddress(final UserInterface client, final String ip)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("addTrustedAddress is not implemented");
	}

	/**
	 * this is not implemented
	 *
	 * @param ip
	 *            trusted IP to remove
	 */
	public void removeTrustedAddress(final UserInterface client, final String ip)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("removeTrustedAddress is not implemented");
	}

	/**
	 * Set workers trace flag.
	 *
	 * @param hosts
	 *            is a hashtable which contains host name as key and their
	 *            dedicated trace flag as value.
	 */
	public void traceWorkers(final UserInterface client, final Hashtable hosts)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException("traceWorkers is not implemented");
//		mileStone("<traceWorkers>");
//		DBInterface.getInstance().traceWorkers(client, hosts);
//		mileStone("</traceWorkers>");
	}

}
