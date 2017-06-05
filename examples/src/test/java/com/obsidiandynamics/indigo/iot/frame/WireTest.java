package com.obsidiandynamics.indigo.iot.frame;

import static com.obsidiandynamics.indigo.util.BinaryUtils.*;
import static org.junit.Assert.*;

import java.nio.*;
import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class WireTest implements TestSupport {
  private Wire wire;
  
  @Before
  public void setup() {
    wire = new Wire(false);
  }
  
  @Test
  public void testMarshalSubscribe() {
    final UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
    final SubscribeFrame orig = new SubscribeFrame(id, "123", new String[]{"a", "a/b", "a/b/c"}, "some-context");
    final String enc = wire.encode(orig);
    log("encoded: '%s'\n", enc);
    assertEquals("S {\"type\":\"Subscribe\",\"remoteId\":\"123\",\"topics\":[\"a\",\"a/b\",\"a/b/c\"],\"context\":\"some-context\",\"id\":\"123e4567-e89b-12d3-a456-426655440000\"}", enc);
    
    final SubscribeFrame decoded = (SubscribeFrame) wire.decode(enc);
    assertEquals(orig, decoded);
  }
  
  @Test
  public void testMarshalSubscribeResponse() {
    testEncodeDecode(new SubscribeResponseFrame(new UUID(0l, 0l), "some-error"));
  }
  
  @Test
  public void testMarshalPublishText() {
    testEncodeDecode(new PublishTextFrame("some/topic/to/publish", "some-payload"));
  }
  
  @Test
  public void testMarshalText() {
    testEncodeDecode(new TextFrame("some/topic", "some-text-here"));
  }
  
  @Test
  public void testMarshalPublishBinary() {
    testEncodeDecode(new PublishBinaryFrame("some/topic/to/publish", 
                                            ByteBuffer.wrap(toByteArray(0x00, 0x01, 0x02))));
  }
  
  @Test
  public void testMarshalBinary() {
    testEncodeDecode(new BinaryFrame("some/topic", ByteBuffer.wrap(toByteArray(0x00, 0x01, 0x02))));
  }
  
  @Test
  public void testMarshalBinaryWithLong() {
    final ByteBuffer buf = ByteBuffer.allocate(8);
    buf.putLong(System.nanoTime());
    buf.flip();
    testEncodeDecode(new BinaryFrame("some/topic", buf));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testIncompleteSubscribeFrame() {
    wire.decode("T");
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testIncompletePublishFrame() {
    wire.decode("P topic");
  }

  private void testEncodeDecode(TextEncodedFrame frame) {
    final String enc = wire.encode(frame);
    log("encoded: '%s'\n", enc);
    final Frame decoded = wire.decode(enc);
    log("decoded: %s\n", decoded);
    assertEquals(frame, decoded);
  }

  private void testEncodeDecode(BinaryEncodedFrame frame) {
    final ByteBuffer enc = wire.encode(frame);
    log("encoded: \n%s\n", dump(toByteArray(enc)));
    final BinaryEncodedFrame decoded = wire.decode(enc);
    log("decoded: %s\n", decoded);
    assertEquals(frame, decoded);
  }
}
