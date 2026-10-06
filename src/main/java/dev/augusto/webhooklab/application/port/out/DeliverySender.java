package dev.augusto.webhooklab.application.port.out;

import dev.augusto.webhooklab.domain.DeliveryAttempt;
import dev.augusto.webhooklab.domain.Event;
import dev.augusto.webhooklab.domain.DeliveryAttemptStatus;

public interface DeliverySender {
    DeliveryResult send(Event event, DeliveryAttempt attempt);

    record DeliveryResult(DeliveryAttemptStatus outcome, Integer httpStatus) { }
}
