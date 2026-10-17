# D-Macro: Especificação do MVP

> Vocabulário: ver [CONTEXT.md](../CONTEXT.md). Decisões arquiteturais: ver [docs/adr](adr/).
> Os itens marcados com **[decisão da spec]** foram fixados ao escrever este documento e não foram discutidos na sessão de design. Revise-os.
> A Ação Enviar WhatsApp foi acrescentada depois do MVP ([ADR 0003](adr/0003-whatsapp-por-automacao-de-acessibilidade.md), tickets 18 a 22).

## 1. Objetivo

App Android de automação pessoal, no estilo MacroDroid. No MVP, o usuário cria **Macros** que, ao receber uma notificação que passa nos Filtros e nas Restrições, **ligam** para um Destinatário e/ou **enviam SMS e e-mail** para vários Destinatários. O conteúdo da notificação pode ser repassado através de Marcadores.

### Fora do escopo do MVP

- Outros Gatilhos (localização, horário, bateria, chamada recebida etc.)
- Outras Ações (apps, tela, volume etc.)
- No WhatsApp: grupos, anexos, enviar com a tela bloqueada, responder pela notificação
- Blocos de controle (if/else, loops, esperas) e variáveis
- Curingas `*`/`?`, regex e filtro por som da notificação
- Escolha de chip por Ação (sempre o chip padrão do sistema)
- Grupos de Destinatários reutilizáveis
- Chamada para vários Destinatários com espera entre as ligações
- Exportar/importar Macros (mas o modelo já nasce serializável)
- Outros idiomas além de pt-BR
- Publicação na Play Store ([ADR 0002](adr/0002-apk-direto-antes-da-play-store.md))

## 2. Plataforma

| Item | Valor |
|---|---|
| Nome | D-Macro |
| applicationId | `br.com.djavanfernando.dmacro` |
| minSdk / targetSdk | 26 / o SDK estável mais recente |
| Linguagem / UI | Kotlin, Jetpack Compose (Material 3), tema claro/escuro seguindo o sistema |
| DI / persistência | Hilt, Room, kotlinx.serialization |
| Trabalho em segundo plano | Coroutines, WorkManager (novas tentativas de e-mail) |
| E-mail | SMTP via Jakarta Mail (Android) ([ADR 0001](adr/0001-email-via-smtp-no-aparelho.md)) |
| WhatsApp | `AccessibilityService` que toca em "Enviar" ([ADR 0003](adr/0003-whatsapp-por-automacao-de-acessibilidade.md)) |
| Distribuição | APK assinado, instalado direto |
| Idioma | pt-BR, com todas as strings em `strings.xml` |

## 3. Arquitetura

Um único módulo Gradle `app`, organizado em pacotes:

```
br.com.djavanfernando.dmacro
├── engine/      Kotlin puro, sem import android.*: modelo, Filtros, Restrições, Marcadores, pipeline
├── data/        Room, repositórios, serialização
├── platform/    Implementações Android das portas (SMS, chamada, e-mail, WhatsApp, listener)
└── ui/          Telas Compose + ViewModels
```

O **motor** (`engine`) depende somente de portas (interfaces):

| Porta | Responsabilidade |
|---|---|
| `java.time.Clock` | Instante atual e fuso horário (abstração da própria JVM, sem porta própria) |
| `SmsSender` | Envia um SMS para um número e devolve o resultado |
| `CallPlacer` | Inicia uma chamada para um número |
| `EmailSender` | Enfileira um e-mail para um endereço (resultado posterior via `ExecutionLog.atualizarItem`) |
| `WhatsAppSender` | Põe uma mensagem na Fila do WhatsApp (resultado posterior via `ExecutionLog.atualizarItem`) |
| `ExecutionLog` | Recebe e atualiza Registros de Execução; informa a última Execução não-Teste |
| `FonteMacros` | Fornece as Macros ativadas |
| `ContadorTeto` | Conta e registra usos de SMS, chamadas e WhatsApp na janela do Teto global |
| `ConfiguracoesMotor` | Estado da Chave geral |
| `VerificadorPermissoes` | Diz se uma permissão de plataforma está concedida |
| `Avisos` | Avisa o usuário fora do fluxo (Teto global atingido) |

