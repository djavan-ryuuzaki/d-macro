# Tickets do MVP

Especificação: [../SPEC.md](../SPEC.md) · Glossário: [../../CONTEXT.md](../../CONTEXT.md) · Roteiro de aceite: [../roteiro-de-aceite.md](../roteiro-de-aceite.md)

Legenda de status: **feito** = implementado e verificado (testes automatizados e/ou emulador); **falta aparelho** = implementado, mas algum critério de aceite só pode ser confirmado num celular real.

| # | Ticket | Depende de | Status |
|---|---|---|---|
| 01 | [Esqueleto do projeto](01-esqueleto-do-projeto.md) | nenhum | feito |
| 02 | [Motor: modelo e Filtros](02-motor-modelo-e-filtros.md) | 01 | feito (as regras com estado foram para o 05) |
| 03 | [Motor: Restrições](03-motor-restricoes.md) | 02 | feito |
| 04 | [Motor: Marcadores](04-motor-marcadores.md) | 02 | feito |
| 05 | [Motor: pipeline, Teto global, Chave geral](05-motor-pipeline.md) | 02, 03, 04 | feito |
| 06 | [Persistência](06-persistencia.md) | 05 | feito |
| 07 | [Telas Macros e Editor](07-lista-e-editor-de-macros.md) | 06 | feito (emulador) |
| 08 | [Gatilho Notificação recebida de ponta a ponta](08-gatilho-notificacao-ponta-a-ponta.md) | 07 | feito (emulador) |
| 09 | [Ação Enviar SMS](09-acao-enviar-sms.md) | 08 | falta aparelho (modo avião / operadora real) |
| 10 | [Ação Efetuar chamada](10-acao-efetuar-chamada.md) | 08 | falta aparelho (tela bloqueada) |
| 11 | [Conta de e-mail e Ação Enviar e-mail](11-conta-de-email-e-acao-email.md) | 08 | falta aparelho (Gmail real, cenários sem rede) |
| 12 | [Restrições na UI](12-restricoes-na-ui.md) | 07 | feito (Limite padrão verificado; Faixa/Dias não exercitados no emulador) |
| 13 | [Tela Registros](13-tela-de-registros.md) | 06 | feito (emulador) |
| 14 | [Saúde do app](14-saude-do-app.md) | 09, 10, 11 | feito (emulador); guia Samsung falta aparelho |
| 15 | [Teste de Macro e aviso de Teto](15-teste-de-macro-e-aviso-de-teto.md) | 09, 10, 11 | feito (emulador) |
| 16 | [Serviço em primeiro plano opcional](16-servico-em-primeiro-plano.md) | 08 | feito (emulador); religar no boot falta aparelho |
| 17 | [Roteiro de aceite do MVP](17-roteiro-de-aceite-do-mvp.md) | todos | roteiro escrito; execução no aparelho e assinatura do release pendentes |

```
01 → 02 ─┬→ 03 ─┐
         └→ 04 ─┴→ 05 → 06 ─┬→ 07 ─┬→ 08 ─┬→ 09 ─┐
                            │      │      ├→ 10 ─┼→ 14, 15 → 17
                            │      │      ├→ 11 ─┘
                            │      │      └→ 16
                            │      └→ 12
                            └→ 13
```

## Ação Enviar WhatsApp (pós-MVP)

Decisão: [ADR 0003](../adr/0003-whatsapp-por-automacao-de-acessibilidade.md). Spec: §5.6 (Enviar WhatsApp) e critérios 8 a 12 de §12.

| # | Ticket | Depende de | Status |
|---|---|---|---|
| 18 | [Motor: Ação Enviar WhatsApp](18-motor-acao-enviar-whatsapp.md) | 05 | feito |
| 19 | [Fila do WhatsApp e reconhecimento da tela](19-fila-e-reconhecimento-da-tela-do-whatsapp.md) | 18 | feito (fixtures do WhatsApp 2.26.40.76 pt-BR; inglês e Business não capturados) |
| 20 | [Serviço de acessibilidade e adaptador Android](20-servico-de-acessibilidade-whatsapp.md) | 19 | feito (Samsung Galaxy M53, Android 16) |
| 21 | [Tela da Ação, divulgação e Saúde](21-ui-acao-whatsapp-e-saude.md) | 18 | feito (aparelho); fluxo de configurações restritas não exercitado (APK instalado por adb) |
| 22 | [Roteiro de aceite do WhatsApp](22-roteiro-de-aceite-whatsapp.md) | 18 a 21 | critérios 8, 10, 11 e 12 ok; 9 ok até o desbloqueio (prazo de 30 min só nos testes automatizados) |

```
18 ─┬→ 19 → 20 ─┐
    └→ 21 ──────┴→ 22
```

Novo ponto de teste **S6**: a Fila do WhatsApp com fakes de `EstadoAparelho`, `TelaWhatsApp` e `PrazoItem` e tempo virtual.

## Testes automatizados (94)

| Ponto de teste | Classe | Testes |
|---|---|---|
| S2: funções puras | `FiltroTextoTest`, `NotificacaoRecebidaTest`, `ValidacaoMacroTest`, `MacroJsonTest`, `RestricaoTest`, `MarcadoresTest`, `TextoSmsTest`, `NumeroWhatsAppTest`, `ReconhecimentoTelaTest` | 45 |
| S1: fachada do Motor | `MotorTest` | 21 |
| S6: Fila do WhatsApp | `FilaWhatsAppTest` | 12 |
| S3: repositórios (Room/Robolectric) | `MacroRepositoryTest`, `RegistroRepositoryTest` | 12 |
| S4: cliente SMTP (GreenMail) | `SmtpClienteTest` | 3 |
| Isolamento do motor | `EngineIsolationTest` | 1 |

Rodar: `./gradlew testDebugUnitTest`
