# Tarefas — WebhookLab V1

Status atual, conforme as evidências registradas neste arquivo:
T-01 a T-07 concluídas; T-08 concluída localmente, com validação
no GitHub Actions pendente; T-09 concluida localmente.

## 7. Tarefas — tasks.md

As entregas e os critérios de aceite estão listados abaixo.
O estado de cada tarefa está em **Dependências e progresso**.
Cenários planejados não constituem evidência de execução.

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
| T-09 | Refatorar JDBC, exceções, envelope e serviços; migrar build e verificação para Docker e remover Maven Wrapper | REF-01 a REF-07; preservar RF-01 a RF-08; construir e verificar pelo Docker sem Wrapper ou Maven instalado no host; rastreabilidade atualizada |

## 8. Primeiro ciclo de implementação: T-01 e T-02

Roteiro inicial mantido como histórico. A persistência entregue na T-02
usa adapter JDBC, conforme as evidências abaixo; a T-09 mantém essa
implementação.

1. Criar um repositório local chamado webhooklab e versionar a especificação.
2. Gerar o projeto Java 21 com Maven Wrapper e dependências de Web MVC, Validation, Data JPA, Flyway, PostgreSQL e testes. Adicionar suporte Flyway para PostgreSQL conforme a versão selecionada.
3. Subir PostgreSQL no Compose e criar a migration de events.
4. Implementar o modelo de evento, as portas necessárias e os casos de uso de registro e consulta.
5. Implementar o adapter de persistência e os dois endpoints.
6. Executar os testes HTTP com PostgreSQL do Testcontainers.
7. No Swagger, registrar o exemplo, consultar pelo ID e confirmar o estado PENDING.
8. Revisar o diff e registrar a evidência antes de concluir a tarefa.

O primeiro marco está concluído quando POST /events e GET /events/{id}
funcionam com persistência real e os testes comprovam seus critérios.
Depois, seguir para listagem e entrega HTTP.

## Dependências e progresso

| Tarefa | Dependência | Estado |
| --- | --- | --- |
| T-01 | Specs e árvore inicial | Concluída: build, teste Testcontainers e inicialização via Compose validados |
| T-02 | T-01 | Concluída: RF-01/RF-02 verificados por testes HTTP com PostgreSQL real via Testcontainers |
| T-03 | T-02 | Concluída: RF-03 verificado por testes HTTP com PostgreSQL real via Testcontainers |
| T-04 | T-02 | Concluída: RF-04 verificado por 7 testes de integração com PostgreSQL real via Testcontainers |
| T-05 | T-04 | Concluída: RF-05/RF-06/RF-07 verificados por integração HTTP com WireMock e PostgreSQL real |
| T-06 | T-03 e T-05 | Concluída: RF-02/RF-08 e erros padronizados verificados por testes HTTP, Testcontainers e reinício da aplicação |
| T-07 | T-06 | Concluída: Compose completo, Swagger UI e demonstração real pelo README verificados |
| T-08 | Build disponível; concluir antes da entrega V1 | Concluída localmente: Surefire/Failsafe, Spotless, workflow, documentação e rastreabilidade; validação remota no GitHub Actions pendente |
| T-09 | T-06, T-07 e critérios locais da T-08 | Pendente: alterações da execução anterior revertidas pelo autor; executar T-09.1 a T-09.5 com o escopo definitivo, incluindo remoção do Wrapper |

## Evidências T-01

- Bootstrap Spring Boot Java 21, Maven Wrapper, configuração PostgreSQL/Flyway/Hibernate, `Dockerfile`, `docker-compose.yml` e teste `WebhookLabApplicationIT` criados.

- Comando: `& .\mvnw.cmd clean verify`. Resultado observado: `BUILD SUCCESS`; Testcontainers 2.0.2 iniciou PostgreSQL 17.11 e o contexto Spring Boot conectou ao banco sem falhas.

