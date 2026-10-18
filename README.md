# AppPayBack Android SDK

[English](#english) · [Português](#português)

## English

The AppPayBack Android SDK links closed-test installs to AppPayBack testers and
confirms that the app was used each day. Integration is automatic: no code in
`MainActivity`, no keys and no SHA-256 to paste anywhere.

### Requirements

- Android 6.0 (API 23) or newer
- Kotlin 1.8+ or Java-only projects (the SDK is compiled to Java 8 bytecode with
  Kotlin language/API level 1.8)
- the app registered in the AppPayBack dashboard with the same `applicationId`

### Installation

1. Add JitPack to the repositories in the root `settings.gradle.kts`:

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

2. Add the dependency to the app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.marcianojm-ai:testing-sdk:1.2.1")
}
```

3. Click **Sync Now**. That's it: the SDK starts by itself when the app opens.

### What else the app needs

Nothing beyond the two steps above: no code, no extra permissions, no keys and
no configuration files. Merged into your build automatically:

- **Permissions:** `INTERNET` and `ACCESS_NETWORK_STATE` (normal permissions,
  granted at install time; the user is never prompted).
- **Startup:** the `com.apppayback.sdk.AppPayBackInitProvider` component.
- **R8/ProGuard:** the SDK's keep rules (nothing to add to `proguard-rules.pro`).
- **Libraries:** Google Play Install Referrer and the Kotlin standard library.

Project requirements: `minSdk` 23+, the SDK build distributed through the
Google Play closed testing track with a `versionCode` higher than production,
and testers installing through the AppPayBack site button.

Only if your project has these settings:

- JitPack block filtered with `content { ... }`: include the
  `com.github.marcianojm-ai` group.
- `tools:node="remove"`/`"removeAll"` on `<provider>`: do not remove
  `com.apppayback.sdk.AppPayBackInitProvider`, or the SDK will not start.
- `network_security_config` with a domain allowlist or certificate pinning:
  allow `apppaybacktesting.web.app` (HTTPS only).

To keep the SDK out of your production build, add it to a closed-testing
flavor only, for example `"alphaImplementation"("com.github.marcianojm-ai:testing-sdk:1.2.1")`.

### How it works

1. You register the `applicationId` in the AppPayBack dashboard, add the
   dependency and publish the app to your closed testing track.
2. The first open of your app associates it with your AppPayBack listing.
3. A tester joins your Google Group and installs through the AppPayBack invite
   link. On the first open, the SDK reads the AppPayBack invite from the Google
   Play Install Referrer and links that install to the tester's account.
4. While the app is visible and online, the SDK sends short heartbeats. After
   about one minute of continuous use, the server confirms the day.

Moving between screens of your app keeps the check-in going. Leaving the app,
losing the connection or killing the process interrupts the current attempt;
the next attempt starts from zero. Only the server confirms a valid day.

### Check-in messages for testers

For linked testers only, the SDK shows two short toasts: one when the daily
check-in starts ("keep the app open for 1 minute") and one when it is
confirmed. Texts follow the device language (Portuguese, English or Spanish,
chosen on the device) and can be updated by AppPayBack without a new SDK
release. People who did not
install through an AppPayBack invite never see them.

To turn them off, add this inside `<application>` in your `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.apppayback.sdk.SHOW_CHECKIN_MESSAGES"
    android:value="false" />
```

### Network and battery

- Without a tester link: one status request when the app opens; once the app
  is associated, at most once a day per app version.
- For linked testers: one request to start the check-in, a heartbeat every
  10 seconds while the app is in the foreground, and nothing once the day is
  confirmed.
- Nothing runs in the background: no services, no WorkManager, no alarms.
- Requests use HTTPS to `apppaybacktesting.web.app`, with a 10-second timeout.

### Google Play Data safety

Answer the form based on your whole app. For the AppPayBack SDK, mark only:

| Data type | What the SDK sends | Collected | Shared | Purpose |
| --- | --- | --- | --- | --- |
| Device or other IDs | A random installation ID created by the SDK, and the participation credential of linked testers | Yes | No | App functionality |
| App activity → App interactions | When the app is open during a daily check-in (start, heartbeats, finish) | Yes | No | App functionality |

Besides these, the SDK sends only your package name, your app's signing
certificate hash and the SDK version, which describe your app, not the user.

- The SDK does **not** collect names, email addresses, location, contacts,
  advertising ID, Android ID, device model, Android version, language, photos,
  files or any content from your app.
- Data is encrypted in transit (HTTPS) and processed by AppPayBack only to run
  the closed test.
- The participation credential is stored on the device with AES-GCM and the
  Android Keystore, in an area excluded from backups; the server stores only
  its hash.

This is guidance, not legal advice; you remain responsible for your app's
Data safety answers and privacy policy.

### Migrating from 1.1.0

Change the version to `1.2.1`. Nothing else is required: existing tester links
and check-ins keep working after the update. If your code ever called
`TestingSDK` directly (not needed for normal use), use `AppPayBack` instead.

### Changelog

**1.2.1**

- Sends only the SDK version; app version, Android version and device
  language are no longer sent (simpler Data safety answers).
- Check-in texts arrive in all languages and the device picks its own.

**1.2.0**

- Check-in toasts for linked testers (PT/EN/ES), with a manifest opt-out.
- Message texts can be updated from the server.
- Reports the SDK version with the check-in.
- Uses the AppPayBack site domain (`/sdk`) instead of the Cloud Functions URL.
- Network timeouts raised from 3 s to 10 s for slow mobile networks.
- The status request runs at most once a day per app version.
- Invites refused for good (expired, already used) are not retried on every open.
- Java 8 bytecode and Kotlin 1.8 API level for wider compatibility.
- Package renamed to `com.apppayback.sdk`; Logcat tag `AppPayBack`.

**1.1.0** — automatic linking through the Install Referrer and daily check-ins.

### Developing the SDK

The `debug` build targets the local emulator at `127.0.0.1:5001`; the `release`
build targets `https://apppaybacktesting.web.app/sdk`. On a USB device:

