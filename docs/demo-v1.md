# Demonstração planejada — V1

Executar este roteiro somente depois de concluir a implementação e verificação.

## 11. Demonstração completa da V1

1. Subir app, PostgreSQL e WireMock pelo Compose.
2. Registrar evento fictício e mostrar seu estado PENDING.
3. Disparar a tentativa com o destino respondendo 200; mostrar DELIVERED.
4. Registrar outro evento e configurar o destino para responder 500; mostrar FAILED e HTTP_ERROR.
5. Registrar outro evento e configurar atraso superior ao timeout; mostrar FAILED e TIMEOUT.
6. Consultar histórico e duração das três tentativas.
7. Repetir o disparo de um evento já tentado e mostrar 409.
8. Mostrar a spec, um teste de integração e o vínculo requisito → teste.
