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

package xtremweb.dispatcher;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import com.iexec.common.ethereum.TransactionStatus;
import com.iexec.common.ethereum.Utils;
import com.iexec.common.model.ContributionStatusEnum;
import com.iexec.scheduler.actuator.ActuatorService;
import xtremweb.common.*;
import xtremweb.communications.URI;

/**
 * HashTaskSet.java
 *
 * Created: Wed Aug 23 14:17:05 2000
 *
 * @author Gilles Fedak
 * @version %I% %G%
 */

public class HashTaskSet extends TaskSet {

	protected HashTaskSet() {
		super();
	}

	/**
	 * This retrieves WAITING jobs and set status to PENDING This retrieves
	 * associated tasks, if any, and set their status to ERROR
	 */
	@Override
	protected void refill() {
		final DBInterface db = DBInterface.getInstance();
		final Date now = new Date();
		try {
			final Collection<WorkInterface> works = Dispatcher.getScheduler().retrieve();
			if (works == null) {
				getLogger().debug("refill works is null");
				return;
			}

			getLogger().debug("refill size = " + works.size());

			for (final WorkInterface theWork  : works) {

				try {
					getLogger().debug("refill = " + theWork.getUID());

					theWork.setPending();

					final Collection<TaskInterface> tasks = db.tasks(theWork);
					if (tasks != null) {
						for (final TaskInterface theTask : tasks) {
							theTask.setError();
							theTask.setRemovalDate(now);
							theTask.update();
						}
					}

					theWork.update();
				} catch (final Exception e) {
					getLogger().exception(e);
				}
			}
		} catch (final Exception e) {
			getLogger().exception(e);
		}
	}

	/**
	 * This checks if given task is lost. A task is lost if (status == RUNNIG)
	 * || (status == DATAREQUEST) || (status == RESULTREQUEST) and if alive
	 * signal not received after 3 alive periods. If lost, the task is set to
	 * ERROR and a new PENDING task is created.
	 */
	private void detectAbortedTask(final TaskInterface theTask) {

		final DBInterface db = DBInterface.getInstance();
		final Date now = new Date();
		try {
			if ((theTask == null) || (theTask.getLastAlive() == null)) {
				return;
			}

			final long delay = System.currentTimeMillis() - theTask.getLastAlive().getTime();

			long aliveTimeOut = Long
					.parseLong(Dispatcher.getConfig().getProperty(XWPropertyDefs.ALIVETIMEOUT.toString()));
			aliveTimeOut *= 1000;

            getLogger().debug("detectAbortedTask : delay = " + delay + "; aliveTimeOut = " + aliveTimeOut);
            getLogger().debug("detectAbortedTask : " + theTask.toXml());

			if (theTask.isUnderProcess() && (delay > aliveTimeOut)) {

				final WorkInterface theWork = db.work(theTask.getWork());
				if (theWork == null) {
					getLogger().warn("No work found for task ; deleting " + theTask.getUID());
					theTask.delete();
					return;
				}

				theWork.lost(XWTools.getLocalHostName());
				theWork.setErrorMsg("rescheduled : worker lost");
				theTask.setError();
                theTask.setErrorMsg("worker lost");
				if(theWork.getMarketOrderUid() != null) {
					theWork.setExpectedHost(null);
				}
				theTask.setRemovalDate(now);

				final UID hostUID = theTask.getHost();
				if (hostUID != null) {
					final HostInterface theHost = db.host(hostUID);
					if (theHost != null) {
                        theHost.decRunningJobs();
                        theHost.incErrorJobs();
						final MarketOrderInterface marketOrder = DBInterface.getInstance().marketOrder(theHost.getMarketOrderUid());
                        theHost.leaveMarketOrder(marketOrder);
						theHost.update();
						if(marketOrder != null) {
//                            marketOrder.setErrorMsg("WARN:workerLost");
                            marketOrder.update();
                        }
					}
				}
				final UID ownerUID = theWork.getOwner();
				if (ownerUID != null) {
					final UserInterface theUser = db.user(ownerUID);
					if (theUser != null) {
						theUser.decRunningJobs();
						theUser.update();
					}
				}
				final UID appUID = theWork.getApplication();
				if (appUID != null) {
					final AppInterface theApp = db.app(appUID);
					if (theApp != null) {
						theApp.decRunningJobs();
						theApp.update();
					}
				}

				getLogger().debug("detectAbortedTask aborted :     " + theTask.toXml());

				theWork.update();
				theTask.update();
			}
		} catch (final Exception e) {
			getLogger().exception("detecAbortedTasks_unitary : can't set tasks lost", e);
		}
	}

