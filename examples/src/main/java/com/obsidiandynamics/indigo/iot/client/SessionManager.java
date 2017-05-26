package com.obsidiandynamics.indigo.iot.client;

import java.net.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.ws.*;

public final class SessionManager {
  private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);
  
  private final class ClientHolder<E extends WSEndpoint<E>> {
    final WSClient<E> client;
    
    public Session connect(URI uri, SessionListener listener) throws Exception {
      if (LOG.isDebugEnabled()) LOG.debug("Connecting to {}", uri);
      final EndpointAdapter<E> adapter = new EndpointAdapter<>(SessionManager.this, listener);
      final E endpoint = client.connect(uri, adapter);
      return new Session(endpoint);
    }

    ClientHolder(WSClient<E> client) {
      this.client = client;
    }
  }
  
  private final ClientHolder<?> clientHolder;
  
  public <E extends WSEndpoint<E>> SessionManager(WSClient<?> client) {
    clientHolder = new ClientHolder<>(client);
  }
  
  public Session connect(URI uri, SessionListener listener) throws Exception {
    return clientHolder.connect(uri, listener);
  }
}
