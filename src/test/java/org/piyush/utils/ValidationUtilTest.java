package org.piyush.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.piyush.constant.Priority;
import org.piyush.constant.TaskStatus;
import org.piyush.model.taskmanagement.TaskCreate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidationUtilTest {

  @Test
  void shouldPassValidation_WhenTaskIsValid() {
    TaskCreate validTask = (TaskCreate) new TaskCreate()
        .setTitle("Test a valid create object")
        .setDescription("Validation should pass")
        .setPriority(Priority.HIGH)
        .setStatus(TaskStatus.IN_PROGRESS);

    assertDoesNotThrow(() -> ValidationUtil.validate(validTask));
  }

  @Test
  void shouldThrowException_WhenTaskIsInvalid() {
    TaskCreate invalidTask = (TaskCreate) new TaskCreate()
        .setDescription("Malformed create object - Validation should fail");

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ValidationUtil.validate(invalidTask)
    );

    String errorMessage = exception.getMessage();
    assertTrue(errorMessage.contains("Validation failed:"));
  }
}