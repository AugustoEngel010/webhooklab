package dev.webhooklab.application.usecase;

import dev.webhooklab.application.port.in.EventPage;
import dev.webhooklab.application.port.out.EventRepository;
import dev.webhooklab.application.port.out.PayloadValidator;
import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.Event;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public final class EventService {
  public record EventHistory(Event event, DeliveryAttempt attempt) {}

  private final EventRepository repository;
  private final PayloadValidator payloadValidator;
  private final Clock clock;

  public EventService(EventRepository repository, PayloadValidator payloadValidator, Clock clock) {
    this.repository = repository;
    this.payloadValidator = payloadValidator;
    this.clock = clock;
  }

  public Event register(String eventType, String payloadJson) {
    if (!payloadValidator.isJsonObject(payloadJson)) {
      throw new IllegalArgumentException("payload must be a JSON object");
    }
    return repository.save(Event.pending(eventType, payloadJson, clock.instant()));
  }

  public Optional<EventHistory> get(UUID id) {
    return repository.findById(id);
  }

  public EventPage list(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
    }
    return repository.findPage(page, size);
  }
}
