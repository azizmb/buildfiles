def githubBuildTargets = [
  "dubizzle": [
    "terra",
  ]
]

githubBuildTargets.each {
    def ghUser = it.key

    it.value.each {
        def ghProject = it
        println "Creating job for ${ghUser} => ${ghProject}"

        job {
            name "build-${ghUser}-${ghProject}"
            scm {
                git {
                    remote {
                        url("git@github.com:${ghUser}/${ghProject}")
                        refspec("+refs/tags/*:refs/remotes/origin/tags/*")
                        branch("*/tags/*")
                        credentials("${GITHUB_CREDENTIALS_UUID}")
                    }
                }
            }
            triggers {
                githubPush()
            }
            steps {
                shell("make docker")
                shell("make docker-push")
            }
            configure { project ->
                project / 'properties' << 'jenkins.plugins.hipchat.HipChatNotifier_-HipChatJobProperty' {
                    startNotification true
                    notifySuccess true
                    notifyAborted true
                    notifyNotBuilt true
                    notifyUnstable true
                    notifyFailure true
                    notifyBackToNormal true
                }
                project / publishers << 'jenkins.plugins.hipchat.HipChatNotifier' {
                    server "${HIPCHAT_SERVER}"
                    authToken "${HIPCHAT_AUTH_TOKEN}"
                    buildServerUrl "${HIPCHAT_BUILD_SERVER_URL}"
                    room "${HIPCHAT_ROOM}"
                    sendAs "${HIPCHAT_SEND_AS}"


                }
            }
        }
    }
}
