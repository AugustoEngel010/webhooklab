package dev.augusto.webhooklab.application.port.in;

import dev.augusto.webhooklab.domain.DeliveryAttempt;
import dev.augusto.webhooklab.domain.Event;
import java.util.UUID;
import java.util.Optional;

public interface GetEventUseCase {
    Optional<EventHistory> get(UUID id);

    record EventHistory(Event event, DeliveryAttempt attempt) { }
}
