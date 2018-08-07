import no.evry.Utils

// Map single cert
assert Utils.certListMap(['foo.crt', 'foo.key']) == [
  [crt: 'foo.crt', key: 'foo.key'],
]

// Custm file path
assert Utils.certListMap(['foo.crt', 'foo.key'], '/etc/certs/') == [
  [crt: '/etc/certs/foo.crt', key: '/etc/certs/foo.key'],
]

// Map multiple certs
assert Utils.certListMap(['foo.crt', 'foo.key', 'bar.crt', 'bar.key']) == [
  [crt: 'foo.crt', key: 'foo.key'],
  [crt: 'bar.crt', key: 'bar.key'],
]

// Ignore missing crt-file
assert Utils.certListMap(['foo.crt', 'foo.key', 'bar.key']) == [
  [crt: 'foo.crt', key: 'foo.key'],
]

// Ignore missing key-file
assert Utils.certListMap(['foo.crt', 'foo.key', 'bar.crt']) == [
  [crt: 'foo.crt', key: 'foo.key'],
]
