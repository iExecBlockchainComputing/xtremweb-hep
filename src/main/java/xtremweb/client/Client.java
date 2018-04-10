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

package xtremweb.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.net.ssl.SSLHandshakeException;

import org.xml.sax.SAXException;

import xtremweb.common.*;
import xtremweb.communications.*;
import xtremweb.security.XWAccessRights;

/**
 * Created: Oct 1st, 2003<br />
 *
 * This class describes a generic client to XtremWeb. It is designed to submit,
 * delete jobs as to get job status and results. <br />
 * <br />
 * Examples can be found in CommandLineParser.
 *
 * @see xtremweb.common.CommandLineParser
 *
 * @author <a href="mailto:lodygens a lal.in2p3.fr">Oleg Lodygensky</a>
 */

public final class Client {
	/**
	 * This aims to display some time stamps
	 */
	private MileStone mileStone;

	/**
	 * This hashtable stores comm clients for each schema
	 */
	private final HashMap<String, CommClient> commClients;

	/**
	 * This is the output stream
	 *
	 * @since 7.0.0
	 */
	private PrintStream out;

	/**
	 * This is the logger
	 *
	 * @since 7.0.0
	 */
	private final Logger logger;
	/**
	 * This stores and tests the command line parameters
	 */
	private CommandLineParser args;

	/**
	 * This stores client config such as login, password, server addr etc.
	 */
	private XWConfigurator config;

	/**
	 * This stores the macro file line number.
	 */
	private int macroLineNumber;
	/**
	 * This is the macro file provided using --xwmacro para
	 */
	private File macroFile;

	/**
	 * This is set to true if a macro file is provided using --xwmacro param
	 */
	private boolean executingMacro;
	/**
	 * This is set to true if --xwshell param has been provided. The client then
	 * opens a socket and waits for incoming connection
	 *
	 * @since 7.0.0
	 */
	private boolean shellRunning;

	/**
	 * This is the zip file
	 */
	private final Zipper zipper;
	/**
	 * Don't forget to remove temp ZIP file when submitting job
	 */
	private boolean newZip = false;


	/**
	 * This is the default constructor
	 */
	private Client(final String[] a) throws ParseException {
		final String[] argv = a.clone();

		logger = new Logger(this);
		out = System.out;
		executingMacro = false;
		shellRunning = false;
		config = null;
		macroLineNumber = 0;
		macroFile = null;
		zipper = new Zipper();
		commClients = new HashMap<>();

		args = new CommandLineParser(argv);

		if (args.help() || ((args.command() == IdRpc.NULL) && (args.getOption(CommandLineOptions.GUI) == null)
				&& (args.getOption(CommandLineOptions.MACRO) == null))) {
			usage(args.command());
		}
		try {
			config = (XWConfigurator) args.getOption(CommandLineOptions.CONFIG);
			setLoggerLevel(config.getLoggerLevel());
		} catch (final NullPointerException e) {
			logger.exception(e);
			if (args.getOption(CommandLineOptions.GUI) == null) {
				logger.fatal("You must provide a config file, using \"--xwconfig\" !");
			} else {
				new MileStone(XWTools.split(""));
				mileStone = new MileStone(Client.class);
			}
		}
	}
	/**
	 * This is needed to call this class methods from GUI
	 */
	public void setArguments(final CommandLineParser a) {
		args = a;
	}

	public XWConfigurator getConfig() {
		return config;
	}

	public void setConfig(final XWConfigurator c) {
		config = c;
	}
	/**
	 * This retrieves and initializes the default communication client
	 *
	 * @return the default communication client
	 * @exception IOException
	 *                is thrown if cache directory can not be created or if we
	 *                can't retrieve the default client
	 * @throws InstantiationException
	 */
	public CommClient commClient() throws IOException, InstantiationException {

		CommClient client = commClients.get(Connection.xwScheme());
		if (client == null) {
			client = config.defaultCommClient();
		}

		client.setLoggerLevel(logger.getLoggerLevel());

		commClients.put(Connection.xwScheme(), client);

		client.setAutoClose(false);
		return client;
	}

	/**
	 * This retrieves the comm client for the givenU RI and initializes it
	 *
	 * @param uri
	 *            is the uri to retrieve comm client for
	 * @return the expected comm client
	 * @throws IOException
	 */
	public CommClient commClient(final URI uri) throws IOException {

		CommClient client = commClients.get(uri.getScheme());

		if (client == null) {
			try {
				client = config.getCommClient(uri);
			} catch (final InstantiationException e) {
				logger.exception(e);
				throw new IOException("can't get client for " + uri);
			}
		}

		logger.debug("commClients.put(" + uri.getScheme() + ")");
		commClients.put(uri.getScheme(), client);

		client.setAutoClose(false);
		return client;
	}

	/**
	 * This retrieves the data for the given URI by calling getData(uri, false)
	 *
	 * @see #getData(URI)URI, boolean)
	 * @param uri
	 *            is the data uri
	 * @return the data
	 * @throws InstantiationException
	 * @throws AccessControlException
	 */
	private DataInterface getData(final URI uri) throws IOException, ClassNotFoundException, SAXException,
	InvalidKeyException, URISyntaxException, AccessControlException, InstantiationException {
		return getData(uri, false);
	}

	/**
	 * This retrieves the data for the given URI
	 *
	 * @param uri
	 *            is the URI of the data to retrieve
	 * @param display
	 *            tells to write data description to stdout
	 * @return the found data
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 *             if data is not found
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws URISyntaxException
	 * @throws InstantiationException
	 */
	private DataInterface getData(final URI uri, final boolean display) throws IOException, InvalidKeyException,
	AccessControlException, ClassNotFoundException, SAXException, URISyntaxException, InstantiationException {

		final XMLRPCCommandGet cmd = new XMLRPCCommandGet(uri);
		final DataInterface data = (DataInterface) sendCommand(cmd, display);
		if (data != null) {
			return data;
		}
		final CommClient commClient = commClient(uri);
		final DataInterface data2 = (DataInterface) commClient.get(uri);

		if (data2 == null) {
			throw new ClassNotFoundException("can't retrieve data " + uri);
		}
		return data2;
	}

	/**
	 * This retrieves the data from server This does nothing if uri parameter is
	 * null.
	 *
	 * @param uri
	 *            is the data uri
	 * @param download
	 *            if false, data content is not downloaded if data already in
	 *            cache; if true, data content is downloaded even if data
	 *            already in cache
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws MalformedURLException
	 * @throws UnknownHostException
	 * @throws ConnectException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws InvalidKeyException
	 * @throws AccessControlException
	 * @throws NoSuchAlgorithmException 
	 */
	private void downloadData(final URI uri, final boolean download)
			throws ConnectException, UnknownHostException, MalformedURLException, ClassNotFoundException, IOException,
			InvalidKeyException, SAXException, URISyntaxException,
			AccessControlException, InstantiationException, NoSuchAlgorithmException,
			XWCommException{

		if (uri == null) {
			return;
		}
		if (uri.isHttp()) {
			wget(uri, new URL(uri.toString()));
			return;
		}

		// insure data is in cache
		final DataInterface data = getData(uri);

		if (!download) {
			return;
		}

		if (data.getShasum() == null) {
			throw new IOException(uri.toString() + " SHASUM is not set");
		}

		final CommClient commClient = commClient(uri);

		final UID uid = uri.getUID();

		String fext = "";
		if (data.getType() != null) {
			fext = data.getType().getFileExtension();
		}
		String dataName = data.getName();
		String fname = uid.toString();

		if (dataName != null) {
			dataName = dataName.replace('/', '_');
			dataName = dataName.replace('\\', '_');
			fname = uid.toString() + "_" + dataName;
		}
		fname += fext;
		File fpath = null;
		if (args.getOption(CommandLineOptions.OUT) != null) {
			final String path = (String) args.getOption(CommandLineOptions.OUT);
			fpath = new File(path);
			if (!fpath.isDirectory()) {
				fpath = null;
				fname = path;
			}
		}
		final File fdata = new File(fpath, fname);
		logger.debug("Download uri = " + uri + " fdata = " + fdata);
		commClient.downloadData(uri, fdata);
		if (data.getShasum().compareTo(XWTools.sha256CheckSum(fdata)) != 0) {
			fdata.delete();
			throw new IOException(uri.toString() + " SHASUM differs : " + data.getShasum() + " != " + XWTools.sha256CheckSum(fdata));
		}
		logger.info("Downloaded to : " + fdata.getAbsolutePath());
	}

	/**
	 * This retrieves the data from an HTTP server Depending on download
	 * parameter, this downloads the data content, if not already in cache
	 *
	 * @param uri
	 *            is the data uri
	 * @param url
	 *            is the data url
	 * @throws IOException
	 *             on cache error
	 */
	private void wget(final URI uri, final URL url) throws IOException, XWCommException {

		if (url == null) {
			return;
		}
		final CommClient commClient = commClient(uri);
		commClient.addToCache(uri);
		final String fname = url.getPath().replace('/', '_').replace(' ', '_');
		final File fdata = new File(fname);

		try (final StreamIO io = new StreamIO(null, new DataInputStream(url.openStream()), false)) {
			mileStone.println("Reading file " + fdata);

			io.readFileContent(fdata);
			io.close();
			mileStone.println("Read file " + fdata);
		} catch (final IOException e) {
			logger.exception(e);
			throw (e);
		}
	}

	/**
	 * This sets the logger level. This also sets the logger levels checkboxes
	 * menu item.
	 */
	public void setLoggerLevel(final LoggerLevel l) {
		logger.setLoggerLevel(l);
		if (zipper != null) {
			zipper.setLoggerLevel(l);
		}
	}

	/**
	 * This shows application usage
	 */
	private void usage(final IdRpc idrpc) {
		usage(idrpc, null);
	}

	/**
	 * This shows application usage
	 *
	 * @see xtremweb.common.CommandLineOptions#usage()
	 */
	private void usage(final IdRpc idrpc, final String msg) {

		if ((idrpc == null) || (idrpc == IdRpc.NULL)) {
			CommandLineParser.usage("This is the XWHEP client to use and manage the platform ("
					+ Version.currentVersion.full() + ")");
		} else {
			println("+--------- XWHEP client " + Version.currentVersion.full()
					+ "  ------------------------------------------+");
			if (msg != null) {
				println("  Error :  " + msg);
			}
			println("  Usage :  " + idrpc.helpClient());
			println("+-----------------------------------------------------------------------------+\n");
		}

		if (shellRunning == false) {
			exit("", XWReturnCode.PARSING);
		}
	}

	/**
	 * This summarizes action details to stdout
	 *
	 */
	private void verbose() {
		println(args.isVerbose(), "server = " + config.getCurrentDispatcher());
		println(args.isVerbose(), "login  = " + config.getUser().getLogin());
		args.verbose();
	}

	/**
	 * This sets the output stream
	 *
	 * @since 7.0.0
	 */
	public void setPrintStream(final PrintStream p) {
		out = p;
	}

	/**
	 * This prints out a string, if expected.<br>
	 * Output is formated accordingly to <code>args.outputFormat</code>.
	 *
	 * @see #args
	 * @param write
	 *            tells whether to write or not
	 * @param str
	 *            is the string to eventually print out
	 */
	private void println(final boolean write, final String str) {
		if (write == false) {
			return;
		}
		if (!args.html()) {
			out.println(str);
			return;
		}

		final Collection<String> array = XWTools.split(str, "\t ,;");

		for (final Iterator<String> iter = array.iterator(); iter.hasNext();) {
			out.println("<td align=\"center\">" + iter.next() + "</td>");
		}
		array.clear();
	}

	/**
	 * This prints out a string if allowed(--verbose option)
	 *
	 * @param str
	 *            is the string to eventually print out
	 */
	private void println(final String str) {
		println(true, str);
	}

	/**
	 * This prints out a string if allowed(--verbose option)
	 *
	 */
	private void println(final URI uri) {
		println(true, uri.toString());
	}

	/**
	 * This prints a message to std err and exits.
	 *
	 * @param msg
	 *            is the message to print to stderr
	 * @param code
	 *            is the return code to use on exit
	 */
	private void exit(final String msg, final XWReturnCode code) {

		final String zFileName = zipper != null ? zipper.getFileName() : null;
		if (newZip && (args.getOption(CommandLineOptions.KEEPZIP) == null) && (zFileName != null)) {
			final File file = new File(zFileName);
			if (file.exists()) {
				file.delete();
			}
		}

		if (code != XWReturnCode.SUCCESS) {
			logger.error(msg);
		} else {
			logger.info(msg);
		}
		if (shellRunning) {
			println(msg);
		}
		try {
			final CommClient client = commClient();
			client.setAutoClose(true);
			commClient().close();
		} catch (final Exception e) {
			logger.exception(e);
		}
		if (shellRunning == false) {
			System.exit(code.ordinal());
		}
	}

