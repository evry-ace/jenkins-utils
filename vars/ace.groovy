#!/usr/bin/env groovy

import no.evry.Slack
import no.evry.Docker

def call(Map opts = [:], body) {
  def buildAgent = opts.buildAgent ?: 'jenkins-docker-3'
  def dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true
  def dockerNameOnly = opts.dockerNameOnly ?: false
  def configSet = opts.containsKey('configSet') ? opts.configSet : true
  def configFile = opts.configFile ?: 'ace.yaml'

  node(buildAgent) {
    buildWorkspace {
      try {
        println "Dedicated to the original ACE, by Alan Turing"

        checkout scm

        if (configSet) {
          body.config = readYaml file: configFile
        }

        if (configSet && dockerSet) {
          body.config.dockerImage = new Docker(this).image()
        }

        if (body.config?.contact?.chat_room) {
          def channel = body.config.contact.slack_notifications
          def alerts = body.config.contact.slack_alerts ?: channel

          body.slack = new Slack(this, channel, alerts)
          body.slack.notifyStarted()
        }

        body()
      } catch (err) {
        if (body.getBinding().hasVariable('slack')) {
          body.slack.notifyFailed()
        }
        throw err
      } finally {
        step([$class: 'WsCleanup'])
        sleep 10
      }
    }
  }
}
