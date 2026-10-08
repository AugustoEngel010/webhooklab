package dev.webhooklab.application.port.out;

import dev.webhooklab.application.port.in.EventPage;
import dev.webhooklab.application.usecase.EventService.EventHistory;
import dev.webhooklab.domain.Event;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
  Event save(Event event);

  Optional<EventHistory> findById(UUID id);

  EventPage findPage(int page, int size);
}
