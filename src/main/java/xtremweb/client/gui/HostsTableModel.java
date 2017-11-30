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
 * HostsTableModel.java
 * <p>
 * Purpose : This is the table model to display XtremWeb workers informations
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ConnectException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;

import org.apache.log4j.Logger;


import xtremweb.common.HostInterface;
import xtremweb.common.Table;
import xtremweb.common.TableColumns;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLVector;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/HostInterface object
 */

class HostsTableModel extends TableModel {

    private static final Logger logger = Logger.getLogger(HostsTableModel.class);

    /**
     * This is the activate button label, alos used as key in hashtable
     */
    public static final String ACTIVATE_LABEL = "Activate";
    /**
     * This is the activate button
     */
    private JButton activateButton;

    /**
     * This is the default constructor.
     */
    public HostsTableModel(final MainFrame p) {
        this(p, true);
    }

    /**
     * This is a constructor.
     *
     * @param detail
     *            tells whether to add a last column to get details
     */
    public HostsTableModel(final MainFrame p, final boolean detail) {
        super(p, new HostInterface(), detail);
        activateButton = null;
    }

    /**
     * This creates new JButton
     *
     * @return a Vector of JButton
     */
    @Override
    public Hashtable getButtons() {

        final Hashtable ret = super.getButtons();

        if (activateButton == null) {
            activateButton = new JButton(ACTIVATE_LABEL);
            activateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    activate();
                }
            });
            activateButton.setEnabled(getParent().privileged());
        }

        ret.put(ACTIVATE_LABEL, activateButton);

        ret.remove(ADD_LABEL);

        return ret;
    }

    /**
     * This adds a host
     */
    @Override
    public void add() {
        logger.error("host add is not implemented");
    }

    /**
     * This views a host
     */
    @Override
    public void view() {
        super.view("Host viewer", "ACTIVE flag is set on server side: only ACTIVE workers may receive jobs\n"
                + "AVAILABLE flas is set by the worker itself, accordingly to its local policy (mouse/keyboard...)");
    }

    /**
     * This replaces UID by human readable columns
     */
    @Override
    protected Vector getViewableRow(final Vector row) {
        final Vector clone = (Vector) row.clone();
        try {
            final int index = TableColumns.OWNERUID.getOrdinal();
            final UID uid = (UID) clone.elementAt(index);
            final UserInterface user = (UserInterface) getParent().commClient().get(uid, false);
            clone.set(index, user.getLogin());
        } catch (final Exception e) {
            logger.error("Caught exception: ", e);
        }
        return clone;
    }

    /**
     * This de/activates a host
     */
    public void activate() {
        logger.error("host activate is not implemented");
    }

    /**
     * This retreives a Vector of worker UID from server
     *
     * @return an empty vector on error
     * @see xtremweb.communications.CommAPI#getHosts()
     */
    @Override
    public XMLVector getRows() throws ConnectException {
        try {
            getParent().setTitleConnected();
            return getParent().commClient().getHosts();
        } catch (final Exception e) {
            getParent().setTitleNotConnected();
            logger.error("Caught exception: ", e);
            throw new ConnectException(e.toString());
        }
    }

    /**
     * This retreives an host from server
     *
     * @return an HostInterface or null on error
     * @see xtremweb.communications.CommAPI#getWorker(UID)
     */
    @Override
    public Table getRow(final UID uid) throws ConnectException {
        try {
            getParent().setTitleConnected();
            final HostInterface host = (HostInterface) getParent().commClient().get(uid);
            if (host == null) {
                return null;
            }
            //
            // Next forces boolean attributes to their default value, if not set
            // Otherwise HostInterface#values[] are null
            // and JTable generates an exception
            //
            host.isTracing();
            host.isActive();
            host.isAvailable();

            return host;
        } catch (final Exception e) {
            getParent().setTitleNotConnected();
            logger.error("Caught exception: ", e);
            throw new ConnectException(e.toString());
        }
    }

    /**
     * This called when Commit button is clicked; it sends all hosts
     * informations to XW server.
     */
    public void onCommitClicked() {

        final HostInterface.Columns column = HostInterface.Columns.TRACES;
        final Hashtable tracerHosts = new Hashtable();
        final Hashtable activatedHosts = new Hashtable();

        for (int row = 0; row < getRowCount(); row++) {

            final String hostName = (String) getValueAt(row, HostInterface.Columns.NAME.ordinal());
            final Boolean trace = (Boolean) getValueAt(row, HostInterface.Columns.TRACES.ordinal());
            final Boolean active = (Boolean) getValueAt(row, HostInterface.Columns.ACTIVE.ordinal());

            activatedHosts.put(hostName, active);
            tracerHosts.put(hostName, trace);

        }

        try {
            logger.error("traceWorkers() error");
        } catch (final Exception e) {
            logger.error("Caught exception, traceWorkers():", e);
        }

        try {
            logger.error("activateWorkers() error");
        } catch (final Exception e) {
            logger.error("Caught exception, activateWorkers():", e);
        }

    }

}
