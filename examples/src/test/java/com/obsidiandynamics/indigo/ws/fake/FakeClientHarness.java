package com.obsidiandynamics.indigo.ws.fake;

import java.io.*;
import java.net.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ws.*;

public final class FakeClientHarness extends ClientHarness implements TestSupport {
  private final FakeClient client;
  
  public FakeClientHarness(int port, int expectedMessageSize) throws UnknownHostException, IOException {
    client = new FakeClient("/", port, expectedMessageSize, new FakeClientCallback() {
      @Override public void connected() {
        log("c: connected\n");
        connected.set(true);
      }

      @Override public void disconnected() {
        log("c: disconnected\n");
        closed.set(true);
      }

      @Override public void received(int messages) {
        log("c: received %d messages\n", messages);
        received.addAndGet(messages);
      }
    });
  }
  
  @Override
  public void close() throws Exception {
    client.close();
  }

  public static ThrowingFactory<FakeClientHarness> factory(int port, int expectedMessageSize) {
    return () -> new FakeClientHarness(port, expectedMessageSize);
  }
}
