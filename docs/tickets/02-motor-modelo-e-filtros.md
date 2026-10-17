# 02: Motor: modelo de domínio e avaliação do Gatilho Notificação recebida

**Depende de:** 01
**Spec:** §4, §5.1 (descartes globais), §5.2, §5.9

## Escopo (TDD, Kotlin puro)
- Tipos `@Serializable` de §4: `Macro`, `Gatilho`, `FiltroTexto`, `Acao`, `Destinatario` e `Restricao`.
- `EventoNotificacao` (pacote, nomeApp, titulo, texto, chave, emCurso, instante).
- Avaliação de `NotificacaoRecebida` contra um evento: apps, emCurso, filtros de título e texto (4 modos, lista de termos, ignorar maiúsculas).
- Impedir múltiplos disparos: conjunto de chaves por Gatilho, incluindo a operação "notificação removida" que libera a chave.
- Descartes globais: pacote próprio e resumo de grupo.
- Validação de §5.9 (`podeAtivar(macro)` com a lista de problemas).

## Critérios de aceite
- [ ] Testes cobrem cada linha da tabela de modos, incluindo "Não contém" com vários termos e Igual/Contém com e sem ignorar maiúsculas.
- [ ] O cenário do título "Banco X" / texto "Compra aprovada" dispara, e "Compra negada" não.
- [ ] A mesma chave não dispara duas vezes; depois de removida, dispara de novo.
- [ ] Os tipos fazem serialização ida e volta em JSON sem perda.
