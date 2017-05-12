package com.obsidiandynamics.indigo.adder.db;

import com.obsidiandynamics.indigo.*;

public interface AdderDB {
  boolean hasTable();
  
  void createTable();
  
  void setSavePoint(SavePoint savePoint);
  
  SavePoint getSavePoint(ActorRef actorRef);
  
  void deleteTable();
}
