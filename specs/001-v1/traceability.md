# Rastreabilidade — WebhookLab V1

## Evidências T-06

| RF-02 | Consulta consolidada sem tentativa, com tentativa completa e STARTED | `EventApiIT`, `DeliveryHttpIT:getsPersistedHttpErrorTimeoutAndConnectionErrorHistory`, `DeliveryAttemptIT:startedAttemptIsVisibleWithNullCompletionFields` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; GET retornou o histórico completo, com campos finais nulos em STARTED |
| RF-08 | Persistência após fechar e iniciar nova instância com o mesmo PostgreSQL | `DurableHistoryIT:samePostgresIsReadByASecondApplicationInstance` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; a segunda instância releu payload, timestamps UTC, duração, outcome e httpStatus do banco |
| RF-08 | SENDING/STARTED preservado após reinício, sem reclassificação ou reenvio | `DurableHistoryIT:startedHistoryRemainsStartedAfterApplicationRestart` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; estado e campos finais nulos permaneceram no mesmo banco |
| RF-02/RF-08 | Erros 400, 404, 409 e 503 no formato público | `ApiExceptionsTest:persistenceFailureUsesPublic503ContractWithoutTechnicalDetail`, `EventApiIT`, `DeliveryAttemptIT` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; códigos públicos confirmados e detalhe técnico não exposto |
| RF-08 | Validação manual após reinício somente da aplicação | PowerShell: `Invoke-RestMethod POST /events`, `POST /events/{id}/deliver`, `GET /events/{id}`, `docker compose stop app`, `docker compose start app`, novo `GET /events/{id}` | Comandos executados pelo usuário em 06/10/2026 | O evento `c09e1e66-6d29-44bc-b0a7-e7b5a74f5d74` e a tentativa `e6a9f141-6ae6-40b8-8c39-d5c786fb4870` permaneceram consultáveis como `FAILED`/`CONNECTION_ERROR`, com `httpStatus: null` e `durationMs: 38`; nenhum novo envio ocorreu |

Todos os cenários abaixo são planejados. Nenhum teste da aplicação foi implementado ou executado nesta estrutura inicial.

