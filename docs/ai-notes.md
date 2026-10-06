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

| 2026-10-06 | T-04 | O proxy transacional do Spring não pode subclassificar um adapter `final` | Remover `final` de `JdbcDeliveryAttemptRepository`; manter `@Transactional` no método de claim para abranger UPDATE e INSERT, com o sender somente após o retorno | Primeira execução falhou na criação do bean por CGLIB; execução seguinte de `DeliveryAttemptIT` passou com 7 testes |
| 2026-10-06 | T-04 | O contexto principal não deve criar um sender fake nem um adapter HTTP incompleto | Condicionar o caso de uso/controller à porta `DeliverySender`; fornecer sender e controller fake apenas no contexto de integração da T-04, deixando o adapter HTTP para T-05 | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test`: 7 testes sem falhas/erros |
| 2026-10-06 | T-04 | Validação reproduzida pelo usuário após a implementação | Registrar separadamente a evidência do ambiente do usuário, sem atribuir ao agente detalhes de saída não fornecidos | Usuário confirmou que `& .\\mvnw.cmd verify` funcionou; `docker ps` mostrou PostgreSQL healthy e aplicação Up |

| 2026-10-06 | T-05 | Classificação HTTP deve ficar na borda | `DeliverySender` retorna `DeliveryResult`; o domínio continua sem tipos HTTP e o adapter classifica 2xx, não-2xx, timeout e conexão | `DeliveryHttpIT`: 4 testes passaram; `DeliveryAttemptIT`: 7 testes passaram |
| 2026-10-06 | T-05 | Resultado externo e persistência são fases distintas | Timeout/conexão retornam resultado conhecido; exceções inesperadas não concluem a tentativa, preservando SENDING/STARTED | Verificado em `DeliveryAttemptIT.unknownSenderResultLeavesSendingAndStarted` e nos cenários WireMock |
| 2026-10-06 | T-05/T-08 | Verificação completa reproduzida após a implementação | Registrar a saída final do Maven separadamente dos avisos operacionais do Springdoc | `& .\mvnw.cmd verify`: `BUILD SUCCESS`; 20 testes, 0 falhas, 0 erros; 43,056 s |

| 2026-10-06 | T-06 | A consulta por ID ignorava `delivery_attempts` e os handlers podiam devolver detalhes técnicos | Criar o agregado de leitura `EventHistory`, carregar evento/tentativa com `LEFT JOIN` no adapter JDBC e usar detalhes públicos fixos para 400/503 | `& .\\mvnw.cmd verify`: BUILD SUCCESS; 24 testes, 0 falhas e 0 erros |
| 2026-10-06 | T-06 | Reinício deve provar leitura de estado persistido, não reutilização de objetos | `DurableHistoryIT` fecha cada contexto Spring, inicia outro com `DB_URL` apontando para o mesmo container e consulta pelo caso de uso recém-criado | `DurableHistoryIT`: 2 testes passaram; Flyway validou a mesma base nas duas instâncias |
| 2026-10-06 | T-06 | A validação manual inicialmente falhou por JSON sem aspas ao passar para `curl.exe` no PowerShell | Usar `Invoke-RestMethod` com bytes UTF-8; depois do restart somente do app, o mesmo evento e tentativa foram relidos do PostgreSQL | Usuário observou `FAILED`/`CONNECTION_ERROR`, `httpStatus: null`, `durationMs: 38` antes e depois do reinício |

Manter notas curtas. Registrar erros detectados, decisões e o que foi necessário explicar à IA; não fabricar ganho de produtividade ou saída de testes.
| 2026-10-05 | T-03 | Contrato de resposta da paginaÃ§Ã£o nÃ£o estava detalhado na V1 | Documentar envelope `content`, `page`, `size`, `totalElements` e `totalPages`; manter `attempt` ausente na listagem e executar `COUNT`/`LIMIT`/`OFFSET` no PostgreSQL | `& .\\mvnw.cmd verify`: 9 testes Failsafe passaram, incluindo 8 em `EventApiIT` |
