package com.obsidiandynamics.indigo.marketstrategy.sync;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.google.gson.*;
import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.marketstrategy.*;
import com.obsidiandynamics.indigo.messagebus.*;

public final class StrategyActorTest {
  private MessageBus bus;
  
  @Before
  public void setup() {
    bus = new ZmqMessageBus("tcp://*:5557", new GsonCodec(new Gson()));
  }
  
  @After
  public void teardown() {
    bus.close();
  }
  
  @Test
  public void test() {
    final List<Order> received = new CopyOnWriteArrayList<>();
    final AtomicBoolean synced = new AtomicBoolean();
    bus.getSubscriber("orders").onReceive(msg -> {
      if (msg.equals("sync")) {
        synced.set(true);
        return;
      }
      received.add((Order) msg);
    });
    
    while (! synced.get()) {
      //TODO
    }
    
    ActorSystem.create()
    .on("router").cue(RouterActor::new)
    .on("strategy").cue(() -> new StrategyActor(() -> new SimpleStochastic(14, 3, 80, 20), bus))
    .shutdownQuietly();
  }
}
