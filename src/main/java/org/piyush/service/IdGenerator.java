package org.piyush.service;

import java.util.UUID;

public class IdGenerator {
  public String generateSubTaskId() {
    return "st-" + UUID.randomUUID();
  }
}