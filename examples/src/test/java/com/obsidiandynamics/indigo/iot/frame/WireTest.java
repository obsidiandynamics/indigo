package com.obsidiandynamics.indigo.iot.frame;

import static com.obsidiandynamics.indigo.util.BinaryUtils.*;
import static org.junit.Assert.*;

import java.nio.*;
import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.frame.Wire.*;
import com.obsidiandynamics.indigo.util.*;

public final class WireTest implements TestSupport {
  private static String requote(String singleQuotedString) {
    return singleQuotedString.replaceAll("'", "\"");
  }
  
  @Test
  public void testBindEqualsHashcode() {
    final BindFrame b1 = new BindFrame(new UUID(0, 0), null, null, new String[0], new String[0], null);
    final BindFrame b2 = new BindFrame();
    assertEquals(b1.hashCode(), b2.hashCode());
    assertEquals(b1, b2);
  }
  
  @Test
  public void testBind() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    final UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
    final BindFrame orig = new BindFrame(id, "123", new BearerAuth("xyz"), 
                                         new String[]{"a", "a/b", "a/b/c"}, new String[0], "some-meta");
    final String enc = wire.encode(orig);
    log("encoded: '%s'\n", enc);
    final String expected = requote("B {"
        + "'type':'Bind','sessionId':'123',"
        + "'auth':{'type':'Bearer','token':'xyz'},"
        + "'subscribe':['a','a/b','a/b/c'],"
        + "'unsubscribe':[],"
        + "'metadata':'some-meta',"
        + "'messageId':'123e4567-e89b-12d3-a456-426655440000'"
        + "}");
    assertEquals(expected, enc);
    
    final BindFrame decoded = (BindFrame) wire.decode(enc);
    assertEquals(orig, decoded);
  }
  
  @Test
  public void testBindResponse() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    testEncodeDecode(wire, new BindResponseFrame(new UUID(0, 0), new GeneralError("some-error")));
  }
  
  @Test
  public void testBindWithHint() {
    final Wire wire = new Wire(false, LocationHint.EDGE);
    final String enc = requote("B {}");
    final BindFrame decoded = (BindFrame) wire.decode(enc);
    assertEquals(new BindFrame(), decoded);
  }
  
  @Test
  public void testBindResponseWithHint() {
    final Wire wire = new Wire(false, LocationHint.REMOTE);
    final String enc = requote("B {"
        + "'messageId'='00000000-0000-0000-0000-000000000000',"
        + "'errors'=[]"
        + "}");
    final BindResponseFrame decoded = (BindResponseFrame) wire.decode(enc);
    assertEquals(new BindResponseFrame(null), decoded);
  }
  
  @Test
  public void testMarshalPublishText() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    testEncodeDecode(wire, new PublishTextFrame("some/topic/to/publish", "some-payload"));
  }
  
  @Test
  public void testText() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    testEncodeDecode(wire, new TextFrame("some/topic", "some-text-here"));
  }
  
  @Test
  public void testPublishBinary() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    testEncodeDecode(wire, new PublishBinaryFrame("some/topic/to/publish", 
                                                  ByteBuffer.wrap(toByteArray(0x00, 0x01, 0x02))));
  }
  
  @Test
  public void testBinary() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    testEncodeDecode(wire, new BinaryFrame("some/topic", ByteBuffer.wrap(toByteArray(0x00, 0x01, 0x02))));
  }
  
  @Test
  public void testBinaryWithLong() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    final ByteBuffer buf = ByteBuffer.allocate(8);
    buf.putLong(System.nanoTime());
    buf.flip();
    testEncodeDecode(wire, new BinaryFrame("some/topic", buf));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testIncompleteSubscribeFrame() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    wire.decode("T");
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testIncompletePublishFrame() {
    final Wire wire = new Wire(false, LocationHint.UNSPECIFIED);
    wire.decode("P topic");
  }

  private void testEncodeDecode(Wire wire, TextEncodedFrame frame) {
    final String enc = wire.encode(frame);
    log("encoded: '%s'\n", enc);
    final Frame decoded = wire.decode(enc);
    log("decoded: %s\n", decoded);
    assertEquals(frame, decoded);
  }

  private void testEncodeDecode(Wire wire, BinaryEncodedFrame frame) {
    final ByteBuffer enc = wire.encode(frame);
    log("encoded: \n%s\n", dump(toByteArray(enc)));
    final BinaryEncodedFrame decoded = wire.decode(enc);
    log("decoded: %s\n", decoded);
    assertEquals(frame, decoded);
  }
}
