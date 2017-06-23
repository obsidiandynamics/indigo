package com.obsidiandynamics.indigo.socketx;

import java.nio.*;

public interface XEndpointListener<E extends XEndpoint> {
  void onConnect(E endpoint);
  
  void onText(E endpoint, String message);
  
  void onBinary(E endpoint, ByteBuffer message);
  
  void onPing(ByteBuffer data);
  
  void onPong(ByteBuffer data);
  
  void onDisconnect(E endpoint, int statusCode, String reason);
  
  void onClose(E endpoint);
  
  void onError(E endpoint, Throwable cause);
}
