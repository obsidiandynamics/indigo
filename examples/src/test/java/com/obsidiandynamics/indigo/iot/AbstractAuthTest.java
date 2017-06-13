package com.obsidiandynamics.indigo.iot;

import static com.obsidiandynamics.indigo.util.Mocks.logger;
import static org.mockito.Mockito.mock;

import java.net.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.Authenticator;
import com.obsidiandynamics.indigo.iot.frame.*;
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
  
  protected void setupEdgeNode(AuthChain authChain) throws Exception {
    edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = AbstractAuthTest.this.port; }})
        .withWire(wire)
        .withSubAuthChain(authChain)
        .build();
    edge.setLoggingEnabled(! SUPPRESS_LOGGING || TestSupport.LOG);
  }
  
  protected RemoteNexus openNexus() throws URISyntaxException, Exception {
    return remote.open(new URI("ws://localhost:" + port + "/"), logger(handler));
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
