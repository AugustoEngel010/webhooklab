package dev.webhooklab.application.usecase;

public final class EventNotFoundException extends RuntimeException {
  public EventNotFoundException() {
    super("Event was not found");
  }
}
