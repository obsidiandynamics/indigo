package com.obsidiandynamics.indigo.ws;

public interface WSServer<E extends WSEndpoint> extends AutoCloseable {
  WSEndpointManager<E> getEndpointManager();
}