package com.obsidiandynamics.indigo.ws;

public interface SendCallback<E extends WSEndpoint<?>> {
  void onComplete(E endpoint);

  void onError(E endpoint, Throwable cause);
}
