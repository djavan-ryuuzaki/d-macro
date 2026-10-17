# WhatsApp por automação de tela com o Serviço de Acessibilidade

O WhatsApp não oferece API para que um app de terceiros envie mensagens pela conta pessoal do usuário. Decidimos enviar abrindo a conversa já com o texto preenchido (link `api.whatsapp.com/send`) e tocando no botão "Enviar" através de um `AccessibilityService` do D-Macro, como faz o MacroDroid. É o único caminho que manda para **qualquer contato** a partir da **conta pessoal** sem depender de servidor.

## Considered Options

- **Só abrir a conversa com o texto preenchido**: simples, mas exige que alguém toque em "Enviar". Não é automação.
- **Responder pela notificação do WhatsApp** (`RemoteInput`): funciona com a tela bloqueada, mas só para quem acabou de mandar mensagem. Pode virar outra Ação no futuro.
- **WhatsApp Business Cloud API (Meta)**: confiável e funciona com a tela bloqueada, mas a mensagem sai de um número comercial, exige modelos aprovados e cobrança por conversa, e obriga a guardar um token de acesso. Quebraria a decisão de "dados só no aparelho".
- **Bibliotecas não oficiais** (protocolo do WhatsApp Web): violam os termos de uso e podem levar ao banimento do número.

## Consequences

- **Exige a tela desbloqueada.** Com a tela bloqueada, o envio espera o desbloqueio dentro de um prazo e falha depois dele. SMS e chamada continuam sendo os canais para alertas que não podem esperar.
- **Frágil por natureza.** O botão é encontrado pelo id da view e pela descrição de acessibilidade; uma atualização do WhatsApp pode quebrar o envio. A lógica de reconhecimento da tela fica isolada numa função pura, testada com despejos reais da árvore de acessibilidade, para facilitar o conserto.
- **Toma a tela por alguns segundos.** Se o usuário estiver usando o celular, o WhatsApp abre na frente dele.
- **Play Store:** a política do Google para a API de Acessibilidade restringe o uso por apps que não são ferramentas de acessibilidade. Isso reforça o [ADR 0002](0002-apk-direto-antes-da-play-store.md) e provavelmente exigirá remover ou trocar esta Ação antes de uma publicação na loja.
- **Configurações restritas (Android 13+):** em APK instalado fora de uma loja, o Android bloqueia ativar a Acessibilidade (e o acesso a notificações) até o usuário liberar em "Informações do app › ⋮ › Permitir configurações restritas". A Saúde do app precisa guiar esse passo.
- **Termos do WhatsApp:** automação de envio pela conta pessoal é uma zona cinzenta nos termos de uso. O risco é mitigado pelo volume baixo e pelo Teto global, mas não eliminado.
