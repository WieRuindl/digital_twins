package org.example.digitaltwin.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.example.digitaltwin.dto.response.StatusMessage.CorrelationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.support.GenericMessage;

class AggregatorFlowLogicTest {

  private final AggregatorFlow aggregatorFlow = new AggregatorFlow();

  @Test
  @DisplayName("MATCH if temperatures are equal")
  void shouldReturnMatch() {
    // GIVEN
    var uid = "test-uuid";
    var conditionerCommand = new ConditionerCommand(uid, 22.0, false);
    var sensorMessage = new SensorMessage(uid, 22.0);
    var group =
        new SimpleMessageGroup(
            List.of(new GenericMessage<>(conditionerCommand), new GenericMessage<>(sensorMessage)),
            uid);

    // WHEN
    var result = aggregatorFlow.calculateStatus(group);

    // THEN
    assertThat(result.uid()).isEqualTo(uid);
    assertThat(result.status()).isEqualTo(CorrelationStatus.MATCH);
  }

  @Test
  @DisplayName("MISMATCH if temperatures are not equal")
  void shouldReturnMismatch() {
    // GIVEN
    var uid = "test-uuid";
    var conditionerCommand = new ConditionerCommand(uid, 25.0, false);
    var sensorMessage = new SensorMessage(uid, 24.0);
    var group =
        new SimpleMessageGroup(
            List.of(new GenericMessage<>(conditionerCommand), new GenericMessage<>(sensorMessage)),
            uid);

    // WHEN
    var result = aggregatorFlow.calculateStatus(group);

    // THEN
    assertThat(result.status()).isEqualTo(CorrelationStatus.MISMATCH);
  }

  @Test
  @DisplayName("LOST if no sensor message")
  void shouldReturnLost() {
    // GIVEN
    var uid = "test-uuid";
    var conditionerCommand = new ConditionerCommand(uid, 25.0, false);
    var group = new SimpleMessageGroup(List.of(new GenericMessage<>(conditionerCommand)), uid);

    // WHEN
    var result = aggregatorFlow.calculateStatus(group);

    // THEN
    assertThat(result.uid()).isEqualTo(uid);
    assertThat(result.status()).isEqualTo(CorrelationStatus.LOST);
  }
}
