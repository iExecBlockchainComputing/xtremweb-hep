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

package xtremweb.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * XMLKey.java
 *
 * Created: March 22nd, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class (un)marshal object to/from XML This is especially used by XMLtuple
 * as key
 *
 * @see XMLtuple
 */
public class XMLKey extends XMLObject {

	private static final Logger logger = Logger.getLogger(XMLKey.class);
	public static final String THISTAG = "XMLKEY";

	/**
	 * This default constructor contructs an empty object
	 */
	public XMLKey() {
		super();
		setXMLTag(THISTAG);
	}

	/**
	 */
	public XMLKey(final Object v) {
		super(v);
		setXMLTag(THISTAG);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on I/O error
	 * @throws SAXException
	 *             on XML error
	 */
	public XMLKey(final DataInputStream input) throws IOException, SAXException {
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This constructs a new object from XML attributes
	 */
	public XMLKey(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This calls toString()
	 *
	 * @param csv
	 *            is never used
	 * @see Table#toString(boolean)
	 */
	@Override
	public String toString(final boolean csv) {
		return toString();
	}
}
