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

/**
 *
 * CertificateValidator : validates a X509 certificate against an X509 certification path
 *      using a PKIX CertPathValidator
 *
 * Author: Oleg Lodygensky
 *
 * Environment variables :
 *   - X509_CERT_DIR   must contain the directory where CA cert are stored
 *   - X509_USER_PROXY must contain the user 509 proxy
 *
 * First this constructs list of CA cert paths from X509_CERT_DIR
 * Then this constructs a cert PATH for the X509_USER_PROXY
 * Finally this validates the X509 proxy path against CA cert paths
 *
 * Example
 *    - X509_USER_PROXY contains two public keys
 *        [0] IssuerDN  = CN=Oleg Lodygensky, OU=LAL, O=CNRS, C=FR, O=GRID-FR
 *        [0] SubjectDN = CN=proxy, CN=Oleg Lodygensky, OU=LAL, O=CNRS, C=FR, O=GRID-FR
 *        [1] IssuerDN  = CN=GRID2-FR, O=CNRS, C=FR
 *        [1] SubjectDN = CN=Oleg Lodygensky, OU=LAL, O=CNRS, C=FR, O=GRID-FR
 *
 *    - X509_CERT_DIR must then contains three public keys to create the following path
 *        [0] IssuerDN  = CN=CNRS2-Projets, O=CNRS, C=FR
 *        [0] SubjectDN = CN=GRID2-FR, O=CNRS, C=FR
 *        [1] IssuerDN  = CN=CNRS2, O=CNRS, C=FR
 *        [1] SubjectDN = CN=CNRS2-Projets, O=CNRS, C=FR
 *        [2] IssuerDN  = CN=CNRS2, O=CNRS, C=FR
 *        [2] SubjectDN = CN=CNRS2, O=CNRS, C=FR
 *
 * If X509_CERT_DIR does not contains this path, the X509_USER_PROXY can not be validated
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;


/**
 */
public final class CertificateValidator {

	private static final Logger logger = Logger.getLogger(CertificateValidator.class);

	/**
	 * This class contains CA cert path and its most trusted certificate. Both
	 * are used to validate a certificate through this path
	 */
	private class CACertPath {
		/**
		 * This is the most trusted certificate parameters
		 */
		private PKIXParameters mostTrustedCertParams;
		/**
		 * This is the cert path belonging to the most trusted certificate
		 */
		private LinkedList<X509Certificate> certPathList = null;

		/**
		 *
		 */
		public CACertPath(final X509Certificate c, final File f) throws IOException, CertificateException {

			try (FileInputStream certis = new FileInputStream(f)) {
				mostTrustedCertParams = createParams(c);
				final CertPath certPath = certFactory.generateCertPath(certis, "PKCS7");
				certPathList = (LinkedList<X509Certificate>) certPath.getCertificates();
			} finally {
			}
		}

		/**
		 * @param p
		 *            is the PKIXParameters of the most trusted cert CA
		 * @param cp
		 *            contains the certificate path; it must not contain the
		 *            most trusted cert CA
		 */
		public CACertPath(final PKIXParameters p, final CertPath cp) throws NoSuchAlgorithmException {

			mostTrustedCertParams = p;
			certPathList = (LinkedList<X509Certificate>) cp.getCertificates();
		}

		/**
		 * @param c
		 *            is the most trusted cert CA
		 * @param cp
		 *            contains the certificate path; it must not contain the
		 *            most trusted cert CA
		 */
		public CACertPath(final X509Certificate c, final CertPath cp) throws IOException, CertificateException {

			mostTrustedCertParams = createParams(c);
			certPathList = (LinkedList<X509Certificate>) cp.getCertificates();
		}

		/**
		 * @param c
		 *            is the most trusted cert CA
		 * @param list
		 *            contains the certificates list of the certificate path; it
		 *            must not contain the most trusted cert CA
		 */
		public CACertPath(final X509Certificate c, final LinkedList<X509Certificate> list)
				throws CertificateException, IOException {

			mostTrustedCertParams = createParams(c);
			certPathList = list;
		}

		/**
		 * @param p
		 *            is the PKIXParameters of the most trusted cert CA
		 * @param list
		 *            contains the certificates list of the certificate path; it
		 *            must not contain the most trusted cert CA
		 */
		public CACertPath(final PKIXParameters p, final LinkedList<X509Certificate> list) throws CertificateException {

			mostTrustedCertParams = p;
			certPathList = list;
		}

