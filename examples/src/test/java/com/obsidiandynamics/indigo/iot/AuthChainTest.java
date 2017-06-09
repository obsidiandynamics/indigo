package com.obsidiandynamics.indigo.iot;

import static com.obsidiandynamics.indigo.util.Mocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.AuthChain.*;
import com.obsidiandynamics.indigo.iot.edge.auth.Authenticator;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public class AuthChainTest {
  private static final int PREFERRED_PORT = 6667;
  private static final boolean SUPPRESS_LOGGING = true;
  
  private Wire wire;

  private RemoteNexusHandler handler;
 
  private EdgeNode edge;
  
  private RemoteNode remote;
  
  private RemoteNexus remoteNexus;
  
  private int port;
  
  @Before
  public void setup() throws Exception {
    port = SocketTestSupport.getAvailablePort(PREFERRED_PORT);
    
    wire = new Wire(true);
    handler = mock(RemoteNexusHandler.class);
    
    remote = RemoteNode.builder()
        .withWire(wire)
        .build();
  }
  
  @After
  public void teardown() throws Exception {
    if (remoteNexus != null) remoteNexus.close();
    if (edge != null) edge.close();
    if (remote != null) remote.close();
  }
  
  private void setupEdgeNode(AuthChain authChain) throws Exception {
    edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = AuthChainTest.this.port; }})
        .withWire(wire)
        .withSubAuthChain(authChain)
        .build();
    edge.setLoggingEnabled(! SUPPRESS_LOGGING || TestSupport.LOG);
  }
  
  private RemoteNexus openNexus() throws URISyntaxException, Exception {
    return remote.open(new URI("ws://localhost:" + port + "/"), logger(handler));
  }
  
  private String generateSessionId() {
    return Long.toHexString(Crypto.machineRandom());
  }

  @Test(expected=NoAuthenticatorException.class)
  public void testEmptyChain() throws Exception {
    setupEdgeNode(AuthChain.createDefault().clear());
  }

  @Test
  public void testDefaultSubChain() throws Exception {
    setupEdgeNode(AuthChain.createDefault());
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();

    final BindFrame bind1 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{"#",
                                                      "a/b/c",
                                                      Flywheel.getRxTopicPrefix(sessionId) + "/#"}, 
                                         null,
                                         null);
    final BindResponseFrame bind1Res = remoteNexus.bind(bind1).get();
    assertTrue(bind1Res.isSuccess());
    
    final BindFrame bind2 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         null,
                                         new String[]{Flywheel.getRxTopicPrefix(sessionId) + "/#",
                                                      Flywheel.getRxTopicPrefix("12345") + "/#",
                                                      Flywheel.getRxTopicPrefix("12346") + "/#"}, 
                                         null,
                                         null);
    final BindResponseFrame bind2Res = remoteNexus.bind(bind2).get();
    assertEquals(2, bind2Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind2Res.getErrors()[0].getClass());
    assertEquals(TopicAccessError.class, bind2Res.getErrors()[1].getClass());
  }
  
  private static Authenticator createBasicAuth(String username, String password) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, Auth auth, String topic, AuthenticationOutcome outcome) {
        if (auth instanceof BasicAuth) {
          final BasicAuth basic = (BasicAuth) auth;
          if (username.equals(basic.getUsername()) && password.equals(basic.getPassword())) {
            outcome.allow();
          } else {
            outcome.forbidden(topic);
          }
        } else {
          outcome.forbidden(topic);
        }
      }
    };
  }

  private static Authenticator createBearerAuth(String token) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, Auth auth, String topic, AuthenticationOutcome outcome) {
        if (auth instanceof BearerAuth) {
          final BearerAuth bearer = (BearerAuth) auth;
          if (token.equals(bearer.getToken())) {
            outcome.allow();
          } else {
            outcome.forbidden(topic);
          }
        } else {
          outcome.forbidden(topic);
        }
      }
    };
  }

  @Test
  public void testCustomSubChain() throws Exception {
    setupEdgeNode(AuthChain.createDefault()
                  .set("custom/basic/#", Mocks.logger(createBasicAuth("user", "pass")))
                  .set("custom/bearer/#", Mocks.logger(createBearerAuth("token"))));
    
    final RemoteNexus remoteNexus = openNexus();
    final String sessionId = generateSessionId();

    // test with the right user/pass on the correct topic; should pass
    final BindFrame bind1 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BasicAuth("user", "pass"),
                                         new String[]{"#",
                                                      "a/b/c",
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
                                         new String[]{"#",
                                                      "a/b/c",
                                                      Flywheel.getRxTopicPrefix(sessionId) + "/#",
                                                      "custom/basic/2"}, 
                                         null,
                                         null);
    final BindResponseFrame bind2Res = remoteNexus.bind(bind2).get();
    assertEquals(1, bind2Res.getErrors().length);
    assertEquals(TopicAccessError.class, bind2Res.getErrors()[0].getClass());

    // should pass, as the authenticator is set on 'custom/basic/#', not 'custom/basic'
    final BindFrame bind3 = new BindFrame(UUID.randomUUID(), 
                                         sessionId,
                                         new BasicAuth("user", "badpass"),
                                         new String[]{"custom/basic"}, 
                                         null,
                                         null);
    final BindResponseFrame bind3Res = remoteNexus.bind(bind3).get();
    assertTrue(bind3Res.isSuccess());
    
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
  }
}
