package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;
import static org.junit.Assert.*;

import org.junit.*;

public class ActorRefTest {
  @Test
  public void testRoleOnly() {
    final ActorRef ref = ActorRef.of("foo");
    assertEquals("foo", ref.role());
    Assert.assertNull(ref.key());
  }
  
  @Test
  public void testRoleKey() {
    final ActorRef ref = ActorRef.of("foo", "bar");
    assertEquals("foo", ref.role());
    assertEquals("bar", ref.key());
  }
  
  @Test
  public void testEqualsSame() {
    final ActorRef ref = ActorRef.of("foo");
    Assert.assertEquals(ref, ref);
  }
  
  @Test
  public void testEqualsNull() {
    assertNotEquals(null, ActorRef.of("foo"));
  }
  
  @Test
  public void testEqualsWrongType() {
    assertNotEquals(3, ((Object) ActorRef.of("foo")));
  }
  
  @Test
  public void testEqualsWrongRole() {
    assertNotEquals(ActorRef.of("foo"), ActorRef.of("oof"));
  }
  
  @Test
  public void testEqualThisNullKey() {
    assertNotEquals(ActorRef.of("foo"), ActorRef.of("foo", "bar"));
  }
  
  @Test
  public void testEqualsOtherNullKey() {
    assertNotEquals(ActorRef.of("foo", "bar"), ActorRef.of("foo"));
  }
  
  @Test
  public void testEqualsWrongKey() {
    assertNotEquals(ActorRef.of("foo", "bar"), ActorRef.of("foo", "baz"));
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
