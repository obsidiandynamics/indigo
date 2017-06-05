package com.obsidiandynamics.indigo.topic;

public final class TopicLibrary {
  private TopicLibrary() {}
  
  public static TopicSpec singleton(int subscribers) {
    return TopicSpec.builder()
        .add(new NodeSpec(subscribers, 0, 0).nodes(1))
        .build();
  }
  
  public static TopicSpec shrub(int leaves) {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 0, 0).nodes(leaves))
        .build();
  }
  
  public static TopicSpec smallLeaves() {
    return TopicSpec.builder()
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  public static TopicSpec mediumLeaves() {
    return TopicSpec.builder()
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(0, 0, 0).nodes(5))
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  public static TopicSpec largeLeaves() {
    return TopicSpec.builder()
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(0, 0, 0).nodes(5))
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(0, 0, 0).nodes(5))
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  public static TopicSpec jumboLeaves() {
    return TopicSpec.builder()
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(0, 0, 0).nodes(5))
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(0, 0, 0).nodes(5))
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(0, 0, 0).nodes(5))
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .add(new NodeSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  public static TopicSpec tiny() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(1))
        .build();
  }
  
  public static TopicSpec small() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  public static TopicSpec medium() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  public static TopicSpec large() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  public static TopicSpec jumbo() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  public static TopicSpec mriya() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
}
