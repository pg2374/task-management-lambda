package org.piyush.repositories;

import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.piyush.config.DynamoDbConfig;
import org.piyush.constant.Priority;
import org.piyush.constant.TaskStatus;
import org.piyush.exception.TaskNotFoundException;
import org.piyush.exception.TaskRepositoryException;
import org.piyush.model.dynamodb.DbTask;
import org.piyush.model.taskmanagement.SubTask;
import org.piyush.repositories.base.DynamoDBBaseRepositoryTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.piyush.repositories.TaskTableTestHelper.waitForTableToBecomeActive;

class TaskRepositoryTest extends DynamoDBBaseRepositoryTest {

  private TaskRepository repository;
  private static DynamoDbTable<DbTask> taskTable;

  @BeforeAll
  static void initializeTable() {
    TableSchema<DbTask> schema = TableSchema.fromBean(DbTask.class);
    System.out.println("Table Schema: " + schema);

    taskTable = DynamoDbConfig.dynamoDbEnhancedClient()
        .table(TABLE_NAME, schema);

    try {
      taskTable.describeTable();
      System.out.println("Table already exists");
    } catch (ResourceNotFoundException e) {
      System.out.println("Creating table...");
      CreateTableEnhancedRequest createTableRequest = CreateTableEnhancedRequest.builder()
          .provisionedThroughput(b -> b
              .readCapacityUnits(10L)
              .writeCapacityUnits(10L)
              .build())
          .globalSecondaryIndices(Arrays.asList(
              EnhancedGlobalSecondaryIndex.builder()
                  .indexName("AssigneeIndex")
                  .provisionedThroughput(b -> b
                      .readCapacityUnits(10L)
                      .writeCapacityUnits(10L)
                      .build())
                  .projection(Projection.builder()
                      .projectionType(ProjectionType.ALL.toString())
                      .build())
                  .build(),
              EnhancedGlobalSecondaryIndex.builder()
                  .indexName("StatusIndex")
                  .provisionedThroughput(b -> b
                      .readCapacityUnits(10L)
                      .writeCapacityUnits(10L)
                      .build())
                  .projection(Projection.builder()
                      .projectionType(ProjectionType.ALL.toString())
                      .build())
                  .build()))
          .build();

      taskTable.createTable(createTableRequest);
      waitForTableToBecomeActive(taskTable);
    }
  }

  @BeforeEach
  void setUp() {
    repository = new TaskRepository();
    cleanTable();
  }

//  @Test
//  void shouldRetrieveTaskByIdAndDeadline() {
//    DbTask task = createSampleTask();
//    repository.save(task);
//
//    DbTask retrieved = repository.findByIdAndDeadline(task.getId(), task.getDeadline());
//    assertNotNull(retrieved);
//    assertEquals(task.getId(), retrieved.getId());
//    assertEquals(task.getDeadline(), retrieved.getDeadline());
//  }

  @Test
  void shouldDeleteTaskById() {
    DbTask task = createSampleTask();
    repository.save(task);

    // Include deadline if your schema requires a sort key
    repository.deleteById(task.getId(), task.getDeadline());

    assertThrows(TaskNotFoundException.class, () -> repository.findById(task.getId()));
  }


  @Test
  void shouldSaveAndRetrieveTask() {
    DbTask task = createSampleTask();
    repository.save(task);

    DbTask retrieved = repository.findById(task.getId());
    assertNotNull(retrieved);
    assertEquals(task.getTitle(), retrieved.getTitle());
  }

  @Test
  void shouldCleanTableBeforeEachTest() {
    DbTask task = createSampleTask();
    repository.save(task);

    cleanTable();
    List<DbTask> tasks = repository.findAll();
    assertTrue(tasks.isEmpty());
  }

  @Test
  void shouldHandleDynamoDbExceptionOnSave() {
    DbTask task = createSampleTask();

    DynamoDbTable mockTable = Mockito.mock(DynamoDbTable.class);

    Mockito.doThrow(DynamoDbException.builder().message("Simulated DynamoDbException").build())
        .when(mockTable)
        .putItem(any(DbTask.class));

    TaskRepository repositoryWithMock = new TaskRepository(mockTable);

    TaskRepositoryException exception = assertThrows(TaskRepositoryException.class, () -> repositoryWithMock.save(task));

    assertTrue(exception.getMessage().contains("Failed to save task"));
  }


  @Test
  void shouldThrowExceptionWhenTaskNotFoundById() {
    String nonExistentId = UUID.randomUUID().toString();

    Exception exception = assertThrows(TaskNotFoundException.class, () -> repository.findById(nonExistentId));

    assertEquals("Task not found with id [" + nonExistentId + "]", exception.getMessage());
  }

  private DbTask createSampleTask() {
    DbTask task = new DbTask();
    task.setId(UUID.randomUUID().toString());
    task.setTitle("Sample Task");
    task.setDescription("This is a test task");
    task.setPriority(Priority.HIGH);
    task.setDeadline(Instant.now().plusSeconds(86400)); // tomorrow
    task.setStatus(TaskStatus.PENDING);
    task.setLabels(Arrays.asList("test", "sample"));

    List<SubTask> subTasks = Arrays.asList(
        createSubTask("Subtask 1"),
        createSubTask("Subtask 2")
    );
    task.setSubTasks(subTasks);

    return task;
  }

  private SubTask createSubTask(String title) {
    SubTask subTask = new SubTask();
    subTask.setId(UUID.randomUUID().toString());
    subTask.setTitle(title);
    subTask.setCompleted(false);
    return subTask;
  }

  private void cleanTable() {
    try {
      System.out.println("Cleaning table before test...");
      var items = taskTable.scan().items();
      items.forEach(taskTable::deleteItem);
    } catch (Exception e) {
      System.out.println("Error clearing table: " + e.getMessage());
    }
  }
}
