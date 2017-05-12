package com.obsidiandynamics.indigo.adder;

import static org.junit.Assert.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.db.*;
import com.obsidiandynamics.indigo.util.*;

public class PersistentSyncActorTest {
  private static final boolean MOCK = PropertyUtils.get("indigo.AdderDB.mock", Boolean::parseBoolean, true);
  
  private AdderDB db;
  
  @Before
  public void setup() {
    db = MOCK ? new MockAdderDB() : DynamoAdderDB.withLocalEndpoint();
    
    // initialise the DB
    if (! db.hasTable()) {
      db.createTable();
    }
  }
  
  @Test
  public void testInterfaceActor_multiIngress() throws InterruptedException {
    final ActorSystem system = ActorSystem.create()
    .on(AdderContract.ROLE).cue(PersistentSyncAdderActor.factory(db));
    
    system.ingress().times(10).act((a, i) ->
      a.to(ActorRef.of(AdderContract.ROLE)).tell(new AdderContract.Add(i + 1))
    );
    
    system.drain(0);
    
    system.ingress(a ->
      a.to(ActorRef.of(AdderContract.ROLE))
      .ask(new AdderContract.Get())
      .onResponse(r -> assertEquals(55, r.<AdderContract.GetResponse>body().getSum()))
    );
    
    system.shutdown();
  }
}
