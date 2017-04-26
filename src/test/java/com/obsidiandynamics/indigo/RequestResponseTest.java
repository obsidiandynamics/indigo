package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class RequestResponseTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String ADDER = "adder";
  private static final String DONE_RUNS = "done_runs";
  private static final String SINK = "SINK";

  @Test
  public void testRequestResponse() {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();

    new TestActorSystemConfig() {}
    .define()
    .when(DRIVER).lambdaAsync(a -> CompletableFuture.completedFuture(new IntegerState()), (a, m, s) -> {
      a.to(ActorRef.of(ADDER)).ask(s.value).onResponse(r -> {
        final int res = r.body();
        if (res == runs) {
          a.to(ActorRef.of(DONE_RUNS)).tell();
        } else {
          assertEquals(s.value + 1, res);
          s.value = res;
          a.toSelf().tell();
        }
      });
    })
    .when(ADDER).lambda((a, m) -> a.reply(m).tell(m.<Integer>body() + 1))
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdownQuietly();

    assertEquals(actors, doneRuns.size());
  }
  
  @Test
  public void testUnsolicitedReply_toSenderOf() {
    final Set<String> receivedRoles = new HashSet<>();
    new TestActorSystemConfig() {}
    .define()
    .when(DRIVER).lambda((a, m) -> {
      receivedRoles.add(m.from().role());
      if (m.from().isIngress()) {
        a.to(ActorRef.of(SINK)).tell();
      }
    })
    .when(SINK).lambda((a, m) -> {
      a.toSenderOf(m).tell();
    })
    .ingress(a -> a.to(ActorRef.of(DRIVER)).tell())
    .shutdownQuietly();
    
    assertEquals(2, receivedRoles.size());
    assertTrue(receivedRoles.contains(ActorRef.INGRESS));
    assertTrue(receivedRoles.contains(SINK));
  }
  
  @Test
  public void testUnsolicitedReply_reply() {
    final Set<String> receivedRoles = new HashSet<>();
    new TestActorSystemConfig() {}
    .define()
    .when(DRIVER).lambda((a, m) -> {
      receivedRoles.add(m.from().role());
      if (m.from().isIngress()) {
        a.to(ActorRef.of(SINK)).tell();
      }
    })
    .when(SINK).lambda((a, m) -> {
      a.reply(m).tell();
    })
    .ingress(a -> a.to(ActorRef.of(DRIVER)).tell())
    .shutdownQuietly();
    
    assertEquals(1, receivedRoles.size());
    assertTrue(receivedRoles.contains(ActorRef.INGRESS));
  }
  
  @Test
  public void testResponseAfterTimeout() {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final AtomicBoolean responded = new AtomicBoolean();
    final AtomicBoolean timedOut = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .define()
    .when(SINK).lambda((a, m) -> {
      // delay the response so that it gets beaten by the timeout
      TestSupport.await(barrier);
      a.reply(m).tell();
      responded.set(true);
    })
    .ingress(a -> {
      a.to(ActorRef.of(SINK)).ask()
      .await(1)
      .onTimeout(() -> {
        TestSupport.await(barrier);
        timedOut.set(true);
      })
      .onResponse(r -> {
        fail("Unexpected response");
      });
    })
    .shutdownQuietly();
    
    assertTrue(responded.get());
    assertTrue(timedOut.get());
  }
}