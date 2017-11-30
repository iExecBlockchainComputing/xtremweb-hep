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
 * GroupsTableModel.java
 *
 * Purpose : This is the table model to display XtremWeb groups informations
 * Created : 2 octobre 2008
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.net.ConnectException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;


import xtremweb.common.GroupInterface;
import xtremweb.common.UID;
import xtremweb.common.XMLVector;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/GroupInterface object
 */

class GroupsTableModel extends TableModel {

	private static final Logger logger = Logger.getLogger(GroupsTableModel.class);

	/**
	 * These defines submission parameters
	 */
	private static final String UID = "UID";
	private static final String USERLABEL = "Owner";
	private static final String GROUPLABEL = "Name";
	/**
	 * This is the help
	 */
	private static final String HELPSTRING = new String("A group aggregates jobs to facilitate job management.<br>"
			+ "A group is valid until you delete it.<br>" + "You can close the client and retreive you group later.<br>"
			+ "When a group is deleted, all associated jobs are deleted.<br><u>" + GROUPLABEL
			+ "</u> is the name of the group");
	/**
	 * These defines submission parameter labels
	 */
	private static final String[] labels = { UID, USERLABEL, GROUPLABEL };

	/**
	 * This is the default constructor.
	 */
	public GroupsTableModel(final MainFrame p) {
		this(p, true);
	}

	/**
	 * This is a constructor.
	 *
	 * @param detail
	 *            tells whether to add a last column to get details
	 */
	public GroupsTableModel(final MainFrame p, final boolean detail) {
		super(p, new GroupInterface(), detail);
	}

	/**
	 * This calls TableModel#getButtons() and enables all buttons
	 *
	 * @return a Vector of JButton
	 * @see xtremweb.client.gui.TableModel#getButtons()
	 */
	@Override
	public Hashtable getButtons() {

		final Hashtable ret = super.getButtons();

		((JButton) (ret.get(ADD_LABEL))).setEnabled(true);
		((JButton) (ret.get(DEL_LABEL))).setEnabled(true);

		return ret;
	}

	/**
	 * This adds a group
	 */
	@Override
	public void add() {

		final UID groupUID = new UID();
		UID userUID = null;
		try {
			userUID = getParent().getClient().getConfig().getUser().getUID();
		} catch (final Exception e) {
			logger.error("user UID is not set ?!?!");
			return;
		}
		final Vector newRow = new Vector();

		newRow.add(groupUID);
		newRow.add(new String()); // user login
		newRow.add(new String()); // group name

		final ViewDialog vdialog = new ViewDialog(getParent(), "Create group", labels, newRow, true);
		setViewDialog(vdialog);
		vdialog.setHelpString(HELPSTRING);

		final JTextField component = (JTextField) vdialog.getFields().get(USERLABEL);
		component.setEditable(false);
		component.setText(getParent().user().getLogin());

		vdialog.setVisible(true);

		if (vdialog.isCancelled() == true) {
			return;
		}

		try {
			final GroupInterface group = new GroupInterface();
			group.setUID(groupUID);
			group.setOwner(userUID);

			group.setName(((JTextField) vdialog.getFields().get(GROUPLABEL)).getText());

			getParent().commClient().send(group);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			JOptionPane.showMessageDialog(getParent(), "Can't send group : " + e, ERROR, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * This views a work
	 */
	@Override
	public void view() {
		super.view("Group viewer");
	}

	/**
	 * This retreives a Vector of work UID from server
	 *
	 * @see xtremweb.communications.CommAPI#getWorks()
	 */
	@Override
	public XMLVector getRows() throws ConnectException {
		try {
			getParent().setTitleConnected();
			return getParent().commClient().getGroups();
		} catch (final Exception e) {
			getParent().setTitleNotConnected();
			logger.error("Caught exception: ", e);
			throw new ConnectException(e.toString());
		}
	}

}
