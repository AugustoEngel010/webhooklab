package dev.augusto.webhooklab;

import dev.augusto.webhooklab.application.port.in.GetEventUseCase;
import dev.augusto.webhooklab.application.port.out.EventRepository;
import dev.augusto.webhooklab.domain.DeliveryAttemptStatus;
import dev.augusto.webhooklab.domain.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DurableHistoryIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void samePostgresIsReadByASecondApplicationInstance() {
        UUID eventId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Instant created = Instant.parse("2026-10-06T12:00:00Z");
        Instant started = Instant.parse("2026-10-06T12:00:01Z");
        Instant completed = Instant.parse("2026-10-06T12:00:02Z");

        try (ConfigurableApplicationContext first = application()) {
            JdbcTemplate jdbc = first.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, 'ORDER_CREATED', ?::jsonb, 'DELIVERED', ?)",
                    eventId, "{\"orderId\":\"A-1\"}", Timestamp.from(created));
            jdbc.update("INSERT INTO delivery_attempts (id, event_id, started_at, completed_at, duration_ms, outcome, http_status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    attemptId, eventId, Timestamp.from(started), Timestamp.from(completed), 1000L,
                    DeliveryAttemptStatus.SUCCEEDED.name(), 204);
        }

        try (ConfigurableApplicationContext second = application()) {
            var history = second.getBean(GetEventUseCase.class).get(eventId).orElseThrow();
            assertEquals(eventId, history.event().id());
            assertEquals(EventStatus.DELIVERED, history.event().status());
            assertEquals("{\"orderId\": \"A-1\"}", history.event().payloadJson());
            assertNotNull(history.attempt());
            assertEquals(attemptId, history.attempt().id());
            assertEquals(started, history.attempt().startedAt());
            assertEquals(completed, history.attempt().completedAt());
            assertEquals(1000L, history.attempt().durationMs());
            assertEquals(204, history.attempt().httpStatus());
        }
    }

    @Test
    void startedHistoryRemainsStartedAfterApplicationRestart() {
        UUID eventId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Instant started = Instant.parse("2026-10-06T13:00:00Z");

        try (ConfigurableApplicationContext first = application()) {
            JdbcTemplate jdbc = first.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, 'UNKNOWN', '{}'::jsonb, 'SENDING', ?)",
                    eventId, Timestamp.from(started));
            jdbc.update("INSERT INTO delivery_attempts (id, event_id, started_at, outcome) VALUES (?, ?, ?, 'STARTED')",
                    attemptId, eventId, Timestamp.from(started));
        }

        try (ConfigurableApplicationContext second = application()) {
            var history = second.getBean(GetEventUseCase.class).get(eventId).orElseThrow();
            assertEquals(EventStatus.SENDING, history.event().status());
            assertEquals(attemptId, history.attempt().id());
            assertEquals(DeliveryAttemptStatus.STARTED, history.attempt().outcome());
            assertNull(history.attempt().completedAt());
            assertNull(history.attempt().durationMs());
            assertNull(history.attempt().httpStatus());
        }
    }

    private ConfigurableApplicationContext application() {
        return new SpringApplicationBuilder(WebhookLabApplication.class)
                .web(WebApplicationType.NONE)
                .properties("DB_URL=" + POSTGRES.getJdbcUrl(),
                        "DB_USERNAME=" + POSTGRES.getUsername(),
                        "DB_PASSWORD=" + POSTGRES.getPassword(),
                        "DELIVERY_TARGET_URL=http://127.0.0.1:1/unavailable")
                .run();
    }
}
