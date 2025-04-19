package org.piyush.model.taskmanagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.piyush.constant.Priority;
import org.piyush.constant.TaskConstants;
import org.piyush.constant.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.piyush.constant.TaskConstants.DEFAULT_STATUS;

@Data
public class Task {
  @NotBlank(message = "Title is mandatory")
  @Size(max = TaskConstants.TITLE_MAX_LENGTH)
  private String title;

  @Size(max = TaskConstants.DESCRIPTION_MAX_LENGTH)
  private String description;

  @NotNull(message = "Priority is mandatory")
  private Priority priority;

  @FutureOrPresent(message = "Deadline cannot be in the past")
  private Instant deadline;

  private List<String> labels = new ArrayList<>();

  @Valid
  private List<SubTask> subTasks = new ArrayList<>();

  private String assignee;

  private TaskStatus status = DEFAULT_STATUS;
}
