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

package xtremweb.client;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import xtremweb.common.CommandLineOptions;
import xtremweb.common.CommandLineParser;
import xtremweb.common.DataInterface;
import xtremweb.common.DataTypeEnum;
import xtremweb.common.Logger;
import xtremweb.common.MileStone;
import xtremweb.common.StatusEnum;
import xtremweb.common.UID;
import xtremweb.common.WorkInterface;
import xtremweb.common.XMLObject;
import xtremweb.common.XMLValue;
import xtremweb.common.XMLVector;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWTools;
import xtremweb.communications.CommClient;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommandGetApps;
import xtremweb.communications.XMLRPCCommandRemove;
import xtremweb.communications.XMLRPCCommandSend;

/**
 * Created: May 23rd, 2014<br />
 *
 * This class implements API usage to retrieve applications list, as example
 * <br />
 * Usage: java -cp xtremweb.jar xtremweb.client.HelloWorld --xwconfig
 * xtremweb.client.conf <br />
 *
 * @author <a href="mailto:lodygens a lal.in2p3.fr">Oleg Lodygensky</a>
 * @since 9.0.0
 */

public final class HelloWorld {
	/**
	 * This is the logger
	 *
	 * @since 7.0.0
	 */
	private final Logger logger;
	/**
	 * This stores and tests the command line parameters
	 */
	private final CommandLineParser args;
	/**
	 * This stores client config such as login, password, server addr etc.
	 */
	private XWConfigurator config;

	/**
	 * This is the default constructor
	 */
	private HelloWorld(final String[] argv) throws ParseException {

		logger = new Logger(this);
		config = null;

		args = new CommandLineParser(argv);
		if (args.getOption(CommandLineOptions.HELP) != null) {
			logger.info("Usage : java HelloWorld [appUID] [inputFileName] [cmdLineParam0] [cmdLineParamN...]");
			System.exit(0);
		}
		try {
			config = (XWConfigurator) args.getOption(CommandLineOptions.CONFIG);
		} catch (final NullPointerException e) {
			logger.exception(e);
			if (args.getOption(CommandLineOptions.GUI) == null) {
				logger.fatal("You must provide a config file, using \"--xwconfig\" !");
			} else {
				new MileStone(XWTools.split(""));
			}
		}
	}

