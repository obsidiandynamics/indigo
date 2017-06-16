package com.obsidiandynamics.indigo.ws;

import java.net.*;

public interface WSClient<E extends WSEndpoint> extends AutoCloseable {
  E connect(URI uri, WSEndpointListener<? super E> listener) throws Exception;
}
