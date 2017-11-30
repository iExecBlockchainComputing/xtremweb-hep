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
 * Created: 18 mai 2017
 * 
 * @author Oleg Lodygensky
 * @version 1.0
 */

public class XMLtupleTest {

	/**
	 * This tests object XML serialization by writing then reading back an object.
	 */
	@Test
	public void start() {
		try {
			final XMLtuple tRead = new XMLtuple();
			final XMLtuple tWrite = new XMLtuple(new Integer(1), new String("un"));
			
			final File temp = File.createTempFile("xw-junit", "itf");
			final FileOutputStream fout = new FileOutputStream(temp);
			final DataOutputStream dos = new DataOutputStream(fout);
			tWrite.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(dos);
			writer.write(tWrite);

			final XMLReader reader = new XMLReader(tRead);
			reader.read(new FileInputStream(temp));
			tRead.setDUMPNULLS(true);
			assertTrue(tWrite.toXml().equals(tRead.toXml()));
		} catch (final Exception e) {
			e.printStackTrace();
			assert(false);
		}
	}
}