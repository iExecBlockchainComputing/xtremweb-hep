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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.expressme.openid.Authentication;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.apache.log4j.Logger;


import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.apis.YahooApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;

import xtremweb.common.MD5;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;
import xtremweb.communications.Connection;
import xtremweb.communications.XWPostParams;

/**
 * This handles HTTP request to /oauth2/
 *
 * Created: 19 fevrier 2013
 *
 * @author Oleg Lodygensky
 * @since 9.1.1
 */

public class HTTPOAuthHandler extends Thread implements org.eclipse.jetty.server.Handler {

	private static final Logger logger = Logger.getLogger(HTTPOAuthHandler.class);

	public class OAuthException extends Exception {

		public OAuthException(final String str) {
			super(str);
		}

	}

	public enum Operator {
		FACEBOOK {
			@Override
			public Class getScribeClass() {
				return FacebookApi.class;
			}
		},
		GOOGLE {
			@Override
			public Class getScribeClass() {
				return GoogleApi20.class;
			}
		},
		TWITTER {
			@Override
			public Class getScribeClass() {
				return TwitterApi.class;
			}
		},
		YAHOO {
			@Override
			public Class getScribeClass() {
				return YahooApi.class;
			}
		};
		/**
		 * This is the application id
		 */
		protected String appId;
		/**
		 * This is the application secrete key
		 */
		protected String appKey;
		/**
		 * This is the OAuth scope
		 */
		protected String scope;
		/**
		 * This is the callback url
		 */
		protected URL callbackUrl;
		/**
		 * This is the OAuth uri
		 * 
		 * @since 10.5.0
		 */
		protected URI authUri;
		/**
		 * This is the userinfo uri
		 * 
		 * @since 10.5.0
		 */
		protected URI userinfoEndpointUri;
		/**
		 * This is the user email attribute name
		 * 
		 * @since 10.5.0
		 */
		protected String emailAttrName;
		/**
		 * This is the user id attribute name
		 * 
		 * @since 10.5.0
		 */
		protected String idAttrName;
		/**
		 * This is the server address
		 * 
		 * @since 10.5.0
		 */
		protected String serverAddress;
		/**
		 * This is the service
		 */
		private ServiceBuilder serviceBuilder;
		private BaseApi api;

		/**
		 * This retrieves the operator class from scribe library
		 *
		 * @return the operator class
		 */
		public abstract Class getScribeClass();

		/**
		 * This sets the String representing the application ID
		 */
		public void setAppId(final String id) {
			this.appId = id;
		}

		/**
		 * This retrieves the String representing the application ID
		 *
		 * @return the application identifier
		 */
		public String getAppId() {
			return this.appId;
		}

		/**
		 * This sets the api
		 */
		public void setApi(final BaseApi _api) {
			this.api = _api;
		}

		/**
		 * This retrieves the String representing the application ID
		 *
		 * @return the application identifier
		 */
		public BaseApi getApi() {
			return this.api;
		}

		/**
		 * This sets the String representing the application key
		 */
		public void setAppKey(final String key) {
			this.appKey = key;
		}

		/**
		 * This retrieves the String representing the application key
		 *
		 * @return the application identifier
		 */
		public String getAppKey() {
			return this.appKey;
		}

		/**
		 * This sets the OAuth service scope
		 */
		public void setScope(final String key) {
			this.scope = key;
		}

		/**
		 * This retrieves the service scope
		 *
		 * @return the application identifier
		 */
		public String getScope() {
			return this.scope;
		}

		/**
		 * This sets the OAuth callback url
		 */
		public void setCallbackUrl(final URL key) {
			this.callbackUrl = key;
		}

		/**
		 * This retrieves the callback url
		 *
		 * @return the application identifier
		 */
		public URL getCallbackUrl() {
			return this.callbackUrl;
		}

		/**
		 * This sets the OAuth URI
		 * 
		 * @since 10.5.0
		 */
		public void setOAuthUri(final URI uri) {
			this.authUri = uri;
		}

		/**
		 * This retrieves the OAuth URI
		 * 
		 * @since 10.5.0
		 * @return the auth URI
		 */
		public URI getOAuthUri() {
			return this.authUri;
		}

		/**
		 * This sets the user info URI
		 * 
		 * @since 10.5.0
		 */
		public void setUserInfoEndpointUri(final URI key) {
			this.userinfoEndpointUri = key;
		}

