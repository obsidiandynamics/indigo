package com.obsidiandynamics.indigo.adder.db;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.obsidiandynamics.indigo.*;

@DynamoDBTable(tableName="Adder")
public final class SavePoint {
  private static final String NULL_KEY = "_null";
  
  private String actorRole;
  
  private String actorKey;
  
  private int sum;

  @DynamoDBHashKey(attributeName="actorRef.role")  
  public final String getActorRole() {
    return actorRole;
  }
  
  public final void setActorRole(String actorRole) {
    this.actorRole = actorRole;
  }

  @DynamoDBRangeKey(attributeName="actorRef.key")  
  public final String getActorKey() {
    return actorKey;
  }
  
  @DynamoDBIgnore
  public ActorRef getActorRef() {
    return ActorRef.of(actorRole, actorKey.equals(NULL_KEY) ? null : actorKey);
  }

  public final void setActorKey(String actorKey) {
    this.actorKey = actorKey;
  }

  @DynamoDBAttribute(attributeName="sum")
  public final int getSum() {
    return sum;
  }

  public final void setSum(int sum) {
    this.sum = sum;
  }

  @Override
  public String toString() {
    return "SavePoint [actorRef=" + getActorRef() + ", sum=" + sum + "]";
  }

  public static SavePoint of(ActorRef actorRef, int sum) {
    final SavePoint savePoint = new SavePoint();
    savePoint.actorRole = actorRef.role();
    savePoint.actorKey = formatActorKey(actorRef);
    savePoint.sum = sum;
    return savePoint;
  }
  
  public static String formatActorKey(ActorRef actorRef) {
    return actorRef.key() != null ? actorRef.key() : NULL_KEY;
  }
}
