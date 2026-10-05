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

## EvidÃªncia T-03

- Contrato paginado documentado; `GET /events` usa `LIMIT/OFFSET` e ordenaÃ§Ã£o `created_at DESC, id DESC`.
- `EventApiIT` cobre padrÃµes, duas pÃ¡ginas, empate, vazio, pÃ¡gina alÃ©m, limite 100 e rejeiÃ§Ãµes.
- Comando: `& .\\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Failsafe executou `EventApiIT` com 8 testes e `WebhookLabApplicationIT` com 1, todos sem falhas/erros; Testcontainers iniciou PostgreSQL 17.11.
| T-04 | T-02 | Pendente |
| T-05 | T-04 | Pendente |
| T-06 | T-03 e T-05 | Pendente |
| T-07 | T-06 | Pendente |
| T-08 | Build disponível; concluir antes da entrega V1 | Pendente |

## Evidências

- T-01: bootstrap Spring Boot Java 21, Maven Wrapper, configuração PostgreSQL/Flyway/Hibernate, `Dockerfile`, `docker-compose.yml` e teste `WebhookLabApplicationIT` criados.
- Comando: `& .\mvnw.cmd clean verify`. Resultado observado: `BUILD SUCCESS`; Testcontainers 2.0.2 iniciou PostgreSQL 17.11 e o contexto Spring Boot conectou ao banco sem falhas.
- Comandos: `docker compose up --build -d`; `docker compose ps`; `docker compose logs app --tail 100`; `curl.exe -i http://localhost:8080/`. Resultado observado: imagem construída, PostgreSQL `healthy`, aplicação em execução e HTTP 404 na raiz, esperado antes dos endpoints da T-02.
- T-02: migration `V1__create_events.sql`, domínio sem frameworks, portas/casos de uso, adapter JDBC e endpoints em `src/main/java`; testes em `EventApiIT`.
- Comando: `& .\\mvnw.cmd verify`. Resultado observado: `BUILD SUCCESS`; Flyway aplicou 1 migration e Failsafe executou 5 testes, sem falhas ou erros.
- T-02 cobre criação 201/Location/PENDING, payload persistido, consulta 200, rejeição 400/sem persistência e consulta 400/404. T-04 em diante permanecem pendentes.

Preencher, por tarefa: requisito atendido, arquivos alterados, comando executado, resultado observado e limitações. Não marcar concluída apenas por existir código ou por o agente afirmar sucesso.
