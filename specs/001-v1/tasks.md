# Tarefas — WebhookLab V1

Status inicial: todas as tarefas de implementação pendentes. A criação desta árvore atende apenas à preparação documental da T-01.

## 7. Tarefas — tasks.md

Todas as tarefas estão pendentes. Os testes descritos são planejados, não evidência de execução.

| Task | Entrega | Aceite / verificação |
| --- | --- | --- |
| T-01 | Criar repositório local, registrar specs e gerar aplicação com Maven Wrapper | Build executa; aplicação inicia e conecta ao PostgreSQL do Compose |
| T-02 | Migration events, domínio, registro e consulta por ID | RF-01 e RF-02; teste HTTP com PostgreSQL real confirma criação, payload persistido, consulta e rejeição de entrada inválida |
| T-03 | Listagem paginada | RF-03; ordenação estável, páginas e parâmetros inválidos; concluída |
| T-04 | Migration delivery_attempts e operação atômica de início | RF-04; teste concorrente confirma uma única reclamação e um único envio |
| T-05 | Adapter HTTP e conclusão da tentativa | RF-05, RF-06 e RF-07; sucesso, erro HTTP, timeout e erro de conexão |
| T-06 | Histórico e erros da API | RF-02 e RF-08; eventos, tentativas e seus resultados permanecem após reiniciar |
| T-07 | Swagger, Compose completo e exemplos de demonstração | Uma pessoa executa localmente seguindo o README |
| T-08 | CI, formatação, documentação e rastreabilidade | Maven verify executa os testes de integração; requisitos apontam para testes existentes |

## 8. Primeiro ciclo de implementação: T-01 e T-02

1. Criar um repositório local chamado webhooklab e versionar a especificação.
2. Gerar o projeto Java 21 com Maven Wrapper e dependências de Web MVC, Validation, Data JPA, Flyway, PostgreSQL e testes. Adicionar suporte Flyway para PostgreSQL conforme a versão selecionada.
3. Subir PostgreSQL no Compose e criar a migration de events.
4. Implementar o modelo de evento, as portas necessárias e os casos de uso de registro e consulta.
5. Implementar o adapter de persistência e os dois endpoints.
6. Executar os testes HTTP com PostgreSQL do Testcontainers.
7. No Swagger, registrar o exemplo, consultar pelo ID e confirmar o estado PENDING.
8. Revisar o diff e registrar a evidência antes de concluir a tarefa.

O primeiro marco está concluído quando POST /events e GET /events/{id} funcionam com persistência real e os testes comprovam seus critérios. Depois, seguir para listagem e entrega HTTP.

## Dependências e progresso

| Tarefa | Dependência | Estado |
| --- | --- | --- |
| T-01 | Specs e árvore inicial | Concluída: build, teste Testcontainers e inicialização via Compose validados |
| T-02 | T-01 | Concluída: RF-01/RF-02 verificados por testes HTTP com PostgreSQL real via Testcontainers |
| T-03 | T-02 | ConcluÃ­da: RF-03 verificado por testes HTTP com PostgreSQL real via Testcontainers |

<!-- Evidência histórica da T-03 consolidada abaixo. -->

