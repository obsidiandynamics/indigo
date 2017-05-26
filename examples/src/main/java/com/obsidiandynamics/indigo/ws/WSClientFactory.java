package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
public interface WSClientFactory<E extends WSEndpoint<E>> {
  WSClient<E> create(WSClientConfig config) throws Exception;
}
