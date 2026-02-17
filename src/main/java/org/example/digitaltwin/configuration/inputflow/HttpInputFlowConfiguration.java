package org.example.digitaltwin.configuration.inputflow;

import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;

@Configuration
public class HttpInputFlowConfiguration {

  private static final String STOP_SIGNAL = ">>> RECEIVED STOP SIGNAL (Last: true)";
  private static final String HTTP_API_PATH = "/api/v1/services/air-conditioner";
  private static final String LOG_CATEGORY = "HTTP_FLOW";
  public static final String SENDING_TO_AGGREGATOR = "Sending to aggregator: ";

  @Bean
  public IntegrationFlow httpInputFlow(MessageChannel inputChannel) {
    return IntegrationFlow.from(
            Http.inboundChannelAdapter(HTTP_API_PATH)
                .requestMapping(m -> m.methods(HttpMethod.POST))
                .requestPayloadType(ConditionerCommand.class))
        .<ConditionerCommand, Boolean>route(
            this::isLastCommand,
            mapping ->
                mapping
                    // ignore message if condition is true
                    .subFlowMapping(
                        true,
                        sf ->
                            sf.log(LoggingHandler.Level.INFO, LOG_CATEGORY, m -> STOP_SIGNAL)
                                .nullChannel())

                    // process message if condition is false
                    .subFlowMapping(
                        false,
                        sf ->
                            sf.log(
                                    LoggingHandler.Level.INFO,
                                    LOG_CATEGORY,
                                    m -> SENDING_TO_AGGREGATOR + m.getPayload())
                                .channel(inputChannel)))
        .get();
  }

  private boolean isLastCommand(ConditionerCommand cmd) {
    return Boolean.TRUE.equals(cmd.last());
  }
}
