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

import xtremweb.common.AppInterface;
import xtremweb.common.DataInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.Logger;
import xtremweb.common.StatusEnum;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;
import xtremweb.common.WorkInterface;
import xtremweb.security.XWAccessRights;

/**
 * This is a decorator pattern aiming to implement SQL requests used to retrieve
 * objects from DB. This implements the SQL request to retrieve works for a
 * given host (OS and architecture) and its identity that fulfills security
 * access according to the given host identity. This has two possible SQL
 * criteria, depending on host configuration. <br />
 *
 * @author <A HREF="mailto:lodygens /at\ lal.in2p3.fr">Oleg Lodygensky </A>
 * @since 5.8.0
 */
public class SQLRequestWorkRequest extends SQLRequest {

	/**
	 * This contains table names for the FROM part of the SQL request. There
	 * must not be a space after the comma
	 */
	protected static final String TABLENAMES = WorkInterface.TABLENAME + " as " + MAINTABLEALIAS + ","
			+ AppInterface.APPTABLENAME + "," + UserInterface.TABLENAME + "," + HostInterface.TABLENAME;

	/**
	 * This is used if host.acceptBin == true. This helps to retrieve any
	 * application for the worker, including apps to be deployed (the worker
	 * will have to download binary) or shared apps (the ones the worker
	 * declares as sharing)
	 */
	protected static final String WORKREQUESTCRITERIAS = " AND maintable.status='%s'"
			+ " AND (ISNULL(maintable.LISTENPORT)      OR maintable.LISTENPORT='' OR %s)"
			+ " AND (ISNULL(maintable.EXPECTEDHOSTUID) OR maintable.EXPECTEDHOSTUID='%s')"
			+ " AND ( ISNULL(maintable.MINCPUSPEED)          OR (maintable.MINCPUSPEED          <= %d))"
			+ " AND ( ISNULL(maintable.MINMEMORY)            OR (maintable.MINMEMORY            <= %d))"
			+ " AND ( ISNULL(maintable.MINFREEMASSSTORAGE)   OR (maintable.MINFREEMASSSTORAGE   <= %d))"
			+ " AND ((NOT (ISNULL(apps.%s) AND ISNULL(apps.JAVAURI)) AND apps.TYPE='DEPLOYABLE') OR apps.TYPE IN (%s) )"
			+ " AND (ISNULL(apps.NEEDEDPACKAGES)             OR  (apps.NEEDEDPACKAGES='')   OR  (apps.NEEDEDPACKAGES IN (%s)))"
			+ " AND (ISNULL(maintable.MARKETORDERUID)        OR  ((hosts.HASCONTRIBUTED='false') AND (maintable.MARKETORDERUID = hosts.MARKETORDERUID)))"
			+ " AND hosts.available='true' and hosts.active='true'"
            + " AND (maintable.appuid=apps.uid)";

	/**
	 * This is used if host.acceptBin == false. This retrieves job referring
	 * shared application only (the worker downloads no binary)
	 *
	 * @since 8.0.0
	 */
	protected static final String WORKREQUESTCRITERIAS_NOBIN = " AND maintable.status='%s'"
			+ " AND ((ISNULL(maintable.LISTENPORT))      OR maintable.LISTENPORT='' OR %s)"
			+ " AND ((ISNULL(maintable.EXPECTEDHOSTUID)) OR maintable.EXPECTEDHOSTUID='%s')"
			+ " AND ( (ISNULL(maintable.MINCPUSPEED))          OR (maintable.MINCPUSPEED          <= %d))"
			+ " AND ( (ISNULL(maintable.MINMEMORY))            OR (maintable.MINMEMORY            <= %d))"
			+ " AND ( (ISNULL(maintable.MINFREEMASSSTORAGE))   OR (maintable.MINFREEMASSSTORAGE   <= %d))"
			+ " AND (apps.TYPE IN (%s))"
            + " AND hosts.available='true' and hosts.active='true'"
            + " AND (ISNULL(apps.NEEDEDPACKAGES)             OR  (apps.NEEDEDPACKAGES='')   OR  (apps.NEEDEDPACKAGES IN (%s)))";

