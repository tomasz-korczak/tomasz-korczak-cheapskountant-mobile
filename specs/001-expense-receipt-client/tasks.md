---
description: "Task list for the expense and receipt Android client"
---

# Tasks: Expense and Receipt Client

**Input**: Design documents from `/specs/001-expense-receipt-client/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/client-api.md, quickstart.md, constitution.md

**Tests**: Included. `plan.md` and `quickstart.md` require JUnit 4, kotlinx-coroutines-test, MockWebServer, Compose UI tests, and Room in-memory coverage. Write each story's tests before that story's implementation so they fail until the listed types exist. Logic tests go in `app/src/test`. Compose journeys and Room in-memory tests go in `app/src/androidTest`.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested as an increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US5)
- Setup, Foundational, and Polish tasks have no story label

## Path Conventions

- Single Android module: `app/src/main/java/pl/cheapskountant/mobile/`
- Unit tests: `app/src/test/java/pl/cheapskountant/mobile/`
- Instrumented tests: `app/src/androidTest/java/pl/cheapskountant/mobile/`
- Polish strings: `app/src/main/res/values/strings.xml`
- English strings: `app/src/main/res/values-en/strings.xml`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the single-module Android project from `plan.md` and research decision 1

- [ ] T001 Create the Gradle 9.6.0 wrapper, `settings.gradle.kts` (root name `cheapskountant-mobile`, include `:app`), root `build.gradle.kts` (declare Android Gradle Plugin 9.4.1, Kotlin serialization, and KSP aligned to the Kotlin version that AGP 9.4.1 selects; do not apply the application plugin at the root), `gradle.properties` (AndroidX, official Kotlin code style, JDK 17), and `gradle/libs.versions.toml` pinning Compose BOM 2026.09.00, Material 3 1.4.0, CameraX 1.6.2 (`camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-compose`), Retrofit 3.0.0, OkHttp 5.5.0, Vico `com.patrykandpatrick.vico:compose-m3` 3.3.1, Room 2.8.5, WorkManager `work-runtime-ktx` 2.12.0, kotlinx.serialization JSON 1.10.0, kotlinx-coroutines-android, the current stable Navigation Compose and lifecycle-viewmodel-compose releases compatible with that BOM, JUnit 4, kotlinx-coroutines-test, and MockWebServer 5.5.0
- [ ] T002 Create `app/build.gradle.kts` for namespace and application id `pl.cheapskountant.mobile`, `compileSdk` 37, `minSdk` 26, `targetSdk` 37, Compose and BuildConfig enabled, serialization and Room KSP applied, and the catalog dependencies for main, `test`, and `androidTest` (Compose UI test junit4 and manifest, Room testing). Read `api.baseUrl` and `api.key` from the repository-root `local.properties` into `BuildConfig.API_BASE_URL` and `BuildConfig.API_KEY`
- [ ] T003 [P] Add `local.properties.example` with `sdk.dir`, `api.baseUrl=http://192.168.1.20:8080/`, and `api.key=example-key`, and extend `.gitignore` with `local.properties`, `.gradle/`, `**/build/`, `*.iml`, and `.cxx/` while leaving `local.properties.example` tracked
- [ ] T004 [P] Create `app/src/main/AndroidManifest.xml` with `android:name="pl.cheapskountant.mobile.CheapskountantApp"`, a portrait-locked launcher activity `pl.cheapskountant.mobile.MainActivity`, `INTERNET`, `CAMERA` with `android:required="false"`, `android:usesCleartextTraffic="true"`, and no sign-in activity
- [ ] T005 [P] Create `app/src/main/java/pl/cheapskountant/mobile/MainActivity.kt` and `app/src/main/java/pl/cheapskountant/mobile/CheapskountantApp.kt` so the activity sets Compose content
- [ ] T006 [P] Create `app/src/main/java/pl/cheapskountant/mobile/ui/theme/Color.kt`, `Type.kt`, and `Theme.kt` using the Papier light palette (`background #F3EEE4`, `surface #FFFCF7`, `ink #1C1917`, `muted #57534E`, `primary #115E59`, `onPrimary #FFFFFF`, `primaryContainer #D7EDEB`, `track #E8E0D2`) and keep that light scheme when the phone is in dark mode
- [ ] T007 Set `android:icon` in `app/src/main/AndroidManifest.xml` to an adaptive mipmap icon using `#115E59`. When `.external-resources/app_icon.png` is present, copy it into `app/src/main/res/` and use that PNG instead
- [ ] T008 [P] Create `app/src/main/res/values/strings.xml` (Polish, the default) and `app/src/main/res/values-en/strings.xml` (English) with every row of the User-Facing Text table in `specs/001-expense-receipt-client/spec.md`. Keep household category names out of string resources

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared database, HTTP client, formatting, navigation, and outbox enqueue that every story uses

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T009 [P] Create Room entities and DAOs under `app/src/main/java/pl/cheapskountant/mobile/data/local/` for `HouseholdCategory`, `MonthSnapshot`, `MonthCategory`, `DaySnapshot`, `DayEntry`, `ReceiptOutbox`, `ReceiptPhoto`, `ExpenseOutbox`, and `ExpenseOutboxLine` matching the fields, keys, and decimal-string money rules in `specs/001-expense-receipt-client/data-model.md`
- [ ] T010 Create `app/src/main/java/pl/cheapskountant/mobile/data/local/AppDatabase.kt` (Room 2.8.5, version 1, `exportSchema = false`) exposing those DAOs, and open a file-backed database from `app/src/main/java/pl/cheapskountant/mobile/CheapskountantApp.kt` (depends on T009)
- [ ] T011 [P] Create `app/src/main/java/pl/cheapskountant/mobile/data/remote/CheapskountantApi.kt` and request/response models for `GET /api/expense`, `GET /api/expense/daily`, `GET /api/expenses`, and `POST /api/expense`. Keep `POST /api/transcription` and `POST /api/receipt` receipt bodies as `kotlinx.serialization.json.JsonElement`. Leave `GET /actuator/health` and `Idempotency-Key` unimplemented
- [ ] T012 Create `app/src/main/java/pl/cheapskountant/mobile/data/remote/BearerInterceptor.kt` and `ApiClient.kt` so every call sends `Authorization: Bearer <BuildConfig.API_KEY>` (prefix `Bearer` plus one space), Retrofit uses `BuildConfig.API_BASE_URL`, and OkHttp timeouts are connect 15s, write 60s, read 90s, and call 120s (depends on T011)
- [ ] T013 [P] Create `app/src/main/java/pl/cheapskountant/mobile/domain/SendOutcome.kt` with three failure classes: no network before the attempt (stay `WAITING`), any HTTP status error (become `ERROR`), and a finished attempt with no HTTP status (become `ERROR`)
- [ ] T014 [P] Create `app/src/main/java/pl/cheapskountant/mobile/data/sync/NetworkStatus.kt` reporting whether a send can start, using the connectivity APIs available on API 26
- [ ] T015 [P] Create `app/src/main/java/pl/cheapskountant/mobile/data/remote/ApiFailure.kt` to parse `{ "reason", "explanation" }` when present, retain `reason` for the outbox, and log `explanation`. User-facing copy stays the spec send-failed sentence
- [ ] T016 [P] Create `app/src/main/java/pl/cheapskountant/mobile/domain/MoneyFormat.kt` writing the symbol after the amount, separated by a space, with two fraction digits and the active locale's decimal mark and thousands separator. Map PLN to `zł`, EUR to `€`, and USD to `$`. Day-list amounts always use `zł`
- [ ] T017 [P] Create `app/src/main/java/pl/cheapskountant/mobile/domain/CalendarDates.kt` for the selected month's inclusive `from` and `to` (`YYYY-MM-DD`), today, and the calendar-date portion of `receipt.issuedAt` with no zone shift
- [ ] T018 [P] Create `app/src/main/java/pl/cheapskountant/mobile/data/AppContainer.kt` constructed from `CheapskountantApp`, holding the database, API, and network status, and readable from composables so `androidTest` can replace it
- [ ] T019 [P] Create `app/src/main/java/pl/cheapskountant/mobile/ui/navigation/CheapskountantNavHost.kt` with routes for landing, photo selection, receipt review (direct versus queued, plus `localId`), and expense registration. Wire it from `app/src/main/java/pl/cheapskountant/mobile/MainActivity.kt`. Add no route for Configuration or About
- [ ] T020 Create `app/src/main/java/pl/cheapskountant/mobile/data/sync/OutboxSync.kt` and `OutboxSyncWorker.kt`: unique work named `outbox-sync`, constrained to a connected network, `ExistingWorkPolicy.KEEP`, enqueued by `OutboxSync.enqueue`. The worker compiles and returns success until Phase 6 fills the loop (depends on T010, T013, T014)

