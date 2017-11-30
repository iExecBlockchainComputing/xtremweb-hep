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

package xtremweb.security;

/**
 * Author : Oleg Lodygensky
 * Date   : July 2010
 *
 * An X509 proxy is a file containing one or more X509 public certificates linked
 * by IssuerDN/SubjectDN and a private key.
 *
 * This class creates a linked list of X509 certificates linked by IssuerDN
 *
 * @since 7.0.0
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;


public final class X509Proxy {
	public static final String BEGINCERT = "-----BEGIN CERTIFICATE-----";
	public static final String ENDCERT = "-----END CERTIFICATE-----";
	public static final String BEGINPRIVATEKEY = "-----BEGIN RSA PRIVATE KEY-----";
	public static final String ENDPRIVATEKEY = "-----END RSA PRIVATE KEY-----";

	public static final String BEGINCERT_NOSPACE = "-----BEGIN_CERTIFICATE-----";
	public static final String ENDCERT_NOSPACE = "-----END_CERTIFICATE-----";
	public static final String BEGINPRIVATEKEY_NOSPACE = "-----BEGIN_RSA_PRIVATE_KEY-----";
	public static final String ENDPRIVATEKEY_NOSPACE = "-----END_RSA_PRIVATE_KEY-----";

	private static final Logger logger = Logger.getLogger(X509Proxy.class);
	/**
	 * This contains certs found from X509 proxy, linked by IssuerDN
	 */
	private final LinkedList<X509Certificate> certsList;
	/**
	 * This contains the cert file content
	 */
	private String content;
	/**
	 * This is the certificate factory
	 */
	private final CertificateFactory certFactory;

	/**
	 * This instanciates certsList and certFactory
	 *
	 * @exception CertificationException
	 *                - on factory.getInstance() error
	 */
	public X509Proxy() throws CertificateException {
		content = "";
		certsList = new LinkedList<>();
		certFactory = CertificateFactory.getInstance("X.509");
	}

	/**
	 * This retrieves X509 public keys from X509 proxy and put them in a linked
	 * list where one element is the issuer of the following, ordered from least
	 * ot most trusted certs.
	 *
	 * @param certFileName
	 *            is the name of the user X509 proxy file
	 * @exception IOException
	 *                - on file access error
	 * @exception CertificateException
	 *                - on certificate error
	 */
	public X509Proxy(final String certFileName) throws IOException, CertificateException {
		this();
		File f = null;
		try {
			f = new File(certFileName);
			getCertificates(f);
		} finally {
			f = null;
		}
	}

	/**
	 * This retrieves X509 public keys from X509 proxy and put them in a linked
	 * list where one element is the issuer of the following, ordered from least
	 * ot most trusted certs.
	 *
	 * @param f
	 *            contains the user X509 proxy
	 * @exception IOException
	 *                - on file access error
	 * @exception CertificateException
	 *                - on certificate error
	 */
	public X509Proxy(final File f) throws IOException, CertificateException {
		this();
		getCertificates(f);
	}

	/**
	 * This retrieves X509 public keys from X509 proxy and put them in a linked
	 * list where one element is the issuer of the following, ordered from least
	 * ot most trusted certs.
	 *
	 * @param is
	 *            - the input stream to reader X509 proxy from
	 * @exception IOException
	 *                - on file access error
	 * @exception CertificateException
	 *                - on certificate error
	 */
	public X509Proxy(final InputStream is) throws IOException, CertificateException {
		this();
		getCertificates(is);
	}

	/**
	 * This retrieves X509 public keys from X509 proxy and put them in a linked
	 * list where one element is the issuer of the following, ordered from least
	 * ot most trusted certs.
	 *
	 * @param certFile
	 *            contains the user X509 proxy
	 * @exception IOException
	 *                - on file access error
	 * @exception CertificateException
	 *                - on certificate error
	 */
	private void getCertificates(final File certFile) throws IOException, CertificateException {

		if (certFile == null) {
			throw new IOException("file is null");
		}
		if (!certFile.exists()) {
			throw new IOException("file not found : " + certFile);
		}

		try (final FileReader certFileReader = new FileReader(certFile);
				final BufferedReader reader = new BufferedReader(certFileReader)) {

			for (String ligne = reader.readLine(); ligne != null; ligne = reader.readLine()) {
				content = content.concat(ligne + "\n");
			}
		}

		try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
			getCertificates(inputStream);
		}
	}

	/**
	 * This retrieves X509 public keys from X509 proxy and put them in a linked
	 * list where one element is the issuer of the following, ordered from least
	 * at most trusted certs.
	 * 
	 * @see getCertificate(BufferedInputStream)
	 * @param inputStream
	 *            - the input stream to reader X509 proxy from
	 * @exception IOException
	 *                - on file access error
	 * @exception CertificateException
	 *                - on certificate error
	 */
	private void getCertificates(final InputStream inputStream) throws IOException, CertificateException {

		try (final BufferedInputStream bis = new BufferedInputStream(inputStream)) {
			while (bis.available() > 0) {
				getCertificate(bis);
			}
		}

		//
		// dump
		//
		logger.info("> Found certificates");

		final Iterator<X509Certificate> certsit = certsList.listIterator();
		while (certsit.hasNext()) {
			final X509Certificate cert = certsit.next();
			logger.info(">> Subject=\"" + cert.getSubjectDN() + "\" Issuer=\"" + cert.getIssuerDN() + "\"");
		}
	}

	/**
	 * This retrieves a certificate from input stream
	 * 
	 * @param bis
	 *            input stream
	 * @throws CertificateException
	 */
	private void getCertificate(final BufferedInputStream bis) throws CertificateException {
		try {
			final X509Certificate theCert = (X509Certificate) certFactory.generateCertificate(bis);
			X500Principal subject = theCert.getSubjectX500Principal();
			final String certSubjectName = subject.getName();
			X500Principal issuer = theCert.getIssuerX500Principal();
			final String certIssuerName = issuer.getName();

			logger.trace(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
			logger.trace("SubjectDN = " + certSubjectName);
			logger.trace("IssuerDN  = " + certIssuerName);
			logger.trace("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< [" + size() + "]  ");

			theCert.checkValidity();

			if (size() == 0) {
				logger.trace(">>>>>>>>>> add " + certSubjectName);
				add(theCert);
				return;
			}

			final X509Certificate firstcert = getFirst();
			final X509Certificate lastcert = getLast();

			subject = firstcert.getSubjectX500Principal();
			final String firstSubjectName = subject.getName();
			subject = lastcert.getSubjectX500Principal();
			final String lastSubjectName = subject.getName();
			issuer = lastcert.getIssuerX500Principal();
			final String lastIssuerName = issuer.getName();

			if (certIssuerName.compareTo(firstSubjectName) == 0) {
				addFirst(theCert);
				logger.trace(">>>>>>>>>> addFirst " + certSubjectName + " is issued by " + firstSubjectName);
				return;
			}

			if (certSubjectName.compareTo(lastIssuerName) == 0) {
				addLast(theCert);
				logger.trace(">>>>>>>>>> addLast  " + certSubjectName + " is issuer of " + lastSubjectName);
				return;
			}

			throw new CertificateParsingException(certSubjectName + " is not in the X509 path ?!?!");
		} catch (final CertificateParsingException e) {
			// this may happen since the X509 proxy also contains X509
			// proxy private key
		}
	}

	/**
	 * This retrieves the X509 proxy file content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * This adds a new entry
	 *
	 * @param v
	 *            is the CACertPath to add
	 */
	public void add(final X509Certificate v) {
		certsList.add(v);
	}

	/**
	 * This adds a new entry at the beginning of the list
	 *
	 * @param v
	 *            is the CACertPath to add
	 */
	public void addFirst(final X509Certificate v) {
		certsList.addFirst(v);
	}

	/**
	 * This adds a new entry at the end of the list
	 *
	 * @param v
	 *            is the CACertPath to add
	 */
	public void addLast(final X509Certificate v) {
		certsList.addLast(v);
	}

	/**
	 * This retrieves the certificates list
	 */
	public List<X509Certificate> getCertificates() {
		return certsList;
	}

	/**
	 * This retrieves the first entry from list
	 */
	public X509Certificate getFirst() {
		return certsList.getFirst();
	}

	/**
	 * This retrieves the last entry from list which is the most trusted cert of
	 * this proxy
	 */
	public X509Certificate getLast() {
		return certsList.getLast();
	}

	/**
	 * This retrieves the subject of the most trusted cert of this proxy
	 *
	 * @see #getLast()
	 */
	public X500Principal getSubject() {
		final X509Certificate last = getLast();
		if (last == null) {
			return null;
		}
		return last.getSubjectX500Principal();
	}

	/**
	 * This retrieves the subject distinguished name of this proxy
	 *
	 * @see #getSubject()
	 */
	public String getSubjectName() {
		final X500Principal subject = getSubject();
		if (subject == null) {
			return null;
		}
		return subject.getName();
	}

	/**
	 * This retrieves the subject of the most trusted cert of this proxy
	 *
	 * @see #getLast()
	 */
	public X500Principal getIssuer() {
		final X509Certificate last = getLast();
		if (last == null) {
			return null;
		}
		return last.getIssuerX500Principal();
	}

	/**
	 * This retrieves the subject distinguished name of this proxy
	 *
	 * @see #getSubject()
	 */
	public String getIssuerName() {
		final X500Principal issuer = getIssuer();
		if (issuer == null) {
			return null;
		}
		return issuer.getName();
	}

	/**
	 * This removes all entries
	 */
	public void clear() {
		certsList.clear();
	}

	/**
	 * This retrieves the size of this vector
	 *
	 * @return the size of this vector
	 */
	public int size() {
		return certsList.size();
	}

	/**
	 * For testing only
	 */
	public static void main(final String[] argv) {
		try {
			final X509Proxy proxy = new X509Proxy(argv[0]);
			System.out.println("proxy.getLast  =" + proxy.getLast().getSubjectX500Principal().getName());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
