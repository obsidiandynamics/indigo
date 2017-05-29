package com.obsidiandynamics.indigo.iot;

import static com.obsidiandynamics.indigo.util.Mocks.*;
import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.ws.*;

public class NodeRouterTest {
  private static final int PORT = 6667;
  
  private Wire wire;

  private TopicBridge bridge;
  
  private RemoteNexusHandler handler;
 
  private EdgeNode edge;
  
  private RemoteNode remote;
  
  @Before
  public void setup() throws Exception {
    wire = new Wire(true);
    bridge = new RoutingTopicBridge();
    handler = mock(RemoteNexusHandler.class);
    
    edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = PORT; }})
        .withWire(wire)
        .withTopicBridge(logger(bridge))
        .build();
    
    remote = RemoteNode.builder()
        .withWire(wire)
        .build();
  }
  
  @After
  public void teardown() throws Exception {
    edge.close();
    remote.close();
  }

  @Test
  public void testInternalPubSub() throws Exception {
    final UUID subId = UUID.randomUUID();
    final RemoteNexus remoteNexus = remote.open(new URI("ws://localhost:" + PORT + "/"), logger(handler));

    final String payload = "hello internal";
    edge.publish("a/b/c", payload); // no subscriber yet - shouldn't be received
    
    final SubscribeFrame sub = new SubscribeFrame(subId, new String[]{"a/b/c"}, "some-context");
    final SubscribeResponseFrame subRes = remoteNexus.subscribe(sub).get();
    
    assertTrue(subRes.isSuccess());
    assertEquals(FrameType.SUBSCRIBE, subRes.getType());
    assertNull(subRes.getError());

    ordered(handler, inOrder -> { // shouldn't have received any data yet
      inOrder.verify(handler).onConnect(anyNotNull());
    });
    
    edge.publish("a/b/c", payload); // a single subscriber at this point
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(handler).onText(anyNotNull(), eq(payload));
    });
    
    remoteNexus.close();
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(handler).onDisconnect(anyNotNull());
    });
    
    ordered(handler, inOrder -> {
      inOrder.verify(handler).onConnect(anyNotNull());
      inOrder.verify(handler).onText(anyNotNull(), eq(payload));
      inOrder.verify(handler).onDisconnect(anyNotNull());
    });
  }

  @Test
  public void testExternalPubSub() throws Exception {
    final UUID subId = UUID.randomUUID();
    final RemoteNexus remoteNexus = remote.open(new URI("ws://localhost:" + PORT + "/"), logger(handler));

    final String payload = "hello external";
    remoteNexus.publish(new PublishTextFrame("a/b/c", payload)); // no subscriber yet - shouldn't be received
    
    final SubscribeFrame sub = new SubscribeFrame(subId, new String[]{"a/b/c"}, "some-context");
    final SubscribeResponseFrame subRes = remoteNexus.subscribe(sub).get();
    
    assertTrue(subRes.isSuccess());
    assertEquals(FrameType.SUBSCRIBE, subRes.getType());
    assertNull(subRes.getError());

    ordered(handler, inOrder -> { // shouldn't have received any data yet
      inOrder.verify(handler).onConnect(anyNotNull());
    });
    
    remoteNexus.publish(new PublishTextFrame("a/b/c", payload)); // itself is a subscriber
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(handler).onText(anyNotNull(), eq(payload));
    });
    
    remoteNexus.close();
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(handler).onDisconnect(anyNotNull());
    });
    
    ordered(handler, inOrder -> {
      inOrder.verify(handler).onConnect(anyNotNull());
      inOrder.verify(handler).onText(anyNotNull(), eq(payload));
      inOrder.verify(handler).onDisconnect(anyNotNull());
    });
  }
}
