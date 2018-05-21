package xtremweb.dispatcher;

import com.iexec.common.contracts.generated.WorkerPool;
import com.iexec.common.ethereum.IexecConfigurationService;
import com.iexec.common.ethereum.Utils;
import com.iexec.common.ethereum.Web3jService;
import com.iexec.common.model.AppModel;
import com.iexec.common.model.ModelService;
import com.iexec.common.model.WorkOrderModel;
import com.iexec.scheduler.actuator.ActuatorService;
import com.iexec.scheduler.database.Contribution;
import com.iexec.scheduler.database.ContributionService;
import com.iexec.scheduler.iexechub.IexecHubService;
import com.iexec.scheduler.iexechub.IexecHubWatcher;
import com.iexec.scheduler.workerpool.WorkerPoolService;
import com.iexec.scheduler.workerpool.WorkerPoolWatcher;
import org.json.JSONException;
import org.web3j.utils.Numeric;
import xtremweb.common.*;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommandSendApp;
import xtremweb.communications.XMLRPCCommandSendWork;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

public class SchedulerPocoWatcherImpl implements IexecHubWatcher, WorkerPoolWatcher {

    private final static IexecHubService iexecHubService = IexecHubService.getInstance();
    private final static WorkerPoolService workerPoolService = WorkerPoolService.getInstance();
    private final static ContributionService contributionService = ContributionService.getInstance();
    private final static ActuatorService actuatorService = ActuatorService.getInstance();
    private final Logger logger;

    private UserInterface administrator = null;


