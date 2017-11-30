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

package xtremweb.dispatcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;

import org.apache.log4j.Logger;

import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;
import xtremweb.communications.Connection;
import xtremweb.communications.XWPostParams;

/**
 * This handles HTTP request to /openid/
 *
 * Created: 12 decembre 2012
 *
 * @author Oleg Lodygensky
 * @since XWHEP 8.2.0
 *
 * @deprecated since 10.0.0 OAuth should be preferred (see
 *             {@link HTTPOAuthHandler} )
 */

@Deprecated
public class HTTPOpenIdHandler extends Thread implements org.eclipse.jetty.server.Handler {

	private static final Logger logger = Logger.getLogger(HTTPOpenIdHandler.class);

	private HttpServletRequest request;
	private HttpServletResponse response;

	/**
	 * This contains the gap while a login is valid
	 *
	 * @see xtremweb.common.XWPropertyDefs#LOGINTIMEOUT
	 */
	long loginTimeout = 0;

	static final String ATTR_MAC = "openid_mac";
	static final String ATTR_ALIAS = "openid_alias";

	private OpenIdManager manager;
	public static final String handlerPath = "/openid";

	public static final String OP_GOOGLE = "Google";
	public static final String OP_YAHOO = "Yahoo";

	public static final String OPENID_NONCE_PARAMETER = "openid.response_nonce";
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

	/** this contains this server URL */
	private URL localRootUrl;

	private static HTTPOpenIdHandler instance;

	/**
	 * @return the instance
	 */
	public static HTTPOpenIdHandler getInstance() {
		return instance;
	}

	/**
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 */
	public HTTPOpenIdHandler() {
		super("HTTPOpenIdHandler");
		if (instance != null) {
			return;
		}
		loginTimeout = Dispatcher.getConfig().getInt(XWPropertyDefs.LOGINTIMEOUT) * 1000;
		manager = new OpenIdManager();
		try {
			localRootUrl = new URL(Connection.HTTPSSLSCHEME + "://" + XWTools.getLocalHostName() + ":"
					+ Dispatcher.getConfig().getPort(Connection.HTTPSPORT));
		} catch (final MalformedURLException e) {
			XWTools.fatal(e.getMessage());
		}
		manager.setRealm(localRootUrl.toString());
		// final String returnto = localRootUrl + HTTPHandler.PATH;
		final String returnto = localRootUrl + handlerPath;
		logger.debug("Return to = " + returnto);
		manager.setReturnTo(returnto);
		instance = this;
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

	@Override
	public void removeLifeCycleListener(final Listener l) {

	}

	@Override
	public void addLifeCycleListener(final Listener l) {

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

	/**
	 * This handles incoming connections. This is inherited from
	 * org.mortbay.jetty.Handler.
	 *
	 * @see xtremweb.communications.XWPostParams
	 */
	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest _request,
			final HttpServletResponse _response) throws IOException, ServletException {

		logger.debug("new connection");
		request = _request;
		response = _response;

		final String path = request.getPathInfo();
		try {
			logger.debug("Handling path info      = " + path);
			logger.debug("Handling target         = " + target);
			logger.debug("Handling request        = " + request.getContentLength() + " " + request.getContentType());
			logger.debug("Handling parameter size = " + request.getParameterMap().size());
			logger.debug("Handling query string   = " + request.getQueryString());
			logger.debug("Handling method         = " + request.getMethod());

			remoteName = request.getRemoteHost();
			remoteIP = request.getRemoteAddr();
			remotePort = request.getRemotePort();

			for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
				final String pname = e.nextElement();
				logger.debug("parameter name " + pname);
				logger.debug("parameter value " + request.getParameter(pname));
			}
			for (final Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
				logger.debug("header " + e.nextElement());
			}

			if (request.getParameterMap().size() > 0) {
				openidRequest(baseRequest);
			}
		} catch (final Exception e) {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println(
					"<html><head><title>OpenId delegation error</title></head><body><h1>OpenId delegation error</h1><p>Error message: "
							+ e.getMessage()
							+ "</p><p>Please contact the administrator of this XtremWeb-HEP server</p></body></html>");
			logger.error("Caught exception: ", e);
		}

		baseRequest.setHandled(true);
		response.getWriter().flush();
		request = null;
		response = null;
	}

	/**
	 * This handles XMLHTTPRequest
	 */
	private void openidRequest(final Request baseRequest) throws IOException {
		final String op = request.getParameter(XWPostParams.AUTH_OPERATOR.toString());
		if (op == null) {
			final Authentication auth = getAuthentication(baseRequest);
			final String url = localRootUrl + "?" + XWPostParams.AUTH_NONCE + "="
					+ baseRequest.getParameter(OPENID_NONCE_PARAMETER) + "&" + XWPostParams.AUTH_EMAIL + "="
					+ auth.getEmail() + "&" + XWPostParams.AUTH_IDENTITY + "=" + auth.getIdentity();
			response.sendRedirect(url);
			return;
		}
		if (OP_GOOGLE.equals(op) || OP_YAHOO.equals(op)) {
			// redirect to Google or Yahoo sign on page:
			final Endpoint endpoint = manager.lookupEndpoint(op);
			final Association association = manager.lookupAssociation(endpoint);
			final HttpSession session = baseRequest.getSession(true);
			session.setAttribute(ATTR_MAC, association.getRawMacKey());
			session.setAttribute(ATTR_ALIAS, endpoint.getAlias());
			final String url = manager.getAuthenticationUrl(endpoint, association);
			response.sendRedirect(url);
		} else {
			throw new IOException("Unsupported OP: " + op);
		}
	}

