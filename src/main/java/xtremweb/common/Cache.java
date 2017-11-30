/*
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import xtremweb.communications.URI;

/**
 * This class caches datas from/to server. Datas are written/retrieved to/from
 * local disk.<br />
 * <br />
 * Objects are cached with their URI as key.<br />
 * The cache file is sequential and contains every event.<br />
 * <br />
 * Example : the following execution
 * <ol>
 * <li>add(anObject1); this adds Object1
 * <li>add(anObject1); this updates Object1
 * <li>add(anObject2); this adds Object2
 * <li>remove(anObject1); this deletes Object1 from cache
 * <li>add(anObject2); this updates Object2
 * </ol>
 * Gives the following cache file : <code>
 *   <anObject1 /><anObject1 /><anObject1 isDeleted="true" /><anObject2 />
 * </code> <br />
 * <br />
 * The constructor clears the cache first The previous cache file is then
 * reconstructed as : <code>
 *   <anObject2 />
 * </code>
 *
 * @author Oleg Lodygensky (lodygens lal.in2p3.fr)
 * @since XWHEP 1.0.0
 */
public final class Cache extends XMLable {

	private static final Logger logger = Logger.getLogger(Cache.class);

	/**
	 * This class defines a cache entry
	 */
	public class CacheEntry extends XMLable {

		/**
		 * This is the XML tag
		 */
		private static final String THISTAG = "XWCacheEntry";
		/**
		 * This is the cached object interface
		 */
		private Table itf;
		/**
		 * This is the file where the content is stored, if any
		 *
		 * @since 8.0.0
		 */
		private File content;
		/**
		 * This is the original URI where the entry came form
		 */
		private URI uri;
		/**
		 * This is the last access date
		 */
		private Date lastAccess;
		/**
		 * This is a semaphore to synchronize accesses
		 */
		private boolean locked;

		/**
		 * This constructs a new entry. This associates a new URI to a
		 * TableInterface
		 *
		 * @exception IOException
		 *                is thrown if no UID is defined in parameter
		 */
		public CacheEntry() {
			super(THISTAG, 0);

			itf = null;
			uri = null;
			setAttributeLength(-1);
			lastAccess = new Date();
			locked = false;
			setContent(null);
		}

		/**
		 * This constructs a new entry.
		 *
		 * @param ti
		 *            defines this entry object
		 * @param u
		 *            is the TableInterace URI
		 * @exception IOException
		 *                is thrown if no UID is defined in parameter
		 */
		public CacheEntry(final Table ti, final URI u) throws IOException {
			this();
			itf = ti;
			uri = u;
		}

		/**
		 * This constructs a new object from XML attributes received from input
		 * stream
		 *
		 * @param input
		 *            is the input stream
		 * @throws IOException
		 *             on XML error
		 */
		public CacheEntry(final DataInputStream input) throws IOException, SAXException {
			this();
			try (final XMLReader reader = new XMLReader(this)) {
				reader.read(input);
			} catch (final InvalidKeyException e) {
				logger.error("Caught exception: ", e);
			}
		}

		/**
		 * This locks this entry
		 *
		 * @since 7.0.0
		 */
		public synchronized void lock() throws IOException {
			logger.trace("CacheEntre#locking " + Thread.currentThread().getName() + " " + uri);
			try {
				while (locked) {
					try {
						logger.trace("CacheEntre#locking : waiting");
						wait();
					} catch (final InterruptedException e) {
					}
				}

				locked = true;

				logger.trace("CacheEntre#locked  " + Thread.currentThread().getName() + " " + uri);
			} finally {
				notify();
			}
		}

		/**
		 * This unlocks this entry
		 *
		 * @since 7.0.0
		 */
		public synchronized void unlock() throws IOException {
			try {
				logger.trace("CacheEntre#unlocking " + Thread.currentThread().getName() + " " + uri);
				locked = false;
				logger.trace("CacheEntre#unlocked  " + Thread.currentThread().getName() + " " + uri);
			} finally {
				notify();
			}
		}

