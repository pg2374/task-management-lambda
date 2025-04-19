package org.piyush.utils;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationUtil {
  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private static final Validator validator = factory.getValidator();

  public static <T> void validate(T object) {
    Set<ConstraintViolation<T>> violations = validator.validate(object);
    if (!violations.isEmpty()) {
      StringBuilder errorMessages = new StringBuilder();
      for (ConstraintViolation<T> violation : violations) {
        errorMessages.append(violation.getMessage()).append("; ");
      }
      throw new IllegalArgumentException("Validation failed: " + errorMessages.toString().trim());
    }
  }
}

