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

package xtremweb.services.rpc;

/**
 * Date    : Mar 25th, 2005
 * Project : RPCXW / RPCXW-C
 * File    : Packet.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 */


import org.apache.log4j.Logger;

import xtremweb.common.XWTools;

/**
 * This implements a Sun RPC Proxy on UDP<br />
 * <br />
 * RPC message structure :
 * <ul>
 * <li>[0] 4 bytes : one 32 bits integer : XID (command unic ID)
 * <li>[1] 4 bytes : one 32 bits integer : MSG type (call, reply)
 * <li>[2] 4 bytes : one 32 bits integer : RPC version
 * <li>[3] 4 bytes : one 32 bits integer : the prog number (expected to auto
 * detect port)
 * <li>[4] 4 bytes : one 32 bits integer : the prog version (expected to auto
 * detect port)
 * <li>[5] 4 bytes : one 32 bits integer : the method number
 * <li>[6] 4 bytes : one 32 bits integer : Credential flavour (1 = AUTH_UNIX)
 * <li>[7] 4 bytes : one 32 bits integer : Credential length
 * <li>[8] 4 bytes : one 32 bits integer : Credential stamp
 * <li>[9] 4 bytes : one 32 bits integer : Credential machine name length
 * <li>some bytes : Credential machine name
 * <li>some bytes : Credential machine name padding bytes ((nameLength + padding
 * bytes) % 4 = 0)
 * <li>4 bytes : one 32 bits integer : UID user id
 * <li>4 bytes : one 32 bits integer : GID group id
 * <li>4 bytes : one 32 bits integer : auxilary GIDs count
 * <li>4 bytes : one 32 bits integer : auxilary GID[0]
 * <li>4 bytes : one 32 bits integer : auxilary GID[...]
 * <li>4 bytes : one 32 bits integer : auxilary GID[(auxilary GIDs count) - 1]
 * <li>4 bytes : one 32 bits integer : verifier flavor
 * <li>4 bytes : one 32 bits integer : verifier length
 *
 * <li>more bytes follow depending on method call
 * </ul>
 *
 * <b>[3]</b> and <b>[4]</b> help to to determine RPC port
 *
 */

public class Packet {

	private byte[] buffer;
	private final int[] integers;
	private final int xid;
	private final int msgType;
	private final int prog;
	private final int version;
	private final int proc;
	private String hostName;

	private static final Logger logger = Logger.getLogger(Packet.class);

	/**
	 * This constructs a new MOUNT packet calling Packet (byte [], Level,
	 * String) with a null third parameter
	 *
	 * @param inBuf
	 *            is the packet datas
	 * @param nbBytes
	 *            is the packet datas length
	 * @param h
	 *            is the new credential host name
	 * @see #Packet (byte [], Level, String)
	 */
	public Packet(final byte[] inBuf, final int nbBytes, final String h) throws Exception {
		this(inBuf, nbBytes, h, 0, 0);
	}

	/**
	 * This constructs a new MOUNT packet and change its credential accordingly
	 * to hostName parameter
	 *
	 * @param inBuf
	 *            is the packet datas
	 * @param nbBytes
	 *            is the packet datas length
	 * @param h
	 *            is the new credential host name
	 * @param userID
	 *            is the user ID of the current process
	 * @param groupID
	 *            is the group ID of the current process
	 */
	public Packet(final byte[] inBuf, final int nbBytes, final String h, final int userID,
			final int groupID) throws Exception {

		buffer = new byte[nbBytes];
		System.arraycopy(inBuf, 0, buffer, 0, nbBytes);

		integers = XWTools.bytes2integers(inBuf, inBuf.length);
		xid = integers[0];
		msgType = integers[1];

		prog = integers[3];
		version = integers[4];
		proc = integers[5];

		dump("***** Received packet *****", buffer, nbBytes);
	}

