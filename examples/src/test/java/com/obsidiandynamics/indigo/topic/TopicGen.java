package com.obsidiandynamics.indigo.topic;

import java.util.*;

import com.obsidiandynamics.indigo.util.*;

final class TopicGen {
  private final Combinations<TopicGenNode> combs;
  
  TopicGen(List<List<TopicGenNode>> topicGenMatrix) {
    combs = new Combinations<>(topicGenMatrix);
  }
  
  static final class TopicGenBuilder {
    private final List<List<TopicGenNode>> matrix = new ArrayList<>();
    
    TopicGenBuilder add(List<TopicGenNode> nodes) {
      matrix.add(nodes);
      return this;
    }
    
    final TopicGen build() {
      return new TopicGen(matrix);
    }
  }
  
  static TopicGenBuilder builder() {
    return new TopicGenBuilder();
  }
}
