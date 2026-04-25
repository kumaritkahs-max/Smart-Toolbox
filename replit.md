# GitHub Control · Android (com.githubcontrol)

A native Android client for GitHub written in Kotlin with Jetpack Compose + Material 3.
This Replit workspace **does not build APKs** — Android tooling and the Java/Kotlin
toolchain required for AGP (Android Gradle Plugin) are not provisioned here. Builds
are produced by GitHub Actions; Replit only hosts the source and a small status
webserver on port 5000 (`tools/server.py`) so the workspace preview shows the
project structure.

## Stack
- Kotlin 2.0.10, AGP 8.5.2, compileSdk 35, minSdk 26
- Jetpack Compose BOM 2024.10, Material 3
- Hilt 2.52 (DI), Room 2.6.1, WorkManager, EncryptedSharedPreferences, DataStore
- Retrofit + OkHttp + kotlinx.serialization for the GitHub REST API
- JGit for the optional in-app Git mode
- Coil for images, Markwon for Markdown

## Module layout (Kotlin)
```
com.githubcontrol
├─ App.kt, MainActivity.kt
├─ data/
│  ├─ api/         Retrofit GitHubApi, RetrofitClient, model classes
│  ├─ auth/        Account, AccountManager, TokenValidator, ScopeCatalog
│  ├─ db/          Room DAOs + AppDatabase
│  └─ repository/  GitHubRepository (high-level facade)
├─ ui/
│  ├─ AppRoot.kt   Compose nav graph
│  ├─ navigation/  Routes + helpers
│  ├─ components/  GhCard, GhBadge, EmbeddedTerminal, ConflictDialog, …
│  └─ screens/     One folder per feature surface
├─ viewmodel/      Hilt-injected ViewModels
├─ upload/         UploadManager (resumable, parallel, dry-run)
├─ worker/         SyncWorker, UploadWorker (WorkManager)
├─ notifications/  Notifier + channels
└─ utils/          Logger (ring buffer), ShareUtils, Diff, RelativeTime, …
```

## Cross-cutting features
- **Logger ring buffer** (`utils/Logger.kt`) feeds every `EmbeddedTerminal` and the
  full `LogScreen`. Sensitive values (`token=`, `Authorization`, `Bearer`) are
  redacted before storage. Capacity is 1500 entries.
- **Token validation** (`data/auth/TokenValidator.kt`) records HTTP code, scopes,
  rate-limit, token type/expiry into `TokenValidation` snapshots that are stored on
  the `Account` (last 20 retained) and rendered in `AccountsScreen` + `LoginScreen`.
- **ScopeCatalog** describes every OAuth scope with a risk level for color-coded
  rendering and recommends a default set (`repo`, `read:user`, `read:org`,
  `notifications`, `workflow`).
- **EmbeddedTerminal** is mounted on Login, Tree, Upload, Sync, Command, Profile,
  SshKeys, BranchProtection, Compare, Collaborators, and is reachable globally via
  Settings → Tools → Terminal log.

## Notable screens added in this iteration
| Route                         | Screen                       |
|-------------------------------|------------------------------|
| `Routes.LOGS`                 | `LogScreen`                  |
| `Routes.PROFILE_EDIT`         | `ProfileEditScreen`          |
| `Routes.SSH_KEYS`             | `SshKeysScreen`              |
| `Routes.BRANCH_PROTECTION`    | `BranchProtectionScreen`     |
| `Routes.COMPARE`              | `CompareScreen`              |
| `Routes.COLLABORATORS`        | `CollaboratorsScreen`        |

These are linked from `SettingsScreen.Tools/Account` and from the `RepoDetailScreen`
"Administration" card. The TreeScreen now supports multi-select, bulk delete,
and ZIP-folder download via the Storage Access Framework.

## Building the APK
The Replit container can NOT compile Android. Use the GitHub Actions workflow
(`.github/workflows/android.yml`) which runs `gradlew assembleDebug` on Ubuntu
with JDK 17 + the Android SDK. The signed/unsigned APK is uploaded as a workflow
artifact.

## Status webserver
`tools/server.py` (auto-started by the **Status** workflow on port 5000) renders an
HTML index of the source tree so the Replit preview pane is non-empty. It is for
human navigation only and does not interact with the app.
