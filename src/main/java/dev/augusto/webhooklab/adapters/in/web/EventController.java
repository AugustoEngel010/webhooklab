package dev.augusto.webhooklab.adapters.in.web;

import dev.augusto.webhooklab.application.port.in.GetEventUseCase;
import dev.augusto.webhooklab.application.port.in.RegisterEventUseCase;
import dev.augusto.webhooklab.application.port.in.ListEventsUseCase;
import dev.augusto.webhooklab.domain.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {
    private final RegisterEventUseCase register;
    private final GetEventUseCase get;
    private final ListEventsUseCase list;
    private final ObjectMapper mapper;

    public EventController(RegisterEventUseCase register, GetEventUseCase get, ListEventsUseCase list, ObjectMapper mapper) {
        this.register = register; this.get = get; this.list = list; this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@RequestBody EventRequest request) {
        if (request == null || request.eventType() == null || request.payload() == null || !request.payload().isObject()) {
            throw new IllegalArgumentException("eventType and an object payload are required");
        }
        Event event = register.register(request.eventType(), mapper.writeValueAsString(request.payload()));
        return ResponseEntity.created(URI.create("/events/" + event.id())).body(EventResponse.from(event, request.payload()));
    }

    @GetMapping
    public EventPageResponse list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return EventPageResponse.from(list.list(page, size), mapper);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable String id) {
        UUID uuid;
        try { uuid = UUID.fromString(id); } catch (IllegalArgumentException e) { throw new MalformedUuidException(); }
        Event event = get.get(uuid).orElseThrow(NotFoundException::new);
        JsonNode payload = mapper.readTree(event.payloadJson());
        return EventResponse.from(event, payload);
    }
}
