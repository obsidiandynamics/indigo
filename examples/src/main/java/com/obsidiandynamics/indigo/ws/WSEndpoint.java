package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.net.*;
import java.nio.*;

/**
 *  The abstract definition of a websocket endpoint.
 *
 *  @param <E> The endpoint type.
 */
public interface WSEndpoint extends AutoCloseable {
  /**
   *  Obtains the context associated with this endpoint.
   *  
   *  @return The context, if set.
   */
  <T> T getContext();
  
  /**
   *  Associates an arbitrary context object with this endpoint.
   *  
   *  @param context The context to associate this endpoint with.
   */
  void setContext(Object context);
  
  /**
   *  Asynchronously sends a text frame.
   *  
   *  @param payload The payload.
   *  @param callback Optional callback, invoked when the send completes (or fails).
   */
  void send(String payload, SendCallback callback);

  /**
   *  Asynchronously sends a binary frame.
   *  
   *  @param payload The payload.
   *  @param callback Optional callback, invoked when the send completes (or fails).
   */
  void send(ByteBuffer payload, SendCallback callback);
  
  /**
   *  Flushing the underlying stream. Depending on the implementation, this method may block.
   *  
   *  @throws IOException
   */
  void flush() throws IOException;

  /**
   *  Asynchronously sends a ping frame.
   */
  void sendPing();
  
  /**
   *  Obtains the socket address of the peer endpoint.
   *  
   *  @return
   */
  InetSocketAddress getRemoteAddress();
  
  /**
   *  Obtains the send backlog - the number of messages sent but yet to be confirmed.
   *  
   *  @return The number of backlogged messages.
   */
  long getBacklog();
}
