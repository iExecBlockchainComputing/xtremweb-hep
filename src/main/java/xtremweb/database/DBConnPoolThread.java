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

/**
 * DBConnPoolThread.java
 * This class connects to DB and executes query.
 * This class extends java.lang.Thread to execute SQL query in background
 *
 * Created: Sun Jun 20 2011
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr ">Oleg Lodygensky</a>
 * @since 7.5.0
 */

package xtremweb.database;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import xtremweb.common.*;
import xtremweb.communications.URI;

/**
 * This is a threaded version of DBConnPool to improve performances This acts as
 * DBConnPool for insert, select and delete This uses a FIFO for update
 *
 * @author Oleg Lodygensky
 * @since 7.5.0
 */
public class DBConnPoolThread extends Thread {

	/**
	 * This is a cache to reduce MySQL accesses
	 * This has been moved from DBInterface to here since 13.0.4
	 * @since 13.0.4
	 */
	private Cache cache = null;

	/**
	 * This stores the table history suffix name = "_history" This was in
	 * xtremweb.dispatcher.TableRow until 9.0.0
	 *
	 * @since 9.0.0
	 */
	public static final String HISTORYSUFFIX = "_history";

	/**
	 * Configurator
	 *
	 * @since 9.0.0
	 */
	private XWConfigurator config;

	private Logger logger;
	/**
	 * This contains the complete reference to the database location
	 */
	private String dburl;

	/**
	 * This contains the number of current connections
	 */
	private int nbConnections;

	/** Maximunm number of connections */
	private int MAXX_CONNECTIONS = 10;
	/**
	 * This is a synchronized list containing connections to database
	 */
	private List<Connection> connPool;
	/**
	 * This is a synchronized list used as a FIFO, containing update requests
	 *
	 * @since 7.5.0
	 */
	private Hashtable<URI, String> updateFifo;

	/**
	 * This is the singleton
	 */
	private static DBConnPoolThread instance = null;

	/**
	 * This contains this class name
	 */
	private String className;

	/**
	 * This is the default and only constructor
	 */
	public DBConnPoolThread(final XWConfigurator c)  throws IOException {

		if (getInstance() != null) {
			return;
		}

		logger = new Logger(this);

		config = c;

		MAXX_CONNECTIONS = config.getInt(XWPropertyDefs.DBCONNECTIONS);
		logger.config("MAXX_CONNECTIONS = " + MAXX_CONNECTIONS);
		try {
			dburl = "jdbc:" + config.getProperty(XWPropertyDefs.DBVENDOR) + "://"
					+ config.getProperty(XWPropertyDefs.DBHOST) + "/" + config.getProperty(XWPropertyDefs.DBNAME);
			logger.config("org.gjt.mm.mysql.Driver");
			Class.forName("org.gjt.mm.mysql.Driver");
		} catch (final java.lang.ClassNotFoundException e) {
			logger.fatal("ClassNotFoundException: " + e.getMessage());
		}

		className = getClass().getName();

		logger.config("dburl      = '" + dburl + "' " + "dbuser     = '" + config.getProperty(XWPropertyDefs.DBUSER)
				+ "' dbpassword = '" + config.getProperty(XWPropertyDefs.DBPASS) + "'");

		connPool = Collections.synchronizedList(new LinkedList<Connection>());
		updateFifo = new Hashtable<>();

		for (int i = 0; i < MAXX_CONNECTIONS; i++) {

			try {
				final Connection conn = getConnection(dburl, config.getProperty(XWPropertyDefs.DBUSER.toString()),
						config.getProperty(XWPropertyDefs.DBPASS.toString()));
				if (conn != null) {
					pushConnection(conn);
				}
			} catch (final Exception e) {
				logger.fatal(e.toString());
			}
		}

		logger.info("Connection to database " + dburl + " is ok, " + connPool.size() + " created");
		nbConnections = 0;

		checkAppTypes();

		cache = new Cache(config);

		if (getInstance() == null) {
			setInstance(this);
		}
	}

