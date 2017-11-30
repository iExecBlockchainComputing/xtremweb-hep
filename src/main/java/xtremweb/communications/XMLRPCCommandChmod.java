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
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;
import xtremweb.security.XWAccessRights;

/**
 * XMLRPCCommandChmod.java
 *
 * This class defines the XMLRPCCommand to change access rights if a given
 * object Created: Nov 11th, 2009
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 5.8.0
 */

public class XMLRPCCommandChmod extends XMLRPCCommand {

	private static final Logger logger = Logger.getLogger(XMLRPCCommandChmod.class);

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.CHMOD;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();
	/**
	 * This is the acces rights column index
	 */
	private static final int ACCESSRIGHTS = URI + 1;

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandChmod() throws IOException {
		super(null, IDRPC, ACCESSRIGHTS);
		setColumnAt(ACCESSRIGHTS, "ACCESSRIGHTS");
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            is the URI of the group to retreive
	 */
	protected XMLRPCCommandChmod(final URI uri) throws IOException {
		super(uri, IDRPC, ACCESSRIGHTS);
		setColumnAt(ACCESSRIGHTS, "ACCESSRIGHTS");
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            is the URI of the group to retreive
	 * @param r
	 *            is the access rights to apply
	 */
	public XMLRPCCommandChmod(final URI uri, final XWAccessRights r) throws IOException {
		this(uri);
		setValueAt(ACCESSRIGHTS, r);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            is the URI of the group to retreive
	 */
	public XMLRPCCommandChmod(final URI uri, final UserInterface u) throws IOException {
		this(uri);
		setUser(u);
	}

	/**
	 * This constructs a new command
	 *
	 * @param r
	 *            is the access rights to apply
	 * @param uri
	 *            is the URI of the group to retreive
	 */
	public XMLRPCCommandChmod(final URI uri, final UserInterface u, final XWAccessRights r) throws IOException {
		this(uri, u);
		setValueAt(ACCESSRIGHTS, r);
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
	public XMLRPCCommandChmod(final InputStream input) throws IOException, SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
	}

	/**
	 * This is called by XML parser This retreives URI, hostUID and activation
	 * params
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);

			if (attribute.compareToIgnoreCase(getColumnLabel(URI)) == 0) {
				try {
					setURI(new URI(value));
				} catch (final URISyntaxException e) {
					logger.error("URI syntax error ; this is not an URI " + value);
				}
			} else if (attribute.compareToIgnoreCase(getColumnLabel(ACCESSRIGHTS)) == 0) {
				try {
					new XWAccessRights(value);
					setValueAt(ACCESSRIGHTS, value);
				} catch (final Exception e) {
					logger.error("not a valid access rights " + value);
					setValueAt(ACCESSRIGHTS, null);
				}
			}
		}
	}

	/**
	 * This sends this command to server and returns answer
	 *
	 * @param comm
	 *            is the communication channel
	 * @return null
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	public XMLable exec(final CommClient comm)
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		comm.chmod(this);
		return null;
	}

	/**
	 * This retrieves access rights modifier
	 *
	 * @return the access rights modifier
	 */
	public XWAccessRights getModifier() {
		return (XWAccessRights) getValueAt(ACCESSRIGHTS);
	}

	/**
	 * This sets access rights modifier
	 *
	 * @since 8.2.0
	 */
	public void setModifier(final XWAccessRights a) {
		setValueAt(ACCESSRIGHTS, a);
	}

	/**
	 * This is for testing only. The first argument must be a valid client
	 * configuration file. Without a second argument, this dumps an
	 * XMLRPCCommandChmod object. If the second argument is an XML file
	 * containing a description of an XMLRPCCommandChmod this creates an object
	 * from XML description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.communications.XMLRPCCommandChmod
	 * aConfigFile [anXMLDescriptionFile]
	 */
	public static void main(final String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			final XMLRPCCommandChmod cmd = new XMLRPCCommandChmod(new URI(config.getCurrentDispatcher(), new UID()),
					config.getUser(), XWAccessRights.DEFAULT);
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
