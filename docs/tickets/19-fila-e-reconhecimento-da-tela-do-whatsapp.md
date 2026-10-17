# 19: Fila do WhatsApp e reconhecimento da tela

**Depende de:** 18
**Spec:** §3 (portas da Fila), §5.6 (Fila, prazo, falhas, reconhecimento), §11

## Pré-requisito do usuário
Despejos da árvore de acessibilidade do WhatsApp num aparelho real, com o celular no USB. Para cada caso, abrir a tela e rodar `adb shell uiautomator dump /sdcard/wa.xml` e `adb pull /sdcard/wa.xml`:
1. Conversa aberta pelo link `api.whatsapp.com/send?phone=...&text=Teste`, com o texto no campo (pt-BR).
2. O mesmo com o aparelho em inglês (opcional, mas recomendado).
3. O aviso de número sem WhatsApp.
4. A conversa logo depois de tocar em "Enviar" (campo vazio).
5. Se usar, os mesmos casos 1 e 4 no WhatsApp Business.

Os XML viram fixtures em `app/src/test/resources/whatsapp/`, sem conteúdo de conversas reais (usar um contato de teste).

## Escopo
- Em `engine/whatsapp/` (Kotlin puro, coberto pelo `EngineIsolationTest`):
  - `NoTela(idView, classe, texto, descricao, clicavel)` e a função pura `reconhecer(nos, app): EstadoTela` → `ConversaPronta(enviar, campo)`, `NumeroSemWhatsApp(botaoOk)` ou `Aguardar`. Ids e textos conhecidos num único objeto.
  - Portas `EstadoAparelho`, `TelaWhatsApp` e `PrazoItem`.
  - `FilaWhatsApp`, que implementa `WhatsAppSender`: fila única, um item por vez, espera por desbloqueio/fim de chamada, conferência do campo antes do toque, confirmação de campo vazio, volta para a tela inicial depois do último item, falhas imediatas e sem nova tentativa após o toque (§5.6). Atualiza o item por `ExecutionLog.atualizarItem` e agenda o prazo por `PrazoItem`.
- Um leitor de XML do `uiautomator dump` → `List<NoTela>`, usado só nos testes.

## Testes (TDD)
- S2: `reconhecer` contra cada fixture, mais casos sintéticos (campo com rascunho diferente → não pronto para enviar; botão encontrado só pela descrição).
- S6 (novo ponto de teste: Fila com fakes e tempo virtual):
  - dois itens enfileirados ao mesmo tempo são enviados um depois do outro;
  - tela bloqueada: o item espera, e é enviado assim que `EstadoAparelho` avisa o desbloqueio;
  - chamada em andamento: espera até ela terminar;
  - conversa não reconhecida em 15 s → "tela do WhatsApp não reconhecida", e a fila segue para o próximo item;
  - número sem WhatsApp → toca no OK do aviso, registra o erro e segue;
  - campo não esvazia em 5 s → "envio não confirmado", sem tocar de novo;
  - rascunho diferente no campo → não toca em "Enviar";
  - volta para a tela inicial uma vez, depois do último item.

## Critérios de aceite
- [ ] Todos os testes passam sem Android.
- [ ] Trocar um id do WhatsApp exige mudar um único arquivo.
