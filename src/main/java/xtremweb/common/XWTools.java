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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONObject;
import org.json.JSONTokener;


/**
 *
 * Some utilities
 *
 * Created: Thu Jun 29 17:47:11 2000
 *
 * @author Gilles Fedak
 * @version %I% %G%
 */

public class XWTools {
	private XWTools() {
	}

	private static final Logger logger = new Logger();

	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * This is used in SQL statements<br />
	 * This is defined to single quote and not to double quote to be HSQLDB
	 * compliant<br />
	 * 6 decembre 2005 : client command line cannot includes this character
	 * until further notification
	 */
	public static final String QUOTE = "'";

	/**
	 * This defines the name of the stdout file
	 */
	public static final String STDOUT = "stdout" + DataTypeEnum.TEXT.getFileExtension();
	/**
	 * This defines the name of the stderr file
	 */
	public static final String JARFILENAME = "xtremweb.jar";
	/**
	 * This defines the name of the stderr file
	 */
	public static final String STDERR = "stderr" + DataTypeEnum.TEXT.getFileExtension();
	/**
	 * This defines buffer size for communications : 16Kb
	 *
	 * @see StreamIO#DEFLENGTH
	 * @see ByteStack#DEFLENGTH
	 * @see BytePacket#BUFFERLENGTH
	 * @see xtremweb.communications.UDPServer#run()
	 */
	public static final int PACKETSIZE = 16 * 1024;
	/**
	 * This defines file size limit over which file is considered as huge :
	 * 250Kb
	 *
	 * @see Zipper#zip(String[], boolean)
	 * @see StreamIO#file2array(File)
	 */
	public static final int LONGFILESIZE = 250 * 1024;
	/**
	 * This is the maximum amount of rows returned by an SQL statement
	 * @since 12.2.10
	 */
	public static final int MAXDBREQUESTLIMIT = 1000;
	/**
	 * This defines the maximum amount of messages in one single socket. The
	 * server automatically closes socket when this limit is reached. This
	 * ensures that client do not block a socket for too long since the server
	 * has a limited amount of simultaneous connections. Otherwise DoS attack
	 * would be too easy.
	 *
	 * @since 7.4.0
	 * @see XWPropertyDefs#MAXCONNECTIONS
	 */
	public static final int MAXMESSAGES = 2000;
	/**
	 * This is the 1024
	 *
	 * @since 9.1.0
	 */
	public static final long ONEKILOBYTES = 1024;
	/**
	 * This is the 1024*1024
	 *
	 * @since 9.1.0
	 */
	public static final long ONEMEGABYTES = ONEKILOBYTES * ONEKILOBYTES;
	/**
	 * This is the 2GB limit above which we NIO is buggy
	 */
	public static final long ONEGIGABYTES = ONEKILOBYTES * ONEMEGABYTES;
	/**
	 * This is the 2GB limit above which we NIO is buggy
	 */
	public static final long TWOGIGABYTES = 2 * ONEGIGABYTES;
	/**
	 * This defines the default wall cloacktime to 300s (5mn)
	 * @since 13.0.0
	 */
	public static final int DEFAULTWALLCLOCKTIME = 300;
	/**
	 * This defines the default CPU speed in percentage
	 * @href https://docs.docker.com/engine/reference/run/#cpu-period-constraint
	 * @since 13.0.0
	 */
	public static final float DEFAULTCPUSPEED = 0.5f;
	/**
	 * This defines file size limit : 100Mb
	 * @since 12.2.3
	 */
	public static final long MAXFILESIZE = 100 * ONEMEGABYTES;
	/**
	 * This defines the maximum size of work disk space (30Gb)
	 *
	 * @since 8.0.0
	 */
	public static final long MAXDISKSIZE = 5 * ONEGIGABYTES;
	/**
	 * This defines the maximum size of work RAM space (1Gb) 
	 *
	 * @since 9.1.0
	 */
	public static final long MAXRAMSIZE = 512 * ONEKILOBYTES;
	/**
	 * This helps to format date : the format is "yyyy-MM-dd HH:mm:ss"
	 */
	private static final SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat defaultDateFormat = new SimpleDateFormat();

