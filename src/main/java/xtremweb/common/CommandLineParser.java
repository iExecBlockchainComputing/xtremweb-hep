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

import java.io.File;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Vector;

import xtremweb.communications.IdRpc;
import xtremweb.communications.URI;

/**
 * Created: August 30th, 2005<br />
 *
 * @see xtremweb.common.CommandLineOptions
 * @see xtremweb.communications.IdRpc
 * @author <a href="mailto:lodygens a lal.in2p3.fr">Oleg Lodygensky</a>
 * @since 1.9.0
 */

public final class CommandLineParser {

	private final Logger logger;

	/**
	 * This is the command line option prefix
	 */
	public static final String PREFIX = "--xw";
	/**
	 * This stores parameters This depends on the command; it can be empty,
	 * contains one or more values
	 *
	 * @see #command
	 */
	private final Object[] commandParams;
	/**
	 * This stores optionnal parameters This depends on the command; it can be
	 * empty, contains one or more values
	 *
	 * @see #command
	 */
	private final Object[] optionnalParameters;

	/**
	 * This stores the command provided on command line
	 */
	private IdRpc command;

	/**
	 * This converts command from a String.
	 *
	 * @param s
	 *            is the value to convert
	 * @return the command represented by the parameter
	 */
	public IdRpc command(final String s) throws IllegalArgumentException {
		String argument = s;
		if (argument.toLowerCase().startsWith(PREFIX)) {
			argument = argument.toUpperCase().substring(PREFIX.length());
		}
		return IdRpc.valueOf(argument);
	}

	/**
	 * This converts command line option from a String.
	 *
	 * @param s
	 *            is the value to convert
	 * @return the command line option represented by the parameter
	 */
	public CommandLineOptions option(final String s) throws IllegalArgumentException {
		String argument = s;
		if (argument.toLowerCase().startsWith(PREFIX)) {
			argument = argument.toUpperCase().substring(PREFIX.length());
		}
		return CommandLineOptions.valueOf(argument);
	}

	/**
	 * This sets an option providing its parameter
	 *
	 * @param opt
	 *            is the option to set param for
	 * @param o
	 *            is the option param
	 */
	public void setOption(final CommandLineOptions opt, final Object o) {
		logger.config("setOption " + opt + ":" + o);
		final int i = opt.ordinal();
		if (opt != CommandLineOptions.ENV) {
			optionnalParameters[i] = o;
			return;
		}
		if (optionnalParameters[i] == null) {
			optionnalParameters[i] = new Vector();
		}
		final Vector v = (Vector) optionnalParameters[i];
		v.add(o);
	}

	/**
	 * This retrieves an option parameter
	 *
	 * @param opt
	 *            is the option to retrieve param for
	 * @return the option parameter
	 * @throws IndexOutOfBoundsException
	 *             if the option has no param
	 */
	public Object getOption(final CommandLineOptions opt) throws IndexOutOfBoundsException {
		return optionnalParameters[opt.ordinal()];
	}

	/**
	 * This retrieves the list of option parameters
	 *
	 * @return the list of option parameters
	 */
	public Object[] getOptions() {
		return optionnalParameters;
	}

	/**
	 * This tries to convert the parameter to URI or UID
	 *
	 * @return an UID if param represents an UID; an URI if param represents an
	 *         URI; param itself otherwise
	 */
	private Object checkParam(final String param) {

		try {
			// is it an UID ?
			return new UID(param);
		} catch (final IllegalArgumentException e) {
		}

		try {
			// is it an URI ?
			final URI u = new URI(param);
			if (u.getScheme() != null) {
				return u;
			}
		} catch (final URISyntaxException e) {
		}

		try {
			// is it a CPU ?
			return CPUEnum.getCpu(param.toUpperCase());
		} catch (final IllegalArgumentException e) {
		}

		try {
			// is it an OS ?
			return OSEnum.valueOf(param.toUpperCase());
		} catch (final IllegalArgumentException e) {
		}

		try {
			// is it a data type ?
			return DataTypeEnum.valueOf(param.toUpperCase());
		} catch (final Exception e) {
		}
		try {
			// an application type ?
			return AppTypeEnum.valueOf(param.toUpperCase());
		} catch (final Exception e) {
		}

		// any other parameter
		return param;
	}

	/**
	 */
	public static void usageHeader(final String header) {
		final Logger l = new Logger();
		if (header != null) {
			l.info(header);
		}
	}

