package com.obsidiandynamics.indigo.iot.beacon;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.util.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class BeaconServer extends Thread implements TopicListener {
  private static final Properties PROPS = new Properties(System.getProperties());
  private static final int PORT = getOrSet(PROPS, "flywheel.beacon.port", Integer::valueOf, 8080);
  private static final String CONTEXT_PATH = getOrSet(PROPS, "flywheel.beacon.contextPath", String::valueOf, "/beacon");
  private static final int INTERVAL_MILLIS = getOrSet(PROPS, "flywheel.beacon.interval", Integer::valueOf, 1_000);
  
  private final EdgeNode edge;
  
  private BeaconServer() throws Exception {
    super("BeaconServer");
    filter("flywheel.beacon", PROPS).entrySet().stream()
    .map(e -> String.format("%-30s: %s", e.getKey(), e.getValue())).forEach(System.out::println);
    
    edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{
          contextPath = CONTEXT_PATH;
          port = PORT;
        }})
        .build();
    edge.addTopicListener(this);
    start();
  }
  
  @Override
  public void onOpen(EdgeNexus nexus) {
    System.out.format("%s: opened\n", nexus);
  }

  @Override
  public void onClose(EdgeNexus nexus) {
    System.out.format("%s: closed\n", nexus);
  }

  @Override
  public void onBind(EdgeNexus nexus, BindFrame bind, BindResponseFrame bindRes) {
    System.out.format("%s: bind %s -> %s\n", nexus, bind, bindRes);
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
    System.out.format("%s: publish %s\n", nexus, pub);
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    System.out.format("%s: publish %s\n", nexus, pub);
  }
  
  @Override
  public void run() {
    for (;;) {
      final String message = new Date().toString();
      for (EdgeNexus nexus : edge.getNexuses()) {
        final String sessionId = nexus.getSession().hasSessionId() ? nexus.getSession().getSessionId() : "anon";
        final String topic = Flywheel.getRxTopicPrefix(sessionId);
        nexus.send(new TextFrame(topic, message));
      }
      try {
        Thread.sleep(INTERVAL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    new BeaconServer();
  }
}
