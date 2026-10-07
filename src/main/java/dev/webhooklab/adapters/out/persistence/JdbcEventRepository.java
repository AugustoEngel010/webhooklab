package dev.webhooklab.adapters.out.persistence;

import dev.webhooklab.application.port.in.EventPage;
import dev.webhooklab.application.port.in.GetEventUseCase.EventHistory;
import dev.webhooklab.application.port.out.EventRepository;
import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.DeliveryAttemptStatus;
import dev.webhooklab.domain.Event;
import dev.webhooklab.domain.EventStatus;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcEventRepository implements EventRepository {
  private final JdbcTemplate jdbc;

  public JdbcEventRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Event save(Event event) {
    jdbc.update(
        "INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, ?, ?::jsonb, ?, ?)",
        event.id(),
        event.eventType(),
        event.payloadJson(),
        event.status().name(),
        Timestamp.from(event.createdAt()));
    return event;
  }

  @Override
  public Optional<EventHistory> findById(UUID id) {
    return jdbc
        .query(
            "SELECT e.id, e.event_type, e.payload::text, e.status, e.created_at, "
                + "a.id AS attempt_id, a.started_at, a.completed_at, a.duration_ms, a.outcome, a.http_status "
                + "FROM events e LEFT JOIN delivery_attempts a ON a.event_id = e.id WHERE e.id = ?",
            (rs, rowNum) -> {
              Event event =
                  new Event(
                      rs.getObject("id", UUID.class),
                      rs.getString("event_type"),
                      rs.getString("payload"),
                      EventStatus.valueOf(rs.getString("status")),
                      rs.getTimestamp("created_at").toInstant());
              DeliveryAttempt attempt =
                  rs.getObject("attempt_id") == null
                      ? null
                      : new DeliveryAttempt(
                          rs.getObject("attempt_id", UUID.class),
                          event.id(),
                          rs.getTimestamp("started_at").toInstant(),
                          rs.getTimestamp("completed_at") == null
                              ? null
                              : rs.getTimestamp("completed_at").toInstant(),
                          rs.getObject("duration_ms", Long.class),
                          DeliveryAttemptStatus.valueOf(rs.getString("outcome")),
                          (Integer) rs.getObject("http_status"));
              return new EventHistory(event, attempt);
            },
            id)
        .stream()
        .findFirst();
  }

  @Override
  public EventPage findPage(int page, int size) {
    long total = jdbc.queryForObject("SELECT count(*) FROM events", Long.class);
    long offset = (long) page * size;
    var history =
        jdbc.query(
            "SELECT e.id, e.event_type, e.payload::text, e.status, e.created_at, "
                + "a.id AS attempt_id, a.started_at, a.completed_at, a.duration_ms, a.outcome, a.http_status "
                + "FROM events e LEFT JOIN delivery_attempts a ON a.event_id = e.id "
                + "ORDER BY e.created_at DESC, e.id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> {
              Event event =
                  new Event(
                      rs.getObject("id", UUID.class),
                      rs.getString("event_type"),
                      rs.getString("payload"),
                      EventStatus.valueOf(rs.getString("status")),
                      rs.getTimestamp("created_at").toInstant());
              DeliveryAttempt attempt =
                  rs.getObject("attempt_id") == null
                      ? null
                      : new DeliveryAttempt(
                          rs.getObject("attempt_id", UUID.class),
                          event.id(),
                          rs.getTimestamp("started_at").toInstant(),
                          rs.getTimestamp("completed_at") == null
                              ? null
                              : rs.getTimestamp("completed_at").toInstant(),
                          rs.getObject("duration_ms", Long.class),
                          DeliveryAttemptStatus.valueOf(rs.getString("outcome")),
                          (Integer) rs.getObject("http_status"));
              return new EventHistory(event, attempt);
            },
            size,
            offset);
    return new EventPage(history, page, size, total);
  }
}
