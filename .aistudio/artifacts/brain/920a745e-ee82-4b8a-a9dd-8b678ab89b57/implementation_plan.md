# Zimbabwe Bird Checklist & Field Trip Log

An offline-first Android application for birdwatchers and hikers in Zimbabwe to manage multiple trip lists, rapidly search the official Zimbabwe bird checklist (400+ species), record seen sightings with one tap, and generate clean PDF trip summary reports for field export.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during the clarification interview and shape the app's design:

- **Confirmed Decision 1 (Sighting Interaction)**: Simple seen checkbox only. Tapping a bird's checkmark instantly toggles seen status on the active trip list without cumbersome dialogs or modals, optimized for fast logging on hiking trails.
- **Confirmed Decision 2 (PDF Report Format)**: Sighting summary report. The exported PDF report focuses specifically on the birds recorded on that trip, featuring trip title, date, location/notes, total species count, and a categorized or alphabetical list of observed birds.
- **Confirmed Decision 3 (Rarity Badges & Tags)**: Subtle text tags (e.g., `Rare`, `Vagrant`, `Confirm?`) styled alongside the species name to preserve checklist accuracy without cluttering the list interface.

---

## 1. Overview & Core Concept

- **What It Does**: The app provides a fast, fully offline digital field book for birding in Zimbabwe. Users can create trip lists with a custom name and date (e.g., "Mana Pools Safari - Oct 2026", "Nyanga National Park Hike"), search and filter through the complete Zimbabwe checklist of over 400 species, toggle birds seen with instant feedback, view sighting progress counters, and export/share an official-style PDF report directly from the field.
- **Target Audience / Persona**: Birders, eco-tourists, researchers, and safari guides hiking in remote national parks (Hwange, Mana Pools, Matobo, Eastern Highlands) where internet connectivity is unavailable.
- **Key Value**: 100% offline reliability with instantaneous search across 400+ native species and zero latency, backed by SQLite (Room) persistence.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Trip Lists Hub**:
   - The user opens the app to see a list of trips sorted by date, with progress pills showing the count of birds seen (e.g., "42 species observed").
   - A Floating Action Button (FAB) "+ New Trip" opens a clean bottom sheet or dialog to enter trip name, date picker, and optional trip notes (e.g., habitat or location).
   - Each trip card supports direct tap to enter the checklist, quick PDF export icon, and a menu to edit/delete.

2. **Trip Checklist & Quick Logging**:
   - Top app bar displays the trip name, date, search bar, and live species counter (e.g., "38 / 412 Seen").
   - Filter pills allow instant view switching: **All Birds**, **Seen Only**, and **Unseen**.
   - Search input supports instant fuzzy search as you type (matching species names like "Lilac-breasted Roller", "Pel's Fishing-owl", "Bateleur").
   - Species items display the bird name, subtle rarity tag (`R`, `V`, `?`), and a clear, responsive checkbox with haptic/ripple feedback.

3. **PDF Trip Report Export**:
   - Tapping "Export PDF" from the trip screen or trip menu instantly renders a professional trip report using Android's native `android.graphics.pdf.PdfDocument`.
   - The report includes a header ("Zimbabwe Bird Sighting Report"), trip metadata (trip name, date, generated timestamp), key statistics (total species observed), and a clean, legible table of all seen birds.
   - The file is saved to the app's cache/documents and launched via Android's native share sheet (`Intent.ACTION_SEND` with `FileProvider`) so the user can save, print, email, or send via messaging apps.

### Visual Identity & Theme

- **Aesthetic Direction**: Naturalist Field Journal meets modern Android Material 3. Earthy safari tones, high-contrast typography for bright outdoor sunlight visibility, and subtle accents.
- **Color Palette**:
  - Primary: Deep Acacia Green (`#2D5A27`)
  - Secondary / Accent: Safari Ochre / Sunbird Amber (`#C88A2C`)
  - Background (Light): Warm Sand / Off-White (`#FBFBF7`)
  - Surface & Card: Pure Cream (`#FFFFFF`)
  - Status Tag (Rare): Earthy Terracotta (`#A63D40`)
  - Status Tag (Vagrant): Slate Indigo (`#4A6B82`)
  - Dark Mode: Charcoal Olive (`#1B1F1A`) with Forest Mint accents.
- **Typography**: Clean, highly readable sans-serif typography with generous spacing so species names are legible at a glance while walking.

---

## 3. Key Product Decisions & Trade-Offs

