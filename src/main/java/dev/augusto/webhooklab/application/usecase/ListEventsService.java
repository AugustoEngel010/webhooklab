package dev.augusto.webhooklab.application.usecase;

import dev.augusto.webhooklab.application.port.in.ListEventsUseCase;
import dev.augusto.webhooklab.application.port.in.EventPage;
import dev.augusto.webhooklab.application.port.out.EventRepository;

public final class ListEventsService implements ListEventsUseCase {
    private final EventRepository repository;

    public ListEventsService(EventRepository repository) { this.repository = repository; }

    @Override
    public EventPage list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
        }
        return repository.findPage(page, size);
    }
}
