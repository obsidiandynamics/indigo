package com.obsidiandynamics.indigo.socketx;

public interface XSendCallback {
  void onComplete(XEndpoint endpoint);

  void onError(XEndpoint endpoint, Throwable cause);
}
