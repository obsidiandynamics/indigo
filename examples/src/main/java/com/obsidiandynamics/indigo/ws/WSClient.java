package com.obsidiandynamics.indigo.ws;

import java.net.*;

public interface WSClient<E extends WSEndpoint<E>> extends AutoCloseable {
  E connect(URI uri, EndpointListener<? super E> listener) throws Exception;
}
