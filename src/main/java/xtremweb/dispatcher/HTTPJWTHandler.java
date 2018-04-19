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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.expressme.openid.Authentication;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.core.oauth.OAuthService;

import xtremweb.common.Logger;
import xtremweb.common.LoggerLevel;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;
import xtremweb.communications.Connection;
import xtremweb.dispatcher.HTTPOAuthHandler.OAuthException;
import xtremweb.dispatcher.HTTPOAuthHandler.Operator;

/**
 * This handles HTTP request to /jwt/ This accepts and verifies
 * http://jwt.io/ Json Web Tokens.
 *
 * @author Oleg Lodygensky
 * @since XWHEP 11.0.0
 */

public abstract class HTTPJWTHandler extends HTTPHandler {


	/**
	 * This contains the gap while a login is valid
	 *
	 * @see xtremweb.common.XWPropertyDefs#LOGINTIMEOUT
	 */
	long loginTimeout = 0;

	public static final String handlerPath = "/jwt";


	/** this contains this server URL */
	protected URL localRootUrl;

	protected static HTTPJWTHandler instance;

	protected Algorithm algorithm;
	protected JWTVerifier verifier;
	final String jwtethsecret = Dispatcher.getConfig().getProperty(XWPropertyDefs.JWTETHSECRET);
	final String jwtethissuer = Dispatcher.getConfig().getProperty(XWPropertyDefs.JWTETHISSUER);

	/**
	 * @return the instance
	 */
	public static HTTPJWTHandler getInstance() {
		return instance;
	}

	/**
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 */
	public HTTPJWTHandler() throws IllegalArgumentException, UnsupportedEncodingException {
		this("HTTPJWTHandler");
	}
	/**
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 */
	protected HTTPJWTHandler(final String name) throws IllegalArgumentException, UnsupportedEncodingException {
		super();
		if (instance != null) {
			return;
		}
		loginTimeout = Dispatcher.getConfig().getInt(XWPropertyDefs.LOGINTIMEOUT) * 1000;
		try {
			localRootUrl = new URL(Connection.HTTPSSLSCHEME + "://" + XWTools.getLocalHostName() + ":"
					+ Dispatcher.getConfig().getPort(Connection.HTTPSPORT));
		} catch (final MalformedURLException e) {
			XWTools.fatal(e.getMessage());
		}

		getLogger().debug("JWT secret = " + jwtethsecret);
		getLogger().debug("JWT issuer = " + jwtethissuer);
		algorithm = Algorithm.HMAC256(jwtethsecret);
		verifier = JWT.require(algorithm)
				.withIssuer(jwtethissuer)
				.build();
		instance = this;
	}

	/**
	 * This constructor call the default constructor and sets the logger level
	 *
	 * @param l
	 *            is the logger level
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 */
	public HTTPJWTHandler(LoggerLevel l) throws IllegalArgumentException, UnsupportedEncodingException {
		this();
		getLogger().setLoggerLevel(l);
	}

