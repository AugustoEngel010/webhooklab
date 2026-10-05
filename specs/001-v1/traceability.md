# Rastreabilidade — WebhookLab V1

Todos os cenários abaixo são planejados. Nenhum teste da aplicação foi implementado ou executado nesta estrutura inicial.

| Requisito | Cenário | Teste implementado | Comando | Resultado |
| --- | --- | --- | --- | --- |
| T-01 | Bootstrap Spring Boot com Java 21 e conexão PostgreSQL | `src/test/java/dev/augusto/webhooklab/WebhookLabApplicationIT.java:contextStartsWithPostgres` | `.\mvnw.cmd verify` | Compilação e empacotamento foram alcançados; falhou no Testcontainers por não encontrar ambiente Docker/daemon |
| T-01 | Empacotamento do bootstrap | `pom.xml` e `src/main/java/dev/augusto/webhooklab/WebhookLabApplication.java` | `.\mvnw.cmd clean package -DskipTests` | BUILD SUCCESS; JAR executável criado em `target/webhooklab-0.1.0-SNAPSHOT.jar` |
| T-01 | PostgreSQL e aplicação no Compose | `docker-compose.yml` e `Dockerfile` | `docker compose up --build -d`; `docker compose ps`; `docker compose logs app --tail 100`; `curl.exe -i http://localhost:8080/` | Imagem construída; PostgreSQL healthy; aplicação iniciou com Java 21, conectou em PostgreSQL 17.11 e respondeu HTTP 404 na raiz, esperado antes dos endpoints da T-02 |
| T-01 | Compatibilidade do teste PostgreSQL com Docker Desktop atual | `pom.xml` e `WebhookLabApplicationIT` | `.\mvnw.cmd clean verify` | BUILD SUCCESS; Testcontainers 2.0.2 iniciou PostgreSQL 17.11, Spring Boot conectou ao banco e o contexto iniciou |
| RF-01 | Registrar evento válido com UUID, data, PENDING, Location e payload persistido | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:createsEventPersistsPayloadAndReturnsPendingEvent` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; Flyway aplicou V1 em PostgreSQL 17.11 e o teste confirmou 201, Location, UUID, PENDING e payload persistido |
| RF-01 | Rejeitar tipo vazio/maior que 80, payload nulo ou não objeto sem persistir | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:rejectsInvalidInputWithoutPersisting` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; entradas inválidas retornaram 400 e a contagem de `events` não mudou |
| RF-02 | Consultar evento existente com payload e attempt nulo | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:getsExistingEventById` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; POST seguido de GET retornou 200, mesmo UUID/tipo/payload, PENDING e `attempt` nulo |
| RF-02 | Consultar UUID malformado e evento inexistente | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:rejectsMalformedUuidAndUnknownEvent` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; GET malformado retornou 400/INVALID_REQUEST e UUID válido inexistente retornou 404/EVENT_NOT_FOUND |
| RF-03 | Paginação, ordenação e parâmetros inválidos | Ver cenários detalhados abaixo em `EventApiIT` | `& .\\mvnw.cmd clean verify` | BUILD SUCCESS; 8 testes de EventApiIT e 1 teste de WebhookLabApplicationIT, sem falhas/erros |
| RF-04 | Rejeitar segundo disparo e garantir reclamação concorrente única | Pendente | — | Não executado |
| RF-05 | Registrar HTTP 2xx como sucesso | Pendente | — | Não executado |
| RF-06 | Registrar não-2xx sem redirecionamento ou retry | Pendente | — | Não executado |
| RF-07 | Registrar timeout e erro de conexão | Pendente | — | Não executado |
| RF-08 | Preservar histórico, timestamps e duração após reinício | Pendente | — | Não executado |

Após executar, substituir Pendente pelo caminho e classe/método reais do teste, preencher o comando e o resultado observado. Acrescentar linhas quando necessário para distinguir cenários.
| RF-03 | Valores padrÃ£o page=0/size=20 e total da pÃ¡gina | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:listsWithDefaultPageAndSize` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; Failsafe confirmou o teste entre 8 testes de EventApiIT |
| RF-03 | Duas pÃ¡ginas sem repetiÃ§Ã£o e desempate por ID com mesmo createdAt | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:listsDistinctPagesInStableOrder` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; PostgreSQL/Testcontainers confirmou ordem e IDs distintos entre pÃ¡ginas |
| RF-03 | Banco vazio e pÃ¡gina alÃ©m dos resultados retornam content vazio; size=100 aceito | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:returnsEmptyPageAndAcceptsMaximumSize` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; respostas HTTP 200 confirmadas |
| RF-03 | page negativo, size 0/negativo/101 e page/size nÃ£o numÃ©ricos | `src/test/java/dev/augusto/webhooklab/EventApiIT.java:rejectsInvalidPaginationParameters` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; todos os seis casos retornaram HTTP 400 |