**Checkpoint**: Foundation ready. User story implementation can begin

---

## Phase 3: User Story 1 - Review a month and a day (Priority: P1) 🎯 MVP

**Goal**: Opening the app shows the current month's one-currency category summary and today's accepted records. Changing the month and changing the day do not affect each other. There is no sign-in

**Independent Test**: With spending already stored for a known month and day, open the app, read the month total and category shares, then choose a different month and a different day and confirm each part changes on its own

### Tests for User Story 1

- [ ] T021 [P] [US1] Write failing month-math tests in `app/src/test/java/pl/cheapskountant/mobile/domain/MonthMathTest.kt` for scale-2 category sums, an empty payload stored as total `0.00` and currency `PLN`, a positive bar ratio with half-up percent (including a positive row whose percent rounds to 0), a negative amount with a signed percent and an empty bar, a zero amount omitted, and a zero total with a non-zero category omitting the percent and skipping division
- [ ] T022 [P] [US1] Write failing money-format tests in `app/src/test/java/pl/cheapskountant/mobile/domain/MoneyFormatTest.kt` for `12,50 zł` under a Polish locale, `1 234,50 €` and `$` suffix forms, two fraction digits, and day-list amounts always using `zł`
- [ ] T023 [P] [US1] Write a failing Compose journey in `app/src/androidTest/java/pl/cheapskountant/mobile/ui/LandingJourneyTest.kt` covering no sign-in, vertical order (month summary, Receipt photo, Register expense, day list), month changes leaving the day in place, day changes leaving the month in place, an empty month showing `0 zł` and `PLN`, the empty-day sentence, and Configuration and About leaving the landing figures unchanged

