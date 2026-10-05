package dev.augusto.webhooklab.domain;

import java.time.Instant;
import java.util.UUID;

public record Event(UUID id, String eventType, String payloadJson, EventStatus status, Instant createdAt) {
    public static Event pending(String eventType, String payloadJson, Instant createdAt) {
        if (eventType == null || eventType.trim().isEmpty() || eventType.trim().length() > 80) {
            throw new IllegalArgumentException("eventType must be non-blank and at most 80 characters");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("payload is required");
        }
        return new Event(UUID.randomUUID(), eventType.trim(), payloadJson, EventStatus.PENDING, createdAt);
    }
}