O motor é desenvolvido com TDD usando fakes dessas portas. O código Android fica restrito às bordas.

A **Fila do WhatsApp** (§5.6) também é Kotlin puro e fica em `engine/whatsapp/`. Ela implementa `WhatsAppSender` e depende das portas abaixo, implementadas em `platform` pelo serviço de acessibilidade:

| Porta | Responsabilidade |
|---|---|
| `EstadoAparelho` | Diz se a tela está bloqueada e se há chamada em andamento; avisa quando isso muda |
| `TelaWhatsApp` | Abre a conversa com o texto, lê a árvore da tela atual como `List<NoTela>`, toca num nó e volta para a tela inicial |
| `PrazoItem` | Agenda a marcação do item como falho no fim do prazo, mesmo se o processo morrer |

## 4. Modelo de domínio

Todo o modelo é serializável com `@Serializable` (hierarquias seladas e polimórficas).

```
Macro
  id: UUID
  nome: String
  ativada: Boolean
  gatilhos: List<Gatilho>            // ≥ 1 para poder ativar
  acoes: List<Acao>                  // ≥ 1 para poder ativar; ordem importa
  restricoes: List<Restricao>

Gatilho (sealed)
  NotificacaoRecebida
    apps: SelecaoApps                // Todos | Somente(Set<packageName>)
    filtroTitulo: FiltroTexto
    filtroTexto: FiltroTexto
    ignorarMaiusculas: Boolean = true
    ignorarEmCurso: Boolean = true      [decisão da spec: padrão true]
    impedirMultiplosDisparos: Boolean = true

FiltroTexto
  modo: Qualquer | Igual | Contem | NaoContem
  termos: List<String>               // ignorado em "Qualquer"; ≥ 1 nos demais modos

Acao (sealed)
  EfetuarChamada(destinatario: Telefone)
  EnviarSms(destinatarios: List<Telefone>, mensagem: String, limiteCaracteres: Int = 300)
  EnviarEmail(destinatarios: List<Email>, assunto: String, corpo: String)
  EnviarWhatsApp(destinatarios: List<Telefone>, mensagem: String, app: AppWhatsApp = Pessoal)

AppWhatsApp
  Pessoal (com.whatsapp) | Business (com.whatsapp.w4b)

Destinatario
  Telefone(numero: String, rotulo: String?)   // rótulo = nome do contato no momento da escolha
  Email(endereco: String, rotulo: String?)

Restricao (sealed)
  FaixaHorario(inicio: LocalTime, fim: LocalTime)    // fim < inicio = passa da meia-noite
  DiasSemana(dias: Set<DayOfWeek>)                   // ≥ 1 dia
  LimiteFrequencia(minutos: Int = 5)
```

Uma Macro nova já vem com `LimiteFrequencia(5)`. O usuário pode removê-la.

## 5. Comportamento

### 5.1 Recepção de notificações

O `NotificationListenerService` converte cada `StatusBarNotification` publicada em um `EventoNotificacao`:

- `pacote` = `sbn.packageName`
- `nomeApp` = rótulo do app, resolvido pelo PackageManager
- `titulo` = `EXTRA_TITLE` (vazio se ausente)
- `texto` = `EXTRA_BIG_TEXT` se existir, senão `EXTRA_TEXT` (vazio se ausente) **[decisão da spec]**
- `chave` = `sbn.key`
- `emCurso` = `FLAG_ONGOING_EVENT` ou `FLAG_FOREGROUND_SERVICE`
- `instante` = horário da recepção

Descartes globais, feitos **antes** de avaliar qualquer Macro:

1. Chave geral desligada
2. Notificação do próprio D-Macro (`pacote == applicationId`)
3. Notificações-resumo de grupo (`FLAG_GROUP_SUMMARY`), porque duplicam as notificações filhas **[decisão da spec]**