- Comandos: `docker compose up --build -d`; `docker compose ps`; `docker compose logs app --tail 100`; `curl.exe -i http://localhost:8080/`. Resultado observado: imagem construída, PostgreSQL `healthy`, aplicação em execução e HTTP 404 na raiz, esperado antes dos endpoints da T-02.

## Evidências T-02

- Migration `V1__create_events.sql`, domínio sem frameworks, portas/casos de uso, adapter JDBC e endpoints em `src/main/java`; testes em `EventApiIT`.

- Comando: `& .\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Flyway aplicou 1 migration e Failsafe executou 5 testes, sem falhas ou erros.

- Cobertura: criação 201/Location/PENDING, payload persistido, consulta 200, rejeição 400 sem persistência e consulta 400/404.

## Evidências T-03

<!-- Evidência histórica da T-03 consolidada abaixo. -->

- Contrato paginado documentado; `GET /events` usa `LIMIT/OFFSET` e ordenação `created_at DESC, id DESC`.

- `EventApiIT` cobre padrões, duas páginas, empate, vazio, página além, limite 100 e rejeições.

- Comando: `& .\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou `EventApiIT` com 8 testes e `WebhookLabApplicationIT` com 1, todos sem falhas/erros; Testcontainers iniciou PostgreSQL 17.11.

- Contrato paginado documentado; `GET /events` usa `LIMIT/OFFSET` e ordenação `created_at DESC, id DESC`; testes HTTP cobrem padrões, duas páginas, empate, vazio, página além, limite 100 e rejeições.

- Comando: `& .\mvnw.cmd clean verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou `EventApiIT` com 8 testes e `WebhookLabApplicationIT` com 1, todos sem falhas/erros; Testcontainers iniciou PostgreSQL 17.11.

- Comandos manuais: `docker compose up --build -d`; `Invoke-RestMethod` para POST de `ORDER_CREATED`; `Invoke-RestMethod "http://localhost:8080/events?page=0&size=20"`; `ConvertTo-Json -Depth 10`. Resultado observado pelo usuário: container iniciado, PostgreSQL saudável, evento `PENDING` criado, `page=0`, `size=20`, `totalElements=1`, `totalPages=1` e payload completo visualizado.

## Evidências T-04

- Migration `V2__create_delivery_attempts.sql` com FK para `events`, `UNIQUE(event_id)`, estados de resultado e timestamps; modelo, portas e claim transacional implementados sem acoplar o domínio a frameworks.

- Comando: `& .\mvnw.cmd -q '-Dtest=DeliveryAttemptIT' test`. Resultado observado: `Tests run: 7, Failures: 0, Errors: 0`; PostgreSQL 17.11 iniciou via Testcontainers.

- Cobertura: PENDING→SENDING com tentativa STARTED, concorrência com um único envio, HTTP 409, rejeição de SENDING/DELIVERED/FAILED, rollback do claim, envio fora da transação e preservação de SENDING/STARTED quando o resultado externo é desconhecido.

- Comando: `& .\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou 16 testes, sem falhas/erros; Flyway validou e aplicou V1 e V2 em PostgreSQL 17.11.

- Comandos reproduzidos pelo usuário: `docker ps`; `& .\mvnw.cmd verify`. Resultado informado: PostgreSQL `healthy`, aplicação `Up` e verificação concluída com sucesso.

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

- Comando: `& .\mvnw.cmd verify`. Resultado observado em 06/10/2026: `BUILD SUCCESS`; 24 testes, 0 falhas e 0 erros, incluindo 2 testes de `DurableHistoryIT` e 1 de `ApiExceptionsTest`.

Preencher, por tarefa: requisito atendido, arquivos alterados, comando
executado, resultado observado e limitações. Não marcar concluída apenas
por existir código ou por o agente afirmar sucesso.

## Evidências T-07

- Arquivos alterados: `Dockerfile`, `docker-compose.yml`, `pom.xml`, `application.yml`, `OpenApiConfig`, controllers anotados, `README.md` e `docs/demo.md`.

- `docker compose config` validou app, PostgreSQL e WireMock, healthchecks, volume `webhooklab-postgres` e `DELIVERY_TARGET_URL=http://wiremock:8080/webhook`.

