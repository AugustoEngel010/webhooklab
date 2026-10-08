# ADR 004 — Serviço de eventos e build containerizado

## Contexto

A V1 tinha três serviços de entrada que apenas delegavam para o mesmo repositório,
SQL e mapeamento JDBC duplicados e instruções dependentes do Maven Wrapper.

## Decisão

Consolidar registro, consulta e listagem em `EventService`, mantendo `DeliverEventService`
separado e preservando as portas de saída úteis. Separar constantes SQL dos mappers
JDBC e reutilizar `JdbcEventMapper` para a conversão do evento. O build e `verify`
passam a executar em containers Maven/JDK 21; o Compose monta o Docker Engine para
Testcontainers e o diretório `target` para relatórios.

## Consequências

O domínio continua sem frameworks, os contratos HTTP e fronteiras transacionais
permanecem inalterados, e o host não precisa ter Maven instalado. A verificação
depende de Docker Desktop/Engine com o socket acessível ao serviço `verify`.
