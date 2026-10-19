# TripSplit BD - In-App Update Engine & GitHub CI/CD Implementation Plan

Integrate an automated GitHub Releases in-app update engine for TripSplit BD, complete with automatic launch checks, semantic version comparison, DownloadManager APK installer with FileProvider package installation, modern Jetpack Compose update dialogs, Settings screen update controls, and a complete GitHub Actions CI/CD release pipeline.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your confirmed choices in Phase 1:
> 1. **Repository Target**: Hardcoded GitHub repository configured at build time (with buildConfig constants and customizable defaults: `BdEmon00/TripSplitBD`).
> 2. **Update Dialog Presentation**: Check on launch and display an interactive update dialog immediately on app launch when a new version is detected.
> 3. **CI/CD Workflow Build**: Build ready-to-install **Debug APKs** (`./gradlew assembleDebug`) in the GitHub Actions workflow so releases are immediately downloadable and installable without requiring signing keystore secrets.

- **Confirmed Decision 1**: Semantic version comparison parses standard semantic release tags (`vX.Y.Z` or `X.Y.Z`) against `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`.
- **Confirmed Decision 2**: Android `DownloadManager` handles the download in background with progress tracking, triggering `FileProvider` package installation intent via `ACTION_VIEW` and `REQUEST_INSTALL_PACKAGES` permission handling.
- **Confirmed Decision 3**: The GitHub Actions workflow triggers on push to `main` and `master`, automatically generating tags and publishing releases with `softprops/action-gh-release@v2`.

---

## 1. Overview & Core Concept

**What It Does**:
The In-App Update Engine gives TripSplit BD users seamless over-the-air updates directly from GitHub Releases without requiring Google Play Store distribution. The application queries the GitHub REST API (`/repos/{owner}/{repo}/releases/latest`), parses release assets for `.apk` binaries and markdown release notes, checks if the remote version exceeds the currently installed build, and prompts the user with an intuitive Material 3 dialog. Upon user confirmation, it initiates background download via Android's `DownloadManager`, tracks progress, and launches the Android Package Installer.

**Target Audience / Persona**:
Expedition members, tour managers, and offline group travelers who need quick access to bug fixes, new features, and performance enhancements directly from project GitHub releases.

**Key Value**:
Frictionless updates, verified security via FileProvider content URIs, clear visibility into release notes, and fully automated continuous delivery from git commit to installed phone update.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Automatic Startup Check**:
   - On `MainActivity` launch, an asynchronous background check queries GitHub's latest release.
   - If a newer version exists (e.g., remote `v1.1.0` > local `v1.0.0`), an Material 3 `AppUpdateDialog` pops up.
   - The user sees:
     - Title: "New Update Available: v1.1.0"
     - Version badge showing `v1.0 (Installed) → v1.1.0 (Latest)`
     - Styled markdown/plaintext scrollable release notes card
     - Primary button: "Update Now" (initiates download)
     - Secondary button: "Later" (dismisses dialog for the session)

2. **Manual Check in Settings**:
   - Settings Screen features an "App Version & Updates" card.
   - Shows current app version, build code, and last checked status.
   - "Check for Updates" button with animated circular progress indicator.
   - Instant feedback: displays "You're on the latest version! 🎉" if up-to-date, or opens the update dialog if an update is found.

3. **Download & Installation Flow**:
   - Tapping "Update Now" changes the dialog state to downloading mode with a progress bar and percentage text.
   - Android's native `DownloadManager` runs with system notification visibility.
   - When download completes, the `BroadcastReceiver` catches the event, fetches the downloaded APK URI via `FileProvider`, and launches `Intent.ACTION_VIEW` (`application/vnd.android.package-archive`).
   - If Android 8.0+ unknown sources permission (`canRequestPackageInstalls()`) is needed, the user is guided to grant it smoothly.

### Visual Styling & Theme

- **Dialog Surface**: Elevated Material 3 card with rounded corners (24.dp), subtle emerald gradient header, and high-contrast typographic hierarchy.
- **Release Notes Box**: Surface variant container with monospace/body styling and vertical scrolling.
- **Progress Indicator**: Dual indicator with LinearProgressIndicator and percentage text ("Downloading... 4.8 MB / 12.4 MB").

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native DownloadManager vs OkHttp In-Memory Stream**
  - *Chosen Approach*: Use Android's native `DownloadManager`.
  - *Why*: Immune to activity destruction, handles network interruptions, provides system download notifications, and writes directly to external cache/downloads safely.
  - *Alternatives Considered*: Direct OkHttp streaming (more vulnerable to process termination in background).

