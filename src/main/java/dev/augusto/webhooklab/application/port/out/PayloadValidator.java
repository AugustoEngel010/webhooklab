package dev.augusto.webhooklab.application.port.out;

public interface PayloadValidator {
    boolean isJsonObject(String payloadJson);
}
