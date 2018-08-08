package no.evry

class AceUtils {
  static def tld = 'evry.site'
  static def credentialPrefix = 'kubernetes.ace'

  static def clusters = [
    alpha: 'alpha',
    dev: 'dev',
    prod: 'prod',
    test: 'dev',
    tooling: 'tooling',
  ]

  static def envCluster(env) {
    if (!this.clusters.containsKey(env)) {
      throw new IllegalArgumentException ("Invalid cluster for environment '${env}'")
    }

    return this.clusters[env]
  }

  static def clusterCredential(cluster) {
    return "${this.credentialPrefix}.${cluster}"
  }

  static def envNamespace(env, namespace, envify = true) {
    return envify != false ? "${namespace}-${env}" : namespace
  }

  static def appHostname(app, env) {
    return "${app}.${env}.${this.tld}"
  }

  /**
    * Construct Helm ingress object for given deployment environment
    *
    * @param app - application name
    * @param env - environment name
    * @param envIngress local environment specific configurations
    * @param defIngress global default ingress configurations
    *
    * @return Map with final ingress configuration
    */
  static def helmIngress(app, env, envIngress = [:], defIngress = [:]) {
    def ingress = [
      enabled: true,
      internal: true,
      hosts: []
    ] + (defIngress ?: [:]) + (envIngress ?: [:])

    if (ingress.enabled) {
      if (ingress.internal) {
        ingress.hosts.push(this.appHostname(ingress.internalHost ?: app, env))
      }

      ingress.remove('internal')
      ingress.remove('internalHost')

      return ingress
    } else {
      return [enabled: false]
    }
  }
}