	/**
	 * This are the status we must monitor to detect lost tasks
	 *
	 * @since 8.2.0
	 */
	private enum abortedStatus {
		RUNNING(StatusEnum.RUNNING),
        DATAREQUEST(StatusEnum.DATAREQUEST),
        REVEALING(StatusEnum.REVEALING),
        CONTRIBUTING(StatusEnum.CONTRIBUTING),
        CONTRIBUTED(StatusEnum.CONTRIBUTED),
        RESULTREQUEST(StatusEnum.RESULTREQUEST);

		private final StatusEnum status;

		abortedStatus(final StatusEnum s) {
			status = s;
		}

		public StatusEnum getStatus() {
			return status;
		}
	};

	/**
	 * This retrieves RUNNING or DATAREQUEST or RESULTREQUEST tasks and calls
	 * detectAbortedTask(Task) for each
	 *
	 * @see #detectAbortedTask(TaskInterface)
	 */
	@Override
	protected void detectAbortedTasks() {
		final DBInterface db = DBInterface.getInstance();
		for (final abortedStatus s : abortedStatus.values()) {

			try {
				final Collection<TaskInterface> tasks = db.tasks(s.getStatus());
				getLogger().debug("detectAbortedTasks " + s + " = " + (tasks == null ? "null" : tasks.size()));
				if (tasks != null) {
					for (final TaskInterface theTask : tasks) {
						detectAbortedTask(theTask);
					}
				}
			} catch (final Exception e) {
				getLogger().exception(e);
			}
		}


        getLogger().debug("detectAbortedTasks : checking PENDING jobs with lost resource");
        try {
            final Collection<WorkInterface> works = db.marketOrderLostPendingWorks();
            if ((works != null) && (works.size() > 0)) {

                getLogger().debug("detectAbortedTasks : market orders lost pending works " + works.size());

                for (final WorkInterface work : works) {
                    getLogger().debug("detectAbortedTasks market orders lost pending works " + work.toXml());

                    final HostInterface expectedHost = db.host(work.getExpectedHost());
                    if(expectedHost != null) {
                        getLogger().debug("detectAbortedTasks market orders lost pending works " + expectedHost.toXml());
                        final MarketOrderInterface marketOrder = db.marketOrder(expectedHost.getMarketOrderUid());
                        if(marketOrder != null) {
                            marketOrder.removeWorker(expectedHost);
                            marketOrder.update();
                        }
                        else {
                            expectedHost.leaveMarketOrder();
                        }
                        expectedHost.decPendingJobs();
                        expectedHost.update();
                    }
                    work.setExpectedHost(null);
                    work.update();
                }
            }
            else {
                getLogger().debug("detectAbortedTasks : no market with lost pending work");
            }

        }
        catch (Exception e) {
            getLogger().exception(e);
        }

        getLogger().debug("detectAbortedTasks : checking computing resource dead locks between market orders");
        try {
            final Collection<MarketOrderInterface> marketOrders = db.marketOrderLockingResources();
            if ((marketOrders != null) && (marketOrders.size() > 0)) {

                getLogger().debug("detectAbortedTasks : market orders locking resources " + marketOrders.size());

                for (final MarketOrderInterface marketOrder : marketOrders) {
                    getLogger().debug("detectAbortedTasks market orders locking resources " + marketOrder.toXml());

                    final Collection<HostInterface> workers = db.hosts(marketOrder);
                    getLogger().debug("detectAbortedTasks market orders locking resources has " + workers.size());

                    if (workers != null) {
                        for (HostInterface worker : workers) {

                            getLogger().debug("detectAbortedTasks market order unlocks resource " + worker.toXml());
                            worker.leaveMarketOrder(marketOrder);
                            worker.update();
                        }
                        marketOrder.update();
                    }
                }
            }
            else {
                getLogger().debug("detectAbortedTasks : no market order locking resource");
            }

        }
        catch (Exception e) {
            getLogger().exception(e);
        }

        checkContributingMarketOrders();
        checkRevealingOrFinalizingMarketOrders();

	}

