package dev.webhooklab.adapters.in.web;

import dev.webhooklab.application.port.in.DeliverEventUseCase;
import dev.webhooklab.application.port.out.DeliveryAttemptRepository.DeliveryClaim;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events/{eventId}/deliver")
@Tag(name = "Delivery", description = "Entrega manual e única por evento")
public class DeliveryController {
  private final DeliverEventUseCase deliver;

  public DeliveryController(DeliverEventUseCase deliver) {
    this.deliver = deliver;
  }

  @PostMapping
  @Operation(
      summary = "Entrega um evento",
      description =
          "Executa a única tentativa síncrona. HTTP 201 confirma a tentativa registrada, inclusive com outcome HTTP_ERROR, TIMEOUT ou CONNECTION_ERROR.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Tentativa registrada; confira outcome e o estado do evento"),
    @ApiResponse(responseCode = "400", description = "UUID inválido"),
    @ApiResponse(responseCode = "404", description = "Evento não encontrado"),
    @ApiResponse(responseCode = "409", description = "Evento já teve uma tentativa")
  })
  public ResponseEntity<DeliveryResponse> deliver(@PathVariable String eventId) {
    UUID id;
    try {
      id = UUID.fromString(eventId);
    } catch (IllegalArgumentException e) {
      throw new MalformedUuidException();
    }
    DeliveryClaim claim = deliver.deliver(id);
    return ResponseEntity.created(URI.create("/events/" + id + "/deliver/" + claim.attempt().id()))
        .body(DeliveryResponse.from(claim));
  }

  record DeliveryResponse(
      UUID id,
      UUID eventId,
      String startedAt,
      String completedAt,
      Long durationMs,
      String outcome,
      Integer httpStatus) {
    static DeliveryResponse from(DeliveryClaim claim) {
      var attempt = claim.attempt();
      return new DeliveryResponse(
          attempt.id(),
          attempt.eventId(),
          attempt.startedAt().toString(),
          attempt.completedAt() == null ? null : attempt.completedAt().toString(),
          attempt.durationMs(),
          attempt.outcome().name(),
          attempt.httpStatus());
    }
  }
}
