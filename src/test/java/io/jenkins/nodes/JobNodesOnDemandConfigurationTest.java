package io.jenkins.nodes;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class JobNodesOnDemandConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    /**
     * Tries to exercise enough code paths to catch common mistakes:
     * <ul>
     * <li>missing {@code load}
     * <li>missing {@code save}
     * <li>misnamed or absent getter/setter
     * <li>misnamed {@code textbox}
     * </ul>
     */
    @Test
    public void uiAndStorage() {
        JobNodesOnDemandConfiguration config = JobNodesOnDemandConfiguration.get();
        rr.then(r -> {
            assertNull("Default value not assigned", config.getGitUrl());
            HtmlForm htmlForm = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = htmlForm.getInputByName("_.gitUrl");
            textbox.setText("hello");
            r.submit(htmlForm);
            assertEquals("global config page let us edit it", "hello", config.getGitUrl());
        });
        rr.then(r -> {
            assertEquals("still there after restart of Jenkins", "hello", config.getGitUrl());
        });
    }

}
