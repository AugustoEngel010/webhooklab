package dev.augusto.webhooklab.adapters.out.persistence;

import dev.augusto.webhooklab.application.port.out.EventRepository;
import dev.augusto.webhooklab.domain.Event;
import dev.augusto.webhooklab.domain.EventStatus;
import dev.augusto.webhooklab.application.port.in.EventPage;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;
import java.util.UUID;
import java.sql.Timestamp;

public final class JdbcEventRepository implements EventRepository {
    private final JdbcTemplate jdbc;
    public JdbcEventRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Event save(Event event) {
        jdbc.update("INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, ?, ?::jsonb, ?, ?)",
                event.id(), event.eventType(), event.payloadJson(), event.status().name(), Timestamp.from(event.createdAt()));
        return event;
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return jdbc.query("SELECT id, event_type, payload::text, status, created_at FROM events WHERE id = ?",
                (rs, rowNum) -> new Event(rs.getObject("id", UUID.class), rs.getString("event_type"),
                        rs.getString("payload"), EventStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant()), id).stream().findFirst();
    }

    @Override
    public EventPage findPage(int page, int size) {
        long total = jdbc.queryForObject("SELECT count(*) FROM events", Long.class);
        long offset = (long) page * size;
        var events = jdbc.query("SELECT id, event_type, payload::text, status, created_at " +
                        "FROM events ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new Event(rs.getObject("id", UUID.class), rs.getString("event_type"),
                        rs.getString("payload"), EventStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant()), size, offset);
        return new EventPage(events, page, size, total);
    }
}
