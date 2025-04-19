package org.piyush.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatus {
  PENDING("PENDING"),
  IN_PROGRESS("IN_PROGRESS"),
  BLOCKED("BLOCKED"),
  IN_REVIEW("IN_REVIEW"),
  COMPLETED("COMPLETED");

  private final String status;
}