    public SchedulerPocoWatcherImpl() {
        logger = new Logger(this);
        logger.info(IexecConfigurationService.getInstance().getCommonConfiguration().getContractConfig().getWorkerPoolConfig().getAddress());
        try {
            logger.info(Web3jService.getInstance().getWeb3j().web3ClientVersion().send().getWeb3ClientVersion());
        } catch (IOException e) {
            e.printStackTrace();
        }
        iexecHubService.registerIexecHubWatcher(this);
        workerPoolService.registerWorkerPoolWatcher(this);
        try {
            administrator = Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) == null ? null
                    : DBInterface.getInstance()
                    .user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
                            + Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) + "'");
        } catch (final Exception e) {
            logger.exception(e);
            administrator = null;
        }
    }

    /**
     * This is a blockchain event watcher automatically called on worker subscription.
     * This retrieves a market order still starving computing resources,
     * and registers the worker as volunteer; if no such market order, this returns.
     * Finally, the market order is created on the blockchain if it has enough volunteers.
     *
     * If it is found that, for a given market order, a wallet has been presented by two or more different workers,
     * all these workers using the same wallet are banned
     *
     * @param workerWalletAddr is the worker wallet address
     */
    @Override
    public void onSubscription(String workerWalletAddr) {
        try {
            final HostInterface theHost = DBInterface.getInstance().host(new EthereumWallet(workerWalletAddr));
            if(theHost == null) {
                logger.warn("onSubscription(" + workerWalletAddr +") : host not found");
                return;
            }
            logger.debug("onSubscription(" + workerWalletAddr + ") : " + theHost.toXml());
            final MarketOrderInterface marketOrder = DBInterface.getInstance().marketOrderUnsatisfied(theHost.getWorkerPoolAddr());
            if(marketOrder == null) {
                logger.info("onSubscription(" + workerWalletAddr +") : no unsatisfied market order");
                return;
            }

            final Collection<HostInterface> hosts = DBInterface.getInstance().hosts(new EthereumWallet(workerWalletAddr),
                    marketOrder);
            logger.debug("onSubscription(" + workerWalletAddr + ") : duplicated wallet " + (hosts == null ? 0 : hosts.size()));

            if (hosts != null) {
                boolean error = false;
                for (HostInterface host : hosts) {
                    if(host.getUID().equals(theHost.getUID()))
                        continue;
                    error = true;
                    logger.error("onSubscription(" + workerWalletAddr +") : more than one wallet owner " + host.getUID());
                    host.leaveMarketOrder(marketOrder);
                    host.setActive(false);
                    host.update();
                }
                if(error) {
                    logger.error("onSubscription(" + workerWalletAddr +") : more than one wallet owner " + theHost.getUID());
                    theHost.leaveMarketOrder(marketOrder);
                    theHost.setActive(false);
                    theHost.update();
                    return;
                }
            }

            if (theHost.canContribute()) {
                marketOrder.addWorker(theHost);
                marketOrder.setWaiting();
                theHost.update();
                marketOrder.update();
            }
            if(marketOrder.canStart()) {
               final BigInteger marketOrderIdx = actuatorService.createMarketOrder(BigInteger.valueOf(marketOrder.getCategoryId()),
                       BigInteger.valueOf(marketOrder.getTrust()),
                       BigInteger.valueOf(marketOrder.getPrice()),
                       BigInteger.valueOf(marketOrder.getVolume()));
               marketOrder.setMarketOrderIdx(marketOrderIdx.longValue());
               marketOrder.update();
            }
        } catch (final IOException e) {
            logger.exception(e);
        }
    }

    /**
     * This creates and inserts into DB a new user
     * @param userAddr is the user ethereum wallet public addr
     * @return
     */
    private UserInterface newUser(String userAddr) {
        if ( ! Dispatcher.getConfig().getBoolean(XWPropertyDefs.DELEGATEDREGISTRATION)) {
            logger.error("DELEGATEDREGISTRATION is not allowed");
            return null;
        }
        if (administrator == null) {
            logger.error("newUser() : user administrator not defined");
            return null;
        }

        try {
            final String random = "" + System.currentTimeMillis() + Math.random();
            final String shastr = XWTools.sha256(random);
            final UserInterface client = new UserInterface();
            client.setUID(new UID());
            client.setOwner(Dispatcher.getConfig().getAdminUid());
            client.setLogin(userAddr);
            client.setPassword(shastr);
            client.setRights(UserRightEnum.STANDARD_USER);
            client.setEMail("");
            DBInterface.getInstance().addUser(administrator, client);

            return client;
        } catch (final Exception e) {
            logger.exception(e);
            return null;
        }
    }
    /**
     * This retrieves user from DB
     * If user does not exist in DB, a new user is created and inserted in DB
     * @param userAddr
     * @return
     */
    private UserInterface getUser(final String userAddr) {
        if((userAddr == null) || (userAddr.length() < 1)) {
            return null;
        }
        try {
            final UserInterface user = DBInterface.getInstance().selectOne(new UserInterface(),
                    SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN +
                            "='" + userAddr + "'");

            if(user != null) {
                return user;
            }

            return newUser(userAddr);

        } catch(final IOException e) {
            logger.exception(e);
            return null;
        }

    }
    /**
     * This retrieves app from DB
     * @param appName
     * @return
     */
    private AppInterface getApp(final String appName) {
        if((appName == null) || (appName.length() < 1)) {
            return null;
        }
        try {
            return DBInterface.getInstance().selectOne(new AppInterface(),
                    SQLRequest.MAINTABLEALIAS + "." + AppInterface.Columns.NAME.toString() +
                            "='" + appName + "'");
        } catch(final IOException e) {
            logger.exception(e);
            return null;
        }

    }
    /**
     * This retrieves the app as defined from the AppModel.
     * If app does not exist in DB, a new app is created and inserted in DB
     * @param appModel is the model received from the blockchain
     * @return the found or created app; null on error
     */
    private AppInterface getApp(final AppModel appModel) {

        if (administrator == null) {
            logger.error("getApp() : user administrator not defined");
            return null;
        }

        try {
            if(appModel == null) {
                return null;
            }
            final AppInterface existingApp = getApp(appModel.getId());
            if (existingApp != null) {
                return existingApp;
            }

            final AppInterface newApp = new AppInterface(new UID());
            final UserInterface appOwner = getUser(appModel.getOwner());
            if(appOwner == null) {
                logger.warn("newApp owner not found " + appModel.getOwner());
                return null;
            }

            newApp.setOwner(appOwner.getUID());
            newApp.setName(appModel.getId());
            newApp.setPrice(appModel.getPrice().longValue());
            newApp.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() | XWAccessRights.STICKYBIT_INT));

            final String appParams = appModel.getParams();
            if(appParams != null) {

                try {
                    final String envvars = XWTools.jsonValueFromString(appModel.getParams(), "envvars");
                    newApp.setEnvVars(envvars);
                } catch (final JSONException e) {
                    logger.warn(e.getMessage());
                }
                try {
                    final String appTypeStr = XWTools.jsonValueFromString(appModel.getParams(), "type");
                    final AppTypeEnum appType = AppTypeEnum.valueOf(appTypeStr);
                    newApp.setType(appType);
                } catch(final JSONException e) {
                    logger.warn(e.getMessage());
                    return null;
                }
            }

            final XMLRPCCommandSendApp cmd =
                    new XMLRPCCommandSendApp(XWTools.newURI(newApp.getUID()),
                            administrator,
                            newApp);

            cmd.setMandatingLogin(appOwner.getLogin());
            DBInterface.getInstance().addApp(cmd);

            return newApp;

        } catch(final Exception e) {
            logger.exception(e);
            return null;
        }
    }

    /**
     * This retrieves a market order from DB
     * @param idx is the market order index
     * @return the market order; null on error, or if not found
     */
    private MarketOrderInterface getMarketOrder(final  long idx) {
        try {
            return DBInterface.getInstance().marketOrderByIdx(idx);
        } catch(final Exception e) {
            logger.exception(e);
            return null;
        }
    }
    /**
     * This retrieves all works of a market order from DB
     * @param idx is the market order index
     * @return the market order; null on error, or if not found
     */
    private Collection<WorkInterface> getMarketOrderWorks(final  long idx) {
        try {
            final MarketOrderInterface marketOrder = getMarketOrder(idx);
            if(marketOrder == null) {
                throw new IOException("can't retrieve market order : " + idx);
            }

            return DBInterface.getInstance().marketOrderWorks(marketOrder);

        } catch(final Exception e) {
            logger.exception(e);
            return null;
        }
    }

    /**
     * This creates a new WorkInterface in DB from the provided WorkOrderModel.
     * This new work has as many replica as expected by the market order
     * @param workModel is the work order model
     * @return the market order of the provided work order model
     */
    private MarketOrderInterface createWork(final String workOrderId, final WorkOrderModel workModel) {

        if (administrator == null) {
            logger.error("createWork() : user administrator not defined");
            return null;
        }

        logger.debug("createWork(" + workModel.getMarketorderIdx().longValue() + ")");

        final MarketOrderInterface marketOrder = getMarketOrder(workModel.getMarketorderIdx().longValue());
        if(marketOrder == null) {
            logger.error("createWork() : can't retrieve market order : "
                    + workModel.getMarketorderIdx().longValue());
            return null;
        }
        if(marketOrder.getWorkerPoolAddr().compareTo(workModel.getWorkerpool()) != 0) {
            logger.error("createWork() : worker pool mismatch : "
                    + marketOrder.getWorkerPoolAddr() + " != "
                    + workModel.getWorkerpool());
            return null;
        }

        final AppModel appModel = ModelService.getInstance().getAppModel(workModel.getApp());
        if(appModel == null) {
            logger.error("createWork() : can't retrieve app workModel "
                    + workModel.getApp());
            return null;
        }
        final AppInterface app = getApp(appModel);
        if(app == null) {
            logger.error ("createWork() : can't add/retrieve app " + appModel.getName());
            return null;
        }

        final UserInterface requester = getUser(workModel.getRequester());
        if (requester == null) {
            logger.error("createWork() : unkown requester " + workModel.getRequester());
            return null;
        }

        try {
            final WorkInterface work = new WorkInterface();
            work.setUID(new UID());
            work.setMarketOrderUid(marketOrder.getUID());
            work.setOwner(requester.getUID());
            work.setApplication(app.getUID());
            work.setDataset(workModel.getDataset());
            work.setBeneficiary(workModel.getBeneficiary());
            work.setWorkerPool(workModel.getWorkerpool());
            work.setEmitCost(workModel.getEmitcost().longValue());

            try {
                final String cmdline = XWTools.jsonValueFromString(workModel.getParams(), "cmdline");
                work.setCmdLine(cmdline);
            } catch(final JSONException e) {
                logger.warn(e.getMessage());
            }
            try {
                final String dirinuri = XWTools.jsonValueFromString(workModel.getParams(), "dirinuri");
                work.setDirin(new URI(dirinuri));
            } catch(final JSONException e) {
                logger.warn(e.getMessage());
            }

            work.setCallback(workModel.getCallback());
            work.setBeneficiary(workModel.getBeneficiary());
            work.setExpectedReplications(marketOrder.getExpectedWorkers());
            work.setCategoryId(marketOrder.getCategoryId());
            work.setWorkOrderId(workOrderId);
            work.setStatus(StatusEnum.PENDING);
            work.setExpectedReplications(marketOrder.getExpectedWorkers());
            work.setReplicaSetSize(marketOrder.getNbWorkers());
            work.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() | XWAccessRights.STICKYBIT_INT));

            final XMLRPCCommandSendWork cmd =
                    new XMLRPCCommandSendWork(XWTools.newURI(work.getUID()),
                            administrator,
                            work);

            cmd.setMandatingLogin(requester.getLogin());
            DBInterface.getInstance().addWork(cmd);

            return marketOrder;

        } catch (final Exception e) {
            logger.exception(e);
            return null;
        }
    }
    /**
     * This is a blockchain event watcher automatically called on market order sale.
     * This first retrieves from DB or creates a new application in DB.
     * Then this registers a new WorkInterface in DB.
     *
     * @param workOrderId is the blockchain work order id
     */
    @Override
    public void onWorkOrderActivated(String workOrderId) {
        final WorkOrderModel workOrderModel = ModelService.getInstance().getWorkOrderModel(workOrderId);
        logger.debug("onWorkOrderActivated(" + workOrderId + ")");
        if(workOrderModel == null) {
            logger.error("onWorkOrderActivated() : can't retrieve work model "
                    + ModelService.getInstance().getWorkOrderModel(workOrderId));
            return;
        }

        final MarketOrderInterface marketOrder = createWork(workOrderId, workOrderModel);
        try {
            final Collection<HostInterface> workers = DBInterface.getInstance().hosts(marketOrder);
            if(workers == null) {
                logger.error("onWorkOrderActivated(" + workOrderId +") : can't find any host" );
                return;
            }

            final ArrayList<String> wallets = new ArrayList();
            for(final HostInterface worker : workers ) {
                if(worker.getEthWalletAddr() != null)
                    wallets.add(worker.getEthWalletAddr());
            }

            contributionService.setCalledWorker(workOrderId, wallets);

            marketOrder.setContributing();
            marketOrder.update();

            actuatorService.allowWorkersToContribute(workOrderId,
                    wallets,
                    "0");

        } catch(final Exception e) {
            logger.exception(e);
        }
        //        DatasetModel datasetModel = ModelService.getInstance().getDatasetModel(workOrderModel.getDataset());
    }

    /**
     * This is a blockchain event watcher automatically called on worker contribution.
     * The scheduler must ask to reveal to all workers as soon as the consensus us reached
     * @param contributeEventResponse is the blockchain worker contribution
     */
    @Override
    public void onContributeEvent(WorkerPool.ContributeEventResponse contributeEventResponse) {

        final WorkOrderModel workOrderModel = ModelService.getInstance().getWorkOrderModel(contributeEventResponse.woid);
        final WorkInterface theWork = DBInterface.getInstance().work(contributeEventResponse);
        if(theWork == null)
            return;

        final String contributionStr = XWTools.byteArrayToHexString(contributeEventResponse.resultHash);
        theWork.setH2h2r(contributionStr);
        logger.debug("onContributeEvent() : " + theWork.toXml());

        final MarketOrderInterface marketOrder = getMarketOrder(workOrderModel.getMarketorderIdx().longValue());
        final Collection<WorkInterface> works = getMarketOrderWorks(workOrderModel.getMarketorderIdx().longValue());

        if(works == null) {
            logger.error("createWork() : can't retrieve any work for market order : "
                    + workOrderModel.getMarketorderIdx().longValue());
            return;
        }

        try {
            final TaskInterface theWorkTask = DBInterface.getInstance().task(theWork);
            final HostInterface theHost = DBInterface.getInstance().host(theWorkTask.getHost());
            Contribution contribution = new Contribution(contributeEventResponse.woid,
                    theHost.getEthWalletAddr(),
                    contributionStr.getBytes());

            contributionService.addContribution(contribution);

        } catch (final IOException e) {
            logger.exception(e);
        }

        final long expectedWorkers = marketOrder.getExpectedWorkers();
        final long trust = marketOrder.getTrust();
        final long expectedContributions = (expectedWorkers * trust / 100);
        long totalContributions = 0L;
        for(final WorkInterface work : works ) {
            if(work.hasContributed()
                    && (work.getH2h2r().compareTo(contributionStr) == 0)) {
                totalContributions++;
            }
        }
        if (totalContributions >= expectedContributions) {
            logger.debug("onContributeEvent() : enough contributions");
            theWork.setRevealing();
            try {
                theWork.update();
            } catch (final IOException e) {
                logger.exception(e);
            }
            logger.debug("onContributeEvent() : work must be revealed " + theWork.toXml());

            for (final WorkInterface contributingWork : works) {

                logger.debug("onContributeEvent() : work must be revealed " + contributingWork.toXml());
                try {
                    contributingWork.setRevealing();
                    contributingWork.update();

                    final TaskInterface contributingTask = DBInterface.getInstance().task(contributingWork);
                    if (contributingTask != null) {
                        contributingTask.setRevealing();
                        contributingTask.update();
                    }
                } catch (final IOException e) {
                    logger.exception(e);
                }
            }

            marketOrder.setRevealing();
            try {
                marketOrder.update();
            } catch(final IOException e) {
                logger.exception(e);
            }


            actuatorService.revealConsensus(contributeEventResponse.woid, Numeric.toHexString(contributeEventResponse.resultHash));

        } else {
            logger.debug("onContributeEvent() : not enough contributions");
        }
    }

    @Override
    public void onReveal(WorkerPool.RevealEventResponse revealEventResponse) {
        final WorkOrderModel workOrderModel = ModelService.getInstance().getWorkOrderModel(revealEventResponse.woid);
        final MarketOrderInterface marketOrder = getMarketOrder(workOrderModel.getMarketorderIdx().longValue());
        marketOrder.setCompleted();
        //actuatorService.finalizeWork(revealEventResponse.woid,"aStdout", "aStderr", "anUri");
    }

    @Override
    public void onWorkOrderClaimed(WorkerPool.WorkOrderClaimedEventResponse workOrderClaimedEventResponse) {

    }

    @Override
    public void onReopenEvent(WorkerPool.ReopenEventResponse reopenEventResponse) {

    }

    @Override
    public void onWorkerEvictionEvent(WorkerPool.WorkerEvictionEventResponse workerEvictionEventResponse) {

    }
}
