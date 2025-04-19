package org.piyush.model.taskmanagement;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class TaskUpdate extends Task {
  @NotNull
  private String id;

  private String parentTaskId;

  private List<String> dependentTaskIds = new ArrayList<>();
}
