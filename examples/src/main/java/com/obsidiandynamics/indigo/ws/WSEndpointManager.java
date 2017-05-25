package com.obsidiandynamics.indigo.ws;

import java.util.*;

public interface WSEndpointManager<E extends WSEndpoint<E>> {
  Collection<E> getEndpoints();
}