	/** This is used to mark buffered input stream */
	public static final int BUFFEREND = 4096;

	/**
	 * @since 10.0.0
	 */
	public final static String PACKAGENAMEHEADER = "XWPKG_";

	/**
	 * This formats a date to String
	 *
	 * @param d
	 *            is the date to format
	 * @return a String containing formatted date
	 * @see #sqlDateFormat
	 */
	public static String getSQLDateTime(final java.util.Date d) {

		if (d == null) {
			return null;
		}

		synchronized (sqlDateFormat) {
			return sqlDateFormat.format(d);
		}
	}

	/**
	 * This retrieves a date from String
	 *
	 * @param d
	 *            is the string containing the date
	 * @return a Date as represented in provided formatted string
	 * @see #sqlDateFormat
	 */
	public static java.util.Date getSQLDateTime(final String d) {

		if (d == null) {
			return null;
		}

		try {
			synchronized (sqlDateFormat) {
				return sqlDateFormat.parse(d);
			}
		} catch (final Exception e) {
			synchronized (defaultDateFormat) {
				try {
					return defaultDateFormat.parse(d);
				} catch (final Exception e2) {
				}
			}
		}
		return null;
	}

	/**
	 * This contains locked ports
	 *
	 * @since 8.0.0
	 * @see #lockPort(int)
	 * @see #releasePort(int)
	 */
	static private Hashtable<Integer, Boolean> lockedPorts = new Hashtable<>();

	/**
	 * Checks to see if a specific port is available. If the port is available,
	 * it is locked in lockedPorts. This is the implementation coming from the
	 * Apache mina project Found at
	 * http://stackoverflow.com/questions/434718/sockets
	 * -discover-port-availability-using-java.
	 *
	 * @param port
	 *            the port to check for availability
	 * @return true if port is availabel, false otherwise
	 * @since 8.0.0
	 * @see #lockedPorts
	 * @see #releasePort(int)
	 */
	public static synchronized boolean lockPort(final int port) {
		final Integer key = port;

		if (lockedPorts.containsKey(key)) {
			return false;
		}

		try (final ServerSocket ss = new ServerSocket(port); final DatagramSocket ds = new DatagramSocket(port)) {

			ss.setReuseAddress(true);
			ds.setReuseAddress(true);
			lockedPorts.put(key, true);
			return true;
		} catch (final IOException e) {
		}

		return false;
	}

	/**
	 * Checks to see if a specific port is available. This is the implementation
	 * coming from the Apache mina project Found at
	 * http://stackoverflow.com/questions
	 * /434718/sockets-discover-port-availability-using-java
	 *
	 * @param port
	 *            the port to check for availability
	 * @since 8.0.0
	 * @see #lockedPorts
	 * @see #lockPort(int)
	 */
	public static void releasePort(final int port) {
		lockedPorts.remove(port);
	}

	private static String localhostName = null;

	/**
	 * This retreives local host name. This calls getHostName("localhost")
	 *
	 * @see #getHostName(String)
	 * @return local host name
	 */
	public static String getLocalHostName() {
		if (localhostName != null) {
			return localhostName;
		}
		try {
			localhostName = java.net.InetAddress.getLocalHost().getHostName();
			if (localhostName.compareTo(java.net.InetAddress.getLocalHost().getHostAddress()) == 0) {
				localhostName = java.net.InetAddress.getLocalHost().getHostName();
			}
		} catch (final IOException e) {
			fatal(e.toString());
		}

		return localhostName;
	}

	/**
	 * This retreives a host name. correct misconfiguered /etc/hosts.
	 *
	 * @throws UnknownHostException
	 */
	public static String getHostName(final String hostname) throws UnknownHostException {
		String ret = "";

		ret = java.net.InetAddress.getByName(hostname).getHostName();

		if (ret.toLowerCase().indexOf("localhost") != -1) {
			ret = getLocalHostName();
		}

		return ret;
	}

