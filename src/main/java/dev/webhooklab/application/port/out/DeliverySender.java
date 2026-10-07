package dev.webhooklab.application.port.out;

import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.DeliveryAttemptStatus;
import dev.webhooklab.domain.Event;

public interface DeliverySender {
  DeliveryResult send(Event event, DeliveryAttempt attempt);

  record DeliveryResult(DeliveryAttemptStatus outcome, Integer httpStatus) {}
}
