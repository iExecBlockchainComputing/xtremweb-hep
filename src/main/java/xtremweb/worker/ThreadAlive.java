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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.xml.sax.SAXException;

import xtremweb.common.DataInterface;
import xtremweb.common.Logger;
import xtremweb.common.StreamIO;
import xtremweb.common.UID;
import xtremweb.common.Version;
import xtremweb.common.XMLValue;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWReturnCode;
import xtremweb.common.XWTools;
import xtremweb.communications.*;

/**
 *
 * This uses xtremweb.communications.HTTPClient as communication layer since RMI
 * is obsolete and UDP does not implement workAlive methods <br/>
 *
 * Signal to the coordinator the worker is still there.<br />
 *
 * Historically, that signal was called 'Alive' since it was preminirarly only
 * used to signal the dispatcher this worker is still here. This processes a
 * much more than signalling only since RPC-V.<br>
 * Since that version, this communication channel is used to:
 * <ul>
 * <li>prevent job reschedulling by informing this worker is still here (as
 * earlier versions)
 * <li>stop computing its current job, for any reason (as earlier versions)
 * <li>ensure safe result storage by keeping job results on this worker disk.
 * This worker can then be asked to definitly remove its local job results
 * copies on sucessfull job result storage on server side, or to resend them on
 * server demand.
 * <li>update some informations :
 * <ul>
 * <li>alive period
 * <li>traces parameters
 * <li>servers list, including current server. If that last is set, this worker
 * connects to the new current server
 * </ul>
 * </ul>
 *
 * Created: Thu Jun 29 17:47:11 2000
 *
 * @author Gilles Fedak
 */

public class ThreadAlive extends Thread {

	private final Logger logger;

	/** Time between workAlive in seconds */
	private int alivePeriod = 300;

	/** This controls the main loop */
	private boolean canRun = true;

	/** Communication layer */
	private final XWConfigurator config;

	/**
	 * This is the only constructor This initializes TCP communication layer
	 * whatever could be written in config file
	 */
	public ThreadAlive(final XWConfigurator conf) {

		super("ThreadAlive");
		logger = new Logger(this);
		config = conf;
		alivePeriod = 300;
	}

	/**
	 * This stops the execution of the thread
	 */
	public void terminate() {
		canRun = false;
		this.interrupt();
	}

	/**
	 * This is the thread execution method
	 */
	@Override
	public void run() {

		logger.info("Thread Alive started");

		while (canRun) {

			try {
				synchronize();
			} catch (InvalidKeyException | AccessControlException | ClassNotFoundException | IOException
					| URISyntaxException | SAXException | NoSuchAlgorithmException e) {
				logger.exception(e);
			}
			try {
				// we don't sleep for the whole timeout as we need to
				// take care that the computation is still on.
				logger.config("Sleep until the next alive (" + alivePeriod + " seconds)");
				java.lang.Thread.sleep(alivePeriod * 1000);

				final Vector<Work> wal = CommManager.getInstance().getPoolWork().getAliveWork();
				if (wal == null) {
					continue;
				}

				logger.debug(" wal size = " + wal.size());

				for (int i = 0; i < wal.size(); i++) {

					final Work w = wal.elementAt(i);
					logger.finest("ThreadAlive  calling checkJob()");
					checkJob(w);
				}
				wal.clear();
			} catch (final InterruptedException e) {
				break;
			} catch (final IOException e) {
				logger.exception(e);
			}
		}

		logger.warn("ThreadAlive terminating");

	}

