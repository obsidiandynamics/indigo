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
import com.obsidiandynamics.indigo.iot.frame.Error;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public class NodeCommsTest {
  private static final int PREFERRED_PORT = 6667;
  
  private Wire wire;

  private TopicBridge bridge;
  
  private RemoteNexusHandler handler;
 
  private EdgeNode edge;
  
  private RemoteNode remote;
  
  private int port;
  
  @Before
  public void setup() throws Exception {
    port = SocketTestSupport.getAvailablePort(PREFERRED_PORT);
    
    wire = new Wire(true);
    bridge = mock(TopicBridge.class);
    handler = mock(RemoteNexusHandler.class);
    
    edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = NodeCommsTest.this.port; }})
        .withWire(wire)
        .withTopicBridge(logger(bridge))
        .build();
    
    remote = RemoteNode.builder()
        .withWire(wire)
        .build();
  }
  
  @After
  public void teardown() throws Exception {
    if (edge != null) edge.close();
    if (remote != null) remote.close();
  }

  @Test
  public void testText() throws Exception {
    final UUID messageId = UUID.randomUUID();
    when(bridge.onBind(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    
    final RemoteNexus remoteNexus = remote.open(new URI("ws://localhost:" + port + "/"), logger(handler));
    final String sessionId = Long.toHexString(Crypto.machineRandom());
    final String[] subscribe = new String[]{"a/b/c"};
    final BindFrame bind = new BindFrame(messageId, 
                                         sessionId,
                                         null,
                                         subscribe, 
                                         null,
                                         "some-context");
    final BindResponseFrame bindRes = remoteNexus.bind(bind).get();
    
    assertTrue(bindRes.isSuccess());
    assertEquals(FrameType.BIND, bindRes.getType());
    assertArrayEquals(new Error[0], bindRes.getErrors());
    
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
    
    final Set<String> expectedTopics = new HashSet<>();
    expectedTopics.addAll(Arrays.asList(subscribe));
    expectedTopics.add(Flywheel.getRxTopicPrefix(sessionId));
    expectedTopics.add(Flywheel.getRxTopicPrefix(sessionId) + "/#");
    ordered(bridge, inOrder -> {
      inOrder.verify(bridge).onConnect(anyNotNull());
      inOrder.verify(bridge).onBind(anyNotNull(), eq(expectedTopics));
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
    final UUID messageId = UUID.randomUUID();
    when(bridge.onBind(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    
    final RemoteNexus remoteNexus = remote.open(new URI("ws://localhost:" + port + "/"), logger(handler));
    final String sessionId = Long.toHexString(Crypto.machineRandom());
    final String[] subscribe = new String[]{"a/b/c"};
    final BindFrame bind = new BindFrame(messageId, 
                                         sessionId,
                                         null,
                                         subscribe, 
                                         null,
                                         "some-context");
    final BindResponseFrame bindRes = remoteNexus.bind(bind).get();
    
    assertTrue(bindRes.isSuccess());
    assertEquals(FrameType.BIND, bindRes.getType());
    assertArrayEquals(new Error[0], bindRes.getErrors());
    
    final PublishBinaryFrame pubRemote = new PublishBinaryFrame("x/y/z", 
                                                                ByteBuffer.wrap("hello from remote".getBytes()));
    remoteNexus.publish(pubRemote);
    
    final EdgeNexus nexus = edge.getNexuses().get(0);
    final BinaryFrame binaryEdge = new BinaryFrame("l/m/n", ByteBuffer.wrap("hello from edge".getBytes()));
    nexus.send(binaryEdge).get();
    
    remoteNexus.close();
    
    given().ignoreException(AssertionError.class).await().atMost(10, SECONDS).untilAsserted(() -> {
      verify(bridge).onDisconnect(anyNotNull());
      verify(handler).onDisconnect(anyNotNull());
    });

    final Set<String> expectedTopics = new HashSet<>();
    expectedTopics.addAll(Arrays.asList(subscribe));
    expectedTopics.add(Flywheel.getRxTopicPrefix(sessionId));
    expectedTopics.add(Flywheel.getRxTopicPrefix(sessionId) + "/#");
    ordered(bridge, inOrder -> {
      inOrder.verify(bridge).onConnect(anyNotNull());
      inOrder.verify(bridge).onBind(anyNotNull(), eq(expectedTopics));
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
