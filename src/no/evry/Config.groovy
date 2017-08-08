package no.evry

class Config implements Serializable {
  def script
  def configDir

  Config(script, path = 'build') {
    this.script = script
    this.configDir = path
  }

  def getBranch() {
    return script.env.BRANCH_NAME.toLowerCase().replaceAll(/[^A-Za-z0-9]/, '-')
  }

  def getFilePath(String fileName) {
    return "${configDir}/${fileName}.properties"
  }

  def defaultProperties() {
    return script.readProperties(file: getFilePath('default'))
  }

  def branchProperties() {
    return envProperties(getBranch())
  }

  def envProperties(env) {
    def conf = defaultProperties()

    try {
      return script.readProperties(file: getFilePath(env), defaults: conf)
    } catch (e) {
      return conf
    }
  }
}
