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

package xtremweb.dispatcher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.*;

import javax.mail.MessagingException;

import com.iexec.common.contracts.generated.IexecHub;
import com.iexec.common.contracts.generated.WorkerPool;
import com.iexec.common.ethereum.*;
import com.iexec.common.model.ConsensusModel;
import com.iexec.common.model.ContributionStatusEnum;
import com.iexec.common.workerpool.WorkerPoolConfig;
import com.iexec.scheduler.actuator.ActuatorService;
import com.iexec.scheduler.marketplace.MarketplaceService;
import xtremweb.common.*;
import xtremweb.communications.*;
import xtremweb.database.ColumnSelection;
import xtremweb.database.DBConnPoolThread;
import xtremweb.database.SQLRequest;
import xtremweb.database.SQLRequestReadable;
import xtremweb.database.SQLRequestWorkStatus;
import xtremweb.security.X509Proxy;
import xtremweb.security.XWAccessRights;

/**
 * DBInterface.java Created: Sun Feb 25 22:06:12 2001
 *
 * @author Oleg Lodygensky
 */

public final class DBInterface {
	/**
	 * Configurator
	 *
	 * @since 9.0.0
	 */
	private final XWConfigurator config;

	private final Logger logger;

	/**
	 * This helps to send mail
	 *
	 * @since 9.1.0
	 */
	private EmailSender emailSender;

	/**
	 * Constants that define some fixed dirnames These are not considered as
	 * "hardcoded", but a part of the specification.
	 */

	@SuppressWarnings("unused")
	private final int statCounter = 0;

	/** This aims to recover works when a server has crashed */
	@SuppressWarnings("unused")
	private final boolean recoverWorks = true;

	private static DBInterface instance = null;

	public static DBInterface getInstance() {
		return instance;
	}

	MileStone mileStone;

	/**
	 * This is the database connection
	 */
	private final DBConnPoolThread dbConnPool;

	/**
	 * This inserts an object to both DB and cache
	 *
	 * @throws IOException on error
	 * @since 9.0.0
	 */
	private <T extends Table> void insert(final T row) throws IOException, URISyntaxException {
		DBConnPoolThread.getInstance().insert(row);
	}

	/**
	 * This retrieves an object interface from cache
	 *
	 * @since 7.4.0
	 */
	private Table getFromCache(final URI uri) {
		return DBConnPoolThread.getInstance().getFromCache(uri);
	}

//	/**
//	 * This retrieves an object from cache
//	 *
//	 * @since 8.2.0
//	 */
//	private <T extends Table> T getFromCache(final URI uri, final T row) {
//		return DBConnPoolThread.getInstance().getFromCache(uri, row);
//	}
//
//	/**
//	 * This retrieves an object from cache
//	 *
//	 * @since 7.4.0
//	 */
//	private <T extends Table> T getFromCache(final UID uid, final T row) {
//        return DBConnPoolThread.getInstance().getFromCache(uid, row);
//	}

	/**
	 * This retrieves an object interface from cache
	 * @param theClient represents the user; authentication is not checked here and must have been check by the caller
	 * @param uri is the uri of the object to retrieve
	 * @since 7.4.0
	 */
	private Table getFromCache(final UserInterface theClient, final URI uri) throws IOException, AccessControlException {

		final Table ret = getFromCache(uri);

		if (ret == null) {
			return null;
		}

		final UserInterface owner = user(ret.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());

		final boolean accessdenied = !ret.canRead(theClient, ownerGroup)
				&& theClient.getRights().lowerThan(UserRightEnum.ADVANCED_USER);

		if (accessdenied) {
			throw new AccessControlException(theClient.getLogin() + " can't access " + uri);
		}
		return ret;
	}

	/**
	 * This retrieves an object from cache
	 *
	 * @since 8.2.0
	 */
	private <T extends Table> T getFromCache(final UserInterface u, final UID uid, final T row)
			throws IOException, AccessControlException {
        return DBConnPoolThread.getInstance().getFromCache(u, uid, row);
	}

	/**
	 * This removes an object from cache
	 *
	 * @since 7.4.0
	 */
	private void removeFromCache(final Table row) {
		try {
			removeFromCache(row.getUID());
		} catch (final Exception e) {
			logger.exception("can't remove from cache", e);
		}
	}

	/**
	 * This removes an object from cache
	 *
	 * @since 7.4.0
	 */
	private void removeFromCache(final UID uid) {
        DBConnPoolThread.getInstance().removeFromCache(uid);
	}

//	/**
//	 * This removes an object from cache
//	 *
//	 * @since 7.4.0
//	 */
//	private void removeFromCache(final URI uri) throws IOException {
//        DBConnPoolThread.getInstance().removeFromCache(uri);
//	}

	/**
	 * This instantiates a DBConnPoolThread, update application pools and set
	 * default SQLRequest attributes
	 *
	 * @see xtremweb.database.DBConnPoolThread
	 * @see xtremweb.database.SQLRequest#setDbName(String)
	 * @see xtremweb.database.SQLRequest#setHsqldb(boolean)
	 */
	public DBInterface(final XWConfigurator c) throws IOException {
		logger = new Logger(this);
		mileStone = new MileStone(this.getClass());
		config = c;
		dbConnPool = new DBConnPoolThread(config);
		dbConnPool.start();

		emailSender = new EmailSender();

		updateAppsPool();

		final String tracesDirName = getTracesPath();

		final File d = new File(tracesDirName);
		if ((!d.exists()) && (!d.mkdirs())) {
			logger.warn("Cannot mkdirs " + d);
		}

		instance = this;
		SQLRequest.setDbName(config.getProperty(XWPropertyDefs.DBNAME));
	}

	/**
	 * This sends an email the theClient
	 *
	 * @since 9.1.0
	 * @param theClient
	 *            is the user modifying/deleting an object
	 * @param row
	 *            is the modified/deleted row
	 * @param msg
	 *            is the email message
	 */
	private <T extends Table> void sendMail(final UserInterface theClient, final T row, final String msg) {
		if (theClient == null) {
			logger.warn("can't send mail : client is null");
			return;
		}
		try {
			final String to = theClient.getEMail();
			if ((to != null) && (to.length() > 1)) {
				emailSender.send("XtremWeb-HEP@" + XWTools.getLocalHostName() + " : " + row.getUID(),
						theClient.getEMail(),
						msg + "\n" + row.toString(false, true) + "\n\n" + "https://" + XWTools.getLocalHostName() + ":"
								+ System.getProperty(XWPropertyDefs.HTTPSPORT.toString()) + "/get/" + row.getUID());
			}
		} catch (MessagingException e) {
			logger.exception(e);
		}
	}

	/**
	 * This inserts missing element from src to dest
	 * @param src is the source
	 * @param dest is the destination
	 * @return dest with missing elements from src
	 */
	private Collection<UID> mergeCollections(final Collection<UID> src, final Collection<UID> dest) {
		if((src == null) || (dest == null)) {
			return dest ;
		}
		for (final UID uid : src) {
			if(!dest.contains(uid)){
				dest.add(uid);
			}
		}
		return dest;
	}
	/**
	 * This retrieves all rows from table and caches them This forces DB read
	 *
	 * @param row
	 *            defines the row type
	 * @return the first found row, or null if not found
	 */
	protected <T extends Table> T select(final T row) throws IOException {
		return select(row, (String) null);
	}

	/**
	 * This retrieves a row from DB by its UID
	 *
	 * @param row
	 *            defines the row type
	 * @param uid
	 *            is the UID of the row to retrieve
	 * @return the first found row, or null if not found
	 */
	protected <T extends Table> T select(final T row, final UID uid) throws IOException {
		if (uid == null) {
			return null;
		}
		return select(row, SQLRequest.MAINTABLEALIAS + ".UID='" + uid.toString() + "'");
	}

	/**
	 * This retrieves all rows from table and caches them This forces DB read
	 *
	 * @param row
	 *            defines the row type
	 * @param criterias
	 *            is the SQL criterias
	 * @return the first found row, or null if not found
	 */
	protected <T extends Table> T select(final T row, final String criterias) throws IOException {
		return selectOne(row, criterias);
	}

	/**
	 * This retrieves rows from DB
	 *
	 * @param row
	 *            defines the row type
	 * @return a collection of TableInterface
	 * @since 5.8.0
	 */
	private <T extends Table> Collection<T> selectAll(final T row) throws IOException {
		return selectAll(row, null);
	}

	/**
	 * This contains accessrights&amp; + XWAccessRights.OTHERREAD_INT + &gt;0;
	 *
	 * @since 7.0.0
	 */
	private final String publicConditions = "accessrights&" + XWAccessRights.OTHERREAD_INT + ">0";

	/**
	 * This retrieves rows from DB that are "anonymously" readable (i.e.
	 * row.accessrights &amp; XWAccessRights.OTHERREAD_INT &gt; 0)
	 *
	 * @param row
	 *            defines the row type
	 * @return a Collection of public rows
	 * @since 7.0.0
	 */
	 <T extends Table> Collection<T> selectAllPublic(final T row) throws IOException {
		if (row == null) {
			logger.warn("selectAll : row is null ?!?!");
			return null;
		}
		return selectAll(row, publicConditions);
	}

	/**
	 * This retrieves rows from DB
	 *
	 * @param row
	 *            defines the row type
	 * @param conditions
	 *            restrict selected rows
	 * @return a Collection of found rows
	 * @since 5.8.0
	 */
	private <T extends Table> Collection<T> selectAll(final T row, final String conditions) throws IOException {
		if (row == null) {
			logger.warn("selectAll : row is null ?!?!");
			return null;
		}

		logger.finest("selectAll : " + row.getClass().getName() + ", " + conditions);

		return DBConnPoolThread.getInstance().selectAll(row, conditions);
	}

    /**
     * This retrieves an object from cache or from DB, bypassing access rights
     *
     * @param uid
     *            is the UID of the host to retrieve
     * @since 13.0.0
     */
    protected <T extends Table> T object(final T t, final UID uid) throws IOException {
		return DBConnPoolThread.getInstance().object(t, uid);
    }
    /**
     * This creates a new readable object to retrieve from DB
     * @param t is the object type
     * @param u
     *            is the requesting user
     * @since 13.0.0
     */
    private <T extends Table> T readableObject(final T t, final UserInterface u) throws IOException {
        final SQLRequestReadable r = new SQLRequestReadable(t.tableName(), u, ColumnSelection.selectAll);
        t.setRequest(r);
        return t;
    }
    /**
     * This creates a new readable object to retrieve from DB
     * @param t is the object type
     * @param u
     *            is the requesting user
     * @param uid
     *            is the UID of the application to retrieve
     * @since 13.0.0
     * @return the object which uid is provided; null if uid is null
     */
    private <T extends Table> T readableObject(final T t, final UserInterface u, final UID uid) throws IOException {
        if (uid == null) {
            return null;
        }
        final SQLRequestReadable r = new SQLRequestReadable(t.tableName(), u, ColumnSelection.selectAll, uid);
        t.setRequest(r);
        return t;
    }
    /**
     * This creates a new readable object to retrieve apps UID from DB
     * @param t is the object type
     * @param u
     *            is the requesting user
     * @since 5.8.0
     */
    private <T extends Table> T readableObjectUID(final T t, final UserInterface u) throws IOException {
        final SQLRequestReadable r = new SQLRequestReadable(t.tableName(), u, ColumnSelection.selectUID);
        t.setRequest(r);
        return t;
    }

	/**
	 * This retrieves UIDs from DB
	 *
	 * @param row
	 *            defines the row type
	 * @return a Collection of found UID
	 * @since 5.8.0
	 */
	private <T extends Table> Collection<UID> selectUID(final T row) throws IOException {
		return selectUID(row, null);
	}

	/**
	 * This retrieves UIDs from DB
	 *
	 * @param row
	 *            defines the row type
	 * @param conditions
	 *            restrict selected rows
	 * @return a Collection of found UID
	 * @since 5.8.0
	 */
	private <T extends Table> Collection<UID> selectUID(final T row, final String conditions) throws IOException {
		if (row == null) {
			logger.warn("selectUID : row is null ?!?!");
			return null;
		}

		logger.finest("selectUID : " + row.getClass().getName() + ", " + conditions);
		return DBConnPoolThread.getInstance().selectUID(row, conditions);
	}

	/**
	 * This retrieves first row
	 *
	 * @param row
	 *            defines the row type
	 * @return the first found row, or null
	 * @since 5.8.0
	 */
	private <T extends Table> T selectOne(final T row) throws IOException {
		return selectOne(row,  null);
	}

	/**
	 * This retrieves one row from DB
	 *
	 * @param row
	 *            defines the row type
	 * @param conditions
	 *            restrict selected rows
	 * @return a Collection of found rows
	 * @since 10.0.0
	 */
	 <T extends Table> T selectOne(final T row, final String conditions) throws IOException {
		if (row == null) {
			logger.warn("selectOne : row is null ?!?!");
			return null;
		}

		logger.finest("selectOne : " + row.getClass().getName() + ", " + conditions);

		return DBConnPoolThread.getInstance().selectOne(row, conditions);
	}

	/**
	 * This deletes a row in DB;
	 *
	 * @param theClient
	 *            is the requesting client
	 * @param row
	 *            is the row to delete
	 * @since 5.8.0
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception AccessControlException
	 *                is thrown if the client does not have sufficient rights to
	 *                delete
	 */
	private <T extends Table> boolean delete(final UserInterface theClient, final T row)
			throws AccessControlException, IOException {

		if (row == null) {
			return false;
		}

		final UserInterface owner = user(row.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());

		if (!row.canWrite(theClient, ownerGroup) && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			if (config.getAdminUid() != null) {
				final UserInterface admin = user(config.getAdminUid());
				sendMail(admin, row, theClient.getLogin() + " can't delete");
			}
			throw new AccessControlException("delete(row) " + theClient.getLogin() + " can't delete " + row.getUID());
		}

		// sendMail(owner, row, theClient.getLogin() + " has deleted");
		row.delete();
		removeFromCache(row);

		return true;
	}

	/**
	 * This checks access rights and eventually deletes a row in DB by calling
	 * row.delete()
	 *
	 * @param theClient
	 *            is the requesting client
	 * @param row
	 *            is the row to delete
	 * @since 5.8.0
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception AccessControlException
	 *                is thrown if the client does not have sufficient rights to
	 *                delete
	 */
	private boolean delete(final UserInterface theClient, final DataInterface row)
			throws IOException, AccessControlException {

		if (row == null) {
			return false;
		}

		final UserInterface owner = user(row.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());

		if (!row.canWrite(theClient, ownerGroup) && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			throw new AccessControlException("delete(data) " + theClient.getLogin() + " can't delete " + row.getUID());
		}
		logger.debug("DBInterface#delete dec links " + row.toXml());
		row.decLinks();
		update(theClient, UserRightEnum.DELETEDATA, row);

		return true;
	}

	/**
	 * This updates a row in DB and cache; this does not check access rights
	 *
	 * @param row
	 *            is the row to update
	 * @since 5.8.0
	 * @see xtremweb.common.UserRightEnum
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception AccessControlException
	 *                is thrown if the client does not have sufficient rights to
	 *                update
	 */
	synchronized protected <T extends Table> void update(final T row) throws IOException, AccessControlException {
		DBConnPoolThread.getInstance().update(row);
	}

	/**
	 * This updates a row in DB
	 *
	 * @param rows
	 *            is the vector of rows to update
	 * @since 5.8.0
	 * @see xtremweb.common.UserRightEnum
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception AccessControlException
	 *                is thrown if the client does not have sufficient rights to
	 *                update
	 */
	synchronized protected <T extends Table> void update(final Collection<T> rows)
			throws IOException, AccessControlException {
		DBConnPoolThread.getInstance().update(rows);
	}

	/**
	 * This updates a row in DB;
	 *
	 * @param theClient
	 *            is the requesting client
	 * @param urights
	 *            is the expected rights to update (INSERTJOB, INSERTDATA etc)
	 * @param row
	 *            is the row to update
	 * @since 5.8.0
	 * @see xtremweb.common.UserRightEnum
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception AccessControlException
	 *                is thrown if the client does not have sufficient rights to
	 *                update
	 */
	private <T extends Table> void update(final UserInterface theClient, final UserRightEnum urights, final T row)
			throws IOException, AccessControlException {

		final UserInterface owner = user(row.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());

		if (!row.canWrite(theClient, ownerGroup) && (theClient.getRights().lowerThan(urights))) {
			if (config.getAdminUid() != null) {
				final UserInterface admin = user(config.getAdminUid());
				sendMail(admin, row, theClient.getLogin() + " can't update");
			}
			throw new AccessControlException("update() : " + theClient.getLogin() + " can't update " + row.toXml());
		}
		DBConnPoolThread.getInstance().update(row);
	}

