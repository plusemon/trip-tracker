# TripSplit BD - Group Travel Expense & Shared Wallet Manager

An offline-first Android application tailored for group tours and expeditions (such as Bandarban, Sajek, or Sylhet), empowering travel groups of 6–10+ members to pool advance shared funds ("চাঁদা"), log shared or personal expenses on the go without cellular connectivity, split costs flexibly, calculate minimized settlement debts, and export itemized PDF and WhatsApp reports.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during Phase 1 clarification:
> - **Initial Launch State**: The app launches clean with a guided trip creation flow / empty state, accompanied by a quick-start action if users ever wish to test with sample Bandarban tour data.
> - **Default Expense Payer**: The Common Fund ("চাঁদা" Pool) is pre-selected as the default payment source with seamless one-tap switching to individual members.
> - **Primary Export Formats**: Dedicated WhatsApp-formatted plain-text clipboard generator and on-device native PDF document export (using Android's `PdfDocument` API).

---

## 1. Overview & Core Concept

When traveling in remote areas with poor or intermittent mobile connectivity (such as the Chittagong Hill Tracts), traditional cloud-only split apps fail. **TripSplit BD** solves this with a 100% offline-first Room database architecture, purpose-built for Bangladeshi group travel dynamics:

- **Advance Fund Collection ("চাঁদা")**: Tour managers collect advance deposits (e.g., ৳5,000 per person) into a shared wallet. The app tracks the real-time cash balance in the tour manager's pocket alongside member deposits.
- **Flexible Group Splitting**: Support for expenses paid from the Common Fund or individual pockets, with Equal Split, Selective Split (excluding members who opted out of specific meals/activities), and Custom exact amounts.
- **Greedy Minimum Cash Flow Engine**: Resolves complex web-of-debts into the absolute minimum number of peer-to-peer settlement transactions.
- **Outdoor-Ready High-Contrast UX**: Clear Bangladeshi Taka (৳) formatting, high-contrast Material 3 cards for daylight visibility, and quick-add FAB.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Trip Setup & Onboarding**:
   - Welcome/Empty state leading to a streamlined Trip Creation wizard: Trip Name, Destination (e.g., Bandarban, Nilgiri), Start & End Dates, Target Budget, and Currency symbol (`৳`).
   - Member roster creation: Add 6–10 members with phone numbers, initials, and distinct color-coded badges.
2. **Shared Fund Management ("চাঁদা" Pool)**:
   - Dedicated Shared Fund card on Dashboard showing Total Collected, Total Spent from Pool, and Current Cash in Hand.
   - Quick "Deposit Fund" dialog allowing instant recording of cash contributions per member.
3. **Expense Logging & Dynamic Splitting**:
   - Quick-add FAB opening an intuitive form: Title, Category (Transportation, Lodging, Food, Guide & Army fees, Sightseeing, Snacks), Amount (৳), and Date/Time.
   - Payment Source toggle: **Tour Common Fund** (default) or **Specific Member(s)**.
   - Split Mode selector:
     - *Equal Split* (all active trip members).
     - *Selective Split* (toggle checkboxes for who participated; e.g. 5 out of 8 members rode Chander Gari).
     - *Custom Split* (specify exact share per member with remaining balance validator).
4. **Member Ledgers & Settlement Matrix**:
   - Summary view of each member's financial health: Total Deposited, Total Paid from Pocket, Share Consumed, and Net Balance ("Gets back ৳X" in green or "Owes ৳Y" in red).
   - "Settle Up" view computing step-by-step transactions: `[Member A] pays ৳1,200 to [Member B]`. Ability to record settlement payouts.
5. **Analytics & Reports**:
   - Category breakdown chart and day-by-day expense timeline.
   - One-tap formatted WhatsApp/SMS clipboard copy.
   - Native on-device PDF generation for physical record-keeping or group sharing.

### Visual Identity & Theme
- **Theme Concept**: *Adventure Expedition & High-Contrast Daylight Clarity*.
- **Color Palette**:
  - Primary: Deep Forest Emerald (`#006C4C` / `#2CE59E`) reflecting scenic hill-tract foliage and trust.
  - Secondary: Warm Earth Ochre (`#835400` / `#FFBA35`) for Common Fund accents and deposits.
  - Tertiary: River Indigo (`#00658E` / `#7DD0FF`) for transport and logistics.
  - Error/Debt: Crimson (`#BA1A1A` / `#FFB4AB`) for negative balances.
  - Success/Credit: Lush Jade (`#1B6D24` / `#6CDB75`) for positive balances.
  - Backgrounds: Clean warm daylight surfaces (`#FBFDF8` light / `#191C1A` dark) ensuring outdoor legibility.
- **Typography & Components**:
  - Prominent ৳ currency tokens with Bengali locale number groupings (`৳12,500`).
  - Tactile 48dp touch targets, rounded M3 elevated cards, and badge chips.

---

## 3. Key Product Decisions & Trade-Offs

- **Local Room Persistence vs. Cloud**:
  - *Decision*: 100% offline Room database with strict foreign keys and cascading relations.
  - *Why*: Travelers in Bandarban/Sajek regularly face zero network connectivity. All CRUD and settlement calculations execute locally in milliseconds.
- **Minimum Cash Flow Settlement Algorithm**:
  - *Decision*: Greedy Net Balance heap/priority algorithm minimizing the transaction count from $O(N^2)$ to at most $N-1$ transactions.
  - *Why*: Simplifies confusing multi-party debts into clear, stress-free transfers.
- **Native Android `PdfDocument` vs Third-Party PDF Library**:
  - *Decision*: Built-in Android `android.graphics.pdf.PdfDocument` combined with standard Android `Canvas` rendering.
  - *Why*: Adds zero APK bloat, requires no external dependencies, and works 100% offline on any Android device.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                         │
│  ┌───────────────┐ ┌────────────────┐ ┌──────────────────────┐  │
│  │ DashboardScreen│ │ AddExpenseModal│ │ SettlementLedgerView │  │
│  │ (Fund & Stats)│ │ (Dynamic Split)│ │ (Minimized Debts)    │  │
│  └───────┬───────┘ └────────┬───────┘ └──────────┬───────────┘  │
│          │                  │                    │              │
│          └──────────────────┼────────────────────┘              │
│                             ▼                                   │
│                  TripSplitViewModel (StateFlow)                 │
│         (Settlement Engine & Currency Calculations)             │
│                             │                                   │
│                             ▼                                   │
│                    TripSplitRepository                          │
│        (Room DAO queries & Transaction orchestrator)            │
│                             │                                   │
│                             ▼                                   │
│              AppDatabase (SQLite / Room Engine)                 │
│  ┌─────────┐  ┌──────────┐  ┌───────────┐  ┌────────────────┐   │
│  │  Trip   ├──┤  Member  ├──┤  Expense  ├──┤  ExpenseSplit  │   │
│  └─────────┘  └────┬─────┘  └─────┬─────┘  └────────────────┘   │
│                    │              │                             │
│                    └───────┬──────┘                             │
│                            ▼                                    │
│                       ┌─────────┐                               │
│                       │ Deposit │                               │
│                       └─────────┘                               │
└─────────────────────────────────────────────────────────────────┘
```

### Data Entities & Schema
- `TripEntity`: `id`, `name`, `destination`, `startDate`, `endDate`, `budget`, `currencySymbol`, `createdAt`.
- `MemberEntity`: `id`, `tripId` (FK), `name`, `phoneNumber`, `colorHex`, `avatarInitials`.
- `DepositEntity`: `id`, `tripId` (FK), `memberId` (FK), `amount`, `date`, `notes`.
- `ExpenseEntity`: `id`, `tripId` (FK), `title`, `amount`, `category`, `date`, `payerType` (COMMON_FUND or MEMBER), `payerMemberId` (FK, nullable), `notes`.
- `ExpenseSplitEntity`: `id`, `expenseId` (FK, CASCADE), `memberId` (FK), `assignedAmount`.
- `SettlementRecordEntity`: `id`, `tripId` (FK), `fromMemberId` (FK), `toMemberId` (FK), `amount`, `settledDate`, `notes`.

### Core Modules & Implementation Plan
1. **Persistence Layer**: Entities, DAOs (`TripDao`, `MemberDao`, `ExpenseDao`, `DepositDao`), Room Database instance, and type converters for Date and Enums.
2. **Business Logic & Math**:
   - `SettlementEngine`: Implements greedy cash-flow minimization and computes per-member net ledgers.
   - `PdfReportGenerator`: Canvas-based document builder with styled header, member summary table, and itemized ledger.
   - `ShareSummaryFormatter`: Formats clean markdown-style text for instant WhatsApp/SMS sharing.
3. **UI Screen Architecture**:
   - `DashboardScreen`: Fund balance ("চাঁদা" pool), target budget progress, quick actions, and recent expenses.
   - `AddExpenseDialog`: Full validation, category chips, payer selector, and dynamic split allocation.
   - `DepositFundDialog`: Add advance payments to the shared pool.
   - `SettlementScreen`: Member net balances, debts to collect/pay, and mark-as-settled action.
   - `AnalyticsScreen`: Category spending charts, daily breakdown, and top spenders.
   - `ExportDialog`: Instant preview of WhatsApp text and PDF export launcher.
4. **Verification**:
   - Unit tests for `SettlementEngine` verifying 0-sum conservation and minimal transactions.
   - Successful build verification with `compile_applet`.
