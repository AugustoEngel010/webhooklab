package dev.webhooklab.adapters.in.web;

import dev.webhooklab.application.usecase.EventService;
import dev.webhooklab.domain.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Registro, consulta e paginação de eventos")
public class EventController {
  private final EventService events;
  private final ObjectMapper mapper;

  public EventController(EventService events, ObjectMapper mapper) {
    this.events = events;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(
      summary = "Registra um evento",
      description = "Cria um novo evento PENDING. O payload deve ser um objeto JSON.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Evento registrado"),
    @ApiResponse(responseCode = "400", description = "Entrada inválida")
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              examples =
                  @ExampleObject(
                      value =
                          "{\"eventType\":\"ORDER_CREATED\",\"payload\":{\"orderId\":\"DEMO-001\",\"amount\":150.0}}")))
  public ResponseEntity<EventResponse> create(
      @org.springframework.web.bind.annotation.RequestBody EventRequest request) {
    if (request == null
        || request.eventType() == null
        || request.payload() == null
        || !request.payload().isObject()) {
      throw new IllegalArgumentException("eventType and an object payload are required");
    }
    Event event =
        events.register(request.eventType(), mapper.writeValueAsString(request.payload()));
    return ResponseEntity.created(URI.create("/events/" + event.id()))
        .body(EventResponse.from(event, request.payload()));
  }

  @GetMapping
  @Operation(
      summary = "Lista eventos",
      description = "Retorna eventos em ordem createdAt DESC e id DESC.")
  @ApiResponses(@ApiResponse(responseCode = "200", description = "Página de eventos"))
  public EventPageResponse list(
      @Parameter(description = "Página zero-based", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Itens por página, de 1 a 100", example = "20")
          @RequestParam(defaultValue = "20")
          int size) {
    return EventPageResponse.from(events.list(page, size), mapper);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consulta um evento",
      description = "Inclui a tentativa completa quando a entrega já foi iniciada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Evento encontrado"),
    @ApiResponse(responseCode = "400", description = "UUID inválido"),
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
  })
  public EventResponse get(@PathVariable String id) {
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new MalformedUuidException();
    }
    var history = events.get(uuid).orElseThrow(NotFoundException::new);
    JsonNode payload = mapper.readTree(history.event().payloadJson());
    return EventResponse.from(history.event(), payload, history.attempt());
  }
}