		/**
		 * This constructor extract the most trusted CA cert from the list. The
		 * most trusted CA cert must be the last cert from the list.
		 *
		 * @param list
		 *            contains the certificates of the path; it must contain the
		 *            most trusted cert CA at last position
		 */
		public CACertPath(final LinkedList<X509Certificate> list) throws CertificateException {

			X509Certificate mostTrustedCert = null;
			try {
				mostTrustedCert = list.removeLast();

				logger.info(">>> CACertPath : Certs of the cert path");

				mostTrustedCertParams = createParams(mostTrustedCert);
				certPathList = list;
				int numcert = 0;
				for (final ListIterator<X509Certificate> certsIterator = list.listIterator(); certsIterator
						.hasNext();) {

					final X509Certificate theCert = certsIterator.next();

					numcert++;

					logger.info("theCert   SubjectDN = " + theCert.getSubjectDN());
					logger.info("theCert   IssuerDN  = " + theCert.getIssuerDN());
				}

				logger.info(">>> CACertPath : Most Trusted");

				logger.info("mostTrustedCert SubjectDN = " + mostTrustedCert.getSubjectDN());
				logger.info("mostTrustedCert IssuerDN  = " + mostTrustedCert.getIssuerDN());

				logger.info("<<< CACertPath");
			} catch (final Exception e) {
				throw new CertificateException(e.toString());
			} finally {
				mostTrustedCert = null;
			}
		}

