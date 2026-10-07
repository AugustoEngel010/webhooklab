package dev.webhooklab.application.port.in;

import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.Event;
import java.util.Optional;
import java.util.UUID;

public interface GetEventUseCase {
  Optional<EventHistory> get(UUID id);

  record EventHistory(Event event, DeliveryAttempt attempt) {}
}
