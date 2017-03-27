package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class LongActivationTest implements TestSupport {
  private static final String TARGET = "target";
  private static final String ECHO = "echo";

  @Test
  public void test() {
    logTestName();
    
    final List<Integer> sequence = new ArrayList<>();
    final AtomicBoolean activated = new AtomicBoolean();
    
    new ActorSystemConfig() {}
    .define()
    .when(TARGET)
    .use(StatelessLambdaActor
         .builder()
         .activated(a -> {
           a.stash(c -> true);
           a.to(ActorRef.of(ECHO)).ask().onResponse(r -> {
             activated.set(true);
             a.unstash();
           });
         })
         .act((a, m) -> {
           assertTrue(activated.get());
           sequence.add(m.body());
         }))
    .when(ECHO).lambda((a, m) -> a.reply(m).tell())
    .ingress().times(4).act((a, i) -> a.to(ActorRef.of(TARGET)).tell(i))
    .shutdown();

    assertEquals(Arrays.asList(0, 1, 2, 3), sequence);
  }
}
