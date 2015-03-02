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
        }
    }
}
