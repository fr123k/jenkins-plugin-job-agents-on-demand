package io.jenkins.nodes;

import static hudson.slaves.NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
import static hudson.slaves.NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
import static java.util.logging.Level.INFO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.fr123k.jenkins.plugins.agents.AgentsJob;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.Strategy;
import hudson.slaves.NodeProvisioner.StrategyDecision;
import jenkins.model.Jenkins;

/**
 * Based on https://github.com/jenkinsci/one-shot-executor-plugin/blob/master/src/main/java/org/jenkinsci/plugins/oneshot/OneShotProvisionerStrategy.java
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
            return CONSULT_REMAINING_STRATEGIES;
        }
        LOGGER.log(INFO, "apply JobNodesOnDemandStrategy");

        applyFoCloud(state);
        return CONSULT_REMAINING_STRATEGIES;
    }

    private StrategyDecision applyFoCloud(@Nonnull NodeProvisioner.StrategyState state) {
        JobNodesOnDemandConfiguration config = JobNodesOnDemandConfiguration.get();
        if (!config.isEnabled()) {
            LOGGER.log(INFO, "Skip dynamic agent creation becuae it's not enabled.");
            return CONSULT_REMAINING_STRATEGIES;
        }

        final Label label = state.getLabel();
        if (label == null) {
            return CONSULT_REMAINING_STRATEGIES;
        }

        

        // //TODO add support for multiple agents for the same label
        // if (getLabels().contains(label.toString())) {
        //     LOGGER.log(INFO, "Skip agent creation label {0} already has agents.", new Object[]{label});
        //     return CONSULT_REMAINING_STRATEGIES;
        // }

        for (Node node : label.getNodes()) {
            if (node.isAcceptingTasks()) {
                LOGGER.log(INFO, "Skip agent creation label {0} already has agents.", new Object[]{label});
                return CONSULT_REMAINING_STRATEGIES;
            }
        }

        LoadStatistics.LoadStatisticsSnapshot snapshot = state.getSnapshot();
        LOGGER.log(INFO, "Available executors={0}, connecting={1}, planned={2}",
        new Object[]{snapshot.getAvailableExecutors(), snapshot.getConnectingExecutors(), state.getPlannedCapacitySnapshot()});
        int availableCapacity =
        snapshot.getAvailableExecutors()
        + snapshot.getConnectingExecutors()
        + state.getPlannedCapacitySnapshot();
        
        int currentDemand = snapshot.getQueueLength();
        LOGGER.log(INFO, "Available capacity={0}, currentDemand={1}",
        new Object[]{availableCapacity, currentDemand});
        

        NodeProvisioner.PlannedNode plannedNode = new AgentsJob().provision(label, 2);

        state.recordPendingLaunches(plannedNode);
        availableCapacity++;
        LOGGER.log(INFO, "After provisioning, available capacity={0}, currentDemand={1}",
                new Object[]{availableCapacity, currentDemand});

        if (availableCapacity >= currentDemand) {
            LOGGER.log(INFO, "Provisioning completed");
            return PROVISIONING_COMPLETED;
        }
        LOGGER.log(INFO, "Provisioning not complete, consulting remaining strategies");
        return CONSULT_REMAINING_STRATEGIES;
    }

    private static Set<String> getLabels() {
        List<Node> nodes = Jenkins.get().getNodes();
        Set<String> labels = new HashSet<>(nodes.size());
        for(Node node : nodes) {
            labels.add(node.getLabelString());
        }
        return labels;
    }

    /**
     * Ping the nodeProvisioner as a new task enters the queue, so it can provision a DockerSlave without delay.
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

            LOGGER.log(INFO, "Check provisioner {0} for label {1}", new Object[]{provisioner.getClass().getName(), label});
            provisioner.suggestReviewNow();
        }
    }
}
