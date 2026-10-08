package dev.webhooklab.adapters.out.http;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record WebhookEnvelope(UUID id, String eventType, Instant createdAt, JsonNode payload) {}
