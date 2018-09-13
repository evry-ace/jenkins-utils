import no.evry.Spinnaker

def call(def script, Map parameters = [:], Map opts = [:]) {
  def spin = new Spinnaker(script)
  spin.makeImageParameters(parameters, opts)
  spin.makeGitParameters(script.env.getEnvironment(), parameters)
  return parameters
}
