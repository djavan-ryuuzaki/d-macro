# Roteiro de aceite do MVP (aparelho real)

Valida os 7 critérios de [SPEC §12](SPEC.md#12-critérios-de-aceite-do-mvp) num aparelho físico, de preferência um Samsung. Execute na ordem; cada passo diz o que observar.

## Preparação

1. Instale o APK (`app/build/outputs/apk/debug/app-debug.apk`, ou o release assinado, ver “APK release” abaixo).
2. Abra o D-Macro → aba **Saúde** e deixe tudo verde: acesso a notificações, SMS, Telefone, notificações do D-Macro e otimização de bateria. Em Samsung, siga o guia de “Apps que nunca entram em suspensão”.
3. **Configurações** → Conta de e-mail → Gmail → seu e-mail + senha de app → **Salvar** → **Enviar e-mail de teste**. O e-mail precisa chegar.
4. Tenha um segundo aparelho (ou um app de testes) capaz de **publicar uma notificação** com título controlado. Opção sem segundo aparelho: com o celular no USB, rode
   `adb shell "cmd notification post -t 'Alarme' tagA 'Sensor da sala'"`.

> **APK instalado pelo navegador ou gerenciador de arquivos (Android 13+):** o Android bloqueia os interruptores do acesso a notificações e da acessibilidade até você liberar em Configurações › Apps › D-Macro › ⋮ › **Permitir configurações restritas**. A aba Saúde mostra esse guia. Instalações por `adb install` não passam por esse bloqueio.

## Critério 1: fluxo completo com a tela bloqueada

1. Nova Macro “Alarme”: Gatilho **Notificação recebida**, app = o app que vai notificar (ou Todos), Filtro do título **Contém** `alarme`.
2. Ações, nesta ordem: **Efetuar chamada** para A; **Enviar SMS** para B e C com `{titulo}: {texto}`; **Enviar e-mail** para D com assunto `Alerta {app}`.
3. Ative a Macro e bloqueie a tela.
4. Publique a notificação “Alarme / Sensor da sala”.
- [ ] A chamada para A começa.
- [ ] B e C recebem “Alarme: Sensor da sala”.
- [ ] D recebe o e-mail.
- [ ] Em **Registros**: “Executada”, com todos os itens “Concluído” (o e-mail passa de “Pendente” para “Concluído”).

## Critério 2: atualizações da mesma notificação

1. Publique a mesma notificação (mesma tag/ID) 5 vezes seguidas, mudando o texto.
- [ ] Só um novo Disparo aparece em Registros (com o Limite de frequência removido da Macro, para isolar o teste).

## Critério 3: Limite de frequência

1. Com o Limite de 5 min, publique duas notificações **diferentes** com 1 min de intervalo.
- [ ] A segunda aparece em Registros como **Barrada**, com “Limite de frequência” como motivo.

## Critério 4: Teto global de SMS

1. Crie uma Macro com uma Ação SMS para 25 destinatários (podem ser números seus repetidos com formatos diferentes) e use **Testar**.
- [ ] 20 SMS saem e 5 itens aparecem como “Bloqueado pelo Teto global”.
- [ ] A notificação “Teto global atingido” aparece.

## Critério 5: Chave geral

1. Desligue a Chave geral (topo da tela Macros) e publique a notificação do critério 1.
- [ ] Nada acontece e nenhum Registro é criado.

## Critério 6: permissão revogada

1. Em Configurações do Android › Apps › D-Macro › Permissões, negue SMS.
- [ ] A Macro aparece com o ícone de alerta e a tela Macros mostra o banner de Saúde.
- [ ] Publicar a notificação gera um Registro “Bloqueada: permissão ausente (SMS)”.

## Critério 7: e-mail sem rede

1. Ative o modo avião (deixe o SMS de fora da Macro para não poluir o teste) e publique a notificação.
- [ ] O item de e-mail fica “Pendente”.
2. Desative o modo avião em menos de 30 min.
- [ ] O e-mail é enviado e o item vira “Concluído”.
3. Repita deixando o modo avião ligado por mais de 30 min.
- [ ] O item vira “Falhou: prazo de 30 minutos esgotado”.

## Ação Enviar WhatsApp (critérios 8 a 12)

Preparação: WhatsApp instalado e logado; um número de teste (o ideal é o seu, a conversa "Você"). Ao adicionar a primeira Ação Enviar WhatsApp, o app mostra a divulgação; toque em **Abrir configurações** › Aplicativos instalados › **D-Macro: enviar WhatsApp** › ativar.

> **Forçar a parada do D-Macro desliga o serviço de acessibilidade** (comportamento do Android). Atualizar o APK não desliga. Se a Macro aparecer com alerta, veja a aba Saúde.

Macro de teste: Gatilho Notificação recebida com título **Contém** `dmacrowa`; Ação Enviar WhatsApp para o número de teste com `Teste {hora}`; sem Limite de frequência. Para publicar a notificação: `adb shell "cmd notification post -t 'dmacrowa' tag1 'texto'"` (troque a tag a cada disparo).

### Critério 8: envio com a tela desbloqueada
- [ ] Publicar a notificação: o WhatsApp abre, a mensagem sai com a hora preenchida e o aparelho volta para a tela inicial em poucos segundos.
- [ ] Em Registros: "Executada", item "Concluído".

### Critério 9: tela bloqueada
1. Bloqueie a tela e publique a notificação.
- [ ] O WhatsApp não abre enquanto a tela estiver bloqueada.
2. Desbloqueie em menos de 30 min.
- [ ] A mensagem sai logo depois do desbloqueio (alguns segundos).
3. Repita deixando bloqueado por mais de 30 min.
- [ ] O item vira "Falhou: tela bloqueada até o fim do prazo de 30 minutos" (ou "prazo de 30 minutos esgotado").

### Critério 10: número sem WhatsApp
1. Ação com dois Destinatários: um número sem WhatsApp e o número de teste.
- [ ] O primeiro vira "Falhou: número sem WhatsApp" (o WhatsApp 2.26 abre a tela "Enviar para…"; o app sai dela com Voltar sem tocar em nada) e o segundo é enviado.

### Critério 11: rascunho
1. Digite um rascunho na conversa do número de teste, sem enviar, e dispare a Macro.
- [ ] O rascunho nunca é enviado. (No WhatsApp 2.26 o link substitui o rascunho pelo texto da Macro, que é enviado normalmente; se uma versão mantiver o rascunho, o item vira "Falhou: a conversa já tinha outro texto digitado".)

### Critério 12: serviço desligado
1. Desligue "D-Macro: enviar WhatsApp" nas configurações de acessibilidade.
- [ ] A Macro aparece com alerta; a Saúde mostra "Serviço de acessibilidade" em vermelho.
- [ ] Publicar a notificação gera "Bloqueada: permissão ausente (Serviço de acessibilidade)".

## APK release

O APK de distribuição sai do GitHub Releases, gerado pelo workflow `.github/workflows/release.yml` a cada tag `vX.Y.Z`. A configuração da chave e dos segredos está no [README](../README.md#publicar-uma-release). Para o critério de "instala do zero", use o APK baixado da release, não o de depuração (um APK assinado com outra chave não instala por cima do de depuração: desinstale antes).

## Resultado da execução no emulador (11/10/2026, Android 15)

Feito com `adb` num emulador API 35. Não substitui o aparelho real (chamada e SMS são simulados pelo modem do emulador, e não há Samsung/One UI).

| Verificação | Resultado |
|---|---|
| App abre, navega pelas 4 abas | ok |
| Criar Macro, Gatilho com filtro, Ação SMS com Marcadores, ativar | ok |
| Pedido da permissão SMS/Telefone ao adicionar a Ação | ok |
| Notificação real dispara SMS para 2 destinatários com `{app}`/`{texto}` preenchidos | ok |
| 3 atualizações da mesma notificação: nenhum novo Disparo | ok |
| Nova notificação dentro de 5 min: Barrada pelo Limite de frequência | ok |
| Notificação que não passa no Filtro: ignorada | ok |
| Teste: confirmação, SMS com valores de exemplo e chamada iniciada | ok |
| Teto global de chamadas (5/h): bloqueio e notificação de aviso | ok |
| Serviço em primeiro plano (`specialUse`) ligado pelas Configurações | ok |
| E-mail via SMTP real | não testado (precisa de credenciais reais; o cliente SMTP é coberto por testes com GreenMail) |

## Resultado da Ação Enviar WhatsApp (17/10/2026, Samsung Galaxy M53, Android 16, WhatsApp 2.26.40.76)

Feito com `adb` no aparelho do usuário, mandando mensagens para o próprio número (conversa "Você").

| Verificação | Resultado |
|---|---|
| Divulgação ao adicionar a Ação e "Abrir configurações" leva à tela de Acessibilidade | ok (no Samsung o serviço fica em Aplicativos instalados) |
| Prévia do número internacional na tela da Ação | ok |
| Teste da Macro: abre a conversa, envia com `{hora}` preenchido e volta para a tela inicial (~4 s) | ok |
| Critério 8: disparo por notificação real com a tela desbloqueada | ok |
| Critério 9: com a tela bloqueada, nada abre; a mensagem sai ~2 s depois do desbloqueio | ok (o prazo de 30 min não foi esperado no aparelho) |
| Critério 10: número sem WhatsApp falha com o motivo, nenhum convite por SMS é enviado e o número seguinte recebe | ok |
| Critério 11: rascunho na conversa não é enviado (o WhatsApp 2.26 substitui o rascunho pelo texto do link) | ok |
| Critério 12: serviço desligado → alerta na lista, Saúde em vermelho, Registro "Bloqueada: permissão ausente" | ok |
| Atualizar o APK mantém o serviço ligado; forçar a parada desliga | observado |
| Samsung "Freecess" congela o processo em segundo plano, mas a espera de ~2 min com a tela bloqueada funcionou | observado; esperas longas devem ser confirmadas no roteiro |
