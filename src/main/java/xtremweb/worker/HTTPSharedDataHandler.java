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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import org.apache.log4j.Logger;


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

public class HTTPSharedDataHandler extends Thread implements Handler {

	public static final String PATH = "/shareddata";

	private static final Logger logger = Logger.getLogger(HTTPSharedDataHandler.class);

	private HttpServletRequest request;
	private HttpServletResponse response;

	/**
	 * This is the HTML parameter to set this worker shared package name
	 *
	 * @since 10.0.0
	 */
	private final static String DATAPACKAGENAME = "datapackagename";
	/**
	 * This is the HTML parameter to set the shared package path
	 *
	 * @since 10.0.0
	 */
	private final static String DATAPACKAGEPATH = "datapackagepath";
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
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 */
	public HTTPSharedDataHandler() {
		super("HTTPSharedHandler");
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void setServer(final Server server) {
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public Server getServer() {
		return null;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isFailed() {
		return true;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isRunning() {
		return false;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarted() {
		return false;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarting() {
		return false;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isStopped() {
		return true;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStopping() {
		return false;
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 */
	@Override
	public void start() {
	}

	/**
	 * This cleans and closes communications
	 */
	public void close() {
		logger.debug("close");
	}

	@Override
	public void removeLifeCycleListener(final Listener l) {

	}

	@Override
	public void addLifeCycleListener(final Listener l) {

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
			logger.debug("Handling target         = " + target);
			logger.debug("Handling request        = " + request.getContentLength() + " " + request.getContentType());
			logger.debug("Handling parameter size = " + request.getParameterMap().size());
			logger.debug("Handling query string   = " + request.getQueryString());
			logger.debug("Handling path info      = " + path);
			logger.debug("Handling method         = " + request.getMethod());

			logger.info("HTTPSharedHandler new connection " + path);

			remoteName = request.getRemoteHost();
			remoteIP = request.getRemoteAddr();
			remotePort = request.getRemotePort();

			if (request.getParameterMap().size() > 0) {
				shareddataRequest();
				baseRequest.setHandled(true);
				return;
			}

			baseRequest.setHandled(true);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
		response.getWriter().flush();
	}

	/**
	 * This handles XMLHTTPRequest
	 */
	private void shareddataRequest() throws IOException {
		try {
			final String pkgName = request.getParameter(DATAPACKAGENAME);
			if (pkgName == null) {
				return;
			}
			Worker.getConfig().getHost().setSharedDatas(pkgName);
			final String pkgPath = request.getParameter(DATAPACKAGEPATH);
			if (pkgPath == null) {
				return;
			}
			Worker.getConfig().setDataPackagesDir(pkgName, pkgPath);

			if (CommManager.getInstance().isSleeping()) {
				CommManager.getInstance().interrupt();
			}
		} catch (final Exception e) {
			logger.error("Caught exception, Can't manage data package", e);
		}
	}
}