```powershell
adb reverse tcp:5001 tcp:5001
```

Full validation:

```powershell
./gradlew.bat :sdk-core:testDebugUnitTest :sdk-core:testReleaseUnitTest `
  :sdk-core:assembleDebug :sdk-core:assembleRelease `
  :sdk-core:publishReleasePublicationToMavenLocal
```

---

## Português

O SDK Android do AppPayBack vincula instalações do teste fechado aos testadores
do AppPayBack e confirma o uso diário do aplicativo. A integração é automática:
nenhum código na `MainActivity`, nenhuma chave e nenhum SHA-256 para informar.

### Requisitos

- Android 6.0 (API 23) ou superior
- Kotlin 1.8+ ou projetos só em Java (bytecode Java 8, Kotlin nível 1.8)
- aplicativo cadastrado no painel AppPayBack com o mesmo `applicationId`

### Instalação

No `settings.gradle.kts` da raiz, inclua `maven { url = uri("https://jitpack.io") }`
nos repositórios (veja o exemplo em inglês acima). No `build.gradle.kts` do
módulo do aplicativo:

```kotlin
dependencies {
    implementation("com.github.marcianojm-ai:testing-sdk:1.2.1")
}
```

Clique em **Sync Now**. O SDK inicia sozinho quando o aplicativo abre.

### O que mais o app precisa

Nada além disso: sem código, sem permissões extras, sem chaves e sem arquivos de
configuração. Entram sozinhos no build as permissões `INTERNET` e
`ACCESS_NETWORK_STATE` (normais, concedidas na instalação, sem perguntar ao
usuário), o componente `com.apppayback.sdk.AppPayBackInitProvider`, as regras
de R8/ProGuard e as bibliotecas Install Referrer e Kotlin.

Requisitos: `minSdk` 23+, build com o SDK na faixa de teste fechado com
`versionCode` maior que o da produção, e testadores instalando pelo botão do
site. Só se o projeto tiver: repositório JitPack filtrado com `content { ... }`
(inclua o grupo `com.github.marcianojm-ai`); `tools:node="remove"` em
`<provider>` (não remova o `AppPayBackInitProvider`); `network_security_config`
restrito ou com fixação de certificado (libere `apppaybacktesting.web.app`).

### Avisos de check-in

Somente para testadores vinculados, o SDK mostra dois avisos curtos: quando o
check-in do dia começa ("mantenha o app aberto por 1 minuto") e quando é
confirmado, no idioma do aparelho (português, inglês ou espanhol). Para
desligar, adicione dentro de `<application>` no `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.apppayback.sdk.SHOW_CHECKIN_MESSAGES"
    android:value="false" />
```

### Segurança dos dados (Google Play)

Marque apenas:

- **IDs do dispositivo ou outros IDs**: coletado, não compartilhado,
  finalidade "Funcionalidade do app" (ID aleatório da instalação e credencial
  de participação do testador);
- **Atividade no app → Interações com o app**: coletado, não compartilhado,
  finalidade "Funcionalidade do app" (horários do check-in com o app aberto).

Além disso, o SDK envia só o nome do pacote, o hash do certificado de
assinatura do app e a versão do SDK, que descrevem o app e não o usuário. O SDK
não coleta nome, e-mail, localização, contatos, ID de publicidade, Android ID,
modelo do aparelho, versão do Android, idioma, fotos, arquivos nem conteúdo do
aplicativo. Os dados trafegam criptografados (HTTPS). Isto é uma orientação,
não aconselhamento jurídico.

### Atualizando da 1.1.0

Troque a versão para `1.2.1`. Vínculos e check-ins existentes continuam
funcionando. Se o seu código chamava `TestingSDK` diretamente (o uso normal não
precisa), use `AppPayBack`.
