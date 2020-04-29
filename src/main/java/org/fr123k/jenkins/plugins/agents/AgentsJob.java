package org.fr123k.jenkins.plugins.agents;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import io.jenkins.nodes.JobNodesOnDemandConfiguration;
import jenkins.model.Jenkins;

/**
 * Docker Cloud configuration. Contains connection configuration,
 * {@link DockerTemplate} contains configuration for running docker image.
 *
 * @author magnayn
 */
public class AgentsJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentsJob.class);

    public AgentsJob() {
    }

    public synchronized NodeProvisioner.PlannedNode provision(final Label label, final int numberOfExecutorsRequired) {
        try {
            LOGGER.info("Asked to provision {} slave(s) for: {}", numberOfExecutorsRequired, label);
            
            final CompletableFuture<Node> future = new CompletableFuture<>();
            NodeProvisioner.PlannedNode plannedNode = new NodeProvisioner.PlannedNode(label.toString(), future, numberOfExecutorsRequired);
            JobNodesOnDemandConfiguration config = JobNodesOnDemandConfiguration.get();

            Slave node = null;
            try {
                final String nodeName = label + "-" + UUID.randomUUID().toString().substring(0, 6);
                node = new DumbSlave(nodeName, config.getAgentWorkDir(), getLauncher(label));
                node.setNodeDescription("Agent [" + label + "]");
                node.setLabelString(label.toString());
                node.setRetentionStrategy(RetentionStrategy.Always.INSTANCE);
                future.complete(node);

                // On provisioning completion, let's trigger NodeProvisioner
                robustlyAddNodeToJenkins(node);

            } catch (final Exception ex) {
                LOGGER.error("Error in provisioning!", ex);
                future.completeExceptionally(ex);
                if (node != null) {
                    node.toComputer().disconnect(new OfflineCause.LaunchFailed());
                }
                throw Throwables.propagate(ex);
            }
            return plannedNode;
        } catch (final Exception e) {
            LOGGER.error("Exception while provisioning for label: '{}'", label, e);
            return null;
        }
    }

    private static ComputerLauncher getLauncher(Label label) {
        JobNodesOnDemandConfiguration config = JobNodesOnDemandConfiguration.get();
        return new JobComputerLauncher(
            config.getGitUrl(), 
            config.getGitRevision(),
            label.toString(),
            config.toAgentImage(label),
            config.getAgentProvisionJob(),
            config.getAgentDecomissionJob());
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
}
