package dev.augusto.webhooklab.application.usecase;

import dev.augusto.webhooklab.application.port.in.RegisterEventUseCase;
import dev.augusto.webhooklab.application.port.out.EventRepository;
import dev.augusto.webhooklab.application.port.out.PayloadValidator;
import dev.augusto.webhooklab.domain.Event;
import java.time.Clock;

public final class RegisterEventService implements RegisterEventUseCase {
    private final EventRepository repository;
    private final PayloadValidator payloadValidator;
    private final Clock clock;

    public RegisterEventService(EventRepository repository, PayloadValidator payloadValidator, Clock clock) {
        this.repository = repository;
        this.payloadValidator = payloadValidator;
        this.clock = clock;
    }

    @Override
    public Event register(String eventType, String payloadJson) {
        if (!payloadValidator.isJsonObject(payloadJson)) {
            throw new IllegalArgumentException("payload must be a JSON object");
        }
        return repository.save(Event.pending(eventType, payloadJson, clock.instant()));
    }
}
