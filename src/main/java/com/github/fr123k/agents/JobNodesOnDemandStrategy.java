package com.github.fr123k.agents;

import static hudson.slaves.NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
import static hudson.slaves.NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.model.Queue;
import hudson.model.LoadStatistics.LoadStatisticsSnapshot;
import hudson.model.queue.QueueListener;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.Strategy;
import hudson.slaves.NodeProvisioner.StrategyDecision;
import jenkins.model.Jenkins;

/**
 * Based on
 * https://github.com/jenkinsci/one-shot-executor-plugin/blob/master/src/main/java/org/jenkinsci/plugins/oneshot/OneShotProvisionerStrategy.java
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JobNodesOnDemandStrategy extends Strategy {

    private static final Logger LOGGER = Logger.getLogger(JobNodesOnDemandStrategy.class.getName());

    @Nonnull
    @Override
    public StrategyDecision apply(@Nonnull NodeProvisioner.StrategyState state) {
        if (Jenkins.get().isQuietingDown()) {
            LOGGER.log(INFO, "label: {0} Skip dynamic agent creation because jenkins is marked for shuting down.", new Object[]{state.getLabel()});
            return CONSULT_REMAINING_STRATEGIES;
        }

        return applyStrategy(state);
    }

    private StrategyDecision applyStrategy(@Nonnull NodeProvisioner.StrategyState state) {
        JobNodesOnDemandConfiguration config = JobNodesOnDemandConfiguration.get();
        final Label label = state.getLabel();
        if (!config.isEnabled()) {
            LOGGER.log(INFO, "label: {0} Skip dynamic agent creation because it's disabled (check configuration).", new Object[]{label});
            return CONSULT_REMAINING_STRATEGIES;
        }

        if (label == null || label.matches(config.toExcludeLabelSet())) {
            LOGGER.log(INFO, "label: {0} null or match excluded labels.", new Object[]{state.getLabel()});
            return CONSULT_REMAINING_STRATEGIES;
        }

        LoadStatistics.LoadStatisticsSnapshot snapshot = state.getSnapshot();
        LOGGER.log(INFO, "label: {0} Available executors={1}, connecting={2}, planned={3}",
                new Object[] { label, snapshot.getAvailableExecutors(), snapshot.getConnectingExecutors(),
                        state.getPlannedCapacitySnapshot() });
        int availableCapacity = getAvailableExecutors(snapshot) + state.getPlannedCapacitySnapshot();

        int currentDemand = snapshot.getQueueLength();
        LOGGER.log(INFO, "label: {0} Available capacity={1}, currentDemand={2}",
                new Object[] { label, availableCapacity, currentDemand });
        if (availableCapacity >= config.getMaxNodesPerLabel()) {
            LOGGER.log(INFO, "label: {0} maximum agents of {1} reached.", new Object[]{label, config.getMaxNodesPerLabel()});
            return PROVISIONING_COMPLETED;
        }
        if (availableCapacity < currentDemand) {
            NodeProvisioner.PlannedNode plannedNode = new AgentsJob().provision(label, 1);
            state.recordPendingLaunches(plannedNode);
            availableCapacity++;
            LOGGER.log(INFO, "label: {0} After provisioning, available capacity={1}, currentDemand={2}",
                    new Object[] { label, availableCapacity, currentDemand });
        }

        if (availableCapacity >= currentDemand) {
            LOGGER.log(INFO, "label: {0} Provisioning completed", new Object[]{label});
            return PROVISIONING_COMPLETED;
        }

        LOGGER.log(INFO, "label: {0} Provisioning not complete, consulting remaining strategies", new Object[]{label});
        return CONSULT_REMAINING_STRATEGIES;
    }

    public static int getAvailableExecutors(LoadStatisticsSnapshot snapshot) {
        // if (snapshot.getDefinedExecutors() > 0 && snapshot.getOnlineExecutors() == 0 ) {
        //     return snapshot.getOnlineExecutors();
        // }
        int available = snapshot.getAvailableExecutors() + snapshot.getConnectingExecutors();
        return Math.max(available, snapshot.getDefinedExecutors());
    }

    /**
     * Ping the nodeProvisioner as a new task enters the queue, so it can provision a new agent without delay.
     */
    @Extension
    public static class FastProvisionning extends QueueListener {

        @Override
        public void onEnterBuildable(Queue.BuildableItem item) {
            final Jenkins jenkins = Jenkins.get();
            final Label label = item.getAssignedLabel();
            final NodeProvisioner provisioner = (label == null
                    ? jenkins.unlabeledNodeProvisioner
                    : label.nodeProvisioner);

            LOGGER.log(FINE, "Check provisioner onEnterBuildable {0} for label {1}", new Object[]{provisioner.getClass().getName(), label});
            provisioner.suggestReviewNow();
        }

        @Override
        public void onEnterWaiting(Queue.WaitingItem item) {
            final Jenkins jenkins = Jenkins.get();
            final Label label = item.getAssignedLabel();
            final NodeProvisioner provisioner = (label == null
                    ? jenkins.unlabeledNodeProvisioner
                    : label.nodeProvisioner);

            LOGGER.log(FINE, "Check provisioner onEnterWaiting {0} for label {1}", new Object[]{provisioner.getClass().getName(), label});
            provisioner.suggestReviewNow();
        }
    }
}
