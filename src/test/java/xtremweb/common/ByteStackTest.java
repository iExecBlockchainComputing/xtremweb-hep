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

import static org.junit.Assert.assertTrue;

/**
 * This tests XML serialization
 * <p>
 * Created: 23 janvier 2013
 *
 * @author Oleg Lodygensky
 * @version 8.2.0
 */

public class ByteStackTest {
    private final int THEINT = 54321;
    private final String THESTRING = "this is for testing only";

    public ByteStackTest() {
    }

    @Test
    public void start() throws Exception {

        final ByteStack b0 = new ByteStack();
        final ByteStack b1 = new ByteStack();

        b0.putInt(THEINT);
        b0.putString(THESTRING);
        b0.pack();
        b1.setData(b0.getData());
        assertTrue(b1.getString().equals(THESTRING));
        assertTrue(b1.getInt() == THEINT);
    }
}
