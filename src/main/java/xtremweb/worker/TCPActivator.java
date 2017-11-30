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

// TCPActivator.java
//
// Created: Mon Apr 22 2002

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import xtremweb.common.XWPropertyDefs;

/**
 * <p>
 * Simple Activator that reads its activity mask from a socket
 * </p>
 *
 * @author Samuel H&eacute;riard
 */

public final class TCPActivator extends Activator implements Runnable {

	private static final Logger logger = Logger.getLogger(TCPActivator.class);
	private final int defaultPort = 4567;

	private int mPort;
	private InetAddress mAddr;
	private boolean mFeedback;

	protected void setListen() throws UnknownHostException {
		final String listen = Worker.getConfig().getProperty("activator.tcp.listen");
		mPort = defaultPort;
		mAddr = InetAddress.getLocalHost();
		if (!((listen == null) || listen.equals(""))) {
			try {
				final int i = listen.indexOf(':');
				if (i < 0) {
					try {
						mPort = Integer.parseInt(listen);
					} catch (final NumberFormatException e) {
						mAddr = InetAddress.getByName(listen);
					}
				} else {

					if ((i > 1) || ((i == 1) && (listen.charAt(0) != '*'))) {
						mAddr = InetAddress.getByName(listen.substring(0, i));
					} else {
						mAddr = null;
					}
					mPort = Integer.parseInt(listen.substring(i + 1, listen.length()));
				}
			} catch (final UnknownHostException e) {
				throw e;
			} catch (final Throwable e) {
				logger.warn("can't parse 'activator.tcp.listen=" + listen + "' " + e);
				mPort = defaultPort;
				mAddr = InetAddress.getLocalHost();
			}
		}
	}

	public TCPActivator() {
		try {
			this.setListen();
			logger.debug("TCPActivator will listen on " + mAddr + ":" + mPort);
			mFeedback = Worker.getConfig().getBoolean(XWPropertyDefs.TCPACTIVATORFEEDBACK);
			if (mFeedback) {
				logger.debug("feedback enable");
			}
		} catch (final Exception e) {
			logger.error("Caught exception, problem while reading config " + e);
		}
	}

	@Override
	public void run() {

		try (ServerSocket servsock = new ServerSocket(mPort, 1, mAddr)) {

			final int soTimeout = 1000;
			while (!Thread.interrupted()) {
				Socket control;
				BufferedReader input;
				PrintWriter output;
				try {
					servsock.setSoTimeout(soTimeout);
					control = servsock.accept();
				} catch (final InterruptedIOException e) {
					continue;
				} catch (final IOException e) {
					logger.error("Caught exception, I/O error" + e);
					Thread.currentThread().interrupt();
					continue;
				}

				try {
					input = new BufferedReader(new InputStreamReader(control.getInputStream()));
					output = null;
					if (mFeedback) {
						output = new PrintWriter(control.getOutputStream());
						output.print("XW TCPActivator\r\n" + getMask() + "> ");
						output.flush();
					}

					for (String line = input.readLine(); line != null; line = input.readLine()) {
						try {
							final int mask = Integer.parseInt(line);
							synchronized (this) {
								setMask(mask);
								notify();
							}
							if (mFeedback) {
								output.print("OK\r\n" + getMask() + "> ");
								output.flush();
							}
						} catch (final NumberFormatException e) {
							if (mFeedback) {
								output.print("ERROR\r\n" + getMask() + "> ");
								output.flush();
							}
						}
					}
				} catch (final IOException e) {
					logger.error("Caught exception: ", e);
					control.close();
				}
			}
		} catch (final IOException ie) {
			logger.error("Caught exception: ", ie);
		}
	}
}
