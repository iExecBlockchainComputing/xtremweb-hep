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

import java.io.DataOutputStream;
import java.io.FileInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.common.XMLEndParseException;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLWriter;
import xtremweb.common.XMLable;
import xtremweb.common.XWReturnCode;
import xtremweb.common.XWTools;

/**
 * This class represents RPC response
 *
 * @author Oleg Lodygensky
 *
 */
public final class XMLRPCResult extends XMLable {

	private static final Logger logger = Logger.getLogger(XMLRPCResult.class);
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "xmlrpcresult";
	/**
	 * This is the message column index
	 */
	public static final int MESSAGE = 0;
	/**
	 * This is the return code column index
	 */
	public static final int RETURNCODE = MESSAGE + 1;
	/**
	 * This is the timestamp column index
	 */
	public static final int TIMESTAMP = RETURNCODE + 1;

	/**
	 * This retreives this message
	 */
	public String getMessage() {
		return (String) getValueAt(MESSAGE);
	}

	/**
	 * This sets this message
	 *
	 * @param msg
	 *            is this message
	 */
	public void setMessage(final String msg) {
		setValueAt(MESSAGE, msg);
	}

	/**
	 * This retrieves this message
	 */
	public long getTimeStamp() {
		return ((Long) (getValueAt(TIMESTAMP))).longValue();
	}

	/**
	 * This sets this command URI
	 *
	 * @param uri
	 *            is this command URI
	 */
	private void setTimeStamp(final long t) {
		setValueAt(TIMESTAMP, new Long(t));
	}

	/**
	 * This retrieves this return code
	 */
	public XWReturnCode getReturnCode() {
		return (XWReturnCode) getValueAt(RETURNCODE);
	}

	/**
	 * This sets this command URI
	 *
	 * @param uri
	 *            is this command URI
	 */
	private void setReturnCode(final XWReturnCode r) {
		setValueAt(RETURNCODE, r);
	}

	/**
	 * this is the default constructoru
	 */
	public XMLRPCResult() {
		// this throws an exception if cmdName is not a valid RPC command name
		// this class has no attribute (LAST_ATTRIBUTE = -1; hence MAX_ATTRIBUTE
		// = 0)

		super(THISTAG, -1);

		setCurrentVersion();
		setAttributeLength(TIMESTAMP);
		setColumns();
		setColumnAt(MESSAGE, "MESSAGE");
		setColumnAt(RETURNCODE, "RETURNCODE");
		setColumnAt(TIMESTAMP, "TIMESTAMP");
		setTimeStamp(System.currentTimeMillis());
	}

	/**
	 * This constructor sets this return code and message
	 *
	 * @param r
	 *            is this return code
	 * @param msg
	 *            is this message
	 */
	public XMLRPCResult(final XWReturnCode r, final String msg) {
		this();
		setMessage(msg);
		setReturnCode(r);
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {
		return getOpenTag() + getCloseTag();
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

		logger.trace("     attribute.length = " + attrs.getLength());

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);

			logger.trace("     attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\"");

			if (attribute.compareToIgnoreCase(getColumnLabel(MESSAGE)) == 0) {
				logger.trace("creating message from " + value);
				try {
					setValueAt(MESSAGE, value);
				} catch (final Exception e) {
				}
			} else if (attribute.compareToIgnoreCase(getColumnLabel(TIMESTAMP)) == 0) {
				logger.trace("creating timestamp from " + value);
				try {
					setValueAt(TIMESTAMP, new Long(value));
				} catch (final Exception e) {
				}
			} else if (attribute.compareToIgnoreCase(getColumnLabel(RETURNCODE)) == 0) {
				logger.trace("creating returncode from " + value);
				try {
					setValueAt(RETURNCODE, XWReturnCode.valueOf(value));
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
				}
			}
		}
	}

	/**
	 * This is called to decode XML elements
	 *
	 * @see xtremweb.common.XMLReader#read(java.io.InputStream)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			super.xmlElementStart(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		logger.trace("Start element - " + qname);

		if (qname.compareToIgnoreCase(getXMLTag()) == 0) {
			fromXml(attrs);
		} else {
			throw new SAXException("not a " + getXMLTag() + " command");
		}
	}

	/**
	 * This calls toString(false)
	 */
	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * This retreives String representation
	 *
	 * @param csv
	 *            tells whether CSV format is expected
	 * @return this object String representation
	 * @see xtremweb.common.Table#toString(boolean)
	 */
	@Override
	public String toString(final boolean csv) {
		final String ret = new String("[" + getTimeStamp() + "] : " + getReturnCode() + "," + getMessage());

		return ret;
	}

	/**
	 * This is for testing only. Without any argument, this dumps a
	 * TaskInterface object. If the first argument is an XML file containing a
	 * description of a TaskInterface, this creates a TaskInterface from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.TaskInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final XMLRPCResult itf = new XMLRPCResult(XWReturnCode.FATAL, "pouet");
			if (argv.length > 0) {
				try {
					final XMLReader reader = new XMLReader(itf);
					reader.read(new FileInputStream(argv[0]));
				} catch (final XMLEndParseException e) {
				}
			}
			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			e.printStackTrace();
			System.out.println("Usage : java -cp " + XWTools.JARFILENAME
					+ " xtremweb.common.TaskInterface [anXMLDescriptionFile]");
		}
	}
}
