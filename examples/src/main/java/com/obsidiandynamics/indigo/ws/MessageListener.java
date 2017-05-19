package com.obsidiandynamics.indigo.ws;

import org.eclipse.jetty.websocket.api.*;

public interface MessageListener {
  void onConnect(Session session);
  
  void onText(Session session, String message);
  
  void onClose(Session session, int statusCode, String reason);
  
  void onError(Session session, Throwable cause);
}
