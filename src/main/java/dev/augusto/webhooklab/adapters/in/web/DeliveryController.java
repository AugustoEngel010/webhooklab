package dev.augusto.webhooklab.adapters.in.web;

import dev.augusto.webhooklab.application.port.in.DeliverEventUseCase;
import dev.augusto.webhooklab.application.port.out.DeliveryAttemptRepository.DeliveryClaim;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/deliver")
public class DeliveryController {
    private final DeliverEventUseCase deliver;

    public DeliveryController(DeliverEventUseCase deliver) { this.deliver = deliver; }

    @PostMapping
    public ResponseEntity<DeliveryResponse> deliver(@PathVariable String eventId) {
        UUID id;
        try { id = UUID.fromString(eventId); } catch (IllegalArgumentException e) { throw new MalformedUuidException(); }
        DeliveryClaim claim = deliver.deliver(id);
        return ResponseEntity.created(URI.create("/events/" + id + "/deliver/" + claim.attempt().id()))
                .body(DeliveryResponse.from(claim));
    }

    record DeliveryResponse(UUID id, UUID eventId, String startedAt, String completedAt, Long durationMs,
                            String outcome, Integer httpStatus) {
        static DeliveryResponse from(DeliveryClaim claim) {
            var attempt = claim.attempt();
            return new DeliveryResponse(attempt.id(), attempt.eventId(), attempt.startedAt().toString(),
                    attempt.completedAt() == null ? null : attempt.completedAt().toString(), attempt.durationMs(),
                    attempt.outcome().name(), attempt.httpStatus());
        }
    }
}
