package dev.webhooklab.adapters.out.persistence;

final class JdbcDeliverySql {
  static final String CLAIM_PENDING_EVENT =
      "UPDATE events SET status = 'SENDING' WHERE id = ? AND status = 'PENDING'";
  static final String EVENT_EXISTS = "SELECT EXISTS (SELECT 1 FROM events WHERE id = ?)";
  static final String INSERT_STARTED_ATTEMPT =
      "INSERT INTO delivery_attempts (id, event_id, started_at, outcome) VALUES (?, ?, ?, ?)";
  static final String FIND_EVENT =
      "SELECT id, event_type, payload::text, status, created_at FROM events WHERE id = ?";
  static final String COMPLETE_EVENT =
      "UPDATE events SET status = ? WHERE id = ? AND status = 'SENDING'";
  static final String COMPLETE_ATTEMPT =
      "UPDATE delivery_attempts SET completed_at = ?, duration_ms = ?, outcome = ?, http_status = ? "
          + "WHERE id = ? AND event_id = ? AND outcome = 'STARTED'";

  private JdbcDeliverySql() {}
}
