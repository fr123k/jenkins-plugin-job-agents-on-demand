package com.github.fr123k.agents.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jvnet.hudson.test.JenkinsRule;

import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

public class JobHelper {

	private final static Logger LOGGER = Logger.getLogger(JobHelper.class.getName());
	private final static int DEFAULT_QUIET_PERIOD = 0;

	public JenkinsRule j;

	public JobHelper(JenkinsRule j) {
		this.j = j;
	}

	static class TestBuilder extends Builder {

		private int sleepTime;

		public TestBuilder(int sleepTime) {
			this.sleepTime = sleepTime;
		}

		@Override
		public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
				throws InterruptedException, IOException {
			LOGGER.info("Building: " + build.getParent().getName());
			Thread.sleep(sleepTime);
			return true;
		}
	}

	public FreeStyleProject createProject(String name) throws Exception {
		FreeStyleProject project = j.createFreeStyleProject(name);
		project.getBuildersList().add(new TestBuilder(100));
		return project;
	}

	public List<FreeStyleProject> createProjects(int numberOfProjects) throws Exception {
		List<FreeStyleProject> projects = new ArrayList<FreeStyleProject>(numberOfProjects);
		for (int i = 0; i < numberOfProjects; i++) {
			FreeStyleProject project = j.createFreeStyleProject("Job " + i);
			project.getBuildersList().add(new TestBuilder(100));
			projects.add(project);
		}
		return projects;
	}

	public JobHelper scheduleProject(String name, Cause cause) throws Exception {
		FreeStyleProject project = createProject(name);
		// Scheduling executors is zero
		project.scheduleBuild(DEFAULT_QUIET_PERIOD, cause);
		Thread.sleep(100);
		return this;
	}

	public JobHelper scheduleProjects(Cause... causes) throws Exception {
		return scheduleProjects(DEFAULT_QUIET_PERIOD, causes);
	}

	public JobHelper scheduleProjects(int quitePreiod, Cause... causes) throws Exception {
		List<FreeStyleProject> projects = createProjects(causes.length);
		// Scheduling executors is zero
		for (int i = 0; i < causes.length; i++) {
			projects.get(i).scheduleBuild(quitePreiod, causes[i]);
		}
		return this;
	}

	public void go() throws Exception {
		// Set the executors to one
		Jenkins.get().setNumExecutors(1);
	}

}
