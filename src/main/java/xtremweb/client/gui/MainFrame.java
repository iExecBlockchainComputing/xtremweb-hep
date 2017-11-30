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
 * MainFrame.java
 *
 * Purpose : XtremWeb client main frame (prev known as XWMA)
 * Created : 18 Avril 2006
 *
 * @author <a href="mailto:lodygens /at\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @version %I, %G
 */

package xtremweb.client.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;


import xtremweb.client.Client;
import xtremweb.common.Browser;
import xtremweb.common.CommonVersion;
import xtremweb.common.JarClassLoader;
import xtremweb.common.MileStone;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;
import xtremweb.common.Version;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWReturnCode;
import xtremweb.common.XWRole;
import xtremweb.common.XWTools;
import xtremweb.communications.CommClient;
import xtremweb.communications.Connection;
import xtremweb.worker.Worker;

/**
 * This class describes the needed swing frame; it implements the
 * <CODE>ActionListener</CODE> interface to catch user events. This is the main
 * class as it contains the <CODE>main</CODE> method.
 */
public final class MainFrame extends JFrame implements ActionListener {

	/**
	 * This stores the total lines
	 */
	private int totalLines;
	/**
	 * This stores the number of selected lines
	 */
	private int selectedLines;
	/**
	 * This shows lines information in the form "selectedLines/totalLines"
	 */
	private final JTextField linesInfo;

	private static final Logger logger = Logger.getLogger(MainFrame.class);

	private final Dimension DEFAULTSIZE = new Dimension(800, 600);

	/**
	 * This is the client
	 */
	private final Client client;

	/**
	 * This is the "quit" menu item, in "File" menu
	 */
	private final JMenuItem itemQuit;
	/**
	 * This is the "Login as" menu item, in "File" menu
	 */
	private final JMenuItem itemLoginAs;
	/**
	 * This is the "Start worker" button
	 */
	private final JButton startWorker;
	/**
	 * This is the "Show worker" button
	 */
	private final JButton showWorker;
	/**
	 * This is the "View tasks" menu item, in "View" menu
	 */
	private final JCheckBoxMenuItem itemTasks;
	/**
	 * This is the "View works" menu item, in "View" menu
	 */
	private final JCheckBoxMenuItem itemWorks;

	/**
	 * This is the "Help" menu item, in "Help" menu
	 */
	private final JMenuItem itemHelp;
	/**
	 * This is the "Version" menu item, in "Help" menu
	 */
	private final JMenuItem itemVersion;
	/**
	 * This is the "logger error" menu item, in "Help" menu
	 */
	private final JCheckBoxMenuItem itemError;
	/**
	 * This is the "logger warn" menu item, in "Help" menu
	 */
	private final JCheckBoxMenuItem itemWarn;
	/**
	 * This is the "logger info" menu item, in "Help" menu
	 */
	private final JCheckBoxMenuItem itemInfo;
	/**
	 * This is the "logger debug" menu item, in "Help" menu
	 */
	private final JCheckBoxMenuItem itemDebug;
	/**
	 * This is the "logger config" menu item, in "Help" menu
	 *
	 * @since 7.0.0
	 */
	private final JCheckBoxMenuItem itemConfig;
	/**
	 * This is the "logger finest" menu item, in "Help" menu
	 *
	 * @since 7.0.0
	 */
	private final JCheckBoxMenuItem itemFinest;
	/**
	 * This is the item to clear cache, in "Comm" menu
	 */
	private final JMenuItem itemClearCache;
	/**
	 * This is the HTTPCommClient menu item, in "Comm" menu
	 */
	private final JCheckBoxMenuItem itemHttp;
	/**
	 * This is the TCPCommClient menu item, in "Comm" menu
	 */
	private final JCheckBoxMenuItem itemTcp;
	/**
	 * This is the UDPCommClient menu item, in "Comm" menu
	 */
	private final JCheckBoxMenuItem itemUdp;

	/**
	 * This is the main panel
	 */
	private final MainPanel myPanel;

	/**
	 * This shows download progress
	 */
	private final JProgressBar progressBar;

