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
 * JobsTableModel.java
 *
 * Purpose : This is the table model to display XtremWeb tasks informations
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import xtremweb.common.AppInterface;
import xtremweb.common.DataInterface;
import xtremweb.common.GroupInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.JobInterface;
import xtremweb.common.SessionInterface;
import xtremweb.common.StatusEnum;
import xtremweb.common.Table;
import xtremweb.common.TaskInterface;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.WorkInterface;
import xtremweb.common.XMLVector;
import xtremweb.communications.URI;
import xtremweb.security.XWAccessRights;

/**
 * This displays a job by collecting informations from works, tasks, users, apps
 * and hosts.<br />
 * This is used to display informations in a more friendly way than
 * xtremweb.common.WorkInterface only. This mainly translates UID to names and
 * labels (i.e. users UID to users name etc.).<br />
 *
 * This gathers informations from apps, works, tasks, users.<br />
 *
 * This derives from xtremweb.common.TableInterface to uniform code so that it
 * can be used where a TableInterface is expected but this does not transit
 * through the network.<br />
 * This is only used on client side.<br />
 * <br />
 * This can be understood as the following SQL command:<br />
 * SELECT works.uid,users.name,apps.name,works.label,works.status,
 * works.resultstatus,works.cmdline,works.arrivaldate,
 * works.completeddate,works.resultdate,works.returncode,works.error_msg,
 * hosts.name FROM users,apps,tasks,hosts WHERE works.uid = tasks.uid AND
 * works.user = users.uid AND works.app = apps.uid AND tasks.host = hosts.uid;
 * <br />
 * <br />
 * Created: 23 avril 2006<br />
 *
 * @author <a href="mailto:lodygens /at\ .in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

class JobsTableModel extends TableModel {

	/**
	 * This is used in the application drop down menu
	 */
	private static final String SELECT = "Select...";

	/**
	 * This is the current directory. This is used to (re)open file choosers at
	 * the last directory
	 */
	private File currentDir;

	/**
	 * These defines submission parameters
	 */
	private static final String UID = "UID";
	private static final String OWNERLABEL = "Owner";
	private static final String APPLABEL = "Application";
	private static final String GROUPLABEL = "Group";
	private static final String SESSIONLABEL = "Session";
	private static final String JOBLABEL = "Label";
	private static final String ACCESSRIGHTSLABEL = "Access rights";
	private static final String CMDLINELABEL = "Command line";
	private static final String X509CERTLABEL = "X509 vertificate proxy";
	private static final String STDINLABEL = "Standard input";
	private static final String DIRINLABEL = "Local directory";
	private static final String EXPECTEDHOSTLABEL = "Expected host";
	private static final String MEMORYLABEL = "Min RAM";
	private static final String CPUSPEEDLABEL = "Min CPU Speed";

	/**
	 * This is the help
	 */
	private static final String HELPSTRING = new String("<u>" + APPLABEL
			+ "</u> is an URI; click to select/deselect<br>" + "<u>" + JOBLABEL
			+ "</u> is an optionnal field to set a job label<br>" + "<u>" + ACCESSRIGHTSLABEL
			+ "</u> : this is not editabled; this is the application one<br>" + "<u>" + GROUPLABEL
			+ "</u> is optionnal; click to select/deselect a job group<br>" + "<u>" + SESSIONLABEL
			+ "</u> is optionnal; click to select/deselect a job session<br>" + "<u>" + CMDLINELABEL
			+ "</u> is an optionnal field to set the job command line<br>" + "<u>" + EXPECTEDHOSTLABEL
			+ "</u> is optionnal; click to specify a worker to run the job<br>" + "<u>" + X509CERTLABEL
			+ "</u> is optionnal; click to select/deselect an X509 certificate proxy<br>" + "<u>" + STDINLABEL
			+ "</u> is optionnal; click to select/deselect an input text file.<br>" + "<u>" + DIRINLABEL
			+ "</u> is optionnal; click to select/deselect a directory.<br>" + "<u>" + MEMORYLABEL
			+ "</u> is optionnal; it aims to specify the minimal required RAM for the job<br>" + "<u>" + CPUSPEEDLABEL
			+ "</u> is optionnal; it aims to specify the minimal required CPU speed for the job");
	/**
	 * These defines submission parameter labels
	 */
	private static final String[] labels = { UID, OWNERLABEL, GROUPLABEL, SESSIONLABEL, APPLABEL, JOBLABEL,
			ACCESSRIGHTSLABEL, CMDLINELABEL, X509CERTLABEL, STDINLABEL, DIRINLABEL, EXPECTEDHOSTLABEL, MEMORYLABEL,
			CPUSPEEDLABEL };

	/**
	 * This combo box contains expected status to download
	 */
	private JComboBox refreshComboBox;

	/**
	 * This is the combo box selected index
	 */
	private StatusEnum refreshSelectedIndex;

	/**
	 * This is the activate button label, alos used as key in hashtable
	 */
	public static final String RESULT_LABEL = "Download results";

	/**
	 * This is the default constructor.
	 */
	public JobsTableModel(final MainFrame p) {
		this(p, true);
	}

	/**
	 * This is a constructor.
	 *
	 * @param detail
	 *            tells whether to add a last column to get details
	 */
	public JobsTableModel(final MainFrame p, final boolean detail) {
		super(p, new JobInterface(), detail);
		refreshSelectedIndex = StatusEnum.NONE;
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
		((JButton) (ret.get(ADD_LABEL))).setText("Submit");
		((JButton) (ret.get(ADD_LABEL))).setMnemonic(KeyEvent.VK_S);

		ret.remove(REFRESH_LABEL);
		refreshComboBox = new JComboBox(StatusEnum.getLabels());
		refreshComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final JComboBox source = (JComboBox) e.getSource();
				final String statusText = (String) source.getSelectedItem();
				refreshSelectedIndex = StatusEnum.fromInt(source.getSelectedIndex());
				try {
					refresh();
				} catch (final ConnectException ex) {
					getParent().setTitleNotConnected();
					getLogger().exception(ex);
				}
			}
		});

		ret.put(REFRESH_LABEL, refreshComboBox);

		((JButton) (ret.get(DEL_LABEL))).setEnabled(true);

		final JButton resultButton = new JButton(RESULT_LABEL);
		resultButton.setMnemonic(KeyEvent.VK_D);
		resultButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				result();
			}
		});
		resultButton.setEnabled(true);
		ret.put(RESULT_LABEL, resultButton);

		return ret;
	}

	/**
	 * This retreives a Vector of work UID from server
	 */
	@Override
	public XMLVector getRows() throws ConnectException {
		try {
			getParent().setTitleConnected();
			return getParent().commClient().getWorks();
		} catch (final Exception e) {
			getParent().setTitleNotConnected();
			getLogger().exception(e);
			throw new ConnectException(e.toString());
		}
	}

	/**
	 * This calls getRow(uid, false)
	 *
	 * @return an JobInterface or null on error
	 */
	@Override
	public Table getRow(final UID uid) throws ConnectException {
		return getRow(uid, false);
	}

	/**
	 * This retrieves a job interface. To improve performances default is to
	 * download Work App and User interfaces only. These are needed to display
	 * list of job in TableModel. Since 7.0.0 we retrieve full job description
	 * when user click on View button only
	 *
	 * @return an JobInterface or null on error
	 * @param uid
	 *            is the uid of the row to retrieve
	 * @param fullDescription
	 *            if true downlaod all parts (users, app, group, session, task,
	 *            host, work) otherwise only WorkInterface, AppInterface and
	 *            UserInterface are downloaded
	 * @since 7.0.0
	 */
	public Table getRow(final UID uid, final boolean fullDescription) throws ConnectException {

		try {
			final WorkInterface work = (WorkInterface) getParent().commClient().get(uid, true);
			if (work == null) {
				getLogger().error("can't find any work " + uid);
				return null;
			}

			if ((refreshSelectedIndex != StatusEnum.ANY) && (refreshSelectedIndex != work.getStatus())) {
				getLogger().info("can't find any work with the given status " + work.getStatus());
				return null;
			}

			AppInterface app = null;
			try {
				app = (AppInterface) getParent().commClient().get(work.getApplication());
			} catch (final Exception e) {
			}
			UserInterface user = null;
			try {
				user = (UserInterface) getParent().commClient().get(work.getOwner());
			} catch (final Exception e) {
				getLogger().exception(e);
			}

			GroupInterface group = null;
			SessionInterface session = null;
			HostInterface host = null;

			if (fullDescription) {
				try {
					group = (GroupInterface) getParent().commClient().get(work.getGroup());
				} catch (final Exception e) {
				}
				try {
					session = (SessionInterface) getParent().commClient().get(work.getSession());
				} catch (final Exception e) {
				}
				try {
					final TaskInterface task = (TaskInterface) getParent().commClient().get(uid);
					host = (HostInterface) getParent().commClient().get(task.getHost(), true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			final JobInterface row = new JobInterface(work, group, session, app, user, host);

			return row;
		} catch (final Exception e) {
			getLogger().exception(e);
			return null;
		}
	}

	/**
	 * This views a job
	 */
	@Override
	public void view() {
		super.view("Job viewer");
	}

	/**
	 * This creates a new ViewDialog to display row details
	 *
	 * @param title
	 *            is the dialog title
	 * @param row
	 *            is the row to edit/display
	 * @param editable
	 *            enables/disables edition
	 * @return a new ViewDialog
	 * @since 7.0.0
	 */
	@Override
	protected ViewDialog getViewDialog(final String title, final Table row, final boolean editable) {
		try {
			final Table job = getRow(row.getUID(), true);
			return new ViewDialog(getParent(), title, job.columns(false), job.toVector(), editable);
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		return null;
	}

	/**
	 * This retreives job results
	 */
	public void result() {

		final int[] selectedRows = getjTable().getSelectedRows();
		final int selection = selectedRows.length;
		if (selection == 0) {
			JOptionPane.showMessageDialog(getParent(), "No row selected!", WARNING, JOptionPane.WARNING_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(getParent(),
				"Do you want to download " + selection + " result(s) ?");
		if (confirm != 0) {
			return;
		}

		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (currentDir != null) {
			fc.setCurrentDirectory(currentDir);
		}

		confirm = fc.showOpenDialog(getParent());

		currentDir = fc.getSelectedFile();

		getLogger().debug("result currentDir = " + currentDir);
		getLogger().debug("result filename = " + fc.getName());

		if ((confirm == JFileChooser.CANCEL_OPTION) && (currentDir == null)) {
			return;
		}

		getLogger().debug("result currentDir = " + currentDir);

		getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		int downloaded = 0;

		for (int i = 0; i < selection; i++) {
			final int selectedRow = getSelectedRowIndex(selectedRows[i]);
			final WorkInterface row = ((JobInterface) getSelectedRow(selectedRow)).getWork();

			try {
				final UID jobUid = row.getUID();
				final String jobLabel = row.getLabel();
				final URI dataUri = row.getResult();
				final UID dataUid = dataUri.getUID();
				final DataInterface data = (DataInterface) getParent().commClient().get(dataUid);
				if (data == null) {
					continue;
				}

				String fext = "";
				if (data.getType() != null) {
					fext = data.getType().getFileExtension();
					System.out.println("02 data type = " + data.getType().getFileExtension());
				}
				File fdata = null;
				if (jobLabel != null) {
					fdata = new File(currentDir, jobUid.toString() + "_" + jobLabel + fext);
				} else {
					fdata = new File(currentDir, jobUid.toString() + fext);
				}

				getParent().commClient().downloadData(dataUid, fdata);
				downloaded++;
			} catch (final Exception e) {
			}
		}

		getParent().setCursor(null);

		JOptionPane.showMessageDialog(getParent(),
				"" + downloaded + " retreived result(s)\n" + "Results are stored in " + currentDir, INFO,
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * This adds a job by calling CommClient.java::send(WorkInterface)
	 */
	@Override
	public void add() {

		final UID jobUID = new UID();
		XWAccessRights accessRights = XWAccessRights.DEFAULT;
		final Vector newRow = new Vector();

		newRow.add(jobUID);
		newRow.add(new String()); // user name
		newRow.add(newContainer(Commands.GROUP)); // group
		newRow.add(newContainer(Commands.SESSION)); // session
		newRow.add(newContainer(Commands.BINARY)); // app
		newRow.add(new String()); // label
		newRow.add(new String()); // access rights
		newRow.add(new String()); // cmd line
		newRow.add(newContainer(Commands.X509CERT)); // cert
		newRow.add(newContainer(Commands.STDIN)); // stdin
		newRow.add(newContainer(Commands.DIRIN)); // dirin
		newRow.add(newContainer(Commands.EXPECTEDHOST)); // expected host
		newRow.add(new String()); // Min memory
		newRow.add(new String()); // Min CPUSpeed

		final ViewDialog vdialog = new ViewDialog(getParent(), "Submit new job", labels, newRow, true);
		setViewDialog(vdialog);

		JTextField component = (JTextField) vdialog.getFields().get(OWNERLABEL);
		component.setEditable(false);
		component.setText(getParent().user().getLogin());
		component = (JTextField) vdialog.getFields().get(ACCESSRIGHTSLABEL);
		component.setEditable(false);

		vdialog.setHelpString(HELPSTRING);

		vdialog.setVisible(true);

		boolean finished = false;

		while (!finished) {
			if (vdialog.isCancelled()) {
				return;
			}

			try {
				final WorkInterface job = new WorkInterface();
				job.setUID(jobUID);

				job.setLabel(((JTextField) vdialog.getFields().get(JOBLABEL)).getText());

				job.setCmdLine(((JTextField) vdialog.getFields().get(CMDLINELABEL)).getText());

				JPanel innerPanel = (JPanel) vdialog.getFields().get(APPLABEL);
				JTextField jtf = (JTextField) innerPanel.getComponent(0);
				String str = jtf.getText();
				try {
					job.setApplication(new UID(str));
				} catch (final Exception e) {
					throw new IOException("Invalid application : '" + str + "'");
				}

				final String accessRightsStr = ((JTextField) vdialog.getFields().get(ACCESSRIGHTSLABEL)).getText();
				if ((accessRightsStr != null) && (accessRightsStr.length() > 0)) {
					try {
						accessRights = new XWAccessRights(accessRightsStr);
					} catch (final Exception e) {
					}
				}
				job.setAccessRights(accessRights);

				innerPanel = (JPanel) vdialog.getFields().get(GROUPLABEL);
				jtf = (JTextField) innerPanel.getComponent(0);
				str = jtf.getText();
				if ((str != null) && (str.length() > 0)) {
					job.setGroup(new UID(jtf.getText()));
				}

				innerPanel = (JPanel) vdialog.getFields().get(SESSIONLABEL);
				jtf = (JTextField) innerPanel.getComponent(0);
				str = jtf.getText();
				if ((str != null) && (str.length() > 0)) {
					job.setSession(new UID(jtf.getText()));
				}

				innerPanel = (JPanel) vdialog.getFields().get(X509CERTLABEL);
				jtf = (JTextField) innerPanel.getComponent(0);
				str = jtf.getText();
				if ((str != null) && (str.length() > 0)) {
					job.setUserProxy(new URI(str));
				}

				innerPanel = (JPanel) vdialog.getFields().get(STDINLABEL);
				jtf = (JTextField) innerPanel.getComponent(0);
				str = jtf.getText();
				if ((str != null) && (str.length() > 0)) {
					job.setStdin(new URI(str));
				}

				innerPanel = (JPanel) vdialog.getFields().get(DIRINLABEL);
				jtf = (JTextField) innerPanel.getComponent(0);
				str = jtf.getText();
				if ((str != null) && (str.length() > 0)) {
					job.setDirin(new URI(str));
				}

				final String minMem = ((JTextField) vdialog.getFields().get(MEMORYLABEL)).getText();
				if ((minMem != null) && (minMem.length() > 0)) {
					job.setMinMemory(new Integer(minMem).intValue());
				}

				final String minSpeed = ((JTextField) vdialog.getFields().get(CPUSPEEDLABEL)).getText();
				if ((minSpeed != null) && (minSpeed.length() > 0)) {
					job.setMinCpuSpeed(new Integer(minSpeed).intValue());
				}

				getParent().commClient().send(job);

				finished = true;
			} catch (final Exception e) {
				getLogger().exception(e);
				JOptionPane.showMessageDialog(getParent(), "Can't send job : " + e, ERROR, JOptionPane.ERROR_MESSAGE);
				vdialog.setVisible(true);
			}
		}
	}

	/**
	 * This retreives the label of the command
	 */
	public JPanel getPanel(final Commands id) {

		String label = null;

		switch (id) {
		case BINARY:
			label = APPLABEL;
			break;
		case GROUP:
			label = GROUPLABEL;
			break;
		case SESSION:
			label = SESSIONLABEL;
			break;
		case X509CERT:
			label = X509CERTLABEL;
			break;
		case EXPECTEDHOST:
			label = EXPECTEDHOSTLABEL;
			break;
		case STDIN:
			label = STDINLABEL;
			break;
		case DIRIN:
			label = DIRINLABEL;
			break;
		default:
			break;
		}
		return (JPanel) getViewDialog().getFields().get(label);
	}

	/**
	 * This retreives the table model for the command
	 */
	public TableModel getTableModel(final Commands id) {

		TableModel tm = null;

		switch (id) {
		case BINARY:
			tm = new AppsTableModel(getParent());
			break;
		case GROUP:
			tm = new GroupsTableModel(getParent());
			break;
		case SESSION:
			tm = new SessionsTableModel(getParent());
			break;
		case X509CERT:
			tm = new DatasTableModel(getParent());
			break;
		case EXPECTEDHOST:
			tm = new HostsTableModel(getParent());
			break;
		case STDIN:
			tm = new DatasTableModel(getParent());
			break;
		case DIRIN:
			tm = new DatasTableModel(getParent());
			break;
		}

		return tm;
	}

	/**
	 * This opens a dialog box to select a row from data table and set binary,
	 * stdin, dirin
	 */
	@Override
	public void selectData(final Commands id) {

		final TableModel tm = getTableModel(id);

		try {
			tm.refresh();
		} catch (final ConnectException e) {
			getParent().setTitleNotConnected();
			getLogger().exception(e);
			return;
		}

		final Table row = selectDialogBox(id.getTitle(), tm);

		if (row == null) {
			System.out.println("row is null");
			return;
		}

		final JPanel innerPanel = getPanel(id);

		if (innerPanel != null) {
			JTextField jtf = (JTextField) innerPanel.getComponent(0);
			if (id == Commands.BINARY) {
				try {
					final UID uid = row.getUID();
					jtf.setText(uid.toString());
					final AppInterface app = (AppInterface) super.getRow(uid);
					jtf = (JTextField) getViewDialog().getFields().get(ACCESSRIGHTSLABEL);
					jtf.setText(app.getAccessRights().toString());
				} catch (final IOException e) {
				}
			} else {
				jtf.setText(((DataInterface) row).getURI().toString());
			}
		}
	}

	/**
	 * This resets command field
	 */
	@Override
	public void resetData(final Commands id) {

		final JPanel innerPanel = getPanel(id);

		if (innerPanel != null) {
			final JTextField jtf = (JTextField) innerPanel.getComponent(0);
			jtf.setText("");
		}
	}

} // class JobsTableModel