	/**
	 * This retrieves authentication from openid server response
	 *
	 * @param baseRequest
	 *            is the HTTP request
	 * @return the authentication if found; null otherwise
	 */
	private Authentication getAuthentication(final Request baseRequest) throws OpenIdException {
		final HttpSession session = baseRequest.getSession(false);
		if (session == null) {
			throw new OpenIdException("session not found");
		}
		// check sign on result from Google or Yahoo:
		checkNonce(baseRequest.getParameter(OPENID_NONCE_PARAMETER));
		// get authentication:
		final byte[] mac_key = (byte[]) session.getAttribute(ATTR_MAC);
		final String alias = (String) session.getAttribute(ATTR_ALIAS);
		final Authentication authentication = manager.getAuthentication(baseRequest, mac_key, alias);
		return authentication;
	}

	private void showAuthentication(final PrintWriter pw, final Authentication auth) {
		pw.print(
				"<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title>Test JOpenID</title></head><body><h1>You have successfully signed on!</h1>");
		pw.print("<p>Identity: " + auth.getIdentity() + "</p>");
		pw.print("<p>Email: " + auth.getEmail() + "</p>");
		pw.print("<p>Full name: " + auth.getFullname() + "</p>");
		pw.print("<p>First name: " + auth.getFirstname() + "</p>");
		pw.print("<p>Last name: " + auth.getLastname() + "</p>");
		pw.print("<p>Gender: " + auth.getGender() + "</p>");
		pw.print("<p>Language: " + auth.getLanguage() + "</p>");
		pw.print("</body></html>");
		pw.flush();
	}

	/**
	 * This checks if nonce is unic and valid If so, the nonce is revalidated
	 * for a new delay
	 *
	 * @see #loginTimeout
	 * @param nonce
	 * @throws OpenIdException
	 *             if nonce is not valid, already exists or if its survival time
	 *             reached
	 */
	private void checkNonce(final String nonce) throws OpenIdException {
		// check response_nonce to prevent replay-attack:
		if ((nonce == null) || (nonce.length() < 20)) {
			throw new OpenIdException("invalid nonce");
		}
		// make sure the time of server is correct:
		final long nonceTime = getNonceTime(nonce);
		final long diff = Math.abs(System.currentTimeMillis() - nonceTime);
		if (diff > loginTimeout) {
			throw new OpenIdException("bad nonce time");
		}
		if (isNonceExist(nonce)) {
			throw new OpenIdException("unknown noce");
		}
		storeNonce(nonce, nonceTime + loginTimeout);
	}

	/**
	 * This checks if nonce exists and is valid
	 *
	 * @param nonce
	 * @throws OpenIdException
	 *             if nonce is not valid, does not exist or its survival time
	 *             reached
	 */
	public void verifyNonce(final String nonce) throws OpenIdException {
		// check response_nonce to prevent replay-attack:
		if ((nonce == null) || (nonce.length() < 20)) {
			throw new OpenIdException("invalid nonce");
		}
		// make sure the time of server is correct:
		final long nonceTime = getNonceTime(nonce);
		final long diff = Math.abs(System.currentTimeMillis() - nonceTime);
		if (diff > loginTimeout) {
			throw new OpenIdException("bad nonce time");
		}
		if (isNonceExist(nonce) == false) {
			throw new OpenIdException("unknown noce");
		}
	}

	// simulate a database that store all nonce:
	private final Set<String> nonceDb = new HashSet<>();

	// check if nonce is exist in database:
	boolean isNonceExist(final String nonce) {
		return nonceDb.contains(nonce);
	}

	// store nonce in database:
	void storeNonce(final String nonce, final long expires) {
		nonceDb.add(nonce);
		logger.debug("storeNonce(" + nonce + "," + expires + ") = " + nonceDb.contains(nonce));
	}

	long getNonceTime(final String nonce) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(nonce.substring(0, 19) + "+0000").getTime();
		} catch (final ParseException e) {
			throw new OpenIdException("Bad nonce time.");
		}
	}

	/**
	 * This is for testing only
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		int i = 0;
		final boolean sav = (args.length <= 0 ? false : (args[0].compareToIgnoreCase("--sav") == 0));
		if (sav) {
			i = 1;
		}
		for (; i < args.length; i++) {
			XWTools.retrieveCertificates(args[i], sav);
		}
	}
}
