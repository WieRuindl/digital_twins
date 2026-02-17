package org.example.digitaltwin.processingflow.channelsconfiguration;

import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

/**
 * Central configuration for Spring Integration message channels. Defines the "pipes" that connect
 * independent logic flows (Input, Aggregation, Output), determining whether message passing is
 * synchronous or asynchronous.
 */
@Configuration
@EnableIntegration
public class IntegrationChannelsConfiguration {

  /** Asynchronous channel acting as a buffer before the Aggregator. */
  @Bean
  public MessageChannel aggregatorInputChannel() {
    return new ExecutorChannel(Executors.newCachedThreadPool());
  }

  /** Channel carrying processed results to the HTTP Output Flow. */
  @Bean
  public MessageChannel aggregatorOutputChannel() {
    return new DirectChannel();
  }

  /** Channel receiving raw messages from the MQTT adapter. */
  @Bean
  public MessageChannel mqttInputChannel() {
    return new DirectChannel();
  }

  /** Channel for handling MQTT connection or processing errors. */
  @Bean
  public MessageChannel mqttErrorChannel() {
    return new DirectChannel();
  }
}
