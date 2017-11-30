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
 * UsersTableModel.java
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;


import xtremweb.common.UID;
import xtremweb.common.UserGroupInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;
import xtremweb.common.XMLValue;
import xtremweb.common.XMLVector;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/UserInterface object
 */

class UsersTableModel extends TableModel {

	private static final Logger logger = Logger.getLogger(UsersTableModel.class);

	/**
	 * These defines submission parameters
	 */
	private static final String UID = "UID";
	private static final String GROUP = "User group";
	private static final String LOGIN = "Login";
	private static final String P = "Password";
	private static final String P2 = "Confirm password";
	private static final String EMAIL = "E-mail";
	private static final String FNAME = "First name";
	private static final String LNAME = "Last name";
	private static final String TEAM = "Team";
	private static final String COUNTRY = "Country";
	private static final String RIGHTS = "Rights";
	/**
	 * These defines submission parameter labels
	 */
	private static final String[] labels = { UID, GROUP, LOGIN, P, P2, EMAIL, FNAME, LNAME, TEAM, COUNTRY, RIGHTS };

	private static final String HELPSTRING = "<u>" + GROUP + "</u> : select an user group (this is optionnal)<br>"
			+ "<u>" + LOGIN + "</u> : a login must be unic in the platform; reusing an existing login updates user<br>"
			+ "<u>" + P + "</u> : please provide a password <br>" + "<u>" + P2
			+ "</u> : please confirm the password<br>" + "<u>" + EMAIL + "</u> : a valid email address is required<br>"
			+ "<u>" + FNAME + "</u> first name<br>" + "<u>" + LNAME + "</u> last name<br>" + "<u>" + RIGHTS
			+ "</u> select a user rights from drop down menu";
	/**
	 * This is used in the user group drop down menu
	 */
	private static final String SELECT = "Select...";

	/**
	 * This is the default constructor.
	 */
	public UsersTableModel(final MainFrame p) throws IOException {
		this(p, true);
	}

	/**
	 * This is a constructor.
	 *
	 * @param detail
	 *            tells whether to add a last column to get details
	 */
	public UsersTableModel(final MainFrame p, final boolean detail) throws IOException {
		super(p, new UserInterface(), detail);
	}

	/**
	 * This adds an user
	 */
	@Override
	public void add() {
		final ArrayList newRow = new ArrayList();
		final UID uid = new UID();
		newRow.add(uid); // UID

		// keep user groups UID
		final Hashtable groupsUID = new Hashtable();

		String[] groupLabels = { "undefined" };

		try {
			final XMLVector groups = getParent().commClient().getUserGroups();
			final Vector<XMLValue> vgroups = (Vector<XMLValue>) groups.getXmlValues();

			groupLabels = new String[vgroups.size() + 1];
			int i = 0;
			groupLabels[i++] = SELECT;

			for (final Enumeration<XMLValue> e = vgroups.elements(); e.hasMoreElements();) {
				final UID groupUID = (UID) e.nextElement().getValue();
				final UserGroupInterface group = (UserGroupInterface) getParent().commClient().get(groupUID, false);
				groupLabels[i++] = group.getLabel();
				groupsUID.put(group.getLabel(), groupUID);
			}
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}

		newRow.add(groupLabels); // group labels

		newRow.add(""); // login
		newRow.add(new JPasswordField()); // password
		newRow.add(new JPasswordField()); // verify password
		newRow.add(""); // email
		newRow.add(""); // first name
		newRow.add(""); // last name
		newRow.add(""); // team
		newRow.add(""); // country

		final String[] urlLabels = new String[UserRightEnum.SIZE];
		for (final UserRightEnum c : UserRightEnum.values()) {
			urlLabels[c.ordinal()] = c.toString();
		}

		newRow.add(urlLabels); // rights

		final ViewDialog dlg = new ViewDialog(getParent(), "Create new user", labels, newRow, true);

		final JTextField component = (JTextField) dlg.getFields().get(UID);
		component.setEditable(false);

		dlg.setHelpString(HELPSTRING);
		dlg.setVisible(true);

		if (dlg.isCancelled()) {
			return;
		}

		try {
			final UserInterface user = new UserInterface(uid);
			JTextField jtf = (JTextField) dlg.getFields().get(LOGIN);
			String login = jtf.getText();
			if (login.length() > UserInterface.USERLOGINLENGTH) {
				login = login.substring(0, UserInterface.USERLOGINLENGTH - 1);
				JOptionPane.showMessageDialog(getParent(), "Login too long; truncated to " + login, WARNING,
						JOptionPane.WARNING_MESSAGE);
			}
			user.setLogin(login);

			if (user.getLogin().length() < 1) {
				JOptionPane.showMessageDialog(getParent(), "You must specify a login", WARNING,
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			jtf = (JTextField) dlg.getFields().get(P);
			final String password = jtf.getText();
			jtf = (JTextField) dlg.getFields().get(P2);
			final String password2 = jtf.getText();

			if (password2.compareTo(password) != 0) {
				JOptionPane.showMessageDialog(getParent(), "Passwords do not match!", WARNING,
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			user.setPassword(password);

			try {
				final JComboBox jcb = (JComboBox) dlg.getFields().get(GROUP);
				final String groupLabel = (String) jcb.getSelectedItem();

				final UID groupUID = (UID) groupsUID.get(groupLabel);
				if (groupUID != null) {
					user.setGroup(groupUID);
				}
			} catch (final Exception e) {
				// group is optionnal
			}

			jtf = (JTextField) dlg.getFields().get(COUNTRY);
			user.setCountry(jtf.getText());

			jtf = (JTextField) dlg.getFields().get(FNAME);
			user.setFirstName(jtf.getText());
			jtf = (JTextField) dlg.getFields().get(LNAME);
			user.setLastName(jtf.getText());

			jtf = (JTextField) dlg.getFields().get(EMAIL);
			user.setEMail(jtf.getText());

			final JComboBox jcb = (JComboBox) dlg.getFields().get(RIGHTS);
			final String rights = (String) jcb.getSelectedItem();
			user.setRights(UserRightEnum.valueOf(rights));

			getParent().commClient().send(user);
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(getParent(), "Can't send user : " + e, ERROR, JOptionPane.ERROR_MESSAGE);
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This views an user
	 */
	@Override
	public void view() {
		super.view("User viewer");
	}

	/**
	 * This replaces UID by human readable columns
	 */
	@Override
	protected Vector getViewableRow(final Vector row) {
		final Vector clone = (Vector) row.clone();
		try {
			final int index = UserInterface.Columns.USERGROUPUID.ordinal();
			final UID uid = (UID) clone.elementAt(index);
			final UserGroupInterface usergroup = (UserGroupInterface) getParent().commClient().get(uid, false);
			clone.set(index, usergroup.getLabel());
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
		return clone;
	}

	/**
	 * This retreives a Vector of user UID from server
	 *
	 * @see xtremweb.communications.CommAPI#getUsers()
	 */
	@Override
	public XMLVector getRows() throws ConnectException {
		try {
			getParent().setTitleConnected();
			return getParent().commClient().getUsers();
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