	/**
	 * This creates a new readable application to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private AppInterface readableApp(final UserInterface u) throws IOException {
        return readableObject(new AppInterface(), u);
	}

	/**
	 * This creates a new readable application to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the application to retrieve
	 * @since 5.8.0
	 * @return the application which uid is provided; null if uid is null
	 */
	private AppInterface readableApp(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new AppInterface(), u, uid);
	}

	/**
	 * This creates a new readable application to retrieve apps UID form DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private AppInterface readableAppUID(final UserInterface u) throws IOException {
        return readableObjectUID(new AppInterface(), u);
	}
    /**
     * This retrieves an application from cache or from DB, bypassing access rights
     *
     * @param uid
     *            is the UID of the host to retrieve
     * @since 8.0.0
     */
    protected AppInterface app(final UID uid) throws IOException {
        return object(new AppInterface(), uid);
    }

	/**
	 * This retrieves an application from cache or from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the application to retrieve
	 * @since 5.8.0
	 * @return the application which uid is provided; null if uid is null
	 */
	public AppInterface app(final UserInterface u, final UID uid) throws IOException, AccessControlException {
		if (uid == null) {
			return null;
		}
		final AppInterface row = new AppInterface();
		AppInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final AppInterface readableRow = readableApp(u, uid);
		ret = select(readableRow);
		return ret;
	}
	/**
	 * This retrieves an application from cache or from DB
	 *
	 * @param command is the command to execute
	 * @since 5.8.0
	 * @return the application which uid is provided; null if uid is null
	 * @throws InvalidKeyException on authn/authz error
	 */
	public AppInterface app(final XMLRPCCommand command) throws IOException, InvalidKeyException {
		final UID uid = command.getURI().getUID();
		return app(command, uid);
	}
	/**
	 * This retrieves an application from cache or from DB
	 */
	public AppInterface app(final XMLRPCCommand command, final UID uid) throws IOException, InvalidKeyException {

		if (uid == null) {
			return null;
		}

		final AppInterface row = new AppInterface();
		try {
			final UserInterface mandatingClient = checkClient(command, UserRightEnum.GETAPP);
			final AppInterface ret = getFromCache(mandatingClient, uid, row);
			if (ret != null) {
				return ret;
			}
			final AppInterface readableRow = readableApp(mandatingClient, uid);
			final AppInterface app = select(readableRow);
			if(command.isMandated() && (app == null)) {
				throw new AccessControlException("maybe mandated?");
			}
			return app;
		}
		catch(final IOException | AccessControlException e) {
			if (!command.isMandated()) {
				throw new AccessControlException("not mandated");
			}
			final UserInterface mandatedClient = checkMandatedClient(command);
			final AppInterface ret = getFromCache(mandatedClient, uid, row);
			if (ret != null) {
				return ret;
			}
			final AppInterface readableRow = readableApp(mandatedClient, uid);
			return select(readableRow);
		}
	}

	/**
	 * This retrieves an app accordingly to conditions
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 5.8.0
	 */
	public AppInterface app(final UserInterface u, final String conditions) throws IOException {
		final AppInterface row = readableApp(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves all applications without any conditions
	 *
	 * @return a Collection of applications
	 * @since 5.8.0
	 */
	private Collection<AppInterface> apps() throws IOException {
		final AppInterface row = new AppInterface();
		return selectAll(row);
	}

	/**
	 * This retrieves UID of readable applications for the given user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	private Collection<UID> appsUID(final UserInterface u) throws IOException {
		final AppInterface row = readableAppUID(u);
		return selectUID(row);
	}

	/**
	 * This creates a new readable data to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the data to retrieve
	 * @since 5.8.0
	 */
	private DataInterface readableData(final UserInterface u, final UID uid) throws IOException {
	    return readableObject(new DataInterface(), u, uid);
	}

	/**
	 * This creates a new readable data to retrieve datas UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private DataInterface readableDataUID(final UserInterface u) throws IOException {
        return readableObjectUID(new DataInterface(), u);
	}

    /**
     * This retrieves a data; it first look in cache, then in DB. Access rights
     * are bypassed
     *
     * @param uri
     *            is the URI of the data to retrieve
     * @since 5.8.0
     */
    protected DataInterface data(final URI uri) throws IOException, AccessControlException {
        if(uri == null) return null;

        return object(new DataInterface(), uri.getUID());
    }
    /**
     * This retrieves a data; it first look in cache, then in DB. Access rights
     * are bypassed
     *
     * @param uid
     *            is the UID of the data to retrieve
     * @since 5.8.0
     */
    protected DataInterface data(final UID uid) throws IOException, AccessControlException {
        return object(new DataInterface(), uid);
    }

	/**
	 * This retrieves a data for the requesting user. It first look in cache,
	 * then in DB. Data access rights are checked.
	 *
	 * @param command is the command to execute
	 * @throws InvalidKeyException on authn/authz error
	 * @since 5.8.0
	 */
	protected DataInterface data(final XMLRPCCommand command) throws IOException, AccessControlException, InvalidKeyException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETDATA);
		final UID uid = command.getURI().getUID();
		final DataInterface row = new DataInterface();
		final DataInterface ret = getFromCache(theClient, uid, row);
		if (ret != null) {
			return ret;
		}

		final DataInterface readableRow = readableData(theClient, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves a set of data UID for the requesting user. Data access
	 * rights are checked.
	 *
	 * @param user
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	private Collection<UID> datasUID(final UserInterface user) throws IOException {
		final DataInterface row = readableDataUID(user);
		return selectUID(row);
	}

	/**
	 * This retrieves a set of UID of data owned by the requesting user.
	 *
	 * @param user
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 7.0.0
	 */
	private Collection<UID> ownerDatasUID(final UserInterface user) throws IOException {
		final DataInterface row = readableDataUID(user);
		return selectUID(row, "maintable.owneruid='" + user.getUID() + "'");
	}

	/**
	 * This retrieves the data cache size
	 *
	 * @since 5.8.0
	 */
	public int dataSize(final UserInterface user) throws IOException {
		final Collection<UID> datasuid = datasUID(user);
		if(datasuid != null) {
			return datasuid.size();
		}
		return 0;
	}

    /**
     * This creates a new readable category to retrieve from DB
     *
     * @param u
     *            is the requesting user
     * @since 13.0.0
     */
    private CategoryInterface readableCategory(final UserInterface u) throws IOException {
        return readableObject(new CategoryInterface(), u);
    }
    /**
     * This creates a new readable category to retrieve from DB
     *
     * @param u
     *            is the requesting user
     * @param uid
     *            is the UID of the category to retrieve
     * @since 13.0.0
     */
    private CategoryInterface readableCategory(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new CategoryInterface(), u, uid);
    }

    /**
     * This creates a new readable category to retrieve category UID from DB
     *
     * @param u
     *            is the requesting user
     * @since 13.0.0
     */
    private CategoryInterface readableCategoryUID(final UserInterface u) throws IOException {
        return readableObjectUID(new CategoryInterface(), u);
    }

    /**
     * This retrieves a category for the requesting user. category access rights
     * are checked.
     *
     * @param u
     *            is the requesting user
     * @param uid
     *            is the UID of the category to retrieve
     * @since 13.0.0
     */
    protected CategoryInterface category(final UserInterface u, final UID uid)
            throws IOException, AccessControlException {

        if (uid == null) {
            return null;
        }
        final CategoryInterface row = new CategoryInterface();
        final CategoryInterface ret = getFromCache(u, uid, row);
        if (ret != null) {
            return ret;
        }
        final CategoryInterface readableRow = readableCategory(u, uid);
        return select(readableRow);
    }

	/**
	 * This retrieves a category
	 *
	 * @param command is the requesting command
	 * @return a category interface
	 * @since 13.0.0
	 */
	protected CategoryInterface category(final XMLRPCCommand command) throws InvalidKeyException, IOException, AccessControlException {
		final UID uid = command.getURI().getUID();
		if (uid == null) {
			return null;
		}

		final CategoryInterface row = new CategoryInterface();
		final UserInterface mandatingClient = checkClient(command, UserRightEnum.GETCATEGORY);
		final CategoryInterface ret = getFromCache(mandatingClient, uid, row);
		if (ret != null) {
			return ret;
		}
		final CategoryInterface readableRow = readableCategory(mandatingClient, uid);
		return select(readableRow);
	}
    /**
     * This retrieves a category from DB by its ID
     *
     * @param u
     *            is the requesting user
     * @param id is the category ID
     * @return the last loaded row
     * @since 13.0.0
     */
    protected CategoryInterface category(final UserInterface u, final long id) throws IOException {
        final CategoryInterface row = readableCategory(u);
        return selectOne(row, "maintable." + CategoryInterface.Columns.CATEGORYID.toString()+ "='" + id + "'");
    }

    /**
     * This retrieves a category from DB for the requesting user according to
     * conditions
     *
     * @param u
     *            is the requesting user
     * @param conditions
     *            restrict selected rows
     * @return the last loaded row
     * @since 13.0.0
     */
    protected CategoryInterface category(final UserInterface u, final String conditions) throws IOException {
        final CategoryInterface row = readableCategory(u);
        return selectOne(row, conditions);
    }

    /**
     * This retrieves a category from DB for the requesting user
     *
     * @param u
     *            is the requesting user
     * @return a Collection of category
     * @since 13.0.0
     */
    protected Collection<CategoryInterface> categories(final UserInterface u) throws IOException {
        final CategoryInterface row = readableCategory(u);
        return selectAll(row);
    }

    /**
     * This retrieves a category from DB for the requesting user according to
     * criteria
     *
     * @param u
     *            is the requesting user
     * @param criteria
     *            restrict selected rows
     * @return a Collection of category
     * @since 13.0.0
     */
    protected Collection<CategoryInterface> categories(final UserInterface u, final String criteria) throws IOException {
        final CategoryInterface row = readableCategory(u);
        return selectAll(row, criteria);
    }

    /**
     * This retrieves category UID from DB for the requesting user
     *
     * @param u
     *            is the requesting user
     * @return a Collection of UID
     * @since 13.0.0
     */
    private Collection<UID> categoriesUID(final UserInterface u) throws IOException {
        return categoriesUID(u, null);
    }

    /**
     * This retrieves a category from DB for the requesting user according to
     * criteria
     *
     * @param u
     *            is the requesting user
     * @return a Collection of UID
     * @since 13.0.0
     */
    private Collection<UID> categoriesUID(final UserInterface u, final String criterias) throws IOException {
        final CategoryInterface row = readableCategoryUID(u);
        return selectUID(row, criterias);
    }

    /**
     * This retrieves the number of category
     *
     * @param u
     *            is the requesting user
     * @return how many category exist
     * @since 13.0.0
     */
    protected int categorieSize(final UserInterface u)  {
        try {
            categoriesUID(u).size();
        } catch (final Exception e) {
        }
        return 0;
    }

	/**
	 * This creates a new readable market order to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 13.1.0
	 */
	private MarketOrderInterface readableMarketOrder(final UserInterface u) throws IOException {
		return readableObject(new MarketOrderInterface(), u);
	}
	/**
	 * This creates a new readable market order to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the market order to retrieve
	 * @since 13.1.0
	 */
	private MarketOrderInterface readableMarketOrder(final UserInterface u, final UID uid) throws IOException {
		return readableObject(new MarketOrderInterface(), u, uid);
	}

	/**
	 * This creates a new readable market order to retrieve market order UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 13.1.0
	 */
	private MarketOrderInterface readableMarketOrderUID(final UserInterface u) throws IOException {
		return readableObjectUID(new MarketOrderInterface(), u);
	}

	/**
	 * This retrieves a market order for the requesting user. readable market access rights
	 * are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the readable market to retrieve
	 * @since 13.1.0
	 */
	private MarketOrderInterface marketOrder(final UserInterface u, final UID uid)
			throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final MarketOrderInterface row = new MarketOrderInterface();
		final MarketOrderInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final MarketOrderInterface readableRow = readableMarketOrder(u, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves a market order
	 *
	 * @param command is the command to execute
	 * @return a market order interface
	 * @since 13.1.0
	 */
	private MarketOrderInterface marketOrder(final XMLRPCCommand command) throws InvalidKeyException, IOException, AccessControlException {
		final UID uid = command.getURI().getUID();
		if (uid == null) {
			return null;
		}

		final MarketOrderInterface row = new MarketOrderInterface();
		final UserInterface mandatingClient = checkClient(command, UserRightEnum.GETMARKETORDER);
		final MarketOrderInterface ret = getFromCache(mandatingClient, uid, row);
		if (ret != null) {
			return ret;
		}
		final MarketOrderInterface readableRow = readableMarketOrder(mandatingClient, uid);
		return select(readableRow);
	}
	/**
	 * This retrieves a market order from DB for the requesting user according to
	 * conditions
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 13.1.0
	 */
    protected MarketOrderInterface marketOrder(final UserInterface u, final String conditions) throws IOException {
        final MarketOrderInterface row = readableMarketOrder(u);
        return selectOne(row, conditions);
    }
    /**
     * This retrieves a market order without computing resource, bypassing access rights
     * @param workerPoolAddr is the address of the worker pool
     * @since 13.1.0
     * @see #marketOrderUnsatisfied(String)
     */
    private MarketOrderInterface marketOrderHavingNoResource(final String workerPoolAddr) throws IOException {

        if(workerPoolAddr == null) {
            return null;
        }

        return marketOrderUnsatisfied(SQLRequest.MAINTABLEALIAS + "." + MarketOrderInterface .Columns.NBWORKERS + "<"
                + MarketOrderInterface .Columns.EXPECTEDWORKERS
                + " AND " + MarketOrderInterface .Columns.NBWORKERS + "=0"
                + " AND " + MarketOrderInterface .Columns.WORKERPOOLADDR + "='" + workerPoolAddr + "'"
                + " AND " + MarketOrderInterface .Columns.STATUS + "!='" + StatusEnum.FINALIZING + "'"
                + " AND " + MarketOrderInterface .Columns.STATUS + "!='" + StatusEnum.ERROR + "'"
                + " AND " + MarketOrderInterface .Columns.STATUS + "!='" + StatusEnum.COMPLETED + "'");
    }
    /**
     * This retrieves a market order already having but lacking computing resources, bypassing access rights
     * @param workerPoolAddr is the address of the worker pool
     * @since 13.1.0
     * @see #marketOrderUnsatisfied(String)
     */
    private MarketOrderInterface marketOrderLackingResources(final String workerPoolAddr) throws IOException {

        if(workerPoolAddr == null) {
            return null;
        }

        return marketOrderUnsatisfied(SQLRequest.MAINTABLEALIAS + "." + MarketOrderInterface .Columns.NBWORKERS + "<"
                + MarketOrderInterface .Columns.EXPECTEDWORKERS
                + " AND " + MarketOrderInterface .Columns.NBWORKERS + ">0"
                + " AND " + MarketOrderInterface .Columns.WORKERPOOLADDR + "='" + workerPoolAddr + "'"
                + " AND " + MarketOrderInterface .Columns.STATUS + "!='" + StatusEnum.FINALIZING + "'"
                + " AND " + MarketOrderInterface .Columns.STATUS + "!='" + StatusEnum.ERROR + "'"
                + " AND " + MarketOrderInterface .Columns.STATUS + "!='" + StatusEnum.COMPLETED + "'");
    }
    /**
     * This retrieves a market order already having computing resources but not enough, bypassing access rights.
     * The idea is to avoid dead lock on computing resources.
     * @since 13.1.0
     * @see HashTaskSet#detectAbortedTasks()
     */
     Collection<MarketOrderInterface> marketOrderLockingResources() throws IOException {
        final MarketOrderInterface row = new MarketOrderInterface();
        final int aliveTimeout = Integer.parseInt(Dispatcher.getConfig().getProperty(XWPropertyDefs.ALIVETIMEOUT.toString()));

        return selectAll(row,SQLRequest.MAINTABLEALIAS + "." + MarketOrderInterface .Columns.NBWORKERS + "<"
                + MarketOrderInterface .Columns.EXPECTEDWORKERS
                + " AND " + MarketOrderInterface .Columns.NBWORKERS + ">0"
                + " AND date_add(" + MarketOrderInterface.Columns.ARRIVALDATE + ", interval " + aliveTimeout + " second) < now()"
                + " AND " + MarketOrderInterface .Columns.STATUS + "='" + StatusEnum.WAITING + "'");
    }
    /**
     * This retrieves a market order from DB according to request, bypassing access rights
     * @param request is the SQL request
     * @since 13.1.0
     */
    private MarketOrderInterface marketOrderUnsatisfied(final String request) throws IOException {
        return selectOne(new MarketOrderInterface(), request);
    }
    /**
     * This retrieves a market order starving computing resources, bypassing access rights
     * This permits to avoid dead lock in computing resource reservation.
     * @param workerPoolAddr is the address of the worker pool
     * @since 13.1.0
     * @see #marketOrderLackingResources(String)
     * @see #marketOrderHavingNoResource(String)
     */
    private MarketOrderInterface marketOrderStarvingResources(final String workerPoolAddr) throws IOException {
        logger.debug("marketOrderStarvingResources(" + workerPoolAddr + ")");
        MarketOrderInterface marketOrder = marketOrderLackingResources(workerPoolAddr);
        if (marketOrder == null) {
            logger.info("marketOrderStarvingResources(" + workerPoolAddr  + ") : no unsatisfied market order");
            marketOrder = marketOrderHavingNoResource(workerPoolAddr);
        }
        return marketOrder;
    }
    /**
     * This retrieves a market order by its id, bypassing access rights
     * @param idx is the market order
     * @since 13.1.0
     */
     MarketOrderInterface marketOrderByIdx(final long idx) throws IOException {
        return selectOne(new MarketOrderInterface(),
                SQLRequest.MAINTABLEALIAS + "." + MarketOrderInterface .Columns.MARKETORDERIDX + "="
                        + idx);
    }
    /**
     * This retrieves a market order, bypassing access rights
     * @since 13.1.0
     */
    protected MarketOrderInterface marketOrder() throws IOException {
        return selectOne(new MarketOrderInterface());
    }
    /**
     * This retrieves a market order by its uid, bypassing access rights
     * @param uid is the market order uid
     * @since 13.1.0
     */
     MarketOrderInterface marketOrder(final UID uid) throws IOException {
        return select(new MarketOrderInterface(), uid);
    }
	/**
	 * This retrieves a market order from DB for the requesting user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of market order
	 * @since 13.1.0
	 */
	protected Collection<MarketOrderInterface> marketOrder(final UserInterface u) throws IOException {
		final MarketOrderInterface row = readableMarketOrder(u);
		return selectAll(row);
	}

	/**
	 * This retrieves market order UID from DB for the requesting user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 13.1.0
	 */
	private Collection<UID> marketOrdersUID(final UserInterface u) throws IOException {
		return marketOrdersUID(u, null);
	}

    /**
     * This retrieves a market order from DB for the requesting user according to
     * criteria
     *
     * @param u
     *            is the requesting user
     * @return a Collection of UID
     * @since 13.1.0
     */
    private Collection<UID> marketOrdersUID(final UserInterface u, final String criterias) throws IOException {
        final MarketOrderInterface row = readableMarketOrderUID(u);
        return selectUID(row, criterias);
    }
    /**
     * This retrieves market order UIDs from DB for the requesting user according to
     * criteria
     *
     * @return a Collection of UID
     * @since 13.1.0
     */
    protected Collection<UID> marketOrdersUID() throws IOException {
        final MarketOrderInterface row = new MarketOrderInterface();
        return selectUID(row, null);
    }
    /**
     * This retrieves market orders from DB, which status is REVEALING
     *
     * @return a Collection of UID
     * @since 13.1.0
     */
    Collection<MarketOrderInterface> revealingOrFinalizingMarketOrders() throws IOException {
        final MarketOrderInterface row = new MarketOrderInterface();
        return selectAll(row,
                MarketOrderInterface .Columns.STATUS + "='" + StatusEnum.REVEALING + "' OR " +
                        MarketOrderInterface .Columns.STATUS + "='" + StatusEnum.FINALIZING + "'");
    }
    /**
     * This retrieves market orders from DB, which status is CONTRIBUTING
     *
     * @return a Collection of UID
     * @since 13.1.0
     */
    Collection<MarketOrderInterface> contributingOrContributedMarketOrders() throws IOException {
        final MarketOrderInterface row = new MarketOrderInterface();
        return selectAll(row,
                MarketOrderInterface .Columns.STATUS + "='" + StatusEnum.CONTRIBUTING + "' OR " +
                        MarketOrderInterface .Columns.STATUS + "='" + StatusEnum.CONTRIBUTED + "'");
    }
    /**
	 * This retrieves the number of market order
	 *
	 * @param u
	 *            is the requesting user
	 * @return how many market order exist
	 * @since 13.1.0
	 */
	protected int marketOrdersSize(final UserInterface u) throws IOException {
		try {
			marketOrdersUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}
	/**
	 * This retrieves all works the the given market order, bypassing access rights
	 * @param marketOrder is the market order
	 * @since 13.1.0
	 */
	Collection<WorkInterface> marketOrderWorks(final MarketOrderInterface marketOrder) throws IOException {
		if(marketOrder == null || marketOrder.getUID() == null){
			return null;
		}
		return selectAll(new WorkInterface(), "maintable.MARKETORDERUID='" + marketOrder.getUID() + "'");
	}
	/**
	 * This retrieves all PENDING works the the given market order, having a "lost" worker
     * (a worker expected but no alive)
	 * This is done bypassing access rights
	 * @since 13.1.0
	 */
	Collection<WorkInterface> marketOrderLostPendingWorks() throws IOException {
		int aliveTimeOut = Integer
				.parseInt(Dispatcher.getConfig().getProperty(XWPropertyDefs.ALIVETIMEOUT.toString()));

		final String query = "select works.* from works,hosts where not isnull(works.marketorderuid) and"
				+ " not isnull(works.expectedhostuid) and works.expectedhostuid=hosts.uid and "
				+ WorkInterface.Columns.STATUS +"='" + StatusEnum.PENDING+ "'"
				+ "and (unix_timestamp(now())-unix_timestamp(hosts.lastalive)) > " + aliveTimeOut;
		return DBConnPoolThread.getInstance().executeQuery(query, new WorkInterface());
 	}

	/**
	 * This creates a new readable group to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private GroupInterface readableGroup(final UserInterface u) throws IOException {
	    return readableObject(new GroupInterface(), u);
	}
	/**
	 * This creates a new readable group to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the group to retrieve
	 * @since 5.8.0
	 */
	private GroupInterface readableGroup(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new GroupInterface(), u);
	}
	/**
	 * This creates a new readable group to retrieve groups UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private GroupInterface readableGroupUID(final UserInterface u) throws IOException {
        return readableObjectUID(new GroupInterface(), u);
	}
	/**
	 * This retrieves a group for the requesting user. It first looks in cache,
	 * then in DB. Group access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the group to retrieve
	 * @since 5.8.0
	 */
	protected GroupInterface group(final UserInterface u, final UID uid) throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final GroupInterface row = new GroupInterface();
		GroupInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}

		final GroupInterface readableRow = readableGroup(u, uid);
		ret = select(readableRow);
		return ret;
	}

	/**
	 * This retrieves a group from DB for the requesting user, accordingly to
	 * conditions. Group access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @since 5.8.0
	 */
	public GroupInterface group(final UserInterface u, final String conditions) throws IOException {
		final GroupInterface row = readableGroup(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves groups UID from DB for the requesting user.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	private Collection<UID> groupsUID(final UserInterface u) throws IOException {
		return groupsUID(u, null);
	}

	/**
	 * This retrieves groups UID for the requesting user. Group access rights
	 * are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param criterias
	 *            is the SQL criterias
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	private Collection<UID> groupsUID(final UserInterface u, final String criterias) throws IOException {
		final GroupInterface row = readableGroupUID(u);
		return selectUID(row, criterias);
	}

	/**
	 * This retrieves the groups cache size for the given user
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	public int groupSize(final UserInterface u) throws IOException {
		try {
			groupsUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}

	/**
	 * This creates a new readable host to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private HostInterface readableHost(final UserInterface u) throws IOException {
        return readableObject(new HostInterface(), u);
	}

	/**
	 * This creates a new readable host to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the host to retrieve
	 * @since 5.8.0
	 */
	private HostInterface readableHost(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new HostInterface(), u, uid);
	}

	/**
	 * This creates a new readable host to retrieve hosts UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private HostInterface readableHostUID(final UserInterface u) throws IOException {
        return readableObjectUID(new HostInterface(), u);
	}

    /**
     * This retrieves a host, bypassing access rights
     *
     * @param uid
     *            is the UID of the host to retrieve
     * @since 5.8.0
     */
    protected HostInterface host(final UID uid) throws IOException {
        return object(new HostInterface(), uid);
    }
    /**
     * This retrieves hosts that participate to the given market order, bypassing access rights
     *
     * @param marketOrder is the market order we want to retrieve participating hosts
     * @return a collection of host interfaces
     * @since 13.1.0
     */
    protected Collection<HostInterface> hosts(final MarketOrderInterface marketOrder) throws IOException {
        if(marketOrder == null)
            return null;
        return selectAll(new HostInterface(), HostInterface.Columns.MARKETORDERUID + "='"
                + marketOrder.getUID() + "'");
    }
    /**
     * This retrieves a host, bypassing access rights
     *
     * @param wallet is the eth wallet of the host to retrieve
     * @since 13.1.0
     */
    protected HostInterface host(final EthereumWallet wallet) throws IOException {
        if ((wallet == null) || (wallet.getAddress() == null)) {
            return null;
        }
        return selectOne(new HostInterface(),
                SQLRequest.MAINTABLEALIAS + "." + HostInterface.Columns.ETHWALLETADDR + "='" + wallet.getAddress() + "'");
    }
    /**
     * This retrieves a list of hosts, given its eth wallet, bypassing access rights
     *
     * @param wallet is the eth wallet of the hosts to retrieve
     * @since 13.1.0
     */
    protected Collection<HostInterface> hosts(final EthereumWallet wallet) throws IOException {
        if((wallet == null) || (wallet.getAddress() == null)) {
            return null;
        }
        return selectAll(new HostInterface(),
                SQLRequest.MAINTABLEALIAS + "." + HostInterface.Columns.ETHWALLETADDR + "='" + wallet.getAddress() + "'");
    }
    /**
     * This retrieves a list of hosts, given its eth wallet, for the given market orderbypassing access rights
     *
     * @param wallet is the eth wallet of the hosts to retrieve
     * @param marketOrder is the PoCo market order
     * @since 13.1.0
     */
    protected Collection<HostInterface> hosts(final EthereumWallet wallet, final MarketOrderInterface marketOrder) throws IOException {
        if((wallet == null)
                || (wallet.getAddress() == null)
                || (marketOrder == null)) {
            return null;
        }
        return selectAll(new HostInterface(),
                SQLRequest.MAINTABLEALIAS + "." + HostInterface.Columns.ETHWALLETADDR +
                        "='" + wallet.getAddress() +
                        "' AND " + SQLRequest.MAINTABLEALIAS + "." + HostInterface.Columns.MARKETORDERUID +
                        "='" + marketOrder.getUID() + "'");
    }
    /**
	 * This retrieves a host for the requesting user. Host access rights are
	 * checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the host to retrieve
	 * @since 5.8.0
	 */
	protected HostInterface host(final UserInterface u, final UID uid) throws IOException {

		if (uid == null) {
			return null;
		}
		final HostInterface row = new HostInterface();
		final HostInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final HostInterface readableRow = readableHost(u, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves a host from DB for the requesting user, accordingly to
	 * conditions. Host access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 5.8.0
	 */
	public HostInterface host(final UserInterface u, final String conditions) throws IOException {
		final HostInterface row = readableHost(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves hosts fot the requesting user. Host access rights are
	 * checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of workers
	 * @since 5.8.0
	 */
	public Collection<HostInterface> hosts(final UserInterface u) throws IOException {
		final HostInterface row = readableHost(u);
		return selectAll(row);
	}

	/**
	 * This retrieves enumeration of Host from DB for the requesting user. Host
	 * access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return a vector of Host
	 * @since 5.8.0
	 */
	public Collection<HostInterface> hosts(final UserInterface u, final String conditions) throws IOException {
		final HostInterface row = readableHost(u);
		return selectAll(row, conditions);
	}

	/**
	 * This retrieves hosts UID from DB for the requesting user. Host access
	 * rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	private Collection<UID> hostsUID(final UserInterface u) throws IOException {
		return hostsUID(u, null);
	}

	/**
	 * This retrieves hosts UID from DB for the requesting user, according to
	 * conditions. Host access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return a Collection of UID
	 * @since 5.9.0
	 */
    private Collection<UID> hostsUID(final UserInterface u, final String conditions) throws IOException {
		final HostInterface row = readableHostUID(u);
		return selectUID(row, conditions);
	}

	/**
	 * This retrieves the hosts cache size
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
    private int hostSize(final UserInterface u) throws IOException {
		try {
			hostsUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}

	/**
	 * This creates a new readable session to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private SessionInterface readableSession(final UserInterface u) throws IOException {
            return readableObject(new SessionInterface(), u);
	}
	/**
	 * This creates a new readable session to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the session to retrieve
	 * @since 5.8.0
	 */
	private SessionInterface readableSession(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new SessionInterface(), u, uid);
	}

	/**
	 * This creates a new readable session to retrieve sessions UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private SessionInterface readableSessionUID(final UserInterface u) throws IOException {
        return readableObjectUID(new SessionInterface(), u);
	}

	/**
	 * This retrieves a session for the requesting user. Session access rights
	 * are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the session to retrieve
	 * @since 5.8.0
	 */
	protected SessionInterface session(final UserInterface u, final UID uid)
			throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final SessionInterface row = new SessionInterface();
		final SessionInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final SessionInterface readableRow = readableSession(u, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves a session from DB for the requesting user according to
	 * conditions. Session access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 5.8.0
	 */
	protected SessionInterface session(final UserInterface u, final String conditions) throws IOException {
		final SessionInterface row = readableSession(u);
		return selectOne(row);
	}

	/**
	 * This retrieves a session from DB for the requesting user. Session access
	 * rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of sessions
	 * @since 5.8.0
	 */
	protected Collection<SessionInterface> sessions(final UserInterface u) throws IOException {
		final SessionInterface row = readableSession(u);
		return selectAll(row);
	}

	/**
	 * This retrieves a session from DB for the requesting user according to
	 * criteria. Session access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param criteria
	 *            restrict selected rows
	 * @return a Collection of sessions
	 * @since 5.8.0
	 */
	protected Collection<SessionInterface> sessions(final UserInterface u, final String criteria) throws IOException {
		final SessionInterface row = readableSession(u);
		return selectAll(row, criteria);
	}

	/**
	 * This retrieves session UID from DB for the requesting user. Session
	 * access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	private Collection<UID> sessionsUID(final UserInterface u) throws IOException {
		return sessionsUID(u, null);
	}

	/**
	 * This retrieves a session from DB for the requesting user according to
	 * criteria. Session access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
    private Collection<UID> sessionsUID(final UserInterface u, final String criterias) throws IOException {
		final SessionInterface row = readableSessionUID(u);
		return selectUID(row, criterias);
	}

	/**
	 * This retrieves the number of sessions
	 *
	 * @param u
	 *            is the requesting user
	 * @return how many sessions exist
	 * @since 5.8.0
	 */
	protected int sessionSize(final UserInterface u) throws IOException {
		try {
			sessionsUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}

	/**
	 * This creates a new readable task to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private TaskInterface readableTask(final UserInterface u) throws IOException {
        return readableObject(new TaskInterface(), u);
	}

	/**
	 * This creates a new readable task to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the task to retrieve
	 * @since 5.8.0
	 */
	private TaskInterface readableTask(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new TaskInterface(), u, uid);
	}

	/**
	 * This creates a new readable task to retrieve tasks UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private TaskInterface readableTaskUID(final UserInterface u) throws IOException {
        return readableObjectUID(new TaskInterface(), u);
	}

	/**
	 * This retrieves a task with its uid. This first looks in cache, then in
	 * DB. Access rights are not checked.
	 *
	 * @param uid
	 *            is the UID of the task to retrieve
	 * @return a task or null
	 * @since 5.8.0
	 */
	protected TaskInterface task(final UID uid) throws IOException {
        return object(new TaskInterface(), uid);
	}

	/**
	 * This retrieves a Collection of tasks with the status. Access rights are
	 * not checked.
	 *
	 * @param status
	 *            is the status of the task to retrieve
	 * @return a Collection of tasks or null
	 * @since 5.8.0
	 */
	protected Collection<TaskInterface> tasks(final StatusEnum status) throws IOException {
		if (status == null) {
			return null;
		}
		return tasks(TaskInterface.Columns.STATUS + "='" + status + "'");
	}

	/**
	 * This retrieves a vector of tasks for a given work Access rights are not
	 * checked.
	 *
	 * @param work
	 *            is the work we look for
	 * @return a Collection of tasks or null
	 * @since 8.0.0
	 */
	protected Collection<TaskInterface> tasks(final WorkInterface work) throws IOException {
		if ((work == null) || (work.getUID() == null)) {
			return null;
		}
		return tasks(TaskInterface.Columns.WORKUID + "='" + work.getUID() + "'");
	}

	/**
	 * This retrieves the first task for a given work and a given worker, having status nor FAILED neither ERROR.
     * Access rights are not checked.
	 *
	 * @param work
	 *            is the work we look for
	 * @param host
	 *            is the worker we look for
	 * @return a task or null
	 * @since 8.0.0
	 */
	 TaskInterface computingTask(final WorkInterface work, final HostInterface host) throws IOException {
		if ((work == null) || (work.getUID() == null) || (host == null)) {
			return null;
		}
		return task(TaskInterface.Columns.WORKUID + "='" + work.getUID() + "' and "
                + TaskInterface.Columns.HOSTUID + "='" + host.getUID() + "' and "
                + TaskInterface.Columns.STATUS + "!='" + StatusEnum.FAILED + "' and "
                + TaskInterface.Columns.STATUS + "!='" + StatusEnum.ERROR + "'");
	}

	/**
	 * This retrieves the first task for a given work and a given status
	 *
	 * @param work
	 *            is the work we look for
	 * @param status
	 *            is the status we look for
	 * @return a task or null if none
	 * @since 8.0.0
	 */
	protected TaskInterface task(final WorkInterface work, final StatusEnum status) throws IOException {
		if ((work == null) || (work.getUID() == null) || (status == null)) {
			return null;
		}
		return task(TaskInterface.Columns.WORKUID + "='" + work.getUID() + "' and " + TaskInterface.Columns.STATUS
				+ "='" + status + "'");
	}

	/**
	 * This retrieves the first task for a given work
	 *
	 * @param work
	 *            is the work we look for
	 * @return a task or null
	 * @since 8.0.0
	 */
	 TaskInterface computingTask(final WorkInterface work) throws IOException {
		if ((work == null) || (work.getUID() == null)) {
			return null;
		}
		return task(TaskInterface.Columns.WORKUID + "='" + work.getUID() + "' and "
                + TaskInterface.Columns.STATUS + "!='" + StatusEnum.FAILED + "' and "
                + TaskInterface.Columns.STATUS + "!='" + StatusEnum.ERROR + "'");
	}

	/**
	 * This retrieves a vector of tasks with the status, bypassing access
	 * rights
	 *
	 * @param criteria
	 *            is the given criteria
	 * @return a Collection of tasks or null
	 * @since 5.8.0
	 */
	protected Collection<TaskInterface> tasks(final String criteria) throws IOException {
		final TaskInterface row = new TaskInterface();
		return selectAll(row, criteria);
	}

	/**
	 * This retrieves a vector of tasks with the status, bypassing access
	 * rights
	 *
	 * @return a Collection of tasks or null
	 * @since 5.8.0
	 */
	protected Collection<TaskInterface> tasks() throws IOException {
		return tasks((String) null);
	}

	/**
	 * This retrieves a task for the requesting user. This first looks in cache,
	 * then in DB. Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the task to retrieve
	 * @return a task or null
	 * @since 5.8.0
	 */
	protected TaskInterface task(final UserInterface u, final UID uid) throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final TaskInterface row = new TaskInterface();
		final TaskInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final TaskInterface readableRow = readableTask(u, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves the first found task accordingly to conditions. Access
	 * rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the first found task
	 * @since 5.8.0
	 */
	protected TaskInterface task(final UserInterface u, final String conditions) throws IOException {
		final TaskInterface row = readableTask(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves the first found task accordingly to conditions Access
	 * rights are not checked.
	 *
	 * @param conditions
	 *            restrict selected rows
	 * @return the first found task
	 * @since 5.8.0
	 */
	protected TaskInterface task(final String conditions) throws IOException {
		final TaskInterface row = new TaskInterface();
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves tasks UID
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	 Collection<UID> tasksUID(final UserInterface u) throws IOException {
		final TaskInterface row = readableTaskUID(u);
		return selectUID(row);
	}

	/**
	 * This retrieves the amount of tasks
	 *
	 * @param u
	 *            is the requesting user
	 * @return the amount of found tasks
	 * @since 5.8.0
	 */
	protected int taskSize(final UserInterface u) throws IOException {
		try {
			tasksUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}

	/**
	 * This retrieves all tasks
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of tasks
	 * @since 5.8.0
	 */
	protected Collection<TaskInterface> tasks(final UserInterface u) throws IOException {
		final TaskInterface row = readableTask(u);
		return selectAll(row);
	}

	/**
	 * This retrieves all tasks according to criterias
	 *
	 * @param u
	 *            is the requesting user
	 * @param criteria
	 *            is the SQL criteria
	 * @return a Collection of tasks
	 * @since 5.8.0
	 */
	protected Collection<TaskInterface> tasks(final UserInterface u, final String criteria) throws IOException {
		final TaskInterface row = readableTask(u);
		return selectAll(row, criteria);
	}

	/**
	 * This creates a new readable trace to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private TraceInterface readableTrace(final UserInterface u) throws IOException {
        return readableObject(new TraceInterface(), u);
	}

	/**
	 * This creates a new readable trace to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the trace to retrieve
	 * @since 5.8.0
	 */
	private TraceInterface readableTrace(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new TraceInterface(), u, uid);
	}

	/**
	 * This creates a new readable trace to retrieve traces UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private TraceInterface readableTraceUID(final UserInterface u) throws IOException {
        return readableObjectUID(new TraceInterface(), u);
	}

	/**
	 * This retrieves a trace
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the trace to retrieve
	 * @since 5.8.0
	 */
	protected TraceInterface trace(final UserInterface u, final UID uid) throws IOException {
		if (uid == null) {
			return null;
		}
		final TraceInterface row = readableTrace(u, uid);
		return select(row);
	}

	/**
	 * This retrieves an trace accordingly to conditions
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 5.8.0
	 */
	protected TraceInterface trace(final UserInterface u, final String conditions) throws IOException {
		final TraceInterface row = readableTrace(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves enumeration of Trace stored UIDs
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of the Trace stored UIDs
	 * @since 5.8.0
	 */
	protected Collection<TraceInterface> traces(final UserInterface u) throws IOException {
		final TraceInterface row = readableTrace(u);
		return selectAll(row);
	}

	/**
	 * This retrieves traces UID
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	 Collection<UID> tracesUID(final UserInterface u) throws IOException {
		final TraceInterface row = readableTraceUID(u);
		return selectUID(row);
	}

	/**
	 * This retrieves the traces cache size
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	protected int traceSize(final UserInterface u) throws IOException {
		try {
			tracesUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}

	/**
	 * This creates a new readable usergroup to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private UserGroupInterface readableUserGroup(final UserInterface u) throws IOException {
        return readableObject(new UserGroupInterface(), u);
	}

	/**
	 * This creates a new readable usergroup to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the usergroup to retrieve
	 * @since 5.8.0
	 */
	private UserGroupInterface readableUserGroup(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new UserGroupInterface(), u, uid);
	}

	/**
	 * This creates a new readable usergroup to retrieve usergroups UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private UserGroupInterface readableUserGroupUID(final UserInterface u) throws IOException {
        return readableObjectUID(new UserGroupInterface(), u);
	}

	/**
	 * This retrieves a UserInterface group for the requesting user. This first
	 * looks in cache, then in DB. Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID
	 * @since 5.8.0
	 */
	protected UserGroupInterface usergroup(final UserInterface u, final UID uid)
			throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final UserGroupInterface row = new UserGroupInterface();
		final UserGroupInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final UserGroupInterface readableRow = readableUserGroup(u, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves a UserInterface group from DB for the requesting user.
	 * Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 5.8.0
	 */
	protected UserGroupInterface usergroup(final UserInterface u, final String conditions) throws IOException {
		final UserGroupInterface row = readableUserGroup(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves a UserInterface group. This first looks in cache, then in
	 * DB. Access rights are not checked.
	 *
	 * @param uid
	 *            is the UID
	 * @since 5.8.0
	 */
	protected UserGroupInterface usergroup(final UID uid) throws IOException {
        return object(new UserGroupInterface(), uid);
	}

	/**
	 * This retrieves UserInterface groups form DB for the requesting user.
	 * Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of the UserGroup stored UIDs
	 */
	protected Collection<UserGroupInterface> usergroups(final UserInterface u) throws IOException {
		final UserGroupInterface row = readableUserGroup(u);
		return selectAll(row);
	}

	/**
	 * This retrieves UserInterface groups form DB for the requesting user.
	 * Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	 Collection<UID> usergroupsUID(final UserInterface u) throws IOException {
		final UserGroupInterface row = readableUserGroupUID(u);
		return selectUID(row);
	}

	/**
	 * This retrieves the UserInterface groups cache size
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	protected int usergroupSize(final UserInterface u) throws IOException {
		try {
			usergroupsUID(u).size();
		} catch (final Exception e) {
		}
		return 0;
	}

	/**
	 * This creates a new readable UserInterface to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private UserInterface readableUser(final UserInterface u) throws IOException {
        return readableObject(new UserInterface(), u);
	}

	/**
	 * This creates a new readable UserInterface to retrieve from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID of the UserInterface to retrieve
	 * @since 5.8.0
	 */
	private UserInterface readableUser(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new UserInterface(), u, uid);
	}

	/**
	 * This creates a new readable UserInterface to retrieve users UID from DB
	 *
	 * @param u
	 *            is the requesting user
	 * @since 5.8.0
	 */
	private UserInterface readableUserUID(final UserInterface u) throws IOException {
        return readableObjectUID(new UserInterface(), u);
	}

	/**
	 * This retrieves an UserInterface with the given criteria, bypassing
	 * access rights
	 *
	 * @param criteria
	 *            aims to select the right user
	 * @return the found UserInterface or null
	 * @since 5.8.0
	 */
	protected UserInterface user(final String criteria) throws IOException {
		final UserInterface row = new UserInterface();
		return select(row, criteria);
	}

	/**
	 * This retrieves users for the requesting user. This first looks in cache,
	 * then in DB. Access rights are not checked.
	 *
	 * @param uid
	 *            is the UID
	 * @since 5.8.0
	 */
	protected UserInterface user(final UID uid) throws IOException {
        return object(new UserInterface(), uid);
	}

	/**
	 * This retrieves users for the requesting user. This first looks in cache,
	 * then in DB. Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param uid
	 *            is the UID
	 * @since 5.8.0
	 */
	protected UserInterface user(final UserInterface u, final UID uid) throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final UserInterface row = new UserInterface();
		final UserInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final UserInterface readableRow = readableUser(u, uid);
		return select(readableRow);
	}

	/**
	 * This retrieves an UserInterface from DB for the requesting user according
	 * to conditions. This first looks in cache, then in DB. Access rights are
	 * checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param conditions
	 *            restrict selected rows
	 * @since 5.8.0
	 */
	protected UserInterface user(final UserInterface u, final String conditions) throws IOException {
		final UserInterface row = readableUser(u);
		return select(row, conditions);
	}

	/**
	 * This retrieves users from DB for the requesting user. Access rights are
	 * checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	 Collection<UID> usersUID(final UserInterface u) throws IOException {
		return usersUID(u, (String) null);
	}

	/**
	 * This retrieves users from DB for the requesting user, according to
	 * criteria. Access rights are checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @param criterias
	 *            filters users
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	 Collection<UID> usersUID(final UserInterface u, final String criterias) throws IOException {
		final UserInterface row = readableUserUID(u);
		return selectUID(row, criterias);
	}

	/**
	 * This creates a new readable work to retrieve from DB
	 *
	 * @param u
	 *            is the requesting work
	 * @since 5.8.0
	 */
	private WorkInterface readableWork(final UserInterface u) throws IOException {
        return readableObject(new WorkInterface(), u);
	}

	/**
	 * This creates a new readable work to retrieve from DB
	 *
	 * @param u
	 *            is the requesting work
	 * @param uid
	 *            is the UID of the work to retrieve
	 * @since 5.8.0
	 */
	private WorkInterface readableWork(final UserInterface u, final UID uid) throws IOException {
        return readableObject(new WorkInterface(), u, uid);
	}

	/**
	 * This creates a new readable work to retrieve works UID from DB,
	 * eventually according to works status
	 *
	 * @param u
	 *            is the requesting work
	 * @param s
	 *            is the work status (e.g RUNNING, PENDING...)
	 * @since 8.2.0
	 */
	private WorkInterface readableWorkUID(final UserInterface u, final StatusEnum s) throws IOException {
		final SQLRequestReadable r = new SQLRequestReadable(WorkInterface.TABLENAME, u, ColumnSelection.selectUID,
				s != null ? WorkInterface.Columns.STATUS + " = '" + s + "'" : null);
		return new WorkInterface(r);
	}

	/**
	 * This calls readableWorkUID(u, null)
	 *
	 * @see #readableWorkUID(UserInterface, StatusEnum)
	 * @since 5.8.0
	 */
	private WorkInterface readableWorkUID(final UserInterface u) throws IOException {
		return readableWorkUID(u, null);
	}

	/**
	 * This retrieves a work. This first looks in cache, then in DB.
     * Access rights are bypassed.
	 *
	 * @param uid
	 *            is the UID of the task to retrieve
	 * @return a task or null
	 * @since 5.8.0
	 */
	protected WorkInterface work(final UID uid) throws IOException {
        return object(new WorkInterface(), uid);
	}

	/**
	 * This retrieves a work from DB for the requesting user, according to
	 * conditions. Access rights are checked.
	 *
	 * @param conditions
	 *            restrict selected rows
	 * @return the last loaded row
	 * @since 5.8.0
	 */
	protected WorkInterface work(final UserInterface u, final String conditions) throws IOException {
		final WorkInterface row = readableWork(u);
		return selectOne(row, conditions);
	}

	/**
	 * This retrieves works from DB for the requesting user. Access rights are
	 * checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of works
	 * @since 5.8.0
	 */
	protected Collection<WorkInterface> works(final UserInterface u) throws IOException {
		final WorkInterface row = readableWork(u);
		return selectAll(row);
	}

	/**
	 * This retrieves a work from DB for the requesting user. Access rights are
	 * checked.
	 *
	 * @param u
	 *            is the requesting user
	 * @return a vector of works
	 * @since 5.8.0
	 */
	protected WorkInterface work(final UserInterface u) throws IOException {
		final WorkInterface row = readableWork(u);
		return selectOne(row);
	}

	/**
	 * This retrieves readable works for the given user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a vector of works
	 * @since 5.8.0
	 */
	protected WorkInterface work(final UserInterface u, final UID uid) throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		final WorkInterface row = new WorkInterface();
		final WorkInterface ret = getFromCache(u, uid, row);
		if (ret != null) {
			return ret;
		}
		final WorkInterface readableRow = readableWork(u, uid);
		return select(readableRow);
	}
    /**
     * This retrieves a work given the contribution, bypassing access rights
     * @since 13.1.0
     */
    protected WorkInterface work(final WorkerPool.ContributeEventResponse contribution) {
        try {
            return selectOne(new WorkInterface(),
                    SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.WORKORDERID + "='"
                            + contribution.woid + "'");
        } catch (IOException e) {
            return null;
        }
    }
    /**
     * This retrieves a work given work order id
     * @since 13.1.0
     */
    protected WorkInterface work(final String workOrderId) {
        try {
            return selectOne(new WorkInterface(),
                    SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.WORKORDERID + "='"
                            + workOrderId + "'");
        } catch (IOException e) {
            return null;
        }
    }
	/**
	 * This retrieves readable works for the given user
	 *
	 * @param command is the command to execute
	 * @return a work interface
	 * @since 11.4.0
	 */
	protected WorkInterface work(final XMLRPCCommand command) throws InvalidKeyException, IOException, AccessControlException {
		final UID uid = command.getURI().getUID();
		if (uid == null) {
			return null;
		}

 		final WorkInterface row = new WorkInterface();
		try {
			final UserInterface mandatingClient = checkClient(command, UserRightEnum.GETJOB);
			final WorkInterface ret = getFromCache(mandatingClient, uid, row);
			if (ret != null) {
				return ret;
			}
			final WorkInterface readableRow = readableWork(mandatingClient, uid);
			final WorkInterface work = select(readableRow);
			if(command.isMandated() && (work == null)) {
				throw new AccessControlException("maybe mandated?");
			}
			return work;
		}
		catch(final IOException | AccessControlException e) {
			if (!command.isMandated()) {
				throw new AccessControlException("not mandated");
			}
			final UserInterface mandatedClient = checkMandatedClient(command);
			final WorkInterface ret = getFromCache(mandatedClient, uid, row);
			if (ret != null) {
				return ret;
			}
			final WorkInterface readableRow = readableWork(mandatedClient, uid);
			return select(readableRow);
		}
	}
	/**
	 * This retrieves a vector of tasks with the status, bypassing access
	 * rights
	 *
	 * @return a Collection of tasks or null
	 * @since 5.8.0
	 */
	protected Collection<WorkInterface> works() throws IOException {
		final WorkInterface row = new WorkInterface();
		return selectAll(row);
	}

	/**
	 * This retrieves all works with the given status, bypassing access rights
	 *
	 * @see Scheduler#retrieve()
	 * @param s
	 *            is the expected status
	 * @return a Collection of works
	 * @since 5.8.0
	 */
	public Collection<WorkInterface> works(final StatusEnum s) throws IOException {
		logger.debug("DBInterface#works(" + s + ")");
		final SQLRequestWorkStatus r = new SQLRequestWorkStatus(s);
		final WorkInterface row = new WorkInterface(r);
		return selectAll(row);
	}

	/**
	 * This retrieves all works with the given status. This is used by scheduler
	 *
	 * @see MatchingScheduler#retrieve()
	 * @param s
	 *            is the expected status
	 * @param owneruid
	 *            is the uid of the owner
	 * @return a Collection of works
	 * @since 5.8.0
	 */
	public Collection<WorkInterface> works(final StatusEnum s, final UID owneruid) throws IOException {
		if (owneruid == null) {
			return null;
		}
		final SQLRequestWorkStatus r = new SQLRequestWorkStatus(s, owneruid);
		final WorkInterface row = new WorkInterface(r);
		return selectAll(row);
	}

	/**
	 * This retrieves UID of readable works for the given user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	protected Collection<UID> worksUID(final UserInterface u) throws IOException {
		return worksUID(u, (String) null);
	}

	/**
	 * This retrieves a collection of readable work UID for the given user,
	 * given work status
	 *
	 * @param u
	 *            is the requesting user
	 * @param s
	 *            is the status of the works to retrieve
	 * @return a Collection of UID
	 * @since 8.1.2
	 */
	protected Collection<UID> worksUID(final UserInterface u, final StatusEnum s) throws IOException {
		final WorkInterface row = readableWorkUID(u, s);
		return selectUID(row);
	}

	/**
	 * This retrieves UID of readable works for the given user, according to
	 * criteria
	 *
	 * @param u
	 *            is the requesting user
	 * @param criterias
	 *            filters works
	 * @return a Collection of UID
	 * @since 5.8.0
	 */
	protected Collection<UID> worksUID(final UserInterface u, final String criterias) throws IOException {
		final WorkInterface row = readableWorkUID(u);
		return selectUID(row, criterias);
	}

	/**
	 * This retrieves works UID owned by the given user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 7.0.0
	 */
	protected Collection<UID> ownerWorksUID(final UserInterface u) throws IOException {
		final WorkInterface row = readableWorkUID(u);
		return selectUID(row, "maintable.owneruid='" + u.getUID() + "'");
	}

	/**
	 * This retrieves replica UID for the given work
	 *
	 * @param u
	 *            is the requesting user
	 * @param originalUid
	 *            is the UID of the replicated work
	 * @return a Collection of UID
	 * @since 10.0.0
	 */
	protected Collection<UID> replicasUID(final UserInterface u, final UID originalUid) throws IOException {
		final WorkInterface row = readableWorkUID(u);
		return selectUID(row, "maintable.replicateduid='" + originalUid + "'");
	}

	/**
	 * This retrieves works UID owned by the given user
	 *
	 * @param u
	 *            is the requesting user
	 * @return a Collection of UID
	 * @since 7.0.0
	 */
	protected Collection<UID> ownerWorksUID(final UserInterface u, final StatusEnum s) throws IOException {
		final WorkInterface row = readableWorkUID(u, s);
		return selectUID(row, "maintable.owneruid='" + u.getUID() + "'");
	}
	/**
	 * An user can be mandated: it can act in name of someone else.
	 * Mandate is an XMLRPCCommand attribute (MANDATINGLOGIN).
	 * This checks if this command client has MANDATED_USER user right
	 *
	 * @param command is the command to execute
	 * @return the mandated client
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws AccessControlException if no mandating user or if not a MANDATED_UER
	 * @since 11.0.0
	 * @see XMLRPCCommand#MANDATINGLOGIN
	 * @see UserRightEnum#MANDATED_USER
	 */
	protected UserInterface checkMandatedClient(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		if(!command.isMandated()) {
			throw new AccessControlException ("not mandated");
		}
		return checkClient(command.getUser(), UserRightEnum.MANDATED_USER);
	}

	/**
	 * An user can act by itself or mandate someone else.
	 * Mandate is an XMLRPCCommand attribute (MANDATINGLOGIN)
	 * 
	 * This checks if the original client has the right to act as actionLevel.
	 * The original client is this command client, if this command MANDATINGLOGIN attr is not set,
	 * or the client denoted by MANDATINGLOGIN.
	 * 
	 * @param command is the XMLRPCCommand to execute
	 * @param actionLevel is the expected user rights to execute the XMLRPCCommand
	 * @return the mandating client
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws AccessControlException if no mandating user or if not a MANDATED_UER
	 * @since 11.0.0
	 * @see XMLRPCCommand#MANDATINGLOGIN
	 * @see UserRightEnum#MANDATED_USER
	 */
	protected UserInterface checkClient(final XMLRPCCommand command, final UserRightEnum actionLevel)
			throws IOException, InvalidKeyException, AccessControlException {

		if(command.isMandated()) {
			checkMandatedClient(command);

			final UserInterface mandatingClient = user(UserInterface.Columns.LOGIN.toString() + "= '" + command.getMandatingLogin() + "'");
			return checkClient(mandatingClient, actionLevel);
		} 
		return checkClient(command.getUser(), actionLevel);
	}

	/**
	 * This tests whether client has the right to connect to this server and to
	 * do the expected action, accordingly to the action level.
	 * Since 7.0.0 two credential types are accepted : login/password (as it
	 * used to be) and also X509 certificate. The certificate must be provided
	 * in the client certificate attribute. This is accepted if and only if the
	 * Dispatcher#proxyValidator attribute is not null. The certificate is then
	 * verified by Dispatcher#proxyValidator. If the validation is successfully
	 * passed, the certificate is stored in DB so that the bridge can use it.
	 *
	 * Since 7.0.0 any UserInterface can connect using its certificate without
	 * being registered. As soon as the dispatcher can validate the
	 * UserInterface certificate against its CA certificates path. If the
	 * certificate is validated through certificates path, a new UserInterface
	 * is inserted into the database, with STANDARD_UserInterface rights and its
	 * certificate concat(subjectDN, issuerDN) as login. Keep in mind that login
	 * may be truncated; please see UserIntergace.USERLOGINLENGTH.
	 *
	 * @param client
	 *            describes the client that is connecting
	 * @param actionLevel
	 *            is the right level
	 * @return a UserInterface object if the client provides a known
	 *         (UserInterface id/password) and if that UserInterface has
	 *         sufficient privilege level; null otherwise.
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 * @see Dispatcher#proxyValidator
	 * @see xtremweb.common.UserInterface#USERLOGINLENGTH
	 */
	protected UserInterface checkClient(final UserInterface client, final UserRightEnum actionLevel)
			throws IOException, InvalidKeyException, AccessControlException {

		logger.debug("checkClient(" + client + ", " + actionLevel + ")");
		if ((client == null) || (actionLevel == null)) {
			throw new IOException("Can't check client");
		}

		logger.finest("checkClient(" + client.toXml() + ", " + actionLevel + ")");

		UserInterface result = null;

		final String certificate = client.getCertificate();

		if ((certificate != null) && (Dispatcher.getProxyValidator() == null)) {
			throw new AccessControlException("Server config : server can't check certificate");
		}
		if ((certificate != null) && (Dispatcher.getProxyValidator() != null)) {

			String subjectName = null;
			String loginName = null;

			final byte[] strb = certificate.getBytes();

			try (ByteArrayInputStream is = new ByteArrayInputStream(strb);){
				logger.finest(certificate);
				final X509Proxy proxy = new X509Proxy(is);
				Dispatcher.getProxyValidator().validate(proxy);
				subjectName = proxy.getSubjectName();
				final String issuerName = proxy.getIssuerName();

				loginName = subjectName + "_" + issuerName;
			} catch (final Exception e) {
				loginName = null;
				logger.exception("Certificate validation failure", e);
				throw new InvalidKeyException(subjectName + " : certificate validation failed");
			} finally {
				subjectName = null;
			}

			try {
				client.setLogin(loginName);
				if (client.getUID() != null) {
					result = user(client.getUID());
				}
				if (result == null) {
					result = user(UserInterface.Columns.LOGIN.toString() + "= '" + client.getLogin() + "'");
				}
				if (result == null) {
					if (config.getAdminUid() == null) {
						throw new IOException("admin.uid is not set");
					}
					final UID useruid = new UID();
					final UserInterface newUserItf = new UserInterface(useruid);
					final String random = subjectName + Math.random();
					final String shastring = XWTools.sha256(random);
					newUserItf.setRights(UserRightEnum.STANDARD_USER);
					newUserItf.setOwner(config.getAdminUid());
					newUserItf.setLogin(loginName);
					newUserItf.setPassword(shastring);
					newUserItf.setEMail("unknown");
					newUserItf.setCertificate(certificate);
					result = new UserInterface(newUserItf);
				}
			} catch (final Exception e) {
				logger.exception("Cant insert new user", e);
				throw new IOException("Cant insert new UserInterface " + loginName);
			} finally {
				loginName = null;
			}
		}

		if (result == null) {
			if ((client.getPassword() == null) || (client.getLogin() == null)) {
				result = null;
			} else {
				if (client.getUID() != null) {
					result = user(client.getUID());
				}
				if (result == null) {
					result = user(UserInterface.Columns.LOGIN.toString() + "= '" + client.getLogin() + "' AND "
							+ UserInterface.Columns.PASSWORD.toString() + "= '" + client.getPassword() + "'");
				}
				if ((result != null) && ((result.getPassword().compareTo(client.getPassword()) != 0)
						|| (result.getLogin().compareTo(client.getLogin()) != 0))) {
					result = null;
				}
			}
		}

		if (result == null) {
			throw new InvalidKeyException("unknown user or bad login/password");
		}

		if ((result.getRights() == null) || (result.getRights() == UserRightEnum.NONE)) {
			result = null;
			throw new AccessControlException(client.getLogin() + " : has no right");
		}

		if (result.getRights().lowerThan(actionLevel)) {
			result = null;
			throw new AccessControlException(client.getLogin() + " : not enough rights to " + actionLevel);
		}

		return result;
	}

	/**
	 * This removes an object from cache and DB Objects are parsed in the most
	 * probable order, to improve performances.
	 *
	 * @param command is the command to execute
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public boolean remove(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UID uid = command.getURI().getUID();
		if (uid == null) {
			return false;
		}

		final UserInterface theClient = checkClient(command, UserRightEnum.STANDARD_USER);

        if (removeMarketOrder(command)) {
            return true;
        }
        if (removeWork(command)) {
            return true;
        }
		if (removeData(theClient, uid)) {
			return true;
		}
		if (removeSession(command)) {
			return true;
		}
		if (removeGroup(command)) {
			return true;
		}
		if (removeTask(command)) {
			return true;
		}

		final AppInterface theApp = app(command, uid);
		boolean ret = false;
		if (theApp != null) {

			final UserInterface owner = user(theApp.getOwner());

			final UID ownerGroup = (owner == null ? null : owner.getGroup());

			ret = removeApplication(theClient, theApp, ownerGroup);
			if (ret) {
				logger.finest("app deleted " + uid);
				return true;
			}
		}

		if (removeUser(theClient, uid)) {
			logger.finest("UserInterface deleted " + uid);
			return true;
		}

        if (removeUserGroup(theClient, uid)) {
            logger.finest("usergroup deleted " + uid);
            return true;
        }
        if (removeMarketOrder(theClient, uid)) {
            logger.finest("market order deleted " + uid);
            return true;
        }

		throw new IOException("Object not found");
	}

	/**
	 * This calls chmod(client, uri.getUID(), chmodstr);
	 */
	public boolean chmod(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException, ParseException {

		final UserInterface theClient = checkClient(command, UserRightEnum.STANDARD_USER);

		final UID uid = ((XMLRPCCommandChmod) command).getURI().getUID();
		final String chmodstr = ((XMLRPCCommandChmod) command).getModifier().toHexString();

		if (uid == null) {
			throw new IOException("uid can't be null");
		}
		if (chmodstr == null) {
			throw new IOException("chmodstr can't be null");
		}

		Table theRow = app(command, uid);
		UserRightEnum userrights = UserRightEnum.INSERTAPP;
		if (theRow == null) {
			theRow = data(uid);
			userrights = UserRightEnum.ADVANCED_USER;
		}
		if (theRow == null) {
			theRow = usergroup(theClient, uid);
			userrights = UserRightEnum.SUPER_USER;
		}
		if (theRow == null) {
			theRow = user(theClient, uid);
			userrights = UserRightEnum.ADVANCED_USER;
		}
		if (theRow == null) {
			theRow = group(theClient, uid);
			userrights = UserRightEnum.ADVANCED_USER;
		}
		if (theRow == null) {
			theRow = session(theClient, uid);
			userrights = UserRightEnum.ADVANCED_USER;
		}
		if (theRow == null) {
			return false;
		}
		final XWAccessRights rights = theRow.getAccessRights();
		rights.chmod(chmodstr);
		theRow.setAccessRights(rights);
		update(theClient, userrights, theRow);

		return true;
	}

	/**
	 * This reads the applications table
	 */
	public final void updateAppsPool() {

		try {
			final Vector<AppInterface> apps = (Vector<AppInterface>) apps();
			if (apps == null) {
				logger.warn("Can't retrieve any app");
				return;
			}

			for (final Iterator<AppInterface> enums = apps.iterator(); enums.hasNext();) {
				final AppInterface theApp = enums.next();
				theApp.getName();
				theApp.isService();
			}
		} catch (final Exception e) {
			logger.exception(e);
		}

	}

	/**
	 * This set all server works to WAITING status and reload works
	 *
	 * @param serverName
	 *            is the server name
	 */
	public void unlockWorks(final String serverName) {
		try {
			DBConnPoolThread.getInstance().unlockWorks(serverName);
		} catch (final Exception e) {
			logger.exception(e);
		}
	}

	/**
	 * This checks if client can read data and if so, increments data links and
	 * updates the data
	 *
	 * @param theClient
	 *            is the client wishing to use data
	 * @param uri
	 *            is the uri of the data to use
	 * @return true if data can't be accessed
	 */
	private boolean useData(final UserInterface theClient, final URI uri) throws IOException, InvalidKeyException {

		final Table row = getFromCache(theClient, uri);
		if (row == null) {
			return false;
		}
		try {
			uri.getUID();
		}
		catch(final Exception e) {
			return false;
		}

		final UserInterface owner = user(row.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());

		if (!row.canRead(theClient, ownerGroup) && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			throw new AccessControlException(theClient.getLogin() + " can't use " + uri);
		}

		final DataInterface theData = data(uri.getUID());
		if(theData == null) {
			return false;
		}
		theData.incLinks();
		update(theClient, UserRightEnum.STANDARD_USER, theData);
		return true;
	}

	/**
	 * This adds/updates a data according to data access rights. If client
	 * rights is higher or equals to WORKER_USER, data access rights can be
	 * bypassed. This allows worker to store a result for users on job
	 * completion. owner may be != theClient in case of updating.
	 *
	 * @param command is the command to execute
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public void addData(final XMLRPCCommand command)
			throws	IOException, InvalidKeyException, AccessControlException,
					URISyntaxException {

		final DataInterface data = (DataInterface) command.getParameter();
		if (data == null) {
			return;
		}

		final UserInterface theClient = checkClient(command, UserRightEnum.INSERTDATA);
		final Date theDate = new java.util.Date();

		final UserInterface owner = user(data.getOwner());
		final UID ownerGroup = (owner == null) ? null : owner.getGroup();

		final DataInterface theData = data(data.getUID());
		if (theData != null) {

			if (theData.canWrite(theClient, ownerGroup)
					|| (theClient.getRights().higherOrEquals(UserRightEnum.WORKER_USER))) {

				theData.updateInterface(data);
				if (theData.getURI() == null) {
					try {
						theData.setURI(new URI(XWTools.getLocalHostName(), theData.getUID()));
					} catch (final URISyntaxException e) {
						logger.exception(e);
						logger.fatal(e.getMessage());
					}
				}
				update(theClient, UserRightEnum.INSERTDATA, theData);
			} else {
				logger.error("addData() : " + theClient.getLogin() + " can't update " + data.getName());
				throw new AccessControlException(theClient.getLogin() + " can't update " + data.getName());
			}

			return;
		}

		if (data.getUID() == null) {
			data.setUID(new UID());
		}
		if (data.getOwner() == null) {
			data.setOwner(theClient.getUID());
		}
		if (data.getAccessRights() == null) {
			data.setAccessRights(XWAccessRights.DEFAULT);
		}
		if (data.getStatus() == null) {
			data.setStatus(StatusEnum.UNAVAILABLE);
		}
		data.getAccessRights(); // this set default value, if not set
		data.setInsertionDate(theDate);
		if (data.getType() == DataTypeEnum.X509) {
			data.setAccessRights(XWAccessRights.USERREADWRITE);
		}
		try {
			data.setURI(new URI(XWTools.getLocalHostName(), data.getUID()));
		} catch (final URISyntaxException e) {
			logger.exception(e);
			logger.fatal(e.getMessage());
		}

		data.setLinks(0);

		insert(data);
	}

	/**
	 * This retrieves data UIDs for the specified client. Since 7.0.0 non
	 * privileged users get their own data only
	 *
	 * @param command is the command to execute
	 * @return null on error; a Collection of UID otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public Collection<UID> getDatas(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTDATA);

		if (theClient.getRights().higherOrEquals(UserRightEnum.ADVANCED_USER)) {
			return datasUID(theClient);
		}
		return ownerDatasUID(theClient);
	}

	/**
	 * This does not check the client integrity; this must have been done by the
	 * caller. This checks client rights is not lower than GETDATA and returns
	 * data(theClient, name)
	 *
	 * @param command is the command to execute
	 * @return the found data or null
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 * @see #data(UID)
	 */
	public DataInterface getData(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		checkClient(command, UserRightEnum.GETDATA);
		return data(command.getURI().getUID());
	}

	/**
	 * This retrieves the path where to store traces
	 *
	 * @return the path
	 */
	public final String getTracesPath() {
		return new String(config.getProperty(XWPropertyDefs.HOMEDIR) + "Traces");
	}

	/**
	 * This retrieves users
	 *
	 * @param command is the command to execute
	 * @return a Collection of users UID, null on error
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 * @since 11.0.0
	 */
	public Collection<UID> getUsers(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTUSER);
		return usersUID(theClient);
	}

	/**
	 * This does not check the client integrity; this must have been done by the
	 * caller. This checks client rights is not lower than GETUserInterface and
	 * returns user(theClient, name)
	 *
	 * @param command is the command to execute
	 * @param login
	 *            is the login of the UserInterface to retrieve
	 * @return the found user or null
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 * @see #user(UserInterface, String)
	 */
	public UserInterface getUserByLogin(final XMLRPCCommand command, final String login)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETUSER);
		return user(theClient, SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN + "='" + login + "'");
	}

	/**
	 * This checks client rights is not lower than GETUserInterface and
	 * returns user(theClient, name)
	 *
	 * @param command is the command to execute
	 * @return the found user or null
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	protected UserInterface getUser(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETUSER);
		try {
			final UID uid = command.getURI().getUID();
			final UserInterface ret = user(theClient, uid);
			if (ret == null) {
				return null;
			}
			final UserInterface itf = new UserInterface(ret);

			itf.setPassword("*****");
			itf.setCertificate("*****");
			return itf;
		} catch (IllegalArgumentException e) {
			final String login = command.getURI().getPath().substring(1);
			final UserInterface ret = getUserByLogin(command, login);
			if (ret == null) {
				return null;
			}
			final UserInterface itf = new UserInterface(ret);

			itf.setPassword("*****");
			itf.setCertificate("*****");
			return itf;
		}
	}
	/**
	 * This adds or updates a new user. Any user with user rights lower than
	 * UserRights.INSERTUSER can only update its own definition But even in this
	 * case, an user can never change its own user rights, nor its user group
	 * (otherwise it would be too easy to gain unexpected privileged access).
	 * UserRights#INSERTUSER privilege to add or to modify UserInterface
	 * definition. If the user already exists in DB, it is updated. If inserted
	 * in a group, new user access rights are its group one.
	 *
	 * @param command is the command to execute
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	protected boolean addUser(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface useritf = (UserInterface) command.getParameter();
		final UserInterface theClient = checkClient(command, UserRightEnum.INSERTUSER);
		return addUser(theClient, useritf);
	}

	/**
	 * This adds or updates a new user. Any user with user rights lower than
	 * UserRights.INSERTUSER can only update its own definition But even in this
	 * case, an user can never change its own user rights, nor its user group
	 * (otherwise it would be too easy to gain unexpected privileged access).
	 * UserRights#INSERTUSER privilege to add or to modify UserInterface
	 * definition. If the user already exists in DB, it is updated. If inserted
	 * in a group, new user access rights are its group one.
	 *
	 * @param theClient is the requesting client
	 * @param useritf
	 *            is a UserInterface object containing new user informations
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	protected boolean addUser(final UserInterface theClient, final UserInterface useritf)
			throws IOException, InvalidKeyException, AccessControlException {

		if (useritf == null) {
			throw new IOException("useritf is null");
		}

		final UID clientGroupUID = theClient.getGroup();
		final UserGroupInterface clientGroup = usergroup(theClient, clientGroupUID);
		if ((clientGroupUID != null) && (clientGroup == null)) {
			throw new IOException("can't find user group " + clientGroupUID);
		}

		final UID newuserGroupUID = theClient.getGroup();
		final UserGroupInterface newuserGroup = usergroup(theClient, newuserGroupUID);
		if ((newuserGroupUID != null) && (newuserGroup == null)) {
			throw new IOException("can't find user group " + newuserGroupUID);
		}

		if (theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			if ((clientGroupUID == null) || (newuserGroupUID == null)
					|| ((clientGroupUID != null) && (clientGroupUID.equals(newuserGroupUID) == false))) {
				throw new AccessControlException(theClient.getLogin() + "/" + clientGroupUID + " can't add users"
						+ (clientGroupUID == null ? " in no group" : " in group " + newuserGroupUID));
			}
		}

		UserInterface newUser = null;

		if (useritf.getUID() != null) {
			newUser = user(theClient, useritf.getUID());
		} else {
			useritf.setUID(new UID());
		}

		if (useritf.getOwner() == null) {
			useritf.setOwner(theClient.getUID());
		}

		XWAccessRights useraccessrights = XWAccessRights.DEFAULT;
		if (newuserGroup != null) {
			useraccessrights = newuserGroup.getAccessRights();
		}

		if (newUser == null) {
			newUser = user(theClient, SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
					+ useritf.getLogin() + "'");
		}

		if (newUser == null) {
			try {
				newUser = new UserInterface(useritf);
				insert(newUser);
			} catch (final Exception e) {
				logger.exception(theClient.getLogin() + " can't create " + useritf.getLogin(), e);
				throw new AccessControlException(theClient.getLogin() + " can't create " + useritf.getLogin());
			}
		} else {
			newUser.updateInterface(useritf);
		}

		final UserRightEnum urights = newUser.getRights();
		if ((urights == null) || (urights.higherOrEquals(UserRightEnum.ADVANCED_USER)) || urights.isWorker()){
			useraccessrights = XWAccessRights.USERALL;
		}

		if (newUser.getOwner() == null) {
			newUser.setOwner(theClient.getUID());
		}

		newUser.setAccessRights(useraccessrights);
		update(theClient, UserRightEnum.INSERTUSER, newUser);
		return true;
	}

	/**
	 * This retrieves user groups from SQL table usergroups. <br>
	 *
	 * @param command is the command to execute
	 * @return a Collection of usergroups UID, null on error
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public Collection<UID> getUserGroups(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTUSERGROUP);
		return usergroupsUID(theClient);
	}

	/**
	 * This does not check the client integrity; this must have been done by the
	 * caller. This checks client rights is not lower than GETUSERGROUP and
	 * returns usergroup(theClient, uid)
	 *
	 * @param command is the command to execute
	 * @return the found usergroup or null
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 * @see #usergroup(UserInterface, UID)
	 */
	public UserGroupInterface getUserGroup(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETUSERGROUP);
		final UID uid = command.getURI().getUID();
		return usergroup(theClient, uid);
	}

	/**
	 * This updates or adds a new user group. UserRights.SUPER_USER privilege is
	 * needed to do so.
	 *
	 * @param command is the command to execute
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public boolean addUserGroup(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException,
					URISyntaxException {

		final UserInterface theClient = checkClient(command, UserRightEnum.INSERTUSERGROUP);
		final UserGroupInterface groupitf = (UserGroupInterface) command.getParameter();
		final UID groupUid = groupitf.getUID();
		if (groupUid == null) {
			throw new IOException("addUserGroup : no UID");
		}

		if (groupitf.getOwner() == null) {
			groupitf.setOwner(theClient.getUID());
		}
		groupitf.setAccessRights(XWAccessRights.OWNERGROUP);

		final UserGroupInterface theGroupByUID = usergroup(theClient, groupUid);
		final UserGroupInterface theGroup = (theGroupByUID != null ? theGroupByUID
				: usergroup(theClient, SQLRequest.MAINTABLEALIAS + "." + UserGroupInterface.Columns.LABEL.toString()
				+ "='" + groupitf.getLabel() + "'"));

		if (theGroup == null) {
			insert(groupitf);
		}

		update(theClient, UserRightEnum.INSERTUSERGROUP, groupitf);

		return true;
	}

    /**
     * This inserts or updates a service. This should not be called from
     * communication handlers since this does not verify client identity. This
     * is to be called from the dispatcher package itself only.
     *
     * @param classname
     *            is the java class name of the service
     * @exception ClassNotFoundException
     *                is throws if classname can't be instantiated
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     */
    public void insertService(final String classname)
            throws ClassNotFoundException, IOException, InvalidKeyException, AccessControlException, URISyntaxException {

        final UserInterface admin = user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
                + config.getProperty(XWPropertyDefs.ADMINLOGIN) + "'");

        if (admin == null) {
            throw new IOException("Can't retrieve UserInterface " + config.getProperty(XWPropertyDefs.ADMINLOGIN));
        }

        final String ifname = classname.substring(0, classname.lastIndexOf('.') + 1) + "Interface";
        final Object obj = Class.forName(ifname);

        if (obj == null) {
            logger.error("DBInterface#insertService() : service '" + ifname + "'not found");
            throw new ClassNotFoundException("insertService() : service '" + ifname + "'not found");
        }

        final AppInterface app = new AppInterface(new UID());
        app.setName(classname);
        app.setService(true);
        addApp(admin, app);
    }
    /**
     * This inserts or updates a category as found from the blockchain
     * @see xtremweb.dispatcher.Dispatcher
     * @since 13.0.0
     * @param category
     *            is the category to insert
     * @exception ClassNotFoundException
     *                is throws if classname can't be instantiated
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     */
    public void insertCategory(final IexecHub.CreateCategoryEventResponse category)
            throws ClassNotFoundException, IOException, InvalidKeyException, AccessControlException, URISyntaxException {

        if (!config.blockchainEnabled()) {
            throw new IOException("blockchain access disabled");
        }

        final UserInterface admin = user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
                + config.getProperty(XWPropertyDefs.ADMINLOGIN) + "'");

        if (admin == null) {
            throw new IOException("Can't retrieve UserInterface " + config.getProperty(XWPropertyDefs.ADMINLOGIN));
        }

        final CategoryInterface catItf = new CategoryInterface(new UID());
        catItf.setCategoryId(category.catid.longValue());
        catItf.setName(category.name);
        catItf.setMaxWallClockTime(category.workClockTimeRef.intValue());
        catItf.setMaxCpuSpeed(1.0f);
        catItf.setMaxFileSize(XWTools.MAXFILESIZE);
        catItf.setMaxFreeMassStorage(XWTools.MAXDISKSIZE);
        catItf.setMaxMemory(XWTools.MAXRAMSIZE);
        addCategory(admin, catItf);
    }

	/**
	 * This adds/updates an application UserRights.SUPER_USER privilege is
	 * needed to insert a global application, an application that anybody can
	 * use and any worker can execute, an application with 0X777 as access
	 * rights. If the client don't have UserRights.SUPER_USER rights, the
	 * application is available for that user only : application access rights
	 * is forced to 0x700 which means that this user will be the only allowed to
	 * insert job, and workers belonging to this user only will be able to
	 * execute. There may be an exception if the application name already exists
	 * e.g. : an user tries to insert its own private application which name is
	 * already used.
	 *
	 * @param command is the command to execute
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public void addApp(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {
		final UserInterface theClient = checkClient(command, UserRightEnum.INSERTAPP);
		final AppInterface appitf = (AppInterface) command.getParameter();
		addApp(theClient, appitf);
	}
	/**
	 * This adds/updates an application UserRights.SUPER_USER privilege is
	 * needed to insert a global application, an application that anybody can
	 * use and any worker can execute, an application with 0X777 as access
	 * rights. If the client don't have UserRights.SUPER_USER rights, the
	 * application is available for that user only : application access rights
	 * is forced to 0x700 which means that this user will be the only allowed to
	 * insert job, and workers belonging to this user only will be able to
	 * execute. There may be an exception if the application name already exists
	 * e.g. : an user tries to insert its own private application which name is
	 * already used.
	 *
	 * @param theClient is the requesting client
	 * @param appitf
	 *            is an ApplicationInterface object
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	protected void addApp(final UserInterface theClient, AppInterface appitf)
			throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {

		if (appitf == null) {
			throw new IOException("addApplication : appitf is null");
		}

		if (appitf.getOwner() == null) {
			appitf.setOwner(theClient.getUID());
		}
		if (appitf.getAccessRights() == null) {
			appitf.setAccessRights(XWAccessRights.DEFAULT);
		}
		final int isStickyBit = appitf.getAccessRights().value() & XWAccessRights.STICKYBIT_INT;
		final UID clientGroup = theClient.getGroup();

		final UserInterface owner = user(appitf.getOwner());
		final UID ownerGroup = (owner != null ? owner.getGroup() : null);
		final AppInterface theApp = (appitf.getUID() != null ? app(theClient, appitf.getUID())
				: app(theClient, AppInterface.Columns.NAME.toString() + "='" + appitf.getName() + "'"));

		if (theApp != null) {
			if ((theClient.getUID().equals(theApp.getOwner())
					&& theClient.getRights().higherOrEquals(UserRightEnum.INSERTAPP))
					|| theApp.canWrite(theClient, ownerGroup)
					|| theClient.getRights().higherOrEquals(UserRightEnum.ADVANCED_USER)) {

				useData(theClient, appitf.getDefaultStdin());
				removeData(theClient, theApp.getDefaultStdin());
				theApp.setDefaultStdin(appitf.getDefaultStdin());
				useData(theClient, appitf.getDefaultDirin());
				removeData(theClient, theApp.getDefaultDirin());
				theApp.setDefaultDirin(appitf.getDefaultDirin());
				useData(theClient, appitf.getBaseDirin());
				removeData(theClient, theApp.getBaseDirin());
				theApp.setBaseDirin(appitf.getBaseDirin());
				useData(theClient, appitf.getLDLinuxIX86());
				removeData(theClient, theApp.getLDLinuxIX86());
				theApp.setLDLinux_ix86(appitf.getLDLinuxIX86());
				useData(theClient, appitf.getLDLinuxPpc());
				removeData(theClient, theApp.getLDLinuxPpc());
				theApp.setLDLinux_ppc(appitf.getLDLinuxPpc());
				useData(theClient, appitf.getLDLinuxAmd64());
				removeData(theClient, theApp.getLDLinuxAmd64());
				theApp.setLDLinux_amd64(appitf.getLDLinuxAmd64());
				useData(theClient, appitf.getLDWin32IX86());
				removeData(theClient, theApp.getLDWin32IX86());
				theApp.setLDWin32_ix86(appitf.getLDWin32IX86());
				useData(theClient, appitf.getLDWin32Amd64());
				removeData(theClient, theApp.getLDWin32Amd64());
				theApp.setLDWin32_amd64(appitf.getLDWin32Amd64());
				useData(theClient, appitf.getLDMacosIX86());
				removeData(theClient, theApp.getLDMacosIX86());
				theApp.setLDMacos_ix86(appitf.getLDMacosIX86());
				useData(theClient, appitf.getLDMacosPpc());
				removeData(theClient, theApp.getLDMacosPpc());
				theApp.setLDMacos_ppc(appitf.getLDMacosPpc());
				useData(theClient, appitf.getLDSolarisSparc());
				removeData(theClient, theApp.getLDSolarisSparc());
				theApp.setLDSolaris_sparc(appitf.getLDSolarisSparc());
				useData(theClient, appitf.getLDSolarisAlpha());
				removeData(theClient, theApp.getLDSolarisAlpha());
				theApp.setLDSolaris_alpha(appitf.getLDSolarisAlpha());
				useData(theClient, appitf.getJava());
				useData(theClient, theApp.getJava());
				theApp.setJava(appitf.getJava());
				useData(theClient, appitf.getLinuxIX86());
				removeData(theClient, theApp.getLinuxIX86());
				theApp.setLinux_ix86(appitf.getLinuxIX86());
				useData(theClient, appitf.getLinuxPpc());
				removeData(theClient, theApp.getLinuxPpc());
				theApp.setLinux_ppc(appitf.getLinuxPpc());
				useData(theClient, appitf.getLinuxAmd64());
				removeData(theClient, theApp.getLinuxAmd64());
				theApp.setLinux_amd64(appitf.getLinuxAmd64());
				useData(theClient, appitf.getWin32IX86());
				removeData(theClient, theApp.getWin32IX86());
				theApp.setWin32_ix86(appitf.getWin32IX86());
				useData(theClient, appitf.getWin32Amd64());
				removeData(theClient, theApp.getWin32Amd64());
				theApp.setWin32_amd64(appitf.getWin32Amd64());
				useData(theClient, appitf.getMacosIX86());
				removeData(theClient, theApp.getMacosIX86());
				theApp.setMacos_ix86(appitf.getMacosIX86());
				useData(theClient, appitf.getMacosPpc());
				removeData(theClient, theApp.getMacosPpc());
				theApp.setMacos_ppc(appitf.getMacosPpc());
				useData(theClient, appitf.getSolarisSparc());
				removeData(theClient, theApp.getSolarisSparc());
				theApp.setSolaris_sparc(appitf.getSolarisSparc());

				if (theClient.getRights().lowerThan(UserRightEnum.ADVANCED_USER)) {
					logger.debug("update app set AR to USERALL " + new XWAccessRights(XWAccessRights.USERALL.value() | isStickyBit));
					appitf.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() | isStickyBit));
				}
				if (theClient.getRights().doesEqual(UserRightEnum.SUPER_USER)) {
					if (appitf.getAccessRights() == null) {
						logger.debug("update app set AR to DEFAULT " + new XWAccessRights(XWAccessRights.DEFAULT.value() | isStickyBit));
						appitf.setAccessRights(new XWAccessRights(XWAccessRights.DEFAULT.value() | isStickyBit));
					}
				} else {
					if ((theClient.getRights().higherOrEquals(UserRightEnum.INSERTAPP)) && (clientGroup != null)) {
						logger.debug("update app set AR to OWNERGROUP " + new XWAccessRights(XWAccessRights.OWNERGROUP.value() | isStickyBit));
						appitf.setAccessRights(new XWAccessRights(XWAccessRights.OWNERGROUP.value() | isStickyBit));
					}
				}
				theApp.updateInterface(appitf);
				update(theClient, UserRightEnum.INSERTAPP, theApp);
			} else {
				logger.error("addApp() : " + theClient.getLogin() + " can't update " + appitf.getName());
				throw new AccessControlException(theClient.getLogin() + " can't update " + appitf.getName());
			}

			return;
		}

		try {
			if (appitf.getUID() == null) {
				appitf.setUID(new UID());
			}
		} catch (final Exception e) {
			logger.exception(theClient.getLogin() + " can't create " + appitf.getName(), e);
			throw new AccessControlException(
					theClient.getLogin() + " can't create " + appitf.getName() + " : " + e.getMessage());
		}

        if (theClient.getRights().lowerThan(UserRightEnum.ADVANCED_USER)) {
			logger.debug("set app AR to USERALL " + new XWAccessRights(XWAccessRights.USERALL.value() | isStickyBit));
			appitf.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() | isStickyBit));
		}
		if (theClient.getRights().doesEqual(UserRightEnum.SUPER_USER)) {
			if (appitf.getAccessRights() == null) {
				logger.debug("set app AR to DEFAULT " + new XWAccessRights(XWAccessRights.DEFAULT.value() | isStickyBit));
				appitf.setAccessRights(new XWAccessRights(XWAccessRights.DEFAULT.value() | isStickyBit));
			}
			else {
                logger.debug("keeping app AR " + appitf.getAccessRights());
            }
		} else {
			if ((theClient.getRights().higherOrEquals(UserRightEnum.INSERTAPP)) && (clientGroup != null)) {
				logger.debug("set app AR to OWNERGROUP " + new XWAccessRights(XWAccessRights.OWNERGROUP.value() | isStickyBit));
				appitf.setAccessRights(new XWAccessRights(XWAccessRights.OWNERGROUP.value() | isStickyBit));
			}
		}

		final AppTypeEnum apptype = appitf.getType();
		if ((apptype == null) || (apptype == AppTypeEnum.NONE)) {
			appitf.setType(AppTypeEnum.DEPLOYABLE);
		}

		insert(appitf);

		useData(theClient, appitf.getDefaultStdin());
		useData(theClient, appitf.getDefaultDirin());
		useData(theClient, appitf.getBaseDirin());
		useData(theClient, appitf.getLDLinuxIX86());
		useData(theClient, appitf.getLDLinuxPpc());
		useData(theClient, appitf.getLDLinuxAmd64());
		useData(theClient, appitf.getLDWin32IX86());
		useData(theClient, appitf.getLDWin32Amd64());
		useData(theClient, appitf.getLDMacosIX86());
		useData(theClient, appitf.getLDMacosPpc());
		useData(theClient, appitf.getLDSolarisSparc());
		useData(theClient, appitf.getLDSolarisAlpha());
		useData(theClient, appitf.getJava());
		useData(theClient, appitf.getLinuxIX86());
		useData(theClient, appitf.getLinuxPpc());
		useData(theClient, appitf.getLinuxAmd64());
		useData(theClient, appitf.getWin32IX86());
		useData(theClient, appitf.getWin32Amd64());
		useData(theClient, appitf.getMacosIX86());
		useData(theClient, appitf.getMacosPpc());
		useData(theClient, appitf.getSolarisSparc());
	}

	/**
	 * retrieve applications from SQL table apps accordingly to conditions.
	 *
	 * @param command is the command to execute
	 * @return a Collection UID objects
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public Collection<UID> getApplications(final XMLRPCCommand command)
			throws IOException, InvalidKeyException {

		final Collection<UID> mandatingUID = new Vector<UID>();
		try {
			final UserInterface mandatingClient = checkClient(command, UserRightEnum.LISTAPP);
			final Collection<UID> uids = appsUID(mandatingClient);
			if(uids != null){
				mandatingUID.addAll(uids);
			}
		} catch(final java.security.AccessControlException e) {
		}

		if(!command.isMandated()) {
			return mandatingUID;
		}

		final UserInterface mandatedClient = checkMandatedClient(command);
		final Collection<UID> mandatedUID = appsUID(mandatedClient);
		return mergeCollections(mandatingUID, mandatedUID);
	}

	/**
	 * This retrieves an object
	 * AccessControlException is catched on each case since there may be a mandat
	 * @param command is the command to execute
	 * @return the object or null if not found
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public Table get(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UID uid = command.getURI().getUID();
		final UserInterface client = command.getUser();
		Table ret = null;

		try {
			ret = getJob(command);
			if (ret != null) {
				return ret;
			}
		} catch (final AccessControlException e) {
		}
		try {
    		ret = getTask(command);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
		try {
    		ret = data(command);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
		try {
    		ret = getApplication(command);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
		try {
    		ret = getUser(command);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
		try {
    		ret = getUserGroup(command);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
		try {
    		ret = getSession(client, uid);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
		try {
    		ret = getGroup(client, uid);
	    	if (ret != null) {
		    	return ret;
		    }
		} catch (final AccessControlException e) {
		}
        try {
            ret = getHost(command);
            if (ret != null) {
                return ret;
            }
        } catch (final AccessControlException e) {
        }
        try {
            ret = getCategory(command);
            if (ret != null) {
                return ret;
            }
        } catch (final AccessControlException e) {
        }
        try {
            ret = getMarketOrder(command);
            if (ret != null) {
                return ret;
            }
        } catch (final AccessControlException e) {
        }

		return null;
	}

	/**
	 * This retrieves a task for the specified client. This specifically permits
	 * to retrieve job instantiation informations (e.g. start date, worker...)
	 *
	 * @param command is the command to execute
	 * @return null on error; a TaskInterface otherwise
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public TaskInterface getTask(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETJOB);
		final UID uid = command.getURI().getUID();
		final TaskInterface theTask = task(theClient, uid);
		if (theTask != null) {
			return theTask;
		}
		final TaskInterface theTaskbyWork = task(theClient,
				TaskInterface.Columns.WORKUID.toString() + "='" + uid + "'");

		if (theTaskbyWork == null) {
			return null;
		}

		final UserInterface owner = user(theTaskbyWork.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());
		if (!theTaskbyWork.canRead(theClient, ownerGroup)
				&& theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			throw new AccessControlException(theClient.getLogin() + " can't read " + uid);
		}
		return theTaskbyWork;
	}

    /**
     * This retrieves a work given its external id
     *
     * @param command is the command to execute
     * @return null on error; a TaskInterface otherwise
     * @exception IOException
     *                is thrown general error
     * @exception InvalidKeyException
     *                is thrown on credential error
     * @exception AccessControlException
     *                is thrown on access rights violation
     * @since 11.1.0
     */
    public WorkInterface getWorkByExternalId(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.GETJOB);
        final URI uri = command.getURI();
        final String extId = uri.getPath().substring(1, uri.getPath().length());

        final WorkInterface theWorkById = work(theClient,
                WorkInterface.Columns.SGID.toString() + "='" + extId + "'");

        if (theWorkById == null) {
            return null;
        }

        final UserInterface owner = user(theWorkById.getOwner());
        final UID ownerGroup = (owner == null ? null : owner.getGroup());
        if (!theWorkById.canRead(theClient, ownerGroup)
                && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
            throw new AccessControlException(theClient.getLogin() + " can't read " + extId);
        }
        return theWorkById;
    }

    /**
     * This retrieves a category given its id
     *
     * @param command is the command to execute
     * @return null on error; a TaskInterface otherwise
     * @exception IOException
     *                is thrown general error
     * @exception InvalidKeyException
     *                is thrown on credential error
     * @exception AccessControlException
     *                is thrown on access rights violation
     * @since 13.0.0
     */
    public CategoryInterface getCategoryById(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.GETCATEGORY);
        final URI uri = command.getURI();
        final String id = uri.getPath().substring(1, uri.getPath().length());

        final CategoryInterface theCategory = category(theClient,
                CategoryInterface.Columns.CATEGORYID.toString() + "='" + id + "'");

        if (theCategory == null) {
            return null;
        }

        final UserInterface owner = user(theCategory.getOwner());
        final UID ownerGroup = (owner == null ? null : owner.getGroup());
        if (!theCategory.canRead(theClient, ownerGroup)
                && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
            throw new AccessControlException(theClient.getLogin() + " can't read " + id);
        }
        return theCategory;
    }

    /**
	 * This retrieves an application given its name
	 *
	 * @param command is the command to execute
	 * @return null on error; a TaskInterface otherwise
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public AppInterface getAppByName(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETJOB);
		final URI uri = command.getURI();
		final String appName = uri.getPath().substring(1, uri.getPath().length());

		final AppInterface theApp = app(theClient,
				AppInterface.Columns.NAME.toString() + "='" + appName + "'");

		if (theApp == null) {
			return null;
		}

		final UserInterface owner = user(theApp.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());
		if (!theApp.canRead(theClient, ownerGroup)
				&& theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			throw new AccessControlException(theClient.getLogin() + " can't read " + appName);
		}
		return theApp;
	}

	/**
	 * @param command is the command to execute
	 * @return the found application or null
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 * @see #app(UserInterface, UID)
	 */
	private AppInterface getApplication(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UID uid = command.getURI().getUID();
		return app(command, uid);
	}

	/**
	 * This removes an application from DB
	 *
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeApplication(final UserInterface theClient, final AppInterface theApp, final UID ownerGroup)
			throws IOException, InvalidKeyException, AccessControlException {

		if (theApp == null) {
			return false;
		}
		if (!theApp.canWrite(theClient, ownerGroup) && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			throw new AccessControlException(theClient.getLogin() + " can not remove app " + theApp.getName());
		}

		removeData(theClient, theApp.getDefaultStdin());
		removeData(theClient, theApp.getBaseDirin());
		removeData(theClient, theApp.getDefaultDirin());
		removeData(theClient, theApp.getLinuxIX86());
		removeData(theClient, theApp.getLinuxAmd64());
		removeData(theClient, theApp.getLinuxPpc());
		removeData(theClient, theApp.getWin32IX86());
		removeData(theClient, theApp.getWin32Amd64());
		removeData(theClient, theApp.getMacosIX86());
		removeData(theClient, theApp.getMacosPpc());
		removeData(theClient, theApp.getMacosX8664());
		removeData(theClient, theApp.getJava());
		removeData(theClient, theApp.getSolarisSparc());
		removeData(theClient, theApp.getLDLinuxIX86());
		removeData(theClient, theApp.getLDLinuxAmd64());
		removeData(theClient, theApp.getLDLinuxPpc());
		removeData(theClient, theApp.getLDWin32IX86());
		removeData(theClient, theApp.getLDWin32Amd64());
		removeData(theClient, theApp.getLDMacosIX86());
		removeData(theClient, theApp.getLDMacosPpc());
		removeData(theClient, theApp.getLDMacosX8664());
		removeData(theClient, theApp.getLDSolarisSparc());
		removeData(theClient, theApp.getLDSolarisAlpha());

		return delete(theClient, theApp);
	}

	/**
	 * This calls removeData(theClient uri.getUID())
	 */
	private boolean removeData(final UserInterface theClient, final URI uri)
			throws IOException, InvalidKeyException, AccessControlException {
	    if(uri == null)
	        return false;
		return removeData(theClient, uri.getUID());
	}

	/**
	 * This calls delete(theClient data(uid))
	 */
	private boolean removeData(final UserInterface theClient, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {

		final DataInterface theData = data(uid);
		if (theData != null) {
			final WorkInterface datajob = work(theData.getWork());
			delete(theClient, datajob);
		}
		return delete(theClient, theData);
	}

	/**
	 * This deletes a group from DB and all its associated jobs
	 *
	 * @param command is the command to execute
	 * @return true on success; false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeGroup(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.DELETEGROUP);
		final UID groupUid = command.getURI().getUID();
		final GroupInterface group = group(theClient, groupUid);
		if (group == null) {
			return false;
		}

		if (!(group.getOwner().equals(theClient.getUID()))
				&& (theClient.getRights().lowerThan(UserRightEnum.SUPER_USER))) {
			throw new AccessControlException(theClient.getLogin() + " : can't remove group " + groupUid);
		}

		removeSession(theClient, group.getSession());
		if (deleteJobs(theClient, getGroupJobs(command))) {
			return delete(theClient, group);
		}
		return true;
	}

    /**
     * This deletes a session from DB and all its associated jobs
     *
     * @param command is the command to execute
     * @return true on success; false otherwise
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     */
    protected boolean removeSession(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.DELETEGROUP);
        final UID sessionUid = command.getURI().getUID();
        if (sessionUid == null) {
            return false;
        }
        final SessionInterface session = session(theClient, sessionUid);
        if (session == null) {
            return false;
        }

        if (deleteJobs(theClient, getSessionJobs(command))) {
            return delete(theClient, session);
        }

        return false;
    }
    /**
     * This deletes a session from DB and all its associated jobs
     *
     * @param client
     *            describes the requesting client
     * @param sessionUid
     *            is the UID of the session to delete
     * @return true on success; false otherwise
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     */
    protected boolean removeSession(final UserInterface client, final UID sessionUid)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(client, UserRightEnum.DELETEGROUP);
        if (sessionUid == null) {
            return false;
        }
        final SessionInterface session = session(theClient, sessionUid);
        if (session == null) {
            return false;
        }

        if (deleteJobs(theClient, getSessionJobs(theClient, sessionUid))) {
            return delete(theClient, session);
        }

        return false;
    }
    /**
     * This deletes a market order from DB
     *
     * @param command is the command to execute
     * @return true on success; false otherwise
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     * @since 13.1.0
     */
    protected boolean removeMarketOrder(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.DELETEMARKETORDER);
        final UID uid = command.getURI().getUID();
        return removeMarketOrder(theClient, uid);
    }
    /**
     * This deletes a session from DB and all its associated jobs
     *
     * @param theClient
     *            describes the requesting client
     * @param uid
     *            is the UID of the session to delete
     * @return true on success; false otherwise
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     * @since 13.1.0
     */
    protected boolean removeMarketOrder(final UserInterface theClient, final UID uid)
            throws IOException, InvalidKeyException, AccessControlException {

        if (uid == null) {
            return false;
        }
        final MarketOrderInterface marketOrder = marketOrder(theClient, uid);
        if (marketOrder == null) {
            return false;
        }
		if(!marketOrder.canBeClosed()) {
			logger.error("removeMarketOrder(" + uid +") : " + " can't be closed " + marketOrder.getStatus());
        	return false;
		}
        logger.debug("removeMarketOrder(" + uid + ")");

        final Collection<HostInterface> workers = DBInterface.getInstance().hosts(marketOrder);
        if(workers != null) {
	        for(final HostInterface worker : workers ) {
				logger.info("removeMarketOrder(); worker.leaveMarketOrder() : " + worker.getUID());
				worker.leaveMarketOrder();
			}
        }

        boolean ret = false;
        if (deleteJobs(theClient, getMarketOrderJobs(theClient, uid))) {
            ret = delete(theClient, marketOrder);
        }

        if(ret == true) {
           MarketplaceService.getInstance().getMarketplace().closeMarketOrder(BigInteger.valueOf(marketOrder.getMarketOrderIdx())).sendAsync();
        }
        return ret;
    }

    /**
	 * This deletes a session from DB and all its associated jobs
	 *
	 * @param command is the command to execute
	 * @return true on success; false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeTask(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.DELETEGROUP);
		final UID taskUid = command.getURI().getUID();
		final TaskInterface theTask = task(theClient, taskUid);
		if (theTask == null) {
			return false;
		}

		if ((theClient.getRights() == null) || (theClient.getRights().lowerThan(UserRightEnum.DELETEJOB))) {
			throw new AccessControlException(theClient.getLogin() + " : not enough rights to delete task " + taskUid);
		}

		return delete(theClient, theTask);
	}

	/**
	 * This removes an user. The UserInterface which UID is provided is
	 * effectively removed if requesting client has UserRights.SUPER_USER
	 * privileges, or provided UID is the client one. <br>
	 * This also removes users's jobs (sessions, groups, tasks and works).
	 *
	 * @param theClient is the user
	 * @param userUID
	 *            is the user UID
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeUser(final UserInterface theClient, final UID userUID)
			throws IOException, InvalidKeyException, AccessControlException {

		boolean ret = true;

		final UserInterface theuser = user(theClient, userUID);
		if (theuser == null) {
			return false;
		}
		if ((theClient.getRights() == null) || (theClient.getRights().lowerThan(UserRightEnum.DELETEUSER))) {
			throw new AccessControlException(
					theClient.getLogin() + " : not enough rights to delete UserInterface " + userUID);
		}

		final Vector<UID> workuids = (Vector<UID>) worksUID(theClient,
				SQLRequest.MAINTABLEALIAS + "." + TableColumns.OWNERUID.toString() + "='" + userUID.toString() + "'");
		if (workuids != null) {
			for (final Enumeration<UID> enums = workuids.elements(); enums.hasMoreElements();) {
				final UID workuid = enums.nextElement();
				if (workuid != null) {
					ret = removeWork(theClient, workuid);
				}
				if (!ret) {
					break;
				}
			}
		}

		if (ret) {
			ret = delete(theClient, theuser);
		}
		return ret;
	}

	/**
	 * This removes an user group and all its members
	 *
	 * @param theClient
	 *            is the requesting user
	 * @param groupUID
	 *            is the user group UID to remove.
	 * @return true on success, false otherwise
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeUserGroup(final UserInterface theClient, final UID groupUID)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserGroupInterface group = usergroup(theClient, groupUID);
		if (group == null) {
			return false;
		}
		if ((theClient.getRights() == null) || (theClient.getRights().lowerThan(UserRightEnum.DELETEUSERGROUP))) {

			throw new AccessControlException(
					theClient.getLogin() + " : not enough rights to delete UserInterface group " + groupUID);
		}
		if (!group.isProject()) {
			return false;
		}

		boolean ret = false;
		final Vector<UID> useruids = (Vector<UID>) usersUID(theClient, SQLRequest.MAINTABLEALIAS + "."
				+ UserInterface.Columns.USERGROUPUID.toString() + "='" + groupUID.toString() + "'");
		if (useruids != null) {
			final Iterator<UID> li = useruids.iterator();
			while (li.hasNext()) {
				final UID userUID = li.next();
				if (userUID == null) {
					logger.error("user uid is null???");
					continue;
				}
				ret = removeUser(theClient, userUID);
				if (!ret) {
					break;
				}
			}
		}

		if (ret) {
			ret = delete(theClient, group);
		}
		return ret;
	}

	/**
	 * Get all known traces.
	 *
	 * @param client
	 *            is the <code>ClientInterface</code> that identifies the client
	 * @param since
	 *            is the date from which to retrieve traces
	 * @param before
	 *            is the date to which to retrieve traces
	 * @return an vector of <CODE>TraceInterface</CODE>
	 */
	public Vector<UID> getRegisteredTraces(final UserInterface client, final java.util.Date since,
			final java.util.Date before) {

		logger.error("getRegisteredTraces() not implemented");
		return null;
	}

	/* Tracer */

	public void writeStatFile(final HostInterface host, final long start, final long end, final byte[] file)
			throws IOException {

		logger.error("writeStatFile() not implemented");
	}

	/**
	 * This retrieves sessions. <br>
	 *
	 * @param command is the command to execute
	 * @return a Collection of UID
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	Collection<UID> getSessions(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTSESSION);
		return getSessions(theClient, theClient.getUID());
	}

	/**
	 * This retrieves sessions for the given client<br />
	 * This method has a private access because it does not call checkClient()
	 * to try to avoid too much DB access. It is typically called from whithin
	 * this current source file from methods which must have already called
	 * checkClient() !<br>
	 * Typically deleteGroup(), deleteSession(), disconnect() etc.
	 *
	 * @param client
	 *            is the client uid
	 * @return a Collection of UID
	 */
	public Collection<UID> getSessions(final UserInterface theClient, final UID client) throws IOException {
		return sessionsUID(theClient,
				SQLRequest.MAINTABLEALIAS + "." + TableColumns.OWNERUID.toString() + "='" + client.toString() + "'");
	}

	/**
	 * This does not check the client integrity; this must have been done by the
	 * caller. This checks client rights is not lower than GETSESSION and
	 * returns work(theClient, uid)
	 *
	 * @param client
	 *            is the requesting client
	 * @param uid
	 *            is the UID of the work to retrieve
	 * @return the found work or null
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception AccessControlException
	 *                is thrown if UserInterface rights lower than GETSESSION
	 * @see #work(UserInterface, UID)
	 */
	public SessionInterface getSession(final UserInterface client, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(client, UserRightEnum.GETSESSION);
		return session(theClient, uid);
	}

	/**
	 * This retrieves groups of the provided client.<br />
	 * This method has a private access because it does not call checkClient()
	 * to try to avoid too much DB access. It is typically called from whithin
	 * this current source file from methods which must have already called
	 * checkClient() !<br>
	 * Typically deleteGroup(), deleteSession(), disconnect() etc.
	 *
	 * @param command is the command to execute
	 * @return a Vector of UID
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	public Collection<UID> getGroups(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final UserInterface theClient = checkClient(command, UserRightEnum.LISTGROUP);
		return groupsUID(theClient,
				SQLRequest.MAINTABLEALIAS + "." + TableColumns.OWNERUID.toString() + "='" + theClient.getUID() + "'");
	}

	/**
	 * This checks client rights and returns group(client, uid)
	 *
	 * @see #group(UserInterface, UID)
	 */
	public GroupInterface getGroup(final UserInterface client, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(client, UserRightEnum.GETGROUP);
		return group(theClient, uid);
	}

	/**
	 * This checks group access rights and eventually then call
	 * getGroupJobs(client, udi)
	 *
	 * @param command is the command to execute
	 * @return a Collection of UID
	 * @see #getGroupJobs(XMLRPCCommand)
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public Collection<UID> getGroupJobs(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTJOB);
		final UID uid = command.getURI().getUID();

		final GroupInterface group = group(theClient, uid);
		if (group == null) {
			return null;
		}
		if ((group.getOwner().equals(theClient.getUID()) == false)
				&& (theClient.getRights().lowerThan(UserRightEnum.SUPER_USER))) {
			throw new AccessControlException(theClient.getLogin() + " : can't retrieve jobs of group " + uid);
		}

		return worksUID(theClient, SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.GROUPUID.toString() + "='"
				+ uid.toString() + "'");
	}

    /**
     * This retrieves jobs for a given session
     *
     * @param command is the command to execute
     * @return a Vector of UID
     * @see #worksUID(UserInterface, String)
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     */
    public Collection<UID> getSessionJobs(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.LISTJOB);
        final UID uid = command.getURI().getUID();
        return worksUID(theClient, SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.SESSIONUID.toString() + "='"
                + uid.toString() + "'");
    }
	/**
	 * This retrieves jobs for a given session
	 *
	 * @param client
	 *            describes the requesting client
	 * @param sessionUid
	 *            is the session UID to retrieve jobs for
	 * @return a Vector of UID
	 * @see #worksUID(UserInterface, String)
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	public Collection<UID> getSessionJobs(final UserInterface client, final UID sessionUid)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(client, UserRightEnum.LISTJOB);
		return worksUID(theClient, SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.SESSIONUID.toString() + "='"
				+ sessionUid.toString() + "'");
	}
    /**
     * This retrieves jobs for a given market order
     *
     * @param command is the command to execute
     * @return a Vector of UID
     * @see #worksUID(UserInterface, String)
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     * @since 13.1.0
     */
    public Collection<UID> getMarketOrderJobs(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.LISTJOB);
        final UID uid = command.getURI().getUID();
        return getMarketOrderJobs(theClient, uid);
    }
    /**
     * This retrieves jobs for a market order
     *
     * @param client
     *            describes the requesting client
     * @param uid
     *            is the market order UID to retrieve jobs for
     * @return a Vector of UID
     * @see #worksUID(UserInterface, String)
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     * @since 13.1.0
     */
    public Collection<UID> getMarketOrderJobs(final UserInterface client, final UID uid)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(client, UserRightEnum.LISTJOB);
        final MarketOrderInterface marketOrder = marketOrder(client, uid);
		return worksUID(theClient, SQLRequest.MAINTABLEALIAS + "." + WorkInterface.Columns.MARKETORDERUID + "='"
                + marketOrder.getUID() + "'");
    }

	/**
	 * This adds/updates a session
	 *
	 * @param command is the command to execute
	 * @return true on success; false on DB error or if session already exists
	 *         in DB
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	protected boolean addSession(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {

		final UserInterface theClient = checkClient(command, UserRightEnum.INSERTSESSION);
		final SessionInterface sessionitf = (SessionInterface) command.getParameter();
		final SessionInterface session = session(theClient, sessionitf.getUID());
		if (session != null) {
			update(theClient, UserRightEnum.INSERTSESSION, session);
			return true;
		}
		if (sessionitf.getUID() == null) {
			final UID uid = new UID();
			sessionitf.setUID(uid);
		}
		if (sessionitf.getOwner() == null) {
			sessionitf.setOwner(theClient.getUID());
		}

		insert(sessionitf);

		return true;
	}

	/**
	 * This adds/updates a group in DB
	 *
	 * @param command is the command to execute
	 * @return true on success; false on DB error or group already exists
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	protected boolean addGroup(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {

		final UserInterface theClient = checkClient(command, UserRightEnum.INSERTGROUP);
		final GroupInterface groupitf = (GroupInterface) command.getParameter();
		final GroupInterface group = group(theClient, groupitf.getUID());
		if (group != null) {
			group.updateInterface(groupitf);
			update(theClient, UserRightEnum.INSERTGROUP, group);
			return true;
		}

		if (groupitf.getUID() == null) {
			final UID uid = new UID();
			groupitf.setUID(uid);
		}
		if (groupitf.getOwner() == null) {
			groupitf.setOwner(theClient.getUID());
		}
		insert(groupitf);

		return true;
	}

	/**
	 * This delete a job (a work and its associated task)
	 *
	 * @see #deleteJobs(UserInterface, Collection)
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeWork(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final UserInterface theClient = checkClient(command, UserRightEnum.DELETEJOB);
		final UID jobUid = command.getURI().getUID();
		return removeWork(theClient, jobUid);
	}
	/**
	 * This delete a job (a work and its associated task)
	 *
	 * @see #deleteJobs(UserInterface, Collection)
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on client integrity error (user unknown, bad
	 *                password...)
	 * @exception AccessControlException
	 *                is thrown if client does not have enough rights
	 */
	private boolean removeWork(final UserInterface theClient, final UID jobUid)
			throws IOException, InvalidKeyException, AccessControlException {

		if (jobUid == null) {
			return false;
		}

		final WorkInterface theWork = work(theClient, jobUid);
		if (theWork == null) {
			logger.debug("no work for jobuid " + jobUid);
			return false;
		}
		if ((theClient.getRights() == null) || (theClient.getRights().lowerThan(UserRightEnum.DELETEJOB))) {
			throw new AccessControlException(theClient.getLogin() + " : not enough rights to delete job " + jobUid);
		}

		try {
			removeData(theClient, theWork.getStdin());
		} catch (final AccessControlException e) {
			logger.warn(e.getMessage());
		}
		try {
			removeData(theClient, theWork.getDirin());
		} catch (final AccessControlException e) {
			logger.warn(e.getMessage());
		}
		try {
			removeData(theClient, theWork.getResult());
		} catch (final AccessControlException e) {
			logger.warn(e.getMessage());
		}
		try {
			removeData(theClient, theWork.getUserProxy());
		} catch (final AccessControlException e) {
			logger.warn(e.getMessage());
		}

		final Vector<TaskInterface> theTasks = (Vector<TaskInterface>) tasks(theWork);

		if (theTasks != null) {

			final Enumeration<TaskInterface> thetaskEnum = theTasks.elements();

			while (thetaskEnum.hasMoreElements()) {

				final TaskInterface theTask = thetaskEnum.nextElement();
				final UID hostUID = theTask.getHost();
                final HostInterface theHost = host(hostUID);
                final MarketOrderInterface marketOrder = marketOrder(theHost.getMarketOrderUid());
				if (delete(theClient, theTask)) {
					if (theHost != null) {
//                        theHost.leaveMarketOrder(marketOrder);
						switch (theWork.getStatus()) {
                            case CONTRIBUTING:
                            case CONTRIBUTED:
                            case REVEALING:
    						case RESULTREQUEST:
                            case DATAREQUEST:
		    				case RUNNING:
			    				theHost.decRunningJobs();
				    			break;
						}
                        theHost.update();
					}
				}
			}
		}

		final UID appUID = theWork.getApplication();
		final AppInterface theApp = app(theClient, appUID);
		final UID expectedHostUID = theWork.getExpectedHost();
		final HostInterface theExpectedHost = (expectedHostUID != null ? host(theClient, expectedHostUID) : null);

		switch (theWork.getStatus()) {
		case PENDING:
			if (theExpectedHost != null) {
				theExpectedHost.decPendingJobs();
			}
			theClient.decPendingJobs();
			if (theApp != null) {
				theApp.decPendingJobs();
			}
			break;
		case RESULTREQUEST:
		case DATAREQUEST:
        case CONTRIBUTED:
        case CONTRIBUTING:
        case REVEALING:
		case RUNNING:
			theClient.decRunningJobs();
			if (theApp != null) {
				theApp.decRunningJobs();
			}
			if (theExpectedHost != null) {
				theExpectedHost.decRunningJobs();
			}
			break;
		}

		deleteJobs(theClient, replicasUID(theClient, theWork.getUID()));

		delete(theClient, theWork);
        theClient.update();
		theApp.update();

		return true;
	}

	/**
	 * This deletes a vector of jobs by iteratively calling
	 * deleJob(UserInterface, UID)
	 *
	 * @see #removeWork(UserInterface, UID)
	 * @param theClient
	 *            describes the requesting client
	 * @param jobs
	 *            is a Collection containing work UIDs
	 * @return true on success; false otherwise
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	private boolean deleteJobs(final UserInterface theClient, final Collection<UID> jobs)
			throws IOException, InvalidKeyException, AccessControlException {

		boolean result = true;

		if ((jobs == null) || jobs.isEmpty()) {
			return true;
		}

		try {
			final Iterator<UID> li = jobs.iterator();
			while (li.hasNext()) {
				final UID jobUID = li.next();
				if ((jobUID != null) && !removeWork(theClient, jobUID)) {
					logger.warn("deletejob(" + jobUID + ") returned false");
					result = false;
				}
			}
		} catch (final Exception e) {
			result = false;
			logger.exception("DeleteJobs", e);
		}

		return result;

	}

	/**
	 * This broadcasts a new job to all workers accordingly to available
	 * application binaries This uses the "expectedHost" job attribute e.g. : if
	 * we don't have win32 binary, we don't broadcast to win32 workers
	 *
	 * @param command is the command to execute
	 * @see #addWork(XMLRPCCommand)
	 * @return a Collection of new jobs UID
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public Collection<UID> broadcast(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		throw new IOException ("broadcast not implemented");
/*
		final UserInterface theClient = checkMandating(command, UserRightEnum.BROADCAST);
		final WorkInterface job = (WorkInterface) command.getParameter();

		final Vector<UID> ret = new Vector<>();
		final AppInterface app = app(theClient, job.getApplication());

		if (app == null) {
			throw new IOException("broadcast() : can't find application");
		}

		final Collection<UID> workers = getRegisteredWorkers(theClient);
		if (workers.isEmpty()) {
			throw new IOException("broadcast() : can't find any worker to broadcast");
		}

		final Iterator<UID> li = workers.iterator();
		while (li.hasNext()) {
			final HostInterface worker = host(theClient, li.next());
			if(worker == null) {
				continue;
			}
			if (app.getBinary(worker.getCpu(), worker.getOs()) == null) {
				logger.warn(worker.getUID() + " : " + app.getName()
				+ " is not broadcasted since there is no compatible binary");
				continue;
			}
			job.setExpectedHost(worker.getUID());
			job.setUID(new UID());
			final WorkInterface work = addWork(theClient, null, job);
			if (work != null) {
				ret.add(work.getUID());
				work.setPending();
			} else {
				logger.warn("broadcast() can't submit");
			}
		}
		return ret;
		*/
	}

	/**
	 * 
	 * This adds/updates a work according to work access rights. If client
	 * rights is higher or equals to WORKER_USER, work access rights can be
	 * bypassed (but not allowing work insertion) This allows workers to update
	 * job (e.g. set job status to COMPLETED) This sets access rights to minimal
	 * value e.g. : if application access rights are 0x700, job ones must be
	 * 0x700 or lower (0x600, 0x500...)
	 *
	 * @param command is the command to execute
	 * @return the provided job uid don activation; a new jobUID if a new job
	 *         has been created; null on error
	 * @exception IOException
	 *                is thrown on DB access or I/O error
	 * @exception InvalidKeyException
	 *                is thrown on rights error (user unknown, not enough right,
	 *                client is a worker that tries to insert a new work...)
	 */
	protected WorkInterface addWork(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {
		final UserInterface mandatingClient = checkClient(command, UserRightEnum.INSERTJOB);
		final UserInterface mandatedClient = command.isMandated() ? checkMandatedClient(command) : null;
		final WorkInterface receivedJob = (WorkInterface) command.getParameter();
		final HostInterface _host = command.getHost();

		final UID jobUID = receivedJob.getUID();

		final UID appUID = receivedJob.getApplication();
		if (appUID == null) {
			throw new IOException("addWork() : job defines no app ?!?");
		}

		AppInterface theApp = app(command, appUID);
		if (theApp == null) {
			throw new IOException("addWork() : app not found " + appUID);
		}
		final UserInterface appOwner = user(theApp.getOwner());

		if (appOwner == null) {
			throw new IOException("addWork() : app has no owner " + appUID);
		}

		final UID appOwnerGroup = appOwner.getGroup();

		if (!theApp.canExec(mandatingClient, appOwnerGroup) &&
				(!theApp.canExec(mandatedClient, appOwnerGroup)) &&
				(mandatingClient.getRights().lowerThan(UserRightEnum.SUPER_USER))) {
			throw new IOException(
					"addWork() : " + mandatingClient.getLogin() + " don't have rights to submit job for app " + appUID);
		}

		receivedJob.setService(theApp.isService());

		final XWAccessRights jobRights = (receivedJob.getAccessRights() == null ? XWAccessRights.DEFAULT
				: receivedJob.getAccessRights());

		final int jobRightsInt = jobRights.value() & theApp.getAccessRights().value();
		logger.finest(String.format("DBInterface#addWork() jobRights.value() & theApp.getAccessRights().value() : %x & %x = %x",
				jobRights.value(),
				theApp.getAccessRights().value(),
				jobRights.value() & theApp.getAccessRights().value()));

		final int appStickyBit = theApp.getAccessRights().value() & XWAccessRights.STICKYBIT_INT;
		logger.finest(String.format("DBInterface#addWork() theApp.getAccessRights().value() & XWAccessRights.STICKYBIT_INT : %x & %x = %x",
				theApp.getAccessRights().value(), XWAccessRights.STICKYBIT_INT, appStickyBit));

		final XWAccessRights newJobRights = new XWAccessRights(jobRightsInt | appStickyBit);
		receivedJob.setAccessRights(newJobRights);
		final UserRightEnum clientRights = mandatingClient.getRights();


		if (receivedJob.getOwner() == null) {
			receivedJob.setOwner(mandatingClient.getUID());
		}

		if (receivedJob.getMinCpuSpeed() < theApp.getMinCpuSpeed()) {
			throw new IOException("job.getMinCpuSpeed < app.getMinCpuSpeed");
		}
		if (receivedJob.getMinFreeMassStorage() < theApp.getMinFreeMassStorage()) {
			throw new IOException("job getMinFreeMassStorage < app.getMinFreeMassStorage");
		}

        final CategoryInterface receivedJobCategory = select(new CategoryInterface(),
                "maintable." + CategoryInterface.Columns.CATEGORYID.toString() + "='" + receivedJob.getCategoryId() + "'");

        if(receivedJobCategory != null) {
            receivedJob.setMaxWallClockTime(receivedJobCategory.getMaxWallClockTime());
            receivedJob.setMaxMemory(receivedJobCategory.getMaxMemory());
            receivedJob.setMaxCpuSpeed(receivedJobCategory.getMaxCpuSpeed());
            receivedJob.setMaxFreeMassStorage(receivedJobCategory.getMaxFreeMassStorage());
            receivedJob.setMaxFileSize(receivedJobCategory.getMaxFileSize());
        } else {
            // reset limits to defaults
            receivedJob.setCategoryId(0);
        }

        final UID receivedJobMarketOrderUid = receivedJob.getMarketOrderUid();
        final MarketOrderInterface marketOrder = marketOrder(receivedJobMarketOrderUid);
        if((receivedJobMarketOrderUid != null) && (marketOrder == null)) {
            throw new IOException("invalid job market order : " + receivedJobMarketOrderUid);
        }


        final WorkInterface theWork = work(mandatingClient, jobUID);
		if (theWork != null) {

			if (!theWork.canWrite(mandatingClient, appOwnerGroup)
                    && !clientRights.doesEqual(UserRightEnum.WORKER_USER)
                    && !clientRights.doesEqual(UserRightEnum.VWORKER_USER)) {
//		final int isStickyBit = appitf.getAccessRights().value() & XWAccessRights.STICKYBIT_INT;
                    throw new AccessControlException("addWork() : " + mandatingClient.getLogin() + " can't update " + jobUID);
            }

            final UID hostUID = (_host == null ? null : _host.getUID());
            final HostInterface theHost = (hostUID == null ? null : host(hostUID));
            TaskInterface theTask = null;
            if (theHost != null) {
                if (clientRights.isWorker()) {
                    if (theHost != null) {
                        theTask = computingTask(theWork, theHost);
                    }
                } else {
                    theTask = computingTask(theWork);
                }
                if (theTask == null) {
                    throw new IOException(
                            mandatingClient.getLogin() + " work " + jobUID + " has no task run by " + _host.getUID());
                }
            }

            final UserInterface jobOwner = user(theWork.getOwner());
            final UserInterface realClient = (mandatingClient.getRights().isWorker() ? jobOwner : mandatingClient);

            final DataInterface theData = data(receivedJob.getResult());
            logger.debug("data is " + ((theData == null) ? "null" : theData.toXml()));
            useData(realClient, receivedJob.getResult());
            final DataInterface theData2 = data(receivedJob.getResult());
            logger.debug("data2 is " + ((theData == null) ? "null" : theData.toXml()));
            
            try {
                useData(realClient, receivedJob.getUserProxy());
            } catch (final AccessControlException e) {
                logger.warn(e.getMessage());
            }
            try {
                removeData(realClient, theWork.getUserProxy());
            } catch (final AccessControlException e) {
                logger.warn(e.getMessage());
            }

            // due to communication delays, it may happen that the scheduler
            // has already marked this job as revealing
            final boolean mustReveal = theWork.isRevealing();

			theWork.updateInterface(receivedJob);

			logger.debug(realClient.getLogin() + " is updating " + theWork.getUID() + " status = "
                    + receivedJob.getStatus());

            switch (theWork.getStatus()) {
            case RESULTREQUEST:
                theWork.setResultRequest();
                if (theTask != null) {
                    theTask.setResultRequest();
                }
                break;
            case DATAREQUEST:
                theWork.setDataRequest();
                if (theTask != null) {
                    theTask.setDataRequest();
                }
                break;
            case ABORTED:
                theWork.setPending();
                if (theTask != null) {
                    theTask.setPending();
                }
                theApp.decRunningJobs();
                theApp.incPendingJobs();
                jobOwner.decRunningJobs();
                jobOwner.incPendingJobs();
                if (theHost != null) {
                    theHost.decRunningJobs();
                    if (hostUID.equals(theWork.getExpectedHost())) {
                        theHost.incPendingJobs();
                    }
                }
                break;
                case CONTRIBUTING:
                    if(marketOrder == null) {
                        final String msg = "work cannot becontributing without market order";
                        theWork.setError(msg);
                        if(theTask != null) {
                            theTask.setErrorMsg(msg);
                        }
                        if (theHost != null) {
                            theHost.incErrorJobs();
                            theHost.decRunningJobs();
                        }
                        theApp.decRunningJobs();
                        theApp.incErrorJobs();
                        jobOwner.incErrorJobs();
                        jobOwner.decRunningJobs();
                        break;
                    }
                    if(theTask != null) {
                        theTask.setContributing();
                    }
                    if (theHost != null) {
                        theHost.setContributing();
                    }
                    theWork.update(false);
                    marketOrder.setContributing();
                    break;

                case CONTRIBUTED:
                    if(marketOrder == null) {
                        final String msg = "work cannot be a contribution without market order";
                        theWork.setError(msg);
                        if(theTask != null) {
                            theTask.setErrorMsg(msg);
                        }
                        if (theHost != null) {
                            theHost.incErrorJobs();
                            theHost.decRunningJobs();
                        }
                        theApp.decRunningJobs();
                        theApp.incErrorJobs();
                        jobOwner.incErrorJobs();
                        jobOwner.decRunningJobs();
                        break;
                    }
                    if(theTask != null) {
                        theTask.setContributed();
                    }
                    if (theHost != null) {
                        theHost.setContributed();
                    }
                    theWork.update(false);
                    marketOrder.setContributing();
                    break;

                case REVEALING:
				if(marketOrder == null) {
					final String msg = "work cannot revealed without market order";
					theWork.setError(msg);
					if(theTask != null) {
						theTask.setErrorMsg(msg);
					}
					if (theHost != null) {
						theHost.incErrorJobs();
						theHost.decRunningJobs();
					}
					theApp.decRunningJobs();
					theApp.incErrorJobs();
					jobOwner.incErrorJobs();
					jobOwner.decRunningJobs();
					break;
				}
				if (theHost != null) {
					theHost.setRevealing();
				}
				theTask.setRevealing();
                break;

            case COMPLETED:

                theApp.decRunningJobs();
                jobOwner.decRunningJobs();
                if (theHost != null) {
                    theHost.decRunningJobs();
                }
                theWork.setResult(receivedJob.getResult());
                theWork.setCompleted();
                if (theTask != null) {
                    final Date startdate = theTask.getLastStartDate();
                    if (startdate != null) {
                        final int exectime = (int) (System.currentTimeMillis() - startdate.getTime());
                        if (theHost != null) {
                            //theHost.leaveMarketOrder(marketOrder);
                            theHost.incAvgExecTime(exectime);
                        }
                        jobOwner.incUsedcputime(exectime);
                        theApp.incAvgExecTime(exectime);
                    }
                    theTask.setCompleted();
                    theTask.update(false);
                }
                final UID originalUid = theWork.getReplicatedUid();
                if (originalUid != null) {
                    synchronized (this) {
                        final WorkInterface replicatedWork = work(originalUid);
                        final long expectedReplications = replicatedWork.getExpectedReplications();
                        final long currentReplications = replicatedWork.getTotalReplica();
                        if ((currentReplications < expectedReplications) || (expectedReplications < 0)) {
                            logger.debug(realClient.getLogin() + " " + originalUid
                                    + " still has replications ; currently " + currentReplications + " ; expected "
                                    + expectedReplications);
                            final WorkInterface newWork = new WorkInterface(replicatedWork);
                            newWork.setUID(new UID());
                            newWork.replicate(originalUid);
                            newWork.insert();

                            theApp.incPendingJobs();
                            jobOwner.incPendingJobs();
                            if (hostUID.equals(theWork.getExpectedHost())) {
                                theHost.incPendingJobs();
                            }

                            replicatedWork.incTotalReplica();
                        }
//                        replicatedWork.setReplicating();
                        if (currentReplications >= replicatedWork.getTotalReplica()) {
                            replicatedWork.setCompleted();
                        }
                        replicatedWork.update();
                    }
                }
//                else {
//                    final long expectedReplications = theWork.getExpectedReplications();
//                    final long currentReplications = theWork.getTotalReplica();
//                    if ((currentReplications < expectedReplications) || (expectedReplications < 0)) {
//                        theWork.setReplicating();
//                    }
//                }

                break;
                case ERROR:
                    if (theHost != null) {
//                        theHost.leaveMarketOrder(marketOrder);
                        theHost.setContributionError();
                        theHost.setErrorMsg("contribution error");
                        theHost.incErrorJobs();
                        theHost.decRunningJobs();
                    }
                    theApp.decRunningJobs();
                    theApp.incErrorJobs();
                    jobOwner.incErrorJobs();
                    jobOwner.decRunningJobs();
                    theWork.setError(receivedJob.getErrorMsg());
                    if (theTask != null) {
                        theTask.setError();
                    }
                    if(theWork.getMarketOrderUid() != null) {
                        final WorkInterface newWork = new WorkInterface(theWork);
                        newWork.setUID(new UID());
                        newWork.replicate(theWork.getReplicatedUid() != null ? theWork.getReplicatedUid() : theWork.getUID());
                        newWork.setExpectedHost(null);
                        newWork.insert();
                        theApp.incPendingJobs();
                        jobOwner.incPendingJobs();
                    }
                    break;
                case FAILED:
                    if (theHost != null) {
						theHost.leaveMarketOrder(marketOrder);
                        theHost.decRunningJobs();
                    }
                    theApp.decRunningJobs();
                    jobOwner.incErrorJobs();
                    jobOwner.decRunningJobs();
                    theWork.setFailed(receivedJob.getErrorMsg());
                    if (theTask != null) {
                        theTask.setFailed();
                    }
                    break;
            }
            if (theTask != null) {
                theTask.update();
            }
            theWork.update();
            theApp.update();
            jobOwner.update();
            marketOrder.update();
            if (theHost != null) {
                theHost.update();
            }
            sendMail(jobOwner, theWork, realClient.getLogin() + " has updated ");

		} else {
			if (mandatingClient.getRights().isWorker()) {
				throw new AccessControlException("a worker can not insert a new work");
			}
			if (jobUID == null) {
				receivedJob.setUID(new UID());
			}

			if (receivedJob.getStatus() == null) {
				receivedJob.setStatus(StatusEnum.UNAVAILABLE);
			}

			receivedJob.setReplicatedUid(null);
			logger.debug(mandatingClient.getLogin() + " " + jobUID + " replications = " + receivedJob.getExpectedReplications()
			+ " by " + receivedJob.getReplicaSetSize());

			// if job.getExpectedReplications() < 0, we replicate for ever
			long replica = receivedJob.getExpectedReplications() < 0
					? receivedJob.getExpectedReplications() - receivedJob.getReplicaSetSize()
					: 0L;

			final Stack<HostInterface> workers = new Stack<HostInterface>();
            workers.addAll(DBInterface.getInstance().hosts(marketOrder));
            if(receivedJob.getExpectedReplications() != workers.size()) {
                logger.error("market order error: " + receivedJob.getExpectedReplications() + "!=" + workers.size());
                marketOrder.setErrorMsg("market order error:" + receivedJob.getExpectedReplications() + "!=" + workers.size());
                marketOrder.setError();
                for(HostInterface w : workers) {
                    w.leaveMarketOrder(marketOrder);
                    w.update();
                }
                marketOrder.update();
                return null;
            }

            WorkInterface returningWork = null;
            boolean firstJob = true;

			for (; (replica <= receivedJob.getReplicaSetSize()) && (replica <= receivedJob.getExpectedReplications() - 1);
                 replica++) {

				final WorkInterface newWork = new WorkInterface(receivedJob);
				newWork.setUID(jobUID); // we insert the original work (to
				// eventually be replicated)
				if (firstJob) {
					newWork.setTotalReplica(Math.min(receivedJob.getReplicaSetSize(), receivedJob.getExpectedReplications()));
					firstJob = false;
				} else {
					newWork.setUID(new UID());
					newWork.replicate(jobUID);
				}
				newWork.setPending();
				newWork.setExpectedHost(workers.pop().getUID());
				newWork.setArrivalDate(new java.util.Date());
				newWork.setActive(true);
				insert(newWork);

				useData(mandatingClient, newWork.getResult());
				useData(mandatingClient, newWork.getStdin());
				useData(mandatingClient, newWork.getDirin());

				newWork.update();
				logger.debug("DBInterface#addWork receivedJob = " + receivedJob.toXml());
                logger.debug("DBInterface#addWork newWork = " + newWork.toXml());
				mandatingClient.incPendingJobs();
				if(returningWork == null)
				    returningWork = newWork;
			}

			mandatingClient.update();
			theApp.incPendingJobs();
			theApp.update();
			return returningWork;
		}

		return theWork;
	}

    /**
	 * This retrieves a job status
	 *
	 * @param command is the command to execute
	 * @return -1 on error, job status otherwise
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public StatusEnum jobStatus(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETJOB);
		final WorkInterface work = work(theClient, command.getURI().getUID());
		final UserInterface owner = user(work.getOwner());
		final UID ownerGroup = (owner == null ? null : owner.getGroup());

		if (!work.canRead(theClient, ownerGroup) && theClient.getRights().lowerThan(UserRightEnum.SUPER_USER)) {
			throw new AccessControlException(theClient.getLogin() + " can't read " + work.getUID());
		}

		return work.getStatus();
	}

    /**
     * This retrieves works UIDs for the specified client. Since 7.0.0 non
     * privileged users get their own jobs only
     *
     * @param command is the command to execute
     * @return null on error; a Collection of UID otherwise
     * @exception IOException
     *                is thrown general error
     * @exception InvalidKeyException
     *                is thrown on credential error
     * @exception AccessControlException
     *                is thrown on access rights violation
     */
    public Collection<UID> getAllJobs(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.LISTJOB);
        final StatusEnum status = ((XMLRPCCommandGetWorks) command).getStatus();

        if (theClient.getRights().higherOrEquals(UserRightEnum.ADVANCED_USER)) {
            return worksUID(theClient, status);
        }
        return ownerWorksUID(theClient, status);
    }

	/**
	 * This checks client rights and returns getJob(UserInterface, UID)
	 *
	 * @see #getJob(XMLRPCCommand)
	 */
	protected WorkInterface getJob(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		return work(command);
	}

	/**
	 * This retrieves categories UID
	 *
	 * @param command is the command to execute
	 * @return null on error; a Collection of UID otherwise
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 * @since 13.0.0
	 */
	public Collection<UID> getAllCategories(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTCATEGORY);
		return categoriesUID(theClient);
	}
	/**
	 * This calls category(command)
	 *
	 * @see #category(XMLRPCCommand)
	 * @since 13.0.0
	 */
	protected CategoryInterface getCategory(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		return category(command);
	}
    /**
     * This adds/updates an category
     *
     * @param u is the requestor
     * @param catItf is the category
     * @return true on success; false on DB error or group already exists
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     * @since 13.0.0
     */
    protected boolean addCategory(final UserInterface u, final CategoryInterface catItf)
            throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {

        final UserInterface theClient = checkClient(u, UserRightEnum.INSERTCATEGORY);
        final CategoryInterface category = category(theClient, catItf.getCategoryId());
        if (category != null) {
            category.updateInterface(catItf);
            update(theClient, UserRightEnum.INSERTCATEGORY, category);
            return true;
        }

        if (catItf.getUID() == null) {
            final UID uid = new UID();
            catItf.setUID(uid);
        }
        if (catItf.getOwner() == null) {
            catItf.setOwner(theClient.getUID());
        }
        insert(catItf);

        // read from DB, in case some values are null (and set to default by insert db)
        DBConnPoolThread.getInstance().putToCache(category(theClient, "maintable.uid='" + catItf.getUID() + "'"));

        return true;
    }
    /**
     * This retrieves market order UID
     *
     * @param command is the command to execute
     * @return null on error; a Collection of UID otherwise
     * @exception IOException
     *                is thrown general error
     * @exception InvalidKeyException
     *                is thrown on credential error
     * @exception AccessControlException
     *                is thrown on access rights violation
     * @since 13.1.0
     */
    public Collection<UID> getAllMarketOrders(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {

        final UserInterface theClient = checkClient(command, UserRightEnum.LISTMARKETORDER);
        return marketOrdersUID(theClient);
    }
    /**
     * This calls marketOrder(command)
     *
     * @see #marketOrder(XMLRPCCommand)
     * @since 13.1.0
     */
    protected MarketOrderInterface getMarketOrder(final XMLRPCCommand command)
            throws IOException, InvalidKeyException, AccessControlException {
        return marketOrder(command);
    }
    /**
     * This adds/updates a market order
     *
     * @param u is the requestor
     * @param moitf is the market order
     * @return true on success; false on DB error or group already exists
     * @exception IOException
     *                is thrown on DB access or I/O error
     * @exception InvalidKeyException
     *                is thrown on client integrity error (user unknown, bad
     *                password...)
     * @exception AccessControlException
     *                is thrown if client does not have enough rights
     * @since 13.1.0
     */
    protected boolean addMarketOrder(final UserInterface u, final MarketOrderInterface moitf)
            throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {

        if (!config.blockchainEnabled()) {
            throw new IOException("blockchain access disabled");
        }
        if ((moitf == null)
                || (moitf.getDirection() == null)
                || (moitf.getCategoryId() == null)
                || (moitf.getExpectedWorkers() == 0)
                || (moitf.getPrice() == 0) ) {
            throw new IOException("add market order error : missing values");
        }

        final UserInterface theClient = checkClient(u, UserRightEnum.INSERTMARKETORDER);

        if (category(theClient, moitf.getCategoryId()) == null) {
            throw new IOException("add market order error : unkown category " + moitf.getCategoryId());
        }

        try {
            final CommonConfiguration commonConfiguration = IexecConfigurationService.getInstance().getCommonConfiguration();
            final WorkerPoolConfig workerPoolConfig = commonConfiguration.getContractConfig().getWorkerPoolConfig();
            if((workerPoolConfig == null)
                    || (workerPoolConfig .getAddress() == null)
                    || (workerPoolConfig .getAddress().length() < 1)) {
                throw new IOException("worker pool addr is not available");
            }
            moitf.setWorkerPoolAddr(workerPoolConfig.getAddress());
            moitf.setWorkerPoolOwnerAddr(CredentialsService.getInstance().getCredentials().getAddress());
        } catch (final Exception e) {
            throw new IOException("add market order error: ", e);
        }

        final MarketOrderInterface marketOrder = marketOrder(theClient, moitf.getUID());
        if (marketOrder != null) {
            marketOrder.updateInterface(moitf);
            update(theClient, UserRightEnum.INSERTMARKETORDER, marketOrder);
            return true;
        }

        // set set expected workers
        moitf.setTrust(moitf.getTrust());
        if (moitf.getUID() == null) {
            final UID uid = new UID();
            moitf.setUID(uid);
        }
        if (moitf.getOwner() == null) {
            moitf.setOwner(theClient.getUID());
        }
        insert(moitf);

        return true;
    }
	/**
	 * This checks client integrity and returns getHost(UserInterface, UID)
	 *
	 * @see #getHost(XMLRPCCommand)
	 */
	protected HostInterface getHost(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.GETHOST);
		return host(theClient, command.getURI().getUID());
	}

	/**
	 * This method is remotly called by worker at start up time. It is inserted
	 * in the hashtable if not present. <BR>
	 * If host is allready in the hashtable which may happen if host has
	 * disconnected and is currently trying to reconnect, its datas are
	 * refreshed and its code returned. <BR>
	 * Host is added to SQL table if missing and SQL table is modified as
	 * needed.
	 *
	 * @param user
	 *            describes host user
	 * @param _host
	 *            describes remote host
	 * @return host(found or created)
	 */
	public HostInterface hostRegister(final UserInterface user, final HostInterface _host)
			throws IOException, InvalidKeyException, AccessControlException, URISyntaxException {

		final String hostName = _host.getName();
		final Date lastAlive = new Date(System.currentTimeMillis());

		if (_host.getUID() == null) {
			throw new IOException("_host UID must not be null!!!");
		}

		_host.setOwner(user.getUID());
		_host.setLastAlive(lastAlive);

		switch (user.getRights()) {
		case STANDARD_USER:
			_host.setAccessRights(XWAccessRights.USERALL);
			break;
		case VWORKER_USER:
		case WORKER_USER:
			if (user.getGroup() != null) {
				_host.setAccessRights(XWAccessRights.OWNERGROUP);
			} else {
				_host.setAccessRights(XWAccessRights.DEFAULT);
			}
			break;
		default:
			throw new AccessControlException("What is that host owner rights ??? :" + user.getRights());
		}

		try {
            HostInterface theHost = host(user, _host.getUID());

			if (theHost == null) {
                try {
                    logger.info(hostName + " not in DB; inserting " + _host.getUID().toString());
                    insert(_host);
                    theHost = _host;
                } catch (final Exception e) {
                    logger.exception(hostName + " can't create new host", e);
                    return null;
                }
            }

            logger.debug("hostRegister " + theHost.toXml());

            // we can't use theHost.updateInterface()
            // because we want to update data aggregated by the worker itself only
            theHost.setUploadBandwidth(_host.getUploadBandwidth());
            theHost.setDownloadBandwidth(_host.getDownloadBandwidth());
            theHost.setNbPing(_host.getNbPing());
            theHost.setAvgPing(_host.getAvgPing());
            theHost.setSharedApps(_host.getSharedApps());
            theHost.setSharedDatas(_host.getSharedDatas());
            theHost.setSharedPackages(_host.getSharedPackages());
            theHost.setIncomingConnections(_host.incomingConnections());
            theHost.setProject(_host.getProject());
            theHost.setIPAddr(_host.getIPAddr());
            theHost.setNatedIPAddr(_host.getNatedIPAddr());
            theHost.setJobId(_host.getJobId());
            theHost.setBatchId(_host.getBatchId());
            theHost.setAccessRights(_host.getAccessRights());
            theHost.setVersion(_host.getVersion());
            theHost.setOs(_host.getOs());
            theHost.setOsVersion(_host.getOsVersion());
            theHost.setJavaVersion(_host.getJavaVersion());
            theHost.setJavaDataModel(_host.getJavaDataModel());
            theHost.setCpu(_host.getCpu());
            theHost.setCpuModel(_host.getCpuModel());
            theHost.setCpuSpeed(_host.getCpuSpeed());
            theHost.setLastAlive(_host.getLastAlive());
            theHost.setAvailable(_host.isAvailable());
            theHost.setSgId(_host.getSgId());
            theHost.setPoolWorkSize(_host.getPoolWorkSize());
            theHost.setFreeTmp(_host.getFreeTmp());
            theHost.setTotalMem(_host.getTotalMem());
            theHost.setEthWalletAddr(_host.getEthWalletAddr());
            theHost.setWorkerPoolAddr(_host.getWorkerPoolAddr());

            return hostContribution(theHost);

		} catch (final Exception e) {
			logger.exception(e);
		}
		return null;
	}

    protected synchronized  void hostContribution(EthereumWallet workerWalletAddr) throws IOException {
        if (workerWalletAddr == null) {
            logger.info("hostContribution() : no wallet");
            return;
        }

        final HostInterface theHost = DBInterface.getInstance().host(workerWalletAddr);
        if(theHost == null) {
            logger.warn("onSubscription(" + workerWalletAddr +") : host not found");
            return;
        }

        hostContribution(theHost);
	}

    /**
     * This inserts the host associated to the provided wallet to a market order, if applicable
     * @param theHost
     * @throws IOException
     * @since 13.1.0
     */
	protected synchronized HostInterface hostContribution(final HostInterface theHost) throws IOException {

        if (theHost == null) {
            logger.info("hostContribution() : host is null");
            return null;
        }

        logger.debug("hostContribution " + theHost.toXml());

        if(theHost.getEthWalletAddr() == null) {
            logger.warn("onSubscription(" + theHost.getUID() +") : has no wallet");
            return theHost;
        }

        if (marketOrder(theHost.getMarketOrderUid()) == null) {
            theHost.leaveMarketOrder();
            theHost.update(false);
        }

        final EthereumWallet workerWalletAddr = new EthereumWallet(theHost.getEthWalletAddr());
        if (!theHost.canJoinMarketOrder()) {
            logger.info("hostContribution(" + workerWalletAddr + ") : cannot join marketorder");
            return theHost;
        }
        logger.debug("hostContribution(" + workerWalletAddr + ") : " + theHost.toXml());
        final MarketOrderInterface marketOrder = marketOrderStarvingResources(theHost.getWorkerPoolAddr());
        if (marketOrder == null) {
            logger.info("hostContribution(" + workerWalletAddr + ") : no starving market order");
            return theHost;
        }

        logger.debug("hostContribution(" + workerWalletAddr + ") : " + marketOrder.toXml());

        if (marketOrder.getWorkerPoolAddr().compareTo(theHost.getWorkerPoolAddr()) != 0) {
            logger.error("hostContribution(" + workerWalletAddr + ") : worker pool mismatch : "
                    + marketOrder.getWorkerPoolAddr() + " != "
                    + theHost.getWorkerPoolAddr());
            return theHost;
        }

        final Collection<HostInterface> hosts = hosts(workerWalletAddr, marketOrder);
        logger.debug("hostContribution(" + workerWalletAddr + ") : duplicated wallet " + (hosts == null ? 0 : hosts.size()));

        if (hosts != null) {
            for (HostInterface ahost : hosts) {
                logger.error("hostContribution(" + workerWalletAddr + ") : more than one wallet owner " + ahost.getUID());
                ahost.leaveMarketOrder(marketOrder);
                ahost.setActive(false);
                ahost.update(false);
            }
        }
        if (theHost.canJoinMarketOrder()) {
            logger.debug("hostContribution(" + workerWalletAddr + ") : joins market order "
                    + marketOrder.getUID());

            marketOrder.addWorker(theHost);
            marketOrder.update(false);
            theHost.update(false);

            final List<WorkInterface> worksList = (List) marketOrderWorks(marketOrder);
            if ((worksList != null) && worksList.size() > 0) {
                SchedulerPocoWatcherImpl.allowWorkerToContribute(worksList.get(0).getWorkOrderId(),
                        marketOrder,
                        theHost);
            }
        } else {
            logger.debug("hostContribution(" + workerWalletAddr + ") : don't want to contribute");
        }

		final Collection<HostInterface> workersInOrder = DBInterface.getInstance().hosts(marketOrder);
		final long workersInOrderSize = workersInOrder != null ? workersInOrder.size() : -1 ;

		if(workersInOrderSize <= marketOrder.getNbWorkers()) {
            logger.warn("hostContribution() resizing : marketOrder.getNbWorkers() = "
                    + marketOrder.getNbWorkers() + "; workers.size = " + workersInOrderSize);
            marketOrder.setNbWorkers(workersInOrderSize);
        } else {
		    final long diff =  workersInOrderSize - marketOrder.getNbWorkers();
		    long counterWorker = 0;
		    for(HostInterface worker : workersInOrder) {
                worker.leaveMarketOrder(marketOrder);
                counterWorker++;
                if(counterWorker >= diff) {
                    break;
                }
            }
        }

        if (marketOrder.canBeCreated()) {
            final ActuatorService actuatorService = ActuatorService.getInstance();
            marketOrder.decRemaining();
            marketOrder.update(false);

            BigInteger marketOrderIdx = null;
            for(int createTry = 0; createTry < 3 && marketOrderIdx == null; createTry++) {

                logger.debug("hostContribution(" + workerWalletAddr + ") : trying to create marketorder " + createTry);
                marketOrderIdx = actuatorService.createMarketOrder(BigInteger.valueOf(marketOrder.getCategoryId()),
                        BigInteger.valueOf(marketOrder.getTrust()),
                        BigInteger.valueOf(marketOrder.getPrice()),
                        BigInteger.valueOf(marketOrder.getVolume()));
                if (marketOrderIdx == null) {
                    try {
                        logger.debug("createMarketOrder; will retry in 10s");
                        Thread.sleep(10000);
                    } catch (final InterruptedException e) {
                    }
                }
            }
            if (marketOrderIdx != null) {
                theHost.setAvailable();
                marketOrder.setMarketOrderIdx(marketOrderIdx.longValue());
                logger.debug("hostContribution(" + workerWalletAddr + ") : marketorder created " + marketOrderIdx);
            } else {
                logger.error("hostContribution(" + workerWalletAddr + ") : cant create marketorder " + marketOrder.getUID());
                marketOrder.setErrorMsg("cant create marketorder");
                marketOrder.setError();
                theHost.leaveMarketOrder(marketOrder);
                final List<HostInterface> workers = (List) hosts(marketOrder);
                for (HostInterface w : workers) {
                    w.leaveMarketOrder(marketOrder);
                    w.update();
                }
            }
        }
        else {
            if (marketOrder.getStatus() == StatusEnum.ERROR)
                logger.debug("hostContribution(" + workerWalletAddr + ") : marketorder ERROR");
            else {
                logger.debug("hostContribution(" + workerWalletAddr + ") : marketorder still waiting");
                marketOrder.setWaiting();
            }
        }

        theHost.update(false);
        marketOrder.update(false);

        return theHost;
    }

	/**
	 * Set worker attribute.
	 *
	 * @param theClient
	 *            is the requesting user
	 * @param uid
	 *            is the worker UID to change flag for
	 * @param column
	 *            defines either TRACES or ACTIVE attribute to change
	 * @param flag
	 *            is the value to set the attribute to
	 * @return true on success; false otherwise.
	 */
	protected boolean changeWorker(final UserInterface theClient, final UID uid, final HostInterface.Columns column,
			final boolean flag) throws IOException {

		final HostInterface host = host(theClient, uid);
		if (host == null) {
			return false;
		}
		switch (column) {
		case TRACES:
			host.setTracing(flag);
			break;
		case ACTIVE:
			host.setActive(flag);
			break;
		}

		host.update();
		return true;
	}

	/**
	 * Set workers attribute.
	 *
	 * @param hosts
	 *            is a hashtable which contains host UID as key and their
	 *            dedicated activate flag as value.
	 * @param column
	 *            defines either TRACES or ACTIVE attribute to change
	 * @return true on success; false otherwise.
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	//	private boolean changeWorkers(final UserInterface client, final Hashtable<String, Boolean> hosts,
	//			final HostInterface.Columns column) throws IOException, InvalidKeyException, AccessControlException {
	//
	//		final UserInterface theClient = checkClient(client, UserRightEnum.SUPER_USER);
	//
	//		if (hosts.isEmpty()) {
	//			return false;
	//		}
	//		final Enumeration<String> enums = hosts.keys();
	//		while (enums.hasMoreElements()) {
	//			final UID uid = new UID(enums.nextElement());
	//			final Boolean flag = hosts.get(uid);
	//			if (!changeWorker(theClient, uid, column, flag.booleanValue())) {
	//				return false;
	//			}
	//		}
	//
	//		return true;
	//	}

	/**
	 * Set active attibute for a given worker
	 *
	 * @param command is the command to execute
	 * @return true on success; false otherwise.
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public boolean activateWorker(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		final UserInterface theClient = checkClient(command, UserRightEnum.SUPER_USER);
		final UID uid = command.getURI().getUID();
		boolean flag = ((XMLRPCCommandActivateHost) command).getActivation();
		return changeWorker(theClient, uid, HostInterface.Columns.ACTIVE, flag);
	}

	//	/**
	//	 * Set active flag for a given workers list.
	//	 *
	//	 * @param hosts
	//	 *            is a hashtable which contains host uid as key and their
	//	 *            dedicated activate flag as value.
	//	 * @return true on success; false otherwise.
	//	 */
	//	public boolean activateWorkers(final UserInterface client, final Hashtable<String, Boolean> hosts)
	//			throws IOException, InvalidKeyException, AccessControlException {
	//		return changeWorkers(client, hosts, HostInterface.Columns.ACTIVE);
	//	}

	//	/**
	//	 * This activate some workers.
	//	 *
	//	 * @param nbWorkers
	//	 *            is the number of workers to activate.
	//	 * @return the number of activated workers on success, -1 on error
	//	 * @exception IOException
	//	 *                is thrown general error
	//	 * @exception InvalidKeyException
	//	 *                is thrown on credential error
	//	 * @exception AccessControlException
	//	 *                is thrown on access rights violation
	//	 */
	//	public int activateWorkers(final UserInterface client, final int nbWorkers)
	//			throws InvalidKeyException, AccessControlException, IOException {
	//
	//		int count = 0;
	//
	//		final UserInterface theClient = checkClient(client, UserRightEnum.SUPER_USER);
	//		final Collection<HostInterface> hosts = hosts(theClient);
	//		for (final Iterator<HostInterface> enums = hosts.iterator(); enums.hasNext();) {
	//			final HostInterface host = enums.next();
	//			if (count < nbWorkers) {
	//				host.setActive(true);
	//				count++;
	//			} else {
	//				host.setActive(false);
	//			}
	//		}
	//		return count;
	//	}

	/**
	 * Set trace flag for a given workers list.
	 *
	 * @param hosts
	 *            is a hashtable which contains host uid as key and their
	 *            dedicated trace flag as value.
	 * @return true on success; false otherwise.
	 */
	//	public boolean traceWorkers(final UserInterface client, final Hashtable<String, Boolean> hosts)
	//			throws IOException, InvalidKeyException, AccessControlException {
	//		return changeWorkers(client, hosts, HostInterface.Columns.TRACES);
	//	}

	/**
	 * This retrieves all registered workers (all workers that have been
	 * connected at least once)
	 *
	 * @return a Collection of UID
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	private Collection<UID> getRegisteredWorkers(final UserInterface theClient)
			throws IOException, InvalidKeyException, AccessControlException {

		final Vector<UID> ret = new Vector<>(100, 50);

		final Collection<HostInterface> hosts = hosts(theClient);
		final Iterator<HostInterface> enums = hosts.iterator();
		while (enums.hasNext()) {
			ret.add(enums.next().getUID());
		}
		return ret;
	}

	/**
	 * This retrieves all alive workers (all workers that have been connected
	 * this last ALIVE period)
	 *
	 * @return a Collection of UID
	 * @since 5.8.0
	 * @exception IOException
	 *                is thrown general error
	 * @exception InvalidKeyException
	 *                is thrown on credential error
	 * @exception AccessControlException
	 *                is thrown on access rights violation
	 */
	public Collection<UID> getAliveWorkers(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {

		final UserInterface theClient = checkClient(command, UserRightEnum.LISTHOST);
		return hostsUID(theClient, "(unix_timestamp(now())-unix_timestamp(lastalive) < 1000)");
	}

	/**
	 * This retrieves server the provided worker should connect to, if any. <br>
	 * The new server is provided to worker through 'Alive' signal. April 4th,
	 * 2003 : no policy is defined yet :(
	 *
	 * @param workerUID
	 *            is the worker host UID
	 * @since v1r2-rc1(RPC-V)
	 */
	public String getServer(final UID workerUID) {
		return null;
	}
}
