# WebhookLab — orientações para o Codex

## Contexto

- Mini projeto backend Java 21, local e publicável no GitHub.
- Arquitetura hexagonal em um único projeto Maven; pacote base dev.augusto.webhooklab.
- Fonte de requisitos: specs/001-v1/requirements.md.
- Estado inicial: diretórios e documentos; bootstrap Java e testes pendentes.

## Workflow

- Ler a tarefa atual e o design antes de editar código.
- Aplicar a skill .agents/skills/webhooklab-sdd/SKILL.md em tarefas de implementação ou revisão.
- Respeitar o modo solicitado pelo usuário: plano, implementação ou revisão.
- Implementar por tarefa e vincular os critérios aos testes.
- Não inventar regras de negócio; manter as mudanças de comportamento refletidas na spec.
- Na T-01, preservar os documentos e a skill ao gerar os arquivos do Spring Boot.

## Arquitetura

- Manter domain sem Spring, JPA, Jackson ou HTTP.
- Definir portas em application/port e casos de uso em application/usecase.
- Separar DTOs HTTP e entidades JPA dos modelos de domínio.
- Versionar schema com Flyway; configurar Hibernate para validar.
- Usar Clock injetado para datas e relógio monotônico para duração.
- Manter HTTP fora da transação e reclamar o envio de forma atômica.

## Verificação e evidência

- Conferir pom.xml e comandos disponíveis antes de executá-los.
- Configurar testes unitários *Test e testes de integração *IT na etapa apropriada.
- Testar persistência e migrations com PostgreSQL real via Testcontainers.
- Usar WireMock para os cenários HTTP da entrega.
- Registrar em traceability.md o teste, o comando e o resultado observado.
- Manter a tarefa pendente ou em andamento se a verificação estiver bloqueada.
- Não declarar comandos executados, testes aprovados ou aplicação funcional sem evidência.

## Code Review Rules

- Conferir o envio único por evento na V1 e a rejeição de segundo disparo.
- Conferir a concorrência na transição PENDING para SENDING.
- Conferir atualização conjunta do evento e da tentativa ao concluir a entrega.
- Conferir distinção entre falha do banco, erro HTTP e timeout do destino.
- Não reclassificar automaticamente resultado desconhecido nem apagar histórico.
