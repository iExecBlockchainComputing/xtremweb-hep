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

package xtremweb.common;

/**
 * XWConfigurator.java
 * Worker, Client and Server Config
 *
 * Created : Mon Mar 25 2002
 *
 * @author Samuel Heriard, Oleg Lodygensky
 */

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.swing.JMenuItem;

import com.iexec.common.ethereum.CommonConfiguration;
import com.iexec.common.ethereum.CredentialsService;
import com.iexec.common.ethereum.IexecConfigurationService;
import com.iexec.common.workerpool.WorkerPoolConfig;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import xtremweb.archdep.ArchDepFactory;
import xtremweb.communications.CommClient;
import xtremweb.communications.Connection;
import xtremweb.communications.URI;
import xtremweb.security.PEMPrivateKey;
import xtremweb.security.PEMPublicKey;
import xtremweb.security.X509Proxy;
import xtremweb.worker.Worker;

import static xtremweb.common.XWPropertyDefs.BLOCKCHAINETHENABLED;

public final class XWConfigurator extends Properties {

	/**
	 * This is the logger
	 */
	private final Logger logger;

	private static final String LOCALHOST = "localhost";
	/**
	 * This contains worker volunteer applications. This contains application
	 * types.
	 *
	 * @see AppTypeEnum
	 * @since 8.0.0 (FG)
	 */
	private Collection<String> localApps;

	/**
	 * @return the localApps
	 */
	public Collection<String> getLocalApps() {
		return localApps;
	}

	/**
	 * This is the config file to read/write properties
	 */
	private File configFile;

	public File getConfigFile() {
		return configFile;
	}

	public void setConfigFile(final File c) {
		configFile = c;
	}

	/**
	 * This is the launch date
	 *
	 * @since XWHEP 1.0.0
	 */
	private final Date upTime;

	/**
	 * This retrieves the launch date
	 *
	 * @return the up time date
	 * @since XWHEP 1.0.0
	 */
	public Date upTime() {
		return upTime;
	}

	/**
	 * This is the current dispatcher index
	 *
	 * @since v1r2-rc1(RPC-V)
	 */
	private int currentDispatcher;

	/**
	 * This is the current data server index
	 *
	 * @since XWHEP 1.0.0
	 */
	private int currentDataServer;

	/**
	 * This is the minimum timeout
	 * <ul>
	 * <li>server : timeout to pool the database
	 * <li>worker : timeout to request a new work
	 * <li>client : timeout to check a result
	 * </ul>
	 * This is set to 10ms
	 *
	 * @since RPCXW
	 */
	public static final int MINTIMEOUT = 10;
	/**
	 * This is the maximum timeout; this is only use by the worker to ensure the
	 * worker comes back sometime to request a new work
	 * <ul>
	 * <li>worker : timeout to request a new work
	 * </ul>
	 * This is set to 5mn
	 *
	 * @since XWHEP
	 */
	public static final int MAXTIMEOUT = 300000;
	/**
	 * This vector contains a set of XtremWeb server adresses. This is used for
	 * replication; this list contains dispatchers read from conf file and
	 * provided by server at connections time.
	 *
	 * @since v1r2-rc1(RPC-V)
	 */
	private Collection<String> dispatchers;
	/**
	 * This vector contains a set of XtremWeb server adresses. This is used for
	 * replication; this list contains dataServers read from conf file and
	 * provided by server at connections time.
	 *
	 * @since v1r2-rc1(RPC-V)
	 */
	private Collection<String> dataServers;
	/**
	 * This is the amount of jobs this worker has already computed
	 *
	 * @since RPCXW
	 */
	private int nbJobs;

	/**
	 * @return the nbJobs
	 */
	public int getNbJobs() {
		return nbJobs;
	}

	/**
	 * This increments nbjobs
	 *
	 * @return the nbJobs
	 */
	public int incNbJobs() {
		return ++nbJobs;
	}

    /**
     * This checks if blockchain service is enabled
     * @return true if blockchain service is enabled
     * @since 13.1.0
     */
    public boolean blockchainEnabled() {
        return getBoolean(BLOCKCHAINETHENABLED);
    }
    /**
     * This disables blockchain access
     * @since 13.1.0
     */
    public void disableBlockchain() {
        setProperty(XWPropertyDefs.BLOCKCHAINETHENABLED, "false");
    }

    /**
	 * This is the maximum jobs this worker will compute before dying This is
	 * expecially usefull to deploy workers over Grids
	 *
	 * @since RPCXW
	 */
	public boolean stopComputing() {
		final int maxJobs = getInt(XWPropertyDefs.COMPUTINGJOBS);
		return ((maxJobs > 0) && (nbJobs > maxJobs));
	}

	/**
	 * This returns the max timeout to wait between two work requests. This is
	 * only used by the worker like this (src/common/CommManager.java) :
	 * nullWorkTimeOut = (nullWorkTimeOut * 2) % config.maxTimeout()
	 *
	 * @see #realTime()
	 * @see #MINTIMEOUT
	 * @see #MAXTIMEOUT
	 * @return if(realTime()) MINTIMEOUT, timeout * 40 otherwise; if timeout is
	 *         not set, this returns MAXTIMEOUT,
	 * @since RPCXW
	 */
	public int getMaxTimeout() {
		try {
			if (realTime()) {
				return MINTIMEOUT;
			} else {
				return Integer.parseInt(getProperty(XWPropertyDefs.TIMEOUT)) * 40;
			}
		} catch (final Exception e) {
			setProperty(XWPropertyDefs.TIMEOUT, "" + MAXTIMEOUT);
			return MAXTIMEOUT;
		}
	}

