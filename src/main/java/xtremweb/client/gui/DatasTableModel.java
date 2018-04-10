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
 * DatasTableModel.java
 *
 * Purpose : This is the table model to display XtremWeb datas informations
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.ConnectException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import xtremweb.common.CPUEnum;
import xtremweb.common.DataInterface;
import xtremweb.common.DataTypeEnum;
import xtremweb.common.OSEnum;
import xtremweb.common.StatusEnum;
import xtremweb.common.TableColumns;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLVector;
import xtremweb.common.XWTools;
import xtremweb.communications.URI;
import xtremweb.security.XWAccessRights;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/DataInterface object
 */

class DatasTableModel extends TableModel {

	/**
	 * These defines submission parameters
	 */
	private static final String UIDLABEL = "UID";
	private static final String OWNERLABEL = "Owner";
	private static final String SIZELABEL = "Length";
	private static final String SHALABEL = "SHASUM";
	private static final String NAMELABEL = "Name";
	private static final String ACCESSRIGHTSLABEL = "Access rights";
	private static final String TYPELABEL = "Type";
	private static final String OSLABEL = "OS";
	private static final String CPULABEL = "CPU";
	private static final String CONTENTLABEL = "File content";
	/**
	 * These defines submission parameter labels
	 */
	private static final String[] labels = { UIDLABEL, OWNERLABEL, NAMELABEL, ACCESSRIGHTSLABEL, TYPELABEL, OSLABEL,
			CPULABEL, CONTENTLABEL, SIZELABEL, SHALABEL};

	private static final String HELPSTRING = new String("<u>" + NAMELABEL
			+ "</u> : is required (automatically sets if selecting a file)<br>" + "<u>" + ACCESSRIGHTSLABEL
			+ "</u> : Linux FS like access rights (default is 0x755)<br>" + "<u>" + TYPELABEL
			+ "</u> : is optional but highly recommanded; select a data type from drop down menu<br>" + "<u>" + OSLABEL
			+ "</u> : is optional but highly recommanded; select an OS from drop down menu<br>" + "<u>" + CPULABEL
			+ "</u> : is optional but highly recommandeed; select a CPU from drop down menu<br>" + "<u>" + CONTENTLABEL
			+ "</u> : is required; select a file or enter a valid URI");
	/**
	 * This is the activate button label, alos used as key in hashtable
	 */
	public static final String DOWNLOAD_LABEL = "Download";

	/**
	 * This stores new UID when adding data
	 */
	private UID newUID;
	/**
	 * This is the data file path
	 */
	private JTextField contentURI;
	/**
	 * This is the data name
	 */
	private JTextField dataNameField;
	/**
	 * This is the data content file
	 */
	private File contentFile;
	/**
	 * This is the current directory. This is used to (re)open file choosers at
	 * the last directory
	 */
	private File currentDir;

	/**
	 * This is the default constructor.
	 */
	public DatasTableModel(final MainFrame p) {
		this(p, true);
	}

	/**
	 * This is a constructor.
	 *
	 * @param detail
	 *            tells whether to add a last column to get details
	 */
	public DatasTableModel(final MainFrame p, final boolean detail) {
		super(p, new DataInterface(), detail);
	}

