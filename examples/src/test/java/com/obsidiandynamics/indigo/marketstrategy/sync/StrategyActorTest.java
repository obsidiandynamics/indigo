package com.obsidiandynamics.indigo.marketstrategy.sync;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.marketstrategy.*;
import com.obsidiandynamics.indigo.marketstrategy.Order.*;

public final class StrategyActorTest {
  @Test
  public void test() {
    final List<Order> orders = new ArrayList<>();
    final Supplier<OrderHandler> orderHandlerFactory = () -> orders::add;
    
    ActorSystem.create()
    .on("router").cue(RouterActor::new)
    .on("strategy").cue(() -> new StrategyActor(() -> new SimpleStochastic(14, 3, 80, 20), orderHandlerFactory))
    .ingress(a -> {
      a.to(ActorRef.of("router")).times(15).tell(new Bar("AAA", 9, 10, 9, 9));   // primes the oscillator
      a.to(ActorRef.of("router")).times(5).tell(new Bar("AAA", 9, 10, 9, 9.9f)); // should generate some sell signals
      a.to(ActorRef.of("router")).times(5).tell(new Bar("AAA", 9, 10, 9, 9.1f)); // should generate some buy signals
    })
    .shutdownSilently();
    
    assertEquals(6, orders.size());
    assertEquals(3, orders.stream().filter(o -> o.getSide() == Side.SELL).count());
    assertEquals(3, orders.stream().filter(o -> o.getSide() == Side.BUY).count());
  }
}
