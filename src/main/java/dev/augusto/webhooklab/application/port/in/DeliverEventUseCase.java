package dev.augusto.webhooklab.application.port.in;

import dev.augusto.webhooklab.application.port.out.DeliveryAttemptRepository.DeliveryClaim;
import java.util.UUID;

public interface DeliverEventUseCase {
    DeliveryClaim deliver(UUID eventId);
}
