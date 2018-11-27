package xtremweb.worker;


import com.iexec.common.ethereum.TransactionStatus;
import com.iexec.common.ethereum.Utils;
import com.iexec.worker.actuator.ActuatorService;
import com.iexec.worker.iexechub.IexecHubService;
import com.iexec.worker.iexechub.IexecHubWatcher;
import com.iexec.worker.workerpool.WorkerPoolService;
import com.iexec.worker.workerpool.WorkerPoolWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtremweb.common.XWPropertyDefs;

import java.math.BigInteger;

public class WorkerPocoWatcherImpl implements WorkerPoolWatcher, IexecHubWatcher {

    private final static IexecHubService iexecHubService = IexecHubService.getInstance();
    private final static WorkerPoolService workerPoolService = WorkerPoolService.getInstance();
    private final static ActuatorService actuatorService = ActuatorService.getInstance();
    private static final Logger log = LoggerFactory.getLogger(WorkerPocoWatcherImpl.class);

    public WorkerPocoWatcherImpl() {
        //iexecHubService.registerIexecHubWatcher(this);
        //workerPoolService.registerWorkerPoolWatcher(this);

        //actuatorService.depositRlc();

        TransactionStatus status = TransactionStatus.FAILURE;
        while (status == TransactionStatus.FAILURE ) {
            status = actuatorService.subscribeToPool();
            log.info("Subscribing to pool " + status);
            if (status==TransactionStatus.FAILURE) {
                try {
                    log.info("Subscribing to pool will retry in 10s");
                    Thread.sleep(10000);
                } catch(final InterruptedException e) {
                }
            }
        }
        Worker.getConfig().setProperty(XWPropertyDefs.SUBSCRIBEDTOPOOL, "true");
    }

    @Override
    public void onAllowWorkerToContribute(String workOrderId) {
        //actuatorService.contribute(workOrderId, "iExec the wanderer", BigInteger.ZERO, "0", "O");
    }

    @Override
    public void onRevealConsensus(String workOrderId) {
        //actuatorService.reveal(workOrderId, "iExec the wanderer");
    }

    @Override
    public void onWorkOrderCompleted(String workOrderId) {
        //got some RLC
    }
}
