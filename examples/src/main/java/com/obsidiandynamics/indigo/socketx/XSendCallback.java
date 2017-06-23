package com.obsidiandynamics.indigo.ws;

public interface SendCallback {
  void onComplete(WSEndpoint endpoint);

  void onError(WSEndpoint endpoint, Throwable cause);
}
