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
 * This defines a modal Dialog to enter server, login and password
 */

package xtremweb.client.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;


import xtremweb.common.Browser;
import xtremweb.security.XWAccessRights;

public class ViewDialog extends JDialog implements ActionListener {

	private static final Logger logger = Logger.getLogger(ViewDialog.class);

	public static final String OK = "Ok";
	public static final String EDIT = "Edit";
	public static final String CANCEL = "Cancel";
	public static final String HELP = "Help";
	private final JFrame _parent;


	private JPanel textPane = null;

	/**
	 * This contains the editable fields
	 */
	private final String[] columns;
	/**
	 * This contains the editable fields
	 */
	private Hashtable fields;
	/**
	 * This is true if user cancelled
	 */
	private boolean cancelled;
	/**
	 * This is true if user is editing
	 */
	private boolean editing;
	/**
	 * This is true if user has edited
	 */
	private boolean edited;
	/**
	 * This contains the help string, if any
	 */
	private String helpString;

	/**
	 * This constructor does everything
	 *
	 * @param f
	 *            is the aprent JFrame
	 * @param title
	 *            is the dialog title
	 * @param thecolumns
	 *            is a String array containing columns name, if c is null, row
	 *            contains a single TableModel
	 * @param row
	 *            is a Vector containing the selected row or a single JComponent
	 *            if c is null
	 * @param editable
	 *            enables/disables edition
	 */
	public ViewDialog(final JFrame f, final String title, final String[] thecolumns, final List row,
			final boolean editable) {

		super(f, title, true);

		final String[] cc = thecolumns.clone();

		setHelpString("No help available");
		setCancelled(true);
		editing = false;
		setEdited(false);

		_parent = f;
		setFields(new Hashtable());
		columns = cc;

		if (columns != null) {
			logger.debug("columns.length = " + columns.length);
			int gloRows = 2;
			final GridLayout glo = new GridLayout(gloRows, 2);
			textPane = new JPanel(glo);

			for (int i = 0; i < columns.length; i++) {

				final Object value = row.get(i);

				logger.debug("" + i + " = " + value);

				JComponent field = null;

				if (value != null) {
					if (value.getClass() == java.lang.Boolean.class) {
						field = (new JCheckBox());
						((JCheckBox) field).setSelected(((Boolean) value).booleanValue());
					} else if (value.getClass().isArray()) {
						final String[] v = (String[]) value;
						final JComboBox jcb = new JComboBox(v);
						field = jcb;
					}

					else if (value instanceof javax.swing.JComponent) {
						field = (JComponent) value;
					} else {
						if (value instanceof XWAccessRights) {
							field = (new JTextField(((XWAccessRights) value).toHexString()));
						} else {
							field = (new JTextField(value.toString()));
						}
					}
				} else {
					field = (new JTextField(""));
				}

				logger.debug("columns[" + i + "] = " + columns[i]);

				if (columns[i] != null) {
					if (columns[i].compareToIgnoreCase("UID") == 0) {
						((JTextField) field).setEditable(false);
					} else {
						if (editable == false) {
							if (field instanceof javax.swing.JTextField) {
								((JTextField) field).setEditable(editable);
							} else {
								field.setEnabled(editable);
							}
						}
					}
					final JLabel label = new JLabel(columns[i]);
					label.setLabelFor(field);
					logger.debug("textPane add " + columns[i] + " " + field);
					glo.setRows(++gloRows);
					textPane.add(label);
					textPane.add(field);
					getFields().put(columns[i], field);
				}
			}
		} else {
			textPane = new JPanel(new GridBagLayout());
			final GridBagLayout gbLayout = (GridBagLayout) textPane.getLayout();
			final GridBagConstraints gbConstraints = new GridBagConstraints();
			gbConstraints.anchor = GridBagConstraints.CENTER;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.gridx = GridBagConstraints.RELATIVE;
			gbConstraints.gridy = GridBagConstraints.RELATIVE;
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
			final JComponent component = (JComponent) row.get(0);
			gbLayout.setConstraints(component, gbConstraints);
			textPane.add(component);
		}

		JButton button = new JButton(OK);
		button.setActionCommand(OK);
		button.setMnemonic(KeyEvent.VK_O);
		button.addActionListener(this);

		textPane.add(button);

		button = new JButton(CANCEL);
		button.setMnemonic(KeyEvent.VK_C);
		button.setActionCommand(CANCEL);
		button.addActionListener(this);

		textPane.add(button);

		button = new JButton(HELP);
		button.setMnemonic(KeyEvent.VK_H);
		button.setActionCommand(HELP);
		button.addActionListener(this);

		textPane.add(button);

		getContentPane().add(textPane);
		pack();
	}

	/**
	 * @return the textPane
	 */
	public JPanel getTextPane() {
		return textPane;
	}
	/**
	 * @return the textPane
	 */
	@Override
	public JFrame getParent() {
		return _parent;
	}

	/**
	 * This toggles edit on/off
	 */
	private void toggleEdit() {

		setEdited(true);
		editing = !editing;

		for (int i = 0; i < columns.length; i++) {

			final JComponent field = (JComponent) getFields().get(columns[i]);
			if (columns[i].compareToIgnoreCase("UID") == 0) {
				field.setEnabled(false);
			} else {
				field.setEnabled(editing);
			}
		}
	}

	/**
	 * This is called when user clicks on any button
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String cmd = e.getActionCommand();

		if (OK.equals(cmd)) {
			setCancelled(false);
			setVisible(false);
		} else if (EDIT.equals(cmd)) {
			toggleEdit();
		} else if (CANCEL.equals(cmd)) {
			setCancelled(true);
			setVisible(false);
		} else if (HELP.equals(cmd)) {
			try {
				final Browser helpViewer = new Browser(getHelpString());
				helpViewer.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						helpViewer.setVisible(false);
					}
				});
				JOptionPane.showMessageDialog(_parent, helpViewer.getPane(), "XWHEP help",
						JOptionPane.INFORMATION_MESSAGE);

			} catch (final Exception ex) {
				logger.error("Caught exception: ", ex);
			}
		}
	}

	/**
	 * @return the fields
	 */
	public Hashtable getFields() {
		return fields;
	}

	/**
	 * @param fields
	 *            the fields to set
	 */
	public void setFields(final Hashtable fields) {
		this.fields = fields;
	}

	/**
	 * @return the helpString
	 */
	public String getHelpString() {
		return helpString;
	}

	/**
	 * @param helpString
	 *            the helpString to set
	 */
	public void setHelpString(final String helpString) {
		this.helpString = helpString;
	}

	/**
	 * @return the cancelled
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * @param cancelled
	 *            the cancelled to set
	 */
	public void setCancelled(final boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * @return the edited
	 */
	public boolean isEdited() {
		return edited;
	}

	/**
	 * @param edited
	 *            the edited to set
	 */
	public void setEdited(final boolean edited) {
		this.edited = edited;
	}

}
