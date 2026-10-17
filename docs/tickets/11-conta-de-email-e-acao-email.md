# 11: Conta de e-mail e Ação Enviar e-mail

**Depende de:** 08
**Spec:** §5.6 (e-mail), §8 (credenciais), §9 (telas 4 e 8), [ADR 0001](../adr/0001-email-via-smtp-no-aparelho.md)

## Escopo
- Configurações → Conta de e-mail: presets Gmail/Outlook/Personalizado (servidor, porta, segurança), usuário e senha. A senha fica em armazenamento criptografado e nunca é exibida de volta. Botão "Enviar e-mail de teste".
- Tela da Ação: Destinatários (seletor `Email.CONTENT_URI` ou manual), assunto e corpo com "Inserir marcador".
- `EmailSender` Android: um job do WorkManager por Destinatário, com restrição de rede, backoff exponencial a partir de 30 s e desistência após 30 min desde o Disparo. O job atualiza o resultado no Registro (pendente → enviado/falhou).
- Sem Conta configurada, a Ação falha com uma mensagem clara.

## Critérios de aceite
- [ ] Com uma senha de app do Gmail, o e-mail de teste chega.
- [ ] Um Disparo sem rede fica "pendente" e é enviado quando a rede volta dentro de 30 min.
- [ ] Depois de 30 min sem rede, o Registro mostra "falhou".
- [ ] A senha não aparece em logs, na UI nem no JSON das Macros.
