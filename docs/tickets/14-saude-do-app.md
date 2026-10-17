# 14: Saúde do app

**Depende de:** 09, 10, 11
**Spec:** §6, §7

## Escopo
- Tela com status e botão "corrigir" para: acesso a notificações / listener conectado (`requestRebind`), `SEND_SMS`, `CALL_PHONE`, `POST_NOTIFICATIONS`, otimização de bateria, guia Samsung (só em aparelhos Samsung), Conta de e-mail (quando alguma Macro usa e-mail) e aviso do Android 15.
- Banner na tela Macros quando algo crítico está faltando.
- Alerta por Macro na lista quando falta uma permissão de que ela precisa.
- Pede `POST_NOTIFICATIONS` ao ativar a primeira Macro.
- Verifica o listener ao abrir o app e no `BOOT_COMPLETED`.

## Critérios de aceite
- [ ] Revogar `SEND_SMS` nas configurações do sistema faz aparecer o alerta na Macro e o banner na tela inicial.
- [ ] Desligar o acesso a notificações faz o item ficar vermelho e o botão leva à tela do sistema.
