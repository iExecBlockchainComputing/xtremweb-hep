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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.database.DBConnPoolThread;
import xtremweb.security.XWAccessRights;
import xtremweb.security.XWAccessRightsValidator;

/**
 * This class was formerly the TableInterface one, until 8.x. TableInterface
 * class has been renamed to Table since 9.0.0 Table derives from Type since
 * 9.0.0. Interface tables implements columns as defined in TableColumns.
 *
 * <br />
 * Created: June 26th, 2003<br />
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @since v1r2-rc3(RPC-V)
 */

public abstract class Table extends Type {

	/**
	 * This grants access
	 *
	 * @since 9.0.7
	 */
	private final XWAccessRightsValidator accessValidator;

	/**
	 * This default constructor sets all attributes to null
	 */
	public Table() {
		super();
		accessValidator = new XWAccessRightsValidator(this);
	}

	/**
	 * This constructor sets the table name as provided
	 *
	 * @param n
	 *            is this tag name
	 * @param t
	 *            is this table name
	 * @see #Table()
	 */
	public Table(final String n, final String t) {
		this();
		tagName = n;
		if (tagName != null) {
			setXMLTag(n);
		}
		tableName = t;
	}

	/**
	 * This sets the access rights
	 *
	 * @return true is value has changed
	 */
	public final boolean setAccessRights(final XWAccessRights v) {
		return setValue(TableColumns.ACCESSRIGHTS, v);
	}

	/**
	 * This retrieves this data access rights
	 *
	 * @return this attribute
	 */
	public final XWAccessRights getAccessRights() {
		try {
			return (XWAccessRights) getValue(TableColumns.ACCESSRIGHTS);
		} catch (final Exception e) {
		}
		setAccessRights(XWAccessRights.DEFAULT);
		return XWAccessRights.DEFAULT;
	}

	/**
	 * This sets the UID
	 *
	 * @return true if UID has changed, false otherwise
	 */
	public final boolean setUID(final UID v) {
		return setValue(TableColumns.UID, v);
	}

	/**
	 * This retrieves the UID, if already set
	 *
	 * @return this attribute
	 * @exception IOException
	 *                is thrown is attribute is nor set, neither well formed
	 */
	public final UID getUID() {
		return (UID) getValue(TableColumns.UID);
	}

	/**
	 * This sets the owner UID
	 *
	 * @param v
	 *            is the owner uid
	 * @return true if value has changed, false otherwise
	 */
	public final boolean setOwner(final UID v) {
		return setValue(TableColumns.OWNERUID, v);
	}

	/**
	 * This retrieves this object owner UID // public abstract UID getOwner()
	 * throws IOException;
	 *
	 * @return this owner UID
	 */
	public final UID getOwner() {
		return (UID) getValue(TableColumns.OWNERUID);
	}

	/**
	 * This sets the error message
	 *
	 * @return true if value has changed, false otherwise
	 * @since 9.0.0
	 */
	public final boolean setErrorMsg(final String v) {
		return setValue(TableColumns.ERRORMSG, v);
	}

	/**
	 * This retrieves the error message
	 *
	 * @return the error message
	 * @since 9.0.0
	 */
	public final String getErrorMsg() {
		return (String) getValue(TableColumns.ERRORMSG);
	}

	/**
	 * This retrieves the last modification date Please note that this field is
	 * automatically set by mysql; there is no way to set this field.
	 *
	 * @return the last modification date
	 * @since 9.0.0
	 */
	public final Date getMTime() {
		return (Date) getValue(TableColumns.MTIME);
	}

	/**
	 * This should test access rights ; this should be overriden
	 *
	 * @param user
	 *            is the user who try to read
	 * @param ownerGroup
	 *            is the group of the owner of this object
	 * @return userCanRead(user) || groupCanRead(ownerGroup, userGroup) ||
	 *         otherCanRead()
	 */
	public final boolean canRead(final UserInterface user, final UID ownerGroup)
			throws AccessControlException, IOException {
		return accessValidator.canRead(user, ownerGroup);
	}

	/**
	 * This should test access rights ; this should be overriden
	 *
	 * @param user
	 *            is the user who try to read
	 * @param ownerGroup
	 *            is the group of the owner of this object
	 * @return userCanWrite(user) || groupCanWrite(ownerGroup, userGroup) ||
	 *         otherCanWrite()
	 */
	public final boolean canWrite(final UserInterface user, final UID ownerGroup)
			throws AccessControlException, IOException {
		return accessValidator.canWrite(user, ownerGroup);
	}

	/**
	 * This should test access rights ; this should be overridden
	 *
	 * @param user
	 *            is the user who try to read
	 * @param ownerGroup
	 *            is the group of the owner of this object
	 * @return userCanExec(user) || groupCanExec(ownerGroup, userGroup) ||
	 *         otherCanExec()
	 */
	public final boolean canExec(final UserInterface user, final UID ownerGroup)
			throws AccessControlException, IOException {
		return accessValidator.canExec(user, ownerGroup);
	}