    private void checkRevealingOrFinalizingMarketOrders(){
        final DBInterface db = DBInterface.getInstance();
        getLogger().debug("detectAbortedTasks : checking revealingOrFinalizingMarketOrders");
        try {
            final Collection<MarketOrderInterface> marketOrders = db.revealingOrFinalizingMarketOrders();
            if ((marketOrders == null) || (marketOrders.size() == 0)) {
                getLogger().debug("detectAbortedTasks : no revealingOrFinalizing market orders ");
                return;
            }

            getLogger().debug("detectAbortedTasks : checking revealingOrFinalizingMarketOrders " + marketOrders.size());

            URI result = null;
            String woid = null;

            for (final MarketOrderInterface marketOrder : marketOrders) {
                getLogger().debug("detectAbortedTasks revealingOrFinalizingMarketOrders " + marketOrder.toXml());

                final Collection<WorkInterface> works = db.marketOrderWorks(marketOrder);
                final long expectedContributions = marketOrder.getExpectedWorkers();
                getLogger().debug("detectAbortedTasks revealingOrFinalizingMarketOrders, ["
                        + marketOrder.getUID() + "] ("
                        + works.size()
                        + ") : " + expectedContributions);

                long totalCompleted = 0L;
                if(works != null) {
                    for (WorkInterface work : works) {

                        getLogger().debug("detectAbortedTasks revealingOrFinalizingMarketOrders, ["
                                + marketOrder.getUID() + "] ("
                                + works.size()
                                + ") : " + work.toXml());

//                    if (work.getStatus() == StatusEnum.ERROR) {
//                       //reopen?
//                    }

                        if (work.getStatus() == StatusEnum.COMPLETED) {
                            totalCompleted++;
                            woid = work.getWorkOrderId();
                            result = work.getResult();
                        }
                    }
                }
                getLogger().debug("detectAbortedTasks revealingOrFinalizingMarketOrders, ["
                        + marketOrder.getUID() + "] ("
                        + works.size()
                        + ") : " + totalCompleted + "/" + expectedContributions);

                if (totalCompleted >= expectedContributions) {
                    getLogger().debug("detectAbortedTasks revealingOrFinalizingMarketOrders, ["
                            + marketOrder.getUID() + "] ("
                            + works.size()
                            + ") : finalizing");
                    try {
                        SchedulerPocoWatcherImpl.doFinalize(woid, result, marketOrder, works, getLogger());
                    } catch(Exception e) {
                        getLogger().exception(e);
                    }
                }
                else {
                    getLogger().debug("detectAbortedTasks revealingOrFinalizingMarketOrders, ["
                            + marketOrder.getUID() + "] ("
                            + works.size()
                            + ") : not finalizing yet");
                }
            }

        }
        catch (Exception e) {
            getLogger().exception(e);
        }

    }
	private void checkContributingMarketOrders() {

        final DBInterface db = DBInterface.getInstance();
        getLogger().debug("detectAbortedTasks : checking contributingOrContributedMarketOrders");

		try {
            final Collection<MarketOrderInterface> marketOrders = db.contributingOrContributedMarketOrders();
            if ((marketOrders == null) || (marketOrders.size() == 0)) {
                getLogger().debug("detectAbortedTasks : no contributing market orders ");
                return;
            }

            getLogger().debug("detectAbortedTasks : checking contributingOrContributedMarketOrders " + marketOrders.size());

            URI result = null;
            String woid = null;

            for (final MarketOrderInterface marketOrder : marketOrders) {
                getLogger().debug("detectAbortedTasks contributingOrContributedMarketOrders " + marketOrder.toXml());

                if(marketOrder.getStatus() == StatusEnum.REVEALING) {
                    getLogger().debug("detectAbortedTasks contributingOrContributedMarketOrders mo is revealing " + marketOrder.getUID());
                    continue;
                }
                final long expectedContributions = marketOrder.getExpectedWorkers();
                final Collection<WorkInterface> works = db.marketOrderWorks(marketOrder);
                getLogger().debug("detectAbortedTasks : expectedContributions: " + expectedContributions + "/" + works.size());
                long totalContributions = 0L;
                long consensusCounter = 0L;
                final Hashtable<String, Integer> contributionCounters = new Hashtable<>();
                WorkInterface theWork = null;

                for (final WorkInterface work : works) {

                    final TaskInterface task = DBInterface.getInstance().computingTask(work);
                    if (task == null) {
                        getLogger().error("detectAbortedTasks : can't retrieve any task for work : " + work.getUID());
                        return;
                    }
                    final HostInterface host = DBInterface.getInstance().host(task.getHost());
                    if (host == null) {
                        getLogger().error("detectAbortedTasks : can't retrieve any host for work : " + work.getUID());
                        return;
                    }
                    getLogger().debug("detectAbortedTasks for work : " + work.toXml() +
                            " by worker " + host.getEthWalletAddr());

                    final ContributionStatusEnum contributionStatus =
                            XWTools.workerContributionStatus(new EthereumWallet(host.getEthWalletAddr()),
                                    work.getWorkOrderId());

                    if(contributionStatus != ContributionStatusEnum.CONTRIBUTED) {
                        getLogger().debug("detectAbortedTasks : not contributed " + work.toXml() + "; " + contributionStatus);
                        continue;
                    }

                    totalContributions++;
                    final String h2h2r = work.getH2h2r();
                    if(h2h2r != null) {
                        final Integer counter = contributionCounters.get(h2h2r) != null ?
                                contributionCounters.get(h2h2r) + 1 :
                                1;
                        contributionCounters.put(h2h2r, counter);
                        getLogger().debug("detectAbortedTasks : counter(" + h2h2r + ") = " + counter);
                        if(counter >= expectedContributions) {
                            getLogger().debug("detectAbortedTasks : counter(" + h2h2r + ") = expectedContributions " +
                                    " (" + expectedContributions + ")");
                            consensusCounter = expectedContributions;
                            theWork = work;
                            break;
                        }

                    }
                }
                getLogger().debug("detectAbortedTasks : expectedContributions : " + expectedContributions);
                getLogger().debug("detectAbortedTasks : totalContributions    : " + totalContributions);
                getLogger().debug("detectAbortedTasks : consensusCounter      : " + consensusCounter);
                if (consensusCounter >= expectedContributions) {
                    getLogger().debug("detectAbortedTasks : enough contributions");

                    for (final WorkInterface contributingWork : works) {

                        getLogger().debug("detectAbortedTasks : work must be revealed " + contributingWork.toXml());
                        try {

                            if(contributingWork.hasContributed()) {
                                contributingWork.setRevealing();
                                contributingWork.update();

                                final TaskInterface contributingTask = DBInterface.getInstance().computingTask(contributingWork);
                                if (contributingTask != null) {
                                    contributingTask.setRevealing();
                                    contributingTask.update();
                                }
                            }
                        } catch (final IOException e) {
                            getLogger().exception(e);
                        }
                    }

                    final TransactionStatus txStatus = ActuatorService.getInstance().revealConsensus(theWork.getWorkOrderId(), theWork.getH2h2r());
                    if ((txStatus == null) || (txStatus == TransactionStatus.FAILURE)) {
                        getLogger().debug("detectAbortedTasks : revealConsensus error; will retry later");
                    }
                    else {

                        marketOrder.setRevealing();
                        getLogger().debug("detectAbortedTasks : market order has been setRevealing: " + marketOrder);
                        try {
                            marketOrder.update();
                        } catch (final IOException e) {
                            getLogger().exception(e);
                        }
                    }
                }
                else {
                    if (totalContributions < expectedContributions) {
                        getLogger().debug("detectAbortedTasks : not enough contributions");
                    }
                    else {
                        // we need one more worker and one more work
                        marketOrder.incExpectedWorkers();
                        marketOrder.setErrorMsg("Warn: need more workers");

//                    final UID originalUid = theWork.getReplicatedUid();
                        final UID originalUid = theWork.getReplicatedUid() != null ? theWork.getReplicatedUid() : theWork.getUID();
//                    if (originalUid != null) {
                        final WorkInterface replicatedWork = db.work(originalUid);
                        final long expectedReplications = replicatedWork.getExpectedReplications();
                        final long currentReplications = replicatedWork.getTotalReplica();
                        getLogger().warn("need more replicas : " + originalUid);
                        final WorkInterface newWork = new WorkInterface(replicatedWork);
                        newWork.setUID(new UID());
                        newWork.replicate(originalUid);
                        newWork.insert();
                        final AppInterface theApp = db.app(theWork.getApplication());
                        final UserInterface jobOwner = db.user(theWork.getOwner());
                        if(theApp != null) {
                            theApp.incPendingJobs();
                            theApp.update();
                        }
                        if(jobOwner != null) {
                            jobOwner.incPendingJobs();
                            jobOwner.update();
                        }
                        replicatedWork.setExpectedReplications(replicatedWork.getExpectedReplications() + 1);
                        replicatedWork.incTotalReplica();
                        replicatedWork.setErrorMsg("Warn: need more replicas for market order");
                        replicatedWork.update();
//                    }
                    }
                }

                marketOrder.update();
            }

        }
        catch (Exception e) {
		    getLogger().exception(e);
        }
    }

    /**
	 * This converts this object to string
	 */
	@Override
	public String toString() {

		String s = "";
		try {
			final Collection<WorkInterface> works = DBInterface.getInstance().works();
			if (works == null) {
				return null;
			}
			for (final Iterator<WorkInterface> enumeration = works.iterator(); enumeration.hasNext();) {
				final WorkInterface theWork = enumeration.next();
				s += theWork.toString() + "\n";
			}
			return s;
		} catch (final Exception e) {
			s = null;
		}
		return s;
	}

}
