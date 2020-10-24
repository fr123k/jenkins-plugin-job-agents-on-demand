package com.github.fr123k.agents;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
/**
 * Example of Jenkins global configuration.
 */
@Extension
public class JobNodesOnDemandConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static JobNodesOnDemandConfiguration get() {
        return GlobalConfiguration.all().get(JobNodesOnDemandConfiguration.class);
    }

    private transient Set<LabelAtom> excludeLabelSet;

    private boolean enabled = false;
    private String agentProvisionJob, agentDecomissionJob, agentImage, agentWorkDir, excludeLabes, gitRevision, gitUrl, revision;

    private Integer maxNodesPerLabel = 5, maxIdleTime = 10;

    public JobNodesOnDemandConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public boolean isEnabled() { return enabled; }
    @DataBoundSetter public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }

    public String getGitUrl() { return gitUrl; }
    public String toGitUrl(Label label) { return replaceLabel(this.gitUrl, label.toString()); }
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
    public String toAgentProvisionJob(Label label) { return replaceLabel(this.agentProvisionJob, label.toString()); }

    @DataBoundSetter public void setAgentProvisionJob(String agentProvisionJob) { this.agentProvisionJob = agentProvisionJob; save(); }

    public FormValidation doCheckAgentProvisionJob(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentProvisionJob.");
        }
        return FormValidation.ok();
    }

    public String getAgentDecomissionJob() { return agentDecomissionJob; }
    public String toAgentDecomissionJob(Label label) { return replaceLabel(this.agentDecomissionJob, label.toString()); }

    @DataBoundSetter public void setAgentDecomissionJob(String agentDecomissionJob) { this.agentDecomissionJob = agentDecomissionJob; save(); }

    public FormValidation doCheckAgentDecomissionJob(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentDecomissionJob.");
        }
        return FormValidation.ok();
    }

    public String getAgentImage() { return agentImage; }
    public String toAgentImage(Label label) { return replaceLabel(this.agentImage, label.toString()); }
    @DataBoundSetter public void setAgentImage(String agentImage) { this.agentImage = agentImage; save(); }

    public FormValidation doCheckAgentImage(@QueryParameter String value) {
        if (StringUtils.isNotEmpty(value)) {
            if (StringUtils.containsNone(value, "{label}")) {
                return FormValidation.warning("Please specify the '{label}' placeholder.");
            }
        }
        return FormValidation.ok();
    }

    public String getAgentWorkDir() { return agentWorkDir; }
    @DataBoundSetter public void setAgentWorkDir(String agentWorkDir) { this.agentWorkDir = agentWorkDir; save(); }

    public FormValidation doCheckAgentWorkDir(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a agentWorkDir.");
        }
        return FormValidation.ok();
    }

    public Integer getMaxNodesPerLabel() { return maxNodesPerLabel; }
    @DataBoundSetter public void setMaxNodesPerLabel(Integer maxNodesPerLabel) { this.maxNodesPerLabel = maxNodesPerLabel; save(); }

    public FormValidation doMaxNodesPerLabel(@QueryParameter Integer value) {
        return FormValidation.ok();
    }

    public Integer getMaxIdleTime() { return maxIdleTime; }
    @DataBoundSetter public void setMaxIdleTime(Integer maxIdleTime) { this.maxIdleTime = maxIdleTime; save(); }

    public FormValidation doMaxIdleTime(@QueryParameter Integer value) {
        return FormValidation.ok();
    }

    public String getExcludeLabels() { return excludeLabes; }
    @DataBoundSetter public void setExcludeLabels(String excludeLabes) { this.excludeLabes = excludeLabes; save(); }

    public FormValidation doCheckExcludeLabels(@QueryParameter String value) {
        return FormValidation.ok();
    }
    public Set<LabelAtom> toExcludeLabelSet() {
        if (excludeLabelSet == null) {
            excludeLabelSet = Label.parse(this.excludeLabes);
        }
        return excludeLabelSet;
    }

    public static String replaceLabel(String value, String label) {
        Pattern regex = Pattern.compile("\\{(.*)\\:label\\}");
        Matcher matcher = regex.matcher(value);
        if (matcher.find()) {
            String delim = matcher.group(1);
            return value.replace(matcher.group(0), delim + label);

        }

        return value.replace("{label}", label);
    }
}