	/**
	 * This creates a new database connector
	 */
	private Connection getConnection(final String dbname, final String dbuser, final String dbpassword)
			throws SQLException {

		return DriverManager.getConnection(dbname, dbuser, dbpassword);
	}

	/**
	 * This waits until nbConnections &lt; MAXX_CONNECTIONS; then this
	 * decrements nbConnections and create a new Connection
	 *
	 * @see #nbConnections
	 */
	private synchronized Connection popConnection() {

		while (connPool.size() <= 0) {
			try {
				logger.finest("DBConnPool#popConnection sleeping (pool size <= 0)");
				wait();
				logger.finest("DBConnPool#popConnection woken up");
			} catch (final InterruptedException e) {
			}
		}
		final Connection conn = connPool.remove(0);
		notifyAll();
		return conn;
	}

	/**
	 * This increments nbConnections
	 *
	 * @see #nbConnections
	 */
	private synchronized void pushConnection(final Connection conn) {
		nbConnections--;
		if (nbConnections < 0) {
			nbConnections = 0;
		}
		connPool.add(conn);
		notify();
	}

	/**
	 * This is the main loop
	 */
	@Override
	public synchronized void run() {
		while (true) {
			try {
				logger.finest("DBConnPoolThread is waiting");
				this.wait();
				logger.finest("DBConnPoolThread woken up");
			} catch (final InterruptedException e) {
			}
			URI key = null;
			try {
//				while (!updateFifo.isEmpty()) {
//					executeQuery(updateFifo.remove(0), null);
//				}
                for (Enumeration<URI> enums = updateFifo.keys(); enums.hasMoreElements();) {
                    key = enums.nextElement();
                    executeQuery(updateFifo.remove(key), null);
                }
			} catch (final Exception e) {
                logger.exception(e);
                if (key != null) {
                    try {
                        removeFromCache(key);
                    } catch (IOException e1) {
                    }
                }
            }
		}
	}

	/**
	 * This checks application types defined in DB by scripts:
	 * <ul>
	 * <li>xwhep-core-tables-create-tables.sql</li>
	 * </ul>
	 * and/or
	 * <ul>
	 * <li>xwhep-core-tables-from-8-create-new-tables-columns-fk.sql</li>
	 * </ul>
	 *
	 * @since 9.0.0
	 */
	private final void checkAppTypes() {
	}

	/**
	 * This executes SQL query
	 *
	 * @param query
	 *            is the SQL query to execute
	 * @param row
	 *            is the row type
	 * @return a vector of rows found in DB, or null if no row found
	 */
	protected final synchronized <T extends Table> Collection<T> executeQuery(final String query, final T row)
			throws IOException {
		return executeQuery(null, query, row);
	}

