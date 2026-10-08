package dev.webhooklab.adapters.out.persistence;

import dev.webhooklab.application.usecase.EventService.EventHistory;
import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.DeliveryAttemptStatus;
import dev.webhooklab.domain.Event;
import dev.webhooklab.domain.EventStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

final class JdbcEventMapper implements RowMapper<EventHistory> {
  @Override
  public EventHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
    Event event = mapEvent(rs);
    return new EventHistory(event, mapAttempt(rs, event.id()));
  }

  static Event mapEvent(ResultSet rs) throws SQLException {
    return new Event(
        rs.getObject("id", UUID.class),
        rs.getString("event_type"),
        rs.getString("payload"),
        EventStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at").toInstant());
  }

  private static DeliveryAttempt mapAttempt(ResultSet rs, UUID eventId) throws SQLException {
    if (rs.getObject("attempt_id") == null) return null;
    var completedAt = rs.getTimestamp("completed_at");
    return new DeliveryAttempt(
        rs.getObject("attempt_id", UUID.class),
        eventId,
        rs.getTimestamp("started_at").toInstant(),
        completedAt == null ? null : completedAt.toInstant(),
        rs.getObject("duration_ms", Long.class),
        DeliveryAttemptStatus.valueOf(rs.getString("outcome")),
        (Integer) rs.getObject("http_status"));
  }
}
