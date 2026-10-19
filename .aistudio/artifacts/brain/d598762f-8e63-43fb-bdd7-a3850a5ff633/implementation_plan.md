# Implementation Plan - Fix Data Loss on App Updates

## Problem Analysis
When users install an APK update downloaded from GitHub or a browser over an existing installation, local database records (Room DB) and app settings (SharedPreferences) are sometimes wiped. This occurs due to:
1. **Signature Mismatch**: If the update APK is signed with a different key (e.g., release keystore vs debug keystore or default debug key), Android's package manager considers them different applications or forces an uninstall/reinstall that clears private storage (`/data/data/...`).
2. **Lack of Auto-Backup/Restore**: Without automatic background backups or recovery prompts, clearing local storage results in permanent data loss.

## Proposed Solutions

### 1. Consistent App Signing Configuration (`build.gradle.kts`)
- Ensure both debug and release builds use a compatible or consistent signing configuration (or fallback gracefully to prevent signature conflicts when updating via downloaded APKs).

### 2. Automatic Local JSON Backup & Auto-Restore Mechanism
- **Auto-Backup**: Periodically (or on trip changes / app close), export critical trip data and settings to a local JSON backup file in app external files / documents directory (`TripTracker_AutoBackup.json`).
- **Auto-Restore on Empty DB**: When the app starts up and detects an empty database (e.g., after a fresh install or update wipe), check for existing local backup files (`TripTracker_AutoBackup.json` or user exported backups) and automatically offer to restore data in 1 tap.

### 3. Settings & Export Improvements
- Provide clear backup status and manual Export/Import JSON options in Settings.
