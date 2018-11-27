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

package xtremweb.database;

import java.io.IOException;

import xtremweb.common.Logger;
import xtremweb.common.UserInterface;
import xtremweb.common.XWConfigurator;

/**
 * This is a decorator pattern aiming to implement SQL requests used to retrieve
 * objects from DB.
 *
 * @author <A HREF="mailto:lodygens /at\ lal.in2p3.fr">Oleg Lodygensky </A>
 * @since 5.8.0
 */
public abstract class SQLRequest {

	private Logger logger;
	/**
	 * This aliased UID of the main table
	 */
	public static final String UIDLABEL = "theuid";
	/**
	 * This aims to aliased main table name
	 */
	public static final String MAINTABLEALIAS = "maintable";
	/**
	 * This stores table names used in the SQL SELECT FROM statement
	 */
	private String tableNames;
	/**
	 * This stores row names used in the SQL SELECT statement
	 */
	private ColumnSelection columnSelection;
	/**
	 * This stores base criteria used in the SQL SELECT FROM WHERE statement
	 */
	private String criterias;
	/**
	 * This stores some more criteria
	 */
	private String moreCriterias;
	/**
	 * This is the requesting user
	 */
	private UserInterface user;
	/**
	 * This stores the table name from which rows are retrieved
	 */
	private String tableName;
	/**
	 * This contains the DB name This is set externally by DBInterface
	 *
	 * @see xtremweb.dispatcher.DBInterface#DBInterface(XWConfigurator)
	 */
	private static String dbName = null;

	public static void setDbName(final String n) {
		dbName = n;
	}

	public static String getDbName() {
		return dbName;
	}

	/**
	 * This is true if using an HSQLDB engine; false otherwise This is set
	 * externally by DBInterface
	 *
	 * @see xtremweb.dispatcher.DBInterface#DBInterface(XWConfigurator)
	 */
	private static boolean hsqldb = false;

	public static void setHsqldb(final boolean n) {
		hsqldb = n;
	}

	public static boolean getHsqldb() {
		return hsqldb;
	}

	protected SQLRequest() {
		tableNames = null;
		tableName = null;
		user = null;
		columnSelection = null;
		criterias = null;
		moreCriterias = null;
		logger = new Logger(this);
	}

	/**
	 * This retrieves "FROM" SQL statement table names
	 *
	 * @return TABLENAMES
	 */
	public String fromTableNames() {
		return String.format(tableNames, tableName);
	}

	/**
	 * This aims to retrieve rows for the "SELECT" SQL statement.
	 *
	 * @return "tablename.uid", or "tablename.*" depending on selected columns
	 */
	public String rowSelection() throws IOException {
		return columnSelection.selectionString();
	}

	/**
	 * This retrieves this SQL criteria
	 *
	 * @return a String containing SQL criteria
	 */
	public abstract String getFullCriterias() throws IOException;

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @param logger
	 *            the logger to set
	 */
	public void setLogger(final Logger logger) {
		this.logger = logger;
	}

	/**
	 * @return the tableNames
	 */
	public String getTableNames() {
		return tableNames;
	}

	/**
	 * @param tableNames
	 *            the tableNames to set
	 */
	public void setTableNames(final String tableNames) {
		this.tableNames = tableNames;
	}

	/**
	 * @return the columnSelection
	 */
	public ColumnSelection getColumnSelection() {
		return columnSelection;
	}

	/**
	 * @param columnSelection
	 *            the columnSelection to set
	 */
	public void setColumnSelection(final ColumnSelection columnSelection) {
		this.columnSelection = columnSelection;
	}

	/**
	 * @return the criteria
	 */
	public String getCriterias() {
		return criterias;
	}

	/**
	 * @param criterias
	 *            the criteria to set
	 */
	public void setCriterias(final String criterias) {
		this.criterias = criterias;
	}

	/**
	 * @return the moreCriterias
	 */
	public String getMoreCriterias() {
		return moreCriterias;
	}

	/**
	 * @param moreCriterias
	 *            the moreCriterias to set
	 */
	public void setMoreCriterias(final String moreCriterias) {
		this.moreCriterias = moreCriterias;
	}

	/**
	 * @return the user
	 */
	public UserInterface getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(final UserInterface user) {
		this.user = user;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @param tableName
	 *            the tableName to set
	 */
	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}
}
