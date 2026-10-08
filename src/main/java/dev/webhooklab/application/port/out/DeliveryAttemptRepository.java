package dev.webhooklab.application.port.out;

import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.Event;
import dev.webhooklab.domain.EventStatus;
import java.time.Instant;
import java.util.UUID;

public interface DeliveryAttemptRepository {
  DeliveryClaim claimForDelivery(UUID eventId, Instant startedAt);

  DeliveryClaim complete(DeliveryClaim claim, EventStatus eventStatus, DeliveryAttempt attempt);

  record DeliveryClaim(Event event, DeliveryAttempt attempt) {}
}
