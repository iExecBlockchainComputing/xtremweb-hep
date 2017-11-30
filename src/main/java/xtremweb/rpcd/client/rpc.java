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

package xtremweb.rpcd.client;

/**
 * Date    : Mar 25th, 2005
 * Project : RPCXW / RPCXW-C
 * File    : rpc.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 */

import xtremweb.common.XWConfigurator;
import org.apache.log4j.Logger;


/**
 * This aims to fake sun RPC by interposing between RPC client and sun RPC This
 * reads input bytes from client, send them to sun RPC, wait for call to
 * complete, reads result bytes and send them back to client.
 *
 * This automatically detects RPC ports on first client connection, using
 * portmapper.c jni library.
 *
 * This virtual class must be derived
 *
 */
public abstract class rpc extends Thread {

	private static final Logger logger = Logger.getLogger(rpc.class);

	/**
	 * This is the handler for RPC messages
	 */
	private Callback callback;

	/**
	 * This is the default handler for other programs
	 */
	private Callback piped;

	/**
	 * This is the transfered buffer size
	 */
	private final int BUFSIZE = 8192;

	/**
	 * This is the command line argument
	 */
	private final String[] argv;

	/**
	 * This stores client config such as login, password, server addr etc.
	 */
	protected XWConfigurator config;

	/**
	 * This is the only constructor
	 *
	 * @param n
	 *            is this thread name
	 * @param a
	 *            is the command line
	 * @param c
	 *            is the XtremWeb config
	 */
	protected rpc(final String n, final String[] a, final XWConfigurator c) {

		super(n);
		config = c;

		argv = a.clone();
		setCallback(new Callback(argv, c));
		if (parse(argv) == true) {
			setPiped(new CallbackPipe(argv, c));
		}
	}

	/**
	 * This parses command line arguments
	 */
	private boolean parse(final String[] argv) {

		int i = 0;

		while (i < argv.length) {

			if (argv[i].toLowerCase().compareTo("--pipe") == 0) {
				return true;
			}
			i++;
		}
		return false;
	}

	/**
	 * @return the bUFSIZE
	 */
	public int getBUFSIZE() {
		return BUFSIZE;
	}

	/**
	 * @return the piped
	 */
	public Callback getPiped() {
		return piped;
	}

	/**
	 * @param piped
	 *            the piped to set
	 */
	public void setPiped(final Callback piped) {
		this.piped = piped;
	}

	/**
	 * @return the callback
	 */
	public Callback getCallback() {
		return callback;
	}

	/**
	 * @param callback
	 *            the callback to set
	 */
	public void setCallback(final Callback callback) {
		this.callback = callback;
	}

}// rpc
