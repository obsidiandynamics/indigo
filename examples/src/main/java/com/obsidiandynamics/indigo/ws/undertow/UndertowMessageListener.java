package com.obsidiandynamics.indigo.ws.undertow;

import io.undertow.websockets.core.*;

public interface UndertowMessageListener {
  void onConnect(WebSocketChannel channel);
  
  void onText(WebSocketChannel channel, BufferedTextMessage message);
  
  void onBinary(WebSocketChannel channel, BufferedBinaryMessage message);
  
  void onClose(WebSocketChannel channel, int code, String reason);
  
  void onError(WebSocketChannel channel, Throwable x);
}
