package com.obsidiandynamics.indigo.adder.db;

import com.amazonaws.auth.*;
import com.amazonaws.client.builder.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.obsidiandynamics.indigo.*;

public final class DynamoAdderDB implements AdderDB {
  private final AmazonDynamoDB db;

  public DynamoAdderDB(AWSCredentialsProvider credentialsProvider, AwsClientBuilder.EndpointConfiguration endpointConfig) {
    db = AmazonDynamoDBClientBuilder.standard()
        .withCredentials(credentialsProvider)
        .withEndpointConfiguration(endpointConfig)
        .build();
  }
  
  public static DynamoAdderDB withLocalEndpoint() {
    final BasicAWSCredentials credentials = new BasicAWSCredentials("", "");
    final AWSCredentialsProvider credentialsProvider = new AWSCredentialsProvider() {
      @Override public AWSCredentials getCredentials() { return credentials; }
      @Override public void refresh() {}
    };
    return new DynamoAdderDB(credentialsProvider,
                             new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", 
                                                                        Regions.DEFAULT_REGION.getName()));
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
  }
}
