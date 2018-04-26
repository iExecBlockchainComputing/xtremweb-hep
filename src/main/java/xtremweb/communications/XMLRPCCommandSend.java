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
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.common.AppInterface;
import xtremweb.common.DataInterface;
import xtremweb.common.CategoryInterface;
import xtremweb.common.GroupInterface;
import xtremweb.common.Logger;
import xtremweb.common.SessionInterface;
import xtremweb.common.Table;
import xtremweb.common.UserGroupInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.common.XMLEndParseException;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWTools;

/**
 * XMLRPCCommandSend.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand to send application definition
 */
public abstract class XMLRPCCommandSend extends XMLRPCCommand {

	public static final XMLRPCCommandSend newCommand(final URI uri, final Table obj) throws IOException{
		if(obj instanceof AppInterface) {
			return new XMLRPCCommandSendApp(uri, obj);
		}
		if(obj instanceof DataInterface) {
			return new XMLRPCCommandSendData(uri, obj);
		}
		if(obj instanceof GroupInterface) {
			return new XMLRPCCommandSendGroup(uri, obj);
		}
		if(obj instanceof SessionInterface) {
			return new XMLRPCCommandSendSession(uri, obj);
		}
		if(obj instanceof UserInterface) {
			return new XMLRPCCommandSendUser(uri, obj);
		}
		if(obj instanceof UserGroupInterface) {
			return new XMLRPCCommandSendUserGroup(uri, obj);
		}
		if(obj instanceof WorkInterface) {
			return new XMLRPCCommandSendWork(uri, obj);
		}
		throw new IOException("unkown ovject type");
	}

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandSend(final URI uri, final IdRpc cmd) throws IOException {
		super(uri, cmd);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param p
	 *            defines the object to send
	 */
	public XMLRPCCommandSend(final URI uri, final IdRpc cmd, final Table p) throws IOException {
		super(uri, cmd);
		setParameter(p);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param u
	 *            defines the user who executes this command
	 * @param p
	 *            defines the object to send
	 */
	public XMLRPCCommandSend(final URI uri, final UserInterface u, final IdRpc cmd, final Table p) throws IOException {

		this(uri, cmd, p);
		setUser(u);
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
	@Override
	public XMLable exec(final CommClient comm)
			throws IOException, ClassNotFoundException, SAXException, InvalidKeyException, AccessControlException {
		comm.send(this);
		return null;
	}

	/**
	 * This is called on XML element open tag.
	 *
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	@Override
	final public void xmlElementStart(final String uri, final String thetag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			xmlElementStartCheckUserAndHost(uri, thetag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		getLogger().finest("XMLRPCCommandSend#xmlElementStartCheckUser(" + uri + ", " + thetag + ", " + qname + ")  "
				+ attrs.getLength());

		try {
			final Table param = (Table) getParameter();
			if (param == null) {
				final Table itf = Table.newInterface(uri, thetag, qname, attrs);
				setParameter(itf);
			} else {
				param.xmlElementStart(uri, thetag, qname, attrs);
			}
		} catch (final ClassNotFoundException e) {
			throw new SAXException("XMLRPCCommandSend not a send command : " + e.getMessage());
		}
	}

	/**
	 * This is called on XML element close tag and sets the XML element value
	 *
	 * @see XMLable#characters(char[], int, int)
	 * @see XMLReader#read(InputStream)
	 * @exception SAXException
	 *                on XML error, or SAXException(XMLEndParseException()) to
	 *                force stop parsing
	 * @since 9.0.0
	 */
	@Override
	final public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		super.xmlElementStop(uri, tag, qname);

		getLogger().finest(
				"XMLRPCCommandSend#xmlElementStop " + uri + ", " + tag + ", " + qname + " = " + getCurrentValue());

		try {
			final Table param = (Table) getParameter();
			if (param != null) {
				param.setValue(qname, getCurrentValue());
			}
		} catch (final IllegalArgumentException e) {
		} finally {
			resetCurrentValue();
		}
	}

	/**
	 * This constructs a new XMLRPCCommand object. This first checks the opening
	 * tag and then instanciate the right object accordingly to the opening tag.
	 *
	 * @param in
	 *            is the input stream to read command from
	 * @exception IOException
	 *                is thrown on I/O error or if provided parameter is null
	 * @throws SAXException if no XMLRPCCommandSend available from input
	 * @throws InvalidKeyException 
	 * @since 11.5.0
	 */
	public static XMLRPCCommand newCommandSend(final InputStream in) throws IOException, SAXException, InvalidKeyException {

		if (in == null) {
			throw new IOException("InputStream is null");
		}
		final BufferedInputStream input = new BufferedInputStream(in);
		XMLRPCCommand ret = null;
		final Logger logger = new Logger(XMLRPCCommand.class);

		try {
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendApp(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command sendapp");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendData(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command senddata");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendGroup(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command sendgroup");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendSession(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command sendsession");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendUser(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command senduser");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendUserGroup(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command sendusergroup");
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return new XMLRPCCommandSendWork(input);
		} catch (final XMLEndParseException e) {
			return ret;
		} catch (final SAXException e) {
		}
		logger.finest("not a command sendwork");

		throw new SAXException("Unknown XMLRPCCommandSend");
	}

}