	/**
	 * This executes SQL query
	 *
	 * @param query
	 *            is the SQL query to execute
	 * @param row
	 *            is the row type
	 * @return a vector of rows found in DB, or null if no row found
	 */
	protected final synchronized <T extends Table> Collection<T> executeQuery(final Connection conn, final String query,
			final T row) throws IOException {

	    if(query == null) {
	        return null;
        }

		// if (row == null) {
		// Thread.currentThread().dumpStack();
		// throw new IOException("executeQuery : row is null");
		// }
		final MileStone mileStone = new MileStone(xtremweb.database.DBConnPoolThread.class);

		// remove comma from milestone to be able to generate CSV files
		// using benchmarks/milestone/scripts/parse.awk
		final String mq = query.substring(0, Math.min(query.length(), 80)).replace(',', '_');
		mileStone.println("<executeQuery>" + mq + "...");

		Connection dbConn = conn;
		if (dbConn == null) {
			try {
				dbConn = popConnection();
			} catch (final Exception e) {
				logger.exception(e);
				logger.fatal(e.toString());
			}
		}

		ResultSet rs = null;
		try (final Statement stmt = dbConn.createStatement()) {
			Vector<T> ret = null;

			logger.debug(query);

			if (stmt.execute(query)) {
				rs = stmt.getResultSet();
			}
			if (rs == null) {
				rs = stmt.getGeneratedKeys();
			}
			if (rs == null) {
				throw new IOException("can't get SQL results");
			}

			ret = new Vector<>();
			if (row != null) {
				while (rs.next()) {
					@SuppressWarnings("unchecked")
					final T theRow = (T) row.getClass().newInstance();
					theRow.fill(rs);
					ret.add(theRow);
				}
			}

			if (ret.isEmpty()) {
				ret = null;
			}

			return ret;

		} catch (final Exception ex2) {
			logger.exception("ExecuteQuery  (" + query + ")", ex2);
			mileStone.println("<executeQueryError />");
            if (row != null) {
                removeFromCache(row.getUID());
            }
            throw new IOException(ex2);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (final Exception e) {
				logger.exception(e);
			}
			if (conn == null) {
				pushConnection(dbConn);
			}
			mileStone.println("</executeQuery>");
			notifyAll();
		}
	}

