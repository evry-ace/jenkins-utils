import no.evry.Docker

def env

{ ->
  this.env = [
    BRANCH_NAME: 'feature/DFP-120_Kubernetes_deployment_from_jenkins',
    BUILD_NUMBER: '6',
    JOB_NAME: 'github evry-bergen/danica-flytt-pensjon/feature%2FDFP-120_Kubernetes_deployment_from_jenkins',
  ]

  docker = new no.evry.Docker(this)
  assert docker.imageName() == 'github-evry-bergen/danica-flytt-pensjon'

  docker = new no.evry.Docker(this, [nameOnly: true])
  assert docker.imageName() == 'danica-flytt-pensjon'

  assert docker.branchTag() == 'feature-dfp-120-kubernetes-deployment-from-jenkins'
  assert docker.buildTag() == 'feature-dfp-120-kubernetes-deployment-from-jenkins-6'

  assert docker.image('myreg') == "myreg/${docker.imageName()}:${docker.buildTag()}"
}()
