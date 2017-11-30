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
 * TableModel.java
 *
 * Purpose : This is the table model to display XtremWeb informations
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I%, %G%
 */

package xtremweb.client.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.ConnectException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;


import xtremweb.common.Table;
import xtremweb.common.UID;
import xtremweb.common.XMLValue;
import xtremweb.common.XMLVector;
import xtremweb.communications.URI;

/**
 * This class defines a swing table model to display XtremWeb informations<br />
 * This displays and manages an src/common/TableInterface object
 */
public abstract class TableModel extends DefaultTableModel {

	private static final Logger logger = Logger.getLogger(TableModel.class);

	public final static String WARNING = "XWHEP Warning";
	public final static String INFO = "XWHEP Information";
	public final static String ERROR = "XWHEP Error";

	/**
	 * These commands are used to set some fields in dialog boxes
	 *
	 * @see #selectData(Commands)
	 */
	protected enum Commands {
		JAVAJAR, LINUXIX86, LINUXIA64, LINUXAMD64, LINUXPPC, MACOSX86_64, MACOSIX86, MACOSPPC, WIN32IX86, WIN32AMD64, BINARY, GROUP, SESSION, X509CERT, EXPECTEDHOST, STDIN, DIRIN;

		public static final String[] titles = { "Select Java JAR binary", "Select linux ix86 binary",
				"Select linux amd64 binary", "Select linux ppc binary", "Select mac os x ix86 binary",
				"Select mac os x ppc binary", "Select win32 ix86 binary", "Select win32 amd64 binary", "Select binary",
				"Select group", "Select session", "Select X509 certificate proxy", "Select the worker to run this job",
				"Select standard input", "Select environment" };

		/**
		 * @return the titles
		 */
		public String getTitle() {
			return titles[this.ordinal()];
		}
	}

	public static final Dimension BUTTONDIMENSION = new Dimension(90, 10);
	public static final Dimension DIALOGDIMENSION = new Dimension(700, 500);

	/**
	 * This is the base dialog box
	 */
	private ViewDialog viewDialog = null;

	/**
	 * This is the select button label, also used as key in hashtable
	 */
	public static final String SELECT_LABEL = "Select all";
	/**
	 * This is the select button
	 */
	private JButton selectButton;

	/**
	 * This is the deselect button label, also used as key in hashtable
	 */
	public static final String UNSELECT_LABEL = "Clear selection";
	/**
	 * This is the unselect button
	 */
	private JButton unselectButton;

	/**
	 * This is the refresh button label, also used as key in hashtable
	 */
	public static final String REFRESH_LABEL = "Refresh";
	/**
	 * This is the refresh button
	 */
	private JButton refreshButton;
	/**
	 * This is the add button label, also used as key in hashtable
	 */
	public static final String ADD_LABEL = "Add";
	/**
	 * This is the add button
	 */
	private JButton addButton;
	/**
	 * This is the view button label, also used as key in hashtable
	 */
	public static final String VIEW_LABEL = "View";
	/**
	 * This is the view button
	 */
	private JButton viewButton;
	/**
	 * This is the del button label, also used as key in hashtable
	 */
	public static final String DEL_LABEL = "Del";
	/**
	 * This is the del button
	 */
	private JButton delButton;

	/**
	 * This tells whether to add a last column to detailed rows.
	 */
	private boolean detailed = false;

	/**
	 * This is the parent main frame
	 */
	private MainFrame parent;

	/**
	 * This is the table
	 */
	private JTable jTable;

	/**
	 * @return the jTable
	 */
	public JTable getjTable() {
		return jTable;
	}

	/**
	 * This is the sorter
	 */
	private TableSorter sorter;

	/**
	 * This is the interface to display and manage
	 */
	private final Table itf;

