# ADR-003 — início atômico e HTTP fora da transação

Status: adotada no design inicial.

## Contexto

Duas chamadas concorrentes para um evento PENDING não podem iniciar duas tentativas.

## Decisão

Reclamar o evento, transicionar para SENDING e criar a tentativa STARTED na mesma transação. Somente o solicitante que obteve a transição envia HTTP. Executar o HTTP fora da transação; persistir o resultado do evento e da tentativa em conjunto.

## Consequências

Validar concorrência em PostgreSQL real. Uma interrupção pode manter SENDING/STARTED e resultado externo desconhecido; preservar esse histórico sem reenvio automático na V1.
