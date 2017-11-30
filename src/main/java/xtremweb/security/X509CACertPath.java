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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.apache.log4j.Logger;


/**
 * This class contains CA cert path and its most trusted certificate. Both are
 * used to validate a certificate through this path
 *
 * @since 7.0.0
 */
public final class X509CACertPath {

	private static final Logger logger = Logger.getLogger(X509CACertPath.class);

	/**
	 * This is the certificate factory to retrieve certificate from file,
	 * certificate paths etc.
	 */
	private CertificateFactory certFactory = null;
	/**
	 * This is the certificate path validator
	 */
	private CertPathValidator pathValidator = null;
	/**
	 * This is the most trusted certificate
	 */
	private X509Certificate trustAnchor;
	/**
	 * This is the most trusted certificate parameters
	 */
	private PKIXParameters trustAnchorParams;
	/**
	 * This is the cert path belonging to the most trusted certificate
	 */
	private LinkedList<X509Certificate> certPathList = null;

	public LinkedList<X509Certificate> getCertPathList() {
		return certPathList;
	}

	/**
	 * This instanciates the cert factory and the cert path validator
	 */
	public X509CACertPath() throws CertificateException, NoSuchAlgorithmException {

		certFactory = CertificateFactory.getInstance("X.509", new BouncyCastleProvider());
		pathValidator = CertPathValidator.getInstance("PKIX", new BouncyCastleProvider());
	}

	/**
	 * This contructs a cert path
	 *
	 * @param c
	 *            is the most trusted certificate
	 * @param f
	 *            is a file containing on eor more certificates. It must not
	 *            contain the most trusted CA. Certificates must be stored in
	 *            file ordered by trust level (from least to most trusted : the
	 *            last one must be issued by c)
	 */
	public X509CACertPath(final X509Certificate c, final File f)
			throws IOException, CertificateException, NoSuchAlgorithmException {

		this();

		FileInputStream certis = null;
		CertPath certPath = null;
		try {
			certis = new FileInputStream(f);
			trustAnchor = c;
			trustAnchorParams = createParams(trustAnchor);
			certPath = certFactory.generateCertPath(certis, "PKCS7");
			certPathList = (LinkedList<X509Certificate>) certPath.getCertificates();
		} finally {
			try {
				certis.close();
			} catch (final Exception e) {
			}
			certis = null;
			certPath = null;
		}
	}

	/**
	 * This constructs a cert path
	 *
	 * @param c
	 *            is the most trusted cert CA
	 * @param cp
	 *            contains the certificate path. It must not contain the most
	 *            trusted cert CA. Certificates must have been ordered by trust
	 *            level (from least to most trusted : the last one must be
	 *            issued by c)
	 */
	public X509CACertPath(final X509Certificate c, final CertPath cp)
			throws IOException, CertificateException, NoSuchAlgorithmException {

		this();

		trustAnchor = c;
		trustAnchorParams = createParams(trustAnchor);
		certPathList = (LinkedList<X509Certificate>) cp.getCertificates();
	}

	/**
	 * This constructs a cert path
	 *
	 * @param c
	 *            is the most trusted cert CA
	 * @param list
	 *            contains the certificates list of the certificate path. It
	 *            must not contain the most trusted cert CA. Certificates must
	 *            be ordered by trust level (from least to most trusted : the
	 *            last one must be issued by c)
	 */
	public X509CACertPath(final X509Certificate c, final LinkedList<X509Certificate> list)
			throws CertificateException, IOException, NoSuchAlgorithmException {

		this();

		trustAnchor = c;
		trustAnchorParams = createParams(trustAnchor);
		certPathList = list;
	}

	/**
	 * This constructor extract the most trusted CA cert from the list. The most
	 * trusted CA cert must be the last cert from the list.
	 *
	 * @param list
	 *            contains the certificates list of the certificate path.
	 *            Certificates must be ordered by trust level (from least to
	 *            most trusted). It must contain the most trusted cert CA at
	 *            last position.
	 */
	public X509CACertPath(final LinkedList<X509Certificate> list)
			throws CertificateException, NoSuchAlgorithmException {

		this();

		try {
			certPathList = list;
			trustAnchor = certPathList.removeLast();
			trustAnchorParams = createParams(trustAnchor);
		} catch (final Exception e) {
			throw new CertificateException(e.getMessage());
		}
	}

	/**
	 * This retrieves the anchor distinguished name
	 */
	public String getAnchorSubjectName() {
		X500Principal p = null;
		try {
			p = trustAnchor.getSubjectX500Principal();
			return p.getName();
		} finally {
			p = null;
		}
	}

