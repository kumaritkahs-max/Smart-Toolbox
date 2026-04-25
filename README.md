# GitHub Control — Native Android Client

A complete native Android GitHub client written in **Kotlin + Jetpack Compose +
Material 3**. The full source lives in this repo; the actual APK is produced by
the GitHub Actions workflow because Replit cannot build Android binaries.

## Building the APK

### From the GitHub Actions workflow (no setup required)

Push to the repo. The `.github/workflows/android.yml` workflow runs on a fresh
Ubuntu runner with JDK 17 + the Android SDK (compileSdk 35), executes
`./gradlew assembleDebug`, and uploads the APK as a workflow artifact you can
download from the run summary.

### From Android Studio

1. Install **Android Studio Hedgehog (2023.1)** or newer.
2. `File → Open` and select the project root.
3. Accept JDK 17 and Android SDK 35 when prompted.
4. Wait for **Gradle Sync** (first sync downloads ~400 MB of dependencies).
5. Pick a device or emulator (API 26+) and click ▶ **Run 'app'**.

### From the command line

```
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # signed release (configure signing first)
```

## Stack

- **Language** Kotlin 2.0.10 (K2 compiler)
- **UI** Jetpack Compose, Material 3 (`androidx.compose.bom:2024.10.00`)
- **DI** Hilt 2.52 + KSP
- **Persistence** Room (encrypted accounts via EncryptedSharedPreferences) + DataStore
- **HTTP** Retrofit 2 + OkHttp + kotlinx.serialization
- **Git engine** JGit 6.10 (clone/fetch/pull/push/commit/branches/stash/reset)
- **Background work** WorkManager + Hilt-Worker
- **Auth** GitHub Personal Access Tokens (PAT) + AndroidX BiometricPrompt unlock
- **Min SDK** 26 (Android 8.0). **Target / compile SDK** 35 (Android 15).

## Feature map

### Accounts & security
- Multi-account PAT sign-in with biometric / device-credential unlock and auto-lock
- Encrypted token vault (Tink-backed `EncryptedSharedPreferences`)
- **Token inspector** — token type detection (classic / fine-grained), per-scope
  risk colouring, missing-recommended-scopes hint, rate-limit + expiry badges,
  validation history (last 20) with one-tap re-validate
- Profile editor (name, email, blog, bio, company, location, Twitter, hireable)
- SSH key list / add / delete

### Repositories
- Browser with sort + filter, pagination, my repos / starred
- Create / fork / star / watch / transfer / archive / delete
- Repo detail with README, branches, languages, contributors, releases
- **Administration card**: Collaborators, Branch protection editor, Compare
- Branch CRUD (create / rename / delete) + protection toggles (reviews, status
  checks, code-owners, lock-branch, force-push & deletion allowances, etc.)

### Files & uploads
- File explorer + multi-select tree (bulk delete, ZIP folder download via SAF)
- **Universal file viewer** — text & every common source extension, Markdown,
  images (PNG/JPG/GIF/WebP/BMP/ICO/HEIC), SVG, HTML (WebView), PDF (native
  PdfRenderer, multi-page), audio & video (MediaPlayer/VideoView), archives (zip
  entry list), unknown binaries (hex dump). Every preview has copy /
  download-to-device / open-with-system-app actions.
- Folder / multi-file / ZIP upload preserving full hierarchy via the **Git Data
  API** (atomic blob → tree → commit → ref-update); conflict modes
  overwrite / skip / rename, pause / resume / cancel, foreground service notifications

### Code & collaboration
- Commits list with pagination, commit detail with split / unified diff and a
  whitespace toggle
- Branch & commit compare screen with file-status + ±line counts
- Pull Requests: list / open / merge / squash / rebase / close / reopen / create
- Issues: list / open / comment / close / reopen / labels / assignees / create
- GitHub Actions: workflow list, recent runs with status badges, manual dispatch
- Search: repos / code / users (cross-repo)

### Operations
- Sync jobs (folder → repo) with WorkManager
- Notifications inbox (mark read / unread)
- Plugin registry, Downloads inbox
- **Command Mode** terminal for power users
- **Dangerous mode** toggle gating destructive ops

### Reliability & observability
- **Embedded terminal log panel** on every key screen (Login, Tree, Upload, Sync,
  Command, Profile, SshKeys, BranchProtection, Compare, Collaborators,
  Permissions) with copy-to-clipboard
