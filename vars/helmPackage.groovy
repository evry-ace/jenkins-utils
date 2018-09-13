#!/usr/bin/env groovy

/*
  Utility var for packaging helm charts
*/

def call(String helmChart, Map opts = [:]) {
  def chartDef = readYaml file: "${helmChart}/Chart.yaml"
  if (!opts.version) {
    chartVersion = chartDef.version
  } else {
    chartVersion = opts.version
  }

  helmVersion = opts.helmVersion ? opts.helmVersion : "2.10.0"

  docker.image("dtzar/helm-kubectl:${helmVersion}").inside("-v $PWD:/src -w /src") {
    sh """
    set -eu
    export HELM_HOME=\$(pwd)

    helm init --client-only
    helm package --version ${chartVersion} ${helmChart}
    """
  }
}
