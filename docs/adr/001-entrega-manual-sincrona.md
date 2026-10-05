# ADR-001 — entrega manual e síncrona na V1

Status: adotada no design inicial.

## Contexto

A V1 deve demonstrar um fluxo de integração completo e verificável em um projeto pequeno.

## Decisão

Separar registro do evento e entrega. Executar uma tentativa síncrona por evento, disparada por POST /events/{id}/deliver. Configurar um destino único no ambiente.

## Consequências

O primeiro marco pode verificar persistência antes da integração HTTP. A entrega responde após o resultado ou timeout. Agendamento e retentativas automáticas precisam de nova spec.
