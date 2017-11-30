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

import org.junit.Test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

import static org.junit.Assert.assertTrue;

/**
 * This tests XML serialization
 * 
 * Created: 18 mai 2017
 * 
 * @author Oleg Lodygensky
 * @version 1.0
 */

public class XMLHashtableTest {

	/**
	 * This tests object XML serialization by writing then reading back an object.
	 */
	@Test
	public void start() {
		try {
			final Collection v = new Vector();
			v.add(new String("a string in vector"));
			v.add(new Integer(100));
			v.add(new Boolean("true"));
			final Hashtable h = new Hashtable();
			final Hashtable subHash = new Hashtable();
			h.put(new Integer(1), new String("un"));
			h.put(new String("deux"), new Integer(2));
			h.put(new String("a vector"), v);
			h.put(new String("a null UID"), UID.NULLUID);
			h.put(new String("a false boolean"), new Boolean("false"));
			subHash.put(new Integer(10), new String("dix"));
			subHash.put(new String("dix"), new Integer(10));
			subHash.put(new String("a vector"), v);
			h.put(new String("an hashtable"), subHash);

			final XMLHashtable hRead = new XMLHashtable();
			final XMLHashtable hWrite = new XMLHashtable(h);

			final File temp = File.createTempFile("xw-junit", "itf");
			final FileOutputStream fout = new FileOutputStream(temp);
			final DataOutputStream out = new DataOutputStream(fout);
			hWrite.setDUMPNULLS(true);
			final XMLWriter writer = new XMLWriter(out);
			writer.write(hWrite);

			final XMLReader reader = new XMLReader(hRead);
			reader.read(new FileInputStream(temp));
			hRead.setDUMPNULLS(true);
			assertTrue(hWrite.toXml().equals(hRead.toXml()));
		} catch (final Exception e) {
			assert(false);
		}
	}
}