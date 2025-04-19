package org.piyush.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskConstants {
  public static final TaskStatus DEFAULT_STATUS = TaskStatus.PENDING;
  public static final int TITLE_MAX_LENGTH = 100;
  public static final int DESCRIPTION_MAX_LENGTH = 500;
}
