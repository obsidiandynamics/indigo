package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
public interface WSServerFactory<E extends WSEndpoint> {
  WSServer<E> create(WSServerConfig config, EndpointListener<? super E> listener) throws Exception;
}
