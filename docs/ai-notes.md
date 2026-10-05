# Aprendizado com IA

Registrar observações concretas durante as tarefas.

| Data | Tarefa | Contexto fornecido | Sugestão corrigida ou decisão | Evidência |
| --- | --- | --- | --- | --- |
| 2026-10-05 | Preparação da V1 | Spec consolidada e escopo do mini projeto | Separar documentação, orientações gerais e skill de execução | Árvore gerada; verificação da aplicação permanece pendente |
| 2026-10-05 | T-01 | Bootstrap solicitado com a árvore documental já criada | Gerar scaffold Spring Boot, Compose PostgreSQL e teste de contexto Testcontainers sem alterar specs ou skill | `java`, `mvn` e `docker` não encontrados; build e inicialização permanecem pendentes |
| 2026-10-05 | T-01 | Validação após instalação do JDK | Corrigir o Maven Wrapper para definir `maven.multiModuleProjectDirectory` e usar repositório Maven local do projeto quando o `.m2` global não for gravável | `mvnw -version` passou; `verify` chegou ao Testcontainers e falhou apenas por Docker daemon indisponível |
| 2026-10-05 | T-01 | Docker Desktop 29.8.2 com Testcontainers 1.21.3 | Atualizar Testcontainers para 2.0.2, pois o cliente Docker Java antigo retorna HTTP 400 contra a API atual | `docker info` funciona via `desktop-linux`, mas `verify` falhou no cliente embutido com `BadRequestException` |
| 2026-10-05 | T-01 | Migração para Testcontainers 2.x | Usar os novos artefatos `testcontainers-junit-jupiter` e `testcontainers-postgresql`, além do pacote `org.testcontainers.postgresql` | Maven rejeitou as coordenadas antigas `junit-jupiter` e `postgresql` na versão 2.0.2 |
| 2026-10-05 | T-01 | API Testcontainers 2.x | Remover o parâmetro genérico de `PostgreSQLContainer` | `test-compile -DskipTests` passou após o ajuste |
| 2026-10-05 | T-02 | Spring Boot 4.1.1 e Flyway | Usar `JdbcTemplate` para o adapter e um `Flyway` explícito com `CommandLineRunner`, pois o initializer esperado não existe nessa versão | `./mvnw.cmd verify`: Flyway aplicou V1 e 5 testes de integração passaram |

Manter notas curtas. Registrar erros detectados, decisões e o que foi necessário explicar à IA; não fabricar ganho de produtividade ou saída de testes.
| 2026-10-05 | T-03 | Contrato de resposta da paginaÃ§Ã£o nÃ£o estava detalhado na V1 | Documentar envelope `content`, `page`, `size`, `totalElements` e `totalPages`; manter `attempt` ausente na listagem e executar `COUNT`/`LIMIT`/`OFFSET` no PostgreSQL | `& .\\mvnw.cmd verify`: 9 testes Failsafe passaram, incluindo 8 em `EventApiIT` |
