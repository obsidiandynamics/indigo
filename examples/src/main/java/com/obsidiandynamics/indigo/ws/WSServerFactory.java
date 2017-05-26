package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
public interface WSServerFactory<E extends WSEndpoint<E>> {
  WSServer<E> create(WSServerConfig config, EndpointListener<? super E> listener) throws Exception;
}
