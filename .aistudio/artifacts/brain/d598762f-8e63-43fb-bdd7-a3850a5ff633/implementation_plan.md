# UI/UX Refinement & Layout Implementation Plan

Enhance text legibility across all screens with high-contrast color tokens, redesign the settlement card layout into a clean multi-line format to prevent name truncation, introduce a persistent Light/Dark/System theme switcher, and simplify the settings screen.

## User Review & Critical Decisions

- **Theme Default**: "Follow system theme by default until explicitly changed" (Confirmed by user).
- **Settlement Display**: "Keep settled transactions in the main list with a Settled badge" (Confirmed by user).

---

## 1. Overview & Core Concept

This update resolves contrast and readability issues, eliminates string truncation on settlement cards, and gives users full control over the app's visual mode (Light/Dark/System Default), while removing non-essential donation UI elements.

---

## 2. User Experience & Visual Design

### High-Contrast Readability
- Upgrade `onSurfaceVariant` and `outline` color definitions in `Color.kt`:
  - **Light Mode**: `#334155` (Slate 700) for `onSurfaceVariant`, `#475569` (Slate 600) for `outline`.
  - **Dark Mode**: `#CBD5E1` (Slate 300) for `onSurfaceVariant`, `#94A3B8` (Slate 400) for `outline`.
- Replace low-opacity text colors across `AnalyticsScreen.kt`, `SettlementScreen.kt`, and `SettlementItemCard.kt` to ensure contrast ratios exceed 4.5:1.

### Redesigned Settlement Item Card (`SettlementItemCard.kt`)
- **Stacked 2-Line Architecture**:
  ```
  ┌─────────────────────────────────────────────────────────────┐
  │  [Payer Avatar]  Payer Name         ৳X,XXX        Receiver Name [Avatar]  │
  │                  (Owes)              ──►           (Receiver)             │
  ├─────────────────────────────────────────────────────────────┤
  │  [ Status: Pending / Settled ]        [ Mark as Settled ]   │
  └─────────────────────────────────────────────────────────────┘
  ```
- **Line 1 (Transfer Flow)**: Payer on left with avatar & "Owes" badge, centered amount badge with arrow, Receiver on right with avatar & "Receiver" badge. Names wrap naturally (`softWrap = true`) with zero ellipsis truncation.
- **Line 2 (Action Bar)**: Payment status badge on the left, compact `FilledTonalButton` ("Mark as Settled" / "Settled") on the right.

### Appearance & Theme Switcher
- Add an **Appearance & Theme** card in `SettingsScreen.kt` featuring a 3-segment control: **System Default**, **Light Mode**, and **Dark Mode**.
- Store the theme choice in `SharedPreferences` via a lightweight `ThemeManager`.
- Bind theme selection state in `MainActivity.kt` to trigger live recomposition of `TripSplitTheme`.

### Settings Cleanup
- Remove the "About TripSplit BD" card and "Buy the Developer a Hill Chai" tip jar completely from `SettingsScreen.kt`.

---

## 3. Key Technical Decisions

```
  ┌───────────────────────┐
  │     ThemeManager      │
  │  (SharedPreferences)  │
  └───────────┬───────────┘
              │ state
              ▼
  ┌───────────────────────┐       ┌────────────────────────┐
  │     MainActivity      ├──────►│     TripSplitTheme     │
  └───────────────────────┘       └───────────┬────────────┘
                                              │ Light/Dark Colors
                                              ▼
                                  ┌────────────────────────┐
                                  │      Compose UI        │
                                  │ (Settings, Settlement, │
                                  │  Analytics, Dashboard) │
                                  └────────────────────────┘
```

- **Theme State Management**: `ThemeManager` manages `ThemeMode` (`SYSTEM`, `LIGHT`, `DARK`) using `SharedPreferences`, notifying Compose runtime via `StateFlow` or Compose `State`.
- **Contrast Assurance**: Re-map color tokens so secondary metrics, subtitle lines, and labels maintain high contrast against card and surface backgrounds.

---

## 4. Implementation Steps

1. **Theme Preference Manager (`ThemeManager.kt`)**:
   - Create `ThemeManager` in `com.example.util` for `ThemeMode` storage and retrieval.

2. **Color Palette Update (`Color.kt`)**:
   - Update `LightOnSurfaceVariant`, `LightOutline`, `DarkOnSurfaceVariant`, and `DarkOutline` to high-contrast WCAG 4.5:1 values.

3. **Settlement Card Redesign (`SettlementItemCard.kt`)**:
   - Refactor into Line 1 (Flow & Amount) and Line 2 (Status & Action Button). Allow text wrapping and remove name truncation.

4. **Settings Screen Adjustments (`SettingsScreen.kt`)**:
   - Add **Appearance & Theme** segmented control card.
   - Remove the "About TripSplit BD" and Tip Jar card.

5. **App Theme Integration (`MainActivity.kt` & `Theme.kt`)**:
   - Wire `ThemeManager` to `TripSplitTheme` in `MainActivity.kt` so theme switches update dynamically across all screens.

6. **Contrast Audit & Cleanup (`AnalyticsScreen.kt` & `SettlementScreen.kt`)**:
   - Update text styles and remove low-alpha transparency on text elements.
