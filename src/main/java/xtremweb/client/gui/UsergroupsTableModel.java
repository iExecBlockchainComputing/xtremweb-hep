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
 * UsergroupsTableModel.java
 *
 * Purpose : This is the table model to display XtremWeb users informations
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;


import xtremweb.common.UID;
import xtremweb.common.UserGroupInterface;
import xtremweb.common.XMLVector;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/UserInterface object
 */

class UsergroupsTableModel extends TableModel {

	private static final Logger logger = Logger.getLogger(UsergroupsTableModel.class);

	/**
	 * These defines submission parameters
	 */
	private static final String UID = "UID";
	private static final String LABEL = "Label";
	/**
	 * These defines submission parameter labels
	 */
	private static final String[] labels = { UID, LABEL };

	/**
	 * This is the default constructor.
	 */
	public UsergroupsTableModel(final MainFrame p) throws IOException {
		this(p, true);
	}

	/**
	 * This is a constructor.
	 *
	 * @param detail
	 *            tells whether to add a last column to get details
	 */
	public UsergroupsTableModel(final MainFrame p, final boolean detail) throws IOException {
		super(p, new UserGroupInterface(), detail);
	}

	/**
	 * This adds an user group
	 */
	@Override
	public void add() {
		final List newRow = new Vector();
		final UID uid = new UID();
		newRow.add(uid); // UID
		newRow.add(new String()); // label

		final ViewDialog dlg = new ViewDialog(getParent(), "Create new user group", labels, newRow, true);

		final JTextField component = (JTextField) dlg.getFields().get(UID);
		component.setEditable(false);

		dlg.setHelpString("<u>Label</u> is free");
		dlg.setVisible(true);

		if (dlg.isCancelled() == true) {
			return;
		}

		try {
			final UserGroupInterface group = new UserGroupInterface(uid);
			final JTextField jtf = (JTextField) dlg.getFields().get(LABEL);
			group.setLabel(jtf.getText());

			if (group.getLabel().length() < 1) {
				JOptionPane.showMessageDialog(getParent(), "You must specify a label", WARNING,
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			getParent().commClient().send(group);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This views an user
	 */
	@Override
	public void view() {
		super.view("User group viewer");
	}

	/**
	 * This retreives a Vector of user group UID from server
	 *
	 * @see xtremweb.communications.CommAPI#getUsers()
	 */
	@Override
	public XMLVector getRows() throws ConnectException {
		try {
			getParent().setTitleConnected();
			return getParent().commClient().getUserGroups();
		} catch (final Exception e) {
			getParent().setTitleNotConnected();
			logger.error("Caught exception: ", e);
			throw new ConnectException(e.toString());
		}
	}

	/**
	 * This creates a new ViewDialog to display row details
	 *
	 * @param title
	 *            is the dialog title
	 * @param selectedRow
	 *            is the row to edit/display
	 * @param editable
	 *            enables/disables edition
	 * @return a new ViewDialog
	 */
	protected ViewDialog getViewDialog(final String title, final int selectedRow, final boolean editable) {
		return new UserDialog(getParent(), title, getInterface().columns(), getDataRows().elementAt(selectedRow),
				editable);
	}
}
