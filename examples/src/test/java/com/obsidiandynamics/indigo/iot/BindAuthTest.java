package com.obsidiandynamics.indigo.iot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.AuthChain.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;

public final class BindAuthTest extends AbstractAuthTest {
  @Test(expected=NoAuthenticatorException.class)
  public void testEmptySubChain() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault(), AuthChain.createSubDefault().clear());
  }

  @Test
  public void testDefaultSubChain() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault(), AuthChain.createSubDefault());
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();

    // bind to our own RX topic
    final BindFrame bind1 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{"a/b/c",
                                                      Flywheel.getRxTopicPrefix(sessionId),
                                                      Flywheel.getRxTopicPrefix(sessionId) + "/#"}, 
                                         null,
                                         null);
    final BindResponseFrame bind1Res = remoteNexus.bind(bind1).get();
    assertTrue(bind1Res.isSuccess());
    
    // bind to someone else's RX and TX topics
    final BindFrame bind2 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{Flywheel.getRxTopicPrefix(sessionId) + "/#",
                                                      Flywheel.getRxTopicPrefix("12345") + "/#",
                                                      Flywheel.getTxTopicPrefix("12346") + "/#"}, 
                                         null,
                                         null);
    final BindResponseFrame bind2Res = remoteNexus.bind(bind2).get();
    assertEquals(2, bind2Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind2Res.getErrors()[0].getClass());
    assertEquals(TopicAccessError.class, bind2Res.getErrors()[1].getClass());

    // bind to the TX topic; should pass
    final BindFrame bind3 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{"a/b/c",
                                                      Flywheel.getTxTopicPrefix(sessionId),
                                                      Flywheel.getTxTopicPrefix(sessionId) + "/#"}, 
                                         null,
                                         null);
    final BindResponseFrame bind3Res = remoteNexus.bind(bind3).get();
    assertTrue(bind3Res.isSuccess());
  }
  
  @Test
  public void testCustomSubChain() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault(),
                  AuthChain.createSubDefault()
                  .set("custom/basic", Mocks.logger(createBasicAuth("user", "pass")))
                  .set("custom/bearer", Mocks.logger(createBearerAuth("token"))));
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();

    // test with the right user/pass on the correct topic; should pass
    final BindFrame bind1 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BasicAuth("user", "pass"),
                                         new String[]{"a/b/c",
                                                      Flywheel.getRxTopicPrefix(sessionId) + "/#",
                                                      "custom/basic/1"}, 
                                         null,
                                         null);
    final BindResponseFrame bind1Res = remoteNexus.bind(bind1).get();
    assertTrue(bind1Res.isSuccess());

    // test with a wrong password; should fail
    final BindFrame bind2 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BasicAuth("user", "badpass"),
                                         new String[]{"a/b/c",
                                                      Flywheel.getRxTopicPrefix(sessionId) + "/#",
                                                      "custom/basic/2"}, 
                                         null,
                                         null);
    final BindResponseFrame bind2Res = remoteNexus.bind(bind2).get();
    assertEquals(1, bind2Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind2Res.getErrors()[0].getClass());

    // test with a wrong password; should fail
    final BindFrame bind3 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BasicAuth("user", "badpass"),
                                         new String[]{"custom/basic"}, 
                                         null,
                                         null);
    final BindResponseFrame bind3Res = remoteNexus.bind(bind3).get();
    assertEquals(1, bind3Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind3Res.getErrors()[0].getClass());
    
    // test with a null auth object; should fail
    final BindFrame bind4 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{"custom/basic/4"}, 
                                         null,
                                         null);
    final BindResponseFrame bind4Res = remoteNexus.bind(bind4).get();
    assertEquals(1, bind4Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind4Res.getErrors()[0].getClass());

    // test with the right token on the correct topic; should pass
    final BindFrame bind5 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BearerAuth("token"),
                                         new String[]{"custom/bearer/1"}, 
                                         null,
                                         null);
    final BindResponseFrame bind5Res = remoteNexus.bind(bind5).get();
    assertTrue(bind5Res.isSuccess());

    // bind to a wildcard with the wrong token; should fail with multiple errors
    final BindFrame bind6 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BearerAuth("badtoken"),
                                         new String[]{"#"}, 
                                         null,
                                         null);
    final BindResponseFrame bind6Res = remoteNexus.bind(bind6).get();
    assertEquals(3, bind6Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind6Res.getErrors()[0].getClass());
    assertEquals(TopicAccessError.class, bind6Res.getErrors()[1].getClass());
    assertEquals(TopicAccessError.class, bind6Res.getErrors()[2].getClass());
  }
}
