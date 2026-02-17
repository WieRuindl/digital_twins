package org.example.digitaltwin.configuration.inputflow;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
public class HttpInputFlowConfiguration {

    public static final String STOP_SIGNAL = ">>> RECEIVED STOP SIGNAL (Last: true). System stopping...";

    @Bean
    public IntegrationFlow httpInputFlow(@NonNull final MessageChannel inputChannel) {
        return IntegrationFlow.from(Http.inboundChannelAdapter("/api/v1/services/air-conditioner")
                        .requestMapping(m -> m.methods(HttpMethod.POST))
                        .requestPayloadType(ConditionerCommand.class))
                .<ConditionerCommand, Boolean>route(
                        cmd -> Boolean.TRUE.equals(cmd.last()),
                        mapping -> mapping
                                .subFlowMapping(true, sf -> sf.handle(m ->
                                        log.info(STOP_SIGNAL)))
                                .subFlowMapping(false, sf -> sf
                                        .log(LoggingHandler.Level.INFO, "HTTP_FLOW", m -> "Sending to aggregator: " + m.getPayload())
                                        .channel(inputChannel))
                )
                .get();
    }
}
