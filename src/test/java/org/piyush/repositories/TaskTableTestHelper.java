package org.piyush.repositories;

import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import org.piyush.config.DynamoDbConfig;
import org.piyush.model.dynamodb.DbTask;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;

import java.util.Arrays;

public class TaskTableTestHelper {
  public static DynamoDbTable<DbTask> createTaskTable(String tableName) {
    TableSchema<DbTask> schema = TableSchema.fromBean(DbTask.class);
    DynamoDbTable<DbTask> taskTable = DynamoDbConfig.dynamoDbEnhancedClient()
        .table(tableName, schema);

    try {
      taskTable.describeTable();
      System.out.println("Table already exists");
    } catch (ResourceNotFoundException e) {
      System.out.println("Creating table...");
      CreateTableEnhancedRequest createTableRequest = createTableRequest();
      taskTable.createTable(createTableRequest);
    }
    return taskTable;
  }

  protected static void waitForTableToBecomeActive(DynamoDbTable<?> table) {
    try {
      int attempts = 0;
      while (attempts < 10) {
        var description = table.describeTable();
        if (description.table().tableStatus() ==
            software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE) {
          System.out.println("Table is now active");
          return;
        }
        Thread.sleep(1000);
        attempts++;
      }
      throw new RuntimeException("Table did not become active within timeout");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for table to become active", e);
    }
  }

  private static CreateTableEnhancedRequest createTableRequest() {
    return CreateTableEnhancedRequest.builder()
        .provisionedThroughput(b -> b
            .readCapacityUnits(10L)
            .writeCapacityUnits(10L)
            .build())
        .globalSecondaryIndices(Arrays.asList(
            createAssigneeIndex(),
            createStatusIndex()))
        .build();
  }

  private static EnhancedGlobalSecondaryIndex createAssigneeIndex() {
    return EnhancedGlobalSecondaryIndex.builder()
        .indexName("AssigneeIndex")
        .provisionedThroughput(b -> b
            .readCapacityUnits(10L)
            .writeCapacityUnits(10L)
            .build())
        .projection(Projection.builder()
            .projectionType(ProjectionType.ALL.toString())
            .build())
        .build();
  }

  private static EnhancedGlobalSecondaryIndex createStatusIndex() {
    return EnhancedGlobalSecondaryIndex.builder()
        .indexName("StatusIndex")
        .provisionedThroughput(b -> b
            .readCapacityUnits(10L)
            .writeCapacityUnits(10L)
            .build())
        .projection(Projection.builder()
            .projectionType(ProjectionType.ALL.toString())
            .build())
        .build();
  }
}
