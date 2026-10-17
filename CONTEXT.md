# D-Macro

App Android de automação pessoal no estilo MacroDroid: o usuário monta Macros que reagem a eventos do aparelho e executam ações em nome dele.

## Language

### Macro e suas partes

**Macro**:
Regra de automação nomeada, composta por um ou mais Gatilhos, uma lista ordenada de Ações e zero ou mais Restrições. Pode estar ativada ou desativada.
_Avoid_: Automação, regra, rotina, tarefa

**Gatilho**:
Evento do aparelho que pode iniciar uma Macro, incluindo os Filtros que definem quais ocorrências desse evento contam. Os Gatilhos de uma Macro se combinam em OU.
_Avoid_: Trigger, evento, disparador

**Filtro**:
Critério que faz parte de um Gatilho e restringe quais ocorrências do evento o satisfazem (ex.: app de origem, título, texto). Os Filtros de um Gatilho se combinam em E. Não é uma Restrição.
_Avoid_: Condição, regra

**Restrição**:
Teste sobre o estado do mundo, avaliado no momento em que um Gatilho é satisfeito. A Macro só segue adiante se todas as Restrições passarem (combinação em E). Uma Restrição nunca inicia uma Macro.
_Avoid_: Condição, constraint, regra

**Ação**:
Passo executado pela Macro. As Ações rodam em sequência, e a falha de uma Ação não interrompe as seguintes.
_Avoid_: Tarefa, comando, passo

### Ciclo de vida

**Disparo**:
Momento em que um Gatilho de uma Macro ativada é satisfeito. Um Disparo pode ser barrado por uma Restrição e não chegar a virar Execução.
_Avoid_: Trigger, ativação, evento

**Execução**:
Processamento das Ações de uma Macro depois de um Disparo cujas Restrições passaram todas. Um Teste também gera uma Execução.
_Avoid_: Run, rodada, disparo

**Teste**:
Execução iniciada manualmente pelo usuário, que ignora Gatilhos e Restrições e preenche os Marcadores com valores de exemplo.
_Avoid_: Simulação, dry-run

**Registro de Execução**:
Entrada do histórico local que descreve um Disparo: o que o causou, qual Restrição o barrou (se alguma barrou) e o resultado de cada Ação.
_Avoid_: Log, histórico, evento

**Limite de frequência**:
Restrição que barra o Disparo se a mesma Macro teve uma Execução há menos de X minutos. Vem ligada por padrão em toda Macro nova.
_Avoid_: Cooldown, throttle

**Teto global**:
Limite fixo, que o usuário não pode desligar, de quantos SMS, chamadas e mensagens de WhatsApp o app inteiro faz por hora. Ao ser atingido, as Ações desse tipo são bloqueadas e o usuário é avisado.
_Avoid_: Limite de segurança, quota

**Chave geral**:
Interruptor único que, desligado, impede qualquer Disparo de qualquer Macro.
_Avoid_: Modo pânico, master switch

### Gatilho de notificação

**Notificação recebida**:
Gatilho satisfeito quando outro app publica ou atualiza uma notificação que passa nos Filtros de app, título e texto.
_Avoid_: Leitura de notificação, alerta

**Notificação em curso**:
Notificação persistente que o usuário não consegue dispensar (música tocando, download, navegação). Pode ser ignorada pelo Gatilho.
_Avoid_: Notificação fixa, ongoing

**Impedir múltiplos disparos**:
Filtro que ignora atualizações da mesma notificação (mesma chave) enquanto ela não for dispensada. Não confundir com o Limite de frequência.
_Avoid_: Debounce, anti-repetição

### Ações de comunicação

**Destinatário**:
Número de telefone ou endereço de e-mail que recebe uma Ação. É uma cópia gravada no momento da escolha, não uma referência viva a um contato da agenda.
_Avoid_: Contato, alvo

**Marcador**:
Trecho do texto de uma mensagem que é trocado por um dado do evento que satisfez o Gatilho (app, título, texto, data/hora) no momento da execução.
_Avoid_: Magic Text, variável, placeholder, tag

**Conta de e-mail**:
Configuração única e global da caixa de e-mail do usuário (servidor SMTP, usuário, senha de app) de onde saem todas as Ações de e-mail.
_Avoid_: Remetente, perfil SMTP, credencial

**Enviar WhatsApp**:
Ação que manda uma mensagem pela conta de WhatsApp do próprio usuário, abrindo a conversa e tocando em "Enviar" por ele. Só acontece com a tela desbloqueada.
_Avoid_: Zap, mensagem automática, bot

**Fila do WhatsApp**:
Fila única do app por onde passam todos os envios de WhatsApp, um de cada vez, porque só existe uma tela. Um envio espera na fila enquanto a tela está bloqueada ou há uma chamada em andamento, até o prazo.
_Avoid_: Agendamento, buffer

### Operação

**Saúde do app**:
Situação das pré-condições para as Macros funcionarem: permissões concedidas, acesso a notificações conectado, serviço de acessibilidade ativo, isenção da otimização de bateria. Uma Macro que depende de algo ausente fica com alerta e não executa.
_Avoid_: Status, diagnóstico
