package dev.augusto.webhooklab.adapters.out.persistence;

import dev.augusto.webhooklab.application.port.out.EventRepository;
import dev.augusto.webhooklab.application.port.out.DeliveryAttemptRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class JdbcPersistenceConfig {
    @Bean EventRepository eventRepository(JdbcTemplate jdbc) { return new JdbcEventRepository(jdbc); }
    @Bean DeliveryAttemptRepository deliveryAttemptRepository(JdbcTemplate jdbc) { return new JdbcDeliveryAttemptRepository(jdbc); }
}
