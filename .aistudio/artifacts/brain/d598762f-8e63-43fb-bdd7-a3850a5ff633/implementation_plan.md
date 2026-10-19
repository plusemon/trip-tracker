# Firebase Realtime Database Sync with 6-Digit Join Code

Enable real-time, multi-device trip collaboration in TripSplit BD via Firebase Realtime Database. Trip leaders can broadcast expenses, deposits, and settlements under a 6-character trip code, while group members join in view-only mode with instant live data updates.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed based on your preferences:
> - **Member Data Persistence**: Synced trip data is stored in the local Room database flagged as `isReadOnly = true`, ensuring fast offline caching and seamless app restarts.
> - **Leader Cloud Initiation**: Every newly created trip automatically generates a unique 6-character trip code (e.g. `BNDR26`) and provisions its node on Firebase Realtime Database, with real-time push on every expense, deposit, or member change.
> - **Join Entry Point**: The "Join Trip with Code" action is prominently accessible both directly on the Dashboard header and inside the Trip Switcher dialog.

---

### 1. Overview & Core Concept

- **What It Does**: Bridges offline Room persistence with Firebase Realtime Database. A trip host/leader creates a tour, which gets a unique 6-character code (e.g. `BNDR26`) and syncs live to the cloud. Co-travelers enter the code on their devices to pull the full tour itinerary, expense ledger, fund deposits, and debt settlement calculations.
- **Target Audience**: Tour managers, friends, family, and group travelers traveling together in Bangladesh and beyond.
- **Key Value**: Eliminates the hassle of manually sending screenshots or exporting PDFs after every single expense. All members see live spending and common fund balances update instantly on their own phones without accidentally modifying or deleting tour records.

---

### 2. User Experience & Visual Design

#### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LEADER / HOST FLOW                              │
│                                                                        │
│   Create Trip ──> Auto-assign 6-Char Code (e.g., BNDR26)               │
│                        │                                               │
│                        ▼                                               │
│   Dashboard: Displays "Share Code" button & Live Sync indicator        │
│                        │                                               │
│                        ▼                                               │
│   Add/Edit Expense, Deposit, or Settlement ──> Instant push to RTDB    │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                        MEMBER / VIEWER FLOW                            │
│                                                                        │
│   Tap "Join Trip with Code" (from Dashboard header or Trip Switcher)   │
│                        │                                               │
│                        ▼                                               │
│   Enter 6-char code (e.g., BNDR26) ──> Fetches /trips/BNDR26           │
│                        │                                               │
│                        ▼                                               │
│   Saves locally as Read-Only & attaches ValueEventListener             │
│                        │                                               │
│                        ▼                                               │
│   UI adjusts: View-Only Badge, FAB & write controls disabled/hidden    │
└────────────────────────────────────────────────────────────────────────┘
```

#### Visual Styling & Read-Only Enforcement
- **Leader Dashboard Header**: Features a sleek chip displaying the trip code with a share icon. Tapping opens a quick-share dialog with one-tap clipboard copy and direct WhatsApp sharing.
- **Viewer Live Sync Badge**: Displays a pill badge at the top: `👀 Live Sync (View-Only) • Code: BNDR26`.
- **Restricted Member Controls**:
  - `+ Add Expense` Floating Action Button (FAB) is hidden.
  - "Deposit Fund" / "Add Income" buttons are hidden.
  - Expense list items do not display delete or edit icons.
  - Settlement action buttons ("Mark as Settled") are hidden.
- **Join Trip Dialog**: Clean Material 3 modal with uppercase 6-character formatted text input, animated validation spinner, and error handling for non-existent codes.

---

### 3. Key Product Decisions & Trade-Offs

- **Firebase Realtime Database vs. Firestore**:
  - *Chosen Approach*: Firebase Realtime Database with instance URL `https://trip-tracker-e602d-default-rtdb.asia-southeast1.firebasedatabase.app/` and disk persistence enabled.
  - *Why*: Direct requirement for low-latency multi-device sync with minimal data overhead in low-connectivity travel environments.