	/**
	 * This is a constructor.
	 *
	 * @param p
	 *            is the parent main frame
	 * @param itf
	 *            is the interface
	 * @param d
	 *            is a boolean that tells whether to add a last column to
	 *            details rows.
	 */
	protected TableModel(final MainFrame p, final Table itf, final boolean d) {

		detailed = d;
		setParent(p);
		this.itf = itf;

		selectButton = null;
		unselectButton = null;
		refreshButton = null;
		addButton = null;
		viewButton = null;
		delButton = null;
		jTable = null;
		sorter = null;
	}

	/**
	 * This contains data. Each rows has also a last item which contains a
	 * <CODE>JButton</CODE> to view data.
	 */
	private final Vector<Table> rows = new Vector<>();

	/**
	 * @param viewDialog
	 *            the viewDialog to set
	 */
	public void setViewDialog(final ViewDialog viewDialog) {
		this.viewDialog = viewDialog;
	}

	/**
	 * @return the viewDialog
	 */
	public ViewDialog getViewDialog() {
		return viewDialog;
	}

	/**
	 * @return the selectButton
	 */
	public JButton getSelectButton() {
		return selectButton;
	}

	/**
	 * @return the unselectButton
	 */
	public JButton getUnselectButton() {
		return unselectButton;
	}
	/**
	 * @return the parent
	 */
	public MainFrame getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(final MainFrame parent) {
		this.parent = parent;
	}
	public Table getInterface() {
		return itf;
	}

	public Vector<Table> getDataRows() {
		return rows;
	}


	/**
	 * This sets the JTable
	 */
	public void setTable(final JTable j) {
		jTable = j;

		final ListSelectionModel selectionListener = jTable.getSelectionModel();
		selectionListener.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}

