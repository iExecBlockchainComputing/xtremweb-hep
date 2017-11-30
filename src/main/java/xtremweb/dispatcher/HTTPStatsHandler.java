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

package xtremweb.dispatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import javax.net.ssl.SSLSocket;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import xtremweb.common.BytePacket;
import xtremweb.common.CommonVersion;
import xtremweb.common.DataInterface;
import org.apache.log4j.Logger;

import xtremweb.common.Table;
import xtremweb.common.UserInterface;
import xtremweb.common.Version;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;
import xtremweb.communications.XMLRPCCommand;

/**
 * This handles HTTP request to /stats/
 *
 * Created: August 2005
 *
 * @author Oleg Lodygensky
 * @version RPCXW
 */

public class HTTPStatsHandler extends xtremweb.dispatcher.CommHandler {

	private static final Logger logger = Logger.getLogger(HTTPStatsHandler.class);

	private HttpServletRequest request;
	private HttpServletResponse response;

	private static String RES_SERVERSTATSHTML = "data/serverstats.html";

	private static final Version CURRENTVERSION = CommonVersion.getCurrent();
	private static final String CURRENTVERSIONSTRING = CURRENTVERSION.toString();

	/** this tag is replaced by the current version in misc/server.html */
	private final String TAGVERSION = "@XWVERSION@";

	public static final String PATH = "/stats";

	public static final String NAME = ("HTTPStatsHandler");

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public HTTPStatsHandler() {
		super(NAME);
	}

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public HTTPStatsHandler(final XWConfigurator c) {
		this(NAME, c);
	}

	/**
	 *
	 */
	public HTTPStatsHandler(final String n, final XWConfigurator c) {
		super(n, c);
	}

	/**
	 * This constructor call the previous constructor
	 *
	 * @param socket
	 *            is not used
	 * @see xtremweb.dispatcher.HTTPHandler#HTTPHandler(XWConfigurator)
	 */
	public HTTPStatsHandler(final SSLSocket socket, final XWConfigurator c) throws RemoteException {
		this(c);
	}

	/**
	 * This does nothing
	 */
	@Override
	public void setSocket(final Socket s) throws RemoteException {
	}

	/**
	 * This does nothing
	 */
	@Override
	public void setSocket(final SSLSocket s) throws RemoteException {
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
		throw new RemoteException("HTTPHandler#setPacket() TCP can't set packet");
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramChannel c, final SocketAddress r, final BytePacket p) throws RemoteException {
		throw new RemoteException("HTTPHandler#setPacket() TCP can't set packet");
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
	 * This writes parameter to output channel
	 */
	@Override
	protected synchronized void write(final XMLable answer) throws IOException {
	}

	/**
	 * This writes parameter to output channel
	 */
	protected synchronized <T extends Table> void writeRows(final T row) throws IOException {
	}

	/**
	 * This write file content to output stream
	 *
	 * @param f
	 *            is the file to write
	 */
	@Override
	public synchronized void writeFile(final File f) throws IOException {
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
	 * org.mortbay.jetty.Handler.
	 *
	 * @see xtremweb.communications.XWPostParams
	 */
	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest _request,
			final HttpServletResponse _response) throws IOException, ServletException {

		logger.debug("new connection");

		request = _request;
		response = _response;

		final String path = request.getPathInfo();
		try {
			logger.debug("Handling target         = " + target);
			logger.debug("Handling request        = " + request.getContentLength() + " " + request.getContentType());
			logger.debug("Handling parameter size = " + request.getParameterMap().size());
			logger.debug("Handling query string   = " + request.getQueryString());
			logger.debug("Handling path info      = " + path);
			logger.debug("Handling method         = " + request.getMethod());

			logger.debug("new connection " + path);
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);

			InputStream reader = null;

			reader = getClass().getClassLoader().getResourceAsStream(RES_SERVERSTATSHTML);

			if ((reader != null) && (reader.available() > 0)) {
				String content = new String();
				byte[] buf = new byte[10240];
				for (int n = reader.read(buf); n > 0; n = reader.read(buf)) {
					String ligne = new String(buf, 0, n);
					ligne = ligne.replaceAll(TAGVERSION, CURRENTVERSIONSTRING);
					content += ligne;
					ligne = null;
				}
				buf = null;
				response.getWriter().println(content);
			}

			response.getWriter().flush();

			baseRequest.setHandled(true);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
		response.getWriter().flush();
	}

	/**
	 * This uploads a data to server<br />
	 * Data must be defined on server side (i.e. sendData() must be called
	 * first)
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uid
	 *            is the UID of the data to upload
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Override
	public synchronized long uploadData(final XMLRPCCommand command)
			throws IOException, InvalidKeyException, AccessControlException {
		return 0L;
	}

	/**
	 * This cleans and closes communications
	 */
	@Override
	public void close() {

		mileStone("<close>");

		try {
			request.getInputStream().close();
		} catch (final Exception e) {
		}

		try {
			response.getWriter().flush();
		} catch (final Exception e) {
		}

		try {
			response.getWriter().close();
		} catch (final Exception e) {
		}

		mileStone("</close>");
	}
}
