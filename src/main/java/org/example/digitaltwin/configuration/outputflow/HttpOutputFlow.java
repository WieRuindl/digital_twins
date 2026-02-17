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
public class HttpOutputFlow {

  private static final String HTTP_API_PATH = "http://localhost:8080/api/v1/status";

  @Bean
  public IntegrationFlow httpOutputFlowProcessor(
      MessageChannel aggregatorOutputChannel, Advice expressionAdvice) {
    return IntegrationFlow.from(aggregatorOutputChannel)
        .handle(
            Http.outboundGateway(HTTP_API_PATH)
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
