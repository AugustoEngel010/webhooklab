package dev.augusto.webhooklab.config;

import dev.augusto.webhooklab.application.port.in.GetEventUseCase;
import dev.augusto.webhooklab.application.port.in.RegisterEventUseCase;
import dev.augusto.webhooklab.application.port.in.ListEventsUseCase;
import dev.augusto.webhooklab.application.port.out.EventRepository;
import dev.augusto.webhooklab.application.port.out.PayloadValidator;
import dev.augusto.webhooklab.application.usecase.GetEventService;
import dev.augusto.webhooklab.application.usecase.RegisterEventService;
import dev.augusto.webhooklab.application.usecase.ListEventsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PayloadValidator payloadValidator(ObjectMapper mapper) {
        return json -> { try { return json != null && mapper.readTree(json).isObject(); } catch (RuntimeException e) { return false; } };
    }
    @Bean RegisterEventUseCase registerEventUseCase(EventRepository repo, PayloadValidator validator, Clock clock) {
        return new RegisterEventService(repo, validator, clock);
    }
    @Bean GetEventUseCase getEventUseCase(EventRepository repo) { return new GetEventService(repo); }
    @Bean ListEventsUseCase listEventsUseCase(EventRepository repo) { return new ListEventsService(repo); }
}
