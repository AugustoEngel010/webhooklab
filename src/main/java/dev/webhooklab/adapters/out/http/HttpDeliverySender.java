package dev.webhooklab.adapters.out.http;

import dev.webhooklab.application.port.out.DeliverySender;
import dev.webhooklab.domain.DeliveryAttempt;
import dev.webhooklab.domain.DeliveryAttemptStatus;
import dev.webhooklab.domain.Event;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class HttpDeliverySender implements DeliverySender {
  private final HttpClient client;
  private final ObjectMapper mapper;
  private final URI target;

  public HttpDeliverySender(
      HttpClient client, ObjectMapper mapper, @Value("${delivery.target-url}") String targetUrl) {
    this.client = client;
    this.mapper = mapper;
    this.target = URI.create(targetUrl);
  }

  @Override
  public DeliveryResult send(Event event, DeliveryAttempt attempt) {
    try {
      ObjectNode envelope = mapper.createObjectNode();
      envelope.put("id", event.id().toString());
      envelope.put("eventType", event.eventType());
      envelope.put("createdAt", event.createdAt().toString());
      envelope.set("payload", mapper.readTree(event.payloadJson()));
      HttpRequest request =
          HttpRequest.newBuilder(target)
              .timeout(Duration.ofSeconds(3))
              .header("Content-Type", "application/json")
              .header("X-Webhook-Event-Id", event.id().toString())
              .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(envelope)))
              .build();
      int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      return status >= 200 && status < 300
          ? new DeliveryResult(DeliveryAttemptStatus.SUCCEEDED, status)
          : new DeliveryResult(DeliveryAttemptStatus.HTTP_ERROR, status);
    } catch (HttpTimeoutException e) {
      return new DeliveryResult(DeliveryAttemptStatus.TIMEOUT, null);
    } catch (ConnectException e) {
      return new DeliveryResult(DeliveryAttemptStatus.CONNECTION_ERROR, null);
    } catch (IOException e) {
      return new DeliveryResult(DeliveryAttemptStatus.CONNECTION_ERROR, null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP delivery was interrupted", e);
    }
  }
}
