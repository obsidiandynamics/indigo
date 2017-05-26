package com.obsidiandynamics.indigo.iot.client;

import java.nio.*;

import com.obsidiandynamics.indigo.ws.*;

final class EndpointAdapter<E extends WSEndpoint<E>> implements EndpointListener<E> {
  private final SessionManager manager;
  private final SessionListener listener;
  private Session session;
  
  public EndpointAdapter(SessionManager manager, SessionListener listener) {
    this.manager = manager;
    this.listener = listener;
  }
  
  void setSession(Session session) {
    this.session = session;
  }
  
  @Override 
  public void onConnect(E endpoint) {
    listener.onConnect(session);
  }

  @Override 
  public void onText(E endpoint, String message) {
    // TODO Auto-generated method stub
    
  }

  @Override 
  public void onBinary(E endpoint, ByteBuffer message) {
    // TODO Auto-generated method stub
    
  }

  @Override 
  public void onClose(E endpoint, int statusCode, String reason) {
    // TODO Auto-generated method stub
    
  }

  @Override 
  public void onError(E endpoint, Throwable cause) {
    // TODO Auto-generated method stub
    
  }
}
