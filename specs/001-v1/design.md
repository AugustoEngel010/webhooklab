# Design — WebhookLab V1

## 3. Stack e decisões iniciais

| Componente | Escolha |
| --- | --- |
| Backend | Java 21, Spring Boot e Maven Wrapper |
| API | Spring MVC e Bean Validation |
| Persistência | PostgreSQL, Spring Data JPA e Flyway |
| Documentação e demonstração | springdoc-openapi / Swagger UI |
| Destino de teste | WireMock |
| Testes | JUnit, Spring Boot Test e Testcontainers |
| Ambiente local | Docker Compose |
| Verificação no GitHub | GitHub Actions executando Maven verify |

Fixar versões compatíveis ao criar o projeto. A consulta de 05/10/2026 identificou Spring Boot 4.1.1 e springdoc-openapi 3.1.1 como referências atuais. Conferir a matriz do springdoc antes de fixar o build. PostgreSQL 17 é uma escolha suficiente para este exercício.

O destino é único e configurado por variável de ambiente: `DELIVERY_TARGET_URL`. Durante desenvolvimento com a aplicação na IDE, o destino pode ser `http://localhost:8081/webhook`; com os dois serviços no Compose, usar o nome do serviço WireMock e sua porta interna.

## 6. Design — design.md

Uma aplicação, um banco e um destino simulado. Separação de responsabilidades em um único projeto Maven:

| Pacote | Responsabilidade |
| --- | --- |
| domain | Evento, tentativa, estados e transições; sem Spring ou JPA |
| application | Casos de uso e interfaces para persistência e envio HTTP |
| adapters.in.web | Controllers, DTOs, validação e tradução dos erros |
| adapters.out.persistence | Entidades JPA, repositories, mapeamento e transações |
| adapters.out.http | Cliente que entrega o envelope ao destino e classifica a resposta |
| config | Composição das dependências e Clock |

Casos de uso: RegisterEvent, GetEvent, ListEvents e DeliverEvent. As interfaces de saída são definidas na aplicação e implementadas pelos adapters. O controller chama os casos de uso; o domínio não recebe entidades JPA ou tipos HTTP.

Estados do evento: PENDING → SENDING → DELIVERED ou FAILED. Resultados da tentativa: STARTED → SUCCEEDED, HTTP_ERROR, TIMEOUT ou CONNECTION_ERROR.

Persistir duas tabelas: events e delivery_attempts. A tentativa referencia seu evento. Na V1, uma restrição unique em event_id impede duas tentativas para o mesmo evento. Uma mudança posterior para múltiplas tentativas exigirá migration versionada.

Para iniciar uma entrega, uma transação deve reclamar o evento somente se estiver PENDING, atualizar para SENDING e criar a tentativa STARTED. Uma atualização condicional ou bloqueio de linha pode implementar essa operação atômica. Somente quem obteve a transição inicia o HTTP. A operação de rede ocorre fora da transação do banco. Ao terminar, outra transação atualiza evento e tentativa juntos.

Se o processo parar após reclamar o evento, o histórico pode ficar em SENDING/STARTED. Esses estados permanecem visíveis; a V1 não reenvia automaticamente, porque o resultado externo pode ser desconhecido. Recuperação e reprocessamento precisam de uma especificação própria na evolução.

Registrar datas em UTC, usando um Clock injetado nos casos de uso. Calcular duração com relógio monotônico. Flyway cria o schema; Hibernate valida o schema em vez de criá-lo automaticamente.

ADRs iniciais:

- ADR-001: entrega manual e síncrona na V1, para construir e testar primeiro o fluxo de uma tentativa.
- ADR-002: domínio separado dos frameworks.
- ADR-003: reclamar a entrega de forma atômica e manter a chamada HTTP fora da transação.

## Convenções de diretórios

Pacote base: `dev.webhooklab`.

- `domain/`: modelos e regras.
- `application/port/in/` e `application/port/out/`: portas necessárias aos casos de uso.
- `application/usecase/`: implementação dos casos de uso.
- `adapters/in/web/`: API e DTOs.
- `adapters/out/persistence/`: persistência e mapeamento JPA.
- `adapters/out/http/`: entrega HTTP.
- `config/`: composição do Spring e Clock.

Criar classes e subpacotes quando a tarefa exigir. O scaffold não contém implementação.
### Listagem paginada

`ListEvents` retorna uma pagina com os eventos, suas tentativas quando existentes, e os metadados `page`, `size`, `totalElements` e `totalPages`. O adapter JDBC executa a ordenacao `created_at DESC, id DESC`, um `COUNT(*)` e a busca com `LEFT JOIN`, `LIMIT/OFFSET` no PostgreSQL.
