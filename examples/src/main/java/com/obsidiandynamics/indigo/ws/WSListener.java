package com.obsidiandynamics.indigo.ws;

import java.nio.*;

public interface WSListener<E extends WSEndpoint> {
  void onConnect(E endpoint);
  
  void onText(E endpoint, String message);
  
  void onBinary(E endpoint, ByteBuffer message);
  
  void onClose(E endpoint, int statusCode, String reason);
  
  void onError(E endpoint, Throwable cause);
}
