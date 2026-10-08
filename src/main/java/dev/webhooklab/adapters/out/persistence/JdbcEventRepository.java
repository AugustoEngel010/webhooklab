package dev.webhooklab.adapters.out.persistence;

import dev.webhooklab.application.port.in.EventPage;
import dev.webhooklab.application.port.out.EventRepository;
import dev.webhooklab.application.usecase.EventService.EventHistory;
import dev.webhooklab.domain.Event;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcEventRepository implements EventRepository {
  private final JdbcTemplate jdbc;
  private final JdbcEventMapper mapper = new JdbcEventMapper();

  public JdbcEventRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Event save(Event event) {
    jdbc.update(
        JdbcEventSql.INSERT_EVENT,
        event.id(),
        event.eventType(),
        event.payloadJson(),
        event.status().name(),
        Timestamp.from(event.createdAt()));
    return event;
  }

  @Override
  public Optional<EventHistory> findById(UUID id) {
    return jdbc.query(JdbcEventSql.FIND_BY_ID, mapper, id).stream().findFirst();
  }

  @Override
  public EventPage findPage(int page, int size) {
    long total = jdbc.queryForObject(JdbcEventSql.COUNT_EVENTS, Long.class);
    long offset = (long) page * size;
    var history = jdbc.query(JdbcEventSql.FIND_PAGE, mapper, size, offset);
    return new EventPage(history, page, size, total);
  }
}