### Implementation for User Story 1

- [ ] T024 [US1] Implement `app/src/main/java/pl/cheapskountant/mobile/domain/MonthMath.kt` so `MonthMathTest` passes. Use the single currency code from the month payload. Store an empty array as total `0.00`, currency `PLN`, and no category rows (depends on T021)
- [ ] T025 [US1] Implement `app/src/main/java/pl/cheapskountant/mobile/data/repository/LandingRepository.kt`. On open and when that period changes, replace `HouseholdCategory` from `GET /api/expenses`, the selected month from `GET /api/expense?from&to` (first and last calendar day, inclusive), and the selected day from `GET /api/expense/daily?payment_date`. Emit the Room snapshot first, then replace it after a successful fetch. A failed GET, including HTTP 401, keeps the previous snapshot and sets `showCachedCopy`. A period with no snapshot stays absent. Leave outbox rows out of both lists. Register the repository in `app/src/main/java/pl/cheapskountant/mobile/data/AppContainer.kt` (depends on T024, T010, T012)
- [ ] T026 [US1] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/landing/LandingViewModel.kt`. Keep the selected month and selected day in memory only, defaulting to the current month and today, changing independently. A cold start returns to the current month and today while older snapshots remain stored (depends on T025)
- [ ] T027 [P] [US1] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/landing/MonthSummarySection.kt`: month and year, the Expenses label, the formatted total, the currency code, and one Vico 3.3.1 `compose-m3` cartesian chart per non-zero category (axes and legend hidden, height 8 dp, one column at the unrounded share, colors cycling `#1D4ED8`, `#B45309`, `#0F766E`, `#6D28D9`, `#78716C`). Positive rows show the name, a filled bar, and a whole-number percent. Negative rows show the signed amount, a signed whole-number percent, and an empty bar. A zero total with a non-zero category omits the percent. An empty month shows `0 zł`, `PLN`, no rows, and the empty-month sentence. If a Vico host cannot draw an 8 dp axis-free column, use a Material 3 linear progress indicator for that bar only (depends on T026)
- [ ] T028 [P] [US1] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/landing/DayListSection.kt` in server `position` order. A `receipt` row shows `name`, the receipt label, and the amount followed by `zł`. An `expense` row shows `name`, the expense label, and the amount followed by `zł`. A stored snapshot with zero entries shows the empty-day sentence (depends on T026)
- [ ] T029 [US1] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/landing/LandingScreen.kt` as a portrait column that scrolls vertically through the month summary, the two actions, and the day list. The month control is a year-month picker and refreshes only the summary. The day control is a date picker and refreshes only the day list. The menu contains Configuration and About; choosing either stays on this screen and leaves spending, language, and parked work unchanged. Show the last-saved-copy sentence only on a section whose snapshot has `showCachedCopy`. A section with no snapshot uses its empty state and does not reuse another period (depends on T027, T028)

**Checkpoint**: User Story 1 is testable on its own. Cached landing content is visible from Room without waiting for the network

