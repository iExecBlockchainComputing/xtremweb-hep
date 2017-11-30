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
 *
 * X509ProxyValidator : validates a X509 proxy against an X509 certification path
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
 *    - X509_CERT_DIR must then contains public keys to create the following path
 *        [0] IssuerDN  = CN=CNRS2-Projets, O=CNRS, C=FR
 *        [0] SubjectDN = CN=GRID2-FR, O=CNRS, C=FR
 *        [1] IssuerDN  = CN=CNRS2, O=CNRS, C=FR
 *        [1] SubjectDN = CN=CNRS2-Projets, O=CNRS, C=FR
 *        [2] IssuerDN  = CN=CNRS2, O=CNRS, C=FR
 *        [2] SubjectDN = CN=CNRS2, O=CNRS, C=FR
 *
 * If X509_CERT_DIR does not contains this path, the X509_USER_PROXY can not be validated
 *
 * @since 7.0.0
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;


import xtremweb.common.XWPropertyDefs;

/**
 */
public final class X509ProxyValidator {

	private static final Logger logger = Logger.getLogger(X509ProxyValidator.class);

	/**
	 * This is the certificate factory to retreive certificate from file,
	 * certificate paths etc.
	 */
	private CertificateFactory certFactory = null;
	/**
	 * This contains the CA certificates paths
	 */
	private final Vector<X509CACertPath> caCertPaths;

