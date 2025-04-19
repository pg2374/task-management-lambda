package org.piyush.model.taskmanagement;

import lombok.Data;
import org.piyush.constant.Priority;
import org.piyush.constant.TaskStatus;

@Data
public class TaskSearchCriteria {
  private TaskStatus[] statuses;
  private Priority[] priorities;
  private String[] labels;
  private DateRange dateRange;
  private String assignee;
  private boolean includeCompleted;
}
