package com.obsidiandynamics.indigo.iot;

import static com.obsidiandynamics.indigo.util.Mocks.*;
import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public class NodeCommsTest {
  private static final int PORT = 6667;
  
  private Wire wire;

  private TopicBridge bridge;
  
  private RemoteNexusHandler handler;
 
  private EdgeNode edge;
  
  private RemoteNode remote;
  
  @Before
  public void setup() throws Exception {
    wire = new Wire(true);
    bridge = mock(TopicBridge.class);
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
  public void testText() throws Exception {
    final UUID subId = UUID.randomUUID();
    final SubscribeResponseFrame mockSubRes = new SubscribeResponseFrame(subId, null);
    when(bridge.onSubscribe(any(), any())).thenReturn(CompletableFuture.completedFuture(mockSubRes));
    
    final RemoteNexus remoteNexus = remote.open(new URI("ws://localhost:" + PORT + "/"), logger(handler));
    final SubscribeFrame sub = new SubscribeFrame(subId, Long.toHexString(Crypto.machineRandom()),
                                                  new String[]{"a/b/c"}, "some-context");
    final SubscribeResponseFrame subRes = remoteNexus.subscribe(sub).get();
    
    assertTrue(subRes.isSuccess());
    assertEquals(FrameType.SUBSCRIBE, subRes.getType());
    assertNull(subRes.getError());
    
    final PublishTextFrame pubRemote = new PublishTextFrame("x/y/z", "hello from remote");
    remoteNexus.publish(pubRemote);
    
    final EdgeNexus edgeNexus = edge.getNexuses().get(0);
    final TextFrame textEdge = new TextFrame("l/m/n", "hello from edge");
    edgeNexus.send(textEdge).get();
    
    remoteNexus.close();
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(bridge).onDisconnect(anyNotNull());
      verify(handler).onDisconnect(anyNotNull());
    });
    
    ordered(bridge, inOrder -> {
      inOrder.verify(bridge).onConnect(anyNotNull());
      inOrder.verify(bridge).onSubscribe(anyNotNull(), eq(sub));
      inOrder.verify(bridge).onPublish(anyNotNull(), eq(pubRemote));
      inOrder.verify(bridge).onDisconnect(anyNotNull());
    });
    
    ordered(handler, inOrder -> {
      inOrder.verify(handler).onConnect(anyNotNull());
      inOrder.verify(handler).onText(anyNotNull(), eq(textEdge.getTopic()), eq(textEdge.getPayload()));
      inOrder.verify(handler).onDisconnect(anyNotNull());
    });
  }

  @Test
  public void testBinary() throws Exception {
    final UUID subId = UUID.randomUUID();
    final SubscribeResponseFrame mockSubRes = new SubscribeResponseFrame(subId, null);
    when(bridge.onSubscribe(any(), any())).thenReturn(CompletableFuture.completedFuture(mockSubRes));
    
    final RemoteNexus session = remote.open(new URI("ws://localhost:" + PORT + "/"), logger(handler));
    final SubscribeFrame sub = new SubscribeFrame(subId, Long.toHexString(Crypto.machineRandom()),
                                                  new String[]{"a/b/c"}, "some-context");
    final SubscribeResponseFrame subRes = session.subscribe(sub).get();
    
    assertTrue(subRes.isSuccess());
    assertEquals(FrameType.SUBSCRIBE, subRes.getType());
    assertNull(subRes.getError());
    
    final PublishBinaryFrame pubRemote = new PublishBinaryFrame("x/y/z", 
                                                                ByteBuffer.wrap("hello from remote".getBytes()));
    session.publish(pubRemote);
    
    final EdgeNexus nexus = edge.getNexuses().get(0);
    final BinaryFrame binaryEdge = new BinaryFrame("l/m/n", ByteBuffer.wrap("hello from edge".getBytes()));
    nexus.send(binaryEdge).get();
    
    session.close();
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(bridge).onDisconnect(anyNotNull());
      verify(handler).onDisconnect(anyNotNull());
    });
    
    ordered(bridge, inOrder -> {
      inOrder.verify(bridge).onConnect(anyNotNull());
      inOrder.verify(bridge).onSubscribe(anyNotNull(), eq(sub));
      inOrder.verify(bridge).onPublish(anyNotNull(), eq(pubRemote));
      inOrder.verify(bridge).onDisconnect(anyNotNull());
    });
    
    ordered(handler, inOrder -> {
      inOrder.verify(handler).onConnect(anyNotNull());
      inOrder.verify(handler).onBinary(anyNotNull(), eq(binaryEdge.getTopic()), eq(binaryEdge.getPayload()));
      inOrder.verify(handler).onDisconnect(anyNotNull());
    });
  }
}