- `docker compose up --build -d` construiu a imagem pelo multi-stage Maven/Java 21; `docker compose ps` observou os três serviços `healthy`.

- Readiness retornou `UP`; OpenAPI 3.1 e Swagger UI responderam; WireMock Admin retornou `healthy`.

- Demonstração real: PENDING; 200→`DELIVERED/SUCCEEDED`; 500→`FAILED/HTTP_ERROR/500`; atraso 4000 ms→`FAILED/TIMEOUT`, `durationMs=3001` e status HTTP nulo; repetição 409 sem aumento de requests (6 antes/depois); restart apenas de app preservou `DELIVERED/SUCCEEDED`.

- `& .\mvnw.cmd verify` com acesso ao Docker: `BUILD SUCCESS`; 24 testes, 0 falhas e 0 erros.

- Confirmação do usuário em 07/10/2026: a execução local seguindo o README funcionou.

## T-08 — fechamento local

Os critérios locais foram concluídos; resta observar uma execução
no GitHub Actions, conforme o estado registrado na tabela de progresso.

Implementada localmente com Surefire/Failsafe estritos, Spotless no
`verify`, workflow GitHub Actions, documentação e rastreabilidade
revisadas. A execução remota do workflow ainda não foi observada;
a validação da V1 permanece pendente até essa evidência.

## T-09 — refatoração de legibilidade e simplificação da V1

Estado: **Pendente**.

O autor informou que reverteu as alterações da execução anterior.
Executar novamente T-09.1 a T-09.5 e verificar todos os critérios
desta versão. Resultados da execução revertida não comprovam
a implementação atual.

Objetivo: aplicar o feedback do code review para reduzir duplicação
e abstrações redundantes, mantendo o comportamento da V1, e
construir/verificar a aplicação pelo Docker sem depender de Maven
Wrapper nem de Maven instalado na máquina.

Requisitos preservados: **RF-01 a RF-08**.

Escopo definitivo desta execução:

- Remover `.mvn/wrapper/`, `mvnw` e `mvnw.cmd`.
- Usar Maven dentro de containers para construir e verificar a aplicação.
- Manter `pom.xml` e o workflow de CI.
- Substituir qualquer orientação anterior de preservar o Wrapper.
- Preservar como histórico os comandos antigos das evidências T-01 a T-08.

Dependências: histórico e erros da T-06, execução containerizada
da T-07 e critérios locais da T-08. A observação do workflow remoto
permanece como pendência da T-08 e não impede iniciar a refatoração local.

### Subtarefas

| Tarefa | Entrega | Aceite / verificação | Estado |
| --- | --- | --- | --- |
| T-09.1 | Inspecionar código, specs, testes, transações, build e configuração Docker; registrar o estado antes da mudança | Conferir `AGENTS.md`, requisitos/design, `pom.xml` e workflow; executar a verificação inicial com Docker disponível e registrar falhas ou limitações preexistentes | Pendente |
| T-09.2 | Separar SQL e mapeamento JDBC; reutilizar construção de `Event`; esclarecer o nome e as etapas do claim | REF-01 e REF-02; consulta preserva `LEFT JOIN`, campos nulos e resultado vazio; concorrência, rollback e fronteiras transacionais preservados | Pendente |
| T-09.3 | Organizar exceções e imports; explicitar o envelope; consolidar registro, consulta e listagem | REF-03 a REF-05; `EventService` concreto, entrega separada e contratos públicos preservados; composição de beans e testes atualizados | Pendente |
| T-09.4 | Migrar o builder para imagem com Maven/JDK 21; implementar verificação via Docker; remover Wrapper e ajustar Compose, CI e documentação | REF-06; build, testes e execução independem de `.mvn/wrapper/`, `mvnw`, `mvnw.cmd` e Maven instalado no host; reconstruir e testar a imagem com o código atual | Pendente |
| T-09.5 | Atualizar design, ADR, instruções do projeto, rastreabilidade e notas de IA; verificar e revisar o diff | REF-07; requisitos apontam para testes reais, comandos e resultados observados; limitações explicitadas | Pendente |

