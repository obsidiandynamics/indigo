package com.obsidiandynamics.indigo.iot;

import java.nio.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.socketx.*;

public final class SendHelper {
  private SendHelper() {}

  public static CompletableFuture<Void> sendAuto(Frame frame, XEndpoint endpoint, Wire wire) {
    if (frame instanceof TextFrame) {
      return send((TextFrame) frame, endpoint, wire);
    } else {
      return send((BinaryFrame) frame, endpoint, wire);
    }
  }
  
  public static void sendAuto(Frame frame, XEndpoint endpoint, Wire wire, Consumer<Throwable> callback) {
    if (frame instanceof TextFrame) {
      send((TextFrame) frame, endpoint, wire, callback);
    } else {
      send((BinaryFrame) frame, endpoint, wire, callback);
    }
  }

  public static CompletableFuture<Void> send(TextEncodedFrame frame, XEndpoint endpoint, Wire wire) {
    final CompletableFuture<Void> f = new CompletableFuture<>();
    final String encoded = wire.encode(frame);
    endpoint.send(encoded, wrapFuture(f));
    return f;
  }
  
  public static void send(TextEncodedFrame frame, XEndpoint endpoint, Wire wire, Consumer<Throwable> callback) {
    final String encoded = wire.encode(frame);
    endpoint.send(encoded, wrapCallback(callback));
  }

  public static CompletableFuture<Void> send(BinaryEncodedFrame frame, XEndpoint endpoint, Wire wire) {
    final CompletableFuture<Void> f = new CompletableFuture<>();
    final ByteBuffer encoded = wire.encode(frame);
    endpoint.send(encoded, wrapFuture(f));
    return f;
  }
  
  public static void send(BinaryEncodedFrame frame, XEndpoint endpoint, Wire wire, Consumer<Throwable> callback) {
    final ByteBuffer encoded = wire.encode(frame);
    endpoint.send(encoded, wrapCallback(callback));
  }
  
  private static XSendCallback wrapFuture(CompletableFuture<Void> f) {
    return new XSendCallback() {
      @Override public void onComplete(XEndpoint endpoint) {
        f.complete(null);
      }

      @Override public void onError(XEndpoint endpoint, Throwable cause) {
        f.completeExceptionally(cause);
      }
    };
  }
  
  private static XSendCallback wrapCallback(Consumer<Throwable> callback) {
    if (callback != null) {
      return new XSendCallback() {
        @Override public void onComplete(XEndpoint endpoint) {
          callback.accept(null);
        }
  
        @Override public void onError(XEndpoint endpoint, Throwable cause) {
          callback.accept(cause);
        }
      };
    } else {
      return null;
    }
  }
}
