package no.evry

import no.evry.Docker
import no.evry.Git

/*
  Utilities for spinnaker
*/
class Spinnaker implements Serializable {
    def docker
    def git
    def script

  Spinnaker(script) {
    this.script = script
    this.git = new Git()
    this.docker = new Docker(script)
  }

  def makeImageParameters(Map parameters = [:], Map opts = [:]) {
    parameters.img_fqn = this.docker.image(opts.registry != null ? opts.registry : '')
    parameters.img_name = this.docker.imageName()
    parameters.img_tag = this.docker.buildTag()
    return parameters
  }

  def makeGitParameters(Map env, Map parameters = [:]) {
    parameters.git_pr_id = this.git.prId(env)
    parameters.git_pr_url = this.git.prUrl(env)
    parameters.git_release_version = this.git.releaseBranchVersion(env)

    def gitCommit = this.script.sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    def gitShortCommit = gitCommit[-8..-1]

    parameters.git_commit_short = gitShortCommit
    parameters.git_commit = gitCommit

    return parameters
  }
}
