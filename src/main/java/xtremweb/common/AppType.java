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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.sql.ResultSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import xtremweb.database.SQLRequest;

/**
 * Created: Mar 31st, 2014<br />
 *
 * @author <a href="mailto:oleg.lodygensky /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 * @since 9.0.0
 */

public final class AppType extends Type {

	private static final Logger logger = Logger.getLogger(AppType.class);

	/**
	 * This is the database table name
	 */
	public static final String TABLENAME = "apptypes";

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "apptype";

	/**
	 * This enumerates this type columns
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is primary index to be used as reference (by apptypeid column of
		 * "apps" table)
		 */
		APPTYPEID {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return an Integer representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public Integer fromString(final String v) {
				return new Integer(v);
			}
		},
		/**
		 * This type name
		 */
		APPTYPENAME {
			/**
			 * This creates an object from String representation for this column
			 * value This cleans the parameter to ensure SQL compliance
			 *
			 * @param v
			 *            the String representation
			 * @return the app type represented by v
			 */
			@Override
			public AppTypeEnum fromString(final String v) {
				return AppTypeEnum.valueOf(v);
			}
		},
		/**
		 * This is the type description
		 */
		APPTYPEDESCRIPTION {
			/**
			 * This creates an object from String representation for this column
			 * value This cleans the parameter to ensure SQL compliance
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public String fromString(final String v) {
				final String val = v;
				return val.replaceAll("[\\n\\s\'\"]+", "_");
			}
		};

		/**
		 * This retrieves the index based ordinal
		 *
		 * @return the index based ordinal
		 */
		@Override
		public int getOrdinal() {
			return this.ordinal();
		}

		/**
		 * This creates a new object from String for the given column This must
		 * be overridden by enum which value is not a String
		 *
		 * @param v
		 *            the String representation
		 * @return v
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		@Override
		public Object fromString(final String v) throws Exception {
			return v;
		}

		/**
		 * This creates a new object from the digen SQL result set
		 *
		 * @param rs
		 *            the SQL result set
		 * @return the object representing the column
		 * @throws Exception
		 *             is thrown on instantiation error
		 */
		public final Object fromResultSet(final ResultSet rs) throws Exception {
			return this.fromString(rs.getString(this.toString()));
		}

		/**
		 * This retrieves an Columns from its integer value
		 *
		 * @param v
		 *            is the integer value of the Columns
		 * @return an Columns
		 */
		public static XWBaseColumn fromInt(final int v) throws IndexOutOfBoundsException {
			for (final Columns c : Columns.values()) {
				if (c.getOrdinal() == v) {
					return c;
				}
			}
			throw new IndexOutOfBoundsException(("unvalid Columns value ") + v);
		}
	}

	/**
	 * This is the columns amount
	 */
	private static final int ENUMSIZE = Columns.values().length;

	/**
	 * This is the default constructor
	 */
	public AppType() {
		super(THISTAG, TABLENAME);
		setAttributeSize(ENUMSIZE);
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 */
	public AppType(final SQLRequest r) {
		this();
		setRequest(r);
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException
	 */
	public AppType(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public AppType(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #AppType(InputStream)
	 */
	public AppType(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 * @see XMLReader#read(InputStream)
	 */
	public AppType(final InputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @see Table#fromXml(Attributes)
	 * @throws IOException
	 *             on XML error
	 */
	public AppType(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This fills columns from DB
	 *
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setId((Integer) Columns.APPTYPEID.fromResultSet(rs));
			setType((String) Columns.APPTYPENAME.fromResultSet(rs));
			setDescription((String) Columns.APPTYPEDESCRIPTION.fromResultSet(rs));
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}

		setDirty(false);
	}

	/**
	 * This retrieves column label from enum Columns
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 */
	@Override
	public String getColumnLabel(final int i) throws IndexOutOfBoundsException {
		return Columns.fromInt(i).toString();
	}

	/**
	 * This retrieves the name
	 *
	 * @return this attribute, or null if not set
	 */
	public AppTypeEnum getType() {
		return (AppTypeEnum) getValue(Columns.APPTYPENAME);
	}

	/**
	 * This retrieves the description
	 *
	 * @return this attribute, or null if not set
	 */
	public String getDescription() {
		try {
			return (String) getValue(Columns.APPTYPEDESCRIPTION);
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the id
	 *
	 * @return this identifier
	 */
	public int getId() {
		return ((Integer) getValue(Columns.APPTYPEID)).intValue();
	}

	/**
	 * This sets parameter value; this is called from
	 * TableInterface#fromXml(Attributes)
	 *
	 * @param attribute
	 *            is the name of the attribute to set
	 * @param v
	 *            is the new attribute value
	 * @return true if value has changed, false otherwise
	 * @see Table#fromXml(Attributes)
	 */
	@Override
	public final boolean setValue(final String attribute, final Object v) {
		final String uppercaseAttr = attribute.toUpperCase();
		return setValue(Columns.valueOf(uppercaseAttr), v);
	}

	/**
	 * This sets the id
	 *
	 * @return true if value has changed, false otherwise
	 */
	private boolean setId(final Integer v) {
		return setValue(Columns.APPTYPEID, v);
	}

	/**
	 * This calls setName(AppTypeEnum.valueOf(v))
	 *
	 * @see #setName(AppTypeEnum);
	 */
	private boolean setType(final String v) {
		return setType(AppTypeEnum.valueOf(v));
	}

	/**
	 * This sets the name
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setType(final AppTypeEnum v) {
		return setValue(Columns.APPTYPENAME, v.toString());
	}

	/**
	 * This sets the description
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setDescription(final String v) {
		return setValue(Columns.APPTYPEDESCRIPTION, v);
	}

	/**
	 * This is for testing only Without any argument, this dumps a AppInterface
	 * object. If the first argument is an XML file containing a description of
	 * a AppInterface, this creates a AppInterface from XML description and
	 * dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.AppInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final AppType itf = new AppType();
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
			logger.error("Caught exception, Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.AppInterface [anXMLDescriptionFile]",
					e);
		}
	}

}
