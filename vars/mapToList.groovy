#!/usr/bin/env groovy

def call(Map map) {
  def List list = []

  for (def key in map.keySet()) {
    list.add("${key}=${map[key]}")
  }

  return list
}
