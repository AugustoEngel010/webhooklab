package dev.webhooklab.application.port.in;

public interface ListEventsUseCase {
  EventPage list(int page, int size);
}