	/**
	 * This retrieves the anchor issuer DN
	 *
	 * @since 8.2.0
	 */
	public String getAnchorIssuerName() {
		X500Principal p = null;
		try {
			p = trustAnchor.getIssuerX500Principal();
			return p.getName();
		} finally {
			p = null;
		}
	}

	/**
	 * This retrieves the anchor
	 *
	 * @since 8.2.0
	 */
	public X509Certificate getAnchor() {
		return trustAnchor;
	}

	/**
	 * This validates th emost trusted certificate from the X509 proxy through
	 * the cert path
	 *
	 * @param proxy
	 *            contains public certificate from X509 proxy
	 */
	public PKIXCertPathValidatorResult validate(final X509Proxy proxy) throws CertPathValidatorException {

		X509Certificate proxyLast = null;

		try {
			proxyLast = proxy.getLast();
			return validate(proxyLast);
		} catch (final CertPathValidatorException e) {
			throw e;
		} catch (final Exception e) {
			logger.trace(e.toString());
			throw new CertPathValidatorException(e);
		} finally {
			proxyLast = null;
		}
	}

	/**
	 * This validates an X509 public certificate through the cert path
	 *
	 * @param proxy
	 *            contains a public certificate
	 */
	public PKIXCertPathValidatorResult validate(final X509Certificate proxy) throws CertPathValidatorException {

		PKIXCertPathValidatorResult ret = null;
		CertPath certPath = null;
		X509Certificate pathFirst = null;
		X500Principal principal = null;
		String proxyIssuerName = null;
		String pathfirstSubjectName = null;
		boolean removefirst = false;

		logger.trace("X509CertPath#validate size= " + certPathList.size());

		try {
			if (certPathList.size() > 0) {
				pathFirst = certPathList.getFirst();

				principal = proxy.getIssuerX500Principal();
				proxyIssuerName = principal.getName();
				principal = pathFirst.getSubjectX500Principal();
				pathfirstSubjectName = principal.getName();
				logger.trace(">> Validating certificate (" + proxyIssuerName + ") ; CA cert path least trusted "
						+ pathfirstSubjectName);
				logger.trace(">> Validating certificate (" + proxyIssuerName + ") ; CA cert path trustAnchor   "
						+ trustAnchor.getSubjectX500Principal().getName());
				logger.trace(">> Validating certificate (" + proxyIssuerName + ") ; CA cert path size          "
						+ certPathList.size());
			}
			certPathList.add(0, proxy);
			removefirst = true;

			certPath = certFactory.generateCertPath(certPathList);
			ret = (PKIXCertPathValidatorResult) pathValidator.validate(certPath, trustAnchorParams);
			return ret;
		} catch (final CertPathValidatorException e) {
			ret = null;
			throw e;
		} catch (final Exception e) {
			ret = null;
			throw new CertPathValidatorException(e);
		} finally {
			if (removefirst == true) {
				certPathList.remove(0);
			}
			certPath = null;
			principal = null;
			proxyIssuerName = null;
			pathfirstSubjectName = null;
			pathFirst = null;
		}
	}

	/**
	 * This creates a PKIXParameters object that is needed to validate a cert
	 * path. A PKIXParameters object is created from one or more most-trusted
	 * CAs (aka root CA that must be trusted).
	 *
	 * @param anchorPathName
	 *            containsthe name of a certificate file or directory path of
	 *            the most trusted CA
	 * @return a PKIXParameters for the most trusted CA
	 */
	public PKIXParameters createParams(final String anchorPathName) throws IOException, CertificateException {
		File anchorFile = null;
		PKIXParameters ret = null;
		try {
			anchorFile = new File(anchorPathName);
			ret = createParams(anchorFile);
		} finally {
			anchorFile = null;
		}
		return ret;
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
		X509Certificate anchorCert = null;
		PKIXParameters ret = null;
		FileInputStream certis = null;
		File[] anchorFiles = null;

		try {
			certis = new FileInputStream(anchorFile);
			anchorCert = (X509Certificate) certFactory.generateCertificate(certis);
			ret = createParams(anchorCert);
		} finally {
			certis = null;
			anchorCert = null;
			anchorFiles = null;
		}
		ret.setRevocationEnabled(false);
		return ret;
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

		TrustAnchor anchorTrust = null;
		Set<TrustAnchor> anchorTrusts = null;
		PKIXParameters ret = null;

		try {
			anchorTrust = new TrustAnchor(anchorCert, null);
			anchorTrusts = Collections.singleton(anchorTrust);
			ret = new PKIXParameters(anchorTrusts);
		} catch (final InvalidAlgorithmParameterException e) {
		} finally {
			anchorTrust = null;
			anchorTrusts = null;
		}
		ret.setRevocationEnabled(false);
		return ret;
	}
}
