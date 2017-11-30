package xtremweb.common;

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

import static org.junit.Assert.assertTrue;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.junit.Test;


/**
 * This tests XML serialization
 * 
 * Created: 15 novembre 2012
 * 
 * @author Oleg Lodygensky
 * @version 1.0
 */

public abstract class TableInterfaceTest {
	/**
	 * This is written to disk
	 */
	private Table itf;
	/**
	 * This is read from disk and must equal itf
	 */
	private Table itf2;

	protected TableInterfaceTest() {
		setItf(null);
		setItf2(null);
	}

	/**
	 * This tests object XML serialization by writing then reading back an object.
	 */
	@Test
	public void start() {

		try {
			final File temp = File.createTempFile("xw-junit", "itf");
			final FileOutputStream fout = new FileOutputStream(temp);
			final DataOutputStream out = new DataOutputStream(fout);
			getItf().setUID(UID.getMyUid());
			getItf().setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(out);
			writer.write(getItf());

			final XMLReader reader = new XMLReader(getItf2());
			reader.read(new FileInputStream(temp));
			getItf2().setDUMPNULLS(true);
			assertTrue(getItf().toXml().equals(getItf2().toXml()));
		} catch (final Exception e) {
			assert(false);
		}
	}

	/**
	 * @return the itf
	 */
	public Table getItf() {
		return itf;
	}

	/**
	 * @param itf
	 *            the itf to set
	 */
	public void setItf(Table itf) {
		this.itf = itf;
	}

	/**
	 * @return the itf2
	 */
	public Table getItf2() {
		return itf2;
	}

	/**
	 * @param itf2
	 *            the itf2 to set
	 */
	public void setItf2(Table itf2) {
		this.itf2 = itf2;
	}
}
