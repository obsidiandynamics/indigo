package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.net.*;
import java.nio.*;

/**
 *  The abstract definition of a websocket endpoint.
 *
 *  @param <E> The endpoint type.
 */
public interface WSEndpoint<E extends WSEndpoint<E>> extends AutoCloseable {
  /**
   *  Asynchronously sends a text frame.
   *  
   *  @param payload The payload.
   *  @param callback Optional callback, invoked when the send completes (or fails).
   */
  void send(String payload, SendCallback<? super E> callback);

  /**
   *  Asynchronously sends a binary frame.
   *  
   *  @param payload The payload.
   *  @param callback Optional callback, invoked when the send completes (or fails).
   */
  void send(ByteBuffer payload, SendCallback<? super E> callback);
  
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
