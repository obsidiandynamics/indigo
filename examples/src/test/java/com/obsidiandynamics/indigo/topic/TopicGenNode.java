package com.obsidiandynamics.indigo.topic;

final class TopicGenNode {
  final String fragment;
  final TopicSpec spec;
  
  TopicGenNode(String fragment, TopicSpec spec) {
    this.fragment = fragment;
    this.spec = spec;
  }

  @Override
  public String toString() {
    return "TopicGenNode [fragment=" + fragment + ", spec=" + spec + "]";
  }
}
