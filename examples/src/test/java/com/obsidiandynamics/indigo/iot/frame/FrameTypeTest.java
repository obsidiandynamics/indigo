package com.obsidiandynamics.indigo.iot.frame;

import static org.junit.Assert.*;

import org.junit.*;

public final class FrameTypeTest {
  @Test
  public void testFromCharCode() {
    for (FrameType type : FrameType.values()) {
      final FrameType loaded = FrameType.fromCharCode(type.getCharCode());
      assertEquals(type, loaded);
    }
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidCharCode() {
    FrameType.fromCharCode('?');
  }
  
  @Test
  public void testFromByteCode() {
    for (FrameType type : FrameType.values()) {
      final FrameType loaded = FrameType.fromByteCode(type.getByteCode());
      assertEquals(type, loaded);
    }
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidByteCode() {
    FrameType.fromByteCode((byte) 0xFF);
  }
}
