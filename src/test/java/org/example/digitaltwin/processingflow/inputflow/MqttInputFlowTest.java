package org.example.digitaltwin.processingflow.inputflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.UncheckedIOException;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {MqttInputFlow.class, MqttInputFlowTest.TestConfig.class})
class MqttInputFlowTest {

  @Autowired private SubscribableChannel mqttInputChannel;

  @Autowired private SubscribableChannel aggregatorInputChannel;

  @Autowired private SubscribableChannel mqttErrorChannel;

  @MockitoBean private MqttPahoMessageDrivenChannelAdapter mqttAdapter;

  private MessageHandler mockAggregatorHandler;
  private MessageHandler mockErrorHandler;

  @BeforeEach
  void setUp() {
    mockAggregatorHandler = mock(MessageHandler.class);
    aggregatorInputChannel.subscribe(mockAggregatorHandler);
    mockErrorHandler = mock(MessageHandler.class);
    mqttErrorChannel.subscribe(mockErrorHandler);
  }

  @AfterEach
  void tearDown() {
    aggregatorInputChannel.unsubscribe(mockAggregatorHandler);
    mqttErrorChannel.unsubscribe(mockErrorHandler);
  }

  @Test
  @DisplayName("Valid JSON should be sent to aggregator input channel")
  void shouldProcessValidMessage() {
    // GIVEN
    var sensorMsg = new SensorMessage("sensor-1", 24.5);
    String jsonPayload = new ObjectMapper().writeValueAsString(sensorMsg);

    // WHEN
    mqttInputChannel.send(new GenericMessage<>(jsonPayload));

    // THEN
    var captor = ArgumentCaptor.forClass(Message.class);
    verify(mockAggregatorHandler).handleMessage(captor.capture());

    var result = captor.getValue().getPayload();
    assertThat(result).isInstanceOf(SensorMessage.class);

    var castedResult = (SensorMessage) result;
    assertThat(castedResult.id()).isEqualTo("sensor-1");
    assertThat(castedResult.temp()).isEqualTo(24.5);

    verify(mockErrorHandler, never()).handleMessage(any());
  }

  @Test
  @DisplayName("JSON without id should be ignored")
  void shouldIgnoreMessageWithNullId() {
    // GIVEN
    var sensorMsg = new SensorMessage(null, 36.6);
    String jsonPayload = new ObjectMapper().writeValueAsString(sensorMsg);

    // WHEN
    mqttInputChannel.send(new GenericMessage<>(jsonPayload));

    // THEN
    verify(mockAggregatorHandler, never()).handleMessage(any());
    verify(mockErrorHandler, never()).handleMessage(any());
  }

  @Test
  @DisplayName("Wrong JSON is not processed")
  void shouldFailOnInvalidJson() {
    String invalidJson = "{ bad json";

    assertThatThrownBy(() -> mqttInputChannel.send(new GenericMessage<>(invalidJson)))
        .isInstanceOf(MessageTransformationException.class)
        .hasCauseInstanceOf(UncheckedIOException.class);

    verify(mockAggregatorHandler, never()).handleMessage(any());
  }

  @TestConfiguration
  @EnableIntegration
  static class TestConfig {
    @Bean
    public SubscribableChannel aggregatorInputChannel() {
      return new DirectChannel();
    }

    @Bean
    public SubscribableChannel mqttInputChannel() {
      return new DirectChannel();
    }

    @Bean
    public SubscribableChannel mqttErrorChannel() {
      return new DirectChannel();
    }
  }
}
