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
import java.rmi.RemoteException;
import java.util.Vector;
import org.apache.log4j.Logger;


/**
 * Created: Sept 20th, 2005<br />
 *
 * This class implements a stack (FILO buffer)<br />
 * Objects that can be converted as bytes array can also be managed<br />
 * This is especially used by UDPClient/UDPHanlder to create/read UDP packets
 * <p>
 * Method names use "put" and "get" instead of (respectively) "push" and "pop"
 * so that we can easily switch between ByteStack and BytePacket
 * </p>
 *
 * @see xtremweb.communications.UDPClient
 * @see xtremweb.dispatcher.UDPHandler
 * @author Oleg Lodygensky
 * @version RPCXW-v6
 */

public final class ByteStack {

	private static final Logger logger = Logger.getLogger(ByteStack.class);

	/**
	 * This is the buffer length
	 *
	 * @see xtremweb.common.XWTools#PACKETSIZE
	 */
	private static final int DEFLENGTH = XWTools.PACKETSIZE;
	/**
	 * This is the buffer
	 */
	private byte[] buffer = null;
	/**
	 * This is the index in buffer
	 */
	private int cursor;

	/**
	 * This constructs the buffer
	 *
	 * @see #reset()
	 */
	public ByteStack() {
		buffer = new byte[DEFLENGTH];
		reset();
	}

	/**
	 * <h3>Note</h3> the top of stack is kept for an integer to store the cursor
	 * value so that we can restore the cursor when receiving packet
	 *
	 * @see #pack()
	 */
	public void reset() {
		cursor = XWTools.SIZEOFINTEGER;
	}

	/**
	 * This stores the cursor to top of stack
	 * <h3>This must be called before sending packet so that the receiver can
	 * first extract the cursor</h3>
	 *
	 * @see #reset()
	 */
	public void pack() {
		final byte[] bcursor = XWTools.integer2bytes(cursor);
		System.arraycopy(bcursor, 0, buffer, 0, XWTools.SIZEOFINTEGER);
	}

	/**
	 * This retreives this stack buffer
	 */
	public byte[] getData() {
		return buffer;
	}

	/**
	 * This retreives this stack size
	 */
	public int getSize() {
		return cursor;
	}

	/**
	 * This sets this stack buffer and reads the cursor from top of stack
	 *
	 * @see #reset()
	 * @see #pack()
	 */
	public void setData(final byte[] v) {
		buffer = v.clone();
		cursor = XWTools.bytes2integer(buffer);
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
		putObject(user);
		putInt(code);
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
		if (obj != null) {
			putObject(obj);
		}
		putUserInterface(user, code);
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
		if (uid != null) {
			putUID(uid);
		}
		putUserInterface(user, code);
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
		if (str != null) {
			putString(str);
		}
		putUserInterface(user, code);
	}

	/**
	 * This pushes a byte
	 *
	 * @param v
	 *            is the byte to insert
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public void putByte(final byte v) throws RemoteException {
		try {
			buffer[cursor++] = v;
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This pops a byte
	 *
	 * @return the extracted byte
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public byte getByte() throws RemoteException {
		try {
			return buffer[--cursor];
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This pushes an integer
	 *
	 * @param v
	 *            is the integer to insert
	 * @exception Exception
	 *                is thrown on I/O error
	 */
	public void putInt(final int v) throws RemoteException {
		try {
			final byte[] bint = XWTools.integer2bytes(v);
			for (int i = 0; i < XWTools.SIZEOFINTEGER; i++) {
				putByte(bint[i]);
			}
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
			final byte[] bint = new byte[XWTools.SIZEOFINTEGER];
			cursor -= XWTools.SIZEOFINTEGER;
			System.arraycopy(buffer, cursor, bint, 0, XWTools.SIZEOFINTEGER);
			return XWTools.bytes2integer(bint);
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This pushes the UTF-8 representation of the given String
	 *
	 * @param v
	 *            is the String to insert
	 * @exception Exception
	 *                is thrown on on I/O error
	 */
	public void putString(final String v) throws RemoteException {
		putArray(v.getBytes(XWTools.UTF8));
	}

	/**
	 * This pops a String
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
	 * This pushes a byte array<br />
	 * This first putes the array content itself if any, then the array size as
	 * an integer<br />
	 * If v is null a single 0 is only inserted
	 *
	 * @param barray
	 *            is the array to insert
	 * @exception Exception
	 *                is thrown on I/O error or if the provided file does not
	 *                exist
	 */
	public void putArray(final byte[] barray) throws RemoteException {
		try {
			if (barray == null) {
				putInt(0);
				return;
			}

			final int length = barray.length;

			// let reverse byte order so that getArray() is just fine
			for (int i = length - 1; i >= 0; i--) {
				putByte(barray[i]);
			}

			putInt(length);
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This pops a byte array <br />
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
			for (int i = 0; i < length; i++) {
				ret[i] = getByte();
			}

			return ret;
		} catch (final Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	/**
	 * This pushes a Vector; this first writes vector size following by vector
	 * datas, if any<br />
	 * If v is null, this writes a single 0
	 *
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

		for (int i = 0; i < v.size(); i++) {
			putString(v.elementAt(i).toString());
		}

		putInt(v.size());
	}

	/**
	 * This pops a Vector; this first reads vector size then vector datas, if
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
	 * This pushes an XML object representation as String
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
	 * This pushes an UID String representation
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
	 * This pops an UID String representation
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
			final ByteStack b0 = new ByteStack();
			final ByteStack b1 = new ByteStack();

			b0.putInt(54321);
			if (args.length > 0) {
				b0.putString(args[0]);
			} else {
				b0.putString("this is a test only");
			}
			b0.pack();
			b1.setData(b0.getData());

			System.out.println("getString = " + b1.getString());
			System.out.println("getInt    = " + b1.getInt());
		} catch (final Exception e) {
			System.out.println(e.toString());
		}
	}

}
