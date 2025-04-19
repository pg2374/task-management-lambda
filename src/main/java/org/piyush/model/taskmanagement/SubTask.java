package org.piyush.model.taskmanagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.piyush.constant.TaskConstants;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
  private String id;

  @NotBlank(message = "SubTask title is mandatory")
  @Size(max = TaskConstants.TITLE_MAX_LENGTH)
  private String title;

  private Boolean completed = false;

  private Instant dueDate;

  @Size(max = TaskConstants.DESCRIPTION_MAX_LENGTH)
  private String description;

  private String assignee;

  private Instant completedAt;
}
