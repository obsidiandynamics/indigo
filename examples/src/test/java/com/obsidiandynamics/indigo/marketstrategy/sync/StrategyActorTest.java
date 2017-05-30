package com.obsidiandynamics.indigo.marketstrategy.sync;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.junit.*;

import com.google.gson.*;
import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.marketstrategy.*;
import com.obsidiandynamics.indigo.xbus.*;
import com.obsidiandynamics.indigo.xbus.codec.*;
import com.obsidiandynamics.indigo.xbus.zmq.*;

public final class StrategyActorTest {
  private XBus bus;
  
  @Before
  public void setup() {
    bus = new ZmqBus("tcp://*:5557", new GsonCodec(new Gson()));
  }
  
  @After
  public void teardown() {
    bus.close();
  }
  
  @Test
  public void test() {
    final XPublisher pub = bus.getPublisher("orders");
    
    final List<Order> received = new CopyOnWriteArrayList<>();
    final AtomicBoolean sync = new AtomicBoolean();
    AsyncSubscriber.using(() -> bus.getSubscriber("orders")).onReceive(msg -> {
      if (msg.equals("sync")) {
        sync.set(true);
        return;
      }
      received.add((Order) msg);
    });

    while (! sync.get()) pub.send("sync");
    
    ActorSystem.create()
    .on("router").cue(RouterActor::new)
    .on("strategy").cue(() -> new StrategyActor(() -> new SimpleStochastic(14, 3, 80, 20), bus))
    .ingress(a -> {
      a.to(ActorRef.of("router")).times(15).tell(new Bar("AAA", 9, 10, 9, 9));   // primes the oscillator
      a.to(ActorRef.of("router")).times(5).tell(new Bar("AAA", 9, 10, 9, 9.9f)); // should generate some sell signals
      a.to(ActorRef.of("router")).times(5).tell(new Bar("AAA", 9, 10, 9, 9.1f)); // should generate some buy signals
    })
    .shutdownQuietly();
    
    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> received.size() >= 6);
    assertEquals(6, received.size());
  }
}
