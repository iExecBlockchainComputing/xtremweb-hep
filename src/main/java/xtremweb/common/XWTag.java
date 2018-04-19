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

import org.xml.sax.Attributes;

public enum XWTag {

	APP {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new AppInterface(attrs);
		}
	},
	CATEGORY {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 13.0.0
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new CategoryInterface(attrs);
		}
	},
	DATA {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new DataInterface(attrs);
		}
	},
	GROUP {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new GroupInterface(attrs);
		}
	},
	HOST {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new HostInterface(attrs);
		}
	},
	JOB {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return null;
		}
	},
	SESSION {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new SessionInterface(attrs);
		}
	},
	TASK {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new TaskInterface(attrs);
		}
	},
	TRACE {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new TraceInterface(attrs);
		}
	},
	USERGROUP {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new UserGroupInterface(attrs);
		}
	},
	USER {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new UserInterface(attrs);
		}
	},
	WORK {
		/**
		 * This creates a new interface
		 *
		 * @param attrs
		 *            contains the attributes found from the XML description
		 * @since 8.2.2
		 */
		@Override
		public Table newInterface(final Attributes attrs) {
			return new WorkInterface(attrs);
		}
	};

	public static final XWTag LAST = WORK;
	public static final int SIZE = LAST.ordinal() + 1;

	/**
	 * This creates a new interface for the given tag
	 *
	 * @param attrs
	 *            contains the attributes found from the XML description
	 * @since 8.2.2
	 */
	public abstract Table newInterface(Attributes attrs);

	/**
	 * This retrieves an XWTag from its integer value
	 *
	 * @param v
	 *            is the integer value of the XWTag
	 * @return an XWTag
	 */
	public static XWTag fromInt(final int v) throws IndexOutOfBoundsException {
		for (final XWTag i : XWTag.values()) {
			if (i.ordinal() == v) {
				return i;
			}
		}
		throw new IndexOutOfBoundsException("unvalid XWTag value " + v);
	}

	public static void main(final String[] argv) {
		for (final XWTag i : XWTag.values()) {
			System.out.println(i.toString() + " = " + i.ordinal() + " valueOf() = " + XWTag.valueOf(i.toString()));
		}
	}

}
