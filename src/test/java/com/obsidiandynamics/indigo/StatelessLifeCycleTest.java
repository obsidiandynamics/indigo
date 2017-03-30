package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class StatelessLifeCycleTest implements TestSupport {
  private static final String TICK = "tick";
  private static final String TOCK = "tock";
  private static final String DONE_RUNS = "done_runs";
  private static final String DONE_ACTIVATION = "done_activation";
  private static final String DONE_PASSIVATION = "done_passivation";

  @Test
  public void test() {
    logTestName();
    
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();
    final Set<ActorRef> doneActivation = new HashSet<>();
    final Set<ActorRef> donePassivation = new HashSet<>();
    
    new TestActorSystemConfig() {}
    .define()
    .when(TICK)
    .use(StatelessLambdaActor
         .builder()
         .act((a, m) -> {
           final int body = m.body();
           if (body == runs) {
             a.to(ActorRef.of(DONE_RUNS)).tell();
           } else {
             a.to(ActorRef.of(TOCK, a.self().key())).tell(body + 1);
             a.passivate();
           }
         })
         .activated(tell(DONE_ACTIVATION))
         .passivated(tell(DONE_PASSIVATION)))
    .when(TOCK)
    .use(StatelessLambdaActor
         .builder()
         .act((a, m) -> {
           final int body = m.body();
           if (body == runs) {
             a.to(ActorRef.of(DONE_RUNS)).tell();
           } else {
             a.toSenderOf(m).tell(body);
             a.passivate();
           }
         })
         .activated(tell(DONE_ACTIVATION))
         .passivated(tell(DONE_PASSIVATION)))
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .when(DONE_ACTIVATION).lambda(refCollector(doneActivation))
    .when(DONE_PASSIVATION).lambda(refCollector(donePassivation))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(TICK, String.valueOf(i))).tell(1))
    .shutdown();

    assertEquals(actors, doneRuns.size());
    assertEquals(actors * 2, doneActivation.size());
    assertEquals(actors * 2, donePassivation.size());
  }
}
