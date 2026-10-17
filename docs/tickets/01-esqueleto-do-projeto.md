# 01: Esqueleto do projeto

**Depende de:** nenhum ticket
**Spec:** §2, §3, §9 (navegação)

## Escopo
- Projeto Android com o módulo `app`, applicationId `br.com.djavanfernando.dmacro`, minSdk 26 e targetSdk mais recente.
- Gradle com version catalog: Compose (Material 3), Hilt, Room, kotlinx.serialization, WorkManager, DataStore e coroutines.
- Pacotes `engine`, `data`, `platform` e `ui`, com uma regra (lint ou teste) que falha se `engine` importar `android.*`.
- Navegação com barra inferior e quatro destinos vazios: Macros, Registros, Saúde e Configurações.
- Tema claro e escuro seguindo o sistema, todas as strings em `strings.xml` (pt-BR) e `allowBackup="false"`.
- Testes unitários JVM rodando com `./gradlew test`.

## Critérios de aceite
- [ ] `./gradlew assembleDebug test` passa.
- [ ] O app abre no aparelho e navega entre as quatro abas.
- [ ] Um teste falha se uma classe de `engine` importar `android.*`.
