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
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.SQLRequest;

/**
 * Created: Apr 15th, 2014<r />
 *
 * @author <a href="oleg.lodygensky /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 * @since 9.0.0
 */

public final class DataType extends Type {

	private static final Logger logger = Logger.getLogger(DataType.class);

	/**
	 * This is the database table name
	 */
	public static final String TABLENAME = "datatypes";

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "datatype";

	/**
	 * This enumerates this type columns
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is primary index to be used as reference (by datatypeid columns
		 * of "datas" table)
		 */
		DATATYPEID {
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
		DATATYPENAME {
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
			public DataTypeEnum fromString(final String v) {
				return DataTypeEnum.valueOf(v);
			}
		},
		/**
		 * This is the type description
		 */
		DATATYPEDESCRIPTION {
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
	public DataType() {
		super(THISTAG, TABLENAME);
		setAttributeSize(ENUMSIZE);
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 */
	public DataType(final SQLRequest r) {
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
	public DataType(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public DataType(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #DataType(InputStream)
	 */
	public DataType(final File f) throws IOException, SAXException {
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
	public DataType(final InputStream input) throws IOException, SAXException {
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
	public DataType(final Attributes attrs) {
		this();
		super.fromXml(attrs);
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
	 * This fills columns from DB
	 *
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setId((Integer) Columns.DATATYPEID.fromResultSet(rs));
			setType((String) Columns.DATATYPENAME.fromResultSet(rs));
			setDescription((String) Columns.DATATYPEDESCRIPTION.fromResultSet(rs));
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}

		setDirty(false);
	}

	/**
	 * This retrieves the type
	 *
	 * @return this attribute, or null if not set
	 */
	public DataTypeEnum getType() {
		return ((DataTypeEnum) getValue(Columns.DATATYPENAME));
	}

	/**
	 * This retrieves the description
	 *
	 * @return this attribute, or null if not set
	 */
	public String getDescription() {
		try {
			return ((String) getValue(Columns.DATATYPEDESCRIPTION));
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
		return ((Integer) getValue(Columns.DATATYPEID)).intValue();
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
	public final boolean setValue(final String attribute, final Object v) throws IllegalArgumentException {
		final String uppercaseAttrA = attribute.toUpperCase();
		return setValue(Columns.valueOf(uppercaseAttrA), v);
	}

	/**
	 * This sets the id
	 *
	 * @return true if value has changed, false otherwise
	 */
	private boolean setId(final Integer v) {
		return setValue(Columns.DATATYPEID, v);
	}

	/**
	 * This calls setType(DataTypeEnum.valueOf(v))
	 *
	 * @see #setType(DataTypeEnum)
	 */
	private boolean setType(final String v) {
		return setType(DataTypeEnum.valueOf(v));
	}

	/**
	 * This sets the type
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setType(final DataTypeEnum v) {
		return setValue(Columns.DATATYPENAME, v.toString());
	}

	/**
	 * This sets the description
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setDescription(final String v) {
		return setValue(Columns.DATATYPEDESCRIPTION, v);
	}

	/**
	 * This is for testing only Without any argument, this dumps a DataType
	 * object. If the first argument is an XML file containing a description of
	 * a DataType, this creates a AppInterface from XML description and dumps
	 * it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.AppInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final DataType itf = new DataType();
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
