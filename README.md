# WebhookLab

Laboratório local de entrega de eventos HTTP, desenvolvido com Java e Spec-Driven Development.

## Estado atual

Bootstrap da aplicação Java, Maven Wrapper, Compose PostgreSQL e teste de contexto criados na T-01. A execução do build e a inicialização local ainda precisam ser verificadas em um ambiente com JDK 21, Docker e rede para baixar dependências.

Primeiro marco: registrar e consultar eventos com persistência no PostgreSQL.
V1 completa: envio manual com uma tentativa por evento, histórico e demonstração de sucesso, erro HTTP e timeout.

## Por onde começar

1. Ler `specs/001-v1/requirements.md`, `design.md` e `tasks.md`.
2. Abrir esta pasta no Codex ou executar Codex a partir dela.
3. Solicitar: `Use $webhooklab-sdd para implementar T-01 seguindo a spec da V1.`
4. Revisar o plano, os arquivos gerados e as evidências antes de concluir a tarefa.
5. Implementar T-02: registro e consulta de eventos, com testes usando PostgreSQL real.

## Organização

| Caminho | Responsabilidade |
| --- | --- |
| AGENTS.md | Orientações do projeto para o Codex |
| .agents/skills/webhooklab-sdd/ | Skill de execução e revisão de tarefas SDD |
| specs/001-v1/ | Requisitos, design, tarefas e rastreabilidade |
| docs/adr/ | Decisões arquiteturais |
| docs/ai-notes.md | Aprendizado e correções das sugestões da IA |
| src/main/java/dev/augusto/webhooklab/ | Pacotes da aplicação |
| src/main/resources/db/migration/ | Migrations Flyway |
| src/test/ | Testes de domínio, aplicação e adapters |
| infra/wiremock/mappings/ | Configuração futura do destino simulado |
| .github/workflows/ | Pipeline futuro de verificação |

Pacote base: `dev.augusto.webhooklab`. Um único projeto Maven. Domínio sem frameworks; portas na aplicação; frameworks nos adapters.

## Verificação

Os comandos de build só estarão disponíveis depois de criar o Maven Wrapper na T-01.
Configurar Surefire para *Test e Failsafe para *IT; a verificação completa será `./mvnw verify`.
Registrar apenas resultados observados em tasks.md e traceability.md.

## Publicação no GitHub

Versionar specs, código, instruções e a skill do repositório. Preservar o histórico das entregas e usar dados fictícios nos exemplos.

## Referências

- [Skills no Codex](https://learn.chatgpt.com/docs/build-skills)
- [Orientações em AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