		/**
		 * This validates an X509 proxy through the cert path
		 *
		 * @param proxy
		 *            contains public certificate from X50 proxy
		 */
		public CertPathValidatorResult validate(final X509CertPath proxy) throws CertPathValidatorException {

			try {
				final X509Certificate proxyLast = proxy.getLast();
				final X509Certificate pathFirst = certPathList.getFirst();

				if (proxyLast.getIssuerDN().toString().compareTo(pathFirst.getSubjectDN().toString()) != 0) {
					throw new CertPathValidatorException(
							pathFirst.getSubjectDN() + " is not the issuer of " + proxyLast.getIssuerDN());
				}
				logger.info(">> Validating proxy most trusted (" + proxyLast.getIssuerDN()
						+ ") is issued by cert path least trusted " + pathFirst.getSubjectDN());
				logger.info(">> Validating MTC " + mostTrustedCertParams);

				certPathList.add(0, proxyLast);

				final ListIterator<X509Certificate> certsIterator = certPathList.listIterator();

				while (certsIterator.hasNext()) {
					final X509Certificate theCert = certsIterator.next();
					logger.info("Validating >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
					logger.info("CPL   SubjectDN = " + theCert.getSubjectDN());
					logger.info("CPL   IssuerDN  = " + theCert.getIssuerDN());
					System.out.print("CPL   keyUsage  = ");
					final boolean[] ku = theCert.getKeyUsage();
					for (int kui = 0; kui < 9; kui++) {
						System.out.print((ku[kui] == false ? "0" : "1"));
					}
					logger.info("Validating <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

				}

				final CertPath certPath = certFactory.generateCertPath(certPathList);
				return pathValidator.validate(certPath, mostTrustedCertParams);
			} catch (final Exception e) {
				throw new CertPathValidatorException(e);
			}
		}
	}

	/**
	 * This class contains public certificates found from X509 proxy linked by
	 * IssuerDN
	 */
	private class X509CertPath {
		/**
		 * This contains certs found from X509 proxy, linked by IssuerDN
		 */
		private final LinkedList<X509Certificate> certsFromProxy;

		/**
		 *
		 */
		public X509CertPath() {
			certsFromProxy = new LinkedList<>();
		}

		/**
		 * This adds a new entry
		 *
		 * @param v
		 *            is the CACertPath to add
		 */
		public void add(final X509Certificate v) {
			certsFromProxy.add(v);
		}

		/**
		 * This adds a new entry at the beginning of the list
		 *
		 * @param v
		 *            is the CACertPath to add
		 */
		public void addFirst(final X509Certificate v) {
			certsFromProxy.addFirst(v);
		}

		/**
		 * This adds a new entry at the end of the list
		 *
		 * @param v
		 *            is the CACertPath to add
		 */
		public void addLast(final X509Certificate v) {
			certsFromProxy.addLast(v);
		}

		/**
		 * This retreive the certificates list
		 */
		public LinkedList<X509Certificate> getCertificates() {
			return certsFromProxy;
		}

		/**
		 * This retreive the first entry from list
		 */
		public X509Certificate getFirst() {
			return certsFromProxy.getFirst();
		}

		/**
		 * This retreive the last entry from list
		 */
		public X509Certificate getLast() {
			return certsFromProxy.getLast();
		}

		/**
		 * This removes all entries
		 */
		public void clear() {
			certsFromProxy.clear();
		}

		/**
		 * This retreives the size of this vector
		 *
		 * @return the size of this vector
		 */
		public int size() {
			return certsFromProxy.size();
		}
	}

	/**
	 * This is the certificate factory to retreive certificate from file,
	 * certificate paths etc.
	 */
	private CertificateFactory certFactory = null;
	/**
	 * This is the certificate path validator
	 */
	private CertPathValidator pathValidator = null;
	/**
	 * This is the vector containing cert paths
	 */
	private final Vector<CACertPath> caCertPaths;

	/**
	 * This constructs a new CertificateValidator
	 *
	 * @exception CertificateException
	 *                - on certificate parsing error
	 * @exception NoSuchAlgorithmeException
	 *                - if no Provider supports X509 and PKIX (this should never
	 *                occur)
	 * @exception IOException
	 *                - if X509_CERT_DIR is not set or does no denote a
	 *                directory of certificates
	 */
	public CertificateValidator() throws CertificateException, IOException, NoSuchAlgorithmException {

		certFactory = CertificateFactory.getInstance("X.509");
		pathValidator = CertPathValidator.getInstance("PKIX");

		String cadir = null;

		if (System.getenv("X509_CERT_DIR") != null) {
			cadir = System.getenv("X509_CERT_DIR");
		}
		if (System.getProperty("X509_CERT_DIR") != null) {
			cadir = System.getProperty("X509_CERT_DIR");
		}

		if (cadir == null) {
			throw new IOException("X509_CERT_DIR is not set");
		}

		caCertPaths = createPaths(cadir);
		cadir = null;
	}

	/**
	 * This validates an user X509 proxy on the certificate path
	 *
	 * @param userCertFileName
	 *            contains the user X509 proxy to validate among the cert path
	 * @exception IOException
	 *                is thrown on file access error
	 * @exception CertificateException
	 *                is thrown on certificate error
	 * @exception CertPathValidatorException
	 *                is thrown on validation error
	 */
	public void validate(final String userCertFileName)
			throws IOException, CertificateException, CertPathValidatorException {

		validate(new File(userCertFileName));
	}

	/**
	 * This validates an X509 proxy through all cert paths
	 *
	 * @param userCertFile
	 *            contains the user X509 cert to validate against the cert path
	 * @exception IOException
	 *                - file access error
	 * @exception CertificateException
	 *                - on certificate error
	 * @exception CertPathValidatorException
	 *                - on validation error
	 */
	public void validate(final File userCertFile) throws IOException, CertificateException, CertPathValidatorException {
		final Enumeration<CACertPath> listsEnum = caCertPaths.elements();
		boolean validated = false;

		logger.info("Validating");

		final X509CertPath certsFromProxy = getCertsFromX509(userCertFile);

		while (listsEnum.hasMoreElements() && !validated) {
			try {
				final CACertPath cacertpath = listsEnum.nextElement();
				cacertpath.validate(certsFromProxy);
				validated = true;
			} catch (final CertPathValidatorException e) {
				logger.info(e.toString());
			}
		}
		if (!validated) {
			throw new CertPathValidatorException("not validated through all CA cert paths");
		}

		logger.info("Validated");
	}

	/**
	 * This retreives X509 public keys from X509 proxy and put them in a linked
	 * list where one element is the issuer if the following
	 *
	 * @param userCertFile
	 *            contains the user X509 proxy to validate among the cert path
	 * @exception IOException
	 *                is thrown on file access error
	 * @exception CertificateException
	 *                is thrown on certificate error
	 * @exception CertPathValidatorException
	 *                is thrown on validation error
	 */
	X509CertPath getCertsFromX509(final File certFile) throws IOException, CertificateException {

		if (certFile == null) {
			throw new IOException("file is null");
		}
		if (!certFile.exists()) {
			throw new IOException("file not found : " + certFile);
		}

		final X509CertPath ret = new X509CertPath();

		try (FileInputStream inputstream = new java.io.FileInputStream(certFile);
				BufferedInputStream bis = new BufferedInputStream(inputstream)) {

			while (bis.available() > 0) {

				try {
					final X509Certificate theCert = (X509Certificate) certFactory.generateCertificate(bis);
					logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + certFile);
					logger.info("SubjectDN = " + theCert.getSubjectDN());
					logger.info("IssuerDN  = " + theCert.getIssuerDN());
					logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< [" + ret.size() + "]  " + certFile);

					theCert.checkValidity();

					if (ret.size() == 0) {
						logger.info(">>>>>>>>>> add " + theCert.getSubjectDN());
						ret.add(theCert);
						continue;
					}

					final X509Certificate firstcert = ret.getFirst();
					final X509Certificate lastcert = ret.getLast();

					if (theCert.getIssuerDN().toString().compareTo(firstcert.getSubjectDN().toString()) == 0) {
						ret.addFirst(theCert);
						logger.info(">>>>>>>>>> addFirst " + theCert.getSubjectDN() + " is issued by "
								+ firstcert.getSubjectDN());
						continue;
					}

					if (theCert.getSubjectDN().toString().compareTo(lastcert.getIssuerDN().toString()) == 0) {
						ret.addLast(theCert);
						logger.info(">>>>>>>>>> addLast  " + theCert.getSubjectDN() + " is issuer of "
								+ lastcert.getSubjectDN());
						continue;
					}

					throw new CertificateParsingException(
							theCert.getSubjectDN().toString() + " is not in the X509 path ?!?!");
				} catch (final CertificateParsingException e) {
				}
			}
		} catch (final IOException e) {
			throw e;
		}

		return ret;
	}

