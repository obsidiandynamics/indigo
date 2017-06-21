package com.obsidiandynamics.indigo.ws;

import java.net.*;
import java.util.*;

public interface WSClient<E extends WSEndpoint> extends AutoCloseable {
  E connect(URI uri, WSEndpointListener<? super E> listener) throws Exception;

  Collection<E> getEndpoints();
}
