package com.obsidiandynamics.indigo.adder;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.AdderContract.*;

public class AdderTest {
  @Test
  public void testInterfaceActor() throws InterruptedException {
    ActorSystem.create()
    .on(AdderContract.ROLE).cue(AdderActor::new)
    .ingress(AdderTest::addAndVerify)
    .shutdown();
  }
  
  static class IntegerSum {
    int sum;
  }
  
  @Test
  public void testLambdaActor_shortForm() throws InterruptedException {
    ActorSystem.create()
    .on(AdderContract.ROLE).cue(IntegerSum::new, (a, m, s) ->
      m.select()
      .when(Add.class).then(b -> s.sum += b.getValue())
      .when(Get.class).then(b -> a.reply(m).tell(new GetResponse(s.sum)))
      .otherwise(a::messageFault)
    )
    .ingress(AdderTest::addAndVerify)
    .shutdown();
  }
  
  @Test
  public void testLambdaActor_longForm() throws InterruptedException {
    ActorSystem.create()
    .on(AdderContract.ROLE)
    .cue(StatefulLambdaActor.<IntegerSum>builder()
         .activated(a -> CompletableFuture.completedFuture(new IntegerSum()))
         .act((a, m, s) -> 
           m.select()
           .when(Add.class).then(b -> s.sum += b.getValue())
           .when(Get.class).then(b -> a.reply(m).tell(new GetResponse(s.sum)))
           .otherwise(a::messageFault)
         ))
    .ingress(AdderTest::addAndVerify)
    .shutdown();
  }
  
  private static void addAndVerify(Activation a) {
    for (int i = 1; i <= 10; i++) {
      a.to(ActorRef.of(AdderContract.ROLE)).tell(new AdderContract.Add(i));
    }
    
    a.to(ActorRef.of(AdderContract.ROLE))
    .ask(new AdderContract.Get())
    .onResponse(r -> assertEquals(55, r.<AdderContract.GetResponse>body().getSum()));
  }
}
