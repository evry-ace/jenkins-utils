#!/usr/bin/env groovy

def call(Map conf, Map opts = [:]) {
  def String path = opts['path'] ?: 'deploy'
  def Boolean record = opts.containsKey('record') ? opts['record'] : true
  def Boolean apply = opts.containsKey('apply') ? opts['apply'] : true
  def Boolean dryrun = opts['dryrun'] ?: false

  def Boolean daemonSetCleanup = opts['daemonSetCleanup'] ?: false
  def String daemonSetName = opts['daemonSetName'] ?: ''
  def String daemonSetNamespace = opts['daemonSetNamespace'] ?: ''
  def Integer daemonSetSleep = opts['daemonSetSleep'] ?: 20

  def String k8sCluster = opts['k8sCluster'] ?: ''
  def String k8sVersion = opts['k8sVersion'] ?: 'v1.5.3'

  withEnv(mapToList(conf)) {
    println env

    withCredentials([file(credentialsId: k8sCluster, variable: 'KUBECONFIG')]) {
      docker.image("lachlanevenson/k8s-kubectl:${k8sVersion}").inside("-u root:root") {
        if (apply) {
          sh """
            set -u
            set -e

            # Install envsubst
            apk add --update bash gettext && rm -rf /var/cache/apk/*

            # Check kubernetes connection
            kubectl version

            # Make deployment directory
            rm -rf "${path}-k8s-deploy"
            mkdir "${path}-k8s-deploy"

            # Substitute environment variables
            for file in ${path}/*.yaml; do
              outfile=${path}-k8s-deploy/\$(basename "\${file}")
              cat "\${file}" | envsubst '${mapToLKeystring(conf)}' > "\${outfile}"
            done

            # Run Kubernetes Deployment
            kubectl apply \
              --dry-run=${dryrun} \
              --record=${record} \
              -f "${path}-k8s-deploy/"
          """
        }

        if (dryrun == false && daemonSetCleanup == true) {
          if (daemonSetName == '') {
            throw new IllegalArgumentException("daemonSetName can not be empty")
          }

          if (daemonSetNamespace == '') {
            throw new IllegalArgumentException("daemonSetNamespace can not be empty")
          }

          sh """
            set -u
            set -e

            pods=\$(kubectl get pods -n ${daemonSetNamespace} \
              | grep ${daemonSetName} \
              | awk '{ print \$1 }' \
            )

            for pod in \$pods; do
              kubectl delete pod \$pod -n ${daemonSetNamespace}
              sleep ${daemonSetSleep}
            done
          """
        }
      }
    }
  }
}

