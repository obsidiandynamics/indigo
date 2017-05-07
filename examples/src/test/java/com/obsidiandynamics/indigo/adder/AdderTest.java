package com.obsidiandynamics.indigo.adder;

import static org.junit.Assert.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public class AdderTest {
  @Test
  public void testInterfaceActor() throws InterruptedException {
    ActorSystem.create()
    .on(AdderContract.ROLE).cue(AdderActor::new)
    .ingress(a -> {
      for (int i = 1; i <= 10; i++) {
        a.to(ActorRef.of(AdderContract.ROLE)).tell(new AdderContract.Add(i));
      }
      
      a.to(ActorRef.of(AdderContract.ROLE))
      .ask(new AdderContract.Get())
      .onResponse(r -> assertEquals(55, r.<AdderContract.GetResponse>body().getSum()));
    })
    .shutdown();
  }
}
