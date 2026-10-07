package dev.webhooklab.application.usecase;

import dev.webhooklab.application.port.in.GetEventUseCase;
import dev.webhooklab.application.port.in.GetEventUseCase.EventHistory;
import dev.webhooklab.application.port.out.EventRepository;
import java.util.Optional;
import java.util.UUID;

public final class GetEventService implements GetEventUseCase {
  private final EventRepository repository;

  public GetEventService(EventRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<EventHistory> get(UUID id) {
    return repository.findById(id);
  }
}
