package dev.augusto.webhooklab.adapters.in.web;

import tools.jackson.databind.JsonNode;

public record EventRequest(String eventType, JsonNode payload) { }
