package dev.augusto.webhooklab.application.port.out;

import dev.augusto.webhooklab.domain.DeliveryAttempt;
import dev.augusto.webhooklab.domain.Event;
import java.time.Instant;
import java.util.UUID;
import dev.augusto.webhooklab.domain.EventStatus;

public interface DeliveryAttemptRepository {
    DeliveryClaim start(UUID eventId, Instant startedAt);

    DeliveryClaim complete(DeliveryClaim claim, EventStatus eventStatus, DeliveryAttempt attempt);

    record DeliveryClaim(Event event, DeliveryAttempt attempt) { }
}
