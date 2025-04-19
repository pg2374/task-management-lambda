package org.piyush.mapper;

import lombok.Setter;
import org.mapstruct.*;
import org.piyush.constant.TaskStatus;
import org.piyush.model.taskmanagement.TaskProgress;
import org.piyush.model.dynamodb.DbTask;
import org.piyush.model.taskmanagement.SubTask;
import org.piyush.model.taskmanagement.TaskCreate;
import org.piyush.model.taskmanagement.TaskRead;
import org.piyush.model.taskmanagement.TaskUpdate;
import org.piyush.service.IdGenerator;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Setter
@Mapper
public abstract class TaskMapper {

  private IdGenerator idGenerator;

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "parentTaskId", ignore = true)
  @Mapping(target = "dependentTaskIds", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "subTasks", expression = "java(mapSubTasks(request.getSubTasks()))")
  public abstract DbTask mapCreateToDb(TaskCreate request);

  @Mapping(target = "metadata.createdAt", source = "createdAt")
  @Mapping(target = "metadata.updatedAt", source = "updatedAt")
  @Mapping(target = "metadata.version", source = "version")
  @Mapping(target = "progress", expression = "java(calculateProgress(task))")
  public abstract TaskRead mapDbToRead(DbTask task);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  public abstract void mapUpdateToDb(TaskUpdate request, @MappingTarget DbTask task);

  @AfterMapping
  protected void setDefaultValues(@MappingTarget DbTask task) {
    if (task.getStatus() == null) {
      task.setStatus(TaskStatus.PENDING);
    }
  }

  protected List<SubTask> mapSubTasks(List<SubTask> subTasks) {
    return Optional.ofNullable(subTasks)
        .map(list -> list.stream()
            .map(this::mapSubTask)
            .toList())
        .orElse(Collections.emptyList());
  }

  protected SubTask mapSubTask(SubTask subTask) {
    if (subTask == null) {
      return null;
    }
    if (subTask.getId() == null || subTask.getId().isBlank()) {
      subTask.setId(idGenerator.generateSubTaskId());
    }
    return subTask;
  }

  protected TaskProgress calculateProgress(DbTask task) {
    int total = Optional.ofNullable(task.getSubTasks()).map(List::size).orElse(0);
    int completed = Optional.ofNullable(task.getSubTasks())
        .orElse(Collections.emptyList())
        .stream()
        .filter(SubTask::getCompleted)
        .mapToInt(e -> 1)
        .sum();

    double percentage = total > 0 ? ((double) completed / total) * 100 : 0;
    return new TaskProgress(total, completed, percentage, Instant.now());
  }
}
