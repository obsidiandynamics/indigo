package com.obsidiandynamics.indigo.topic;

import static org.junit.Assert.*;

import org.junit.*;

public final class TopicTest {
  @Test
  public void testToString() {
    assertEquals("", Topic.root().toString());
    assertEquals("a/b/c", Topic.of("a/b/c").toString());
  }
  
  @Test
  public void testInvalid() {
    assertException(IllegalArgumentException.class, ()-> Topic.of(""));
    assertException(IllegalArgumentException.class, ()-> Topic.of("//"));
    assertException(IllegalArgumentException.class, ()-> Topic.of("a/b/"));
    assertException(IllegalArgumentException.class, ()-> Topic.of("a/#/b"));
    assertException(IllegalArgumentException.class, ()-> Topic.of("a/++/b"));
    assertException(IllegalArgumentException.class, ()-> Topic.of("a/b/##"));
  }
  
  private static void assertException(Class<? extends Exception> exClass, Runnable r) {
    try {
      r.run();
      fail("Expected " + exClass.getName());
    } catch (Exception e) {
      assertEquals(exClass, e.getClass());
    }
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
  
  @Test
  public void testAcceptsExact() {
    assertTrue(Topic.of("a/b/c").accepts(Topic.of("a/b/c")));
    
    assertFalse(Topic.of("a/b/c").accepts(Topic.of("a/b")));
    assertFalse(Topic.of("a/b").accepts(Topic.of("a/b/c")));
    assertFalse(Topic.root().accepts(Topic.of("a/b/c")));
    assertFalse(Topic.of("x/b/c").accepts(Topic.of("a/b/c")));
    assertFalse(Topic.of("a/b/c").accepts(Topic.of("x/b/c")));
    assertFalse(Topic.of("a/b/c").accepts(Topic.root()));
  }
  
  @Test
  public void testAcceptsSingleLevelWildcard() {
    assertTrue(Topic.of("a/b/c").accepts(Topic.of("a/b/c")));
    assertTrue(Topic.of("+/b/c").accepts(Topic.of("a/b/c")));
    assertTrue(Topic.of("a/+/c").accepts(Topic.of("a/b/c")));
    assertTrue(Topic.of("a/b/+").accepts(Topic.of("a/b/c")));

    assertFalse(Topic.of("+").accepts(Topic.of("a/b")));
    assertFalse(Topic.of("a/b/+").accepts(Topic.of("a/b")));
    assertFalse(Topic.of("a/+").accepts(Topic.of("a/b/c")));
    assertFalse(Topic.of("x/+/c").accepts(Topic.of("a/b/c")));
    assertFalse(Topic.of("+").accepts(Topic.root()));
  }
  
  @Test
  public void testAcceptsMultiLevelWildcard() {
    assertTrue(Topic.of("a/b/#").accepts(Topic.of("a/b/c")));
    assertTrue(Topic.of("a/#").accepts(Topic.of("a/b")));
    assertTrue(Topic.of("a/#").accepts(Topic.of("a/b/c")));
    assertTrue(Topic.of("#").accepts(Topic.of("a/b/c")));
    
    assertFalse(Topic.of("x/b/#").accepts(Topic.of("a/b/c")));
    assertFalse(Topic.of("a/#").accepts(Topic.of("x/b/c")));
    assertFalse(Topic.of("#").accepts(Topic.root()));
  }
}
