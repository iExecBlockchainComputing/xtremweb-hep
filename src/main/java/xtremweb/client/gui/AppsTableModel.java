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
 * AppsTableModel.java
 *
 * Purpose : This is the table model to display XtremWeb applications informations
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.io.File;
import java.net.ConnectException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;


import xtremweb.common.AppInterface;
import xtremweb.common.CPUEnum;
import xtremweb.common.DataInterface;
import xtremweb.common.DataTypeEnum;
import xtremweb.common.OSEnum;
import xtremweb.common.Table;
import xtremweb.common.UID;
import xtremweb.common.XMLVector;
import xtremweb.communications.URI;
import xtremweb.security.XWAccessRights;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/AppInterface object
 */

class AppsTableModel extends TableModel {

	private static final Logger logger = Logger.getLogger(AppsTableModel.class);

	/**
	 * These defines submission parameters
	 */
	private static final String UIDLABEL = "UID";
	private static final String OWNERLABEL = "Owner";
	private static final String NAMELABEL = "Name";
	private static final String ACCESSRIGHTSLABEL = "Access rights";
	private static final String JAVAJARLABEL = "Java JAR";
	private static final String LINUXIX86LABEL = "Linux ix86";
	private static final String LINUXAMD64LABEL = "Linux AMD 64";
	private static final String LINUXIA64LABEL = "Linux Intel Itanium";
	private static final String LINUXPPCLABEL = "Linux PPC";
	private static final String MACOSX86_64LABEL = "Mac OS X x86_64";
	private static final String MACOSIX86LABEL = "Mac OS X ix86";
	private static final String MACOSPPCLABEL = "Mac OS X PPC";
	private static final String WIN32IX86LABEL = "Win32 ix86";
	private static final String WIN32AMD64LABEL = "Win32 AMD 64";
	private static final String STDINLABEL = "Standard input";
	private static final String DIRINLABEL = "Environment";
	private static final String MEMORYLABEL = "Min RAM";
	private static final String CPUSPEEDLABEL = "Min CPU Speed";
	/**
	 * These defines submission parameter labels
	 */
	private static final String[] labels = { UIDLABEL, OWNERLABEL, NAMELABEL, ACCESSRIGHTSLABEL, JAVAJARLABEL,
			LINUXIX86LABEL, LINUXAMD64LABEL, LINUXIA64LABEL, LINUXPPCLABEL, MACOSX86_64LABEL, MACOSIX86LABEL,
			MACOSPPCLABEL, WIN32IX86LABEL, WIN32AMD64LABEL, STDINLABEL, DIRINLABEL, MEMORYLABEL, CPUSPEEDLABEL };

	private static final String HELPSTRING = "<u>" + NAMELABEL
			+ "</u> : should be unic in the platform; reusing an existing name update application<br>" + "<u>"
			+ ACCESSRIGHTSLABEL + "</u> : Linux FS like access rights"
			+ "<ul><li>default is 0x755 for administrator,<li>0x700 otherwise<li>if you're not administrator you can't modify this</ul>"
			+ "<br> In the following, you can enter a file URI to select a local file (e.g. &quot;file:///bin/ls&quot;)<br><br>"
			+ "<u>" + JAVAJARLABEL + "</u> : Select a JAR file or enter a valid URI<br>" + "<u>" + LINUXIX86LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + LINUXAMD64LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + LINUXIA64LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + LINUXPPCLABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + MACOSX86_64LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + MACOSIX86LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + MACOSPPCLABEL
			+ "</u> : Select a binay or enter a valid URI<br>" + "<u>" + WIN32IX86LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + WIN32AMD64LABEL
			+ "</u> : Select a binary or enter a valid URI<br>" + "<u>" + STDINLABEL
			+ "</u> : Select a text file or enter a valid URI<br>" + "<u>" + DIRINLABEL
			+ "</u> : Select a file (typically a ZIP file containing a directory tree) or enter a valid URI<br>" + "<u>"
			+ MEMORYLABEL + "</u> : Enter the minimal memory size requirements<br>" + "<u>" + CPUSPEEDLABEL
			+ "</u> : Enter the minimal CPU speed requirements";

	/**
	 * This is the default constructor.
	 */
	public AppsTableModel(final MainFrame p) {
		this(p, true);
	}

	/**
	 * This is a constructor.
	 *
	 * @param detail
	 *            tells whether to add a last column to get details
	 */
	public AppsTableModel(final MainFrame p, final boolean detail) {
		super(p, new AppInterface(), detail);
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

		return ret;
	}

