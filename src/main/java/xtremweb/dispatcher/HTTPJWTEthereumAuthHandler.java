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

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.jetty.server.Request;
import org.apache.log4j.Logger;

import xtremweb.communications.XWPostParams;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This handles HTTP request to /ethauth/. This accepts and verifies ethereum
 * public address
 *
 * @author Oleg Lodygensky
 * @since XWHEP 11.0.0
 */

public class HTTPJWTEthereumAuthHandler extends HTTPJWTHandler {

	private static final Logger logger = Logger.getLogger(HTTPJWTEthereumAuthHandler.class);

	public static final String handlerPath = "/ethauth";
	/**
	 * This is the name of the cookie containing the JWT
	 */
	private static final String COOKIENAME_ETHAUTHTOKEN = "ethauthtoken";
	/**
	 * This is the name of the cookie containing the state
	 */
	private static final String COOKIENAME_STATE = XWPostParams.AUTH_STATE.toString();
	/**
	 * This is the JWT claim containing the ethereum address
	 */
	private static final String CLAIM_NAME = "blockchainaddr";
	/**
	 * This is the name of the header variable User-Agent"
	 */
	private static final String USERAGENTNAME = "User-Agent";
	
	/**
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 */
	public HTTPJWTEthereumAuthHandler() throws IllegalArgumentException, UnsupportedEncodingException {
		super("HTTPEthereumAuthHandler");
	}

	/**
	 * This handles XMLHTTPRequest
	 * 
	 * @throws JWTVerificationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	@Override
	final protected void jwtRequest(Request baseRequest)
			throws IllegalArgumentException, JWTVerificationException, IOException {

		final HttpSession session = request.getSession(true);
		logger.debug("session = " + session.getId());
		final Cookie ethauthCookie = getCookie(COOKIENAME_ETHAUTHTOKEN);
		String strState = (String) session.getAttribute(XWPostParams.AUTH_STATE.toString());
		if (strState == null) {
			try {
				final Cookie stateCookie = getCookie(COOKIENAME_STATE);
				if(stateCookie != null) {
					strState = stateCookie.getValue();
				}
			} catch(IllegalArgumentException e) {
			}
		}
		final DecodedJWT token = ethauthCookie != null ? getToken(ethauthCookie) : getToken(getState(strState));

		logger.debug("cookie = " + ethauthCookie);
		logger.debug("getToken(cookie) = " + getToken(ethauthCookie));
		logger.debug("strState = " + strState);
		if(strState != null) {
			logger.debug("getState(strState) = " + getState(strState));
		}
		if(ethauthCookie != null) {
			logger.debug("cookie = " + ethauthCookie.getName() + ":" + ethauthCookie.getValue());
		}

		if (token == null) {
			baseRequest.setHandled(false);
			throw new JWTVerificationException("JWT not found");
		}
		logger.debug("ADDR = " + getEthereumAddress(token));

		if (ethauthCookie != null) {
			final String newState = newState(ethauthCookie);
			logger.debug("newState = " + newState);
			session.setAttribute(XWPostParams.AUTH_STATE.toString(), newState);
			final Cookie cookieUser = new Cookie(XWPostParams.AUTH_STATE.toString(), newState);
			response.addCookie(cookieUser);
			logger.debug("setCookie " + XWPostParams.AUTH_STATE.toString() + " = " + newState);
			storeState(newState, ethauthCookie);
			strState = newState;
		}

		final String url = localRootUrl + "?" + XWPostParams.AUTH_STATE + "=" + strState;
		logger.debug("sendRedirectUrm = " + url);
		logger.debug(USERAGENTNAME + " = "  + request.getHeader(USERAGENTNAME));
		response.sendRedirect(url);

		baseRequest.setHandled(true);
	}

	public String getEthereumAddress(final DecodedJWT token) {
		return token.getClaim(CLAIM_NAME).asString();
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
