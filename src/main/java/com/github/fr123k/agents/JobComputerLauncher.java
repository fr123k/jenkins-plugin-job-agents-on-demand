package com.github.fr123k.agents;

import static java.util.logging.Level.INFO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import com.google.common.base.Strings;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

public class JobComputerLauncher extends JNLPLauncher {
    private static final Logger LOGGER = Logger.getLogger(JobComputerLauncher.class.getName());

    private String gitUrl, gitRevision, label, image, provisionJobName, decomissionJobName, revision;

    @DataBoundConstructor
    public JobComputerLauncher(final String gitUrl, final String revision, final String label, final String image, final String provisionJobName, final String decomissionJobName, final String gitRevision) {
        super(true);
        this.gitUrl = gitUrl;
        this.revision = revision;
        this.label = label;
        this.image = image;
        this.provisionJobName = provisionJobName;
        this.decomissionJobName = decomissionJobName;
        this.gitRevision = gitRevision;
    }

    @Override
    public boolean isLaunchSupported() {
        return true;
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener) {
        final Jenkins jenkins = Jenkins.get();
        LOGGER.log(INFO, "Launch agent {0}.", new Object[] { computer.getName() });
        print(listener, "Launch agent %s.", computer.getName());

        final List<ParameterValue> parameters = new ArrayList<>();

        addParam(parameters, "node", computer.getName());
        addParam(parameters, "label", label);
        addParam(parameters, "revision", revision);
        addParam(parameters, "gitUrl", gitUrl);
        addParam(parameters, "image", image);
        addParam(parameters, "gitRevision", gitRevision);

        final ParameterizedJob job = jenkins.getItemByFullName(provisionJobName, ParameterizedJob.class);

        if (job == null) {
            throw new RuntimeException("The agent provision job '" + provisionJobName
                    + "' of type 'ParameterizedJob.class' was not found!");
        }
        LOGGER.log(INFO, "Trigger job {0} for agent {1}.", new Object[] { job.getDisplayName(), computer.getName() });
        print(listener, "Trigger job %s for agent %s.", job.getDisplayName(), computer.getName());
        final QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0, new ParametersAction(parameters));
        try {
            if (build != null) {
                WorkflowRun buildRun = build.get();
                print(listener, "Triggered Build: %s", buildRun.getUrl());
                writeLog(buildRun, listener);
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            listener.error("%s ocuured with message %s", e.getClass(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void print(TaskListener listener, String message, Object... args) {
        listener.getLogger().println(String.format(message, args));
    }

    private static void writeLog(WorkflowRun workflowRun, TaskListener listener) throws IOException {
        long logFileLength = workflowRun.getLogText().length();
        long pos = 0;
        while (pos < logFileLength) {
            try {
                pos = workflowRun.getLogText().writeLogTo(pos, listener.getLogger());
            } catch (IOException e) {
                LOGGER.log(INFO, "", e);
            }
        }
    }

    private static List<ParameterValue> addParam(final List<ParameterValue> parameters, final String name,
            final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return parameters;
        }
        parameters.add(new StringParameterValue(name, value));
        return parameters;
    }

    /**
     * Allows the {@link ComputerLauncher} to tidy-up after a disconnect.
     *
     * <p>
     * This method is invoked after the {@link Channel} to this computer is
     * terminated.
     *
     * <p>
     * Disconnect operation is performed asynchronously, so there's no guarantee
     * that the corresponding {@link SlaveComputer} exists for the duration of the
     * operation.
     */
    @Override
    public void afterDisconnect(final SlaveComputer computer, final TaskListener listener) {
        if (!computer.isAcceptingTasks()) {
            LOGGER.log(INFO, "Decomission of agent {0} is in progress.", new Object[]{computer.getName()});
            return;
        }
        LOGGER.log(INFO, "Stop agent {0}.", new Object[]{computer.getName()});
        final Jenkins jenkins = Jenkins.get();
        computer.setAcceptingTasks(false);
        computer.recordTermination();
        
        final List<ParameterValue> parameters = new ArrayList<>();
        addParam(parameters, "node", computer.getName());
        addParam(parameters, "label", label);
        addParam(parameters, "revision", revision);

        final ParameterizedJob job = jenkins.getItemByFullName(decomissionJobName, ParameterizedJob.class);

        if (job == null) {
            throw new RuntimeException("The agent provision job '" + decomissionJobName
                    + "' of type 'ParameterizedJob.class' was not found!");
        }
        LOGGER.log(INFO, "Trigger job {0} for agent {1}.", new Object[] { job.getDisplayName(), computer.getName() });
        final QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0, new ParametersAction(parameters));
        try {
            if (build != null) {
                WorkflowRun buildRun = build.get();
                print(listener, "Triggered Build: %s", buildRun.getUrl());
                writeLog(buildRun, listener);
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            computer.setAcceptingTasks(true);
            listener.error("%s ocuured with message %s", e.getClass(), e.getMessage());
            throw new RuntimeException(e);
        }        

        try {
            Node node;
            if ((node = computer.getNode()) != null) {
                Jenkins.get().removeNode(node);
            }
        } catch (IllegalStateException | IOException e) {
            listener.error("%s ocuured with message %s", e.getClass(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        return Jenkins.get().getDescriptor(JobComputerLauncher.class);
    }

    @Extension 
    @Symbol("jobAgent")
    public static class JobAgentDescriptorImpl extends Descriptor<ComputerLauncher> {
        public JobAgentDescriptorImpl() {
        }

        public String getDisplayName() {
            return "JobAgentLauncher";
        }
        
        // /**
        //  * Checks if Work Dir settings should be displayed.
        //  * 
        //  * This flag is checked in {@code config.jelly} before displaying the 
        //  * {@link JNLPLauncher#workDirSettings} property.
        //  * By default the configuration is displayed only for {@link JNLPLauncher},
        //  * but the implementation can be overridden.
        //  * @return {@code true} if work directories are supported by the launcher type.
        //  * @since 2.73
        //  */
        // public boolean isWorkDirSupported() {
        //     // This property is included only for JNLPLauncher by default. 
        //     // Causes JENKINS-45895 in the case of includes otherwise
        //     return true;
        // }

        // public FormValidation doCheckWebSocket(@QueryParameter boolean webSocket, @QueryParameter String tunnel) {
        //     if (webSocket) {
        //         return FormValidation.error("WebSocket are not supported in this Launcher");
        //     } else {
        //         if (Jenkins.get().getTcpSlaveAgentListener() == null) {
        //             return FormValidation.error("Either WebSocket mode is selected, or the TCP port for inbound agents must be enabled");
        //         }
        //     }
        //     return FormValidation.ok();
        // }
    }
}
