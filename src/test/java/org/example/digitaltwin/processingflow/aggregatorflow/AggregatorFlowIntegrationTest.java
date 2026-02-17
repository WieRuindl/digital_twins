package org.example.digitaltwin.processingflow.aggregatorflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.example.digitaltwin.dto.response.StatusMessage;
import org.example.digitaltwin.dto.response.StatusMessage.CorrelationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {AggregatorFlow.class, AggregatorFlowIntegrationTest.TestConfig.class})
@TestPropertySource(properties = "aggregator.timeout=1000")
class AggregatorFlowIntegrationTest {

  @Autowired private MessageChannel aggregatorInputChannel;

  @Autowired private QueueChannel aggregatorOutputChannel;

  @BeforeEach
  void clear() {
    aggregatorOutputChannel.clear();
  }

  @Test
  @DisplayName("Success matching")
  void shouldAggregateMatch() {
    // GIVEN
    var uid = "uuid-1";

    // WHEN
    aggregatorInputChannel.send(new GenericMessage<>(new ConditionerCommand(uid, 20.0, false)));
    assertThat(aggregatorOutputChannel.receive(500)).isNull();
    aggregatorInputChannel.send(new GenericMessage<>(new SensorMessage(uid, 20.0)));

    // THEN
    var result = aggregatorOutputChannel.receive(1000);
    assertThat(result).isNotNull();
    var status = (StatusMessage) result.getPayload();
    assertThat(status.uid()).isEqualTo(uid);
    assertThat(status.status()).isEqualTo(CorrelationStatus.MATCH);
  }

  @Test
  @DisplayName("Timeout leads to LOST")
  void shouldTimeoutAndSendLost() {
    // GIVEN
    var uid = "uuid-lost";

    // WHEN
    aggregatorInputChannel.send(new GenericMessage<>(new ConditionerCommand(uid, 20.0, false)));

    // THEN
    var result = aggregatorOutputChannel.receive(1500);
    assertThat(result).isNotNull();
    var status = (StatusMessage) result.getPayload();
    assertThat(status.uid()).isEqualTo(uid);
    assertThat(status.status()).isEqualTo(CorrelationStatus.LOST);
  }

  @TestConfiguration
  @EnableIntegration
  static class TestConfig {
    @Bean
    public MessageChannel aggregatorInputChannel() {
      return new DirectChannel();
    }

    @Bean
    public QueueChannel aggregatorOutputChannel() {
      return new QueueChannel(10);
    }
  }
}