| Requisito | Cenário | Teste implementado | Comando | Resultado |
| --- | --- | --- | --- | --- |
| T-01 | Bootstrap Spring Boot com Java 21 e conexão PostgreSQL | `src/test/java/dev/webhooklab/WebhookLabApplicationIT.java:contextStartsWithPostgres` | `.\mvnw.cmd verify` | Compilação e empacotamento foram alcançados; falhou no Testcontainers por não encontrar ambiente Docker/daemon |
| T-01 | Empacotamento do bootstrap | `pom.xml` e `src/main/java/dev/webhooklab/WebhookLabApplication.java` | `.\mvnw.cmd clean package -DskipTests` | BUILD SUCCESS; JAR executável criado em `target/webhooklab-0.1.0-SNAPSHOT.jar` |
| T-01 | PostgreSQL e aplicação no Compose | `docker-compose.yml` e `Dockerfile` | `docker compose up --build -d`; `docker compose ps`; `docker compose logs app --tail 100`; `curl.exe -i http://localhost:8080/` | Imagem construída; PostgreSQL healthy; aplicação iniciou com Java 21, conectou em PostgreSQL 17.11 e respondeu HTTP 404 na raiz, esperado antes dos endpoints da T-02 |
| T-01 | Compatibilidade do teste PostgreSQL com Docker Desktop atual | `pom.xml` e `WebhookLabApplicationIT` | `.\mvnw.cmd clean verify` | BUILD SUCCESS; Testcontainers 2.0.2 iniciou PostgreSQL 17.11, Spring Boot conectou ao banco e o contexto iniciou |
| RF-01 | Registrar evento válido com UUID, data, PENDING, Location e payload persistido | `src/test/java/dev/webhooklab/EventApiIT.java:createsEventPersistsPayloadAndReturnsPendingEvent` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; Flyway aplicou V1 em PostgreSQL 17.11 e o teste confirmou 201, Location, UUID, PENDING e payload persistido |
| RF-01 | Rejeitar tipo vazio/maior que 80, payload nulo ou não objeto sem persistir | `src/test/java/dev/webhooklab/EventApiIT.java:rejectsInvalidInputWithoutPersisting` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; entradas inválidas retornaram 400 e a contagem de `events` não mudou |
| RF-02 | Consultar evento existente com payload e attempt nulo | `src/test/java/dev/webhooklab/EventApiIT.java:getsExistingEventById` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; POST seguido de GET retornou 200, mesmo UUID/tipo/payload, PENDING e `attempt` nulo |
| RF-02 | Consultar UUID malformado e evento inexistente | `src/test/java/dev/webhooklab/EventApiIT.java:rejectsMalformedUuidAndUnknownEvent` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; GET malformado retornou 400/INVALID_REQUEST e UUID válido inexistente retornou 404/EVENT_NOT_FOUND |
| RF-03 | Paginação, ordenação e parâmetros inválidos | Ver cenários detalhados abaixo em `EventApiIT` | `& .\\mvnw.cmd clean verify` | BUILD SUCCESS; 8 testes de EventApiIT e 1 teste de WebhookLabApplicationIT, sem falhas/erros |
| RF-04 | Evento PENDING inicia como SENDING com tentativa STARTED e timestamp UTC | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:pendingEventBecomesSendingAndGetsStartedAttempt` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | `Tests run: 7, Failures: 0, Errors: 0`; PostgreSQL 17.11/Testcontainers confirmou estado e tentativa persistidos |
| RF-04 | Duas execuções concorrentes persistem uma tentativa e fazem um envio | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:concurrentExecutionsPersistOneAttemptAndSendOnce` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | Mesmo resultado observado; uma chamada retornou claim e outra conflito, com uma linha em `delivery_attempts` |
| RF-04 | Segundo disparo retorna HTTP 409 sem nova tentativa ou envio | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:secondDeliveryIsHttp409WithoutAnotherSend` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | Mesmo resultado observado; MockMvc confirmou 409/DELIVERY_CONFLICT |
| RF-04 | SENDING, DELIVERED e FAILED rejeitam nova tentativa | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:sendingDeliveredAndFailedEventsAreRejected` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | Mesmo resultado observado; nenhum registro ou envio adicional |
| RF-04 | Falha no INSERT da tentativa desfaz a transição do evento | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:failedAttemptInsertRollsBackEventClaim` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | Mesmo resultado observado; evento permaneceu PENDING |
| RF-04 | Porta de envio é chamada sem transação de banco ativa | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:senderRunsAfterDatabaseTransactionCommits` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | Mesmo resultado observado; fake observou transação inativa |
| RF-04 | Resultado externo desconhecido preserva SENDING/STARTED | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java:unknownSenderResultLeavesSendingAndStarted` | `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test` | Mesmo resultado observado; exceção do fake não reclassificou nem apagou o histórico |
| RF-04 | Verificação reproduzida no ambiente local do usuário | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java` e migrations V1/V2 | `docker ps`; `& .\\mvnw.cmd verify` | Usuário confirmou execução bem-sucedida; Docker mostrou PostgreSQL saudável e aplicação em execução |
| RF-05 | Registrar HTTP 2xx como sucesso, envelope/header e persistência | `src/test/java/dev/webhooklab/DeliveryHttpIT.java:sendsEnvelopeAndHeaderAndPersistsSuccess` | `& .\mvnw.cmd -q '-Dtest=DeliveryHttpIT' test` | 4 testes, 0 falhas/erros; cenário passou com PostgreSQL 17.11 e WireMock |
| RF-06 | Registrar 302 como HTTP_ERROR sem redirecionamento | `src/test/java/dev/webhooklab/DeliveryHttpIT.java:persistsHttpErrorAndDoesNotFollowRedirect` | `& .\mvnw.cmd -q '-Dtest=DeliveryHttpIT' test` | Cenário passou; status 302 preservado e uma chamada verificada |
| RF-07 | Registrar timeout e erro de conexão sem status | `src/test/java/dev/webhooklab/DeliveryHttpIT.java:persistsTimeoutAndRejectsSecondDeliveryWithoutSendingAgain`, `classifiesConnectionFailureWithoutHttpStatus` | `& .\mvnw.cmd -q '-Dtest=DeliveryHttpIT' test` | Cenários passaram; resposta síncrona após timeout e httpStatus nulo |
| RF-05/RF-06/RF-07 | Verificação completa da entrega HTTP, persistência e concorrência | `DeliveryAttemptIT`, `DeliveryHttpIT`, `EventApiIT`, `WebhookLabApplicationIT` | `& .\mvnw.cmd verify` | Observado em 06/10/2026: `BUILD SUCCESS`; 20 testes, 0 falhas, 0 erros; duração 43,056 s |

