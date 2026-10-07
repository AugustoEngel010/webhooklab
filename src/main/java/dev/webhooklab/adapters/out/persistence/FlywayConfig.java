package dev.webhooklab.adapters.out.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {
  @Bean
  Flyway flyway(DataSource dataSource) {
    return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
  }

  @Bean
  CommandLineRunner migrate(Flyway flyway) {
    return args -> flyway.migrate();
  }
}
