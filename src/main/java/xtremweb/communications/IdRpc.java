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

package xtremweb.communications;

import java.io.IOException;
import java.util.Hashtable;

import xtremweb.common.HostInterface;
import xtremweb.common.Table;
import xtremweb.common.UserInterface;

/**
 * This defines command that can be sent to the server <br />
 *
 * Created: Thu May 31 10:03:43 2001 <br />
 *
 * This is used to create communication messages.<br />
 * This is also used as client command line argument.
 *
 * @author <a href="mailto: lodygens a t lal - in2p3 - fr">Oleg Lodygensky</a>
 */

public enum IdRpc {
	/**
	 * This is the null command
	 */
	NULL {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : does nothing";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : does nothing";
		}
	},
	/**
	 * This disconnects from the platform and cleans server side
	 */
	DISCONNECT {
		@Override
		public XMLRPCCommandDisconnect newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandDisconnect(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : disconnects";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : disconnects";
		}
	},
	/**
	 * This retrieves server version
	 */
	VERSION {
		@Override
		public XMLRPCCommandVersion newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandVersion(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves server version ";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : retrieves server version ";
		}
	},
	/**
	 * This retrieves an object from server, except tasks
	 */
	GET {
		@Override
		public XMLRPCCommandGet newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGet(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " UID | URI : retrieves an object";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves an object";
		}
	},
	/**
	 * This retrieves a task from server from its UID **or** its WORKUID
	 */
	GETTASK {
		@Override
		public XMLRPCCommandGetTask newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetTask(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " UID | URI : retrieves a task";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves a task";
		}
	},
	/**
	 * This sends an object to server
	 */
	SEND {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : insert/update an object";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends an object";
		}
	},
	/**
	 * This sends an application to server
	 */
	SENDAPP {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString()
					+ " appName appType cpuType osName URI | UID : inserts/updates an application; URI or UID points to binary file ; application name must be the first parameter";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends an application";
		}
	},
	/**
	 * This retrieves all application UID from server
	 */
	GETAPPS {
		@Override
		public XMLRPCCommandGetApps newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetApps(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " retrieves all applications";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : retrieves all applications";
		}
	},
	/**
	 * This removes an object from server; removing a work/task removes the
	 * associated task/work
	 */
	REMOVE {
		@Override
		public XMLRPCCommandRemove newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandRemove(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " URI | UID  [ URI | UID...] : removes objects ";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : removes an object";
		}
	},
	/**
	 * This sends a group to server
	 */
	SENDGROUP {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " groupName [sessionUID] : sends/updates a group";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends a group";
		}
	},
	/**
	 * This retrieves all groups UID from server
	 */
	GETGROUPS {
		@Override
		public XMLRPCCommandGetGroups newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetGroups(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves all groups";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves all groups";
		}
	},
	/**
	 * This retrieves all group works UID from server
	 */
	GETGROUPWORKS {
		@Override
		public XMLRPCCommandGetGroupWorks newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetGroupWorks(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " UID | URI : retrieves all group works";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves all group works";
		}
	},
	/**
	 * This sends a host to server
	 */
	SENDHOST {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This retrieves all hosts UID from server
	 */
	GETHOSTS {
		@Override
		public XMLRPCCommandGetHosts newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetHosts(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrevies all hosts";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/" + this.toString() + " : retrieves all hosts";
		}
	},
	/**
	 * This activates/deactivates a host
	 */
	ACTIVATEHOST {
		@Override
		public XMLRPCCommandActivateHost newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandActivateHost(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " [on|off] hostUID [hostUID...] : activates/deactivates hosts";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID?" + XWPostParams.PARAMETER
					+ "=true|false : activates/deactivates a host";
		}
	},
	SETWORKERSPARAMETERS {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	GETWORKERSPARAMETERS {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	SETWORKERSNB {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This sends a session to server
	 */
	SENDSESSION {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " sessionName clientUID : sends/updates a session";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends a session";
		}
	},
	/**
	 * This retrieves session UID from server
	 */
	GETSESSIONS {
		@Override
		public XMLRPCCommandGetSessions newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetSessions(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves all sessions";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves all sessions";
		}
	},
	/**
	 * This retrieves all session works UID from server
	 */
	GETSESSIONWORKS {
		@Override
		public XMLRPCCommandGetSessionWorks newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetSessionWorks(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves all session works";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves all session works";
		}
	},
	/**
	 * This retrieves all tasks UID from server
	 */
	GETTASKS {
		@Override
		public XMLRPCCommandGetTasks newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetTasks(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves all tasks";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : retrieves all tasks";
		}
	},
	/**
	 * This sends a trace to server
	 */
	SENDTRACE {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This retrieves all traces UID from server
	 */
	GETTRACES {
		@Override
		public XMLRPCCommandGetTraces newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetTraces(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This sends an user group to server
	 */
	SENDUSERGROUP {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString()
					+ " usergroupName groupAdminLogin groupAdminPassword groupAdminEmail : sends/updates an user group";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends a user group";
		}
	},
	/**
	 * This retrieves all user groups UID from server
	 */
	GETUSERGROUPS {
		@Override
		public XMLRPCCommandGetUserGroups newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetUserGroups(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves all user groups";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID : retrieves all user groups";
		}
	},
	/**
	 * This sends an user to server
	 */
	SENDUSER {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " login password email rights [<a user group UID | URI> ] : sends/updates a user";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description :  send an user";
		}
	},
	/**
	 * This retrieves an user by its login
	 */
	GETUSERBYLOGIN {
		@Override
		public XMLRPCCommandGetUserByLogin newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetUserByLogin(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " userLogin [userLogin...] : retrevies users given their login";
		}

		@Override
		public String helpRestApi() {
			return this.toString() + "/aLogin";
		}
	},
	/**
	 * This retrieves an application by its name
	 * @since 12.2.9
	 */
	GETAPPBYNAME {
		@Override
		public XMLRPCCommandGetAppByName newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetAppByName(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " appName [appName...] : retrieves applications by name";
		}

		@Override
		public String helpRestApi() {
			return this.toString() + "/appName";
		}
	},
	/**
	 * This retrieves all users UID from server
	 */
	GETUSERS {
		@Override
		public XMLRPCCommandGetUsers newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetUsers(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrevies all users";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : retrives all users";
		}
	},
	/**
	 * This sends a work to server
	 */
	SENDWORK {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString()
					+ " <application name | URI | UID> [ --xwcert <X.509 cert or proxy> ] [ --xwenv <dirinFile | URI | UID> ] [ --xwstdin <stdinFile> ] [--xwsession <UID | URI>] [--xwgroup <UID | URI>]";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends a work";
		}
	},
	/**
	 * This sends an envelope to server
	 * @since 13.0.0
	 */
	SENDENVELOPE {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString()
					+ " <id> [name] [maxcpuspeed] [maxwallclocktime] [maxmemory] [maxstorage] [maxfilesize]";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends an envelope";
		}
	},
	/**
	 * This broadcasts a work to all workers
	 */
	BROADCASTWORK {
		@Override
		public XMLRPCCommandBroadcastWork newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandBroadcastWork(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " (same parameters as SENDWORK) : broadcast a work to all hosts";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC
					+ "=an xml description : send a work to all hosts";
		}
	},
	/**
	 * This requests a work to compute from server
	 */
	WORKREQUEST {
		@Override
		public XMLRPCCommandWorkRequest newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandWorkRequest(uri, null);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : sends the work alive signal (for debugging purposes only)";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : sends the work alive signal (for debugging purposes only)";
		}
	},
    /**
     * This retrieves all works UID from server
     */
    GETWORKS {
        @Override
        public XMLRPCCommandGetWorks newCommand(final URI uri, final UserInterface client, final Table obj)
                throws IOException {
            return new XMLRPCCommandGetWorks(uri, client);
        }

        @Override
        public String helpClient() {
            return this.toString() + " : retrieves all works";
        }

        @Override
        public String helpRestApi() {
            return "/" + this.toString() + ": retrieves all works";
        }
    },
    /**
     * This retrieves a work by its external ID (another scheduler, blockchain transaction hash etc.)
     * @since 11.1.0
     */
    GETWORKBYEXTERNALID {
        @Override
        public XMLRPCCommandGetWorkByExternalId newCommand(final URI uri, final UserInterface client, final Table obj)
                throws IOException {
            return new XMLRPCCommandGetWorkByExternalId(uri, client);
        }

        @Override
        public String helpClient() {
            return this.toString() + " aString [...] : retrevies work given their id";
        }

        @Override
        public String helpRestApi() {
            return this.toString() + "/anId";
        }
    },
    /**
     * This retrieves all envelopes UID from server
     * @since 13.0.0
     */
    GETENVELOPES {
        @Override
        public XMLRPCCommandGetEnvelopes newCommand(final URI uri, final UserInterface client, final Table obj)
                throws IOException {
            return new XMLRPCCommandGetEnvelopes(uri, client);
        }

        @Override
        public String helpClient() {
            return this.toString() + " : retrieves all envelopes";
        }

        @Override
        public String helpRestApi() {
            return "/" + this.toString() + ": retrieves all envelopes";
        }
    },
    /**
     * This retrieves an envelope by its ID
     * @since 13.1.0
     */
    GETENVELOPEBYID {
        @Override
        public XMLRPCCommandGetEnvelopeById newCommand(final URI uri, final UserInterface client, final Table obj)
                throws IOException {
            return new XMLRPCCommandGetEnvelopeById(uri, client);
        }

        @Override
        public String helpClient() {
            return this.toString() + " anInteger [...] : retrieves envelopes given their id";
        }

        @Override
        public String helpRestApi() {
            return this.toString() + "/anInteger";
        }
    },
	/**
	 * This send a work status to server
	 */
	WORKALIVEBYUID {
		@Override
		public XMLRPCCommandWorkAliveByUID newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandWorkAliveByUID(uri, client, (HostInterface) obj);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This sends a worker status to server
	 */
	WORKALIVE {
		@Override
		public XMLRPCCommandWorkAlive newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandWorkAlive(uri, client, (HostInterface) obj, (Hashtable) null);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : sends the alive signal (for debugging purposes only)";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : sends the alive signal (for debugging purposes only)";
		}
	},
	/**
	 * This pings the server
	 */
	PING {
		@Override
		public XMLRPCCommandPing newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandPing(uri);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : pings the server";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : pings the server";
		}
	},
	/**
	 * These are not used
	 */
	TACTIVITYMONITOR {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	GETTRUSTEDADDRESSES {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	ADDTRUSTEDADDRESS {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	REMOVETRUSTEDADDRESS {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	TRACEWORKERS {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This mounts an NFS volume (this is very experimental, don't do that ;) )
	 */
	MOUNT {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This unmounts an NFS volume (this is very experimental, don't do that ;)
	 * )
	 */
	UMOUNT {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This is sent by worker when it shuts down (if it has time to do so)
	 */
	SHUTDOWN {
		@Override
		public XMLRPCCommand newCommand(final URI uri, final UserInterface client, final Table obj) throws IOException {
			return null;
		}

		@Override
		public String helpClient() {
			return this.toString() + " : not available";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : not available";
		}
	},
	/**
	 * This sends a data to server
	 */
	SENDDATA {
		@Override
		public XMLRPCCommandSend newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return newXMLRPCCommandSend(uri, client, obj);
		}

		@Override
		public String helpClient() {
			return this.toString()
					+ " dataName [cpuType] [osName] [dataType] [accessRigths] [dataFile | dataURI | dataUID] : sends data and uploads data if dataFile provided";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "?" + XWPostParams.XMLDESC + "=an xml description : sends a data";
		}
	},
	/**
	 * This retrieves all data UID from server
	 */
	GETDATAS {
		@Override
		public XMLRPCCommandGetDatas newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetDatas(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves all data";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : retrieves all data";
		}
	},
	/**
	 * This changes access rights of an object
	 */
	CHMOD {
		@Override
		public XMLRPCCommandChmod newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandChmod(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " newAccessRights URI | UID [ URI | UID...] : changes access rights";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID?" + XWPostParams.PARAMETER + "=chmod_octal : change access rights";
		}
	},
	/**
	 * This retrieves the SmartSockets hub address
	 *
	 * @since 8.0.0
	 */
	GETHUBADDR {
		@Override
		public XMLRPCCommandGetHubAddr newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandGetHubAddr(uri);
		}

		@Override
		public String helpClient() {
			return this.toString() + " : retrieves the SmartSockets hub address";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + " : retrieves the SmartSockets hub address";
		}
	},
	/**
	 * This uploads a data content to server
	 */
	UPLOADDATA {
		@Override
		public XMLRPCCommandUploadData newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandUploadData(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " UID file : sends data content from file to server";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString();
		}
	},
	/**
	 * This downloads a data content from server
	 */
	DOWNLOADDATA {
		@Override
		public XMLRPCCommandDownloadData newCommand(final URI uri, final UserInterface client, final Table obj)
				throws IOException {
			return new XMLRPCCommandDownloadData(uri, client);
		}

		@Override
		public String helpClient() {
			return this.toString() + " UID | URI : downloads data content from server";
		}

		@Override
		public String helpRestApi() {
			return "/" + this.toString() + "/anUID";
		}
	};

	public static final IdRpc LAST = DOWNLOADDATA;
	public static final int SIZE = LAST.ordinal() + 1;

	/**
	 * @since 11.5.0
	 */
	private final static XMLRPCCommandSend newXMLRPCCommandSend(final URI uri, final UserInterface client, final Table obj) throws IOException {
		final XMLRPCCommandSend cmd = XMLRPCCommandSend.newCommand(uri, obj);
		cmd.setUser(client);
		return cmd;
	}
	
	/**
	 * This creates a new XMLRPCCommand
	 *
	 * @param uri
	 *            is the command URI
	 * @param client
	 *            describes the client making this RPC call
	 * @param obj
	 *            is an optional command parameter
	 * @since 8.2.2
	 */
	public abstract XMLRPCCommand newCommand(URI uri, UserInterface client, Table obj) throws IOException;

	/**
	 * This retrieves help for the command line client
	 *
	 * @since 8.2.3
	 * @return a string containing the help
	 */
	public abstract String helpClient();

	/**
	 * This retrieves help for the REST interface
	 *
	 * @since 8.2.3
	 * @return a string containing the help
	 */
	public abstract String helpRestApi();

	public String toXml() {
		return toXml(false);
	}

	public String toXml(final boolean close) {
		return "<" + (close == true ? "/" : "") + this.toString().toLowerCase() + ">";
	}

	/**
	 * This retrieves an IdRpc from its integer value
	 *
	 * @param v
	 *            is the integer value of the IdRpc
	 * @return an IdRpc
	 */
	public static IdRpc fromInt(final int v) throws IndexOutOfBoundsException {
		for (final IdRpc i : IdRpc.values()) {
			if (i.ordinal() == v) {
				return i;
			}
		}
		throw new IndexOutOfBoundsException("unvalid IdRpc value " + v);
	}

	/**
	 * This dumps enums to stdout
	 */
	public static void main(final String[] argv) {
		for (final IdRpc i : IdRpc.values()) {
			System.out.println(i.helpClient());
		}
	}
}
