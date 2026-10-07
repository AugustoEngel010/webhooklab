# ADR-002 — domínio sem frameworks

Status: adotada no design inicial.

## Contexto

Regras de evento e tentativa precisam ser testáveis sem banco ou servidor HTTP.

## Decisão

Manter modelos e regras em domain sem Spring, JPA, Jackson ou HTTP. Definir portas na aplicação. Adaptar persistência e HTTP nos adapters.

## Consequências

DTOs, entidades JPA e modelos de domínio são separados. A configuração liga as implementações concretas.

Na T-08, a formatação é aplicada ao código Java sem introduzir dependências de framework no domínio; a separação permanece coberta pelo layout real de `src/main/java`.
