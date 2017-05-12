package com.obsidiandynamics.indigo.adder.db;

import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.*;
import com.obsidiandynamics.indigo.util.*;

public class AdderDBTest {
  private static final boolean MOCK = PropertyUtils.get("indigo.AdderDB.mock", Boolean::parseBoolean, true);
  
  private AdderDB db;
  
  @Before
  public void setup() {
    db = MOCK ? new MockAdderDB() : DynamoAdderDB.withLocalEndpoint();
    if (db.hasTable()) {
      db.deleteTable();
    }
  }
  
  @Test
  public void test() {
    assertFalse(db.hasTable());
    
    db.createTable();
    assertTrue(db.hasTable());
    
    final ActorRef actorRef = ActorRef.of(AdderContract.ROLE);
    
    final SavePoint saved0 = db.getSavePoint(actorRef);
    assertNull(saved0);
    
    db.setSavePoint(SavePoint.of(actorRef, 42));
    
    final SavePoint saved1 = db.getSavePoint(actorRef);
    assertNotNull(saved1);
    assertEquals(42, saved1.getSum());
    
    db.setSavePoint(SavePoint.of(actorRef, 43));
    
    final SavePoint saved2 = db.getSavePoint(actorRef);
    assertNotNull(saved2);
    assertEquals(43, saved2.getSum());
    
    db.deleteTable();
    assertFalse(db.hasTable());
  }
}
