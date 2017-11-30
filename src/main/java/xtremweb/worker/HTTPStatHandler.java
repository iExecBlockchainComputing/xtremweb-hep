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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.xml.sax.SAXException;

import xtremweb.common.*;
import org.apache.log4j.Logger;

import xtremweb.communications.CommClient;

/**
 * This handles incoming communications through HTTP. This class aims to help
 * the worker owner to configure its worker. This owner can configure its worker
 * via a web browser.<br />
 *
 * Created: Octobre 2007
 *
 * @author Oleg Lodygensky
 * @version XWHEP 1.0.0
 */

public class HTTPStatHandler extends Thread implements Handler {

	private static final Logger logger = Logger.getLogger(HTTPStatHandler.class);

	public static final String PATH = "/";

	private HttpServletRequest request;
	private HttpServletResponse response;
	private final Hashtable activators;
	private final Hashtable activatorHelps;
	private final Hashtable activatorParams;

	/**
	 * This is the default project : by default the worker accepts any project
	 */
	private static final String DEFAULTPROJECT = "any";
	/** This is an HTML form input name */
	private static final String MAXJOBS = "maxJobs";
	/** This is the HTML params to stop the worker */
	private static final String STOPWORKER = "exit";
	/** This is the HTML params to retreive worker infos */
	private static final String GETHOST = "gethost";
	/** This is the HTML params to retreive the max number of job to compute */
	private static final String GETMAXRUN = "getmaxrun";
	/** This is the HTML params to set the max number of job to compute */
	private static final String SETMAXRUN = "setmaxrun";
	/** This is the HTML params to retreive worker infos */
	private static final String GETRUNNING = "getrunning";
	/** This is the HTML params to retreive availability */
	private static final String GETAVAILABILITY = "getavailability";
	/** This is the HTML params to retreive activators */
	private static final String GETACTIVATORS = "getactivators";
	/** This is the HTML params to retreive activators */
	private static final String GETACTIVATORPARAMS = "getactivatorparams";
	/** This is the HTML params to retreive activators */
	private static final String GETWORKERLEVEL = "getworkerlevel";
	/** This is the HTML params to retreive projects */
	private static final String GETPROJECTS = "getprojects";
	/** This is the HTML params to retreive projects */
	private static final String GROUPJOBSONLY = "setgroupjobsonly";
	/** This is the HTML params to retreive activators */
	private static final String GETAPPLICATION = "getapplication";
	/** This is an HTML form input name */
	private static final String ACTIVATORSELECTOR = "selectActivator";
	/** This is an HTML form input name */
	private static final String ACTIVATORPARAMSNAME = "activatorParams";
	/** This is an HTML form input name */
	private static final String PROJECTSELECTOR = "selectProject";
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
	/**
	 * This has been constructed from config file
	 */
	private Date dater;

	private String projectLabel;

	@Override
	public void removeLifeCycleListener(final Listener l) {

	}

	@Override
	public void addLifeCycleListener(final Listener l) {

	}