	/**
	 * This creates a PKIXParameters object that is needed to validate a cert
	 * path. A PKIXParameters object is created from one or more most-trusted
	 * CAs (aka root CA that must be trusted).
	 *
	 * @param anchorPathName
	 *            contains a certificate file or directory path of the most
	 *            trusted CA
	 * @return a PKIXParameters for the most trusted CA
	 */
	public PKIXParameters createParams(final String anchorPathName) throws IOException, CertificateException {
		final File anchorFile = new File(anchorPathName);
		return createParams(anchorFile);
	}

	/**
	 * This creates a PKIXParameters object that is needed to validate a cert
	 * path. Is the given parameter denotes a directory this calls
	 * createParams(File[]), otherwise this calls createParams(File).
	 *
	 * @param anchorFile
	 *            is the certificate file of the most trusted CA
	 * @see #createParams(String)
	 * @return a PKIXParameters for the most trusted CA
	 */
	public PKIXParameters createParams(final File anchorFile) throws IOException, CertificateException {

		try (FileInputStream certis = new FileInputStream(anchorFile)) {
			final X509Certificate anchorCert = (X509Certificate) certFactory.generateCertificate(certis);
			final PKIXParameters ret = createParams(anchorCert);
			ret.setRevocationEnabled(false);
			return ret;
		} finally {
		}
	}

	/**
	 * This creates a PKIXParameters object that is needed to validate a cert
	 * path.
	 *
	 * @param anchorCert
	 *            is the certificate of the most trusted CA
	 * @see #createParams(String)
	 * @return a PKIXParameters for the most trusted CA
	 */
	public PKIXParameters createParams(final X509Certificate anchorCert) throws IOException, CertificateException {

		PKIXParameters ret = null;

		try {
			final TrustAnchor anchorTrust = new TrustAnchor(anchorCert, null);
			final Set<TrustAnchor> anchorTrusts = Collections.singleton(anchorTrust);
			ret = new PKIXParameters(anchorTrusts);
		} catch (final InvalidAlgorithmParameterException e) {
		}
		ret.setRevocationEnabled(false);
		return ret;
	}

	/**
	 * This creates a Vector of certificate paths from certificate file name. If
	 * file name denotes a file, the vector will contain a single cert path, if
	 * any. If file name denotes a directory, the vector will contain a certain
	 * amount of cert paths, if any.
	 *
	 * @param certFilePathName
	 *            is a certificate file or directory path name
	 * @return a vector containing all found cert paths
	 */
	public Vector<CACertPath> createPaths(final String certFilePathName)
			throws NoSuchAlgorithmException, IOException, CertificateException {

		final File certFile = new File(certFilePathName);
		return createPaths(certFile);
	}

