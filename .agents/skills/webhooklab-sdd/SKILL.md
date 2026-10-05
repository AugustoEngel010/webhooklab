---
name: webhooklab-sdd
description: Implementar ou revisar tarefas SDD do WebhookLab em Java, com arquitetura hexagonal, critérios de aceite e rastreabilidade requisito-teste. Usar ao trabalhar nas specs do WebhookLab, escolher sua próxima tarefa ou verificar uma entrega. Não usar como processo genérico para outros projetos.
---

# WebhookLab SDD

## Localizar a tarefa

1. Localizar a raiz do WebhookLab pelo README e pela pasta `specs/001-v1/`; usar a raiz Git quando disponível.
2. Ler `AGENTS.md` e `specs/001-v1/tasks.md`. Respeitar o modo pedido: planejamento, implementação ou revisão.
3. Selecionar a tarefa solicitada. Se não houver ID, escolher a primeira pendente cujas dependências estejam satisfeitas.
4. Ler os requisitos e o design relevantes, além das linhas correspondentes em `traceability.md`. Abrir apenas os arquivos de código necessários.
5. Confirmar quais critérios a tarefa precisa demonstrar. Identificar ambiguidades que alterem comportamento e resolvê-las com o usuário; decidir detalhes técnicos rotineiros e documentá-los quando necessário.

## Executar a mudança

- Apresentar um plano curto ligado ao ID da tarefa e aos requisitos. Em pedidos de implementação, continuar com a execução autorizada; em pedidos de plano, entregar o plano.
- Preservar as decisões da V1: registro separado da entrega; envio manual, síncrono e com uma tentativa por evento; destino único por configuração.
- Manter domínio sem dependências de Spring, JPA, Jackson ou HTTP. Definir portas na aplicação e adaptar frameworks nas bordas.
- Usar migrations versionadas; não alterar migration já aplicada nem usar Hibernate para criar o schema.
- Manter o HTTP fora da transação do banco. Reclamar a entrega de forma atômica antes de enviar, conforme o design.
- Preservar o histórico quando o resultado externo for desconhecido. Não reclassificar automaticamente SENDING como PENDING.
- Limitar a mudança à tarefa e às correções necessárias para que ela funcione. Registrar mudanças de comportamento na spec antes de implementá-las.
- Na T-01, gerar o bootstrap Java sem sobrescrever specs, AGENTS.md ou a skill existente.

## Verificar e registrar

1. Ler [verification.md](references/verification.md) para selecionar verificações pertinentes à mudança.
2. Definir cenários a partir dos critérios de aceite. Revisar as asserções dos testes gerados e conferir resultados positivos, negativos e limites relevantes.
3. Executar comandos que existam no projeto e revisar o diff. Informar claramente verificações que o ambiente não permitiu executar.
4. Atualizar `traceability.md` com requisito, cenário e teste realmente implementado. Preencher evidência apenas com comandos e resultados observados.
5. Atualizar a tarefa para concluída somente quando seus critérios e verificações estiverem satisfeitos. Manter em andamento ou pendente quando faltar evidência.
6. Registrar decisões ou correções relevantes em `docs/ai-notes.md`; criar ADR apenas quando houver decisão arquitetural.
7. Entregar um resumo do resultado, IDs atendidos, verificação realizada e limitações concretas. Nunca declarar testes aprovados ou comandos disponíveis sem conferir.
