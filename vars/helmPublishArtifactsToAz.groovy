/* This is a utility function to upload
  - helm chart packaged via helmPackage or alike
  - helm value files residing under values/file.yaml, the files will be changed to have names like <original filaneme>-<chartVersion>.yaml
*/
def call(appName, Map opts = [:]) {
  def chartDef = readYaml file: "chart/${appName}/Chart.yaml"
  if (!opts.version) {
    chartVersion = chartDef.version
  } else {
    chartVersion = opts.version
  }

  def secret = opts.secret ?: 'az-artifacts'

  azureUpload storageCredentialId: secret, storageType: 'blobstorage', containerName: appName, filesPath: "${appName}-${chartVersion}.tgz", virtualPath: "packages"

  def valueFiles = findFiles(glob: 'values/**.yaml')
  valueFiles.each {f ->
    def base = f.name.minus(".yaml")

    /* This copy thing is just a way to get around azureUpload not allowing you to set the remote path correctly */
    def releaseValueFileName = "${base}-${chartVersion}.yaml"
    sh """
    mv values values.tmp
    cp -R values.tmp values
    mv values/${f.name} values/$releaseValueFileName
    """

    azureUpload storageCredentialId: secret, storageType: 'blobstorage', containerName: appName, filesPath: "values/${releaseValueFileName}"

    sh """
    rm -rf values
    mv values.tmp values
    """
  }
}
