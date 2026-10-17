# E-mail enviado via SMTP direto do aparelho

O Android não tem API para enviar e-mail sem interação: uma Intent apenas abre o app de e-mail e exige que o usuário toque em "Enviar". Decidimos que as Ações de e-mail falam SMTP direto do aparelho, usando uma Conta de e-mail global configurada pelo usuário (ex.: senha de app do Gmail) e guardada com criptografia (Android Keystore). Assim não dependemos de servidor próprio, e o e-mail sai do endereço do próprio usuário.

## Considered Options

- **Gmail API via OAuth**: rejeitada porque o escopo `gmail.send` exige verificação do Google para um app distribuído, e porque funcionaria só com Gmail.
- **Backend próprio (SendGrid, Resend etc.)**: rejeitado porque exigiria servidor, custo e conta, e porque o e-mail sairia de um remetente nosso, não do usuário.
- **Intent para o app de e-mail**: rejeitada porque não é automação de verdade.

## Consequences

- A senha da Conta de e-mail nunca entra em exportação ou backup de Macros.
- Provedores que desligarem a autenticação SMTP por senha de app vão quebrar essa Ação. Se isso acontecer, voltamos à opção OAuth.
