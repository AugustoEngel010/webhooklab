package dev.webhooklab.application.port.out;

public interface PayloadValidator {
  boolean isJsonObject(String payloadJson);
}
