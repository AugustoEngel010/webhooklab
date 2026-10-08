package dev.webhooklab.adapters.out.persistence;

final class JdbcEventSql {
  static final String INSERT_EVENT =
      "INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, ?, ?::jsonb, ?, ?)";
  static final String EVENT_HISTORY_COLUMNS =
      "e.id, e.event_type, e.payload::text, e.status, e.created_at, "
          + "a.id AS attempt_id, a.started_at, a.completed_at, a.duration_ms, a.outcome, a.http_status";
  static final String FIND_BY_ID =
      "SELECT "
          + EVENT_HISTORY_COLUMNS
          + " FROM events e "
          + "LEFT JOIN delivery_attempts a ON a.event_id = e.id WHERE e.id = ?";
  static final String FIND_PAGE =
      "SELECT "
          + EVENT_HISTORY_COLUMNS
          + " FROM events e "
          + "LEFT JOIN delivery_attempts a ON a.event_id = e.id "
          + "ORDER BY e.created_at DESC, e.id DESC LIMIT ? OFFSET ?";
  static final String COUNT_EVENTS = "SELECT count(*) FROM events";

  private JdbcEventSql() {}
}
