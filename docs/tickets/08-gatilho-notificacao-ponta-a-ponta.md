# 08: Gatilho Notificação recebida de ponta a ponta

**Depende de:** 07
**Spec:** §5.1, §5.2, §6 (acesso a notificações), §9 (tela 3)

## Escopo
- `NotificationListenerService`: converte `StatusBarNotification` em `EventoNotificacao` (§5.1), repassa ao motor e trata `onNotificationRemoved` (libera a chave).
- Registro dos pacotes que já publicaram notificações (fonte do seletor de apps).
- Tela de configuração: apps (Todos / lista de apps com ícone na tela inicial via `<queries>` + apps que já notificaram), dois Filtros (modo + termos) e três opções.
- Pede o acesso a notificações ao adicionar o Gatilho.
- Aviso das "notificações aprimoradas" no Android 15+.
- Wiring do motor via Hilt. Para validar este ticket sozinho, uma Ação temporária de depuração pode só gravar o Registro (removida no ticket 09).

## Critérios de aceite
- [ ] Uma notificação real (de outro app ou de um app de teste) que passa nos Filtros gera um Disparo registrado.
- [ ] Atualizar a mesma notificação 5 vezes gera 1 Disparo.
- [ ] Notificações do próprio D-Macro e resumos de grupo são ignorados.
