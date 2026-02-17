package org.example.digitaltwin.configuration.inputflow;

import org.example.digitaltwin.configuration.inputflow.HttpInputFlowTest.TestConfig;
import org.example.digitaltwin.dto.request.ConditionerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({HttpInputFlow.class, TestConfig.class})
class HttpInputFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscribableChannel aggregatorInputChannel;

    private MessageHandler mockHandler;

    @BeforeEach
    void setUp() {
        mockHandler = mock(MessageHandler.class);
        aggregatorInputChannel.subscribe(mockHandler);
    }

    @Test
    @DisplayName("Message with last=false goes to inputChannel")
    void shouldRouteToAggregatorInputChannel_WhenNotLast() throws Exception {
        // GIVEN
        var inputMessage = givenMessage(false);

        // WHEN
        mockMvc.perform(post("/api/v1/services/air-conditioner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inputMessage))
                .andExpect(status().isOk());

        // THEN
        var messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockHandler).handleMessage(messageCaptor.capture());
        var captured = (ConditionerCommand) messageCaptor.getValue().getPayload();
        assertThat(captured.uid()).isEqualTo("uid-123-456");
    }

    @Test
    @DisplayName("Message with last=true doesn't go to inputChannel")
    void shouldIgnore_WhenLastIsTrue() throws Exception {
        // GIVEN
        var inputMessage = givenMessage(true);

        // WHEN
        mockMvc.perform(post("/api/v1/services/air-conditioner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inputMessage))
                .andExpect(status().isOk());

        // THEN
        verify(mockHandler, never()).handleMessage(any());
    }

    private String givenMessage(boolean isLast) {
        var command = new ConditionerCommand("uid-123-456", 0.0, isLast);
        return objectMapper.writeValueAsString(command);
    }

    @TestConfiguration
    @EnableIntegration
    static class TestConfig {
        @Bean
        public SubscribableChannel aggregatorInputChannel() {
            return new DirectChannel();
        }
    }
}