	/**
	 * This constructs a new XMLRPCCommand object
	 *
	 * @param io
	 *            is stream handler to read XML representation from
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	public static Table newInterface(final StreamIO io)
			throws ClassNotFoundException, IOException, InvalidKeyException, AccessControlException {
		return newInterface(io.input());
	}

	/**
	 * This reads a new XMLRPCCommand object from input stream
	 *
	 * @param input
	 *            is the input stream
	 * @param itf
	 *            is the interface to read
	 * @throws IOException
	 *             on I/O error
	 * @throws InvalidKeyException
	 *             on authentication or authorization error
	 * @throws SAXException
	 *             on XML exception error
	 * @return the read interface
	 */
	private static Table readInterface(final BufferedInputStream input, final Table itf)
			throws InvalidKeyException, SAXException, IOException {
		final XMLReader reader = new XMLReader(itf);
		reader.read(input);
		return itf;
	}

	/**
	 * This constructs a new XMLRPCCommand object. This first checks the opening
	 * tag and then instanciate the right object accordingly to the opening tag.
	 *
	 * @param in
	 *            is the input stream to read interface from
	 * @throws IOException
	 *             on I/O error
	 * @throws InvalidKeyException
	 *             on authentication or authorization error
	 */
	public static Table newInterface(final InputStream in) throws IOException, InvalidKeyException {

		final BufferedInputStream input = new BufferedInputStream(in);
		input.mark(XWTools.BUFFEREND);

		try {
			final Table ret = new WorkInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new TaskInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new AppInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new DataInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new CategoryInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new UserInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new GroupInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new HostInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new SessionInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new TraceInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			final Table ret = new UserGroupInterface();
			return readInterface(input, ret);
		} catch (final SAXException e) {
		}

		throw new IOException("Unable to create new Interface from input stream");
	}

	/**
	 * This constructs a new XMLRPCCommand object. This first checks the opening
	 * tag and then instantiate the right object accordingly
	 *
	 * @see AppInterface#AppInterface(Attributes)
	 * @see DataInterface#DataInterface(Attributes) etc.
	 */
	public static Table newInterface(final String uri, final String xmltag, final String qname, final Attributes attrs)
			throws ClassNotFoundException {

		try {
			final XWTag tag = XWTag.valueOf(qname.toUpperCase());
			return tag.newInterface(attrs);
		} catch (final Exception e) {
			throw new ClassNotFoundException("newInterface : unknwon tag " + qname, e);
		}
	}

	/**
	 * This sets this objects attributes from provided interface This does not
	 * insert/update this object into DB
	 *
	 * @since 9.0.0
	 */
	public abstract void updateInterface(Table t) throws IOException;

	/**
	 * This calls update(true)
	 *
	 * @see #update(boolean)
	 */
	public void update() throws IOException {
		update(true);
	}

	/**
	 * This updates this object in the database table, if needed
	 *
	 * @param pool
	 *            uses pool mode if true; execute query immediately if false
	 * @since 8.0.0
	 */
	public void update(final boolean pool) throws IOException {

		if (isDirty() == false) {
			return;
		}

		try {
			DBConnPoolThread.getInstance().update(this, (String) null, pool);
			setDirty(false);
		} catch (final Exception e) {
			getLogger().exception(e);
			throw new IOException(e.toString());
		}
	}

	/**
	 * This reads from DB
	 *
	 * @see xtremweb.common.Table#criteria()
	 * @exception IOException
	 *                is thrown on error
	 */
	@Override
	public void select() throws IOException {
		try {
			DBConnPoolThread.getInstance().select(this);
		} catch (final Exception e) {
			getLogger().exception(e);
			throw new IOException(e.toString());
		}
	}

	/**
	 * This inserts this object in DB and re-reads it immediately so that we
	 * have the correct primary key (which is auto-increment)
	 *
	 * @see #select()
	 * @see xtremweb.common.Table#valuesToString()
	 * @see #tagName
	 * @exception IOException
	 *                is thrown on error
	 */
	@Override
	public void insert() throws IOException {
		DBConnPoolThread.getInstance().insert(this);
	}

	/**
	 * This copy row in history table and deletes row
	 *
	 * @see xtremweb.common.Table#criteria()
	 * @see #tagName
	 */
	public void delete() throws IOException {
		DBConnPoolThread.getInstance().delete(this);
	}

	/**
	 * This aims to retreive rows for the "SELECT" SQL statement.
	 *
	 * @return DEFAULTSELECTIONROW
	 * @since 5.8.0
	 */
	@Override
	public String rowSelection() throws IOException {
		if (getRequest() != null) {
			return getRequest().rowSelection();
		}
		return DEFAULTSELECTIONROW;
	}

	/**
	 * This is for testing only.<br />
	 * This creates a XMLRPCCommand from given XML String representation of any
	 * XMLRPCCommand descendant<br />
	 * argv[0] must contain an XML representation.<br />
	 * The object is finally dumped
	 */
	public static void main(final String[] argv) {
		try {
			final Table itf = Table.newInterface(new ByteArrayInputStream(argv[0].getBytes(XWTools.UTF8)));
			if (itf.getUID() == null) {
				itf.setUID(UID.getMyUid());
			}
			System.out.println(itf.openXmlRootElement() + itf.toXml() + itf.closeXmlRootElement());
		} catch (final Exception e) {
			final Logger logger = new Logger();
			logger.exception("Usage : java -cp " + XWTools.JARFILENAME
					+ " xtremweb.common.TableInterface [anXMLDescriptionFile]", e);
		}
	}
}
