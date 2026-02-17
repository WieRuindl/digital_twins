package org.example.digitaltwin.configuration;

import java.util.Objects;
import lombok.NonNull;
import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.example.digitaltwin.dto.response.StatusMessage;
import org.example.digitaltwin.dto.response.StatusMessage.CorrelationStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Configuration
public class AggregatorFlowConfiguration {

  @Bean
  public IntegrationFlow aggregatorFlow(
      @NonNull final MessageChannel inputChannel, @NonNull final MessageChannel outputChannel) {
    return IntegrationFlow.from(inputChannel)
        .aggregate(
            a ->
                a.correlationStrategy(
                        m -> {
                          Object payload = m.getPayload();
                          if (payload instanceof ConditionerCommand c) return c.uid();
                          if (payload instanceof SensorMessage s) return s.id();
                          return null;
                        })
                    .releaseStrategy(g -> g.size() == 2)
                    .groupTimeout(20_000)
                    .sendPartialResultOnExpiry(true)
                    .outputProcessor(this::calculateStatus))
        .<StatusMessage>filter(s -> s.status() != null, e -> e.discardChannel("nullChannel"))
        .channel(outputChannel)
        .get();
  }

  private StatusMessage calculateStatus(MessageGroup group) {
    var messages = group.getMessages().stream().map(Message::getPayload).toList();

    var conditionerCommand =
        messages.stream()
            .filter(p -> p instanceof ConditionerCommand)
            .map(ConditionerCommand.class::cast)
            .findFirst()
            .orElse(null);

    var sensor =
        messages.stream()
            .filter(p -> p instanceof SensorMessage)
            .map(SensorMessage.class::cast)
            .findFirst()
            .orElse(null);

    if (conditionerCommand != null && sensor != null) {
      var status =
          Objects.equals(conditionerCommand.temperature(), sensor.temp())
              ? CorrelationStatus.MATCH
              : CorrelationStatus.MISMATCH;
      return new StatusMessage(conditionerCommand.uid(), status);
    }

    if (conditionerCommand != null) {
      return new StatusMessage(conditionerCommand.uid(), CorrelationStatus.LOST);
    }

    return new StatusMessage("IGNORE", null);
  }
}
