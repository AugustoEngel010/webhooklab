# WebhookLab

Laboratório local da V1 para registrar eventos JSON, entregá-los manualmente a um destino HTTP simulado e consultar histórico persistente.

Estado atual: T-07 concluída com Compose completo, Swagger UI e roteiro reproduzível. A V1 mantém uma única tentativa síncrona por evento; o destino único vem de `DELIVERY_TARGET_URL`.

## Pré-requisitos

Docker Desktop 29+ (ou Docker Engine com Compose v2). Java e Maven não são necessários para a stack: o Dockerfile compila com Maven e Java 21 dentro da imagem de build. Para `mvnw verify` fora do Compose, use JDK 21.

## Iniciar

PowerShell:
```powershell
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Start-Process http://localhost:8080/swagger-ui/index.html
```

Linux/WSL:
```bash
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health/readiness
xdg-open http://localhost:8080/swagger-ui/index.html 2>/dev/null || true
```

URLs: API `http://localhost:8080`, Swagger UI `http://localhost:8080/swagger-ui/index.html`, OpenAPI `http://localhost:8080/v3/api-docs`, WireMock Admin `http://localhost:8081/__admin`. Dentro do Compose, a aplicação usa `http://wiremock:8080/webhook`.

O roteiro completo, com cenários 200, 500, timeout, 409 e reinício, está em [docs/demo.md](docs/demo.md). O 201 da entrega confirma a tentativa registrada, inclusive quando `outcome` indica falha.

## Parar e preservar dados

```bash
docker compose down
docker compose up --build -d
```

Isso preserva o volume `webhooklab-postgres`. Para apagar deliberadamente o histórico, use `docker compose down -v`.

Requisitos e decisões estão em `specs/001-v1/`; evidências em `specs/001-v1/traceability.md`. O build local completo é `./mvnw verify` ou `.\mvnw.cmd verify`.
