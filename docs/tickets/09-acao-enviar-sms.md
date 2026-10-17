# 09: Ação Enviar SMS

**Depende de:** 08
**Spec:** §5.5, §5.6 (SMS), §6, §9 (tela 4)

## Escopo
- Tela de configuração: Destinatários (seletor de contatos do sistema em `Phone.CONTENT_URI` sem `READ_CONTACTS`, ou digitação manual; guarda número + rótulo), mensagem com "Inserir marcador", limite de caracteres e estimativa de partes.
- Pede `SEND_SMS` ao adicionar a Ação.
- `SmsSender` Android: `sendMultipartTextMessage` pelo chip padrão, resultado via `sentIntent` e uma nova tentativa após 1 minuto.
- Remove a Ação temporária de depuração do ticket 08.

## Critérios de aceite
- [ ] Num aparelho real, uma notificação que passa nos Filtros envia SMS para 2 Destinatários com os Marcadores preenchidos.
- [ ] Em modo avião, o Registro mostra o erro depois da nova tentativa.
- [ ] Uma mensagem longa é cortada em 300 caracteres com "…".
