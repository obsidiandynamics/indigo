package com.obsidiandynamics.indigo.adder.db;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class MockAdderDB implements AdderDB {
  private Map<ActorRef, SavePoint> map;
  
  @Override
  public boolean hasTable() {
    return map != null;
  }

  @Override
  public void createTable() {
    assertNull(map);
    map = new ConcurrentHashMap<>();
  }

  @Override
  public void setSavePoint(SavePoint savePoint) {
    assertNotNull(map);
    assertNotNull(savePoint);
    map.put(savePoint.getActorRef(), savePoint);
  }

  @Override
  public SavePoint getSavePoint(ActorRef actorRef) {
    assertNotNull(map);
    assertNotNull(actorRef);
    return map.get(actorRef);
  }

  @Override
  public void deleteTable() {
    assertNotNull(map);
    map = null;
  }
}
