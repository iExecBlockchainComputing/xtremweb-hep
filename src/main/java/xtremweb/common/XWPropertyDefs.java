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

/**
 * Date : March 13th, 2007
 * @author Oleg Lodygensky (lodygens a t lal.in2p3.fr)
 *
 * This defines properties than can be read from config files.
 * @since 2.0.0
 */

package xtremweb.common;

import xtremweb.communications.Connection;

public enum XWPropertyDefs {

	/**
	 * Server : an integer containing the amount of seconds users don't have to
	 * relogin to Web interface
	 * <p>
	 * Property type : integer
	 * </p>
	 *
	 * @since 8.2.2
	 */
	LOGINTIMEOUT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "3600"
		 */
		@Override
		public String defaultValue() {
			return "3600";
		}
	},
	/**
	 * Server : a boolean allowing registration using delegated OpenId or OAuth authentication
	 * Property type : boolean
	 *
	 * @since 8.2.0
	 */
	DELEGATEDREGISTRATION {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * Server : the JWT issuer to authenticate with ethereum public key address
	 * @since 11.0.0
	 */
	JWTETHISSUER,
	/**
	 * Server : the JWT secrete key to decode eth auth
	 * @since 11.0.0
	 */
	JWTETHSECRET,
	/**
	 * All : ethereum wallet path
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 12.2.7
	 */
	ETHWALLETPATH,
	/**
	 * All : ethereum wallet password
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 12.2.7
	 */
	ETHWALLETPASSWORD,
	/**
	 * Server : Deposit value for the server
	 * <p>
	 * Property type : integer
	 * </p>
	 *
	 * @since 13.1.0
	 */
	RLCDEPOSIT,
	/**
	 * Server, Worker : do we connect to Ethereum blockchain ?
	 * <p>
	 * Property type : boolean
	 * </p>
	 *
	 * @since 13.0.0
	 */
	BLOCKCHAINETHENABLED,
	/**
	 *
	 */
	WORKERETHNODEADDRESS,
	/**
	 * This is the challenge public address
	 * @since 13.1.0
	 */
	ENCLAVECHALLENGE,
    IEXECWORKERPUBLICADDR,
	/**
	 * Worker : send a false contribution to blockchain; this is for testing only
	 * Property type : boolean
	 *
	 * @since 13.1.0
	 */
	FAKECONTRIBUTE,
	/**
	 * Worker : send ERROR on reveal()
	 * Property type : boolean
	 *
	 * @since 13.1.0
	 */
	FAKEREVEAL,
	/**
	 * Server, Worker : do we connect to Ethereum blockchain ?
	 * <p>
	 * Property type : boolean
	 * </p>
	 *
	 * @since 13.1.0
	 */
	SUBSCRIBEDTOPOOL,
	/**
	 * Server : the facebook application ID for
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 8.2.2
	 */
	FACEBOOKAPPID,
	/**
	 * Server : the facebook application secret key for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 8.2.2
	 */
	FACEBOOKAPPKEY,
	/**
	 * Server : the facebook auth uri
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	FACEBOOKAUTHURI,
	/**
	 * Server : the facebook discovery uri
	 * <p>
	 * Property type : URI
	 * </p>
	 *
	 * @since 10.5.0
	 */
	FACEBOOK_DISCOVERYURI,
	/**
	 * Server : the facebook scope
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	FACEBOOKSCOPE,
	/**
	 * Server : the facebook callback url
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	FACEBOOKCALLBACKURL,
	/**
	 * Server : the google discovery uri
	 * <p>
	 * Property type : URI
	 * </p>
	 *
	 * @since 10.5.0
	 */
	GOOGLE_DISCOVERYURI,
	/**
	 * Server : the google user info uri
	 * <p>
	 * Property type : URI
	 * </p>
	 *
	 * @since 10.5.0
	 */
	GOOGLE_USERINFOURI,
	/**
	 * Server : the google application ID for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 9.1.1
	 */
	GOOGLEAPPID,
	/**
	 * Server : the google application secret key for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 9.1.1
	 */
	GOOGLEAPPKEY,
	/**
	 * Server : the google auth uri
	 * <p>
	 * Property type : URI
	 * </p>
	 *
	 * @since 10.5.0
	 */
	GOOGLEAUTHURI,
	/**
	 * Server : the google scope
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	GOOGLESCOPE,
	/**
	 * Server : the google callback url
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	GOOGLECALLBACKURL,
	/**
	 * Server : the twitter application ID for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 9.1.1
	 */
	TWITTERAPPID,
	/**
	 * Server : the twitter application secret key for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 9.1.1
	 */
	TWITTERAPPKEY,
	/**
	 * Server : the twitter auth uri
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	TWITTERAUTHURI,
	/**
	 * Server : the twitter discovery uri
	 * <p>
	 * Property type : URI
	 * </p>
	 *
	 * @since 10.5.0
	 */
	TWITTER_DISCOVERYURI,
	/**
	 * Server : the twitter scope
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	TWITTERSCOPE,
	/**
	 * Server : the twitter callback url
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	TWITTERCALLBACKURL,
	/**
	 * Server : the yahoo application ID for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 9.1.1
	 */
	YAHOOAPPID,
	/**
	 * Server : the yahoo application secret key for OAuth
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 9.1.1
	 */
	YAHOOAPPKEY,
	/**
	 * Server : the yahoo auth uri
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	YAHOOAUTHURI,
	/**
	 * Server : the yahoo discovery uri
	 * <p>
	 * Property type : URI
	 * </p>
	 *
	 * @since 10.5.0
	 */
	YAHOO_DISCOVERYURI,
	/**
	 * Server : the yahoo scope
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	YAHOOSCOPE,
	/**
	 * Server : the yahoo callback url
	 * <p>
	 * Property type : string
	 * </p>
	 *
	 * @since 10.5.0
	 */
	YAHOOCALLBACKURL,
	/**
	 * All : os version
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: System.getProperty("os.version")
	 * </p>
	 */
	OSVERSION {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty("os.version")
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(propertyName());
		}

		/**
		 * This retrieves a property name as found in config file 
		 *
		 * @since 8.2.0
		 * @return "os.version"
		 */
		@Override
		public String propertyName() {
			return "os.version";
		}
	},
	/**
	 * All : java version
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : System.getProperty("java.version")
	 * </p>
	 */
	JAVAVERSION {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty("java.version")
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(propertyName());
		}

		/**
		 * This retrieves a property name as found in config file 
		 *
		 * @since 8.2.0
		 * @return "java.version"
		 */
		@Override
		public String propertyName() {
			return "java.version";
		}
	},
	/**
	 * All : java library path
	 * @since 11.5.0
	 */
	JAVALIBPATH {
		@Override
		public String defaultValue() {
			return System.getProperty(propertyName());
		}
		/**
		 * @return "java.library.path"
		 */
		@Override
		public String propertyName() {
			return "java.library.path";
		}
	},
	/**
	 * All : java version
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : System.getProperty("sun.arch.data.model")
	 * </p>
	 */
	JAVADATAMODEL {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty("sun.arch.data.model")
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(propertyName());
		}

		/**
		 * @return "sun.arch.data.model"
		 */
		@Override
		public String propertyName() {
			return "sun.arch.data.model";
		}
	},
	/**
	 * All : configuration file name
	 * <p>
	 * Property type : string
	 * </p>
	 */
	CONFIGFILE,
	/**
	 * All : log4j config file
	 * Property type : string
	 * @since 11.5.0
	 */
	LOG4JCONFIGFILE {
		@Override
		public String propertyName() {
			return "log4j.configurationFile";
		}
	},
	/**
	 * All : logger level
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : WARN
	 * </p>
	 */
	LOGGERLEVEL {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return LoggerLevel.WARN.toString()
		 */
		@Override
		public String defaultValue() {
			return LoggerLevel.WARN.toString();
		}
	},
	/**
	 * All : the uid
	 * <p>
	 * Property type : string
	 * </p>
	 */
	UID,
	/**
	 * All : this tells if a new uid should be generated. This is necessary if
	 * launching using a shared config file (i.e. launching in a cloud resource)
	 * If this is true, configuration is not modified on disk.
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 */
	FORCENEWUID {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * All : system user directory
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : System.getProperty("user.home")
	 * </p>
	 */
	USERHOMEDIR {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty("user.home")
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(propertyName());
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "user.home"
		 */
		@Override
		public String propertyName() {
			return "user.home";
		}
	},
	/**
	 * All : launcher URL where to find XtremWeb binary
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	LAUNCHERURL,
	/**
	 * All : this defines this process role
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	ROLE,
	/**
	 * Client : this defines a loop delay to get job status. This is in
	 * milliseconds.
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 60000
	 * </p>
	 */
	CLIENTLOOPDELAY {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "60000"
		 */
		@Override
		public String defaultValue() {
			return "60000";
		}
	},
	/**
	 * Worker : this tells if host accepts to run job that listens for incoming
	 * connections
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 *
	 * @since 8.0.0
	 */
	INCOMINGCONNECTIONS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * All: internode communications needs a running SmartSockets hub
	 * <p>
	 * <p>
	 * Property type : string
	 * </p>
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 8.0.0
	 */
	SMARTSOCKETSHUBADDR,
	/**
	 * Server: SmartSocket Hub may need its public IP. e.g. in a VM, external IP
	 * may not be known.
	 * <p>
	 * <p>
	 * Property type : string
	 * </p>
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 10.5.1
	 */
	SMARTSOCKETSEXTERNALADDRESS,
	/**
	 * Worker : this tells if several workers can run on a single host
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 *
	 * @see #INSTANCES
	 */
	ALONE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "true"
		 */
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * Server : admin login
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 * NB : January 2018, this is still used
	 */
	ADMINLOGIN,
	/**
	 * Server : admin password
	 * Property type : string
	 * Default : null
	 * @deprecated since hsqldb usage is not maintained any more
	 */
	ADMINPASSWORD,
	/**
	 * Server : admin uid
	 * Property type : string
	 * Default : null
	 */
	ADMINUID,
	/**
	 * Worker , Client : login
	 * Property type : string
	 * Default : null
	 */
	LOGIN,
	/**
	 * Client : this permits to act for someone else. 
	 * Delegation requires sufficient user rights.
	 * Property type : string
	 * Default : null
	 * @since 11.0.0
	 * @see UserRightEnum#MANDATED_USER
	 */
	MANDATINGLOGIN,
	/**
	 * Worker , Client : password
	 * Property type : string
	 * Default : null
	 */
	PASSWORD,
	/**
	 * Worker , Client : user uid
	 * Property type : string
	 * Default : null
	 */
	USERUID,
	/**
	 * Worker : this is the project the worker wants to help In practice, this
	 * is a user group. If this is set the worker runs jobs for this group only
	 * Property type : string
	 * Default : null
	 */
	PROJECT,
	/**
	 * All : temporary directory
	 * Property type : string
	 * Default : System.getProperty("java.io.tmp")
	 */
	TMPDIR {
		@Override
		public String defaultValue() {
			return System.getProperty(JAVATMPDIR);
		}
	},
	/**
	 * Worker : how many worker instance per host
	 * Property type : boolean
	 * Default : false
	 *
	 * @see #ALONE
	 */
	INSTANCES {
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * All : socket timeout. This is in milliseconds
	 * Property type : long integer
	 * Default : 60000 ms
	 *
	 * @since RPCXW
	 * @see #OPTIMIZENETWORK
	 */
	SOTIMEOUT {
		@Override
		public String defaultValue() {
			return "60000";
		}
	},
	/**
	 * Client, worker : how many times we keep trying on socket connection error
	 * Property type : integer
	 * Default : 50
	 *
	 * @since 7.4.0
	 * @see #OPTIMIZENETWORK
	 */
	SORETRIES {
		@Override
		public String defaultValue() {
			return "50";
		}
	},
	/**
	 * <ul>
	 * <li>Worker : delay between two work requests</li>
	 * <li>Client : delay between two result requests</li>
	 * <li>Server : delay between two database polling</li>
	 * </ul>
	 * This is in milliseconds
	 * Property type : integer
	 * Default : 15000
	 */
	TIMEOUT {
		@Override
		public String defaultValue() {
			return "15000";
		}
	},
	/**
	 * Server : how many wallclocktime before requiring more workers?
	 * Property type : integer
	 * Default : 10
	 */
	CONTRIBUTETIMEOUTMULTIPLICATOR {
		@Override
		public String defaultValue() {
			return "5";
		}
	},
	/**
	 * Server : how many wallclocktime before giving up a market order?
	 * Property type : integer
	 * Default : 10
	 */
	REVEALTIMEOUTMULTIPLICATOR {
		@Override
		public String defaultValue() {
			return "10";
		}
	},
	/**
	 * Worker : this tells how long to wait when there is no job to compute
	 * If the worker receive no job within this delay, it shuts down This is
	 * especially helpful when deploying workers on a cluster; this ensures we
	 * don't lock CPU for nothing.
	 * Property type : integer
	 * Default : -1
	 */
	NOOPTIMEOUT {
		@Override
		public String defaultValue() {
			return "-1";
		}
	},
	/**
	 * All : this tells to optimize file transfer or not
	 * If set to true we don't waste time to zip small single files
	 * Property type : boolean
	 * Default : true
	 */
	OPTIMIZEZIP {
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * All : this tells to optimize network or not
	 * If true, following are set <code>
	 * setSoLinger(false, 0); // don't wait on
	 * close s.setTcpNoDelay(true); // don't wait to send
	 * s.setTrafficClass(0x08); // maximize throughput
	 * s.setKeepAlive(false); // don't keep alive
	 * </code>
	 * Property type : boolean
	 * Default : true
	 *
	 * @see #SOTIMEOUT
	 */
	OPTIMIZENETWORK {
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * All : This is not used
	 * Property type : boolean
	 * Default : true
	 */
	OPTIMIZEDISK {
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * Dispatcher, worker : this defines the amount of physical memory; this en
	 * variable is set by xtremwebconf.sh
	 * Property type : long
	 * Default: System.getProperty("HWMEM")
	 *
	 * @see xtremweb.common.XWTools#MAXRAMSIZE
	 * @since 10.1.0
	 */
	HWMEM {
		@Override
		public String defaultValue() {
			return System.getProperty("HWMEM");
		}
	},
	/**
	 * All : default max instances of a job before we put that job in error
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 10
	 * </p>
	 */
	MAXRETRY {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "10"
		 */
		@Override
		public String defaultValue() {
			return "10";
		}
	},
	/**
	 * Dispatcher: mail server address
	 * Property type : String
	 * Default : null
	 *
	 * @since 9.1.0
	 */
	MAILSERVERADDRESS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return null
		 */
		@Override
		public String defaultValue() {
			return null;
		}
	},
	/**
	 * Dispatcher: mail server protocol
	 * Property type : String
	 * Default : "smtp"
	 *
	 * @since 9.1.0
	 */
	MAILPROTOCOL {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return null
		 */
		@Override
		public String defaultValue() {
			return "smtp";
		}
	},
	/**
	 * Dispatcher: server mail address
	 * Property type : String
	 * Default : null
	 *
	 * @since 9.1.0
	 */
	MAILSENDERADDRESS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return null
		 */
		@Override
		public String defaultValue() {
			return null;
		}
	},
	/**
	 * Server : amount of simultaneous incoming connections
	 * Property type : integer
	 * Default : 200
	 */
	MAXCONNECTIONS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "200"
		 */
		@Override
		public String defaultValue() {
			return "200";
		}
	},
	/**
	 * Server : maximum messages per connection
	 * Property type : integer
	 * Default : 2000
	 *
	 * @since 7.4.0
	 */
	MAXMESSAGES {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "2000"
		 */
		@Override
		public String defaultValue() {
			return "2000";
		}
	},
	/**
	 * Server : amount of simultaneous connection to DB
	 * Property type : integer
	 * Default : 70
	 *
	 * @since 7.4.0
	 */
	DBCONNECTIONS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "70"
		 */
		@Override
		public String defaultValue() {
			return "70";
		}
	},
	/**
	 * Worker : how many jobs to compute before shutting down
	 * <p>
	 * Property type : integer
	 * </p>
	 */
	COMPUTINGJOBS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "-1"
		 */
		@Override
		public String defaultValue() {
			return "-1";
		}
	},
	/**
	 * All : home directory
	 * Property type : string
	 * Default: System.getenv("user.home")
	 */
	HOMEDIR {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getenv("user.home")
		 */
		@Override
		public String defaultValue() {
			return System.getenv("user.home");
		}
	},
	/**
	 * All : path to xtremweb.jar
	 * Property type : string
	 */
	XWCP {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return XWOSes.getClassPath()
		 */
		@Override
		public String defaultValue() {
			return OSEnum.getClassPath();
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "xtremweb.cp"
		 */
		@Override
		public String propertyName() {
			return "xtremweb.cp";
		}
	},
	/**
	 * Dispatcher : SQL file to create DB on the fly this is typically used with
	 * hsqldb
	 * Property type : string
	 * Default: null
	 */
	DBSQLFILE,
	/**
	 * Dispatcher : database vendor
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: XWDBs.default
	 * </p>
	 */
	DBVENDOR {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return XWDBs.DEFAULT.toString()
		 */
		@Override
		public String defaultValue() {
			return XWDBs.DEFAULT.toString();
		}
	},
	/**
	 * Dispatcher : database engine
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : ""
	 * </p>
	 *
	 * @since 7.0.0
	 */
	DBENGINE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return ""
		 */
		@Override
		public String defaultValue() {
			return "";
		}
	},
	/**
	 * Dispatcher : database name
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	DBNAME,
	/**
	 * Dispatcher : database server
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	DBHOST,
	/**
	 * Dispatcher : database user
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	DBUSER,
	/**
	 * Dispatcher : database password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	DBPASS,
	/**
	 * Dispatcher : database request limit
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : 1000
	 * </p>
	 */
	DBREQUESTLIMIT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "1000"
		 */
		@Override
		public String defaultValue() {
			return "" + XWTools.MAXDBREQUESTLIMIT;
		}
	},
	/**
	 * All : class to milestone
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @see xtremweb.common.MileStone
	 */
	MILESTONES,
	/**
	 * Dispatcher : comma separated list of services to manage Property type :
	 * string Default : null
	 */
	SERVICES,
	/**
	 * Dispatcher : scheduler class
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : xtremweb.dispather.MatchingScheduler
	 * </p>
	 */
	SCHEDULERCLASS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return xtremweb.dispatcher.MatchingScheduler.class.getName()
		 */
		@Override
		public String defaultValue() {
			return xtremweb.dispatcher.MatchingScheduler.class.getName();
		}
	},
	/**
	 * All : comma separated list of known dispatchers (used for replication)
	 * Property type : string
	 * Default: null
	 */
	DISPATCHERS,
	/**
	 * All : comma separated list of known data servers (used for replication)
	 * Property type : string
	 * Default: null
	 */
	DATASERVERS,
	/**
	 * All : comma separated list of trusted adresses allowed to connect
	 * Property type : string
	 * Default: null
	 */
	TRUSTED,
	/**
	 * Dispatcher : alive period This is automatically forwarded to workers
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 300 seconds
	 * </p>
	 */
	ALIVEPERIOD {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "300"
		 */
		@Override
		public String defaultValue() {
			return "300";
		}
	},
	/**
	 * Dispatcher : alive timeout This is automatically calculated
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 3 x ALIVE
	 * </p>
	 */
	ALIVETIMEOUT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + (Integer.parseInt(ALIVEPERIOD.defaultValue()) * 5)
		 */
		@Override
		public String defaultValue() {
			return "" + (Integer.parseInt(ALIVEPERIOD.defaultValue()) * 3);
		}
	},
	/**
	 * All : SSL key directory Property name : JAVAKEYSTORE
	 * Property type : string
	 */
	JAVATRUSTSTORE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty(JAVATRUSTSTORESTRING)
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(JAVATRUSTSTORESTRING);
		}

		/**
		 * This retrieves a property name as found in config file
		 *
		 * @since 8.2.0
		 * @return "javax.net.ssl.trustStore"
		 */
		@Override
		public String propertyName() {
			return "javax.net.ssl.trustStore";
		}
	},
	/**
	 * All : SSL key directory
	 * Property type : string
	 * Default : null
	 */
	SSLKEYSTORE,
	/**
	 * All : SSL password
	 * Property type : string
	 * Default: null
	 */
	SSLKEYPASSWORD,
	/**
	 * All : SSL pass phrase
	 * Property type : string
	 * Default : null
	 */
	SSLKEYPASSPHRASE,
	/**
	 * Worker : this tells if this worker is a pilot job running on an SG
	 * resource (i.e. EGEE)
	 * Property type : boolean
	 * Default : false
	 */
	PILOTJOB {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * Worker : this is the pilotjob SG identifier (i.e. EGEE)
	 * Property type : string
	 * Default : System.getenv("GLITE_WMS_JOBID")
	 *
	 * @since 7.0.0
	 */
	SGID {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getenv("GLITE_WMS_JOBID")
		 */
		@Override
		public String defaultValue() {
			return System.getenv("GLITE_WMS_JOBID");
		}
	},
	/**
	 * Worker : this is for SpeQuloS (EDGI/JRA2) If this is set, the worker runs
	 * this job only
	 * Property type : String
	 * Default: null
	 *
	 * @since 7.2.0
	 */
	JOBID,
	/**
	 * Worker : this is for SpeQuloS (EDGI/JRA2) If this is set, the worker runs
	 * jobs for this group of jobs only
	 * Property type : String
	 * Default : null
	 *
	 * @since 7.2.0
	 */
	BATCHID,
	/**
	 * All : this is the URI of the keystore containing the server public key.
	 * This is used to update server keys so that workers and clients can
	 * download it. Security is still ensured since : the keystore contains the
	 * server public key only; the current keystore is expected to connect to
	 * server and encrypt communications; login/password are expected to access
	 * this data (the new keystore).
	 * <p>
	 * Property type : URI
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	KEYSTOREURI,
	/**
	 * Server : the directory containing user public keys
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.4.0
	 */
	USERCERTDIR,
	/**
	 * Client : user certificate file This files contains single public key
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.4.0
	 */
	USERCERT,
	/**
	 * Client : user certificate file This files contains single private key
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.4.0
	 */
	USERKEY,
	/**
	 * Client : user certificate password This contains the private key password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.4.0
	 */
	USERKEYPASSWORD,
	/**
	 * Client : user X509 proxy file This file contains X509 proxy.
	 * <p>
	 * Property type : String
	 * </p>
	 * <p>
	 * Default : $X509_USER_PROXY
	 * </p>
	 */
	X509USERPROXY {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getenv("X509_USER_PROXY")
		 */
		@Override
		public String defaultValue() {
			return System.getenv("X509_USER_PROXY");
		}

		/**
		 * This retrieves a property name as found in config file 
		 *
		 * @since 8.2.0
		 * @return "X509_USER_PROXY"
		 */
		@Override
		public String propertyName() {
			return "X509_USER_PROXY";
		}
	},
	/**
	 * Server : the directory containing CA public keys
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : $X509_CERT_DIR
	 * </p>
	 */
	CACERTDIR {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getenv("X509_CERT_DIR")
		 */
		@Override
		public String defaultValue() {
			return System.getenv("X509_CERT_DIR");
		}

		/**
		 * This retrieves a property name as found in config file
		 *
		 * @since 8.2.0
		 * @return "X509CERTDIR"
		 */
		@Override
		public String propertyName() {
			return "X509CERTDIR";
		}
	},
	/**
	 * All : cache directory
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : System.getProperty("java.io.tmpdir")
	 * </p>
	 */
	CACHEDIR {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty(JAVATMPDIR)
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(JAVATMPDIR);
		}
	},
	/**
	 * All : max amount of entry in cache <
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 10.000
	 * </p>
	 *
	 * @since 7.4.0
	 */
	CACHESIZE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "10000"
		 */
		@Override
		public String defaultValue() {
			return "10000";
		}
	},
	/**
	 * All : default communication layer
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : TCPClient
	 * </p>
	 */
	COMMLAYER {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return xtremweb.communications.TCPClient.class.getName()
		 */
		@Override
		public String defaultValue() {
			return xtremweb.communications.TCPClient.class.getName();
		}
	},
	/**
	 * Worker : this contains a list of local applications the worker shares So
	 * that users could submit jobs for this application The worker then don't
	 * download the application binary This is a comma separated list of
	 * application name. Where the application name must be the one of the
	 * registered application on server side.
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 * <p>
	 * Example :<code>
	 * virtualbox,/Applications/VirtualBox.app/Contents/MacOS/VBoxHeadless
	 * ;vlc,/Applications/VLC.app/Contents/MacOS/VLC
	 * </code>
	 * </p>
	 *
	 * @since 8.0.0
	 * @see xtremweb.common.AppTypeEnum
	 */
	SHAREDAPPS,
	/**
	 * Worker : this contains a list of local data the worker shares. This is a
	 * comma separated list of data names.
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 * <p>
	 * Example :<code> root,geant4 </code>
	 * </p>
	 *
	 * @since 8.0.0
	 */
	SHAREDDATAS,
	/**
	 * Worker : this contains a list of local paths the worker can find shared
	 * data. This is a comma separated list of paths. It must be in the same
	 * order as SHAREDDATAS. If set, the worker uses these paths as current
	 * working directory of works processing these data. If set, the worker does
	 * not send result to the XWHEP server. If set, the worker does delete data
	 * processing result; it is the responsibility of the data owner
	 *
	 * Example : SHAREDDATASPATH=/path/one,/path/two
	 *
	 * If SHAREDDATAS=dataset1,dataset2 Then the worker find dataset1 content in
	 * /path/one and dataset2 content in /path/two
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 * <p>
	 * Example :<code> root,geant4 </code>
	 * </p>
	 *
	 * @since 10.0.0
	 */
	SHAREDDATASPATH,
	/**
	 * Worker : this contains a list of local packages the worker shares This is
	 * a comma separated list of packages name
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 * <p>
	 * Example : <code>root,geant4 </code>
	 * </p>
	 *
	 * @since 8.0.0
	 */
	SHAREDPACKAGES,
	/**
	 * All : comma separated list of colon separated tuples of scheme/handler
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 * <p>
	 * Example : <code>xw:xtremweb.communications.TCPClient,http
	 * :org.apache.commons.httpclient.HttpClient</code>
	 * </p>
	 */
	COMMHANDLERS,
	/**
	 * Worker : this tells to use java runtime or src/exec/Executor
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 */
	JAVARUNTIME {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * Worker : this tells if the worker accepts binary
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 */
	ACCEPTBIN {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "true"
		 */
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * Worker : sandbox enabled
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 */
	SANDBOXENABLED {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * Worker : sandbox path
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : XWOSes.getSandboxPath()
	 * </p>
	 */
	SANDBOXPATH {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return XWOSes.getSandboxPath()
		 */
		@Override
		public String defaultValue() {
			return OSEnum.getSandboxPath();
		}
	},
	/**
	 * Worker : launch sandbox arguments
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	SANDBOXSTARTARGS,
	/**
	 * Worker : this tells to collect host traces
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 * @deprecated since long
	 */
	TRACES {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * Worker : activator call name
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: xtremweb.worker.AlwaysActive
	 * </p>
	 */
	ACTIVATORCLASS {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return xtremweb.worker.AlwaysActive.class.getName()
		 */
		@Override
		public String defaultValue() {
			return xtremweb.worker.AlwaysActive.class.getName();
		}
	},
	/**
	 * Worker : activator polling delay in minutes
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default: 1
	 * </p>
	 *
	 * @see xtremweb.worker.Activator
	 */
	ACTIVATORDELAY {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "1"
		 */
		@Override
		public String defaultValue() {
			return "1";
		}
	},
	/**
	 * Worker : activation date
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @see xtremweb.worker.DateActivator
	 */
	ACTIVATIONDATE,
	/**
	 * Worker : I don't know... this is inherited from XtremWeb 1.6 by INRIA
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 */
	TCPACTIVATORFEEDBACK {
		/**
		 * This returns "false"
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}
	},
	/**
	 * Worker : this tells to use system tray icon
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 */
	SYSTRAY {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "true"
		 */
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * Worker : this tell how many simultaneous jobs to compute in parallel
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : amount of available CPUs
	 * </p>
	 */
	WORKPOOLSIZE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "-1"
		 */
		@Override
		public String defaultValue() {
			return "-1";
		}
	},
	/**
	 * Worker : cpuLoad used with CpuActivator
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 50
	 * </p>
	 *
	 * @see xtremweb.worker.CpuActivator
	 */
	CPULOAD {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "50"
		 */
		@Override
		public String defaultValue() {
			return "50";
		}
	},
	/**
	 * All : system user name
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: System.getenv("USER")
	 * </p>
	 */
	USERNAME {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getenv("USER")
		 */
		@Override
		public String defaultValue() {
			return System.getenv("USER");
		}
	},
	/**
	 * All: use nio
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 */
	JAVANIO {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "true"
		 */
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * Worker, server : this tells to start http server
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 */
	STARTSERVERHTTP {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "true"
		 */
		@Override
		public String defaultValue() {
			return Boolean.TRUE.toString();
		}
	},
	/**
	 * Worker, server : incoming communications ACL
	 * <p>
	 * Property type : reg exp
	 * </p>
	 * <p>
	 * Default : "localhost"
	 * </p>
	 */
	SERVERCOMMACL {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "localhost"
		 */
		@Override
		public String defaultValue() {
			return "localhost";
		}
	},
	/**
	 * Worker, server : incoming communications for server status ACL
	 * <p>
	 * Property type : reg exp
	 * </p>
	 * <p>
	 * Default : "localhost"
	 * </p>
	 */
	SERVERSTATACL {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "localhost"
		 */
		@Override
		public String defaultValue() {
			return "localhost";
		}
	},
	/**
	 * Worker proxy server name or IP address
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.3.0
	 */
	PROXYSERVER ,
	/**
	 * Worker proxy server port
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.3.0
	 */
	PROXYPORT,
	/**
	 * All : this is the TCP port
	 * <p>
	 * Default : Connection.TCPPORT.defaultPortValue()
	 * </p>
	 */
	TCPPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.TCPPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.TCPPORT.defaultPortValue();
		}
	},
	/**
	 * All : this is the SSL TCP port
	 * <p>
	 * Default : Connection.TCPSPORT.defaultPortValue()
	 * </p>
	 */
	TCPSPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.TCPSPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.TCPSPORT.defaultPortValue();
		}
	},
	/**
	 * All : this is the UDP port
	 * <p>
	 * Default : Connection.UDPPORT.defaultPortValue()
	 * </p>
	 */
	UDPPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.UDPPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.UDPPORT.defaultPortValue();
		}
	},
	/**
	 * Worker : this is the worker stats HTTP port
	 * <p>
	 * Default : Connection.HTTPWORKERPORT.defaultPortValue()
	 * </p>
	 */
	HTTPWORKERPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.HTTPWORKERPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.HTTPWORKERPORT.defaultPortValue();
		}
	},
	/**
	 * All : this is the secured HTTP port
	 * <p>
	 * Default : Connection.HTTPPORT.defaultPortValue()
	 * </p>
	 */
	HTTPPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.HTTPPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.HTTPPORT.defaultPortValue();
		}
	},
	/**
	 * All : this is the secured HTTP port
	 * <p>
	 * Default : Connection.HTTPSPORT.defaultPortValue()
	 * </p>
	 */
	HTTPSPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return return "" + Connection.HTTPSPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.HTTPSPORT.defaultPortValue();
		}
	},
	/**
	 * Server : this is the SmartSockets hub port Client : this is the listening
	 * port to forward to Smarsocket
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : Connection.SMARTSOCKETSPORT.defaultPortValue()
	 * </p>
	 *
	 * @since 8.2.0
	 */
	SMARTSOCKETSPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.SMARTSOCKETSPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.SMARTSOCKETSPORT.defaultPortValue();
		}
	},
	/**
	 * All : this is the XMLRPC port
	 * <p>
	 * Default : Connection.XMLRPCPORT.defaultPortValue()
	 * </p>
	 */
	XMLRPCPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.XMLRPCPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.XMLRPCPORT.defaultPortValue();
		}
	},
	/**
	 * All : this is the Sun RPC port to forward requests
	 * <p>
	 * Default : Connection.SUNRPCPORT.defaultPortValue()
	 * </p>
	 */
	SUNRPCPORT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "" + Connection.SUNRPCPORT.defaultPortValue()
		 */
		@Override
		public String defaultValue() {
			return "" + Connection.SUNRPCPORT.defaultPortValue();
		}
	};

	/** This is the name of a Java property */
	private static final String JAVATRUSTSTORESTRING = "javax.net.ssl.trustStore";
	/** This is the name of a Java property */
	public static final String JAVATMPDIR = "java.io.tmpdir";

	/**
	 * This retrieves the default value. This must be overriden if not null
	 * expected
	 *
	 * @return System.getProperty(propertyName());
	 */
	public String defaultValue() {
		return System.getProperty(propertyName());
	}

	/**
	 * This retrieves a property name as needed in config file Since 8.2.0
	 * configuration files contains property names as defined in this enum.
	 * "Old" enum must overide this method for backward compatibility E.G:
	 * LAUNCHERURL was used to be stored as "launcher.url" and is now stored as
	 * "launcherurl" in config file
	 *
	 * @since 8.2.0
	 */
	public String propertyName() {
		return this.toString();
	}

	/**
	 * This retrieves a property from its name as found in config file This is
	 * for backward compatibility only
	 *
	 * @param s
	 *            is the property name
	 * @return the property from its property name
	 * @exception ArrayIndexOutOfBoundsException
	 *                if no property can be found given the property name
	 * @since 8.2.0
	 */
	public static XWPropertyDefs fromString(final String s) throws ArrayIndexOutOfBoundsException {

		for (final XWPropertyDefs p : XWPropertyDefs.values()) {
			if (s.compareToIgnoreCase(p.propertyName()) == 0) {
				return p;
			}
		}

		throw new ArrayIndexOutOfBoundsException("unknown property : " + s);
	}

	/**
	 * This dumps enums to stdout
	 */
	public static void main(final String[] argv) {
		for (final XWPropertyDefs p : XWPropertyDefs.values()) {
			System.out.println(p.toString() + " : " + p.defaultValue());
		}
	}
}