	/**
	 * This constructor creates the main window. Including a <CODE>Panel</CODE>
	 * panel.
	 *
	 * @param c
	 *            is the client
	 */
	public MainFrame(final Client c) {

		super("XWHEP Client");

		this.client = c;

		addWindowListener(new WinListener());

		/*
		 * Adds menus.
		 */
		final JMenuBar menuBar = new JMenuBar();

		final JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);

		itemQuit = new JMenuItem("Close", KeyEvent.VK_C);
		itemQuit.addActionListener(this);

		itemLoginAs = new JMenuItem("Login as...", KeyEvent.VK_L);
		itemLoginAs.addActionListener(this);

		menuFile.add(itemLoginAs);
		menuFile.add(itemQuit);
		menuBar.add(menuFile);

		final JMenu menuView = new JMenu("View");
		menuView.setMnemonic(KeyEvent.VK_W);

		itemTasks = new JCheckBoxMenuItem("Show tasks");
		itemTasks.setMnemonic(KeyEvent.VK_T);
		itemTasks.addActionListener(this);
		itemTasks.setSelected(false);

		itemWorks = new JCheckBoxMenuItem("Show works");
		itemWorks.setMnemonic(KeyEvent.VK_W);
		itemWorks.addActionListener(this);
		itemWorks.setSelected(false);

		menuView.add(itemWorks);
		menuView.add(itemTasks);
		menuBar.add(menuView);

		final JMenu menuComm = new JMenu("Comm");
		menuView.setMnemonic(KeyEvent.VK_C);

		itemHttp = new JCheckBoxMenuItem("HTTP");
		itemHttp.setMnemonic(KeyEvent.VK_H);
		itemHttp.addActionListener(this);
		boolean selected = true;

		if (client.getConfig().getProperty(XWPropertyDefs.COMMLAYER) == null) {
			client.getConfig().setProperty(XWPropertyDefs.COMMLAYER);
		}

		selected = (client.getConfig().getProperty(XWPropertyDefs.COMMLAYER)
				.compareToIgnoreCase(Connection.HTTPPORT.layer()) == 0);
		itemHttp.setSelected(selected);

		itemTcp = new JCheckBoxMenuItem("TCP");
		itemTcp.setMnemonic(KeyEvent.VK_T);
		itemTcp.addActionListener(this);
		selected = false;
		selected = (client.getConfig().getProperty(XWPropertyDefs.COMMLAYER)
				.compareToIgnoreCase(Connection.TCPPORT.layer()) == 0);
		itemTcp.setSelected(selected);

		itemUdp = new JCheckBoxMenuItem("UDP");
		itemUdp.setMnemonic(KeyEvent.VK_U);
		itemUdp.addActionListener(this);
		selected = false;
		selected = (client.getConfig().getProperty(XWPropertyDefs.COMMLAYER)
				.compareToIgnoreCase(Connection.UDPPORT.layer()) == 0);
		itemUdp.setSelected(selected);

		itemClearCache = new JMenuItem("Clear cache", KeyEvent.VK_C);
		itemClearCache.addActionListener(this);

		menuComm.add(itemClearCache);
		menuComm.add(new JSeparator());
		menuComm.add(itemHttp);
		menuComm.add(itemTcp);
		menuComm.add(itemUdp);
		menuBar.add(menuComm);

		final JMenu menuHelp = new JMenu("?");
		itemHelp = new JMenuItem("Help", KeyEvent.VK_H);
		itemHelp.addActionListener(this);
		menuHelp.add(itemHelp);

		itemVersion = new JMenuItem("Version", KeyEvent.VK_V);
		itemVersion.addActionListener(this);
		menuHelp.add(itemVersion);

		menuHelp.add(new JSeparator());

		final JMenu menuLog = new JMenu("Log level");
		menuLog.setMnemonic(KeyEvent.VK_L);

