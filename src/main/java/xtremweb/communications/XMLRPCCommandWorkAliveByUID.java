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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.common.HostInterface;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

/**
 * XMLRPCCommandWorkAliveByUID.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand heartbeat monitor
 */
public class XMLRPCCommandWorkAliveByUID extends XMLRPCCommand {

	private static final Logger logger = Logger.getLogger(XMLRPCCommandWorkAliveByUID.class);

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.WORKALIVEBYUID;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandWorkAliveByUID() throws IOException {
		super(null, IDRPC);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to; its path must contain the UID
	 *            of the job beeing computed
	 */
	public XMLRPCCommandWorkAliveByUID(final URI uri) throws IOException {
		super(uri, IDRPC);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to; its path must contain the UID
	 *            of the job beeing computed
	 * @param u
	 *            defines the host owner
	 * @param h
	 *            defines the computing host
	 */
	public XMLRPCCommandWorkAliveByUID(final URI uri, final UserInterface u, final HostInterface h) throws IOException {
		this(uri);
		setUser(u);
		setHost(h);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	public XMLRPCCommandWorkAliveByUID(final InputStream input) throws IOException, SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
	}

	/**
	 * This is called to decode XML elements
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {
		xmlElementStartCheckUserAndHost(uri, tag, qname, attrs);
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
		return comm.workAliveByUid(this);
	}

	/**
	 * This is for testing only
	 */
	public static void main(final String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			XMLRPCCommandWorkAliveByUID cmd = new XMLRPCCommandWorkAliveByUID(
					new URI(config.getCurrentDispatcher(), new UID()), config.getUser(), config.getHost());
			if (argv.length > 1) {
				cmd = new XMLRPCCommandWorkAliveByUID(new FileInputStream(argv[1]));
			}
			logger.info(cmd.openXmlRootElement() + cmd.toXml() + cmd.closeXmlRootElement());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