		/**
		 * This sets last access date
		 */
		public synchronized void setLastAccess() {
			lastAccess = null;
			lastAccess = new Date();
		}

		/**
		 * This retrieves last access date
		 */
		public synchronized Date lastAccess() {
			return lastAccess;
		}

		/**
		 * This retrieves this object attributes
		 *
		 * @param attrs
		 *            contains attributes XML representation
		 */
		@Override
		public void fromXml(final Attributes attrs) {

			if (attrs == null) {
				return;
			}

			for (int a = 0; a < attrs.getLength(); a++) {
				final String attribute = attrs.getQName(a);
				final String value = attrs.getValue(a);
				logger.trace(
						"Cache  ##  attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\"");

				setLastAccess();
			}
		}

		/**
		 * This serializes this object to a String as an XML object<br />
		 *
		 * @return a String containing this object definition as XML
		 * @see #fromXml(Attributes)
		 */
		@Override
		public String toXml() {

			final StringBuilder ret = new StringBuilder("<" + THISTAG + ">");
			if (uri != null) {
				ret.append(uri.toXml());
			}
			if (itf != null) {
				ret.append(itf.toXml());
			}
			ret.append("</" + THISTAG + ">");

			return ret.toString();
		}

		/**
		 * This is called to decode XML elements
		 *
		 * @see XMLReader#read(InputStream)
		 */
		@Override
		public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
				throws SAXException {

			try {
				super.xmlElementStart(uri, tag, qname, attrs);
				return;
			} catch (final SAXException ioe) {
			}

			logger.trace("  xmlElementStart()   qname=\"" + qname + "\"");

			if (qname.compareToIgnoreCase(THISTAG) == 0) {
				fromXml(attrs);
			} else if (qname.compareToIgnoreCase(URI.THISTAG) == 0) {
				try {
					this.uri = new URI(attrs);
				} catch (final Exception e) {
					logger.error("Caught exception: ", e);
				}
			} else {
				try {
					itf = Table.newInterface(uri, tag, qname, attrs);
				} catch (final Exception e) {
				}
			}
		}

		/**
		 * This does nothing.
		 *
		 * @see XMLReader#read(InputStream)
		 */
		@Override
		public void xmlElementStop(final String uri, final String tag, final String qname) throws SAXException {

			logger.trace("     xmlElementStop()  qname=\"" + qname + "\"");

			if (uri == null) {
				throw new SAXException("URI was not defined");
			}
			try {
				super.xmlElementStop(uri, tag, qname);
			} catch (final XMLEndParseException e) {
			}
		}

		/**
		 * This retrieves this entry UID
		 *
		 * @return this entry UID
		 */
		public UID getUID() {
			try {
				return itf.getUID();
			} catch (final Exception e) {
				return null;
			}
		}

		/**
		 * This retrieves this entry URI
		 *
		 * @return this entry URI
		 */
		public URI getURI() {
			return uri;
		}

		/**
		 * This retrieves this entry UID
		 *
		 * @return this entry TableInterface
		 */
		public Table getInterface() {
			try {
				return itf;
			} catch (final Exception e) {
				return null;
			}
		}

		/**
		 * This calls toString(false)
		 */
		@Override
		public String toString() {
			return toString(false);
		}

		/**
		 * This retrieves String representation
		 *
		 * @return this object String representation
		 * @see xtremweb.common.Table#toString(boolean)
		 */
		@Override
		public String toString(final boolean csv) {
			String ret = "";

			if (itf != null) {
				ret += itf.toString(csv);
			}

			return ret;
		}

		/**
		 * This retrieves the content path
		 *
		 * @see #content
		 * @since 8.0.0
		 */
		public File getContent() {
			return this.content;
		}

		/**
		 * This sets the content path
		 *
		 * @see #content
		 * @param c
		 *            is the new content path
		 * @since 8.0.0
		 */
		public void setContent(final File c) {
			this.content = c;
		}