	/**
	 * This prints usage
	 */
	public static void usage(final String header) {
		final Logger l = new Logger();
		usageHeader(header);
		l.info("Available commands:");
		for (final IdRpc i : IdRpc.values()) {
			l.info(i.helpClient());
		}
		l.info("Available options :");
		for (final CommandLineOptions c : CommandLineOptions.values()) {
			l.info(c.usage());
		}
	}

	/**
	 * This prints usage for a given command
	 *
	 * @param header
	 *            is a String
	 * @param i
	 *            is the command to print usage for
	 */
	public static void usage(final String header, final IdRpc i) {
		usageHeader(header);
		final Logger l = new Logger();
		l.info("\t" + i.helpClient());
	}

	/**
	 * This prints usage for a given command
	 *
	 * @param header
	 *            is a String
	 * @param i
	 *            is the command to print usage for
	 */
	public static void usage(final String header, final CommandLineOptions i) {
		usageHeader(header);
		final Logger l = new Logger();
		l.info("\t" + i.usage());
	}

	/**
	 * This retrieve the command found on command line
	 */
	public IdRpc command() {
		return command;
	}

	/**
	 * This retrieves the commandParams
	 */
	public Object commandParams(final IdRpc c) {
		return commandParams[c.ordinal()];
	}

	/**
	 * This retrieves the commandParams for the command found on command line
	 */
	public Object commandParams() {
		return commandParams[command.ordinal()];
	}

	/**
	 * <p>
	 * This sets the default commandParams for the command found on command
	 * line. <blockquote> This is typically used to set UIDs found on command
	 * line. </blockquote>
	 * </p>
	 * <p>
	 * This does nothing if commandParams is already set <blockquote> (i.e. if
	 * setAction() has been called) </blockquote>
	 * </p>
	 *
	 * @see #setAction(IdRpc)
	 */
	private void setCommandParams(final Vector v) {
		if (command == IdRpc.NULL) {
			return;
		}
		if (commandParams[command.ordinal()] == null) {
			commandParams[command.ordinal()] = v;
		}
	}

	/**
	 * This sets action with no commandParams
	 *
	 * @param i
	 *            the action to perform in this instance
	 * @see #setAction(IdRpc, Object)
	 */
	private void setAction(final IdRpc i) throws ParseException, IndexOutOfBoundsException {
		logger.config("setAction " + i);
		setAction(i, null);
	}

	/**
	 * This sets action and its commandParams
	 *
	 * @see #commandParams
	 * @see #command
	 */
	private void setAction(final IdRpc i, final Object obj) throws ParseException, IndexOutOfBoundsException {

		if (command != IdRpc.NULL) {
			throw new ParseException("can't define two simultaneous actions", 0);
		}

		command = i;
		commandParams[command.ordinal()] = obj;
		logger.config("setAction " + i + ":" + obj);
	}

