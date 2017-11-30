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

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.SQLRequest;
import xtremweb.security.X509Proxy;
import xtremweb.security.XWAccessRights;

/**
 * Created: Feb 19th, 2002<br />
 * This class describes a row of the works SQL table.
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

public final class UserInterface extends Table {

	private static final Logger logger = Logger.getLogger(UserInterface.class);

	/**
	 * This is the database table name This was stored in
	 * xtremweb.dispatcher.User
	 *
	 * @since 9.0.0
	 */
	public static final String TABLENAME = ("users");
	/**
	 * This the user login length as defined in DB
	 */
	public static final int USERLOGINLENGTH = 250;

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "user";

	/**
	 * This enumerates this interface columns
	 */
	public enum Columns implements XWBaseColumn {
		/**
		 * This is the column index of the user group UID, if any
		 */
		USERGROUPUID {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @returnan UID representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public UID fromString(final String v) {
				return new UID(v);
			}
		},
		/**
		 * This is the column index of the X.509 certificate itself (begin with
		 * "-----BEGIN CERTIFICATE-----")
		 *
		 * @see X509Proxy#BEGINCERT
		 */
		CERTIFICATE {
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
				String val = v;
				val = val.replaceAll(X509Proxy.BEGINCERT, X509Proxy.BEGINCERT_NOSPACE);
				val = val.replaceAll(X509Proxy.ENDCERT, X509Proxy.ENDCERT_NOSPACE);
				val = val.replaceAll(X509Proxy.BEGINPRIVATEKEY, X509Proxy.BEGINPRIVATEKEY_NOSPACE);
				val = val.replaceAll(X509Proxy.ENDPRIVATEKEY, X509Proxy.ENDPRIVATEKEY_NOSPACE);

				val = val.replaceAll("\\s", "\n");

				val = val.replaceAll(X509Proxy.BEGINCERT_NOSPACE, X509Proxy.BEGINCERT);
				val = val.replaceAll(X509Proxy.ENDCERT_NOSPACE, X509Proxy.ENDCERT);
				val = val.replaceAll(X509Proxy.BEGINPRIVATEKEY_NOSPACE, X509Proxy.BEGINPRIVATEKEY);
				val = val.replaceAll(X509Proxy.ENDPRIVATEKEY_NOSPACE, X509Proxy.ENDPRIVATEKEY);
				return val;
			}
		},
		/**
		 * This column tells whether the client is using a private/public keys
		 * pair
		 *
		 * @since 7.5.0
		 */
		CHALLENGING {
			/**
			 * This creates an object from String representation for this column
			 * value
			 *
			 * @param v
			 *            the String representation
			 * @return a Boolean representing the column value
			 * @throws Exception
			 *             is thrown on instantiation error
			 */
			@Override
			public Boolean fromString(final String v) throws URISyntaxException {
				return new Boolean(v);
			}
		},
		/**
		 * This is the column index of the login
		 */
		LOGIN {
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
		},
		/**
		 * This is the column index of the password
		 */
		PASSWORD {
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
		},
		/**
		 * This is the column index of the email address
		 */
		EMAIL {
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
		},
		/**
		 * This is the column index of hte first name
		 */
		FNAME {
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
		},
		/**
		 * This is the column index of the last name
		 */
		LNAME {
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
		},
		/**
		 * This is the column index of the country
		 */
		COUNTRY {
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
		},
		/**
		 * This is the column index of the completed job counter; this is
		 * updated on job completion
		 */
		NBJOBS {
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
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the pending job counter; this is updated
		 * on job submission
		 *
		 * @since 7.0.0
		 */
		PENDINGJOBS {
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
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the running job counter this is updated
		 * on succesfull worker request
		 *
		 * @since 7.0.0
		 */
		RUNNINGJOBS {
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
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the column index of the error job counter; this is updated on
		 * job error
		 *
		 * @since 7.0.0
		 */
		ERRORJOBS {
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
				return Integer.valueOf(v);
			}
		},
		/**
		 * This is the average execution time; this is calculated as finished
		 * jobs return
		 *
		 * @since 7.0.0
		 */
		USEDCPUTIME {
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
			public Long fromString(final String v) {
				return new Long(v);
			}
		},
		/**
		 * This is the column index of the user rights
		 */
		RIGHTS {
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
			public UserRightEnum fromString(final String v) {
				return UserRightEnum.valueOf(v);
			}
		};
		/**
		 * This is the index based on ordinal so that the first value is
		 * TableColumns + 1
		 *
		 * @see xtremweb.common#TableColumns
		 * @see Enum#ordinal()
		 * @since 8.2.0
		 */
		private int ord;

		/**
		 * This constructor sets the ord member as ord = this.ordinal +
		 * TableColumns.SIZE
		 *
		 * @since 8.2.0
		 */
		Columns() {
			ord = this.ordinal() + TableColumns.SIZE;
		}

		/**
		 * This retrieves the index based ordinal
		 *
		 * @return the index based ordinal
		 * @since 8.2.0
		 */
		@Override
		public int getOrdinal() {
			return ord;
		}

		/**
		 * This creates a new object from String for the given column
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
		 * This creates a new object from SQL result set
		 *
		 * @param rs
		 *            is the SQL result set
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
			try {
				return TableColumns.fromInt(v);
			} catch (final Exception e) {
			}
			for (final Columns c : Columns.values()) {
				if (c.getOrdinal() == v) {
					return c;
				}
			}
			throw new IndexOutOfBoundsException(("unvalid Columns value ") + v);
		}
	}

	/**
	 * This is the size, including TableColumns
	 */
	private static final int ENUMSIZE = Columns.values().length;

	/**
	 * This retrieves column label from enum Columns. This takes cares of this
	 * version. If this version is null, this version is prior to 5.8.0. Before
	 * 5.8.0, OWNERUID and ACCESSRIGHTS did not exist. Then this returns null
	 * for these two values.
	 *
	 * @param i
	 *            is an ordinal of an Columns
	 * @since 5.8.0
	 */
	@Override
	public String getColumnLabel(final int i) throws IndexOutOfBoundsException {
		try {
			return TableColumns.fromInt(i).toString();
		} catch (final Exception e) {
		}
		return Columns.fromInt(i).toString();
	}

	/**
	 * This is the default constructor
	 */
	public UserInterface() {

		super(THISTAG, TABLENAME);

		setAttributeLength(ENUMSIZE);

		setRights(UserRightEnum.NONE);
		setAccessRights(XWAccessRights.DEFAULT);
		setShortIndexes(new int[] { TableColumns.UID.getOrdinal(), Columns.LOGIN.getOrdinal() });
	}

	/**
	 * This is a copy constructor
	 *
	 * @param src
	 *            is the UserInterface to duplicate
	 * @throws IOException
	 */
	public UserInterface(final UserInterface src) throws IOException {
		this();
		setUID(src.getUID());
		setOwner(src.getOwner());
		setAccessRights(src.getAccessRights());
		setGroup(src.getGroup());
		setLogin(src.getLogin());
		setPassword(src.getPassword());
		setEMail(src.getEMail());
		setFirstName(src.getFirstName());
		setLastName(src.getLastName());
		setCountry(src.getCountry());
		setNbJobs(src.getNbJobs());
		setPendingJobs(src.getPendingJobs());
		setRunningJobs(src.getRunningJobs());
		setErrorJobs(src.getErrorJobs());
		setRights(src.getRights());
		setCertificate(src.getCertificate());
		setUsedcputime(src.getUsedcputime());
	}

	/**
	 * This constructs a new object providing its primary key value
	 *
	 * @param uid
	 *            is this new object UID
	 */
	public UserInterface(final UID uid) {
		this();
		setUID(uid);
	}

	/**
	 * This constructs a new object providing its user login and password
	 *
	 * @param l
	 *            is the login
	 * @param p
	 *            is the password
	 */
	public UserInterface(final String l, final String p) {
		this();
		setLogin(l);
		setPassword(p);
	}

	/**
	 * This constructs a new object from XML attributes
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @see Table#fromXml(Attributes)
	 * @throws IOException
	 *             on XML error
	 */
	public UserInterface(final Attributes attrs) {
		this();
		super.fromXml(attrs);
	}

	/**
	 * This calls this(StreamIO.stream(input));
	 *
	 * @param input
	 *            is a String containing an XML representation
	 */
	public UserInterface(final String input) throws IOException, SAXException {
		this(StreamIO.stream(input));
	}

	/**
	 * This constructs a new object from an XML file
	 *
	 * @param f
	 *            is the XML file
	 * @see #UserInterface(InputStream)
	 */
	public UserInterface(final File f) throws IOException, SAXException {
		this(new FileInputStream(f));
	}

	/**
	 * This creates a new object that will be retrieved with a complex SQL
	 * request
	 *
	 * @since 5.8.0
	 */
	public UserInterface(final SQLRequest r) {
		this();
		setRequest(r);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @see XMLReader#read(InputStream)
	 * @throws IOException
	 *             on XML error
	 */
	public UserInterface(final InputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This constructs an object from DB
	 *
	 * @param rs
	 *            is an SQL request result
	 * @exception IOException
	 */
	public UserInterface(final ResultSet rs) throws IOException {
		this();
		fill(rs);
	}

	/**
	 * This fills columns from DB
	 *
	 * @since 9.0.0
	 * @param rs
	 *            is the SQL data set
	 * @throws IOException
	 */
	@Override
	public void fill(final ResultSet rs) throws IOException {

		try {
			setUID((UID) TableColumns.UID.fromResultSet(rs));
			setOwner((UID) TableColumns.OWNERUID.fromResultSet(rs));
			setAccessRights((XWAccessRights) TableColumns.ACCESSRIGHTS.fromResultSet(rs));
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			throw new IOException(e.toString());
		}
		try {
			setUsedcputime((Long) Columns.USEDCPUTIME.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setNbJobs((Integer) Columns.NBJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setPendingJobs((Integer) Columns.PENDINGJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setRunningJobs((Integer) Columns.RUNNINGJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setErrorJobs((Integer) Columns.ERRORJOBS.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setRights((UserRightEnum) Columns.RIGHTS.fromResultSet(rs));
		} catch (final Exception e) {
			setRights(UserRightEnum.NONE);
		}
		try {
			setGroup((UID) Columns.USERGROUPUID.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setCertificate((String) Columns.CERTIFICATE.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLogin((String) Columns.LOGIN.fromResultSet(rs));
		} catch (final Exception e) {
			setRights(UserRightEnum.NONE);
		}
		try {
			setPassword((String) Columns.PASSWORD.fromResultSet(rs));
		} catch (final Exception e) {
			setRights(UserRightEnum.NONE);
		}
		try {
			setEMail((String) Columns.EMAIL.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setFirstName((String) Columns.FNAME.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setLastName((String) Columns.LNAME.fromResultSet(rs));
		} catch (final Exception e) {
		}
		try {
			setCountry((String) Columns.COUNTRY.fromResultSet(rs));
		} catch (final Exception e) {
		}
		setChallenging(false);
	}

	/**
	 * This updates this object from the given interface. This does not update
	 * row in DB
	 */
	@Override
	public void updateInterface(final Table useritf) throws IOException {

		final UserInterface itf = (UserInterface) useritf;

		if (itf.getGroup() != null) {
			setGroup(itf.getGroup());
		}
		if (itf.getFirstName() != null) {
			setFirstName(itf.getFirstName());
		}
		if (itf.getLastName() != null) {
			setLastName(itf.getLastName());
		}
		if (itf.getEMail() != null) {
			setEMail(itf.getEMail());
		}
		if (itf.getCountry() != null) {
			setCountry(itf.getCountry());
		}
		if (itf.getPassword() != null) {
			setPassword(itf.getPassword());
		}
		if (itf.getRights() != null) {
			setRights(itf.getRights());
		}
		if (itf.getAccessRights() != null) {
			setAccessRights(itf.getAccessRights());
		}
		if (itf.getOwner() != null) {
			setOwner(itf.getOwner());
		}
		if (itf.getCertificate() != null) {
			setCertificate(itf.getCertificate());
		}
		setNbJobs(itf.getNbJobs());
		setPendingJobs(itf.getPendingJobs());
		setRunningJobs(itf.getRunningJobs());
		setErrorJobs(itf.getErrorJobs());
		setUsedcputime(itf.getUsedcputime());
	}

	/**
	 * This gets the user rights<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return user rights or UserRights.NONE if not set
	 */
	public UserRightEnum getRights() {
		final UserRightEnum ret = (UserRightEnum) getValue(Columns.RIGHTS);
		if (ret != null) {
			return ret;
		}
		setRights(UserRightEnum.NONE);
		return UserRightEnum.NONE;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 * @exception IOException
	 *                is thrown is attribute is not well formed
	 */
	public UID getGroup() {
		try {
			return (UID) getValue(Columns.USERGROUPUID);
		} catch (final NullPointerException e) {
			return null;
		}
	}

	/**
	 * This gets the amount of already executed jobs for this user<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 */
	public int getNbJobs() {
		final Integer ret = (Integer) getValue(Columns.NBJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setNbJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of pending jobs for this user<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 * @since 7.0.0
	 */
	public int getPendingJobs() {
		final Integer ret = (Integer) getValue(Columns.PENDINGJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setPendingJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of running jobs for this user<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 * @since 7.0.0
	 */
	public int getRunningJobs() {
		final Integer ret = (Integer) getValue(Columns.RUNNINGJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setRunningJobs(0);
		return 0;
	}

	/**
	 * This gets the amount of erroneus jobs for this user<br />
	 * If not set, this attr is forced to 0
	 *
	 * @return the attribute, or 0 is not set
	 * @since 7.0.0
	 */
	public int getErrorJobs() {
		final Integer ret = (Integer) getValue(Columns.ERRORJOBS);
		if (ret != null) {
			return ret.intValue();
		}
		setErrorJobs(0);
		return 0;
	}

	/**
	 * This get the average execution time; if not set it is forced to 0
	 *
	 * @return exec average time or 0 if not set
	 * @since 7.0.0
	 */
	public long getUsedcputime() {
		final Long ret = (Long) getValue(Columns.USEDCPUTIME);
		if (ret != null) {
			return ret.longValue();
		}
		setUsedcputime(0L);
		return 0;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getFirstName() {
		try {
			return ((String) getValue(Columns.FNAME));
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getLastName() {
		try {
			return ((String) getValue(Columns.LNAME));
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getEMail() {
		try {
			return ((String) getValue(Columns.EMAIL));
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This retrieves the certificate URI
	 *
	 * @return this attribute, or null if not set
	 */
	public String getCertificate() {
		return (String) getValue(Columns.CERTIFICATE);
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getLogin() {
		try {
			return ((String) getValue(Columns.LOGIN));
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getPassword() {
		try {
			return ((String) getValue(Columns.PASSWORD));
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This gets an attribute
	 *
	 * @return this attribute, or null if not set
	 */
	public String getCountry() {
		try {
			return ((String) getValue(Columns.COUNTRY));
		} catch (final Exception e) {
		}
		return null;
	}

	/**
	 * This gets an attribute. This attr is forced to false if not set
	 *
	 * @return this attribute
	 * @since 7.5.0
	 */
	public boolean isChallenging() {
		try {
			final Boolean ret = (Boolean) getValue(Columns.CHALLENGING);
			return ret.booleanValue();
		} catch (final NullPointerException e) {
			setChallenging(false);
			return false;
		}
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
		final String A = attribute.toUpperCase();
		try {
			return setValue(TableColumns.valueOf(A), v);
		} catch (final Exception e) {
			return setValue(Columns.valueOf(A), v);
		}
	}

	/**
	 * This sets the rights
	 *
	 * @return true is value has changed
	 * @see xtremweb.common.UserRightEnum
	 */
	public boolean setRights(final UserRightEnum r) {
		return setValue(Columns.RIGHTS, r);
	}

	/**
	 * This sets amount of executed jobs
	 *
	 * @return true is value has changed
	 */
	public boolean setNbJobs(int v) {
		if (v < 0) {
			v = 0;
		}
		Integer i = Integer.valueOf(v);
		final boolean ret = setValue(Columns.NBJOBS, i);
		i = null;
		return ret;
	}

	/**
	 * This sets amount of pending jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setPendingJobs(final int v) {
		Integer i = null;
		if (v < 0) {
			i = new Integer(0);
		} else {
			i = Integer.valueOf(v);
		}
		final boolean ret = setValue(Columns.PENDINGJOBS, i);
		i = null;
		return ret;
	}

	/**
	 * This sets amount of running jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setRunningJobs(final int v) {
		Integer i = null;
		if (v < 0) {
			i = new Integer(0);
		} else {
			i = Integer.valueOf(v);
		}
		final boolean ret = setValue(Columns.RUNNINGJOBS, i);
		i = null;
		return ret;
	}

	/**
	 * This sets amount of erroneus jobs
	 *
	 * @return true is value has changed
	 * @since 7.0.0
	 */
	public boolean setErrorJobs(final int v) {
		return setValue(Columns.ERRORJOBS, Integer.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 7.0.0
	 */
	public boolean setUsedcputime(final long v) {
		return setValue(Columns.USEDCPUTIME, Long.valueOf(v < 0 ? 0 : v));
	}

	/**
	 * This updates total used CPU time for this user and increments NbJobs
	 *
	 * @param v
	 *            is the last execution time
	 */
	public void incUsedcputime(final long v) {
		long old = getUsedcputime();

		long w = v;
		if (w <= 0) {
			w = 0;
		}
		if (old <= 0) {
			old = 0;
		}

		setUsedcputime(w + old);
		incNbJobs();
	}

	/**
	 * This increments the amount of executed jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incNbJobs() {
		setNbJobs(getNbJobs() + 1);
	}

	/**
	 * This increments the amount of pending jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incPendingJobs() {
		setPendingJobs(getPendingJobs() + 1);
	}

	/**
	 * This increments the amount of running jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incRunningJobs() {
		setRunningJobs(getRunningJobs() + 1);
	}

	/**
	 * This increments the amount of erroneus jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void incErrorJobs() {
		setErrorJobs(getErrorJobs() + 1);
	}

	/**
	 * This decrements the amount of pending jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void decPendingJobs() {
		setPendingJobs(getPendingJobs() - 1);
	}

	/**
	 * This decrements the amount of running jobs for this application
	 *
	 * @since 7.0.0
	 */
	public void decRunningJobs() {
		setRunningJobs(getRunningJobs() - 1);
	}

	/**
	 * This sets the group
	 *
	 * @return true is value has changed
	 */
	public boolean setGroup(final UID v) {
		return setValue(Columns.USERGROUPUID, v);
	}

	/**
	 * This sets first name
	 *
	 * @return true is value has changed
	 */
	public boolean setFirstName(final String v) {
		return setValue(Columns.FNAME, (v == null ? null : v));
	}

	/**
	 * This sets last name
	 *
	 * @return true is value has changed
	 */
	public boolean setLastName(final String v) {
		return setValue(Columns.LNAME, (v == null ? null : v));
	}

	/**
	 * This set the certificate URI
	 *
	 * @return true if value has changed, false otherwise
	 */
	public boolean setCertificate(final String v) {
		return setValue(Columns.CERTIFICATE, v);
	}

	/**
	 * This sets the login
	 *
	 * @return true is value has changed
	 */
	public boolean setLogin(String v) {
		if ((v != null) && (v.length() > USERLOGINLENGTH)) {
			v = v.substring(0, USERLOGINLENGTH - 1);
			logger.warn("Login too long; truncated to " + v);
		}
		return setValue(Columns.LOGIN, (v == null ? null : v));
	}

	/**
	 * This sets password
	 *
	 * @return true is value has changed
	 */
	public boolean setPassword(final String v) {
		return setValue(Columns.PASSWORD, (v == null ? null : v));
	}

	/**
	 * This sets email
	 *
	 * @return true is value has changed
	 */
	public boolean setEMail(final String v) {
		return setValue(Columns.EMAIL, (v == null ? null : v));
	}

	/**
	 * This sets the country
	 *
	 * @return true is value has changed
	 */
	public boolean setCountry(final String v) {
		return setValue(Columns.COUNTRY, (v == null ? null : v));
	}

	/**
	 * @return true if value has changed, false otherwise
	 * @since 7.5.0
	 */
	public boolean setChallenging(final boolean v) {
		Boolean b = new Boolean(v);
		final boolean ret = setValue(Columns.CHALLENGING, b);
		b = null;
		return ret;
	}

	/**
	 * This change login to "DEL_" + uid and set ISDELETED flag to TRUE Because
	 * login is a DB constraint and one may want to insert and delete and then
	 * reinsert a user with the same login. And delete does not remove row from
	 * DB : it simply set ISDELETED flag to true
	 */
	@Override
	public void delete() throws IOException {
		setLogin("DEL_ " + getUID().toString());
		update();
		super.delete();
	}

	/**
	 * This is for testing only. Without any argument, this dumps a
	 * UserInterface object. If the first argument is an XML file containing a
	 * description of a UserInterface, this creates a UserInterface from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.common.UserInterface [xmlFile]
	 */
	public static void main(final String[] argv) {
		try {
			final UserInterface itf = new UserInterface();
			itf.setUID(UID.getMyUid());
			if (argv.length > 0) {
				try {
					final XMLReader reader = new XMLReader(itf);
					reader.read(new FileInputStream(argv[0]));
				} catch (final XMLEndParseException e) {
				}
			}
			if (itf.getCertificate() != null) {
				final ByteArrayInputStream is = new ByteArrayInputStream(itf.getCertificate().getBytes(XWTools.UTF8));
				try {
					XWTools.checkCertificate(is);
				} catch (final Exception e) {
					System.err.println("Invalid certificate : " + e);
				}
			}

			itf.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(new DataOutputStream(System.out));
			writer.write(itf);
		} catch (final Exception e) {
			logger.error("Caught exception, Usage : java -cp " + XWTools.JARFILENAME + " xtremweb.common.UserInterface [anXMLDescriptionFile]",
					e);
		}
	}
}