Após executar, substituir Pendente pelo caminho e classe/método reais do teste, preencher o comando e o resultado observado. Acrescentar linhas quando necessário para distinguir cenários.
| RF-03 | Valores padrÃ£o page=0/size=20 e total da pÃ¡gina | `src/test/java/dev/webhooklab/EventApiIT.java:listsWithDefaultPageAndSize` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; Failsafe confirmou o teste entre 8 testes de EventApiIT |
| RF-03 | Duas pÃ¡ginas sem repetiÃ§Ã£o e desempate por ID com mesmo createdAt | `src/test/java/dev/webhooklab/EventApiIT.java:listsDistinctPagesInStableOrder` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; PostgreSQL/Testcontainers confirmou ordem e IDs distintos entre pÃ¡ginas |
| RF-03 | Banco vazio e pÃ¡gina alÃ©m dos resultados retornam content vazio; size=100 aceito | `src/test/java/dev/webhooklab/EventApiIT.java:returnsEmptyPageAndAcceptsMaximumSize` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; respostas HTTP 200 confirmadas |
| RF-03 | page negativo, size 0/negativo/101 e page/size nÃ£o numÃ©ricos | `src/test/java/dev/webhooklab/EventApiIT.java:rejectsInvalidPaginationParameters` | `& .\\mvnw.cmd verify` | BUILD SUCCESS; todos os seis casos retornaram HTTP 400 |

## T-07 — Compose, Swagger e demonstração

| Requisito | Cenário | Teste/verificação | Comando | Resultado observado |
| --- | --- | --- | --- | --- |
| T-07 | Stack completa e build sem Java/Maven no host | Dockerfile multi-stage e Compose | `docker compose up --build -d`; `docker compose ps` | Imagem construída com Maven/Java 21; app, PostgreSQL e WireMock `healthy`; volume persistente ativo |
| T-07 | Prontidão e conectividade | Actuator, healthchecks e WireMock Admin | `docker compose config`; readiness; WireMock health | `DELIVERY_TARGET_URL` interno `http://wiremock:8080/webhook`; app `UP`; WireMock `healthy` |
| T-07 | Swagger UI e contrato OpenAPI | Springdoc em execução | GET `/v3/api-docs`; GET `/swagger-ui/index.html` | OpenAPI 3.1 servido; Swagger retornou HTTP 200 |
| RF-01/RF-02/RF-03 | Registro PENDING, consulta e paginação | Demonstração manual em `docs/demo.md` | POST `/events`; GET `/events/{id}`; GET `/events?page=0&size=20` | Evento novo PENDING e envelope paginado consultados |
| RF-05 | WireMock 200 | Demonstração manual | mapping 200 + POST `/events/{id}/deliver` + GET | HTTP 201; `DELIVERED/SUCCEEDED` |
| RF-06 | WireMock 500 | Demonstração manual | mapping 500 + POST `/events/{id}/deliver` + GET | HTTP 201; `FAILED/HTTP_ERROR`; `httpStatus=500` |
| RF-07 | WireMock atrasado | Demonstração manual | mapping 200/4000 ms + POST `/events/{id}/deliver` + GET | HTTP 201; `FAILED/TIMEOUT`; `durationMs=3001`; `httpStatus=null` |
| RF-04 | Repetição sem novo envio | Demonstração manual e contagem WireMock | segundo POST `/events/{id}/deliver`; GET `/__admin/requests` | HTTP 409; requests ficaram 6 antes/depois |
| RF-08 | Reinício somente da aplicação | Compose e consulta HTTP | `docker compose restart app`; GET `/events/{id}` | `DELIVERED/SUCCEEDED` e histórico permaneceram persistidos |
| T-08/regressão | Testes unitários e integração | `*Test` e `*IT` | `& .\\mvnw.cmd verify` | `BUILD SUCCESS`; 24 testes, 0 falhas e 0 erros |
| T-07 | Execução por outra pessoa seguindo o README | Confirmação manual do usuário | 07/10/2026: execução local conforme `README.md` e `docs/demo.md` | Usuário confirmou que funcionou |
| RF-02/RF-03 | Listagem retorna a tentativa persistida quando existente | `src/test/java/dev/webhooklab/EventApiIT.java:listsPersistedAttemptWithEvent` | `& .\\mvnw.cmd -q '-Dtest=EventApiIT' test` | BUILD SUCCESS; 9 testes, 0 falhas e 0 erros; `attempt.id`, `SUCCEEDED`, `httpStatus=200` e `durationMs=100` confirmados |
# T-08 — CI, formatação e fechamento documental

Resultado pós-mudança observado em 07/10/2026: `& .\\mvnw.cmd -B verify` executou Surefire com 1 teste, Failsafe com 25 testes e Spotless com 38 arquivos limpos; 0 falhas, 0 erros, 0 ignorados; `BUILD SUCCESS`.

Os testes abaixo distinguem implementação (caminhos reais), execução local (comando e saída observada) e validação remota (ainda não observada).

