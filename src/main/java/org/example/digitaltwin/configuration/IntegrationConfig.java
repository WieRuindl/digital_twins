package org.example.digitaltwin.configuration;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

import java.util.concurrent.Executors;

@Configuration
@EnableIntegration
public class IntegrationConfig {

    @Bean
    public MessageChannel inputChannel() {
        return new ExecutorChannel(Executors.newCachedThreadPool());
    }

    @Bean
    public MessageChannel outputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttErrorChannel() {
        return new DirectChannel();
    }

}
