#!/usr/bin/env groovy
secret = new k8sSecretKeys()

def yamlString = """
apiVersion: v1
kind: Secret
data:
  foo.crt: aa
  foo.key: bb
""".trim()

def yamlMap = [
  apiVersion: 'v1',
  kind: 'Secret',
  data: [
    ('foo.crt'): 'aa',
    ('foo.key'): 'bb',
  ],
]

def shOk = { Map args ->
  def str = args.script ?: ''
  def std = args.returnStdout ?: false

  if (str == 'kubectl version') {
    assert std == false
    return
  } else if (str == 'kubectl get secret name -n ns -o yaml') {
    assert std == true
    return yamlString
  } else {
    throw new Exception("Unexpected script '${str}")
  }
}

def shErr = { Map args ->
  def str = args.script ?: ''
  def std = args.returnStdout ?: false

  if (str == 'kubectl version') {
    assert std == false
    return
  } else {
    throw new Exception("Shell Script Exception")
  }
}

def readYaml = { Map args ->
  assert args.text == yamlString
  return yamlMap
}

secret.metaClass.file = { }

// Test Success
secret.metaClass.withCredentials = { List args1, Closure cl1 ->
  cl1.docker = [image: { return [inside: { String args2, Closure cl2 ->
    cl2.sh = shOk
    cl2.readYaml = readYaml
    cl2()
  }]}]

  cl1()
}
assert secret.call('name', 'ns', 'cluster') == ['foo.crt', 'foo.key']

// Test Shell Error
secret.metaClass.withCredentials = { List args1, Closure cl1 ->
  cl1.docker = [image: { return [inside: { String args2, Closure cl2 ->
    cl2.sh = shErr
    cl2.readYaml = readYaml
    cl2()
  }]}]

  cl1()
}
assert secret.call('name', 'ns', 'cluster') == []
