package xtremweb.worker;


import com.iexec.worker.actuator.ActuatorService;
import com.iexec.worker.iexechub.IexecHubService;
import com.iexec.worker.iexechub.IexecHubWatcher;
import com.iexec.worker.workerpool.WorkerPoolService;
import com.iexec.worker.workerpool.WorkerPoolWatcher;

import java.math.BigInteger;

public class WorkerPocoWatcherImpl implements WorkerPoolWatcher, IexecHubWatcher {

    private final static IexecHubService iexecHubService = IexecHubService.getInstance();
    private final static WorkerPoolService workerPoolService = WorkerPoolService.getInstance();
    private final static ActuatorService actuatorService = ActuatorService.getInstance();

    public WorkerPocoWatcherImpl() {
        iexecHubService.registerIexecHubWatcher(this);
        workerPoolService.registerWorkerPoolWatcher(this);
        actuatorService.subscribeToPool();
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