	/**
	 * This checks the provided job accordingly to the server status (i.e.
	 * should we continue computing the job ? )
	 *
	 * @param theJob
	 *            is the work to signal.
	 * @throws IOException
	 */
	private void checkJob(final Work theJob) throws IOException {

		if (theJob == null) {
			logger.debug("ThreadAlive::checkJob() : theJob = null");
			return;
		}

		Hashtable rmiResults = null;

		//
		// send job UID to the server and collect informations about that
		// job
		//
		try {
			rmiResults = workAlive(theJob.getUID());
		} catch (final Exception e) {
			logger.error("connection error" + e);
		}

		if (rmiResults == null) {
			logger.debug("ThreadAlive::checkJob() : rmiResults = null");
			return;
		}

		//
		// should this worker stop current computation ?
		//
		final Boolean keepWorking = (Boolean) rmiResults.get("keepWorking");
		if (keepWorking != null) {

			final String msg = "workAlive(" + theJob.getUID() + ")";

			if (keepWorking.booleanValue() == false) {

				final ThreadWork tw = ThreadLaunch.getInstance().getThreadByWork(theJob);

				if (tw != null) {

					logger.info(msg + " stop working thread");
					tw.stopProcess();
				} else {
					logger.warn(msg + " can't find working thread");
				}
				CommManager.getInstance().getPoolWork().removeWork(theJob.getUID());

				ThreadLaunch.getInstance().raz();
			} else {
				logger.debug(msg + " ok");
			}
		}
	}

