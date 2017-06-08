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
  
  private static String requote(String singleQuotedString) {
    return singleQuotedString.replaceAll("'", "\"");
  }
  
  @Test
  public void testMarshalSubscribe() {
    final UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
    final BindFrame orig = new BindFrame(id, "123", null, new String[]{"a", "a/b", "a/b/c"}, null, "some-meta");
    final String enc = wire.encode(orig);
    log("encoded: '%s'\n", enc);
    final String expected = requote("B {'type':'Bind','sessionId':'123','subscribe':['a','a/b','a/b/c'],'metadata':'some-meta','messageId':'123e4567-e89b-12d3-a456-426655440000'}");
    assertEquals(expected, enc);
    
    final BindFrame decoded = (BindFrame) wire.decode(enc);
    assertEquals(orig, decoded);
  }
  
  @Test
  public void testMarshalSubscribeResponse() {
    testEncodeDecode(new BindResponseFrame(new UUID(0l, 0l), new GeneralError("some-error")));
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
