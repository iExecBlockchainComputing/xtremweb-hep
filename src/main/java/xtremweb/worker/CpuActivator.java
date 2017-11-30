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
 *  File   : CpuActivator.java
 *  Date   : June 15th, 2004.
 *  Author : Oleg Lodygensky (lodygens /at\ lal.in2p3.fr)
 */

import xtremweb.archdep.ArchDepFactory;
import xtremweb.archdep.XWUtil;
import xtremweb.common.XWConfigurator;
import org.apache.log4j.Logger;


/**
 * <CODE>CpuActivator</CODE> monitors CPU activity
 *
 * @author Oleg Lodygensky
 *
 */

public class CpuActivator extends PollingActivator {

	private static final Logger logger = Logger.getLogger(CpuActivator.class);
	private int remains = 0;
	private XWUtil irq = null;

	/** This is for debug purposes only */
	private boolean lastState;

	/**
	 * This is the default contructor This initializes IRQ counter
	 */
	public CpuActivator() {

		irq = ArchDepFactory.xwutil();

		irq.getCpuLoad();
		irq.getProcessLoad();

		lastState = false;
	}

	/**
	 * This initializes the activator accordingly to the config file
	 *
	 * @param config
	 *            is the Properties read from file
	 */
	@Override
	public void initialize(final XWConfigurator config) {

		super.initialize(config);

		remains = getActivationDelay();
		raz();
	}

	/**
	 * This resets all
	 *
	 * @see xtremweb.archdep.XWUtilLinux#raz()
	 */
	@Override
	public void raz() {
		irq.raz();
	}

	/**
	 * This detect this host activity
	 *
	 * @see xtremweb.common.XWConfigurator#cpuLoad
	 * @return true if the machine is loaded and XWWorker should top computing
	 */
	private boolean isActive() {

		boolean ret = true;
		int cpuLoad = irq.getCpuLoad();
		int processLoad = irq.getProcessLoad();
		int i = 0;

		for (i = 0; ((cpuLoad < 0) || (cpuLoad > 100) || (processLoad < 0) || (processLoad > 100));) {

			try {
				Thread.sleep(1);
			} catch (final Exception e) {
			}

			cpuLoad = irq.getCpuLoad();
			processLoad = irq.getProcessLoad();

			if (++i > 50) {
				break;
			}
		}

		final int others = Math.abs(cpuLoad - processLoad);

		if (Worker.getConfig().cpuLoad() > others) {
			ret = false;
		}

		if (ret != lastState) {
			logger.debug(
					"  %CPU(host) " + cpuLoad + "  %CPU(worker) = " + processLoad + "  %CPU(others) = " + others);

			logger.debug("Cpu " + (ret == false ? "not" : "") + " available : " + Worker.getConfig().cpuLoad()
					+ " > " + others);

			lastState = ret;
		}

		System.gc();

		return ret;
	}

	/**
	 * This tells whether the worker can start computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work can start computing
	 */
	@Override
	protected boolean canStart() {

		if (this.isActive()) {
			remains = getActivationDelay();
		} else {
			remains -= getWaitingProbeInterval();
		}
		logger.info("activation in " + remains + " ms");

		return (remains <= 0);

	}

	/**
	 * This tells whether the worker must stop computing, accordingly to the
	 * local activation policy
	 *
	 * @return true if the work must stop computing
	 */
	@Override
	protected boolean mustStop() {

		boolean ret = isActive();
		int i;

		// let try 3 times to ensure this is not due to a peack only...

		for (i = 0; (i < 3) && ret; i++) {

			try {
				Thread.sleep(500);
			} catch (final Exception e) {
			}

			ret = isActive();
		}

		if (ret) {
			remains = getActivationDelay();
		}
		return ret;
	}

}
