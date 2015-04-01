def githubBuildTargets = [
  "dubizzle": [
    "dubizzle": [
        "steps": [
            "env",
            "BUILD_SERVER_CONN_STRING=${BUILD_SERVER_CONN_STRING} ./docker/build_helper.sh",
            "make docker",
            "make docker-push"
        ],
        "create": [
            "stage": "dubizzle-uae",
            "production": "dubizzle-uae"
        ]
    ],
    "fuego": [],
    "codi": [],
    "kraken": [
        "hipchat": [
            "room": "The Dreamers"
        ],
        "steps": [
            "make docker"
        ],
        "create": [
            "stage": "kraken",
            "production": "kraken"
        ],
        "deploy": [
            "stage": "kraken-staging"
        ]
    ],    
    "lilith": [
        "hipchat": [
            "room": "The Dreamers"
        ],
        "steps": [
            "make docker"
        ],
        "create": [
            "stage": "lilith",
            "production": "lilith"
        ],
        "deploy": [
            "stage": "lilith-staging"
        ]
    ],
    "greedy": [
        "hipchat": [
            "room": "The Dreamers"
        ],
        "steps": [
            "make docker"
        ],
        "create": [
            "stage": "greedy",
            "production": "greedy"
        ],
        "deploy": [
            "stage": "greedy-staging"
        ]
    ],
    "terra": [
        "steps": [
            "make docker",
            "make docker-push"
        ],
        "create": [
            "test": "terra",
            "production": "terra"
        ],
        "deploy": [
            "test": "terra"
        ],
    ]
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

        def hipchatRoom = ghProjectSettings.hipchat?.room ?: "${HIPCHAT_ROOM}"

        println "${ghProject} | HipChat room => ${hipchatRoom}"

        def buildSteps = ghProjectSettings.steps ?: defaultBuildSteps

        def createVersionEnvs = ghProjectSettings.create ?: []
        def deployVersionEnvs = ghProjectSettings.deploy ?: []

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
                colorizeOutput("gnome-terminal")
            }
            steps {
                buildSteps.each {
                    shell("${it}")
                }

                if (createVersionEnvs) {
                    shell("make beanstalk-source-bundle")

                    createVersionEnvs.each {
                        shell("ebizzle --profile=${it.key} create ${it.value}")
                    }
                }

                deployVersionEnvs.each {
                    shell("ebizzle --profile=${it.key} deploy ${it.value}")
                }
            }
            configure { project ->
                project / "properties" << "jenkins.plugins.hipchat.HipChatNotifier_-HipChatJobProperty" {
                    startNotification true
                    notifySuccess true
                    notifyAborted true
                    notifyNotBuilt true
                    notifyUnstable true
                    notifyFailure true
                    notifyBackToNormal true
                }
                project / publishers << "jenkins.plugins.hipchat.HipChatNotifier" {
                    server "${HIPCHAT_SERVER}"
                    authToken "${HIPCHAT_AUTH_TOKEN}"
                    buildServerUrl "${HIPCHAT_BUILD_SERVER_URL}"
                    room "${hipchatRoom}"
                    sendAs "${HIPCHAT_SEND_AS}"
                }
                project / publishers << "com.chikli.hudson.plugin.naginator.NaginatorPublisher" {
                    rerunIfUnstable false
                    rerunMatrixPart false
                    maxSchedule 0
                    checkRegexp true
                    regexpForRerun "Error getting container"
                    delay(class: "com.chikli.hudson.plugin.naginator.FixedDelay") {
                        delay 5
                    }
                }
            }
        }
    }
}
