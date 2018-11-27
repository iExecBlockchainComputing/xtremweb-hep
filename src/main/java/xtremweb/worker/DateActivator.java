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

package xtremweb.worker;

/**
 *  File   : DateActivator.java<br />
 *  Date   : Jan 7th, 2005.
 *  @author : Oleg Lodygensky (lodygens /at\ lal.in2p3.fr)
 */

import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import xtremweb.common.Logger;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;

/**
 * This activates the worker accordingly to date/time gaps 'a la' crontab,
 * providing a string with up to two fields.<br>
 * Gap may define day of week and hour of day<br>
 * Several comma separated gaps may be provided<br>
 * <br>
 * Field grammar:
 * <ul>
 * <li>'*' : every possible values
 * <li>&lt;x&gt; : a unic value
 * <li>&lt;x-y&gt; : a couple of values separated by an hyphen ('-') defining a
 * gap from x to y
 * </ul>
 * <br>
 * Two fields are allowed :
 * <ul>
 * <li>1st field defines days of week
 * <ul>
 * <li>'*' for every day
 * <li>'1', 'Sun' or 'Sunday'
 * <li>'2', 'Mon' or 'Monday'
 * <li>...
 * <li>'7', 'Sat' or 'Saturday'
 * <li>&lt;N-M&gt; a couple (of integers or Strings) defining days from N to M
 * </ul>
 * <li>2nd field defines hours
 * <ul>
 * <li>'*' every hour
 * <li>&lt;N&gt; an integer defining a complete hour (0 <= N <= 23)
 * <li>&lt;N-M&gt; a couple of integers defining hours from N:00 to M:59 (0,0 <=
 * N,M <= 23,23)
 * </ul>
 * </ul>
 * <br>
 * Examples:
 * <ul>
 * <li>* * : every days, full day
 * <li>* 1 : every days, from 1:00AM to 1:59AM
 * <li>* 11-17 : every days, from 11:00AM to 5:59PM
 * <li>1 * : every sunday, full day
 * <li>Thu * : every thursday, full day
 * <li>2-Thu * : from monday to thursday, full day
 * <li>5-2 * : from thursday to monday, full day
 * <li>Thu-Tue 18-3 : from thursday to tuesday, from 6:00PM to 3:59AM
 * <li>Two gaps at a time:
 * <ul>
 * <li>Thu-Tue 18-3, Sat-Sun * : from thursday to tuesday, from 6:00PM to 3:59AM
 * **AND** every week end, full day
 * </ul>
 * </ul>
 *
 * @see java.util.Calendar
 * @see xtremweb.common.XWConfigurator#refresh(boolean)
 *
 */

public class DateActivator extends PollingActivator {

	/**
	 * This inner class contains one gap : hour and day
	 */
	public class Gap {
		/**
		 * This is the first day of week gap
		 */
		private int firstDay;
		/**
		 * This is the last day of week gap
		 */
		private int lastDay;
		/**
		 * This is the first hour of day gap
		 */
		private int firstHour;
		/**
		 * This is the last hour of day gap
		 */
		private int lastHour;

		/**
		 * This only and default constructor sets all to -1
		 */
		public Gap() {
			firstDay = -1;
			lastDay = -1;
			firstHour = -1;
			lastHour = -1;
		}

		/**
		 * This returns firstDay
		 */
		public int firstDay() {
			return firstDay;
		}

		/**
		 * This returns lastDay
		 */
		public int lastDay() {
			return lastDay;
		}

		/**
		 * This returns firstHour
		 */
		public int firstHour() {
			return firstHour;
		}

		/**
		 * This returns lastHour
		 */
		public int lastHour() {
			return lastHour;
		}

		/**
		 * This sets firstDay
		 */
		public void setFirstDay(final int v) {
			firstDay = v;
		}

		/**
		 * This sets lastDay
		 */
		public void setLastDay(final int v) {
			lastDay = v;
		}

		/**
		 * This sets firstHour
		 */
		public void setFirstHour(final int v) {
			firstHour = v;
		}

		/**
		 * This sets lastHour
		 */
		public void setLastHour(final int v) {
			lastHour = v;
		}

		@Override
		public String toString() {
			return "" + firstDay + "-" + lastDay + " " + firstHour + "-" + lastHour;
		}
	}

