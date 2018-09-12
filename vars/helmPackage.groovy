#!/usr/bin/env groovy

/*
  Utility var for packaging helm charts
*/

def call(String helmChart, String helmVersion = "2.8.2", String chartVersion = '') {
  def chartDef = readYaml file: "${helmChart}/Chart.yaml"
  if (!chartVersion) {
    chartVersion = chartDef.version
  }

  docker.image("dtzar/helm-kubectl:${helmVersion}").inside("-v $PWD:/src -w /src") {
    sh """
    set -eu
    export HELM_HOME=\$(pwd)

    helm init --client-only
    helm package --version ${chartVersion} ${helmChart}
    """
  }
}