---

## Phase 4: User Story 2 - Register expenses by hand (Priority: P2)

**Goal**: A person enters one or more expenses and Save sends the whole list when every row is valid. A list that cannot start because the network is unavailable waits on this screen and stays out of the landing figures

**Independent Test**: Open expense registration, enter two valid expenses, save while the server is reachable, and confirm the submission is accepted and the landing screen returns

### Tests for User Story 2

- [ ] T030 [P] [US2] Write failing validator tests in `app/src/test/java/pl/cheapskountant/mobile/domain/ExpenseValidatorTest.kt` for a missing, zero, or negative amount; more than 6 digits before the decimal mark; more than 2 fraction digits; a value above `999999.99`; a description longer than 100 characters; a description made only of spaces; an unset category; an empty category table; and an empty list. Accept `999999.99`, a past date, a future date, and a blank description
- [ ] T031 [P] [US2] Write failing MockWebServer tests in `app/src/test/java/pl/cheapskountant/mobile/data/ExpenseSubmitTest.kt` proving `POST /api/expense` is a JSON array of 1 to 500 objects, `amount` is a JSON number, a blank description is omitted, a spaces-only description is never posted, HTTP 201 deletes the outbox row, any HTTP error or a missing HTTP status becomes `ERROR` and keeps the form, and no network before the attempt stays `WAITING`
- [ ] T032 [P] [US2] Write a failing Compose journey in `app/src/androidTest/java/pl/cheapskountant/mobile/ui/ExpenseJourneyTest.kt` for one empty row (today, PLN, no category), Add, row trash, header back discarding the draft and keeping parked rows, invalid Save explaining the problem and staying on the form, and a valid reachable Save opening the landing screen

### Implementation for User Story 2

- [ ] T033 [US2] Implement `app/src/main/java/pl/cheapskountant/mobile/domain/ExpenseValidator.kt` so `ExpenseValidatorTest` passes. Parse amounts with the active locale's decimal mark. Do not trim descriptions. Map each failure to the matching User-Facing Text sentence: amount missing, amount rejected, description rejected, description spaces, no category chosen, categories unavailable, and expense list empty (depends on T030)
- [ ] T034 [US2] Implement `app/src/main/java/pl/cheapskountant/mobile/data/repository/ExpenseRepository.kt`. Validate the whole draft before any call. When the attempt starts, store one `ExpenseOutbox` plus its lines as `SENDING`. HTTP 201 deletes that row and requests the landing screen without writing the day cache. No network before the attempt stores `WAITING`, resets the draft to one empty row, and stays on expense registration. Any HTTP error, including 401, or no HTTP status stores `ERROR`, keeps the entered rows, and stays on the form. Refuse Save while `HouseholdCategory` is empty. `delete(localId)` removes that `ExpenseOutbox` row and its lines when the status is `WAITING` or `ERROR`, and does not call the API. `delete` is refused while the status is `SENDING`. Register the repository in `app/src/main/java/pl/cheapskountant/mobile/data/AppContainer.kt` (depends on T033, T012, T013, T031)
- [ ] T035 [US2] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/expense/ExpenseViewModel.kt` holding the draft rows and the outbox list. Refresh `GET /api/expenses` when the screen opens and keep the previous category table when that refresh fails (depends on T034)
- [ ] T036 [US2] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/expense/ExpenseScreen.kt` with description, amount, payment date, currency limited to PLN, EUR, and USD, a category menu of exact `HouseholdCategory.name` values with none pre-selected, trash, and Add (today, PLN, category unset). Save uses the Save label. Under the form, list each parked submission's categories, amounts, and currencies. `WAITING` offers Delete. `SENDING` is shown as waiting to send and offers no Delete. `ERROR` offers Delete and Resubmit and shows the send-failed sentence. Delete calls `ExpenseRepository.delete`. Header back drops only the current draft (depends on T035)
- [ ] T037 [US2] Point the Register expense action in `app/src/main/java/pl/cheapskountant/mobile/ui/landing/LandingScreen.kt` at the expense route, and call `OutboxSync.enqueue` from `ExpenseRepository` when a row is stored as `WAITING` (depends on T029, T036, T020)

**Checkpoint**: User Stories 1 and 2 both work. An accepted expense appears on the landing day list only after the next successful daily GET

---

## Phase 5: User Story 3 - Submit a receipt from photos (Priority: P3)

