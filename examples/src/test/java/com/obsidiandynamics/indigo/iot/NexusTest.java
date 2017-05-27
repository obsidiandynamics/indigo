package com.obsidiandynamics.indigo.iot;

import static com.obsidiandynamics.indigo.util.Mocks.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.iot.client.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public class NexusTest {
  private static final int PORT = 6667;
  
  private Wire wire;
  
  @Before
  public void setup() {
    wire = new Wire(true);
  }

  @Test
  public void test() throws Exception {
    final UUID subId = UUID.randomUUID();
    final TopicBridge bridge = mock(TopicBridge.class);
    Mockito
    .when(bridge.onSubscribe(any(), any()))
    .thenReturn(CompletableFuture.completedFuture(new SubscribeResponseFrame(subId, null)));
    
    final SessionHandler handler = mock(SessionHandler.class);
    
    final Edge edge = new Edge(UndertowServer.factory(), 
                               new WSServerConfig()  {{ port = PORT; }}, 
                               wire,
                               bridge);
    
    final SessionManager manager = new SessionManager(UndertowClient.factory(),
                                                      new WSClientConfig(),
                                                      wire);
    
    final Session session = manager.open(new URI("ws://localhost:" + PORT + "/"), handler);
    final SubscribeFrame sub = new SubscribeFrame(subId, new String[]{"a/b/c"}, "some-context");
    final SubscribeResponseFrame subRes = session.subscribe(sub).get();
    
    assertTrue(subRes.isSuccess());
    assertEquals(FrameType.SUBSCRIBE, subRes.getType());
    assertNull(subRes.getError());
    
    final PublishFrame pub = new PublishFrame("x/y/z", "hello from publisher");
    session.publish(pub);
    
    edge.close();
    manager.close();
    
    ordered(bridge, inOrder -> {
      inOrder.verify(bridge).onConnect(anyNotNull());
      inOrder.verify(bridge).onSubscribe(anyNotNull(), eq(sub));
      inOrder.verify(bridge).onPublish(anyNotNull(), eq(pub));
    });
  }
}