	/**
	 * This is called on communication error This may occur if requested object
	 * if not found This terminates this application.
	 *
	 * @since 5.8.0
	 */
	private void objectNotFound() {
		if (executingMacro == false) {
			exit("object not found", XWReturnCode.NOTFOUND);
		}
	}

	/**
	 * This is called on communication error This may occur if user don't hae
	 * the right to execute the command, or if requested object if not found
	 * This terminates this application.
	 */
	private void connectionRefused() {
		exit("connection refused", XWReturnCode.CONNECTION);
	}

	/**
	 * This is called when XtremWeb server is not reachable. This terminates
	 * this application.
	 */
	private void handshakeError() {
		exit("Encryption error : bad public key", XWReturnCode.HANDSHAKE);
	}

	/**
	 * This tells whether XMLable.XMLHEADER has been printed
	 */
	private boolean xmlHeaderPrinted = false;

	/**
	 * This prints XMLable.XMLHEADER to stdout if not already printed
	 */
	private void printXMLHeader() {
		if (xmlHeaderPrinted == false) {
			println(XMLable.XMLHEADER);
			xmlHeaderPrinted = true;
		}
	}

	/**
	 * This inserts an HTML header This displays nothing on any other output
	 * format. Header is only inserted if there is no macro or if force is true
	 *
	 */
	private void header(final IdRpc cmd) {

		if (args.xml()) {
			printXMLHeader();
			println(cmd.toXml());
		}

		if (args.html() == false) {
			return;
		}
		final Date currentDate = new Date();

		println("<html><head><title>XWHEP Client " + cmd.toString() + "(" + currentDate + ")"
				+ "</title></head><body><center>");

		println("<h1>XWHEP Client " + cmd.toString() + "(" + currentDate + ")" + "</h1><br><table border='1'>");
	}

	/**
	 * This inserts a trailer, depending on output format This displays nothing
	 * on any other output format. Trailer is only inserted if there is no macro
	 * or if force is true
	 *
	 */
	private void trailer(final IdRpc cmd) {

		if (args.xml()) {
			println(cmd.toXml(true));
		}
		if (args.html()) {
			println("</table></center></body></html>");
		}
	}

	/**
	 * This writes out an HTML and XML tag. Does nothing for any other format.
	 *
	 * @param t
	 *            is the ML tag
	 * @param open
	 *            is true to open the tags, false to close it
	 */
	private void tag(final boolean open, final String t) {

		if (args.html() || args.xml()) {
			if ((t != null) && (t.length() > 0)) {
				println(true, "<" + (open == true ? "" : "/") + t + ">");
			}
		}
	}

	/**
	 * This starts a new line, depending on output format. For HTML format, this
	 * opens a TR tag; for XML format, this opens the provided tag. This
	 * displays nothing on any other output format.
	 *
	 * @param t
	 *            is the XML tag to open, if any
	 */
	private void startLine(final String t) {
		String str = t;
		if (args.html()) {
			str = "tr";
		}
		if (args.html() || args.xml()) {
			tag(true, str);
		}
	}

	/**
	 * This opens a new HTML table row. This does nothing if output format is
	 * not HTML.
	 */
	private void startLine() {
		startLine(null);
	}

	/**
	 * This ends a line, depending on output format. For HTML format, this
	 * closes a TR tag; for XML format, this closes the provided tag. This
	 * displays nothing on any other output format.
	 *
	 * @param t
	 *            is the XML tag to open
	 */
	private void endLine(final String t) {
		String str = t;
		if (args.html()) {
			str = "tr";
		}
		if (args.html() || args.xml()) {
			tag(false, str);
		}
	}

	/**
	 * This closes an HTML table row. This does nothing if output format is not
	 * HTML.
	 */
	private void endLine() {
		endLine(null);
	}

	/**
	 * This sends command to XtremWeb server, read answer and eventually write
	 * retrieved object to stdout
	 *
	 * @param command
	 *            is the XMLRPC command to send
	 * @param display
	 *            tells to write downloaded object to terminal
	 * @return an XMLable object or null
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public XMLable sendCommand(final XMLRPCCommand command, final boolean display) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException, SAXException, InstantiationException {

		final XMLable result = command.exec(commClient());

		if (display) {
			startLine();

			if (args.xml()) {
				println(true, result.toXml());
			} else {
				println(true, result.toHexString(args.csv() || args.html(), args.shortFormat()));
			}
			endLine();
		}

		return result;
	}

	/**
	 * This changes access rights <blockquote> Command line parameters :
	 * --xwchmod MOD UID | URI [UID | URI ...] </blockquote> MOD follows unix
	 * chmod syntax :
	 * <ul>
	 * <li>[aoug][+-][rwx]
	 * <li>an octal value (e.g. 777)
	 * </ul>
	 * URI | UID may represent any objects
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void chmod() throws IOException, ClassNotFoundException, ParseException, SAXException, URISyntaxException,
	InvalidKeyException, AccessControlException, InstantiationException, IllegalAccessException {

		final Collection params = (Collection) args.commandParams();
		if (params == null) {
			XWAccessRights.help(new PrintStream(System.out));
			exit("no UID provided", XWReturnCode.PARSING);
		}

		final Iterator enumParams = params.iterator();
		final String modstr = (String) enumParams.next();

		while (enumParams.hasNext()) {
			final Object param = enumParams.next();
			URI uri = null;
			if (param instanceof UID) {
				uri = commClient().newURI((UID) param);
			}
			if (param instanceof URI) {
				uri = (URI) param;
			}
			if (uri == null) {
				throw new ParseException("uri is null", 0);
			}

			final Table obj = get(uri, false, true);
			if (obj != null) {
				final XWAccessRights rights = obj.getAccessRights();
				rights.chmod(modstr);
				obj.setAccessRights(rights);
//				final XMLRPCCommandSend cmd = new XMLRPCCommandSend(uri, obj);
				final XMLRPCCommandSend cmd = XMLRPCCommandSend.newCommand(uri, obj);
				commClient().send(cmd);
			}
		}

		params.clear();
	}

	/**
	 * This retrieves command line parameter and calls get(Vector) <blockquote>
	 * Command line parameters : --xwget URI | UID [ URI | UID...] </blockquote>
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void get() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
	InvalidKeyException, AccessControlException, InstantiationException, XWCommException {

		final Collection params = (Collection) args.commandParams();
		get(params, true);
		params.clear();
	}

	/**
	 * This retrieves objects from server by calling get(URI, display, false)
	 * for each Vector element
	 *
	 * @param params
	 *            is a Vector of UID/URI
	 * @param display
	 *            tells to print found object or not
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @see #get(URI, boolean, boolean)
	 */
	private void get(final Collection params, final boolean display) throws InvalidKeyException, AccessControlException,
			IOException, SAXException, URISyntaxException, ClassNotFoundException, InstantiationException,
			XWCommException {

		if (params == null) {
			throw new IOException("no URI provided");
		}
		header(IdRpc.GET);
		boolean headoneshot = true;

		boolean bypass = (args.command() == IdRpc.GETWORKS) || (args.command() == IdRpc.GETTASKS)
				|| (args.command() == IdRpc.GETHOSTS);

		if (args.command() == IdRpc.GET) {
			bypass = true;
		}

		final ArrayList<UID> replicatedWorkUIDs = new ArrayList<>();
		final Iterator theEnum = params.iterator();
		while (theEnum.hasNext()) {
			final Object param = theEnum.next();
			Table obj = null;

			try {
				obj = get((URI) param, display, bypass);
			} catch (final ClassCastException cce) {
				// maybe it is an UID...
				try {
					obj = get((UID) param, display, bypass);
				} catch (final ClassCastException e1) {
					try {
						// this may be the case if param has been retrieved
						// from server
						obj = get((UID) ((XMLValue) param).getValue(), display, bypass);
					} catch (final ClassCastException e3) {
						obj = getApp((String) param);
						if (obj == null) {
							obj = getUser((String) param, false);
						}
						if (obj != null) {
							obj = get(obj.getUID(), display, bypass);
							continue;
						}
					}
				}
			}

			if ((obj == null) || (args.getOption(CommandLineOptions.DOWNLOAD) == null)) {
				continue;
			}

			if (headoneshot && (args.html() || args.csv())) {
				println(true, new AppInterface().getColumns());
				headoneshot = false;
			}

			URI dataUri = null;
			StatusEnum jobStatus = StatusEnum.NONE;
			boolean jobResult = false;
			if (obj instanceof WorkInterface) {
				dataUri = ((WorkInterface) obj).getResult();
				jobStatus = ((WorkInterface) obj).getStatus();
				jobResult = true;
			} else if (obj instanceof DataInterface) {
				dataUri = ((DataInterface) obj).getURI();
				jobStatus = StatusEnum.COMPLETED;
			}

			switch (jobStatus) {
			case COMPLETED:
				if (dataUri != null) {
					try {
						downloadData(dataUri, true);
					} catch (NoSuchAlgorithmException e) {
						logger.exception(e);
					}
				}
				break;
			default:
				break;
			}

			if ((jobResult == false) || (args.getOption(CommandLineOptions.NOERASE) != null)) {
				continue;
			}

			switch (jobStatus) {
			case COMPLETED:
			case FAILED:
			case ERROR:
				if (((WorkInterface) obj).getExpectedReplications() > 0) {
					// we delay replicated work removal to keep a chance to
					// manage all replica results
					replicatedWorkUIDs.add(obj.getUID());
					continue;
				}
				if (replicatedWorkUIDs.contains(((WorkInterface) obj).getReplicatedUid())) {
					// replicas are automatically deleted at replicated work
					// removal, on server side
					continue;
				}

				remove(obj.getUID());
				break;
			default:
				break;
			}
		}

		if (args.getOption(CommandLineOptions.NOERASE) == null) {
			//
			// finally we delete replicated works which also deletes associated
			// replica
			//
			final Iterator<UID> li = replicatedWorkUIDs.iterator();
			while (li.hasNext()) {
				final UID uid = li.next();
				logger.debug("Removing job " + uid);
				remove(uid);
			}
		}
		if (display) {
			trailer(IdRpc.GET);
		}
	}

	/**
	 * This calls get(commClient.newUIR(uid), false, false);
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public Table get(final UID uid) throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
	InvalidKeyException, AccessControlException, InstantiationException {
		final URI uri = commClient().newURI(uid);
		return get(uri, false, false);
	}

	/**
	 * This calls get(uri, false, false);
	 *
	 * @throws SAXException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	public Table get(final URI uri) throws InvalidKeyException, AccessControlException, IOException, SAXException,
	URISyntaxException, InstantiationException {
		return get(uri, false, false);
	}

	/**
	 * This calls get(newUIR(uid), display, bypass);
	 *
	 * @param uid
	 * @param display
	 * @param bypass
	 * @return
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private Table get(final UID uid, final boolean display, final boolean bypass)
			throws URISyntaxException, IOException, InvalidKeyException, AccessControlException, ClassNotFoundException,
			SAXException, InstantiationException {
		final URI uri = commClient().newURI(uid);
		return get(uri, display, bypass);
	}

	/**
	 * This retrieves an object from XtremWeb server This may write description
	 * to stdout accordingly to display parameter <blockquote> Command line
	 * parameters : --xwget UID [UID...] </blockquote>
	 *
	 * @param uri
	 *            is the application URI
	 * @param display
	 *            tells whether to write object description to stdout
	 * @param bypass
	 *            tells to use cache or not; if true cache is not used, if false
	 *            cache is used
	 * @return an AppInterface
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private Table get(final URI uri, final boolean display, final boolean bypass) throws IOException,
	InvalidKeyException, AccessControlException, SAXException, URISyntaxException, InstantiationException {

		final CommClient commClient = commClient();
		Table ret;

		if ((args.command() == IdRpc.GETTASKS) || (args.command() == IdRpc.GETTASK)) {
			ret = commClient.getTask(uri, bypass);
		} else {
			ret = commClient.get(uri, bypass);
		}
		if (ret == null) {
			return null;
		}

		if (display) {
			startLine();
			String str;
			if (args.xml()) {
				str = ret.toXml();
			} else {
				str = ret.toString(args.csv() || args.html(), args.shortFormat());
			}
			println(true, str);
			endLine();
		}
		return ret;
	}

	/**
	 * This retrieves application from XtremWeb server This may write app
	 * description to stdout, accordingly to dislay parameter <blockquote>
	 * Command line parameters : --xwgetapp UID [UID...] </blockquote>
	 *
	 * @param name
	 *            is the application name
	 * @return an AppInterface
	 * @throws InstantiationException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	public AppInterface getApp(final String name) throws InvalidKeyException, AccessControlException, IOException,
	ClassNotFoundException, SAXException, URISyntaxException, InstantiationException {
		return commClient().getApp(name, false);
	}

	/**
	 * This retrieves, stores and displays applications installed in XtremWeb
	 * server. <blockquote> Command line parameters : --xwgetapps </blockquote>
	 *
	 * @throws IllegalAccessException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @see #getApps(boolean)
	 */
	public Collection<XMLValue> getApps() throws InvalidKeyException, AccessControlException, URISyntaxException,
	IOException, InstantiationException, ClassNotFoundException, SAXException, XWCommException {
		return getApps(false);
	}