- **Decision 2: FileProvider Security Architecture**
  - *Chosen Approach*: Configure `androidx.core.content.FileProvider` with `res/xml/provider_paths.xml` granting read permissions to the system installer.
  - *Why*: Mandatory on Android 7.0+ (API 24+) to avoid `FileUriExposedException`.

- **Decision 3: Robust Semantic Version Parsing**
  - *Chosen Approach*: Multi-segment numerical parser splitting major, minor, patch, and build numbers with regex normalization.
  - *Why*: Handles variations like `v1.0.1`, `1.0.1`, `v1.1`, or `v1.0.0-rc1` reliably without crashes.

---

## 4. Technical Architecture & Component Structure

```
┌────────────────────────────────────────────────────────────────────────┐
│                        GitHub REST API                                 │
│          GET /repos/{owner}/{repo}/releases/latest                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ JSON Response
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      GitHubReleaseService / Client                     │
│                  (Retrofit + Moshi Data Parsing)                       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          VersionComparator                             │
│       Compares remote tag_name vs BuildConfig.VERSION_NAME/CODE        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
┌───────────────────────────────────┐   ┌────────────────────────────────┐
│      UpdateCheckerViewModel       │   │     TripSplitViewModel         │
│  - isChecking / isDownloading     │   │  (Delegates check in Settings) │
│  - downloadProgress               │   └────────────────────────────────┘
│  - updateInfo                     │
└─────────────────┬─────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        AppUpdateDialog (M3)                            │
│           - Changelog scroll view                                      │
│           - Update Now & Later buttons                                 │
│           - Download progress bar                                      │
└─────────────────┬──────────────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 ApkDownloaderAndInstaller                              │
│  - Android DownloadManager request                                     │
│  - BroadcastReceiver (ACTION_DOWNLOAD_COMPLETE)                        │
│  - FileProvider.getUriForFile()                                        │
│  - Intent.ACTION_VIEW (package-archive)                                │
└────────────────────────────────────────────────────────────────────────┘
```

### File-Level Implementation Breakdown

1. **Gradle Build Config (`app/build.gradle.kts`)**:
   - Add `buildConfigField("String", "GITHUB_REPO_OWNER", "\"BdEmon00\"")`
   - Add `buildConfigField("String", "GITHUB_REPO_NAME", "\"TripSplitBD\"")`
   - Ensure permissions and build feature compatibility.

2. **Android Manifest & Permissions (`app/src/main/AndroidManifest.xml`)**:
   - Declare permissions:
     - `android.permission.INTERNET`
     - `android.permission.REQUEST_INSTALL_PACKAGES`
   - Add `<external-path name="download_files" path="." />` and `<cache-path name="apk_updates" path="updates/" />` to `res/xml/provider_paths.xml` (or update `res/xml/file_paths.xml`).
   - Register the download broadcast receiver.

3. **Data Models & Networking (`com.example.updater.model` & `com.example.updater.api`)**:
   - `GitHubRelease.kt`: Data model for release metadata, assets, release body, tag name.
   - `GitHubReleaseService.kt`: Retrofit endpoint interface.
   - `GitHubApiClient.kt`: Retrofit client singleton.

4. **Version Logic (`com.example.updater.util.VersionComparator`)**:
   - Semantic version parsing and unit test suite.
   - Handles `v1.2.3` vs `1.2.0`, build code fallback.

5. **Download & Install Engine (`com.example.updater.installer.ApkInstaller`)**:
   - `DownloadManager` enqueue, status query, and listener.
   - Safe package installation intent creation with `FLAG_GRANT_READ_URI_PERMISSION`.
   - Handling of unknown app sources permission flow for Android 8+.

6. **ViewModel & State (`com.example.updater.ui.UpdateViewModel`)**:
   - StateFlow for `UpdateUiState`.
   - Methods: `checkForUpdates(manual: Boolean)`, `startDownload()`, `dismiss()`.

7. **Compose UI Components (`com.example.updater.ui.AppUpdateDialog.kt`)**:
   - Dialog with release changelog and progress display.
   - Integrated into `MainActivity.kt` and `SettingsScreen.kt`.

8. **CI/CD Workflow (`.github/workflows/release.yml`)**:
   - Complete GitHub Actions YAML:
     - Push to `main`/`master`.
     - Java 17 setup, Gradle cache, chmod `./gradlew`.
     - `./gradlew assembleDebug`.
     - Automatic version tagging from `build.gradle.kts` / GitHub run number.
     - Release generation using `softprops/action-gh-release@v2`.
     - Attached artifact: `app/build/outputs/apk/debug/app-debug.apk`.

9. **Verification**:
   - Unit tests for `VersionComparator`.
   - Compilation check with `compile_applet`.
