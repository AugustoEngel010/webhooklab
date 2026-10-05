package dev.augusto.webhooklab.adapters.in.web;

import dev.augusto.webhooklab.application.port.in.EventPage;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

public record EventPageResponse(List<EventResponse> content, int page, int size, long totalElements, int totalPages) {
    public static EventPageResponse from(EventPage source, ObjectMapper mapper) {
        var content = source.content().stream().map(event ->
                EventResponse.from(event, mapper.readTree(event.payloadJson()))).toList();
        return new EventPageResponse(content, source.page(), source.size(), source.totalElements(), source.totalPages());
    }
}