	/**
	 * This synchronizes with the server :
	 * <ul>
	 * <li>ping the server
	 * <li>send the list of local results
	 * <li>retrieve results status (can we definitly delete them ? should we
	 * re-send them ?)
	 * </ul>
	 * Since 9.1.1, this sends the first 20 job results only, otherwise the
	 * message may be too long and reset the comm channel
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException 
	 *
	 * @see xtremweb.dispatcher.CommHandler#workAlive(XMLRPCCommand, Hashtable)
	 */
	private void synchronize() throws IOException, URISyntaxException, InvalidKeyException, AccessControlException,
			ClassNotFoundException, SAXException, NoSuchAlgorithmException {

		Hashtable rmiResults = null;

		ping();
		//
		// retrieve stored job results
		//
		final Vector<UID> jobResults = new Vector<>();
		final Hashtable<UID, Work> savingWorks = CommManager.getInstance().getPoolWork().getSavingWork();

		final Enumeration<Work> theEnumeration = savingWorks.elements();

		for (int i = 0; (i < 20) && theEnumeration.hasMoreElements(); i++) {
			final Work aWork = theEnumeration.nextElement();
			if (aWork == null) {
				continue;
			}

			logger.debug("threadAlive() : job results " + aWork.getUID());

			jobResults.add(aWork.getUID());
		}

		logger.debug("threadAlive() : jobResults.size() = " + jobResults.size());
		final Hashtable rmiParams = new Hashtable();
		rmiParams.put(XWPostParams.JOBRESULTS.toString(), jobResults);

		//
		// send them to the server and retrieve some informations
		//
		try {
			rmiResults = workAlive(rmiParams);
		} catch (final Exception e) {
			logger.exception("workAlive : connection error", e);
			return;
		}

		rmiParams.clear();

		if (rmiResults == null) {
			logger.debug("ThreadAlive::synchronize() : rmiResults = null");
			return;
		}

		final String serverVersion = (String) rmiResults.get(XWPostParams.CURRENTVERSION.toString());
		if ((serverVersion != null) && !serverVersion.equals(Version.currentVersion.toString())) {
			logger.info("**********  **********  **********");
			logger.info("\nCurrent version : " + Version.currentVersion.toString());
			logger.info("Server  version : " + serverVersion);
			logger.info("We must upgrade");
			logger.info("Restarting now");
			logger.info("\n**********  **********  **********");
			System.exit(XWReturnCode.RESTART.ordinal());
		}

		//
		// RPC-V : retrieve saved tasks and remove them from from
		// PoolWork::savingTasks
		//
		final Vector finishedTasks = (Vector) rmiResults.get(XWPostParams.FINISHEDTASKS.toString());

		if (finishedTasks != null) {
			logger.debug("ThreadAlive() : finishedTasks.size() = " + finishedTasks.size());
			final Iterator<XMLValue> li = finishedTasks.iterator();

			while (li.hasNext()) {
				final UID uid = (UID) li.next().getValue();
				if (uid != null) {
					CommManager.getInstance().getPoolWork().removeWork(uid);
				}
			}
		}

		//
		// RPC-V : retrieve tasks which results are expected by the
		// coordinator
		//
		final Vector resultsExpected = (Vector) rmiResults.get(XWPostParams.RESULTEXPECTEDS.toString());

		if (resultsExpected != null) {
			logger.debug("ThreadAlive() : resultsExpected.size() = " + resultsExpected.size());

			final Iterator<XMLValue> li = resultsExpected.iterator();

			while (li.hasNext()) {
				final UID uid = (UID) li.next().getValue();
				if (uid == null) {
					continue;
				}

				Work theWork = CommManager.getInstance().getPoolWork().getSavingWork(uid);
				if (theWork == null) {
					final ThreadWork threadWork = ThreadLaunch.getInstance().getThreadByWorkUid(uid);
					if (threadWork == null) {
						logger.error("ThreadAlive() : can't retreive running work = " + uid);
						continue;
					}
					try {
						threadWork.zipResult();
						theWork = threadWork.getCurrentWork();
					} catch (final Exception e) {
						logger.exception(e);
						theWork = null;
					}
				}
				if (theWork != null) {
					CommManager.getInstance().sendResult(theWork);
				}
			}
		}

		//
		// Retrieve new server key
		//
		final String keystoreUriStr = (String) rmiResults.get(XWPostParams.KEYSTOREURI.toString());
		if ((keystoreUriStr != null) && (keystoreUriStr.length() > 0)) {
			logger.info("ThreadAlive() KEYSTOREURI : " + keystoreUriStr);
			boolean newkeystore = false;
			final URI keystoreUri = new URI(keystoreUriStr);
			final File currentKeystoreFile = new File(System.getProperty(XWPropertyDefs.JAVAKEYSTORE.toString()));
			logger.debug("currentKeystoreFile : " + currentKeystoreFile + " length = " + currentKeystoreFile.length());

			final DataInterface newKeystoreData = CommManager.getInstance().getData(keystoreUri);
			if (newKeystoreData == null) {
				throw new IOException("Can't retrieve new keystore data " + keystoreUri);
			}

			final String currentKeystoreSHA = XWTools.sha256CheckSum(currentKeystoreFile);

			if (newKeystoreData.getShasum().compareTo(currentKeystoreSHA) != 0) {
				logger.info("Downloading new keystore");
				try {
					CommManager.getInstance().downloadData(keystoreUri, XWPostParams.MAXUPLOADSIZE, false);
				} catch(final XWCommException e) {
					logger.exception(e);
				}
				final File newKeystoreFile = CommManager.getInstance().commClient(keystoreUri)
						.getContentFile(keystoreUri);

				try (final FileOutputStream foutput = new FileOutputStream(currentKeystoreFile);
						final DataOutputStream output = new DataOutputStream(foutput);
						final StreamIO io = new StreamIO(output, null, false)) {

					logger.debug("newKeystoreFile : " + newKeystoreFile + " length = " + newKeystoreFile.length());
					io.writeFileContent(newKeystoreFile);
					newkeystore = true;
				} catch (final Exception e) {
					logger.exception("can't download KEYSTOREURI", e);
				} finally {
					if (newkeystore == true) {
						logger.info("**********  **********  **********");
						logger.info("New keystore received");
						logger.info("Restarting now");
						logger.info("**********  **********  **********");
						System.exit(XWReturnCode.RESTART.ordinal());
					}
				}
			}
		}

		//
		// retrieve new server to connect to
		//
		final String newServer = (String) rmiResults.get(XWPostParams.NEWSERVER.toString());
		if (newServer != null) {
			logger.debug("ThreadAlive() new server : " + newServer);
			config.addDispatcher(newServer);
		}

		//
		// the SmartSockets hub address
		//
		final String hubAddrStr = (String) rmiResults.get(Connection.HUBPNAME);
		String dbgMsg = "SmartSockets hub address = " + (hubAddrStr == null ? "unknwown" : hubAddrStr);
		if (hubAddrStr != null) {
			System.setProperty(XWPropertyDefs.SMARTSOCKETSHUBADDR.toString(), hubAddrStr);
		}

		final Boolean traces = (Boolean) rmiResults.get(XWPostParams.TRACES.toString());
		if (traces != null) {
			if (traces.booleanValue()) {
				dbgMsg += "; tracing";
			} else {
				dbgMsg += "; stop tracing";
			}
		}

		final Integer tracesSendResultDelay = (Integer) rmiResults.get(XWPostParams.TRACESSENDRESULTDELAY.toString());
		int sDelay = 0;

		if (tracesSendResultDelay != null) {
			sDelay = tracesSendResultDelay.intValue();
			dbgMsg += "; " + tracesSendResultDelay.intValue();
		}

		final Integer tracesResultDelay = (Integer) rmiResults.get(XWPostParams.TRACESRESULTDELAY.toString());
		int rDelay = 0;
		if (tracesResultDelay != null) {
			rDelay = tracesResultDelay.intValue();
			dbgMsg += "; " + tracesResultDelay.intValue();
		}

		logger.debug(dbgMsg);

		if (XWTracer.getInstance() != null) {
			if (traces != null) {
				XWTracer.getInstance().setConfig(traces.booleanValue(), rDelay, sDelay);
			} else {
				XWTracer.getInstance().setConfig(rDelay, sDelay);
			}
		}

		final Integer newAlivePeriod = (Integer) rmiResults.get(XWPostParams.ALIVEPERIOD.toString());
		if (newAlivePeriod != null) {
			alivePeriod = newAlivePeriod.intValue();
			logger.info("Alive period from server = " + alivePeriod);
		}
	}