- Contrato paginado documentado; `GET /events` usa `LIMIT/OFFSET` e ordenaÃ§Ã£o `created_at DESC, id DESC`.
- `EventApiIT` cobre padrÃµes, duas pÃ¡ginas, empate, vazio, pÃ¡gina alÃ©m, limite 100 e rejeiÃ§Ãµes.
- Comando: `& .\\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou `EventApiIT` com 8 testes e `WebhookLabApplicationIT` com 1, todos sem falhas/erros; Testcontainers iniciou PostgreSQL 17.11.
| T-04 | T-02 | Concluída: RF-04 verificado por 7 testes de integração com PostgreSQL real via Testcontainers |
| T-05 | T-04 | Concluída: RF-05/RF-06/RF-07 verificados por integração HTTP com WireMock e PostgreSQL real |
| T-06 | T-03 e T-05 | Concluída: RF-02/RF-08 e erros padronizados verificados por testes HTTP, Testcontainers e reinício da aplicação |
| T-07 | T-06 | Concluída: Compose completo, Swagger UI e demonstração real pelo README verificados |
| T-08 | Build disponível; concluir antes da entrega V1 | Pendente |

## Evidências T-01

- Bootstrap Spring Boot Java 21, Maven Wrapper, configuração PostgreSQL/Flyway/Hibernate, `Dockerfile`, `docker-compose.yml` e teste `WebhookLabApplicationIT` criados.
- Comando: `& .\mvnw.cmd clean verify`. Resultado observado: `BUILD SUCCESS`; Testcontainers 2.0.2 iniciou PostgreSQL 17.11 e o contexto Spring Boot conectou ao banco sem falhas.
- Comandos: `docker compose up --build -d`; `docker compose ps`; `docker compose logs app --tail 100`; `curl.exe -i http://localhost:8080/`. Resultado observado: imagem construída, PostgreSQL `healthy`, aplicação em execução e HTTP 404 na raiz, esperado antes dos endpoints da T-02.

## Evidências T-02

- Migration `V1__create_events.sql`, domínio sem frameworks, portas/casos de uso, adapter JDBC e endpoints em `src/main/java`; testes em `EventApiIT`.
- Comando: `& .\\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Flyway aplicou 1 migration e Failsafe executou 5 testes, sem falhas ou erros.
- Cobertura: criação 201/Location/PENDING, payload persistido, consulta 200, rejeição 400 sem persistência e consulta 400/404.

## Evidências T-03

- Contrato paginado documentado; `GET /events` usa `LIMIT/OFFSET` e ordenação `created_at DESC, id DESC`; testes HTTP cobrem padrões, duas páginas, empate, vazio, página além, limite 100 e rejeições.
- Comando: `& .\\mvnw.cmd clean verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou `EventApiIT` com 8 testes e `WebhookLabApplicationIT` com 1, todos sem falhas/erros; Testcontainers iniciou PostgreSQL 17.11.
- Comandos manuais: `docker compose up --build -d`; `Invoke-RestMethod` para POST de `ORDER_CREATED`; `Invoke-RestMethod "http://localhost:8080/events?page=0&size=20"`; `ConvertTo-Json -Depth 10`. Resultado observado pelo usuário: container iniciado, PostgreSQL saudável, evento `PENDING` criado, `page=0`, `size=20`, `totalElements=1`, `totalPages=1` e payload completo visualizado.

## Evidências T-04