	/**
	 * This Vector contains <code>gap</code> objects
	 */
	private final Collection<Gap> gaps;

	/**
	 * This defines the day of week first index
	 */
	private final int MIN_DAY = Calendar.SUNDAY;
	/**
	 * This defines the day of week last index
	 */
	private final int MAX_DAY = Calendar.SATURDAY;
	/**
	 *  * This defines the hour of day first index
	 */
	private final int MIN_HOUR = 0;
	/**
	 * This defines the hour of day last index
	 */
	private final int MAX_HOUR = 23;

	/**
	 * This stores the day of week values that can be used in gaps definition
	 */
	private final Hashtable days;

	/**
	 * This calendar helps to calculate event occurences
	 */
	private final GregorianCalendar calendar;

	/**
	 * This is the default contructor, initializing an empty gap.
	 */
	public DateActivator() {
		super();

		gaps = new Vector<>();
		days = new Hashtable();
		calendar = new GregorianCalendar();
		days.put(new Integer(Calendar.SUNDAY), new Integer(Calendar.SUNDAY));
		days.put(new Integer(Calendar.MONDAY), new Integer(Calendar.MONDAY));
		days.put(new Integer(Calendar.TUESDAY), new Integer(Calendar.TUESDAY));
		days.put(new Integer(Calendar.WEDNESDAY), new Integer(Calendar.WEDNESDAY));
		days.put(new Integer(Calendar.THURSDAY), new Integer(Calendar.THURSDAY));
		days.put(new Integer(Calendar.FRIDAY), new Integer(Calendar.FRIDAY));
		days.put(new Integer(Calendar.SATURDAY), new Integer(Calendar.SATURDAY));
		days.put(new String("sun"), new Integer(Calendar.SUNDAY));
		days.put(new String("sunday"), new Integer(Calendar.SUNDAY));
		days.put(new String("mon"), new Integer(Calendar.MONDAY));
		days.put(new String("monday"), new Integer(Calendar.MONDAY));
		days.put(new String("tue"), new Integer(Calendar.TUESDAY));
		days.put(new String("tuesday"), new Integer(Calendar.TUESDAY));
		days.put(new String("wed"), new Integer(Calendar.WEDNESDAY));
		days.put(new String("wednesday"), new Integer(Calendar.WEDNESDAY));
		days.put(new String("thu"), new Integer(Calendar.THURSDAY));
		days.put(new String("thursday"), new Integer(Calendar.THURSDAY));
		days.put(new String("fri"), new Integer(Calendar.FRIDAY));
		days.put(new String("friday"), new Integer(Calendar.FRIDAY));
		days.put(new String("sat"), new Integer(Calendar.SATURDAY));
		days.put(new String("saturday"), new Integer(Calendar.SATURDAY));
	}

	/**
	 * This initializes the activator accordingly to the config file
	 *
	 * @param c
	 *            is the Properties read from file
	 * @see PollingActivator#initialize(XWConfigurator)
	 * @see #parse(String)
	 * @exception Exception
	 *                is thrown on gap definition error
	 */
	@Override
	public void initialize(final XWConfigurator c) {

		super.initialize(c);

		if (getConfig() == null) {
			getLogger().error("config is null");
		}
		setParams(getConfig().getProperty(XWPropertyDefs.ACTIVATIONDATE));
		try {
			this.parse(getParams());
		} catch (final Exception e) {
			getLogger().exception(e);
		}
	}

	/**
	 * This sets this activator parameters and retreives activation gaps. If p
	 * is not a valid parameter, this keeps the old params
	 *
	 * @param p
	 *            is this activator parameters string
	 * @see #parse()
	 */
	@Override
	public void setParams(final String p) {
		final String oldp = getParams();
		try {
			super.setParams(p);
			getConfig().setProperty(XWPropertyDefs.ACTIVATIONDATE, oldp);
			parse();
			return;
		} catch (final Exception e) {
			getLogger().exception(e);
		}
		try {
			super.setParams(oldp);
			getConfig().setProperty(XWPropertyDefs.ACTIVATIONDATE, oldp);
			parse();
		} catch (final Exception e) {
			getLogger().exception(e);
		}
	}

	/**
	 * This parses a String to get activation gaps
	 *
	 * @see #setParams(String)
	 */
	public void parse(final String p) {
		setParams(p);
	}