**Goal**: One to five photos of a single receipt are transcribed, reviewed with category edits and line removal, and saved. The header total stays the original `amountDue`. Photos leave the phone when the server accepts the receipt

**Independent Test**: Select two photos of one receipt, save while the server can read them, change one line's category, delete another line, save, and confirm the app returns to the landing screen only after the update is accepted

### Tests for User Story 3

- [ ] T038 [P] [US3] Write failing tests in `app/src/test/java/pl/cheapskountant/mobile/domain/ReceiptDocumentTest.kt` proving a category edit and a line removal keep every other field, keep surviving items in original order, leave `totals.amountDue` unchanged, omit `id` on the posted document, read the date portion of `issuedAt` with no zone shift, show `discount.total` (already negative) and a blank discount when the object is absent, and block Save for `Unknown` or any category missing from `HouseholdCategory` and for zero remaining lines
- [ ] T039 [P] [US3] Write failing MockWebServer tests in `app/src/test/java/pl/cheapskountant/mobile/data/ReceiptSubmitTest.kt` for `POST /api/transcription` repeating the `images` part in selection order with `image/jpeg`, `image/png`, or `image/webp` at most 10 MB, HTTP 200 storing the JSON and moving the row to `READY`, and `POST /api/receipt` HTTP 201 deleting the row. Map any HTTP error and a missing HTTP status to `ERROR`, and map no network before the attempt to `WAITING`
- [ ] T040 [P] [US3] Write a failing Compose journey in `app/src/androidTest/java/pl/cheapskountant/mobile/ui/ReceiptJourneyTest.kt` for the photo count out of 5, a refused sixth photo showing “A receipt can include at most 5 photos”, Save with zero photos, the reading sentence before review, a read-only header, Change selected and Change all guards, trash leaving the header total unchanged, an unlisted category blocking Save with “Choose a listed category for every line”, zero lines blocking Save with “A receipt must keep at least one line”, and an accepted Save returning to landing

### Implementation for User Story 3

