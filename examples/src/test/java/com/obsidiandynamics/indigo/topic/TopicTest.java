package com.obsidiandynamics.indigo.topic;

import static org.junit.Assert.*;

import org.junit.*;

public final class TopicTest {
  @Test
  public void testToString() {
    assertEquals("", Topic.root().toString());
    assertEquals("", Topic.of("").toString());
    assertEquals("a/b/c", Topic.of("a/b/c").toString());
  }
  
  @Test
  public void testParts() {
    assertArrayEquals(new String[]{"a", "b", "c"}, Topic.of("a/b/c").getParts());
  }
  
  @Test
  public void testAppend() {
    assertEquals("a", Topic.root().append("a").toString());
    assertEquals("a/b/c/d", Topic.of("a/b/c").append("d").toString());
  }
  
  @Test
  public void testSubtopic() {
    assertEquals("a/b", Topic.of("a/b/c").subtopic(0, 2).toString());
    assertEquals("b/c", Topic.of("a/b/c").subtopic(1, 3).toString());
  }
  
  @Test
  public void testTopicLength() {
    assertEquals(0, Topic.root().length());
    assertEquals(3, Topic.of("a/b/c").length());
  }
}
