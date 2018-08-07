package no.evry

class AceUtils {
  static def tld = 'ace.evry.services'

  static def clusters = [
    alpha: 'alpha',
    dev: 'test',
    prod: 'prod',
    test: 'test',
    tools: 'tools',
  ]

  static def envCluster(def env) {
    if (!this.clusters.containsKey(env)) {
      throw new IllegalArgumentException ("Invalid cluster for environment '${env}'")
    }

    return this.clusters[env]
  }

  static def envNamespace(env, namespace, envify = true) {
    return envify != false ? "${namespace}-${env}" : namespace
  }

  static def envHostName(env, cluster) {
    return "${env}.${cluster}.${this.tld}"
  }

  /**
    * Construct Helm ingress configuration for given deployment environment
    *
    * @param envIngress local environment specific configurations
    * @param defIngress global default ingress configurations
    *
    * @return Map with final ingress configuration
    */
  static def helmIngress(def env, def cluster, def envIngress = [:], def defIngress = [:]) {
    def ingress = [enabled: true, internal: true, hosts: []] + (defIngress ?: [:]) + (envIngress ?: [:])

    if (ingress.enabled) {
      if (ingress.internal) {
        ingress.hosts.push(this.envHostName(ingress.internalHost ?: env, cluster))
      }

      ingress.remove('internal')
      ingress.remove('internalHost')

      return ingress
    } else {
      return [enabled: false]
    }
  }
}