- [ ] T041 [US3] Implement `app/src/main/java/pl/cheapskountant/mobile/data/local/ReceiptPhotoStore.kt`. Copy each accepted file under app-private `receipt-queue/<localId>/`. Accept JPEG, JPG, PNG, and WebP only. Recompress a file still over 10 MB to JPEG under 10 MB, and refuse it when that still fails. Store a generated display name for a camera photo, the gallery display name otherwise, and the byte size. Delete that directory when the outbox row is deleted (depends on T039)
- [ ] T042 [US3] Implement `app/src/main/java/pl/cheapskountant/mobile/domain/ReceiptDocument.kt` so `ReceiptDocumentTest` passes. Header fields are derived from `seller.tradeName`, the date portion of `receipt.issuedAt`, `receipt.currency`, and `totals.amountDue`. Checkbox state stays in memory. Change selected updates `items[].category` on checked lines. Change all updates every remaining line. Trash removes that item object and does not change `totals` (depends on T038)
- [ ] T043 [US3] Implement `app/src/main/java/pl/cheapskountant/mobile/data/repository/ReceiptRepository.kt`. Foreground photo Save with at least one photo sets `TRANSCRIBE`/`SENDING` and shows the reading sentence; HTTP 200 stores `receiptJson` and `baselineJson` and moves the row to `READY`. No network before a transcription attempt stores `TRANSCRIBE`/`WAITING`, stays on photo selection, and does not open review. Any HTTP error or a missing HTTP status on that foreground transcription stores `ERROR` and stays on photo selection. Review Save posts the edited JSON only when every remaining line's category is in `HouseholdCategory` and at least one line remains. HTTP 201 deletes the row and its photos and does not copy the body into the day cache. No network before a confirm attempt stores `CONFIRM`/`WAITING` and returns to photo selection. Any HTTP error or missing status on a foreground review Save stores `ERROR` and stays on review with the working list. `delete(localId)` removes the `ReceiptOutbox` row and deletes `receipt-queue/<localId>/` through `ReceiptPhotoStore` when the status is `WAITING`, `READY`, or `ERROR`, and does not call the API. `delete` is refused while the status is `SENDING`. Register the repository in `app/src/main/java/pl/cheapskountant/mobile/data/AppContainer.kt` (depends on T041, T042, T012)
- [ ] T044 [US3] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/photos/PhotoViewModel.kt` and `app/src/main/java/pl/cheapskountant/mobile/ui/photos/PhotoScreen.kt`. CameraX `ImageCapture` writes a JPEG into app-private storage. The system photo picker adds one gallery image at a time. Show a thumbnail, name, size, remove control, `{n} of 5`, and the same-receipt note. Refuse a sixth photo and show “A receipt can include at most 5 photos” / “Paragon może mieć co najwyżej 5 zdjęć”. A file the store rejects is not added. Save with zero photos shows “Add at least one photo of the receipt” / “Dodaj co najmniej jedno zdjęcie paragonu”. Header back returns to landing and deletes only the current selection that was never parked (depends on T041, T043)
- [ ] T045 [US3] Implement `app/src/main/java/pl/cheapskountant/mobile/ui/receipt/ReceiptReviewViewModel.kt` and `app/src/main/java/pl/cheapskountant/mobile/ui/receipt/ReceiptReviewScreen.kt`. Show the read-only store, payment date, currency, and total, then lines with checkbox, name, amount, discount, category, and trash. Tint checked rows with `primaryContainer`. Apply the Change selected and Change all guards using the spec sentences. Save with no remaining lines shows “A receipt must keep at least one line” / “Paragon musi zachować co najmniej jedną pozycję” and sends nothing. Save while any remaining line’s category is missing from `HouseholdCategory` shows “Choose a listed category for every line” / “Wybierz kategorię z listy dla każdej pozycji” and sends nothing. A direct open (the person is still waiting on this photo Save) returns to landing on an accepted Save, and header back deletes that session's row and photos. A queued open returns to photo selection on an accepted Save; header back restores `baselineJson`, leaves the row `READY`, and keeps the photos (depends on T042, T043)
- [ ] T046 [US3] List parked receipts on `app/src/main/java/pl/cheapskountant/mobile/ui/photos/PhotoScreen.kt`. `TRANSCRIBE` or `CONFIRM` with `WAITING` shows waiting to send and offers Delete. `SENDING` shows waiting to send and offers no Delete. `READY` offers Edit and Delete. `ERROR` offers Delete and Resubmit. Delete calls `ReceiptRepository.delete`. Edit opens review as a queued open. Call `OutboxSync.enqueue` when a receipt row is stored as `WAITING` (depends on T044, T045, T020)
- [ ] T047 [US3] Point the Receipt photo action in `app/src/main/java/pl/cheapskountant/mobile/ui/landing/LandingScreen.kt` at photo selection, and refresh `GET /api/expenses` when review opens without clearing the category table on failure (depends on T029, T046)

**Checkpoint**: User Stories 1–3 work on a reachable server. Transcription opens review only for the foreground save the person is still waiting on

---

## Phase 6: User Story 4 - Keep working when the server is unreachable (Priority: P4)

**Goal**: Previously retrieved months, days, and categories remain readable. Parked photos and expenses send when the network returns, including while the app is closed, and they stay out of the landing figures until a later successful GET

**Independent Test**: Retrieve a month while the server is reachable, then make it unreachable, confirm those figures remain, save a new expense and a receipt photo, and confirm both stay in their waiting lists and are not counted in the month total until the server accepts them

### Tests for User Story 4

- [ ] T048 [P] [US4] Write failing tests in `app/src/test/java/pl/cheapskountant/mobile/domain/OutboxTransitionTest.kt` for the receipt and expense diagrams in `specs/001-expense-receipt-client/data-model.md`: no network stays `WAITING`; any HTTP error, including 401, and a missing HTTP status become `ERROR`; Resubmit from a transcription error returns to `TRANSCRIBE`/`WAITING`; Resubmit from a confirm error returns to `CONFIRM`/`WAITING`; expense Resubmit returns to `WAITING`; Delete is refused while `SENDING`; an `ERROR` row is not eligible to send again until Resubmit
- [ ] T049 [P] [US4] Write a failing Room in-memory test in `app/src/androidTest/java/pl/cheapskountant/mobile/data/OutboxDaoTest.kt` proving outbox rows, photo rows, month snapshots, day snapshots, and categories survive a reopened database, and that deleting a receipt row removes its photo rows
- [ ] T050 [P] [US4] Write a failing Compose journey in `app/src/androidTest/java/pl/cheapskountant/mobile/ui/OfflineJourneyTest.kt` for the last-saved-copy sentence on a failed refresh, the empty state for a period that was never stored, a parked expense and a parked receipt staying out of the landing totals, an error row offering only Delete and Resubmit, and that row remaining unsent until Resubmit

### Implementation for User Story 4

- [ ] T051 [US4] Implement `app/src/main/java/pl/cheapskountant/mobile/domain/OutboxTransitions.kt` so `OutboxTransitionTest` passes, and use it from `ExpenseRepository` and `ReceiptRepository` (depends on T048, T034, T043)
- [ ] T052 [US4] Fill `doWork` in `app/src/main/java/pl/cheapskountant/mobile/data/sync/OutboxSyncWorker.kt`. Loop `WAITING` rows only, set `SENDING` before each attempt, and query again before returning so a row parked during the run is included. Transcription HTTP 200 moves the row to `READY` and does not navigate. Receipt HTTP 201 deletes the row and its files. Expense HTTP 201 deletes the row and does not open landing. The worker does not request a battery-optimization exemption and does not copy accepted bodies into the day cache (depends on T051, T020, T049)
- [ ] T053 [US4] Enqueue `outbox-sync` from `app/src/main/java/pl/cheapskountant/mobile/CheapskountantApp.kt` on process start, and from Resubmit in `ExpenseRepository` and `ReceiptRepository` after the transition back to `WAITING`. A phone that blocks the worker while the screen is off sends those `WAITING` rows the next time the process starts and the network is connected (depends on T052)
- [ ] T054 [US4] Keep read-failure behavior in `app/src/main/java/pl/cheapskountant/mobile/data/repository/LandingRepository.kt`: HTTP 401 and every other failed GET leave an existing month, day, or category snapshot in place and set `showCachedCopy` only when that snapshot is shown because the later fetch failed. A write rejected with HTTP 401 stays an outbox `ERROR` with the send-failed sentence. The UI still has no sign-in (depends on T025, T052)

**Checkpoint**: Parked work survives process death. Landing figures change only when a later GET replaces the cache

---

## Phase 7: User Story 5 - Read the app in Polish or English (Priority: P5)

**Goal**: Polish is the default. English is used when the phone language is English. Any other phone language uses Polish. Category names stay exactly as the server spelled them

**Independent Test**: Set the phone to Polish and read the landing, photo, review, and expense screens, then set it to English and confirm the same controls use the English text

### Tests for User Story 5

- [ ] T055 [P] [US5] Write a failing instrumented test in `app/src/androidTest/java/pl/cheapskountant/mobile/ui/LocaleJourneyTest.kt` that, under a Polish configuration and under a language that is neither Polish nor English, the four screens use the Polish column of User-Facing Text, and under English they use the English column. Assert `app/src/main/res/values/strings.xml` and `app/src/main/res/values-en/strings.xml` contain the same keys

### Implementation for User Story 5

- [ ] T056 [US5] Update `app/src/main/java/pl/cheapskountant/mobile/ui/landing/LandingScreen.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/landing/MonthSummarySection.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/landing/DayListSection.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/photos/PhotoScreen.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/receipt/ReceiptReviewScreen.kt`, and `app/src/main/java/pl/cheapskountant/mobile/ui/expense/ExpenseScreen.kt` so every User-Facing Text control is a string resource. Format money and dates with the configuration locale (depends on T008, T029, T036, T046, T055)
- [ ] T057 [US5] Render category and store names from server strings in `app/src/main/java/pl/cheapskountant/mobile/ui/landing/DayListSection.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/expense/ExpenseScreen.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/receipt/ReceiptReviewScreen.kt`, and the outbox rows. Add `app/src/test/java/pl/cheapskountant/mobile/domain/CategoryLabelTest.kt` asserting a household name such as `Jedzenie` is returned unchanged for both Polish and English locales (depends on T056)

**Checkpoint**: All five stories are independently functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Match the reference layout and run the quickstart checks

- [ ] T058 [P] Compare `app/src/main/java/pl/cheapskountant/mobile/ui/landing/LandingScreen.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/photos/PhotoScreen.kt`, `app/src/main/java/pl/cheapskountant/mobile/ui/receipt/ReceiptReviewScreen.kt`, and `app/src/main/java/pl/cheapskountant/mobile/ui/expense/ExpenseScreen.kt` with `.external-resources/screens-papier.png` and `.external-resources/screens-papier.html` for stacking, Papier colors, and the primary button. Keep the spec overrides in `specs/001-expense-receipt-client/quickstart.md`: Save on the photo screen, parked-receipt and parked-expense lists, an inert Configuration and About menu, and review limited to back, line trash, and the original header total
- [ ] T059 Run `gradlew.bat :app:assembleDebug` and `gradlew.bat :app:testDebugUnitTest` from the repository root and fix failures in the app module. `gradlew.bat :app:connectedDebugAndroidTest` runs when an API 26+ device or emulator is connected
- [ ] T060 Walk scenarios 1–7 in `specs/001-expense-receipt-client/quickstart.md` against a service that implements `docs/api/openapi.yaml`, and fix gaps those scenarios show in the four screens
- [ ] T061 Confirm `app/src/main/java/pl/cheapskountant/mobile/` is the only client module, accepted records are shown from the last successful GET, outbox rows stay out of month and day figures, the client does not convert currency, and the client does not collect a password

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies. Start immediately
- **Foundational (Phase 2)**: Depends on Setup. Blocks every user story
- **User Story 1 (Phase 3)**: Depends on Foundational. No dependency on later stories
- **User Story 2 (Phase 4)**: Depends on Foundational and on the landing actions from US1
- **User Story 3 (Phase 5)**: Depends on Foundational and on the landing actions from US1. Implement after US2 because both register repositories in `AppContainer.kt`
- **User Story 4 (Phase 6)**: Depends on the US2 expense submit path and the US3 receipt submit path
- **User Story 5 (Phase 7)**: Depends on the four screens from US1–US3
- **Polish (Phase 8)**: Depends on the user stories selected for the increment

### User Story Dependencies

- **User Story 1 (P1)**: First story after Foundational. Delivers the MVP
- **User Story 2 (P2)**: Uses the landing navigation and category cache from US1. Its online Save is testable without US3–US5
- **User Story 3 (P3)**: Uses the landing navigation and category cache from US1. Its online photo-and-review path is testable without US4–US5
- **User Story 4 (P4)**: Requires the outbox rows US2 and US3 create. The cache banner uses the snapshots US1 stores
- **User Story 5 (P5)**: Requires the screens whose labels it checks. String resources themselves are created in Setup

### Within Each User Story

- Tests are written and fail before that story's implementation tasks
- Domain rules before repositories
- Repositories before view models
- View models before screens
- Wiring back to landing and `OutboxSync.enqueue` after the screen exists

### Parallel Opportunities

- Phase 1: T003, T004, T005, T006, and T008 can run together after T002. T007 follows T004
- Phase 2: T009, T011, T013, T014, T015, T016, T017, T018, and T019 can run together. T010 follows T009. T012 follows T011. T020 follows T010, T013, and T014
- US1: T021, T022, and T023 together; then T027 and T028 together
- US2: T030, T031, and T032 together
- US3: T038, T039, and T040 together
- US4: T048, T049, and T050 together
- US5: T055 can be written while the string audit is planned; T056 and T057 stay sequential
- US2 and US3 touch `AppContainer.kt` and `LandingScreen.kt`, so those two wiring tasks stay sequential

---

## Parallel Example: User Story 1

```bash
# Tests together:
Task: "Write failing month-math tests in app/src/test/java/pl/cheapskountant/mobile/domain/MonthMathTest.kt"
Task: "Write failing money-format tests in app/src/test/java/pl/cheapskountant/mobile/domain/MoneyFormatTest.kt"
Task: "Write a failing Compose journey in app/src/androidTest/java/pl/cheapskountant/mobile/ui/LandingJourneyTest.kt"