- **Room Cache with Read-Only Flag vs. Memory-Only State**:
  - *Chosen Approach*: Persist joined trips in Room with `isReadOnly = true`.
  - *Why*: Allows travelers to view their trip expenses even when traveling in remote areas (e.g. Sajek Valley, Bandarban hill tracts) with spotty cellular reception.
- **Device Identification**:
  - *Chosen Approach*: Generate or retrieve an installation UUID stored in SharedPreferences to distinguish the Leader device from Member devices.

---

### 4. Technical Architecture & Data Strategy

#### System & Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                APPLICATION LAYER                                │
│                                                                                 │
│   ┌──────────────────────────┐             ┌────────────────────────────────┐   │
│   │     DashboardScreen      │             │         TripSplitApp           │   │
│   │  - Read-Only Status Chip │             │  - TopAppBar Join/Share Action │   │
│   │  - Share Code Dialog     │             │  - JoinTripDialog              │   │
│   └────────────┬─────────────┘             └───────────────┬────────────────┘   │
│                │                                           │                    │
│                ▼                                           ▼                    │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                           TripSplitViewModel                            │   │
│   │   - manageTripCode()           - joinTripWithCode(code)                 │   │
│   │   - autoSyncToFirebase()       - observeRemoteTrip(tripCode)            │   │
│   └────────────┬───────────────────────────────────────────┬────────────────┘   │
│                │                                           │                    │
│                ▼                                           ▼                    │
│   ┌──────────────────────────┐             ┌────────────────────────────────┐   │
│   │   TripSplitRepository    │             │      FirebaseSyncManager       │   │
│   │   (Room Database SQLite) │             │  - FirebaseDatabase (RTDB)     │   │
│   │   - trips (isReadOnly)   │             │  - Disk Persistence: true      │   │
│   │   - members, expenses    │             │  - /trips/{tripCode}           │   │
│   └──────────────────────────┘             └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
                   Firebase Realtime Database Cloud Node:
                               /trips/{tripCode}
```

#### Firebase Cloud Schema Mapping (`/trips/{tripCode}`)
- **`metadata`**:
  - `tripId`: String
  - `tripCode`: String (e.g., `"BNDR26"`)
  - `tripName`: String
  - `leaderDeviceId`: String
  - `baseCurrency`: String (`"BDT"`)
  - `targetBudget`: Double
  - `updatedAt`: Long (timestamp)
- **`members`**: Array/Map of `{ "id": String, "name": String, "deposited": Double }`
- **`transactions`**: Map of `{ "tx_id": { "title": String, "amount": Double, "category": String, "paidBy": String, "timestamp": Long } }`
- **`settlements`**: Array/Map of `{ "payer": String, "receiver": String, "amount": Double, "isSettled": Boolean }`

#### Implementation Steps
1. **Dependencies & Setup**: Add `com.google.firebase:firebase-database-ktx` to `app/build.gradle.kts` and configure `MainApplication.kt` with disk persistence (`setPersistenceEnabled(true)`).
2. **Trip Entity Migration**: Update `TripEntity` with `tripCode`, `isReadOnly`, and `leaderDeviceId`. Increment `AppDatabase` version to 5.
3. **FirebaseSyncManager**: Create a dedicated synchronization manager to handle push updates to `/trips/{tripCode}` and listen to remote snapshots with a live `ValueEventListener`.
4. **ViewModel Integration**:
   - Auto-generate 6-char alphanumeric code on trip creation and sync initial snapshot.
   - Trigger push on expense, deposit, and settlement mutations when acting as leader.
   - Implement `joinTripWithCode` to fetch the trip, insert into Room as read-only, and register live updates.
5. **UI & Dialogs**:
   - Add Join Trip with Code modal.
   - Add Share Trip Code modal with clipboard copy & WhatsApp intent sharing.
   - Enforce read-only UI states across Dashboard, Expenses, and Settlement screens.
6. **Verification**: Run `compile_applet` and verify unit tests.