### 5.2 Avaliação do Gatilho Notificação recebida

O Gatilho é satisfeito quando **todas** as condições abaixo são verdadeiras:

1. `apps` = Todos, ou `pacote` ∈ Somente
2. Se `ignorarEmCurso`, então `!emCurso`
3. `filtroTitulo` aceita `titulo`
4. `filtroTexto` aceita `texto`
5. Se `impedirMultiplosDisparos`, `chave` não está no conjunto de chaves já disparadas por este Gatilho

Semântica do `FiltroTexto`, aplicando *lowercase* aos dois lados quando `ignorarMaiusculas` está ligado. Não há normalização de acentos **[decisão da spec]**:

| Modo | Aceita quando |
|---|---|
| Qualquer | sempre |
| Igual | o valor é igual a **algum** termo |
| Contém | o valor contém **algum** termo |
| Não contém | o valor não contém **nenhum** termo |

**Impedir múltiplos disparos**: quando o Gatilho é satisfeito, a `chave` entra no conjunto do Gatilho. Ela sai quando a notificação é removida (`onNotificationRemoved`). O conjunto fica em memória e não sobrevive a um reinício do processo **[decisão da spec]**.

### 5.3 Do Disparo à Execução

Para cada Macro ativada cujo Gatilho foi satisfeito, acontece um **Disparo**. Macros diferentes são processadas em paralelo. Os Disparos de uma **mesma** Macro passam por uma fila serializada (um `Mutex` por Macro).

1. Se a Saúde do app reprova a Macro (falta alguma permissão de que ela precisa), grava um Registro "bloqueada: permissão X ausente" e para.
2. Avalia as Restrições em ordem. Na primeira que reprovar, grava um Registro "barrado por <Restrição>" e para.
3. Grava o início da **Execução** (o Limite de frequência passa a contar a partir deste instante).
4. Executa as Ações **em sequência**. Uma falha é registrada e a próxima Ação roda mesmo assim.
5. Fecha o Registro com o resultado de cada Ação/Destinatário.

### 5.4 Restrições

- **FaixaHorario**: com `inicio ≤ fim`, passa se `inicio ≤ agora < fim`. Com `fim < inicio`, passa se `agora ≥ inicio || agora < fim`.
- **DiasSemana**: passa se o dia da semana **no instante do Disparo** está em `dias`.
- **LimiteFrequencia**: passa se não houve Execução desta Macro nos últimos `minutos`. Testes não contam.

### 5.5 Marcadores

Os Marcadores são substituídos na mensagem do SMS, no assunto e corpo do e-mail e na mensagem do WhatsApp:

| Marcador | Valor |
|---|---|
| `{app}` | `nomeApp` |
| `{titulo}` | `titulo` |
| `{texto}` | `texto` |
| `{data}` | `dd/MM/yyyy` do `instante` |
| `{hora}` | `HH:mm` do `instante` |

Marcadores desconhecidos ficam como estão no texto. Valores ausentes viram string vazia. No Teste, os valores são: app "App de exemplo", título "Título de exemplo", texto "Texto de exemplo", data e hora atuais.

### 5.6 Ações

**Efetuar chamada**
- `TelecomManager.placeCall` com `CALL_PHONE`, pelo chip padrão.
- Não espera a chamada terminar: a próxima Ação roda logo em seguida. Sem novas tentativas.
- Conta 1 no Teto global de chamadas.

**Enviar SMS**
- Um envio por Destinatário, via `SmsManager.sendMultipartTextMessage`, pelo chip padrão.
- Depois de aplicar os Marcadores, um texto acima de `limiteCaracteres` é cortado em `limiteCaracteres - 1` caracteres e recebe "…" no fim.
- O resultado vem do `sentIntent` (timeout de 60 s). Em caso de falha, o item fica “pendente” e uma nova tentativa acontece após 1 minuto, sem bloquear as Ações seguintes. Se falhar de novo, registra erro. A nova tentativa não conta de novo no Teto global.
- Cada Destinatário conta 1 no Teto global de SMS, independentemente do número de partes **[decisão da spec]**.
- O editor mostra a estimativa de partes (`SmsManager.divideMessage` sobre o texto de exemplo).

