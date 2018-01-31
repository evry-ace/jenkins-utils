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
  def String k8sVersion = opts['k8sVersion'] ?: 'v1.6.2'
  def String k8sNamespace = conf['K8S_NAMESPACE'] ?: ''
  def String dockerRegistry = conf['DOCKER_REGISTRY'] ?: ''
  
  withEnv(mapToList(conf)) {
    println env

    withCredentials([file(credentialsId: k8sCluster, variable: 'KUBECONFIG'), 
                    usernamePassword(credentialsId: dockerRegistry, usernameVariable: 'docker_user', passwordVariable: 'docker_passw')]) {
      docker.image("lachlanevenson/k8s-kubectl:${k8sVersion}").inside("-u root:root") {
        if (apply) {
          sh """
            set -u
            set -e

            # Install envsubst
            apk add --update bash gettext && rm -rf /var/cache/apk/*

            # Check kubernetes connection
            kubectl version
            kubectl get namespaces | if grep -q ${k8sNamespace}; then 
               echo "Namespace already exists" 
            else 
                echo "creating namespace" 
                kubectl create namespace ${k8sNamespace} 
                kubectl create secret docker-registry ${dockerRegistry} --docker-server=${dockerRegistry} --docker-username=${docker_user} --docker-password=${docker_passw} --docker-email=evrybgoprod@evry.com --namespace=${k8sNamespace}
            fi

            # Make deployment directory
            rm -rf "${path}-k8s-deploy"
            mkdir "${path}-k8s-deploy"

            # Substitute environment variables
            for file in ${path}/*.yaml; do
              outfile=${path}-k8s-deploy/\$(basename "\${file}")
              cat "\${file}" | envsubst '${mapToKeystring(conf)}' > "\${outfile}"
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
