import no.evry.Git

env = [BRANCH_NAME: 'master']
assert Git.isMasterBranch(env) == true
env = [BRANCH_NAME: 'develop']
assert Git.isMasterBranch(env) == false

env = [BRANCH_NAME: 'master']
assert Git.isDevelopBranch(env) == false
env = [BRANCH_NAME: 'develop']
assert Git.isDevelopBranch(env) == true

env = [BRANCH_NAME: 'master']
assert Git.isFeatureBranch(env) == false
env = [BRANCH_NAME: 'feature/ABC-123-xkcd']
assert Git.isFeatureBranch(env) == true

env = [BRANCH_NAME: 'master']
assert Git.isReleaseBranch(env) == false
env = [BRANCH_NAME: 'release/v1.2.3']
assert Git.isReleaseBranch(env) == true

env = [BRANCH_NAME: 'master']
assert Git.releaseBranchVersion(env) == ''
env = [BRANCH_NAME: 'release/v1.2.3']
assert Git.releaseBranchVersion(env) == 'v1.2.3'

env = [:]
assert Git.isPR(env) == false
env = [CHANGE_ID: '123']
assert Git.isPR(env) == true

env = [:]
assert Git.prId(env) == ''
env = [CHANGE_ID: '123']
assert Git.prId(env) == '123'

env = [:]
assert Git.prUrl(env) == ''
env = [CHANGE_URL: 'http://something/123']
assert Git.prUrl(env) == 'http://something/123'
