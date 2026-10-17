# 03: Motor: Restrições

**Depende de:** 02
**Spec:** §5.4

## Escopo (TDD)
- Porta `Clock`.
- `FaixaHorario` (incluindo faixas que passam da meia-noite), `DiasSemana` (dia avaliado no momento do Disparo) e `LimiteFrequencia` (a partir do instante da última Execução não-Teste, fornecido por uma porta).
- Avaliação em ordem, parando na primeira reprovada e devolvendo qual foi.

## Critérios de aceite
- [ ] 22:00–06:00 passa às 23:00 e às 02:00, e não passa às 12:00.
- [ ] "Segunda" + 22:00–06:00: um Disparo às 02:00 de terça não passa.
- [ ] Limite de 5 min: uma Execução às 10:00 barra às 10:04 e libera às 10:05.
- [ ] Testes não contam para o Limite de frequência.
