package dev.augusto.webhooklab.application.port.in;

import dev.augusto.webhooklab.domain.Event;

public interface RegisterEventUseCase {
    Event register(String eventType, String payloadJson);
}