	/**
	 * This changes credential accordingly to parameter. If this packet is not a
	 * request, this does nothing
	 *
	 * @param h
	 *            is the new host name for the credential
	 * @param userID
	 *            is the user ID of the current process
	 * @param groupID
	 *            is the group ID of the current process
	 */
	protected void changeCredential(final String h, final int userID, final int groupID) throws Exception {

		if (msgType != rpcdefs.RPC_CALL) {
			return;
		}

		hostName = h;
		if (hostName == null) {
			hostName = new String("");
		}

		if (integers == null) {
			throw new Exception("integers is not set ???");
		}

		final int nbBytes = buffer.length;

		if (integers[6] != rpcdefs.AUTH_UNIX) {
			logger.warn(rpcdefs.AUTH_TEXT[integers[6]]);
		}

		final int credentialLength = integers[7];
		final int credentialMachineNameLength = integers[9];

		int padLen = 4 - (credentialMachineNameLength % 4);
		if (padLen == 4) {
			padLen = 0;
		}

		// bypass first 10 first integers and UID and GID and credential machine
		// name
		int srcOffset = (48 + credentialMachineNameLength + padLen);
		final int auxGids = integers[srcOffset / 4];

		final int newauxGids = 2;
		final int newCredentialMachineNameLength = hostName.length();
		int newPadLen = 4 - (newCredentialMachineNameLength % 4);
		if (newPadLen == 4) {
			newPadLen = 0;
		}
		final int newCredentialLength = (((credentialLength - credentialMachineNameLength)
				+ newCredentialMachineNameLength) - ((auxGids - newauxGids) * 4) - padLen) + newPadLen;

		final int newNbBytes = (nbBytes - credentialLength) + newCredentialLength;
		final byte[] newBuffer = new byte[newNbBytes];

		// copy the 7 first 32 bits integer 'as is'
		int destOffset = 0;
		System.arraycopy(buffer, 0, newBuffer, destOffset, 7 * 4);

		// modify the credential length : the 8th 32 bits integer
		byte[] anInteger = XWTools.integer2bytes(newCredentialLength);
		destOffset = 7 * 4;
		System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

		// copy the credential stamp 'as is' : the 9th 32 bits integer
		anInteger = XWTools.integer2bytes(integers[8]);
		destOffset = 8 * 4;
		System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

		// modify the credential machine name length : the 10th 32 bits integer
		anInteger = XWTools.integer2bytes(newCredentialMachineNameLength);
		destOffset = 9 * 4;
		System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

		// modify the credential machine name : following the 10th 32 bits
		// integer
		destOffset = 10 * 4;
		System.arraycopy(hostName.getBytes(), 0, newBuffer, destOffset, newCredentialMachineNameLength);

		// add necessary padding '0' at the end of the credential machine name
		if (newPadLen != 0) {
			final byte[] padding = new byte[newPadLen];
			destOffset = (10 * 4) + newCredentialMachineNameLength;
			System.arraycopy(padding, 0, newBuffer, destOffset, newPadLen);
		}

		destOffset = (10 * 4) + newCredentialMachineNameLength + newPadLen;

		int tailLen = newNbBytes - destOffset;

		srcOffset = (10 * 4) + credentialMachineNameLength + padLen;

		if (integers[6] == rpcdefs.AUTH_UNIX) {
			// change user id
			anInteger = XWTools.integer2bytes(userID);
			System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

			// change group id
			anInteger = XWTools.integer2bytes(groupID);
			destOffset += 4;
			System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

			// change nb of aux gids : set it to 2
			srcOffset += 8;
			anInteger = XWTools.integer2bytes(newauxGids);
			destOffset += 4;
			System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

			// change aux user id
			anInteger = XWTools.integer2bytes(userID);
			destOffset += 4;
			System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

			// change aux group id
			anInteger = XWTools.integer2bytes(groupID);
			destOffset += 4;
			System.arraycopy(anInteger, 0, newBuffer, destOffset, 4);

			// copy the tail 'as is'; remove all other aux uids/gids, if any
			srcOffset += ((auxGids + 1) * 4);
			destOffset += 4;
		}

		tailLen = nbBytes - srcOffset;

		System.arraycopy(buffer, srcOffset, newBuffer, destOffset, tailLen);

		buffer = newBuffer;
		dump("***** Modified packet *****", buffer, 160);
	}

	/**
	 * This retrieves this packet datas
	 */
	public byte[] getBuffer() {
		return buffer;
	}

	/**
	 * This retrieves this packet datas length
	 */
	public int getLength() {
		return buffer.length;
	}

	public int getXid() {
		return xid;
	}

