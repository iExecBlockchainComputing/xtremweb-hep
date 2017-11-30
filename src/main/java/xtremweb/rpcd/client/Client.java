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

package xtremweb.rpcd.client;

/**
 * Date    : Mar 25th, 2005
 * Project : RPCXW / Client
 * File    : Client.java
 *
 * @author <a href="mailto:lodygens_a_lal.in2p3.fr">Oleg Lodygensky</a>
 * @version
 */

import org.apache.log4j.Logger;

import xtremweb.common.XWConfigurator;
import xtremweb.common.Zipper;
import xtremweb.communications.CommClient;

/**
 * This class instanciates an RPC UDP proxy<br />
 * This is the Client part of RPCXW
 */
public class Client {

	private static final Logger logger = Logger.getLogger(Client.class);

	public static final int ERROK = 0;
	public static final int ERRCONNECTION = 1;
	public static final int ERRDISK = 2;
	public static final int ERRUID = 3;
	public static final int ERR = 4;

	/**
	 * RMI communication channel
	 */
	private CommClient comm;
	/**
	 * The zipper/unzipper
	 */
	private final Zipper zipper;

	/**
	 * This stores client config such as login, password, server addr etc.
	 */
	private XWConfigurator config;
	/**
	 * This tells whether the user wants a daemon or a client for discovering
	 * worker exported directories If true, the daemon is not started, but the
	 * client This is set to true if "--discover" is found on command line
	 */
	private boolean discovering;

	/**
	 * This is the default constructor
	 */
	public Client(final String argv[]) {
		discovering = false;
		parse(argv);
		zipper = new Zipper();
	}

	/**
	 * This parses command line arguments
	 */
	private void parse(final String argv[]) {

		int i = 0;

		while (i < argv.length) {

			if (argv[i].toLowerCase().compareTo("--xwconfig") == 0) {
				config = new XWConfigurator(argv[++i], false);
			} else if (argv[i].toLowerCase().compareTo("--discover") == 0) {
				discovering = true;
			}

			i++;
		}
		if (config == null) {
			config = new XWConfigurator(null, false);
		}
	}

	/**
	 * This retrieves worker exported directories This is done by broadcasting a
	 * "cat /etc/exports" to workers
	 */
	private void discover() {

		System.out.println("Client::discover not implemented");

		/*
		 *
		 * try { comm =
		 * (CommClient)Class.forName(config.getProperty(XWPropertyDefs
		 * .COMMLAYER)).newInstance(); comm.setConfig(config);
		 * comm.initComm(config.getCurrentServer()); } catch(Exception e) {
		 * util.fatal("Can't init comm :  " + e); }
		 *
		 * if(comm == null) util.fatal("Can't init comm");
		 *
		 * UID jobUID = new UID(); MobileWork job = new MobileWork(jobUID,
		 * logger.getEffectiveLevel());
		 *
		 * String appName = "cat"; String appParams = "/etc/exports";
		 *
		 * // job.setServer(config.getCurrentServer()); //
		 * job.setApplicationName(appName); job.setCmdLine(appParams);
		 *
		 * String ret = null; Vector broadcasted = new Vector(); Vector
		 * completed = new Vector();
		 *
		 * try { broadcasted = comm.broadcast(job); } catch(Exception ce) {
		 * ce.printStackTrace(); System.exit(ERRCONNECTION); }
		 *
		 * while(broadcasted.size() > completed.size()) {
		 *
		 * Iterator li = broadcasted.iterator();
		 *
		 * while(li.hasNext()) {
		 *
		 * UID uid =(UID)li.next(); MobileResult result = null; TaskInterface
		 * task = null;
		 *
		 * if(completed.contains(uid)) { logger.debug(uid + " already completed"
		 * ); continue; }
		 *
		 * try { result = comm.getResult(uid); task = comm.getTask(uid); }
		 * catch(Exception ce) { logger.debug(ce.toString());
		 * System.exit(ERRCONNECTION); }
		 *
		 * if(result == null) { logger.debug(uid + " not COMPLETED yet");
		 * continue; }
		 *
		 * if(task == null) { logger.error(uid + " task is null ???"); continue;
		 * }
		 *
		 * try { String dirname = task.getHost() +"_exports"; String filename =
		 * dirname + File.separator + "catexports.zip"; boolean resultRetreived
		 * = true; File output = new File(dirname);
		 *
		 * output.mkdirs();
		 *
		 * logger.debug("saving " + filename);
		 *
		 * try { output = new File(filename); FileOutputStream fos = new
		 * FileOutputStream(output); fos.write(result.getResult()); fos.close();
		 * } catch(Exception e) { logger.error(e.toString());
		 * System.exit(ERRDISK); }
		 *
		 * zipper.setFileName(filename); if(zipper.unzip(dirname) == true) new
		 * File(filename).delete();
		 *
		 * try { BufferedReader inbuf = new BufferedReader(new FileReader(new
		 * File(dirname, util.STDOUT)));
		 *
		 * for(String line = inbuf.readLine(); line != null; line =
		 * inbuf.readLine()) {
		 *
		 * line = line.trim(); if(line.startsWith("/")) System.out.println(
		 * "XWNFS : " + task.getHost() + " is exporting " + line); }
		 *
		 * if(logger.getEffectiveLevel() != Level.DEBUG) util.deleteDir(logger,
		 * new File(dirname)); } catch(Exception e) { logger.error(e); }
		 * logger.debug(uid + " will be deleted"); completed.add(uid); }
		 * catch(Exception e) { logger.error("can't retreive result for " + uid
		 * + "(" + e.toString() + ")"); System.exit(ERRDISK); } }
		 *
		 * logger.debug("" + broadcasted.size() + " ; " + completed.size());
		 *
		 * try { Thread.sleep(2500); } catch(InterruptedException e){ } }
		 *
		 * try { comm.removeWorks(completed); } catch(RemoteException ce) {
		 * logger.error(ce.toString()); System.exit(ERRCONNECTION); }
		 *
		 *
		 * try { comm.disconnect(); } catch(Exception ce) {
		 * logger.error(ce.toString()); System.exit(ERRCONNECTION); }
		 */
	}

	/**
	 * This is the standard main function; this parses args
	 */
	public static void main(final String[] argv) {

		final Client c = new Client(argv);

		if (c.discovering == true) {
			c.discover();
			System.exit(Client.ERROK);
		}

		c.config.dump(System.out, "XtremWeb SunRPC client started ");
		new rpcudp(argv, c.config).start();
	}

}// proxy
