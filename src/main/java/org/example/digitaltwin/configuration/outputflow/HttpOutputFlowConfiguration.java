package org.example.digitaltwin.configuration.outputflow;

import org.aopalliance.aop.Advice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;

@Configuration
public class HttpOutputFlowConfiguration {

    @Bean
    public IntegrationFlow outputFlow(MessageChannel outputChannel,
                                      Advice expressionAdvice) {
        return IntegrationFlow.from(outputChannel)
                .handle(Http.outboundGateway("http://localhost:8080/api/v1/status")
                        .httpMethod(HttpMethod.POST)
                        .expectedResponseType(String.class),
                        e -> e.advice(expressionAdvice))
                .channel("nullChannel")
                .get();
    }

    @Bean
    public Advice expressionAdvice() {
        var advice = new ExpressionEvaluatingRequestHandlerAdvice();
        advice.setOnFailureExpressionString("null");
        advice.setTrapException(true);
        return advice;
    }
}
