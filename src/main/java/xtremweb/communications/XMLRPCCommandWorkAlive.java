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
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.common.HostInterface;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

/**
 * XMLRPCCommandWorkAlive.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand hearbeat monitor
 */
public class XMLRPCCommandWorkAlive extends XMLRPCCommand {

	private static final Logger logger = Logger.getLogger(XMLRPCCommandWorkAlive.class);

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.WORKALIVE;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandWorkAlive() throws IOException {
		super(null, IDRPC);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param h
	 *            is the hashtable WorkAlive parameter
	 */
	public XMLRPCCommandWorkAlive(final URI uri, final XMLHashtable h) throws IOException {
		super(uri, IDRPC);
		setParameter(h);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param h
	 *            is the hashtable WorkAlive parameter
	 */
	public XMLRPCCommandWorkAlive(final URI uri, final Hashtable h) throws IOException {
		this(uri, new XMLHashtable(h));
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param u
	 *            defines the host owner
	 * @param h
	 *            defines the computing host
	 * @param p
	 *            is the hashtable WorkAlive parameter
	 */
	public XMLRPCCommandWorkAlive(final URI uri, final UserInterface u, final HostInterface h, final XMLHashtable p)
			throws IOException {
		this(uri, p);
		setUser(u);
		setHost(h);
	}

	/**
	 * This constructs a new command
	 *
	 * @param uri
	 *            contains the URI to connect to
	 * @param u
	 *            defines the host owner
	 * @param h
	 *            defines the computing host
	 * @param p
	 *            is the hashtable WorkAlive parameter
	 */
	public XMLRPCCommandWorkAlive(final URI uri, final UserInterface u, final HostInterface h, final Hashtable p)
			throws IOException {
		this(uri, u, h, new XMLHashtable(p));
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
	public XMLRPCCommandWorkAlive(final InputStream input) throws IOException, SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
	}

	/**
	 * This is called to decode XML elements
	 *
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			xmlElementStartCheckUserAndHost(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		try {
			final XMLable param = getParameter();
			if (param == null) {
				if (qname.compareToIgnoreCase(XMLHashtable.THISTAG) != 0) {
					throw new SAXException(
							"XMLRPCCommandWorkAlive : not a " + getXMLTag() + " command (" + qname + ")");
				}
				setParameter(new XMLHashtable(attrs));
				getParameter().setCurrentVersion(getCurrentVersion());
			} else {
				param.xmlElementStart(uri, tag, qname, attrs);
			}
		} catch (final Exception e) {
			throw new SAXException("XMLRPCCommandWorkAlive : not a " + getXMLTag() + " command (" + qname + ")");
		}
	}

	/**
	 * This decrements nested on each "</XMLHashtable>" or "</XMLVector>".<br />
	 * This increment currentIndex is nested is 0; otherwise this calls
	 * tuples[currentIndex].xmlElementStop()
	 *
	 * @see xtremweb.common.XMLObject#xmlElementStop(String, String, String)
	 */
	@Override
	public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

		super.xmlElementStop(uri, tag, qname);

		logger.trace(
				"XMLRPCCommandWorkAlive#xmlElementStop " + uri + ", " + tag + ", " + qname + " = " + getCurrentValue());

		if (getParameter() != null) {
			try {
				getParameter().xmlElementStop(uri, tag, qname);
			} finally {
				resetCurrentValue();
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
	 */
	@Override
	public XMLable exec(final CommClient comm)
			throws IOException, ClassNotFoundException, SAXException, InvalidKeyException, AccessControlException {
		return comm.workAlive(this);
	}

	/**
	 * This is for testing only.<br />
	 * argv[0] must contain config file name<br />
	 * If argv[1] is empty this creates a dummy XML representation<br />
	 * Otherwise, argv[1] may content an XML file name containing an XML
	 * representation to read. <br />
	 * <br />
	 * The dummy or read representation is finally dumped
	 */
	public static void main(final String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);

			final Collection v = new Vector();
			v.add(new String("a string in vector"));
			v.add(new Integer(100));
			v.add(new Boolean("true"));
			final Hashtable h = new Hashtable();
			final Hashtable h2 = new Hashtable();
			h.put(new Integer(1), new String("un"));
			h.put(new String("deux"), new Integer(2));
			h.put(new String("a vector"), v);
			h.put(new String("a null UID"), UID.NULLUID);
			h.put(new String("a false boolean"), new Boolean("false"));
			h2.put(new Integer(10), new String("dix"));
			h2.put(new String("dix"), new Integer(10));
			h2.put(new String("a vector"), v);
			h.put(new String("an hashtable"), h2);
			final XMLHashtable xmlh = new XMLHashtable(h);

			XMLRPCCommandWorkAlive cmd = new XMLRPCCommandWorkAlive(new URI(config.getCurrentDispatcher()),
					config.getUser(), config.getHost(), xmlh);
			if (argv.length > 1) {
				cmd = new XMLRPCCommandWorkAlive(new FileInputStream(argv[1]));
			}
			logger.info(cmd.openXmlRootElement() + cmd.toXml() + cmd.closeXmlRootElement());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