### Critérios de aceite

- [ ] **REF-01 — JDBC legível:** SQL em text block ou constante adequada; execução separada dos mappers; conversão de `Event` reutilizada onde houver duplicação. `findById` continua retornando evento com tentativa nula quando ela não existe e `Optional.empty()` para ID ausente, sem consultas adicionais por registro. `completedAt`, `durationMs` e `httpStatus` nulos continuam nulos.

- [ ] **REF-02 — Claim claro e atômico:** `start`, quando representar a reclamação da entrega, recebe nome como `claimForDelivery` e delega etapas que ajudem a leitura. Preservar a atualização condicional de `PENDING` para `SENDING` e a criação de `STARTED` na mesma transação, com rollback se a inserção falhar. Evento ausente continua 404; evento não elegível, 409. O HTTP permanece fora da transação; evento e tentativa são concluídos juntos em outra transação. Não depender de chamada interna a método anotado para abrir uma transação via proxy.

- [ ] **REF-03 — Exceções e imports:** exceções próprias em arquivos próprios, na camada correspondente; `MalformedUuidException` no adapter web; exceção específica para evento inexistente quando a implementação usar erro genérico. Usar imports, inclusive de `DataAccessException`, sem levar tipos Spring ao domínio. Preservar a tradução dos erros e o contrato `status`, `code`, `detail`.

- [ ] **REF-04 — Envelope explícito:** usar DTO/record `WebhookEnvelope`, ou representação equivalente existente, no adapter HTTP. Reutilizar o mapper configurado e preservar `id`, `eventType`, `createdAt`, `payload` e `X-Webhook-Event-Id`. Payload continua objeto JSON, inclusive vazio ou aninhado. Identificadores novos em inglês; `envelope` já é um nome válido em inglês e `createObjectNode()` não exige substituição apenas por ser uma factory.

- [ ] **REF-05 — Serviços simples:** consolidar `register`, `get` e `list` em um `EventService` concreto; remover interfaces de entrada que existam apenas como pares redundantes de delegação. Manter a entrega separada no `DeliverEventService` existente, ou equivalente. Preservar portas de saída úteis para persistência/HTTP, domínio sem frameworks e controllers sem SQL ou orquestração de entrega. Não criar um novo par `EventService`/`EventServiceImpl` sem necessidade real.

- [ ] **REF-06 — Docker sem Maven Wrapper:** adaptar o Dockerfile multi-stage para usar uma imagem com Maven e JDK 21 no build, executando `mvn` dentro do container. A imagem final executa o JAR com JRE 21 e mantém os healthchecks necessários. Remover `.mvn/wrapper/`, `mvnw` e `mvnw.cmd` do repositório; remover suas referências ativas de Dockerfile, scripts, workflow e documentação. Manter `pom.xml` e qualquer outra configuração `.mvn` realmente necessária. Implementar verificação via Docker com testes unitários, integração e Spotless, mantendo CI. Comprovar build e execução do código atual sem Wrapper nem Maven instalado no host. A remoção é obrigatória; migração para Wrapper `only-script` não atende a este objetivo.

- [ ] **REF-07 — SDD alinhado ao código:** atualizar `design.md`, `traceability.md`, este `tasks.md` e `docs/ai-notes.md`. Registrar a simplificação dos serviços e a manutenção das portas úteis em ADR com o próximo número disponível. Ajustar instruções locais que imponham interface por use case, preservando as regras de domínio e transações. Evidências incluem caminhos reais, comandos, resultados e limitações.

### Regras preservadas

- Registro separado da entrega; entrega manual, síncrona e com uma tentativa por evento. Manter `UNIQUE(event_id)` e não executar retries ou redirects automáticos.

- Preservar validações, paginação, ordenação estável, códigos HTTP e mensagens públicas existentes. Falha de persistência permanece 503; falha do destino registrada permanece 201 na operação local de entrega.

