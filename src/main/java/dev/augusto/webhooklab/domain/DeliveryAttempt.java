package dev.augusto.webhooklab.domain;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttempt(UUID id, UUID eventId, Instant startedAt, Instant completedAt,
                              Long durationMs, DeliveryAttemptStatus outcome, Integer httpStatus) {
    public static DeliveryAttempt started(UUID eventId, Instant startedAt) {
        return new DeliveryAttempt(UUID.randomUUID(), eventId, startedAt, null, null,
                DeliveryAttemptStatus.STARTED, null);
    }
}
