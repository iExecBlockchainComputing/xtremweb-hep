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

package xtremweb.worker;

/**
 * CommManager.java
 * This class manages communications on worker side
 *
 * Created: Sat Jun  9 14:31:58 2001
 *
 * @author <a href="mailto:fedak@lri.fr">Gilles Fedak</a>
 * @version %I% %G%
 */

import java.io.*;
import java.net.*;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.xml.sax.SAXException;

import xtremweb.common.*;
import xtremweb.communications.*;
import xtremweb.communications.URI;

public final class CommManager extends Thread {

	private final Logger logger;

	/**
	 * This is a time stamp to keep how long we are waiting for a valid job
	 * If(xtremweb.common.XWConfigurator#noopTimeout is set) and if (currentTime
	 * - firstWorkRequest > xtremweb.common.XWConfigurator#noopTimeout) then
	 * exit
	 *
	 * @see XWConfigurator#getMaxTimeout()
	 * @since RPCXW v3
	 */
	private long firstWorkRequest;

	private CommQueue commQueue = null;

	private static CommManager instance = null;
	private boolean canRun = true;

	/**
	 * This aims to display some time stamps
	 */
	private final MileStone mileStone;
	/**
	 * This is for debug purposes only; this tells whether we are connected
	 */
	private boolean connected = true;
	/**
	 * This tells if this thread is sleeping
	 *
	 * @since 10.1.0
	 */
	private boolean sleeping;

	/**
	 * @return the sleeping
	 * @since 10.1.0
	 */
	public boolean isSleeping() {
		return sleeping;
	}

	/**
	 * @param sleeping
	 *            the sleeping to set
	 * @since 10.1.0
	 */
	private void setSleeping(final boolean sleeping) {
		this.sleeping = sleeping;
	}

	/**
	 * This manages works
	 */
	private PoolWork poolWork;

	private int initWorkTimeOut;
	private int nullWorkTimeOut;
	private int maxWorkTimeOut;

	private synchronized void resetTimeouts() {
		try {
			initWorkTimeOut = Worker.getConfig().getInt(XWPropertyDefs.TIMEOUT);
			nullWorkTimeOut = initWorkTimeOut;
			maxWorkTimeOut = Worker.getConfig().getMaxTimeout();
		} finally {
			notify();
		}
	}

	private synchronized void incTimeouts() {
		try {
			nullWorkTimeOut = (nullWorkTimeOut * 2);
			if (nullWorkTimeOut > maxWorkTimeOut) {
				nullWorkTimeOut = maxWorkTimeOut;
			}
		} finally {
			notify();
		}
	}