		/**
		 * This retrieves the user info URI
		 * 
		 * @since 10.5.0
		 * @return the auth URI
		 */
		public URI getUserInfoEndpointUri() {
			return this.userinfoEndpointUri;
		}

		/**
		 * This sets the user info URI
		 * 
		 * @since 10.5.0
		 */
		public void setEmailAttrName(final String key) {
			this.emailAttrName = key;
		}

		/**
		 * This retrieves the user info URI
		 * 
		 * @since 10.5.0
		 * @return the auth URI
		 */
		public String getEmailAttrName() {
			return this.emailAttrName;
		}

		/**
		 * This sets the user info URI
		 * 
		 * @since 10.5.0
		 */
		public void setIdAttrName(final String key) {
			this.idAttrName = key;
		}

		/**
		 * This retrieves the user info URI
		 * 
		 * @since 10.5.0
		 * @return the auth URI
		 */
		public String getIdAttrName() {
			return this.idAttrName;
		}

		/**
		 * This sets the server address
		 * 
		 * @since 10.5.0
		 */
		public void setServerAddress(final String key) {
			this.serverAddress = key;
		}

		/**
		 * This retrieves the server address
		 * 
		 * @since 10.5.0
		 * @return the auth URI
		 */
		public String getServerAddress() {
			return this.serverAddress;
		}

		/**
		 * This sets the service
		 * 
		 * @since 10.5.0
		 */
		public void setServiceBuilder(final ServiceBuilder key) {
			this.serviceBuilder = key;
		}

		/**
		 * This retrieves the service
		 * 
		 * @since 10.5.0
		 * @return the auth URI
		 */
		public ServiceBuilder getServiceBuilder() {
			return this.serviceBuilder;
		}

		/**
		 * This retrieves the operator from its name
		 *
		 * @return the operator
		 */
		public static Operator fromString(final String opname) {
			for (final Operator o : Operator.values()) {
				if (o.toString().compareToIgnoreCase(opname) == 0) {
					return o;
				}
			}
			throw new IndexOutOfBoundsException("unvalid Operator value " + opname);
		}
	}

	static final long ONE_HOUR = 3600000L;
	static final long TWO_HOUR = ONE_HOUR * 2L;

	/**
	 * This is the path handled by this handler
	 */
	public static final String handlerPath = "/oauth2";
	/**
	 * This is the facebook operator name
	 */
	// https://www.facebook.com/dialog/oauth?
	// client_id=YOUR_APP_ID
	// &redirect_uri=YOUR_REDIRECT_URI
	// &state=SOME_ARBITRARY_BUT_UNIQUE_STRING
	public static final String OP_FACEBOOK = "Facebook";
	public static final String FACEBOOK_SERVER_ADDR = "www.facebook.com";
	public static final String FACEBOOK_USERINFO_ATTR_NAME = "userinfo_endpoint";
	public static final String FACEBOOK_USEREMAIL_ATTR_NAME = "email";
	public static final String FACEBOOK_USERID_ATTR_NAME = "id";

	/**
	 * These are the google informations
	 *
	 * scope=https://www.googleapis.com/auth/userinfo.email
	 * redirect_uri=https://xwservpub.lal.in2p3.fr/oauth2/google
	 *
	 */
	public static final String GOOGLE_SERVER_ADDR = "www.google.com";
	public static final String GOOGLE_USERINFO_ENDPOINT_ATTR_NAME = "userinfo_endpoint";
	public static final String GOOGLE_USEREMAIL_ATTR_NAME = "email";
	public static final String GOOGLE_USERID_ATTR_NAME = "sub";

	/**
	 * These are the yahoo informations
	 */
	public static final String OP_YAHOO = "Yahoo";
	public static final String YAHOO_SERVER_ADDR = "www.yahoo.com";
	public static final String YAHOO_USERINFO_ATTR_NAME = "userinfo_endpoint";
	public static final String YAHOO_USERID_ATTR_NAME = "id";
	public static final String YAHOO_USEREMAIL_ATTR_NAME = "email";

	/**
	 * These are the twitter informations
	 */
	public static final String OP_TWITTER = "Twitter";
	public static final String TWITTER_SERVER_ADDR = "www.twitter.com";
	public static final String TWITTER_USERINFO_ATTR_NAME = "userinfo_endpoint";
	public static final String TWITTER_USEREMAIL_ATTR_NAME = "email";
	public static final String TWITTER_USERID_ATTR_NAME = "id";

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

