package org.piyush.exception;

public class TaskRepositoryException extends RuntimeException {
  public TaskRepositoryException(String message) {
    super(message);
  }

  public TaskRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
