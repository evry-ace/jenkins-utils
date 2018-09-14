/*
  Helper to take files like chart and value artifacts into a standard payload

  Assuming the follow structure
  chart/<name>
  values/production.yaml
  values/development.yaml

  We end up with
  [
    [
      kind: "s3",
      type: "s3/object",
      name: "s3://<name>-<suffix>/packages/<chart>.tgz",
      reference: "s3://<name>-<suffix>/packages/<chart>.yaml"
    ],
    [
      kind: "s3",
      type: "s3/object",
      name: "s3://<name>-<suffix>/values/development.yaml",
      reference: "s3://<name>-<suffix>/values/development-<version>.yaml"
    ],
    [
      kind: "s3",
      type: "s3/object",
      name: "s3://<name>-<suffix>/values/qa.yaml",
      reference: "s3://<name>-<suffix>/values/qa-<version>.yaml"
    ]
  ]
*/
def _genS3Artifact(artifacts, name, ref) {
  artifacts.push([
    kind: "s3",
    type: "s3/object",
    name: "s3://${name}",
    reference: "s3://${ref}"
  ])
}

def call(String name, String version, String suffix="") {
  def artifacts = []

  def bucket = [name, suffix].join("-")
  _genS3Artifact(artifacts, "${bucket}/packages/${name}.tgz", "${bucket}/packages/${name}-${version}.tgz")

  def valueFiles = findFiles(glob: 'values/**.yaml')
  valueFiles.each {f ->
    def valueFileName = f.name.minus(".yaml")
    _genS3Artifact(artifacts, "${bucket}/values/${f.name}", "${bucket}/values/${valueFileName}-${version}.yaml")
  }

  return artifacts
}
