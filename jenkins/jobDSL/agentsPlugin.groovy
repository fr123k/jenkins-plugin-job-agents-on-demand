multibranchPipelineJob("Job-Agents-On-demand-Plugin") {
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(0)
        }
    }

    description("Builds for jenkins-plugin-job-agents-on-demand")

    branchSources {
        // Adds a GitHub branch source.
        github {
            // Specifies a unique ID for this branch source.
            id("jenkins-plugin-job-agents-on-demand-id")
            // Sets checkout credentials for authentication with GitHub.
            checkoutCredentialsId("deploy-key-shared-library")
            // Sets the name of the GitHub Organization or GitHub User Account.
            repoOwner("fr123k")
            // Sets the name of the GitHub repository.
            repository("jenkins-plugin-job-agents-on-demand")
        }
    }

    factory {
        workflowBranchProjectFactory {
            scriptPath('jenkins/Jenkinsfile')
        }
    }

    // trigger the repository scan once a day to delete stale jobs
    triggers {
        periodicFolderTrigger {
            interval('1d')
        }
    }
}