	/**
	 * This retrieves, stores and displays applications installed in XtremWeb
	 * server. <blockquote> Command line parameters : --xwgetapps </blockquote>
	 *
	 * @param display
	 *            is true to display applications
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws IllegalAccessException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private Collection<XMLValue> getApps(final boolean display)
			throws URISyntaxException, IOException, InstantiationException, InvalidKeyException, AccessControlException,
			ClassNotFoundException, SAXException, XWCommException {

		final URI uri = commClient().newURI();
		final XMLRPCCommandGetApps cmd = new XMLRPCCommandGetApps(uri, config.getUser());
		final XMLVector xmluids = (XMLVector) sendCommand(cmd, false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts/updates an application in server.<br>
	 * This does not read from cache because we need the exact last app
	 * definition in order to correctly update it if it exists. <blockquote>
	 * Command line parameters : --xwsendapp appName cpuType osName binFileName
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws IOException
	 * @throws ParseException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException 
	 */
	private void sendApp() throws IOException, ParseException, InvalidKeyException, AccessControlException,
	URISyntaxException, InstantiationException, ClassNotFoundException, SAXException, NoSuchAlgorithmException {

		final List appParams = (List) args.commandParams();

		AppInterface app;
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);

		if (xmlFile != null) {
			final FileInputStream fis = new FileInputStream(xmlFile);
			app = (AppInterface) Table.newInterface(fis);
			if (app.getName() == null) {
				throw new IOException("application name cannot be null");
			}
			if (app.getUID() == null) {
				app.setUID(new UID());
			}
		} else {
			if (appParams == null) {
				throw new ParseException("no param provided", 0);
			}
			try {
				app = (AppInterface) get((URI) appParams.get(0), false, true);
			} catch (final Exception appe) {
				// maybe it is an UID...
				try {
					app = (AppInterface) get((UID) appParams.get(0), false, true);
				} catch (final ClassCastException e) {
					// maybe it is a string...
                    app = commClient().getApp((String) appParams.get(0), true);
				}
			}
			if (app == null) {
				app = new AppInterface(new UID());
				app.setName((String) appParams.get(0));
			} else {
				// let see if we can modify this app
				commClient().send(app);
			}

			File binaryFile = null;
			URI binaryUri = null;
			CPUEnum cpu = null;
			OSEnum os = null;
			XWAccessRights accessRights = XWAccessRights.DEFAULT;
			AppTypeEnum apptype = null;

			for (int i = 1; i < appParams.size(); i++) {

				logger.debug("params[" + i + "] = " + appParams.get(i).toString());

				if ((binaryFile == null) && (binaryUri == null)) {
					try {
						try {
							binaryUri = (URI) appParams.get(i);
							if (binaryUri.isFile()) {
								binaryFile = new File(binaryUri.getPath());
								throw new IOException(binaryUri.toString() + " is a local file");
							}
						} catch (final Exception e0) {
							binaryUri = null;
							//
							// maybe a local file
							//
							if (binaryFile == null) {
								binaryFile = new File((String) appParams.get(i));
							}
							if (binaryFile.exists()) {
								logger.debug(appParams.get(i).toString() + " is local file");
								continue;
							}
							binaryFile = null;
						}
					} catch (final Exception e) {
						logger.finest(appParams.get(i).toString() + " is not an URI");
						binaryFile = null;
					}
				}

				try {
					accessRights = new XWAccessRights((String) appParams.get(i));
					logger.debug(appParams.get(i).toString() + " are access rights");
					continue;
				} catch (final Exception e) {
					logger.finest(appParams.get(i).toString() + " are not access rights");
				}
				try {
					apptype = (AppTypeEnum) appParams.get(i);
					logger.debug(appParams.get(i).toString() + " is an applicaiton type");
					continue;
				} catch (final Exception e) {
					logger.finest(appParams.get(i).toString() + " is not an application type");
				}
				try {
					if (binaryUri == null) {
						binaryUri = commClient().newURI((UID) appParams.get(i));
						logger.debug(appParams.get(i).toString() + " is an UID");
						continue;
					}
				} catch (final Exception e) {
					logger.finest(appParams.get(i).toString() + " is not an UID");
				}
				try {
					if (cpu == null) {
						cpu = (CPUEnum) appParams.get(i);
						logger.debug(appParams.get(i).toString() + " is a CPU");
						continue;
					}
				} catch (final Exception e) {
					logger.finest(appParams.get(i).toString() + " is not a CPU");
				}
				try {
					if (os == null) {
						os = (OSEnum) appParams.get(i);
						logger.debug(appParams.get(i).toString() + " is an OS");
						continue;
					}
				} catch (final Exception e) {
					logger.finest(appParams.get(i).toString() + " is not an OS");
				}
			}

			if (apptype == null) {
				throw new ParseException("unknown application type", 0);
			}
			if (os == null) {
				logger.info("OS is not set; will not send any binary");
			}
			else {
				if ((cpu == null) && (os != OSEnum.JAVA)) {
					throw new ParseException("CPU is not set", 1);
				}
				if (binaryUri == null) {
					if (binaryFile != null) {
						DataTypeEnum type = DataTypeEnum.getFileType(binaryFile);
						if (type == null) {
							type = DataTypeEnum.BINARY;
						}
						binaryUri = sendData(os, cpu, type, accessRights,
								new URI("file://" + binaryFile.getAbsolutePath()), binaryFile.getName());
					} else {
						logger.debug("no binary found (no URI, no file)");
					}
				}

				logger.debug("uri = " + binaryUri + " os = " + os + " cpu = " + cpu);
				app.setBinary(cpu, os, binaryUri);
			}
			app.setAccessRights(accessRights);
			app.setType(apptype);
		}

		if (args.getOption(CommandLineOptions.ENVVAR) != null) {
			app.setEnvVars((String) args.getOption(CommandLineOptions.ENVVAR));
		}

