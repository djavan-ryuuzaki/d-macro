# 21: Tela da Ação WhatsApp, divulgação da acessibilidade e Saúde

**Depende de:** 18 (a ativação do serviço só funciona de verdade com o 20)
**Spec:** §5.8, §6 (divulgação, Saúde), §9 (tela 4)

## Escopo
- "Enviar WhatsApp" na lista de Ações do editor.
- Tela da Ação: Destinatários pelo seletor de contatos (`Phone.CONTENT_URI`) ou digitados, com a prévia do número internacional ao lado de cada um (ou o aviso "inclua o DDD"); mensagem com "Inserir marcador"; escolha WhatsApp / WhatsApp Business, só entre os instalados (sem nenhum instalado, a tela avisa e não deixa confirmar); aviso de que o envio exige a tela desbloqueada.
- Divulgação: ao adicionar a Ação sem o serviço ativo, uma tela explica o que o serviço faz e não faz (§6) e oferece "Abrir configurações de acessibilidade" (`ACTION_ACCESSIBILITY_SETTINGS`) ou "Agora não".
- Saúde do app:
  - "Serviço de acessibilidade" e "WhatsApp instalado" (só quando alguma Macro usa WhatsApp), cada um com o botão de corrigir.
  - Guia "Permitir configurações restritas" (Android 13+) quando o acesso a notificações ou a acessibilidade estiverem faltando: abre a tela de informações do app (`ACTION_APPLICATION_DETAILS_SETTINGS`) e mostra os passos (⋮ › Permitir configurações restritas).
  - A Macro com WhatsApp e serviço desligado aparece com alerta na lista.
- Teste: a confirmação lista "enviar WhatsApp para N pessoas" e avisa que o WhatsApp vai abrir.
- Registros: rótulo do tipo WhatsApp e os motivos de falha de §5.6.
- Notificação de Teto global atingido também para WhatsApp.
- Todas as strings novas em `strings.xml`.

## Testes
- S5: sem testes automatizados de UI.

## Critérios de aceite
- [ ] Do zero, num APK instalado pelo navegador ou gerenciador de arquivos (Android 13+), dá para ativar o serviço seguindo só o que o app mostra.
- [ ] Recusar a divulgação salva a Ação e deixa a Macro com alerta.
- [ ] O lint do release continua sem erros.
