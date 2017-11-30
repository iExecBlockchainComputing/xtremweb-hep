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

import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

/**
 * XMLRPCCommandActivateHost.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand to activate host
 */
public final class XMLRPCCommandActivateHost extends XMLRPCCommand {

	private static final Logger logger = Logger.getLogger(XMLRPCCommandActivateHost.class);

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.ACTIVATEHOST;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();
	/**
	 * This is the activation column index
	 */
	private static final int ACTIVATION = URI + 1;

	/**
	 * This constructs a new command
	 */
	protected XMLRPCCommandActivateHost() throws IOException {
		super(null, IDRPC, ACTIVATION);
		setColumnAt(ACTIVATION, "ACTIVATION");
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            to connect to; it must contains the UID of the host to
	 *            (de)activate
	 */
	protected XMLRPCCommandActivateHost(final URI uri) throws IOException {
		super(uri, IDRPC, ACTIVATION);
		setColumnAt(ACTIVATION, "ACTIVATION");
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            to connect to; it must contains the UID of the host to
	 *            (de)activate
	 * @param flag
	 *            is the activation flag
	 */
	protected XMLRPCCommandActivateHost(final URI uri, final boolean flag) throws IOException {
		this(uri);
		setActivation(flag);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            to connect to; it must contains the UID of the host to
	 *            (de)activate
	 * @param u
	 *            defines the user who executes this command
	 * @param flag
	 *            is the activation flag
	 */
	public XMLRPCCommandActivateHost(final URI uri, final UserInterface u, final boolean flag) throws IOException {
		this(uri, flag);
		setUser(u);
	}

	/**
	 * This constructs a new command setting the actiovation flag to false
	 *
	 * @since 8.2.2
	 * @param uri
	 *            to connect to; it must contains the UID of the host to
	 *            (de)activate
	 * @param u
	 *            defines the user who executes this command
	 */
	public XMLRPCCommandActivateHost(final URI uri, final UserInterface u) throws IOException {
		this(uri);
		setActivation(false);
		setUser(u);
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
	public XMLRPCCommandActivateHost(final InputStream input) throws IOException, SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
	}

	/**
	 * This retrieves the host UID
	 *
	 * @return the UID of the host to activate
	 */
	public UID getHostUID() {
		return getURI().getUID();
	}

	/**
	 * This retrieves this command activation flag
	 *
	 * @return the activation flag
	 */
	public boolean getActivation() {
		return ((Boolean) getValueAt(ACTIVATION)).booleanValue();
	}

	/**
	 * This sets this command activation flag
	 */
	public void setActivation(final boolean b) {
		setActivation(new Boolean(b));
	}

	/**
	 * This sets this command activation flag
	 *
	 * @since 8.2.0
	 */
	public void setActivation(final Boolean b) {
		setValueAt(ACTIVATION, b);
	}

	/**
	 * This is called by XML parser This retrieves URI, hostUID and activation
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
				} catch (final Exception e) {
					logger.error("not a valid URI " + value);
					setURI(null);
				}
			} else if (attribute.compareToIgnoreCase(getColumnLabel(ACTIVATION)) == 0) {
				setActivation(Boolean.getBoolean(value));
			}
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
	 * @since 9.0.0
	 */
	@Override
	public XMLable exec(final CommClient comm)
			throws IOException, ClassNotFoundException, SAXException, InvalidKeyException, AccessControlException {
		comm.activateHost(this);
		return null;
	}

	/**
	 * This is for testing only. The first argument must be a valid client
	 * configuration file. Without a second argument, this dumps an
	 * XMLRPCCommandActivateHost object. If the second argument is an XML file
	 * containing a description of an XMLRPCCommandActivateHost this creates an
	 * object from XML description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar
	 * xtremweb.communications.XMLRPCCommandActivateHost aConfigFile
	 * [anXMLDescriptionFile]
	 */
	public static void main(final String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			final XMLRPCCommandActivateHost cmd = new XMLRPCCommandActivateHost(
					new URI(config.getCurrentDispatcher(), new UID()), config.getUser(), false);
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