- **Local Storage (Room Database)**:
  - *Chosen Approach*: Room Database with two tables (`Trip` and `TripSighting`), with the static Zimbabwe species catalog loaded from a bundled data repository.
  - *Why*: Room provides ACID transactions, full offline reliability in remote bush locations, and reactive `Flow` updates without requiring user accounts or network access.
  - *Alternatives Considered*: Cloud Firestore was ruled out because the user specifically requested remote offline hiking access without account login barriers.
- **Native Android PDF Generation (`android.graphics.pdf.PdfDocument`)**:
  - *Chosen Approach*: Android framework's built-in `PdfDocument` and `Canvas` API.
  - *Why*: Built into standard Android without heavy 3rd-party PDF SDKs, completely offline, zero bundle size overhead, full control over layout, fonts, margins, headers, and pagination.
  - *Alternatives Considered*: HTML-to-PDF via WebView is slow, unreliable offline, and prone to rendering delays. Native `PdfDocument` is instantaneous and reliable.
- **Catalog Architecture**:
  - *Chosen Approach*: The comprehensive Zimbabwe checklist of 400+ species (extracted directly from the provided checklist) is stored in a structured catalog repository. Sighting records link `tripId` and `speciesId`.
  - *Why*: Ensures list creation is instantaneous (no duplicating 400 rows per trip) while allowing fast joins or query sets for seen birds.

---

## 4. Technical Architecture & Data Strategy

### System Architecture

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │   TripListScreen      │   │  ChecklistScreen     │  │
│  │ (Trip cards, stats)   │   │(Search, Seen toggle) │  │
│  └───────────┬───────────┘   └──────────┬───────────┘  │
│              │                          │              │
│              ▼                          ▼              │
│  ┌──────────────────────────────────────────────────┐  │
│  │         BirdListViewModel & StateFlow            │  │
│  └───────────────────────┬──────────────────────────┘  │
└──────────────────────────┼─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                   Domain & Data Layer                  │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │   BirdRepository      │   │   PdfReportExporter  │  │
│  └───────────┬───────────┘   └──────────┬───────────┘  │
│              │                          │              │
│              ▼                          ▼              │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │  Room Database (DAOs) │   │ Android PdfDocument  │  │
│  │ - TripDao             │   │ & FileProvider Share │  │
│  │ - SightingDao         │   │                      │  │
│  │ - SpeciesCatalog      │   │                      │  │
│  └───────────────────────┘   └──────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Data Models & Entities

1. **`Trip`**:
   - `id: Long` (Auto-generated Primary Key)
   - `name: String` (e.g. "Mana Pools Expedition")
   - `dateMillis: Long` (Epoch millis for selected date)
   - `location: String` (Optional field notes / park)
   - `createdAt: Long`

2. **`BirdSpecies`**:
   - `id: String` (Unique normalized key, e.g. "lilac_breasted_roller")
   - `commonName: String` (e.g. "Lilac-breasted Roller")
   - `category: String` (e.g. "Rollers", "Raptors", "Waterbirds", "Passerines")
   - `rarityTag: String?` ("R" for Rare, "V" for Vagrant, "?" for Confirmation required, "Feral")

3. **`Sighting`**:
   - `tripId: Long` (Foreign Key to `Trip.id`)
   - `speciesId: String` (Foreign Key to `BirdSpecies.id`)
   - `seenAt: Long` (Timestamp when recorded)
   - *Composite Primary Key*: `(tripId, speciesId)`

### Implementation Phases

1. **Setup & Manifest**: Configure application name in `metadata.json` and `strings.xml`, add `FileProvider` in `AndroidManifest.xml` for PDF sharing.
2. **Species Data Catalog**: Construct the complete 400+ Zimbabwe species catalog directly from the provided checklist document.
3. **Room Database & Repositories**: Create `AppDatabase`, `TripDao`, `SightingDao`, and `BirdRepository`.
4. **PDF Generator Service**: Implement `PdfReportGenerator` using `PdfDocument` with clean multi-page pagination, trip details, and formatted species columns.
5. **ViewModels & State**: Implement reactive state flows for trip management, checklist searching, filtering, and toggle interactions.
6. **UI Screens**:
   - `TripListScreen`: Trip cards with count progress, new trip dialog, delete confirmation.
   - `TripChecklistScreen`: Fast searchable checklist, filter chips (All, Seen, Unseen), instant seen checkbox, stats banner.
   - `PdfPreviewDialog / Share`: Direct share launcher for exported PDF documents.
7. **Testing & Verification**: Compile and test using `compile_applet`.
