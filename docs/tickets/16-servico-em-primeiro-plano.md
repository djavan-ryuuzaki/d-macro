# 16: Serviço em primeiro plano opcional

**Depende de:** 08
**Spec:** §7, §9 (tela 8)

## Escopo
- Opção em Configurações (desligada por padrão).
- Serviço do tipo `specialUse` com a notificação permanente "D-Macro ativo", iniciado no boot quando a opção estiver ligada.

## Critérios de aceite
- [ ] Ligar a opção mostra a notificação permanente, inclusive após reiniciar o aparelho.
- [ ] Desligar a opção remove a notificação e encerra o serviço.
