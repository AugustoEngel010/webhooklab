package dev.augusto.webhooklab.adapters.in.web;

import dev.augusto.webhooklab.domain.Event;
import dev.augusto.webhooklab.domain.DeliveryAttempt;
import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(UUID id, String eventType, JsonNode payload, String status, Instant createdAt, AttemptResponse attempt) {
    public static EventResponse from(Event event, JsonNode payload) {
        return from(event, payload, null);
    }
    public static EventResponse from(Event event, JsonNode payload, DeliveryAttempt attempt) {
        return new EventResponse(event.id(), event.eventType(), payload, event.status().name(), event.createdAt(),
                attempt == null ? null : AttemptResponse.from(attempt));
    }
    record AttemptResponse(UUID id, Instant startedAt, Instant completedAt, Long durationMs,
                           String outcome, Integer httpStatus) {
        static AttemptResponse from(DeliveryAttempt attempt) {
            return new AttemptResponse(attempt.id(), attempt.startedAt(), attempt.completedAt(), attempt.durationMs(),
                    attempt.outcome().name(), attempt.httpStatus());
        }
    }
}
