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

import org.xml.sax.SAXException;
import xtremweb.common.*;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

/**
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 13.0.0
 */

/**
 * This class defines the XMLRPCCommand to send application definition
 */
public class XMLRPCCommandSendCategory extends XMLRPCCommandSend  {

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.SENDCATEGORY;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandSendCategory() throws IOException {
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
	public XMLRPCCommandSendCategory(final URI uri, final Table p) throws IOException {
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
	public XMLRPCCommandSendCategory(final URI uri, final UserInterface u, final Table p) throws IOException {

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
	 * @see XMLReader#read(InputStream)
	 */
	public XMLRPCCommandSendCategory(final InputStream input) throws IOException, SAXException, InvalidKeyException {
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
			final XMLRPCCommandSend cmd = new XMLRPCCommandSendCategory(new URI(config.getCurrentDispatcher(), new UID()),
					config.getUser());
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
