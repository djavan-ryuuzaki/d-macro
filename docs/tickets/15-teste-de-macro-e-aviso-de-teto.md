# 15: Teste de Macro e aviso do Teto global

**Depende de:** 09, 10, 11
**Spec:** §5.7, §5.8

## Escopo
- "Testar" no menu do editor, com uma confirmação que descreve o que vai acontecer; chama `motor.testar(macro)`.
- Canal de notificações do próprio app, com uma notificação quando o Teto global é atingido (mostrando o tipo e quando a janela libera).

## Critérios de aceite
- [ ] O Teste envia de verdade com os valores de exemplo e aparece nos Registros como Teste.
- [ ] Ao atingir o Teto global via Testes, a notificação de teto atingido aparece e o envio é bloqueado.
