package org.example.digitaltwin.configuration.inputflow;

import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
@Slf4j
public class MqttInputFlowConfiguration {

  private static final String STOP_SIGNAL = ">>> RECEIVED MQTT STOP SIGNAL (Empty JSON).";

  @Bean
  public IntegrationFlow mqttInputFlow(
      @NonNull final MessageChannel aggregatorInputChannel,
      @NonNull final MqttPahoMessageDrivenChannelAdapter mqttAdapter) {
    return IntegrationFlow.from(mqttAdapter)
        .transform(Transformers.fromJson(SensorMessage.class))
        .<SensorMessage, Boolean>route(
            msg -> msg.id() == null,
            mapping ->
                mapping
                    .subFlowMapping(true, sf -> sf.handle(m -> log.info(STOP_SIGNAL)))
                    .subFlowMapping(
                        false,
                        sf ->
                            sf.log(
                                    LoggingHandler.Level.INFO,
                                    "MQTT_FLOW",
                                    m -> "Sending to aggregator: " + m.getPayload())
                                .channel(aggregatorInputChannel)))
        .get();
  }

  @Bean
  public MqttPahoMessageDrivenChannelAdapter mqttAdapter(MessageChannel mqttErrorChannel) {
    String brokerUrl = "tcp://localhost:1883";
    String clientId = "spring-client-" + UUID.randomUUID();

    var adapter =
        new MqttPahoMessageDrivenChannelAdapter(brokerUrl, clientId, "sensor/temperature");
    adapter.setCompletionTimeout(20_000);
    adapter.setConverter(new DefaultPahoMessageConverter());
    adapter.setQos(1);

    adapter.setErrorChannel(mqttErrorChannel);

    return adapter;
  }
}
