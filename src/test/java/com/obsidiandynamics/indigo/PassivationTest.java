package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public class PassivationTest implements TestSupport {
  private static final String A = "a";
  private static final String B = "b";
  private static final String DONE_RUN = "done_run";
  private static final String DONE_ACTIVATION = "done_activation";
  private static final String DONE_PASSIVATION = "done_passivation";

  @Test
  public void test() {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRun = new HashSet<>();
    final Set<ActorRef> doneActivation = new HashSet<>();
    final Set<ActorRef> donePassivation = new HashSet<>();
    
    new ActorSystem()
    .when(A)
    .use(StatelessLambdaActor
         .builder()
         .act(a -> {
           final int msg = a.message().body();
           if (msg == runs) {
             a.to(ActorRef.of(DONE_RUN)).tell();
           } else {
             a.to(ActorRef.of(B, a.self().key())).tell(msg + 1);
             a.passivate();
           }
         })
         .activated(a -> {
           a.to(ActorRef.of(DONE_ACTIVATION)).tell();
         })
         .passivated(a -> {
           a.to(ActorRef.of(DONE_PASSIVATION)).tell();
         }))
    .when(B)
    .use(StatelessLambdaActor
         .builder()
         .act(a -> {
           final int msg = a.message().body();
           if (msg == runs) {
             a.to(ActorRef.of(DONE_RUN)).tell();
           } else {
             a.toSender().tell(msg);
             a.passivate();
           }
         })
         .activated(a -> {
           a.to(ActorRef.of(DONE_ACTIVATION)).tell();
         })
         .passivated(a -> {
           a.to(ActorRef.of(DONE_PASSIVATION)).tell();
         }))
    .when(DONE_RUN).apply(refCollector(doneRun))
    .when(DONE_ACTIVATION).apply(refCollector(doneActivation))
    .when(DONE_PASSIVATION).apply(refCollector(donePassivation))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        a.to(ActorRef.of(A, i + "")).tell(1);
      }
    })
    .shutdown();

    assertEquals(actors, doneRun.size());
    assertEquals(actors * 2, doneActivation.size());
    assertEquals(actors * 2, donePassivation.size());
  }
}