	public static boolean searchInArray(final Object[] a, final Object b) {
		int i = 0;
		boolean found = false;
		boolean complete = false;
		if (a == null) {
			return (false);
		}
		while (!complete) {
			found = (b.equals(a[i]));
			i++;
			complete = (i == a.length) || found;
		}
		return (found);
	}

	public static void fatal(final String s) {

		final SimpleDateFormat logDateFormat = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]", Locale.US);
		logger.fatal(logDateFormat.format(new Date()) + " Fatal : " + s);
	}

	/**
	 * Restart error: cause the program to exit and restart
	 */
	public static void restart(final String s) {
		logger.info("restarting : " + s);
		System.exit(XWReturnCode.RESTART.ordinal());
	}

	public static void fileCopy(final File in, final File out) throws IOException {
		try (FileInputStream fis = new FileInputStream(in); FileOutputStream fos = new FileOutputStream(out);) {
			final byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
		} catch (final IOException e) {
			throw e;
		}
	}

	/**
	 * This retreives an X.509 certificate from file
	 */
	public static X509Certificate certificateFromFile(final String certFileName)
			throws CertificateException, CertificateExpiredException, IOException {

		return certificateFromFile(new File(certFileName));
	}

	/**
	 * This retreives an X.509 certificate from file
	 */
	public static X509Certificate certificateFromFile(final File certFile)
			throws CertificateException, CertificateExpiredException, IOException {

		try (final FileInputStream inStream = new FileInputStream(certFile)) {
			final CertificateFactory cf = CertificateFactory.getInstance("X.509");
			return (X509Certificate) cf.generateCertificate(inStream);
		}
	}

	private static CertificateFactory certificateFactory = null;

	/**
	 * This checks X.509 validity
	 */
	public static X509Certificate checkCertificate(final File certFile)
			throws CertificateException, CertificateExpiredException, FileNotFoundException, IOException {

		X509Certificate cert = null;
		try (FileInputStream inStream = new FileInputStream(certFile)) {
			cert = checkCertificate(inStream);
		} finally {
		}
		return cert;
	}

	/**
	 * This checks X.509 validity
	 */
	public static X509Certificate checkCertificate(final InputStream in)
			throws CertificateException, CertificateExpiredException, FileNotFoundException, IOException {

		if (certificateFactory == null) {
			certificateFactory = CertificateFactory.getInstance("X.509");
		}
		final X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(in);
		cert.checkValidity();
		return cert;
	}

	/**
	 * This compares two certificates
	 */
	public static boolean compareCertificates(final X509Certificate key1, final X509Certificate key2)
			throws CertificateEncodingException {
		return (Arrays.equals(key1.getEncoded(), key2.getEncoded())) ? true : false;
	}

	/**
	 * This creates a subdir from UID. This defines up to 1000 sub directories
	 * name "0" to "999". This has been introduced to solve file system
	 * limitations.
	 *
	 * @param parent
	 *            is the parent directory of the new sub directory to create
	 * @param uid
	 *            is the uid to create a new subdir for
	 * @return the subdir name
	 * @since 1.3
	 */
	public static File createDir(final String parent, final UID uid) throws IOException {

		final File fparent = new File(parent);
		return createDir(fparent, uid);
	}

	/**
	 * This creates a subdir from UID. This defines up to 1000 sub directories
	 * name "0" to "999". This has been introduced to solve file system
	 * limitations.
	 *
	 * @param parent
	 *            is the parent directory of the new sub directory to create
	 * @param uid
	 *            is the uid to create a new subdir for
	 * @return the subdir name
	 * @since 1.3
	 */
	public static File createDir(final File parent, final UID uid) throws IOException {

		final String dirName = Integer.toString(uid.hashCode() % 1000);
		final File dir = new File(parent, dirName);
		checkDir(dir);
		return dir;
	}

	/**
	 * This ensures that a directory exists.
	 *
	 * If the parameter is not a directory, the file will be deleted. If the
	 * directory does not exists, <code>checkDir</code> will be called on its
	 * parent before creating it.
	 * </p>
	 *
	 * @param dir
	 *            the directory to create.
	 * @exception IOException
	 *                if the directory does not exist and can't be created
	 */
	public static void checkDir(final File dir) throws IOException {

		if (dir == null) {
			return;
		}

		if ((dir.exists()) && (dir.isDirectory())) {
			return;
		}

		if (!dir.isDirectory()) {
			if (dir.exists()) {
				dir.delete();
			} else {
				checkDir(dir.getParentFile());
			}
		}
		if ((!dir.exists()) && (!dir.mkdirs())) {
			throw new IOException("can't create directory : " + dir.getAbsolutePath());
		}
	}

	public static void checkDir(final String str) throws IOException {
		checkDir(new File(str));
	}

	/**
	 * This delete a full directory
	 */
	static public boolean deleteDir(final File path) throws IOException {
		if ((path == null) || (!path.exists())) {
			return false;
		}

		final File[] files = path.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDir(files[i]);
				} else {
					files[i].delete();
				}
			}
		}

		return path.delete();
	}

	/**
	 * This calls hash(src, ",", ":")
	 *
	 * @see #hash(String,String,String)
	 * @since XWHEP 1.0.0
	 */
	public static Map<String, String> hash(final String src) {
		return hash(src, ",", ":");
	}

	/**
	 * This converts a string into an hash table of strings accordingly to
	 * separators.
	 *
	 * @param src
	 *            is the String to split
	 * @param separator1
	 *            contains separators to split Tuples
	 * @param separator2
	 *            contains separators to split keys from values
	 * @return an hash table of String
	 * @since XWHEP 1.0.0
	 */
	public static Map<String, String> hash(final String src, final String separator1, final String separator2) {

		final Hashtable<String, String> ret = new Hashtable<>();

		if ((src == null) || (separator1 == null) || (separator2 == null)) {
			return ret;
		}

		final Collection<String> tuples = split(src, separator1);
		if (tuples == null) {
			return null;
		}

		for (int i = 0; i < tuples.size(); i++) {

			final Collection<String> tuple = split(((Vector<String>) tuples).elementAt(i), separator2);
			if ((tuple != null) && (tuple.size() == 2)) {
				ret.put(((Vector<String>) tuple).elementAt(0), ((Vector<String>) tuple).elementAt(1));
			}
		}
		return ret;
	}

	/**
	 * This converts a string into an array of strings accordingly to a
	 * separator.
	 *
	 * @param src
	 *            is the String to split
	 * @param separator
	 *            contains all separator to split <CODE>src</CODE>
	 * @return an array of String
	 * @since v1r2-rc1(RPC-V)
	 */
	public static Collection<String> split(final String src, final String separator) {
		if ((src == null) || (separator == null)) {
			return null;
		}
		final Vector<String> ret = new Vector<>();
		final StringTokenizer tokenizer = new StringTokenizer(src, separator);

		while (tokenizer.hasMoreTokens()) {
			final String elem = tokenizer.nextToken();
			ret.addElement(elem.trim());
		}

		return ret;
	}

	/**
	 * This converts a string into an array of strings accordingly to a
	 * separator.
	 *
	 * @param src
	 *            is the String to split
	 * @return an array of String
	 * @since v1r2-rc1(RPC-V)
	 */
	public static Collection<String> split(final String src) {
		return split(src, " \t");
	}

	/**
	 * This aims to launch a browser
	 *
	 * Bare Bones Browser Launch Version 1.5 (December 10, 2005) By Dem Pilafian
	 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP Example Usage: String url
	 * = "http://www.centerkey.com/"; BareBonesBrowserLaunch.openURL(url);
	 * Public Domain Software -- Free to Use as You Like
	 *
	 * @param url
	 *            is the URL to display
	 */
	public static void launchBrowser(final String url) {
		final String osName = System.getProperty("os.name");
		try {
			if (osName.startsWith("Mac OS")) {
				final Class fileMgr = Class.forName("com.apple.eio.FileManager");
				final Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
				openURL.invoke(null, new Object[] { url });
			} else if (osName.startsWith("Windows")) {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			} else {
				final String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
				String browser = null;
				for (int count = 0; (count < browsers.length) && (browser == null); count++) {
					if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) {
						browser = browsers[count];
					}
				}
				if (browser == null) {
					throw new Exception("Could not find web browser");
				} else {
					Runtime.getRuntime().exec(new String[] { browser, url });
				}
			}
		} catch (final Exception e) {
			logger.exception("Launch browser error", e);
		}
	}

	/**
	 * This defines sizeof byte in Java language, in bytes
	 */
	public static final int SIZEOFBYTE = 1;
	/**
	 * This defines sizeof short in Java language, in bytes
	 */
	public static final int SIZEOFSHORT = 2;
	/**
	 * This defines sizeof integer in Java language, in bytes
	 */
	public static final int SIZEOFINTEGER = 4;
	/**
	 * This defines sizeof long in Java language, in bytes
	 */
	public static final int SIZEOFLONG = 8;

	/**
	 * This converts an array of 4 bytes to an integer
	 *
	 * @param datas
	 *            is a 4 elements array
	 * @param len
	 *            is the number of bytes to use in datas
	 */
	public static int[] bytes2integers(final byte[] datas, final int len) {

		final int nbint = len / SIZEOFINTEGER;
		final int[] integers = new int[nbint + 1];
		final byte[] bytes = new byte[SIZEOFINTEGER];

		for (int i = 0; i < nbint; i++) {

			for (int j = 0; j < SIZEOFINTEGER; j++) {
				bytes[j] = datas[(SIZEOFINTEGER * i) + j];
			}

			integers[i] = bytes2integer(bytes);
		}

		return integers;
	}

	/**
	 * This converts an array of 4 bytes to an integer
	 *
	 * @param bytes
	 *            [] is a 4 elements array
	 */
	public static int bytes2integer(final byte[] bytes) {

		return (((bytes[0] << 24) & 0xff000000) + ((bytes[1] << 16) & 0x00ff0000) + ((bytes[2] << 8) & 0x0000ff00)
				+ (bytes[3] & 0x000000ff));
	}

	/**
	 * This converts a long to an array of 4 bytes
	 *
	 * @param data
	 *            is the long to convert
	 * @return a byte array with height elements
	 */
	public static byte[] long2bytes(final long data) {

		final byte[] bytes = new byte[SIZEOFLONG];
		bytes[0] = (byte) ((data & 0xff00000000000000L) >> 56);
		bytes[1] = (byte) ((data & 0x00ff000000000000L) >> 48);
		bytes[2] = (byte) ((data & 0x0000ff0000000000L) >> 40);
		bytes[3] = (byte) ((data & 0x000000ff00000000L) >> 32);
		bytes[4] = (byte) ((data & 0x00000000ff000000L) >> 24);
		bytes[5] = (byte) ((data & 0x0000000000ff0000L) >> 16);
		bytes[6] = (byte) ((data & 0x000000000000ff00L) >> 8);
		bytes[7] = (byte) (data & 0x00000000000000ffL);

		return bytes;
	}

	/**
	 * This converts an integer to an array of 4 bytes
	 *
	 * @param data
	 *            is the integer to convert
	 * @return a byte array with four elements
	 */
	public static byte[] integer2bytes(final int data) {

		final byte[] bytes = new byte[SIZEOFINTEGER];

		bytes[0] = (byte) ((data & 0xff000000) >> 24);
		bytes[1] = (byte) ((data & 0x00ff0000) >> 16);
		bytes[2] = (byte) ((data & 0x0000ff00) >> 8);
		bytes[3] = (byte) (data & 0x000000ff);

		return bytes;
	}

	/**
	 * This converts a short to an array of 4 bytes
	 *
	 * @param data
	 *            is the short to convert
	 * @return a byte array with two elements
	 */
	public static byte[] short2bytes(final short data) {

		final byte[] bytes = new byte[SIZEOFSHORT];

		bytes[0] = (byte) ((data & 0xff00) >> 8);
		bytes[1] = (byte) (data & 0x00ff);

		return bytes;
	}

	public static String intToHexString(final int value) {
		return "0x" + Integer.toHexString(value);
	}

	static final String HEXES = "0123456789ABCDEF";

	public static String byteArrayToHexString(final byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	public static byte[] hexStringToByteArray(final String s) {
		final int len = s.length();
		final byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	/**
	 * This retrieves CA paths for the given host
	 *
	 * @param host
	 *            is the host name
	 * @param sav
	 *            if true, public key is stored to local FS
	 * @return an array of certificates
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws CertificateEncodingException
	 */
	public static final X509Certificate[] retrieveCertificates(final String host, final boolean sav)
			throws IOException, CertificateEncodingException {
		final int port = 443;

		final SocketFactory factory = SSLSocketFactory.getDefault();
		final SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

		socket.startHandshake();

		final X509Certificate[] certs = (X509Certificate[]) socket.getSession().getPeerCertificates();
		final Logger logger = new Logger("XWTools");
		logger.debug(host + " : certs retrieved = " + certs.length);
		int i = 0;
		for (final X509Certificate cert : certs) {
			PrintStream out = System.out;
			logger.debug("CN        = " + cert.getSubjectX500Principal().getName());
			logger.debug("Issuer CN = " + cert.getIssuerX500Principal().getName());
			if (sav) {
				logger.debug("Saving to " + host + "_" + i + ".pem");
				out = new PrintStream(new File(host + "_" + i++ + ".pem"));
			}
			if((sav) || (logger.debug())) {
				out.println("-----BEGIN CERTIFICATE-----");
				out.println(new sun.misc.BASE64Encoder().encode(cert.getEncoded()));
				out.println("-----END CERTIFICATE-----");
			}
			if (sav) {
				out.close();
			}
		}
		return certs;
	}

	/**
	 * This extracts a JSon value from an URL
	 * 
	 * @param u
	 *            is the URL to retrieve JSon content
	 * @param key
	 *            is the JSon key name to retrieve
	 * @throws IOException
	 *             on connection error
	 * @since 10.5.0
	 */
	public static String jsonValueFromURL(final String u, final String key) throws IOException {
		final URL url = new URL(u);
		try (final InputStream is = url.openStream();) {
			final JSONTokener jst = new JSONTokener(is);
			final JSONObject obj = new JSONObject(jst);
			return obj.getString(key);
		}
	}

	/**
	 * This extracts a JSon value from an URL
	 * 
	 * @param s
	 *            contains the JSon representation
	 * @param key
	 *            is the JSon key name to retrieve
	 * @since 10.5.0
	 */
	public static String jsonValueFromString(final String s, final String key) {
		final JSONTokener jst = new JSONTokener(s);
		final JSONObject obj = new JSONObject(jst);
		return obj.getString(key);
	}

	public static final String GOOGLE_ADDR = "www.google.com";
	public static final String YAHOO_ADDR = "www.yahoo.com";

	/**
	 * This inserts all known CA certificates to the provided keystore
	 *
	 * @param store
	 *            is the keystore to add certificate to
	 * @return this returns null, if parameter is null; else this returns the
	 *         keystore filled with some new entries
	 * @since 8.0.2
	 */
	public static KeyStore setCACertificateEntries(final KeyStore store) {
		if (store == null) {
			return null;
		}
		final Logger logger = new Logger();
		try {
			final X509Certificate[] gcerts = XWTools.retrieveCertificates(GOOGLE_ADDR, false);
			for (int i = 0; i < gcerts.length; i++) {
				final X509Certificate cert = gcerts[i];
				final String alias = cert.getSubjectDN().toString();
				logger.finest("KeyStore set entry= " + alias + "; KeyStore.size = " + store.size());
				store.setCertificateEntry(alias, cert);
			}
		} catch (final Exception e) {
			logger.config("Can't add google certs to keystore");
		}
		try {
			final X509Certificate[] ycerts = XWTools.retrieveCertificates(YAHOO_ADDR, false);
			for (int i = 0; i < ycerts.length; i++) {
				final X509Certificate cert = ycerts[i];
				final String alias = cert.getSubjectDN().toString();
				logger.debug("KeyStore set entry= " + alias + "; KeyStore.size = " + store.size());
				store.setCertificateEntry(alias, cert);
			}
		} catch (final Exception e) {
			logger.config("Can't add yahoo certs to keystore");
		}
		return store;
	}

//    public static String sha256CheckSum(final File data) throws NoSuchAlgorithmException, FileNotFoundException, IOException
//    {
//        final MessageDigest md = MessageDigest.getInstance("SHA-256");
//        try (final FileInputStream fis = new FileInputStream(data)) {
//
//        	final byte[] dataBytes = new byte[1024];
//
//        	int nread = 0;
//	        while ((nread = fis.read(dataBytes)) != -1) {
//	          md.update(dataBytes, 0, nread);
//	        };
//	        final byte[] mdbytes = md.digest();
//
//	       //convert the byte to hex format method 2
//	        final StringBuffer hexString = new StringBuffer();
//	    	for (int i=0;i<mdbytes.length;i++) {
//	    	  hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
//	    	}
//	
//	    	return hexString.toString();
//        } 
//    }

    public static String sha256CheckSum(final File data) throws NoSuchAlgorithmException, IOException 
    {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (final FileInputStream fis = new FileInputStream(data)) {

        	final byte[] dataBytes = new byte[1024];

        	int nread = 0;
	        while ((nread = fis.read(dataBytes)) != -1) {
	          md.update(dataBytes, 0, nread);
	        };
	        final byte[] digest = md.digest();

	        return String.format( "%064x", new BigInteger( 1, digest ) );
        } 
    }

//    public static String sha256(final String data) throws NoSuchAlgorithmException
//    {
//    	final MessageDigest md = MessageDigest.getInstance("SHA-256");
//        md.update(data.getBytes());
//
//        byte byteData[] = md.digest();
//
//        final StringBuffer hexString = new StringBuffer();
//    	for (int i=0;i<byteData.length;i++) {
//    		String hex=Integer.toHexString(0xff & byteData[i]);
//   	     	if(hex.length()==1) hexString.append('0');
//   	     	hexString.append(hex);
//    	}
//    	return hexString.toString();
//    }
    public static String sha256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance( "SHA-256" );
        String text = "Text to hash, cryptographically.";

        // Change this to UTF-16 if needed
        md.update( text.getBytes( StandardCharsets.UTF_8 ) );
        byte[] digest = md.digest();

        return String.format( "%064x", new BigInteger( 1, digest ) );
      }
    /**
	 * This is for testing only
	 */
	public static void main(final String[] argv) {
		try {
			UID uid = null;

			if (argv.length > 0) {
				uid = new UID(argv[0]);
			} else {
				uid = new UID();
			}

			logger.info("uid " + uid + " " + uid.hashCode() + " " + (uid.hashCode() % 1000));
			logger.info("uid " + uid + " " + createDir("/tmp", uid));
			logger.info();

			final UID uid2 = new UID(uid.toString());

			logger.info("uid2 " + uid2 + " " + uid2.hashCode() + " " + (uid2.hashCode() % 1000));
			logger.info("uid2 " + uid2 + " " + createDir("/tmp", uid2));
			logger.info();

			final UID uid3 = uid;

			logger.info("uid3 " + uid3 + " " + uid3.hashCode() + " " + (uid3.hashCode() % 1000));
			logger.info("uid3 " + uid3 + " " + createDir("/tmp", uid3));

			logger.info("sha256  (\"test string to sha256\") = " + sha256("test string to sha256"));

			if (argv.length > 1) {
				logger.info("sha256CheckSum  (" + argv[1] + ") = " + sha256CheckSum(new File(argv[1])));
			}

		} catch (final Exception e) {
			logger.exception(e);
		}
	}
}
