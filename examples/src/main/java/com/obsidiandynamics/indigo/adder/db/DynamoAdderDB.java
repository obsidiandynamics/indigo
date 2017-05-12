package com.obsidiandynamics.indigo.adder.db;

import static java.util.Arrays.*;

import com.amazonaws.client.builder.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.obsidiandynamics.indigo.*;

public final class DynamoAdderDB implements AdderDB {
//  private static final String TABLE_NAME = "Adder";
//  private static final String ATT_ROLE = "actorRef.role";
//  private static final String ATT_KEY = "actorRef.key";
  
  private final AmazonDynamoDB db;

  public DynamoAdderDB() {
    db = AmazonDynamoDBClientBuilder.standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", Regions.DEFAULT_REGION.getName()))
        .build();
  }
  
  private DynamoDBMapper mapper() {
    return new DynamoDBMapper(db);
  }
  
  private String getTableName() {
    return mapper().generateDeleteTableRequest(SavePoint.class).getTableName();
  }
  
  @Override
  public boolean hasTable() {
    final ListTablesResult rs = db.listTables(new ListTablesRequest());
    return rs.getTableNames().contains(getTableName());
  }

  @Override
  public void createTable() {
    final CreateTableRequest rq = mapper().generateCreateTableRequest(SavePoint.class);
    rq.setProvisionedThroughput(new ProvisionedThroughput(10L, 10L));
    db.createTable(rq);
//    db.createTable(asList(new AttributeDefinition(ATT_ROLE, ScalarAttributeType.S),
//                          new AttributeDefinition(ATT_KEY, ScalarAttributeType.S)),
//                   TABLE_NAME,
//                   asList(new KeySchemaElement(ATT_ROLE, KeyType.HASH),
//                          new KeySchemaElement(ATT_KEY, KeyType.RANGE)),
//                   new ProvisionedThroughput(10L, 10L));
  }

  @Override
  public void setSavePoint(SavePoint savePoint) {
    mapper().save(savePoint);
  }

  @Override
  public SavePoint getSavePoint(ActorRef actorRef) {
    return mapper().load(SavePoint.class, actorRef.role(), SavePoint.formatActorKey(actorRef));
  }

  @Override
  public void deleteTable() {
    db.deleteTable(mapper().generateDeleteTableRequest(SavePoint.class));
//    db.deleteTable(TABLE_NAME);
  }
}