	/**
	 * This does nothing and must be overridden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void setServer(Server server) {
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
	public void removeLifeCycleListener(Listener l) {

	}

	@Override
	public void addLifeCycleListener(Listener l) {

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
		getLogger().debug("close");
	}

	/**
	 * This handles incoming connections. This is inherited from
	 * org.mortbay.jetty.Handler.
	 *
	 * @see xtremweb.communications.XWPostParams
	 */
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest _request, HttpServletResponse _response)
			throws IOException, ServletException {

		getLogger().debug("new connection");
		request = _request;
		response = _response;
        final HttpSession session = request.getSession(true);

		final String path = request.getPathInfo();
		try {
			getLogger().debug("Handling path info      = " + path);
			getLogger().debug("Handling target         = " + target);
			getLogger().debug("Handling request        = " + request.getContentLength() + " " + request.getContentType());
			getLogger().debug("Handling parameter size = " + request.getParameterMap().size());
			getLogger().debug("Handling query string   = " + request.getQueryString());
			getLogger().debug("Handling method         = " + request.getMethod());

			for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
				final String pname = e.nextElement();
				getLogger().debug("parameter name " + pname);
				getLogger().debug("parameter value " + request.getParameter(pname));
			}
			for (final Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
				getLogger().debug("header " + e.nextElement());
			}
			jwtRequest(baseRequest);

		} catch (final Exception e) {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println(
					"<html><head><title>OpenId delegation error</title></head><body><h1>OpenId delegation error</h1><p>Error message: "
							+ e.getMessage()
							+ "</p><p>Please contact the administrator of this XtremWeb-HEP server</p></body></html>");
			getLogger().exception(e);
		}

		response.getWriter().flush();
		request = null;
		response = null;
        baseRequest.setHandled(true);
    }

	/**
	 * This handles XMLHTTPRequest
	 */
	protected abstract void jwtRequest(Request baseRequest) throws IOException;
	/**
	 * This retrieves authentication from openid server response
	 *
	 * @param baseRequest
	 *            is the HTTP request
	 * @return the authentication if found; null otherwise
	 */
	protected Authentication getAuthentication(final Request baseRequest, final String attrName) throws OpenIdException {
		final HttpSession session = baseRequest.getSession(false);
		if (session == null) {
			throw new OpenIdException("session not found");
		}

		// get authentication:
		final String alias = (String) session.getAttribute(attrName);
		return null;
	}

	protected void showAuthentication(PrintWriter pw, Authentication auth) {
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
	 * This retrieves the JWT
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 * @param cookie contains the string representation of the JWT
	 */
	final protected DecodedJWT getToken(final Cookie cookie)
			throws IllegalArgumentException, UnsupportedEncodingException, JWTVerificationException {
			return getToken(cookie.getValue());
	}
	/**
	 * This verifies the JWT
	 * @param strToken is the string representation of the JWT
	 */
	final protected DecodedJWT getToken(final String strToken)
			throws IllegalArgumentException, UnsupportedEncodingException, JWTVerificationException {
		if (strToken == null) {
			throw new JWTVerificationException("string token is null");
		}
		return verifier.verify(strToken);
	}
	/**
	 * This retrieves the expected cookie
	 * @return the found cookie or null
	 */
	final protected Cookie getCookie(final String cookieName) throws IllegalArgumentException {

		final Cookie[] cookies = request.getCookies();
        getLogger().debug("getCookie : " + (cookies == null ? "null" : "" + cookies.length));
		if ((cookies == null) || (cookies.length < 1)){
			return getCookieFromQueryString(cookieName);
		}

		getLogger().debug("cookies.length = " + cookies.length);
		for (int cookieN = 0; cookieN < cookies.length; cookieN++) {
			if (cookies[cookieN].getName().compareTo(cookieName) == 0) {
				return cookies[cookieN];
			}
		}
		throw new IllegalArgumentException("cookie not found : " + cookieName);
	}
	/**
	 * This retrieves the expected cookie from query string
	 * @return the found cookie or null
	 * @since 12.2.8
	 */
	final protected Cookie getCookieFromQueryString(final String cookieName) throws IllegalArgumentException {

		try {
            final String cookieValue = request.getParameter(cookieName);
			getLogger().debug("getCookieFromQueryString ; cookie value : " + cookieValue);
			return new Cookie(cookieName, cookieValue);
		} catch (final Exception e) {
			throw new IllegalArgumentException("getCookieFromQueryString : " + e);
		}
	}
	/**
	 * This simulates a database that store all states:
	 */
	protected final Hashtable<String, Cookie> stateDb = new Hashtable<>();

	/**
	 * This generates a new state (a random string) and stores it in stateDb
	 *
	 * @return the new generated state
	 * @throws NoSuchAlgorithmException 
	 */
	protected String newState(final Cookie token) throws NoSuchAlgorithmException {
		return XWTools.sha256(token.getValue() + System.currentTimeMillis() + Math.random());
	}

	/**
	 * This tests if state exist in database:
	 *
	 * @param state
	 * @throws OAuthException
	 *             is thrown if provided state does not exist
	 */
	void checkState(final String state) throws AccessControlException {
		if (!stateExists(state)) {
			throw new AccessControlException("invalid state");
		}
	}

	/**
	 * This tests if state exist in database:
	 *
	 * @param state
	 * @return true if state exists
	 */
	boolean stateExists(final String state) {
		return stateDb.containsKey(state);
	}

	/**
	 * This tests if state exist in database:
	 *
	 * @param state
	 * @return true if state exists
	 */
	Cookie getState(final String state) {
		return stateDb.get(state);
	}

	/**
	 * This stores state in database, if not already stored
	 * @param state is the state to store
	 * @param jwt is a Cookie containing the signed JWT 
	 */
	void storeState(final String state, final Cookie jwt) {
		stateDb.put(state, jwt);
	}

	/**
	 * This is for testing only
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	}
}
