package xtremweb.communications;
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import xtremweb.common.XMLReader;
import xtremweb.common.XMLWriter;

import org.junit.Test; 

import static org.junit.Assert.assertTrue;


/**
 * This tests XML serialization
 *
 * Created: 20 novembre 2012
 *
 * @author Oleg Lodygensky
 * @since 8.2.0
 */

public abstract class XMLRPCCommandTest {

	/**
	 * This is written to disk
	 */
	private XMLRPCCommand cmd;
	/**
	 * This is read from disk and must equals cmd
	 */
	private XMLRPCCommand cmd2;

	protected XMLRPCCommandTest() {
		setCmd(null);
		setCmd2(null);
	}

	/**
	 * This tests object XML serialization by writing then reading back an object.
	 */
	@Test public void start() {

		try {
			final File temp = File.createTempFile("xw-junit", "rpccommanditf");
			final FileOutputStream fout = new FileOutputStream(temp);
			final DataOutputStream out = new DataOutputStream(fout);
			getCmd().setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(out);
			writer.write(getCmd());

			final XMLReader reader = new XMLReader(getCmd2());
			reader.read(new FileInputStream(temp));
			getCmd2().setDUMPNULLS(true);
			assertTrue(getCmd().toXml().equals(getCmd2().toXml()));
		} catch (final Exception e) {
			e.printStackTrace();
			assert(false);
		}
	}

	/**
	 * @return the cmd
	 */
	public XMLRPCCommand getCmd() {
		return cmd;
	}

	/**
	 * @param cmd the cmd to set
	 */
	public void setCmd(XMLRPCCommand cmd) {
		this.cmd = cmd;
	}

	/**
	 * @return the cmd2
	 */
	public XMLRPCCommand getCmd2() {
		return cmd2;
	}

	/**
	 * @param cmd2 the cmd2 to set
	 */
	public void setCmd2(XMLRPCCommand cmd2) {
		this.cmd2 = cmd2;
	}
}
