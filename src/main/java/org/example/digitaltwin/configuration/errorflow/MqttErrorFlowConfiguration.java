package org.example.digitaltwin.configuration.errorflow;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
public class MqttErrorFlowConfiguration {

  public static final String INVALID_JSON_RECEIVED =
      ">>> MQTT Payload error (invalid JSON ignored). Bad message caught.";

  @Bean
  public IntegrationFlow mqttErrorFlow(@NonNull final MessageChannel mqttErrorChannel) {
    return IntegrationFlow.from(mqttErrorChannel)
        .handle(m -> log.info(INVALID_JSON_RECEIVED))
        .get();
  }
}
