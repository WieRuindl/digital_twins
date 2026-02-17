package org.example.digitaltwin.dto.response;

import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.example.digitaltwin.dto.request.SensorMessage;

/**
 * DTO class that indicates if {@link ConditionerCommand#temperature()} matches {@link
 * SensorMessage#temp()}
 *
 * @param uid id of the message
 * @param status matching status
 */
public record StatusMessage(String uid, CorrelationStatus status) {

  public enum CorrelationStatus {
    MATCH,
    MISMATCH,
    LOST
  }
}
