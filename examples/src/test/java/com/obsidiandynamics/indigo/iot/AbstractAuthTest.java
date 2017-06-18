package com.obsidiandynamics.indigo.iot;

import static com.obsidiandynamics.indigo.util.Mocks.*;

import java.net.*;
import java.nio.*;
import java.util.concurrent.*;

import org.awaitility.*;
import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.Authenticator;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.frame.Wire.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public abstract class AbstractAuthTest {
  private static final int PREFERRED_PORT = 6667;
  private static final boolean SUPPRESS_LOGGING = true;
  
  private Wire wire;

  private RemoteNexusHandler handler;
 
  protected EdgeNode edge;
  
  protected RemoteNode remote;
  
  protected RemoteNexus remoteNexus;
  
  protected volatile Errors errors;
  
  protected volatile TextFrame text;
  
  protected volatile BinaryFrame binary;
  
  private int port;
  
  @Before
  public void setup() throws Exception {
    port = SocketTestSupport.getAvailablePort(PREFERRED_PORT);
    
    wire = new Wire(true, LocationHint.UNSPECIFIED);
    handler = new RemoteNexusHandlerAdapter() {
      @Override public void onText(RemoteNexus nexus, String topic, String payload) {
        if (topic.endsWith("/errors")) {
          errors = wire.decodeJson(payload, Errors.class);
        } else {
          text = new TextFrame(topic, payload);
        }
      }

      @Override public void onBinary(RemoteNexus nexus, String topic, ByteBuffer payload) {
        binary = new BinaryFrame(topic, payload);
      }
    };
    
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
  
  protected void clearReceived() {
    errors = null;
    text = null;
    binary = null;
  }
  
  protected void awaitReceived() {
    Awaitility.await().dontCatchUncaughtExceptions()
    .atMost(10, TimeUnit.SECONDS).until(() -> errors != null || text != null || binary != null);
  }
  
  protected void setupEdgeNode(AuthChain pubAuthChain, AuthChain subAuthChain) throws Exception {
    edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = AbstractAuthTest.this.port; }})
        .withWire(wire)
        .withPubAuthChain(pubAuthChain)
        .withSubAuthChain(subAuthChain)
        .build();
    edge.setLoggingEnabled(! SUPPRESS_LOGGING || TestSupport.LOG);
    edge.addTopicListener(new TopicListenerBase() {
      @Override public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
        if (pub.getTopic().endsWith("/tx")) {
          edge.publish(Flywheel.getRxTopicPrefix(nexus.getSession().getSessionId()), pub.getPayload());
        }
      }
      @Override public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
        if (pub.getTopic().endsWith("/tx")) {
          edge.publish(Flywheel.getRxTopicPrefix(nexus.getSession().getSessionId()), pub.getPayload());
        }
      }
    });
  }
  
  protected RemoteNexus openNexus() throws URISyntaxException, Exception {
    return remote.open(new URI("ws://localhost:" + port + "/"), logger(RemoteNexusHandler.class, handler));
  }
  
  protected String generateSessionId() {
    return Long.toHexString(Crypto.machineRandom());
  }
  
  protected static Authenticator createBasicAuth(String username, String password) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
        if (nexus.getSession().getAuth() instanceof BasicAuth) {
          final BasicAuth basic = nexus.getSession().getAuth();
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

  protected static Authenticator createBearerAuth(String token) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
        if (nexus.getSession().getAuth() instanceof BearerAuth) {
          final BearerAuth bearer = nexus.getSession().getAuth();
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
}