	/**
	 * This is the default and only constructor. This installs communication
	 * layers and try to connect to server.
	 *
	 * The specialized error <CODE>UpdateException</CODE>, defined in
	 * xtremweb.upgrade package, may occur while getting connected. In such a
	 * case we have to download a new version of the worker software and restart
	 * worker. This exception is not catched here, so caller(typically
	 * <CODE>ThreadLaunch::run()</CODE>) can get it.
	 */
	public CommManager() {

		super("CommManager");
		setPriority(Thread.MAX_PRIORITY);

		logger = new Logger(this);

		mileStone = new MileStone(getClass());

		setSleeping(false);
		resetTimeouts();

		instance = this;
		try {
			commQueue = new CommLL();
		} catch (final Error NoClassDefFoundError) {
			commQueue = new CommStack();
		}

		firstWorkRequest = -1;

		setPoolWork(new PoolWork());
		final Hashtable<UID, Work> sWorks = getPoolWork().getCompletedWorks();

		if (sWorks != null) {
			final Enumeration<Work> theEnumeration = sWorks.elements();
			while (theEnumeration.hasMoreElements()) {
				final Work w = theEnumeration.nextElement();
				if (w != null) {
					sendResult(w);
				}
			}
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				close();
			}
		});

	}

	private enum SleepEvent {
		NONE, NOCOMMEVENT, POOLWORKFULL, WORKREQUEST, SENDRESULT, NOWORKAVAILABLE, DOWNLOADERROR, RECONNECTION;
	}

	private void sleeping(final SleepEvent s) {
		sleeping(s, null, nullWorkTimeOut);
	}

	private void sleeping(final SleepEvent s, final String msg) {
		sleeping(s, msg, nullWorkTimeOut);
	}

	private void sleeping(final SleepEvent s, final String msg, final int d) {
		logger.debug("sleeping " + s + " (" + (msg == null ? "" : msg) + ") " + d);

		try {
			setSleeping(true);
			Thread.sleep(d);
		} catch (final Exception e) {
			setSleeping(false);
			resetTimeouts();
		}
	}

	/**
	 * @see #message(boolean, String)
	 */
	private void message(final boolean lost) {
		message(lost, null);
	}

	/**
	 * This prints a message on connection/deconnection events. This prints out
	 * a single message for several equivalent events in order to reduce
	 * outputs. This sets the connected attribute.
	 *
	 * @param lost
	 *            tells whether connection is lost or not
	 * @param trailer
	 *            is an optionnal message trailer
	 * @see #connected
	 */
	private void message(final boolean lost, final String trailer) {
		if (!lost) {
			if (!connected) {
				logger.error("XWHEP Worker (" + Version.currentVersion.full() + ") connected to \""
						+ Worker.getConfig().getCurrentDispatcher() + (trailer == null ? "\"" : "\" : " + trailer));
			}
			connected = true;
		} else {
			if (connected) {
				logger.error("XWHEP Worker (" + Version.currentVersion.full() + ") " + " connection lost from \""
						+ Worker.getConfig().getCurrentDispatcher() + (trailer == null ? "\"" : "\" : " + trailer));
			}
			connected = false;
		}
	}

	/**
	 * This inserts a work request event on event queue
	 */
	public void workRequest() {
		commQueue.workRequest();
	}

	/**
	 * This inserts a send result event on event queue
	 */
	protected void sendResult(final Work w) {
	    if (w == null) {
	        logger.debug("sendResult : uid is null");
            return;
        }
        logger.debug("CommManager#sendResult : " + w.toXml());

		commQueue.sendResult(w);
		resetTimeouts();
		this.interrupt();
	}

	/**
	 * This inserts a send work event on event queue
	 *
	 * @since 8.3.0
	 */
	protected void sendWork(final Work w) {
        if (w == null) {
            logger.debug("sendWork : uid is null");
            return;
        }

        logger.debug("CommManager#sendWork : " + w.toXml());
        commQueue.sendWork(w);
		resetTimeouts();
		this.interrupt();
	}

	/**
	 * This was previously named sendWork(). This has been renamed since 8.3.0
	 * This sends back the work to server. A worker must set
	 * XMLRPCCommandSendWork.host to make the server able to update the work and
	 * the associated task
	 *
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws UnknownHostException
	 * @throws ConnectException
	 * @throws SAXException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @since 8.0.0
	 */
	private void workSend(final Work w) throws ConnectException, UnknownHostException, URISyntaxException, IOException,
	InvalidKeyException, AccessControlException, SAXException {

		final URI uri = commClient().newURI(w.getUID());
		final XMLRPCCommandSend cmd = XMLRPCCommandSend.newCommand(uri, w);
		cmd.setHost(Worker.getConfig().getHost());
		logger.debug("CommManager#workSend " + cmd.toXml());
		commClient().send(cmd);
	}

	/**
	 * @return the instance
	 */
	public static CommManager getInstance() {
		return instance;
	}

	/**
	 * @param instance
	 *            the instance to set
	 */
	public static void setInstance(final CommManager instance) {
		CommManager.instance = instance;
	}

	/**
	 * This closes communication channel
	 */
	private void close() {
		try {
			commClient().close();
		} catch (final Exception e) {
		}
	}

	/**
	 * This retrieves some parameters from server
	 */
	public WorkerParameters getWorkersParameters() {
		logger.error("CommManager::getWorkerBin() not implemented yet");
		return null;
	}

	/**
	 * This asks the server for a new worker version
	 */
	public WorkInterface getWorkerBin(final HostInterface host) throws IOException {
		throw new IOException("CommManager::getWorkerBin() not implemented yet");
	}

	/**
	 * This sends traces to the server
	 *
	 * @throws IOException
	 */
	public void tactivityMonitor(final String hostName, final String login, final long start, final long end,
			final byte[] file) throws IOException {
		throw new IOException("CommManager::tactivityMonitor() not implemented yet");
	}

	/**
	 * This calls interrupt()
	 *
	 * @see #interrupt()
	 */
	public void terminate() {

		canRun = false;
		this.interrupt();
	}

	/**
	 * This retrieves the default comm client and initializes it
	 *
	 * @return the default comm client
	 */
	public CommClient commClient() throws UnknownHostException, IOException, ConnectException {


		try {
            final CommClient commClient = Worker.getConfig().defaultCommClient();
            CommClient.setConfig(Worker.getConfig());
            commClient.setAutoClose(true);
            return commClient;
		} catch (final Exception e) {
			throw new IOException(e.toString());
		}
	}

	/**
	 * This retrieves the comm client for the given URI and initializes it
	 *
	 * @param uri
	 *            is the uri to retrieve comm client for
	 * @return the expected comm client
	 */
	public CommClient commClient(final URI uri) throws UnknownHostException, ConnectException, IOException {

		try {
            final CommClient commClient = Worker.getConfig().getCommClient(uri);
            commClient.setAutoClose(true);

            logger.finest("commClient(" + uri + ")");

            return commClient;
		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException(e.getMessage());
		}
    }

	/**
	 * This connects to server to request a new work Since 7.2.0, this resets
	 * host.jobId because this must be used only once
	 *
	 * Since 9.1.0, this updates this host free disk space before remotely
	 * calling workRequest()
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private WorkInterface getWork() throws ClassNotFoundException, UnknownHostException, ConnectException, IOException,
	SAXException, URISyntaxException, InvalidKeyException, AccessControlException {

		final HostInterface workerHost = Worker.getConfig().getHost();
		final File d = Worker.getConfig().getTmpDir();
		workerHost.setFreeTmp(d.getFreeSpace() / XWTools.ONEMEGABYTES);
        if((workerHost.getWorkerPoolAddr() == null) || (workerHost.getWorkerPoolAddr().trim().length() < 1)){
			Worker.getConfig().getBlockchainEthConfig();
		}
		final WorkInterface ret = commClient().workRequest(workerHost);
		Worker.getConfig().getHost().setJobId(null);
		return ret;
	}

	/**
	 * This retrieves the app for the given UID
	 *
	 * @param uid
	 *            is the app uid
	 * @return the app
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private AppInterface getApp(final UID uid) throws ClassNotFoundException, UnknownHostException, ConnectException,
	IOException, SAXException, URISyntaxException, InvalidKeyException, AccessControlException {

		final AppInterface app = (AppInterface) commClient().get(uid, false);
		if (app == null) {
			throw new IOException("application not found");
		}
		return app;
	}

	/**
	 * This downloads data from an URI pass through file and call installFile()
	 * for each uri. This stops on the first error while trying to install URIs.
	 *
	 * @param throughUri
	 *            is the URI of the URI pass through file
	 * @return the directory of the last installed data
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @since 8.0.0
	 */
	protected File uriPassThrough(final URI throughUri, final long maxLength) throws IOException, URISyntaxException, InvalidKeyException,
	AccessControlException, ClassNotFoundException, SAXException, XWCategoryException {

		final File ret = null;
		boolean islocked = false;
		BufferedReader reader = null;

		try {
			logger.debug("uriPassThrough 00");

			CommManager.instance.commClient().lock(throughUri);
			logger.debug("uriPassThrough 01");
			islocked = true;
			final File fData = CommManager.instance.commClient().getContentFile(throughUri);
			logger.debug("uriPassThrough 02");
			reader = new BufferedReader(new FileReader(fData));
			while (true) {
				String line = null;
				try {
					logger.debug("uriPassThrough 00 line = " + line);
					line = reader.readLine();
					logger.debug("uriPassThrough 01 line = " + line);
				} catch (final Exception e) {
					line = null;
				}
				if ((line == null) || (line.length() < 4)) {
					break;
				}
				if (line.indexOf(Connection.SCHEMESEPARATOR) < 0) {
					continue;
				}
				logger.debug("uriPassThrough line = " + line);
				final URI uri = new URI(line);
				if ((uri.getScheme() == null) || (uri.getHost() == null)) {
					throw new URISyntaxException(line, "syntax error (sheme or host is null)");
				}
				logger.debug("uriPassThrough uri = " + line);
				downloadData(uri, maxLength, false);
			}
		} finally {
			if (islocked) {
				CommManager.instance.commClient().unlock(throughUri);
			}
			try {
				reader.close();
			} catch (final Exception e) {
			}
			reader = null;
		}

		logger.debug("uriPassThrough 03");

		return ret;
	}

	/**
	 * This all data associated to the current work
	 *
	 * @param theWork is the work
	 *
	 */
	private void downloadWork(final Work theWork) throws IOException, XWCategoryException {

		try {
			final float downloadBandwidth = downloadData(theWork.getStdin(), theWork.getMaxFileSize(), false);
			theWork.setDownloadBandwidth(downloadBandwidth);
        } catch (final XWCategoryException e) {
            throw new XWCategoryException("stdin " + e.getMessage());
        } catch (final Exception e) {
            throw new IOException("can't download stdin (" + theWork.getStdin() + ")");
        }

        try {
			final URI uri = theWork.getDirin();
			final float downloadBandwidth = downloadData(uri, theWork.getMaxFileSize(), false);
            theWork.setDownloadBandwidth(downloadBandwidth);
            final DataInterface dirin = getData(uri, false);
			if (dirin != null) {
				final DataTypeEnum dirinType = dirin.getType();
				logger.debug("dirinType = " + dirinType);
				if ((dirinType != null) && (dirinType == DataTypeEnum.URIPASSTHROUGH)) {
					uriPassThrough(uri, theWork.getMaxFileSize());
				}
			}
        } catch (final XWCategoryException e) {
            throw new XWCategoryException("dirin " + e.getMessage());
		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException("can't download dirin (" + e.getMessage() + ")");
		}

		try {
			final DataInterface drivenData = getData(theWork.getDataDriven(), false);
			if (drivenData != null) {
				final String sharedDataPkgs = Worker.getConfig().getHost().getSharedDatas();
				logger.debug("downloadWork worker sharedDataPkg = " + sharedDataPkgs);
				final Collection<String> datas = XWTools.split(sharedDataPkgs, ",");
				boolean found = false;
				for (final Iterator<String> datasIterator = datas.iterator(); datasIterator.hasNext();) {
					final String sharedData = datasIterator.next();
					if (drivenData.getPackage().compareTo(sharedData) == 0) {
						theWork.setDataPackage(sharedData);
						found = true;
						break;
					}
				}
				if (!found) {
					throw new IOException("Driven data package (" + drivenData.getPackage()
					+ ") doesn't match worker packages : " + sharedDataPkgs);
				}
			}
		} catch (final Exception e) {
			logger.exception(e);
			throw new IOException("can't download driven data (" + e.getMessage() + ")");
		}
	}

    /**
     * This retrieves the app This also retrieves all app files contents (bin,
     * stdin, dirin...)
     *
     * @param uid
     *            is the data uid
     * @throws AccessControlException
     * @throws InvalidKeyException
     */
    private void downloadApp(final UID uid) throws ClassNotFoundException, UnknownHostException, ConnectException,
            IOException, SAXException, URISyntaxException, InvalidKeyException, AccessControlException {

        final AppInterface app = getApp(uid);
        final CPUEnum cpu = Worker.getConfig().getHost().getCpu();
        final OSEnum os = Worker.getConfig().getHost().getOs();

        URI uri = null;

        final AppTypeEnum appType = app.getType();
        final String apptypestr = appType.toString();
        final boolean localapp = Worker.getConfig().getLocalApps() == null ?
                false:
                    Worker.getConfig().getLocalApps().contains(apptypestr);

        logger.error("CommManager : can't use app library; please use executables");
        uri = app.getBinary(cpu, os);

        if ((!localapp) && (uri == null)) {
            throw new IOException("binary not defined");
        }

        try {
            final DataInterface data = getData(uri, false);
            downloadData(uri,
                    data != null ? data.getSize() : 0,
                    false);
        } catch (final Exception e) {
            logger.exception(e);
            throw new IOException("can't download binary (" + uri + ")");
        }

        try {
            logger.error("CommManager : can't use app library; please use executables");
            uri = app.getLibrary(cpu, os);
            final DataInterface data = getData(uri, false);
            downloadData(uri,
                    data != null ? data.getSize() : 0,
                    false);
        } catch (final Exception e) {
            logger.exception(e);
            throw new IOException("can't download library (" + uri + ")");
        }
        
        try {
            uri = app.getBaseDirin();
            final DataInterface data = getData(uri, false);
            downloadData(uri,
                    data != null ? data.getSize() : 0,
                    false);
        } catch (final Exception e) {
            logger.exception(e);
            throw new IOException("can't download base dirin (" + uri + ")");
        }

        try {
            uri = app.getDefaultDirin();
            final DataInterface data = getData(uri, false);
            downloadData(uri,
                    data != null ? data.getSize() : 0,
                    false);
        } catch (final Exception e) {
            logger.exception(e);
            throw new IOException("can't download default dirin (" + uri + ")");
        }

        try {
            uri = app.getDefaultStdin();
            final DataInterface data = getData(uri, false);
            downloadData(uri,
                    data != null ? data.getSize() : 0,
                    false);
        } catch (final Exception e) {
            logger.exception(e);
            throw new IOException("can't download default stdin (" + uri + ")");
        }
    }

	/**
	 * This retrieves the data for the given URI
	 *
	 * @param uri
	 *            is the data uri
	 * @return the data
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	public DataInterface getData(final URI uri) throws ClassNotFoundException, UnknownHostException, ConnectException,
	IOException, SAXException, InvalidKeyException, AccessControlException, URISyntaxException {

		return getData(uri, true);
	}

	/**
	 * This retrieves the data for the given URI
	 *
	 * @param uri
	 *            is the data uri
	 * @return the data
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private DataInterface getData(final URI uri, final boolean bypass)
			throws ClassNotFoundException, UnknownHostException, ConnectException, IOException, SAXException,
			InvalidKeyException, AccessControlException, URISyntaxException {

		if (uri == null) {
			return null;
		}
		final CommClient commClient = commClient(uri);
		final DataInterface data = (DataInterface) commClient.get(uri, bypass);

		if (data == null) {
			throw new IOException("can't retreive data " + uri);
		}
		return data;
	}

	/**
	 * This retrieves the data for the given URI This finally retrieves the data
	 * content for the given URI
	 *
	 * @param uri
	 *            is the data uri
     * @param maxLength is the max data size as definedby the category
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private float uploadData(final URI uri, final long maxLength) throws ClassNotFoundException, UnknownHostException, ConnectException,
	IOException, SAXException, InvalidKeyException, AccessControlException, URISyntaxException, XWCategoryException {

		if (uri == null) {
			throw new IOException("uploadData() : uri is null");
		}

		boolean islocked = false;
		try {
			final DataInterface data = getData(uri);
			if (data == null) {
				throw new IOException("uploadData(" + uri.toString() + ") can't get data");
			}
			final long start = System.currentTimeMillis();
			commClient().lock(uri);
			islocked = true;
			final File fdata = commClient().getContentFile(uri);
            if (fdata == null) {
                throw new IOException("uploadData(" + uri.toString() + ") can't get content file");
            }
            if (fdata.length() > maxLength) {
                throw new XWCategoryException("uploadData(" + uri.toString() +
                        ") file too long for the category (" +
                        fdata.length() + ", " +
                        maxLength + ")");
            }
			final long fsize = fdata.length();

			logger.debug("CommManager#uploadData " + fdata);
			commClient(uri).uploadData(uri, fdata);

			final long end = System.currentTimeMillis();
			final float bandwidth = fsize / (end - start);
			logger.info("Upload bandwidth = " + bandwidth);
			Worker.getConfig().getHost().setUploadBandwidth(bandwidth);
			return bandwidth;
		} finally {
			if (islocked) {
                commClient().unlock(uri);
			}
		}
	}

	/**
	 * This does nothing if uri parameter is null. This retrieves the data from
	 * a server Depending on download parameter, this downloads the data
	 * content, if not already in cache. This first tries to call
	 * downloadData(new URL(uri), download) to try to download data from an HTTP
	 * server if uri is an URL
	 *
	 * @param uri
	 *            is the data uri
	 * @param maxLength is the data size limit given by the category
	 * @param bypass
	 *            if false, data content is not downloaded if data already in
	 *            cache and if data integrity is fine; if true, data content is
	 *            downloaded even if data already in cache
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @since 13.0.0
	 */
	protected synchronized float downloadData(URI uri, final long maxLength, final boolean bypass)
			throws ClassNotFoundException, UnknownHostException, ConnectException, IOException, SAXException,
			InvalidKeyException, AccessControlException, URISyntaxException, XWCategoryException {

		File fdata = null;
		boolean islocked = false;

		CommClient commClient = null;

		try {
			logger.debug("downloadData(" + uri + "," + bypass + ")");

			if ((uri == null) || (uri.isNull())) {
				logger.finest("downloadData : uri is null");
				return -1;
			}

			commClient = commClient(uri);

			if (uri.isHttp() || uri.isHttps() || uri.isAttic()) {
				return wget(uri, maxLength);
			}

			DataInterface data = null;
			try {
				data = getData(uri, bypass);
			} catch (final Exception e) {
				data = null;
			}

			if (data == null) {
				throw new IOException("can't retreive data " + uri);
			}

			if (data.getShasum() == null) {
				throw new IOException(uri.toString() + " SHASUM is not set");
			}
            if (data.getSize() > maxLength) {
                throw new XWCategoryException("downloadData(" + uri.toString() +
                                ") file too long for the category (" +
                                fdata.length() + ", " +
                                maxLength + ")");
            }
			commClient.lock(uri);
			islocked = true;
			final URI datauri = data.getURI();
			if (datauri != null) {
				if (islocked) {
					commClient.unlock(uri);
					islocked = false;
				}
				uri = datauri;
				commClient.lock(uri);
				islocked = true;
			}

			commClient.addToCache(data, uri);

			fdata = commClient.getContentFile(uri);
			if (data.getType() == DataTypeEnum.BAT) {
				final UID uid = data.getUID();
				final String cmdname = uid.toString() + DataTypeEnum.BAT.getFileExtension();
				fdata = new File(fdata.getParentFile(), cmdname);
				commClient.setContentFile(uri, fdata);
			}

			logger.debug("downloadData(" + uri + ") = " + fdata);

			if (fdata == null) {
				throw new IOException(uri.toString() + " can't cache data");
			}

			final long start = System.currentTimeMillis();
			long fsize = fdata.length();

			if ((fdata.exists()) && (!bypass) && (data.getShasum().compareTo(XWTools.sha256CheckSum(fdata)) == 0)
					&& (data.getSize() == fsize)) {
				logger.config("Not necessary to download data " + data.getUID());
				return -1;
			}

			if (uri.isHttp() || uri.isAttic()) {
				if (islocked) {
					commClient.unlock(uri);
					islocked = false;
				}
				final String name = data.getName();
				wget(uri, maxLength);
				commClient.lock(uri);
				islocked = true;
				data = getData(uri, bypass);
				data.setName(name);
				commClient.addToCache(data, uri);
			} else if (uri.isXtremWeb()) {
				commClient.downloadData(uri, fdata);
			} else if (!uri.isFile()) {
				throw new IOException(uri.toString() + " : unknown schema");
			}

			fsize = fdata.length();
			final long end = System.currentTimeMillis();
			final float bandwidth = fsize / ((end - start) + 1);
			Worker.getConfig().getHost().setDownloadBandwidth(bandwidth);
			logger.info("Download bandwidth = " + bandwidth);

			if ((data.getShasum().compareTo(XWTools.sha256CheckSum(fdata)) != 0) || (data.getSize() != fsize)) {
				throw new IOException(uri.toString() + " SHASUM or size differs");
			}

            return bandwidth;

        } catch (NoSuchAlgorithmException e) {
            logger.exception(e);
		} finally {
			if (fdata != null) {
				for (int nbtry = 0; nbtry < 2; nbtry++) {
					if (fdata.exists()) {
						break;
					}
					try {
						sleeping(SleepEvent.DOWNLOADERROR, "download error", 100);
					} catch (final Exception e) {
					}
				}
			}

			if (islocked && (commClient != null)) {
				commClient.unlock(uri);
			}
			commClient = null;
            fdata = null;
			notifyAll();
		}
		return -1;
	}

	/**
	 * This retrieves the data from an HTTP server
	 *
	 * @param uri
	 *            is the data uri
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private float wget(final URI uri, final long maxLength) throws ClassNotFoundException, UnknownHostException, ConnectException, IOException,
	SAXException, InvalidKeyException, AccessControlException, URISyntaxException {

		if (uri == null) {
			return -1;
		}
		CommClient commClient = null;
		DataInterface data = null;
		boolean islocked = false;

		try {
			commClient = commClient(uri);
			try {
				data = (DataInterface) commClient.get(uri);
			} catch (final Exception e) {
				data = null;
			}
			if (data == null) {
				commClient.addToCache(uri);
			}
			data = (DataInterface) commClient.get(uri);
			commClient.lock(uri);
			islocked = true;
			final File fdata = commClient.getContentFile(uri);

			logger.debug("wget(" + uri + ") = " + fdata);

			if (fdata == null) {
				throw new IOException(uri.toString() + " can't cache data");
			}
//			data.setName(uri.getPath());
			data.setName(uri.getName());
			commClient.addToCache(data, uri);
			data = null;

			final long start = System.currentTimeMillis();

			StreamIO io = null;
			try {
				final URL url = new URL(uri.toString().replaceAll("&amp;", "&"));
				final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.connect();  // conn.setFollowRedirects(true); true is the default
				mileStone.println("<readfile file='" + fdata + "'>");
//				io = new StreamIO(null, new DataInputStream(url.openStream()), false);
				io = new StreamIO(null, new DataInputStream(conn.getInputStream()), false);
				io.readFileContent(fdata, maxLength);
				mileStone.println("</readfile>");
			} catch (final Exception e) {
				logger.exception(e);
				throw new IOException(e.getMessage());
			} finally  {
				if (io != null) {
					io.close();
				}
			}

			final long fsize = fdata.length();
			final long end = System.currentTimeMillis();
			final float bandwidth = fsize / (end - start);
			Worker.getConfig().getHost().setDownloadBandwidth(bandwidth);
			logger.info("Download bandwidth = " + bandwidth);
			return bandwidth;
		} finally {
			if (islocked && (commClient != null)) {
				commClient.unlock(uri);
			}
			commClient = null;
		}
	}

	/**
	 * This is the main loop This pools out a communication event and process it
	 */
	@Override
	public void run() {

		while (canRun) {

			resetTimeouts();

			logger.debug("TIMEOUT = " + nullWorkTimeOut + " ; INITTIMEOUT = " + initWorkTimeOut + " ; MAXTIMEOUT = "
					+ maxWorkTimeOut);

			mileStone.clear();
			final CommEvent ce = commQueue.getCommEvent();
			if (ce == null) {
				if (commQueue.size() < 1) {
					logger.error(
							"no pending comm event : is there a bug left in CommManager ? (forcing a work request)");
					workRequest();
				} else {
					logger.debug("found no comm event");
				}
				sleeping(SleepEvent.NOCOMMEVENT);
				continue;
			}

			commQueue.removeCommEvent(ce);

			logger.debug("sending " + ce.getCommType());

			switch (ce.getCommType()) {

			case WORKREQUEST:

				WorkInterface mw = null;

				if (firstWorkRequest == -1) {
					firstWorkRequest = System.currentTimeMillis() / 1000;
				}
				if (getPoolWork().isFull() || !ThreadLaunch.getInstance().available()) {
					sleeping(SleepEvent.POOLWORKFULL);
					workRequest();
					continue;
				}

				try {
					final long noopTimeout = Worker.getConfig().getInt(XWPropertyDefs.NOOPTIMEOUT);

					if ((noopTimeout > 0) && (firstWorkRequest > 0)) {
						final long current = System.currentTimeMillis() / 1000;
						final long delai = current - firstWorkRequest;

						logger.debug("delai = " + delai);

						if (delai > noopTimeout) {
							System.out.println("XWHEP Worker (" + Version.currentVersion.full() + ") [" + new Date()
									+ "] ended : not waiting any longer (" + delai + " > " + noopTimeout + ")");
							System.exit(0);
						}
					}

					mw = getWork();

					message(false);
				} catch (final Exception e) {
					if (e instanceof SAXException) {
						logger.info("Server gave no work to compute");
					} else {
						logger.exception(e);
					}
					close();
					close();

					sleeping(SleepEvent.WORKREQUEST, e.toString());

					mw = null;
				}

				incTimeouts();

				if (mw == null) {
					sleeping(SleepEvent.NOWORKAVAILABLE);
					workRequest();
					continue;
				}

				if (mw.getUID() == null) {
					logger.error("mw.iud = null ?!?");
					continue;
				}

				Worker.getConfig().incNbJobs();
				if (Worker.getConfig().stopComputing()) {
					if (commQueue.size() < 1) {
						System.out.println("XWHEP Worker (" + Version.currentVersion.full() + ") [" + new Date()
								+ "] ended : enough computings (" + Worker.getConfig().getNbJobs() + " > "
								+ Worker.getConfig().getInt(XWPropertyDefs.COMPUTINGJOBS) + ")");
						System.exit(0);
					}

					logger.warn("Enough jobs! (" + Worker.getConfig().getNbJobs() + " already downloaded)"
							+ " but there's still " + commQueue.size() + " results to send");

					continue;
				}

				firstWorkRequest = -1;

				mileStone.println("got new work");

				Work newWork = null;

				try {
					mw.setStatus(StatusEnum.WAITING);
					newWork = getPoolWork().addWork(mw);
					ThreadLaunch.getInstance().wakeup();

                    downloadWork(newWork);
                    downloadApp(newWork.getApplication());

					mileStone.println("got new work files");

					newWork.setPending();
					newWork.setDataReadyDate(new Date());
                } catch (final XWCategoryException e) {
                    logger.exception("Category error", e);
                    newWork.setErrorMsg("Category error : " + e.getMessage());
                    newWork.setCompleted();
                } catch (final Exception e) {
                    logger.exception("Downloading error", e);
                    newWork.setError("IOError : " + e.getMessage());
                } finally {
					close();

					if ((newWork != null) && (newWork.isCompleted() || newWork.isError())){
						sendResult(newWork);
					} else {
						logger.error("Downloading error : newWork = null ?!?!");
					}
				}
				resetTimeouts();
				break;

			case UPLOADDATA:
				Work finishedWork = null;
				try {
					finishedWork = ce.getWork();
					try {
						if (finishedWork != null) {
							uploadResults(finishedWork);
						}
					} catch (final Exception e) {
						logger.exception(e);
					}
				} catch (final Exception e) {
					logger.exception(e);

					if (finishedWork != null) {
						sendResult(finishedWork);
					}
					sleeping(SleepEvent.SENDRESULT, e.toString());
				} finally {
					close();
				}
				break;

			case SENDWORK:
				Work workToSend = null;
				try {
					workToSend = ce.getWork();
					try {
						if (workToSend != null) {
							workSend(workToSend);
						}
					} catch (final Exception e) {
						logger.exception(e);
					}
				} catch (final Exception e) {
					logger.exception(e);

					if (workToSend != null) {
						sendWork(workToSend);
					}
					sleeping(SleepEvent.SENDRESULT, e.toString());
				} finally {
					close();
				}
				break;

			default:
				logger.warn("Unknown Communication type");
				break;
			}
		}

		logger.warn("CommManager terminating");
	}

	/**
	 * This uploads result to server and updates job to server
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	protected void uploadResults(final Work theWork) throws IOException, ClassNotFoundException, SAXException,
	URISyntaxException, InvalidKeyException, AccessControlException, XWCommException {

		if (theWork == null) {
			logger.error("CommManager#uploadResults : theWork is null");
			return;
		}

		final URI resultURI = theWork.getResult();

		if (resultURI == null) {
			logger.debug("uploadResults : no result to upload");
//			workSend(theWork);
//			getPoolWork().saveCompletedWork(theWork);
			return;
		}

        mileStone.println("<uploadResults>");

        final DataInterface data = getData(resultURI, false);
		final CommClient commClient = commClient(resultURI);
		try {
		    if(theWork.isRevealed()) {
		        logger.debug("CommManager#uploadResults : the work can reveal " + theWork.toXml());
                final File content = commClient.getContentFile(resultURI);
                logger.debug("CommManager#uploadResults " + content);
                if (content.exists()) {
					// we must send data now to reveal
					// but we must not reveal now : set shasum to null
					data.setSize(content.length());
                    data.setShasum(theWork.getHiddenH2r());
                    commClient.send(data);
                    logger.debug("CommManager#uploadResults revealing " + data.toXml());
                    try {
                        final float updloadBandwidth = uploadData(resultURI, theWork.getMaxFileSize());
                        theWork.setUploadBandwidth(updloadBandwidth);
                    } catch (final XWCategoryException e) {
                        logger.exception("CommManager#uploadResults", e);
                        theWork.setErrorMsg(e.getMessage());
                    } finally {
                        theWork.setStatus(StatusEnum.COMPLETED);
                    }
	    	    }

                message(false);
            } else {
                logger.debug("CommManager#uploadResults : the work can not reveal " + theWork.toXml());
                if(theWork.isContributing()) {

                	// we must send data now to comply to xtremweb workflow
                	// but we must not reveal now : set shasum to null
                	data.setSize(0);
                	data.setShasum(null);
                	data.setStatus(StatusEnum.UNAVAILABLE);
					commClient.send(data);
				}
			}

		} finally {

            try {
                logger.debug("CommManager#uploadResults : " + theWork.toXml());
                workSend(theWork);
            } catch (final Exception e) {
                logger.exception(e);
            }

//            getPoolWork().saveWork(theWork);

            if (Worker.getConfig().stopComputing()) {
                System.err.println("XWHEP Worker (" + Version.currentVersion.full() + ") [" + new Date()
                        + "] ended : enough computings (" + Worker.getConfig().getNbJobs() + " > "
                        + Worker.getConfig().getInt(XWPropertyDefs.COMPUTINGJOBS) + ")");

                System.exit(0);
            }

        }
        mileStone.println("</uploadResults>");
	}

	/**
	 * @return the poolWork
	 */
	public PoolWork getPoolWork() {
		return poolWork;
	}

	/**
	 * @param poolWork
	 *            the poolWork to set
	 */
	public void setPoolWork(final PoolWork poolWork) {
		this.poolWork = poolWork;
	}
}