	/**
	 * This parses a String to get acitvation gaps.
	 *
	 * @param gaps
	 *            defines the intervalles
	 * @exception ParseException
	 *                if provided values are unexpected(unknown string, bad
	 *                format...)
	 */
	private void parse() throws ParseException {

		if (getParams() == null) {
			return;
		}

		final StringTokenizer severalChoices = new StringTokenizer(getParams(), ",;");
		while (severalChoices.hasMoreTokens()) {

			final String aChoice = severalChoices.nextToken();

			final StringTokenizer tokenizer = new StringTokenizer(aChoice, "\t ");
			final Object[][] tab = new Object[2][2];

			tab[0][0] = null;
			tab[0][1] = null;
			tab[1][0] = null;
			tab[1][1] = null;

			for (int i = 0; tokenizer.hasMoreTokens();) {

				final String value = tokenizer.nextToken();
				final StringTokenizer st = new StringTokenizer(value, "-");

				for (int j = 0; st.hasMoreTokens();) {

					final String tuple = st.nextToken();
					if (tuple.compareTo("*") == 0) {
						tab[i][j] = new Integer(Integer.MAX_VALUE);
					} else {
						try {
							tab[i][j] = new Integer(tuple);
						} catch (final NumberFormatException e) {
							tab[i][j] = new String(tuple.toLowerCase());
						}
					}
					j = (j + 1) % 2;
				}

				i = (i + 1) % 2;
			}

			final Gap aGap = new Gap();

			aGap.setFirstHour(MIN_HOUR);
			aGap.setLastHour(MAX_HOUR);

			try {
				try {
					if (((Integer) tab[0][0]).intValue() == Integer.MAX_VALUE) {
						aGap.setFirstDay(MIN_DAY);
						aGap.setLastDay(MAX_DAY);
					} else {
						aGap.setFirstDay(((Integer) days.get(tab[0][0])).intValue());
					}
				} catch (final ClassCastException cce) {
					aGap.setFirstDay(((Integer) days.get(tab[0][0])).intValue());
				}
			} catch (final Exception e) {
				throw new ParseException("first day invalid format", 0);
			}

			try {
				try {
					if (((Integer) tab[0][1]).intValue() == Integer.MAX_VALUE) {
						aGap.setFirstDay(MIN_DAY);
						aGap.setLastDay(MAX_DAY);
					} else {
						aGap.setLastDay(((Integer) days.get(tab[0][1])).intValue());
					}
				} catch (final ClassCastException cce) {
					aGap.setLastDay(((Integer) days.get(tab[0][1])).intValue());
				}
			} catch (final NullPointerException np) {
				if (tab[0][1] != null) {
					throw new ParseException("last day invalid format", 0);
				}
			} catch (final Exception e) {
				throw new ParseException("last day invalid format", 0);
			}

			try {
				final int v = ((Integer) tab[1][0]).intValue();
				if ((v >= MIN_HOUR) && (v <= MAX_HOUR)) {
					aGap.setFirstHour(v);
					aGap.setLastHour(v);
				} else if (v == Integer.MAX_VALUE) {
					aGap.setFirstHour(MIN_HOUR);
					aGap.setLastHour(MAX_HOUR);
				} else {
					throw new ParseException("first hour invalid format", 2);
				}
			} catch (final NullPointerException np) {
			}

			try {
				final int v = ((Integer) tab[1][1]).intValue();
				if ((v >= MIN_HOUR) && (v <= MAX_HOUR)) {
					aGap.setLastHour(v);
				} else if (v == Integer.MAX_VALUE) {
					aGap.setLastHour(MIN_HOUR);
				} else {
					throw new ParseException("last hour invalid format", 2);
				}
			} catch (final NullPointerException np) {
			}

			if (aGap.firstDay() == -1) {
				if (aGap.lastDay() != -1) {
					aGap.setFirstDay(aGap.lastDay());
				}
			} else {
				if (aGap.lastDay() == -1) {
					aGap.setLastDay(aGap.firstDay());
				}
			}
			if (aGap.firstHour() == -1) {
				if (aGap.lastHour() != -1) {
					aGap.setFirstHour(aGap.lastHour());
				}
			} else {
				if (aGap.lastHour() == -1) {
					aGap.setLastHour(aGap.firstHour());
				}
			}

			gaps.add(aGap);
		}
	}

