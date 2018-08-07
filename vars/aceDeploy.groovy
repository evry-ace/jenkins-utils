#!/usr/bin/env groovy
import no.evry.AceUtils

def call(inputValues, environment, opts = [:]) {
  def debug = opts.containsKey('debug') ? opts.debug : true
  def dryrun = opts.dryrun ?: false
  def wait = opts.containsKey('wait') ? opts.wait : true
  def timeout = opts.timeout ?: 600
  def migrate = opts.migrate ?: false
  def dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true

  def helmVersion = opts.containsKey('helmVersion') ? opts['helmVersion'] : '2.9.0'

  def credVar = 'KUBECONFIG'

  // Make a copy of input values to prevent reuse of value HashMap in Jenkins
  writeYaml file: 'helmdeploytemp.yaml', data: inputValues
  def values = readYaml file: 'helmdeploytemp.yaml'

  // Path to Helm configuration values
  def helmPath = values.helm?.path ?: 'deploy'

  // Name of Helm release
  def helmName = values.name ? "${values.name}-${environment}" : ''
  if (helmName == '') { throw new IllegalArgumentException("name can not be empty") }

  // Name of Helm chart
  // https://github.com/evry-ace/helm-charts
  def helmChart = values.helm.chart ?: ''
  if (helmChart == '') { throw new IllegalArgumentException("helmChart can not be empty") }

  // Helm Chart Repo
  // https://github.com/evry-ace/helm-charts
  def helmRepo = values.helm.repo ?: 'https://evry-ace.github.io/helm-charts'
  def helmRepoName = values.helm.repoName ?: 'ace'

  // Valid version of chart
  // Can be a version "range" such as "^1.0.0"
  def chartVersion = values.helm.version ?: ''
  if (chartVersion == '') { throw new IllegalArgumentException("chartVersion can not be empty") }

  // Docker Image to inject to Helm chart
  def dockerImage = values.dockerImage ?: values.helm?.default?.image?.image ?: ''
  if (dockerSet && dockerImage == '') {
    throw new IllegalArgumentException("dockerImage can not be empty")
  }

  // Kubernetes namespace for Helm chart
  if (!values.namespace) { throw new IllegalArgumentException("namespace can not be empty") }
  namespace = AceUtils.envNamespace(environment, values.namespace, values.namespaceEnvify)

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Default Helm Configurations
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
  values.helm.default = values.helm.default ?: [:]

  // Docker Image
  if (dockerImage) {
    values.helm.default.image = values.helm.default.image ?: [:]
    values.helm.default.image.image = values.dockerImage ?: values.helm.default.image.image
  }

  // Helm Release Name
  values.helm.default.name = values.helm.default.name ?: values.name

  // Helm Metadata
  values.helm.default.meta = values.helm.default.meta ?: [:]

  // Contact Metadata
  values.helm.default.meta.contact = values.helm.default.meta.contact ?: values.contact ?: [:]

  // Git Metadata
  values.helm.default.meta.git = [
    git_commit: gitCommit(),
    git_url: gitUrl(),
    branch_name: env.branch_name,
  ]

  // Jenkins Metadata
  values.helm.default.meta.jenkins = [
    job_name: env.job_name,
    build_number: env.build_number,
    build_url: env.build_url,
  ]

  // Kubernetes Secrets
  if (values.helm.default.secrets) {
    for (secret in values.helm.default.secrets) {
      secret.valueFrom.secretKeyRef.remove('type')
      secret.valueFrom.secretKeyRef.remove('desc')
    }
  }

  // ConfigMap Parsing
  if (values.helm.default.configFiles) {
    values.helm.default.configFiles = parseConfigFiles(values.helm.default.configFiles)
  }

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Environment Specific Configurations
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
  values.deploy = values.deploy ?: [:]

  def deploy = values.deploy[environment] ?: [:]

  // Dereive Kuberentes Cluster from Environment
  def cluster = deploy.cluster ?: AceUtils.envCluster(environment)

  // Allow Kubernetes Namespace from Environment
  namespace = deploy.namespace ?: namespace

  deploy.values = deploy.values ?: [:]

  // Set App Environment
  deploy.values.appEnv = deploy.values.appEnv ?: environment

  // Set Env Ingress
  deploy.values.ingress = AceUtils.helmIngress(environment, cluster, deploy.values.ingress, values.helm.default.ingress)
  values.helm.default.remove('ingress')

  // Set Env Vars
  deploy.values.environment = (deploy.values.environment ?: []) + (values.helm.default.environment ?: [])
  values.helm.default.remove('environment')

  // ConfigMap Parsing
  if (deploy.values.configFiles) {
    deploy.values.configFiles = parseConfigFiles(deploy.values.configFiles)
  }

  if (debug) {
    println "writing ${helmPath}/default.yaml..."
    println values.helm.default

    println "writing ${helmPath}/${environment}.yaml..."
    println deploy.values
  }

  writeYaml file: "${helmPath}/default.yaml", data: values.helm.default
  writeYaml file: "${helmPath}/${environment}.yaml", data: deploy.values

  withCredentials([file(credentialsId: cluster, variable: credVar)]) {
    docker.image("dtzar/helm-kubectl:${helmVersion}").inside() {
      sh """
        set -u
        set -e

        # Check Kubernetes connection
        kubectl version

        # Set Helm Home
        export HELM_HOME=\$(pwd)

        # Install Helm locally
        helm init -c

        # Check Helm connection
        helm version
      """
      def helmExists = sh(script: "helm history ${helmName}", returnStatus: true) == 0

      if (migrate) {
        def kubectlExists = sh(script: "kubectl get deploy ${values.name} -n ${namespace}", returnStatus: true) == 0

        println "helmDeploy2: migrate=${migrate}, helmExists=${helmExists}, kubectlExists=${kubectlExists}"

        if (!helmExists && kubectlExists && !dryrun) {
          sh """
            set -u

            kubectl delete deploy ${values.name} -n ${namespace} || true
            kubectl delete service ${values.name} -n ${namespace} || true
            kubectl delete ingress ${values.name} ${values.name}-internal ${values.name}-external -n ${namespace} || true
          """
        }
      }

      try {
        sh """
          set -u
          set -e

          # Set Helm Home
          export HELM_HOME=\$(pwd)

          # Add Helm repository
          helm repo add ${helmRepoName} ${helmRepo}
          helm repo update

          helm upgrade --install \
            --namespace ${namespace} \
            -f ${helmPath}/default.yaml \
            -f ${helmPath}/${environment}.yaml \
            --debug=${debug} \
            --dry-run=${dryrun} \
            --wait=${wait} \
            --timeout=${timeout} \
            --version=${chartVersion} \
            ${helmName} \
            ${helmChart}
        """
      } catch (err) {
        if (!helmExists) {
          try {
            sh(script: "helm delete ${helmName} --purge", returnStatus: true)
          } catch (e) {}
        }

        throw err
      }
    }
  }
}
