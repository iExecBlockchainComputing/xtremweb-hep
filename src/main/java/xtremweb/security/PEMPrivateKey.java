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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Hex;

import org.apache.log4j.Logger;

import xtremweb.common.XWReturnCode;

/**
 * This reads X509 private key from PEM files
 *
 * @since 7.4.0
 */
public final class PEMPrivateKey {

	private static final Logger logger = Logger.getLogger(PEMPrivateKey.class);

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private static final String SIGNATUREALGONAME = "SHA256WithRSAEncryption";

	private static class DefaultPasswordFinder implements PasswordFinder {

		private final char[] password;

		private DefaultPasswordFinder(final char[] password) {
			this.password = password.clone();
		}

		@Override
		public char[] getPassword() {
			return Arrays.copyOf(password, password.length);
		}
	}

	/**
	 * This is the read public key, if any This is for testing only
	 */
	private PublicKey publicKey;
	/**
	 * This is the read private key
	 */
	private PrivateKey privateKey;

	/**
	 * Default constructor
	 */
	public PEMPrivateKey() {
		publicKey = null;
	}

	/**
	 * This calls read(new File(keyPath), password)
	 *
	 * @param keyPath
	 *            is the private key file path
	 * @param password
	 *            is the private key password
	 * @see #read(File, String)
	 */
	public void read(final String keyPath, final String password) throws CertificateException, IOException {

		final File f = new File(keyPath);
		read(f, password);
	}

	/**
	 * This calls read(f, password.toCharArray())
	 *
	 * @param f
	 *            is the private key file
	 * @param password
	 *            is the private key password
	 * @see #read(File, char[])
	 */
	public void read(final File f, final String password) throws CertificateException, IOException {

		final char[] p = password == null ? null : password.toCharArray();
		read(f, p);
	}

	/**
	 * This reads both public and private from file. (public key is for testing
	 * only)
	 *
	 * @param keyFile
	 *            is the private key file
	 * @param password
	 *            is the private key password
	 */
	public void read(final File keyFile, final char[] password) throws CertificateException, IOException {

		if (keyFile == null) {
			throw new IOException("key file is null");
		}
		if (password == null) {
			throw new IOException("password is null");
		}

		final DefaultPasswordFinder pfinder = new DefaultPasswordFinder(password);
		try (final FileReader fr = new FileReader(keyFile); final PEMReader r = new PEMReader(fr, pfinder)) {

			final KeyPair kp = (KeyPair) r.readObject();
			try {
				publicKey = kp.getPublic();
			} catch (final Exception ignore) {
			}
			privateKey = kp.getPrivate();
		} catch (final ClassCastException e) {
			throw new CertificateException(e);
		}
	}

	/**
	 * This inserts this private key into the provided KeyStore
	 *
	 * @param store
	 *            is the keystore to add certificate to
	 * @param cert
	 *            is the associated public key
	 * @param password
	 *            is this private key password
	 * @return the keystore filled with some new entries
	 * @since 8.0.2
	 */
	public KeyStore setKeyntries(final KeyStore store, final X509Certificate cert, final String password) {

		try {
			final X509Certificate[] chain = new X509Certificate[1];
			chain[0] = cert;
			final char[] p = password.toCharArray();
			store.setKeyEntry(cert.getSubjectX500Principal().getName(), privateKey, p, chain);
		} catch (final KeyStoreException e) {
			logger.error("Caught exception, Can't insert key into keystore", e);
		}
		return store;
	}

	/**
	 * This is for testing only
	 */
	public void sendAuthentication(final OutputStream outStream)
			throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

		final DataOutputStream out = new DataOutputStream(outStream);
		final long t = System.currentTimeMillis();
		final double q = Math.random();

		final Signature s = Signature.getInstance(SIGNATUREALGONAME);
		s.initSign(privateKey);
		s.update(Protection.makeBytes(t, q));
		final byte[] signature = s.sign();

		out.writeLong(t);
		out.writeDouble(q);
		out.writeInt(signature.length);
		out.write(signature);
		out.flush();
	}

	/**
	 * This is for testing only
	 */
	public static void main(final String[] args) throws Exception {

		if (args.length < 2) {
			logger.info("Usage : PrivateKey file password [--connect]");
			logger.info("Where : file is the private key file");
			logger.info("        password is the private key password");
			logger.info("        --connect to connect to localhost:79999 for testing");
			logger.info("        (of course you have started PublicKey as server - see PublicKeyReader)");
			System.exit(1);
		}
		final String message = "hello world";
		final PEMPrivateKey reader = new PEMPrivateKey();
		reader.read(args[0], args[1]);

		logger.info("privateKey = " + reader.privateKey.toString());

		final Signature signature = Signature.getInstance(SIGNATUREALGONAME);
		signature.initSign(reader.privateKey);
		signature.update(message.getBytes());
		final byte[] signatureBytes = signature.sign();
		logger.info(new String(Hex.encode(signatureBytes)));

		final Signature verifier = Signature.getInstance(SIGNATUREALGONAME);
		verifier.initVerify(reader.publicKey);
		verifier.update(message.getBytes());
		if (verifier.verify(signatureBytes)) {
			logger.info("Signature is valid");
		} else {
			logger.error("Signature is invalid");
			System.exit(XWReturnCode.FATAL.ordinal());
		}

		if (args.length > 2) {
			final int port = 7999;
			try (final Socket s = new Socket("localhost", port)) {
				reader.sendAuthentication(s.getOutputStream());
			}
		}
	}
}
