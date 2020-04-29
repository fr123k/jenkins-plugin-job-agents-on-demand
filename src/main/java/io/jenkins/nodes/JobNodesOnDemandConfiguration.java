package io.jenkins.nodes;

import hudson.Extension;
import hudson.model.Label;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class JobNodesOnDemandConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static JobNodesOnDemandConfiguration get() {
        return GlobalConfiguration.all().get(JobNodesOnDemandConfiguration.class);
    }

    private boolean enabled = false;
    private String agentProvisionJob, agentDecomissionJob, agentImage, agentWorkDir, gitRevision, gitUrl, revision;

    public JobNodesOnDemandConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public boolean isEnabled() { return enabled; }
    @DataBoundSetter public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }

    public String getGitUrl() { return gitUrl; }
    @DataBoundSetter public void setGitUrl(String gitUrl) { this.gitUrl = gitUrl; save(); }

    public FormValidation doCheckGitUrl(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a gitUrl.");
        }
        return FormValidation.ok();
    }

    public String getGitRevision() { return gitRevision; }
    @DataBoundSetter public void setGitRevision(String gitRevision) { this.gitRevision = gitRevision; save(); }

    public FormValidation doCheckGitRevision(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a gitRevision.");
        }
        return FormValidation.ok();
    }

    public String getRevision() { return revision; }
    @DataBoundSetter public void setRevision(String revision) { this.revision = revision; save(); }

    public FormValidation doCheckRevision(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a gitRevision.");
        }
        return FormValidation.ok();
    }

    public String getAgentProvisionJob() { return agentProvisionJob; }
    @DataBoundSetter public void setAgentProvisionJob(String agentProvisionJob) { this.agentProvisionJob = agentProvisionJob; save(); }

    public FormValidation doCheckAgentProvisionJob(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentProvisionJob.");
        }
        return FormValidation.ok();
    }

    public String getAgentDecomissionJob() { return agentDecomissionJob; }
    @DataBoundSetter public void setAgentDecomissionJob(String agentDecomissionJob) { this.agentDecomissionJob = agentDecomissionJob; save(); }

    public FormValidation doCheckAgentDecomissionJob(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentDecomissionJob.");
        }
        return FormValidation.ok();
    }

    public String getAgentImage() { return agentImage; }
    @DataBoundSetter public void setAgentImage(String agentImage) { this.agentImage = agentImage; save(); }

    public FormValidation doCheckAgentImage(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentImage.");
        }
        if (StringUtils.containsNone(value, "{label}")) {
            return FormValidation.warning("Please specify the '{label}' placeholder.");
        }
        return FormValidation.ok();
    }

    public String toAgentImage(Label label) {
        return this.agentImage.replace("{label}", label.toString());
    }

    public String getAgentWorkDir() { return agentWorkDir; }
    @DataBoundSetter public void setAgentWorkDir(String agentWorkDir) { this.agentWorkDir = agentWorkDir; save(); }

    public FormValidation doCheckAgentWorkDir(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentImage.");
        }
        return FormValidation.ok();
    }
}
