package com.obsidiandynamics.indigo.topic;

import java.util.*;

public final class NodeSpec {
  final int exacts;
  final int slWildcards;
  final int mlWildcards;
  
  public NodeSpec(int exacts, int slWildcards, int mlWildcards) {
    this.exacts = exacts;
    this.slWildcards = slWildcards;
    this.mlWildcards = mlWildcards;
  }
  
  public List<TopicNode> nodes(int numNodes) {
    final List<TopicNode> nodes = new ArrayList<>();
    for (int i = 0; i < numNodes; i++) {
      nodes.add(new TopicNode(String.valueOf(i), this));
    }
    return nodes;
  }

  @Override
  public String toString() {
    return "TopicSpec [exacts=" + exacts + ", (+)=" + slWildcards + ", (#)=" + mlWildcards + "]";
  }
}
