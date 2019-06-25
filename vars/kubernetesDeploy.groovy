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
  def String p12Key = opts['p12Key'] ?: ''
  def String k8sVersion = opts['k8sVersion'] ?: 'latest'
  def String k8sNamespace = conf['K8S_NAMESPACE'] ?: ''
  def String dockerRegistry = conf['DOCKER_REGISTRY']
  def String dockerEmail = conf['DOCKER_EMAIL'] ?: 'test@example.com'
  
  withEnv(mapToList(conf)) {
    println env

    def credentials = [file(credentialsId: k8sCluster, variable: 'KUBECONFIG')]
    if (p12Key?.trim()) {
      credentials.push(certificate(credentialsId: p12Key, variable: 'JENKINS_P12_KEY'));
    }

    if (dockerRegistry?.trim()) {
        credentials.push(usernamePassword(credentialsId: dockerRegistry, usernameVariable: 'docker_user', passwordVariable: 'docker_passw'))
    } else {
        //Need to add them so that the script for the docker container is properly resolved
        env.docker_user = ''
        env.docker_passw = ''
    }

    withCredentials(credentials) {
      docker.image("mskjeret/k8s-kubectl:${k8sVersion}").inside("-u root:root") {
        if (apply) {
          sh """
            set -u
            set -e

            # Install envsubst
            apk add --update bash gettext && rm -rf /var/cache/apk/*

            # Check kubernetes connection
            kubectl version
            kubectl get namespaces | cut -f 1 -d " " | if grep -q "^${k8sNamespace}\$"; then  
               echo "Namespace already exists" 
            else 
                echo "creating namespace" 
                kubectl create namespace ${k8sNamespace} 
         
                if [ -n \"$dockerRegistry\" ]; then
                    kubectl create secret docker-registry ${dockerRegistry} --docker-server=${dockerRegistry} --docker-username=${docker_user} --docker-password=${docker_passw} --docker-email=${dockerEmail} --namespace=${k8sNamespace}
                fi
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
