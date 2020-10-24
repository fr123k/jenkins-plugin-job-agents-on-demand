package com.github.fr123k.agents;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import com.google.common.base.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.Node.Mode;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

/**
 *
 * @author fr123k
 */
public class AgentsJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentsJob.class);

    public AgentsJob() {}

    public synchronized NodeProvisioner.PlannedNode provision(final Label label, final int numberOfExecutorsRequired) {
        try {
            LOGGER.info("Asked to provision {} node(s) for: {}", numberOfExecutorsRequired, label);
            CreateNodeTask taskToCreateNewNode = new CreateNodeTask(label, JobNodesOnDemandConfiguration.get(), numberOfExecutorsRequired);
            NodeProvisioner.PlannedNode plannedNode = new NodeProvisioner.PlannedNode(label.toString(), taskToCreateNewNode.getFuture(), numberOfExecutorsRequired);
            Computer.threadPoolForRemoting.submit(taskToCreateNewNode);       
            return plannedNode;
        } catch (final Exception e) {
            LOGGER.error("Exception while provisioning for label: '{}'", label, e);
            return null;
        }
    }

    /**
     * Workaround for Jenkins core issue in Nodes.java. There's a line there saying
     * "<i>TODO there is a theoretical race whereby the node instance is
     * updated/removed after lock release</i>". When we're busy adding nodes this is
     * not merely "theoretical"!
     * 
     * @see <a href=
     *      "https://github.com/jenkinsci/jenkins/blob/d2276c3c9b16fd46a3912ab8d58c418e67d8ce3e/core/src/main/java/jenkins/model/Nodes.java#L141">
     *      Nodes.java</a>
     * 
     * @param slave The slave to be added to Jenkins
     * @throws IOException if it all failed horribly every time we tried.
     */
    private static void robustlyAddNodeToJenkins(final Slave slave) throws IOException {
        // don't retry getInstance - fail immediately if that fails.
        final Jenkins jenkins = Jenkins.get();
        final int maxAttempts = 10;
        for (int attempt = 1;; attempt++) {
            try {
                // addNode can fail at random due to a race condition.
                jenkins.addNode(slave);
                return;
            } catch (IOException | RuntimeException ex) {
                if (attempt > maxAttempts) {
                    throw ex;
                }
                final long delayInMilliseconds = 10L * attempt;
                try {
                    Thread.sleep(delayInMilliseconds);
                } catch (final InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    private static class CreateNodeTask implements Runnable {

        private Label label;
        private JobNodesOnDemandConfiguration config;
        private int numberOfExecutors;
        private CompletableFuture<Node> future;
        public CreateNodeTask(Label label, JobNodesOnDemandConfiguration config, int numberOfExecutors) {
            this.label = label;
            this.config = config;
            this.numberOfExecutors = numberOfExecutors;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void run() {
            Slave node = null;
            try {
                final String nodeName = label + "-" + UUID.randomUUID().toString().substring(0, 6);
                node = new DumbSlave(nodeName, config.getAgentWorkDir(), getLauncher(label));
                node.setNodeDescription("Agent [" + label + "]");
                node.setLabelString(label.toString());
                node.setMode(Mode.EXCLUSIVE);
                node.setNumExecutors(numberOfExecutors);
                //TODO make retention strategy configurable
                // node.setRetentionStrategy(RetentionStrategy.Always.INSTANCE);
                node.setRetentionStrategy(new RetentionStrategy.Demand(0, new Random().nextInt(config.getMaxIdleTime())));
                future.complete(node);

                robustlyAddNodeToJenkins(node);
            } catch (final Exception ex) {
                LOGGER.error("Error in provisioning!", ex);
                future.completeExceptionally(ex);
                Computer computer = (node == null)? null : node.toComputer();
                if (computer != null) {
                    computer.disconnect(new OfflineCause.LaunchFailed());
                }
                throw Throwables.propagate(ex);
            }
            return;
        }

        public Future<Node> getFuture() {
            return future;
        }

        private static ComputerLauncher getLauncher(Label label) {

            final Jenkins jenkins = Jenkins.get();
            JobNodesOnDemandConfiguration config = JobNodesOnDemandConfiguration.get();

            String agentProvisionJobName = config.toAgentProvisionJob(label);
        
            final ParameterizedJob job = jenkins.getItemByFullName(agentProvisionJobName, ParameterizedJob.class);

            if (job == null) {
                agentProvisionJobName = config.getAgentProvisionJob();
            }

            return new JobComputerLauncher(
                config.toGitUrl(label), 
                config.getRevision(),
                label.toString(),
                config.toAgentImage(label),
                agentProvisionJobName,
                config.toAgentDecomissionJob(label),
                config.getGitRevision());
        }
    };
}
