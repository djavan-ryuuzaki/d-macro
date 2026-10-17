# 18: Motor: Ação Enviar WhatsApp

**Depende de:** 05
**Spec:** §3 (portas), §4, §5.5, §5.6 (WhatsApp: número internacional e papel do Motor), §5.7, §5.8, §5.9

## Escopo
- Modelo: `EnviarWhatsApp(destinatarios: List<Telefone>, mensagem, app: AppWhatsApp = Pessoal)` e `AppWhatsApp { Pessoal("com.whatsapp"), Business("com.whatsapp.w4b") }`, serializáveis (`"tipo": "enviar_whatsapp"`).
- Função pura `numeroWhatsApp(numero): String?` com as regras de §5.6 (DDI 55 padrão; `null` quando não dá para converter).
- Validação: número que não converte para WhatsApp é um problema novo da Macro ("inclua o DDD"); mensagem vazia e lista vazia, como no SMS.
- Porta `WhatsAppSender.enfileirar(idItem, numero, texto, app, prazo): ResultadoAcao` (normalmente devolve `Pendente`; o resultado chega por `ExecutionLog.atualizarItem`).
- No Motor: Marcadores na mensagem, reserva de `TipoTeto.WhatsApp(20)` por Destinatário, `TipoAcao.WhatsApp` no Registro, prazo de 30 min a partir do Disparo.
- `Permissao.Acessibilidade` em `permissoesNecessarias` para Macros com WhatsApp.
- Adaptações mínimas para compilar: `when` exaustivos na UI (resumo, Teste, Registro) e um `WhatsAppSender` provisório que devolve `Erro("ainda não implementado")` até o ticket 20.

## Testes (TDD)
- S2: `numeroWhatsApp` (com `+`, com DDD, com zero à esquerda, sem DDD, curto demais, com máscara); ida e volta do JSON; validação.
- S1 (`MotorTest`): item pendente com a mensagem já com Marcadores e o número internacional; 21º envio bloqueado pelo Teto e aviso uma vez por Execução; Macro bloqueada sem `Acessibilidade`; Teste usa os valores de exemplo.

## Critérios de aceite
- [ ] Todos os testes anteriores continuam passando.
- [ ] Uma Macro com WhatsApp salva, reabre e ativa; com um número sem DDD, não ativa e mostra o problema.
