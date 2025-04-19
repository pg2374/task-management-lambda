package org.piyush.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.piyush.exception.TaskNotFoundException;
import org.piyush.mapper.TaskMapperImpl;
import org.piyush.model.dynamodb.DbTask;
import org.piyush.model.taskmanagement.*;
import org.piyush.repositories.TaskRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private TaskMapperImpl taskMapper;

  @InjectMocks
  private TaskService taskService;

  @Test
  void createTask_Success() {
    TaskCreate taskCreate = new TaskCreate();
    taskCreate.setTitle("Test Task");

    DbTask dbTask = new DbTask();
    dbTask.setId("test-id");
    dbTask.setTitle("Test Task");

    TaskRead expectedRead = new TaskRead();
    expectedRead.setId("test-id");
    expectedRead.setTitle("Test Task");

    when(taskMapper.mapCreateToDb(taskCreate)).thenReturn(dbTask);
    when(taskRepository.save(dbTask)).thenReturn(dbTask);
    when(taskMapper.mapDbToRead(dbTask)).thenReturn(expectedRead);

    TaskRead result = taskService.createTask(taskCreate);

    assertThat(result).isEqualTo(expectedRead);
    verify(taskMapper).mapCreateToDb(taskCreate);
    verify(taskRepository).save(dbTask);
    verify(taskMapper).mapDbToRead(dbTask);
  }

  @Test
  void getTask_Success() {
    String taskId = "test-id";
    DbTask dbTask = new DbTask();
    dbTask.setId(taskId);

    TaskRead expectedRead = new TaskRead();
    expectedRead.setId(taskId);

    when(taskRepository.findById(taskId)).thenReturn(dbTask);
    when(taskMapper.mapDbToRead(dbTask)).thenReturn(expectedRead);

    TaskRead result = taskService.getTask(taskId);

    assertThat(result).isEqualTo(expectedRead);
    verify(taskRepository).findById(taskId);
    verify(taskMapper).mapDbToRead(dbTask);
  }

  @Test
  void getTask_NotFound() {
    String taskId = "test-id";
    when(taskRepository.findById(taskId)).thenThrow(TaskNotFoundException.class);

    assertThrows(TaskNotFoundException.class, () -> taskService.getTask(taskId));
    verify(taskRepository).findById(taskId);
    verify(taskMapper, never()).mapDbToRead(any());
  }

  @Test
  void updateTask_Success() {
    TaskUpdate taskUpdate = new TaskUpdate();
    taskUpdate.setId("test-id");
    taskUpdate.setTitle("Updated Task");

    DbTask existingTask = new DbTask();
    existingTask.setId("test-id");

    TaskRead expectedRead = new TaskRead();
    expectedRead.setId("test-id");
    expectedRead.setTitle("Updated Task");

    when(taskRepository.findById(taskUpdate.getId())).thenReturn(existingTask);
    when(taskRepository.save(existingTask)).thenReturn(existingTask);
    when(taskMapper.mapDbToRead(existingTask)).thenReturn(expectedRead);

    TaskRead result = taskService.updateTask(taskUpdate);

    assertThat(result).isEqualTo(expectedRead);
    verify(taskRepository).findById(taskUpdate.getId());
    verify(taskMapper).mapUpdateToDb(taskUpdate, existingTask);
    verify(taskRepository).save(existingTask);
    verify(taskMapper).mapDbToRead(existingTask);
  }

  @Test
  void updateTask_NotFound() {
    TaskUpdate taskUpdate = new TaskUpdate();
    taskUpdate.setId("test-id");

    when(taskRepository.findById(taskUpdate.getId())).thenThrow(TaskNotFoundException.class);

    assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(taskUpdate));
    verify(taskRepository).findById(taskUpdate.getId());
    verify(taskMapper, never()).mapUpdateToDb(any(), any());
    verify(taskRepository, never()).save(any());
  }

  @Test
  void getAllTasks_Success() {
    DbTask task1 = new DbTask();
    task1.setId("id1");
    DbTask task2 = new DbTask();
    task2.setId("id2");

    TaskRead taskRead1 = new TaskRead();
    taskRead1.setId("id1");
    TaskRead taskRead2 = new TaskRead();
    taskRead2.setId("id2");

    when(taskRepository.findAll()).thenReturn(Arrays.asList(task1, task2));
    when(taskMapper.mapDbToRead(task1)).thenReturn(taskRead1);
    when(taskMapper.mapDbToRead(task2)).thenReturn(taskRead2);

    List<TaskRead> result = taskService.getAllTasks();

    assertThat(result).hasSize(2)
        .containsExactly(taskRead1, taskRead2);
    verify(taskRepository).findAll();
    verify(taskMapper).mapDbToRead(task1);
    verify(taskMapper).mapDbToRead(task2);
  }

  @Test
  void deleteTask_Success() {
    String taskId = "test-id";

    List<DbTask> existingTasks = List.of(new DbTask());
    existingTasks.get(0).setId(taskId);

    when(taskRepository.findAllByIdOrderByDeadline(taskId)).thenReturn(existingTasks);

    taskService.deleteTask(taskId);

    verify(taskRepository).findAllByIdOrderByDeadline(taskId);
    verify(taskRepository).deleteById(taskId);
  }

  @Test
  void deleteTask_NotFound() {
    String taskId = "test-id";
    when(taskRepository.findAllByIdOrderByDeadline(taskId)).thenThrow(TaskNotFoundException.class);

    assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(taskId));
    verify(taskRepository).findAllByIdOrderByDeadline(taskId);
    verify(taskRepository, never()).deleteById(any());
  }
}