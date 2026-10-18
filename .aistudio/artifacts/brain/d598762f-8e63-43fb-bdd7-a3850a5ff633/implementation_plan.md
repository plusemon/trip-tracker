# TripSplit BD - Comprehensive Multi-Trip Expense, Income & Shared Debt Manager

An offline-first Android application built with Jetpack Compose, Material 3, and Room Database designed for effortless travel budgeting, group fund ("চাঁদা") pooling, multi-currency conversions with manual exchange rates, receipt photo attachments, multi-payer dynamic bill splitting, minimum-cash-flow debt simplification, and offline JSON backup and export.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and design choices were confirmed from your feedback and specifications:
> - **Visual Theme & Palette**: *Emerald and Teal Adventure* palette with deep forest emerald primary (`#006C4C`), vibrant teal secondary (`#006A6A`), warm ochre accents for shared funds (`#835400`), and high-contrast daylight-optimized cards.
> - **Initial Launch & Onboarding**: Starts completely blank with a clean empty state and an intuitive "Create Your First Trip" guided setup wizard.
> - **Default Currency & Exchange Rate Presets**: Bangladeshi Taka (৳ BDT) as base currency with instant presets for USD, THB, INR, MYR, and EUR, plus an offline manual exchange rate editor.
> - **Common Fund ("চাঁদা") & Income Pools**: Supports both initial member contributions and general inflows (allowances, refunds, sponsorships) with dedicated cash pool tracking alongside out-of-pocket transactions.
> - **Offline Image Storage**: Receipts and travel memory photos are copied into secure app-internal storage with URI paths persisted in Room and displayed via Coil.

---

## 1. Overview & Core Concept