- Manter os estados, resultados, UTC, `Clock` e cálculo monotônico de duração. Tentativas `STARTED` conservam os campos finais nulos; resultado desconhecido não gera reenvio nem retorno automático a `PENDING`.

- Manter JDBC e as dependências compatíveis existentes. Não alterar migrations aplicadas, schema, campos JSON ou regras de negócio durante esta tarefa.

- Usar métodos ou mappers dedicados conforme a necessidade, sem criar hierarquias genéricas ou uma interface para cada método extraído.

### Verificação planejada

Os comandos e cenários abaixo são planejados para esta nova execução
da T-09. A implementação, o build da imagem e a verificação via Docker
devem ser executados e registrados no repositório local.

#### Execução Docker exigida pelo REF-06

- Fixar a versão da imagem oficial Maven com JDK 21, escolhendo uma versão compatível com o build existente. Fixar também uma versão explícita do runtime JRE 21; não usar `latest` como política de versão.

- O builder usa o `pom.xml` e os fontes do repositório, sem copiar ou chamar Wrapper e sem exigir um JAR previamente construído no host. Maven permanece no ambiente de build; a imagem final contém o necessário para executar a aplicação.

- Criar um serviço `verify`, no profile `tools` do Compose, que execute `mvn -B -ntp verify` em container com a mesma versão de Maven/JDK do builder. Disponibilizar os relatórios no workspace para inspeção e publicação no CI.

- Configurar acesso ao Docker Engine para Testcontainers no serviço `verify` e documentar os ajustes necessários para Docker Desktop e para o runner de CI. Quando os testes usarem volumes, conferir a correspondência dos caminhos entre host e container. No Docker Desktop, configurar o host acessível pelos containers conforme a documentação do Testcontainers.

- O build da imagem e a verificação completa têm finalidades diferentes. Se o empacotamento pular testes, isso não constitui aceite: o comando `verify` deve executar a suíte real com PostgreSQL/WireMock, sem skips usados para declarar aprovação.

- Atualizar o workflow existente para usar o comando de verificação via Docker, publicar os relatórios e falhar se testes ou Spotless falharem. Preservar CI; não exigir a execução remota para alegar apenas uma validação local.

- Atualizar README, `docs/demo.md`, design, instruções locais e scripts ativos para os novos comandos. Preservar como históricos os comandos com `mvnw` registrados nas evidências de T-01 a T-08.

- Reaproveitar PostgreSQL, WireMock, volume e healthchecks do Compose. Uma imagem customizada do fake não é exigida por esta correção, cujo objetivo é remover o Wrapper do build da aplicação.

#### Cenários de regressão

Reutilizar as classes registradas nas evidências anteriores e confirmar
seus caminhos e cenários na implementação real. Atualizar referências
quando a refatoração alterar nomes. Acrescentar testes somente onde
faltar cobertura de comportamento afetado; não criar testes para imports,
nomes privados ou placeholders.

| Requisito | Cenário | Referência existente a conferir | Estado da verificação T-09 |
| --- | --- | --- | --- |
| REF-01; RF-02/RF-08 | Evento sem tentativa, ID inexistente, tentativa STARTED com campos finais nulos e histórico concluído | `EventApiIT` e `DurableHistoryIT`; localizar e completar lacunas de mapeamento | Não executada |
| REF-02/REF-05; RF-04 | Reclamação concorrente: um vencedor, uma tentativa e um envio; conflito 409 | `DeliveryAttemptIT` e `DeliveryHttpIT` | Não executada |
| REF-02; RF-04 | Falha de inserção reverte claim; HTTP fora da transação; conclusão atômica | `DeliveryAttemptIT` e `DeliveryHttpIT`; conferir que o rollback usa PostgreSQL real | Não executada |
| REF-03/REF-05; RF-02/RF-04 | UUID inválido, evento ausente, conflito e falha de persistência preservam respostas 400/404/409/503 | `ApiExceptionsTest` e testes HTTP existentes | Não executada |
| REF-04; RF-05/RF-06/RF-07 | Envelope/header preservados, payload objeto; sucesso, erro HTTP, timeout e conexão | `DeliveryHttpIT`; completar casos de payload vazio/aninhado se ausentes | Não executada |
| REF-01 a REF-05; RF-01 a RF-08 | Regressão de registro, consulta, paginação e histórico durável | `EventApiIT`, `DeliveryAttemptIT`, `DeliveryHttpIT`, `DurableHistoryIT` e demais testes do projeto | Não executada |
| REF-06 | Construir e executar a imagem sem Wrapper nem JAR local prévio; rodar testes/Spotless via Docker e preservar CI | Dockerfile multi-stage, serviço `verify` no profile `tools`, workflow, README e `docs/demo.md` | Não executada |
| REF-07 | Diff, ADR, design, instruções e rastreabilidade coerentes | Revisão documental e relatórios reais de testes | Não executada |

