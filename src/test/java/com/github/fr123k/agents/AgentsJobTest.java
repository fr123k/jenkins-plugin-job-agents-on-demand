package com.github.fr123k.agents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.ParametersAction;
import hudson.model.Slave;
import hudson.model.StringParameterValue;
import hudson.remoting.Channel;

public class AgentsJobTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private WorkflowJob agentBootstrapJob;

    @Before
    public void setup() throws IOException, URISyntaxException {
        System.out.println("Slave: " + new Slave.JnlpJar("slave.jar").getURL());
        String jenkinsURL = j.getURL() + "computer/${params.node}/slave-agent.jnlp";
        String agentCmd = "/usr/local/openjdk-8/jre/bin/java -jar /usr/src/agents-ondemand/target/jenkins-for-test/WEB-INF/lib/agent.jar -jnlpUrl "+ jenkinsURL +" -secret ${secret}";
        agentBootstrapJob = j.jenkins.createProject(WorkflowJob.class, "AgentBootstrap");

        agentBootstrapJob.addAction(new ParametersAction(Arrays.asList(new StringParameterValue("node", "agent-12345"))));
        agentBootstrapJob.setDefinition(new CpsFlowDefinition(
            "node('master') {\n" + "sh \"echo 'Created Agent!'\"\n" +
            "secret = jenkins.model.Jenkins.instance.getNode(params.node)?.computer?.jnlpMac?.trim()\n" +
            "sh \"JENKINS_NODE_COOKIE=dontKillMe "+ agentCmd +" \"\n" +
            "}", false /* for hudson.scm.NullSCM */));

        agentBootstrapJob.scheduleBuild2(0, new ParametersAction(Arrays.asList(new StringParameterValue("node", "agent-12345"))));
    }

    @Test
    @LocalData
	public void one_agent() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node(\"agent\") {\n" + "sh \"echo 'Hello World!'\"\n" +
            "}", false /* for hudson.scm.NullSCM */));
 
        assertTrue("No runs has been performed and there should be no SCMs", p.getSCMs().isEmpty());

        j.buildAndAssertSuccess(p);
        assertEquals("Expect one Agent.", 1, j.jenkins.getNodes().size());
    }

    @Test
    @LocalData
    public void two_agents() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p1");
        p.setDefinition(new CpsFlowDefinition(
            "node(\"agent1\") {\n" + "sh \"echo 'Hello World!'\"\n" +
            "}", false /* for hudson.scm.NullSCM */));
 
        assertTrue("No runs has been performed and there should be no SCMs", p.getSCMs().isEmpty());

        j.buildAndAssertSuccess(p);
        assertEquals("Expect one Agent.", 1, j.jenkins.getNodes().size());

        p = j.jenkins.createProject(WorkflowJob.class, "p2");
        p.setDefinition(new CpsFlowDefinition(
            "node(\"agent2\") {\n" + "sh \"echo 'Hello World!'\"\n" +
            "}", false /* for hudson.scm.NullSCM */));
 
        assertTrue("No runs has been performed and there should be no SCMs", p.getSCMs().isEmpty());

        j.buildAndAssertSuccess(p);
        assertEquals("Expect one Agent.", 2, j.jenkins.getNodes().size());
    }
}
