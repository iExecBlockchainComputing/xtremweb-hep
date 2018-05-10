package xtremweb.dispatcher;

import com.iexec.common.contracts.generated.WorkerPool;
import com.iexec.common.ethereum.IexecConfigurationService;
import com.iexec.common.ethereum.Web3jService;
import com.iexec.common.model.AppModel;
import com.iexec.common.model.ModelService;
import com.iexec.common.model.WorkOrderModel;
import com.iexec.scheduler.actuator.ActuatorService;
import com.iexec.scheduler.database.ContributionService;
import com.iexec.scheduler.iexechub.IexecHubService;
import com.iexec.scheduler.iexechub.IexecHubWatcher;
import com.iexec.scheduler.workerpool.WorkerPoolService;
import com.iexec.scheduler.workerpool.WorkerPoolWatcher;
import xtremweb.common.*;
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
     * @param workerWalletAddr is the worker wallet address
     */
    @Override
    public void onSubscription(String workerWalletAddr) {
        try {
            final HostInterface host = DBInterface.getInstance().host(workerWalletAddr);
            if(host == null) {
                logger.warn("onSubscription(" + workerWalletAddr +") : host not found");
                return;
            }
            logger.debug("onSubscription(" + workerWalletAddr + ") : " + host.toXml());
            final MarketOrderInterface marketOrder = DBInterface.getInstance().marketOrderUnsatisfied(host.getWorkerPoolAddr());
            if(marketOrder == null) {
                logger.info("onSubscription(" + workerWalletAddr +") : no unsatisfied market order");
                return;
            }
            if (host.canContribute()) {
                marketOrder.addWorker(host);
                host.update();
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
            final AppInterface existingApp = getApp(appModel.getName());
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
            newApp.setName(appModel.getName());
            newApp.setPrice(appModel.getPrice().longValue());
            newApp.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() & XWAccessRights.STICKYBIT_INT));

            final String appParams = appModel.getParams();
            if(appParams != null) {
                try {
                    final String envvars = XWTools.jsonValueFromString(appModel.getParams(), "envvars");
                    newApp.setEnvVars(envvars);
                } catch (final Exception e) {
                    logger.warn("can't extract newApp envvars from " + appParams);
                }
                try {
                    final String appTypeStr = XWTools.jsonValueFromString(appModel.getParams(), "type");
                    final AppTypeEnum appType = AppTypeEnum.valueOf(appTypeStr);
                    newApp.setType(appType);
                } catch (final Exception e) {
                    logger.warn("can't extract newApp type from " + appParams);
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
     * @param model is the work order model
     * @return the market order of the provided work order model
     */
    private MarketOrderInterface createWork(final String workOrderId, final WorkOrderModel model) {

        if (administrator == null) {
            logger.error("createWork() : user administrator not defined");
            return null;
        }

        logger.debug("createWork(" + model.getMarketorderIdx().longValue() + ")");

        final MarketOrderInterface marketOrder = getMarketOrder(model.getMarketorderIdx().longValue());
        if(marketOrder == null) {
            logger.error("createWork() : can't retrieve market order : "
                    + model.getMarketorderIdx().longValue());
            return null;
        }
        if(marketOrder.getWorkerPoolAddr().compareTo(model.getWorkerpool()) != 0) {
            logger.error("createWork() : worker pool mismatch : "
                    + marketOrder.getWorkerPoolAddr() + " != "
                    + model.getWorkerpool());
            return null;
        }

        final AppModel appModel = ModelService.getInstance().getAppModel(model.getApp());
        if(appModel == null) {
            logger.error("createWork() : can't retrieve app model "
                    + model.getApp());
            return null;
        }
        final AppInterface app = getApp(appModel);
        if(app == null) {
            logger.error ("createWork() : can't add/retrieve app " + appModel.getName());
            return null;
        }

        final UserInterface requester = getUser(model.getRequester());
        if (requester == null) {
            logger.error("createWork() : unkown requester " + model.getRequester());
            return null;
        }

        try {
            final WorkInterface work = new WorkInterface();
            work.setUID(new UID());
            work.setMarketOrderUid(marketOrder.getUID());
            work.setOwner(requester.getUID());
            work.setApplication(app.getUID());
            work.setDataset(model.getDataset());
            work.setBeneficiary(model.getBeneficiary());
            work.setWorkerPool(model.getWorkerpool());
            work.setEmitCost(model.getEmitcost().longValue());
            work.setCmdLine(model.getParams());
            work.setCallback(model.getCallback());
            work.setBeneficiary(model.getBeneficiary());
            work.setExpectedReplications(marketOrder.getExpectedWorkers());
            work.setCategoryId(marketOrder.getCategoryId());
            work.setWorkOrderId(workOrderId);
            work.setStatus(StatusEnum.PENDING);
            work.setExpectedReplications(marketOrder.getExpectedWorkers());
            work.setReplicaSetSize(marketOrder.getNbWorkers());
            work.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() & XWAccessRights.STICKYBIT_INT));

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

            actuatorService.allowWorkersToContribute(workOrderId,
                    wallets,
                    "O");

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

        final String contribution = XWTools.byteArrayToHexString(contributeEventResponse.resultHash);
        theWork.setH2r(contribution);
        logger.debug("onContributeEvent() : " + theWork.toXml());

        final MarketOrderInterface marketOrder = getMarketOrder(workOrderModel.getMarketorderIdx().longValue());
        final Collection<WorkInterface> works = getMarketOrderWorks(workOrderModel.getMarketorderIdx().longValue());

        if(works == null) {
            logger.error("createWork() : can't retrieve any work for market order : "
                    + workOrderModel.getMarketorderIdx().longValue());
            return;
        }

        marketOrder.getTrust();
        final long expectedWorkers = marketOrder.getExpectedWorkers();
        final long trust = marketOrder.getTrust();
        final long expectedContributions = (expectedWorkers * trust / 100);
        long totalContributions = 0L;
        for(final WorkInterface work : works ) {
            if(work.hasContributed()
                    && (work.getH2r().compareTo(contribution) == 0)) {
                totalContributions++;
            }
        }
        if (totalContributions >= expectedContributions) {
            theWork.setRevealing();
            try {
                theWork.update();
            } catch(final IOException e) {
                logger.exception(e);
            }

            for(final WorkInterface contributingWork : works ) {

                try {
                    contributingWork.setRevealing();
                    contributingWork.update();

                    final TaskInterface contributingTask = DBInterface.getInstance().task(contributingWork);
                    if(contributingTask != null) {
                        contributingTask.setRevealing();
                        contributingTask.update();
                    }
                } catch(final IOException e) {
                    logger.exception(e);
                }
            }
        }

        //actuatorService.revealConsensus(contributeEventResponse.woid, Utils.hashResult("iExec the wanderer"));
    }

    @Override
    public void onReveal(WorkerPool.RevealEventResponse revealEventResponse) {
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