	/**
	 * This creates a Vector of certificate paths from given file. If given file
	 * denotes a file, the vector will contain a single cert path, if any (by
	 * calling createPaths(File).) If given file denotes a directory, the vector
	 * will contain a certain amount of cert paths, if any (by calling
	 * createPaths(File[])).
	 *
	 * @param certFile
	 *            is the certificate file
	 * @return a vector containing all found cert paths
	 */
	public Vector<CACertPath> createPaths(final File certFile)
			throws NoSuchAlgorithmException, IOException, CertificateException {

		FileInputStream certis = null;
		Vector<CACertPath> ret = new Vector<>();
		Collection<X509Certificate> certs = null;
		LinkedList<X509Certificate> certslist = null;
		final Exception e = null;
		CACertPath validator = null;

		try {
			if (certFile.isDirectory()) {
				final File[] certFiles = certFile.listFiles();
				ret = createPaths(certFiles);
			} else {
				validator = new CACertPath((X509Certificate) null, certFile);
				ret.add(validator);
			}
		} catch (final CertificateException ce) {
			certs = (Collection<X509Certificate>) (certFactory.generateCertificates(certis));
			certslist = new LinkedList<>(certs);
			validator = new CACertPath((X509Certificate) null, certslist);
			ret.add(validator);
		} finally {
			if (certslist != null) {
				certslist.clear();
			}
			certslist = null;
			if (certs != null) {
				certs.clear();
			}
			certs = null;
			certis = null;
			validator = null;
		}
		return ret;
	}

