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
	 * Server : the facebook application ID for OAuth
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
			return System.getProperty("os.version");
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
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
			return System.getProperty("java.version");
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
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
			return System.getProperty("sun.arch.data.model");
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
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
	CONFIGFILE {
		/**
		 * This retrieves a property name as found in configuration file This is
		 * for backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "xtremweb.config"
		 */
		@Override
		public String propertyName() {
			return "xtremweb.config";
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
			return System.getProperty("user.home");
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
	LAUNCHERURL {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "launcher.url"
		 */
		@Override
		public String propertyName() {
			return "launcher.url";
		}
	},
	/**
	 * All : this defines this process role
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	ROLE {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "xtremweb.role"
		 */
		@Override
		public String propertyName() {
			return "xtremweb.role";
		}
	},
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "client.loopdelay"
		 */
		@Override
		public String propertyName() {
			return "client.loopdelay";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "xtremweb.alone"
		 */
		@Override
		public String propertyName() {
			return "xtremweb.alone";
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
	 */
	ADMINLOGIN {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "admin.login"
		 */
		@Override
		public String propertyName() {
			return "admin.login";
		}
	},
	/**
	 * Server : admin password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	ADMINPASSWORD {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "admin.password"
		 */
		@Override
		public String propertyName() {
			return "admin.password";
		}
	},
	/**
	 * Server : admin uid
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	ADMINUID {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "admin.uid"
		 */
		@Override
		public String propertyName() {
			return "admin.uid";
		}
	},
	/**
	 * Server : worker login
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	WORKERLOGIN {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "worker.login"
		 */
		@Override
		public String propertyName() {
			return "worker.login";
		}
	},
	/**
	 * Server : worker password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	WORKERPASSWORD {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "worker.password"
		 */
		@Override
		public String propertyName() {
			return "worker.password";
		}
	},
	/**
	 * Server : worker uid
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	WORKERUID {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "worker.uid"
		 */
		@Override
		public String propertyName() {
			return "worker.uid";
		}
	},
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
	 * @see UserRightEnum#DELEGATION_USER
	 */
	MANDATINGLOGIN,
	/**
	 * Worker , Client : password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	PASSWORD,
	/**
	 * Worker , Client : user uid
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	USERUID {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "user.uid"
		 */
		@Override
		public String propertyName() {
			return "user.uid";
		}
	},
	/**
	 * Worker : this is the project the worker wants to help In practice, this
	 * is a user group. If this is set the worker runs jobs for this group only
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	PROJECT,
	/**
	 * All : temporary directory
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : System.getProperty("java.io.tmp")
	 * </p>
	 */
	TMPDIR {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty(JAVATMPDIR)
		 * @see #JAVATMPDIR
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(JAVATMPDIR);
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "path.tmpdir"
		 */
		@Override
		public String propertyName() {
			return "path.tmpdir";
		}
	},
	/**
	 * Worker : how many worker instance per host
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
	 *
	 * @see #ALONE
	 */
	INSTANCES {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "multipleInstances"
		 */
		@Override
		public String propertyName() {
			return "multipleInstances";
		}
	},
	/**
	 * All : socket timeout. This is in milliseconds
	 * <p>
	 * Property type : long integer
	 * </p>
	 * <p>
	 * Default : 60000 ms
	 * </p>
	 *
	 *
	 * @since RPCXW
	 * @see #OPTIMIZENETWORK
	 */
	SOTIMEOUT {
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
	 * Client, worker : how many times we keep trying on socket connection error
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 50
	 * </p>
	 *
	 * @since 7.4.0
	 * @see #OPTIMIZENETWORK
	 */
	SORETRIES {
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
	 * <ul>
	 * <li>Worker : delay between two work requests</li>
	 * <li>Client : delay between two result requests</li>
	 * <li>Server : delay between two database polling</li>
	 * </ul>
	 * <p>
	 * This is in milliseconds
	 * </p>
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 15000
	 * </p>
	 */
	TIMEOUT {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "15000"
		 */
		@Override
		public String defaultValue() {
			return "15000";
		}
	},
	/**
	 * Worker : this tells how long to wait when there is no job to compute
	 * <p>
	 * If the worker receive no job within this delay, it shuts down This is
	 * especially helpful when deploying workers on a cluster; this ensures we
	 * don't lock CPU for nothing.</>p
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : -1
	 * </p>
	 */
	NOOPTIMEOUT {
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
	 * All : this tells to optimize file transfer or not
	 * <p>
	 * If set to true we don't waste time to zip small single files
	 * </p>
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 */
	OPTIMIZEZIP {
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
	 * All : this telss to optimize network or not
	 * <p>
	 * If true, following are set <code>
	 * setSoLinger(false, 0); // don't wait on
	 * close s.setTcpNoDelay(true); // don't wait to send
	 * s.setTrafficClass(0x08); // maximize throughput
	 * s.setKeepAlive(false); // don't keep alive
	 * </code>
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 *
	 * @see #SOTIMEOUT
	 */
	OPTIMIZENETWORK {
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
	 * All : This is not used
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 */
	OPTIMIZEDISK {
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
	 * Dispatcher, worker : this defines the amount of physical memory; this en
	 * variable is set by xtremwebconf.sh
	 * <p>
	 * Property type : long
	 * </p>
	 * <p>
	 * Default: System.getProperty("HWMEM")
	 * </p>
	 *
	 * @see xtremweb.common.XWTools#MAXDISKSIZE
	 * @since 10.1.0
	 */
	HWMEM {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getproperty("HWMEM");
		 */
		@Override
		public String defaultValue() {
			return System.getProperty("HWMEM");
		}
	},
	/**
	 * Dispatcher, worker : this defines the maximum usable disk space per
	 * application and work; this is in Mb
	 * <p>
	 * Property type : long
	 * </p>
	 * <p>
	 * Default : XWTools.MAXDISKSIZE
	 * </p>
	 *
	 * @see xtremweb.common.XWTools#MAXDISKSIZE
	 * @since 9.1.0
	 */
	MAXDISKSPACE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return XWTools.MAXDISKSIZE
		 */
		@Override
		public String defaultValue() {
			return "" + XWTools.MAXDISKSIZE;
		}
	},
	/**
	 * Dispatcher, worker : this defines the maximum usable RAM space per
	 * application and work; this is in Kb
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : XWTools.MAXRAMSIZE
	 * </p>
	 *
	 * @see xtremweb.common.XWTools#MAXRAMSIZE
	 * @since 9.1.0
	 */
	MAXRAMSPACE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return XWTools.MAXDISKSIZE
		 */
		@Override
		public String defaultValue() {
			return "" + XWTools.MAXRAMSIZE;
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
	 * Server : wallclocktime usage; if true, wallclocktime is used Property
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : true
	 * </p>
	 *
	 * @since 8.2.0
	 */
	WALLCLOCKTIME {
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
	 * All : default max computing seconds Property name : "wallclocktimevalue"
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 21,600 s (6 hours)
	 * </p>
	 *
	 * @since 8.2.0
	 */
	WALLCLOCKTIMEVALUE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return "21600"
		 */
		@Override
		public String defaultValue() {
			return "21600";
		}
	},
	/**
	 * Dispatcher: mail server address
	 * <p>
	 * Property type : String
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
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
	 * <p>
	 * Property type : String
	 * </p>
	 * <p>
	 * Default : "smtp"
	 * </p>
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
	 * <p>
	 * Property type : String
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
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
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 200
	 * </p>
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
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 2000
	 * </p>
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "max.messages"
		 */
		@Override
		public String propertyName() {
			return "max.messages";
		}
	},
	/**
	 * Server : amount of simultaneous connection to DB
	 * <p>
	 * Property type : integer
	 * </p>
	 * <p>
	 * Default : 70
	 * </p>
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "computing.jobs"
		 */
		@Override
		public String propertyName() {
			return "computing.jobs";
		}
	},
	/**
	 * All : home directory
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: System.getenv("user.home")
	 * </p>
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
	 * <p>
	 * Property type : string
	 * </p>
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
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	DBSQLFILE {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbsqlfile"
		 */
		@Override
		public String propertyName() {
			return "XWdbsqlfile";
		}
	},
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbVendor"
		 */
		@Override
		public String propertyName() {
			return "XWdbVendor";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbEngine"
		 */
		@Override
		public String propertyName() {
			return "XWdbEngine";
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
	DBNAME {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbName"
		 */
		@Override
		public String propertyName() {
			return "XWdbName";
		}
	},
	/**
	 * Dispatcher : database server
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	DBHOST {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbHost"
		 */
		@Override
		public String propertyName() {
			return "XWdbHost";
		}
	},
	/**
	 * Dispatcher : database user
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	DBUSER {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbUser"
		 */
		@Override
		public String propertyName() {
			return "XWdbUser";
		}
	},
	/**
	 * Dispatcher : database password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	DBPASS {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbPass"
		 */
		@Override
		public String propertyName() {
			return "XWdbPass";
		}
	},
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
			return "1000";
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "XWdbRequestLimit"
		 */
		@Override
		public String propertyName() {
			return "XWdbRequestLimit";
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
	MILESTONES {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "mileStones"
		 */
		@Override
		public String propertyName() {
			return "mileStones";
		}
	},
	/**
	 * Dispatcher : comma separated list of services to manage Property type :
	 * string Default : null
	 */
	SERVICES {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "xtremweb.services"
		 */
		@Override
		public String propertyName() {
			return "xtremweb.services";
		}
	},
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "scheduler.class"
		 */
		@Override
		public String propertyName() {
			return "scheduler.class";
		}
	},
	/**
	 * All : comma separated list of known dispatchers (used for replication)
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	DISPATCHERS {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "dispatcher.servers"
		 */
		@Override
		public String propertyName() {
			return "dispatcher.servers";
		}
	},
	/**
	 * All : comma separated list of known data servers (used for replication)
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	DATASERVERS {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "data.servers"
		 */
		@Override
		public String propertyName() {
			return "data.servers";
		}
	},
	/**
	 * All : comma separated list of trusted adresses allowed to connect
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	TRUSTED {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "setTrusted"
		 */
		@Override
		public String propertyName() {
			return "setTrusted";
		}
	},
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "alive.period"
		 */
		@Override
		public String propertyName() {
			return "alive.period";
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
		 * @return "" + (Integer.parseInt(ALIVEPERIOD.defaultValue()) * 3)
		 */
		@Override
		public String defaultValue() {
			return "" + (Integer.parseInt(ALIVEPERIOD.defaultValue()) * 3);
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "alive.timeout"
		 */
		@Override
		public String propertyName() {
			return "alive.timeout";
		}
	},
	/**
	 * All : SSL key directory Property name : JAVAKEYSTORE
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: System.getProperty("javax.net.ssl.trustStore")
	 * </p>
	 */
	JAVAKEYSTORE {
		/**
		 * This retrieves the String representation of the default value
		 *
		 * @return System.getProperty(JAVAKEYSTORESTRING)
		 */
		@Override
		public String defaultValue() {
			return System.getProperty(JAVAKEYSTORESTRING);
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
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
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	SSLKEYSTORE {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "SSLKeyStore"
		 */
		@Override
		public String propertyName() {
			return "SSLKeyStore";
		}
	},
	/**
	 * Dispatcher : SSL password
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 */
	SSLKEYPASSWORD {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "SSLKeypassword"
		 */
		@Override
		public String propertyName() {
			return "SSLKeypassword";
		}
	},
	/**
	 * All : SSL pass phrase
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
	 */
	SSLKEYPASSPHRASE {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "SSLKeypassphrase"
		 */
		@Override
		public String propertyName() {
			return "SSLKeypassphrase";
		}
	},
	/**
	 * Worker : this tells if this worker is a pilot job running on an SG
	 * resource (i.e. EGEE)
	 * <p>
	 * Property type : boolean
	 * </p>
	 * <p>
	 * Default : false
	 * </p>
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
	 * <p>
	 * Property type : string
	 * </p>
	 * <p>
	 * Default : System.getenv("GLITE_WMS_JOBID")
	 * </p>
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
	 * <p>
	 * Property type : URI
	 * </p>
	 * <p>
	 * Default: null
	 * </p>
	 *
	 * @since 7.2.0
	 */
	JOBID,
	/**
	 * Worker : this is for SpeQuloS (EDGI/JRA2) If this is set, the worker runs
	 * jobs for this group of jobs only
	 * <p>
	 * Property type : URI
	 * </p>
	 * <p>
	 * Default : null
	 * </p>
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
	KEYSTOREURI {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "keystore.uri"
		 */
		@Override
		public String propertyName() {
			return "keystore.uri";
		}
	},
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
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
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
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "xtremweb.cache"
		 */
		@Override
		public String propertyName() {
			return "xtremweb.cache";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "tracer.enable"
		 */
		@Override
		public String propertyName() {
			return "tracer.enable";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "activator.class"
		 */
		@Override
		public String propertyName() {
			return "activator.class";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "activator.pool.delay"
		 */
		@Override
		public String propertyName() {
			return "activator.pool.delay";
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
	ACTIVATIONDATE {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "activator.date"
		 */
		@Override
		public String propertyName() {
			return "activator.date";
		}
	},
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
		 * This retrieves the String representation of the default value
		 *
		 * @return "false"
		 */
		@Override
		public String defaultValue() {
			return Boolean.FALSE.toString();
		}

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "activator.tcp.feedback"
		 */
		@Override
		public String propertyName() {
			return "activator.tcp.feedback";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "systemtray"
		 */
		@Override
		public String propertyName() {
			return "systemtray";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "workpool.size"
		 */
		@Override
		public String propertyName() {
			return "workpool.size";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "user.name"
		 */
		@Override
		public String propertyName() {
			return "user.name";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "java.nio"
		 */
		@Override
		public String propertyName() {
			return "java.nio";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "server.http"
		 */
		@Override
		public String propertyName() {
			return "server.http";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "server.comm.acl"
		 */
		@Override
		public String propertyName() {
			return "server.comm.acl";
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "server.stat.acl"
		 */
		@Override
		public String propertyName() {
			return "server.stat.acl";
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
	PROXYSERVER {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return "proxyname"
		 */
		@Override
		public String propertyName() {
			return "proxyname";
		}
	},
	/**
	 * Worker proxy server port
	 * <p>
	 * Default : null
	 * </p>
	 *
	 * @since 7.3.0
	 */
	PROXYPORT {
		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.PROXYPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.PROXYPORT.propertyName();
		}
	},
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.TCPPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.TCPPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.TCPSPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.TCPSPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.UDPPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.UDPPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.HTTPWORKERPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.HTTPWORKERPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.HTTPPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.HTTPPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.HTTPSPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.HTTPSPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.SMARTSOCKETSPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.SMARTSOCKETSPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.XMLRPCPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.XMLRPCPORT.propertyName();
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

		/**
		 * This retrieves a property name as found in config file This is for
		 * backward compatibility only
		 *
		 * @since 8.2.0
		 * @return Connection.SUNRPCPORT.propertyName()
		 */
		@Override
		public String propertyName() {
			return Connection.SUNRPCPORT.propertyName();
		}
	};

	/** This is the name of a Java property */
	private static final String JAVAKEYSTORESTRING = "javax.net.ssl.trustStore";
	/** This is the name of a Java property */
	public static final String JAVATMPDIR = "java.io.tmpdir";

	/**
	 * This retrieves the default value. This must be overriden if not null
	 * expected
	 *
	 * @return null
	 */
	public String defaultValue() {
		return null;
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
