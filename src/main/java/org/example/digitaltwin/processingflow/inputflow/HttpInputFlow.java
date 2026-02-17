package org.example.digitaltwin.processingflow.inputflow;

import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;

/**
 * Configures the HTTP inbound flow for receiving air conditioner commands. Exposed as a POST
 * endpoint to accept {@link ConditionerCommand} payloads.
 */
@Configuration
public class HttpInputFlow {

  private static final String LOG_CATEGORY = "HTTP_FLOW";
  private static final String STOP_SIGNAL = ">>> RECEIVED STOP SIGNAL (Last: true)";
  private static final String SENDING_TO_AGGREGATOR = "Sending to aggregator: ";

  @Value("${http.input.url:/api/v1/services/air-conditioner}")
  private String httpInputUrl;

  /**
   * Defines the flow that handles incoming HTTP POST requests. Logic: If {@link
   * ConditionerCommand#last()=true}: Logs the stop signal and discards the message. If {@link
   * ConditionerCommand#last()=false}: Logs and forwards the command to the aggregator channel.
   */
  @Bean
  public IntegrationFlow httpInputFlowProcessor(MessageChannel aggregatorInputChannel) {
    return IntegrationFlow.from(
            Http.inboundChannelAdapter(httpInputUrl)
                .requestMapping(m -> m.methods(HttpMethod.POST))
                .requestPayloadType(ConditionerCommand.class))
        .<ConditionerCommand, Boolean>route(
            this::isLastCommand,
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

  private boolean isLastCommand(ConditionerCommand cmd) {
    return Boolean.TRUE.equals(cmd.last());
  }
}
