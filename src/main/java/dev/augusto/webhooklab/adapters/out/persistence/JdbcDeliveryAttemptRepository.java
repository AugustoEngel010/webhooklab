package dev.augusto.webhooklab.adapters.out.persistence;

import dev.augusto.webhooklab.application.port.out.DeliveryAttemptRepository;
import dev.augusto.webhooklab.application.usecase.DeliveryConflictException;
import dev.augusto.webhooklab.domain.DeliveryAttempt;
import dev.augusto.webhooklab.domain.DeliveryAttemptStatus;
import dev.augusto.webhooklab.domain.Event;
import dev.augusto.webhooklab.domain.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class JdbcDeliveryAttemptRepository implements DeliveryAttemptRepository {
    private final JdbcTemplate jdbc;

    public JdbcDeliveryAttemptRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public DeliveryClaim start(UUID eventId, Instant startedAt) {
        int updated = jdbc.update("UPDATE events SET status = 'SENDING' WHERE id = ? AND status = 'PENDING'", eventId);
        if (updated == 0) {
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM events WHERE id = ?)", Boolean.class, eventId));
            if (!exists) throw new IllegalArgumentException("Event was not found");
            throw new DeliveryConflictException();
        }

        DeliveryAttempt attempt = DeliveryAttempt.started(eventId, startedAt);
        jdbc.update("INSERT INTO delivery_attempts (id, event_id, started_at, outcome) VALUES (?, ?, ?, ?)",
                attempt.id(), eventId, Timestamp.from(startedAt), DeliveryAttemptStatus.STARTED.name());
        Event event = jdbc.queryForObject("SELECT id, event_type, payload::text, status, created_at FROM events WHERE id = ?",
                (rs, rowNum) -> new Event(rs.getObject("id", UUID.class), rs.getString("event_type"),
                        rs.getString("payload"), EventStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant()), eventId);
        return new DeliveryClaim(event, attempt);
    }

    @Override
    @Transactional
    public DeliveryClaim complete(DeliveryClaim claim, EventStatus eventStatus, DeliveryAttempt attempt) {
        int eventRows = jdbc.update("UPDATE events SET status = ? WHERE id = ? AND status = 'SENDING'",
                eventStatus.name(), claim.event().id());
        int attemptRows = jdbc.update("UPDATE delivery_attempts SET completed_at = ?, duration_ms = ?, outcome = ?, http_status = ? " +
                        "WHERE id = ? AND event_id = ? AND outcome = 'STARTED'",
                Timestamp.from(attempt.completedAt()), attempt.durationMs(), attempt.outcome().name(), attempt.httpStatus(),
                attempt.id(), attempt.eventId());
        if (eventRows != 1 || attemptRows != 1) throw new IllegalStateException("Delivery completion was not applied");
        return new DeliveryClaim(new Event(claim.event().id(), claim.event().eventType(), claim.event().payloadJson(),
                eventStatus, claim.event().createdAt()), attempt);
    }
}
