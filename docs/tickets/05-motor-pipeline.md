# 05: Motor: pipeline Disparo → Execução, Teto global e Chave geral

**Depende de:** 02, 03, 04
**Spec:** §3 (portas), §5.3, §5.7, §5.8 (comportamento do motor)

## Escopo (TDD com fakes)
- Portas `SmsSender`, `CallPlacer`, `EmailSender`, `ExecutionLog`, `MacroSource`, `SafetyCounter`, `AppSettings` e uma porta de verificação de permissões.
- Entrada `onNotificacao(evento)`: descartes globais (incluindo a Chave geral), seleção das Macros satisfeitas e um Disparo por Macro.
- Fila serializada por Macro; Macros diferentes em paralelo.
- Ordem: permissões → Restrições → início da Execução → Ações em sequência que continuam após falha → Registro.
- Teto global (20 SMS / 5 chamadas em 1 h, janela deslizante): o envio que estouraria o teto é bloqueado e registrado, e um evento "teto atingido" é emitido.
- Entrada `testar(macro)`: ignora Gatilhos, Restrições, Limite e Chave geral; respeita o Teto e as permissões; o Registro sai marcado como Teste.
- Modelo do Registro de Execução: Disparo, motivo do bloqueio e resultado por Ação/Destinatário (ok, erro com motivo, pendente).

## Critérios de aceite
- [ ] Uma falha no SMS para B não impede o SMS para C nem o e-mail seguinte.
- [ ] Dois eventos simultâneos para a mesma Macro com Limite de 5 min resultam em exatamente 1 Execução.
- [ ] Com a Chave geral desligada, nenhum Disparo acontece; o Teste funciona mesmo assim.
- [ ] O 21º SMS na janela é bloqueado e registrado; após 1 h, o envio volta a ser liberado.
- [ ] Uma Macro sem `SEND_SMS` gera o Registro "bloqueada: permissão SMS ausente".