	/**
	 */
	public X509ProxyValidator() throws CertificateException, IOException, NoSuchAlgorithmException {

		certFactory = CertificateFactory.getInstance("X.509");

		String cadir = null;

		cadir = System.getProperty(XWPropertyDefs.CACERTDIR.toString());

		if (cadir == null) {
			throw new IOException(XWPropertyDefs.CACERTDIR + " is not set");
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
	public PKIXCertPathValidatorResult validate(final String userCertFileName)
			throws IOException, CertificateException, CertPathValidatorException {

		File userCertFile = null;

		try {
			userCertFile = new File(userCertFileName);
			return validate(userCertFile);
		} finally {
			userCertFile = null;
		}
	}

	/**
	 * This validates an user X509 proxy on the certificate path
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
	public PKIXCertPathValidatorResult validate(final File userCertFile)
			throws IOException, CertificateException, CertPathValidatorException {

		X509Proxy proxy = null;
		try {
			proxy = new X509Proxy(userCertFile);
			return validate(proxy);
		} finally {
			proxy = null;
		}
	}

	/**
	 * This validates an user X509 proxy on the certificate paths
	 *
	 * @param proxy
	 *            contains the user X509 proxy to validate among the cert path
	 * @exception IOException
	 *                is thrown on file access error
	 * @exception CertificateException
	 *                is thrown on certificate error
	 * @exception CertPathValidatorException
	 *                is thrown on validation error
	 */
	public PKIXCertPathValidatorResult validate(final X509Proxy proxy)
			throws IOException, CertificateException, CertPathValidatorException {

		logger.trace("<< ******************************************************************************** <<");
		logger.trace("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.trace("proxy.getLast  =" + proxy.getLast().getSubjectX500Principal().getName());
		logger.trace("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		X509Certificate last = null;
		try {
			last = proxy.getLast();
			return validate(last);
		} finally {
			last = null;
		}
	}

	/**
	 * This validates an user X509 proxy on the certificate paths
	 *
	 * @param cert
	 *            contains the user X509 proxy to validate among the cert path
	 * @exception IOException
	 *                is thrown on file access error
	 * @exception CertificateException
	 *                is thrown on certificate error
	 * @exception CertPathValidatorException
	 *                is thrown on validation error
	 */
	public PKIXCertPathValidatorResult validate(final X509Certificate cert)
			throws IOException, CertificateException, CertPathValidatorException {

		logger.trace("<< ******************************************************************************** <<");
		logger.trace("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.trace("cert.subject =" + cert.getSubjectX500Principal().getName());
		logger.trace("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		final Enumeration<X509CACertPath> listsEnum = caCertPaths.elements();
		while (listsEnum.hasMoreElements()) {
			X509CACertPath cacertpath = null;
			try {
				cacertpath = listsEnum.nextElement();
				final PKIXCertPathValidatorResult ret = cacertpath.validate(cert);
				return ret;
			} catch (final CertPathValidatorException e) {
				logger.trace(e.toString());
			} finally {
				cacertpath = null;
			}
		}

		throw new CertPathValidatorException("not validated by any known certificate path");
	}

	/**
	 * This creates a Vector of certificate paths from certificate file name. If
	 * file name denotes a file, the vector will contain a single cert path, if
	 * any. If file name denotes a directory, the vector will contain a certain
	 * amount of cert paths, if any.
	 *
	 * @param certFilePathName
	 *            is a certificate file or directory path name
	 * @return a ProxyValidator containing all found cert paths
	 */
	public Vector<X509CACertPath> createPaths(final String certFilePathName)
			throws NoSuchAlgorithmException, IOException, CertificateException {

		File certFile = null;
		Vector<X509CACertPath> ret = null;

		try {
			certFile = new File(certFilePathName);
			ret = createPaths(certFile);
		} finally {
			certFile = null;
		}
		return ret;
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
	 * @return a Vector of ProxyValidator
	 */
	public Vector<X509CACertPath> createPaths(final File certFile)
			throws NoSuchAlgorithmException, IOException, CertificateException {

		FileInputStream certis = null;
		Vector<X509CACertPath> ret = new Vector<>();
		Collection<X509Certificate> certs = null;
		LinkedList<X509Certificate> certslist = null;
		File[] certFiles = null;
		X509CACertPath validator = null;

		try {
			if (certFile.isDirectory()) {
				certFiles = certFile.listFiles();
				ret = createPaths(certFiles);
			} else {
				validator = new X509CACertPath((X509Certificate) null, certFile);
				ret.add(validator);
			}
		} catch (final CertificateException ce) {
			certs = (Collection<X509Certificate>) (certFactory.generateCertificates(certis));
			certslist = new LinkedList<>(certs);
			validator = new X509CACertPath((X509Certificate) null, certslist);
			ret.add(validator);
		} finally {
			certFiles = null;
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
	public Vector<X509CACertPath> createPaths(final File[] certs) throws IOException, CertificateException {
		if (certs == null) {
			throw new IOException("certs is null");
		}

		final CertificateException ce = null;
		LinkedList<X509Certificate> list = null;
		FileInputStream certis = null;
		final Vector<LinkedList<X509Certificate>> lists = new Vector<>();
		final Vector<X509CACertPath> ret = new Vector<>();
		X500Principal principal = null;

		//
		// create linked lists of certificates
		//
		logger.trace("Certificate files in directory = " + certs.length);

		for (int i = 0; i < certs.length; i++) {

			X509Certificate certFromFile = null;
			try {
				certis = new FileInputStream(certs[i]);
				certFromFile = (X509Certificate) certFactory.generateCertificate(certis);
			} catch (final Exception e) {
			} finally {
				if (certis != null) {
					certis.close();
				}
				certis = null;
			}

			if (certFromFile == null) {
				// no cert found in file
				continue;
			}

			principal = certFromFile.getSubjectX500Principal();
			String certSubjectName = principal.getName();
			principal = certFromFile.getIssuerX500Principal();
			String certIssuerName = principal.getName();

			if (lists.isEmpty()) {
				list = new LinkedList<>();
				logger.trace("> add first new list (" + certs[i] + ") " + certSubjectName);
				list.add(certFromFile);
				lists.add(list);
				list = null;
				continue;
			}

			//
			// try to find a list of certs that current certFromFile belongs to
			//
			boolean inserted = false;

			final Enumeration<LinkedList<X509Certificate>> listsEnum = lists.elements();
			final int numlist = 0;
			while (listsEnum.hasMoreElements() && (inserted == false)) {
				try {
					final LinkedList<X509Certificate> cacertsList = listsEnum.nextElement();

					X509Certificate firstcert = null;
					X509Certificate lastcert = null;
					String firstcertIssuerName = null;
					String firstcertSubjectName = null;
					String lastcertIssuerName = null;
					String lastcertSubjectName = null;
					try {
						firstcert = cacertsList.getFirst();
						lastcert = cacertsList.getLast();
						principal = firstcert.getIssuerX500Principal();
						firstcertIssuerName = principal.getName();
						principal = firstcert.getSubjectX500Principal();
						firstcertSubjectName = principal.getName();
						principal = lastcert.getIssuerX500Principal();
						lastcertIssuerName = principal.getName();
						principal = lastcert.getSubjectX500Principal();
						lastcertSubjectName = principal.getName();
						principal = null;

						if (certSubjectName.compareTo(firstcertSubjectName) == 0) {
							logger.trace(">>> 01 certSubjectName = " + certSubjectName + " firstcertSubjectName = "
									+ firstcertSubjectName);
							continue;
						}

						if (certSubjectName.compareTo(lastcertSubjectName) == 0) {
							logger.trace(">>> 02 certSubjectName = " + certSubjectName + " lastcertSubjectName = "
									+ lastcertSubjectName);
							continue;
						}

						if (certIssuerName.compareTo(firstcertSubjectName) == 0) {
							cacertsList.addFirst(certFromFile);
							logger.trace(">>> addFirst (" + cacertsList.size() + ") " + certSubjectName
									+ " is issued by " + firstcertSubjectName);
							inserted = true;
							break;
						}

						if (certSubjectName.compareTo(lastcertIssuerName) == 0) {
							cacertsList.addLast(certFromFile);
							logger.trace(">>> addLast  (" + cacertsList.size() + ") " + certSubjectName
									+ " is issuer of " + lastcertSubjectName);
							inserted = true;
							break;
						}
					} finally {
						firstcert = null;
						lastcert = null;
						principal = null;
						firstcertIssuerName = null;
						firstcertSubjectName = null;
						lastcertIssuerName = null;
						lastcertSubjectName = null;
					}
				} catch (final Exception e) {
				}
			}

			if (inserted == false) {
				list = new LinkedList<>();
				logger.trace("> add another new list (" + certs[i] + ") " + certSubjectName);
				list.add(certFromFile);
				lists.add(list);
				list = null;
			}
			certFromFile = null;
			certIssuerName = null;
			certSubjectName = null;
		}

		logger.trace("> Paths found   " + lists.size());

		//
		// second, check if some lists should also be linked to each other
		//
		Iterator<LinkedList<X509Certificate>> listsIterator = lists.listIterator();

		while (listsIterator.hasNext()) {

			X509Certificate firstCert = null;
			X509Certificate lastCert = null;
			String firstcertIssuerName = null;
			String firstcertSubjectName = null;
			String lastcertIssuerName = null;
			String lastcertSubjectName = null;

			try {
				final LinkedList<X509Certificate> cacertsList = listsIterator.next();
				if (cacertsList == null) {
					continue;
				}

				firstCert = cacertsList.getFirst();
				lastCert = cacertsList.getLast();
				principal = firstCert.getIssuerX500Principal();
				firstcertIssuerName = principal.getName();
				principal = firstCert.getSubjectX500Principal();
				firstcertSubjectName = principal.getName();
				principal = lastCert.getIssuerX500Principal();
				lastcertIssuerName = principal.getName();
				principal = lastCert.getSubjectX500Principal();
				lastcertSubjectName = principal.getName();

				final Iterator<LinkedList<X509Certificate>> listsIterator2 = lists.listIterator();

				while (listsIterator2.hasNext()) {
					final LinkedList<X509Certificate> cacertsList2 = listsIterator2.next();

					X509Certificate firstCert2 = null;
					X509Certificate lastCert2 = null;
					String firstcert2IssuerName = null;
					String firstcert2SubjectName = null;
					String lastcert2IssuerName = null;
					String lastcert2SubjectName = null;
					try {
						firstCert2 = cacertsList2.getFirst();
						lastCert2 = cacertsList2.getLast();

						principal = firstCert2.getIssuerX500Principal();
						firstcert2IssuerName = principal.getName();
						principal = firstCert2.getSubjectX500Principal();
						firstcert2SubjectName = principal.getName();
						principal = lastCert2.getIssuerX500Principal();
						lastcert2IssuerName = principal.getName();
						principal = lastCert2.getSubjectX500Principal();
						lastcert2SubjectName = principal.getName();

						if (firstcertIssuerName.compareTo(firstcert2IssuerName) == 0) {
							continue;
						}

						if (firstcertIssuerName.compareTo(lastcert2SubjectName) == 0) {
							cacertsList.addAll(cacertsList2);
							listsIterator2.remove();
							logger.trace(">>> List addFirst (" + cacertsList.size() + ") " + lastcert2SubjectName
									+ " is issuer of " + firstcertSubjectName);
							break;
						}
						if (firstcert2IssuerName.compareTo(lastcertSubjectName) == 0) {
							cacertsList2.addAll(cacertsList);
							listsIterator.remove();
							logger.trace(">>> List addLast (" + cacertsList.size() + ") " + lastcertSubjectName
									+ " is issuer of " + firstcert2SubjectName);
							break;
						}
					} finally {
						firstCert2 = null;
						lastCert2 = null;
						firstcert2IssuerName = null;
						firstcert2SubjectName = null;
						lastcert2IssuerName = null;
						lastcert2SubjectName = null;
					}
				}
			} catch (final Exception e) {
			} finally {
				firstCert = null;
				lastCert = null;
				firstcertIssuerName = null;
				firstcertSubjectName = null;
				lastcertIssuerName = null;
				lastcertSubjectName = null;
			}
		}

		//
		// finally, dump and create the ProxyValidator from lists
		//
		logger.info("Found certificate paths");

		listsIterator = lists.listIterator();
		int numlist = 0;
		while (listsIterator.hasNext()) {
			try {
				final LinkedList<X509Certificate> cacertsList = listsIterator.next();

				if (cacertsList == null) {
					continue;
				}

				X509CACertPath v = null;
				try {
					v = new X509CACertPath(cacertsList);

					String dn = v.getAnchorSubjectName();
					String idn = v.getAnchorIssuerName();
					final boolean trusted = (idn.compareTo(dn) == 0);
					if (trusted) {
						logger.info("> #" + numlist++ + " " + dn);
						ret.add(v);
					} else {
						logger.error(">  !! Trust Anchor Error !! " + dn + " is not a trusted anchor: it is issued by "
								+ idn + ". We miss the issuer certificate (the CA cert path is incomplete)");
					}
					dn = null;
					idn = null;

					final Iterator<X509Certificate> certit = cacertsList.listIterator();
					while (certit.hasNext()) {
						try {
							final X509Certificate cert = certit.next();
							if (trusted) {
								logger.info(">> Subject=\"" + cert.getSubjectDN() + "\" Issuer=\""
										+ cert.getIssuerDN() + "\"");
							} else {
								logger.error(">> !! Trust Anchor Error !!  Subject=\"" + cert.getSubjectDN()
										+ "\" Issuer=\"" + cert.getIssuerDN() + "\"");
							}
						} catch (final Exception e) {
						}
					}
				} finally {
					v = null;
				}
			} catch (final Exception e) {
			}
		}

		listsIterator = null;
		logger.info(" Total cert paths = " + ret.size());
		return ret;
	}

	/**
	 * This inserts all known CA certificates to the provided keystore
	 *
	 * @param store
	 *            is the keystore to add certificate to
	 * @return this returns null, if parameter is null; else this returns the
	 *         keystore filled with some new entries
	 * @since 8.0.2
	 */
	public KeyStore setCACertificateEntries(final KeyStore store) {
		if (store == null) {
			return null;
		}
		final Iterator<X509CACertPath> pathsit = caCertPaths.iterator();
		while (pathsit.hasNext()) {
			try {
				final X509CACertPath certpath = pathsit.next();
				final LinkedList<X509Certificate> certlist = certpath.getCertPathList();
				store.setCertificateEntry(certpath.getAnchorSubjectName(), certpath.getAnchor());
				final Iterator<X509Certificate> certit = certlist.iterator();
				while (certit.hasNext()) {
					try {
						final X509Certificate cert = certit.next();
						final String alias = cert.getSubjectDN().toString();
						logger.trace("KeyStore set entry= " + alias + "; KeyStore.size = " + store.size());
						store.setCertificateEntry(alias, cert);
					} catch (final Exception e) {
						logger.error("Caught exception, Can't add new entry to keystore", e);
					}
				}
			} catch (final Exception e) {
				logger.error("Caught exception, Can't retrieve ca cert path", e);
			}
		}
		return store;
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

		final X509ProxyValidator validator = new X509ProxyValidator();
		validator.validate(certToCheck);
		System.out.println("X509 proxy  validated against CA cert path :)");
	}
}
