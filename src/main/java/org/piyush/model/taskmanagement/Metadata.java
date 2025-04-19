package org.piyush.model.taskmanagement;

import lombok.Data;

import java.time.Instant;

@Data
public class Metadata {
  private Instant createdAt;
  private Instant updatedAt;
  private Long version;

  public Metadata() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }
}
