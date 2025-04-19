package org.piyush.repositories;

import org.piyush.config.DynamoDbConfig;
import org.piyush.exception.TaskNotFoundException;
import org.piyush.exception.TaskRepositoryException;
import org.piyush.model.dynamodb.DbTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class TaskRepository {
  private static final Logger logger = LoggerFactory.getLogger(TaskRepository.class);
  private static final String TABLE_NAME = System.getenv().getOrDefault("DYNAMODB_TABLE_NAME", "task_management");
  private final DynamoDbTable<DbTask> taskTable;

  public TaskRepository() {
    this.taskTable = DynamoDbConfig.dynamoDbEnhancedClient()
        .table(TABLE_NAME, TableSchema.fromBean(DbTask.class));
  }

  // New constructor for testing
  public TaskRepository(DynamoDbTable<DbTask> taskTable) {
    this.taskTable = taskTable;
  }

  public DbTask save(DbTask task) {
    try {
      taskTable.putItem(task);
      logger.info("Task saved successfully with ID: {}", task.getId());
      return task;
    } catch (DynamoDbException e) {
      throw new TaskRepositoryException(String.format("Failed to save task with ID [%s]", task.getId()), e);
    }
  }

  public DbTask findById(String taskId) {
    try {
      // Scan the table for the specific ID since we don't know the deadline
      Iterator<DbTask> results = taskTable.query(r -> r
          .queryConditional(QueryConditional.keyEqualTo(k -> k
              .partitionValue(taskId)))
      ).items().iterator();

      if (results.hasNext()) {
        DbTask task = results.next();
        logger.info("Task retrieved successfully with ID: {}", taskId);
        return task;
      } else {
        throw new TaskNotFoundException(String.format("Task not found with id [%s]", taskId));
      }
    } catch (DynamoDbException e) {
      throw new TaskRepositoryException(String.format("Failed to retrieve task with ID [%s]", taskId), e);
    }
  }

  // Get a task by both ID and deadline (more efficient)
  public DbTask findByIdAndDeadline(String taskId, Instant deadline) {
    try {
      // ✅ Ensure the format exactly matches DynamoDB stored format
      String formattedDeadline = DateTimeFormatter.ISO_INSTANT.format(deadline);
      logger.info("🔍 Querying table: {}", taskTable.tableName());

      logger.info("🔍 Querying task with ID: {} and formatted deadline: {}", taskId, formattedDeadline);

      DbTask task = taskTable.getItem(Key.builder()
          .partitionValue(taskId)
          .sortValue(formattedDeadline) // 🔥 Use exact format stored in DynamoDB
          .build());

      if (task != null) {
        logger.info("✅ Task retrieved successfully with ID: {} and deadline: {}", taskId, formattedDeadline);
        return task;
      } else {
        logger.warn("❌ Task not found with ID [{}] and deadline [{}]", taskId, formattedDeadline);
        throw new TaskNotFoundException(
            String.format("Task not found with id [%s] and deadline [%s]", taskId, formattedDeadline));
      }
    } catch (DynamoDbException e) {
      logger.error("🚨 DynamoDB query failed: {}", e.getMessage(), e); // 🔥 Log the actual error message
      throw new TaskRepositoryException(
          String.format("Failed to retrieve task with ID [%s] and deadline [%s]", taskId, deadline), e);
    }
  }

  // Find all tasks for a given ID ordered by deadline
  public List<DbTask> findAllByIdOrderByDeadline(String taskId) {
    return taskTable.query(r -> r
            .queryConditional(QueryConditional.keyEqualTo(k -> k
                .partitionValue(taskId))))
        .items()
        .stream()
        .toList();
  }

  public List<DbTask> findAll() {
    try {
      List<DbTask> tasks = taskTable.scan()
          .items()
          .stream()
          .toList();
      logger.info("Retrieved {} tasks from the table.", tasks.size());
      return tasks;
    } catch (DynamoDbException e) {
      throw new TaskRepositoryException("Failed to retrieve tasks", e);
    }
  }

  public void deleteById(String taskId) {
    try {
      taskTable.deleteItem(Key.builder().partitionValue(taskId).build());
      logger.info("Task deleted successfully with ID: {}", taskId);
    } catch (DynamoDbException e) {
      throw new TaskRepositoryException(String.format("Failed to delete task with ID [%s]", taskId), e);
    }
  }

  public void deleteById(String taskId, Instant deadline) {
    try {
      Key key = Key.builder()
          .partitionValue(taskId)
          .sortValue(deadline.toString()) // Add the sort key here
          .build();

      taskTable.deleteItem(key);
      logger.info("Task deleted successfully with ID: {}", taskId);
    } catch (DynamoDbException e) {
      throw new TaskRepositoryException(String.format("Failed to delete task with ID [%s]", taskId), e);
    }
  }
}
