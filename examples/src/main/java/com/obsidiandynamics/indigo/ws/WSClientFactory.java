package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
public interface WSClientFactory<E extends WSEndpoint> {
  WSClient<E> create(WSClientConfig config) throws Exception;
}
