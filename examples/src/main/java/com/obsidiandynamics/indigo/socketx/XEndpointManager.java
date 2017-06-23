package com.obsidiandynamics.indigo.socketx;

import java.util.*;

public interface XEndpointManager<E extends XEndpoint> {
  Collection<E> getEndpoints();
}