- Full **Terminal log** screen — live tail, level / text filter, snapshot pause,
  copy / share / clear
- **Persistent crash reports** — every uncaught exception (any thread) is saved
  permanently with full stack trace, cause chain, device info, and the last 200
  log entries; viewer has copy / share / delete-all actions
- **Permissions hub** — every permission the app uses, current status, and a
  one-tap Grant button per row plus an "Ask all" batch request. Special
  permissions (battery optimisation, all-files access) deep-link to system Settings.

### Customization (Settings → Appearance)
- Mode: **system / light / dark / AMOLED dark**
- **Material You** dynamic colors on Android 12+
- **Accent palette** of 11 swatches (blue, purple, pink, red, orange, yellow,
  green, teal, cyan, indigo, slate)
- **Density**: compact / comfortable / cozy
- **Corner radius** slider (0–28 dp)
- **Text size** slider (0.7×–1.6×) + separate **mono / code font scale**
- **Terminal palette**: GitHub dark/light, Dracula, Solarized dark/light,
  Monokai, Nord, Matrix
- One-tap **Reset to defaults**

### Home-screen widget
- Material 3 launcher widget with Open and Refresh actions
- Subtitle reflects the latest published status (login, last sync, etc.)
- Resizable (min 3×2 cells) and works on the system home screen

## Required permissions

Declared in `AndroidManifest.xml` and surfaced in **Settings → Permissions**:

| Group        | Permissions |
|--------------|-------------|
| Networking   | `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` |
| Auth         | `USE_BIOMETRIC`, `USE_FINGERPRINT` |
| Notifications| `POST_NOTIFICATIONS`, `VIBRATE` |
| Background   | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` |
| Storage      | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (≤32), `WRITE_EXTERNAL_STORAGE` (≤29), `MANAGE_EXTERNAL_STORAGE` (opt-in) |

## First run

1. Generate a PAT at <https://github.com/settings/tokens>.
   Recommended classic scopes: **`repo`, `workflow`, `read:user`, `notifications`,
   `delete_repo`** (only if you want delete).
2. Paste the token on the first screen and tap **Sign in**.
3. Enable biometric unlock from **Settings → Security** if you want it.
4. Visit **Settings → Permissions** to grant any system permissions you want
   (the app only requests them on demand otherwise).
5. Customise look-and-feel from **Settings → Appearance**.

## Project layout

```
android-app/
├── build.gradle.kts            # Root Gradle (Kotlin DSL)
├── settings.gradle.kts
├── gradle.properties
├── .github/workflows/          # android.yml — APK build on every push
├── app/
│   ├── build.gradle.kts        # App module
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                # values / drawable / layout / mipmap / xml
│       └── java/com/githubcontrol/
│           ├── App.kt          # Hilt @HiltAndroidApp, channels, CrashHandler.install
│           ├── MainActivity.kt # setContent { AppRoot() }
│           ├── ai/             # offline commit-message heuristic
│           ├── data/
│           │   ├── api/        # Retrofit + Models + RetrofitClient
│           │   ├── auth/       # AccountManager + ScopeCatalog + TokenValidator
│           │   ├── db/         # Room entities + DAOs
│           │   ├── git/        # JGitService
│           │   ├── repository/ # GitHubRepository facade
│           │   └── AppModule.kt
│           ├── notifications/  # Notifier + channel ids
│           ├── plugins/        # PluginRegistry
│           ├── ui/
│           │   ├── AppRoot.kt          # NavHost + theme + SessionGate routing
│           │   ├── components/         # GhCard / GhBadge / EmbeddedTerminal / …
│           │   ├── navigation/Routes.kt
│           │   ├── theme/              # ThemeSettings + AccentPalette + TerminalPalette
│           │   └── screens/            # auth, repos, files, upload, commits,
│           │                           # pulls, issues, actions, search, sync,
│           │                           # notifications, plugins, downloads,
│           │                           # command, settings (Appearance,
│           │                           # Permissions, CrashLog), logs,
│           │                           # profile, keys, branches, compare, collab
│           ├── upload/         # UploadManager + ConflictMode
│           ├── utils/          # Logger / CrashHandler / PermissionsCatalog /
│           │                   # ShareUtils / Diff / GitignoreMatcher / RelativeTime
│           ├── viewmodel/      # Hilt ViewModels
│           ├── widget/         # Home-screen widget provider
│           └── worker/         # SyncWorker + UploadWorker
```
