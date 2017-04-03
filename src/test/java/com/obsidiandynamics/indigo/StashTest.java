package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class StashTest implements TestSupport {
  private static final String SINK = "sink";
  
  @Test
  public void testStashInAct() {
    logTestName();
    
    final List<Integer> sequence = new ArrayList<>();

    new TestActorSystemConfig() {}
    .define()
    .when(SINK).lambda((a, m) -> {
      final int body = m.body();
      if (body == 0) {
        a.stash(c -> c.<Integer>body() % 4 != 0);
      } else if (body == 8) {
        a.unstash();
      }
      sequence.add(body);
    })
    .ingress().times(9).act((a, i) -> a.to(ActorRef.of(SINK)).tell(i))
    .shutdown();

    assertEquals(Arrays.asList(0, 4, 8, 1, 2, 3, 5, 6, 7), sequence);
  }
  
  @Test
  public void testGuard() {
    logTestName();
    
    final AtomicBoolean activated = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .define()
    .when(SINK)
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           try {
             a.stash(Functions::alwaysTrue);
             fail("Didn't guard stash() in activation");
           } catch (IllegalStateException e) {}
           
           try {
             a.unstash();
             fail("Didn't guard unstash() in activation");
           } catch (IllegalStateException e) {}
           activated.set(true);
         })
         .act((a, m) -> {
           a.passivate();
         })
         .passivated(a -> {
           try {
             a.stash(Functions::alwaysTrue);
             fail("Didn't guard stash() in passivation");
           } catch (IllegalStateException e) {}
           
           try {
             a.unstash();
             fail("Didn't guard unstash() in passivation");
           } catch (IllegalStateException e) {}
           passivated.set(true);
         })
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).tell())
    .shutdown();
    
    assertTrue(activated.get());
    assertTrue(passivated.get());
  }
}
