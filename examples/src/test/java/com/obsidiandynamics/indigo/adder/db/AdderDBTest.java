package com.obsidiandynamics.indigo.adder.db;

import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.*;

public class AdderDBTest {
  private AdderDB db;
  
  @Before
  public void setup() {
    db = DynamoAdderDB.withLocalEndpoint();
    if (db.hasTable()) {
      db.deleteTable();
    }
  }
  
  @Test
  public void test() {
    assertFalse(db.hasTable());
    
    db.createTable();
    assertTrue(db.hasTable());
    
    db.setSavePoint(SavePoint.of(ActorRef.of(AdderContract.ROLE), 42));
    
    final SavePoint saved1 = db.getSavePoint(ActorRef.of(AdderContract.ROLE));
    assertNotNull(saved1);
    assertEquals(42, saved1.getSum());
    
    db.setSavePoint(SavePoint.of(ActorRef.of(AdderContract.ROLE), 43));
    
    final SavePoint saved2 = db.getSavePoint(ActorRef.of(AdderContract.ROLE));
    assertNotNull(saved2);
    assertEquals(43, saved2.getSum());
    
    db.deleteTable();
    assertFalse(db.hasTable());
  }
}