# Screen sections together, after LandingViewModel:
Task: "Implement app/src/main/java/pl/cheapskountant/mobile/ui/landing/MonthSummarySection.kt"
Task: "Implement app/src/main/java/pl/cheapskountant/mobile/ui/landing/DayListSection.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run `MonthMathTest`, `MoneyFormatTest`, and `LandingJourneyTest`
5. Demo the landing screen against a reachable service

### Incremental Delivery

1. Setup + Foundational → project builds and the navigation shell opens
2. User Story 1 → month and day review (MVP)
3. User Story 2 → manual expenses and the expense waiting list
4. User Story 3 → photos, review, and the receipt waiting list
5. User Story 4 → background send, error/resubmit, and cached reads
6. User Story 5 → Polish and English on the four screens
7. Polish → reference layout and `quickstart.md` scenarios 1–7

### Parallel Team Strategy

After Foundational:

1. One developer completes User Story 1
2. Then expense registration (US2) and receipt capture (US3) can be split by screen, with `AppContainer.kt` and `LandingScreen.kt` edited by one person at a time
3. User Story 4 starts once both submit repositories exist
4. User Story 5 follows the four screens

---

## Notes

- [P] tasks use different files and do not depend on incomplete tasks
- [US1]–[US5] map to the priorities in `spec.md`
- Each story is completable up to its independent test
- Commit after each task or logical group
- Stop at any checkpoint to validate that story alone
