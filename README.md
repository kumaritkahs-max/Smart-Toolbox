# GitHub Control — Native Android Client

A complete native Android GitHub client written in Kotlin + Jetpack Compose + Material 3.
Replit cannot build native APKs, so the source is delivered for Android Studio.

## Build in Android Studio

1. Install **Android Studio Hedgehog (2023.1)** or newer (Iguana / Koala / Ladybug all work).
2. `File → Open` and select the `android-app/` folder.
3. When prompted, accept the JDK (17+) and **Android SDK 35** download.
4. Wait for **Gradle Sync** to finish (first sync downloads ~400 MB of dependencies).
5. Pick a device or emulator (API 26+) and click ▶ **Run 'app'**.

To produce an installable APK without the IDE:

```
cd android-app
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # signed release (configure signing first)
```

## Stack

- **Language**: Kotlin 2.0.10 (K2 compiler)
- **UI**: Jetpack Compose, Material 3 (`androidx.compose.bom:2024.10.00`)
- **DI**: Hilt 2.52 + KSP
- **Persistence**: Room (encrypted accounts via EncryptedSharedPreferences) + DataStore
- **HTTP**: Retrofit 2 + OkHttp + kotlinx.serialization
- **Git engine**: JGit 6.10 (clone/fetch/pull/push/commit/branches/stash/reset)
- **Background work**: WorkManager + Hilt-Worker
- **Auth**: GitHub Personal Access Tokens (PAT) + AndroidX BiometricPrompt unlock
- **Min SDK**: 26 (Android 8.0). **Target SDK**: 35 (Android 15).

## Features

- Multi-account PAT sign-in with biometric / device-credential unlock and auto-lock
- Encrypted token vault, scope display, token validation
- Repo browser: my repos / starred, sort + filter, create / fork / star / watch / transfer / archive / delete
- Repository detail with README, branches, languages, contributors, releases
- File explorer + raw / image / markdown preview, raw download
- Folder / multi-file / ZIP upload preserving full hierarchy via the **Git Data API** (atomic blob → tree → commit → ref-update)
- Conflict modes: overwrite / skip / rename, pause / resume / cancel, foreground service notifications
- Branch CRUD (create / rename / delete / protect badge)
- Commits list with pagination, commit detail with split / unified diff and whitespace toggle
- Pull Requests: list / open / merge / squash / rebase / close / reopen / create
- Issues: list / open / comment / close / reopen / labels / assignees / create
- GitHub Actions: workflows list, recent runs with status badges, manual dispatch
- Search: repos / code / users
- Analytics: rate limit, language pie, top contributors
- Sync jobs (folder → repo) with WorkManager
- In-app notifications inbox (mark read / unread)
- Plugin registry, downloads inbox
- **Command Mode** terminal for power users
- **Dangerous Mode** toggle gating destructive ops
- Theme: system / light / dark, dynamic color on Android 12+

## Project layout

```
android-app/
├── build.gradle.kts            # Root Gradle (Kotlin DSL)
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts        # App module
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                # values / drawable / mipmap / xml backup-rules
│       └── java/com/githubcontrol/
│           ├── App.kt          # Hilt @HiltAndroidApp + notification channels
│           ├── MainActivity.kt
│           ├── ai/             # offline commit-message heuristic
│           ├── data/
│           │   ├── api/        # Retrofit + Models + RetrofitClient
│           │   ├── auth/       # AccountManager / SessionGate / TokenValidator
│           │   ├── db/         # Room (4 entities + 4 DAOs)
│           │   ├── git/        # JGitService
│           │   ├── repository/ # GitHubRepository façade
│           │   └── AppModule.kt
│           ├── notifications/  # Notifier + channel ids
│           ├── plugins/        # PluginRegistry
│           ├── ui/
│           │   ├── AppRoot.kt          # NavHost + theme + SessionGate routing
│           │   ├── components/         # GhCard / GhBadge / StatPill / etc.
│           │   ├── navigation/Routes.kt
│           │   ├── theme/              # GitHub-flavored Material 3
│           │   └── screens/            # 29 Compose screens (auth, repos,
│           │                           # files, upload, commits, PRs,
│           │                           # issues, actions, search, sync,
│           │                           # notifications, plugins, downloads,
│           │                           # command, settings, analytics,
│           │                           # branches, accounts)
│           ├── upload/         # UploadManager + ConflictMode
│           ├── utils/          # Base64Ext / ByteFormat / Diff / GitignoreMatcher / RelativeTime
│           ├── viewmodel/      # 18 Hilt ViewModels
│           └── worker/         # SyncWorker + UploadWorker
```

## First run

1. Generate a PAT at <https://github.com/settings/tokens> (classic).
   Recommended scopes: **`repo`, `workflow`, `read:user`, `notifications`, `delete_repo`** (only if you want delete).
2. Paste the token on the first screen and tap **Sign in**.
3. The token is encrypted at rest (Tink-backed `EncryptedSharedPreferences`).
4. Optional: enable biometric unlock from **Settings**.
