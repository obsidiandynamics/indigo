package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

public class ActorRefTest {
  @Test
  public void testRoleOnly() {
    final ActorRef ref = ActorRef.of("foo");
    assertEquals("foo", ref.role());
    assertNull(ref.key());
  }
  
  @Test
  public void testRoleKey() {
    final ActorRef ref = ActorRef.of("foo", "bar");
    assertEquals("foo", ref.role());
    assertEquals("bar", ref.key());
  }
  
  @Test
  public void testEncodeRoleOnly() {
    assertEquals("foo", ActorRef.of("foo").encode()); 
  }
  
  @Test
  public void testEncodeRoleOnlyEscape() {
    assertEquals("foo\\:", ActorRef.of("foo:").encode());
    assertEquals("\\:\\:foo\\:", ActorRef.of("::foo:").encode());
  }
  
  @Test
  public void testEncodeRoleKey() {
    assertEquals("foo:bar", ActorRef.of("foo", "bar").encode()); 
  }
  
  @Test
  public void testEncodeRoleKeyEscape() {
    assertEquals("foo\\::\\:bar", ActorRef.of("foo:", ":bar").encode()); 
  }
}
