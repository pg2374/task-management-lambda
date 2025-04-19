package org.piyush.model.taskmanagement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgress {
  private int totalSubTasks;
  private int completedSubTasks;
  private double progressPercentage;
  private Instant lastUpdated;
}
