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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.common.HostInterface;
import xtremweb.common.Logger;
import xtremweb.common.LoggerLevel;
import xtremweb.common.StreamIO;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLEndParseException;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLWriter;
import xtremweb.common.XMLable;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;

/**
 * XMLRPCCommand.java defines XMLRPC command
 * Created: March 22nd, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */
public abstract class XMLRPCCommand extends XMLable {

	private enum XMLTAGS {
		NONE, USER, HOST
	};

	private XMLTAGS currentTag;

	/**
	 * This is this command id
	 */
	private IdRpc idrpc;

	/**
	 * This is the MANDATINGLOGIN column index
	 * This is used to run a command for another user
	 * @since 11.0.0
	 */
	public static final int MANDATINGLOGIN = 0;

	/**
	 * This retrieves this command mandating login
	 * @since 11.0.0
	 */
	public final String getMandatingLogin() {
		return (String) getValueAt(MANDATINGLOGIN);
	}

	/**
	 * This sets this command mandating login
	 * @param dl is this command mandating login
	 * @since 11.0.0
	 */
	public final void setMandatingLogin(final String dl) {
		setValueAt(MANDATINGLOGIN, dl);
	}

	/**
	 * This sets this command mandating login
	 * @since 11.4.0
	 */
	public final boolean isMandated() {
		final String mandatingLogin = getMandatingLogin();
		return ((mandatingLogin != null) && (mandatingLogin.length() > 0));
	}

	/**
	 * This is the URI column index
	 */
	protected static final int URI = 1;
	/**
	 * This retrieves this command URI
	 */
	public final URI getURI() {
		return (URI) getValueAt(URI);
	}