**Enviar e-mail**
- Um e-mail por Destinatário, enviado pela Conta de e-mail via SMTP (STARTTLS/SSL conforme o preset).
- Cada envio vira um `WorkManager` job com restrição de rede e backoff exponencial começando em 30 s. Um segundo job, sem restrição de rede, dispara ao fim do prazo de 30 minutos e marca o item como falho se ele ainda estiver pendente (cobre o caso de ficar sem rede o tempo todo). Falha de autenticação não é tentada de novo.
- O Registro mostra "pendente" até o job terminar, e então "enviado" ou "falhou: <motivo>".
- Sem Conta de e-mail configurada, a Ação falha com "Conta de e-mail não configurada".
- Não conta no Teto global.

**Enviar WhatsApp**
- Uma mensagem por Destinatário, pela conta do app escolhido (WhatsApp ou WhatsApp Business). Sem limite de caracteres.
- **Número internacional.** O WhatsApp exige o número com DDI. A conversão é uma função pura: tira tudo que não é dígito; com `+` no início, usa os dígitos como estão (8 a 15 dígitos); sem `+`, tira zeros à esquerda e, se sobrarem 10 ou 11 dígitos (DDD + número), acrescenta `55` **[decisão da spec: DDI padrão fixo 55]**; com 12 ou 13 dígitos começando por `55`, usa como estão. Qualquer outro formato é inválido para WhatsApp (ex.: número sem DDD).
- O Motor reserva o Teto global e registra o item como "pendente". A próxima Ação roda logo em seguida. Os itens só entram na Fila do WhatsApp **depois** que o Registro é gravado, porque a Fila atualiza esses itens.
- **Fila do WhatsApp**: única no app, processa um item por vez, na ordem de chegada. Para cada item:
  1. **Espera** enquanto a tela estiver bloqueada ou houver chamada em andamento (`AudioManager.mode` em chamada, sem pedir `READ_PHONE_STATE`) **[decisão da spec: esperar em vez de falhar na hora]**. A espera termina no desbloqueio ou no fim da chamada. Tela apagada conta como bloqueada. O adaptador verifica a cada 1 s e exige o aparelho liberado por mais 1,5 s, para não abrir o WhatsApp por cima de uma chamada que a própria Macro acabou de iniciar.
  2. Abre `https://api.whatsapp.com/send?phone=<número>&text=<mensagem>` no pacote do app escolhido.
  3. Lê a tela até reconhecer a conversa (até 15 s). Antes de tocar em "Enviar", confere que o campo de digitação contém a mensagem, para nunca enviar um rascunho do usuário.
  4. Toca em "Enviar" e confirma (até 5 s) que o campo já não contém a mensagem ou que o botão "Enviar" sumiu. Resultado: "enviado". (Depois do envio, o campo mostra a dica "Mensagem" como texto, por isso a confirmação não exige o campo vazio.)
  5. Depois do último item da fila, volta para a tela inicial (`GLOBAL_ACTION_HOME`) **[decisão da spec]**.
- **Prazo de 30 minutos** a partir do Disparo, como no e-mail. Um item ainda pendente no fim do prazo vira "falhou: tela bloqueada até o fim do prazo" (ou o motivo que o segurou). O prazo é garantido por um job do WorkManager, como no e-mail, para cobrir a morte do processo.
- **Falhas imediatas** (o item não espera):
  - WhatsApp escolhido não instalado: "WhatsApp não instalado".
  - Serviço de acessibilidade desligado no momento do envio: "serviço de acessibilidade desligado".
  - O número não tem conta: "número sem WhatsApp". No WhatsApp 2.26 o link abre a tela "Enviar para…" com um painel "Enviar convite por SMS" por cima (texto "…não está no WhatsApp", botões "Enviar convite por SMS" e "Agora não"). O app só toca em botões que apenas fecham ("Agora não", "Cancelar", "OK", nessa ordem), ou usa Voltar; **nunca toca no botão de convite nem na lista de contatos**. A tela "Enviar para…" também aparece **de passagem** antes de uma conversa válida, por isso só conta como número recusado depois de 1,5 s seguidos.
  - A conversa não foi reconhecida a tempo: "tela do WhatsApp não reconhecida".
  - A mensagem continuou no campo depois do toque: "envio não confirmado".
  - O campo tinha outro texto (rascunho do usuário): "a conversa já tinha outro texto digitado; nada foi enviado".
