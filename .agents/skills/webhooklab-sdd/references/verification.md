# Verificação por tipo de mudança

## Escolher verificações

| Mudança | Verificação necessária |
| --- | --- |
| Somente árvore ou documentação | Conferir caminhos, links relativos e consistência entre requisito, design e tarefas |
| Bootstrap Java | Conferir Java selecionado, build real e inicialização com PostgreSQL do Compose; não concluir T-01 apenas por criar diretórios |
| Regra de domínio | Teste unitário baseado na regra, com Clock controlado quando necessário |
| Registro e consulta HTTP | Teste do contrato com PostgreSQL do Testcontainers, validação negativa e persistência dos campos |
| Migration ou repository | Aplicar Flyway em PostgreSQL real e verificar restrições e consultas relevantes |
| Integração HTTP | Usar WireMock para 2xx, não-2xx, timeout e falha de conexão |
| Reclamação concorrente | Executar duas chamadas concorrentes para o mesmo evento; verificar uma tentativa persistida e um envio |
| CI | Confirmar que o comando configurado executa testes unitários e de integração |

## Conferir o build existente

- Antes de executar Maven, ler o pom.xml e verificar a existência do Maven Wrapper.
- Quando configurados, usar Surefire para classes *Test e Failsafe para classes *IT.
- Usar `./mvnw verify` para a verificação completa somente após o wrapper e os plugins estarem disponíveis.
- Para uma verificação focal, selecionar a classe ou cenário pertinente usando a configuração real do projeto.
- Conferir os relatórios de testes. Um build que ignorou testes não comprova os critérios.
- Testcontainers precisa de um ambiente Docker acessível; se estiver ausente, relatar o bloqueio e manter a verificação pendente.
- Repetir ou ampliar verificações apenas quando mudanças, falhas ou dúvidas concretas justificarem.

## Revisar a entrega

- Conferir domínio sem frameworks e DTOs separados das entidades JPA.
- Conferir erro de persistência separado do resultado do destino.
- Conferir consistência entre estados do evento e da tentativa.
- Conferir transação atômica de reclamação, HTTP fora da transação e término persistido em conjunto.
- Conferir que 2xx significa reconhecimento HTTP, e timeout não prova ausência de processamento no destino.
- Preservar códigos e comportamento definidos em requirements.md.

## Registrar evidência

Em traceability.md, preencher uma linha por cenário verificado:
`requisito | cenário | caminho e classe do teste | comando | resultado observado`.

Quando um teste não foi executado, escrever `Não executado` e o motivo.
Não copiar saída fictícia, não usar um teste espelhado da implementação como única evidência e não marcar a tarefa como concluída se os critérios não foram verificados.