		itemError = new JCheckBoxMenuItem("Error");
		itemError.setMnemonic(KeyEvent.VK_E);
		itemError.addActionListener(this);
		menuLog.add(itemError);
		itemWarn = new JCheckBoxMenuItem("Warning");
		itemWarn.setMnemonic(KeyEvent.VK_W);
		itemWarn.addActionListener(this);
		itemWarn.setSelected(true);
		menuLog.add(itemWarn);
		itemInfo = new JCheckBoxMenuItem("Info");
		itemInfo.setMnemonic(KeyEvent.VK_I);
		itemInfo.addActionListener(this);
		menuLog.add(itemInfo);
		itemConfig = new JCheckBoxMenuItem("Config");
		itemConfig.setMnemonic(KeyEvent.VK_D);
		itemConfig.addActionListener(this);
		menuLog.add(itemConfig);
		itemDebug = new JCheckBoxMenuItem("Debug");
		itemDebug.setMnemonic(KeyEvent.VK_D);
		itemDebug.addActionListener(this);
		menuLog.add(itemDebug);
		itemFinest = new JCheckBoxMenuItem("Finest");
		itemFinest.setMnemonic(KeyEvent.VK_F);
		itemFinest.addActionListener(this);
		menuLog.add(itemFinest);

		menuHelp.add(menuLog);

