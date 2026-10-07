package dev.augusto.webhooklab.application.port.in;

import dev.augusto.webhooklab.application.port.in.GetEventUseCase.EventHistory;
import java.util.List;

public record EventPage(List<EventHistory> content, int page, int size, long totalElements) {
    public int totalPages() {
        return totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }
}