	/**
	 * This detect this host activity
	 *
	 * @see xtremweb.common.XWConfigurator#cpuLoad
	 * @return 0 if can run now; a long integer representing the time to wait
	 *         before being allowed to compute
	 */
	private long isActive() {

		long minimum = 60000;
		final Iterator<Gap> theIterator = gaps.iterator();
		while (theIterator.hasNext()) {
			final Gap aGap = theIterator.next();

			calendar.setTime(new Date());

			final int curDay = calendar.get(Calendar.DAY_OF_WEEK);
			int diffDay = aGap.firstDay() - curDay;

			if (diffDay < 0) {
				diffDay = MAX_DAY + diffDay;
			}
			final Logger logger = getLogger();

			if ((aGap.firstDay() <= curDay) && (aGap.lastDay() >= curDay)) {
				logger.info("[" + aGap + "] This day(" + curDay + ") applies");
				minimum = 0;
			} else {
				logger.info("[" + aGap + "] This day(" + curDay + ") does not apply; " + diffDay + " days left");
				final long timing = diffDay * 24 * 60 * 60 * 1000;
				logger.debug("[" + aGap + "] timing = " + timing + " (minimum = " + minimum + ")");
				minimum = (minimum < timing ? minimum : timing);
			}

			final int curHour = calendar.get(Calendar.HOUR_OF_DAY);
			final int minutesLeft = 60 - calendar.get(Calendar.MINUTE);
			int diffHour = aGap.firstHour() - curHour - 1;

			if (diffHour < 0) {
				diffHour = 24 + diffHour;
			}

			boolean test = false;

			if (aGap.firstHour() <= aGap.lastHour()) {
				test = ((aGap.firstHour() <= curHour) && (curHour <= aGap.lastHour()));
			} else {
				test = ((aGap.firstHour() <= curHour) || (curHour <= aGap.lastHour()));
			}

			if (test) {
				logger.info("[" + aGap + "] This hour(" + curHour + ") applies");
			} else {
				logger.info("[" + aGap + "] This hour(" + curHour + ") does not apply; " + diffHour + ":" + minutesLeft
						+ " left");
				final long timing = ((diffHour * 60) + minutesLeft) * 60 * 1000;
				logger.debug("[" + aGap + "] timing = " + timing + " (minimum = " + minimum + ")");
				minimum = (minimum < timing ? timing : minimum);
			}
		}

		return minimum;
	}

	/**
	 * This tells whether the worker can start computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work can start computing
	 */
	@Override
	protected boolean canStart() {

		setWaitingProbeInterval(isActive());
		if (getWaitingProbeInterval() == 0) {
			setWaitingProbeInterval(60000);
			return true;
		}
		return false;
	}

	/**
	 * This tells whether the worker must stop computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work must stop computing
	 */
	@Override
	protected boolean mustStop() {

		setWorkingProbeInterval(isActive());
		if (getWorkingProbeInterval() == 0) {
			setWorkingProbeInterval(60000);
			return false;
		}
		return true;
	}

	private void dump() {
		int i = 0;
		final Iterator<Gap> theIterator = gaps.iterator();
		while (theIterator.hasNext()) {
			final Gap aGap = theIterator.next();

			getLogger().info("[" + i++ + "] fd " + aGap.firstDay() + " ld " + aGap.lastDay() + " fh " + aGap.firstHour()
					+ " lh " + aGap.lastHour());
		}
	}

	/**
	 * This parses command line options to retreive config file This is for test
	 * pruposes only
	 *
	 * @param args
	 *            is an array containing the command line options
	 * @see #parse(String)
	 * @see #main(String[])
	 */
	private void parse(final String[] args) throws ParseException {

		int argc = 0;
		if (args[0].compareTo("--xwconfig") == 0) {
			setConfig(new XWConfigurator(args[1], true));
			argc = 2;
		}
		parse(args[argc]);
	}

	/**
	 * This is the standard main method for testing this class
	 */
	public static void main(final String[] args) {

		final DateActivator c = new DateActivator();
		final Logger logger = new Logger();
		try {
			c.parse(args);
		} catch (final Exception e) {
			logger.exception(e);
			System.exit(1);
		}

		c.dump();

		logger.info("isActive = " + c.isActive());
		logger.info("canBeCreated = " + c.canStart());
		logger.info("mustSop  = " + c.mustStop());
	}
}
