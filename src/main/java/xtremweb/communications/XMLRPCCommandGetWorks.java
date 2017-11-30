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
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.common.StatusEnum;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

/**
 * XMLRPCCommandGetWorks.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand to retrieve works UID
 *
 * Since 8.2.0, we can provide a status so that we can retrieve running work,
 * error works etc.
 */
public class XMLRPCCommandGetWorks extends XMLRPCCommand {

	private static final Logger logger = Logger.getLogger(XMLRPCCommandGetWorks.class);

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.GETWORKS;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This is the status column index
	 *
	 * @since 8.2.0
	 */
	private static final int STATUS = URI + 1;
	/**
	 * This is the status column label
	 *
	 * @since 8.2.0
	 */
	private static final String STATUS_LABEL = "STATUS";

	/**
	 * This constructs a new command
	 */
	protected XMLRPCCommandGetWorks() throws IOException {
		super(null, IDRPC, STATUS);
		setColumnAt(STATUS, STATUS_LABEL);
	}

	/**
	 * This constructs a new command to retrieve works for the given user
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param u
	 *            define the user who executes this command
	 */
	public XMLRPCCommandGetWorks(final URI uri, final UserInterface u) throws IOException {
		super(uri, IDRPC, STATUS);
		setColumnAt(STATUS, STATUS_LABEL);
		setUser(u);
	}

	/**
	 * This constructs a new command to retrieve works with the given status
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param s
	 *            is the work status
	 */
	protected XMLRPCCommandGetWorks(final URI uri, final StatusEnum s) throws IOException {
		this(uri, (UserInterface) null);
		setStatus(s);
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
	public XMLRPCCommandGetWorks(final InputStream input) throws IOException, SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
	}

	/**
	 * This retrieves status flag
	 *
	 * @return the status flag
	 * @since 8.2.0
	 */
	public StatusEnum getStatus() {
		return (StatusEnum) getValueAt(STATUS);
	}

	/**
	 * This sets status flag
	 *
	 * @since 8.2.0
	 */
	public void setStatus(final StatusEnum s) {
		setValueAt(STATUS, s);
	}

	/**
	 * This sends this command to server and returns answer
	 *
	 * @param comm
	 *            is the communication channel
	 * @return always null since this expect no answer
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	public XMLable exec(final CommClient comm)
			throws IOException, SAXException, InvalidKeyException, AccessControlException {
		return comm.getWorks(this);
	}

	/**
	 * This is called by XML parser This retrieves URI, hostUID and activation
	 * params
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @since 8.2.0
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
			} else if (attribute.compareToIgnoreCase(getColumnLabel(STATUS)) == 0) {
				setStatus(StatusEnum.valueOf(value));
			}
		}
	}

	/**
	 * This is for testing only. The first argument must be a valid client
	 * configuration file. Without a second argument, this dumps an
	 * XMLRPCCommandGetWorks object. If the second argument is an XML file
	 * containing a description of an XMLRPCCommandGetWorks this creates an
	 * object from XML description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar
	 * xtremweb.communications.XMLRPCCommandGetWorks aConfigFile
	 * [anXMLDescriptionFile]
	 */
	public static void main(final String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			final XMLRPCCommandGetWorks cmd = new XMLRPCCommandGetWorks(
					new URI(config.getCurrentDispatcher(), new UID()), config.getUser());
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