		commClient().send(app);
		println(commClient().newURI(app.getUID()));
	}

	/**
	 * This removes objects from server.<br>
	 * <blockquote> Command line parameters : --xwremove UID [UID...]
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SAXException
	 */
	private void remove() throws IOException, ParseException, ClassNotFoundException, InvalidKeyException,
	AccessControlException, InstantiationException, IllegalAccessException, SAXException {

		final Collection params = (Collection) args.commandParams();
		if (params == null) {
			exit("no URI provided", XWReturnCode.PARSING);
		}
		final Iterator enumParams = params.iterator();
		while (enumParams.hasNext()) {
			final Object param = enumParams.next();
			try {
				remove((UID) param);
			} catch (final ClassCastException e) {
				remove((URI) param);
			} catch (final InvalidKeyException | AccessControlException e) {
				throw e;
			} catch (final Exception e) {
				throw new IOException(e.getMessage());
			}
		}
		params.clear();
	}

	/**
	 * This removes an object from server.<br>
	 * <blockquote> Command line parameters : --xwremove UID | URI </blockquote>
	 * The user must have the right to do so
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SAXException
	 */
	public void remove(final URI uri) throws IOException, ClassNotFoundException, InvalidKeyException,
	AccessControlException, InstantiationException, SAXException {

		final XMLRPCCommandRemove cmd = new XMLRPCCommandRemove(uri);
		sendCommand(cmd, false);
	}

	/**
	 * This removes an object from server.<br>
	 * <blockquote> Command line parameters : --xwremove UID | URI </blockquote>
	 * The user must have the right to do so
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @throws SAXException
	 * @throws IllegalAccessException
	 */
	public void remove(final UID uid)
			throws IOException, UnknownHostException, ConnectException, ClassNotFoundException, URISyntaxException,
			InvalidKeyException, AccessControlException, InstantiationException, SAXException {
		remove(commClient().newURI(uid));
	}

	/**
	 * This retrieves, stores and displays data installed in XtremWeb server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @see #getDatas(boolean)
	 */
	public Collection<XMLValue> getDatas() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
	InvalidKeyException, AccessControlException, InstantiationException, XWCommException {
		return getDatas(false);
	}

	/**
	 * This retrieves, stores and displays data installed in XtremWeb server.
	 * <blockquote> Command line parameters : --xwdatas </blockquote>
	 *
	 * @param display
	 *            is true to write data descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @see #getDatas()
	 */
	private Collection<XMLValue> getDatas(final boolean display)
			throws IOException, ClassNotFoundException, SAXException, URISyntaxException, InvalidKeyException,
			AccessControlException, InstantiationException, XWCommException {

		final URI uri = commClient().newURI();
		final XMLRPCCommandGetDatas cmd = new XMLRPCCommandGetDatas(uri, config.getUser());
		final XMLVector xmluids = (XMLVector) sendCommand(cmd, false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new data in server.<br>
	 * <blockquote> Command line parameters : --xwsenddata dataName cpuType
	 * osName DataTypeEnum accessRight [binFileName | binFileURI | binFileUID ]
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws IOException
	 *             on I/O error
	 * @throws InvalidKeyException
	 *             on authentication or authorization error (user don't have
	 *             rights to send data)
	 * @throws ParseException
	 *             on command line parameter error
	 * @throws InstantiationException
	 *             if the data URI scheme is unknwon
	 * @throws URISyntaxException
	 *             on URI error
	 * @throws CertificateException
	 * @throws CertificateExpiredException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws NoSuchAlgorithmException 
	 */
	private void sendData() throws IOException, ParseException, InvalidKeyException, URISyntaxException,
	InstantiationException, CertificateExpiredException, CertificateException, AccessControlException,
	ClassNotFoundException, SAXException, NoSuchAlgorithmException {

		File dataFile = null;
		String dataName = null;
		CPUEnum cpu = null;
		OSEnum os = null;
		XWAccessRights accessRights = null;
		DataTypeEnum dataTypeEnum = null;
		URI dataUri = null;
		final UID uid = new UID();

		DataInterface data;
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);

		if (xmlFile != null) {
			final FileInputStream fis = new FileInputStream(xmlFile);
			data = (DataInterface) Table.newInterface(fis);
			if (data.getUID() == null) {
				data.setUID(uid);
			}
			dataName = data.getName();
		} else {
			final List dataParams = (List) args.commandParams();
			if (dataParams == null) {
				throw new ParseException("no param provided", 0);
			}
			for (int i = 0; i < dataParams.size(); i++) {
				logger.debug("params[" + i + "] = " + dataParams.get(i).toString());
				if ((dataFile == null) && (dataUri == null)) {
					try {
						try {
							dataUri = (URI) dataParams.get(i);
							if (dataUri.isFile()) {
								dataFile = new File(dataUri.getPath());
								throw new ClassCastException(dataUri.toString() + " is a local file");
							}
						} catch (final ClassCastException e0) {

							dataUri = null;

							if (dataFile == null) {
								dataFile = new File((String) dataParams.get(i));
							}
							if (!dataFile.exists()) {
								throw new IOException("file not found");
							}
							if (!dataFile.isFile() && !dataFile.isDirectory()) {
								exit("" + dataFile + " is not a regular file", XWReturnCode.FATAL);
							}
							if (dataFile.isHidden()) {
								exit("" + dataFile + " is hidden", XWReturnCode.FATAL);
							}

							continue;
						}
					} catch (final Exception e) {
						logger.debug("not an URI");
						dataFile = null;
					}
				}
				try {
					cpu = (CPUEnum) dataParams.get(i);
					continue;
				} catch (final Exception e) {
					logger.debug("not a CPU");
				}
				try {
					os = (OSEnum) dataParams.get(i);
					continue;
				} catch (final Exception e) {
					logger.debug("not an OS");
				}
				try {
					accessRights = new XWAccessRights((String) dataParams.get(i));
					continue;
				} catch (final Exception e) {
					logger.debug("not an access right");
				}
				try {
					dataTypeEnum = (DataTypeEnum) dataParams.get(i);
					continue;
				} catch (final Exception e) {
					logger.debug("not a data type");
				}

				try {
					dataName = (String) dataParams.get(i);
				} catch (final ClassCastException e) {
				}
			}

			dataParams.clear();

			data = new DataInterface(uid);
			if ((dataUri == null) || (dataUri.isFile())) {
				dataUri = commClient().newURI(data.getUID());
			} else if (dataUri.isXtremWeb() == false) {
				throw new ParseException("You must provide an XML file to insert an URI like " + dataUri.getScheme(),
						0);
			}
			data.setURI(dataUri);

			if ((dataName == null) && (dataFile != null)) {
				dataName = dataFile.getName();
			}
			data.setName(dataName);

			if ((dataTypeEnum == null) && (dataFile != null)) {
				data.setType(xtremweb.common.DataTypeEnum.getFileType(dataFile));
			}

			if (dataTypeEnum != null) {
				data.setType(dataTypeEnum);
				if (dataTypeEnum == xtremweb.common.DataTypeEnum.X509) {
					if (dataFile != null) {
						XWTools.checkCertificate(dataFile);
						accessRights = XWAccessRights.USERREADWRITE;
					}
				}
			}

			if (accessRights != null) {
				data.setAccessRights(accessRights);
			}
			if (dataFile != null) {
				logger.debug("Data file = " + dataFile.toString());
			} else {
				logger.info("no data content");
			}
			if (cpu != null) {
				data.setCpu(cpu);
			}
			if (os != null) {
				data.setOs(os);
			}

			if (args.getOption(CommandLineOptions.EXPECTEDWORK) != null) {
				data.setWork((UID) args.getOption(CommandLineOptions.EXPECTEDWORK));
			}
			if (args.getOption(CommandLineOptions.PACKAGE) != null) {
				data.setPackage((String) args.getOption(CommandLineOptions.PACKAGE));
			}
		}

		if ((dataName != null)
				&& (dataName.toLowerCase().endsWith(xtremweb.common.DataTypeEnum.ZIP.getFileExtension()) == false)) {
			newZip = zip(dataFile);
		}
		if (newZip) {
			data.setType(xtremweb.common.DataTypeEnum.ZIP);
			dataFile = new File(zipper.getFileName());
		}
		sendData(data, dataFile);
		if (newZip && (args.getOption(CommandLineOptions.KEEPZIP) == null)) {
			final File file = new File(zipper.getFileName());
			if (file.exists()) {
				file.delete();
			}
			newZip = false;
		}

		if (dataUri == null) {
			dataUri = commClient().newURI(data.getUID());
		}
		println(dataUri);
	}

	/**
	 * This inserts a new data in server
	 *
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @throws NoSuchAlgorithmException 
	 */
	public URI sendData(final OSEnum os, final CPUEnum cpu, final DataTypeEnum type, final XWAccessRights accessRights,
			final URI uri, final String name) throws IOException, URISyntaxException, InvalidKeyException,
	AccessControlException, ClassNotFoundException, SAXException, InstantiationException, NoSuchAlgorithmException {

		logger.debug(
				"sendData(" + os + ", " + cpu + ", " + type + ", " + accessRights + ", " + uri + ", " + name + ")");

		File dataFile = null;
		final DataInterface data = new DataInterface(new UID());

		data.setType(type);
		data.setOs(os);
		data.setCpu(cpu);
		data.setAccessRights(accessRights);
		data.setURI(commClient().newURI(data.getUID()));

		if (uri != null) {
			if (uri.isFile()) {
				dataFile = new File(uri.getPath());
				data.setName(dataFile.getName());
				final DataTypeEnum thisType = DataTypeEnum.getFileType(dataFile);
				if ((thisType == DataTypeEnum.TEXT) && (type == DataTypeEnum.URIPASSTHROUGH)) {
					data.setType(type);
				}
				if (thisType == DataTypeEnum.X509) {
					data.setAccessRights(XWAccessRights.USERALL);
				}
			}
		}
		return sendData(data, dataFile);
	}

	/**
	 * This calls sendData(data, new File(dataPath))
	 *
	 * @param data
	 *            is the meta data of the data to send
	 * @param dataPath
	 *            is the path to the data content on local fs
	 * @return the data URI
	 * @throws NoSuchAlgorithmException 
	 * @see #sendData(DataInterface, File)
	 */
	public URI sendData(final DataInterface data, final String dataPath)
			throws InvalidKeyException, AccessControlException, IOException, ClassNotFoundException, SAXException,
			URISyntaxException, InstantiationException, NoSuchAlgorithmException {
		final File f = new File(dataPath);
		return sendData(data, f);
	}

	/**
	 * This inserts a new data in server.<br>
	 * <blockquote> Command line parameters : --xwsenddata &lt;data name&gt;
	 * &lt;cpu type&gt; &lt;os name&gt; &lt;bin file name&gt; </blockquote> The
	 * user must have the right to do so
	 *
	 * @return the data URI
	 * @param data
	 *            describe the data to send
	 * @param dataFile
	 *            contains data itself
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @throws NoSuchAlgorithmException 
	 */
	public URI sendData(final DataInterface data, final File dataFile) throws IOException, InvalidKeyException,
	AccessControlException, ClassNotFoundException, SAXException, URISyntaxException, InstantiationException, NoSuchAlgorithmException {

		if (dataFile == null) {
			data.setStatus(StatusEnum.AVAILABLE);
		} else {
			if (!dataFile.exists()) {
				throw new IOException(dataFile.getName() + " does not exist");
			}
			data.setStatus(StatusEnum.UNAVAILABLE);
			data.setSize(dataFile.length());
			data.setShasum(XWTools.sha256CheckSum(dataFile));
		}

		logger.debug("sendData(" + data.toXml() + ", " + dataFile + ")");

		commClient().send(data);
		final URI uri = data.getURI();
		if (dataFile != null) {
			try {
				commClient().uploadData(uri, dataFile);
			} catch (final Exception e) {
				logger.exception("Upload error", e);
				try {
					commClient().setAutoClose(true);
					commClient().close();
					commClient().remove(uri);
				} finally {
					exit("Upload error " + uri, XWReturnCode.DISK);
				}
			}
		}

		return uri;
	}

	/**
	 * This retrieves, stores and displays applications installed in XtremWeb
	 * server. <blockquote> Command line parameters : --xwgetapps </blockquote>
	 *
	 * @param display
	 *            is true to display applications
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws IllegalAccessException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @since 13.0.0
	 */
	private Collection<XMLValue> getEnvelopes(final boolean display)
			throws URISyntaxException, IOException, InstantiationException, InvalidKeyException, AccessControlException,
			ClassNotFoundException, SAXException, XWCommException {

		final URI uri = commClient().newURI();
		final XMLRPCCommandGetEnvelopes cmd = new XMLRPCCommandGetEnvelopes(uri, config.getUser());
		final XMLVector xmluids = (XMLVector) sendCommand(cmd, false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}
	/**
	 * This inserts/updates an application in server.<br>
	 * This does not read from cache because we need the exact last app
	 * definition in order to correctly update it if it exists. <blockquote>
	 * Command line parameters : --xwsendapp appName cpuType osName binFileName
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws IOException
	 * @throws ParseException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws URISyntaxException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @since 13.0.0
	 */
	private void sendEnvelope() throws IOException, ParseException, InvalidKeyException, AccessControlException,
			URISyntaxException, InstantiationException, ClassNotFoundException, SAXException, NoSuchAlgorithmException {

		final List envelopeParams = (List) args.commandParams();

		EnvelopeInterface env;
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);

		if (xmlFile != null) {
			final FileInputStream fis = new FileInputStream(xmlFile);
			env = (EnvelopeInterface) Table.newInterface(fis);
			if (env.getEnvId() <= 0) {
				throw new IOException("envelope id can not be zero");
			}
			if (env.getUID() == null) {
				env.setUID(new UID());
			}
		} else {
            if (envelopeParams == null) {
                throw new ParseException("no param provided", 0);
            }
            env = new EnvelopeInterface(new UID());
            env.setName((String) envelopeParams.get(0));
            env.setMaxCpuSpeed(Integer.valueOf((String)envelopeParams.get(1)));
            env.setMaxWallClockTime(Integer.valueOf((String)envelopeParams.get(2)));
            env.setMaxMemory(Long.valueOf((String)envelopeParams.get(3)));
			env.setMaxFreeMassStorage(Long.valueOf((String)envelopeParams.get(4)));
			env.setMaxFileSize(Long.valueOf((String)envelopeParams.get(5)));

        }
		commClient().send(env);
		println(commClient().newURI(env.getUID()));
	}
	/**
	 * This retrieves, stores and displays groups installed in XtremWeb server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @see #getGroups(boolean)
	 */
	public Collection<XMLValue> getGroups() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
	InvalidKeyException, AccessControlException, InstantiationException, IllegalAccessException, XWCommException {
		return getGroups(false);
	}

	/**
	 * This retrieves, stores and displays groups installed in XtremWeb server.
	 * <blockquote> Command line parameters : --xwgroups </blockquote>
	 *
	 * @param display
	 *            is true to write group descriptions to stdout
	 * @throws URISyntaxException
	 * @throws InstantiationException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 */
	private Collection<XMLValue> getGroups(final boolean display)
			throws InvalidKeyException, AccessControlException, IOException, ClassNotFoundException, SAXException,
			InstantiationException, URISyntaxException, XWCommException {

		final XMLVector xmluids = (XMLVector) sendCommand(
				new XMLRPCCommandGetGroups(commClient().newURI(), config.getUser()), false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new group in server.<br>
	 * <blockquote> Command line parameters : --xwaddgroup &lt;group name&gt;
	 * &lt;cpu type&gt; &lt;os name&gt; &lt;bin file name&gt; </blockquote> The
	 * user must have the right to do so
	 */
	private void sendGroup() throws ParseException, IOException, SAXException, URISyntaxException {

		final List params = (List) args.commandParams();

		GroupInterface group = null;
		final UID uid = new UID();
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);
		if (xmlFile != null) {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				group = (GroupInterface) Table.newInterface(fis);
			} catch (final Exception e) {
				throw new IOException(e);
			}
			if (group.getUID() == null) {
				group.setUID(uid);
			}
			if ((group.getName() == null) || (group.getOwner() == null)) {
				throw new IOException("group and/or group owner can not be null");
			}
		} else {
			group = new GroupInterface(uid);
			if (group.getOwner() == null) {
				group.setOwner(config.getUser().getUID());
			}
			int paramIdx = 0;
			try {
				group.setName((String) params.get(paramIdx++));
			} catch (final Exception e) {
				throw new ParseException("group name not provided", 0);
			}
			try {
				group.setSession((UID) params.get(paramIdx++));
			} catch (final Exception e) {
			}
		}
		try {
			commClient().send(group);
			println(commClient().newURI(uid));
		} catch (final Exception e) {
			throw new IOException(e.getMessage());
		} finally {
			if (params != null) {
				params.clear();
			}
			group = null;
		}
	}

	/**
	 * This retrieves, stores and displays hosts installed in XtremWeb server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getHosts(boolean)
	 */
	public Collection<XMLValue> getHosts() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
	InvalidKeyException, AccessControlException, InstantiationException, XWCommException {
		return getHosts(false);
	}

	/**
	 * This retrieves, stores and displays hosts installed in XtremWeb server.
	 * <blockquote> Command line parameters : --xwhosts </blockquote>
	 *
	 * @param display
	 *            is true to write host descriptions to stdout
	 * @throws URISyntaxException
	 * @throws InstantiationException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws IllegalAccessException
	 * @see #getHosts
	 */
	private Collection<XMLValue> getHosts(final boolean display) throws InvalidKeyException, AccessControlException,
			IOException, ClassNotFoundException, SAXException, URISyntaxException, InstantiationException,
			XWCommException{

		final XMLVector xmluids = (XMLVector) sendCommand(
				new XMLRPCCommandGetHosts(commClient().newURI(), config.getUser()), false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new host in server.<br>
	 * <blockquote> Command line parameters : --xwaddhost &lt;host name&gt;
	 * &lt;cpu type&gt; &lt;os name&gt; &lt;bin file name&gt; </blockquote> The
	 * user must have the right to do so
	 */
	private void sendHost() throws IOException {
		exit("Can't send host", XWReturnCode.FATAL);
	}

	/**
	 * This retrieves, stores and displays sessions installed in XtremWeb
	 * server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getSessions(boolean)
	 */
	public Collection<XMLValue> getSessions() throws IOException, ClassNotFoundException, SAXException,
			URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {
		return getSessions(false);
	}

	/**
	 * This retrieves, stores and displays sessions installed in XtremWeb
	 * server. <blockquote> Command line parameters : --xwsessions </blockquote>
	 *
	 * @param display
	 *            is true to write session descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getSessions
	 */
	private Collection<XMLValue> getSessions(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {

		final XMLVector xmluids = (XMLVector) sendCommand(
				new XMLRPCCommandGetSessions(commClient().newURI(), config.getUser()), false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new session in server.<br>
	 * <blockquote> Command line parameters : --xwaddsession &lt;session
	 * name&gt; &lt;cpu type&gt; &lt;os name&gt; &lt;bin file name&gt;
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private void sendSession() throws ParseException, IOException, ClassNotFoundException, SAXException,
			URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {

		final List params = (List) args.commandParams();
		final UID uid = new UID();
		SessionInterface session = null;
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);

		if (xmlFile != null) {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				session = (SessionInterface) Table.newInterface(fis);
			} catch (final Exception e) {
				throw new IOException(e);
			}
			if (session.getUID() == null) {
				session.setUID(uid);
			}
			if (session.getName() == null) {
				throw new ParseException("session name can not be null", 0);
			}
			if (session.getOwner() == null) {
				throw new ParseException("session client can not be null", 0);
			}
		} else {
			session = new SessionInterface();
			int paramIdx = 0;

			session.setUID(uid);
			try {
				session.setName((String) params.get(paramIdx++));
			} catch (final Exception e) {
				throw new ParseException("session name not provided", 0);
			}
			session.setOwner(config.getUser().getUID());
		}

		commClient().send(session);
		final URI uri = commClient().newURI(session.getUID());
		println(uri);
		params.clear();
		session = null;
	}

	/**
	 * This retrieves registered tasks
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getTasks(boolean)
	 */
	public Collection<XMLValue> getTasks() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException, InstantiationException,
			XWCommException {
		return getTasks(false);
	}

	/**
	 * This retrieves registered tasks <blockquote> Command line parameters :
	 * --xwtasks </blockquote>
	 *
	 * @param display
	 *            is true to write task descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getTasks
	 */
	private Collection<XMLValue> getTasks(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException,
			AccessControlException, InstantiationException, XWCommException {

		final XMLVector xmluids = (XMLVector) sendCommand(
				new XMLRPCCommandGetTasks(commClient().newURI(), config.getUser()), false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This retrieves, stores and displays traces installed in XtremWeb server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getTraces(boolean)
	 */
	public Collection<XMLValue> getTraces() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException, InstantiationException, XWCommException {
		return getTraces(false);
	}

	/**
	 * This retrieves, stores and displays traces installed in XtremWeb server.
	 * <blockquote> Command line parameters : --xwtraces </blockquote>
	 *
	 * @param display
	 *            is true to write trace descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getTraces
	 */
	private Collection<XMLValue> getTraces(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {

		final XMLVector xmluids = (XMLVector) sendCommand(
				new XMLRPCCommandGetTraces(commClient().newURI(), config.getUser()), false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new trace in server.<br>
	 * <blockquote> Command line parameters : --xwaddtrace &lt;trace name&gt;
	 * &lt;cpu type&gt; &lt;os name&gt; &lt;bin file name&gt; </blockquote> The
	 * user must have the right to do so
	 */
	private void sendTrace()  {
		exit("Can't send trace", XWReturnCode.FATAL);
	}

	/**
	 * This retrieves, stores and displays userGroups installed in XtremWeb
	 * server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getUserGroups(boolean)
	 */
	public Collection<XMLValue> getUserGroups() throws IOException, ClassNotFoundException, SAXException,
			URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {
		return getUserGroups(false);
	}

	/**
	 * This retrieves, stores and displays userGroups installed in XtremWeb
	 * server. <blockquote> Command line parameters : --xwuserGroups
	 * </blockquote>
	 *
	 * @param display
	 *            is true to write userGroup descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getUserGroups
	 */
	public Collection<XMLValue> getUserGroups(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {

		final XMLVector xmluids = (XMLVector) sendCommand(
				new XMLRPCCommandGetUserGroups(commClient().newURI(), config.getUser()), false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new userGroup in server.<br>
	 * <blockquote> Command line parameters : --xwsenduserGroup &lt;userGroup
	 * name&gt; &lt;cpu type&gt; &lt;os name&gt; &lt;bin file name&gt;
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private void sendUserGroup() throws ParseException, IOException, ClassNotFoundException, SAXException,
	URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {

		UserGroupInterface group = null;
		UserInterface groupAdmin = null;
		final UID groupUID = new UID();

		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);
		if (xmlFile != null) {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				group = (UserGroupInterface) Table.newInterface(fis);
			} catch (final Exception e) {
				throw new IOException(e.getMessage());
			}
			if (group.getUID() == null) {
				group.setUID(groupUID);
			}
			if (group.getLabel() == null) {
				throw new ParseException("user group label can not be null", 0);
			}
		} else {
			List params = (List) args.commandParams();
			if (params == null) {
				throw new ParseException("no param provided", 0);
			}
			String groupLabel = (String) params.get(0);

			group = new UserGroupInterface(groupUID);
			group.setLabel(groupLabel);
			String adminLogin = null;
			String adminPassword = null;
			String adminEmail = null;
			try {
				adminLogin = (String) params.get(1);
			} catch (final Exception e) {
				throw new ParseException("group admin login not provided", 0);
			}
			try {
				adminPassword = (String) params.get(2);
			} catch (final Exception e) {
				throw new ParseException("group admin password not provided", 0);
			}
			try {
				adminEmail = (String) params.get(3);
			} catch (final Exception e) {
				throw new ParseException("group admin mail not provided", 0);
			}

			UserRightEnum adminRights = UserRightEnum.ADVANCED_USER;
			UID adminUID = new UID();
			groupAdmin = new UserInterface(adminUID);
			groupAdmin.setLogin(adminLogin);
			groupAdmin.setPassword(adminPassword);
			groupAdmin.setEMail(adminEmail);
			groupAdmin.setRights(adminRights);

			sendUser(groupAdmin, false);
			group.setOwner(adminUID);

			params.clear();
			params = null;
			groupLabel = null;
			adminLogin = null;
			adminPassword = null;
			adminEmail = null;
			adminUID = null;
			adminRights = null;
		}
		try {
			commClient().send(group);
			println(commClient().newURI(group.getUID()));
			if (groupAdmin != null) {
				groupAdmin.setGroup(groupUID);
				sendUser(groupAdmin, true, " administrator of group " + group.getLabel());
			}
		} catch (final Exception e) {
			connectionRefused();
		} finally {
			group = null;
			groupAdmin = null;
		}
	}

	/**
	 * This retrieves command line parameter and writes user description
	 *
	 * @return an UserInterface
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private void getUser() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException, InstantiationException,
			XWCommException {
		final Collection params = (Collection) args.commandParams();
		get(params, true);
		params.clear();
	}

	/**
	 * This calls getUser(login, false)
	 *
	 * @param login
	 *            is the user login
	 * @return an UserInterface
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getUser(String, boolean)
	 */
	public UserInterface getUser(final String login) throws IOException, ClassNotFoundException, SAXException,
	URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {
		return getUser(login, false);
	}

	/**
	 * This retrieves user from XtremWeb server This may write description to
	 * stdout, accordingly to dislay parameter
	 *
	 * @param login
	 *            is the use login
	 * @param display
	 *            tells to write descriptio to stdout, or not
	 * @return an UserInterface
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private UserInterface getUser(final String login, final boolean display) throws IOException, ClassNotFoundException,
	SAXException, URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {

		final UserInterface user = commClient().getUser(login, false);

		if (display) {
			startLine();
			final String str = args.xml() ? user.toXml() : user.toString(args.csv() || args.html());
			println(true, str);
			endLine();
		}
		return user;
	}

	/**
	 * This retrieves user from XtremWeb server This may write description to
	 * stdout, accordingly to dislay parameter
	 *
	 * @param extId is the external id
	 * @param display
	 *            tells to write descriptio to stdout, or not
	 * @return an UserInterface
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private WorkInterface getWorkByExternalId(final String extId, final boolean display) throws IOException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {

		final WorkInterface work = (WorkInterface)commClient().getWorkByExternalId(extId);

		if (display) {
			startLine();
			final String str = args.xml() ? work.toXml() : work.toString(args.csv() || args.html());
			println(true, str);
			endLine();
		}
		return work;
	}

	/**
	 * This retrieves, stores and displays users installed in XtremWeb server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getUsers(boolean)
	 */
	public Collection<XMLValue> getUsers() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {
		return getUsers(false);
	}

	/**
	 * This retrieves, stores and displays users installed in XtremWeb server.
	 * <blockquote> Command line parameters : --xwusers </blockquote>
	 *
	 * @param display
	 *            is true to write user descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private Collection<XMLValue> getUsers(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {

		final URI uri = commClient().newURI();
		final XMLRPCCommandGetUsers cmd = new XMLRPCCommandGetUsers(uri, config.getUser());
		final XMLVector xmluids = (XMLVector) sendCommand(cmd, false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This inserts a new user in server.<br>
	 * <blockquote> Command line parameters : --xwsenduser &lt;user login&gt;
	 * &lt;uesr password&gt; &lt;user email&gt; &lt;user rights&gt;
	 * </blockquote> The user must have the right to do so
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 */
	private void sendUser() throws ParseException, IOException, ClassNotFoundException, SAXException,
	URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {

		final UID uid = new UID();
		UserInterface user = null;
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);
		if (xmlFile != null) {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				user = (UserInterface) Table.newInterface(fis);
			} catch (final Exception e) {
				throw new IOException(e);
			}
			if (user.getLogin() == null) {
				throw new ParseException("user login can not be null", 0);
			}
			if (user.getPassword() == null) {
				throw new ParseException("user password can not be null", 0);
			}
		} else {
			final List params = (List) args.commandParams();
			if (params == null) {
				throw new ParseException("no param provided", 0);
			}
			String login = null;
			String password = null;
			String email = null;
			UserRightEnum rights = UserRightEnum.STANDARD_USER;
			URI groupURI = null;

			try {
				login = (String) params.get(0);
			} catch (final Exception e) {
				throw new ParseException("login not provided", 0);
			}
			try {
				password = (String) params.get(1);
			} catch (final Exception e) {
				throw new ParseException("password not provided", 0);
			}
			try {
				email = (String) params.get(2);
			} catch (final Exception e) {
				throw new ParseException("mail not provided", 0);
			}
			int paramindex = 3;
			try {
				rights = UserRightEnum.valueOf(((String) params.get(paramindex)).toUpperCase());
			} catch (final IllegalArgumentException iae) {
				try {
					rights = UserRightEnum.fromInt(Integer.parseInt((String) params.get(paramindex)));
				} catch (final Exception another) {
				}
			} catch (final ArrayIndexOutOfBoundsException aiobe) {
			} catch (final ClassCastException e) {
				paramindex = 2;
			}
			paramindex++;
			try {
				groupURI = (URI) params.get(paramindex);
			} catch (final ClassCastException e) {
				try {
					groupURI = commClient().newURI((UID) params.get(paramindex));
				} catch (final ClassCastException e2) {
					exit("Not a group :" + params.get(paramindex), XWReturnCode.FATAL);
				}
			} catch (final ArrayIndexOutOfBoundsException e) {
			}

			user = new UserInterface(uid);
			user.setLogin(login);
			user.setPassword(password);
			user.setEMail(email);
			user.setRights(rights);
			if (groupURI != null) {
				user.setGroup(groupURI.getUID());
			}
			params.clear();
			password = null;
			email = null;
			rights = null;
			groupURI = null;
		}
		sendUser(user);
		user = null;
	}

	private void sendUser(final UserInterface user) throws ParseException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException {
		sendUser(user, true);
	}

	private void sendUser(final UserInterface user, final boolean display)
			throws ParseException, IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException, InstantiationException {
		final String msg = null;
		sendUser(user, display, msg);
	}

	private void sendUser(final UserInterface user, final boolean display, final String msg)
			throws ParseException, IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException, InstantiationException {

		commClient().send(user);
		if (display == false) {
			return;
		}
		println("# XWHEP client config file");
		if (msg != null) {
			println("# " + msg);
		}
		println("# " + Version.currentVersion.full());
		println("# Role");
		println(XWPropertyDefs.ROLE + "=" + XWRole.CLIENT);
		println("# XWHEP server");
		println(XWPropertyDefs.DISPATCHERS + "=" + config.getCurrentDispatcher());
		println("# Login and password to connect to the server");
		println(XWPropertyDefs.LOGIN + "=" + user.getLogin());
		println(XWPropertyDefs.PASSWORD + "=" + user.getPassword());
		println(XWPropertyDefs.USERUID + "=" + user.getUID());
		println("# logger level");
		println(XWPropertyDefs.LOGGERLEVEL + "=" + LoggerLevel.INFO);
		final String certPath = config.getPath(XWPropertyDefs.SSLKEYSTORE);
		if ((certPath != null) && (new File(certPath)).exists()) {
			println("# keystore");
			println(XWPropertyDefs.SSLKEYSTORE + "=" + certPath);
			println(XWPropertyDefs.SSLKEYPASSPHRASE + "=" + config.getProperty(XWPropertyDefs.SSLKEYPASSPHRASE));
			println(XWPropertyDefs.SSLKEYPASSWORD + "=" + config.getProperty(XWPropertyDefs.SSLKEYPASSWORD));
		}
	}

	/**
	 * This retrieves, stores and displays works installed in XtremWeb server.
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getWorks(boolean)
	 */
	public Collection<XMLValue> getWorks() throws IOException, ClassNotFoundException, SAXException, URISyntaxException,
			InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {
		return getWorks(false);
	}

	/**
	 * This retrieves works from server <blockquote> Command line parameters :
	 * --xwworks </blockquote>
	 *
	 * @param display
	 *            is true to write work descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getWorks
	 */
	private Collection<XMLValue> getWorks(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {

		final StatusEnum status = (StatusEnum) args.getOption(CommandLineOptions.STATUS);
		final XMLRPCCommandGetWorks cmd = new XMLRPCCommandGetWorks(commClient().newURI(), config.getUser());
		cmd.setStatus(status);
		final XMLVector xmluids = (XMLVector) sendCommand(cmd, false);
		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);
		return uids;
	}

	/**
	 * This retrieves works for a given group <blockquote> Command line
	 * parameters : --xwgroupworks </blockquote>
	 *
	 * @param display
	 *            is true to write work descriptions to stdout
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @see #getWorks
	 */
	private Collection<XMLValue> getGroupWorks(final boolean display) throws IOException, ClassNotFoundException,
			SAXException, URISyntaxException, InvalidKeyException, AccessControlException,
			InstantiationException, XWCommException {

		final Collection params = (Collection) args.commandParams();
		if (params == null) {
			usage(IdRpc.GETGROUPWORKS);
		}
		final Iterator theEnum = params.iterator();
		final Object param = theEnum.next();
		GroupInterface group = null;

		try {
			group = (GroupInterface) get((URI) param);
		} catch (final ClassCastException e) {
			group = (GroupInterface) get((UID) param);
		}

		final XMLRPCCommandGetGroupWorks cmd = new XMLRPCCommandGetGroupWorks(commClient().newURI(group.getUID()),
				config.getUser());

		final XMLVector xmluids = (XMLVector) sendCommand(cmd, false);

		final Collection<XMLValue> uids = xmluids.getXmlValues();
		get(uids, display);

		params.clear();

		return uids;
	}

	/**
	 * This calls zip(path.getName() + DataTypeEnum.ZIP.getFileExtension(),
	 * path);
	 *
	 * @param path
	 *            is the path to compress
	 * @see #zip(String, File)
	 * @since 8.0.0
	 * @return true if a new zip file is created; false otherwise
	 */
	private boolean zip(final File path) throws IOException {
		if (path == null) {
			return false;
		}
		return zip(path.getName() + DataTypeEnum.ZIP.getFileExtension(), path);
	}

	/**
	 * This compresses a file or a directory
	 *
	 * @param path
	 *            is the path to compress
	 * @param zFileName
	 *            is the compressed file name
	 * @since 9.1.0
	 */
	private boolean zip(final String zFileName, final File path) throws IOException {

		if (args.getOption(CommandLineOptions.DONTZIP) != null) {
			return false;
		}
		if (path == null) {
			return false;
		}
		mileStone.println("<zip>");

		boolean zipped = true;

		final String[] filesHierarchy = new String[1];
		filesHierarchy[0] = path.getAbsolutePath();

		zipper.setFileName(zFileName);

		if (zipper.zip(filesHierarchy, true) == false) {
			logger.debug(zFileName + " is not zipped; it is kept 'as is'");
			zipped = false;
		}

		mileStone.println("<zipstatus='" + zipped + "' /></zip>");

		return zipped;
	}

	/**
	 * This inserts (submits) a new work in server.<br>
	 * <blockquote> Command line parameters : --xwsendwork application name or
	 * UID [ --xwcert X.509 cert or proxy ] [ --xwenv dirinFile | URI | UID ] [
	 * '<' <stdinFile> ] </blockquote>
	 *
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @throws InstantiationException
	 * @throws NoSuchAlgorithmException 
	 */
	private void sendWork() throws IOException, ParseException, ClassNotFoundException, SAXException,
	URISyntaxException, InvalidKeyException, AccessControlException, InstantiationException, NoSuchAlgorithmException {

		final UID uid = new UID();
		WorkInterface work = null;
		final File xmlFile = (File) args.getOption(CommandLineOptions.XML);

		if (xmlFile != null) {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				work = (WorkInterface) Table.newInterface(fis);
			} catch (final Exception e) {
				throw new IOException(e);
			}
			if (work.getUID() == null) {
				work.setUID(uid);
			}
			try {
				if (get(work.getApplication()) == null) {
					throw new IOException("Can't retrieve application");
				}
			} catch (final Exception e2) {
				throw new IOException(e2);
			}
		} else {
			work = new WorkInterface();
			List params = (List) args.commandParams();
			work.setUID(uid);

			AppInterface app = null;
			try {
				app = (AppInterface) get(((URI) params.get(0)).getUID());
			} catch (final Exception e) {
				try {
					app = (AppInterface) get((UID) params.get(0));
				} catch (final Exception e2) {
					try {
						app = getApp((String) params.get(0));
					} catch (final Exception e3) {
						app = null;
					}
				}
			}
			if (app == null) {
				if (params != null) {
					params.clear();
				}
				params = null;
				throw new ParseException(
						"Can't retrieve application " + (params != null ? params.get(0).toString() : ""), 0);
			}

			work.setApplication(app.getUID());
			app = null;

			try {
				final String lp = (String) args.getOption(CommandLineOptions.LISTENPORT);
				if (lp != null) {
					work.setListenPort(lp);
				}
			} catch (final Exception e) {
				logger.exception("Can't set LISTENPORT", e);
			}

			try {
				final String fa = (String) args.getOption(CommandLineOptions.FORWARDADDRESSES);
				if (fa != null) {
					work.setSmartSocketClient(fa);
				}
			} catch (final Exception e) {
				logger.exception("Can't set FORWARDADDRESSES", e);
			}

			try {
				final Integer replica = (Integer) args.getOption(CommandLineOptions.REPLICA);
				if (replica != null) {
					work.setExpectedReplications(replica.intValue());
				}
			} catch (final Exception e) {
				logger.exception("Can't set REPLICA", e);
			}
			try {
				final Integer rsize = (Integer) args.getOption(CommandLineOptions.REPLICASIZE);
				if (rsize != null) {
					work.setReplicaSetSize(rsize.intValue());
				}
			} catch (final Exception e) {
				logger.exception("Can't set REPLICASIZE", e);
			}

			DataTypeEnum dirinType = null;
			final List envs = (List) args.getOption(CommandLineOptions.ENV);
			if (envs != null) {
				String envPath = null;
				if (envs.size() == 1) {
					final Object envparam = envs.get(0);
					try {
						final URI envuri = (URI) envparam;
						if (envuri.isFile() == false) {
							work.setDirin(envuri);
						} else {
							envPath = envuri.getPath();
						}
					} catch (final ClassCastException e) {
						try {
							final UID envuid = (UID) envparam;
							work.setDirin(commClient().newURI(envuid));
						} catch (final ClassCastException e2) {
							envPath = (String) envparam;
						}
					}
				} else {
					dirinType = DataTypeEnum.URIPASSTHROUGH;
					final String fname = uid.toString() + DataTypeEnum.TEXT.getFileExtension();
					final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
					final Iterator envsenum = envs.iterator();
					while (envsenum.hasNext()) {
						final String envp = envsenum.next().toString();
						if ((envp == null) || (envp.indexOf(Connection.SCHEMESEPARATOR) < 0)) {
							throw new URISyntaxException(envp, "scheme error");
						}
						writer.println(envp);
					}
					writer.close();
					envPath = fname;
				}

				if (envPath != null) {
					final File envFile = new File(envPath);
					zipper.setFileName(envPath);
					if (envPath.toLowerCase().endsWith(DataTypeEnum.ZIP.getFileExtension()) == false) {
						newZip = zip(work.getUID().toString() + DataTypeEnum.ZIP.getFileExtension(), envFile);
					}
					if (newZip) {
						dirinType = DataTypeEnum.ZIP;
					}
					final File zipFile = new File(zipper.getFileName());
					URI zipUri = sendData(OSEnum.NONE, CPUEnum.NONE, dirinType, XWAccessRights.DEFAULT,
							new URI("file://" + zipFile.getAbsolutePath()), zipFile.getName());
					work.setDirin(zipUri);
					if (newZip) {
						final File file = new File(zipper.getFileName());
						if (file.exists()) {
							file.delete();
						}
						newZip = false;
					}
					zipUri = null;
				}
			}

			if (args.getOption(CommandLineOptions.STDIN) != null) {
				File stdin = null;
				try {
					final URI uri = (URI) args.getOption(CommandLineOptions.STDIN);
					if (uri.isFile() == false) {
						work.setStdin((URI) args.getOption(CommandLineOptions.STDIN));
					} else {
						stdin = new File(uri.getPath());
					}
				} catch (final ClassCastException e) {
					try {
						work.setStdin(commClient().newURI((UID) args.getOption(CommandLineOptions.STDIN)));
					} catch (final ClassCastException e2) {
						stdin = new File((String) args.getOption(CommandLineOptions.STDIN));
					}
				}
				if (stdin != null) {
					final URI stdinUri = sendData(OSEnum.NONE, CPUEnum.NONE, DataTypeEnum.TEXT, XWAccessRights.DEFAULT,
							new URI("file://" + stdin.getAbsolutePath()), stdin.getName());
					work.setStdin(stdinUri);
				}
			}

			String cmdLineStr = new String(" ");
			// i = 1 to bypass application name
			for (int i = 1; i < params.size(); i++) {
				cmdLineStr += params.get(i).toString() + " ";
			}

			if (cmdLineStr.indexOf(XWTools.QUOTE) != -1) {
				throw new ParseException("6 dec 2005 : command line cannot have \"" + XWTools.QUOTE
						+ "\" character until further notification", 0);
			}
			work.setCmdLine(cmdLineStr);
			cmdLineStr = null;

			if (args.getOption(CommandLineOptions.ENVVAR) != null) {
				work.setEnvVars((String) args.getOption(CommandLineOptions.ENVVAR));
			}
			if (args.getOption(CommandLineOptions.LABEL) != null) {
				work.setLabel((String) args.getOption(CommandLineOptions.LABEL));
			}
			if (args.getOption(CommandLineOptions.EXPECTEDHOST) != null) {
				work.setExpectedHost((UID) args.getOption(CommandLineOptions.EXPECTEDHOST));
			}
			if (args.getOption(CommandLineOptions.SESSION) != null) {
				work.setSession((UID) args.getOption(CommandLineOptions.SESSION));
			}
			if (args.getOption(CommandLineOptions.GROUP) != null) {
				work.setGroup((UID) args.getOption(CommandLineOptions.GROUP));
			}

			File certFile = null;

			certFile = config.getFile(XWPropertyDefs.X509USERPROXY);

			if (args.getOption(CommandLineOptions.CERT) != null) {

				try {
					URI uri = (URI) args.getOption(CommandLineOptions.CERT);
					if (uri.isFile() == false) {
						work.setUserProxy(uri);
						certFile = null;

						logger.debug("cert uri " + uri);
					} else {
						certFile = new File(uri.getPath());
					}
					uri = null;
				} catch (final ClassCastException e) {
					try {
						work.setUserProxy(commClient().newURI((UID) args.getOption(CommandLineOptions.CERT)));
						certFile = null;

						logger.debug("cert uid " + work.getUserProxy());
					} catch (final ClassCastException e2) {
						certFile = new File((String) args.getOption(CommandLineOptions.CERT));
					}
				}
			}

			if (certFile != null) {
				try {
					XWTools.checkCertificate(certFile);
				} catch (final Exception e) {
					if (args.getOption(CommandLineOptions.CERT) != null) {
						exit("Invalid certificate : " + e, XWReturnCode.FATAL);
					} else {
						logger.info("Will not use $" + XWPropertyDefs.X509USERPROXY + " : " + e);
						certFile = null;
					}
				}
				if (certFile != null) {
					final URI certUri = sendData(OSEnum.NONE, CPUEnum.NONE, DataTypeEnum.X509, XWAccessRights.USERALL,
							new URI("file://" + certFile.getAbsolutePath()), certFile.getName());
					work.setUserProxy(certUri);
				}
			}

			params.clear();
			params = null;
		}

		if (work != null) {
			// data driven
			if (args.getOption(CommandLineOptions.PACKAGE) != null) {
				try {
					work.setDataDriven((URI) args.getOption(CommandLineOptions.PACKAGE));
				} catch (final ClassCastException e) {
					try {
						work.setDataDriven(commClient().newURI((UID) args.getOption(CommandLineOptions.PACKAGE)));
					} catch (final ClassCastException e2) {
						final URI dataDrivenURI = commClient().newURI(new UID());
						final DataInterface dataDriven = new DataInterface();
						dataDriven.setUID(dataDrivenURI.getUID());
						dataDriven.setURI(dataDrivenURI);
						dataDriven.setPackage((String) args.getOption(CommandLineOptions.PACKAGE));
						dataDriven.setWork(work.getUID());
						dataDriven.setStatus(StatusEnum.WAITING);
						commClient().send(dataDriven);

						work.setDataDriven(dataDrivenURI);
					}
				}
			}

			try {
				if ((work.getExpectedReplications() != 0) && (work.getReplicaSetSize() < 1)) {
					logger.warn("Forcing replication set size to 1");
					work.setReplicaSetSize(1);
				}

				commClient().send(work);
				println(commClient().newURI(work.getUID()));
			} catch (final ClassNotFoundException e) {
				try {
					if (work.getStdin() != null) {
						commClient().remove(work.getStdin());
					}
				} catch (final Exception e1) {
				}
				try {
					if (work.getDirin() != null) {
						commClient().remove(work.getDirin());
					}
				} catch (final Exception e2) {
				}
				throw e;
			}
		}

		work = null;
	}

	/**
	 * This pings the server
	 *
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	private void ping() throws InvalidKeyException, AccessControlException, IOException {

		CommClient client;
		try {
			client = commClient();
			while (true) {
				final long start = System.currentTimeMillis();
				client.ping();
				final long end = System.currentTimeMillis();
				final int pingdelai = (int) (end - start);
				println("Ping to " + config.getCurrentDispatcher() + ": time=" + pingdelai + " ms");
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
				}
			}
		} catch (final InstantiationException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * This is for communications testing<br>
	 * <blockquote> Command line parameters : --xwworkrequest </blockquote>
	 */
	private void workRequest() throws IOException {

		try {
			config.getHost().setAcceptBin(true);
			final WorkInterface work = commClient().workRequest(config.getHost());
			println("Work received: " + work.toXml());
		} catch (final Exception e) {
			if (e instanceof SAXException) {
				logger.info("Server gave no work to compute");
			} else {
				logger.exception(e);
			}
		}
	}

	/**
	 * This is for communications testing<br>
	 * <blockquote> Command line parameters : --xwworkalive [UID] </blockquote>
	 */
	private void workAlive() throws IOException {

		final List params = (List) args.commandParams();

		try {
			final Hashtable rmiResults = commClient().workAlive((UID) params.get(0)).getHashtable();
			final Boolean keepWorking = (Boolean) rmiResults.get("keepWorking");
			println("alive returns: " + rmiResults.toString());
			if (keepWorking != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : keepWorking = " + keepWorking);
			}
		} catch (final Exception e) {
			//
			// a NullPointerException is thrown if user gave no parameter
			// on the command line
			// even if there may be other exceptions, we just bypass them
			//

			final Collection jobResults = new Vector();

			println("Alive to " + config.getCurrentDispatcher() + " : jobResults.size() = " + jobResults.size());
			final Hashtable rmiParams = new Hashtable();
			rmiParams.put(XWPostParams.JOBRESULTS.toString(), jobResults);
			jobResults.clear();

			Hashtable rmiResults = null;
			try {
				rmiResults = commClient().workAlive(rmiParams).getHashtable();
			} catch (final Exception e2) {
				logger.exception(e2);
				exit("Alive to " + config.getCurrentDispatcher() + " : connection error " + e2.toString(),
						XWReturnCode.CONNECTION);
				return;
			}

			rmiParams.clear();

			if (rmiResults == null) {
				exit("Alive to " + config.getCurrentDispatcher() + " : rmiResults = null", XWReturnCode.CONNECTION);
				return;
			}

			logger.debug(rmiResults.toString());

			final String serverVersion = (String) rmiResults.get(XWPostParams.CURRENTVERSION.toString());
			logger.debug("serverVersion = " + serverVersion);
			if (serverVersion != null) {
				println("Server version : " + serverVersion);
			}
			final List<UID> finishedTasks = (List<UID>) rmiResults.get(XWPostParams.FINISHEDTASKS.toString());

			if (finishedTasks != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : finishedTasks.size() = "
						+ finishedTasks.size());
				final Iterator<UID> li = finishedTasks.iterator();

				while (li.hasNext()) {
					final UID uid = li.next();
					if (uid != null) {
						println("Alive to " + config.getCurrentDispatcher() + " : finishedTasks = " + uid);
					}
				}
			}

			final List<UID> resultsExpected = (List<UID>) rmiResults.get(XWPostParams.RESULTEXPECTEDS.toString());

			if (resultsExpected != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : resultsExpected.size() = "
						+ resultsExpected.size());

				final Iterator<UID> li = resultsExpected.iterator();

				while (li.hasNext()) {
					final UID uid = li.next();
					if (uid != null) {
						println("Alive to " + config.getCurrentDispatcher() + " : resultExpected = " + uid);
					}
				}
			}

			final String newServer = (String) rmiResults.get(XWPostParams.NEWSERVER.toString());
			if (newServer != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : new server = " + newServer);
			}

			final Boolean traces = (Boolean) rmiResults.get(XWPostParams.TRACES.toString());
			if (traces != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : tracing = " + traces);
			}
			final Integer tracesSendResultDelay = (Integer) rmiResults
					.get(XWPostParams.TRACESSENDRESULTDELAY.toString());

			if (tracesSendResultDelay != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : tracesSendResultDelay = "
						+ tracesSendResultDelay);
			}
			final Integer tracesResultDelay = (Integer) rmiResults.get(XWPostParams.TRACESRESULTDELAY.toString());
			if (tracesResultDelay != null) {
				println("Alive to " + config.getCurrentDispatcher() + " : tracesResultDelay = " + tracesResultDelay);
			}
			final Integer newAlivePeriod = (Integer) rmiResults.get(XWPostParams.ALIVEPERIOD.toString());
			if (newAlivePeriod != null) {
				println("Alive to " + config.getCurrentDispatcher() + ": new alive period = " + newAlivePeriod);
			}

			final String keystoreUriStr = (String) rmiResults.get(XWPostParams.KEYSTOREURI.toString());
			if ((keystoreUriStr != null) && (keystoreUriStr.length() > 0)) {
				try {
					final URI keystoreUri = new URI(keystoreUriStr);
					final File currentKeystoreFile = new File(
							System.getProperty(XWPropertyDefs.JAVAKEYSTORE.toString()));

					final DataInterface newKeystoreData = getData(keystoreUri);
					if (newKeystoreData == null) {
						throw new IOException("Can't retrieve new keystore data " + keystoreUri);
					}
					final String currentKeystoreSHA = XWTools.sha256CheckSum(currentKeystoreFile);

					if (newKeystoreData.getShasum().compareTo(currentKeystoreSHA) != 0) {
						println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
						println("*                                                 *");
						println("*                  ATTENTION                      *");
						println("*  There is a new keystore available from server  *");
						println("*                                                 *");
						println("* You must download " + keystoreUriStr);
						println("* and copy it to " + currentKeystoreFile);
						println("*                                                 *");
						println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
					}
				} catch (final Exception e2) {
					logger.exception("can't check keystore", e2);
				}
			}
		}
		if (params != null) {
			params.clear();
		}
	}

	/**
	 * This retrieves the SmartSockets hub address <blockquote> Command line
	 * parameters : --xwgethubaddr </blockquote>
	 *
	 * @since 8.0.0
	 */
	private String getHubAddr(final boolean display) throws IOException {

		try {
			final Hashtable<String, String> rmiResults = commClient().getHubAddress().getHashtable();
			final String hubAddr = rmiResults.get(Connection.HUBPNAME);
			if (display) {
				println("SmartSockets hub addr = " + (hubAddr == null ? "unknown" : hubAddr));
			}
			return hubAddr;
		} catch (final Exception e) {
			logger.exception("Can't retrieve SmartSocket hub address", e);
			throw new IOException("Can't retrieve SmartSocket hub address");
		}
	}

	/**
	 * This updates a worker in server by setting its status to true or false
	 * <br />
	 * <blockquote> Command line parameters : --xwupdateworkers &lt;on|off&gt;
	 * [uids list] </blockquote> Administrator privileges required
	 */
	private void updateWorkers() throws IOException {

		final Collection uids = (Collection) args.commandParams();

		if (uids == null) {
			throw new IOException("no param provided");
		}
		boolean status;
		final Iterator li = uids.iterator();

		final String onoff = (String) li.next();

		if (onoff == null) {
			throw new IOException("missing valid parameter : on | off");
		}
		if ((onoff.compareTo("on") == 0) || (onoff.compareTo("1") == 0)
				|| (onoff.compareTo(Boolean.toString(true)) == 0) || (onoff.compareTo("yes") == 0)) {
			status = true;
		} else if ((onoff.compareTo("off") == 0) || (onoff.compareTo("0") == 0)
				|| (onoff.compareTo(Boolean.toString(false)) == 0) || (onoff.compareTo("no") == 0)) {
			status = false;
		} else {
			throw new IOException("invalid parameter : " + onoff);
		}

		while (li.hasNext()) {

			final UID uid = (UID) li.next();
			try {
				commClient().activateHost(uid, status);
			} catch (final Exception e) {
				throw new IOException(e.getMessage());
			}
		}
		uids.clear();
	}

	/**
	 * This executes requested actions.<br>
	 * It first tries to connect to the XtremWeb dispatcher.
	 */
	private void execute() throws IOException {

		try {
			mileStone = new MileStone(getClass());
		} catch (final Exception e) {
			logger.exception(e);
			if (args.getOption(CommandLineOptions.GUI) == null) {
				exit("Can't init comm :  " + e, XWReturnCode.FATAL);
			}
		}

		if (config != null) {
			try {
				final boolean challenge = config.getChallenging();
				final String pass = config.getUser().getPassword();
				final String certificate = config.getUser().getCertificate();

				logger.finest("Client#execute temporarly removing mandate to get client info");
				final String savmandating = config.getMandate();
				config.setMandate("");
				config.setUser(getUser(config.getUser().getLogin()));
				logger.finest("Client#execute restoring mandate " + savmandating);
				config.setMandate(savmandating);

				config.getUser().setPassword(pass);
				config.getUser().setCertificate(certificate);
				config.setProperty(XWPropertyDefs.USERUID, config.getUser().getUID());
				config.setChallenging(challenge);
				config.getUser().setChallenging(challenge);
				CommClient.setConfig(config);
			} catch (final SSLHandshakeException e) {
				logger.exception(e);
				handshakeError();
			} catch (final InvalidKeyException e) {
				logger.exception(e);
				exit(e.getMessage(), XWReturnCode.AUTHENTICATION);
			} catch (final AccessControlException e) {
				logger.exception(e);
				exit(e.getMessage(), XWReturnCode.AUTHORIZATION);
			} catch (final IOException e) {
				logger.exception(e);
				if (args.getOption(CommandLineOptions.GUI) == null) {
					connectionRefused();
				}
			} catch (final SAXException e) {
				connectionRefused();
			} catch (final Exception e) {
				logger.exception(e);
			}
		}

		if (args.getOption(CommandLineOptions.GUI) != null) {
			if (config == null) {
				config = new XWConfigurator();
				CommClient.setConfig(config);
			}
			final xtremweb.client.gui.MainFrame frame = new xtremweb.client.gui.MainFrame(this);
			frame.pack();
			frame.setVisible(true);
		} else {
			try {
				if (args.getOption(CommandLineOptions.SHELL) != null) {
					execShell();
				}
				if (args.getOption(CommandLineOptions.SMARTSOCKETSPROXY) != null) {
					smartSocketsProxy();
				} else if (args.getOption(CommandLineOptions.MACRO) != null) {
					execMacros();
				} else {
					doItNow();
				}
				final CommClient client = commClient();
				client.setAutoClose(true);
				client.disconnect();
			} catch (final SSLHandshakeException e) {
				handshakeError();
			} catch (final Exception e) {
				exit(e.getMessage(), XWReturnCode.CONNECTION);
			}
		}

	}

	/**
	 * This is finally where everything takes place. This is called at least
	 * once, or once per line found in "macro" file, if any.
	 *
	 */
	private void doItNow() {

		try {
			logger.debug("doItNow " + args.command());

			switch (args.command()) {
			case VERSION:
				println("Current version : " + Version.currentVersion.toString());
				try {
					final Version serverVersion = commClient().version();
					logger.debug("serverVersion = " + serverVersion);
					if ((serverVersion != null)
							&& !serverVersion.toString().equals(Version.currentVersion.toString())) {
						println("Server  version : " + serverVersion);
						println("**********  **********  **********");
						println("You should upgrade your client");
						println("**********  **********  **********");
					}
				} catch (final Exception e) {
					logger.exception(e);
				}
				exit("", XWReturnCode.SUCCESS);
				break;
			case GET:
				get();
				break;
			case GETAPPS:
				getApps(true);
				break;
			case SENDAPP:
				sendApp();
				break;
			case REMOVE:
				remove();
				break;
			case GETDATAS:
				getDatas(true);
				break;
			case SENDDATA:
				sendData();
				break;
			case GETENVELOPES:
				getEnvelopes(true);
				break;
			case SENDENVELOPE:
				sendEnvelope();
				break;
			case GETGROUPS:
				getGroups(true);
				break;
			case SENDGROUP:
				sendGroup();
				break;
			case GETHOSTS:
				getHosts(true);
				break;
			case SENDHOST:
				sendHost();
				break;
			case GETSESSIONS:
				getSessions(true);
				break;
			case SENDSESSION:
				sendSession();
				break;
			case GETTASK:
				get();
				break;
			case GETTASKS:
				getTasks(true);
				break;
			case GETTRACES:
				getTraces(true);
				break;
			case SENDTRACE:
				sendTrace();
				break;
			case GETUSERBYLOGIN:
				getUser();
				break;
			case GETUSERS:
				getUsers(true);
				break;
			case SENDUSER:
				sendUser();
				break;
			case GETUSERGROUPS:
				getUserGroups(true);
				break;
			case SENDUSERGROUP:
				sendUserGroup();
				break;
			case GETWORKS:
				getWorks(true);
				break;
			case GETGROUPWORKS:
				getGroupWorks(true);
				break;
			case SENDWORK:
				sendWork();
				break;
			case PING:
				ping();
				break;
			case WORKALIVE:
			case WORKALIVEBYUID:
				workAlive();
				break;
			case WORKREQUEST:
				workRequest();
				break;
			case CHMOD:
				chmod();
				break;
			case GETHUBADDR:
				getHubAddr(true);
				break;
			case ACTIVATEHOST:
				updateWorkers();
				break;
			}
		} catch (final SSLHandshakeException ssle) {
			logger.exception(ssle);
			handshakeError();
		} catch (final ParseException ce) {
			logger.exception(ce);
			usage(args.command(), ce.getMessage());
		} catch (final ConnectException ce) {
			logger.exception(ce);
			connectionRefused();
		} catch (final ClassNotFoundException e) {
			logger.exception(e);
			exit(e.getMessage(), XWReturnCode.DISK);
		} catch (final CertificateExpiredException e) {
			logger.exception(e);
			exit(e.getMessage(), XWReturnCode.AUTHENTICATION);
		} catch (final CertificateException e) {
			logger.exception(e);
			exit(e.getMessage(), XWReturnCode.AUTHENTICATION);
		} catch (final InvalidKeyException e) {
			logger.exception(e);
			exit(e.getMessage(), XWReturnCode.AUTHENTICATION);
		} catch (final AccessControlException e) {
			logger.exception(e);
			exit(e.getMessage(), XWReturnCode.AUTHORIZATION);
		} catch (final FileNotFoundException e) {
			logger.exception(e);
			exit(e.getMessage(), XWReturnCode.DISK);
		} catch (final IOException e) {
			logger.exception(e);
			objectNotFound();
		} catch (final Exception e) {
			logger.exception(e);
			usage(args.command());
		}
	}

	/**
	 * This executes what reader provides
	 *
	 * @see #doItNow()
	 * @since 7.0.0
	 */
	public IdRpc exec(final BufferedReader reader) throws ParseException, IOException, FileNotFoundException {

		final String line = reader.readLine();

		logger.finest("line = " + line);

		final Collection<String> params = XWTools.split(line);

		if ((params == null) || (params.size() < 1)) {
			return null;
		}
		final String[] arrayType = new String[0];
		final String[] paramsarray = params.toArray(arrayType);

		args = new CommandLineParser(paramsarray);

		doItNow();
		return args.command();
	}

	/**
	 * This parses "macro" file and executes each line calling doItNow()
	 * <blockquote> Command line parameters : --xwmacro &lt;macro file name&gt;
	 * </blockquote>
	 *
	 * @see #doItNow()
	 */
	private void execMacros() {

		executingMacro = true;
		final boolean noverbose = (args.getOption(CommandLineOptions.NOVERBOSE) != null);

		logger.debug("macro = " + args.getOption(CommandLineOptions.MACRO).toString());
		macroFile = new File((String) args.getOption(CommandLineOptions.MACRO));

		if (args.xml()) {
			printXMLHeader();
		}

		try (final BufferedReader reader = new BufferedReader(new FileReader(macroFile))) {
			while (true) {
				final IdRpc cmd = exec(reader);
				if (cmd == null) {
					break;
				}
				if (noverbose == false) {
					logger.info("macro [" + ++macroLineNumber + "] " + "(" + cmd + ") ");
				}
			}
		} catch (final Exception e) {
			exit(e.getMessage(), XWReturnCode.PARSING);
		}

	}

	/**
	 * This opens a socket and wait for incoming connections. This parses socket
	 * input stream and executes the command by calling doItNow()
	 *
	 * @see #doItNow()
	 * @since 7.0.0
	 */
	private void execShell() throws InterruptedException {

		shellRunning = true;
		try {
			final Shell shell = new Shell();
			shell.initComm(this);
			shell.start();
			shell.join();
		} finally {
			shellRunning = false;
		}
	}

	/**
	 * This instantiates SmartSockets proxies. If CommandLineOptions.FORWARDPORT
	 * is set, let start a server like proxy waiting incoming connections from
	 * local SmartSocketAddress and forwarding them to a local port so that jobs
	 * running on XWHEP worker will be able to connect on server like
	 * application running on XWHEP client side.
	 *
	 * If CommandLineOptions.FORWARDADDRESSES is set, let start a client like
	 * proxy waiting incoming connections from local port and forwarding them to
	 * remote SmartSocketAddress so that applications running on XWHEP client
	 * side have a chance to connect on server like application running on XWHEP
	 * worker side.
	 *
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @since 8.0.0
	 */
	private void smartSocketsProxy() throws InterruptedException, InvalidKeyException, AccessControlException,
	IOException, ClassNotFoundException, SAXException, URISyntaxException {

		final Vector params = (Vector) args.commandParams();

		String hubAddr = null;
		try {
			args.getOption(CommandLineOptions.SMARTSOCKETSPROXY);
			hubAddr = getHubAddr(false);
		} catch (final Exception e) {
			hubAddr = (String) args.getOption(CommandLineOptions.SMARTSOCKETSPROXY);
		}
		logger.debug("hubaddr = " + hubAddr);
		if ((hubAddr == null) || (hubAddr.length() < 1)) {
			exit("No SmartSockets hub address provided", XWReturnCode.PARSING);
		}
		int forwardPort = -1;
		try {
			final String forwardPortStr = (String) args.getOption(CommandLineOptions.FORWARDPORT);
			if (forwardPortStr != null) {
				forwardPort = Integer.parseInt(forwardPortStr);
			}
		} catch (final Exception e1) {
			exit("Invalid forwarding port : " + e1.getMessage(), XWReturnCode.PARSING);
		}

		if (forwardPort != -1) {
			try {
				final SmartSocketsProxy smartSocketsProxy = new SmartSocketsProxy(hubAddr, null, forwardPort, true);
				println("Starting proxy; listening address : " + smartSocketsProxy.getLocalAddress()
				+ ", forwarding port : " + forwardPort);
				smartSocketsProxy.start();
			} catch (final Exception e) {
				logger.exception("Can't start new SmartSocket proxy", e);
				XWTools.releasePort(forwardPort);
			}
			return;
		}

		int listenPort = -1;
		try {
			listenPort = Integer.parseInt((String) args.getOption(CommandLineOptions.LISTENPORT));
		} catch (final Exception e1) {
			exit("Invalid listening port : " + e1.getMessage(), XWReturnCode.PARSING);
		}

		int portlimit;
		for (portlimit = 0; portlimit < 1000; portlimit++) {
			logger.debug("Checking port availability " + listenPort);
			if (XWTools.lockPort(listenPort)) {
				break;
			}
			listenPort++;
		}
		if (portlimit == 1000) {
			throw new IOException("can't find any available port to listen to");
		}

		final String forwardAddr = (String) args.getOption(CommandLineOptions.FORWARDADDRESSES);

		if (forwardAddr != null) {
			try {
				final SmartSocketsProxy smartSocketsProxy = new SmartSocketsProxy(hubAddr, forwardAddr, listenPort,
						false);
				println("Starting proxy; listening port : " + listenPort + ", forwarding address : "
						+ smartSocketsProxy.getRemoteAddress());
				smartSocketsProxy.start();
			} catch (final Exception e) {
				logger.exception("Can't start new SmartSocket proxy", e);
				XWTools.releasePort(forwardPort);
			}
			return;
		}

		WorkInterface theWork = null;
		try {
			final Enumeration theEnum = params.elements();
			if (theEnum.hasMoreElements()) {
				final Object param = theEnum.nextElement();
				try {
					theWork = (WorkInterface) get((URI) param);
				} catch (final ClassCastException cce) {
					theWork = (WorkInterface) get((UID) param);
				}
			}
		} catch (final Exception e1) {
			exit("No valid URI/UID provided", XWReturnCode.PARSING);
		}

		if ((theWork == null) || (theWork.getStatus() != StatusEnum.RUNNING)) {
			exit("Ivalid work status", XWReturnCode.ERROR);
		}
		final Collection<String> serverAddresses = XWTools.split(theWork.getSmartSocketAddr(), ",");
		if ((serverAddresses == null) || (serverAddresses.isEmpty())) {
			exit("No valid server addresses found from work", XWReturnCode.CONNECTION);
		}
		final Iterator<String> addressesenum = serverAddresses.iterator();
		int portloop = 0;

		while (addressesenum.hasNext()) {

			final String serverAddr = addressesenum.next();

			final int fport = listenPort + portloop++;

			try {
				final SmartSocketsProxy smartSocketsProxy = new SmartSocketsProxy(hubAddr, serverAddr, fport, false);
				println("Starting proxy; listening port : " + fport + ", forwarding address : "
						+ smartSocketsProxy.getLocalAddress());
				smartSocketsProxy.start();
			} catch (final Exception e) {
				logger.exception("Can't start new SmartSocket proxy", e);
				XWTools.releasePort(fport);
			}
		}
	}

	/**
	 * This is the standard main method
	 */
	public static void main(final String[] argv) {

		try {
			final Client client = new Client(argv);
			client.verbose();
			client.execute();
		} catch (final Exception e) {
			System.err.println(e.getMessage());
		}

	}

}