	/**
	 * This retrieve the help flag
	 */
	public boolean help() {
		try {
			return getOption(CommandLineOptions.HELP) != null;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This retrieve the verbose flag ("--xwverbose")
	 */
	public boolean isVerbose() {
		try {
			return getOption(CommandLineOptions.VERBOSE) != null;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This tests whether there's an output format on command line
	 */
	public boolean format() {
		try {
			return getOption(CommandLineOptions.FORMAT) != null;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This tells if output format is SHORT
	 *
	 * @since 7.0.0
	 */
	public boolean shortFormat() {
		try {
			return (OutputFormat) getOption(CommandLineOptions.FORMAT) == OutputFormat.SHORT;
		} catch (final Exception e) {
			logger.exception(e);
			return false;
		}
	}

	/**
	 * This tells if output format is TEXT
	 */
	public boolean text() {
		try {
			return (OutputFormat) getOption(CommandLineOptions.FORMAT) == OutputFormat.TEXT;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This tells if output format is TEXT
	 */
	public boolean csv() {
		try {
			return (OutputFormat) getOption(CommandLineOptions.FORMAT) == OutputFormat.CSV;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This tells if output format is XML
	 */
	public boolean xml() {
		try {
			return (OutputFormat) getOption(CommandLineOptions.FORMAT) == OutputFormat.XML;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This tells if output format is HTML
	 */
	public boolean html() {
		try {
			return (OutputFormat) getOption(CommandLineOptions.FORMAT) == OutputFormat.HTML;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * This dumps all parameters if verbose flag is set ("--xwverbose")
	 */
	public void verbose() {

		if (!isVerbose()) {
			return;
		}

		try {
			logger.info("request    = " + command);
			if (commandParams[command.ordinal()] instanceof Vector) {
				int i = 0;
				for (final Enumeration e = ((Vector) commandParams[command.ordinal()]).elements(); e
						.hasMoreElements();) {
					logger.info("\tparameter[" + i++ + "] = " + e.nextElement());
				}
			} else {
				logger.info("parameters = " + commandParams[command.ordinal()]);
			}
		} catch (final Exception e) {
		}
	}

	public static String optionText(final IdRpc c) {
		return PREFIX + c.toString().toLowerCase();
	}

	public static String optionText(final CommandLineOptions c) {
		return PREFIX + c.toString().toLowerCase();
	}

	/**
	 * This is the default constructor
	 */
	public CommandLineParser() {
		super();
		logger = new Logger(this);
		optionnalParameters = new Object[IdRpc.SIZE];
		commandParams = new Object[IdRpc.SIZE];
		command = IdRpc.NULL;
		setOption(CommandLineOptions.FORMAT, OutputFormat.SHORT);
	}

	/**
	 * This constructor parses arguments
	 *
	 * @param args
	 *            is an array of String ocntaining the command line arguments
	 */
	public CommandLineParser(final String[] args) throws IllegalArgumentException, ParseException {

		this();

		if ((args == null) || (args.length == 0)) {
			throw new IllegalArgumentException("no arg provided");
		}

		final Vector params = new Vector();

		int i = 0;

		for (i = 0; i < args.length;) {

			IdRpc arg = IdRpc.NULL;

			CommandLineOptions opt = CommandLineOptions.NONE;
			try {
				arg = command(args[i]);
				setAction(arg);
			} catch (final IllegalArgumentException notACommand) {
				try {
					opt = option(args[i]);

					switch (opt) {
					case SMARTSOCKETSPROXY:
						// smartsockets hub address is optional
						try {
							option(args[i + 1]);
							setOption(opt, new Boolean(true));
						} catch (final IllegalArgumentException pouet) {
							try {
								command(args[i + 1]);
								setOption(opt, new Boolean(true));
							} catch (final IllegalArgumentException pouet2) {
								// args[i+1] is neither a command, nor an
								// option, it's certainly a hub address
								setOption(opt, args[++i]);
							}
						}
						break;
					case MACRO:
					case LABEL:
					case FORWARDADDRESSES:
					case FORWARDPORT:
					case LISTENPORT:
						setOption(opt, args[++i]);
						break;
					case XML:
						setOption(opt, new File(args[++i]));
						break;
					case CLIENTLOOPDELAY:
					case REPLICA:
					case REPLICASIZE:
						setOption(opt, new Integer(args[++i]));
						break;
					case GROUP:
					case SESSION:
					case EXPECTEDHOST:
					case EXPECTEDWORK:
						setOption(opt, new UID(args[++i]));
						break;
					case ENV:
					case ENVVAR:
					case STDIN:
					case CERT:
					case OUT:
					case PACKAGE:
						setOption(opt, checkParam(args[++i]));
						break;
					case UPDATEWORKERS:
						params.add(args[++i].toLowerCase());
						setOption(opt, new Boolean(true));
						setAction(IdRpc.ACTIVATEHOST);
						break;
					case FORMAT:
						try {
							setOption(opt, OutputFormat.valueOf(args[++i].toUpperCase()));
						} catch (final Exception ef) {
							logger.debug("Forcing format to SHORT : " + ef);
							setOption(CommandLineOptions.FORMAT, OutputFormat.SHORT);
							i--;
						}
						break;
					case STATUS:
						try {
							setOption(opt, StatusEnum.valueOf(args[++i].toUpperCase()));
						} catch (final Exception e) {
							logger.exception(e);
						}
						break;
					case CONFIG:
						try {
							final XWConfigurator config = new XWConfigurator(args[++i], false);
							setOption(opt, config);
							logger.setLoggerLevel(config.getLoggerLevel());
							break;
						} catch (final Exception e) {
							logger.exception(e);
							System.exit(1);
						}
						break;
					default:
						setOption(opt, new Boolean(true));
						break;
					}
				} catch (final Exception notAnOption) {
					//
					// this is not a an option
					//
					logger.finest(args[i] + " is an argument");

					params.add(checkParam(args[i]));
				}
			}
			i++;
		}

		try {
			if (params.size() == 0) {
				setCommandParams(null);
			} else {
				setCommandParams(params);
			}
		} catch (final IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid argument :" + e.getMessage());
		}
	}

	public static void main(final String[] argv) {
		IdRpc.main(argv);
		CommandLineOptions.main(argv);
	}
}
