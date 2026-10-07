package dev.webhooklab.adapters.out.persistence;

import dev.webhooklab.application.port.out.DeliveryAttemptRepository;
import dev.webhooklab.application.port.out.EventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class JdbcPersistenceConfig {
  @Bean
  EventRepository eventRepository(JdbcTemplate jdbc) {
    return new JdbcEventRepository(jdbc);
  }

  @Bean
  DeliveryAttemptRepository deliveryAttemptRepository(JdbcTemplate jdbc) {
    return new JdbcDeliveryAttemptRepository(jdbc);
  }
}