	/**
	 * This checks the provided job accordingly to the server status
	 *
	 * @param jobUID
	 *            is the UID of the currently computed job
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @see #checkJob(Work)
	 */
	public Hashtable workAlive(final UID jobUID) throws InvalidKeyException, URISyntaxException {

		CommClient commClient = null;
		Hashtable result = null;
		try {
			commClient = commClient();
			result = commClient.workAlive(jobUID).getHashtable();
		} catch (final SAXException | IOException ce) {
			logger.exception(ce);
		} finally {
			if (commClient != null) {
				commClient.close();
			}
			commClient = null;
		}
		return result;
	}

	/**
	 * This synchronizes with the server
	 *
	 * @param rmiParams
	 *            is a Hashtable containing the list of local results
	 * @see #synchronize()
	 */
	public Hashtable workAlive(final Hashtable rmiParams) throws InterruptedException {

		CommClient commClient = null;
		Hashtable result = null;
		try {
			config.getHost().setAvailable(ThreadLaunch.getInstance().available());
			commClient = commClient();
			result = commClient.workAlive(rmiParams).getHashtable();
		} catch (final RemoteException ce) {
			logger.exception(ce);
		} catch (final Exception e) {
			logger.exception(e);
		} finally {
			if (commClient != null) {
				commClient.close();
			}
			commClient = null;
		}
		return result;
	}

	/**
	 * This pings the server
	 *
	 * @see #synchronize()
	 */
	public void ping() {

		CommClient commClient = null;
		try {
			final long start = System.currentTimeMillis();
			commClient = commClient();
			commClient().ping();
			final long end = System.currentTimeMillis();
			final int pingdelai = (int) (end - start);
			logger.info("Ping = " + pingdelai + " ms");
			config.getHost().incAvgPing(pingdelai);
		} catch (final Exception e) {
			logger.exception(e);
		} finally {
			if (commClient != null) {
				commClient.close();
			}
			commClient = null;
		}
	}

	/**
	 * This retreives the default comm client and initializes it
	 *
	 * @return the default comm client
	 */
	private CommClient commClient() throws RemoteException, UnknownHostException, ConnectException {

		CommClient commClient = null;
		try {
			commClient = config.defaultCommClient();
			commClient.setAutoClose(true);
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
		return commClient;
	}

}