	/**
	 * This retrieves and initializes the default communication client
	 *
	 * @return the default communication client
	 * @exception IOException
	 *                is thrown if cache directory can not be created or if we
	 *                can't retrieve the default client
	 * @throws InstantiationException
	 */
	public CommClient commClient() throws IOException {
		try {
			final CommClient client = config.defaultCommClient();
			client.setLoggerLevel(logger.getLoggerLevel());
			client.setAutoClose(false);
			return client;
		} catch (final Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * This prints a message to std err and exits.
	 */
	private void exit() {

		try {
			final CommClient client = commClient();
			client.setAutoClose(true);
			client.close();
		} catch (final Exception e) {
			logger.exception(e);
		}
	}

	/**
	 * This first retrieves registered applications; then submit a job for the
	 * 1st found application.
	 */
	private void execute() throws IOException {

		try {
			final CommClient client = commClient();
			final URI uri = client.newURI();
			//
			// retrieve register applications
			//
			final XMLRPCCommandGetApps cmd = new XMLRPCCommandGetApps(uri, config.getUser());
			final XMLVector xmluids = (XMLVector) cmd.exec(client);
			final Collection<XMLValue> uids = xmluids.getXmlValues();
			if ((uids == null) || (uids.isEmpty())) {
				logger.warn("no application found");
				return;
			}

			final Iterator<XMLValue> theEnum = uids.iterator();

			UID appUid = null;

			final List commandLineParams = (List) args.commandParams();
			if ((commandLineParams == null) || (commandLineParams.isEmpty())) {
				if (theEnum.hasNext()) {
					appUid = (UID) ((XMLObject) theEnum.next()).getValue();
				}
			} else {
				try {
					appUid = ((URI) commandLineParams.get(0)).getUID();
				} catch (final Exception e) {
					try {
						appUid = (UID) commandLineParams.get(0);
					} catch (final Exception e2) {
					}
				}
			}
			if (appUid == null) {
				logger.fatal("Can't retrieve application");
			}
			//
			// submit new work
			//
			WorkInterface work = new WorkInterface();
			work.setUID(new UID());
			work.setApplication(appUid);

			//
			// if there any input file?
			// here, as example, let suppose input file is name 'input.zip'
			// but of course, we could programmatically create a Zip file here,
			// if for example, 'input' is a directory
			//
			String inputFileName = "input.zip";
			if ((commandLineParams != null) && (commandLineParams.size() > 1)) {
				inputFileName = (String) commandLineParams.get(1);
			}
			final File dataFile = inputFileName == null ? null : new File(inputFileName);
			if ((dataFile != null) && (dataFile.exists())) {
				final DataInterface data = new DataInterface(new UID());
				final URI dataUri = commClient().newURI(data.getUID());
				data.setStatus(StatusEnum.UNAVAILABLE);
				data.setSize(dataFile.length());
				data.setName(inputFileName);
				data.setShasum(XWTools.sha256CheckSum(dataFile));
				final DataTypeEnum inputType = DataTypeEnum.getFileType(dataFile);
				if (inputType != null) {
					data.setType(inputType);
				}
				//
				// 1st, send data definition
				//
				logger.info("Sending data  '" + inputFileName + "' : " + data.toXml());
				final XMLRPCCommandSend cmdSend = XMLRPCCommandSend.newCommand(dataUri, data);
				cmdSend.exec(client);
				//
				// then send data content
				//
				logger.info("Uploading data  '" + inputFileName + "'");
				commClient().uploadData(dataUri, dataFile);
				logger.info("Uploaded  data  '" + inputFileName + "'");

				work.setDirin(dataUri);
			} else {
				logger.info("File not found '" + inputFileName + "'");
			}

			final StringBuilder cmdLineStr = new StringBuilder(" ");
			for (int i = 1; (commandLineParams != null) && (i < commandLineParams.size()); i++) {
				cmdLineStr.append(commandLineParams.get(i).toString() + " ");
			}

			if (cmdLineStr.indexOf(XWTools.QUOTE) != -1) {
				throw new ParseException("6 dec 2005 : command line cannot have \"" + XWTools.QUOTE
						+ "\" character until further notification", 0);
			}
			work.setCmdLine(cmdLineStr.toString());

			logger.info("Submitting a new work for application '" + appUid + "' : " + work.toXml());

			final XMLRPCCommandSend cmdSend = XMLRPCCommandSend.newCommand(uri, work);
			cmdSend.exec(client);
			logger.info("Submitted " + work.getUID());

			//
			// wait for job completion
			//
			work = commClient().waitForCompletedWork(work.getUID());

			if (work.getResult() != null) {
				logger.info("Downloading results");
				final DataInterface result = (DataInterface) commClient().get(work.getResult());
				final DataTypeEnum resultType = result.getType();
				final String resultFileName = "myresult" + (resultType == null ? "" : "." + resultType.fileExtension());
				final File fileResult = new File(resultFileName);
				client.downloadData(work.getResult(), fileResult);
			}

			logger.info("Removing work from server");
			final URI workURI = client.newURI(work.getUID());
			final XMLRPCCommandRemove cmdRemove = new XMLRPCCommandRemove(workURI);
			cmdRemove.exec(client);

			logger.info("Disconnecting");
			client.disconnect();
		} catch (final Exception e) {
			logger.exception(e);
			exit();
		}
	}

	/**
	 * This is the standard main method
	 * 
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void main(final String[] argv) throws IOException, ParseException {
		new HelloWorld(argv).execute();
	}
}
