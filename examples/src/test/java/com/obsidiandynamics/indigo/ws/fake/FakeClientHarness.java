package com.obsidiandynamics.indigo.ws.fake;

import java.io.*;
import java.net.*;
import java.nio.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.fake.FakeClientHarness.*;

public final class FakeClientHarness extends ClientHarness<FakeEndpoint> implements TestSupport {
  public static final class FakeEndpoint implements WSEndpoint<FakeEndpoint> {
    @Override public void close() throws Exception {
    }
    @Override public void send(String payload, SendCallback<? super FakeEndpoint> callback) {
    }
    @Override public void send(ByteBuffer payload, SendCallback<? super FakeEndpoint> callback) {
    }
    @Override public void flush() throws IOException {
    }
    @Override public void sendPing() {
    }
    @Override public InetSocketAddress getRemoteAddress() {
      return null;
    } 
    @Override public long getBacklog() {
      return 0;
    }
    @Override public <T> T getContext() {
      return null;
    }
    @Override public void setContext(Object context) {
    }
  }
  
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
}
