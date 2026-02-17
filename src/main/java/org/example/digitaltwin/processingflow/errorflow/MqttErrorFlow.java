package org.example.digitaltwin.processingflow.errorflow;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

/**
 * Handles MQTT processing errors (e.g., malformed JSON). Consumes failed messages to prevent the
 * adapter from crashing.
 */
@Slf4j
@Configuration
public class MqttErrorFlow {

  public static final String INVALID_JSON_MESSAGE = ">>> MQTT Payload error (invalid JSON ignored)";

  /**
   * Defines a flow that listens to the error channel and logs a warning for invalid payloads
   * without stopping the system.
   */
  @Bean
  public IntegrationFlow mqttErrorFlowProcessor(@NonNull final MessageChannel mqttErrorChannel) {
    return IntegrationFlow.from(mqttErrorChannel).handle(m -> log.info(INVALID_JSON_MESSAGE)).get();
  }
}
