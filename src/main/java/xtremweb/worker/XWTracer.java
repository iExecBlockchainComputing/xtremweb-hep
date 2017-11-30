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
 * Project : XTremWeb
 * File    : XWTracer.java
 *
 * Initial revision : July 2001
 * By               : Anna Lawer
 *
 * New revision : January 2002
 * By           : Oleg Lodygensky
 * e-mail       : lodygens /at\ lal.in2p3.fr
 */

package xtremweb.worker;

import org.apache.log4j.Logger;

import xtremweb.archdep.ArchDepFactory;
import xtremweb.archdep.XWTracerNative;
import xtremweb.common.TracerZipFile;
import xtremweb.common.WorkerParameters;

/**
 * The <CODE>XWTracer</CODE> class describes the XtremWeb tracer thread. It has
 * a single infinite loop, where it collects system informations thanks to
 * native (hence externals) methods.
 */

public class XWTracer extends Thread {

	private static final Logger logger = Logger.getLogger(XWTracer.class);
	private boolean running = true;
	/** 10 sec */
	private int resultDelay = 1000;
	/** 60 * resultDelay = 10 mn */
	private int sendResultDelay = 60;
	private long dateStart;
	private int runningLoop = 0;
	private static XWTracer instance = null;

	/** This is the default constructor */
	XWTracer(final boolean r) {
		super("XWTracerThreadSend");
		instance = this;
		running = r;
	}

	public static XWTracer getInstance() {
		return instance;
	}

	@Override
	public void run() {

		final XWTracerNative tracerImpl = ArchDepFactory.xwtracer();

		tracerImpl.setOutputDir(Worker.getConfig().getTmpDir().toString());

		logger.info("Tracing");

		dateStart = System.currentTimeMillis();

		try {
			final WorkerParameters params = CommManager.getInstance().getWorkersParameters();
			resultDelay = params.getResultDelay();
			sendResultDelay = params.getSendResultDelay();
		} catch (final Exception ex) {
			logger.error("XWTracer getworkersparams : " + ex);
		}

		while (!Thread.interrupted()) {
			try {

				sleep(resultDelay);

				if (running == false) {
					runningLoop = 0;
					continue;
				}

				if (runningLoop <= 0) {

					dateStart = System.currentTimeMillis();
					tracerImpl.checkNodeState(0);
					tracerImpl.collectNodeConfig(0);
					// Not implemented yet.
				} else {

					tracerImpl.checkNodeState(1);
					tracerImpl.collectNodeConfig(1);
					// Not implemented yet.
				}

				if (runningLoop >= sendResultDelay) {
					sendResult();
				}

				runningLoop++;
			} catch (final InterruptedException e) {
				break;
			}
		}

		logger.info("[XWTracer] terminating");
	}

	public void setConfig(final boolean r, final int rDelay, final int sDelay) {
		if (instance == null) {
			return;
		}

		if ((running && (r == false)) || ((rDelay != -1) && (rDelay != resultDelay))
				|| ((sDelay != -1) && (sDelay != sendResultDelay))) {

			sendResult();
		}

		if (rDelay != -1) {
			resultDelay = rDelay;
		}

		if (sDelay != -1) {
			sendResultDelay = sDelay;
		}

		running = r;
	}

	public void setConfig(final int rDelay, final int sDelay) {

		if (instance == null) {
			return;
		}

		if (((rDelay != -1) && (rDelay != resultDelay)) || ((sDelay != -1) && (sDelay != sendResultDelay))) {
			sendResult();
		}

		if (rDelay != -1) {
			resultDelay = rDelay;
		}

		if (sDelay != -1) {
			sendResultDelay = sDelay;
		}

		logger.debug("setConfig(), sendresultDelay = " + sendResultDelay);
		logger.debug("setConfig(),     resultDelay = " + resultDelay);
	}

	private void sendResult() {
		if (instance == null) {
			return;
		}

		final long dateEnd = System.currentTimeMillis();

		runningLoop = -1;

		try {
			final String fName = Worker.getConfig().getTmpDir() + "/sta.zip";

			final TracerZipFile z = new TracerZipFile(fName, Worker.getConfig().getHost(), resultDelay,
					sendResultDelay);

			try {
				/*
				 * try to insert Unix traces
				 */

				z.addEntry("mask", Worker.getConfig().getTmpDir() + "/mask");
				z.addEntry("state", Worker.getConfig().getTmpDir() + "/sta");
				z.addEntry("config", Worker.getConfig().getTmpDir() + "/config");
				z.addEntry("console", Worker.getConfig().getTmpDir() + "/console");
			} catch (final Exception e) {
				try {
					/*
					 * try to insert Win32 traces
					 */
					z.addEntry("", Worker.getConfig().getTmpDir() + "");
				} catch (final Exception e1) {
					/*
					 * try to insert other traces (?)
					 */
					throw new Exception("zip traces err");
				}
			}

			z.close();

			XWTracerThreadSend send;
			send = new XWTracerThreadSend(fName, dateStart, dateEnd);
			send.setDaemon(true);
			send.start();
		} catch (final Exception e) {
			logger.error("can't send trace results");
		}

	}

}
