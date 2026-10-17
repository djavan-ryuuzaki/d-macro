# 07: Telas Macros e Editor de Macro

**Depende de:** 06
**Spec:** §5.9, §9 (telas 1 e 2)

## Escopo
- Tela Macros: Chave geral no topo, lista (nome, resumo do Gatilho, chave de ativação), FAB "Nova Macro" e estado vazio.
- Editor: nome, três cartões (Gatilhos, Ações e Restrições, cada um com +, editar, remover; Ações reordenáveis) e menu com Excluir.
- Salvar rascunho sempre; a chave de ativação fica bloqueada com a lista de problemas enquanto `podeAtivar` falhar.
- O "+" de cada cartão abre um seletor de tipos (no MVP: 1 Gatilho, 3 Ações, 3 Restrições). As telas de configuração dos tipos vêm nos tickets seguintes; aqui elas ficam como placeholders.

## Critérios de aceite
- [ ] Criar, renomear, ativar/desativar e excluir Macros funciona e persiste.
- [ ] Uma Macro incompleta não pode ser ativada, e o app mostra o motivo.
- [ ] A Chave geral persiste entre aberturas do app.
