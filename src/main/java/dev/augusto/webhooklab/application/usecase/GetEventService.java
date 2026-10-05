package dev.augusto.webhooklab.application.usecase;

import dev.augusto.webhooklab.application.port.in.GetEventUseCase;
import dev.augusto.webhooklab.application.port.out.EventRepository;
import dev.augusto.webhooklab.domain.Event;
import java.util.Optional;
import java.util.UUID;

public final class GetEventService implements GetEventUseCase {
    private final EventRepository repository;

    public GetEventService(EventRepository repository) { this.repository = repository; }

    @Override
    public Optional<Event> get(UUID id) { return repository.findById(id); }
}
