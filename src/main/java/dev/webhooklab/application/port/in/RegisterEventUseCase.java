package dev.webhooklab.application.port.in;

import dev.webhooklab.domain.Event;

public interface RegisterEventUseCase {
  Event register(String eventType, String payloadJson);
}
