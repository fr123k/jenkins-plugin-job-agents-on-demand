# job-agents-on-demand

**Beta Version**

## Introduction

This plugin implements an agents on demand approach for jenkins. 
It creates jenkins agents on the fly for labels that don't have
an agent yet.

The on the fly created agents have a new launch type called JobComputerLauncher.
This launcher just trigger configured jenkins job with pre defined parameters
the provisioning of the agent itself is in the reasonability of the triggered
Job.

The plugin support two different types of jobs for the agent lifecycle.
* provisioning
* decommissioning

The motivation and inspiration came from the following jenkins plugins
[one-shot-executor-plugin](https://github.com/jenkinsci/one-shot-executor-plugin) and [docker-plugin](https://github.com/jenkinsci/docker-plugin).
The first provides one shot jenkins agents that only perform one build and and then
get destroyed.
The second provides jenkins agents based on Dockerfile templates that can be
provided for specific labels. But it has a couple of limitations that
lead to the decision to implement an different plugin.

The main differences between this plugin and the docker-plugin are:
* the jenkins don't need a connection to the docker daemon (support jenkins as docker)
* the agents are created on the fly (no need to preconfigure them)
* the agents are launched by trigger a configured jenkins job 

## Use Case

## Example

For example when a jenkins job is started and requested an node with the label `golang`.
```
node("golang") {
    sh("go --version")
}
```
If no agents exists with that assigned label then it will create a new agent
with a random name like `golang-h234k` and assign the `golang` label to it.

The agent use the job launcher to call the configured jenkins job with couple
of predefined parameters like
* label `golang`
* node `golang-h234k`
.

## Todo
* setup travis build
* provide full documentation
* upload to the jenkins plugin registry
