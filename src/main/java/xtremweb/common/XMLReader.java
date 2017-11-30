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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import xtremweb.communications.XMLRPCResult;
import xtremweb.communications.XWCommException;

/**
 * Created: 20 janvier 2010<br />
 * This reads an object XML description from input stream
 *
 * @author Oleg Lodygensky
 * @since 5.8.0
 */

public class XMLReader implements AutoCloseable {

	private static final Logger logger = Logger.getLogger(XMLReader.class);

	private final XMLable xmlObject;

	/**
	 * This default constructor
	 */
	public XMLReader(final XMLable o) {
		xmlObject = o;
		xmlObject.resetCurrentVersion();
	}

	/**
	 * This reads this object definition from an XML stream. This first set
	 * version to null : version should be read from input stream.
	 *
	 * @param in
	 *            is the input stream to get XML definition
	 * @throws IOException
	 *             on I/O error
	 * @throws SAXException
	 *             on XML error
	 * @throws InvalidKeyException
	 *             on authentication or authorization error
	 */
	public void read(final InputStream in) throws SAXException, IOException, InvalidKeyException {

		final BufferedInputStream input = new BufferedInputStream(in);

		try {
			input.mark(XWTools.BUFFEREND);
			final String dtd = null;
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			final DescriptionHandler handler = new DescriptionHandler(dtd);
			parser.parse(input, handler);
		} catch (final SAXException saxe) {
			logger.trace(saxe.getMessage());
			if (!(saxe instanceof XMLEndParseException)) {
				input.reset();
				final XMLRPCResult theresult = new XMLRPCResult();
				try {
					final String dtd = null;
					final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
					final DescriptionHandler handler = new DescriptionHandler(dtd);
					parser.parse(input, handler);
				} catch (final ParserConfigurationException e) {
					throw new IOException(e);
				} catch (final SAXException e2) {
					if (e2 instanceof XMLEndParseException) {
						final XWCommException xwce = new XWCommException(theresult);
						xwce.authentication();
						xwce.authorization();
						xwce.disk();
					} else {
						throw saxe;
					}
				}
			}
		} catch (final ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

	/*********************************************************************
	 **
	 ** This class extends the XML DefaultHandler
	 **
	 *********************************************************************/
	private class DescriptionHandler extends DefaultHandler {
		private final String dtd;

		DescriptionHandler(final String dtd) {
			this.dtd = dtd;
		}

		/**
		 * This method decodes XML elements value
		 *
		 * @since 9.0.0
		 */
		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException {
			xmlObject.characters(ch, start, length);
		}

		/**
		 * This method decodes XML open tags
		 */
		@Override
		public void startElement(final String uri, final String tag, final String qname, final Attributes attrs)
				throws SAXException {
			xmlObject.xmlElementStart(uri, tag, qname, attrs);
		}

		/**
		 * This method decodes XML close tags
		 *
		 * @since 9.0.0
		 */
		@Override
		public void endElement(final String uri, final String tag, final String qname) throws SAXException {
			xmlObject.xmlElementStop(uri, tag, qname);
		}
	}

	@Override
	public void close() {
	}
}
