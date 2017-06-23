package com.obsidiandynamics.indigo.socketx;

public interface XServer<E extends XEndpoint> extends AutoCloseable {
  XEndpointManager<E> getEndpointManager();
}
