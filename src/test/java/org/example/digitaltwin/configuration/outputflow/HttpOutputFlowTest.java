package org.example.digitaltwin.configuration.outputflow;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.example.digitaltwin.dto.response.StatusMessage;
import org.example.digitaltwin.dto.response.StatusMessage.CorrelationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {HttpOutputFlow.class, HttpOutputFlowTest.TestConfig.class})
class HttpOutputFlowTest {

  @Autowired private SubscribableChannel aggregatorOutputChannel;

  @Autowired private RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void init() {
    mockServer = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  @DisplayName("Success: POST request with correct JSON")
  void shouldSendPostRequest() {
    // GIVEN
    var statusMsg = new StatusMessage("uuid-1", CorrelationStatus.MATCH);
    var expectedJson = new ObjectMapper().writeValueAsString(statusMsg);

    // WHEN
    mockServer
        .expect(requestTo("http://localhost:8080/api/v1/status"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(expectedJson))
        .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

    // THEN
    aggregatorOutputChannel.send(new GenericMessage<>(statusMsg));
    mockServer.verify();
  }

  @Test
  @DisplayName("Server Error: Advice catches the exception, the flow doesn't fail")
  void shouldTrapExceptionOnServerError() {
    // GIVEN
    var statusMsg = new StatusMessage("uuid-error", CorrelationStatus.LOST);

    // WHEN
    mockServer
        .expect(requestTo("http://localhost:8080/api/v1/status"))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    // THEN
    assertThatCode(() -> aggregatorOutputChannel.send(new GenericMessage<>(statusMsg)))
        .doesNotThrowAnyException();
    mockServer.verify();
  }

  @TestConfiguration
  @EnableIntegration
  static class TestConfig {
    @Bean
    public SubscribableChannel aggregatorOutputChannel() {
      return new DirectChannel();
    }
  }
}
