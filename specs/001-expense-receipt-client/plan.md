# Implementation Plan: Expense and Receipt Client

**Branch**: `001-expense-receipt-client` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-expense-receipt-client/spec.md`

## Summary

Native portrait Android client, Kotlin and Jetpack Compose, for household expenses and receipts. The phone shows a one-currency month summary and a day list, registers manual expenses, and submits one to five receipt photos for server transcription and review. The server at `api.baseUrl` remains the authority for accepted records. Room keeps the last successful reads and an outbox of work the server has not accepted. WorkManager sends that outbox when the network returns, including while the app is closed, and the next launch retries it if the phone blocked background work.

Screen data uses the existing HTTP API in `docs/api/openapi.yaml`:

| UI | Operation |
| --- | --- |
| Month summary | `GET /api/expense?from&to` |
| Day list | `GET /api/expense/daily?payment_date` |
| Category menus | `GET /api/expenses` |
| Photo Save | `POST /api/transcription` (multipart `images`) |
| Receipt Save | `POST /api/receipt` |
| Expense Save | `POST /api/expense` |

Protected calls send `Authorization: Bearer <api.key>` from `local.properties`. There is no user sign-in. Polish is the default UI language; English is `values-en`. Layout follows the Papier reference, and the spec wins where that picture differs (Save on every primary button, parked-work lists, inert Configuration and About).

## Technical Context

**Language/Version**: Kotlin via Android Gradle Plugin 9.4.1 built-in Kotlin (baseline 2.2.10; use the version AGP 9.4.1 selects)

**Primary Dependencies**: Jetpack Compose BOM 2026.09.00, Material 3, CameraX 1.6.2, Retrofit 3.0.0, OkHttp 5.5.0, Vico `compose-m3` 3.3.1, Room 2.8.5, WorkManager 2.12.0, kotlinx.serialization JSON 1.10.0, Navigation Compose (current stable)

**Storage**: Room database for the read cache and the outbox. Receipt photos in app-private files. API base URL and API key in `local.properties` (`api.baseUrl`, `api.key`), compiled into `BuildConfig`.

**Testing**: JUnit 4, kotlinx-coroutines-test, MockWebServer 5.5.0, Compose UI test, Room in-memory. Manual scenarios in [quickstart.md](quickstart.md).

**Target Platform**: Android 8.0 and newer (`minSdk` 26). `compileSdk` 37, `targetSdk` 37. JDK 17. Gradle wrapper 9.6.0.

**Project Type**: Single-module native Android application. This repository stays the client only.

**Performance Goals**: Cached landing content is on screen within 5 seconds of open (SC-001). Transcription waits for the server’s 60-second model budget; OkHttp read timeout is 90 seconds. UI lists stay on the composition frame budget for a household-sized month (tens of categories, not thousands of rows).

**Constraints**: Portrait only. Cleartext HTTP allowed for the configured LAN base URL. No sign-in. No currency conversion. No second authority for accepted records. Offline: last retrieved month, day, and categories remain; new photos and expenses wait in the outbox and stay out of the landing figures until a later successful `GET`. Polish default, English available. Configuration and About are visible and do nothing.

**Scale/Scope**: One person, one phone, four working screens plus an inert menu. At most 5 photos per receipt, at most 500 expenses per manual submit (the UI submits the current form, normally a handful of rows). Category dictionary is 34 names.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Result |
| --- | --- | --- |
| I. Native Android graphical client | Expense and receipt tasks run in a Compose UI on Android. The product is not a web or command-line client. | Pass |
| II. Client of the server | Accepted records are read and written only through `docs/api/openapi.yaml`. Room cache displays the last server payload and does not override it. The outbox holds work the server has not accepted and is excluded from month and day figures. Transcription, category assignment of record, currency conversion, and expense creation from receipt lines stay on the server. | Pass |
| III. Expense and receipt records | Review can change a line’s category or remove a line, then submit that edited receipt. Other receipt fields are posted unchanged. Header total stays the original `amountDue`. Photos are deleted when the server returns 201 or when the person deletes the parked receipt. | Pass |
| Platform constraints | One Android app module. No server implementation in this repository. `minSdk` 26, portrait, graphical. | Pass |

Post-design re-check: the data model, client binding, and research decisions keep the same split. The month payload has one currency. Day-list amounts use zł. An empty month shows 0 zł and PLN. A receipt with no lines is blocked on the phone. Only a send that cannot start offline stays waiting; every HTTP error and every missing HTTP status is an error until Resubmit. Delete is unavailable while a send is in progress. No gate failed.

## Project Structure

### Documentation (this feature)

```text
specs/001-expense-receipt-client/
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   └── client-api.md    # How the client uses docs/api/openapi.yaml
└── tasks.md             # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/pl/cheapskountant/mobile/
    │   │   ├── MainActivity.kt
    │   │   ├── CheapskountantApp.kt
    │   │   ├── ui/
    │   │   │   ├── theme/                 # Papier Material 3 colors
    │   │   │   ├── navigation/
    │   │   │   ├── landing/               # Month summary, actions, day list, menu
    │   │   │   ├── photos/                # CameraX, gallery, current selection, receipt outbox
    │   │   │   ├── receipt/               # Read-only header, line list, bulk category
    │   │   │   └── expense/               # Draft rows, expense outbox
    │   │   ├── data/
    │   │   │   ├── remote/                # Retrofit, OkHttp bearer interceptor, JSON tree
    │   │   │   ├── local/                 # Room entities and DAOs
    │   │   │   ├── repository/
    │   │   │   └── sync/                  # OutboxSyncWorker
    │   │   └── domain/                    # Month math, money and date format, validation
    │   └── res/
    │       ├── values/strings.xml         # Polish
    │       ├── values-en/strings.xml      # English
    │       └── drawable/                  # Adaptive launcher icon, Papier primary #115E59. Use .external-resources/app_icon.png when that file is present.
    ├── test/                              # Month math, validation, outbox, MockWebServer
    └── androidTest/                       # Compose journeys for the four screens

