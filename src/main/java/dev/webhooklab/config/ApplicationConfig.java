package dev.webhooklab.config;

import dev.webhooklab.adapters.out.http.HttpDeliverySender;
import dev.webhooklab.application.port.in.DeliverEventUseCase;
import dev.webhooklab.application.port.in.GetEventUseCase;
import dev.webhooklab.application.port.in.ListEventsUseCase;
import dev.webhooklab.application.port.in.RegisterEventUseCase;
import dev.webhooklab.application.port.out.DeliveryAttemptRepository;
import dev.webhooklab.application.port.out.DeliverySender;
import dev.webhooklab.application.port.out.EventRepository;
import dev.webhooklab.application.port.out.PayloadValidator;
import dev.webhooklab.application.usecase.DeliverEventService;
import dev.webhooklab.application.usecase.GetEventService;
import dev.webhooklab.application.usecase.ListEventsService;
import dev.webhooklab.application.usecase.RegisterEventService;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ApplicationConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  HttpClient httpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(DeliverySender.class)
  DeliverySender httpDeliverySender(
      HttpClient client,
      ObjectMapper mapper,
      @org.springframework.beans.factory.annotation.Value("${delivery.target-url}")
          String targetUrl) {
    return new HttpDeliverySender(client, mapper, targetUrl);
  }

  @Bean
  PayloadValidator payloadValidator(ObjectMapper mapper) {
    return json -> {
      try {
        return json != null && mapper.readTree(json).isObject();
      } catch (RuntimeException e) {
        return false;
      }
    };
  }

  @Bean
  RegisterEventUseCase registerEventUseCase(
      EventRepository repo, PayloadValidator validator, Clock clock) {
    return new RegisterEventService(repo, validator, clock);
  }

  @Bean
  GetEventUseCase getEventUseCase(EventRepository repo) {
    return new GetEventService(repo);
  }

  @Bean
  ListEventsUseCase listEventsUseCase(EventRepository repo) {
    return new ListEventsService(repo);
  }

  @Bean
  @ConditionalOnBean(DeliverySender.class)
  DeliverEventUseCase defaultDeliverEventUseCase(
      DeliveryAttemptRepository attempts, DeliverySender sender, Clock clock) {
    return new DeliverEventService(attempts, sender, clock);
  }
}
