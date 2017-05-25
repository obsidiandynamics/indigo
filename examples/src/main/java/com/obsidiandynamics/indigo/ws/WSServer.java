package com.obsidiandynamics.indigo.ws;

public interface WSServer<E extends WSEndpoint<E>> extends AutoCloseable {
  WSEndpointManager<E> getEndpointManager();
}
