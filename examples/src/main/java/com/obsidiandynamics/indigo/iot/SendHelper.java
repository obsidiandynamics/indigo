package com.obsidiandynamics.indigo.iot;

import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class SendHelper {
  private SendHelper() {}

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static CompletableFuture<Void> send(Frame frame, WSEndpoint<?> endpoint, Wire wire) {
    final CompletableFuture<Void> f = new CompletableFuture<>();
    final String encoded = wire.encode(frame);
    final SendCallback<?> sendCallback = new SendCallback() {
      @Override public void onComplete(WSEndpoint endpoint) {
        f.complete(null);
      }

      @Override public void onError(WSEndpoint endpoint, Throwable cause) {
        f.completeExceptionally(cause);
      }
    };
    endpoint.send(encoded, (SendCallback) sendCallback);
    return f;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void send(Frame frame, WSEndpoint<?> endpoint, Wire wire, Consumer<Throwable> callback) {
    final String encoded = wire.encode(frame);
    final SendCallback<?> sendCallback;
    if (callback != null) {
      sendCallback = new SendCallback() {
        @Override public void onComplete(WSEndpoint endpoint) {
          callback.accept(null);
        }
  
        @Override public void onError(WSEndpoint endpoint, Throwable cause) {
          callback.accept(cause);
        }
      };
    } else {
      sendCallback = null;
    }
    endpoint.send(encoded, (SendCallback) sendCallback);
  }
}
