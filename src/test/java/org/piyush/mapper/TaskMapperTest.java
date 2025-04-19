package org.piyush.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.piyush.constant.Priority;
import org.piyush.constant.TaskStatus;
import org.piyush.model.taskmanagement.TaskProgress;
import org.piyush.model.dynamodb.DbTask;
import org.piyush.model.taskmanagement.SubTask;
import org.piyush.model.taskmanagement.TaskCreate;
import org.piyush.model.taskmanagement.TaskRead;
import org.piyush.model.taskmanagement.TaskUpdate;
import org.piyush.service.IdGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskMapperTest {
  @Mock
  private IdGenerator idGenerator;

  @InjectMocks
  private TaskMapperImpl mapper;

  private static final Instant FIXED_TIME = Instant.parse("2024-01-17T10:00:00Z");
  private static final List<String> DEFAULT_LABELS = Arrays.asList("Label1", "Label2");
  private static final String DEFAULT_ASSIGNEE = "John Doe";

  @Test
  void mapCreateTob() {
    TaskCreate taskCreate = getTaskCreate();
    SubTask subTask1 = new SubTask().setId("").setTitle("SubTask 1");
    SubTask subTask2 = new SubTask().setTitle("SubTask 2");
    taskCreate.setSubTasks(Arrays.asList(subTask1, subTask2));

    when(idGenerator.generateSubTaskId())
        .thenReturn("st-1")
        .thenReturn("st-2");

    DbTask actual = mapper.mapCreateToDb(taskCreate);

    assertThat(actual)
        .satisfies(result -> {
          assertThat(result.getSubTasks()).hasSize(2);
          assertThat(result.getSubTasks().get(0).getId()).isEqualTo("st-1");
          assertThat(result.getSubTasks().get(1).getId()).isEqualTo("st-2");
        });

    verify(idGenerator, times(2)).generateSubTaskId();
  }

  @Test
  void mapDbToRead() {
    DbTask dbTask = getDbTask();
    dbTask.setSubTasks(Arrays.asList(
            new SubTask().setId("st-1").setTitle("SubTask 1"),
            new SubTask().setId("st-2").setTitle("SubTask 2")
        ));

    TaskRead actual = mapper.mapDbToRead(dbTask);

    assertThat(actual.getSubTasks())
        .hasSize(2)
        .extracting("id")
        .containsExactly("st-1", "st-2");

    verify(idGenerator, never()).generateSubTaskId();
  }

  @Test
  void mapUpdateToDb() {
    DbTask existingTask = getExistingTask();
    TaskUpdate updateRequest = (TaskUpdate) getUpdateRequest()
        .setSubTasks(new ArrayList<>(Collections.singletonList(new SubTask().setTitle("New SubTask"))));

    when(idGenerator.generateSubTaskId()).thenReturn("st-new");

    mapper.mapUpdateToDb(updateRequest, existingTask);

    assertThat(existingTask.getSubTasks())
        .hasSize(1)
        .extracting("id")
        .containsExactly("st-new");

    verify(idGenerator).generateSubTaskId();
  }

  @Test
  void mapSubTask() {
    SubTask input = new SubTask()
        .setTitle("Test SubTask")
        .setDescription("Test Description")
        .setAssignee(DEFAULT_ASSIGNEE);

    when(idGenerator.generateSubTaskId()).thenReturn("st-test");

    SubTask actual = mapper.mapSubTask(input);

    assertThat(actual.getId()).isEqualTo("st-test");
    verify(idGenerator).generateSubTaskId();
  }

  @Test
  void mapSubTask_WithExistingId() {
    SubTask input = new SubTask()
        .setId("existing-id")
        .setTitle("Test SubTask");

    SubTask actual = mapper.mapSubTask(input);

    assertThat(actual.getId()).isEqualTo("existing-id");
    verify(idGenerator, never()).generateSubTaskId();
  }

  @Test
  void mapSubTask_WithNullInput() {
    SubTask actual = mapper.mapSubTask(null);

    assertThat(actual).isNull();
    verify(idGenerator, never()).generateSubTaskId();
  }

  @Test
  void mapSubTask_WithEmptyId() {
    SubTask input = new SubTask()
        .setId("")
        .setTitle("Test SubTask");

    when(idGenerator.generateSubTaskId()).thenReturn("st-test");

    SubTask actual = mapper.mapSubTask(input);

    assertThat(actual.getId()).isEqualTo("st-test");
    verify(idGenerator).generateSubTaskId();
  }

  @Test
  void mapSubTask_WithBlankId() {
    SubTask input = new SubTask()
        .setId("   ")
        .setTitle("Test SubTask");

    when(idGenerator.generateSubTaskId()).thenReturn("st-test");

    SubTask actual = mapper.mapSubTask(input);

    assertThat(actual.getId()).isEqualTo("st-test");
    verify(idGenerator).generateSubTaskId();
  }

  @Test
  void mapCreateToDb_ShouldInitializeNullFields() {
    TaskCreate taskCreate = (TaskCreate) new TaskCreate()
        .setTitle("Test Task")
        .setStatus(null)
        .setSubTasks(null);

    DbTask result = mapper.mapCreateToDb(taskCreate);

    assertThat(result.getStatus())
        .as("Default status should be PENDING")
        .isEqualTo(TaskStatus.PENDING);

    assertThat(result.getSubTasks())
        .as("Default subtasks should be empty list")
        .isNotNull()
        .isEmpty();
  }

  @Test
  void calculateProgress_ShouldHandleNullOrEmptySubTasks() {
    DbTask nullSubTasksTask = new DbTask();
    nullSubTasksTask.setId("task-1");
    nullSubTasksTask.setSubTasks(null);
    DbTask emptySubTasksTask = new DbTask();
    emptySubTasksTask.setId("task-2");
    emptySubTasksTask.setSubTasks(Collections.emptyList());

    assertThat(mapper.mapDbToRead(nullSubTasksTask).getProgress())
        .satisfies(progress -> {
          assertThat(progress.getTotalSubTasks()).isZero();
          assertThat(progress.getCompletedSubTasks()).isZero();
          assertThat(progress.getProgressPercentage()).isEqualTo(0.0);
        });

    assertThat(mapper.mapDbToRead(emptySubTasksTask).getProgress())
        .satisfies(progress -> {
          assertThat(progress.getTotalSubTasks()).isZero();
          assertThat(progress.getCompletedSubTasks()).isZero();
          assertThat(progress.getProgressPercentage()).isEqualTo(0.0);
        });
  }

  @Test
  void calculateProgress_ShouldHandleNoCompletedSubTasks() {
    DbTask dbTask = new DbTask();
    dbTask.setId("task-3");
    dbTask.setSubTasks(List.of(
            new SubTask().setId("st-1").setCompleted(false),
            new SubTask().setId("st-2").setCompleted(false)
        ));

    TaskProgress progress = mapper.mapDbToRead(dbTask).getProgress();

    assertThat(progress)
        .satisfies(p -> {
          assertThat(p.getTotalSubTasks()).isEqualTo(2);
          assertThat(p.getCompletedSubTasks()).isZero();
          assertThat(p.getProgressPercentage()).isEqualTo(0.0);
        });
  }

  @Test
  void calculateProgress_ShouldHandlePartialAndFullCompletion() {
    DbTask partialCompletionTask = new DbTask();
    partialCompletionTask.setId("task-4");
    partialCompletionTask.setSubTasks(List.of(
            new SubTask().setId("st-1").setCompleted(true),
            new SubTask().setId("st-2").setCompleted(false)
        ));

    DbTask fullCompletionTask = new DbTask();
    fullCompletionTask.setId("task-5");
    fullCompletionTask.setSubTasks(List.of(
            new SubTask().setId("st-1").setCompleted(true),
            new SubTask().setId("st-2").setCompleted(true)
        ));

    assertThat(mapper.mapDbToRead(partialCompletionTask).getProgress())
        .satisfies(progress -> {
          assertThat(progress.getTotalSubTasks()).isEqualTo(2);
          assertThat(progress.getCompletedSubTasks()).isEqualTo(1);
          assertThat(progress.getProgressPercentage()).isEqualTo(50.0);
        });

    assertThat(mapper.mapDbToRead(fullCompletionTask).getProgress())
        .satisfies(progress -> {
          assertThat(progress.getTotalSubTasks()).isEqualTo(2);
          assertThat(progress.getCompletedSubTasks()).isEqualTo(2);
          assertThat(progress.getProgressPercentage()).isEqualTo(100.0);
        });
  }

  private TaskCreate getTaskCreate() {
    return (TaskCreate) new TaskCreate()
        .setTitle("Test Task")
        .setDescription("Test Description")
        .setPriority(Priority.HIGH)
        .setDeadline(FIXED_TIME)
        .setLabels(DEFAULT_LABELS)
        .setAssignee(DEFAULT_ASSIGNEE);
  }

  private TaskUpdate getUpdateRequest() {
    return (TaskUpdate) new TaskUpdate()
        .setTitle("New Title")
        .setDescription("New Description")
        .setPriority(Priority.HIGH)
        .setDeadline(FIXED_TIME.plusSeconds(3600))
        .setLabels(Arrays.asList("NewLabel1", "NewLabel2"))
        .setAssignee("New Assignee");
  }

  private DbTask getExistingTask() {
    DbTask existingTask = new DbTask();
    existingTask.setId("task-1");
    existingTask.setTitle("Old Title");
    existingTask.setDescription("Old Description");
    existingTask.setPriority(Priority.LOW);
    existingTask.setDeadline(FIXED_TIME);
    existingTask.setLabels(new ArrayList<>(Collections.singletonList("OldLabel1")));
    existingTask.setAssignee("Old Assignee");
    return existingTask;
  }

  private DbTask getDbTask() {
    DbTask dbTask = new DbTask();
    dbTask.setId("task-1");
    dbTask.setTitle("Test Task");
    dbTask.setDescription("Test Description");
    dbTask.setPriority(Priority.HIGH);
    dbTask.setDeadline(FIXED_TIME);
    dbTask.setLabels(DEFAULT_LABELS);
    dbTask.setAssignee(DEFAULT_ASSIGNEE);
    return dbTask;
  }
}