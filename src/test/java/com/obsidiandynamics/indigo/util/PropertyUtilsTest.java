package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;

import java.io.*;
import java.util.*;

import org.junit.*;

public class PropertyUtilsTest {
  @Test
  public void testExistingValue() {
    final Properties props = new Properties();
    props.put("foo", "bar");
    assertEquals("bar", PropertyUtils.get(props, "foo", String::valueOf, "baz"));
  }
  
  @Test
  public void testDefaultValue() {
    assertEquals("bar", PropertyUtils.get(new Properties(), "foo", String::valueOf, "bar"));
  }
  
  @Test
  public void testLoadExisting() throws IOException {
    final Properties props = PropertyUtils.load("property-utils-test.properties");
    assertTrue(props.containsKey("foo"));
  }
  
  @Test
  public void testLoadExistingDefault() throws IOException {
    final Properties props = PropertyUtils.load("property-utils-test.properties", null);
    assertNotNull(props);
    assertTrue(props.containsKey("foo"));
  }
  
  @Test(expected=FileNotFoundException.class)
  public void testLoadNonExisting() throws IOException {
    PropertyUtils.load("non-existing.properties");
  }

  @Test
  public void testLoadNonExistingDefault() throws IOException {
    final Properties def = new Properties();
    def.put("foo", "bar");
    final Properties props = PropertyUtils.load("non-existing.properties", def);
    assertTrue(props.containsKey("foo"));
  }
}
