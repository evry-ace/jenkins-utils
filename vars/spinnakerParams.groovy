import no.evry.Spinnaker

def call(def script, Map env, Map parameters = [:], Map opts = [:]) {
  def spin = new Spinnaker(script)
  spin.makeImageParameters(parameters, opts)
  spin.makeGitParameters(env, parameters)
  return parameters
}
