package com.obsidiandynamics.indigo.topic;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.topic.TopicGen.*;

public class TopicGenTest {
  /**
   *  Tests topics without subscribers present.
   */
  @Test
  public void test1LevelEmpty() {
    final TopicGen gen = TopicGen.builder()
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .build();
    assertEquals(Arrays.asList(Topic.of("0"), Topic.of("1")), gen.getLeafTopics());
    assertEquals(Arrays.asList(), gen.getExactInterests());
    assertEquals(Arrays.asList(), gen.getSingleLevelWildcardInterests());
    assertEquals(Arrays.asList(), gen.getMultiLevelWildcardInterests());
  }
  
  /**
   *  Tests a basic one-level structure with a number of subscribers of varying types.
   */
  @Test
  public void test1Level() {
    final TopicGen gen = TopicGen.builder()
        .add(new TopicSpec(1, 2, 3).nodes(2))
        .build();
    assertEquals(Arrays.asList(Topic.of("0"), Topic.of("1")), gen.getLeafTopics());
    assertEquals(Arrays.asList(new Interest(Topic.of("0"), 1), new Interest(Topic.of("1"), 1)), gen.getExactInterests());
    assertEquals(Arrays.asList(new Interest(Topic.of("+"), 2)), gen.getSingleLevelWildcardInterests());
    assertEquals(Arrays.asList(new Interest(Topic.of("#"), 3)), gen.getMultiLevelWildcardInterests());
  }
  
  /**
   *  Tests a three-level structure with different types of subscribers.
   */
  @Test
  public void test3Level() {
    final TopicGen gen = TopicGen.builder()
        .add(new TopicSpec(1, 2, 3).nodes(2))
        .add(new TopicSpec(4, 5, 6).nodes(3))
        .add(new TopicSpec(7, 8, 9).nodes(1))
        .build();
    assertEquals(Arrays.asList(Topic.of("0/0/0"), Topic.of("0/1/0"), Topic.of("0/2/0"),
                               Topic.of("1/0/0"), Topic.of("1/1/0"), Topic.of("1/2/0")), gen.getLeafTopics());
    
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
                               new Interest(Topic.of("1/2/0"), 7)), gen.getExactInterests());
    
    assertEquals(Arrays.asList(new Interest(Topic.of("+"), 2), 
                               new Interest(Topic.of("0/+"), 5),
                               new Interest(Topic.of("0/0/+"), 8),
                               new Interest(Topic.of("0/1/+"), 8),
                               new Interest(Topic.of("0/2/+"), 8),
                               new Interest(Topic.of("1/+"), 5),
                               new Interest(Topic.of("1/0/+"), 8),
                               new Interest(Topic.of("1/1/+"), 8),
                               new Interest(Topic.of("1/2/+"), 8)), gen.getSingleLevelWildcardInterests());
    
    assertEquals(Arrays.asList(new Interest(Topic.of("#"), 3), 
                               new Interest(Topic.of("0/#"), 6),
                               new Interest(Topic.of("0/0/#"), 9),
                               new Interest(Topic.of("0/1/#"), 9),
                               new Interest(Topic.of("0/2/#"), 9),
                               new Interest(Topic.of("1/#"), 6),
                               new Interest(Topic.of("1/0/#"), 9),
                               new Interest(Topic.of("1/1/#"), 9),
                               new Interest(Topic.of("1/2/#"), 9)), gen.getMultiLevelWildcardInterests());
  }
}
