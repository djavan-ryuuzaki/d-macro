# 06: Persistência de Macros, Registros e configurações

**Depende de:** 05
**Spec:** §8

## Escopo
- Room: `macros` (listas em JSON), `execucoes` e `execucao_acoes`.
- Repositórios que implementam `MacroSource`, `ExecutionLog`, `SafetyCounter` e a consulta da última Execução (Limite de frequência).
- Retenção de 7 dias / 500 entradas, aplicada na inserção; "limpar tudo".
- DataStore para a Chave geral e o serviço em primeiro plano (`AppSettings`).
- Toda Macro nova é criada com `LimiteFrequencia(5)`.

## Critérios de aceite
- [ ] Uma Macro com todos os tipos de Gatilho, Ação e Restrição faz o ciclo salvar → ler sem perda.
- [ ] Ao inserir a entrada 501, a mais antiga é removida; entradas com mais de 7 dias também são removidas.
- [ ] O Teto global e o Limite de frequência continuam valendo depois de matar o processo.