- Migration `V2__create_delivery_attempts.sql` com FK para `events`, `UNIQUE(event_id)`, estados de resultado e timestamps; modelo, portas e claim transacional implementados sem acoplar o domínio a frameworks.
- Comando: `& .\\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test`. Resultado observado: `Tests run: 7, Failures: 0, Errors: 0`; PostgreSQL 17.11 iniciou via Testcontainers.
- Cobertura: PENDING→SENDING com tentativa STARTED, concorrência com um único envio, HTTP 409, rejeição de SENDING/DELIVERED/FAILED, rollback do claim, envio fora da transação e preservação de SENDING/STARTED quando o resultado externo é desconhecido.
- Comando: `& .\\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou 16 testes, sem falhas/erros; Flyway validou e aplicou V1 e V2 em PostgreSQL 17.11.
- Comandos reproduzidos pelo usuário: `docker ps`; `& .\\mvnw.cmd verify`. Resultado informado: PostgreSQL `healthy`, aplicação `Up` e verificação concluída com sucesso.
- Limitação histórica da T-04: o adapter HTTP e a conclusão foram entregues na T-05.

## Evidências T-05

- `HttpDeliverySender` usa `DELIVERY_TARGET_URL`, envelope JSON, `X-Webhook-Event-Id`, timeout de conexão de 1 s, timeout de request de 3 s e `Redirect.NEVER`; não há retentativas.
- `DeliverEventService` confirma a reclamação antes do sender e conclui evento/tentativa em nova transação; datas usam UTC e duração usa relógio monotônico.
- `DeliveryHttpIT` cobre envelope/header, 204, 302 sem redirect, timeout, erro de conexão, persistência, 409 e envio único.
- `& .\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test`: Surefire observou 7 testes, 0 falhas/erros.
- `& .\mvnw.cmd -q '-Dtest=DeliveryHttpIT' test`: Surefire observou 4 testes, 0 falhas/erros; Testcontainers iniciou PostgreSQL 17.11.
- Comando adicional: `& .\mvnw.cmd verify`. Resultado observado em 06/10/2026 às 17:45:46: `BUILD SUCCESS`; Failsafe executou 20 testes, 0 falhas e 0 erros, em 43,056 s.
- Os avisos do Springdoc sobre `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` são apenas recomendações de configuração para produção e não afetaram a verificação.

## Evidências T-06

- `GET /events/{id}` agora lê `events` e `delivery_attempts` em uma consulta com `LEFT JOIN`, retornando o histórico completo; `STARTED` permanece com campos de término nulos.
- Evidência manual reproduzida pelo usuário em 06/10/2026: `docker compose stop app`; `docker compose start app`; `Start-Sleep -Seconds 5`; `Invoke-RestMethod GET /events/{id}`. O evento `c09e1e66-6d29-44bc-b0a7-e7b5a74f5d74` continuou `FAILED`, com a mesma tentativa `e6a9f141-6ae6-40b8-8c39-d5c786fb4870`, `CONNECTION_ERROR`, `httpStatus: null`, timestamps e `durationMs: 38`.
- Resultado observado: PostgreSQL permaneceu preservado e a nova instância da aplicação consultou o histórico persistido sem novo envio.
- Erros de entrada, UUID/parâmetros inválidos, evento ausente, conflito de entrega e falha de persistência usam `status`, `code` e `detail`, sem expor exceções técnicas.
- `DurableHistoryIT` fecha a primeira instância, inicia uma segunda contra o mesmo PostgreSQL/Testcontainers e consulta novamente o banco; cobre sucesso, timestamps UTC, duração e SENDING/STARTED sem reenvio.
- Comando: `& .\\mvnw.cmd verify`. Resultado observado em 06/10/2026: `BUILD SUCCESS`; 24 testes, 0 falhas e 0 erros, incluindo 2 testes de `DurableHistoryIT` e 1 de `ApiExceptionsTest`.

Preencher, por tarefa: requisito atendido, arquivos alterados, comando executado, resultado observado e limitações. Não marcar concluída apenas por existir código ou por o agente afirmar sucesso.

## Evidências T-07

- Arquivos alterados: `Dockerfile`, `docker-compose.yml`, `pom.xml`, `application.yml`, `OpenApiConfig`, controllers anotados, `README.md` e `docs/demo.md`.
- `docker compose config` validou app, PostgreSQL e WireMock, healthchecks, volume `webhooklab-postgres` e `DELIVERY_TARGET_URL=http://wiremock:8080/webhook`.
- `docker compose up --build -d` construiu a imagem pelo multi-stage Maven/Java 21; `docker compose ps` observou os três serviços `healthy`.
- Readiness retornou `UP`; OpenAPI 3.1 e Swagger UI responderam; WireMock Admin retornou `healthy`.
- Demonstração real: PENDING; 200→`DELIVERED/SUCCEEDED`; 500→`FAILED/HTTP_ERROR/500`; atraso 4000 ms→`FAILED/TIMEOUT`, `durationMs=3001` e status HTTP nulo; repetição 409 sem aumento de requests (6 antes/depois); restart apenas de app preservou `DELIVERED/SUCCEEDED`.
- `& .\\mvnw.cmd verify` com acesso ao Docker: `BUILD SUCCESS`; 24 testes, 0 falhas e 0 erros.
- Confirmação do usuário em 07/10/2026: a execução local seguindo o README funcionou.