- **Sem novas tentativas** depois de tocar em "Enviar", para nunca mandar a mesma mensagem duas vezes **[decisão da spec]**.
- **Reconhecimento da tela**: uma função pura recebe a árvore (`NoTela`: id da view, classe, texto, descrição, clicável) e decide entre "conversa pronta (nó do Enviar, nó do campo)", "número sem WhatsApp" e "outra tela, aguardar". Procura primeiro pelos ids das views (`…:id/send` e `…:id/entry`, conferidos no WhatsApp 2.26.40.76) e depois pela descrição de acessibilidade **exata** em pt-BR e inglês ("Enviar"/"Send"; o botão de áudio diz "…Solte para enviar." e não pode ser confundido). Os ids e textos ficam num único lugar, para o conserto ser rápido quando o WhatsApp mudar.
- Cada Destinatário conta 1 no Teto global de WhatsApp.

### 5.7 Teto global

- Janela deslizante de 1 hora, global para o app: **20 SMS**, **5 chamadas** e **20 mensagens de WhatsApp** **[decisão da spec: 20 para WhatsApp]**.
- Um envio que estouraria o teto **não acontece**: o Registro marca "bloqueado pelo Teto global" e o app publica uma notificação própria avisando o usuário.
- O usuário não consegue desligar nem alterar o teto. Ele vale também para Testes.

### 5.8 Teste

- O botão "Testar" no editor abre uma confirmação que lista o que vai acontecer ("Ligar para Fulano; enviar SMS para 3 pessoas; enviar e-mail para 2 pessoas; enviar WhatsApp para 1 pessoa"). Com WhatsApp, a confirmação avisa que o app vai abrir o WhatsApp e tocar em "Enviar".
- Ignora Gatilhos, Restrições, Limite de frequência e a Chave geral. Respeita o Teto global e as permissões.
- Gera um Registro de Execução marcado como Teste.

### 5.9 Validação da Macro

- É possível salvar uma Macro incompleta (rascunho), mas ela só pode ser **ativada** se tiver ≥ 1 Gatilho, ≥ 1 Ação e todos os campos obrigatórios válidos: termos não vazios nos modos que exigem termos, ≥ 1 Destinatário, número/e-mail em formato válido (para WhatsApp, um número que se converte para o formato internacional, §5.6) e mensagem não vazia.
- A Macro aparece com alerta na lista quando falta alguma permissão de que ela precisa.

## 6. Permissões e Saúde do app

As permissões são pedidas **na hora em que são necessárias**:

| Permissão | Pedida quando |
|---|---|
| Acesso a notificações (tela do sistema) | Ao adicionar um Gatilho Notificação recebida |
| `SEND_SMS` | Ao adicionar uma Ação Enviar SMS |
| `CALL_PHONE` | Ao adicionar uma Ação Efetuar chamada |
| `POST_NOTIFICATIONS` (33+) | Ao ativar a primeira Macro |
| Serviço de acessibilidade do D-Macro (tela do sistema) | Ao adicionar uma Ação Enviar WhatsApp, depois de uma tela de divulgação |

**Divulgação da acessibilidade.** Antes de levar o usuário às configurações de acessibilidade, o app mostra uma tela própria explicando: o serviço só age dentro do WhatsApp e do WhatsApp Business, só durante um envio, toca apenas em "Enviar" e não lê nem guarda conversas. O usuário pode recusar; nesse caso a Ação é salva, mas a Macro fica com alerta.

