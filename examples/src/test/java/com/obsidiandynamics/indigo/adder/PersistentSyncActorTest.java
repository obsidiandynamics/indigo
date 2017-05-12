package com.obsidiandynamics.indigo.adder;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.AdderContract.*;

public class PersistentSyncActorTest {
  @Test
  public void testInterfaceActor_multiIngress() throws InterruptedException {
    //TODO
//    final ActorSystem system = ActorSystem.create()
//    .on(AdderContract.ROLE).cue(TransientAdderActor::new);
//    
//    system.ingress().times(10).act((a, i) ->
//      a.to(ActorRef.of(AdderContract.ROLE)).tell(new AdderContract.Add(i + 1))
//    );
//    
//    system.drain(0);
//    
//    system.ingress(a ->
//      a.to(ActorRef.of(AdderContract.ROLE))
//      .ask(new AdderContract.Get())
//      .onResponse(r -> assertEquals(55, r.<AdderContract.GetResponse>body().getSum()))
//    );
//    
//    system.shutdown();
  }
}
