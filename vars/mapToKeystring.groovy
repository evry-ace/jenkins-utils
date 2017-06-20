#!/usr/bin/env groovy

def call(Map map) {
  def List list = []

  for (def key in map.keySet()) {
    list.add("\$${key}")
  }

  def String str = list.join ' '

  return str
}
