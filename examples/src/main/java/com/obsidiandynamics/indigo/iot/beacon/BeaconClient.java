package com.obsidiandynamics.indigo.iot.beacon;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.net.*;
import java.nio.*;
import java.util.*;

import com.obsidiandynamics.indigo.iot.remote.*;

public final class BeaconClient {
  private static final Properties PROPS = new Properties(System.getProperties());
  private static final String HOST = getOrSet(PROPS, "flywheel.beacon.host", String::valueOf, "localhost");
  private static final int PORT = getOrSet(PROPS, "flywheel.beacon.port", Integer::valueOf, 8080);
  private static final String CONTEXT_PATH = getOrSet(PROPS, "flywheel.beacon.contextPath", String::valueOf, "/beacon");

  private BeaconClient() throws Exception {
    filter("flywheel.beacon", PROPS).entrySet().stream()
    .map(e -> String.format("%-30s: %s", e.getKey(), e.getValue())).forEach(System.out::println);
    
    final RemoteNode remote = RemoteNode.builder().build();
    remote.open(new URI(String.format("ws://%s:%d%s", HOST, PORT, CONTEXT_PATH)), new RemoteNexusHandler() {
      @Override
      public void onConnect(RemoteNexus nexus) {
        System.out.format("%s: connected\n", nexus);
      }

      @Override
      public void onDisconnect(RemoteNexus nexus) {
        System.out.format("%s: disconnected\n", nexus);
      }

      @Override
      public void onText(RemoteNexus nexus, String topic, String payload) {
        System.out.format("%s: text %s %s\n", nexus, topic, payload);
      }

      @Override
      public void onBinary(RemoteNexus nexus, String topic, ByteBuffer payload) {
        System.out.format("%s: binary %s %s\n", nexus, topic, payload);
      }
    });
  }
  
  public static void main(String[] args) throws Exception {
    new BeaconClient();
    Thread.sleep(Long.MAX_VALUE);
  }
}