	/**
	 * This test whether we try "real time"
	 *
	 * @since RPCXW
	 */
	public boolean realTime() {
		try {
			return Long.parseLong(getProperty(XWPropertyDefs.TIMEOUT)) <= MINTIMEOUT;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This is used to set new object owner when automatically inserting new
	 * object. This is typically the case on unregistered user first connection.
	 * If the user X509 certificate is validated against CA cert path, the
	 * server automatically registers the user by inserting it to DB. The new
	 * user owner is set to _adminuid.
	 */
	private UID _adminuid;

	/**
	 * @return the _adminuid
	 */
	public UID getAdminUid() {
		return _adminuid;
	}

	/** This is the host identification */
	private HostInterface _host;

	/**
	 * @return the _host
	 */
	public HostInterface getHost() {
		return _host;
	}

	/** This is the user identification */
	private UserInterface _user;

	/**
	 * @param _user
	 *            the _user to set
	 */
	public void setUser(final UserInterface _user) {
		this._user = _user;
	}

	/**
	 * @return the _user
	 */
	public UserInterface getUser() {
		return _user;
	}

	/** various hardware configuration flag */
	private boolean hasKeyboard = true;
	private boolean hasMouse = true;

	/** This is the dir name where binaries are stored */
	private String binCachedPath;

	public String cachedPath() {
		return binCachedPath;
	}

	/** This is the max amout of binaries stored on worker side */
	public static final int MAX_BIN = 5;

	/**
	 * This is the file that contain the ethereum wallet
	 * @since 12.2.7
	 */
	private File ethWalletFile;
	private String ethWalletPassword;

	public File getEthWalletFile() {
		return ethWalletFile;
	}

	public void setEthWalletFile(final File wallet) {
		this.ethWalletFile = wallet;
	}

	/**
	 * This is the Web3j entry point
	 * @since 12.2.7
	 */
	private Web3j web3;
	/**
	 * This is the Web3j walletCredentials
	 * @since 12.2.7
	 */
	private Credentials walletCredentials;

	/**
	 * This is the file that contain the keystore
	 */
	private File keyStoreFile;

	public File getKeyStoreFile() {
		return keyStoreFile;
	}

	public void setKeyStoreFile(final File keyStoreFile) {
		this.keyStoreFile = keyStoreFile;
	}

	/**
	 * This is the keystore manager
	 */
	private KeyManagerFactory keyManagerFactory;

	public KeyManagerFactory getKeyManagerFactory() {
		return keyManagerFactory;
	}

	public void setKeyManagerFactory(final KeyManagerFactory keyManagerFactory) {
		this.keyManagerFactory = keyManagerFactory;
	}

	/**
	 * This is the keystore
	 *
	 * @since 8.0.2
	 */
	private KeyStore keyStore;

	public KeyStore getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(final KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	/**
	 * This is the user X509 proxy
	 *
	 * @since 7.0.0
	 */
	private X509Proxy x509Proxy;
	/**
	 * This is the user X509 private key
	 *
	 * @since 7.4.1
	 */
	private PEMPrivateKey privateKey;

	/**
	 * @return the privateKey
	 */
	public PEMPrivateKey getPrivateKey() {
		return privateKey;
	}

	/**
	 * This is the user X509 public key
	 *
	 * @since 7.5.0
	 */
	private PEMPublicKey publicKey;

	/**
	 * @return the privateKey
	 */
	public PEMPublicKey getPublicKey() {
		return publicKey;
	}

	/**
	 * This tells is we can challenge connections
	 */
	private boolean challenging;

	public boolean getChallenging() {
		return challenging;
	}

	public void setChallenging(final boolean v) {
		challenging = v;
	}

	/**
	 * This is the a mandate: a user login for which actions should be taken
	 * @since 11.0.0
	 * @see UserRightEnum#MANDATED_USER
	 */
	private String mandate;


	/**
	 * @return the mandate
	 * @since 11.0.0
	 */
	public String getMandate() {
		return mandate;
	}

	/**
	 * @param mandat the mandate to set
	 * @since 11.0.0
	 */
	public void setMandate(final String mandat) {
		this.mandate = mandat;
	}

	/** notify configuration */
	private boolean useNotify = false;

	/** size of the work pool */
	private int workPoolSize;

	/**
	 * @return the workPoolSize
	 */
	public int getWorkPoolSize() {
		return workPoolSize;
	}

	/**
	 * This tells whether to load system dependant libraries default is true;
	 * must be set to false for the client
	 */
	private boolean loadLibraries;
	/**
	 * This contains this process environment variables
	 *
	 * @since 8.0.1
	 */
	private String[] baseEnvVars;

	/**
	 * @return the baseEnvVars
	 */
	public String[] getBaseEnvVars() {
		return baseEnvVars;
	}

	/**
	 * Use java NIO ?
	 *
	 * @since XWHEP 1.0.0
	 */
	public boolean nio() {
		return getBoolean(XWPropertyDefs.JAVANIO);
	}

	/**
	 * Start HTTP server ?
	 *
	 * @since XWHEP 1.0.0
	 */
	public boolean http() {
		return getBoolean(XWPropertyDefs.STARTSERVERHTTP);
	}

	/**
	 * @since XWHEP 1.0.0
	 */
	public URL launcherURL() throws MalformedURLException {
		return new URL(getProperty(XWPropertyDefs.LAUNCHERURL));
	}

	/**
	 * This return the name of the SQL file needed to create DB This is
	 * typically used with hsqldb to create tables at boot
	 *
	 * @since XWHEP 4.0.0
	 */
	public String sqlFile() {
		return getProperty(XWPropertyDefs.DBSQLFILE);
	}

	/**
	 * This return the limit of returned rows by SELECT SQL statement
	 *
	 * @since XWHEP 5.8.0
	 * @see xtremweb.common.XWPropertyDefs#DBREQUESTLIMIT
	 */
	public int requestLimit() {
		int ret =  getInt(XWPropertyDefs.DBREQUESTLIMIT);
		if (ret > XWTools.MAXDBREQUESTLIMIT) {
		    ret = XWTools.MAXDBREQUESTLIMIT;
		}
		return ret;
	}

	/**
	 * This sets sql request limit (how many rows an SQL SELECT could returns)
	 *
	 * @since 5.8.0
	 */
	public void setRequestLimit(final int l) {
		setProperty(XWPropertyDefs.DBREQUESTLIMIT, "" + l);
	}

	/**
	 * This returns true if we use HSQLDB:MEMORY
	 */
	public boolean dbmem() {
		final String p = getProperty(XWPropertyDefs.DBENGINE);
		return p == null ? false : p.compareToIgnoreCase(XWDBs.MEMENGINE) == 0;
	}

	/**
	 * This is the maximum cpu load dedicated to XWWorker This should be
	 * understood as follow : IF this.cpuLoad > XWWorkerEffectiveCpuLoad THEN
	 * the worker stop computing because the cpu is expected for other processes
	 * than XWWorker
	 *
	 * XWWorker can not load cpu more than this.cpuLoad if the cpu is expected
	 * by its owner
	 */
	public int cpuLoad() {
		return getInt(XWPropertyDefs.CPULOAD);
	}

	public LoggerLevel getLoggerLevel() {

		final String p = getProperty(XWPropertyDefs.LOGGERLEVEL);
		if (p == null) {
			setProperty(XWPropertyDefs.LOGGERLEVEL);
			return LoggerLevel.valueOf(XWPropertyDefs.LOGGERLEVEL.defaultValue());
		} else {
			return LoggerLevel.valueOf(p.toUpperCase());
		}
	}

	/**
	 * This initializes default values. This is used to be able to launch the
	 * client GUI without any config file
	 */
	public XWConfigurator() {
		super();

		defaults = new Properties();

		setProperty(XWPropertyDefs.LOGGERLEVEL);

		logger = new Logger(this);

		upTime = new Date();
		dispatchers = new Vector<>();
		dataServers = new Vector<>();
		try {
			addDispatcher(LOCALHOST);
			setCurrentDispatcher(LOCALHOST);
			_host = new HostInterface();
			_host.setName(XWTools.getLocalHostName());
			_user = new UserInterface();

			Runtime.getRuntime().addShutdownHook(new Thread(XWRole.getMyRole().toString() + "Cleaner") {
				@Override
				public void run() {
					try {
						if (XWRole.getMyRole() == XWRole.WORKER) {
							XWTools.deleteDir(getCacheDir());
							XWTools.deleteDir(getTmpDir());
						}
					} catch (final IOException e) {
					}
				}
			});
		} catch (final Exception e) {
		}
	}

	/**
	 * This contructs XtremWeb client side(XWClient, XWWorker..) properties.<br>
	 * Properties may be read from several places:
	 * <ol>
	 * <li>the file which name is provided as arguments;
	 * <li>some 'standard' files from
	 * <ol>
	 * <li>$HOME/.xtremweb/xtremweb.[client|server|worker].conf
	 * <li>/etc/xwrc
	 * <li>file pointed by the environment variable 'xtremweb.config'
	 * <ol>
	 * <li>the file included in the JAR file
	 * </ol>
	 *
	 * @param cfn
	 *            is the name of the config file, or null if none
	 * @param firstTime
	 *            is used to know whether the worker has upgrade
	 */
	public XWConfigurator(final String cfn, final boolean firstTime) {

		this();

		final String configFileName = cfn;

		configFile = null;

		if (configFileName != null) {
			configFile = new File(configFileName);
			try {
				setProperty(XWPropertyDefs.CONFIGFILE, configFile.getAbsolutePath());
			} catch (final Exception e) {
				setProperty(XWPropertyDefs.CONFIGFILE, configFile.toString().trim());
			}
		}

		if ((configFile == null) || (!configFile.exists())) {

			final String fname = "xtremweb." + XWRole.getMyRole().toString().toLowerCase() + ".conf";
			final String[] stdLoc = {
					getProperty(XWPropertyDefs.USERHOMEDIR) + File.separator + ".xtremweb" + File.separator + fname,
					getProperty(XWPropertyDefs.USERHOMEDIR) + File.separator + ".xtremweb" + File.separator
							+ "config.defaults",
					"/etc/" + fname, getProperty(XWPropertyDefs.CONFIGFILE) };
			for (int i = 0; i < stdLoc.length; i++) {
				try {
					configFile = new File(stdLoc[i]);
					if (configFile.exists()) {
						setProperty(XWPropertyDefs.CONFIGFILE, configFile.getAbsolutePath());
						break;
					}
				} catch (final Exception e) {
				}
				configFile = null;
			}
		}

		try {
			InputStream def = null;

			if ((configFile == null) || (!configFile.exists())) {
				def = getClass().getResourceAsStream("/misc/config.defaults");
			} else {
				def = new FileInputStream(configFile);
			}

			load(def);
			def.close();
		} catch (final Exception e) {
			logger.exception("Can't open config file " + configFileName, e);
			System.exit(XWReturnCode.DISK.ordinal());
		}

		logger.setLoggerLevel(getLoggerLevel());

		try {
			configFile = configFile.getCanonicalFile();
		} catch (final Exception e) {
		}

		setProperty(XWPropertyDefs.CONFIGFILE, configFile.toString());

		// don't forget to update the config
		// i.e. sync the instances variables with the Properties object
		try {
			refresh(firstTime);
			setProperty(XWPropertyDefs.ALONE);
		} catch (final Exception e) {
			e.printStackTrace();
			logger.exception("error in config file", e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}

		currentDispatcher = 0;
		currentDataServer = 0;
		loadLibraries = true;

		//
		// retrieve all environment variables
		//
		final Map<String, String> envmap = System.getenv();
		baseEnvVars = new String[envmap.size()];
		final Set<Map.Entry<String, String>> envset = envmap.entrySet();
		final Iterator<Map.Entry<String, String>> envit = envset.iterator();
		int i = 0;
		while (envit.hasNext()) {
			final Map.Entry<String, String> entry = envit.next();
			baseEnvVars[i++] = new String(entry.getKey() + "=" + entry.getValue());
		}
	}

	/**
	 * This sets the loadLibraries member attribute
	 *
	 * @param a
	 *            tells to load libraries or not; must be false for the client
	 */
	public void loadLibrary(final boolean a) {
		loadLibraries = a;
	}

	/**
	 * This updates all the fields using the current values of the Properties
	 * object This sets some default values if not defined in the config file.
	 * If using a X509 certificate, the user login is set to : subjectName + "_"
	 * + issuerName;
	 *
	 * @param firstTime
	 *            is used to know whether the worker has upgrade
	 */
	private void refresh(final boolean firstTime)
			throws IOException, CertificateException, CertificateExpiredException {

		// parse all properties from config file
		// we must be case insensitive
		// we still allow old names too
		final Enumeration<String> names = (Enumeration<String>) propertyNames();
		while (names.hasMoreElements()) {
			final String pname = names.nextElement();
			try {
				setProperty(XWPropertyDefs.valueOf(pname.toUpperCase()), getProperty(pname), false);
			} catch (final Exception e) {
				try {
					// this is for backward compatibility
					setProperty(XWPropertyDefs.fromString(pname), getProperty(pname), false);
				} catch (final Exception e2) {
					logger.exception(e);
				}
			}
		}

		// set properties to default value, if not set
		// Property can be set (by priority)
		// -1- environment variable
		// -2- system variable
		// -3- config file
		for (final XWPropertyDefs p : XWPropertyDefs.values()) {
			if (p == XWPropertyDefs.XWCP) {
				try {
					setProperty(XWPropertyDefs.XWCP,
							configFile.getParentFile().getParentFile().getAbsolutePath() + File.separator + "lib");
				} catch (final Exception e) {
					setProperty(XWPropertyDefs.XWCP,
							configFile.getParentFile().getParentFile().getPath() + File.separator + "lib");
				}
			}

			final String pstr = p.toString();
			final String pSysEnv = System.getenv(pstr);
			final String pSysProp = System.getProperty(pstr);
			final String pProp = getProperty(pstr);
			logger.finest("pProp(" + p + ") = " + pProp);
			logger.finest("pSysProp(" + p + ") = " + pSysProp);
			logger.finest("pSysEnv(" + p + ") = " + pSysEnv);
			if (pSysProp != null) {
				setProperty(p, pSysProp);
			}
			else if (pProp != null) {
				setProperty(p, pProp);
			}
			else if (pSysEnv != null) {
				setProperty(p, pSysEnv);
			}
			else {
				setProperty(p);
			}
			logger.debug("getProperty(" + p + ") = " + getProperty(p));
		}

		if (getInt(XWPropertyDefs.SORETRIES) < 1) {
			setProperty(XWPropertyDefs.SORETRIES);
		}

		logger.setLoggerLevel(getLoggerLevel());

		final String p = getProperty(XWPropertyDefs.ROLE);
		if (p == null) {
			throw new IOException("can't retreive role");
		}
		XWRole.setRole(XWRole.valueOf(p.toUpperCase()));

		//
		// 2 nov 2011 : we don't want any device at all
		// See :
		// http://java.sun.com/developer/technicalArticles/J2SE/Desktop/headless/
		if (XWRole.isWorker() || XWRole.isDispatcher()) {
			setProperty("java.awt.headless", "true");
		}

		final String mandat = getProperty(XWPropertyDefs.MANDATINGLOGIN);
		setProperty(XWPropertyDefs.MANDATINGLOGIN, mandat, true);
		setMandate(getProperty(XWPropertyDefs.MANDATINGLOGIN));

		if (!XWRole.isDispatcher()) {
			final String login = getProperty(XWPropertyDefs.LOGIN);
			final String passwd = getProperty(XWPropertyDefs.PASSWORD);
			if ((login == null) || (login.length() < 1) ||
					(passwd == null) || (passwd.length() < 1)){
				throw new IOException("You must provide Login and password");
			}
			_user.setLogin(login);
			_user.setPassword(passwd);
			try {
				final String uidstr = getProperty(XWPropertyDefs.USERUID);
				if (uidstr != null) {
					_user.setUID(new UID(uidstr));
				}
			} catch (final IllegalArgumentException e) {
			}
		} else {

			final String uidstr = getProperty(XWPropertyDefs.ADMINUID);
			if (uidstr != null) {
				_adminuid = new UID(uidstr);
			}
	
			final String login = getProperty(XWPropertyDefs.ADMINLOGIN);
			if (login == null) {
				throw new IOException("No admin login provided");
			}
			setProperty(XWPropertyDefs.ADMINLOGIN, login);
		}

		keyStoreFile = getFile(XWPropertyDefs.SSLKEYSTORE);
		if ((keyStoreFile == null) || !keyStoreFile.exists()) {
			keyStoreFile = getFile(XWPropertyDefs.JAVAKEYSTORE);
		}

		if ((keyStoreFile == null) || !keyStoreFile.exists()) {
			logger.warn(XWPropertyDefs.SSLKEYSTORE.toString() + " (" + keyStoreFile + ") does not exist");

			try {
				keyStoreFile = new File("xwhep" + XWRole.getMyRole().toString().toLowerCase() + ".keys");
				setProperty(XWPropertyDefs.SSLKEYSTORE, keyStoreFile.getAbsolutePath());
				extractResource("misc/xwhep" + XWRole.getMyRole().toString().toLowerCase() + ".keys", keyStoreFile);
			} catch (final Exception e) {
				logger.exception(e);
				keyStoreFile = null;
			}
		}

		if ((keyStoreFile != null) && keyStoreFile.exists()) {
			final String passPhrase = getProperty(XWPropertyDefs.SSLKEYPASSPHRASE);
			final String passWord = getProperty(XWPropertyDefs.SSLKEYPASSWORD);
			try {
				logger.finest("keystore password = " + passWord);
				keyStore = KeyStore.getInstance("JKS");
				keyStore.load(new FileInputStream(keyStoreFile), (passWord == null ? null : passWord.toCharArray()));
				keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
				keyManagerFactory.init(keyStore, (passPhrase == null ? null : passPhrase.toCharArray()));
			} catch (final IOException e) {
				logger.exception("Can't open keystore", e);
				throw e;
			} catch (final KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
				keyStore = null;
				logger.exception("Can't init keystore", e);
			}
			XWTools.setCACertificateEntries(keyStore);
		}

		if ((keyStore != null) && (keyStoreFile != null)) {
			setProperty(XWPropertyDefs.SSLKEYSTORE, keyStoreFile.getAbsolutePath());
		} else {
			setProperty(XWPropertyDefs.SSLKEYSTORE, "");
		}

		ethWalletFile = getFile(XWPropertyDefs.ETHWALLETPATH);
		if ((ethWalletFile == null) || !ethWalletFile.exists()) {
			logger.info("No Ethereum wallet file or file not found");
		} else {
			web3 = Web3j.build(new HttpService());  // defaults to http://localhost:8545/
			try {
				Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
				final String clientVersion = web3ClientVersion.getWeb3ClientVersion();
				logger.info("Web3j client version " + clientVersion);
			} catch (IOException e) {
				logger.exception("Web3j error; cancelling wallet access", e);
				ethWalletFile = null;
			}

			ethWalletPassword = getProperty(XWPropertyDefs.ETHWALLETPASSWORD);
			try {
				walletCredentials = WalletUtils.loadCredentials(ethWalletPassword, ethWalletFile);
				logger.info("Credentials " + walletCredentials.getAddress());
			} catch (IOException | CipherException e) {
				logger.fatal("Web3j error; can't read walletCredentials : " + e);
			}
		}
		// worker don't use certificates
		if (!XWRole.isClient()) {
			setProperty(XWPropertyDefs.X509USERPROXY, "");
			setProperty(XWPropertyDefs.USERCERT, "");
			setProperty(XWPropertyDefs.USERKEY, "");
			setProperty(XWPropertyDefs.USERKEYPASSWORD, "");
		}
		//
		// X509USERPROXY overrides USERCERT, USERKEY, USERKEYPASSWORD
		//
		try {
			final File proxyfile = getFile(XWPropertyDefs.X509USERPROXY);
			if ((proxyfile != null) && (proxyfile.exists())) {
				XWTools.checkCertificate(proxyfile);
				x509Proxy = new X509Proxy(proxyfile);
				_user.setCertificate(x509Proxy.getContent());
				final String subjectName = x509Proxy.getSubjectName();
				final String issuerName = x509Proxy.getIssuerName();

				// a subject name may not necessarely be unic
				// subject name associated to issuer name is necessarely unic!
				final String loginName = subjectName + "_" + issuerName;
				_user.setLogin(loginName);
				_user.setPassword(null);
				setProperty(XWPropertyDefs.USERCERT, "");
				setProperty(XWPropertyDefs.USERKEY, "");
				setProperty(XWPropertyDefs.USERKEYPASSWORD, "");
				setChallenging(false);
				privateKey = null;
				publicKey = null;
			}
		} catch (final Exception e) {
			x509Proxy = null;
			logger.fatal("X509 proxy error : " + e);
		}
		try {
			final File proxyfile = getFile(XWPropertyDefs.USERCERT);
			if ((proxyfile != null) && (proxyfile.exists())) {
				publicKey = new PEMPublicKey();
				publicKey.read(proxyfile);
				final String subjectName = publicKey.getSubjectName();
				final String issuerName = publicKey.getIssuerName();

				// a subject name may not necessarily be unique
				// subject name associated to issuer name is necessarily unique!
				final String loginName = subjectName + "_" + issuerName;
				_user.setLogin(loginName);
				_user.setPassword(null);
			}
		} catch (final Exception e) {
			logger.fatal("User cert error : " + e);
		}

		setChallenging(false);

		try {
			final File userkeyfile = getFile(XWPropertyDefs.USERKEY);
			final String userkeypassword = getProperty(XWPropertyDefs.USERKEYPASSWORD);

			if (userkeyfile != null) {
				if ((userkeypassword == null) || (userkeypassword.length() < 1)) {
					logger.fatal("Private key password can not be empty");
				}
				if (userkeyfile.exists()) {
					privateKey = new PEMPrivateKey();
					privateKey.read(userkeyfile, userkeypassword);
					privateKey.setKeyntries(keyStore, publicKey.getCertificate(), userkeypassword);
					logger.config("KeyStore.size = " + keyStore.size());
					setChallenging(true);
					_user.setChallenging(true);
					_user.setPassword(null);
				} else {
					logger.fatal("Can't retrieve private key file : " + userkeyfile);
				}
			}
		} catch (final Exception e) {
			logger.exception("Private key error : ", e);
			logger.fatal("Private key error");
		}

		XWTools.deleteDir(getTmpDir());
		setTmpDir();

		currentDispatcher = 0;
		currentDataServer = 0;
		dispatchers = getServerNames(XWPropertyDefs.DISPATCHERS);
		dataServers = getServerNames(XWPropertyDefs.DATASERVERS);
		retrieveDispatchers();
		retrieveDataServers();

		if ((dispatchers == null) || (dispatchers.size() == 0)) {
			if (XWRole.isDispatcher()) {
				dispatchers = new Vector<>();
				dispatchers.add(XWTools.getLocalHostName());
			} else {
				throw new IOException("No XWHEP Server Found");
			}
		}

		if (dataServers == null) {
			logger.debug("no data servers found; using dispatchers as data servers");
			dataServers = dispatchers;
		}

		try {
			UID uid = UID.getMyUid();
			try {
				final String uidStr = getProperty(XWPropertyDefs.UID);
				logger.debug("UID = " + uidStr);
				if ((uidStr != null) && (uidStr.length() > 0)) {
					uid = new UID(uidStr);
				}
			} catch (final IllegalArgumentException e) {
				logger.exception("UID format error ; reseting to " + UID.getMyUid(), e);
				uid = UID.getMyUid();
			}
			if (getBoolean(XWPropertyDefs.FORCENEWUID)) {
				uid = new UID();
			}

			UID.setMyUid(uid);
			setProperty(XWPropertyDefs.UID, uid);
			_host.setUID(uid);
		} catch (final Exception e) {
			logger.fatal("can't set UID ???");
		}

		try {
			final int cpuload = getInt(XWPropertyDefs.CPULOAD);
			if ((cpuload < 0) || (cpuload > 100)) {
				setProperty(XWPropertyDefs.CPULOAD);
			}
			_host.setCpuLoad(getInt(XWPropertyDefs.CPULOAD));
		} catch (final Exception e) {
		}

		if (!isAlone() && firstTime) {
			if (XWRole.isWorker()) {
				throw new IOException("Another instance of the worker is already running");
			} else {
				setProperty(XWPropertyDefs.ALONE);
			}
		}
		if (getProperty(XWPropertyDefs.CACHEDIR) == null) {
			setProperty(XWPropertyDefs.CACHEDIR, getCacheDir().getAbsolutePath());
		}

		try {
			binCachedPath = getCacheDir().getAbsolutePath() + File.separator + "bin";
			XWTools.checkDir(binCachedPath);
		} catch (final Exception e) {
			//setProperty(XWPropertyDefs.TMPDIR, System.getProperty(XWPropertyDefs.JAVATMPDIR));
			setTmpDir();
		}
		try {
			binCachedPath = getCacheDir().getAbsolutePath() + File.separator + "bin";
			XWTools.checkDir(binCachedPath);
		} catch (final Exception e) {
			logger.error("Can't set binCachePath" + e.toString());
		}

		final int alive = getInt(XWPropertyDefs.ALIVEPERIOD);
		setProperty(XWPropertyDefs.ALIVEPERIOD, "" + alive);
		setProperty(XWPropertyDefs.ALIVETIMEOUT, "" + (alive * 3));

		final Version version = Version.currentVersion;

		long timeout = getInt(XWPropertyDefs.TIMEOUT);
		if (timeout < MINTIMEOUT) {
			timeout = MINTIMEOUT;
		}
		setProperty(XWPropertyDefs.TIMEOUT, "" + getInt(XWPropertyDefs.TIMEOUT));

		setProperty(XWPropertyDefs.NOOPTIMEOUT, "" + getInt(XWPropertyDefs.NOOPTIMEOUT));

		nbJobs = 0;

		try {
			final String commLayer = getProperty(XWPropertyDefs.COMMLAYER);
			if (commLayer != null) {
				setProperty(XWPropertyDefs.COMMLAYER, commLayer);
			}
		} catch (final Exception e) {
			logger.exception("Comm layer definition error ", e);
			setProperty(XWPropertyDefs.COMMLAYER, getProperty(XWPropertyDefs.COMMLAYER));
			logger.warn("Comm layer set to '" + getProperty(XWPropertyDefs.COMMLAYER) + "'");
		}

		String classes = getProperty(XWPropertyDefs.MILESTONES);
		if (classes == null) {
			classes = new String();
		}
		new MileStone(XWTools.split(classes));

		String ip;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (final Exception e) {
			try {
				ip = java.net.InetAddress.getByName(LOCALHOST).getHostAddress();
			} catch (final Exception e2) {
				throw new IOException("can't determine IP address: " + e2);
			}
		}

		URI uri = null;
		try {
			final String uriStr = getProperty(XWPropertyDefs.JOBID);
			logger.debug("JOBID = " + uriStr);
			if ((uriStr != null) && (uriStr.length() > 0)) {
				uri = new URI(uriStr);
			}
			_host.setJobId(uri);
		} catch (final Exception e) {
			logger.exception(e);
		}
		try {
			final String uriStr = getProperty(XWPropertyDefs.BATCHID);
			logger.debug("BATCHID = " + uriStr);
			if ((uriStr != null) && (uriStr.length() > 0)) {
				uri = new URI(uriStr);
			}
			_host.setBatchId(uri);
		} catch (final Exception e) {
			logger.exception(e);
		}
		try {
			final String ptxt = getProperty(XWPropertyDefs.INCOMINGCONNECTIONS);
			logger.debug("INCOMINGCONNECTIONS = " + ptxt);
			_host.setIncomingConnections(Boolean.parseBoolean(ptxt));
		} catch (final Exception e) {
			logger.exception(e);
		}

		_host.setNatedIPAddr(ip);
		_host.setNatedIPAddr(ip);
		_host.setIPAddr(ip);
		_host.setHWAddr("");
		_host.setTimeZone(TimeZone.getDefault().getID());
		_host.setProject(getProperty(XWPropertyDefs.PROJECT));
		_host.setTimeOut(getInt(XWPropertyDefs.TIMEOUT));
		_host.setVersion(version.full());
		_host.setSgId(getProperty(XWPropertyDefs.SGID));

		_host.setCpuNb(1);
		_host.setCpuSpeed(0);
		_host.setCpuModel("unknown");
		_host.setTotalMem(0);
		_host.setTotalSwap(0);
		_host.setOs(OSEnum.getOs());

		_host.setCpu(CPUEnum.getCpu());
		_host.setCpuNb(Runtime.getRuntime().availableProcessors());
		_host.setAvailable(false);
		_host.setPilotJob(false);

		if (!XWRole.isWorker()) {
			return;
		}
		_host.setAcceptBin(getBoolean(XWPropertyDefs.ACCEPTBIN));
		if (ArchDepFactory.xwutil() != null) {
			try {
				_host.setCpuSpeed(ArchDepFactory.xwutil().getSpeedProc());
			} catch (final UnsatisfiedLinkError e) {
			}
			try {
				_host.setCpuModel(ArchDepFactory.xwutil().getProcModel());
			} catch (final UnsatisfiedLinkError e) {
			}
			try {
				_host.setTotalMem(ArchDepFactory.xwutil().getTotalMem());
			} catch (final UnsatisfiedLinkError e) {
			}
			final File f = new File(".");
			try {
				_host.setTotalTmp(f.getTotalSpace() / XWTools.ONEMEGABYTES);
			} catch (final UnsatisfiedLinkError e) {
			}
			try {
				_host.setFreeTmp(f.getFreeSpace() / XWTools.ONEMEGABYTES);
			} catch (final UnsatisfiedLinkError e) {
			}
			try {
				_host.setTotalSwap(ArchDepFactory.xwutil().getTotalSwap());
			} catch (final UnsatisfiedLinkError e) {
			}
		} else {
			logger.fatal("can't load xwutil library");
		}

		final String localAppsProperty = getProperty(XWPropertyDefs.SHAREDAPPS);
		final StringBuilder localAppNames = new StringBuilder();

		logger.debug("localAppsProperty = " + localAppsProperty);

		if (localAppsProperty != null) {

			localApps = XWTools.split(localAppsProperty.toUpperCase(), ",");

			if (localApps != null) {

				final Iterator<String> enumapps = localApps.iterator();

				while (enumapps.hasNext()) {

					final String apptype = enumapps.next();
					try {
						final AppTypeEnum at = AppTypeEnum.valueOf(apptype);
						if (at.available()) {
							localAppNames.append(apptype + " ");
						}
					} catch (final Exception e) {
						logger.exception("Invalid application type : " + apptype, e);
						enumapps.remove();
					}
				}
			}
			_host.setSharedApps(localAppNames.toString().trim().replace(' ', ','));
		}

		final String localPkgsProperty = getProperty(XWPropertyDefs.SHAREDPACKAGES);
		if (localPkgsProperty != null) {
			_host.setSharedPackages(localPkgsProperty.trim());
		}
		final String localDatasProperty = getProperty(XWPropertyDefs.SHAREDDATAS);
		if (localDatasProperty != null) {
			_host.setSharedDatas(localDatasProperty.trim());
		}
		final String localDatasPathProperty = getProperty(XWPropertyDefs.SHAREDDATASPATH);
		if ((localDatasProperty != null) && (localDatasPathProperty != null)) {
			setDataPackagesDir(localDatasProperty, localDatasProperty);
		}

		File sandboxBinFile = null;
		try {
			//
			// since 12.1.0 we can use AppTypeEnum.DOCKER as sandbox
			// This has the advantage that AppTypEnum knows where is docker tool binary, for each OS
			//
			final String sandboxAttr = Worker.getConfig().getProperty(XWPropertyDefs.SANDBOXPATH).trim().toUpperCase();
			final AppTypeEnum appTypeEnum = AppTypeEnum.valueOf(sandboxAttr);
			sandboxBinFile = appTypeEnum.getPath();
			logger.debug("sandboxBinFile = " + sandboxBinFile);
		} catch(final Exception e) {
		}
		if (sandboxBinFile == null) {
			try {
				//
				// since 12.1.0 we can still define the full sandbox path
				//
				sandboxBinFile = new File(Worker.getConfig().getProperty(XWPropertyDefs.SANDBOXPATH).trim());
				logger.debug("sandboxBinFile = " + sandboxBinFile);
			} catch(final Exception e) {
			}
		}
		if ((sandboxBinFile != null) && !sandboxBinFile.exists()) {
			logger.fatal("Sandboxing not found : \"" + getProperty(XWPropertyDefs.SANDBOXPATH) + "\"");
		}

		_host.setTracing(getBoolean(XWPropertyDefs.TRACES));

		loadLibrary(XWRole.isWorker());

		if (getProperty(XWPropertyDefs.PILOTJOB) != null) {
			_host.setPilotJob(getBoolean(XWPropertyDefs.PILOTJOB));
		}
		/**
		 * Configuration of the activation
		 */
		if (loadLibraries) {
			String act = getProperty(XWPropertyDefs.ACTIVATORCLASS);
			if (act == null) {
				act = getProperty(XWPropertyDefs.ACTIVATORCLASS);
				_host.setAvailable(true);
			}
			setProperty(XWPropertyDefs.ACTIVATORCLASS, act);
			final String actDate = getProperty(XWPropertyDefs.ACTIVATIONDATE);
			if (actDate == null) {
				setProperty(XWPropertyDefs.ACTIVATIONDATE, "* 19-7");
			}
			logger.config(XWPropertyDefs.ACTIVATIONDATE.toString() + "= " + getProperty(XWPropertyDefs.ACTIVATIONDATE));

			if (act.compareTo("xtremweb.worker.CpuActivator") == 0) {
				if (OSEnum.getOs().isWin32()) {
					act = new String("xtremweb.worker.WinSaverActivator");
				} else if (OSEnum.getOs().isMacosx()) {
					act = new String("xtremweb.worker.DateActivator");
				} else {
					act = new String("xtremweb.worker.AlwaysActive");
				}
				logger.warn("xtremweb.worker.CpuActivator is not avaible;" + " sorry for inconveniences");
			}
		}

		useNotify = getBoolean(XWPropertyDefs.SYSTRAY);
		if (useNotify) {

			final File iconFile = new File("xw.ico");
			try {
				extractResource("misc/" + iconFile, iconFile);
			} catch (final Exception e) {
				try {
					extractResource("misc/" + iconFile, iconFile);
				} catch (final Exception e2) {
					logger.exception("can't extract resource " + iconFile, e2);
					useNotify = false;
				}
			}

			final String iconPath = binCachedPath + File.separator + iconFile;
			TrayIcon trayIcon = null;
			try {
				if (SystemTray.isSupported()) {
					final SystemTray tray = SystemTray.getSystemTray();
					final Image image = Toolkit.getDefaultToolkit().getImage(iconPath);
					final ActionListener listener = new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent ae) {
							final JMenuItem src = (JMenuItem) ae.getSource();
						}
					};
					final PopupMenu popup = new PopupMenu();
					final MenuItem defaultItem = new MenuItem("Exit");
					defaultItem.addActionListener(listener);
					popup.add(defaultItem);
					trayIcon = new TrayIcon(image, "XtremWeb-HEP " + XWRole.getMyRole(), popup);
					trayIcon.addActionListener(listener);
					try {
						tray.add(trayIcon);
					} catch (final AWTException e) {
						logger.exception(e);
					}
					// ...
				} else {
					logger.info("SystemTray not supported");
				}
			} catch (final Throwable e) {
				logger.warn("SystemTray initialization error : " + e);
			}

		} else {
			logger.info("SystemTray not enabled");
		}
		workPoolSize = getInt(XWPropertyDefs.WORKPOOLSIZE);
		if (workPoolSize < 1) {
			workPoolSize = _host.getCpuNb();
		}
		if (workPoolSize > _host.getCpuNb()) {
			logger.warn("workPoolSize > nb cpu");
		}
		setProperty(XWPropertyDefs.WORKPOOLSIZE, "" + workPoolSize);
		_host.setPoolWorkSize(workPoolSize);
	}

	/**
	 * This retrieve the communication client for the default scheme
	 *
	 * @return the default communication client
	 * @exception IOException
	 *                is thrown if cache directory can not be created
	 * @throws InstantiationException
	 *             if the class referred by the scheme can not be instantiated
	 */
	public CommClient defaultCommClient() throws IOException, InstantiationException {

		CommClient.setConfig(this);
		final CommClient ret = CommClient.getClient(Connection.xwScheme());
		return ret;
	}

	/**
	 * This retrieve the communication client for the default scheme
	 *
	 * @return the default communication client
	 * @exception IOException
	 *                is thrown if cache directory can not be created
	 * @throws InstantiationException
	 *             if the class referred by the scheme can not be instantiated
	 */
	public CommClient getCommClient(final URI uri) throws IOException, InstantiationException {

		CommClient.setConfig(this);
		final CommClient ret = CommClient.getClient(uri);
		return ret;
	}

	/**
	 * This calls extractResource(resName, new File(fileName))
	 *
	 * @see #extractResource(String, File)
	 */
	public File extractResource(final String resName, final String fileName) throws IOException {
		return extractResource(resName, new File(fileName));
	}

	/**
	 * This extracts a resource from JAR file
	 *
	 * @param resName
	 *            is the resource name to extract
	 * @param file
	 *            is the file to store extracted resource
	 * @return the file where the resources is stored on local file system
	 */
	public File extractResource(final String resName, final File file) throws IOException {

		final InputStream ls = getClass().getClassLoader().getResourceAsStream(resName);

		if (ls == null) {
			throw new IOException(resName + " not found");
		}

		final byte[] buf = new byte[1024];
		final File ret = new File(binCachedPath, file.getName());
		final FileOutputStream lf = new FileOutputStream(ret);
		for (int n = ls.read(buf); n > 0; n = ls.read(buf)) {
			lf.write(buf, 0, n);
		}
		ls.close();
		lf.close();
		logger.debug("extractResource " + resName + " saved to " + ret.getAbsolutePath());
		return ret;
	}

	/**
	 * This set the default temp dir
	 *
	 * @since 7.0.0
	 */
	public File setTmpDir() throws IOException {
		final String dirname = "XW." + XWRole.getMyRole().toString() + "."
				+ UID.getMyUid().toString();
//		System.out.println("setTmpDir : getProperty(XWPropertyDefs.TMPDIR) = " + getProperty(XWPropertyDefs.TMPDIR));  
		final String parent = getProperty(XWPropertyDefs.TMPDIR) == null ? System.getProperty(XWPropertyDefs.JAVATMPDIR) : getProperty(XWPropertyDefs.TMPDIR);  
		final String p = parent == null ? "/tmp" : parent; 
		final File dir = p.endsWith(dirname) ? new File(p) : new File(p, dirname);

		XWTools.checkDir(dir);
		_host.setTotalTmp(dir.getTotalSpace() / XWTools.ONEMEGABYTES);
		_host.setFreeTmp(dir.getFreeSpace() / XWTools.ONEMEGABYTES);
		dir.deleteOnExit();
		this.defaults.put(XWPropertyDefs.TMPDIR, dir.getAbsolutePath());
//		System.out.println("setTmpDir : dir.getAbsolutePath() = " + dir.getAbsolutePath());  
//		setProperty(XWPropertyDefs.TMPDIR, dir.getAbsolutePath());
		return dir;
	}

	/**
	 * This retrieves the default temp dir
	 * @throws IOException 
	 */
	public File getTmpDir() throws IOException {
		return setTmpDir();
	}

	/**
	 * This sets and eventually creates a package directory for the given
	 * package name
	 *
	 * @param pkgNames
	 *            contains a comma separated data package names
	 * @param pkgPaths
	 *            contains a comma separated data package paths
	 * @throws IOException
	 * @since 10.0.0
	 */
	public void setDataPackagesDir(final String pkgNames, final String pkgPaths) throws IOException {
		final Collection<String> names = XWTools.split(pkgNames, ",");
		final Collection<String> paths = XWTools.split(pkgPaths, ",");
		if ((names == null) || (paths == null)) {
			return;
		}
		final Iterator<String> namesIterator = names.iterator();
		final Iterator<String> pathsIterator = paths.iterator();
		for (; namesIterator.hasNext();) {
			final String name = namesIterator.next();
			final String path = pathsIterator.next();
			if ((name == null) || (path == null)) {
				continue;
			}
			setDataPackageDir(name, path);
		}
	}

	/**
	 * This sets and eventually creates a package directory for the given
	 * package name
	 *
	 * @param pkgName
	 *            is the package name
	 * @param path
	 *            is the package path
	 * @throws IOException
	 * @since 10.0.0
	 */
	public void setDataPackageDir(final String pkgName, final String path) throws IOException {
		final File fdir = ((path == null) || (path.length() < 1)) ? null : new File(path);
		setDataPackageDir(pkgName, fdir);
	}

	/**
	 * This sets and eventually creates a package directory for the given
	 * package name To avoid property conflict, the name of the package is
	 * automatically prefixed by XWTools#PACKAGENAMEHEADER
	 *
	 * @param pkgName
	 *            is the package name
	 * @param dir
	 *            is the package directory
	 * @throws IOException
	 * @since 10.0.0
	 * @see XWTools#PACKAGENAMEHEADER
	 */
	public void setDataPackageDir(final String pkgName, final File dir) throws IOException {
		if (dir != null) {
			XWTools.checkDir(dir);
			dir.deleteOnExit();
		}
		logger.debug("setDataPackageDir : " + pkgName + ", " + (dir == null ? "" : dir.getAbsolutePath()));
		setProperty(XWTools.PACKAGENAMEHEADER + pkgName, (dir == null ? "" : dir.getAbsolutePath()));
	}

	/**
	 * This resets and eventually creates a package directory To avoid property
	 * conflict, the name of the package is automatically prefixed by
	 * XWTools#PACKAGENAMEHEADER
	 *
	 * @param pkgName
	 *            is the package name
	 * @throws IOException
	 * @since 10.0.0
	 * @see XWTools#PACKAGENAMEHEADER
	 */
	public void resetDataPackageDir(final String pkgName) throws IOException {
		setProperty(XWTools.PACKAGENAMEHEADER + pkgName, "");
	}

	/**
	 * This retrieves a package directory, if any
	 *
	 * @param pkgName
	 *            is the package name
	 * @throws IOException
	 * @since 10.0.0
	 * @return the package directory if set; the default tmp dir otherwise
	 */
	public File getDataPackageDir(final String pkgName) throws IOException {
		final String path = getProperty(XWTools.PACKAGENAMEHEADER + pkgName);
		if ((path == null) || (path.length() < 1)) {
			logger.debug("getDataPackageDir : return tmpdir " + getTmpDir());
			return getTmpDir();
		}
		logger.debug("getDataPackageDir (" + pkgName + ") : " + path);
		return new File(path);
	}

	/**
	 * This retrieves the default cache dir; it is created if necessary
	 *
	 * @exception IOException
	 *                is thrown if cache directory can not be created
	 */
	public File getCacheDir() throws IOException {

		final String c = getTmpDir().getAbsolutePath() + ".cache";
		final File d = new File(c);
		XWTools.checkDir(d);
		return d;
	}

	/**
	 * This retrieves the default works dir; it is created if necessary
	 * @throws IOException 
	 */
	public File getWorksDir() throws IOException {
		final File d = new File(getTmpDir(), "works");
		d.deleteOnExit();
		return d;
	}

	/**
	 * This calls getProperty(prop.toString())
	 *
	 * @param prop
	 *            - the property to retrieve the value for
	 */
	public String getProperty(final XWPropertyDefs prop) {
		if (prop == null) {
			return null;
		}
		final String p = getProperty(prop.toString());
		if (p != null) {
			return p.trim();
		}
		return null;
	}

	/**
	 * This sets a property to its default value
	 *
	 * @param prop
	 *            - the property to set
	 */
	public void setProperty(final XWPropertyDefs prop) {
		final String p = prop.defaultValue();
		if (p != null ) {
			setProperty(prop.toString(), p);
		}
	}

	/**
	 * This set a property to the given value. If uid is null this sets this
	 * property to empty string.
	 *
	 * @param prop
	 *            - the property to set
	 * @param uid
	 *            - the value to set the property to
	 */
	public void setProperty(final XWPropertyDefs prop, final UID uid) {
		setProperty(prop.toString(), (uid != null ? uid.toString() : ""));
	}

	/**
	 * This set a property to the given value. If value is null, this sets the
	 * property to its default value. If default value is null, this sets this
	 * property to empty string.
	 *
	 * @param prop
	 *            - the property to set
	 * @param val
	 *            - the value to set the property to
	 */
	public void setProperty(final XWPropertyDefs prop, final String val) {
		setProperty(prop, val, true);
	}

	/**
	 * This set a property to the given value. If value is null, this sets the
	 * property to its default value. If default value is null, this sets this
	 * property to empty string.
	 *
	 * @param prop
	 *            is the property to set
	 * @param val
	 *            is the value to set the property to
	 * @param sys
	 *            tells to set System property
	 */
	public void setProperty(final XWPropertyDefs prop, final String val, final boolean sys) {
		String v = val;
		if (v == null) {
			v = prop.defaultValue();
		}
		if (v == null) {
			v = "";
		}
		setProperty(prop.toString(), v.trim());
		if (sys == true) {
			System.setProperty(prop.toString(), getProperty(prop));
		}
		logger.config("setProperty(" + prop.toString() + ", " + v + ") = " + getProperty(prop));
	}
	/**
	 *
	 * @since 7.3.0
	 */
	public XWRole getRole() {
		final String srole = getProperty(XWPropertyDefs.ROLE).toUpperCase();
		XWRole ret = XWRole.getMyRole();
		try {
			ret = XWRole.valueOf(srole);
		} catch (final Exception e) {
			logger.exception(e);
		}
		return ret;
	}

	/**
	 * This retrieves a port value
	 *
	 * @param port
	 * @return the port value as defined by a property; default port value if
	 *         property not set
	 * @see xtremweb.communications.Connection#defaultPortValue()
	 */
	public int getPort(final Connection port) {
		final String s = getProperty(port.toString());
		final int defaultp = port.defaultPortValue();

		if (s == null) {
			return defaultp;
		}
		try {
			final int p = Integer.parseInt(s);
			if (!((p > 0) && (p < (1 << 15)))) {
				logger.fatal("Invalid port number (" + port + ") = " + s);
			}
			return p;
		} catch (final NumberFormatException e) {
			logger.fatal("Invalid port number (" + port + ") = " + s);
		}
		return -1;
	}

	/**
	 * This retrieves a key as integer
	 *
	 * @param key
	 *            is the key to retrieve
	 * @return the integer value of the key as defined by property; default
	 *         value if property not set
	 * @see XWPropertyDefs#defaultValue()
	 */
	public int getInt(final XWPropertyDefs key) {
		final String defval = key.defaultValue();
		final String s = getProperty(key) == null ? defval : getProperty(key); 
		try {
			return Integer.parseInt(s);
		} catch (final NumberFormatException e) {
			logger.warn("Invalid int number for property " + key + " (" + s + ") returning -1");
			return -1;
		}
	}

	/**
	 * This retrieves a key as long
	 *
	 * @param key
	 *            is the key to retrieve
	 * @return the long value of the key as defined by property; default value
	 *         if property not set
	 * @see XWPropertyDefs#defaultValue()
	 * @since 8.2.0
	 */
	public long getLong(final XWPropertyDefs key) {
		final String defval = key.defaultValue();
		final String s = getProperty(key) == null ? defval : getProperty(key); 
		try {
			return Long.parseLong(s);
		} catch (final NumberFormatException e) {
			logger.warn("Invalid int number for property " + key + " (" + s + ") returning -1");
			return -1;
		}
	}

	/**
	 * This retrieves a key as a boolean.
	 *
	 * @param key
	 *            contains the property name
	 * @return the boolean value of the key
	 */
	public boolean getBoolean(final XWPropertyDefs key) {
		final String value = getProperty(key);
		final boolean defaultb = Boolean.parseBoolean(key.defaultValue());

		if (value == null) {
			return defaultb;
		}

		Boolean ret = null;
		try {
			ret = new Boolean(value);
		} catch (final Exception e) {
			logger.warn("not a boolean : " + key + "=" + value + " set to default (" + defaultb + ")");
			ret = new Boolean(defaultb);
		}
		return ret.booleanValue();
	}

	/**
	 * This retrieves the file represented by the given property.
	 *
	 * @param key
	 *            contains the property name
	 * @return null if the key value is null or empty
	 */
	public File getFile(final XWPropertyDefs key) throws IOException {
		final String s = getProperty(key);

		if ((s == null) || (s.length() == 0)) {
			return null;
		}
		return new File(s).getCanonicalFile();
	}

	/**
	 * This retrieves the absolute path represented by the given property.
	 *
	 * @param key
	 *            contains the property name
	 * @return null if the key value is null or empty
	 */
	public String getPath(final XWPropertyDefs key) throws IOException {
		final File f = getFile(key);
		if (f != null) {
			return f.getAbsolutePath();
		}
		return null;
	}

	/**
	 * Check whether another worker is already running
	 *
	 * @return false if an other worker with the same parameters is running
	 */
	public boolean isAlone() throws IOException {
		final File dir = getTmpDir();

		if (!dir.exists()) {
			XWTools.checkDir(dir);
			return true;
		}

		if ((!loadLibraries) || getBoolean(XWPropertyDefs.INSTANCES)) {
			return true;
		}
		final File pid = new File(dir, "pid");
		if (pid.exists()) {
			final BufferedReader pidf = new BufferedReader(new FileReader(pid));
			try {
				final String line = pidf.readLine();
				if (line != null) {
					final int i = Integer.parseInt(line);
					if (i == ArchDepFactory.xwutil().getPid()) {
						logger.info("config file reload or update");
					} else if (ArchDepFactory.xwutil().isRunning(i)) {
						return false;
					}
				}
			} catch (final NumberFormatException e) {
				logger.warn("ignoring corrupted pid file : " + pid);
			} finally {
				pidf.close();
			}
			pid.delete();
		}
		pid.createNewFile();
		final PrintWriter pidf = new PrintWriter(new FileOutputStream(pid));
		pidf.println(Integer.toString(ArchDepFactory.xwutil().getPid()));
		pidf.close();
		return true;
	}

	/**
	 * This returns the server list
	 *
	 * @return a Collection of known dispatchers
	 * @since v1r2-rc1(RPC-V)
	 */
	public Collection<String> getDispatchers() {
		return dispatchers;
	}

	/**
	 * This returns the data server list
	 *
	 * @return a Collection of known data servers
	 * @since XWHEP 1.0.0
	 */
	public Collection<String> getDataServers() {
		return dataServers;
	}

	/**
	 * This retrieves the current dispatcher
	 *
	 * @return a String containing current server name
	 * @since v1r2-rc1(RPC-V)
	 */
	public String getCurrentDispatcher() {
		return ((Vector<String>) dispatchers).elementAt(currentDispatcher);
	}

	/**
	 * This retrieves the current data server
	 *
	 * @return a String containing current server name
	 * @since v1r2-rc1(RPC-V)
	 */
	public String getCurrentDataServer() {
		return ((Vector<String>) dataServers).elementAt(currentDataServer);
	}

	/**
	 * This determines the current dispatcher
	 *
	 * @param srv
	 *            is a String containing server name to make current one
	 * @return an integer containing current server index; -1 if the server is
	 *         not known
	 * @since v1r2-rc1(RPC-V)
	 */
	public synchronized int setCurrentDispatcher(final String srv) throws IOException {
		final String hn = XWTools.getHostName(srv);
		currentDispatcher = ((Vector<String>) dispatchers).indexOf(hn);
		if (currentDispatcher == -1) {
			logger.warn("Can't change current server to : " + hn);
		}
		notifyAll();
		return currentDispatcher;
	}

	/**
	 * This determines the current data server
	 *
	 * @param srv
	 *            is a String containing server name to make current one
	 * @return an integer containing current server index; -1 if the server is
	 *         not known
	 * @since XWHEP 1.0.0
	 */
	public synchronized int setCurrentDataServer(final String srv) throws IOException {
		final String hn = XWTools.getHostName(srv);
		currentDataServer = ((Vector<String>) dataServers).indexOf(hn);
		if (currentDataServer == -1) {
			logger.warn("Can't change current server to : " + hn);
		}
		notifyAll();
		return currentDataServer;
	}

	/**
	 * This returns the next available server name
	 *
	 * @return a String containing the next available server name
	 * @since v1r2-rc1(RPC-V)
	 */
	public String getNextDispatcher() {
		nextDispatcher();
		return ((Vector<String>) dispatchers).elementAt(currentDispatcher);
	}

	/**
	 * This increments the dispatcher
	 *
	 * @since v1r2-rc1(RPC-V)
	 */
	private synchronized void nextDispatcher() {
		currentDispatcher = (currentDispatcher + 1) % dispatchers.size();
		notifyAll();
	}

	/**
	 * This returns the next available dispatcher
	 *
	 * @return a String containing the next available dispatcher
	 * @since v1r2-rc1(RPC-V)
	 */
	public String getNextDataServer() {
		nextDataServer();
		return ((Vector<String>) dataServers).elementAt(currentDataServer);
	}

	/**
	 * This increments the data server
	 *
	 * @since XWHEP 1.0.0
	 */
	private synchronized void nextDataServer() {
		currentDataServer = (currentDataServer + 1) % dataServers.size();
		notifyAll();
	}

	/**
	 * This adds a server to server list, if not already present
	 *
	 * @param srv
	 *            is a String containing a new server name
	 * @return true if new server inserted, false if server name already known
	 * @since XWHEP 1.0.0
	 */
	public synchronized boolean addServer(final Collection<String> v, final String srv) throws IOException {

		boolean ret = false;
		if ((v == null) || (srv == null)) {
			return false;
		}
		final String hn = XWTools.getHostName(srv);
		if (!v.contains(srv)) {
			v.add(hn);
			ret = true;
		}

		notifyAll();
		return ret;
	}

	/**
	 * This adds a server to server list, if not already present
	 *
	 * @param srv
	 *            is a String containing a new server name
	 * @return true if new server inserted, false if server name already known
	 * @since XWHEP 1.0.0
	 */
	public boolean addDispatcher(final String srv) throws IOException {
		return addServer(dispatchers, srv);
	}

	/**
	 * This adds a server to server list, if not already present
	 *
	 * @param srv
	 *            is a String containing a new server name
	 * @return true if new server inserted, false if server name already known
	 * @since XWHEP 1.0.0
	 */
	public boolean addDataServer(final String srv) throws IOException {
		return addServer(dataServers, srv);
	}

	/**
	 * This adds some server to list, if not already present
	 *
	 * @param srv
	 *            is a String array containing new server names
	 * @return true if at least one new server inserted, false otherwise
	 * @since v1r2-rc1(RPC-V)
	 */
	private boolean addServers(final Collection<String> v, final Collection<String> srv) throws IOException {

		if (srv == null) {
			return false;
		}

		boolean ret = false;

		for (final Iterator<String> iter = srv.iterator(); iter.hasNext();) {
			final String server = iter.next();
			logger.debug("server " + server + " added");
			if (addServer(v, server)) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * This adds some dispatchers to dispatchers list, if not already present
	 *
	 * @param srv
	 *            is a String array containing new server names
	 * @return true if at least one new server inserted, false otherwise
	 * @since v1r2-rc1(RPC-V)
	 */
	public boolean addDispatchers(final Collection<String> srv) throws IOException {
		return addServers(dispatchers, srv);
	}

	/**
	 * This adds some data servers to list, if not already present
	 *
	 * @param srv
	 *            is a String array containing new server names
	 * @return true if at least one new server inserted, false otherwise
	 * @since XWHEP 1.0.0
	 */
	public boolean addDataServers(final Collection<String> srv) throws IOException {
		return addServers(dataServers, srv);
	}

	/**
	 * This print dispatchers to a string
	 *
	 * @return a String containing server names in(tag:name) pair
	 * @since v1r2-rc1(RPC-V)
	 */
	protected synchronized String serversToString(final Collection<String> servers) {

		String ret = new String("");

		if (servers == null) {
			return ret;
		}
		for (final Iterator<String> iter = servers.iterator(); iter.hasNext();) {
			final String s = iter.next();
			ret = ret.concat(s + " ");
		}

		notifyAll();
		return ret;
	}

	/**
	 * This print dispatchers to a string
	 *
	 * @return a String containing server names in(tag:name) pair
	 * @since v1r2-rc1(RPC-V)
	 */
	public String dispatchersToString() {

		return "" + XWPropertyDefs.DISPATCHERS.toString() + "=" + serversToString(dispatchers);
	}

	/**
	 * This print dispatchers to a string
	 *
	 * @return a String containing server names in(tag:name) pair
	 * @since v1r2-rc1(RPC-V)
	 */
	public String dataServersToString() {

		return "" + XWPropertyDefs.DISPATCHERS.toString() + "=" + serversToString(dataServers);
	}

	/**
	 * This retrieves a set of servers.
	 *
	 * @since XWHEP 1.0.0
	 */
	private Collection<String> getServerNames(final XWPropertyDefs property) {

		final Collection<String> ret = XWTools.split(getProperty(property));
		if (ret == null) {
			return null;
		}
		for (final Iterator<String> iter = ret.iterator(); iter.hasNext();) {
			final String srv = iter.next();
			try {
				final String srv_hn = XWTools.getHostName(srv);
				ret.remove(srv);
				ret.add(srv_hn);
			} catch (final IOException e) {
				logger.exception(e);
			}
		}
		return ret;
	}

	/**
	 * This determines dispatchers file
	 * @throws IOException 
	 *
	 * @since v1r2-rc1(RPC-V)
	 */
	private synchronized File dispatchersFile() throws IOException {
		return new File(getTmpDir() + "XW." + "known_dispatchers");
	}

	/**
	 * This determines dispatchers file
	 * @throws IOException 
	 *
	 * @since XWHEP 1.0.0
	 */
	private synchronized File dataServersFile() throws IOException {
		return new File(getTmpDir() + "XW." + "known_dataservers");
	}

	/**
	 * This writes server list to disk
	 *
	 * @since XWHEP 1.0.0
	 */
	private synchronized void saveServers(final File server, final String servers) {

		if (server.exists()) {
			server.delete();
		}
		try (final FileWriter fw = new FileWriter(server)) {
			fw.write(servers);
			fw.flush();
		} catch (final Exception e) {
			logger.exception(e);
		}
		notifyAll();
	}

	/**
	 * This writes dispatcher list to disk
	 *
	 * @since v1r2-rc1(RPC-V)
	 */
	public void saveDispatchers() {
		try {
			saveServers(dispatchersFile(), dispatchersToString());
		} catch (IOException e) {
			logger.exception(e);
		}
	}

	/**
	 * This writes dispatcher list to disk
	 *
	 * @since XWHEP 1.0.0
	 */
	public void saveDataServers() {
		try {
			saveServers(dataServersFile(), dataServersToString());
		} catch (IOException e) {
			logger.exception(e);
		}
	}

	/**
	 * This read server list from disk
	 *
	 * @since XWHEP 1.0.0
	 */
	private void retrieveServers(final Collection<String> v, final File file) {

		try (final FileReader fr = new FileReader(file)) {
			final BufferedReader bufferFile = new BufferedReader(fr);
			final String l = bufferFile.readLine();

			final Collection<String> newSrv = XWTools.split(l.substring(l.indexOf(new String("=")) + 1).trim());
			bufferFile.close();
			if (newSrv == null) {
				return;
			}
			addServers(v, newSrv);
		} catch (final Exception e) {
		}
	}

	/**
	 * This read dispatcher list from disk
	 *
	 * @since XWHEP 1.0.0
	 */
	public void retrieveDispatchers() {
		try {
			retrieveServers(dispatchers, dispatchersFile());
		} catch (IOException e) {
			logger.exception(e);
		}
	}

	/**
	 * This read data server list from disk
	 *
	 * @since XWHEP 1.0.0
	 */
	public void retrieveDataServers() {
		try {
			retrieveServers(dataServers, dataServersFile());
		} catch (IOException e) {
			logger.exception(e);
		}
	}

	/**
	 * This calls store(header)
	 *
	 * @see #store(String)
	 */
	public void store() {
		store("# XWHEP configuration file\n" + "# XWHEP version : " + Version.currentVersion + "\n"
				+ "# Saved on " + new Date());
	}

	/**
	 * This calls store(header, configFile)
	 *
	 * @param header
	 *            is written at the beginning to the file
	 * @see #store(String, File)
	 */
	public void store(final String header) {
		store(header, configFile);
	}

	/**
	 * This writes properties. This insert a header containing the storage date
	 *
	 * @param header
	 *            is written at the beginning to the file
	 */
	public void store(final String header, final File out) {
		if (getBoolean(XWPropertyDefs.FORCENEWUID)) {
			logger.config("Don't write config ; FORCENEWUID == " + getBoolean(XWPropertyDefs.FORCENEWUID));
			return;
		}
		try (final FileOutputStream o = new FileOutputStream(out)){
			store(o, header);
		} catch (final Exception e) {
			logger.exception("Can't store config", e);
		}
	}

	/**
	 * This checks directory trees; this aims to switch between users
	 *
	 * @see xtremweb.client.gui.MainFrame#processLoginAs()
	 */
	public void check() {
		try {
			XWTools.checkDir(getCacheDir());
			XWTools.checkDir(getTmpDir());
			binCachedPath = getCacheDir().getAbsolutePath() + File.separator + "bin";
			XWTools.checkDir(binCachedPath);
		} catch (final Exception e) {
			logger.exception(e);
		}
	}

	/**
	 * This erases cache
	 */
	public void clean() {
		try {
			XWTools.deleteDir(getCacheDir());
			XWTools.deleteDir(getTmpDir());

			XWTools.checkDir(getCacheDir());
			XWTools.checkDir(getTmpDir());
			binCachedPath = getCacheDir().getAbsolutePath() + File.separator + "bin";
			XWTools.checkDir(binCachedPath);
		} catch (final Exception e) {
			logger.exception(e);
		}
	}

	/**
	 * This writes properties
	 */
	public void dump(final PrintStream out, final String header) {
		if (header != null) {
			out.println(header + " " + new java.util.Date().toString());
		}
		out.println("Name             : " + XWTools.getLocalHostName());
		out.println("Version          : " + Version.currentVersion);
		out.println("Identity         : " + _user.getLogin());
		out.println("XWRole           : " + XWRole.getMyRole().toString());
		out.println("Started on       : " + upTime());
		if (XWRole.isDispatcher()) {
			out.println("Scheduler        : " + getProperty(XWPropertyDefs.SCHEDULERCLASS));
		}
		if (XWRole.isWorker()) {
			out.println("Project          : " + getProperty(XWPropertyDefs.PROJECT));
		}
		out.println("SmartSockets hub : " + getProperty(XWPropertyDefs.SMARTSOCKETSHUBADDR));
		out.println("Server HTTP      : " + getProperty(XWPropertyDefs.STARTSERVERHTTP));
		out.println("Alive            : " + getProperty(XWPropertyDefs.ALIVEPERIOD));
		out.println("Alive time out   : " + getProperty(XWPropertyDefs.ALIVETIMEOUT));
		if (XWRole.isWorker()) {
			out.println("IncomingConnections : " + getProperty(XWPropertyDefs.INCOMINGCONNECTIONS));
			out.println("Activator        : " + getProperty(XWPropertyDefs.ACTIVATORCLASS));
			out.println("Activation       : " + getProperty(XWPropertyDefs.ACTIVATIONDATE));
			out.println("workPool size    : " + workPoolSize);
			out.println("Noop Time out    : " + getProperty(XWPropertyDefs.NOOPTIMEOUT));
			out.println("Max jobs         : " + getProperty(XWPropertyDefs.COMPUTINGJOBS));
			out.println("Runtime.exec()   : " + getProperty(XWPropertyDefs.JAVARUNTIME));
			out.println("Multiple instances : " + getProperty(XWPropertyDefs.INSTANCES));
		}
		out.println("Polling time out : " + Long.parseLong(getProperty(XWPropertyDefs.TIMEOUT)));
		out.println("Optimize zip     : " + getBoolean(XWPropertyDefs.OPTIMIZEZIP));
		out.println("Optimize network : " + getBoolean(XWPropertyDefs.OPTIMIZENETWORK));

		out.println("NIO              : " + getBoolean(XWPropertyDefs.JAVANIO));
		out.println("Comm layer       : " + getProperty(XWPropertyDefs.COMMLAYER));
		out.println("Socket time out  : " + getInt(XWPropertyDefs.SOTIMEOUT));
		out.println("Socket retries   : " + getInt(XWPropertyDefs.SORETRIES));
		out.println("Max connections  : " + getInt(XWPropertyDefs.MAXCONNECTIONS));
		out.println("TCP port         : " + getPort(Connection.TCPPORT));
		out.println("UDP port         : " + getPort(Connection.UDPPORT));
		if (XWRole.isWorker()) {
			out.println("HTPP worker port : " + getPort(Connection.HTTPWORKERPORT));
		}
		out.println("HTPP  port       : " + getPort(Connection.HTTPPORT));
		out.println("HTPPS port       : " + getPort(Connection.HTTPSPORT));
		out.println("Sun RPC interposition port : " + getPort(Connection.SUNRPCPORT));
		out.println("Sandbox path : " + getProperty(XWPropertyDefs.SANDBOXPATH));
		out.println("Sandbox launch args : " + getProperty(XWPropertyDefs.SANDBOXSTARTARGS));
		out.println("Blockchain service  : " + getBoolean(BLOCKCHAINETHENABLED));

		if (blockchainEnabled()
				&& (IexecConfigurationService.getInstance() != null)
				&& (IexecConfigurationService.getInstance().getCommonConfiguration() != null)) {

			_host.setEthWalletAddr(CredentialsService.getInstance().getCredentials().getAddress());
			final CommonConfiguration commonConfiguration = IexecConfigurationService.getInstance().getCommonConfiguration();
			out.println("Wallet     addr     : " + _host.getEthWalletAddr());
			out.println("Eth client addr     : " + commonConfiguration.getNodeConfig().getClientAddress());
			out.println("iExec Hub  addr     : " + commonConfiguration.getContractConfig().getIexecHubAddress());
			out.println("iExec RLC  addr     : " + commonConfiguration.getContractConfig().getRlcAddress());
			WorkerPoolConfig workerPoolConfig = commonConfiguration.getContractConfig().getWorkerPoolConfig();
			if (workerPoolConfig != null) {
				out.println("iExec WorkerPool name : " + workerPoolConfig.getName());
				out.println("iExec WorkerPool addr : " + workerPoolConfig.getAddress());
				_host.setWorkerPoolAddr(workerPoolConfig.getAddress());
			}
		} else {
		    disableBlockchain();
        }

		out.println("Host " + _host.toXml());
	}

	/**
	 * @return the hasKeyboard
	 */
	public boolean hasKeyboard() {
		return hasKeyboard;
	}

	/**
	 * @param hasKeyboard
	 *            the hasKeyboard to set
	 */
	public void setHasKeyboard(final boolean hasKeyboard) {
		this.hasKeyboard = hasKeyboard;
	}

	/**
	 * @return the hasMouse
	 */
	public boolean hasMouse() {
		return hasMouse;
	}

	/**
	 * @param hasMouse
	 *            the hasMouse to set
	 */
	public void setHasMouse(final boolean hasMouse) {
		this.hasMouse = hasMouse;
	}

	/**
	 * This is for debug purposes only
	 *
	 * @param args
	 *            is the array containing command line options
	 */
	public static void main(final String[] args) {
		new XWConfigurator(args[0], true);
	}
}
