package dev.augusto.webhooklab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI webhookLabOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("WebhookLab V1 API")
                .version("v1")
                .description("Laboratório local de entrega manual, síncrona e única por evento. "
                        + "O HTTP 201 de /deliver confirma a tentativa registrada, inclusive quando outcome é uma falha."));
    }
}
