# 20: Serviço de acessibilidade e adaptador Android do WhatsApp

**Depende de:** 19
**Spec:** §5.6, §6 (serviço de acessibilidade, `<queries>`), §10, [ADR 0003](../adr/0003-whatsapp-por-automacao-de-acessibilidade.md)

## Escopo
- `DMacroAcessibilidade : AccessibilityService`, declarado no manifesto com `BIND_ACCESSIBILITY_SERVICE` e `res/xml/acessibilidade.xml`: `packageNames="com.whatsapp,com.whatsapp.w4b"`, eventos de mudança de janela e de conteúdo, `canRetrieveWindowContent`, `flagReportViewIds`, `isAccessibilityTool="false"` e descrição em pt-BR que diz exatamente o que o serviço faz.
- `TelaWhatsApp` Android: abre o link com `setPackage(app.pacote)` e `FLAG_ACTIVITY_NEW_TASK` a partir do contexto do serviço; converte `rootInActiveWindow` em `List<NoTela>` (reciclando os nós); toca com `ACTION_CLICK`; volta com `GLOBAL_ACTION_HOME`. Sem o serviço conectado, devolve a falha "serviço de acessibilidade desligado".
- `EstadoAparelho` Android: `KeyguardManager.isKeyguardLocked` (tela apagada conta como bloqueada), `AudioManager.mode` e verificação a cada 1 s, exigindo o aparelho liberado por mais 1,5 s.
- `PrazoItem` Android: job do WorkManager sem restrição de rede, no mesmo padrão do `EmailPrazoWorker` (`falharSePendente`).
- "WhatsApp não instalado": verificado no momento de enfileirar, via `PackageManager`.
- `VerificadorPermissoes`: `Acessibilidade` concedida quando o serviço do D-Macro está em `ENABLED_ACCESSIBILITY_SERVICES`.
- Manifesto: `<queries>` com `<package>` para `com.whatsapp` e `com.whatsapp.w4b`.
- Hilt: troca o `WhatsAppSender` provisório do ticket 18 pela `FilaWhatsApp`, num escopo único do app.
- Nada do que o serviço lê vai para logs ou Registros.

## Testes
- S5: sem testes automatizados (adaptador de plataforma). Verificação manual guiada no ticket 22.

## Critérios de aceite
- [ ] Num aparelho real, com a tela desbloqueada, o Teste de uma Macro com WhatsApp envia a mensagem e volta para a tela inicial.
- [ ] Com o WhatsApp aberto em outra conversa, o envio abre a conversa certa.
- [ ] Desligar o serviço nas configurações faz o próximo envio falhar com "serviço de acessibilidade desligado".
