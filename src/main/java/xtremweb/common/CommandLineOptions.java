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

import xtremweb.communications.Connection;

/**
 * Created: Decembre 2006<br />
 *
 * This enumerates available command line options. This is used by
 * CommandLineParser.
 *
 * @see xtremweb.common.CommandLineParser
 * @author <a href="mailto:lodygens a lal.in2p3.fr">Oleg Lodygensky</a>
 * @since 1.9.0
 */

public enum CommandLineOptions {
	/**
	 * Exec nothing
	 */
	NONE {
		@Override
		public String help() {
			return this.toString() + " : does nothing";
		}
	},
	/**
	 * Get online help
	 */
	HELP {
		@Override
		public String help() {
			return this.toString() + " : to get this help";
		}
	},
	/**
	 * Get work according to status
	 *
	 * @since 8.2.0
	 */
	STATUS {
		@Override
		public String help() {
			return this.toString()
					+ " <STATUS> : to specify a status (to get list: java -cp xtremweb.jar xtremweb.common.XWStatus)";
		}
	},
	/**
	 * This provides a macro file. <br />
	 * Parameter : a file name
	 */
	MACRO {
		@Override
		public String help() {
			return this.toString() + " <macro file name> : provide a macro file (a text file)";
		}
	},
	/**
	 * This provides an XML file describing an object. <br />
	 * Parameter : a file name
	 */
	XML {
		@Override
		public String help() {
			return this.toString() + " <macro file name> : provide a macro file (an XML file)";
		}
	},
	/**
	 * This sets verbose mode on : verbose mode details used credentials
	 */
	VERBOSE {
		@Override
		public String help() {
			return this.toString() + " : verbose mode to get more details";
		}
	},
	/**
	 * This hides macro line number
	 */
	NOVERBOSE {
		@Override
		public String help() {
			return this.toString() + " : hide macro execution details";
		}
	},
	/**
	 * This sets the output format : text, html or xml. <br />
	 * Parameter : [html | xml | text]
	 */
	FORMAT {
		@Override
		public String help() {
			return this.toString() + " [short | xml | csv | html] : to specify output format (default = short)";
		}
	},
	/**
	 * This provides a label to the job being submitted. <br />
	 * Parameter : a string
	 */
	LABEL {
		@Override
		public String help() {
			return this.toString() + " <label> : to provide a job label";
		}
	},
	/**
	 * This inserts in a session the job being submitted. <br />
	 * Parameter : an uid
	 */
	SESSION {
		@Override
		public String help() {
			return this.toString() + " <UID> : to provide a job session";
		}
	},
	/**
	 * This inserts in a session the job being submitted. <br />
	 * Parameter : an uid
	 */
	GROUP {
		@Override
		public String help() {
			return this.toString() + " <UID> : to provide a job group";
		}
	},
	/**
	 * This sets the X509 user proxy to use. <br />
	 * Parameter : a file name
	 */
	CERT {
		@Override
		public String help() {
			return this.toString() + " <URI | file name> : to provide an X.509 proxy for a job";
		}
	},
	/**
	 * This defines the worker that must run the job being submitted. <br />
	 * Parameter : an uid
	 */
	EXPECTEDHOST {
		@Override
		public String help() {
			return this.toString() + " <UID> : to specify the worker to run the job";
		}
	},
	/**
	 * This for data driven scheduling. This defines the work to execute on data
	 * event. <br />
	 * Parameter : an uid
	 *
	 * @since 10.0.0
	 */
	EXPECTEDWORK {
		@Override
		public String help() {
			return this.toString() + " <UID> : to specify the work to launch on data event";
		}
	},
	/**
	 * This for data driven scheduling. This defines the package this data
	 * belongs to. <br />
	 * Parameter : a text
	 *
	 * @since 10.0.0
	 */
	PACKAGE {
		@Override
		public String help() {
			return this.toString() + " <text> : to specify the package this data belongs to";
		}
	},
	/**
	 * This defines the environment of the job being submitted. <br />
	 * Parameter : [ an uri | an uid | a file name | a directory name ]
	 */
	ENV {
		@Override
		public String help() {
			return this.toString()
					+ " [URI | UID | dir name | file name | zip file name] : to provide a job environment file";
		}
	},
	/**
	 * This defines the environment variables of the job being submitted
	 * Parameter : String
	 * @since 11.0.0
	 */
	ENVVAR {
		@Override
		public String help() {
			return this.toString()
					+ " [URI | UID | dir name | file name | zip file name] : to provide a job environment file";
		}
	},
	/**
	 * This defines the standard input file. <br />
	 * Parameter : [ an uri | an uid | a file name | a directory name ]
	 */
	STDIN {
		@Override
		public String help() {
			return this.toString() + " [URI | UID | file name ] : to provide job stdin";
		}
	},
	/**
	 * This defines the config file. <br />
	 * Parameter : a file name
	 */
	CONFIG {
		@Override
		public String help() {
			return this.toString() + " <config file name> : to provide a config file";
		}
	},
	/**
	 * This defines the downlaod command
	 */
	DOWNLOAD {
		@Override
		public String help() {
			return this.toString()
					+ " download datas (this works with --xwgetdata and --xwgetwork to retrieve results)";
		}
	},
	/**
	 * This defines the update worker command
	 */
	UPDATEWORKERS {
		@Override
		public String help() {
			return this.toString() + " [uids list] [on | off] : enable/disable workers";
		}
	},
	/**
	 * This option tells to not extract the ZIP result file
	 */
	NOEXTRACT {
		@Override
		public String help() {
			return this.toString() + " : to not extract the zip result file";
		}
	},
	/**
	 * This option tells to NOT compress data to be sent
	 *
	 * @since 8.1.1
	 */
	DONTZIP {
		@Override
		public String help() {
			return this.toString() + " : to not compress data";
		}
	},
	/**
	 * This option tells to keep ZIP files (results or data zipped before being
	 * sent to server)
	 */
	KEEPZIP {
		@Override
		public String help() {
			return this.toString() + " : to not delete the zip result file";
		}
	},
	/**
	 * This option tells to keep the result on server side even on successfull
	 * result download
	 */
	NOERASE {
		@Override
		public String help() {
			return this.toString() + " : to keep a copy of result on server";
		}
	},
	/**
	 * This option tells to override local files
	 */
	OVERRIDE {
		@Override
		public String help() {
			return this.toString() + " : this is not used";
		}
	},
	/**
	 * This option sets the client loop delay
	 */
	CLIENTLOOPDELAY {
		@Override
		public String help() {
			return this.toString() + " : this is not used";
		}
	},
	/**
	 * This option tells to wait for job completion
	 */
	WAIT {
		@Override
		public String help() {
			return this.toString() + " : this is not used";
		}
	},
	/**
	 * This option tells the client to listen on socket
	 */
	SHELL {
		@Override
		public String help() {
			return this.toString() + " <portNumber> : to create an XWHEP proxy (default port "
					+ +Connection.XMLRPCPORT.defaultPortValue() + ")";
		}
	},
	/**
	 * This option tells the client to act as a SmartSockets proxy. <br />
	 * Parameter : (optional) the SmartSockets hub address
	 *
	 * @since 8.0.0
	 */
	SMARTSOCKETSPROXY {
		@Override
		public String help() {
			return this.toString() + " <smartsockets hub addr>";
		}
	},
	/**
	 * This option defines the amount of expected replica for a work. <br />
	 * Parameter : the amount of expected replica
	 *
	 * @since 10.0.0
	 */
	REPLICA {
		@Override
		public String help() {
			return this.toString() + " <expected amount of replica>";
		}
	},
	/**
	 * This option defines the size of the replica set (how many replica in
	 * parallel). <br />
	 * Parameter : the size of the replica set
	 *
	 * @since 10.0.0
	 */
	REPLICASIZE {
		@Override
		public String help() {
			return this.toString() + " <replica set size>";
		}
	},
	/**
	 * This option sets the output file name
	 */
	OUT {
		@Override
		public String help() {
			return this.toString() + " <fileName> : to set output file name";
		}
	},
	/**
	 * This option sets the ports to listen to. This expects a comma separated
	 * ports list. This is used at submission to ask the worker to create
	 * proxies for the server like job to be reachable on worker side. This is
	 * also used to create proxies on client side listening these ports and
	 * forwarding to SmartSockets.
	 *
	 * @since 8.0.0
	 */
	LISTENPORT {
		@Override
		public String help() {
			return this.toString() + " <portNumber> : to set the local port to listen to";
		}
	},
	/**
	 * This option sets the ports to forward to. This is used to create a
	 * SmartSocket end point on client side using xwproxy script
	 *
	 * @since 8.1.0
	 */
	FORWARDPORT {
		@Override
		public String help() {
			return this.toString() + " <portNumber : to set the local port to forward to";
		}
	},
	/**
	 * This expects semicolon list containing tuple of SmartSockets address and
	 * local port. This helps a job running on XWHEP worker side to connect to a
	 * server like application running on XWHEP client side. e.g. "Saddr1,
	 * port1; Saddr2, port2"
	 *
	 * @since 8.0.0
	 */
	FORWARDADDRESSES {
		@Override
		public String help() {
			return this.toString() + " <smartSocketAddr> : to set the SmartSockets address to forward to";
		}
	},
	/**
	 * This option opens the GUI
	 */
	GUI {
		@Override
		public String help() {
			return this.toString() + " : to display the GUI";
		}
	};

	public static final CommandLineOptions LAST = GUI;
	public static final int SIZE = LAST.ordinal() + 1;

	/**
	 * This retrieves help
	 *
	 * @since 8.3.0
	 * @return a string containing the help
	 */
	public abstract String help();

	public static String help(final CommandLineOptions i) {
		return i.help();
	}

	public String usage() {
		return CommandLineParser.PREFIX + this.toString().toLowerCase() + help();
	}

	public static void main(final String[] argv) {
		for (final CommandLineOptions i : CommandLineOptions.values()) {
			System.out.println(i.help());
		}
	}
}
