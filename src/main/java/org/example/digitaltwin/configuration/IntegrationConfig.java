package org.example.digitaltwin.configuration;

import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class IntegrationConfig {

  @Bean
  public MessageChannel aggregatorInputChannel() {
    return new ExecutorChannel(Executors.newCachedThreadPool());
  }

  @Bean
  public MessageChannel aggregatorOutputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel mqttErrorChannel() {
    return new DirectChannel();
  }
}