	/**
	 * This concatenates SQLRequestAccessible.CRITERIAS and
	 * WORKREQUESTCRITERIAS_NOBIN
	 */
	private static final String CRITERIAS = SQLRequestAccessible.SQLCRITERIAS + WORKREQUESTCRITERIAS;
	/**
	 * This concatenates SQLRequestAccessible.CRITERIAS and
	 * WORKREQUESTCRITERIAS_NOBIN
	 */
	private static final String CRITERIAS_NOBIN = SQLRequestAccessible.SQLCRITERIAS + WORKREQUESTCRITERIAS_NOBIN;

	/**
	 * This is the equivalent of CRITERIAS for HyperSQL This is only because
	 * HSQLDB does not accept '&amp;' as bitwise operator :(
	 *
	 * @see #CRITERIAS
	 */
	private static final String CRITERIAS_HSQL = SQLRequestAccessible.CRITERIAS_HSQL + WORKREQUESTCRITERIAS;
	/**
	 * This is the equivalent of CRITERIAS_NOBIN for HyperSQL This is only
	 * because HSQLDB does not accept '&amp;' as bitwise operator :(
	 */
	private static final String CRITERIAS_HSQL_NOBIN = SQLRequestAccessible.CRITERIAS_HSQL + WORKREQUESTCRITERIAS_NOBIN;

	/**
	 * This is used when host.project is set.
	 */
	protected static final String PROJECT_ACCESS = "   AND maintable.owneruid IN" + // any
																					// job
																					// submitted
																					// in
																					// the
																					// worker
																					// usergroup,
																					// and
																					// this
																					// group
																					// only
			"     (" + "      SELECT users.uid " + "      FROM users,usergroups"
			+ "      WHERE     users.usergroupuid=usergroups.uid " + "            AND usergroups.label='%s' "
			+ "            AND usergroups.isdeleted='false'" + "            AND users.isdeleted='false'" + "     )"
			+ "   AND users.usergroupuid =" + // the group of the worker only
			"     (" + "      SELECT usergroups.uid " + "      FROM usergroups"
			+ "      WHERE usergroups.label='%s' and usergroups.isdeleted='false'" + "     )" + "   AND ("
			+ "      users.uid=maintable.owneruid" + // are this job AND this
														// worker owned by the
														// same user ?
			"      OR (" + "              users.userrightid>'" + UserRightEnum.WORKER_USER.ordinal() + "'" + // can
																								// this
																								// worker
																								// run
																								// jobs
																								// for
																								// others?
			"          AND maintable.accessrights > " + XWAccessRights.USERALL_INT + "	  )" + " )";
	/**
	 * This is used when host.project is set.
	 */
	private static final String WORKREQUESTPROJECTCRITERIAS = WORKREQUESTCRITERIAS + PROJECT_ACCESS;
	/**
	 * This contains criteria for the WHERE part of the SQL request. This
	 * retrieves public, group or private jobs for a group only. This is
	 * typically used when host.project is set.
	 */
	private static final String WORKREQUESTPROJECTCRITERIAS_NOBIN = WORKREQUESTCRITERIAS_NOBIN + PROJECT_ACCESS;

	/**
	 * This concatenates SQLRequestAccessible.CRITERIAS and
	 * WORKREQUESTCRITERIAS_NOBIN
	 */
	private static final String PROJECTCRITERIAS = SQLRequestAccessible.SQLCRITERIAS + WORKREQUESTPROJECTCRITERIAS;
	/**
	 * This concatenates SQLRequestAccessible.CRITERIAS and
	 * WORKREQUESTCRITERIAS_NOBIN
	 */
	private static final String PROJECTCRITERIAS_NOBIN = SQLRequestAccessible.SQLCRITERIAS
			+ WORKREQUESTPROJECTCRITERIAS_NOBIN;
	/**
	 * This is the equivalent of CRITERIAS for HyperSQL This is only because
	 * HSQLDB does not accept '&amp;' as bitwise operator :(
	 *
	 * @see #CRITERIAS
	 */
	private static final String PROJECTCRITERIAS_HSQL = SQLRequestAccessible.CRITERIAS_HSQL
			+ WORKREQUESTPROJECTCRITERIAS;
	/**
	 * This is the equivalent of CRITERIAS_NOBIN for HyperSQL This is only
	 * because HSQLDB does not accept '&amp;' as bitwise operator :(
	 */
	private static final String PROJECTCRITERIAS_HSQL_NOBIN = SQLRequestAccessible.CRITERIAS_HSQL
			+ WORKREQUESTPROJECTCRITERIAS_NOBIN;

