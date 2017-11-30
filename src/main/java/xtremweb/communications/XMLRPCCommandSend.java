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

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.common.Table;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

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
public class XMLRPCCommandSend extends XMLRPCCommand {

	private static final Logger logger = Logger.getLogger(XMLRPCCommandSend.class);

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.SEND;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandSend() throws IOException {
		super(null, IDRPC);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param p
	 *            defines the object to send
	 */
	public XMLRPCCommandSend(final URI uri, final Table p) throws IOException {
		super(uri, IDRPC);
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
	public XMLRPCCommandSend(final URI uri, final UserInterface u, final Table p) throws IOException {

		this(uri, p);
		setUser(u);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws InvalidKeyException
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	public XMLRPCCommandSend(final InputStream input) throws IOException, SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
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
	public void xmlElementStart(final String uri, final String thetag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			xmlElementStartCheckUserAndHost(uri, thetag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		logger.trace("XMLRPCCommandSend#xmlElementStartCheckUser(" + uri + ", " + thetag + ", " + qname + ")  "
				+ attrs.getLength());

		try {
			final Table param = (Table) getParameter();
			if (param == null) {
				final Table itf = Table.newInterface(uri, thetag, qname, attrs);
				itf.setCurrentVersion(getCurrentVersion());
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

		logger.trace(
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
	 * This is for testing only. The first argument must be a valid client
	 * configuration file. Without a second argument, this dumps an
	 * XMLRPCCommandSend object. If the second argument is an XML file
	 * containing a description of an XMLRPCCommandSend this creates an object
	 * from XML description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.communications.XMLRPCCommandSend
	 * aConfigFile [anXMLDescriptionFile]
	 */
	public static void main(final String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			final XMLRPCCommandSend cmd = new XMLRPCCommandSend(new URI(config.getCurrentDispatcher(), new UID()),
					config.getUser());
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
