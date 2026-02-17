package org.example.digitaltwin.configuration.errorflow;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
public class MqttErrorFlow {

  public static final String INVALID_JSON_MESSAGE = ">>> MQTT Payload error (invalid JSON ignored)";

  @Bean
  public IntegrationFlow mqttErrorFlowProcessor(@NonNull final MessageChannel mqttErrorChannel) {
    return IntegrationFlow.from(mqttErrorChannel).handle(m -> log.info(INVALID_JSON_MESSAGE)).get();
  }
}