	/**
	 * This is the requesting host
	 */
	protected HostInterface host;
	/**
	 * This is the requested status
	 */
	protected StatusEnum status;
	/**
	 * This is the other access rights
	 */
	protected final int otherAccess;
	/**
	 * This is the group access rights
	 */
	protected final int groupAccess;

	private String sqlIsNull00(final String what) {
		if (!getHsqldb()) {
			return "ISNULL(" + what + ")";
		}
		return what + " IS NULL";
	}

	/*
	 * public String sqlIsNull(String what) { if (!hsqldb()) { return "ISNULL("
	 * + what + ")"; } return what + " IS NULL"; }
	 */
	/**
	 * This sets expected status to PENDING and column selection to
	 * ColumnSelection.selectAll
	 *
	 * @see ColumnSelection
	 */
	public SQLRequestWorkRequest() throws IOException {
		super();
		setTableNames(TABLENAMES);
		setTableName(WorkInterface.TABLENAME);

		if (getHsqldb()) {
			setCriterias(CRITERIAS_HSQL);
		} else {
			setCriterias(CRITERIAS);
		}

		host = null;
		status = StatusEnum.PENDING;
		setUser(null);
		setColumnSelection(ColumnSelection.selectAll);
		otherAccess = XWAccessRights.OTHEREXEC_INT;
		groupAccess = XWAccessRights.GROUPEXEC_INT;
	}

	/**
	 * This calls this(h, u, XWStatus.PENDING)
	 *
	 * @see #SQLRequestWorkRequest(HostInterface, UserInterface, StatusEnum)
	 */
	public SQLRequestWorkRequest(final HostInterface h, final UserInterface u) throws IOException {
		this();
		host = h;
		setUser(u);

		final boolean acceptBin = h.acceptBin();

		if ((host.getProject() != null) && (host.getProject().length() > 0)) {
			if (acceptBin) {
				if (getHsqldb()) {
					setCriterias(PROJECTCRITERIAS_HSQL);
				} else {
					setCriterias(PROJECTCRITERIAS);
				}
			} else {
				if (getHsqldb()) {
					setCriterias(PROJECTCRITERIAS_HSQL_NOBIN);
				} else {
					setCriterias(PROJECTCRITERIAS_NOBIN);
				}
			}
		} else {
			if (acceptBin) {
				if (getHsqldb()) {
					setCriterias(CRITERIAS_HSQL);
				} else {
					setCriterias(CRITERIAS);
				}
			} else {
				if (getHsqldb()) {
					setCriterias(CRITERIAS_HSQL_NOBIN);
				} else {
					setCriterias(CRITERIAS_NOBIN);
				}
			}
		}
	}

	/**
	 * This sets host, user and status to those provided
	 *
	 * @param h
	 *            is the host to retrieve a work for
	 * @param u
	 *            is the host identity
	 * @param s
	 *            is the status of the work to retrieve
	 */
	public SQLRequestWorkRequest(final HostInterface h, final UserInterface u, final StatusEnum s) throws IOException {
		this(h, u);
		status = s;
	}

