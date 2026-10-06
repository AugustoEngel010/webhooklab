package dev.augusto.webhooklab;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.augusto.webhooklab.adapters.out.http.HttpDeliverySender;
import dev.augusto.webhooklab.application.port.out.DeliverySender;
import dev.augusto.webhooklab.domain.DeliveryAttempt;
import dev.augusto.webhooklab.domain.DeliveryAttemptStatus;
import dev.augusto.webhooklab.domain.Event;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryHttpIT {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());
    static { WIREMOCK.start(); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("delivery.target-url", () -> WIREMOCK.baseUrl() + "/webhook");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    final JsonMapper mapper = new JsonMapper();

    @BeforeEach void clean() {
        jdbc.update("DELETE FROM delivery_attempts");
        jdbc.update("DELETE FROM events");
        WIREMOCK.resetAll();
    }

    @AfterAll static void stopWireMock() { WIREMOCK.stop(); }

    @Test
    void sendsEnvelopeAndHeaderAndPersistsSuccess() throws Exception {
        WIREMOCK.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(204)));
        UUID id = createEvent();

        mockMvc.perform(post("/events/{id}/deliver", id))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.outcome").value("SUCCEEDED"))
                .andExpect(jsonPath("$.httpStatus").value(204)).andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.durationMs").isNumber());
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("X-Webhook-Event-Id", equalTo(id.toString()))
                .withRequestBody(matchingJsonPath("$.id", equalTo(id.toString())))
                .withRequestBody(matchingJsonPath("$.eventType", equalTo("ORDER_CREATED")))
                .withRequestBody(matchingJsonPath("$.payload.orderId", equalTo("A-1"))));
        assertEquals("DELIVERED", jdbc.queryForObject("SELECT status FROM events WHERE id = ?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM delivery_attempts WHERE event_id = ? AND completed_at IS NOT NULL AND duration_ms >= 0", Integer.class, id));
    }

    @Test
    void persistsHttpErrorAndDoesNotFollowRedirect() throws Exception {
        WIREMOCK.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(302).withHeader("Location", "/other")));
        UUID id = createEvent();
        mockMvc.perform(post("/events/{id}/deliver", id)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("HTTP_ERROR"))
                .andExpect(jsonPath("$.httpStatus").value(302));
        assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM events WHERE id = ?", String.class, id));
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    void persistsTimeoutAndRejectsSecondDeliveryWithoutSendingAgain() throws Exception {
        WIREMOCK.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/webhook")).willReturn(aResponse().withFixedDelay(4000).withStatus(200)));
        UUID id = createEvent();
        mockMvc.perform(post("/events/{id}/deliver", id)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("TIMEOUT"))
                .andExpect(jsonPath("$.httpStatus").doesNotExist());
        mockMvc.perform(post("/events/{id}/deliver", id)).andExpect(status().isConflict());
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    void classifiesConnectionFailureWithoutHttpStatus() {
        HttpDeliverySender sender = new HttpDeliverySender(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                mapper, "http://127.0.0.1:1/unavailable");
        Event event = new Event(UUID.randomUUID(), "TEST", "{}", dev.augusto.webhooklab.domain.EventStatus.SENDING, Instant.now());
        DeliverySender.DeliveryResult result = sender.send(event, DeliveryAttempt.started(event.id(), Instant.now()));
        assertEquals(DeliveryAttemptStatus.CONNECTION_ERROR, result.outcome());
        assertNull(result.httpStatus());
    }

    private UUID createEvent() throws Exception {
        String body = mockMvc.perform(post("/events").contentType("application/json")
                        .content("{\"eventType\":\"ORDER_CREATED\",\"payload\":{\"orderId\":\"A-1\"}}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(body).get("id").asText());
    }
}
