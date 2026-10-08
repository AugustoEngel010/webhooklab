# Como o SDD funciona no WebhookLab

O objetivo principal do WebhookLab é demonstrar **Spec-Driven Development (SDD)**: o comportamento esperado é escrito e revisado antes de ser implementado; a implementação é dividida em tarefas; os testes verificam os critérios; e a rastreabilidade registra o que foi realmente observado.

SDD não significa escrever documentação depois do código. A especificação funciona como um contrato de trabalho e como uma explicação verificável das decisões do projeto.

## Contexto e skills do agente

O SDD deste projeto não depende apenas de um prompt isolado. O agente recebe camadas de contexto que delimitam como deve trabalhar.

### `AGENTS.md`

É o contexto geral do repositório. Ele informa o objetivo do WebhookLab, o pacote base, a arquitetura, as regras de negócio que não podem ser alteradas, o workflow de verificação e as regras de revisão. Também lembra o agente de não declarar testes ou comandos sem evidência.

### `webhooklab-sdd/SKILL.md`

É a skill específica do projeto, localizada em `.agents/skills/webhooklab-sdd/SKILL.md`. Ela foi criada manualmente para este repositório, como uma instrução reutilizável para tarefas SDD do WebhookLab.

A skill orienta o agente a:

- localizar a raiz e ler `AGENTS.md` e `specs/001-v1/`;
- selecionar a tarefa e ler requisitos, design e rastreabilidade relevantes;
- apresentar um plano curto ligado à tarefa;
- preservar as decisões da V1;
- manter o domínio sem dependências de framework;
- implementar por tarefa e relacionar critérios aos testes;
- executar verificações reais;
- atualizar `tasks.md`, `traceability.md` e `docs/ai-notes.md`;
- não marcar uma tarefa como concluída sem evidência.

### Skill não é código da aplicação

A skill não é carregada pelo Spring Boot, não participa do JAR e não executa em produção. Ela é uma ferramenta de desenvolvimento: ajuda o Codex a interpretar a spec, seguir o processo e manter consistência entre documentos, código e testes.

```text
Contexto do agente             Runtime da aplicação
AGENTS.md                      Spring Boot
webhooklab-sdd/SKILL.md        controllers e casos de uso
requirements/design/tasks      PostgreSQL e Flyway
traceability e ai-notes        WireMock e cliente HTTP
```

### Como o contexto é aplicado

Ao iniciar uma tarefa, o agente deve:

1. ler as instruções gerais do `AGENTS.md`;
2. aplicar a skill específica da tarefa;
3. ler somente os requisitos e arquivos necessários;
4. propor o vínculo entre tarefa, critérios e testes;
5. implementar dentro do escopo autorizado;
6. verificar a mudança no ambiente real quando possível;
7. registrar fatos observados, limitações e decisões.

O contexto reduz ambiguidades, mas não substitui revisão humana. A pessoa responsável pelo projeto continua decidindo se a mudança atende ao objetivo e se a evidência é suficiente.

## O ciclo do projeto

```text
Problema
   ↓
requirements.md  →  regras e critérios de aceite
   ↓
design.md        →  arquitetura e decisões técnicas
   ↓
tasks.md         →  entregas pequenas e dependências
   ↓
código + testes   →  comportamento implementado
   ↓
traceability.md  →  requisito ligado ao teste e à evidência
   ↓
README/demo      →  execução reproduzível por outra pessoa
```

O ciclo pode voltar para uma etapa anterior. Por exemplo, quando o contrato de `GET /events` foi alterado para incluir `attempt`, primeiro `requirements.md` e `design.md` foram atualizados; depois o modelo, o adapter JDBC e o teste foram ajustados.

## O papel de cada arquivo

### `specs/001-v1/requirements.md`

Define o que a V1 deve fazer, sem detalhar a implementação. Contém:

- objetivo e escopo;
- requisitos funcionais, como RF-01 até RF-08;
- critérios de aceite observáveis;
- estados e regras de negócio;
- contrato HTTP e exemplos.

Exemplo: RF-05 diz que uma resposta HTTP 2xx do destino produz evento `DELIVERED` e tentativa `SUCCEEDED`. Isso permite testar o comportamento sem exigir uma classe específica.

### `specs/001-v1/design.md`

Explica como o comportamento será sustentado tecnicamente. Registra a arquitetura hexagonal, separação entre domínio, aplicação e adapters, persistência, transações, Flyway, relógios e decisões de integração.

O design não deve inventar regras que não estejam no requisito. Ele transforma os requisitos em uma solução técnica coerente.

