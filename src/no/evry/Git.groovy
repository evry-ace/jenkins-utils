package no.evry

class Git {
  static def isMasterBranch(Map env) {
    return env.BRANCH_NAME == 'master'
  }

  static def isDevelopBranch(Map env) {
    return env.BRANCH_NAME == 'develop'
  }

  static def isFeatureBranch(Map env) {
    return !!(env.BRANCH_NAME =~ /^feature\//)
  }

  static def isReleaseBranch(Map env) {
    return !!(env.BRANCH_NAME =~ /^release\/(v[0-9]+\.[0-9]+\.[0-9]+)/)
  }

  static def releaseBranchVersion(Map env) {
    def m

    if ((m = env.BRANCH_NAME =~ /^release\/(v[0-9]+\.[0-9]+\.[0-9]+)/)) {
      return m.group(1)
    } else {
      return ''
    }
  }

  static def gitCommit(def script) {
    return script.sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
  }

  static def gitShortCommit(def script) {
    return gitCommit(script)[-8..-1]
  }

  static def isPR(Map env) {
    return !!env.CHANGE_ID
  }

  static def prId(Map env) {
    return env.CHANGE_ID ?: ''
  }

  static def prUrl(Map env) {
    return env.CHANGE_URL ?: ''
  }
}