	public int getMsgType() {
		return msgType;
	}

	public int getProg() {
		return prog;
	}

	public int getVersion() {
		return version;
	}

	public int getProc() {
		return proc;
	}

	/**
	 * This dumps out a bytes array on debug mode only
	 *
	 * @param msg
	 *            is a message to display first
	 * @param datas
	 *            is the array to dump
	 * @param len
	 *            is the effective length to dump
	 */
	public final void dump(final String msg, final byte[] datas, final int len) {

		String dbg = "";
		for (int i = 0; i < len; i++) {
			dbg = dbg + " " + Integer.toHexString(datas[i] & 0x000000ff);
			if (((i + 1) % 16) == 0) {
				dbg = dbg + "\n";
			}
		}

		System.out.println("\n\n" + msg + "\n" + "dump() length =  " + len + "\n" + "datas\n" + dbg);

		int[] ints;
		ints = XWTools.bytes2integers(datas, datas.length);

		logger.debug("   XID                         = " + ints[0]);
		logger.debug("   Msg  type                   = " + ints[1] + "(" + rpcdefs.RPC_TEXT[ints[1]] + ")");
		logger.debug("   RPC  Version                = " + ints[2]);
		switch (ints[3]) {
		case rpcdefs.RPC_MOUNT:
			logger.debug("   Prog Number                 = " + ints[3] + "(MOUNT)");
			break;
		case rpcdefs.RPC_NFS:
			logger.debug("   Prog Number                 = " + ints[3] + "(NFS)");
			break;
		default:
			logger.error("Unknown prog number : " + ints[3]);
		}
		logger.debug("   Prog Version                = " + ints[4]);
		try {
			if (ints[3] == rpcdefs.RPC_MOUNT) {
				logger.debug(
						"   Method number               = " + ints[5] + "(" + rpcdefs.MOUNTPROC_TEXT[ints[5]] + ")");
			} else if (ints[3] == rpcdefs.RPC_NFS) {
				logger.debug("   Method number               = " + ints[5] + "(" + rpcdefs.NFSPROC_TEXT[ints[5]] + ")");
			}
		} catch (final Exception e) {
			logger.error("Unknown method number : " + ints[5]);
		}

		logger.debug("   Credential flavor           = " + ints[6] + "(" + rpcdefs.AUTH_TEXT[ints[6]] + ")");
		logger.debug("   Credential length           = " + ints[7]);
		logger.debug("   Credential stamp            = " + ints[8]);
		final int hostnameLength = ints[9];
		logger.debug("   Credential host name length = " + hostnameLength);

		int padLen = 4 - (hostnameLength % 4);
		if (padLen == 4) {
			padLen = 0;
		}
		// bypass first 10 integers
		int offset = (40 + hostnameLength + padLen);

		final String hostname = new String(datas, (10 * 4), hostnameLength);
		logger.debug("   Credential host name        = " + hostname);

		if (integers[6] == rpcdefs.AUTH_UNIX) {
			final int uid = ints[offset / 4];
			offset += 4;
			final int gid = ints[offset / 4];

			logger.debug("   UID                         = " + uid);
			logger.debug("   GID                         = " + gid);

			offset += 4;
			final int auxgidsCount = ints[offset / 4];
			logger.debug("   Aux GIDs count              = " + auxgidsCount);
			for (int i = 0; i < auxgidsCount; i++) {
				offset += 4;
				final int auxgid = ints[offset / 4];
				logger.debug("   Aux GIDs [" + i + "]                 = " + auxgid);
			}
			offset += 4;

			logger.debug("   Verifier flavor             = " + ints[offset / 4]);
			offset += 4;
			final int verifierLength = ints[offset / 4];
			logger.debug("   Verifier length             = " + verifierLength);
			if (verifierLength != 0) {
				logger.error("Don't known how to manage Verifier length = " + verifierLength);
			}
		}

		if ((ints[3] == rpcdefs.RPC_MOUNT) && (ints[5] == rpcdefs.MOUNTPROC_MNT)) {
			offset += 4;
			final String path = new String(datas, offset, ints[offset / 4]);
			logger.debug("   Mount path length           = " + ints[offset / 4]);
			logger.debug("   Mount path                  = " + path);
		}

	}

}
