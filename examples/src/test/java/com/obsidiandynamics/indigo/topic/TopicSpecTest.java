package com.obsidiandynamics.indigo.topic;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.topic.TopicSpec.*;

public class TopicSpecTest {
  /**
   *  Tests topics without subscribers present.
   */
  @Test
  public void test1LevelEmpty() {
    final TopicSpec ts = TopicSpec.builder()
        .add(new NodeSpec(0, 0, 0).nodes(2))
        .build();
    assertEquals(Arrays.asList(Topic.of("0"), Topic.of("1")), ts.getLeafTopics());
    assertEquals(Arrays.asList(), ts.getExactInterests());
    assertEquals(Arrays.asList(), ts.getSingleLevelWildcardInterests());
    assertEquals(Arrays.asList(), ts.getMultiLevelWildcardInterests());
  }
  
  /**
   *  Tests a basic one-level structure with a number of subscribers of varying types.
   */
  @Test
  public void test1Level() {
    final TopicSpec ts = TopicSpec.builder()
        .add(new NodeSpec(1, 2, 3).nodes(2))
        .build();
    assertEquals(Arrays.asList(Topic.of("0"), Topic.of("1")), ts.getLeafTopics());
    assertEquals(Arrays.asList(new Interest(Topic.of("0"), 1), new Interest(Topic.of("1"), 1)), ts.getExactInterests());
    assertEquals(Arrays.asList(new Interest(Topic.of("+"), 2)), ts.getSingleLevelWildcardInterests());
    assertEquals(Arrays.asList(new Interest(Topic.of("#"), 3)), ts.getMultiLevelWildcardInterests());
  }
  
  /**
   *  Tests a three-level structure with different types of subscribers.
   */
  @Test
  public void test3Level() {
    final TopicSpec ts = TopicSpec.builder()
        .add(new NodeSpec(1, 2, 3).nodes(2))
        .add(new NodeSpec(4, 5, 6).nodes(3))
        .add(new NodeSpec(7, 8, 9).nodes(1))
        .build();
    assertEquals(Arrays.asList(Topic.of("0/0/0"), Topic.of("0/1/0"), Topic.of("0/2/0"),
                               Topic.of("1/0/0"), Topic.of("1/1/0"), Topic.of("1/2/0")), ts.getLeafTopics());
    
    assertEquals(Arrays.asList(new Interest(Topic.of("0"), 1), 
                               new Interest(Topic.of("0/0"), 4),
                               new Interest(Topic.of("0/0/0"), 7),
                               new Interest(Topic.of("0/1"), 4),
                               new Interest(Topic.of("0/1/0"), 7),
                               new Interest(Topic.of("0/2"), 4),
                               new Interest(Topic.of("0/2/0"), 7),
                               new Interest(Topic.of("1"), 1),
                               new Interest(Topic.of("1/0"), 4),
                               new Interest(Topic.of("1/0/0"), 7),
                               new Interest(Topic.of("1/1"), 4),
                               new Interest(Topic.of("1/1/0"), 7),
                               new Interest(Topic.of("1/2"), 4),
                               new Interest(Topic.of("1/2/0"), 7)), ts.getExactInterests());
    
    assertEquals(Arrays.asList(new Interest(Topic.of("+"), 2), 
                               new Interest(Topic.of("0/+"), 5),
                               new Interest(Topic.of("0/0/+"), 8),
                               new Interest(Topic.of("0/1/+"), 8),
                               new Interest(Topic.of("0/2/+"), 8),
                               new Interest(Topic.of("1/+"), 5),
                               new Interest(Topic.of("1/0/+"), 8),
                               new Interest(Topic.of("1/1/+"), 8),
                               new Interest(Topic.of("1/2/+"), 8)), ts.getSingleLevelWildcardInterests());
    
    assertEquals(Arrays.asList(new Interest(Topic.of("#"), 3), 
                               new Interest(Topic.of("0/#"), 6),
                               new Interest(Topic.of("0/0/#"), 9),
                               new Interest(Topic.of("0/1/#"), 9),
                               new Interest(Topic.of("0/2/#"), 9),
                               new Interest(Topic.of("1/#"), 6),
                               new Interest(Topic.of("1/0/#"), 9),
                               new Interest(Topic.of("1/1/#"), 9),
                               new Interest(Topic.of("1/2/#"), 9)), ts.getMultiLevelWildcardInterests());
  }
}
