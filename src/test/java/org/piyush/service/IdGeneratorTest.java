package org.piyush.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdGeneratorTest {

  IdGenerator idGenerator;

  @BeforeEach
  void setUp() {
    idGenerator = new IdGenerator();
  }

  @Test
  void generateSubTaskId() {
    String response = idGenerator.generateSubTaskId();
    assertTrue(response.startsWith("st-"));

    String uuid = response.substring(3);
    assertDoesNotThrow(() -> UUID.fromString(uuid));
  }
}