	/**
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 */
	public HTTPStatHandler() {
		super("HTTPStatHandler");
		activators = new Hashtable();
		activatorHelps = new Hashtable();
		activatorParams = new Hashtable();
		activators.put("always", "xtremweb.worker.AlwaysActive");
		activators.put("crontab", "xtremweb.worker.DateActivator");

		activatorHelps.put("always", "This fully enables computation");
		activatorHelps.put("crontab", "This enables computation according to schedule gaps");

		activatorParams.put("always", new Boolean(false));
		activatorParams.put("crontab", new Boolean(true));
		projectLabel = null;
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

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 */
	@Override
	public void start() {
	}

	/**
	 * This cleans and closes communications
	 */
	public void close() {
		// Clean up
		logger.debug("close");
	}

	/**
	 * This handles incoming connections. This is inherited from
	 * org.mortbay.jetty.Handler.
	 *
	 * @see xtremweb.communications.XWPostParams
	 */
	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest _request,
			final HttpServletResponse _response) throws IOException, ServletException {

		request = _request;
		response = _response;

		final String path = request.getPathInfo();
		try {
			if (path.compareTo(PATH) != 0) {
				logger.debug("ignoring " + path);
				return;
			}
			logger.debug("new connection " + path);
			remoteName = request.getRemoteHost();
			remoteIP = request.getRemoteAddr();
			remotePort = request.getRemotePort();

			if (request.getParameterMap().size() > 0) {
				xmlHttpRequest();
				baseRequest.setHandled(true);
				return;
			}

			index();
			baseRequest.setHandled(true);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
		response.getWriter().flush();
	}

	/**
	 * This handles XMLHTTPRequest
	 */
	private void xmlHttpRequest() throws IOException {
		final Map paramsMap = request.getParameterMap();

		try {
			final String param = ((String[]) paramsMap.get(STOPWORKER))[0];
			exit();
		} catch (final NullPointerException e) {
		}

		try {

			final String param = ((String[]) paramsMap.get(GETHOST))[0];
			System.out.println("param = " + GETHOST);
			response.getWriter().println("<?xml version='1.0' encoding='ISO-8859-1'?>");
			response.getWriter().println("<worker>");
			response.getWriter().println(Worker.getConfig().getHost().toXml());
			response.getWriter().println("</worker>");
			response.getWriter().flush();
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String param = ((String[]) paramsMap.get(GETAVAILABILITY))[0];
			logger.debug("param = " + GETAVAILABILITY);
			response.getWriter().println("" + ThreadLaunch.getInstance().available() + " : the local activation policy "
					+ (ThreadLaunch.getInstance().available() ? "allows" : "does not allow") + " computation");
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String param = ((String[]) paramsMap.get(GETRUNNING))[0];
			logger.debug("param = " + GETRUNNING);
			getRunning();
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String param = ((String[]) paramsMap.get(GETACTIVATORS))[0];
			logger.debug("param = " + GETACTIVATORS);
			activators();
			return;
		} catch (final NullPointerException e) {
		}
		try {
			final String param = ((String[]) paramsMap.get(GETACTIVATORPARAMS))[0];
			logger.debug("param = " + GETACTIVATORPARAMS);
			final String actParams = ThreadLaunch.getInstance().getActivator().getParams();
			response.getWriter().println(actParams != null ? actParams : "");
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String activator = ((String[]) paramsMap.get(ACTIVATORSELECTOR))[0];
			final String actParams = ((String[]) paramsMap.get(ACTIVATORPARAMSNAME))[0];
			logger.debug("activator = " + activator);
			logger.debug("params = " + actParams);
			setActivator(activator, actParams);
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String maxrun = ((String[]) paramsMap.get(SETMAXRUN))[0];
			logger.debug("maxrun = " + maxrun);
			setMaxRun(Integer.parseInt(maxrun));
			return;
		} catch (final NullPointerException e) {
		}
		try {
			final String maxrun = ((String[]) paramsMap.get(GETMAXRUN))[0];
			logger.debug("maxrun = " + maxrun);
			getMaxRun();
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String param = ((String[]) paramsMap.get(GETPROJECTS))[0];
			logger.debug("param = " + GETPROJECTS);
			projects();
			return;
		} catch (final Exception e) {
		}
		try {
			final String param = ((String[]) paramsMap.get(GROUPJOBSONLY))[0];
			logger.debug("param = " + GROUPJOBSONLY + " = " + param);
			groupJobsOnly(param);
			return;
		} catch (final NullPointerException e) {
		}

		try {
			final String param = ((String[]) paramsMap.get(GETAPPLICATION))[0];
			logger.debug("param = " + GETAPPLICATION);
			currentApplication();
			return;
		} catch (final Exception e) {
		}

		try {
			final String param = ((String[]) paramsMap.get(GETWORKERLEVEL))[0];
			logger.debug("param = " + GETWORKERLEVEL);
			workerLevel();
			return;
		} catch (final NullPointerException e) {
		}
	}

	/**
	 * This display a last page informing that worker is now dead and exits
	 */
	private void exit() throws IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(
				"<div style='left:30%;top:10%;font-size:26px; width:50%;height:25%;vertical-align:middle;text-align:center;'>"
						+ "<p>XWHEP Worker has been stopped</p>"
						+ "<p style='font-size:14px'><i>It will restart on next reboot (if installed)</i></p>"
						+ "</div>");
		response.getWriter().flush();
		logger.error("HTTPStatHandler : exit on user request");
		System.exit(XWReturnCode.FATAL.ordinal());
	}