**Serviço de acessibilidade.** Declarado com `packageNames="com.whatsapp,com.whatsapp.w4b"`, `canRetrieveWindowContent`, `flagReportViewIds` e `isAccessibilityTool="false"`. Fora de um envio em andamento, o serviço ignora os eventos. Para o motor, o serviço desligado é a permissão `Acessibilidade` ausente (Desfecho "bloqueada: permissão ausente").

O manifesto ganha `<queries>` com os pacotes `com.whatsapp` e `com.whatsapp.w4b`. Abrir o WhatsApp a partir do segundo plano é permitido porque o app tem um serviço de acessibilidade vinculado pelo sistema.

Os Destinatários vêm do seletor de contatos do sistema (`ACTION_PICK` em `Phone.CONTENT_URI` / `Email.CONTENT_URI`), que concede leitura temporária. Por isso o app **não pede `READ_CONTACTS`** **[decisão da spec]**. Também é possível digitar o número ou e-mail manualmente.

A tela **Saúde do app** mostra, cada item com o status e um botão para corrigir:
- Acesso a notificações concedido / listener conectado (se desconectado, chama `requestRebind`)
- Cada permissão em tempo de execução
- Isenção da otimização de bateria (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
- Em aparelhos Samsung: um guia passo a passo para "Apps que nunca entram em suspensão"
- Conta de e-mail configurada (só quando alguma Macro usa e-mail)
- Serviço de acessibilidade ativo e WhatsApp escolhido instalado (só quando alguma Macro usa WhatsApp)
- Guia "Permitir configurações restritas" (Android 13+), mostrado quando o acesso a notificações ou a acessibilidade não estão concedidos, porque em APK instalado fora de uma loja o Android bloqueia esses interruptores até esse passo
- Android 15+: aviso de que as "notificações aprimoradas" podem ocultar conteúdo sensível (códigos), deixando o Gatilho sem disparar

Ao abrir o app, se algo crítico estiver faltando, um banner na tela Macros leva à Saúde do app.

## 7. Execução em segundo plano

- O `NotificationListenerService` é o processo de longa duração; o sistema mantém o vínculo com ele.
- Há um **serviço em primeiro plano opcional** (Configurações, desligado por padrão), do tipo `specialUse`, com a notificação permanente "D-Macro ativo", para aparelhos que encerram o listener.
- O app verifica a conexão do listener ao ser aberto e após o boot (`BOOT_COMPLETED`).

## 8. Persistência

- Room com as tabelas `macros` (id, nome, ativada, JSON serializado das listas), `execucoes` (Registros de Execução) e `execucao_acoes` (resultado por Ação/Destinatário).
- Retenção dos Registros de Execução: o que for menor entre os últimos **7 dias** e as últimas **500 entradas**, aplicada na inserção. Há um botão "Limpar registros".
- O Limite de frequência usa a tabela `execucoes`. O Teto global usa uma tabela própria, `usos_teto`, para que “Limpar registros” não zere o teto. Ambos sobrevivem a reinícios.
- Configurações (Chave geral, serviço em primeiro plano) em DataStore.
- Conta de e-mail: guardada inteira (servidor, porta, segurança, usuário e **senha**) em EncryptedSharedPreferences com chave no Keystore. A senha nunca é exibida de volta e nunca é serializada junto com as Macros.

## 9. Telas

1. **Macros** (inicial): Chave geral no topo, banner de Saúde do app, lista de Macros (nome, resumo do Gatilho, chave de ativação, alerta de problema) e FAB "Nova Macro".
2. **Editor de Macro**: campo de nome e três cartões, Gatilhos (vermelho), Ações (azul) e Restrições (verde), cada um com "+", lista, edição e remoção. As Ações podem ser reordenadas. Menu com Testar e Excluir.
3. **Configurar Gatilho Notificação recebida**: seletor de apps (Todos / escolher; lista de apps com ícone na tela inicial mais apps que já notificaram, via `<queries>`), dois Filtros (modo + lista de termos) e as três opções. Aviso do Android 15.
4. **Configurar Ação**: uma tela por tipo. Seletor de Destinatários (agenda ou manual), campos de texto com botão "Inserir marcador", estimativa de partes do SMS e limite de caracteres. Na Ação WhatsApp: escolha entre WhatsApp e WhatsApp Business (só os instalados), prévia do número internacional de cada Destinatário e aviso de que o envio exige a tela desbloqueada.
5. **Configurar Restrição**: uma tela por tipo.
6. **Registros**: lista cronológica (Macro, horário, resumo do resultado), detalhe com app/título/texto, motivo do bloqueio e resultado por Ação/Destinatário, e "Limpar registros".
7. **Saúde do app**: ver §6.
8. **Configurações**: Conta de e-mail (presets Gmail/Outlook/Personalizado, "Enviar e-mail de teste") e serviço em primeiro plano.

Navegação: barra inferior com Macros, Registros, Saúde e Configurações.

## 10. Privacidade e segurança

- Nenhum dado sai do aparelho, exceto os próprios SMS, chamadas, e-mails e mensagens de WhatsApp configurados pelo usuário. Sem analytics.
- O serviço de acessibilidade só recebe eventos do WhatsApp, só lê a tela durante um envio e não grava nada do que lê (nem nos Registros, nem em logs).
- Os Registros guardam conteúdo de notificações. Ficam só no aparelho e têm retenção limitada.
- `android:allowBackup="false"` e `dataExtractionRules` excluindo tudo (backup na nuvem e transferência entre aparelhos), para que Registros e credenciais nunca saiam do aparelho **[decisão da spec]**.

## 11. Estratégia de testes

- **Motor**: testes unitários JVM (TDD) cobrindo cada regra das seções 5.1 a 5.8, com fakes das portas e um `Clock` controlável.
- **Dados**: testes de Room (instrumentados ou com Robolectric) para a serialização ida e volta de Macros e a retenção dos Registros.
- **Fila do WhatsApp**: testes unitários JVM com fakes de `EstadoAparelho`, `TelaWhatsApp` e `PrazoItem` e tempo virtual (`TestScope`).
- **Reconhecimento da tela do WhatsApp**: testes da função pura com árvores capturadas de um aparelho real (`uiautomator dump`, WhatsApp em pt-BR e em inglês), guardadas como fixtures.
- **Plataforma**: testes manuais guiados num aparelho físico (Samsung). Um roteiro de testes manuais acompanha os tickets de SMS, chamada, e-mail e WhatsApp.

## 12. Critérios de aceite do MVP

1. Uma Macro "notificação do app X contendo 'alarme' → ligar para A; SMS para B e C com `{titulo}: {texto}`; e-mail para D" funciona num Samsung real com a tela bloqueada.
2. Atualizações repetidas da mesma notificação disparam a Macro uma única vez.
3. Com o Limite de frequência de 5 min, uma segunda notificação dentro da janela é barrada e aparece no Registro.
4. O 21º SMS em uma hora é bloqueado e o usuário recebe um aviso.
5. Desligar a Chave geral impede todos os Disparos.
6. Revogar a permissão de SMS faz a Macro aparecer com alerta e a Execução ser bloqueada com o motivo no Registro.
7. Uma falha de e-mail sem rede é tentada de novo e enviada quando a rede volta (dentro de 30 min).

### Critérios de aceite da Ação Enviar WhatsApp

8. Com a tela desbloqueada, uma Macro "notificação contendo 'alarme' → WhatsApp para A com `{titulo}: {texto}`" envia a mensagem pela conta pessoal e volta para a tela inicial.
9. Com a tela bloqueada, o item fica "pendente" e é enviado logo depois do desbloqueio (dentro de 30 min); depois de 30 min bloqueada, vira "falhou".
10. Um número sem conta no WhatsApp gera "número sem WhatsApp" no Registro, e os outros Destinatários da mesma Ação são enviados.
11. Com um rascunho diferente já digitado na conversa do Destinatário, o app não envia o rascunho.
12. Desligar o serviço de acessibilidade faz a Macro aparecer com alerta e a Execução ser bloqueada com o motivo no Registro.
