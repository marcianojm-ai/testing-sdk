# AppPayBack Android SDK

SDK Android do AppPayBack para vincular instalações do teste fechado e confirmar
o uso diário do aplicativo. A integração é automática e não exige código na
`MainActivity`.

## Requisitos

- Android 6.0 (API 23) ou superior
- projeto Android com repositórios configurados em `settings.gradle.kts`
- aplicativo cadastrado no painel AppPayBack com o mesmo `applicationId`

## Instalação

### 1. Adicione o JitPack

No `settings.gradle.kts` da raiz do projeto, acrescente o JitPack ao bloco de
repositórios existente:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Adicione a dependência

No `build.gradle.kts` do módulo do aplicativo:

```kotlin
dependencies {
    implementation("com.github.marcianojm-ai:testing-sdk:1.1.0")
}
```

Depois, clique em **Sync Now**.

## Uso

Nenhuma chamada manual é necessária. O SDK inicia automaticamente quando o
aplicativo abre. Não adicione código à `MainActivity`, não informe SHA-256 e não
inclua token, senha ou chave Firebase no aplicativo.

O fluxo esperado é:

1. o desenvolvedor cadastra o `applicationId` no painel AppPayBack;
2. incorpora a dependência acima e publica o aplicativo no teste fechado;
3. o testador entra no grupo oficial e instala pelo link preparado no convite;
4. na primeira abertura, o SDK lê o Install Referrer da Google Play e vincula a
   instalação à conta que criou o convite;
5. com o aplicativo visível e a internet validada, o SDK envia os heartbeats do
   check-in diário automaticamente.

Trocas entre telas do próprio aplicativo preservam o check-in. Sair do aplicativo,
perder a rede ou encerrar o processo interrompe a tentativa em andamento; quando o
uso é retomado, uma nova tentativa começa do zero. Somente o servidor confirma um
dia válido.

## Privacidade e segurança

- o convite temporário nunca é gravado em logs;
- a credencial definitiva é gerada no aparelho e armazenada com AES-GCM e Android
  Keystore em uma área excluída de backup;
- o backend armazena apenas o hash da credencial;
- o SDK lê somente o payload versionado do AppPayBack no Install Referrer e não
  armazena parâmetros comuns de atribuição;
- a variante publicada usa exclusivamente o endpoint HTTPS do backend.

## Desenvolvimento do SDK

O build `debug` aponta para o backend local em `127.0.0.1:5001`; o build `release`
aponta para o backend HTTPS. Em aparelho USB, o aplicativo interno de QA deve liberar
HTTP apenas no manifest de debug e usar:

```powershell
adb reverse tcp:5001 tcp:5001
```

Validação completa:

```powershell
./gradlew.bat :sdk-core:testDebugUnitTest :sdk-core:testReleaseUnitTest `
  :sdk-core:assembleDebug :sdk-core:assembleRelease
```

Para validar a publicação Maven local:

```powershell
./gradlew.bat :sdk-core:publishReleasePublicationToMavenLocal
```

O release só deve ser publicado depois que o endpoint de produção estiver implantado
e o fluxo completo for repetido em uma instalação real da faixa da Google Play.