gradle/libs.versions.toml
settings.gradle.kts
build.gradle.kts
gradle.properties
local.properties.example
```

**Structure Decision**: One Android application module, package `pl.cheapskountant.mobile`. The server stays in `cheapskountant-service` and is consumed only through HTTP. Feature UI is grouped by the four screens. Remote, Room, and WorkManager sit behind repositories so composables read cached state and call use cases.

## Screen behavior

Portrait column, vertical scroll. Reference: `.external-resources/screens-papier.html` and `.external-resources/screens-papier.png`.

**Landing.** Top to bottom: month and year (year-month picker), “Wydatki” / “Expenses”, total with currency symbol, currency code, Vico share bars with name and whole-number percent, primary Zdjęcie paragonu, outlined Wpisz wydatek, day heading (date picker), day rows. Menu holds Konfiguracja and O aplikacji and does not navigate. Month changes refresh only the summary. Day changes refresh only the day list. Empty copy comes from the spec table. Last-saved-copy sentence appears only when that section is showing a snapshot after a failed fetch.

**Photo selection.** Back returns to landing and drops the current selection only. Take photo (CameraX) and Choose from gallery, count `n` of 5, same-receipt note, thumbnail rows with name, size, and remove. Primary button Zapisz. Under the selection, when any exist: waiting receipts with status and the allowed actions (delete; edit and delete; delete and resubmit).

**Receipt review.** Back destination is landing for a direct open (session discarded) and photo selection for a queued open (edits dropped, row kept). Read-only store, payment date, currency, and total. Lines: checkbox, name, amount, discount (negative amount, or blank), category, trash. Category picker plus Zmień zaznaczone and Zmień wszystkie. Zapisz. Checked rows use the Papier primary-container tint.

**Expense registration.** Back returns to landing and drops the draft, keeping parked submissions. Rows: description, amount, payment date, currency PLN/EUR/USD, category, trash. Plus adds a row (today, PLN, no category). Zapisz. Under the form: waiting expenses with categories, amounts, currencies, and delete or delete plus resubmit.

## Client flow

```text
Landing --reads--> Room cache --filled by--> GET month, GET day, GET categories

Photo Zapisz --online--> POST /api/transcription --> review
             --offline--> outbox TRANSCRIBE/WAITING

Review Zapisz --lines listed, online--> POST /api/receipt --> delete photos, leave review
              --lines listed, offline--> outbox CONFIRM/WAITING, photo screen
              --line not in category list--> stay, explain

Expense Zapisz --valid, online--> POST /api/expense --> landing
               --valid, offline--> outbox WAITING, clear form, stay
               --invalid--> stay, explain

WorkManager, network connected, and again on next process start
    waiting TRANSCRIBE --> POST /api/transcription
    waiting CONFIRM    --> POST /api/receipt
    waiting expenses   --> POST /api/expense
```

Foreground photo Save shows the reading sentence and opens review on 200. The worker does not navigate. Accepted writes are not copied into the day list; the next landing `GET` is what updates figures.

## Complexity Tracking

No constitution gate failed. Nothing to justify in this table.
