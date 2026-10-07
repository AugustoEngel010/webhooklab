# WebhookLab

## Verificação do build

O build separa testes unitários `*Test` (Surefire) de testes de integração `*IT` (Failsafe). `verify` executa a formatação, a suíte unitária e a suíte de integração; os testes de integração precisam de Docker acessível para PostgreSQL via Testcontainers e WireMock.

```powershell
& .\mvnw.cmd -B verify
```

Para verificar ou corrigir a formatação Java:

```powershell
& .\mvnw.cmd spotless:check
& .\mvnw.cmd spotless:apply
```

O Maven falha se a suíte unitária ou de integração não descobrir testes. Não use `-DskipTests`, `-DskipITs` ou propriedades `spotless.*.skip` ao validar a V1. O workflow [`ci.yml`](.github/workflows/ci.yml) repete `./mvnw -B verify` em pull requests e pushes para `main`, com Java 21, cache Maven e relatórios Surefire/Failsafe mesmo quando o job falha.

O WebhookLab é um laboratório local para entender e testar a entrega de webhooks.

Ele registra um evento JSON, salva esse evento no PostgreSQL e, quando solicitado, envia uma notificação HTTP para um sistema externo simulado pelo WireMock. Depois, salva o resultado da tentativa e permite consultar o histórico.

> **Em uma frase:** a API REST controla o laboratório; o WebhookLab envia o webhook; o WireMock finge ser o sistema externo que recebe esse webhook.

## Processo de desenvolvimento

Este projeto foi desenvolvido com **Spec-Driven Development (SDD)**: os requisitos e decisões de design foram definidos antes da implementação, cada entrega foi dividida em tarefas e os critérios de aceite foram relacionados aos testes e às evidências em `specs/001-v1/`.

Para entender ou apresentar esse processo, consulte [docs/sdd.md](docs/sdd.md). Ele explica como uma ideia vira requisito, tarefa, código, teste e evidência neste projeto.

O processo também é apoiado pelo contexto do agente e pela skill local [`webhooklab-sdd`](.agents/skills/webhooklab-sdd/SKILL.md), criada manualmente para este repositório.

## O problema que o projeto demonstra

Imagine uma loja que acabou de criar um pedido. Ela precisa avisar outro sistema:

```text
"O pedido ORDER-001 foi criado."
```

Em um sistema real, essa mensagem poderia ser enviada para um ERP, gateway de pagamento ou serviço de logística. O WebhookLab demonstra esse processo sem depender de um sistema externo real.

O projeto permite verificar:

- criação e persistência de eventos JSON;
- envio manual e síncrono de um evento para um destino HTTP;
- sucesso, erro HTTP, timeout e erro de conexão;
- rejeição de uma segunda tentativa para o mesmo evento;
- preservação do histórico após reiniciar a aplicação.

## Como as partes se comunicam

```text
Você / Swagger
       |  API REST: POST /events
       v
WebhookLab (aplicação Java)
       |  salva o evento
       v
PostgreSQL

Você / Swagger
       |  API REST: POST /events/{id}/deliver
       v
WebhookLab
       |  webhook: POST /webhook
       v
WireMock (sistema externo simulado)
       |  responde 200, 500 ou demora
       v
WebhookLab
       |  salva o resultado
       v
PostgreSQL
```

### REST e webhook neste projeto

Os endpoints da aplicação são uma API REST usada para controlar o laboratório:

```text
POST /events                 cria um evento
GET  /events/{id}             consulta um evento e seu histórico
GET  /events                  lista eventos com paginação
POST /events/{id}/deliver     solicita uma tentativa de entrega
```

O webhook é a chamada de saída feita pela aplicação para o destino externo:

```http
POST http://wiremock:8080/webhook
```

Na V1, criar o evento não dispara o webhook automaticamente. O envio é solicitado manualmente por `POST /events/{id}/deliver`, para que o fluxo fique explícito e fácil de testar.

## O papel de cada componente

### WebhookLab

É a aplicação principal, escrita em Java 21. Ela valida entradas, controla os estados, envia o webhook e registra o resultado.

### PostgreSQL

É o banco persistente. Ele guarda o evento, o payload JSON, a tentativa de entrega, os horários, a duração e o resultado.

### WireMock

É um servidor HTTP simulado executado em um container Docker. Ele representa o sistema externo que receberia o webhook. Não é o banco e não cria eventos: apenas recebe a chamada e devolve a resposta configurada.

Por exemplo, um mapping pode dizer:

```text
POST /webhook -> HTTP 200
```

Também é possível configurar `500` ou um atraso de quatro segundos para demonstrar falha e timeout.

### Swagger UI

É uma interface visual para executar os endpoints REST no navegador, sem precisar escrever comandos HTTP manualmente.

## Ciclo de vida de um evento

```text
PENDING -> SENDING -> DELIVERED
                    \-> FAILED
```

Ao iniciar a entrega, a aplicação cria uma única tentativa:

```text
STARTED -> SUCCEEDED
        \-> HTTP_ERROR
        \-> TIMEOUT
        \-> CONNECTION_ERROR
```

Na V1, cada evento pode ter somente uma tentativa. Por isso, uma segunda chamada para `/deliver` retorna `409 DELIVERY_CONFLICT` e não gera outro envio.

