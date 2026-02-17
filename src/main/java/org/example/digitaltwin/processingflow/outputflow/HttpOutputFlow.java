package org.example.digitaltwin.processingflow.outputflow;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpOutputFlow {

  @Value("${output.http.url:http://localhost:8080/api/v1/status}")
  private String outputHttpUrl;

  @Bean
  public IntegrationFlow httpOutputFlowProcessor(
      MessageChannel aggregatorOutputChannel,
      ExpressionEvaluatingRequestHandlerAdvice httpErrorAdvice,
      @Qualifier("outputHttpRestTemplate") RestTemplate restTemplate) {

    return IntegrationFlow.from(aggregatorOutputChannel)
        .transform(Transformers.toJson())
        .handle(
            Http.outboundGateway(outputHttpUrl, restTemplate)
                .httpMethod(HttpMethod.POST)
                .expectedResponseType(String.class),
            e -> e.advice(httpErrorAdvice))
        .channel("nullChannel")
        .get();
  }

  @Bean
  public ExpressionEvaluatingRequestHandlerAdvice httpErrorAdvice() {
    var advice = new ExpressionEvaluatingRequestHandlerAdvice();
    advice.setOnFailureExpressionString("null");
    advice.setTrapException(true);
    return advice;
  }

  @Bean("outputHttpRestTemplate")
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
