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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.rmi.RemoteException;
import java.util.Vector;
import org.apache.log4j.Logger;

import xtremweb.communications.XMLRPCCommand;

/**
 * Created: Oct, 2005<br />
 *
 * This class implements a FIFO buffer<br />
 * This is used by NIOClient/NIOHanlder to create/read non blocking TCP packets
 *
 * @author Oleg Lodygensky
 * @version RPCXW-v7
 */

public final class BytePacket {

	private static final Logger logger = Logger.getLogger(BytePacket.class);

	/**
	 * This is the buffer length
	 *
	 * @see xtremweb.common.XWTools#PACKETSIZE
	 */
	public static final int BUFFERLENGTH = XWTools.PACKETSIZE;
	/**
	 * This is the buffer to ue for I/O
	 */
	protected ByteBuffer buffer;

	/**
	 * This constructs the buffer
	 *
	 * @see #setData(byte [])
	 */
	public BytePacket() {
		final byte[] b = new byte[BUFFERLENGTH];
		setData(b);
	}

	/**
	 * This must be called when receiving packet
	 *
	 * @see #pack()
	 */
	public void reset() {
		buffer.clear();
	}

	/**
	 * This must be called before sending packet
	 *
	 * @see #reset()
	 */
	public void pack() {
		buffer.flip();
	}

	/**
	 * This retreives this stack buffer
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}

	/**
	 * This retreives this stack array
	 */
	public byte[] getData() {
		return buffer.array();
	}

	/**
	 * This sets this stack buffer
	 */
	public void setData(final byte[] v) {
		buffer = ByteBuffer.wrap(v);
		buffer.order(ByteOrder.BIG_ENDIAN);
	}

	/**
	 * This pushes an UserInterface and an integer to packet
	 *
	 * @param user
	 *            is the UserInterface to push
	 * @param code
	 *            is the IdRpc code to push
	 */
	public void putUserInterface(final UserInterface user, final int code) throws RemoteException {
		putInt(code);
		putObject(user);
	}

	/**
	 * This pushes an XMLRPCCommand to packet
	 *
	 * @param cmd
	 *            is the XMLRPCCcommand to put
	 */
	public void putXMLRPCCommand(final XMLRPCCommand cmd) throws RemoteException {
		putObject(cmd);
	}

	/**
	 * This pushes an UserInterface and an integer to packet and an optionnal
	 * parameter
	 *
	 * @param user
	 *            is the UserInterface to push
	 * @param code
	 *            is the IdRpc code to push
	 * @param obj
	 *            is an optionnal object to push
	 */
	public void putUserInterface(final UserInterface user, final int code, final XMLable obj) throws RemoteException {
		putUserInterface(user, code);
		if (obj != null) {
			putObject(obj);
		}
	}

	/**
	 * This pushes an UserInterface and an integer to packet and an optionnal
	 * parameter
	 *
	 * @param user
	 *            is the UserInterface to push
	 * @param code
	 *            is the IdRpc code to push
	 * @param uid
	 *            is an optionnal object to push
	 */
	public void putUserInterface(final UserInterface user, final int code, final UID uid) throws RemoteException {
		putUserInterface(user, code);
		if (uid != null) {
			putUID(uid);
		}
	}

	/**
	 * This sends the IrRpc code, the UserInterface that identifies the client
	 * and an optionnal UID
	 *
	 * @param code
	 *            is the IdRpc code to send
	 * @param str
	 *            is the String to send
	 * @see xtremweb.common.ByteStack#putUID(UID)
	 * @see #buffer
	 */
	public void putUserInterface(final UserInterface user, final int code, final String str) throws RemoteException {
		putUserInterface(user, code);
		if (str != null) {
			putString(str);
		}
	}