O `201` retornado por `/deliver` significa que a tentativa foi registrada. O resultado pode ser sucesso ou falha; consulte o evento para ver `outcome` e `httpStatus`.

## Pré-requisitos

- Docker Desktop 29+ (ou Docker Engine com Compose v2).
- Java e Maven não são necessários para executar a stack: o Dockerfile compila com Maven e Java 21 dentro da imagem.
- Para executar `mvnw verify` diretamente no computador, use JDK 21.

## Iniciar o ambiente

Na raiz do projeto, no PowerShell:

```powershell
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Start-Process http://localhost:8080/swagger-ui/index.html
```

Os containers são:

| Componente | Acesso no computador | Uso |
| --- | --- | --- |
| WebhookLab | `http://localhost:8080` | API e Swagger |
| PostgreSQL | `localhost:5432` | Persistência |
| WireMock | `http://localhost:8081` | Admin e consulta das requisições |

Dentro da rede Docker, a aplicação acessa o WireMock por `http://wiremock:8080/webhook`. O nome `wiremock` é o nome do serviço no `docker-compose.yml`; ele não é usado pelo navegador do host.

## Primeiro teste pelo Swagger

Abra:

```text
http://localhost:8080/swagger-ui/index.html
```

Siga esta sequência:

1. Execute `POST /events` com:

   ```json
   {
     "eventType": "ORDER_CREATED",
     "payload": {
       "orderId": "DEMO-001",
       "amount": 150
     }
   }
   ```

2. Copie o `id` retornado.
3. Execute `GET /events/{id}` e confirme o estado `PENDING`.
4. Configure uma resposta `200` no WireMock usando o comando abaixo.
5. Execute `POST /events/{id}/deliver` no Swagger.
6. Execute `GET /events/{id}` novamente.

O resultado esperado é:

```text
event.status       = DELIVERED
attempt.outcome    = SUCCEEDED
attempt.httpStatus = 200
```

## Configurar o WireMock para responder

O endereço `/__admin/requests` apenas consulta chamadas recebidas. Para configurar respostas, use `/__admin/mappings`.

### Resposta 200

```powershell
$mapping = @{
  request = @{
    method = "POST"
    url = "/webhook"
  }
  response = @{
    status = 200
  }
  persistent = $false
}

Invoke-RestMethod `
  -Method Post `
  "http://localhost:8081/__admin/mappings" `
  -ContentType "application/json" `
  -Body ($mapping | ConvertTo-Json -Depth 10)
```

### Resposta 500

Altere o status no mapping para `500`. O resultado esperado no evento será:

```text
event.status       = FAILED
attempt.outcome    = HTTP_ERROR
attempt.httpStatus = 500
```

### Timeout

Configure também um atraso:

```json
{
  "request": { "method": "POST", "url": "/webhook" },
  "response": { "status": 200, "fixedDelayMilliseconds": 4000 }
}
```

O cliente da aplicação espera aproximadamente três segundos. Portanto, o resultado será `TIMEOUT`, sem status HTTP.

Antes de trocar de cenário, remova os mappings anteriores:

```powershell
Invoke-RestMethod -Method Delete "http://localhost:8081/__admin/mappings"
```

Use um evento novo para cada cenário. Na V1, um evento que já teve uma tentativa não pode ser reenviado.

## Conferir o que o WireMock recebeu

Abra no navegador:

```text
http://localhost:8081/__admin/requests
```

Ou consulte pelo PowerShell:

```powershell
Invoke-RestMethod "http://localhost:8081/__admin/requests" |
  ConvertTo-Json -Depth 20
```

Você verá o `POST /webhook`, o payload enviado, o header `X-Webhook-Event-Id` e a resposta devolvida pelo WireMock.

## Roteiro completo de apresentação

1. Mostrar a arquitetura: WebhookLab, PostgreSQL e WireMock.
2. Subir os containers e mostrar o health check.
3. Abrir o Swagger.
4. Criar um evento e mostrar `PENDING`.
5. Configurar WireMock para `200` e executar a entrega.
6. Mostrar `DELIVERED/SUCCEEDED` e a requisição em `/__admin/requests`.
7. Criar outro evento, configurar `500` e mostrar `FAILED/HTTP_ERROR`.
8. Criar outro evento, configurar atraso e mostrar `FAILED/TIMEOUT`.
9. Repetir a entrega de um evento concluído e mostrar `409 DELIVERY_CONFLICT`.
10. Reiniciar somente a aplicação e consultar o evento para provar que o histórico está no PostgreSQL.

O roteiro com comandos Bash e PowerShell está em [docs/demo.md](docs/demo.md).

## Parar sem apagar os dados

```powershell
docker compose down
docker compose up --build -d
```

O volume `webhooklab-postgres` é preservado. Para apagar deliberadamente o histórico:

```powershell
docker compose down -v
```

## Estrutura e decisões

- Backend: Java 21, Spring Boot e Maven.
- API: Spring MVC, Bean Validation e Swagger/OpenAPI.
- Persistência: PostgreSQL, JDBC e Flyway.
- Arquitetura: hexagonal, com domínio separado dos frameworks.
- Entrega: manual, síncrona e com uma tentativa por evento na V1.
- Testes: JUnit, Spring Boot Test, Testcontainers e WireMock.

Requisitos, decisões e rastreabilidade ficam em `specs/001-v1/`. O build local completo é `./mvnw verify` ou `./mvnw.cmd verify`.