	/**
	 * This sets this command URI
	 *
	 * @param uri
	 *            is this command URI
	 */
	public final void setURI(final URI uri) {
		setValueAt(URI, uri);
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
	 * The remote address is build as: remoteName + "/" + remoteIP + ":" + remotePort
	 * @return a string containing the remote address
	 */
	public String remoteAddresse() {
		return remoteName + "/" + remoteIP + ":" + remotePort;
	}

	/**
	 * This is the caller identity
	 */
	private UserInterface user;
	/**
	 * This is the HostInterface, if any. This is needed by workAlive() and
	 * workRequest()
	 */
	private HostInterface host;
	/**
	 * This is an optional parameter.
	 *
	 * @see XMLRPCCommandSend
	 * @see XMLRPCCommandWorkAlive
	 */
	private XMLable parameter;

	/**
	 * @return the parameter
	 */
	public XMLable getParameter() {
		return parameter;
	}

	/**
	 * @param parameter
	 *            the parameter to set
	 */
	public void setParameter(final XMLable parameter) {
		this.parameter = parameter;
	}

	/**
	 * This set parameter to null
	 *
	 * @since 9.0.0
	 */
	public void resetParameter() {
		parameter = null;
	}

	/**
	 * This clears this object
	 *
	 * @since 5.8.0
	 */
	@Override
	public void clear() {
		super.clear();
		user = null;
		host = null;
		resetParameter();
	}

	/**
	 * This calls XMLRPCCommand(uri, cmdName, URI)
	 *
	 * @see #XMLRPCCommand(URI uri, String, int)
	 */
	protected XMLRPCCommand(final URI uri, final String cmdName) throws IOException {
		this(uri, cmdName, URI);
	}

	/**
	 * This constructs a new object accordingly to the RPCcommand provided. This
	 * sets LAST_ATTRIBUTE = l and MAX_ATTRIBUTE = LAST_ATTRIBUTE + 1. This sets
	 * columns and value arrays. This class has no attribute (LAST_ATTRIBUTE =
	 * -1; hence MAX_ATTRIBUTE = 0)
	 *
	 * @param uri
	 *            is this command URI
	 * @param cmdName
	 *            is this command name (IdRpc string representation)
	 * @param l
	 *            is this command param length
	 * @exception IOException
	 *                is throws if cmdName is not a valid RPC command
	 * @see xtremweb.communications.IdRpc
	 */
	protected XMLRPCCommand(final URI uri, final String cmdName, final int l) throws IOException {
		super(IdRpc.valueOf(cmdName).toString().toLowerCase(), -1);

		idrpc = IdRpc.valueOf(cmdName);
		user = null;
		host = null;
		setParameter(null);

		setAttributeLength(l);
		setColumns();
		setColumnAt(URI, "URI");
		setURI(uri);
		setColumnAt(MANDATINGLOGIN, "MANDATINGLOGIN");
		setMandatingLogin(null);
		currentTag = XMLTAGS.NONE;
	}

	/**
	 * This calls XMLRPCCommand(uri, cmd, URI);
	 *
	 * @see #XMLRPCCommand(URI, String, int)
	 */
	protected XMLRPCCommand(final URI uri, final IdRpc cmd) throws IOException {
		this(uri, cmd, URI);
	}

	/**
	 * This constructor a new object accordingly to the IdRpc command provided
	 *
	 * @see #XMLRPCCommand(URI, String, int)
	 */
	protected XMLRPCCommand(final URI uri, final IdRpc cmd, final int l) throws IOException {
		this(uri, cmd.toString(), l);
	}

	/**
	 * This constructor sets user attribute This class has no attribute
	 * (LAST_ATTRIBUTE = -1; hence MAX_ATTRIBUTE = 0)
	 *
	 * @see #XMLRPCCommand(URI, String, int)
	 */
	protected XMLRPCCommand(final URI uri, final String cmdName, final UserInterface u, final int l)
			throws IOException {
		this(uri, cmdName, l);
		user = u;
	}

	/**
	 * This constructor sets user attribute
	 *
	 * @see #XMLRPCCommand(URI, String, int)
	 */
	protected XMLRPCCommand(final URI uri, final IdRpc cmd, final UserInterface u, final int l) throws IOException {
		this(uri, cmd, l);
		user = u;
	}

	/**
	 * This constructor sets host attribute This class has no attribute
	 * (LAST_ATTRIBUTE = -1; hence MAX_ATTRIBUTE = 0)
	 *
	 * @see #XMLRPCCommand(URI, String, int)
	 * @exception IOException
	 *                is thrown if cmdName is not a valid RPC command name
	 */
	protected XMLRPCCommand(final URI uri, final String cmdName, final HostInterface h, final int l)
			throws IOException {
		this(uri, cmdName, l);
		host = h;
	}

	/**
	 * @see #XMLRPCCommand(URI, String, HostInterface, int)
	 */
	protected XMLRPCCommand(final URI uri, final IdRpc cmd, final HostInterface h, final int l) throws IOException {
		this(uri, cmd.toString(), h, l);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 */
	protected XMLRPCCommand(final BufferedInputStream input) throws IOException, SAXException {
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This sends this command to server and returns answer
	 *
	 * @param comm
	 *            is the communication channel
	 * @return always null
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @exception RemoteException
	 *                is thrown on comm error
	 */
	public abstract XMLable exec(CommClient comm)
			throws IOException, ClassNotFoundException, SAXException, InvalidKeyException, AccessControlException;

	/**
	 * This converts id rpc to string. This catches IOException so that this may
	 * be used to create THISTAG
	 */
	public static String idRpc(final IdRpc id) {
		return id.toString();
	}

	/**
	 * This retreives this command parameter
	 *
	 * @return this command parameter
	 */
	public XMLable getParam() {
		return getParameter();
	}

	/**
	 * This sets the user for this command
	 *
	 * @param u
	 *            defines this command user
	 */
	public void setUser(final UserInterface u) {
		user = u;
	}

	/**
	 * This reterives the user for this command
	 *
	 * @return the user executing this command
	 */
	public UserInterface getUser() {
		return user;
	}

	/**
	 * This sets the host for this command.
	 *
	 * @param h
	 *            defines this command host
	 */
	public void setHost(final HostInterface h) {
		host = h;
	}

	/**
	 * This reterives the host for this command
	 *
	 * @return the host executing this command
	 */
	public HostInterface getHost() {
		return host;
	}

	/**
	 * This retrieves the RPC code
	 *
	 * @return an integer representing the RPC code
	 * @see xtremweb.communications.IdRpc
	 */
	public IdRpc getIdRpc() throws IOException {
		return idrpc;
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @throws IOException
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		final StringBuilder ret = new StringBuilder(getOpenTag(getURI(), getMandatingLogin()));

		if (user != null) {
			ret.append(user.toXml());
		}
		if (host != null) {
			ret.append(host.toXml());
		}
		if (getParameter() != null) {
			ret.append(getParameter().toXml());
		}

		ret.append(getCloseTag());

		return ret.toString();
	}

	/**
	 * This is called by XML parser
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}
		final Logger logger = getLogger();

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);

			logger.finest("XMLRPCCommand  ##  attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value
					+ "\"");

			if (attribute.compareToIgnoreCase(getColumnLabel(MANDATINGLOGIN)) == 0) {
				logger.finest("XMLRPCCommand  ##  creating mandating login from " + value);
				try {
					setValueAt(MANDATINGLOGIN, value);
				} catch (final Exception e) {
					logger.exception(e);
				}
				continue;
			}
			if (attribute.compareToIgnoreCase(getColumnLabel(URI)) == 0) {
				logger.finest("XMLRPCCommand  ##  creating uri from " + value);
				try {
					setValueAt(URI, new URI(value));
				} catch (final Exception e) {
					logger.exception(e);
				}
				continue;
			}
		}
	}

	/**
	 * This calls xmlElementStartCheckUser(uri, tag, qname, attrs)
	 *
	 * @see #xmlElementStartCheckUser(String, String, String, Attributes)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {
		xmlElementStartCheckUser(uri, tag, qname, attrs);
	}

	/**
	 * This is called by the XML parser. <br />
	 * This must set user once only to allow user interface usage e.g. the
	 * parameter of XMLRPCCommandSendUser is the interface of the new user (so
	 * XMLRPCCommandSendUser embeds two UserInterface) This instantiates user
	 * and resets its default values so that xmlElementStop() has a chance to
	 * set attributes by calling Type#getValue(String) and Type#setValue(String,
	 * Object)
	 *
	 * @see xtremweb.common.Type#setValue(String, Object)
	 * @see #xmlElementStop(String, String, String)
	 * @see #user
	 * @see xtremweb.common.UserInterface#UserInterface()
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	public void xmlElementStartCheckUser(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			super.xmlElementStart(uri, tag, qname, attrs);
			return;
		} catch (final SAXException se) {
		}

		getLogger().finest("XMLRPCCommand#xmlElementStartCheckUser(" + uri + ", " + tag + ", " + qname + ")  "
				+ attrs.getLength());
		if (qname.compareToIgnoreCase(getXMLTag()) == 0) {
			fromXml(attrs);
			return;
		}

		if (qname.compareToIgnoreCase(UserInterface.THISTAG) == 0) {
			if (user == null) {
				currentTag = XMLTAGS.USER;
				user = new UserInterface(attrs);
				getLogger().finest("XMLRPCCommand#xmlElementStartCheckUser " + user.toXml());
			} else {
				throw new SAXException(
						"XMLRPCCommand not a " + getXMLTag() + " command (" + qname + ") (user already set)");
			}
		}
		if (user != null) {
			user.xmlElementStart(uri, tag, qname, attrs);
		} else {
			throw new SAXException("XMLRPCCommand not a " + getXMLTag() + " command (" + qname + ")");
		}
	}

	/**
	 * This is called to decode XML elements.<br />
	 * This is used by work alive and work request commands only.<br />
	 * This first calls xmlElementStartCheckUser(uri, tag, qname, attrs); then
	 * this checks host XML description.<br />
	 * This instantiates host and resets its default values so that
	 * xmlElementStop() has a chance to set attributes by calling
	 * Type#getValue(String) and Type#setValue(String, Object)
	 *
	 * @see xtremweb.common.Type#setValue(String, Object)
	 * @see #xmlElementStop(String, String, String)
	 * @see #xmlElementStartCheckUser(String, String, String, Attributes)
	 * @see xtremweb.common.HostInterface#HostInterface()
	 * @see #host
	 */
	public void xmlElementStartCheckUserAndHost(final String uri, final String tag, final String qname,
			final Attributes attrs) throws SAXException {

		try {
			this.xmlElementStartCheckUser(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		getLogger().finest("XMLRPCCommand#xmlElementStartCheckUserAndHost(" + uri + ", " + tag + ", " + qname + ")");

		if (qname.compareToIgnoreCase(HostInterface.THISTAG) == 0) {
			if (host == null) {
				currentTag = XMLTAGS.HOST;
				host = new HostInterface(attrs);

				getLogger().finest("XMLRPCCommand#xmlElementStartCheckUserAndHost " + host.toXml());
			} else {
				throw new SAXException(
						"XMLRPCCommand not a " + getXMLTag() + " command (" + qname + ") (host already set)");
			}
		}
		if (host != null) {
			host.xmlElementStart(uri, tag, qname, attrs);
		} else {
			throw new SAXException("XMLRPCCommand  not a " + getXMLTag() + " command (" + qname + ")");
		}
	}

	/**
	 * This is called on XML element close tag and sets the XML element values.
	 * <br />
	 * This sets user and host attributes by calling Type#getValue(String) and
	 * Type#setValue(String, Object)
	 *
	 * @see xtremweb.common.Type#setValue(String, Object)
	 * @see #user
	 * @see #host
	 * @see XMLable#characters(char[], int, int)
	 * @see XMLReader#read(InputStream)
	 * @exception SAXException
	 *                on XML error, or SAXException(XMLEndParseException()) to
	 *                force stop parsing
	 * @since 9.0.0
	 */
	@Override
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		super.xmlElementStop(uri, tag, qname);

		getLogger()
				.finest("XMLRPCCommand#xmlElementStop " + uri + ", " + tag + ", " + qname + " = " + getCurrentValue());

		switch (currentTag) {
		case USER:
			if (user != null) {
				try {
					getLogger().finest("XMLRPCCommand#xmlElementStop set user value " + uri + ", " + tag + ", " + qname
							+ " = " + getCurrentValue());
					user.setValue(qname, getCurrentValue());
					resetCurrentValue();
				} catch (final IllegalArgumentException e) {
				}
			}
			break;
		case HOST:
			if (host != null) {
				try {
					getLogger().finest("XMLRPCCommand#xmlElementStop set host value " + uri + ", " + tag + ", " + qname
							+ " = " + getCurrentValue());
					host.setValue(qname, getCurrentValue());
					resetCurrentValue();
				} catch (final IllegalArgumentException e) {
				}
			}
			break;
		case NONE:
			getLogger().finest("currentTag = NONE ???");
			break;
		}

		if ((qname.compareToIgnoreCase(UserInterface.THISTAG) == 0)
				|| (qname.compareToIgnoreCase(HostInterface.THISTAG) == 0)) {
			currentTag = XMLTAGS.NONE;
		}
	}

	/**
	 * This constructs a new XMLRPCCommand object
	 *
	 * @param io
	 *            is stream handler to read XML representation from
	 */
	public static XMLRPCCommand newCommand(final StreamIO io) throws IOException {
		return newCommand(io.input());
	}

	/**
	 * This constructs a new XMLRPCCommand object.
	 *
	 * @param xmlString
	 *            is a String containing a full XML representation
	 */
	public static XMLRPCCommand newCommand(final String xmlString) throws IOException {
		return newCommand(StreamIO.stream(xmlString));
	}

	/**
	 * This constructs a new XMLRPCCommand object. This first checks the opening
	 * tag and then instanciate the right object accordingly to the opening tag.
	 *
	 * @param in
	 *            is the input stream to read command from
	 * @exception IOException
	 *                is thrown on I/O error or if provided paremeter is null
	 */
	public static XMLRPCCommand newCommand(final InputStream in) throws IOException {

		if (in == null) {
			throw new IOException("InputStream is null");
		}
		final BufferedInputStream input = new BufferedInputStream(in);
		XMLRPCCommand ret = null;
		final Logger logger = new Logger(XMLRPCCommand.class);

		try {
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandVersion(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command version");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandPing(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command ping");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandDisconnect(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command disconnect");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetUserByLogin(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getuserby login");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGet(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command get");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetWorkByExternalId(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a XMLRPCCommandGetWorkByExternalId");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetAppByName(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a XMLRPCCommandGetAppByName");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetTask(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command gettask");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetWorks(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetCategories(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getcategories");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetCategoryById(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getvategorybyid");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendCategory(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command sendcategory");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetHubAddr(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command gethubaddr");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return XMLRPCCommandSend.newCommandSend(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command send");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetTasks(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command gettasks");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandDownloadData(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command download data");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetApps(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getapps");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetDatas(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getdatas");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetGroupWorks(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getgroupworks");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetGroups(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getgroups");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetHosts(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command gethosts");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetSessionWorks(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getsessionworks");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetSessions(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getsessions");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandActivateHost(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command activatehost");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandBroadcastWork(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command broadcastwork");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetUserGroups(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getusergroups");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetUsers(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command getusers");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandRemove(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command remove");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandUploadData(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command uploaddata");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandChmod(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command chmod");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandWorkRequest(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command workrequest");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandWorkAlive(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command workalive");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandWorkAliveByUID(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}
		logger.finest("not a command workalivebyuid");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandGetTraces(input);
		} catch (final SAXException e) {
			if (e instanceof XMLEndParseException) {
				return ret;
			}
		} catch (final InvalidKeyException e) {
			logger.exception(e);
		}

		throw new IOException("Unknown XMLRPCCommand");
	}

	/*
	 * This calls toString(false)
	 */
	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * This retrieves String representation
	 *
	 * @param csv
	 *            tells whether CSV format is expected
	 * @return this object String representation
	 * @see xtremweb.common.Table#toString(boolean)
	 */
	@Override
	public String toString(final boolean csv) {
		String ret = new String();

		if (user != null) {
			ret += user.toString(csv, false);
		}
		if (host != null) {
			ret += host.toString(csv, false);
		}
		if (getParameter() != null) {
			ret += getParameter().toString(csv, false);
		}
		return ret;
	}

	/**
	 * This is for testing only. This creates a XMLRPCCommand from given XML
	 * String representation of any XMLRPCCommand descendant argv[0] must
	 * contain a config file name argv[1] must contain an XML representation.
	 * The object is finally dumped.
	 */
	public static void main(final String[] argv) {
		final Logger logger = new Logger();
		try (final DataInputStream dis = new DataInputStream(new FileInputStream(argv[1]));
				final StreamIO streamIO = new StreamIO(null, new DataInputStream(dis))) {
			final XMLRPCCommand cmd = XMLRPCCommand.newCommand(streamIO);
			cmd.getLogger().info(cmd.openXmlRootElement() + cmd.toXml() + cmd.closeXmlRootElement());
		} catch (final Exception e) {
			logger.exception("Usage : java -cp " + XWTools.JARFILENAME
					+ " xtremweb.communications.XMLRPCCommand <aConfigFile> <anXMLDescriptionFile>", e);
		}
	}

	/**
	 * This is for testing only.
	 *
	 * @since 8.2.0
	 */
	public void test(final String[] argv) {
		try (final XMLReader reader = new XMLReader(this);
				final DataOutputStream dos = new DataOutputStream(System.out)) {
			LoggerLevel logLevel = LoggerLevel.INFO;
			try {
				logLevel = LoggerLevel.valueOf(System.getProperty(XWPropertyDefs.LOGGERLEVEL.toString()));
			} catch (final Exception e) {
			}
			if (argv.length > 1) {
				resetParameter();

				reader.read(new FileInputStream(argv[1]));
			}
			setLoggerLevel(logLevel);
			setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(dos);
			writer.write(this);
		} catch (final Exception e) {
			e.printStackTrace();
			final Logger logger = new Logger();
			logger.fatal("Usage : java -cp " + XWTools.JARFILENAME + " a_class <aConfigFile> [anXMLDescriptionFile]");
		}
	}
}
