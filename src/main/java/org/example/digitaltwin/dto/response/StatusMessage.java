package org.example.digitaltwin.dto.response;

public record StatusMessage(String uid, CorrelationStatus status) {

  public enum CorrelationStatus {
    MATCH,
    MISMATCH,
    LOST
  }
}