	/**
	 * This retreives all CA certificate from the given parameter and creates a
	 * vector of linked list of certificates path
	 *
	 * @return a Vector of ProxyValidator
	 */
	public Vector<CACertPath> createPaths(final File[] certs) throws IOException, CertificateException {
		if (certs == null) {
			throw new IOException("certs is null");
		}

		LinkedList<X509Certificate> list = null;
		final Vector<LinkedList<X509Certificate>> lists = new Vector<>();
		final Vector<CACertPath> ret = new Vector<>();
		logger.info("certs.length = " + certs.length);

		for (int i = 0; i < certs.length; i++) {

			logger.info(">");
			logger.info("> Paths found " + lists.size());
			logger.info(">");

			X509Certificate certFromFile = null;
			try (FileInputStream certis = new FileInputStream(certs[i])) {

				certFromFile = (X509Certificate) certFactory.generateCertificate(certis);

				logger.info("> CertFromFile  " + certs[i]);
				logger.info("> IssuerDN  = " + certFromFile.getIssuerDN());
				logger.info("> SubjectDN = " + certFromFile.getSubjectDN());
				logger.info("< CertFromFile  " + certs[i]);
			} catch (final Exception e) {
			}

			if (certFromFile == null) {
				logger.info("certFromFile is null ?!?!?!?!?!");
				continue;
			}

			if (lists.isEmpty()) {
				list = new LinkedList<>();
				logger.info("> add first new list " + certFromFile.getSubjectDN());
				list.add(certFromFile);
				lists.add(list);
				list = null;
				continue;
			}

			boolean inserted = false;

			final Enumeration<LinkedList<X509Certificate>> listsEnum = lists.elements();
			int numlist = 0;
			while (listsEnum.hasMoreElements() && !inserted) {
				try {
					final LinkedList<X509Certificate> cacertsList = listsEnum.nextElement();

					logger.info(">>>");
					logger.info(">>> Path #" + numlist++ + " contains " + cacertsList.size() + " certs");

					if (cacertsList == null) {
						continue;
					}

					final X509Certificate firstcert = cacertsList.getFirst();
					final X509Certificate lastcert = cacertsList.getLast();

					logger.info(">>> firstCert    IssuerDN  = " + firstcert.getIssuerDN());
					logger.info(">>> firstCert    SubjectDN = " + firstcert.getSubjectDN());
					logger.info(">>> lastCert     IssuerDN  = " + lastcert.getIssuerDN());
					logger.info(">>> lastCert     SubjectDN = " + lastcert.getSubjectDN());
					logger.info(">>> certFromFile IssuerDN  = " + certFromFile.getIssuerDN());
					logger.info(">>> certFromFile SubjectDN = " + certFromFile.getSubjectDN());

					if (certFromFile.getSubjectDN().toString().compareTo(firstcert.getSubjectDN().toString()) == 0) {
						logger.info(">>> certFromFile equals firstCert");
						continue;
					}

					if (certFromFile.getSubjectDN().toString().compareTo(lastcert.getSubjectDN().toString()) == 0) {
						logger.info(">>> certFromFile equals lastCert");
						continue;
					}

					if (certFromFile.getIssuerDN().toString().compareTo(firstcert.getSubjectDN().toString()) == 0) {
						cacertsList.addFirst(certFromFile);
						logger.info(">>> addFirst " + certFromFile.getSubjectDN() + " is issued by "
								+ firstcert.getSubjectDN());
						inserted = true;
						break;
					}

					if (certFromFile.getSubjectDN().toString().compareTo(lastcert.getIssuerDN().toString()) == 0) {
						cacertsList.addLast(certFromFile);
						logger.info(">>> addLast  " + certFromFile.getSubjectDN() + " is issuer of "
								+ lastcert.getSubjectDN());
						inserted = true;
						break;
					}
				} catch (final Exception e) {
				}
			}

			if (!inserted) {
				logger.info(">\n> no Cert Path found : " + certs[i]);
				list = new LinkedList<>();
				logger.info("> add another new list " + certFromFile.getSubjectDN() + "\n>");
				list.add(certFromFile);
				lists.add(list);
				list = null;
			}
			certFromFile = null;
		}

		Iterator<LinkedList<X509Certificate>> listsIterator = lists.listIterator();

		while (listsIterator.hasNext()) {

			X509Certificate firstCert = null;
			X509Certificate lastCert = null;

			try {
				final LinkedList<X509Certificate> cacertsList = listsIterator.next();
				if (cacertsList == null) {
					continue;
				}

				firstCert = cacertsList.getFirst();
				lastCert = cacertsList.getLast();

				final Iterator<LinkedList<X509Certificate>> listsIterator2 = lists.listIterator();

				while (listsIterator2.hasNext()) {
					final LinkedList<X509Certificate> cacertsList2 = listsIterator2.next();

					final X509Certificate firstCert2 = cacertsList2.getFirst();
					final X509Certificate lastCert2 = cacertsList2.getLast();

					logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
					logger.info("IssuerDN  = " + firstCert2.getIssuerDN());
					logger.info("SubjectDN = " + firstCert2.getSubjectDN());
					logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

					if (firstCert.getIssuerDN().toString().compareTo(firstCert2.getIssuerDN().toString()) == 0) {
						continue;
					}

					if (firstCert.getIssuerDN().toString().compareTo(lastCert2.getSubjectDN().toString()) == 0) {
						logger.info("List add first " + lastCert2.getSubjectDN() + " is the issuer of "
								+ firstCert.getSubjectDN());

						cacertsList.addAll(cacertsList2);
						listsIterator2.remove();

						break;
					}
					if (firstCert2.getIssuerDN().toString().compareTo(lastCert.getSubjectDN().toString()) == 0) {
						logger.info("List add last " + lastCert.getSubjectDN() + " is the issuer of "
								+ firstCert2.getSubjectDN());

						cacertsList2.addAll(cacertsList);
						listsIterator.remove();
						break;
					}
				}
			} catch (final Exception e) {
			} finally {
				firstCert = null;
				lastCert = null;
			}
		}

		logger.info("\n\nDumping\n");

		listsIterator = lists.listIterator();
		int numlist = 0;
		while (listsIterator.hasNext()) {
			logger.info(">");
			try {
				final LinkedList<X509Certificate> cacertsList = listsIterator.next();

				if (cacertsList == null) {
					continue;
				}

				logger.info(">\n> CACertPath #" + numlist++);

				X509Certificate firstCert = null;
				CACertPath v = null;
				try {
					v = new CACertPath(cacertsList);
					ret.add(v);
				} finally {
					firstCert = null;
					v = null;
				}
			} catch (final Exception e) {
			}
			logger.info("<");
		}

		logger.info("\nCertPath contains " + ret.size() + " paths\n\n");
		return ret;
	}

	/**
	 * This is the main method for test purposes only
	 */
	public static void main(final String[] args) throws Exception {

		String certToCheck = System.getenv("X509_USER_PROXY");

		if (System.getProperty("X509_USER_PROXY") != null) {
			certToCheck = System.getProperty("X509_USER_PROXY");
		}

		if (certToCheck == null) {
			System.err.println("X509_USER_PROXY must be set");
			System.exit(1);
		}

		final CertificateValidator validator = new CertificateValidator();

		validator.validate(certToCheck);
	}
}