### `specs/001-v1/tasks.md`

Divide a entrega em unidades incrementais com dependências e verificações. A V1 foi organizada assim:

- T-01: bootstrap;
- T-02: registro e consulta;
- T-03: paginação;
- T-04: reclamação atômica da entrega;
- T-05: envio HTTP e conclusão;
- T-06: histórico durável e erros;
- T-07: Swagger, Compose e demonstração;
- T-08: CI, formatação e revisão final.

Uma tarefa só deve ser marcada como concluída quando o critério tiver evidência. Código existente, por si só, não é evidência.

### `specs/001-v1/traceability.md`

É a matriz requisito → cenário → teste → comando → resultado. Ela responde:

1. Qual requisito foi atendido?
2. Qual cenário prova isso?
3. Qual teste ou verificação foi executado?
4. Com qual comando?
5. O que foi observado?

Por exemplo, RF-07 é ligado aos cenários de timeout e erro de conexão de `DeliveryHttpIT`, ao comando `docker compose --profile tools run --rm verify` e ao resultado persistido com `httpStatus` nulo.

### `docs/adr/`

Contém decisões arquiteturais que merecem explicação própria. Exemplos do WebhookLab: entrega manual e síncrona, domínio sem frameworks e reclamação atômica antes do envio HTTP.

### `docs/ai-notes.md`

Registra decisões, correções e limitações encontradas durante o desenvolvimento assistido por IA. Não é um diário de respostas; deve conter observações concretas, como um healthcheck corrigido ou uma limitação do ambiente de testes.

## Como os testes participam do SDD

Os testes não são escritos apenas para aumentar cobertura. Cada teste deve demonstrar um critério da spec:

| Camada | Exemplo | O que prova |
| --- | --- | --- |
| Unitário | `ApiExceptionsTest` | Contrato público de erros |
| Integração HTTP | `EventApiIT` | API, PostgreSQL, paginação e persistência |
| Concorrência | `DeliveryAttemptIT` | Uma única reclamação e uma única tentativa |
| Destino HTTP | `DeliveryHttpIT` | 2xx, erro HTTP, timeout e conexão |
| Durabilidade | `DurableHistoryIT` | Histórico após nova instância da aplicação |
| Demonstração | Compose + WireMock | Execução real pelo README |

A regra prática é: o teste deve falhar se o comportamento definido na spec deixar de existir, e a evidência deve registrar a execução real, não apenas a intenção do teste.

## Como a IA é usada

Neste projeto, a IA atua como agente de implementação e revisão, mas não como fonte única de verdade. O fluxo recomendado é:

1. informar a tarefa e pedir a leitura da spec;
2. revisar o plano e os critérios de aceite;
3. implementar somente o escopo da tarefa;
4. executar os comandos disponíveis;
5. revisar o diff e os testes;
6. atualizar a rastreabilidade com resultados observados;
7. manter a tarefa pendente quando a verificação estiver bloqueada.

A skill `.agents/skills/webhooklab-sdd/SKILL.md` formaliza esse processo para o Codex. O `AGENTS.md` acrescenta as regras específicas do projeto, como domínio sem Spring, uma tentativa por evento, HTTP fora da transação e preservação de estados desconhecidos.

## Exemplo de mudança orientada pela spec

Quando a listagem retornava `attempt: null` para todos os eventos, o problema foi tratado como mudança de contrato:

1. o requisito da listagem foi atualizado para incluir a tentativa quando existente;
2. o design passou a exigir `LEFT JOIN` com `delivery_attempts`;
3. `EventPage` passou a transportar `EventHistory`;
4. o adapter JDBC passou a carregar evento e tentativa juntos;
5. `EventApiIT` ganhou um cenário com tentativa persistida;
6. `traceability.md` registrou o teste e o resultado.

Esse encadeamento é o que o projeto pretende demonstrar: a mudança não termina no código; ela percorre contrato, design, tarefa, teste, evidência e documentação.

## Como apresentar o SDD em uma demonstração

Uma apresentação curta pode seguir esta ordem:

1. mostrar `requirements.md` e um requisito, como RF-05;
2. mostrar em `design.md` como a arquitetura suporta esse requisito;
3. mostrar a tarefa correspondente em `tasks.md`;
4. abrir o teste que verifica o cenário;
5. executar o cenário no Compose com WireMock;
6. consultar o resultado persistido;
7. mostrar a linha correspondente em `traceability.md`.

Assim, a audiência vê a cadeia completa: intenção, decisão, implementação, teste e evidência.
