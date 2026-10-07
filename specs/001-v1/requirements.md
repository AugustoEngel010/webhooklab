# Requisitos — WebhookLab V1

Status: definidos para orientar a implementação; testes ainda pendentes.

## 1. Objetivo

Construir um laboratório local de entrega de eventos HTTP. Um evento é registrado, enviado manualmente para um destino simulado e acompanhado por um histórico persistente. O projeto usa Java, arquitetura hexagonal e desenvolvimento assistido por IA, com vínculo entre requisitos e testes.

O primeiro marco é registrar e consultar um evento no PostgreSQL. O segundo adiciona uma única tentativa de entrega. A primeira demonstração usa Swagger UI; um painel simples poderá consumir a mesma API depois que esse fluxo estiver funcionando.

## 2. Escopo da V1

- Registrar evento com tipo e payload JSON.
- Consultar um evento e listar eventos com paginação.
- Disparar manualmente uma tentativa de entrega por evento.
- Registrar horário, duração, resultado e status HTTP da tentativa.
- Demonstrar sucesso, erro HTTP e timeout com WireMock.

Retentativas automáticas, agendamento, autenticação, destinos configuráveis pela API, assinatura de webhook e prevenção de duplicidade na entrada serão evoluções posteriores. Cada POST de registro cria um novo evento na V1. A única tentativa refere-se ao evento registrado, não a uma garantia de processamento único pelo destino.

## 4. Requisitos — requirements.md

| ID | Requisito | Critérios de aceite |
| --- | --- | --- |
| RF-01 | Registrar evento | Tipo não vazio, até 80 caracteres após trim, e payload JSON objeto produzem HTTP 201, Location e evento PENDING; UUID e createdAt são gerados pelo servidor |
| RF-02 | Consultar evento | Retornar evento e tentativa, se houver; ID inexistente resulta em 404 e UUID malformado em 400 |
| RF-03 | Listar eventos | Paginação padrão page=0 e size=20, máximo 100; ordenar por createdAt decrescente e ID; parâmetros inválidos resultam em 400 |
| RF-04 | Iniciar entrega única | Apenas PENDING pode iniciar a tentativa; chamadas concorrentes para o mesmo evento resultam em uma tentativa iniciada e rejeição 409 das demais |
| RF-05 | Registrar sucesso | Resposta HTTP 2xx do destino resulta em evento DELIVERED e tentativa SUCCEEDED |
| RF-06 | Registrar erro HTTP | Resposta HTTP fora de 2xx resulta em evento FAILED e tentativa HTTP_ERROR, com o status recebido |
| RF-07 | Registrar falha de transporte | Timeout ou erro de conexão resultam em evento FAILED e tentativa TIMEOUT ou CONNECTION_ERROR; httpStatus fica nulo |
| RF-08 | Consultar histórico durável | Evento e tentativa permanecem consultáveis após reiniciar a aplicação; duração, início e término são registrados |

Entradas inválidas, incluindo payload nulo ou que não seja um objeto JSON, resultam em 400 e não são persistidas. Objeto JSON vazio é permitido.

A V1 não segue redirecionamentos HTTP nem executa retentativas internas do cliente. Falhas na entrega ficam registradas e podem ser consultadas; uma segunda chamada de entrega do mesmo evento, inclusive FAILED, retorna 409.

DELIVERED significa que o destino respondeu 2xx. TIMEOUT significa que o emissor não recebeu uma resposta dentro do prazo; não permite concluir se o destino processou o evento.

## 5. Contrato HTTP inicial

| Método e rota | Operação | Resposta |
| --- | --- | --- |
| POST /events | Registrar evento | 201 com o evento PENDING |
| GET /events/{id} | Consultar evento e tentativa | 200 / 400 / 404 |
| GET /events?page=0&size=20 | Listar eventos | 200 / 400 |
| POST /events/{id}/deliver | Criar e executar a única tentativa | 201 com resultado da tentativa / 400 / 404 / 409 |

A operação deliver é síncrona na V1: a resposta ocorre depois de obter o resultado do destino ou atingir o timeout. O 201 confirma a criação e o registro da tentativa, inclusive quando seu resultado é uma falha. O consumidor identifica o resultado pelo campo outcome e pelo estado do evento.

Exemplo de registro:

```json
{
  "eventType": "ORDER_APPROVED",
  "payload": {
    "orderId": "DEMO-001",
    "amount": 150.00
  }
}
```

O envio ao destino usa um envelope contendo id, eventType, createdAt e payload. Adicionar o mesmo id no header `X-Webhook-Event-Id` para correlacionar os registros.

Resposta de consulta: id, eventType, payload, status, createdAt e attempt. Antes da entrega, attempt é nulo. Após o início, attempt contém id, startedAt, completedAt, durationMs, outcome e httpStatus. Campos de término permanecem nulos enquanto a tentativa está STARTED.

Erros da API usam um formato único: status, code e detail. Falhas de persistência resultam em 503; não são confundidas com falhas do destino. O destino recebe um timeout de conexão de 1 segundo e um prazo configurado para resposta de 3 segundos. Testes unitários usam uma porta HTTP fake; cenários reais de timeout usam WireMock.
### Contrato da listagem

`GET /events` retorna um envelope JSON com `content` (lista de eventos incluindo `attempt`, nulo quando ainda não há tentativa), `page`, `size`, `totalElements` e `totalPages`. Quando houver tentativa, `attempt` contém id, startedAt, completedAt, durationMs, outcome e httpStatus. `page` e zero-based; `totalPages` e zero quando nao ha eventos. Uma pagina alem dos resultados retorna HTTP 200 com `content: []` e os metadados solicitados. A paginacao deve ser executada no PostgreSQL.
