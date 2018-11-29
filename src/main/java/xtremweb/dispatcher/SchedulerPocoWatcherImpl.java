package xtremweb.dispatcher;

import com.iexec.common.contracts.generated.WorkerPool;
import com.iexec.common.ethereum.IexecConfigurationService;
import com.iexec.common.ethereum.TransactionStatus;
import com.iexec.common.ethereum.Web3jService;
import com.iexec.common.model.*;
import com.iexec.scheduler.actuator.ActuatorService;
import com.iexec.scheduler.iexechub.IexecHubService;
import com.iexec.scheduler.iexechub.IexecHubWatcher;
import com.iexec.scheduler.workerpool.WorkerPoolService;
import com.iexec.scheduler.workerpool.WorkerPoolWatcher;
import org.json.JSONException;
import xtremweb.common.*;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommandSendApp;
import xtremweb.communications.XMLRPCCommandSendWork;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SchedulerPocoWatcherImpl implements IexecHubWatcher, WorkerPoolWatcher {

    private final static IexecHubService iexecHubService = IexecHubService.getInstance();
    private final static WorkerPoolService workerPoolService = WorkerPoolService.getInstance();
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
        //actuatorService.depositRlc();
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
    public synchronized void onSubscription(String workerWalletAddr) {

        try {
            DBInterface.getInstance().hostContribution(new EthereumWallet(workerWalletAddr));
        } catch (final IOException e) {
            logger.exception(e);
        }
        notifyAll();
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

            final UID clientGroup = appOwner.getGroup();

            newApp.setOwner(appOwner.getUID());
            newApp.setName(appModel.getId());
            newApp.setPrice(appModel.getPrice().longValue());
            newApp.setAccessRights(null);

            if (appOwner.getRights().lowerThan(UserRightEnum.ADVANCED_USER)) {
                logger.debug("set app AR to USERALL " + new XWAccessRights(XWAccessRights.USERALL.value() | XWAccessRights.STICKYBIT_INT));
                newApp.setAccessRights(new XWAccessRights(XWAccessRights.USERALL.value() | XWAccessRights.STICKYBIT_INT));
            }
            if (appOwner.getRights().doesEqual(UserRightEnum.SUPER_USER)) {
                if (newApp.getAccessRights() == null) {
                    logger.debug("set app AR to DEFAULT " + new XWAccessRights(XWAccessRights.DEFAULT.value()));
                    newApp.setAccessRights(new XWAccessRights(XWAccessRights.DEFAULT.value()));
                }
                else {
                    logger.debug("keeping app AR " + newApp.getAccessRights());
                }
            } else {
                if ((appOwner.getRights().higherOrEquals(UserRightEnum.INSERTAPP)) && (clientGroup != null)) {
                    logger.debug("set app AR to OWNERGROUP " + new XWAccessRights(XWAccessRights.OWNERGROUP.value()));
                    newApp.setAccessRights(new XWAccessRights(XWAccessRights.OWNERGROUP.value()));
                }
            }

            final String appParams = appModel.getParams();
            if(appParams != null) {

                try {
                    final String envvars = XWTools.jsonValueFromString(appModel.getParams(), "envvars");
                    newApp.setEnvVars(envvars);
                } catch (final JSONException e) {
                    logger.info("Can't retreive app envvars : " + e.getMessage());
                }
                try {
                    final String appTypeStr = XWTools.jsonValueFromString(appModel.getParams(), "type");
                    final AppTypeEnum appType = AppTypeEnum.valueOf(appTypeStr);
                    newApp.setType(appType);
                } catch(final JSONException e) {
                    logger.error("Can't retreive app type : " + e.getMessage());
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
            return DBInterface.getInstance().marketOrderWorks(marketOrder);
        } catch(final Exception e) {
            logger.exception(e);
            return null;
        }
    }

    /**
     * This creates a new WorkInterface in DB from the provided WorkOrderModel.
     * This new work has as many replica as expected by the market order
     * The work status is set to UNAVAILABLE so that the it is not scheduled yet
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
        if (marketOrder.getStatus() != StatusEnum.AVAILABLE) {
            logger.error("createWork() : market order status error : " + marketOrder.getStatus());
            return marketOrder;
        }

        logger.debug("createWork() : " + marketOrder.toXml());

        if((marketOrder.getWorkerPoolAddr() != null) && marketOrder.getWorkerPoolAddr().compareTo(workModel.getWorkerpool()) != 0) {
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
        final AppInterface theApp = getApp(appModel);
        if(theApp == null) {
            logger.error ("createWork() : can't add/retrieve app " + appModel.getName());
            return null;
        }

        String user = workModel.getRequester();
        // ISSUE add support of beneficiary param : https://github.com/iExecBlockchainComputing/xtremweb-hep/issues/93
        if( !workModel.getBeneficiary().equals("0x")){
            logger.info("Beneficiary is not null replace current getRequester["+user+"] by ["+workModel.getBeneficiary()+"]");
            user=workModel.getBeneficiary();
        }
        final UserInterface requester = getUser(user);
        if (requester == null) {
            logger.error("createWork() : unkown requester " + user);
            return null;
        }

        try {
            final WorkInterface work = new WorkInterface();
            work.setUID(new UID());
            work.setMarketOrderUid(marketOrder.getUID());
            work.setOwner(requester.getUID());
            work.setApplication(theApp.getUID());
            work.setDataset(workModel.getDataset());
            work.setBeneficiary(workModel.getBeneficiary());
            work.setWorkerPool(workModel.getWorkerpool());
            work.setEmitCost(workModel.getEmitcost().longValue());

            try {
                final String cmdline = XWTools.jsonValueFromString(workModel.getParams(), "cmdline");
                work.setCmdLine(cmdline);
            } catch(final JSONException e) {
                logger.debug(e.getMessage());
                logger.info("Can't retreive task cmdline : " + e.getMessage());
            }
            try {
                final String dirinuri = XWTools.jsonValueFromString(workModel.getParams(), "dirinuri");
                work.setDirin(new URI(dirinuri));
            } catch(final JSONException e) {
                logger.info("Can't retreive task dirinuri : " + e.getMessage());
            }

            work.setCallback(workModel.getCallback());
            work.setBeneficiary(workModel.getBeneficiary());
            work.setCategoryId(marketOrder.getCategoryId());
            work.setWorkOrderId(workOrderId);
            work.setStatus(StatusEnum.PENDING);
            work.setExpectedReplications(marketOrder.getExpectedWorkers());
            work.setReplicaSetSize(marketOrder.getExpectedWorkers());
            work.setAccessRights(theApp.getAccessRights());
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
        logger.debug("onWorkOrderActivated() : onWorkOrderActivated(" + workOrderId + "), workOrderModel: " + workOrderModel);
        if(workOrderModel == null) {
            logger.error("onWorkOrderActivated() : can't retrieve work model " + workOrderId);
            return;
        }

        try {
            MarketOrderInterface marketOrder = null;

            int createWorkTry;
            for(createWorkTry = 0; createWorkTry < 3; createWorkTry++) {

                marketOrder = createWork(workOrderId, workOrderModel);
                if (marketOrder != null) {
                    break;
                }
                try {
                    logger.warn("onWorkOrderActivated; will retry in 3s");
                    Thread.sleep(3000);
                } catch (final InterruptedException e) {
                }
            }

            if (createWorkTry >= 3) {
                logger.error("onWorkOrderActivated() : can't create work for workOrderId " + workOrderId);
                return;
            }
            if (marketOrder.getStatus() != StatusEnum.AVAILABLE) {
                logger.error("onWorkOrderActivated() : market order status error : " + marketOrder.getStatus());
                return;
            }

            logger.info("onWorkOrderActivated() : marketOrder " + marketOrder);
            final Collection<HostInterface> workers = DBInterface.getInstance().hosts(marketOrder);
            if(workers == null) {
                logger.warn("onWorkOrderActivated(" + workOrderId +") : can't find any host" );
                marketOrder.setErrorMsg("onWorkOrderActivated(" + workOrderId +") : can't find any host" );
                marketOrder.setError();
                marketOrder.update();
                return;
            }

            for(final HostInterface worker : workers ) {
                logger.error("onWorkOrderActivated() : worker: " + worker);
                worker.setPending();
                worker.update();
                if (worker.getEthWalletAddr() != null) {
                    logger.debug("onWorkOrderActivated(" + workOrderId +") : allowing " + worker.getEthWalletAddr());
                }
                allowWorkerToContribute(workOrderId, marketOrder, worker);
            }

            marketOrder.setPending();
            marketOrder.update();

        } catch(final Exception e) {
            logger.exception(e);
        }
        //        DatasetModel datasetModel = ModelService.getInstance().getDatasetModel(workOrderModel.getDataset());
    }

    /**
     * This allows one worker to contribute
     * @param workOrderId
     * @param marketOrder
     * @param worker
     */
    public static synchronized void allowWorkerToContribute(final String workOrderId,
                                                            final MarketOrderInterface marketOrder,
                                                            final HostInterface worker)
            throws IOException{

        if(worker.getEthWalletAddr() == null) {
            System.out.println("allowWorkerToContribute() : no wallet");
            return;
        }
        final Date now = new Date();
        final EthereumWallet wallet = new EthereumWallet(worker.getEthWalletAddr());

        final ContributionModel contribution = WorkerPoolService.getInstance().getWorkerContributionModelByWorkOrderId(workOrderId,
                wallet.getAddress());
        if((contribution != null) && (contribution.getStatus() != ContributionStatusEnum.UNSET)) {
            System.out.println("[" + now + "] allowWorkerToContribute() : " + wallet.getAddress()
                    + " cannot contribute to " + workOrderId
                    + " (" + contribution.getStatus() + ")");
            return;
        }

        final ArrayList<String> wallets = new ArrayList<>();
        wallets.add(wallet.getAddress());

        final String enclageChallengeValue = Dispatcher.getConfig().getProperty(XWPropertyDefs.ENCLAVECHALLENGE);
        final String enclaveChallenge = enclageChallengeValue == null || enclageChallengeValue.length() < 1 ?
                "0" : enclageChallengeValue;

        System.out.println("[" + now + "] allowWorkerToContribute(" + workOrderId + "," +
                (marketOrder == null ? "null" : marketOrder.getUID()) + "," +
                (wallet == null ? "null" : wallet.getAddress()) + "," +
                (worker == null ? "null" : worker.getUID()) + ") " +
                "enclaveChallenge = " + enclaveChallenge);

 			final Thread allowToContributeThread = new Thread(Thread.currentThread().getName() + "allowWorkerToContribute") {
                @Override
                public void run() {
                    try {
                        int contributeTry;
                        for (contributeTry = 0; contributeTry < 3; contributeTry++) {

                            final TransactionStatus txStatus =
                                    actuatorService.allowWorkersToContribute(workOrderId,
                                            wallets,
                                            enclaveChallenge);

                            if ((txStatus == null) || (txStatus == TransactionStatus.FAILURE)) {
                                try {
                                    System.out.println("[" + now + "] allowWorkersToContribute; will retry in 1s " + txStatus);
                                    Thread.sleep(1000);
                                } catch (final InterruptedException e) {
                                }
                            } else {
                                break;
                            }
                        }

                        if (contributeTry >= 3) {
                            final Collection<HostInterface> workers = DBInterface.getInstance().hosts(marketOrder);
                            for (final HostInterface w : workers) {
                                marketOrder.removeWorker(worker);
                                worker.update();
                            }
                            marketOrder.setErrorMsg("transaction error : allowWorkersToContribute");
                            marketOrder.setError();
                            marketOrder.update();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
 			allowToContributeThread.start();
    }

    /**
     * This allows a list of workers to contribute
     * @param workOrderId
     * @param marketOrder
     * @param workers
     */
    public static synchronized void allowWorkersToContribute(final String workOrderId,
                                                             final MarketOrderInterface marketOrder,
                                                             final Collection<HostInterface> workers)
            throws IOException{

        for (final HostInterface worker : workers) {
            allowWorkerToContribute(workOrderId,marketOrder, worker);
        }
    }
    /**
     * This is a blockchain event watcher automatically called on worker contribution.
     * The scheduler must ask to reveal to all workers as soon as the consensus us reached
     * @param contributeEventResponse is the blockchain worker contribution
     */
    @Override
    public void onContributeEvent(WorkerPool.ContributeEventResponse contributeEventResponse) {

//        final WorkOrderModel workOrderModel = ModelService.getInstance().getWorkOrderModel(contributeEventResponse.woid);
//        final WorkInterface theWork = DBInterface.getInstance().work(contributeEventResponse);
//        if(theWork == null)
//            return;
//
//        final String contributionStr = XWTools.byteArrayToHexString(contributeEventResponse.resultHash);
//        theWork.setH2h2r(contributionStr);
//        logger.debug("onContributeEvent() : " + theWork.toXml());
//
//        final MarketOrderInterface marketOrder = getMarketOrder(workOrderModel.getMarketorderIdx().longValue());
//        final Collection<WorkInterface> works = getMarketOrderWorks(workOrderModel.getMarketorderIdx().longValue());
//
//        if(works == null) {
//            logger.error("createWork() : can't retrieve any work for market order : "
//                    + workOrderModel.getMarketorderIdx().longValue());
//            return;
//        }
//
//        try {
//            final TaskInterface theWorkTask = DBInterface.getInstance().task(theWork);
//            final HostInterface theHost = DBInterface.getInstance().host(theWorkTask.getHost());
//            theHost.setContributed();
//            theHost.update();
//        } catch (final IOException e) {
//            logger.exception(e);
//        }
//
//        final long expectedWorkers = marketOrder.getExpectedWorkers();
//        logger.debug("onContributeEvent() : expected workers: " + expectedWorkers);
//        final long trust = marketOrder.getTrust();
//        logger.debug("onContributeEvent() : trust: " + trust);
//        final long expectedContributions = (expectedWorkers * trust / 100);
//        logger.debug("onContributeEvent() : expectedContributions: " + expectedContributions);
//        long totalContributions = 0L;
//        for(final WorkInterface work : works ) {
//            if(work.hasContributed()
//                    && (work.getH2h2r().compareTo(contributionStr) == 0)) {
//                totalContributions++;
//            }
//        }
//        if (totalContributions >= expectedContributions) {
//            logger.debug("onContributeEvent() : enough contributions");
//            theWork.setRevealing();
//            try {
//                theWork.update();
//            } catch (final IOException e) {
//                logger.exception(e);
//            }
//            logger.debug("onContributeEvent() : work must be revealed " + theWork.toXml());
//
//            for (final WorkInterface contributingWork : works) {
//
//                logger.debug("onContributeEvent() : work must be revealed " + contributingWork.toXml());
//                try {
//                    contributingWork.setRevealing();
//                    contributingWork.update();
//
//                    final TaskInterface contributingTask = DBInterface.getInstance().task(contributingWork);
//                    if (contributingTask != null) {
//                        contributingTask.setRevealing();
//                        contributingTask.update();
//                    }
//                } catch (final IOException e) {
//                    logger.exception(e);
//                }
//            }
//
//            marketOrder.setRevealing();
//            try {
//                marketOrder.update();
//            } catch(final IOException e) {
//                logger.exception(e);
//            }
//
//
//            if(actuatorService.revealConsensus(contributeEventResponse.woid, Numeric.toHexString(contributeEventResponse.resultHash)) == TransactionStatus.FAILURE) {
//                marketOrder.setErrorMsg("transaction error : revealConsensus");
//                marketOrder.setError();
//                try {
//                    marketOrder.update();
//                } catch(final IOException e) {
//                    logger.exception(e);
//                }
//            }
//
//        } else {
//            logger.debug("onContributeEvent() : not enough contributions");
//        }

    }

    @Override
    public void onReveal(final WorkerPool.RevealEventResponse revealEventResponse) {
//        final WorkOrderModel workOrderModel = ModelService.getInstance().getWorkOrderModel(revealEventResponse.woid);
//        final MarketOrderInterface marketOrder = getMarketOrder(workOrderModel.getMarketorderIdx().longValue());
//        final Collection<WorkInterface> works = getMarketOrderWorks(workOrderModel.getMarketorderIdx().longValue());
//        if (works == null) {
//            logger.error("can't find any work fot work order " + revealEventResponse.woid);
//            return;
//        }
//
//        URI result = null;
//
//        int MAX_TRY = 30;
//
//        boolean doFinalize = true;
//
//        for (final WorkInterface work : works) {
//            logger.debug("onReveal(): work: " + work.toXml());
//            try {
//                innerloop:
//                for (int count = 0; count < MAX_TRY; count++) {
//                    if (result == null) {
//                        WorkInterface workval = DBInterface.getInstance().work(work.getUID());
//                        logger.debug("onReveal(): loop: " + count);
//                        TimeUnit.SECONDS.sleep(2);
//                        result = workval.getResult();
//                    } else {
//                        logger.debug("onReveal(): result: " + result);
//                        break innerloop;
//                    }
//                }
//
//                if (work.getStatus() != StatusEnum.COMPLETED) {
//                    doFinalize = false;
//                }
//
//            } catch (final IOException | InterruptedException e) {
//                logger.exception(e);
//            }
//        }
//
//        if (!doFinalize) {
//            logger.info("not finalizing " + marketOrder.getMarketOrderIdx() + " : missing contribution");
//            return;
//        }
//        else {
//            doFinalize(revealEventResponse.woid, result, marketOrder, works, logger);
//        }
    }

    protected static synchronized void doFinalize(final String woid,
                                     final URI result,
                                     final MarketOrderInterface marketOrder,
                                     final Collection<WorkInterface> works,
                                     final Logger logger) {

        logger.debug("doFinalize(" + woid + ", "
                + result + ", "
                + marketOrder.getUID() +", "
                + (works == null ? "null" : works.size()) + ")");

        if(works == null)
            return;


        final ConsensusModel theConsensusModel = XWTools.getConsensusModel(woid);
        final Date revealDate = new Date(theConsensusModel.getRevealDate().longValue());
        final long winnerCount = theConsensusModel.getWinnerCount().longValue();
        final long revealCounter = theConsensusModel.getRevealCounter().longValue();
        final Date now = new Date();
        logger.debug("doFinalize() : consensModel.revealCounter = " + revealCounter);
        logger.debug("doFinalize() : consensModel.revealDate  = " + revealDate +
                " (now is " + now + ")");

        final boolean canFinalize = (revealCounter > 0 && revealDate.compareTo(now) >= 0) ||
                (revealCounter == winnerCount);

        logger.debug("doFinalize() : canFinalize = " + canFinalize);

        if(!canFinalize)
            return;

        for(final WorkInterface work : works ) {
            try {
                logger.debug("doFinalize() : " + work.getUID());

                final TaskInterface theWorkTask = DBInterface.getInstance().computingTask(work);
                if(theWorkTask == null) {
                    logger.debug("doFinalize() can't find any running task for the work " + work.getUID());
                    continue;
                }
                theWorkTask.setCompleted();
                theWorkTask.update(false);
                final HostInterface theHost = DBInterface.getInstance().host(theWorkTask.getHost());

                if(theHost == null) {
                    logger.error("doFinalize() can't find the host for the work " + work.getUID());
                    continue;
                }

                marketOrder.removeWorker(theHost);
                logger.debug("doFinalize() " + theHost.toXml());
                theHost.update(false);

            } catch (final IOException e) {
                logger.exception(e);
            }
        }

        try {
            marketOrder.setFinalizing();
            marketOrder.update();
        } catch(final IOException e) {
            logger.exception(e);
        }

        //Valorized stdOutCallback and stdErrCallback for finalizeWork and triggerWorkOrderCallback
        String stdOutCallback="";
        String stdErrCallback="";
        try {

          final WorkInterface work = DBInterface.getInstance().work(woid);
          final DataInterface data = work == null ?
                  null :
                  DBInterface.getInstance().data(work.getResult());
          final WorkOrderModel workOrderModel = (data != null) && (data.getType() == DataTypeEnum.TEXT) ?
                    ModelService.getInstance().getWorkOrderModel(woid) :
                    null;
          if (workOrderModel != null && !workOrderModel.getCallback().equals("0x")) { // check callback is set
                final FileInputStream finput = new FileInputStream(data.getPath());
                final DataInputStream input = new DataInputStream(finput);
                final StreamIO io = new StreamIO(null, input,false);
                final boolean isStdErr =
                         data != null &&
                         data.getName() != null &&
                         data.getName().compareTo(XWTools.STDERR) == 0;

                 String content= io.readString();
                 if(content != null && content.length() >250){
                   logger.debug("cut callback content to 250 char");
                   content=content.substring(0, 250);
                 }
                 if(isStdErr){
                   stdErrCallback =content;
                   logger.debug("valorize stdOutCallback to :"+stdErrCallback);
                 }
                 else{
                   stdOutCallback=content;
                   logger.debug("valorize stdOutCallback to :"+stdOutCallback);
                 }
            }
            else{
              logger.debug("callback is unset no stdout valorized");
            }
        } catch(final Exception e) {
            logger.error("Failed to valorized  stdoutCallback and stderrCallback");
            logger.exception(e);
        }



        if (actuatorService.finalizeWork(woid,
                stdOutCallback,
                stdErrCallback,
                result == null ? "" : result.toString()) == TransactionStatus.FAILURE) {

            logger.debug("doFinalize() : WARN:stillFinalizing");
            marketOrder.setErrorMsg("WARN:stillFinalizing");
        }
        else {
            logger.debug("doFinalize() : INFO:finalized");
            marketOrder.setCompleted();
            marketOrder.setErrorMsg("INFO:finalized");
/*
            XWTools.STDOUT
			XWTools.STDERR
            data.setType(DataTypeEnum.TEXT);
            protected WorkInterface work(final String workOrderId) {

*/
          // ISSUE add callback support https://github.com/iExecBlockchainComputing/xtremweb-hep/issues/94
          try {
              final WorkInterface work = DBInterface.getInstance().work(woid);
              final DataInterface data = work == null ?
                    null :
                    DBInterface.getInstance().data(work.getResult());
              final WorkOrderModel workOrderModel = (data != null) && (data.getType() == DataTypeEnum.TEXT) ?
                        ModelService.getInstance().getWorkOrderModel(woid) :
                        null;
              if (workOrderModel != null && !workOrderModel.getCallback().equals("0x")) { // check callback is set
                  ActuatorService.getInstance().triggerWorkOrderCallback(woid,
                              stdOutCallback,
                              stdErrCallback,
                              result == null ? "" : result.toString());

               }
            } catch(IOException e) {
                logger.exception(e);
            }
        }
//        final long revealingDate = marketOrder.getRevealingDate().getTime();
//        final long now = new Date().getTime();
//        if(now - revealingDate > (Dispatcher.getConfig().getInt(XWPropertyDefs.REVEALTIMEOUTMULTIPLICATOR)
//                * Dispatcher.getConfig().getTimeout())) {
//            marketOrder.setError();
//            marketOrder.setErrorMsg("ERROR:finalizationTimeOut");
//            logger.debug("doFinalize() : ERROR:finalizationTimeOut");
//        }
        try {
            marketOrder.update();
        } catch(final IOException e) {
            logger.exception(e);
        }
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
