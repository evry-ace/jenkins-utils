/* Utility helper for s3Cmd when other plugins doesnt work */

def call(String s3Command, Map opts = [:]) {
  def secretKey = env.AWS_SECRET_ACCESS_KKEY != '' ? env.AWS_SECRET_ACCESS_KKEY : opts.awsSecretKey
  def accessKey = env.AWS_ACCESS_KEY_ID != '' ? env.AWS_ACCESS_KEY_ID : opts.awsAccessKeyId

  def runArgs = [
    "-e AWS_ACCESS_KEY_ID=${accessKey}",
    "-e AWS_SECRET_ACCESS_KEY=${secretKey}",
    "-v $PWD:/data -w /data"
  ]

  def shellCommand = [
    "s3cmd",
  ]

  def payload = groovy.json.JsonOutput.toJson(opts)
  echo payload

  if (opts.host) {
    shellCommand.push("--host ${opts.host}")
  }

  shellCommand.push(s3Command)

  if (opts.cmdOpts) {
    shellCommand.push(opts.cmdOpts)
  }
  echo shellCommand.join(" ")
  // def cmd = s3cmd.join((" ")
  // docker.image("gerland/docker-s3cmd").inside(runArgs.join(" ")) {
  //   sh "${cmd}"
  // }
}