	/**
	 * This display the default page
	 */
	private void index() throws IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		final String resname = "data/worker.html";
		final InputStream ls = getClass().getClassLoader().getResourceAsStream(resname);
		if ((ls != null) && (ls.available() > 0)) {
			String content = new String();
			final byte[] buf = new byte[10240];
			for (int n = ls.read(buf); n > 0; n = ls.read(buf)) {
				content = content + new String(buf, 0, n);
			}
			response.getWriter().println(content);
		}

		response.getWriter().flush();

	}

	/**
	 * This retreives the name of the current application
	 */
	private void workerLevel() throws IOException {
		if (Worker.getConfig().getUser().getRights().higherThan(UserRightEnum.NONE)) {
			final boolean groupWorker = (Worker.getConfig().getUser().getGroup() != null);
			logger.debug(Worker.getConfig().getUser().getRights().toString() + " .higherOrEquals("
					+ UserRightEnum.WORKER_USER + ") = "
					+ Worker.getConfig().getUser().getRights().higherOrEquals(UserRightEnum.WORKER_USER));
			final boolean workerUser = Worker.getConfig().getUser().getRights()
					.higherOrEquals(UserRightEnum.WORKER_USER);
			String confinement = "Public";
			if (groupWorker) {
				confinement = "Group";
			}
			if (workerUser == false) {
				confinement = "Private";
			}
			response.getWriter().println(confinement + " Worker : this worker runs " + confinement + " Jobs");
		} else {
			response.getWriter().println("Unknwon Worker level");
		}
	}

	/**
	 * This retreives the name of the current application
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private void currentApplication() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException {
		final Vector runs = ThreadLaunch.getInstance().runningWorks();
		if (runs.size() <= 0) {
			response.getWriter().println("<i>nothing</i>");
		} else {
			for (final Iterator it = runs.iterator(); it.hasNext();) {

				CommClient commClient = null;
				try {
					commClient = Worker.getConfig().defaultCommClient();
				} catch (final Exception e) {
					throw new IOException(e.toString());
				}
				CommClient.setConfig(Worker.getConfig());

				final Work work = (Work) it.next();
				if (work == null) {
					continue;
				}

				final AppInterface app = (AppInterface) commClient.get(work.getApplication());

				response.getWriter().println(app.getName());
			}
		}
	}

	/**
	 * This retreives activators and create a dropdown list
	 */
	private void activators() throws IOException {

		final Enumeration theEnum = activators.keys();

		response.getWriter().println("<select name='" + ACTIVATORSELECTOR + "' id='" + ACTIVATORSELECTOR
				+ "'  onchange='showActHelp(this.form, 170)'>");

		final String currAct = ThreadLaunch.getInstance().getActivatorName();

		while (theEnum.hasMoreElements()) {
			final String act = (String) theEnum.nextElement();
			final String actClassName = (String) activators.get(act);
			response.getWriter().print("<option value='" + actClassName + "'");

			final boolean testAct = ((currAct != null) && (currAct.compareToIgnoreCase(actClassName) == 0));

			if (testAct) {
				response.getWriter().print(" selected='selected'");
			}

			response.getWriter().println(">" + act + "</option>");
		}
		response.getWriter().println("</select>");
	}

	/**
	 * This set activator with its policy
	 */
	private void setActivator(final String activator, final String actParams) throws IOException {

		if (activator != null) {
			try {
				ThreadLaunch.getInstance().setupActivator(activator);
			} catch (final InstantiationException e) {
				throw new IOException(e.toString());
			}
		}

		logger.debug("actParams = " + actParams);
		ThreadLaunch.getInstance().getActivator().setParams(actParams);
		logger.debug("ThreadLaunch.instance.getActivator().getParams() = "
				+ ThreadLaunch.getInstance().getActivator().getParams());

		Worker.getConfig().store();
	}

	/**
	 * This displays projects
	 *
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	private void projects() throws IOException, InvalidKeyException, AccessControlException, SAXException,
			URISyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		final boolean groupWorker = (Worker.getConfig().getUser().getGroup() != null);
		final boolean workerUser = Worker.getConfig().getUser().getRights().higherOrEquals(UserRightEnum.WORKER_USER);

		if (groupWorker == false) {
			if (workerUser) {
				response.getWriter().println("Any Public Jobs");
			} else {
				response.getWriter().println("Any Private Jobs");
			}
			return;
		}

		final CommClient commClient = Worker.getConfig().defaultCommClient();
		CommClient.setConfig(Worker.getConfig());
		final Vector<XMLValue> uids = (Vector<XMLValue>) commClient.getUserGroups().getXmlValues();
		String jobTypes = "Public and Group jobs";

		for (final Enumeration<XMLValue> e = uids.elements(); e.hasMoreElements();) {
			final UID uid = (UID) e.nextElement().getValue();
			UserGroupInterface group = null;

			try {
				group = (UserGroupInterface) commClient.get(uid);
			} catch (final Exception ce) {
				logger.error(ce.toString());
				continue;
			}

			if (group.isProject() == false) {
				continue;
			}

			System.out.println("host project " + Worker.getConfig().getHost().getProject());
			System.out.println("group label  " + group.getLabel());
			if (projectLabel == null) {
				if ((Worker.getConfig().getHost().getProject() != null)
						&& (Worker.getConfig().getHost().getProject().compareToIgnoreCase(group.getLabel()) == 0)) {
					projectLabel = group.getLabel();
				}
			}
			if (projectLabel != null) {
				jobTypes = "Group jobs only";
				break;
			}
		}
		response.getWriter().println(jobTypes);
	}

	/**
	 * This sets the max number of jobs
	 */
	private void groupJobsOnly(final String p) {
		logger.debug("groupJobsOnly " + p);
	}

	/**
	 * This retrieves the application currently running
	 */
	private void getRunning() {
		try {
			final Vector runs = ThreadLaunch.getInstance().runningWorks();
			if (runs.size() <= 0) {
				response.getWriter().println("<i>nothing</i>");
			} else {
				for (final Iterator it = runs.iterator(); it.hasNext();) {

					final CommClient commClient = Worker.getConfig().defaultCommClient();
					CommClient.setConfig(Worker.getConfig());

					final Work work = (Work) it.next();
					if (work == null) {
						continue;
					}

					final AppInterface app = (AppInterface) commClient.get(work.getApplication());
					response.getWriter().println(" " + app.getName() + "; ");
				}
			}
		} catch (final Exception e) {
			try {
				response.getWriter().println("<i>n/a</i>");
			} catch (final Exception e2) {
			}
		}
	}

	/**
	 * This sets the max number of jobs
	 */
	private void setMaxRun(final int max) {
		Worker.getConfig().setProperty(XWPropertyDefs.COMPUTINGJOBS, "" + max);
		Worker.getConfig().store();
	}

	/**
	 * This retreives the max number of jobs
	 */
	private void getMaxRun() {
		try {
			response.getWriter().println("" + Worker.getConfig().getInt(XWPropertyDefs.COMPUTINGJOBS));
		} catch (final Exception e) {
		}
	}
}