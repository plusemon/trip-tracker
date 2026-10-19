# Replace JSON Backup/Restore with CSV Export & Restore

## Overview
Replace the current JSON backup/restore mechanism (which used clipboard copy/paste of JSON strings) with a robust CSV-based export and restore system. Export all trips, members, deposits, expenses, categories, and splits into a structured CSV file, save it to device storage, and share it via the system share sheet. Allow importing data via a `.csv` file picker that completely replaces existing local database data with the imported CSV data.

## User Review & Critical Decisions
> [!IMPORTANT]
> Confirmed preferences from user interview:
> - **Included Data**: All trips and associated expenses, deposits, and members.
> - **Restore Behavior**: Replace all existing local database data with the imported CSV data.
> - **Export Delivery**: Save file and share via system share sheet / file picker.

## 1. Core Architecture & Components

- **CsvBackupManager**: 
  - Generates a comprehensive CSV text format containing sections/tables for Trips, Members, Deposits, Expenses, Categories, Splits, and Payers.
  - Parses imported CSV text into `TripBackupBundle` or multiple bundles/entities to populate the Room database.
- **File Operations & Sharing**:
  - Export: Write CSV to a temporary cache file (`TripTracker_Export_<timestamp>.csv`) and invoke system share sheet (`ACTION_SEND` with `text/csv` URI via `FileProvider`) and/or save to downloads.
  - Import: Use `ActivityResultContracts.GetContent()` (or `OpenDocument`) filtering for `.csv` or text files to let the user select a CSV file from device storage, read its text, parse it, and replace local database records.
- **SettingsScreen & ViewModel Updates**:
  - Update `SettingsScreen` UI from "Offline JSON Backup & Transfer" (with text copy/paste dialog) to "CSV Backup & Restore" (with "Export All Trips (CSV)" and "Restore from CSV File" buttons).
  - Update `TripSplitViewModel` to handle CSV export and CSV file import.

## 2. CSV Format Design
To support multiple entities (Trips, Members, Deposits, Expenses, Categories, Splits, Payers) in a single CSV file, we will use tagged section headers or a structured multi-table CSV format:
- `# SECTION: TRIPS` followed by CSV header and rows.
- `# SECTION: MEMBERS` followed by CSV header and rows.
- `# SECTION: DEPOSITS` followed by CSV header and rows.
- `# SECTION: EXPENSES` followed by CSV header and rows.
- `# SECTION: CATEGORIES` followed by CSV header and rows.
- `# SECTION: SPLITS` followed by CSV header and rows.
- `# SECTION: PAYERS` followed by CSV header and rows.

When parsing, `CsvBackupManager` will read line by line, detect section headers, parse CSV rows (handling quoted fields properly), and reconstruct all entities.

## 3. Implementation Steps
1. Create `CsvBackupManager.kt` replacing or superseding `JsonBackupManager.kt`.
2. Update `TripSplitViewModel.kt` methods (`exportCsvBackup`, `importCsvBackup`, and auto-backup).
3. Update `SettingsScreen.kt` UI for CSV export (sharing file) and CSV import (file picker intent).
4. Update `MainActivity.kt` or any callers if needed.
5. Compile and test (`compile_applet`).