When traveling in groups—whether on domestic expeditions (Bandarban, Sajek, Cox's Bazar, Sylhet) or international holidays (Thailand, India, Malaysia)—group finances become chaotic. Traditional split apps fail due to spotty remote mobile reception, inability to track group cash pools ("চাঁদা"), lack of manual exchange rate overrides, and rigid equal-split assumptions.

**TripSplit BD** delivers a 100% offline-first experience providing:
- **Unlimited Multi-Trip Management**: Organize trips by status (Active, Upcoming, Completed / Archived) with individual target budgets, dates, and base currencies.
- **Common Fund & Inflow Tracking**: Monitor the tour manager's running cash-in-hand pool alongside member contribution ledgers.
- **Custom Categories & Icon/Color Picker**: Built-in travel categories plus user-defined categories with customized Material icons and colors.
- **Offline Multi-Currency & Rate Overrides**: Enter expenses in any foreign currency (e.g., THB, USD) with custom exchange rate tables converted automatically into the trip's base currency.
- **Receipt & Memory Attachments**: Capture or pick photos directly from camera/gallery, store them locally, and view them in a pinch-to-zoom full-screen viewer.
- **Advanced Dynamic Splitting & Multi-Payer**: Split equally, selectively (opt-outs), or by custom amounts. Allow multiple members to co-pay an expense.
- **Greedy Cash-Flow Debt Simplification**: Reduce multi-party debt webs to the minimum number of direct peer-to-peer repayments with 1-tap "Mark as Settled".
- **Visual Analytics**: Interactive category breakdown donut charts and day-by-day spending bars.
- **Zero-Internet Sharing & Backup**: Formatted WhatsApp plain-text summaries, on-device PDF generation, and local JSON backup export/import.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                        User Journey & Flow                             │
└────────────────────────────────────────────────────────────────────────┘
  1. Onboarding / Blank Slate ──► "Create First Trip" Wizard
                                  (Name, Destination, Currency, Budget, Members)
  2. Main Navigation Bar ───────► Dashboard (Fund Pool & Target Budget)
                                ├─► Transactions (Expenses & Incomes with Photos)
                                ├─► Debts & Settlements (Minimized Cash Flow)
                                ├─► Analytics (Charts, Timeline & Breakdown)
                                └─► Settings (Trips, Exchange Rates, Categories, Backup)
  3. Quick Actions (FAB) ───────► "Add Expense" (Dynamic Splits, Multi-Payer, Camera)
                                └─► "Enter Income / Deposit" (Pool Contribution)
  4. Debt Settlement ───────────► Review Balances ──► Record Payment ──► Mark Settled
  5. Export & Backup ───────────► Plain-Text Share ──► PDF Document ──► JSON File
```

1. **Trip Switcher & Creation Wizard**:
   - Empty state presents a welcoming hero card prompting the user to create their first tour.
   - Header top bar features a dropdown trip switcher displaying Active, Upcoming, and Archived trips with one-tap switching and status toggling.
2. **Dashboard & Pool Health**:
   - Dual-stat hero card: Running **Common Fund Pool** (Total Inflows vs. Total Pool Outflows = Cash in Hand) and **Trip Budget Progress** (Spent vs. Target Budget with visual progress meter).
   - Quick action pills: "Add Expense", "Deposit / Income", "Exchange Rates", and "Settle Debts".
3. **Transaction Entry (Expenses & Inflow / Income)**:
   - Segmented toggle: **Expense** vs. **Income / Deposit**.
   - For Incomes: Select contributor or external source, amount, currency, and income category.
   - For Expenses:
     - Payment Source: **Common Fund** or **Member(s)** with joint multi-payer support.
     - Currency selector with automatic base currency conversion preview based on custom rate.
     - Category chips with custom category creator (+ button with Icon & Color grid).
     - Camera/Gallery photo picker with thumbnail strip, remove badge, and full-screen preview.
     - Splitting selector: Equal split, Selective split (checkboxes for who shared), or Custom exact amount.
4. **Debt Simplification & Settlement**:
   - Member balance summary cards: "Gets back ৳X" (Jade green) vs. "Owes ৳Y" (Crimson red).
   - Step-by-step transaction guidance: `[Member A] ➔ pays ৳1,500 ➔ [Member B]`.
   - "Record Settlement" dialog with instant balance recalculation and history audit.
5. **Interactive Charts & Filters**:
   - Donut chart with category percentages and center total.
   - Daily spending bar chart with touch selection.
   - Filter bar: Date range, Category, and Payer filters.
6. **Settings, Backup & Support**:
   - Manage exchange rates between currency pairs.
   - Offline JSON backup export to device storage and JSON restore parser.
   - App credits, version info, and developer support link / simulated tip jar.

### Visual Identity & Theme
- **Style**: *Emerald & Teal Adventure* with rich tactile Material 3 surfaces.
- **Color Tokens**:
  - Primary: Deep Forest Emerald (`#006C4C` light / `#2CE59E` dark)
  - Secondary: Lush Teal (`#006A6A` light / `#4DDAD8` dark)
  - Tertiary / Shared Pool: Warm Ochre (`#835400` light / `#FFBA35` dark)
  - Success / Credit: Jade Green (`#1B6D24` light / `#6CDB75` dark)
  - Error / Debt: Crimson Red (`#BA1A1A` light / `#FFB4AB` dark)
  - Surfaces: Daylight-optimized off-white (`#FBFDF8`) / Deep obsidian dark (`#191C1A`)
- **Typography & Layout**:
  - 48dp minimum touch targets for reliable outdoor tapping.
  - High-contrast monetary values formatted with Bengali / International comma separations (`৳24,500.00`).
  - Animated dialog transitions and staggered list item reveals.

---

## 3. Key Product Decisions & Trade-Offs

- **100% Offline Room Database vs Cloud Backend**:
  - *Decision*: Strictly local Room database with foreign key cascades and reactive `StateFlow` queries.
  - *Why*: Travel in remote wilderness and international roaming without SIMs requires uninterrupted functionality.
- **Local JSON Export/Import vs Cloud Backup**:
  - *Decision*: Export formatted JSON files via Android's file storage intent or internal documents, readable back into Room.
  - *Why*: Allows zero-dependency backups and cross-device sharing via Bluetooth, AirDrop/Nearby Share, or SD cards without requiring user accounts or internet.
- **Scoped Internal Storage for Photos**:
  - *Decision*: Save compressed copies of captured/picked receipts into `context.filesDir/receipts/` and store local relative paths in Room.
  - *Why*: Eliminates security/permission issues with external photo URIs and ensures photos survive gallery deletions.
- **Greedy Minimum Cash Flow Graph Simplification**:
  - *Decision*: Compute net balances for each member and greedily match largest creditor with largest debtor.
  - *Why*: Minimizes transactions from $O(N^2)$ pairwise debts to at most $N-1$ clear repayments.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        System Architecture                             │
└────────────────────────────────────────────────────────────────────────┘

                     Jetpack Compose UI (Material 3)
     ┌──────────────────┬─────────────────┬─────────────────┐
     │ DashboardScreen  │ ExpensesScreen  │ SettlementScreen│
     │ (Fund & Metrics) │ (Entry & Photos)│ (Graph Min Flow)│
     └────────┬─────────┴────────┬────────┴────────┬────────┘
              │                  │                 │
     ┌────────┴──────────────────┴─────────────────┴────────┐
     │ AnalyticsScreen │ SettingsScreen │ Dialogs & Modals  │
     └───────────────────────────┬──────────────────────────┘
                                 │
                                 ▼
                   TripSplitViewModel (MVVM StateFlow)
     ┌──────────────────────────────────────────────────────┐
     │ - Reactive UI State with Active Trip Filter          │
     │ - SettlementEngine (Net Balance & Debt Simplification)│
     │ - CurrencyConverter (Custom Exchange Rate Table)     │
     │ - BackupManager (JSON Serializer / Deserializer)     │
     └───────────────────────────┬──────────────────────────┘
                                 │
                                 ▼
                     TripSplitRepository
     ┌──────────────────────────────────────────────────────┐
     │ - Room Transactions for Multi-Payer & Dynamic Splits │
     │ - Scoped Internal File Storage for Receipts (Coil)   │
     └───────────────────────────┬──────────────────────────┘
                                 │
                                 ▼
                Room SQLite Local Database (AppDatabase)
  ┌────────────┐   ┌────────────┐   ┌────────────────┐   ┌──────────────┐
  │ TripEntity │───┤MemberEntity├───┤TransactionEntity├───┤SplitDetail   │
  └─────┬──────┘   └─────┬──────┘   └────────┬───────┘   └──────────────┘
        │                │                   │
  ┌─────┴──────┐   ┌─────┴──────┐      ┌─────┴──────────┐
  │ExchangeRate│   │CategoryItem│      │ReceiptPhotoPath│
  └────────────┘   └────────────┘      └────────────────┘
```

### Room Schema & Entity Definitions
1. **`TripEntity`**: `id` (PK), `name`, `destination`, `startDate`, `endDate`, `baseCurrency` (e.g., "BDT"), `budgetTarget`, `status` (`ACTIVE`, `UPCOMING`, `COMPLETED`), `createdAt`.
2. **`MemberEntity`**: `id` (PK), `tripId` (FK), `name`, `phoneNumber`, `colorHex`, `avatarInitials`.
3. **`CategoryEntity`**: `id` (PK), `tripId` (FK, nullable for global defaults), `name`, `iconName`, `colorHex`, `isIncomeCategory`.
4. **`ExchangeRateEntity`**: `id` (PK), `tripId` (FK), `fromCurrency`, `toCurrency`, `rate` (e.g. 1 USD = 120.0 BDT).
5. **`TransactionEntity`** (unified for both Expenses and Incomes/Deposits):
   - `id` (PK), `tripId` (FK), `type` (`EXPENSE` or `INCOME`), `title`, `amountOriginal`, `currencyCode`, `exchangeRate`, `amountBase`, `categoryId` (FK), `date`, `paymentSource` (`COMMON_FUND`, `SINGLE_MEMBER`, `MULTI_PAYER`), `notes`.
6. **`TransactionPayerEntity`**: `id` (PK), `transactionId` (FK), `memberId` (FK), `paidAmountBase`.
7. **`TransactionSplitEntity`**: `id` (PK), `transactionId` (FK), `memberId` (FK), `allocatedAmountBase`.
8. **`ReceiptAttachmentEntity`**: `id` (PK), `transactionId` (FK), `localFilePath`, `timestamp`.
9. **`SettlementRecordEntity`**: `id` (PK), `tripId` (FK), `fromMemberId` (FK), `toMemberId` (FK), `amountBase`, `date`, `notes`.

### Core Functional Implementation Steps
1. **Database & Schema Upgrade**:
   - Refactor Room Entities to support multi-trip isolation, unified transactions (inflow/outflow), multi-payer tables, custom categories, exchange rates, and photo attachments.
   - Update `AppDatabase` with type converters and migration/fallback destructive migration.
2. **Domain & Calculation Engines**:
   - `SettlementEngine`: Implements greedy minimum cash-flow debt simplification supporting both Common Fund contributions and direct peer payments.
   - `ExchangeRateEngine`: Handles auto-conversion from foreign amounts to the trip's base currency using custom defined rates.
   - `ShareSummaryFormatter` & `PdfReportGenerator`: Formats readable plain-text for WhatsApp and native printable PDFs.
   - `JsonBackupManager`: Generates and parses comprehensive JSON bundles containing trips, members, transactions, splits, and categories.
3. **UI Polish & Jetpack Compose Components**:
   - **Trip Setup & Management**: Trip switcher top bar, "Create New Trip" wizard with member builder, and trip status switcher (Active/Upcoming/Completed).
   - **Custom Category & Icon/Color Picker**: Modal with a rich grid of Material travel icons (bus, flight, hotel, restaurant, ticket, guide, mountain, coffee, shopping, etc.) and palette color circles.
   - **Transaction Modal**: Dynamic toggle (Expense vs. Income), Currency & rate input, Multi-payer slider/input, Dynamic split allocation, and Photo attachment camera/gallery launcher.
   - **Receipt Viewer**: Thumbnail preview in cards with one-tap full-screen zoomable view.
   - **Debts & Settlement**: Interactive payment checklist, individual balances ("Owes" / "Gets back"), and "Mark as Settled" handler.
   - **Analytics**: Donut chart with category breakdown, daily spending histogram, and multi-trip comparison.
   - **Settings & About**: Exchange rate manager, JSON backup import/export, and Developer support / tip-jar screen.
4. **App Launcher Icon & Branding**:
   - Generate custom adaptive launcher icon reflecting "TripSplit BD" (adventure mountain/compass with currency symbol) replacing default green icon.
5. **Verification & Testing**:
   - Compile verification with `compile_applet`.
   - Local unit tests verifying settlement engine math and conversion accuracy.
