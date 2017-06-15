package com.obsidiandynamics.indigo.iot;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertTrue;

import java.nio.*;
import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.AuthChain.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;

public final class PubAuthTest extends AbstractAuthTest {
  @Test(expected=NoAuthenticatorException.class)
  public void testEmptyPubChain() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault().clear(), AuthChain.createSubDefault());
  }

  @Test
  public void testDefaultPubChain() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault(), AuthChain.createSubDefault());
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();
    
    // bind to the test topic
    final BindFrame bind = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{"test"},
                                         new String[]{},
                                         null);
    final BindResponseFrame bindRes = remoteNexus.bind(bind).get();
    assertTrue(bindRes.isSuccess());

    // publish on a test topic and receive on the same topic
    remoteNexus.publish(new PublishTextFrame("test", "hello"));
    awaitReceived();
    assertNull(errors);
    assertEquals(new TextFrame("test", "hello"), text);
    clearReceived();
    
    final String ourTx = Flywheel.getTxTopicPrefix(sessionId);
    final String ourRx = Flywheel.getRxTopicPrefix(sessionId);
    
    // publish a binary on our TX topic; the test Edge is configured to echo so we expect a response on the RX topic
    remoteNexus.publish(new PublishBinaryFrame(ourTx, ByteBuffer.wrap("hello".getBytes())));
    awaitReceived();
    assertNull(errors);
    assertNull(text);
    assertEquals(new BinaryFrame(ourRx, ByteBuffer.wrap("hello".getBytes())), binary);
    clearReceived();
    
    // publish a text on our RX topic, which by default echoes to the RX
    remoteNexus.publish(new PublishTextFrame(ourRx, "hello"));
    awaitReceived();
    assertNull(errors);
    assertNull(binary);
    assertEquals(new TextFrame(ourRx, "hello"), text);
    clearReceived();
    
    final String illegalTx = "$remote/123456/tx";
    final String illegalRx = "$remote/123456/rx";
    
    // publish a text on someone else's TX topic; expect an error
    remoteNexus.publish(new PublishTextFrame(illegalTx, "hello"));
    awaitReceived();
    assertNotNull(errors);
    assertNull(text);
    assertNull(binary);
    assertEquals(1, errors.getErrors().length);
    assertEquals(new TopicAccessError("Restricted to " + Flywheel.getSessionTopicPrefix(sessionId) + "/#", illegalTx), 
                 errors.getErrors()[0]);
    clearReceived();
    
    // publish a binary on someone else's RX topic; expect an error
    remoteNexus.publish(new PublishBinaryFrame(illegalRx, ByteBuffer.wrap("hello".getBytes())));
    awaitReceived();
    assertNotNull(errors);
    assertNull(text);
    assertNull(binary);
    assertEquals(1, errors.getErrors().length);
    assertEquals(new TopicAccessError("Restricted to " + Flywheel.getSessionTopicPrefix(sessionId) + "/#", illegalRx), 
                 errors.getErrors()[0]);
    clearReceived();
  }
  
  @Test
  public void testCustomPubChain() throws Exception {
    final String customBasic = "custom/basic";
    final String customBearer = "custom/bearer";
    
    setupEdgeNode(AuthChain.createPubDefault()
                  .set(customBasic, Mocks.logger(createBasicAuth("user", "pass")))
                  .set(customBearer, Mocks.logger(createBearerAuth("token"))),
                  AuthChain.createSubDefault());
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();
    
    // publish to custom without binding; expect an error
    remoteNexus.publish(new PublishTextFrame(customBasic, "hello"));
    awaitReceived();
    assertEquals(1, errors.getErrors().length);
    assertNull(binary);
    assertNull(text);
    assertEquals(TopicAccessError.class, errors.getErrors()[0].getClass());
    clearReceived();
    
    remoteNexus.publish(new PublishTextFrame(customBasic + "/foo", "hello"));
    awaitReceived();
    assertEquals(1, errors.getErrors().length);
    assertNull(binary);
    assertNull(text);
    assertEquals(TopicAccessError.class, errors.getErrors()[0].getClass());
    clearReceived();
    
    remoteNexus.publish(new PublishBinaryFrame(customBearer, ByteBuffer.wrap("hello".getBytes())));
    awaitReceived();
    assertEquals(1, errors.getErrors().length);
    assertNull(binary);
    assertNull(text);
    assertEquals(TopicAccessError.class, errors.getErrors()[0].getClass());
    clearReceived();
    
    // bind custom/basic with the wrong password
    assertTrue(remoteNexus.bind(new BindFrame(UUID.randomUUID(), 
                                              sessionId,
                                              new BasicAuth("user", "badpass"),
                                              new String[] {customBasic, customBasic + "/#"},
                                              new String[]{},
                                              null)).get().isSuccess());

    // publish to custom/basic without with a bad auth; expect an error
    remoteNexus.publish(new PublishTextFrame(customBasic, "hello"));
    awaitReceived();
    assertEquals(1, errors.getErrors().length);
    assertNull(binary);
    assertNull(text);
    assertEquals(TopicAccessError.class, errors.getErrors()[0].getClass());
    clearReceived();
    
    remoteNexus.publish(new PublishTextFrame(customBasic + "/foo", "hello"));
    awaitReceived();
    assertEquals(1, errors.getErrors().length);
    assertNull(binary);
    assertNull(text);
    assertEquals(TopicAccessError.class, errors.getErrors()[0].getClass());
    clearReceived();
    
    // bind custom/bearer with the wrong password
    assertTrue(remoteNexus.bind(new BindFrame(UUID.randomUUID(), 
                                              sessionId,
                                              new BearerAuth("badtoken"),
                                              new String[] {customBearer, customBearer + "/#"},
                                              new String[]{},
                                              null)).get().isSuccess());
    
    // publish to custom/bearer without with a bad auth; expect an error
    remoteNexus.publish(new PublishBinaryFrame(customBearer, ByteBuffer.wrap("hello".getBytes())));
    awaitReceived();
    assertEquals(1, errors.getErrors().length);
    assertNull(binary);
    assertNull(text);
    assertEquals(TopicAccessError.class, errors.getErrors()[0].getClass());
    clearReceived();
    
    // bind custom/basic with the right password
    assertTrue(remoteNexus.bind(new BindFrame(UUID.randomUUID(), 
                                              sessionId,
                                              new BasicAuth("user", "pass"),
                                              new String[] {customBasic, customBasic + "/#"},
                                              new String[]{},
                                              null)).get().isSuccess());
    
    // publish to custom/basic with good auth; expect success
    remoteNexus.publish(new PublishTextFrame(customBasic, "hello"));
    awaitReceived();
    assertNull(errors);
    assertNull(binary);
    assertEquals(new TextFrame(customBasic, "hello"), text);
    clearReceived();
    
    remoteNexus.publish(new PublishTextFrame(customBasic + "/foo", "hello"));
    awaitReceived();
    assertNull(errors);
    assertNull(binary);
    assertEquals(new TextFrame(customBasic + "/foo", "hello"), text);
    clearReceived();
    
    // bind custom/bearer with the right token
    assertTrue(remoteNexus.bind(new BindFrame(UUID.randomUUID(), 
                                              sessionId,
                                              new BearerAuth("token"),
                                              new String[] {customBearer, customBearer + "/#"},
                                              new String[]{},
                                              null)).get().isSuccess());
    
    // publish to custom/bearer with good auth; expect success
    remoteNexus.publish(new PublishBinaryFrame(customBearer, ByteBuffer.wrap("hello".getBytes())));
    awaitReceived();
    assertNull(errors);
    assertNull(text);
    assertEquals(new BinaryFrame(customBearer, ByteBuffer.wrap("hello".getBytes())), binary);
    clearReceived();
  }
}
