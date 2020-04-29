package org.fr123k.jenkins.plugins.agents;

import static java.util.logging.Level.INFO;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Strings;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

public class JobComputerLauncher extends JNLPLauncher {
    private static final Logger LOGGER = Logger.getLogger(JobComputerLauncher.class.getName());

    private final String gitUrl, gitRevision, label, image, provisionJobName, decomissionJobName;

    public JobComputerLauncher(final String gitUrl, final String gitRevision, final String label, final String image,
            final String provisionJobName, final String decomissionJobName) {
        super(true);
        this.gitUrl = gitUrl;
        this.gitRevision = gitRevision;
        this.label = label;
        this.image = image;
        this.provisionJobName = provisionJobName;
        this.decomissionJobName = decomissionJobName;
    }

    @Override
    public boolean isLaunchSupported() {
        return true;
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener) {
        final Jenkins jenkins = Jenkins.get();
        LOGGER.log(INFO, "Launch agent {0}.", new Object[] { computer.getName() });

        List<ParameterValue> parameters = new ArrayList<>();

        addParam(parameters, "node", computer.getName());
        addParam(parameters, "label", label);
        addParam(parameters, "revision", gitRevision);
        addParam(parameters, "gitUrl", gitUrl);
        addParam(parameters, "image", image);
        addParam(parameters, "gitRevision", gitRevision);

        ParameterizedJob job = jenkins.getItemByFullName(provisionJobName, ParameterizedJob.class);

        if (job == null) {
            throw new RuntimeException(
                    "The agent provision job '" + provisionJobName + "' of type 'ParameterizedJob.class' was not found!");
        }
        LOGGER.log(INFO, "Trigger job {0} for agent {1}.", new Object[] { job, computer.getName() });
        job.scheduleBuild2(1, new ParametersAction(parameters));
    }

    private static List<ParameterValue> addParam(List<ParameterValue> parameters, String name, String value) {
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
     * This method is invoked after the {@link Channel} to this computer is terminated.
     *
     * <p>
     * Disconnect operation is performed asynchronously, so there's no guarantee
     * that the corresponding {@link SlaveComputer} exists for the duration of the
     * operation.
     */
    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        final Jenkins jenkins = Jenkins.get();
        LOGGER.log(INFO, "Stop agent {0}.", new Object[] { computer.getName() });

        List<ParameterValue> parameters = new ArrayList<>();
        addParam(parameters, "agentID", computer.getName());
        addParam(parameters, "label", label);

        ParameterizedJob job = jenkins.getItemByFullName(decomissionJobName, ParameterizedJob.class);

        if (job == null) {
            throw new RuntimeException(
                    "The agent provision job '" + decomissionJobName + "' of type 'ParameterizedJob.class' was not found!");
        }
        LOGGER.log(INFO, "Trigger job {0} for agent {1}.", new Object[] { job, computer.getName() });
        job.scheduleBuild2(1, new ParametersAction(parameters));
    }

    // @Extension
    // @Symbol({"jobAgent"})
    // public static class DescriptorImpl extends Descriptor<ComputerLauncher> {

    // /**
    // * {@inheritDoc}
    // */
    // public String getDisplayName() {
    // return "jobAgent";
    // }

    // public FormValidation doCheckAgentJob(@QueryParameter String value) {
    // // if (StringUtils.isEmpty(value)) {
    // // return FormValidation.error(Messages.SSHLauncher_PortNotSpecified());
    // // }
    // // try {
    // // int portValue = Integer.parseInt(value);
    // // if (portValue <= 0) {
    // // return FormValidation.error(Messages.SSHLauncher_PortLessThanZero());
    // // }
    // // if (portValue >= 65536) {
    // // return FormValidation.error(Messages.SSHLauncher_PortMoreThan65535());
    // // }
    // return FormValidation.ok();
    // // } catch (NumberFormatException e) {
    // // return FormValidation.error(e, Messages.SSHLauncher_PortNotANumber());
    // // }
    // }

    // public FormValidation doCheckgitUrlUrl(@QueryParameter String value) {
    // FormValidation ret = FormValidation.ok();
    // // if (StringUtils.isEmpty(value)) {
    // // return FormValidation.error(Messages.SSHLauncher_HostNotSpecified());
    // // }
    // return ret;
    // }
    // }
}
