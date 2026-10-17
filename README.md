<p align="center"><img src="docs/marca/logo-dmacro.png" alt="D-Macro" width="600"></p>

App Android de automação pessoal no estilo MacroDroid. Você monta **Macros**: quando chega uma notificação que passa nos seus filtros, o D-Macro **liga** para um contato e **envia SMS, e-mail e WhatsApp** para quem você escolher, repassando o conteúdo da notificação.

Tudo fica no aparelho: sem conta, sem servidor, sem analytics.

## Instalar

O D-Macro é distribuído só pelo **[GitHub Releases](../../releases)** (não está na Play Store).

1. Na release mais recente, baixe o arquivo `D-Macro-vX.Y.Z.apk` pelo celular.
2. **Brasil, desde 30/09/2026:** o Android exige que apps venham de desenvolvedores verificados pelo Google, e o D-Macro não é registrado. Antes da primeira instalação, ative o "fluxo avançado" uma única vez:
   - Configurações › Sobre o telefone › toque 7 vezes em **Número de compilação** para liberar as Opções do desenvolvedor.
   - Opções do desenvolvedor › **Permitir apps de desenvolvedores não verificados**. Leia os avisos e aguarde a espera de 24 horas que o Android impõe; depois volte à mesma tela para concluir.
3. Abra o APK baixado e permita **instalar apps desconhecidos** para o app que abriu o arquivo (navegador ou Arquivos). O Play Protect pode mostrar um aviso: toque em "Instalar mesmo assim".
4. Opcional: confira o arquivo com o `.sha256` publicado junto.

### Primeiro uso

Abra a aba **Saúde** e deixe tudo verde: acesso a notificações, SMS, Telefone, notificações do D-Macro, bateria e (se for usar WhatsApp) o serviço de acessibilidade. Se o Android não deixar ativar o acesso a notificações ou a acessibilidade, siga o guia **Configurações restritas** da própria aba Saúde (Configurações › Apps › D-Macro › ⋮ › Permitir configurações restritas). Em aparelhos Samsung, siga também o guia de "Apps que nunca entram em suspensão".

### Atualizar

Baixe o APK da nova release e instale por cima: suas Macros são mantidas. O app não se atualiza sozinho; acompanhe a página de releases (ou use um app como o Obtainium, que avisa quando sai versão nova).

## Para desenvolvedores

Especificação em [docs/SPEC.md](docs/SPEC.md), glossário em [CONTEXT.md](CONTEXT.md), decisões em [docs/adr](docs/adr/) e tickets em [docs/tickets](docs/tickets/README.md).

```bash
./gradlew testDebugUnitTest   # testes
./gradlew assembleDebug       # APK de depuração
```

### Publicar uma release

O workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) roda os testes, gera o APK assinado e cria a release a cada tag `vX.Y.Z` (com hífen, como `v0.3.0-beta1`, sai como pré-release). A versão do app vem da tag: `v1.2.3` vira `versionName 1.2.3` e `versionCode 10203`.

Configuração única:

1. Crie a chave de assinatura e **guarde o arquivo e as senhas em lugar seguro** (sem ela não dá para publicar atualizações que instalem por cima):

   ```bash
   keytool -genkeypair -v -keystore dmacro-release.jks -alias dmacro -keyalg RSA -keysize 4096 -validity 10000
   ```

2. No GitHub, em Settings › Secrets and variables › Actions, crie os segredos `DMACRO_KEYSTORE_BASE64` (saída de `base64 -w0 dmacro-release.jks`), `DMACRO_STORE_PASSWORD`, `DMACRO_KEY_ALIAS` e `DMACRO_KEY_PASSWORD`.

Depois, cada release é:

```bash
git tag v0.2.0
git push origin v0.2.0
```

Para gerar o APK assinado localmente, crie um `keystore.properties` na raiz (ignorado pelo git) com `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD` e rode `./gradlew assembleRelease`.