	/**
	 * This retrieves this criteria using this host member variable. If host
	 * does not have cpuspeed, freetmp or totalmem attributes set,
	 * Long.MAX_VALUE is used instead to ensure the host can compute job by
	 * default
	 *
	 * @see #host
	 * @return a String containing criteria
	 */
	@Override
	public String getFullCriterias() throws IOException {

		if (host == null) {
			throw new IOException("SQLRequestWorkRequest : host is null ?!?!");
		}

		final Logger logger = getLogger();
		logger.finest("criterias = " + getCriterias());

		final String binaryFieldName = AppInterface.getBinaryField(host.getCpu(), host.getOs());

		String hostSharedAppNames = host.getSharedApps();
		if (hostSharedAppNames != null) {
			hostSharedAppNames = hostSharedAppNames.replaceAll("[\\n\\s\'\"]+", "_");
			hostSharedAppNames = "'" + hostSharedAppNames.replaceAll(",", "','") + "'";
		}
		String hostSharedPkgNames = host.getSharedPackages();
		if (hostSharedPkgNames != null) {
			hostSharedPkgNames = hostSharedPkgNames.replaceAll("[\\n\\s\'\"]+", "_");
			hostSharedPkgNames = "'" + hostSharedPkgNames.replaceAll(",", "','") + "'";
		}
		String hostSharedData = host.getSharedDatas();
		if (hostSharedData != null) {
			hostSharedData = hostSharedData.replaceAll("[\\n\\s\'\"]+", "_");
			hostSharedData = "'" + hostSharedData.replaceAll(",", "','") + "'";
		}

		final String projectLabel = (host.getProject() != null ? host.getProject().trim() : null);
		final boolean incomingConnections = host.incomingConnections();
		final UID hostUid = host.getUID();

		logger.finest("hostSharedAppNames  = " + hostSharedAppNames);
		logger.finest("hostSharedPkgNames  = " + hostSharedPkgNames);
		logger.finest("hostSharedDataNames = " + hostSharedData);
		logger.finest("projectLabel        = " + projectLabel);

		String ret = null;
		if ((projectLabel == null) || (projectLabel.length() <= 0)) {
			if (host.acceptBin()) {
				ret = String.format(getCriterias(), getUser().getUID().toString(), otherAccess, otherAccess,
						groupAccess, groupAccess, status.toString(), incomingConnections, hostUid.toString(),
						(host.getCpuSpeed() > 0 ? host.getCpuSpeed() : Long.MAX_VALUE),
						(host.getAvailableMem() > 0 ? host.getAvailableMem() : Long.MAX_VALUE),
						(host.getFreeTmp() > 0 ? host.getFreeTmp() : Long.MAX_VALUE), binaryFieldName,
						hostSharedAppNames, hostSharedPkgNames, hostSharedData);
			} else {
				ret = String.format(getCriterias(), getUser().getUID().toString(), otherAccess, otherAccess,
						groupAccess, groupAccess, status.toString(), incomingConnections, hostUid.toString(),
						(host.getCpuSpeed() > 0 ? host.getCpuSpeed() : Long.MAX_VALUE),
						(host.getAvailableMem() > 0 ? host.getAvailableMem() : Long.MAX_VALUE),
						(host.getFreeTmp() > 0 ? host.getFreeTmp() : Long.MAX_VALUE), hostSharedAppNames,
						hostSharedPkgNames, hostSharedData);
			}
		} else {
			logger.debug("projectLabel = " + projectLabel);
			if (host.acceptBin()) {
				ret = String.format(getCriterias(), getUser().getUID().toString(), otherAccess, otherAccess,
						groupAccess, groupAccess, status.toString(), incomingConnections, hostUid.toString(),
						(host.getCpuSpeed() > 0 ? host.getCpuSpeed() : Long.MAX_VALUE),
						(host.getAvailableMem() > 0 ? host.getAvailableMem() : Long.MAX_VALUE),
						(host.getFreeTmp() > 0 ? host.getFreeTmp() : Long.MAX_VALUE), binaryFieldName,
						hostSharedAppNames, hostSharedPkgNames, hostSharedData, projectLabel, projectLabel);
			} else {
				ret = String.format(getCriterias(), getUser().getUID().toString(), otherAccess, otherAccess,
						groupAccess, groupAccess, status.toString(), incomingConnections,
						(host.getCpuSpeed() > 0 ? host.getCpuSpeed() : Long.MAX_VALUE),
						(host.getAvailableMem() > 0 ? host.getAvailableMem() : Long.MAX_VALUE),
						(host.getFreeTmp() > 0 ? host.getFreeTmp() : Long.MAX_VALUE), hostUid.toString(),
						hostSharedAppNames, hostSharedPkgNames, hostSharedData, projectLabel, projectLabel);
			}
		}
		hostSharedAppNames = null;
		hostSharedPkgNames = null;

		return ret;
	}
}
