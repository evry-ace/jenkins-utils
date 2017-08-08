import no.evry.Config

def env
def readProperties

{ ->
  conf = new no.evry.Config(this, 'build')

  this.env = [BRANCH_NAME: 'master']
  assert conf.getBranch() == 'master'

  this.env = [BRANCH_NAME: 'feat/awesome-branch']
  assert conf.getBranch() == 'feat-awesome-branch'

  assert conf.getFilePath('default') == 'build/default.properties'
  assert conf.getFilePath('master') == 'build/master.properties'
  assert conf.getFilePath('prod') == 'build/prod.properties'
  assert conf.getFilePath('test') == 'build/test.properties'

  this.readProperties = { opts ->
    defaults = [:]

    if (opts.defaults) { defaults = opts.defaults }

    switch(opts.file) {
      case 'build/default.properties':
        return defaults + [FOO: 'foo', BAR: 'default']
      case 'build/master.properties':
        return defaults + [BAR: 'master', BAZ: 'baz']
      case 'build/prod.properties':
        return defaults + [BAR: 'prod', BAZ: 'baz']
      default:
        throw new IOException()
    }
  }

  assert conf.defaultProperties() == [FOO: 'foo', BAR: 'default']

  this.env = [BRANCH_NAME: 'master']
  assert conf.branchProperties() == [FOO: 'foo', BAR: 'master', BAZ: 'baz']

  this.env = [BRANCH_NAME: 'notexists']
  assert conf.branchProperties() == [FOO: 'foo', BAR: 'default']

  assert conf.envProperties('master') == [FOO: 'foo', BAR: 'master', BAZ: 'baz']
  assert conf.envProperties('prod') == [FOO: 'foo', BAR: 'prod', BAZ: 'baz']

  assert conf.envProperties('notexists') == [FOO: 'foo', BAR: 'default']
}()
