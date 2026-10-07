package dev.webhooklab.application.usecase;

import dev.webhooklab.application.port.in.DeliverEventUseCase;
import dev.webhooklab.application.port.out.DeliveryAttemptRepository;
import dev.webhooklab.application.port.out.DeliveryAttemptRepository.DeliveryClaim;
import dev.webhooklab.application.port.out.DeliverySender;
import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.DeliveryAttemptStatus;
import dev.webhooklab.domain.EventStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class DeliverEventService implements DeliverEventUseCase {
  private final DeliveryAttemptRepository attempts;
  private final DeliverySender sender;
  private final Clock clock;

  public DeliverEventService(
      DeliveryAttemptRepository attempts, DeliverySender sender, Clock clock) {
    this.attempts = attempts;
    this.sender = sender;
    this.clock = clock;
  }

  @Override
  public DeliveryClaim deliver(UUID eventId) {
    DeliveryClaim claim = attempts.start(eventId, clock.instant());
    long startedNanos = System.nanoTime();
    DeliverySender.DeliveryResult result = sender.send(claim.event(), claim.attempt());
    Instant completedAt = clock.instant();
    long durationMs = Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    DeliveryAttempt completed =
        new DeliveryAttempt(
            claim.attempt().id(),
            claim.attempt().eventId(),
            claim.attempt().startedAt(),
            completedAt,
            durationMs,
            result.outcome(),
            result.httpStatus());
    EventStatus eventStatus =
        result.outcome() == DeliveryAttemptStatus.SUCCEEDED
            ? EventStatus.DELIVERED
            : EventStatus.FAILED;
    return attempts.complete(claim, eventStatus, completed);
  }
}