	/**
	 * This executes SQL query
	 *
	 * @param query
	 *            is the SQL query to execute
	 * @return a vector of rows found in DB, or null if no row found
	 */
	protected synchronized Collection<UID> queryUID(final String query) throws IOException {

		final MileStone mileStone = new MileStone(xtremweb.database.DBConnPoolThread.class);

		if (query.length() < 80) {
			mileStone.println("<executeQuery>" + query);
		} else {
			mileStone.println("<executeQuery>" + query.substring(0, 80) + "...");
		}

		Connection dbConn = null;
		try {
			dbConn = popConnection();
		} catch (final Exception e) {
			logger.exception(e);
			logger.fatal(e.toString());
		}

		ResultSet rs = null;

		try (Statement stmt = dbConn.createStatement()) {

			logger.finest(query);

			if (stmt.execute(query)) {
				rs = stmt.getResultSet();
			}
			if (rs == null) {
				rs = stmt.getGeneratedKeys();
			}
			if (rs == null) {
				throw new IOException("can't get SQL results");
			}

			final Vector<UID> ret = new Vector<>();

			while (rs.next()) {
				ret.add(new UID(rs.getString("theuid")));
			}

			if (ret.isEmpty()) {
				return null;
			}
			return ret;
		} catch (final Exception e) {
			logger.exception("ExecuteQuery  (" + query + ")", e);

			mileStone.println("<executeQueryError />");
			throw new IOException(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (final Exception e2) {
				logger.exception(e2);
			}

			pushConnection(dbConn);
			mileStone.println("</executeQuery>");
			notifyAll();
		}
	}

	/**
	 * This updates the objects contained in the provided vector in the database
	 * table, if needed
	 *
	 * @param rows
	 *            is the vector of rows to update
	 */
	public <T extends Table> void update(final Collection<T> rows) throws IOException {
		update(rows, null);
	}

	/**
	 * This calls update(row, null, true)
	 *
	 * @see #update(Table, String, boolean)
	 */
	public <T extends Table> void update(final T row) throws IOException {
		update(row, null, true);
	}

	/**
	 * This updates objects in the database table
	 *
	 * @param rows
	 *            is a vector of rows to update
	 * @param criterias
	 *            is string to use in SQL SELECT WHERE clause if not null
	 *            otherwise TableRow#criterias() is used
	 */
	public <T extends Table> void update(final Collection<T> rows, final String criterias) throws IOException {

	    if(rows == null) {
	        return;
        }

		try {
			final Iterator<T> rowit = rows.iterator();
			while (rowit.hasNext()) {
				final T row = rowit.next();
				if (row == null) {
					continue;
				}
				update(row, criterias, true);
			}
		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException(e.toString());
		}
	}

	/**
	 * This calls update(row, criteria, true)
	 *
	 * @see #update(Table, String, boolean)
	 */
	public synchronized <T extends Table> void update(final T row, final String criteria, final boolean pool)
			throws IOException {

	    if(!row.isDirty()) {
	        return;
        }
		try {
			final String rowset = row.toString();
			final String theCriteria = criteria != null ? criteria : row.criteria();

			if ((theCriteria == null) || (rowset == null)) {
				throw new IOException("unable to get update criteria");
			}

			final String query = "UPDATE " + config.getProperty(XWPropertyDefs.DBNAME) + "." + row.tableName() + " SET "
					+ rowset + " WHERE " + theCriteria;

			if (pool == true) {
				logger.finest("updateFifo.add(" + query + ")");
				updateFifo.put(XWTools.newURI(row.getUID()), query);
			} else {
				executeQuery(query, row);
			}
			row.setDirty(false);
            putToCache(row);
            notify();

		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException(e.toString());
		}
	}

	/**
	 * This creates a FROM sql statement part for the given row in the form
	 * "dbname.t1 [...][,dbname.t2 [...]]"
	 *
	 * @since 7.0.0
	 * @param row
	 *            is the TableRow to use in SQL statement
	 * @return a String containing the FROM SQL statement part
	 */
	private String rowTableNames(final Type row) throws IOException {
		if (row == null) {
			throw new IOException("row is null ?!?!");
		}
		return config.getProperty(XWPropertyDefs.DBNAME) + "."
				+ row.fromTableNames().replaceAll(",", "," + config.getProperty(XWPropertyDefs.DBNAME) + ".");
	}

	/**
	 * This select rows from table
	 *
	 * @param row
	 *            is the row type
	 * @param criterias
	 *            is string to use in SQL SELECT WHERE clause if not null
	 *            otherwise TableInterface#criteria() is used
	 * @see Table#criteria()
	 * @since 10.0.0
	 */
	public <T extends Table> T selectOne(final T row, final String criterias) throws IOException {

		final Vector<T> v = (Vector<T>) select(row, criterias, 1);
		if ((v != null) && (!v.isEmpty())) {
			return v.get(0);
		}
		return null;
	}

	/**
	 * This rows from table
	 *
	 * @param row
	 *            is the row type
	 * @param criterias
	 *            is string to use in SQL SELECT WHERE clause if not null
	 *            otherwise TableInterface#criteria() is used
	 * @see Table#criteria()
	 * @since 10.0.0
	 */
	public <T extends Table> Collection<T> selectAll(final T row, final String criterias) throws IOException {

		return select(row, criterias, config.requestLimit());
	}

	/**
	 * This select rows from table
	 *
	 * @param row
	 *            is the row type
	 * @param criterias
	 *            is string to use in SQL SELECT WHERE clause if not null
	 *            otherwise TableInterface#criteria() is used
	 * @param limit
	 *            is the max expected amount of rows
	 * @see Table#criteria()
	 */
	public <T extends Table> Collection<T> select(final T row, final String criterias, final int limit)
			throws IOException {

		final String groupBy = row.groupBy();
		final String rowcriteria = row.criteria();
		String conditions = null;
		if (rowcriteria != null) {
			conditions = rowcriteria;
			if ((criterias != null) && (criterias.length() > 0)) {
				conditions += " AND " + criterias;
			}
		} else {
			if ((criterias != null) && (criterias.length() > 0)) {
				conditions = criterias;
			}
		}
		final String query = "SELECT " + row.rowSelection() + " FROM " + rowTableNames(row)
				+ (conditions == null ? "" : " WHERE " + conditions) + (groupBy == null ? "" : " GROUP BY " + groupBy)
				+ " LIMIT " + limit;

		return executeQuery(query, row);
	}

	/**
	 * This reads from DB
	 *
	 * @param row
	 *            is the row type
	 */
	public <T extends Table> Collection<T> select(final T row) throws IOException {
		return select(row, null, config.requestLimit());
	}

	/**
	 * This retrieves UID from table
	 *
	 * @param row
	 *            is the row type
	 * @param criterias
	 *            is string to use in SQL SELECT WHERE clause if not null
	 *            otherwise TableInterface#criteria() is used
	 * @see Table#criteria()
	 */
	public <T extends Table> Collection<UID> selectUID(final T row, final String criterias) throws IOException {

		if (row.criteria() == null) {
			throw new IOException("row.criteria == null ?!?");
		}

		try {
			final String rowcriteria = row.criteria();
			String conditions = null;
			if (rowcriteria != null) {
				conditions = rowcriteria;
				if (criterias != null) {
					conditions += " AND " + criterias;
				}
			} else {
				if (criterias != null) {
					conditions = criterias;
				}
			}
			final String query = "SELECT " + row.rowSelection() + " FROM " + rowTableNames(row)
					+ (conditions == null ? "" : " WHERE " + conditions) + " LIMIT " + config.requestLimit();

			return (Vector<UID>) queryUID(query);
		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException(e.toString());
		}
	}

	/**
	 * This inserts this object in DB and cache
	 *
	 * @param row
	 *            is the row to insert
	 */
	public synchronized <T extends Table> void insert(final T row) throws IOException, URISyntaxException {

		final String criteria = row.valuesToString();

		if (criteria == null) {
			throw new IOException("unable to get insertion criteria");
		}

		final String query = "INSERT INTO " + config.getProperty(XWPropertyDefs.DBNAME) + "." + row.tableName() + ("(")
				+ row.getColumns() + (") ") + " VALUES (" + criteria + ")";

		executeQuery(query, row);
		//updateFifo.put(XWTools.newURI(row.getUID()), query);
        putToCache(row);
		notify();
	}

	/**
	 * Since XWHEP 1.0.0, this does not delete row from table but updates the
	 * row and sets isdeleted flag to true. Hence the row stays in table.
	 *
	 * @param row
	 *            is the to delete
	 */
	public synchronized <T extends Table> void delete(final T row) throws IOException {

		try {
			final String criteria = row.criteria();
			if (criteria == null) {
				throw new IOException("unable to get delete criteria");
			}

			String query = "INSERT INTO " + config.getProperty(XWPropertyDefs.DBNAME) + "." + row.tableName()
					+ HISTORYSUFFIX + " SELECT * FROM " + config.getProperty(XWPropertyDefs.DBNAME) + "."
					+ row.tableName() + " WHERE " + criteria;
			executeQuery(query, row);

			query = "DELETE FROM " + config.getProperty(XWPropertyDefs.DBNAME) + "." + row.tableName() + " WHERE "
					+ criteria;

            executeQuery(query, row);
			notify();
		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException(e.toString());
		} finally {
            removeFromCache(row);
        }
	}

	/**
	 * This set all server works to WAITING status
	 *
	 * @param serverName
	 *            is the server name
	 */
	public synchronized void unlockWorks(final String serverName) {
		try {
			final String query = "UPDATE " + config.getProperty(XWPropertyDefs.DBNAME) + ".works SET "
					+ WorkInterface.Columns.STATUS.toString() + "='" + StatusEnum.WAITING + "',"
					+ WorkInterface.Columns.SERVER.toString() + "='NULL'  WHERE "
					+ WorkInterface.Columns.SERVER.toString() + "='" + serverName + "' and(("
					+ WorkInterface.Columns.STATUS.toString() + "='" + StatusEnum.WAITING + "' or "
					+ WorkInterface.Columns.STATUS.toString() + "='" + StatusEnum.PENDING + "') OR ISNULL(status))";

			executeQuery(query, null);
			notify();
		} catch (final Exception e) {
			logger.exception(e);
		}
	}

	/**
	 * @return the instance
	 */
	public static DBConnPoolThread getInstance() {
		return instance;
	}

	/**
	 * @param instance
	 *            the instance to set
	 */
	public static void setInstance(final DBConnPoolThread instance) {
		DBConnPoolThread.instance = instance;
	}

	/**
	 * This caches an object interface
	 *
	 * @since 13.0.4
	 */
	public void putToCache(final Table itf) {

		if (itf == null) {
			return;
		}

		try {
			final UID uid = itf.getUID();
			final URI uri = XWTools.newURI(uid);
			cache.add(itf, uri);
		} catch (final Exception e) {
			logger.exception("can't put to cache", e);
		}
	}

	/**
	 * This retrieves an object from cache
	 *
	 * @since 13.0.4
	 */
	public <T extends Table> T getFromCache(final UID uid, final T row) {
		if (uid == null) {
			return null;
		}
		try {
			final URI uri = XWTools.newURI(uid);
			if (uri == null) {
				return null;
			}
			return getFromCache(uri, row);
		} catch (final Exception e) {
			logger.exception("can't retrieve from cache", e);
			return null;
		}
	}
	/**
	 * This retrieves an object from cache
	 *
	 * @since 8.2.0
	 */
	public <T extends Table> T getFromCache(final URI uri, final T row) {
		final Table itf = getFromCache(uri);
		if (itf == null) {
			return null;
		}
		//
		// we must check interface type because the cache contains all object
		// types
		//
		if (itf.getClass().toString().compareTo(row.getClass().toString()) != 0) {
			return null;
		}
		return (T) itf;
	}
	/**
	 * This retrieves an object from cache
	 *
	 * @since 13.0.4
	 */
	public <T extends Table> T getFromCache(final UserInterface u, final UID uid, final T row)
			throws IOException, AccessControlException {

		if (uid == null) {
			return null;
		}
		try {
			final URI uri = XWTools.newURI(uid);
			return getFromCache(u, uri, row);
		} catch (final URISyntaxException e) {
			logger.exception(e);
			return null;
		}
	}
	/**
	 * This retrieves an object from cache
	 *
	 * @since 7.4.0
	 */
	private <T extends Table> T getFromCache(final UserInterface u, final URI uri, final T row)
			throws IOException, AccessControlException {
		if (uri == null) {
			return null;
		}
		final Table itf = getFromCache(u, uri);
		if (itf == null) {
			return null;
		}
		return getFromCache(uri, row);
	}
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
	 * This retrieves an object independently of access rights from cache
	 * or from DB
	 *
	 * @param uid
	 *            is the UID of the host to retrieve
	 * @since 13.0.0
	 */
	public <T extends Table> T object(final T t, final UID uid) throws IOException {
		T ret = getFromCache(uid, t);
		if (ret != null) {
			return ret;
		}
		ret = select(t, uid);
		return ret;
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
     * This removes an object from cache
     *
     * @since 13.0.4
     */
    public <T extends Table> void removeFromCache(final T row) {
        try {
            removeFromCache(row.getUID());
        } catch (final Exception e) {
            logger.exception("can't remove from cache", e);
        }
    }

    /**
	 * This removes an object from cache
	 *
	 * @since 13.0.4
	 */
	public void removeFromCache(final UID uid) {
		try {
			final URI uri = XWTools.newURI(uid);
			removeFromCache(uri);
		} catch (final Exception e) {
			logger.exception("can't remove from cache", e);
		}
	}

	/**
	 * This caches an object interface
	 *
	 * @since 13.0.4
	 */
	public Table getFromCache(final URI uri) {
		return cache.get(uri);
	}
	/**
	 * This removes an object from cache
	 *
	 * @since 13.0.4
	 */
	public void removeFromCache(final URI uri) throws IOException {
		if (uri == null) {
			return;
		}
		cache.remove(uri);
	}
}