				final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if (!lsm.isSelectionEmpty()) {
					getParent().setSelectedLines(getSelectionLength());
				}
			}
		});
		jTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					view();
				}
			}
		});
	}

	/**
	 * This gets the JTable
	 */
	public JTable getTable() {
		return jTable;
	}

	/**
	 * This sets the TableSorter
	 */
	public void setSorter(final TableSorter s) {
		sorter = s;
	}

	/**
	 * This retreives buttons. If button are not created yet (on first call),
	 * they are created.<br />
	 * Some buttons may be disabled accordingly to user rights
	 *
	 * @return a Vector of JButton
	 */
	public Hashtable getButtons() {

		final Hashtable ret = new Hashtable();

		if (selectButton == null) {
			selectButton = new JButton(SELECT_LABEL);
			selectButton.setMnemonic(KeyEvent.VK_A);
			selectButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					selectAll();
				}
			});
		}

		ret.put(SELECT_LABEL, selectButton);

		if (unselectButton == null) {
			unselectButton = new JButton(UNSELECT_LABEL);
			unselectButton.setMnemonic(KeyEvent.VK_C);
			unselectButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					unselectAll();
				}
			});
		}

		ret.put(UNSELECT_LABEL, unselectButton);

		if (refreshButton == null) {
			refreshButton = new JButton(REFRESH_LABEL);
			refreshButton.setMnemonic(KeyEvent.VK_R);
			refreshButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						getParent().setTitleConnected();
						refresh();
					} catch (final ConnectException ex) {
						logger.error("Caught exception: ", ex);
						getParent().setTitleNotConnected();
					}
				}
			});
		}

		ret.put(REFRESH_LABEL, refreshButton);

		if (addButton == null) {
			addButton = new JButton(ADD_LABEL);
			addButton.setMnemonic(KeyEvent.VK_D);
			addButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					add();
				}
			});
		}

		addButton.setEnabled(getParent().privileged());
		ret.put(ADD_LABEL, addButton);

		if (viewButton == null) {
			viewButton = new JButton(VIEW_LABEL);
			viewButton.setMnemonic(KeyEvent.VK_V);
			viewButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					view();
				}
			});
		}

		ret.put(VIEW_LABEL, viewButton);

		if (delButton == null) {
			delButton = new JButton(DEL_LABEL);
			delButton.setMnemonic(KeyEvent.VK_E);
			delButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					del();
				}
			});
		}

		delButton.setEnabled(getParent().privileged());
		ret.put(DEL_LABEL, delButton);

		return ret;
	}

	/**
	 * This adds an object
	 */
	public abstract void add();

	/**
	 * This retreives selected row indexes, if any. To retreive a row, each
	 * element of the returned array must be used as parameter of
	 * getSelectedRow() since the sorter may have modified row order
	 *
	 * @see #getSelectedRowIndex(int)
	 * @see #getSelectedRow(int)
	 */
	public int[] getSelection() {
		return jTable.getSelectedRows();
	}

	/**
	 * This retreives the amount of selected rows, if any
	 */
	public int getSelectionLength() {
		return jTable.getSelectedRows().length;
	}

	/**
	 * This retreives a selected row index, accordingly to the sorter indexation
	 *
	 * @param index
	 *            is the selected row index retreived with getSelection()
	 * @see #getSelection()
	 */
	public int getSelectedRowIndex(final int index) {
		return sorter.modelIndex(index);
	}

	/**
	 * This retreives a selected row, accordingly to the sorter indexation
	 *
	 * @param index
	 *            is the selected row index retreived with getSelection()
	 * @see #getSelection()
	 */
	public Table getSelectedRow(final int index) {
		final int selectedRow = getSelectedRowIndex(index);
		return rows.elementAt(selectedRow);
	}

	/**
	 * This deletes an entry
	 */
	public void del() {

		final int[] selectedRows = getSelection();
		if (selectedRows.length == 0) {
			JOptionPane.showMessageDialog(getParent(), "No row selected!", WARNING, JOptionPane.WARNING_MESSAGE);
			return;
		} else if (selectedRows.length > 1) {
			JOptionPane.showMessageDialog(getParent(), "You can not delete more than one row at a time", WARNING,
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		final int confirm = JOptionPane.showConfirmDialog(getParent(), "Are you sure to delete one row ?");
		if (confirm != 0) {
			return;
		}

		URI uri = null;
		Table row = null;
		try {
			row = getSelectedRow(selectedRows[0]);

			uri = getParent().commClient().newURI(row.getUID());
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			JOptionPane.showMessageDialog(getParent(), e.toString(), WARNING, JOptionPane.WARNING_MESSAGE);
			return;
		} finally {
			row = null;
		}

		try {
			getParent().commClient().remove(uri);
			getParent().setTitleConnected();
			refresh();
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			getParent().setTitleNotConnected();
		} finally {
			uri = null;
		}
	}

	/**
	 * This details objects
	 */
	public abstract void view();

	/**
	 * This saved objects to server
	 */
	protected void save(final Hashtable columns) {
		logger.error("TableModel#save() does nothing");
	}

	/**
	 * This replaces UID by human readable columns
	 */
	protected Vector getViewableRow(final Vector row) {
		logger.error("TableModel#getViewableRow() does nothing");
		return row;
	}

	/**
	 * This select all rows
	 */
	protected void selectAll() {
		jTable.selectAll();
	}

	/**
	 * This unselect all rows
	 */
	protected void unselectAll() {
		jTable.clearSelection();
		getParent().setSelectedLines(0);
	}

	/**
	 * This views an object
	 */
	protected void view(final String title) {
		this.view(title, "No help available");
	}

	/**
	 * This views an object
	 */
	protected void view(final String title, final String help) {
		final int[] selectedRows = getSelection();
		if (selectedRows.length == 0) {
			JOptionPane.showMessageDialog(getParent(), "No row selected!", WARNING, JOptionPane.WARNING_MESSAGE);
			return;
		} else if (selectedRows.length > 1) {
			JOptionPane.showMessageDialog(getParent(), "You can not view more than one row at a time", WARNING,
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		final ViewDialog dlg = getViewDialog(title, getSelectedRow(selectedRows[0]), false);
		if (dlg != null) {
			dlg.setHelpString(help);
			dlg.setVisible(true);
			if ((dlg.isEdited() == true) && (!dlg.isCancelled())) {
				save(dlg.getFields());
			}
		}
	}

	/**
	 * This retreives a Vector of object UID from server
	 */
	public abstract XMLVector getRows() throws ConnectException;

	/**
	 * This retrieves an object from cache or server if not in cache
	 *
	 * @return a TableInterface or null on error
	 * @see xtremweb.communications.CommClient#get(UID, boolean)
	 */
	public Table getRow(final UID uid) throws ConnectException {
		if(uid == null) {
			return null;
		}
		try {
			getParent().setTitleConnected();
			final Table ret = getParent().commClient().get(uid, false);
			return ret;
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
	 * @param row
	 *            is the row to edit/display
	 * @param editable
	 *            enables/disables edition
	 * @return a new ViewDialog
	 */
	protected ViewDialog getViewDialog(final String title, final Table row, final boolean editable) {
		return new ViewDialog(getParent(), title, row.notnullcolumns(false), row.toVector(), editable);
	}

	/**
	 * This reset table
	 */
	public void reset() {

		rows.clear();
		fireTableChanged(new TableModelEvent(this));
	}

	/**
	 * This retreives datas from server
	 *
	 * @see #getRows()
	 */
	public void refresh() throws ConnectException {

		int lines = 0;

		rows.clear();
		final XMLVector datas = getRows();
		final Vector<XMLValue> vdatas = (Vector<XMLValue>) datas.getXmlValues();

		getParent().setProgressStringPainted(true);
		getParent().setProgressValue(0);
		getParent().setProgressMinimum(0);
		getParent().setProgressMaximum(vdatas.size());

		getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		if (datas != null) {

			for (final Enumeration<XMLValue> e = vdatas.elements(); e.hasMoreElements();) {
				final UID uid = (UID) e.nextElement().getValue();

				getParent().incProgressValue();
				final Table row = getRow(uid);

				if (row == null) {
					continue;
				}

				rows.addElement(row);
				lines++;
			}
		}
		fireTableChanged(new TableModelEvent(this));

		getParent().setProgressStringPainted(false);
		getParent().setCursor(null);
		getParent().setTotalLines(lines);
		getParent().setSelectedLines(0);

	}

	public void close() {
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public int getColumnCount() {
		try {
			return itf.columns(true).length;
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			return 0;
		}
	}

	@Override
	public int getRowCount() {
		try {
			return rows.size();
		} catch (final Exception e) {
			return 0;
		}
	}

	@Override
	public String getColumnName(final int col) {
		final String cname = itf.columns(true)[col];
		if (cname != null) {
			return cname;
		} else {
			return "";
		}
	}

	@Override
	public Object getValueAt(final int arow, final int acol) {
		try {
			final Table row = rows.elementAt(arow);
			return row.getValue(row.getIndex(acol, true));
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			return null;
		}
	}

	/**
	 * This changes value in table thanks to its coordinates.
	 *
	 * @param value
	 *            is the new value
	 * @param arow
	 *            is the row where data is
	 * @param acol
	 *            is the column where data is
	 */
	@Override
	public void setValueAt(final Object value, final int arow, final int acol) {

		if (!isCellEditable(arow, acol)) {
			return;
		}

		try {
			final Table row = rows.elementAt(arow);
			row.setValue(acol, value);
		} catch (final Exception e) {
			logger.error("Caught exception, TasksTableModel::setValueAt () ", e);
			return;
		}

	}

	/**
	 * JTable uses this method to determine the default renderer/ editor for
	 * each cell. If we didn't implement this method, then the last column would
	 * contain text ("true"/"false"), rather than a check box.
	 */
	@Override
	public Class getColumnClass(final int column) {
		try {
			if (getValueAt(0, column) == null) {
				return String.class;
			}
			return getValueAt(0, column).getClass();
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			return String.class;
		}
	}

	/**
	 * This is called to determine whether a cell is editable or not. This
	 * returns true for the last column only to enable row details edition.
	 *
	 * @param row
	 *            : cell row
	 * @param column
	 *            : cell column
	 * @return true if cell is editable
	 */
	@Override
	public boolean isCellEditable(final int row, final int column) {
		return false;
	}

	/**
	 * This create a new JPanel containing a JTextField with associated button
	 * The button calls selectData()
	 *
	 * @return the new JPanel
	 */
	protected JPanel newContainer(final Commands id) {
		final JButton selectButton = new JButton("Select");
		final JButton resetButton = new JButton("Reset");
		final JTextField uri = new JTextField();
		selectButton.setMaximumSize(BUTTONDIMENSION);
		selectButton.setMinimumSize(BUTTONDIMENSION);
		selectButton.setPreferredSize(BUTTONDIMENSION);
		selectButton.setSize(BUTTONDIMENSION);
		selectButton.addActionListener(new ActionListener() {
			@Override
			public final void actionPerformed(final ActionEvent e) {
				selectData(id);
			}
		});
		resetButton.setMaximumSize(BUTTONDIMENSION);
		resetButton.setMinimumSize(BUTTONDIMENSION);
		resetButton.setPreferredSize(BUTTONDIMENSION);
		resetButton.setSize(BUTTONDIMENSION);
		resetButton.addActionListener(new ActionListener() {
			@Override
			public final void actionPerformed(final ActionEvent e) {
				resetData(id);
			}
		});

		final JPanel container = new JPanel(new GridBagLayout());
		final GridBagLayout gbLayout = (GridBagLayout) container.getLayout();
		final GridBagConstraints gbConstraints = new GridBagConstraints();
		gbConstraints.anchor = GridBagConstraints.CENTER;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.gridx = GridBagConstraints.RELATIVE;
		gbConstraints.gridy = GridBagConstraints.RELATIVE;
		gbConstraints.weightx = 1.0;
		gbConstraints.weighty = 0.0;
		container.add(uri);
		container.add(selectButton);
		container.add(resetButton);
		gbLayout.setConstraints(uri, gbConstraints);
		gbLayout.setConstraints(container, gbConstraints);
		gbConstraints.weightx = 0.0;
		gbLayout.setConstraints(selectButton, gbConstraints);
		gbLayout.setConstraints(resetButton, gbConstraints);

		return container;
	}

	/**
	 * This does nothing and must be overriden, if needed
	 */
	public void selectData(final Commands id) {
		logger.error("selectData does nothing for : " + id);
	}

	/**
	 * This does nothing and must be overriden, if needed
	 */
	public void resetData(final Commands id) {
		logger.error("selectData does nothing for : " + id);
	}

	/**
	 * This opens a dialog box to display the provided table model. The can
	 * select a row from the provided table model
	 */
	public Table selectDialogBox(final String title, final TableModel tableModel) {

		final Vector newRow = new Vector();

		final JPanel container = new JPanel(new GridBagLayout());
		final GridBagLayout gbLayout = (GridBagLayout) container.getLayout();
		final GridBagConstraints gbConstraints = new GridBagConstraints();
		gbConstraints.anchor = GridBagConstraints.CENTER;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.gridx = GridBagConstraints.RELATIVE;
		gbConstraints.gridy = GridBagConstraints.RELATIVE;
		gbConstraints.weightx = 1.0;
		gbConstraints.weighty = 1.0;
		gbConstraints.gridwidth = GridBagConstraints.REMAINDER;

		final TablePanel panel = new TablePanel(tableModel);
		gbLayout.setConstraints(panel, gbConstraints);
		container.add(panel);
		newRow.add(container);

		final ViewDialog dlg = new ViewDialog(getParent(), title, null, newRow, false);

		dlg.setSize(DIALOGDIMENSION);
		dlg.setVisible(true);

		if ((dlg.isCancelled() == true) || (tableModel.getSelectionLength() != 1)) {
			return null;
		}

		final int selection = tableModel.getSelection()[0];
		Table row = tableModel.getSelectedRow(selection);
		UID uid = null;
		try {
			uid = row.getUID();
			final Table ret = getParent().commClient().get(uid, false);
			return ret;
		} catch (final Exception exc) {
		} finally {
			row = null;
			uid = null;
		}
		return null;
	}
}