| Requisito | Cenário | Teste implementado | Comando | Resultado observado |
| --- | --- | --- | --- | --- |
| RF-01/RF-02 | Registro, consulta e validação de evento | `src/test/java/dev/webhooklab/EventApiIT.java` | `& .\\mvnw.cmd -B verify` | Executado em 07/10/2026: Failsafe executou a suíte; BUILD SUCCESS, sem falhas/erros |
| RF-03 | Paginação, ordenação e parâmetros inválidos | `src/test/java/dev/webhooklab/EventApiIT.java` | `& .\\mvnw.cmd -B verify` | Executado: cenários descobertos e concluídos sem falhas/erros |
| RF-04 | Reclamação única, concorrência, 409 e preservação de STARTED | `src/test/java/dev/webhooklab/DeliveryAttemptIT.java` | `& .\\mvnw.cmd -B verify` | Executado: 8 testes, 0 falhas, 0 erros |
| RF-05 | Entrega 2xx e conclusão conjunta | `src/test/java/dev/webhooklab/DeliveryHttpIT.java` | `& .\\mvnw.cmd -B verify` | Executado: cenários WireMock concluídos sem falhas/erros |
| RF-06 | Erro HTTP sem redirect | `src/test/java/dev/webhooklab/DeliveryHttpIT.java` | `& .\\mvnw.cmd -B verify` | Executado: status externo preservado e suíte concluída sem falhas/erros |
| RF-07 | Timeout e falha de conexão sem status HTTP | `src/test/java/dev/webhooklab/DeliveryHttpIT.java` | `& .\\mvnw.cmd -B verify` | Executado: cenários WireMock concluídos sem falhas/erros |
| RF-08 | Histórico após reinício e campos de duração | `src/test/java/dev/webhooklab/DurableHistoryIT.java` | `& .\\mvnw.cmd -B verify` | Executado: 2 testes, 0 falhas, 0 erros |
| T-08 | Unidade e integração não podem ficar silenciosamente vazias | `pom.xml` (`maven-surefire-plugin`/`maven-failsafe-plugin`, `failIfNoTests=true`) | `& .\\mvnw.cmd -B verify` | Implementado; execução local anterior à mudança observou 1 unitário e 25 integração. Repetir após a mudança é obrigatório. |
| T-08 | Formatação Java verificada no build | `pom.xml` (`spotless-maven-plugin`) | `& .\\mvnw.cmd spotless:check` | Implementado; resultado pós-mudança pendente nesta revisão |
| T-08 | CI com Java 21, wrapper, cache, Docker e relatórios | `.github/workflows/ci.yml` | Execução no GitHub Actions | Pendente: não há URL nem run remoto observado nesta entrega |
## T-09 — verificação local observada

| Critério | Evidência | Comando | Resultado observado |
| --- | --- | --- | --- |
| REF-01 | SQL e mapper JDBC separados; conversão de Event reutilizada; LEFT JOIN, Optional.empty e nulos preservados | `docker compose --profile tools run --rm verify` | Failsafe concluiu com 25 testes, 0 falhas e 0 erros |
| REF-02 | `claimForDelivery`, claim condicional, STARTED atômico, rollback, HTTP fora da transação e conclusão atômica | `docker compose --profile tools run --rm verify` | DeliveryAttemptIT 8/8 e DeliveryHttpIT 5/5 passaram com PostgreSQL/Testcontainers |
| REF-03 | Exceções próprias em arquivos separados e imports explícitos | `docker compose --profile tools run --rm verify` | ApiExceptionsTest 1/1 passou; contrato público preservado |
| REF-04 | WebhookEnvelope com id/eventType/createdAt/payload e header X-Webhook-Event-Id | `docker compose --profile tools run --rm verify` | Cenários WireMock passaram |
| REF-05 | EventService concreto para register/get/list; DeliverEventService separado | `docker compose --profile tools run --rm verify` | EventApiIT 9/9 e suite completa passaram |
| REF-06 | Dockerfile Maven 3.9.11/JDK 21 + JRE 21, Wrapper removido, verify com socket Docker e CI via Docker | `docker compose --profile tools config`; `docker compose build --no-cache app`; `docker compose --profile tools run --rm verify` | Configuração válida; imagem reconstruída; Surefire 1/1, Failsafe 25/25, Spotless limpo, BUILD SUCCESS; relatórios em target/ |
| REF-07 | Design, ADR, AGENTS, README, docs, tasks e traceability atualizados | `git diff --check`; `rg --files -g 'mvnw*' -g '.mvn/wrapper/**'` | Sem erro de whitespace; nenhum Wrapper presente; workflow remoto T-08 não observado |

Smoke test Compose observado: readiness 200; registro do evento `8159216f-6a95-4188-8c0c-4145820b4ec0`; primeira entrega 201; segundo disparo 409.

Verificação inicial registrada antes da implementação: `docker version` observou Docker Server 29.8.2; `docker compose config` passou; `docker compose build app` passou no estado anterior, ainda usando o Wrapper. Essa evidência é apenas baseline e não foi reutilizada para declarar o aceite pós-T-09.
