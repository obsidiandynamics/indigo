package com.obsidiandynamics.indigo.ws;

import org.eclipse.jetty.websocket.api.*;

public interface MessageListener {
  void onConnect(Session session);
  
  void onText(Session session, String message);
  
  void onBinary(Session session, byte[] payload, int offset, int len);
  
  void onClose(Session session, int statusCode, String reason);
  
  void onError(Session session, Throwable cause);
}
