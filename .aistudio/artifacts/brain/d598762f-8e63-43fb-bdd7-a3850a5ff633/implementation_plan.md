# Implementation Plan - Remove All Demo Data

## Objective
Remove all demo data, sample data provider files, demo loading functions, and UI buttons so the application starts completely empty with no preloaded trips or demo data.

## Proposed Changes

### 1. Delete Sample Data Provider
- Delete `/app/src/main/java/com/example/domain/engine/SampleDataProvider.kt`.

### 2. Update ViewModel (`TripSplitViewModel.kt`)
- Remove `loadDemoBandarbanTour()` function which invoked `SampleDataProvider.populateBandarbanTour()`.

### 3. Update UI Screens & MainActivity
- Remove `onLoadDemoTrip` / `onLoadDemo` callbacks and buttons from:
  - `DashboardScreen.kt` (Remove the "Explore Sajek / Bandarban Demo" banner/button from empty state).
  - `SettingsScreen.kt` (Remove demo data loading options).
  - `MainActivity.kt` (Remove wiring of `loadDemoBandarbanTour`).

### 4. Verification
- Run `compile_applet` to verify compilation and ensure no broken references remain.
