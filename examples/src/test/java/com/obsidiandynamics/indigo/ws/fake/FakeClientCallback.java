package com.obsidiandynamics.indigo.ws.fake;

public interface FakeClientCallback {
  void connected();
  
  void disconnected();
  
  void received(int messages);
}