	private static HTTPOAuthHandler instance;

	/**
	 * @return the instance
	 */
	public static HTTPOAuthHandler getInstance() {
		return instance;
	}

	/**
	 * This is the default constructor which only calls super("HTTPStatHandler")
	 */
	public HTTPOAuthHandler() {
		super("HTTPOAuthHandler");
		if (instance != null) {
			return;
		}
		try {
			localRootUrl = new URL(Connection.HTTPSSLSCHEME + "://" + XWTools.getLocalHostName() + ":"
					+ Dispatcher.getConfig().getPort(Connection.HTTPSPORT));
		} catch (final MalformedURLException e) {
			XWTools.fatal(e.getMessage());
		}
		instance = this;

		try {
			Operator.FACEBOOK.setAppId(Dispatcher.getConfig().getProperty(XWPropertyDefs.FACEBOOKAPPID));
			Operator.FACEBOOK.setAppKey(Dispatcher.getConfig().getProperty(XWPropertyDefs.FACEBOOKAPPKEY));
			Operator.FACEBOOK.setOAuthUri(new URI(Dispatcher.getConfig().getProperty(XWPropertyDefs.FACEBOOKAUTHURI)));
			Operator.FACEBOOK.setScope(Dispatcher.getConfig().getProperty(XWPropertyDefs.FACEBOOKSCOPE));
			Operator.FACEBOOK.setServerAddress(FACEBOOK_SERVER_ADDR);
			Operator.FACEBOOK
					.setCallbackUrl(new URL(Dispatcher.getConfig().getProperty(XWPropertyDefs.FACEBOOKCALLBACKURL)));
			Operator.FACEBOOK.setApi(FacebookApi.instance());
			Operator.FACEBOOK.setUserInfoEndpointUri(new URI(
					XWTools.jsonValueFromURL(Dispatcher.getConfig().getProperty(XWPropertyDefs.FACEBOOK_DISCOVERYURI),
							FACEBOOK_USERINFO_ATTR_NAME)));
			Operator.FACEBOOK.setEmailAttrName(FACEBOOK_USEREMAIL_ATTR_NAME);
			Operator.FACEBOOK.setIdAttrName(FACEBOOK_USERID_ATTR_NAME);
			Operator.FACEBOOK.setServiceBuilder(new ServiceBuilder().apiKey(Operator.FACEBOOK.getAppId())
					.apiSecret(Operator.FACEBOOK.getAppKey()).callback(Operator.FACEBOOK.getCallbackUrl().toString())
					.scope(Operator.FACEBOOK.getScope()));
		} catch (final Exception e) {
			logger.warn(Operator.FACEBOOK.toString() + " : " + e.toString());
		}
		try {
			Operator.GOOGLE.setAppId(Dispatcher.getConfig().getProperty(XWPropertyDefs.GOOGLEAPPID));
			Operator.GOOGLE.setAppKey(Dispatcher.getConfig().getProperty(XWPropertyDefs.GOOGLEAPPKEY));
			Operator.GOOGLE.setOAuthUri(new URI(Dispatcher.getConfig().getProperty(XWPropertyDefs.GOOGLEAUTHURI)));
			Operator.GOOGLE.setScope(Dispatcher.getConfig().getProperty(XWPropertyDefs.GOOGLESCOPE));
			Operator.GOOGLE.setServerAddress(GOOGLE_SERVER_ADDR);
			Operator.GOOGLE
					.setCallbackUrl(new URL(Dispatcher.getConfig().getProperty(XWPropertyDefs.GOOGLECALLBACKURL)));
			Operator.GOOGLE.setApi(GoogleApi20.instance());
			Operator.GOOGLE.setUserInfoEndpointUri(new URI(
					XWTools.jsonValueFromURL(Dispatcher.getConfig().getProperty(XWPropertyDefs.GOOGLE_DISCOVERYURI),
							GOOGLE_USERINFO_ENDPOINT_ATTR_NAME)));
			Operator.GOOGLE.setEmailAttrName(GOOGLE_USEREMAIL_ATTR_NAME);
			Operator.GOOGLE.setIdAttrName(GOOGLE_USERID_ATTR_NAME);
			Operator.GOOGLE.setServiceBuilder(
					new ServiceBuilder().apiKey(Operator.GOOGLE.getAppId()).apiSecret(Operator.GOOGLE.getAppKey())
							.callback(Operator.GOOGLE.getCallbackUrl().toString()).scope(Operator.GOOGLE.getScope()));
		} catch (final Exception e) {
			logger.warn(Operator.GOOGLE.toString() + " : " + e.toString());
		}

		try {
			Operator.TWITTER.setAppId(Dispatcher.getConfig().getProperty(XWPropertyDefs.TWITTERAPPID));
			Operator.TWITTER.setAppKey(Dispatcher.getConfig().getProperty(XWPropertyDefs.TWITTERAPPKEY));
			Operator.TWITTER.setOAuthUri(new URI(Dispatcher.getConfig().getProperty(XWPropertyDefs.TWITTERAUTHURI)));
			Operator.TWITTER.setScope(Dispatcher.getConfig().getProperty(XWPropertyDefs.TWITTERSCOPE));
			Operator.TWITTER.setServerAddress(TWITTER_SERVER_ADDR);
			Operator.TWITTER
					.setCallbackUrl(new URL(Dispatcher.getConfig().getProperty(XWPropertyDefs.TWITTERCALLBACKURL)));
			Operator.TWITTER.setApi(TwitterApi.instance());
			Operator.TWITTER.setUserInfoEndpointUri(new URI(
					XWTools.jsonValueFromURL(Dispatcher.getConfig().getProperty(XWPropertyDefs.TWITTER_DISCOVERYURI),
							TWITTER_USERINFO_ATTR_NAME)));
			Operator.TWITTER.setEmailAttrName(TWITTER_USEREMAIL_ATTR_NAME);
			Operator.TWITTER.setIdAttrName(TWITTER_USERID_ATTR_NAME);
			Operator.TWITTER.setServiceBuilder(
					new ServiceBuilder().apiKey(Operator.TWITTER.getAppId()).apiSecret(Operator.TWITTER.getAppKey())
							.callback(Operator.TWITTER.getCallbackUrl().toString()).scope(Operator.TWITTER.getScope()));
		} catch (final Exception e) {
			logger.warn(Operator.TWITTER.toString() + " : " + e.toString());
		}

		try {
			Operator.YAHOO.setAppId(Dispatcher.getConfig().getProperty(XWPropertyDefs.YAHOOAPPID));
			Operator.YAHOO.setAppKey(Dispatcher.getConfig().getProperty(XWPropertyDefs.YAHOOAPPKEY));
			Operator.YAHOO.setOAuthUri(new URI(Dispatcher.getConfig().getProperty(XWPropertyDefs.YAHOOAUTHURI)));
			Operator.YAHOO.setScope(Dispatcher.getConfig().getProperty(XWPropertyDefs.YAHOOSCOPE));
			Operator.YAHOO.setServerAddress(YAHOO_SERVER_ADDR);
			Operator.YAHOO.setCallbackUrl(new URL(Dispatcher.getConfig().getProperty(XWPropertyDefs.YAHOOCALLBACKURL)));
			Operator.YAHOO.setApi(YahooApi.instance());
			Operator.YAHOO.setUserInfoEndpointUri(new URI(XWTools.jsonValueFromURL(
					Dispatcher.getConfig().getProperty(XWPropertyDefs.YAHOO_DISCOVERYURI), YAHOO_USERINFO_ATTR_NAME)));
			Operator.YAHOO.setEmailAttrName(YAHOO_USEREMAIL_ATTR_NAME);
			Operator.YAHOO.setIdAttrName(YAHOO_USERID_ATTR_NAME);
			Operator.YAHOO.setServiceBuilder(
					new ServiceBuilder().apiKey(Operator.YAHOO.getAppId()).apiSecret(Operator.YAHOO.getAppKey())
							.callback(Operator.YAHOO.getCallbackUrl().toString()).scope(Operator.YAHOO.getScope()));
		} catch (final Exception e) {
			logger.warn(Operator.YAHOO.toString() + " : " + e.toString());
		}

		for (final Operator op : Operator.values()) {
			if (op.getServiceBuilder() == null) {
				continue;
			}
			logger.info("" + op + " server   = " + op.getServerAddress());
			logger.info("" + op + " auth uri = " + op.getOAuthUri());
			logger.info("" + op + " userinfo uri = " + op.getUserInfoEndpointUri());
			logger.info("" + op + " scope    = " + op.getScope());
			logger.info("" + op + " app id   = " + op.getAppId());
			logger.info("" + op + " app key  = " + op.getAppKey());
			logger.info("" + op + " service  = " + op.getServiceBuilder());
		}
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
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
			final HttpServletResponse response) throws IOException, ServletException {

		logger.debug("new connection");

		final String path = request.getPathInfo();
		try {
			logger.debug("Handling path info      = " + path);
			logger.debug("Handling target         = " + target);
			logger.debug("Handling request        = " + request.getContentLength() + " " + request.getContentType());
			logger.debug("Handling parameter size = " + request.getParameterMap().size());
			logger.debug("Handling query string   = " + request.getQueryString());
			logger.debug("Handling method         = " + request.getMethod());
			logger.debug("Requested Session ID    = " + request.getRequestedSessionId());
			logger.debug("Auth type               = " + request.getAuthType());
			logger.debug("Remot host              = " + request.getRemoteHost());
			logger.debug("Server name             = " + request.getServerName());

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
				oauthRequest(request, response);
			}

			baseRequest.setHandled(true);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			baseRequest.setHandled(false);
		}
		response.getWriter().flush();
	}

	/**
	 * This handles XMLHTTPRequest
	 */
	private void oauthRequest(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, OAuthException {

		try {
			final String code = request.getParameter(XWPostParams.AUTH_CODE.toString());
			if ((code == null) || (code.length() < 1)) {
				throw new OAuthException("no code found");
			}
			logger.debug("code = " + code);
			// Token accessToken = service.getAccessToken(state);
			final String state = request.getParameter(XWPostParams.AUTH_STATE.toString());
			logger.debug("state = " + state);
			final HttpSession session = request.getSession(false);
			if (session == null) {
				throw new OAuthException("no session found");
			}
			logger.debug("session = " + session.getId());

			checkState(state);
			final OAuthService service = getState(state);
			if (service == null) {
				throw new OAuthException("can't retrieve service for " + state);
			}
			Token accessToken = null;
			if (service.getClass() == OAuth20Service.class) {
				accessToken = ((OAuth20Service) service).getAccessToken(code);
			}

			final Operator op = operatorFromRequest(request);
			logger.debug("userinfo_endpoint " + op.getUserInfoEndpointUri());
			logger.debug("accessTocken " + accessToken + ", " + accessToken.getRawResponse());
			final OAuthRequest oauthRequest = new OAuthRequest(Verb.GET,
					op.getOAuthUri().toString(), service);
			service.signRequest(accessToken, oauthRequest);
			final Response oauthResponse = oauthRequest.send();

			if (oauthResponse.getCode() != 200) {
				throw new OAuthException("OAuth code error : " + oauthResponse.getCode());
			}
			final String json = oauthResponse.getBody();

			final JSONTokener jst = new JSONTokener(json);
			final JSONObject obj = new JSONObject(jst);
			final Authentication auth = getAuthentication(op, session, obj);

			final String url = localRootUrl + "?" + XWPostParams.AUTH_STATE + "=" + state + "&"
					+ XWPostParams.AUTH_EMAIL + "=" + auth.getEmail() + "&" + XWPostParams.AUTH_IDENTITY + "="
					+ auth.getIdentity();
			logger.debug("sendRedirectUrm = " + url);
			response.sendRedirect(url);
			return;
		} catch (final OAuthException e) {

			try {
				final Operator op = operatorFromRequest(request);
				oauthRequestForOPerator(request, op, response);
			} catch (final Exception e2) {
				throw new OAuthException("Unsupported operator : " + e);
			}
		}
	}

	/**
	 * This retrieves the operator from the request
	 * 
	 * @param request
	 *            is the HTTP request
	 * @return the found operator
	 * @throws OAuthException
	 *             is no operator found
	 */
	private Operator operatorFromRequest(final HttpServletRequest request) throws OAuthException {
		try {
			final String operatorName = request.getParameter(XWPostParams.AUTH_OPERATOR.toString());
			final String path = request.getPathInfo();
			final Operator op = operatorName != null ? Operator.valueOf(operatorName)
					: Operator.valueOf(path.substring(1).toUpperCase());
			logger.debug("operator = " + op);
			return op;
		} catch (final Exception e) {
			throw new OAuthException("can't retrieve operator from request : " + e);
		}
	}

	/**
	 * This handles XMLHTTPRequest for Google delegation
	 *
	 * @param request
	 *            is the HTTP request
	 * @param operator
	 *            is the OAuth operator (google, facebook etc.)
	 * @since 9.1.1
	 */
	private void oauthRequestForOPerator(final HttpServletRequest request, final Operator operator,
			final HttpServletResponse response) throws IOException, OAuthException {
		if (operator == null) {
			throw new OAuthException("operator is null");
		}

		if (operator.getServiceBuilder() == null) {
			throw new OAuthException(operator.toString() + " : auth uri is null");
		}

		final String newState = newState(operator);
		logger.debug("newState = " + newState);

		final URI oauthUri = operator.getOAuthUri();
		logger.debug("oathUrl = " + oauthUri);

		final HttpSession session = request.getSession(true);
		logger.debug("session id = " + session.getId());
		session.setAttribute(XWPostParams.AUTH_STATE.toString(), newState);
		session.setAttribute(XWPostParams.AUTH_OPERATOR.toString(), operator.toString());

		final OAuthService service = operator.getServiceBuilder().state(newState).build(operator.getApi());
		storeState(newState, service);

		if (service.getClass() == OAuth10aService.class) {
			final OAuth1RequestToken token = ((OAuth10aService) service).getRequestToken();
			response.sendRedirect(((OAuth10aService) service).getAuthorizationUrl(token));
		}
		if (service.getClass() == OAuth20Service.class) {
			response.sendRedirect(((OAuth20Service) service).getAuthorizationUrl());
		}
	}

	/**
	 * This retrieves authentication from OAuth server response
	 *
	 * @param request
	 *            is the HTTP request
	 * @return the authentication if found; null otherwise
	 */
	private Authentication getAuthentication(final Operator op, final HttpSession session, final JSONObject obj)
			throws OAuthException {

		String email = obj.getString(op.getEmailAttrName());
		String id = obj.getString(op.getIdAttrName());

		if ((email == null) && (session != null)) {
			email = (String) session.getAttribute(XWPostParams.AUTH_EMAIL.toString());
		}
		if ((id == null) && (session != null)) {
			id = (String) session.getAttribute(XWPostParams.AUTH_IDENTITY.toString());
		}

		if ((email == null) || (id == null)) {
			throw new OAuthException("can't retrieve authentication informations");
		}
		final Authentication authentication = new Authentication();
		authentication.setEmail(email);
		authentication.setIdentity(id);
		return authentication;
	}

	/**
	 * This simulates a database that store all states:
	 */
	private final Hashtable<String, OAuthService> stateDb = new Hashtable<>();

	/**
	 * This generates a new state (a random string) and stores it in stateDb
	 *
	 * @return the new generated state
	 */
	private String newState(final Operator op) {
		final MD5 md5 = new MD5(op.getAppId() + System.currentTimeMillis() + Math.random());
		return md5.asHex();
	}

	/**
	 * This tests if state exist in database:
	 *
	 * @param state
	 * @throws OAuthException
	 *             is thrown if provided state does not exist
	 */
	void checkState(final String state) throws OAuthException {
		if (!stateExists(state)) {
			throw new OAuthException("invalid state");
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
	OAuthService getState(final String state) {
		return stateDb.get(state);
	}

	/**
	 * This stores state in database, if not already stored
	 *
	 * @param state
	 *            is the state to store
	 */
	void storeState(final String state, final OAuthService s) {
		stateDb.put(state, s);
	}

	/**
	 * This inserts all known CA certificates to the provided keystore
	 *
	 * @param store
	 *            is the keystore to add certificate to
	 * @return this returns null, if parameter is null; else this returns the
	 *         keystore filled with some new entries
	 * @since 8.0.2
	 */
	public static KeyStore setCACertificateEntries(final KeyStore store) {
		if (store == null) {
			return null;
		}
		try {
			for (final Operator op : Operator.values()) {
				final X509Certificate[] gcerts = XWTools.retrieveCertificates(op.getServerAddress(), false);
				for (int i = 0; i < gcerts.length; i++) {
					final X509Certificate cert = gcerts[i];
					try {
						final String alias = cert.getSubjectDN().toString();
						logger.trace("KeyStore set entry= " + alias + "; KeyStore.size = " + store.size());
						store.setCertificateEntry(alias, cert);
					} catch (final Exception e) {
						logger.error("Caught exception, Can't add new entry to keystore", e);
					}
				}
			}
		} catch (final Exception e) {
			logger.error("Caught exception, Can't add new entry to keystore", e);
		}
		return store;
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
