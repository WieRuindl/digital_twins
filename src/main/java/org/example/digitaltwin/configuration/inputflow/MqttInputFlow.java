package org.example.digitaltwin.configuration.inputflow;

import java.util.UUID;
import org.example.digitaltwin.dto.request.SensorMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttInputFlow {

  private static final String STOP_SIGNAL = ">>> RECEIVED MQTT STOP SIGNAL (Empty JSON).";
  private static final String LOG_CATEGORY = "MQTT_FLOW";
  private static final String SENDING_TO_AGGREGATOR = "Sending to aggregator: ";

  @Value("${mqtt.url:tcp://localhost:1883}")
  private String mqttUrl;

  @Value("${mqtt.topic:sensor/temperature}")
  private String mqttTopic;

  @Bean
  public IntegrationFlow mqttInputFlowProcessor(
      MessageChannel mqttInputChannel, MessageChannel aggregatorInputChannel) {
    return IntegrationFlow.from(mqttInputChannel)
        .transform(Transformers.fromJson(SensorMessage.class))
        .<SensorMessage, Boolean>route(
            msg -> msg.id() == null,
            mapping ->
                mapping
                    .subFlowMapping(true, ignoreMessage())
                    .subFlowMapping(false, processMessage(aggregatorInputChannel)))
        .get();
  }

  private IntegrationFlow ignoreMessage() {
    return sf -> sf.log(Level.INFO, LOG_CATEGORY, m -> STOP_SIGNAL).nullChannel();
  }

  private IntegrationFlow processMessage(MessageChannel aggregatorInputChannel) {
    return sf ->
        sf.log(Level.INFO, LOG_CATEGORY, m -> SENDING_TO_AGGREGATOR + m.getPayload())
            .channel(aggregatorInputChannel);
  }

  @Bean
  public MqttPahoMessageDrivenChannelAdapter mqttAdapter(
      MessageChannel mqttInputChannel, MessageChannel mqttErrorChannel) {
    String clientId = "spring-client-" + UUID.randomUUID();

    var adapter = new MqttPahoMessageDrivenChannelAdapter(mqttUrl, clientId, mqttTopic);
    adapter.setCompletionTimeout(20_000);
    adapter.setConverter(new DefaultPahoMessageConverter());
    adapter.setQos(1);

    adapter.setOutputChannel(mqttInputChannel);
    adapter.setErrorChannel(mqttErrorChannel);

    return adapter;
  }
}
