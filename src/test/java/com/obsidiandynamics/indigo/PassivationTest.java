package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class PassivationTest implements TestSupport {
  private static final String TARGET = "target";
  private static final String ECHO = "echo";

//  @Test
//  public void testShort() {
//    logTestName();
//    
//    final List<Integer> sequence = new ArrayList<>();
//    final AtomicBoolean activated = new AtomicBoolean();
//    
//    new TestActorSystemConfig() {}
//    .define()
//    .when(TARGET)
//    .use(StatelessLambdaActor
//         .builder()
//         .passivated(a -> activated.set(true))
//         .act((a, m) -> {
//           assertTrue(activated.get());
//           sequence.add(m.body());
//         }))
//    .when(ECHO).lambda((a, m) -> a.reply(m).tell())
//    .ingress().times(4).act((a, i) -> a.to(ActorRef.of(TARGET)).tell(i))
//    .shutdown();
//
//    assertEquals(Arrays.asList(0, 1, 2, 3), sequence);
//  }

  @Test
  public void testLong() {
    logTestName();
    
    final List<Integer> sequence = new ArrayList<>();
    
    final AtomicInteger received = new AtomicInteger();
    final AtomicBoolean activated = new AtomicBoolean();
    final AtomicBoolean passivating = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .define()
    .when(TARGET)
    .use(StatelessLambdaActor
         .builder()
         .activated(a -> {
           System.out.println("activation");
           assertFalse(activated.get());
           assertFalse(passivating.get());
           activated.set(true);
           passivated.set(false);
         })
         .act((a, m) -> {
           System.out.println("act " + m.body());
           assertTrue(activated.get());
           assertFalse(passivated.get());
           received.incrementAndGet();
           a.passivate();
           sequence.add(m.body());
         })
         .passivated(a -> {
           System.out.println("passivation");
           assertTrue(activated.get());
           assertFalse(passivated.get());
           activated.set(false);
           passivating.set(true);
//           passivating.set(false);
//           passivated.set(true);
           // ask once and wait for a response
           a.to(ActorRef.of(ECHO)).ask().onResponse(r -> {
             // ask a second time... for good measure
             a.to(ActorRef.of(ECHO)).ask().onResponse(r2 -> {
               passivating.set(false);
               passivated.set(true);
             });
           });
         }))
    .when(ECHO).lambda((a, m) -> a.reply(m).tell())
    .ingress().act(a -> {
      for (int i = 0; i < 40000; i++) {
        a.to(ActorRef.of(TARGET)).tell(i);
      }
    })
    .shutdown();

    //assertEquals(Arrays.asList(0, 1, 2, 3), sequence);
    assertFalse(activated.get());
    assertFalse(passivating.get());
    assertTrue(passivated.get());
  }
}
