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

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.rmi.RemoteException;

import javax.net.ssl.SSLSocket;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import xtremweb.common.BytePacket;
import org.apache.log4j.Logger;

import xtremweb.common.StreamIO;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLable;
import xtremweb.communications.IdRpc;

/**
 * This handles incoming communications through HTTP.<br>
 * This answers request from HTTPClient as well as any web browser
 *
 * Created: Octobre 2007
 *
 * @see xtremweb.communications.TCPClient
 * @author Oleg Lodygensky
 * @version RPCXW
 */

public class HTTPHandler extends xtremweb.worker.CommHandler {

	private StreamIO io;
	private HttpServletRequest request;
	private HttpServletResponse response;

	private final IdRpc idRpc;
	private final UserInterface user;
	private static final Logger logger = Logger.getLogger(HTTPHandler.class);

	/**
	 * This is the default constructor which only calls super("HTTPHandler")
	 */
	public HTTPHandler() {
		super("HTTPHandler");
		user = null;
		idRpc = null;
	}

	/**
	 * This is the default constructor which only calls super("HTTPHandler")
	 */
	public HTTPHandler(final String n) {
		super(n);
		user = null;
		idRpc = null;
	}

	/**
	 * This constructor call the default constructor
	 *
	 * @param socket
	 *            is not used
	 * @see #HTTPHandler()
	 */
	public HTTPHandler(final SSLSocket socket) throws RemoteException {
		this();
	}

	/**
	 * This does nothing
	 */
	@Override
	public void setSocket(final SSLSocket s) throws RemoteException {
	}

	/**
	 * This does nothing
	 */
	@Override
	public void setSocket(final Socket s) throws RemoteException {
	}

	/**
	 * This throws an exception since setPacket() is dedicated to UDP comms
	 *
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramSocket s, final DatagramPacket p) throws RemoteException {
		throw new RemoteException("HTTPHandler#setPacket() HTTP can't set packet");
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramChannel c, final SocketAddress r, final BytePacket p) throws RemoteException {
		throw new RemoteException("HTTPHandler#setPacket() HTTP can't set packet");
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void setServer(final Server server) {
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public Server getServer() {
		return null;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isFailed() {
		return true;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isRunning() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarted() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarting() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isStopped() {
		return true;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStopping() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 */
	@Override
	public void start() {
	}

	/**
	 *
	 */
	@Override
	protected void write(final XMLable cmd) {
		try {
			if (response == null) {
				logger.error("Can't write : this.response is not set");
				return;
			}

			logger.debug("HTTPHandler#write(" + cmd.toXml() + ")");
			response.setContentType("text/xml");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(XMLable.XMLHEADER + cmd.toXml());
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This is not implemented and always throws an IOException
	 */
	@Override
	public void writeFile(final File f) throws IOException {
		throw new IOException("HTTPHandler#writeFile not implemented");
	}

	/**
	 * This is not implemented and always throws an IOException
	 */
	@Override
	public void readFile(final File f) throws IOException {
		throw new IOException("HTTPHandler#readFile not implemented");
	}

	/**
	 * This handles incoming connections. This is inherited from
	 * org.mortbay.jetty.Handler. <br />
	 * This expects a POST parameter : XWPostParams.COMMAND
	 *
	 * @see xtremweb.communications.XWPostParams
	 */
	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest _request,
			final HttpServletResponse _response) throws IOException, ServletException {
	}

	/**
	 * This does nothing since everything is done in HttpClient
	 */
	@Override
	public void run() {
	}

	/**
	 * This cleans and closes communications
	 */
	@Override
	public void close() {
		logger.debug("close");
		io.close();
	}
}
