#!/usr/bin/env groovy

def call(def name, def namespace, def cluster, def opts = [:]) {
  def k8sVersion = opts['k8sVersion'] ?: 'v1.6.2'

  withCredentials([file(credentialsId: cluster, variable: 'KUBECONFIG')]) {
    docker.image("lachlanevenson/k8s-kubectl:${k8sVersion}").inside("-u root:root") {
      sh script: 'kubectl version'

      try {
        secretString = sh(
          script: "kubectl get secret ${name} -n ${namespace} -o yaml",
          returnStdout: true,
        )?.trim()
      } catch(e) {
        return []
      }

      def secret = readYaml(text: secretString)
      def keys = []
      secret.data.each{ k, v -> keys.add(k) }

      return keys
    }
  }
}
