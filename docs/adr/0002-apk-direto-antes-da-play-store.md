# Distribuição por APK direto antes da Play Store

A Google Play só libera `SEND_SMS` e `CALL_PHONE` para apps aprovados numa revisão manual de exceções, e a aprovação para automação não é garantida. Decidimos distribuir o MVP como APK direto para validar o produto sem depender dessa revisão. Ao mesmo tempo, evitamos escolhas que fechariam a porta da loja depois: por exemplo, a lista de apps nos Filtros usa `<queries>` em vez de `QUERY_ALL_PACKAGES`.

O canal de distribuição é o **GitHub Releases**: um workflow do GitHub Actions gera o APK assinado e publica a release a cada tag `vX.Y.Z`.

## Consequences

- Atualizações não são automáticas no MVP: o usuário precisa instalar cada APK novo.
- A chave de assinatura do release é única e insubstituível: perder a chave impede atualizações por cima das instalações existentes. Ela vive só nos segredos do repositório e numa cópia segura do autor.
- Desde 30/09/2026 o Android no Brasil exige desenvolvedor verificado também fora das lojas. Como o D-Macro não é registrado, quem instala precisa ativar uma vez o "fluxo avançado" (Opções do desenvolvedor, com espera de 24 h) ou instalar por `adb`. Registrar o app no Android Developer Console eliminaria esse passo.
- Toda nova permissão sensível deve ser avaliada pensando na futura publicação na Play Store.
