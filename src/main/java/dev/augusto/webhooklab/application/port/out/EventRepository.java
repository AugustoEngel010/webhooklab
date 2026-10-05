package dev.augusto.webhooklab.application.port.out;

import dev.augusto.webhooklab.domain.Event;
import java.util.Optional;
import java.util.UUID;
import dev.augusto.webhooklab.application.port.in.EventPage;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(UUID id);
    EventPage findPage(int page, int size);
}
