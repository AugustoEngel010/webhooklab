package dev.webhooklab.adapters.in.web;

import dev.webhooklab.application.port.in.EventPage;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

public record EventPageResponse(
    List<EventResponse> content, int page, int size, long totalElements, int totalPages) {
  public static EventPageResponse from(EventPage source, ObjectMapper mapper) {
    var content =
        source.content().stream()
            .map(
                history ->
                    EventResponse.from(
                        history.event(),
                        mapper.readTree(history.event().payloadJson()),
                        history.attempt()))
            .toList();
    return new EventPageResponse(
        content, source.page(), source.size(), source.totalElements(), source.totalPages());
  }
}
