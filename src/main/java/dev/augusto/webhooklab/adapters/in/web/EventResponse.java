package dev.augusto.webhooklab.adapters.in.web;

import dev.augusto.webhooklab.domain.Event;
import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(UUID id, String eventType, JsonNode payload, String status, Instant createdAt, Object attempt) {
    public static EventResponse from(Event event, JsonNode payload) {
        return new EventResponse(event.id(), event.eventType(), payload, event.status().name(), event.createdAt(), null);
    }
}
