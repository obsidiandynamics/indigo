package com.obsidiandynamics.indigo.iot.client;

import java.nio.*;

public interface SessionHandler {
  void onConnect(Session session);
  
  void onDisconnect(Session session);
  
  void onText(Session session, String payload);
  
  void onBinary(Session session, ByteBuffer payload);
}