	/**
	 * This creates new JButton
	 *
	 * @return a Vector of JButton
	 */
	@Override
	public Hashtable getButtons() {

		final Hashtable ret = super.getButtons();

		((JButton) (ret.get(ADD_LABEL))).setEnabled(true);
		((JButton) (ret.get(DEL_LABEL))).setEnabled(true);

		final JButton downloadButton = new JButton(DOWNLOAD_LABEL);
		downloadButton.setMnemonic(KeyEvent.VK_D);
		downloadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				download();
			}
		});

		downloadButton.setEnabled(true);
		ret.put(DOWNLOAD_LABEL, downloadButton);

		return ret;
	}

	/**
	 * This adds an data
	 */
	@Override
	public void add() {

		final Vector newRow = new Vector();
		newUID = new UID();
		newRow.add(newUID);
		final String owner = new String();
		newRow.add(owner);
		dataNameField = new JTextField();
		final String name = new String();
		newRow.add(name);
		newRow.add(XWAccessRights.DEFAULT);
		newRow.add(DataTypeEnum.getLabels());
		newRow.add(OSEnum.getLabels());
		newRow.add(CPUEnum.getLabels());

		final ViewDialog vdialog = new ViewDialog(getParent(), "Add data", labels, newRow, true);
		setViewDialog(vdialog);

		final JButton binButton = new JButton("Select");
		binButton.setMaximumSize(BUTTONDIMENSION);
		binButton.setPreferredSize(BUTTONDIMENSION);
		binButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				selectFile();
			}
		});
		final JButton resetButton = new JButton("Reset");
		resetButton.setMaximumSize(BUTTONDIMENSION);
		resetButton.setPreferredSize(BUTTONDIMENSION);
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				((JTextField) vdialog.getFields().get(SHALABEL)).setText("");
				((JTextField) vdialog.getFields().get(SIZELABEL)).setText("");
				contentURI.setText("");
			}
		});

		contentURI = new JTextField();

		final JPanel container = new JPanel(new GridBagLayout());
		final GridBagLayout gbLayout = (GridBagLayout) container.getLayout();
		final GridBagConstraints gbConstraints = new GridBagConstraints();
		gbConstraints.anchor = GridBagConstraints.CENTER;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.gridx = GridBagConstraints.RELATIVE;
		gbConstraints.gridy = GridBagConstraints.RELATIVE;
		gbConstraints.weightx = 1.0;
		gbConstraints.weighty = 0.0;
		container.add(contentURI);
		container.add(binButton);
		gbLayout.setConstraints(contentURI, gbConstraints);
		gbLayout.setConstraints(container, gbConstraints);
		gbConstraints.weightx = 0.0;
		gbLayout.setConstraints(binButton, gbConstraints);
		container.add(resetButton);
		gbLayout.setConstraints(resetButton, gbConstraints);
		newRow.add(container);

		JTextField field = new JTextField(); // SIZE
		field.setEditable(false);
		newRow.add(field);

		field = new JTextField(); // SHASUM
		field.setEditable(false);
		newRow.add(field);

		JTextField component = (JTextField) vdialog.getFields().get(UIDLABEL);
		component.setEditable(false);
		component = (JTextField) vdialog.getFields().get(OWNERLABEL);
		component.setEditable(false);
		component.setText(getParent().user().getLogin());

		vdialog.setHelpString(HELPSTRING);
		vdialog.setVisible(true);

		if (vdialog.isCancelled()) {
			contentFile = null;
			return;
		}

		final String dataName = dataNameField.getText();
		if ((dataName == null) || (dataName.length() == 0)) {
			JOptionPane.showMessageDialog(getParent(), "You must specify either a data name or a file", WARNING,
					JOptionPane.WARNING_MESSAGE);
			contentFile = null;
			return;
		}
		final String accessRights = ((JTextField) vdialog.getFields().get(ACCESSRIGHTSLABEL)).getText();
		final String shaValue = ((JTextField) vdialog.getFields().get(SHALABEL)).getText();
		final String sizeValue = ((JTextField) vdialog.getFields().get(SIZELABEL)).getText();
		final String type = (String) ((JComboBox) vdialog.getFields().get(TYPELABEL)).getSelectedItem();
		final String os = (String) ((JComboBox) vdialog.getFields().get(OSLABEL)).getSelectedItem();
		final String cpu = (String) ((JComboBox) vdialog.getFields().get(CPULABEL)).getSelectedItem();
		final JPanel filePanel = (JPanel) vdialog.getFields().get(CONTENTLABEL);
		final JTextField jtf = (JTextField) filePanel.getComponent(0);
		final String fileName = jtf.getText();

		try {
			final DataInterface data = new DataInterface(newUID);
			data.setName(dataName);
			data.setShasum(shaValue);
			if ((sizeValue != null) && (sizeValue.length() > 0)) {
				data.setSize(new Long(sizeValue).longValue());
			}
			data.setAccessRights(new XWAccessRights(accessRights));
			data.setType(DataTypeEnum.valueOf(type.toUpperCase()));
			data.setCpu(CPUEnum.valueOf(cpu.toUpperCase()));
			data.setOs(OSEnum.valueOf(os.toUpperCase()));
			data.setURI(getParent().commClient().newURI(data.getUID()));

			try {
				if (contentFile == null) {
					data.setStatus(StatusEnum.AVAILABLE);
				} else {
					if (contentFile.exists()) {
						data.setStatus(StatusEnum.UNAVAILABLE);
					} else {
						JOptionPane.showMessageDialog(getParent(), "File not found", WARNING,
								JOptionPane.WARNING_MESSAGE);
						contentFile = null;
						return;
					}
				}

				getParent().commClient().send(data);
				if (contentFile != null) {
					getParent().commClient().uploadData(data.getUID(), contentFile);
				}
			} catch (final Exception e) {
				getLogger().exception(e);
				JOptionPane.showMessageDialog(getParent(), "Can't send data : " + e, ERROR, JOptionPane.ERROR_MESSAGE);
			}
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		contentFile = null;
	}

	/**
	 * This opens a file chooser dialog
	 */
	public void selectFile() {
		final JFileChooser fc = new JFileChooser();
		final ViewDialog vdialog = getViewDialog();

		if (currentDir != null) {
			fc.setCurrentDirectory(currentDir);
		}

		fc.showOpenDialog(getParent());

		if (fc.getCurrentDirectory() != null) {
			currentDir = fc.getCurrentDirectory();
		}

		contentFile = fc.getSelectedFile();

		if (contentFile != null) {
			contentURI.setText(contentFile.getName());
			if ((dataNameField.getText() == null) || (dataNameField.getText().length() == 0)) {
				dataNameField.setText(contentFile.getName());
			}
			try {
				((JTextField) vdialog.getFields().get(SHALABEL)).setText(XWTools.sha256CheckSum(contentFile));
				((JTextField) vdialog.getFields().get(SIZELABEL)).setText("" + contentFile.length());
			} catch (final Exception e) {
			}
		} else {
			((JTextField) vdialog.getFields().get(SHALABEL)).setText("");
			((JTextField) vdialog.getFields().get(SIZELABEL)).setText("");
			contentURI.setText("");
		}
	}

	/**
	 * This views an data
	 */
	@Override
	public void view() {
		super.view("Data viewer");
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
			getLogger().exception(e);
		}
		return clone;
	}

	/**
	 * This retreives a Vector of data UID from server
	 *
	 * @return an empty vector on error
	 * @see xtremweb.communications.CommAPI#getDatas()
	 */
	@Override
	public XMLVector getRows() throws ConnectException {
		try {
			getParent().setTitleConnected();
			return getParent().commClient().getDatas();
		} catch (final Exception e) {
			getParent().setTitleNotConnected();
			getLogger().exception(e);
			throw new ConnectException(e.toString());
		}
	}

	/**
	 * This downloads data content
	 *
	 * @see xtremweb.client.Client#result()
	 */
	public void download() {

		final int[] selectedRows = getSelection();
		if (selectedRows.length == 0) {
			JOptionPane.showMessageDialog(getParent(), "No row selected!", WARNING, JOptionPane.WARNING_MESSAGE);
			return;
		} else if (selectedRows.length > 1) {
			JOptionPane.showMessageDialog(getParent(), "You can not download more than one data at a time", WARNING,
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (currentDir != null) {
			fc.setCurrentDirectory(currentDir);
		}

		final int confirm = fc.showOpenDialog(getParent());

		currentDir = fc.getSelectedFile();

		if ((confirm == JFileChooser.CANCEL_OPTION) && (currentDir == null)) {
			return;
		}

		getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		final URI uri = null;
		UID dataUid = null;
		DataInterface data = null;
		try {
			data = (DataInterface) getSelectedRow(selectedRows[0]);
			dataUid = data.getUID();
		} catch (final Exception e) {
			getLogger().exception(e);
			JOptionPane.showMessageDialog(getParent(), e.toString(), WARNING, JOptionPane.WARNING_MESSAGE);
			getParent().setCursor(null);
			return;
		}
		try {
			String fext = "";
			if (data.getType() != null) {
				fext = data.getType().getFileExtension();
			}

			final String dataName = data.getName();
			File fdata = null;
			if (dataName != null) {
				fdata = new File(currentDir, dataUid.toString() + "_" + dataName + fext);
			} else {
				fdata = new File(currentDir, dataUid.toString() + fext);
			}
			getParent().commClient().downloadData(dataUid, fdata);
		} catch (final Exception e) {
			getLogger().exception(e);
			getParent().setTitleNotConnected();
		}
		getParent().setCursor(null);
	}

}
