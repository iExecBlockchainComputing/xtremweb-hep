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

import xtremweb.common.*;
import xtremweb.security.XWAccessRights;

import java.io.IOException;

/**
 * This is a decorator pattern aiming to implement SQL requests used to retrieve
 * objects from DB. This implements the SQL request to retrieve works for a
 * given host (OS and architecture) and its identity that fulfills security
 * access according to the given host identity. This has two possible SQL
 * criteria, depending on host configuration. <br />
 *
 * @author <A HREF="mailto:lodygens /at\ lal.in2p3.fr">Oleg Lodygensky </A>
 * @since 13.0.0
 */
public class SQLRequestWorkRequestDataDriven extends SQLRequestWorkRequest {

    /**
     * This contains table names for the FROM part of the SQL request. There
     * must not be a space after the comma
     */
    private static final String DATADRIVENCRITERIASTABLENAMES = SQLRequestWorkRequest.TABLENAMES + "," + DataInterface.DATATABLENAME;

    /**
	 * This is used if host.acceptBin == true. This helps to retrieve any
	 * application for the worker, including apps to be deployed (the worker
	 * will have to download binary) or shared apps (the ones the worker
	 * declares as sharing)
	 */
	private static final String DATADRIVENCRITERIAS = " AND ((maintable.DATADRIVENURI IS NULL) OR (maintable.DATADRIVENURI='') OR ((maintable.DATADRIVENURI=datas.URI) AND (datas.PACKAGE IS NOT NULL) AND (datas.PACKAGE IN (%s))))";


	/**
	 * This concatenates SQLRequestAccessible.CRITERIAS and
	 * WORKREQUESTCRITERIAS_NOBIN
	 */
	private static final String WORKREQUESTDATADRIVENCRITERIAS = SQLRequestAccessible.SQLCRITERIAS + WORKREQUESTCRITERIAS + DATADRIVENCRITERIAS;
	/**
	 * This concatenates SQLRequestAccessible.CRITERIAS and
	 * WORKREQUESTCRITERIAS_NOBIN
	 */
	private static final String WORKREQUESTDATADRIVENCRITERIAS_NOBIN = SQLRequestAccessible.SQLCRITERIAS + WORKREQUESTCRITERIAS_NOBIN + DATADRIVENCRITERIAS;

	/**
	 * This is the equivalent of CRITERIAS for HyperSQL This is only because
	 * HSQLDB does not accept '&amp;' as bitwise operator :(
	 *
	 * @see #CRITERIAS
	 */
	private static final String WORKREQUESTDATADRIVENCRITERIAS_HSQL = SQLRequestAccessible.CRITERIAS_HSQL + WORKREQUESTDATADRIVENCRITERIAS + DATADRIVENCRITERIAS;
	/**
	 * This is the equivalent of CRITERIAS_NOBIN for HyperSQL This is only
	 * because HSQLDB does not accept '&amp;' as bitwise operator :(
	 */
	private static final String WORKREQUESTDATADRIVENCRITERIAS_HSQL_NOBIN = SQLRequestAccessible.CRITERIAS_HSQL + WORKREQUESTDATADRIVENCRITERIAS_NOBIN + DATADRIVENCRITERIAS;

	/**
	 * This is used when host.project is set.
	 */
	private static final String WORKREQUESTPROJECTCRITERIAS = WORKREQUESTDATADRIVENCRITERIAS + PROJECT_ACCESS;
	/**
	 * This contains criteria for the WHERE part of the SQL request. This
	 * retrieves public, group or private jobs for a group only. This is
	 * typically used when host.project is set.
	 */
	private static final String WORKREQUESTPROJECTCRITERIAS_NOBIN = WORKREQUESTDATADRIVENCRITERIAS_NOBIN + PROJECT_ACCESS;

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
	public SQLRequestWorkRequestDataDriven() throws IOException {
		super();
        setTableNames(DATADRIVENCRITERIASTABLENAMES);

		if (getHsqldb()) {
			setCriterias(WORKREQUESTDATADRIVENCRITERIAS_HSQL);
		} else {
			setCriterias(WORKREQUESTDATADRIVENCRITERIAS);
		}
	}

	/**
	 * This calls this(h, u, XWStatus.PENDING)
	 *
	 */
	public SQLRequestWorkRequestDataDriven(final HostInterface h, final UserInterface u) throws IOException {
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
					setCriterias(WORKREQUESTDATADRIVENCRITERIAS_HSQL);
				} else {
					setCriterias(WORKREQUESTDATADRIVENCRITERIAS);
				}
			} else {
				if (getHsqldb()) {
					setCriterias(WORKREQUESTDATADRIVENCRITERIAS_HSQL_NOBIN);
				} else {
					setCriterias(WORKREQUESTDATADRIVENCRITERIAS_NOBIN);
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
	public SQLRequestWorkRequestDataDriven(final HostInterface h, final UserInterface u, final StatusEnum s) throws IOException {
		this(h, u);
		status = s;
	}

}
