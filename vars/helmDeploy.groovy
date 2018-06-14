#!/usr/bin/env groovy

def call(environment, opts = [:]) {

    def config = readYaml file: 'ace.yaml'
    def helmVersion = config.helm?.helmVersion ?: '2.8.2'
    def credVar = 'KUBECONFIG'
    def helmPath = 'deploy'
    def debug = true
    def dryrun = opts.dryRun ?: false

    // Name of Helm release
    def helmName = "${config.name}-${environment}"
    if (helmName == '') {
        throw new IllegalArgumentException("name can not be empty")
    }

    def app = config.app ?: ''
    if (app == '') {
        throw new IllegalArgumentException("app can not be empty")
    }

    def name = config.name ?: ''
    if (name == '') {
        throw new IllegalArgumentException("name can not be empty")
    }

    // Name of Helm chart
    def helmChart = config.helm.chart ?: ''
    if (helmChart == '') {
        throw new IllegalArgumentException("helmChart can not be empty")
    }

    // Helm Chart Repo
    // https://stash.fiskeridirektoratet.no/projects/K8S/repos/helm-charts/browse
    def helmRepo = config.helm.url ?: 'https://evry-ace.github.io/ace-app-chart/'
    def helmRepoName = config.helm.repoName ?: 'evry-ace'

    // Valid version of chart
    // Can be a version "range" such as "^1.0.0"
    def chartVersion = config.helm.version ?: ''
    if (chartVersion == '') {
        throw new IllegalArgumentException("chartVersion can not be empty")
    }

    // Docker Image to inject to Helm chart
    def dockerImage = opts.image ?: ''
    if (dockerImage == '') {
        throw new IllegalArgumentException("dockerImage can not be empty")
    }

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
  * Default Helm Configurations
  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
  def values = config.common ?: [:]

  /*****
  * Set the image
  */
  values.deployment.image.name = dockerImage
  values.name = name
  values.app = app
  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Environment Specific Configurations
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
  def env = config.environments[environment] ?: [:]

  // Dereive Kuberentes Cluster from Environment
  def cluster = env.cluster ?: ''
  if (dockerImage == '') {
    throw new IllegalArgumentException("cluster can not be empty")
  }

  // Allow Kubernetes Namespace from Environment
  values.namespace = env.namespace ?: values.namespace

  // Kubernetes namespace for Helm chart
  if (!values.namespace) { throw new IllegalArgumentException("namespace can not be empty") }

  if (debug) {
    println "writing ${helmPath}/default-${environment}.yaml..."
    println values

    println "writing ${helmPath}/${environment}.yaml..."
    println env.overrides
  }

  writeYaml file: "${helmPath}/default-${environment}.yaml", data: values
  writeYaml file: "${helmPath}/${environment}.yaml", data: env.overrides

  withCredentials([file(credentialsId: cluster, variable: credVar)]) {
    docker.image("dtzar/helm-kubectl:${helmVersion}").inside() {
        sh """
          set -u
          set -e

          # Set Helm Home
          export HELM_HOME=\$(pwd)
          helm init --client-only             

          # Add Helm repository
          helm repo add ${helmRepoName} ${helmRepo}
          helm repo update

          helm upgrade --install \
            --namespace ${values.namespace} \
            -f ${helmPath}/default-${environment}.yaml \
            -f ${helmPath}/${environment}.yaml \
            --debug=${debug} \
            --dry-run=${dryrun} \
            --version=${chartVersion} \
            ${helmName} \
            ${helmChart}
            
          rm ${helmPath}/default-${environment}.yaml
          rm ${helmPath}/${environment}.yaml
        """
    }
  }
}