	/**
	 * This adds an application
	 */
	@Override
	public void add() {
		XWAccessRights accessRights = XWAccessRights.DEFAULT;
		final Vector newRow = new Vector();
		final UID newUID = new UID();
		newRow.add(newUID); // UID
		newRow.add(new String()); // owner
		newRow.add(new String()); // name
		newRow.add(accessRights); // access rights
		newRow.add(newContainer(Commands.JAVAJAR));// binary
		newRow.add(newContainer(Commands.LINUXIX86));// binary
		newRow.add(newContainer(Commands.LINUXAMD64));// binary
		newRow.add(newContainer(Commands.LINUXIA64));// binary
		newRow.add(newContainer(Commands.LINUXPPC));// binary
		newRow.add(newContainer(Commands.MACOSX86_64));// binary
		newRow.add(newContainer(Commands.MACOSIX86));// binary
		newRow.add(newContainer(Commands.MACOSPPC));// binary
		newRow.add(newContainer(Commands.WIN32IX86));// binary
		newRow.add(newContainer(Commands.WIN32AMD64));// binary
		newRow.add(newContainer(Commands.STDIN)); // stdin
		newRow.add(newContainer(Commands.DIRIN)); // dirin
		newRow.add(new String()); // Min memory
		newRow.add(new String()); // Min CPUSpeed

		final ViewDialog vdialog = new ViewDialog(getParent(), "Add application", labels, newRow, true);
		setViewDialog(vdialog);

		JTextField component = (JTextField) vdialog.getFields().get(UIDLABEL);
		component.setEditable(false);
		component = (JTextField) vdialog.getFields().get(OWNERLABEL);
		component.setEditable(false);
		component.setText(getParent().user().getLogin());

		if (getParent().privileged() == false) {
			final JTextField jtf = (JTextField) vdialog.getFields().get(ACCESSRIGHTSLABEL);
			accessRights = XWAccessRights.USERALL;
			jtf.setText(accessRights.toString());
			jtf.setEditable(false);
		}

		vdialog.setHelpString(HELPSTRING);
		vdialog.setVisible(true);

		if (vdialog.isCancelled()) {
			return;
		}

		String appName = ((JTextField) vdialog.getFields().get(NAMELABEL)).getText();
		if ((appName == null) || (appName.length() == 0)) {
			JOptionPane.showMessageDialog(getParent(), "You must specify an application name", WARNING,
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (appName.length() > AppInterface.APPNAMELENGTH) {
			appName = appName.substring(0, AppInterface.APPNAMELENGTH - 1);
			JOptionPane.showMessageDialog(getParent(), "Application name too long; truncated to " + appName, WARNING,
					JOptionPane.WARNING_MESSAGE);
		}

		try {
			final AppInterface app = new AppInterface(newUID);
			app.setName(appName);

			JPanel innerPanel = (JPanel) vdialog.getFields().get(JAVAJARLABEL);
			JTextField jtf = (JTextField) innerPanel.getComponent(0);
			URI fileUri = null;

			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.JAVA, CPUEnum.NONE, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.NONE, XWOSes.JAVA, fileUri);
			} catch (final Exception e) {
				logger.debug("No java JAR : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(LINUXIX86LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.LINUX, CPUEnum.IX86, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.IX86, XWOSes.LINUX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Linux ix86 binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(LINUXAMD64LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.LINUX, CPUEnum.AMD64, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.AMD64, XWOSes.LINUX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Linux ix86 binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(LINUXIA64LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.LINUX, CPUEnum.IA64, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.IA64, XWOSes.LINUX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Linux ia64 binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(LINUXPPCLABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.LINUX, CPUEnum.PPC, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.PPC, XWOSes.LINUX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Linux ppc binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(WIN32IX86LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.WIN32, CPUEnum.IX86, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.PPC, XWOSes.LINUX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Win32 ix86 binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(WIN32AMD64LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.WIN32, CPUEnum.AMD64, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.PPC, XWOSes.LINUX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Win32 AMD64 binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(MACOSX86_64LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.MACOSX, CPUEnum.X86_64, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.X86_64, XWOSes.MACOSX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Mac OS X x86_64  binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(MACOSIX86LABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.MACOSX, CPUEnum.IX86, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.IX86, XWOSes.MACOSX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Mac OS X ix86  binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(MACOSPPCLABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			try {
				fileUri = new URI(jtf.getText());
				if (fileUri.isFile()) {
					fileUri = getParent().getClient().sendData(OSEnum.MACOSX, CPUEnum.PPC, DataTypeEnum.BINARY,
							XWAccessRights.DEFAULT, fileUri, new File(fileUri.getPath()).getName());
				}
				// app.setBinary(XWCPUs.PPC, XWOSes.MACOSX, fileUri);
			} catch (final Exception e) {
				logger.debug("No Mac OS X PPC binary : " + e.toString());
			}

			innerPanel = (JPanel) vdialog.getFields().get(STDINLABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			String str = jtf.getText();
			if ((str != null) && (str.length() > 0)) {
				app.setDefaultStdin(new URI(str));
			}

			innerPanel = (JPanel) vdialog.getFields().get(DIRINLABEL);
			jtf = (JTextField) innerPanel.getComponent(0);
			str = jtf.getText();
			if ((str != null) && (str.length() > 0)) {
				app.setDefaultDirin(new URI(str));
			}

			final String accessRightsStr = ((JTextField) vdialog.getFields().get(ACCESSRIGHTSLABEL)).getText();
			if ((accessRightsStr != null) && (accessRightsStr.length() > 0)) {
				try {
					accessRights = new XWAccessRights(accessRightsStr);
				} catch (final Exception e) {
				}
			}
			app.setAccessRights(accessRights);

			final String minMem = ((JTextField) vdialog.getFields().get(MEMORYLABEL)).getText();
			if ((minMem != null) && (minMem.length() > 0)) {
				app.setMinMemory(new Integer(minMem).intValue());
			}

			final String minSpeed = ((JTextField) vdialog.getFields().get(CPUSPEEDLABEL)).getText();
			if ((minSpeed != null) && (minSpeed.length() > 0)) {
				app.setMinCpuSpeed(new Integer(minSpeed).intValue());
			}

			getParent().commClient().send(app);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			JOptionPane.showMessageDialog(getParent(), "Can't send application : " + e, ERROR,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * This retreives the panel of the specified command
	 */
	public JPanel getPanel(final Commands id) {

		JPanel innerPanel = null;
		final ViewDialog vdialog = getViewDialog();

		switch (id) {
		case LINUXIX86:
			innerPanel = (JPanel) vdialog.getFields().get(LINUXIX86LABEL);
			break;
		case LINUXAMD64:
			innerPanel = (JPanel) vdialog.getFields().get(LINUXAMD64LABEL);
			break;
		case LINUXIA64:
			innerPanel = (JPanel) vdialog.getFields().get(LINUXIA64LABEL);
			break;
		case LINUXPPC:
			innerPanel = (JPanel) vdialog.getFields().get(LINUXPPCLABEL);
			break;
		case MACOSX86_64:
			innerPanel = (JPanel) vdialog.getFields().get(MACOSX86_64LABEL);
			break;
		case MACOSIX86:
			innerPanel = (JPanel) vdialog.getFields().get(MACOSIX86LABEL);
			break;
		case MACOSPPC:
			innerPanel = (JPanel) vdialog.getFields().get(MACOSPPCLABEL);
			break;
		case WIN32IX86:
			innerPanel = (JPanel) vdialog.getFields().get(WIN32IX86LABEL);
			break;
		case WIN32AMD64:
			innerPanel = (JPanel) vdialog.getFields().get(WIN32AMD64LABEL);
			break;
		case STDIN:
			innerPanel = (JPanel) vdialog.getFields().get(STDINLABEL);
			break;
		case DIRIN:
			innerPanel = (JPanel) vdialog.getFields().get(DIRINLABEL);
			break;
		}

		return innerPanel;
	}

	/**
	 * This opens a dialog box to select a row from data table and sets set the
	 * according field
	 */
	@Override
	public void selectData(final Commands id) {

		final TableModel tm = new DatasTableModel(getParent());
		try {
			getParent().setTitleConnected();
			tm.refresh();
		} catch (final ConnectException e) {
			getParent().setTitleNotConnected();
			logger.error("Caught exception: ", e);
			return;
		}

		final DataInterface data = (DataInterface) selectDialogBox(id.getTitle(), tm);

		if (data == null) {
			return;
		}

		final JPanel innerPanel = getPanel(id);
		if (innerPanel != null) {
			final JTextField jtf = (JTextField) innerPanel.getComponent(0);
			if (jtf != null) {
				jtf.setText(data.getURI().toString());
			}
		}
	}

	/**
	 * This reset the command field
	 */
	@Override
	public void resetData(final Commands id) {

		final JPanel innerPanel = getPanel(id);
		if (innerPanel != null) {
			final JTextField jtf = (JTextField) innerPanel.getComponent(0);
			if (jtf != null) {
				jtf.setText("");
			}
		}
	}

	/**
	 * This views an application
	 */
	@Override
	public void view() {
		super.view("Application viewer");
	}

	/**
	 * This retreives a Vector of application UID from server
	 *
	 * @return an empty vector on error
	 * @see xtremweb.communications.CommAPI#getApps()
	 */
	@Override
	public XMLVector getRows() throws ConnectException {
		try {
			getParent().setTitleConnected();
			return getParent().commClient().getApps();
		} catch (final Exception e) {
			getParent().setTitleNotConnected();
			logger.error("Caught exception: ", e);
			throw new ConnectException(e.toString());
		}
	}

	/**
	 * This retreives an application from server
	 *
	 * @return an AppInterface or null on error
	 * @see xtremweb.communications.CommAPI#getApp(UID)
	 */
	@Override
	public Table getRow(final UID uid) throws ConnectException {
		try {
			final AppInterface app = (AppInterface) super.getRow(uid);
			if (app == null) {
				return null;
			}
			//
			// Next forces ISSERVICE attribute to its default value, if not set
			// Otherwise AppInterface#values[ISSERVICE] is null
			// and JTable generates an exception
			//
			app.isService();

			final Table row = app;

			return row;
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			return null;
		}
	}

}
