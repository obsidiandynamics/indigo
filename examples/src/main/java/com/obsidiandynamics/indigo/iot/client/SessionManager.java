package com.obsidiandynamics.indigo.iot.client;

import java.net.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class SessionManager implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);
  
  private final class ClientHolder<E extends WSEndpoint<E>> {
    final WSClient<E> client;
    
    public Session connect(URI uri, SessionHandler handler) throws Exception {
      if (LOG.isDebugEnabled()) LOG.debug("Connecting to {}", uri);
      final Session session = new Session(SessionManager.this);
      final EndpointAdapter<E> adapter = new EndpointAdapter<>(SessionManager.this, session, handler);
      client.connect(uri, adapter);
      return session;
    }

    ClientHolder(WSClient<E> client) {
      this.client = client;
    }
  }
  
  private final ClientHolder<?> clientHolder;
  
  private final Wire wire;
  
  public SessionManager(WSClientFactory<?> clientFactory, WSClientConfig config, Wire wire) throws Exception {
    clientHolder = new ClientHolder<>(clientFactory.create(config));
    this.wire = wire;
  }
  
  public Session open(URI uri, SessionHandler handler) throws Exception {
    return clientHolder.connect(uri, handler);
  }
  
  Wire getWire() {
    return wire;
  }

  @Override
  public void close() throws Exception {
    clientHolder.client.close();
  }
}
