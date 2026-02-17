package org.example.digitaltwin.processingflow.aggregatorflow;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.example.digitaltwin.dto.response.StatusMessage;
import org.example.digitaltwin.dto.response.StatusMessage.CorrelationStatus;
import org.example.digitaltwin.utils.VisibleForTesting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/** Configures the aggregation flow to correlate outgoing commands with incoming sensor data. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AggregatorFlow {

  @Value("${aggregator.timeout:20000}")
  private long timeoutInMS;

  /**
   * Defines the flow that aggregates messages by UID. Groups are released when a pair (Command +
   * Sensor) is complete or upon timeout. Orphan sensor messages (status=null) are filtered out.
   */
  @Bean
  public IntegrationFlow aggregatorFlowProcessor(
      MessageChannel aggregatorInputChannel, MessageChannel aggregatorOutputChannel) {

    return IntegrationFlow.from(aggregatorInputChannel)
        .aggregate(
            a ->
                a.correlationStrategy(this::correlateByUid)
                    .releaseStrategy(g -> g.size() == 2)
                    .groupTimeout(timeoutInMS)
                    .sendPartialResultOnExpiry(true)
                    .outputProcessor(this::calculateStatus))
        .<StatusMessage>filter(s -> s.status() != null, e -> e.discardChannel("nullChannel"))
        .channel(aggregatorOutputChannel)
        .get();
  }

  /** Extracts the unique correlation ID from the message payload. */
  private Object correlateByUid(Message<?> message) {
    Object payload = message.getPayload();
    if (payload instanceof ConditionerCommand c) return c.uid();
    if (payload instanceof SensorMessage s) return s.id();

    log.warn("Unknown message type in aggregator: {}", payload.getClass());
    return null;
  }

  /**
   * Determines the final status (MATCH, MISMATCH, LOST) based on the grouped messages. Returns a
   * status with null value for orphan sensor readings to allow filtering.
   */
  @VisibleForTesting
  protected StatusMessage calculateStatus(MessageGroup group) {
    var messages = group.getMessages().stream().map(Message::getPayload).toList();

    var conditionerCommand =
        messages.stream()
            .filter(ConditionerCommand.class::isInstance)
            .map(ConditionerCommand.class::cast)
            .findFirst()
            .orElse(null);

    var sensorMessage =
        messages.stream()
            .filter(SensorMessage.class::isInstance)
            .map(SensorMessage.class::cast)
            .findFirst()
            .orElse(null);

    if (conditionerCommand != null && sensorMessage != null) {
      boolean temperatureMatches =
          Objects.equals(conditionerCommand.temperature(), sensorMessage.temp());
      return new StatusMessage(
          conditionerCommand.uid(),
          temperatureMatches ? CorrelationStatus.MATCH : CorrelationStatus.MISMATCH);
    }

    if (conditionerCommand != null) {
      return new StatusMessage(conditionerCommand.uid(), CorrelationStatus.LOST);
    }

    return new StatusMessage("IGNORE", null);
  }
}