		menuBar.add(menuHelp);
		menuBar.add(Box.createHorizontalGlue());
		startWorker = new JButton("Start a new worker");
		startWorker.setMnemonic(KeyEvent.VK_W);
		startWorker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				startWorker();
			}
		});
		menuBar.add(startWorker);
		showWorker = new JButton("Show worker");
		showWorker.setMnemonic(KeyEvent.VK_W);
		showWorker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showWorker();
			}
		});
		showWorker.setEnabled(false);
		menuBar.add(showWorker);

		setJMenuBar(menuBar);

		myPanel = new MainPanel(this);

		myPanel.setSize(DEFAULTSIZE);
		myPanel.setPreferredSize(DEFAULTSIZE);
		getContentPane().add(myPanel, BorderLayout.CENTER);

		final JPanel statusPanel = new JPanel(new GridBagLayout());
		final GridBagLayout gbLayout = (GridBagLayout) statusPanel.getLayout();
		final GridBagConstraints gbConstraints = new GridBagConstraints();

		linesInfo = new JTextField();
		linesInfo.setEnabled(false);
		linesInfo.setMinimumSize(new Dimension(150, 10));
		linesInfo.setSize(150, 10);
		linesInfo.setHorizontalAlignment(SwingConstants.CENTER);
		gbConstraints.anchor = GridBagConstraints.EAST;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.gridx = GridBagConstraints.RELATIVE;
		gbConstraints.gridy = GridBagConstraints.RELATIVE;
		gbConstraints.weightx = 0.0;
		gbConstraints.weighty = 0.0;
		gbLayout.setConstraints(linesInfo, gbConstraints);
		statusPanel.add(linesInfo);

		progressBar = new JProgressBar();
		progressBar.setStringPainted(false);
		progressBar.setMinimum(0);

		gbConstraints.anchor = GridBagConstraints.WEST;
		gbConstraints.fill = GridBagConstraints.BOTH;
		gbConstraints.gridx = GridBagConstraints.RELATIVE;
		gbConstraints.gridy = GridBagConstraints.RELATIVE;
		gbConstraints.weightx = 1.0;
		gbConstraints.weighty = 0.0;
		gbLayout.setConstraints(progressBar, gbConstraints);
		statusPanel.add(progressBar);

		getContentPane().add(statusPanel, BorderLayout.SOUTH);

		setSize(DEFAULTSIZE);
		setPreferredSize(DEFAULTSIZE);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);

		getUser();
		myPanel.enableButtons();

	}

	/**
	 * This retrieves the client
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * This retrieves the communication layer
	 *
	 * @throws IOException
	 * @throws InstantiationException
	 */
	public CommClient commClient() throws IOException, InstantiationException {
		return client.commClient();
	}

	/**
	 * This sets the progressbar value
	 */
	public void setProgressValue(final int v) {
		progressBar.setValue(v);
		progressBar.paintImmediately(progressBar.getVisibleRect());
	}

	/**
	 * This sets the progressbar value
	 */
	public void setProgressStringPainted(final boolean b) {
		progressBar.setStringPainted(b);
	}

	/**
	 * This increments the progressbar value
	 */
	public void incProgressValue() {
		setProgressValue(getProgressValue() + 1);
	}

	/**
	 * This returns the progressbar value
	 */
	public int getProgressValue() {
		return progressBar.getValue();
	}

	/**
	 * This sets the progressbar minimum
	 */
	public void setProgressMinimum(final int v) {
		progressBar.setMinimum(v);
	}

	/**
	 * This sets the progressbar maximum
	 */
	public void setProgressMaximum(final int v) {
		progressBar.setMaximum(v);
	}

	/**
	 * This sets title to "not connected"
	 */
	public void setTitleNotConnected() {
		setTitle("XWHEP : not connected");
		myPanel.setVisible(false);
	}

	/**
	 * This sets title to "login@server"
	 */
	public void setTitleConnected() {
		setTitleConnected(client.getConfig().getUser().getLogin(), client.getConfig().getCurrentDispatcher());
	}

	/**
	 * This sets title to "login@server"
	 */
	public void setTitleConnected(final String login, final String server) {
		setTitle("XWHEP : " + login + "@" + server);
		myPanel.setVisible(true);
	}

	/**
	 * This sets the total lines and refresh linesInfo
	 */
	public void setTotalLines(final int v) {
		totalLines = v;
		linesInfo.setText("" + selectedLines + "/" + totalLines);
	}

	/**
	 * This retrieves the total lines and refresh linesInfo
	 */
	public int getTotalLines() {
		return totalLines;
	}

	/**
	 * This increments the total lines and refresh linesInfo
	 */
	public void incTotalLines() {
		setTotalLines(totalLines + 1);
	}

	/**
	 * This sets the selected lines and refresh linesInfo
	 */
	public void setSelectedLines(final int v) {
		selectedLines = v;
		linesInfo.setText("" + selectedLines + "/" + totalLines);
	}

	/******************************************************************/
	/* inner class WinListener */
	/******************************************************************/

	/**
	 * This inner class implements the interface <CODE>WindowAdapter</CODE> to
	 * catch window close event.
	 */
	class WinListener extends WindowAdapter {
		/**
		 * This is the only method of that class, inherited from interface
		 * <CODE>WindowAdapter</CODE>.
		 *
		 * @param ev
		 *            is the event to process.
		 */
		@Override
		public void windowClosing(final WindowEvent ev) {
			processQuit();
		}

	}

	/**
	 * This checks whether user is administrator
	 *
	 * @return true if user is administrator, false on connection error
	 * @see xtremweb.common.UserRightEnum
	 */
	public boolean privileged() {
		try {
			return (client.getConfig().getUser().getRights().higherOrEquals(UserRightEnum.SUPER_USER));
		} catch (final NullPointerException e) {
			return false;
		}
	}

	/**
	 * This instanciates the ActionListener interface method to catch menu
	 * events.
	 *
	 * @param ev
	 *            is the event to process.
	 */
	@Override
	public void actionPerformed(final ActionEvent ev) {
		final JMenuItem src = (JMenuItem) ev.getSource();

		if (src == itemQuit) {
			processQuit();
		} else if (src == itemLoginAs) {
			processLoginAs();
		} else if (src == itemHttp) {
			processHttp();
		} else if (src == itemTcp) {
			processTcp();
		} else if (src == itemUdp) {
			processUdp();
		} else if (src == itemTasks) {
			itemTasks.setSelected(myPanel.toggleTasks());
		} else if (src == itemWorks) {
			itemWorks.setSelected(myPanel.toggleWorks());
		} else if (src == itemVersion) {
			processVersion();
		} else if (src == itemHelp) {
			processHelp();
		} else if (src == itemClearCache) {
			processClearCache();
		}
	}

	/**
	 * This is called by menu item 'HTTP',
	 */
	private void processHttp() {
		try {
			client.getConfig().setProperty(XWPropertyDefs.COMMLAYER, Connection.HTTPPORT.layer());
			CommClient.addHandler(Connection.xwScheme(), Connection.HTTPPORT.layer());
			tryGetUser();
			itemHttp.setState(true);
			itemTcp.setState(false);
			itemUdp.setState(false);
		} catch (final Exception e) {
			itemHttp.setState(false);
			JOptionPane.showMessageDialog(this, e.toString(), TableModel.WARNING, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * This is called by menu item 'TCP',
	 */
	private void processTcp() {
		try {
			client.getConfig().setProperty(XWPropertyDefs.COMMLAYER, Connection.TCPPORT.layer());
			CommClient.addHandler(Connection.xwScheme(), Connection.TCPPORT.layer());
			tryGetUser();
			itemHttp.setState(false);
			itemTcp.setState(true);
			itemUdp.setState(false);
		} catch (final Exception e) {
			itemTcp.setState(false);
			JOptionPane.showMessageDialog(this, e.toString(), TableModel.WARNING, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * This is called by menu item 'UDP',
	 */
	private void processUdp() {
		try {
			client.getConfig().setProperty(XWPropertyDefs.COMMLAYER, Connection.UDPPORT.layer());
			CommClient.addHandler(Connection.xwScheme(), Connection.UDPPORT.layer());
			tryGetUser();
			itemHttp.setState(false);
			itemTcp.setState(false);
			itemUdp.setState(true);
		} catch (final Exception e) {
			itemUdp.setState(false);
			JOptionPane.showMessageDialog(this, e.toString(), TableModel.WARNING, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * This is called by menu item 'Quit',
	 */
	private void processQuit() {
		setVisible(false);
		System.exit(XWReturnCode.SUCCESS.ordinal());
	}

	/**
	 * This is called by menu item 'Login as...',
	 */
	private void processLoginAs() {

		final LoginDialog dlg = new LoginDialog(this);

		try {
			new MileStone(new Vector());

			if (client.getConfig() == null) {
				client.setConfig(new XWConfigurator());
			}
			client.getConfig().addDispatcher("");
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			logger.error("Can instanciate new communication layer (this is a huuuuuge bug, folks)");
			System.exit(XWReturnCode.FATAL.ordinal());
		}

		dlg.getServer().setText(client.getConfig().getCurrentDispatcher());
		dlg.setVisible(true);
		if (dlg.isCancelled() == true) {
			return;
		}

		final String login = new String(dlg.getLogin().getText());
		final String password = new String(dlg.getPassword().getPassword());

		try {
			client.getConfig().addDispatcher(dlg.getServer().getText());
			client.getConfig().setCurrentDispatcher(dlg.getServer().getText());
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(this, e.toString(), TableModel.WARNING, JOptionPane.ERROR_MESSAGE);
			return;
		}

		client.getConfig().setProperty(XWPropertyDefs.LOGIN, login);
		client.getConfig().setProperty(XWPropertyDefs.PASSWORD, password);

		try {
			commClient().disconnect();
		} catch (final Exception e) {
		}

		client.getConfig().getUser().setLogin(login);
		client.getConfig().getUser().setPassword(password);
		client.getConfig().getUser().setUID(null);

		tryGetUser();
	}

	/**
	 * This is called by menu item 'Clear Cache',
	 */
	private void processClearCache() {
		try {
			commClient().clearCache();
		} catch (final Exception e) {
		}
	}

	/**
	 * This is the window to display
	 * http://www.xtremweb-hep.org/lal/doc/xwhephelp.html
	 */
	private Browser helpViewer = null;

	/**
	 * This is called by menu item 'Help' This loads
	 * http://dghep.lal.in2p3.fr/lal/doc/xwhephelp.html and displays it in a new
	 * window
	 */
	private void processHelp() {
		try {
			if (helpViewer == null) {
				helpViewer = new Browser();
				helpViewer.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						helpViewer.setVisible(false);
					}
				});
			}
			helpViewer.setVisible(true);
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(this, "Can't get help from " + Browser.getURL(), TableModel.WARNING,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * This is called by menu item 'Version',
	 */
	private void processVersion() {
		final String url = "http://www.xtremweb-hep.org";
		Version serverVersion = null;

		try {
			serverVersion = commClient().version();
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}

		System.out.println("currentVersion " + CommonVersion.getCurrent().toString());
		System.out.println(" serverVersion " + serverVersion.toString());
		String versionWarn = "unknown";
		if (serverVersion != null) {
			versionWarn = serverVersion.toString();
		}
		if (versionWarn.compareTo(CommonVersion.getCurrent().toString()) != 0) {
			versionWarn += "\n\n* * * * * * \nYou should upgrade your client\n* * * * * * \n\n";
		}

		JOptionPane.showMessageDialog(this,
				"Current version : " + CommonVersion.getCurrent() + "\nServer  version : " + versionWarn
						+ "\nWritten by Oleg Lodygensky\n" + "LAL IN2P3 CNRS France - " + url
						+ "\n\nBased on XtremWeb 1.8.0 by LRI INRIA France\n\n" + "This software is under GPL license\n"
						+ "THIS SOFTWARE IS PROVIDED \"AS IS\" AND ANY EXPRESSED OR IMPLIED WARRANTIES,\n"
						+ "INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS\n"
						+ "FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS\n"
						+ "BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n"
						+ "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n"
						+ "LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY\n"
						+ "OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE\n"
						+ "OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF\n"
						+ "THE POSSIBILITY OF SUCH DAMAGE.\n\n",
				"XWHEP Version", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * This returns this client user
	 */
	public UserInterface user() {
		return client.getConfig().getUser();
	}

	private void tryGetUser() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			commClient().disconnect();

			if (!getUser()) {
				JOptionPane.showMessageDialog(this,
						"Connection error!!!\n\n" + "This may be due to :\n" + "  - server is not reachable;\n"
								+ "  - communication layer is wrong" + " (try another one in 'Comm' menu);\n"
								+ "  - wrong login/password.",
						TableModel.WARNING, JOptionPane.ERROR_MESSAGE);
				startWorker.setEnabled(false);
			}

			myPanel.reset();
			myPanel.enableButtons();
			client.getConfig().check();
			commClient();
			CommClient.changeConfig(client.getConfig());
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			setTitleNotConnected();
			startWorker.setEnabled(false);
			myPanel.setEnabled(false);
			JOptionPane.showMessageDialog(this,
					"Can not connect to server " + client.getConfig().getCurrentDispatcher(), TableModel.WARNING,
					JOptionPane.WARNING_MESSAGE);
		}

		setCursor(null);
	}

	/**
	 * This retrieve user's properties from server
	 */
	private boolean getUser() {
		boolean ret = true;
		try {
			String pass = null;
			String certificate = null;
			pass = client.getConfig().getUser().getPassword();
			certificate = client.getConfig().getUser().getCertificate();
			client.getConfig().setUser(commClient().getUser(client.getConfig().getUser().getLogin()));
			client.getConfig().getUser().setPassword(pass);
			client.getConfig().getUser().setCertificate(certificate);

			if (!privileged()) {
				JOptionPane.showMessageDialog(this, "You are not administrator : " + "some options are disabled",
						TableModel.WARNING, JOptionPane.WARNING_MESSAGE);
			}
			setTitleConnected(client.getConfig().getUser().getLogin(), client.getConfig().getCurrentDispatcher());

			myPanel.setEnabled(true);
			startWorker.setEnabled(true);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			setTitleNotConnected();
			myPanel.setEnabled(false);
			startWorker.setEnabled(false);
			myPanel.setEnabled(false);
			ret = false;
		}

		pack();
		return ret;
	}

	/**
	 * This is the HTTP port the worker is listening to
	 */
	private int workerPort;

	private URL workerURL = null;

	public static final String[] cursors = { "|", "/", "-", "\\" };

	/**
	 * This launches a new worker
	 */
	private void startWorker() {

		//
		// verifying URL launcher
		//
		String launchURL = client.getConfig().getProperty(XWPropertyDefs.LAUNCHERURL);
		if ((launchURL == null) || (launchURL.length() == 0)) {
			final String strurl = JOptionPane.showInputDialog(this,
					XWPropertyDefs.LAUNCHERURL.toString() + " is not set.\n" + "Do you knwow a valid URL ?",
					TableModel.WARNING, JOptionPane.WARNING_MESSAGE);
			if ((strurl == null) || (strurl.length() <= 0)) {
				return;
			}

			client.getConfig().setProperty(XWPropertyDefs.LAUNCHERURL, strurl);
			launchURL = client.getConfig().getProperty(XWPropertyDefs.LAUNCHERURL);

		}

		boolean validJarUrl = false;
		while (!validJarUrl) {

			try {
				URL url = new URL(launchURL);
				if ((url.getPath() == null) || (url.getPath().length() == 0)) {
					url = new URL(url.toString() + "/XWHEP/download/xtremweb.jar");
				}
				logger.debug("url launcher = " + launchURL);
				final JarClassLoader cl = new JarClassLoader(url);
				cl.getMainClassName();

				validJarUrl = true;
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
				final String strurl = JOptionPane.showInputDialog(this,
						"Invalid launch URL : " + launchURL + "\n" + "Do you have a valid URL ?", TableModel.WARNING,
						JOptionPane.WARNING_MESSAGE);
				if ((strurl == null) || (strurl.length() <= 0)) {
					return;
				}

				client.getConfig().setProperty(XWPropertyDefs.LAUNCHERURL, strurl);
				launchURL = client.getConfig().getProperty(XWPropertyDefs.LAUNCHERURL);
			}
		}

		showWorker.setEnabled(true);
		startWorker.setEnabled(false);

		//
		// Launching
		//
		new Thread() {
			@Override
			public void run() {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					// try to find a free port to ensure worker can start
					// a new HTTP server in order to be able to stop the worker
					workerPort = client.getConfig().getPort(Connection.HTTPWORKERPORT);

					boolean success = false;

					final JOptionPane pane = new JOptionPane("Please wait   " + cursors[0],
							JOptionPane.INFORMATION_MESSAGE);
					pane.setVisible(true);
					int i = 0;
					while (!success) {
						try {
							workerURL = new URL("http://localhost:" + workerPort + "/");
							final URLConnection connection = workerURL.openConnection();
							connection.connect();
							workerPort++;

							pane.setMessage("Please wait   " + cursors[++i % 4]);
						} catch (final Exception e) {
							logger.error("Caught exception: ", e);
							success = true;
						}
					}

					pane.setVisible(true);

					// create a configuration file
					final XWConfigurator clone = (XWConfigurator) client.getConfig().clone();
					clone.setProperty(XWPropertyDefs.ROLE, XWRole.WORKER.toString());
					clone.setProperty(Connection.HTTPPORT.toString(), "" + workerPort);
					clone.setProperty(XWPropertyDefs.STARTSERVERHTTP, "true");

					final File out = new File(System.getProperty(XWPropertyDefs.JAVATMPDIR.toString()),
							"workerconf.txt");

					clone.setConfigFile(out);
					clone.setProperty(XWPropertyDefs.CONFIGFILE, out.getCanonicalPath());

					clone.store("# Launched by client", out);

					final String[] argv = { "--xwconfig", out.getCanonicalPath() };

					setCursor(null);

					final Worker worker = new Worker();
					worker.initialize(argv);
					worker.run();
				} catch (final IOException e) {
					new JOptionPane("Worker launch error : " + e, JOptionPane.ERROR_MESSAGE).setVisible(true);
					startWorker.setEnabled(true);
					showWorker.setEnabled(false);
					setCursor(null);
				}
			}
		}.start();

		setCursor(null);
	}

	/**
	 * This launches a new worker
	 */
	private void showWorker() {

		final JOptionPane pane = new JOptionPane("Please wait   " + cursors[0], JOptionPane.INFORMATION_MESSAGE);
		pane.setVisible(true);

		for (int i = 9; i >= 0; i--) {
			try {
				Thread.sleep(1000);
			} catch (final Exception e) {
			}
			try {
				final URLConnection connection = workerURL.openConnection();
				connection.connect();

				XWTools.launchBrowser(workerURL.toString());
				break;
			} catch (final Exception e) {
			}
			pane.setMessage(new String("Please wait   " + cursors[i % 4]));
		}

		pane.setVisible(false);
	}

	/**
	 * This launches a new worker
	 */
	private void stopWorker() {

		startWorker.setEnabled(true);
		showWorker.setEnabled(false);

		try {
			final URL url = new URL("http://localhost:" + workerPort + "/?exit=1");
			url.openStream();
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
	}

}
