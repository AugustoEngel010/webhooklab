# Demonstração local da V1

Todos os eventos de entrega abaixo são novos. O endpoint `POST /events/{id}/deliver` é síncrono: retorna 201 depois de registrar o resultado, inclusive `HTTP_ERROR` e `TIMEOUT`.

## Subir e conferir

```bash
docker compose up --build -d
docker compose ps
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
```

PowerShell:
```powershell
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Invoke-RestMethod http://localhost:8080/v3/api-docs | Out-Null
```

Swagger: `http://localhost:8080/swagger-ui/index.html`.

## Helpers

Linux/WSL (requer `jq`):
```bash
api=http://localhost:8080; wm=http://localhost:8081
create_event() { curl -fsS -X POST "$api/events" -H 'Content-Type: application/json' -d "$1"; }
set_mapping() { curl -fsS -X POST "$wm/__admin/mappings" -H 'Content-Type: application/json' -d "$1"; }
clear_mapping() { curl -fsS -X DELETE "$wm/__admin/mappings"; }
pending=$(create_event '{"eventType":"ORDER_CREATED","payload":{"orderId":"PENDING-001","amount":10.0}}')
event_pending=$(printf '%s' "$pending" | jq -r .id)
curl -fsS "$api/events/$event_pending" | jq .
curl -fsS "$api/events?page=0&size=20" | jq .
```

PowerShell:
```powershell
$api='http://localhost:8080'; $wm='http://localhost:8081'
function New-DemoEvent($id) { Invoke-RestMethod -Method Post "$api/events" -ContentType 'application/json' -Body (@{eventType='ORDER_CREATED';payload=@{orderId=$id;amount=10.0}} | ConvertTo-Json -Depth 5) }
function Clear-Mappings { Invoke-RestMethod -Method Delete "$wm/__admin/mappings" }
function Set-Response($status, $delay=0) { $r=@{status=$status}; if($delay -gt 0){$r.fixedDelayMilliseconds=$delay}; $m=@{request=@{method='POST';url='/webhook'};response=$r;persistent=$false}; Invoke-RestMethod -Method Post "$wm/__admin/mappings" -ContentType 'application/json' -Body ($m|ConvertTo-Json -Depth 10) }
$pending=New-DemoEvent 'PENDING-001'; $event_pending=$pending.id
Invoke-RestMethod "$api/events/$event_pending" | ConvertTo-Json -Depth 10
Invoke-RestMethod "$api/events?page=0&size=20" | ConvertTo-Json -Depth 10
```

## Cenários de entrega

Antes de cada cenário, limpe e crie o mapping correspondente.

200 — esperado `DELIVERED/SUCCEEDED`:
```bash
clear_mapping; set_mapping '{"request":{"method":"POST","url":"/webhook"},"response":{"status":200},"persistent":false}'
event_200=$(create_event '{"eventType":"ORDER_CREATED","payload":{"orderId":"OK-001"}}'|jq -r .id)
curl -i -X POST "$api/events/$event_200/deliver"; curl -fsS "$api/events/$event_200"|jq .
```

500 — esperado `FAILED/HTTP_ERROR`, `httpStatus:500`:
```bash
clear_mapping; set_mapping '{"request":{"method":"POST","url":"/webhook"},"response":{"status":500},"persistent":false}'
event_500=$(create_event '{"eventType":"ORDER_CREATED","payload":{"orderId":"ERROR-001"}}'|jq -r .id)
curl -i -X POST "$api/events/$event_500/deliver"; curl -fsS "$api/events/$event_500"|jq .
```

Timeout — esperado `FAILED/TIMEOUT`, `httpStatus:null`; o cliente espera 3 s:
```bash
clear_mapping; set_mapping '{"request":{"method":"POST","url":"/webhook"},"response":{"status":200,"fixedDelayMilliseconds":4000},"persistent":false}'
event_timeout=$(create_event '{"eventType":"ORDER_CREATED","payload":{"orderId":"TIMEOUT-001"}}'|jq -r .id)
time curl -i -X POST "$api/events/$event_timeout/deliver"; curl -fsS "$api/events/$event_timeout"|jq .
```

Confira em cada consulta `createdAt`, `startedAt`, `completedAt` e `durationMs`.

## 409 e persistência

```bash
curl -i -X POST "$api/events/$event_200/deliver"
curl -fsS "$wm/__admin/requests" | jq '.requests|length'
docker compose restart app
curl -fsS "$api/events/$event_200" | jq .
```

A repetição retorna 409 `DELIVERY_CONFLICT` e não cria novo envio. Após reiniciar apenas a aplicação, o evento continua `DELIVERED` e a tentativa `SUCCEEDED`, com os mesmos IDs, horários e duração.

PowerShell para o mesmo fluxo:
```powershell
Clear-Mappings; Set-Response 200
$event200=(New-DemoEvent 'OK-PS-001').id
Invoke-RestMethod -Method Post "$api/events/$event200/deliver"|ConvertTo-Json -Depth 10
try { Invoke-RestMethod -Method Post "$api/events/$event200/deliver" } catch { $_.ErrorDetails.Message }
docker compose restart app
Invoke-RestMethod "$api/events/$event200"|ConvertTo-Json -Depth 10
```

Para timeout no PowerShell: `Clear-Mappings; Set-Response 200 4000`, crie outro evento e invoque `POST /deliver`. Para parar sem apagar dados use `docker compose down`; retome com `docker compose up --build -d`. `docker compose down -v` apaga o volume.

Este roteiro demonstra um evento `ORDER_CREATED` concreto: ele e persistido como `PENDING`, enviado manualmente ao WireMock e depois consultado como `DELIVERED`, `HTTP_ERROR` ou `TIMEOUT`. Para a verificacao automatizada, execute `& .\mvnw.cmd -B verify` na raiz; o comando inclui `spotless:check`, Surefire e Failsafe e exige Docker para PostgreSQL/WireMock.
