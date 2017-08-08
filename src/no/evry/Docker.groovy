#!/usr/bin/env groovy

package no.evry

class Docker implements Serializable {
  def script
  def opts
  def nameOnly

  Docker(script, Map opts = [:]) {
    this.script = script
    this.opts = opts

    this.nameOnly = opts['nameOnly'] ?: false
  }

  def imageName() {
    def list = this.script.env.JOB_NAME.split('/')

    if (list.size() == 3) {
      if (this.nameOnly) {
        return list[1]
      } else {
        return "${list[0]}/${list[1]}".replaceAll(/[^A-Za-z0-9-\/]/, '-')
      }
    } else {
      return this.script.env.JOB_NAME
    }
  }

  def branchTag() {
    return scrub(this.script.env.BRANCH_NAME)
  }

  def buildTag() {
    return "${scrub(this.script.env.BRANCH_NAME)}-${this.script.env.BUILD_NUMBER}"
  }

  def scrub(String str) {
    return str.toLowerCase().replaceAll(/[^A-Za-z0-9]/, '-')
  }

  def image(String registry) {
    return "${registry}/${imageName()}:${buildTag()}"
  }
}
