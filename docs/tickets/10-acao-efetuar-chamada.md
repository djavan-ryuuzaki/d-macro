# 10: Ação Efetuar chamada

**Depende de:** 08
**Spec:** §5.6 (chamada), §6

## Escopo
- Tela de configuração com um único Destinatário (seletor de contatos ou manual).
- Pede `CALL_PHONE` ao adicionar a Ação.
- `CallPlacer` Android: `TelecomManager.placeCall` pelo chip padrão; a Ação não espera a chamada terminar.

## Critérios de aceite
- [ ] Num aparelho real com a tela bloqueada, a notificação que passa nos Filtros inicia a chamada.
- [ ] A Ação seguinte (ex.: SMS) roda logo em seguida, sem esperar a chamada terminar.