Comandos planejados, após conferir a configuração real:

- Verificação inicial, antes da remoção: usar o caminho de build existente e registrar o resultado como baseline. Depois da migração, o fluxo documentado e o CI não dependem de `mvnw`.

- Novo comando a implementar: `docker compose --profile tools run --rm verify`. Conferir a execução de Surefire/Failsafe e Spotless, o código de saída e os relatórios. Testcontainers precisa acessar o Docker Engine; não declarar aprovação se a integração não executar.

- Validar configuração com `docker compose config`, reconstruir e subir o código atual com `docker compose up --build -d`, conferir os serviços e executar a demonstração conforme README/`docs/demo.md`. A validação da T-07 é histórica e não substitui a validação da nova imagem.

- Confirmar no diff que os limites das transações e os contratos foram preservados. Registrar a execução remota do workflow separadamente, somente quando observada.

Não usar a contagem histórica de 24 testes como resultado da T-09.
Preencher o total observado na nova execução e conferir quais cenários
foram realmente executados.

### Evidências T-09

Evidências desta nova execução ainda não registradas.
Preencher durante a implementação da T-09 completa:

- Requisitos atendidos e subtarefas concluídas: pendente.
- Arquivos alterados e decisões adotadas: pendente.
- Comandos executados e relatórios de testes: não executados.
- Resultados observados: não registrados.
- Build sem Wrapper, testes via Docker e demonstração da nova imagem: pendentes.
- Limitações e pendências: registrar as condições reais; a execução remota da T-08 permanece pendente até ser observada.

Concluir a T-09 somente após os critérios aplicáveis e a regressão
estarem verificados, com documentação e rastreabilidade atualizadas.

A pendência remota da T-08 deve continuar explícita, sem atribuir
a esta edição documental evidências de execução.

Referências para o REF-06:

- [Imagem oficial Maven](https://hub.docker.com/_/maven)
- [Docker: build em múltiplas etapas](https://docs.docker.com/build/building/multi-stage/)
- [Testcontainers: testes executados dentro de containers](https://java.testcontainers.org/supported_docker_environment/continuous_integration/dind_patterns/)

## Fechamento observado da T-09

As subtarefas T-09.1 a T-09.5 foram concluídas localmente. Os critérios REF-01 a REF-07 foram verificados na implementação e na execução final via Docker. A rodada final observou Surefire com 1 teste, Failsafe com 25 testes, Spotless limpo e `BUILD SUCCESS`.

Evidências adicionais observadas: `docker compose --profile tools config`; `docker compose --profile tools run --rm verify`; `docker compose build --no-cache app`; `docker compose up -d --force-recreate`; readiness 200; POST `/events` com `eventType`/`payload`, primeira entrega 201 e segunda entrega 409. Os relatórios permaneceram em `target/surefire-reports` e `target/failsafe-reports`.

Os históricos T-01 a T-08 permanecem intactos. A execução remota do T-08 no GitHub Actions continua pendente por não ter sido observada.

Checklist final observado: [x] REF-01; [x] REF-02; [x] REF-03; [x] REF-04; [x] REF-05; [x] REF-06; [x] REF-07.
