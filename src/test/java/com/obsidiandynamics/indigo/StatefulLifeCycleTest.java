package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

public final class StatefulLifeCycleTest implements TestSupport {
  private static final String TICK = "tick";
  private static final String TOCK = "tock";
  private static final String DONE_RUNS = "done_runs";
  private static final String DONE_ACTIVATION = "done_activation";
  private static final String DONE_PASSIVATION = "done_passivation";
  
  private static class MockDB {
    private final Map<ActorRef, IntegerState> states = new ConcurrentHashMap<>();
    
    IntegerState get(ActorRef ref) {
      if (! states.containsKey(ref)) {
        final IntegerState state = new IntegerState();
        states.put(ref, state);
        return state;
      } else {
        return states.get(ref);
      }
    }
    
    void put(ActorRef ref, IntegerState state) {
      states.put(ref, state);
    }
    
    int size() { return states.size(); }
  }

  @Test
  public void test() {
    logTestName();
    
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();
    final Set<ActorRef> doneActivation = new HashSet<>();
    final Set<ActorRef> donePassivation = new HashSet<>();
    
    final MockDB db = new MockDB();
    
    new TestActorSystemConfig() {}
    .define()
    .when(TICK)
    .use(StatelessLambdaActor
         .builder()
         .act((a, m) -> {
           final int msg = m.body();
           if (msg == runs) {
             a.to(ActorRef.of(DONE_RUNS)).tell();
           } else {
             a.to(ActorRef.of(TOCK, a.self().key())).tell(msg + 1);
             a.passivate();
           }
         })
         .activated(tell(DONE_ACTIVATION))
         .passivated(tell(DONE_PASSIVATION)))
    .when(TOCK)
    .use(StatefulLambdaActor
         .<IntegerState>builder()
         .act((a, m, s) -> {
           final int body = m.body();
           assertEquals(s.value + 1, body);
           s.value = body;
           
           if (body == runs) {
             a.to(ActorRef.of(DONE_RUNS)).tell();
           } else {
             a.toSenderOf(m).tell(body);
             a.passivate();
           }
         })
         .activated(a -> {
           a.to(ActorRef.of(DONE_ACTIVATION)).tell();
           return db.get(a.self());
         })
         .passivated((a, s) -> {
           db.put(a.self(), s);
           a.to(ActorRef.of(DONE_PASSIVATION)).tell();
         }))
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .when(DONE_ACTIVATION).lambda(refCollector(doneActivation))
    .when(DONE_PASSIVATION).lambda(refCollector(donePassivation))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(TICK, String.valueOf(i))).tell(0))
    .shutdown();

    assertEquals(actors, doneRuns.size());
    assertEquals(actors * 2, doneActivation.size());
    assertEquals(actors * 2, donePassivation.size());
    assertEquals(actors, db.size());
  }
}
