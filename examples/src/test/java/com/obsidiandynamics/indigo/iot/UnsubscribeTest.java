package com.obsidiandynamics.indigo.iot;

import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import org.junit.*;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertTrue;

public final class UnsubscribeTest extends AbstractAuthTest {
  @Test
  public void testSubUnsub() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault(), AuthChain.createSubDefault());
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();
    
    // bind to topics a and b
    final BindFrame bind1 = new BindFrame(UUID.randomUUID(),
                                          sessionId,
                                          null,
                                          new String[]{"a", "b"},
                                           new String[]{},
                                         null);
    final BindResponseFrame bind1Res = remoteNexus.bind(bind1).get();
    assertTrue(bind1Res.isSuccess());

    edge.publish("a", "hello");
    awaitReceived();
    assertNull(errors);
    assertEquals(new TextFrame("a", "hello"), text);
    assertNull(binary);
    clearReceived();

    edge.publish("b", "hello");
    awaitReceived();
    assertNull(errors);
    assertEquals(new TextFrame("b", "hello"), text);
    assertNull(binary);
    clearReceived();

    // bind to topics a and b
    final BindFrame bind2 = new BindFrame(UUID.randomUUID(),
                                          sessionId,
                                          null,
                                          new String[] {},
                                          new String[] { "b" },
                                          null);
    final BindResponseFrame bind2Res = remoteNexus.bind(bind2).get();
    assertTrue(bind2Res.isSuccess());

    edge.publish("a", "hello");
    awaitReceived();
    assertNull(errors);
    assertEquals(new TextFrame("a", "hello"), text);
    assertNull(binary);
    clearReceived();

    edge.publish("b", "hello");
    TestSupport.sleep(50);
    assertNull(errors);
    assertNull(text);
    assertNull(binary);
    clearReceived();
  }
}