		/**
		 * This sets the default content path calculated from UID
		 *
		 * @see #content
		 * @since 8.0.0
		 */
		public File setContent() throws IOException {
			final UID uid = getUID();
			this.content = new File(XWTools.createDir(contentDir, uid), uid.toString());
			return this.content;
		}
	}

	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = "XWCache";

	/**
	 * This is the size column index
	 */
	private static final int SIZE = 0;
	/**
	 * This is max entries the cache can content Default 10K entries This may
	 * also be set from config file, if any
	 *
	 * @see XWPropertyDefs#CACHESIZE
	 */
	private int maxCacheSize = Integer.parseInt(XWPropertyDefs.CACHESIZE.defaultValue());
	/**
	 * This stores objects using UID as keys
	 */
	private final Hashtable<URI, CacheEntry> cache;
	/**
	 * This is the configuration as read from config file
	 */
	private XWConfigurator config;
	/**
	 * This is the cache file name
	 */
	public static final String CACHENAME = "XWHEP." + XWRole.getMyRole() + ".cache";
	/**
	 * This is the cache file name
	 */
	public static final String CONTENTDIRNAME = "XWHEP." + XWRole.getMyRole() + ".cache.contents";
	/**
	 * This is the cache file
	 */
	private File cacheFile;
	/**
	 * This is the directory where data contents are stored
	 */
	private File contentDir;
	/**
	 * This helps to read/write cache file
	 */
	private StreamIO streamer;
	/**
	 * This is the least recently used cache entry
	 *
	 * @since 5.8.0
	 */
	private CacheEntry leastRecentlyUsedEntry;

	/**
	 * This constructs a new cache and retrieves stored values from disk
	 *
	 * @see #read()
	 */
	public Cache() {
		super(THISTAG, SIZE);
		setAttributeLength(SIZE);

		setColumnAt(SIZE, "SIZE");

		config = null;
		cache = new Hashtable<>(100);
		cacheFile = null;
		try {
			contentDir = File.createTempFile("xw-junit", "cache");
		} catch (final IOException e1) {
			contentDir = null;
		}
		streamer = null;
		maxCacheSize = Integer.parseInt(XWPropertyDefs.CACHESIZE.defaultValue());

		leastRecentlyUsedEntry = null;

		Runtime.getRuntime().addShutdownHook(new Thread("XWCacheCleaner") {
			/**
			 * This cleans the cache at shut down time
			 */
			@Override
			public void run() {
				flush();
				try {
					if (cacheFile != null) {
						cacheFile.delete();
					}
					XWTools.deleteDir(contentDir);
				} catch (final IOException e) {
				}
			}
		});
	}

