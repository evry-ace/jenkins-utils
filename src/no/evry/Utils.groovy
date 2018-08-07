#!/usr/bin/env groovy

package no.evry

class Utils {
  static def certListMap(def certs, path = '') {
    def map = [:]
    def list = []

    // Create Intermediate Map Representation
    certs.each { file ->
      def name = file.substring(0, file.lastIndexOf('.'))
      def type = file.substring(file.lastIndexOf('.') + 1)

      assert type in ['key', 'crt']

      map[name] = map[name] ?: [:]
      map[name][type] = "${path}${file}"
    }

    // Create Final List Representation
    map.each { k, v ->
      if (v.crt && v.key) {
        list.add(v)
      }
    }

    return list
  }
}
