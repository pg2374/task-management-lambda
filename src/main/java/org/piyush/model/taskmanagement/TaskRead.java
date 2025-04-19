package org.piyush.model.taskmanagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskRead extends Task {
  @NotNull
  private String id;

  private String parentTaskId;
  private final List<String> dependentTaskIds = new ArrayList<>();

  @Valid
  private Metadata metadata;

  private TaskProgress progress;
}