	/**
	 * This constructs a new cache and retrieves stored values from disk
	 *
	 * @param c
	 *            is the configuration as read from config file
	 * @see #read()
	 * @exception IOException
	 *                is thrown if cache directory can not be created
	 */
	public Cache(final XWConfigurator c) throws IOException {
		this();
		config = c;
		cacheFile = null;
		contentDir = null;
		contentDir = new File(config.getCacheDir(), CONTENTDIRNAME);
		try {
			XWTools.checkDir(contentDir);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
		maxCacheSize = config.getInt(XWPropertyDefs.CACHESIZE);
		streamer = null;
		read();
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 */
	public Cache(final DataInputStream input) throws IOException, SAXException {
		this();
		try (final XMLReader reader = new XMLReader(this)) {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This is called on program termination (CTRL+C) This flushes and closes
	 * cache file
	 */
	protected void flush() {
		try {
			logger.trace("XWCache flush");
			if ((cacheFile != null) && (streamer != null) && (cacheFile.exists())) {

				streamer.writeBytes("</" + THISTAG + ">");
				streamer.close();
			}
		} catch (final Exception e) {
			logger.error("Caught exception, can't flush", e);
		}
		streamer = null;
	}

	/**
	 * This clears cache : memory and disk
	 */
	@Override
	public synchronized void clear() {
		try {
			logger.trace("XWCacheCleaner");
			cache.clear();
			flush();
			leastRecentlyUsedEntry = null;

			if ((cacheFile != null) && (cacheFile.exists())) {
				cacheFile.delete();
			}

			XWTools.deleteDir(contentDir);
			XWTools.checkDir(contentDir);
			write();
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
			System.exit(XWReturnCode.FATAL.ordinal());
		}
		notifyAll();
	}

	/**
	 * This retrieves this object attributes
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 */
	@Override
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}

		for (int a = 0; a < attrs.getLength(); a++) {
			final String attribute = attrs.getQName(a);
			final String value = attrs.getValue(a);
			logger.trace("     attribute #" + a + ": name=\"" + attribute + "\"" + ", value=\"" + value + "\"");
		}
	}

	/**
	 * This serializes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		final StringBuilder ret = new StringBuilder("<" + THISTAG + " " + "SIZE=\"" + cache.size() + "\" >");
		for (final CacheEntry entry : cache.values()) {
			try {
				ret.append(entry.toXml());
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}

		ret.append("</" + THISTAG + ">");

		return ret.toString();
	}

	/**
	 * This writes this object XML representation to output stream
	 *
	 * @param o
	 *            is the output stream to write to
	 */
	@Override
	public void toXml(final DataOutputStream o) throws IOException {

		final String openTag = "<" + THISTAG + " " + "SIZE=\"" + cache.size() + "\" >";
		o.write(openTag.getBytes(XWTools.UTF8));

		for (final CacheEntry entry : cache.values()) {
			try {
				entry.toXml(o);
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}

		final String closeTag = "</" + THISTAG + ">";
		o.write(closeTag.getBytes(XWTools.UTF8));
	}

	/**
	 * This is current CacheEntry beeing retrieved from XML description
	 */
	private CacheEntry currentEntry = null;

	/**
	 * This is called to decode XML elements
	 *
	 * @see XMLReader#read(InputStream)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			super.xmlElementStart(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		logger.trace("  xmlElementStart()   qname=\"" + qname + "\"");

		if (qname.compareToIgnoreCase(THISTAG) == 0) {
			fromXml(attrs);
		} else {
			if (currentEntry == null) {
				currentEntry = new CacheEntry();
			}
			currentEntry.xmlElementStart(uri, tag, qname, attrs);
		}
	}

	/**
	 * This does nothing.
	 *
	 * @see XMLReader#read(InputStream)
	 */
	@Override
	public void xmlElementStop(final String saxuri, final String tag, final String qname) throws SAXException {

		logger.trace("     xmlElementStop()  qname=\"" + qname + "\"");

		if (qname.compareToIgnoreCase(CacheEntry.THISTAG) == 0) {

			currentEntry.xmlElementStop(saxuri, tag, qname);

			final URI uri = currentEntry.getURI();
			cache.remove(uri);
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> Cache : should be tested");
			put(currentEntry);
			currentEntry = null;
		}

		try {
			super.xmlElementStop(saxuri, tag, qname);
		} catch (final XMLEndParseException e) {
		}
	}

	/**
	 * This retrieves this cache content from cache file. This rewrites cache to
	 * file to remove duplicated and unnecessary entries
	 *
	 * @see #streamer
	 */
	protected void read() {
		logger.trace("read()");

		if ((cacheFile != null) && (cacheFile.exists())) {
			try (final FileInputStream fis = new FileInputStream(cacheFile)) {
				final DataInputStream input = new DataInputStream(fis);
				try (final XMLReader reader = new XMLReader(this)) {
					reader.read(input);
				}
			} catch (final InvalidKeyException | IOException | SAXException e) {
				logger.error("Caught exception: ", e);
			} finally {
				if (streamer != null) {
					streamer.close();
				}
				streamer = null;
				logger.trace("XWHEP Cache : " + cache.size() + " objects cached");
			}
		}

		write();
	}

	/**
	 * This writes this cache content to cache file
	 */
	protected void write() {
		logger.trace("write()");

		if (cacheFile != null) {
			try {
				final FileOutputStream fos = new FileOutputStream(cacheFile);
				if (streamer == null) {
					final DataOutputStream output = new DataOutputStream(fos);
					streamer = new StreamIO(output, null, 10240, config.nio());
				}

				final String header = "<" + THISTAG + " " + "SIZE=\"" + cache.size() + "\" >";
				streamer.writeBytes(header);
				for (final CacheEntry entry : cache.values()) {
					writeEntry(entry);
				}
			} catch (final IOException e) {
				logger.error("Caught exception: ", e);
			}
		}
	}

	private void writeEntry(CacheEntry entry) {
		try {
			write(entry);
		} catch (final Exception e) {
			logger.error("Caught exception: ", e);
		}
	}

	/**
	 * This appends the argument to cache file
	 *
	 * @param entry
	 *            is the object to write
	 */
	protected void write(final CacheEntry entry) throws IOException {
		final String str = entry.toXml();
		if (streamer != null) {
			logger.trace("Writing to cache : " + str);
			streamer.writeBytes(str);
		} else {
			logger.trace("streamer is null : not writing to disk");
		}
	}

	/**
	 * This adds/updates a new entry to cache and appends it to cache file. This
	 * creates an new URI for this TableInterface. If cache size > maxCacheSize,
	 * this first calls removeLeastRecentlyUsedEntry()
	 *
	 * @param itf
	 *            is the interface to cache
	 * @see #write(CacheEntry)
	 * @see #removeLeastRecentlyUsedEntry()
	 */
	public void add(final Table itf, final URI uri) throws IOException {
		if ((itf == null) || (uri == null)) {
			return;
		}
		if (cache.size() > maxCacheSize) {
			removeLeastRecentlyUsedEntry();
		}

		try {
			final CacheEntry entry = new CacheEntry(itf, uri);
			write(entry);
			put(entry);
		} catch (final Exception e) {
			logger.error("Caught exception, Unable to insert entry in cache", e);
		}
	}

	/**
	 * This adds/updates a new entry to cache and appends it to cache file. If
	 * uri is not an XtremWeb one (http etc.), this creates a new empty
	 * DataInterface. If cache size > maxCacheSize, this first calls
	 * removeLeast()
	 *
	 * @param uri
	 *            is the uri to cache
	 * @see #write(CacheEntry)
	 * @see #removeLeastRecentlyUsedEntry()
	 */
	public void add(final URI uri) throws IOException {

		if (uri == null) {
			return;
		}
		UID uid = new UID();
		try {
			uid = uri.getUID();
		}catch(final Exception e) {
		}
		add(new DataInterface(uid), uri);
	}

	/**
	 * This puts an entry into cache
	 */
	private synchronized void put(final CacheEntry entry) {
		final URI uri = entry.getURI();
		logger.trace("Put to cache " + entry.toXml());
		updateEntryDateAccess(entry);
		cache.put(uri, entry);
		notifyAll();
	}

	/**
	 * This retrieves an entry from cache
	 */
	public Table get(final URI uri) {
		if (uri == null) {
			return null;
		}
		final CacheEntry entry = cache.get(uri);
		if (entry == null) {
			return null;
		}
		logger.trace("Retreived from cache " + entry.toXml());
		updateEntryDateAccess(entry);
		return entry.getInterface();
	}

	/**
	 * This retrieves an app form cache by its name
	 *
	 * @param name
	 *            is the name of the app to retrieve
	 * @return the app or null on error
	 */
	public AppInterface appByName(final String name) {
		for (final CacheEntry entry : cache.values()) {
			final Table itf = entry.getInterface();

			if (!(itf instanceof AppInterface)) {
				continue;
			}
			if (name.compareTo(((AppInterface) itf).getName()) == 0) {
				updateEntryDateAccess(entry);
				return (AppInterface) itf;
			}
		}
		return null;
	}

	/**
	 * This retrieves an usergroup form cache by its label
	 *
	 * @param label
	 *            is the label of the usergroup to retrieve
	 * @return the usergroup or null on error
	 */
	public UserGroupInterface userGroupByLabel(final String label) {
		for (final CacheEntry entry : cache.values()) {
			final Table itf = entry.getInterface();

			if (!(itf instanceof UserInterface)) {
				continue;
			}
			if (label.compareTo(((UserGroupInterface) itf).getLabel()) == 0) {
				updateEntryDateAccess(entry);
				return (UserGroupInterface) itf;
			}
		}
		return null;
	}

	/**
	 * This reterives a user form cache by its login
	 *
	 * @param login
	 *            is the login of the user to retrieve
	 * @return the user or null on error
	 */
	public UserInterface userByLogin(final String login) {

		for (final CacheEntry entry : cache.values()) {
			final Table itf = entry.getInterface();
			if (!(itf instanceof UserInterface)) {
				continue;
			}
			if (login.compareTo(((UserInterface) itf).getLogin()) == 0) {
				updateEntryDateAccess(entry);
				return (UserInterface) itf;
			}
		}
		return null;
	}

	/**
	 * This locks an entry. The entry must be already cached.
	 *
	 * @since 7.0.0
	 * @see CacheEntry#lock()
	 */
	public void lock(final URI uri) throws IOException {

		if (uri == null) {
			logger.error("Cache#lock : uri cannot be null");
			return;
		}

		final CacheEntry entry = cache.get(uri);
		if (entry == null) {
			logger.error("Cache#lock : can't retrieve " + uri);
			return;
		}

		entry.lock();
		logger.debug("locked " + uri);
	}

	/**
	 * This unlocks an entry. The entry must be already cached.
	 *
	 * @since 7.0.0
	 * @see CacheEntry#unlock()
	 */
	public void unlock(final URI uri) throws IOException {
		if (uri == null) {
			logger.error("Cache#unlock : uri cannot be null");
			return;
		}

		final CacheEntry entry = cache.get(uri);
		if (entry == null) {
			logger.error("Cache#unlock : can't retrieve " + uri);
			return;
		}

		entry.unlock();
		logger.debug("unlocked " + uri);
	}

	/**
	 * This set content file for the given UID. The object must be already
	 * cached.
	 *
	 * @since 8.0.0
	 * @exception IOException
	 *                is thrown on IO error or of entry cache does not exist
	 */
	public void setContentFile(final URI uri, final File c) throws IOException {
		final CacheEntry entry = cache.get(uri);
		if (entry == null) {
			throw new IOException("Cache#setContentFile : can't retrieve cache entry " + uri);
		}
		entry.setContent(c);
	}

	/**
	 * This retrieves content file for the given UID The object must be already
	 * cached
	 *
	 * @return a File where to store content for the cached object, null
	 *         otherwise
	 */
	public File getContentFile(final URI uri) throws IOException {

		if (uri == null) {
			logger.error("Cache#getContentFile : uri cannot be null");
			return null;
		}

		final CacheEntry entry = cache.get(uri);
		if (entry == null) {
			logger.warn("Cache#getContentFile : can't retrieve cache entry " + uri);
			return null;
		}

		File content = entry.getContent();
		if (content == null) {
			content = entry.setContent();
		}

		return content;
	}

	/**
	 * This updates entry date access and calls setLeastRecentlyUsedEntry(entry)
	 *
	 * @param entry
	 *            is the entry to update date access
	 * @see #setLeastRecentlyUsedEntry(CacheEntry)
	 * @since 5.8.0
	 */
	private void updateEntryDateAccess(final CacheEntry entry) {
		if (entry == null) {
			return;
		}
		entry.setLastAccess();
		setLeastRecentlyUsedEntry(entry);
	}

	/**
	 * This sets the least recently used element. This updates
	 * leastRecentlyUsedEntry accordingly to entry date access
	 *
	 * @param entry
	 *            is the element to date last date access
	 * @see #leastRecentlyUsedEntry
	 * @since 5.8.0
	 */
	private void setLeastRecentlyUsedEntry(final CacheEntry entry) {
		if (entry == null) {
			return;
		}

		if (leastRecentlyUsedEntry == null) {
			leastRecentlyUsedEntry = entry;
		} else {
			if (entry.lastAccess().before(leastRecentlyUsedEntry.lastAccess())) {
				leastRecentlyUsedEntry = entry;
			}
		}
	}

	/**
	 * This retrieves the least recently used element from cache and stores it
	 * in leastRecentlyUsedEntry
	 *
	 * @since 5.8.0
	 */
	protected void retrieveLeastRecentlyUsedEntry() throws IOException {

		final Enumeration<URI> keys = cache.keys();
		while (keys.hasMoreElements()) {
			setLeastRecentlyUsedEntry(cache.get(keys.nextElement()));
		}
	}

	/**
	 * This removes the least recently used element from cache. Then, this
	 * retrieves the new least recently used element from cache
	 *
	 * @since 5.8.0
	 */
	protected void removeLeastRecentlyUsedEntry() throws IOException {
		if (leastRecentlyUsedEntry == null) {
			retrieveLeastRecentlyUsedEntry();
		}

		final CacheEntry entry = leastRecentlyUsedEntry;
		logger.trace("removeLeastRecentlyUsedEntry() removing " + entry.getURI());
		leastRecentlyUsedEntry = null;
		remove(entry);
	}

	/**
	 * This calls remove(entry.getUID())
	 *
	 * @param entry
	 *            is the entry to remove
	 * @exception IOException
	 *                is thrown on error
	 */
	public void remove(final CacheEntry entry) throws IOException {
		if (entry == null) {
			return;
		}
		remove(entry.getURI());
	}

	/**
	 * This removes an entry from cache given its URI If uri to remove is
	 * leastRecentlyUsedEntry one, this calls retrieveLeastRecentlyUsedEntry()
	 *
	 * @param uri
	 *            is the URI of the entry to remove
	 * @exception IOException
	 *                is thrown on error
	 * @see #retrieveLeastRecentlyUsedEntry()
	 */
	public synchronized void remove(final URI uri) throws IOException {
		final CacheEntry entry = cache.get(uri);
		logger.trace("Removing from cache " + uri);
		if (entry == null) {
			notifyAll();
			return;
		}

		final File f = getContentFile(uri);

		write(entry);
		cache.remove(uri);

		if (f != null) {
			logger.trace("deleting " + f);
			f.delete();
		}

		if ((leastRecentlyUsedEntry == null) || (leastRecentlyUsedEntry.getURI().equals(uri))) {
			retrieveLeastRecentlyUsedEntry();
		}

		notifyAll();
	}

	/**
	 * This dumps this cache content to stdout
	 */
	public void dump() {
		System.out.println("XWHEP cache");
		for (final CacheEntry entry : cache.values()) {
			try {
				System.out.println(entry.getInterface().toXml());
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}
	}

	/**
	 * This calls toString(false)
	 */
	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * This retrieves String representation
	 *
	 * @return this object String representation
	 * @see xtremweb.common.Table#toString(boolean)
	 */
	@Override
	public String toString(final boolean csv) {
		final StringBuilder ret = new StringBuilder();

		for (final Object entry : cache.values()) {
			try {
				ret.append(((CacheEntry) entry).getInterface().toXml());
			} catch (final Exception e) {
				logger.error("Caught exception: ", e);
			}
		}

		return ret.toString();
	}

	/**
	 * This is for testing only
	 */
	public static void main(final String[] argv) {

		try {
			if (argv.length < 1) {
				System.out.println("Usage : java -cp " + XWTools.JARFILENAME + " <configFile> [anXmlDefinition]");
				System.exit(1);
			}

			final XWConfigurator config = new XWConfigurator(argv[0], false);
			new Cache(config);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