	/**
	 * This puts a byte
	 *
	 * @param v
	 *            is the byte to insert
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public void putByte(final byte v) throws RemoteException {
		try {
			buffer.put(v);
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This gets a byte
	 *
	 * @return the extracted byte
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public byte getByte() throws RemoteException {
		try {
			return buffer.get();
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This puts an integer
	 *
	 * @param v
	 *            is the integer to insert
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public void putInt(final int v) throws RemoteException {
		try {
			buffer.putInt(v);
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This posp an integer
	 *
	 * @return the extracted integer
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public int getInt() throws RemoteException {
		try {
			return buffer.getInt();
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This puts the UTF-8 representation of the given String
	 *
	 * @param v
	 *            is the String to insert
	 * @exception RemoteException
	 *                is thrown error (buffer overflow...)
	 */
	public void putString(final String v) throws RemoteException {
		try {
			putArray(v.getBytes(XWTools.UTF8));
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This gets a String
	 *
	 * @return the extracted String
	 * @exception Exception
	 *                is thrown on on I/O error
	 */
	public String getString() throws RemoteException {
		final byte[] b = getArray();
		String str = "";
		if (b != null) {
			str = new String(b);
		}
		return str;
	}

	/**
	 * This tries to put a byte array accordingly to BUFFERLENGTH. If there is
	 * not enough spaces on the buffer, this throws an exception.<br />
	 * This first puts the array content itself if any, then the array size as
	 * an integer<br />
	 * If v is null a single 0 is only inserted
	 *
	 * @param barray
	 *            is the array to insert
	 * @exception Exception
	 *                is thrown on error or if the array is too large
	 */
	public void putArray(final byte[] barray) throws RemoteException {
		try {
			if (barray == null) {
				putInt(0);
				return;
			}

			final int length = barray.length;

			if (length >= (buffer.remaining() - XWTools.SIZEOFLONG)) {
				logger.error("BytePacket#putArray length = " + length + "(" + buffer.remaining() + ")");
				throw new RemoteException("array too large!!!");
			}

			putInt(length);
			buffer.put(barray);
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This gets a byte array <br />
	 * This first gets the array size, then the array content itself, if any
	 *
	 * @return a byte array, of null if the read array size is 0
	 * @exception Exception
	 *                is thrown on I/O error or if the provided file does not
	 *                exist
	 */
	public byte[] getArray() throws RemoteException {
		try {
			final int length = getInt();

			if (length == 0) {
				return null;
			}

			final byte[] ret = new byte[length];
			buffer.get(ret);

			return ret;
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This tries to put a Vector accordingly to BUFFERLENGTH. Hence this may
	 * send a subset of the vector elements only.<br />
	 * This first writes vector size following by vector datas, if any<br />
	 * If v is null, this writes a single 0
	 *
	 * @see #BUFFERLENGTH
	 * @param v
	 *            is the Vector to send
	 * @exception RemoteException
	 *                is thrown on I/O error
	 */
	public void putVector(final Vector v) throws RemoteException {

		if (v == null) {
			putInt(0);
			return;
		}

		String str = "";
		int nbElem;
		for (nbElem = 0; nbElem < v.size(); nbElem++) {
			str = str.concat(v.elementAt(nbElem).toString());
			if (str.length() >= (buffer.remaining() - XWTools.SIZEOFLONG)) {
				nbElem--;
				if (nbElem < 0) {
					nbElem = 0;
				}
				logger.warn("BytePacket#putVector puts only " + nbElem + " (" + str.length() + ", " + buffer.remaining()
						+ ")");
				break;
			}
		}

		putInt(nbElem);
		for (int i = 0; i < nbElem; i++) {
			putString(v.elementAt(i).toString());
		}
	}

	/**
	 * This gets a Vector; this first reads vector size then vector datas, if
	 * any<br />
	 * This tries to store received objects as UID into Vector; if received
	 * objects are not UID, they are stored as String into returned Vector
	 *
	 * @return a Vector of UID, or a Vector of String, or an empty Vector
	 * @exception RemoteException
	 *                is thrown on I/O error
	 */
	public Vector getVector() throws RemoteException {

		final Vector ret = new Vector();
		final int size = getInt();

		if (size == 0) {
			return ret;
		}

		for (int i = 0; i < size; i++) {
			final String str = getString();
			try {
				ret.add(new UID(str));
			} catch (final Exception e) {
				ret.add(str);
			}
		}
		return ret;
	}

	/**
	 * This puts an XML object representation as String
	 *
	 * @param o
	 *            is the object to insert
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public void putObject(final XMLable o) throws RemoteException {
		if (o == null) {
			putString("");
		} else {
			putString(o.toXml());
		}
	}

	/**
	 * This puts an UID String representation
	 *
	 * @param uid
	 *            is the UID to write
	 * @exception RemoteException
	 *                is thrown on I/O error
	 * @see #getUID()
	 */
	public void putUID(final UID uid) throws RemoteException {
		putString(uid.toString());
	}

	/**
	 * This gets an UID String representation
	 *
	 * @exception RemoteException
	 *                is thrown on I/O error
	 * @exception RemoteException
	 *                is thrown is read String does not represent a valid UID
	 * @see #putUID(UID)
	 * @return UID as read from input stream
	 */
	public UID getUID() throws RemoteException, IOException {
		return new UID(getString());
	}

	/**
	 * This is the standard main method<br />
	 * This is for debug purposes only
	 */
	public static void main(final String[] args) {
		try {
			final BytePacket b0 = new BytePacket();
			final BytePacket b1 = new BytePacket();

			b0.putInt(12345);
			b0.putInt(54321);
			if (args.length > 0) {
				b0.putString(args[0]);
			} else {
				b0.putString("this is a test only");
			}

			b1.setData(b0.getData());
			logger.info("b1.getInt    = " + b1.getInt());
			logger.info("b1.getInt    = " + b1.getInt());
			logger.info("b1.getString = " + b1.getString());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
