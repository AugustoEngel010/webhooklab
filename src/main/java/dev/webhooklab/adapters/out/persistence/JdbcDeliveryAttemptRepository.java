package dev.webhooklab.adapters.out.persistence;

import dev.webhooklab.application.port.out.DeliveryAttemptRepository;
import dev.webhooklab.application.usecase.DeliveryConflictException;
import dev.webhooklab.application.usecase.EventNotFoundException;
import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.DeliveryAttemptStatus;
import dev.webhooklab.domain.Event;
import dev.webhooklab.domain.EventStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class JdbcDeliveryAttemptRepository implements DeliveryAttemptRepository {
  private final JdbcTemplate jdbc;

  public JdbcDeliveryAttemptRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public DeliveryClaim claimForDelivery(UUID eventId, Instant startedAt) {
    int updated = jdbc.update(JdbcDeliverySql.CLAIM_PENDING_EVENT, eventId);
    if (updated == 0) {
      boolean exists =
          Boolean.TRUE.equals(
              jdbc.queryForObject(JdbcDeliverySql.EVENT_EXISTS, Boolean.class, eventId));
      if (!exists) throw new EventNotFoundException();
      throw new DeliveryConflictException();
    }

    DeliveryAttempt attempt = DeliveryAttempt.started(eventId, startedAt);
    jdbc.update(
        JdbcDeliverySql.INSERT_STARTED_ATTEMPT,
        attempt.id(),
        eventId,
        Timestamp.from(startedAt),
        DeliveryAttemptStatus.STARTED.name());
    Event event =
        jdbc.queryForObject(
            JdbcDeliverySql.FIND_EVENT, (rs, rowNum) -> JdbcEventMapper.mapEvent(rs), eventId);
    return new DeliveryClaim(event, attempt);
  }

  @Override
  @Transactional
  public DeliveryClaim complete(
      DeliveryClaim claim, EventStatus eventStatus, DeliveryAttempt attempt) {
    int eventRows =
        jdbc.update(JdbcDeliverySql.COMPLETE_EVENT, eventStatus.name(), claim.event().id());
    int attemptRows =
        jdbc.update(
            JdbcDeliverySql.COMPLETE_ATTEMPT,
            Timestamp.from(attempt.completedAt()),
            attempt.durationMs(),
            attempt.outcome().name(),
            attempt.httpStatus(),
            attempt.id(),
            attempt.eventId());
    if (eventRows != 1 || attemptRows != 1)
      throw new IllegalStateException("Delivery completion was not applied");
    return new DeliveryClaim(
        new Event(
            claim.event().id(),
            claim.event().eventType(),
            claim.event().payloadJson(),
            eventStatus,
            claim.event().createdAt()),
        attempt);
  }
}
