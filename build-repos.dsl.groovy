def githubBuildTargets = [
  "dubizzle": [
    "terra": [],
    "codi": [],
    "kraken": [
        "hipchat": [
            "room": "The Dreamers"
        ],
        "steps": [
            "IMAGE_VERSION=buildops make docker_build",
            "IMAGE_VERSION=buildops make docker_push"
        ]
    ],
  ]
]

def defaultBuildSteps = [
    "make docker",
    "make docker-push"
]

githubBuildTargets.each {
    def ghUser = it.key

    it.value.each {
        def ghProject = it.key
        def ghProjectSettings = it.value

        println "Creating job for ${ghUser} => ${ghProject}"

        def hipchatRoom = ghProjectSettings.hipchat?.room ?: HIPCHAT_ROOM
        def buildSteps = ghProjectSettings.steps ?: defaultBuildSteps

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
            wrappers {
                sshAgent("${GITHUB_CREDENTIALS_UUID}")
            }
            steps {
                buildSteps.each {
                    shell("${it}")
                }
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
                    room "${hipchatRoom}"
                    sendAs "${HIPCHAT_SEND_AS}"
                }
                project / publishers << 'com.chikli.hudson.plugin.naginator.NaginatorPublisher' {
                    rerunIfUnstable false
                    rerunMatrixPart false
                    maxSchedule 0
                    checkRegexp true
                    regexpForRerun 'Error getting container'
                }
            }
        }
    }
}
