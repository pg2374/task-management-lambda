package org.piyush.service;

import org.mapstruct.factory.Mappers;
import org.piyush.mapper.TaskMapper;
import org.piyush.model.dynamodb.DbTask;
import org.piyush.model.taskmanagement.*;
import org.piyush.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class TaskService {
  private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
  private final TaskRepository taskRepository;
  private final TaskMapper taskMapper;

  // ✅ Default constructor for AWS Lambda compatibility
  public TaskService() {
    this.taskRepository = new TaskRepository(); // Ensure TaskRepository has a default constructor
    this.taskMapper = Mappers.getMapper(TaskMapper.class); // Ensure TaskMapper has a default constructor
  }

  public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
    this.taskRepository = taskRepository;
    this.taskMapper = taskMapper;
  }

  public TaskRead createTask(TaskCreate taskCreate) {
    logger.info("Creating a new task with title: {}", taskCreate.getTitle());
    DbTask dbTask = taskMapper.mapCreateToDb(taskCreate);
    DbTask savedTask = taskRepository.save(dbTask);
    logger.info("Task created successfully with ID: {}", savedTask.getId());
    return taskMapper.mapDbToRead(savedTask);
  }

  public TaskRead getTask(String taskId) {
    DbTask dbTask = taskRepository.findById(taskId);
    return taskMapper.mapDbToRead(dbTask);
  }

  public TaskRead getTask(String taskId, Instant deadline) {
    DbTask dbTask = taskRepository.findByIdAndDeadline(taskId, deadline);
    return taskMapper.mapDbToRead(dbTask);
  }

  public TaskRead updateTask(TaskUpdate taskUpdate) {
    DbTask existingTask = taskRepository.findById(taskUpdate.getId());
    taskMapper.mapUpdateToDb(taskUpdate, existingTask);
    DbTask updatedTask = taskRepository.save(existingTask);
    logger.info("Task with ID [{}] updated successfully.", updatedTask.getId());
    return taskMapper.mapDbToRead(updatedTask);
  }

  public List<TaskRead> getAllTasks() {
    List<DbTask> tasks = taskRepository.findAll();
    logger.info("Retrieved {} tasks from the repository", tasks.size());
    return tasks.stream()
        .map(taskMapper::mapDbToRead)
        .toList();
  }

  public void deleteTask(String taskId) {
    taskRepository.findAllByIdOrderByDeadline(taskId);
    taskRepository.deleteById(taskId);
    logger.info("Task with ID [{}] deleted successfully.", taskId);
  }
}