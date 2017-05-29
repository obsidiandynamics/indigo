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
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public class NexusRouterTest {
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
    edge = new EdgeNode(UndertowServer.factory(), 
                        new WSServerConfig()  {{ port = PORT; }}, 
                        wire,
                        logger(bridge));
    remote = new RemoteNode(UndertowClient.factory(),
                            new WSClientConfig(),
                            wire);
  }
  
  @After
  public void teardown() throws Exception {
    edge.close();
    remote.close();
  }

  @Test
  public void testInternalPub() throws Exception {
    final UUID subId = UUID.randomUUID();
    final RemoteNexus remoteNexus = remote.open(new URI("ws://localhost:" + PORT + "/"), logger(handler));
    final SubscribeFrame sub = new SubscribeFrame(subId, new String[]{"a/b/c"}, "some-context");
    final SubscribeResponseFrame subRes = remoteNexus.subscribe(sub).get();
    
    assertTrue(subRes.isSuccess());
    assertEquals(FrameType.SUBSCRIBE, subRes.getType());
    assertNull(subRes.getError());
    
    final String payload = "hello internal";
    edge.publish("a/b/c", payload);
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(handler).onText(anyNotNull(), eq(payload));
    });
    
    remoteNexus.close();
    
    ordered(handler, inOrder -> {
      inOrder.verify(handler).onConnect(anyNotNull());
      inOrder.verify(handler).onText(anyNotNull(), eq(payload));
      inOrder.verify(handler).onDisconnect(anyNotNull());
    });
  }
}
