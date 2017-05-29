package com.obsidiandynamics.indigo.topic;

import java.util.*;

public final class TopicSpec {
  final int exacts;
  final int slWildcards;
  final int mlWildcards;
  
  public TopicSpec(int exacts, int slWildcards, int mlWildcards) {
    this.exacts = exacts;
    this.slWildcards = slWildcards;
    this.mlWildcards = mlWildcards;
  }
  
  public List<TopicGenNode> nodes(int numNodes) {
    final List<TopicGenNode> nodes = new ArrayList<>();
    for (int i = 0; i < numNodes; i++) {
      nodes.add(new TopicGenNode(String.valueOf(i), this));
    }
    return nodes;
  }

  @Override
  public String toString() {
    return "TopicSpec [exacts=" + exacts + ", (+)=" + slWildcards + ", (#)=" + mlWildcards + "]";
  }
}
