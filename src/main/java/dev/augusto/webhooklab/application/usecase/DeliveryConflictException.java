package dev.augusto.webhooklab.application.usecase;

public class DeliveryConflictException extends RuntimeException {
    public DeliveryConflictException() { super("Event has already started delivery"); }
}
