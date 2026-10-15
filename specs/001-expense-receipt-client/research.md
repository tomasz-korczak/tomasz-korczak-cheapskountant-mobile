# Research: Expense and Receipt Client

Decisions for the native Android client. The server contract is `docs/api/openapi.yaml` (Cheapskountant API 1.0.0). The feature spec wins where the Papier mock (`.external-resources/screens-papier.html`, `.external-resources/screens-papier.png`) differs. `.external-resources/cache.md` is an input idea; this file records where the client follows it and where the spec replaces it.

## 1. Platform and library versions

**Decision**: New single-module Gradle app.

| Piece | Choice |
| --- | --- |
| Language | Kotlin, built in with Android Gradle Plugin 9.4.1 (plugin baseline 2.2.10; keep the Kotlin version AGP 9.4.1 selects) |
| UI | Jetpack Compose, BOM `2026.09.00` (Material 3 1.4.0) |
| Build | AGP 9.4.1, Gradle wrapper 9.6.0, JDK 17 |
| SDK | `minSdk` 26 (Android 8.0), `compileSdk` 37, `targetSdk` 37 |
| Camera | CameraX 1.6.2 (`camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-compose`) |
| HTTP | Retrofit 3.0.0, OkHttp 5.5.0 |
| Lists | Compose `LazyColumn` / `Column` |
| Charts | Vico 3.3.1 `com.patrykandpatrick.vico:compose-m3` |
| Database | Room 2.8.5 with KSP aligned to the AGP Kotlin plugin |
| Background send | WorkManager 2.12.0 (`work-runtime-ktx`) |
| JSON | kotlinx.serialization JSON 1.10.0, receipt documents kept as a JSON tree |
| Tests | JUnit 4, kotlinx-coroutines-test, MockWebServer 5.5.0, Compose UI test, Room in-memory |

**Rationale**: These are the current stable lines that satisfy the requested stack. Compose 1.12 requires `compileSdk` 37 and AGP 9. WorkManager 2.12 requires API 24 or newer, which `minSdk` 26 already meets. Retrofit 3.0.0 is binary-compatible with OkHttp 5. Vico 3.3.1 publishes a Material 3 Compose artifact and requires `minSdk` 23 or newer.

**Alternatives considered**: An older AGP 8 line would not match the current Compose BOM. Room 3.0.x exists beside Room 2.8.5; this feature stays on `androidx.room` 2.8.5. Generating a full Kotlin client from OpenAPI would emit a lossy receipt model; the client keeps the receipt as JSON instead.

## 2. Which call fills which screen

**Decision**: Bind screens to the existing operations as follows. `GET /actuator/health` is unused.

| Screen or step | Call | When |
| --- | --- | --- |
| Landing, month summary | `GET /api/expense?from&to` | Open, and when the month changes, if the server answers |
| Landing, day list | `GET /api/expense/daily?payment_date` | Open, and when the day changes, if the server answers |
| Category menus and receipt-line checks | `GET /api/expenses` | Landing open, and again when an expense or review screen opens, if the server answers |
| Photo Save, server reachable | `POST /api/transcription` multipart field `images` | Immediately, with the reading state on the photo screen |
| Photo Save, server unreachable | none | Park the photos |
| Receipt Save | `POST /api/receipt` JSON | After local category checks, immediately if reachable, otherwise from the outbox |
| Expense Save | `POST /api/expense` JSON array | Immediately if reachable and the rows are valid, otherwise from the outbox |

Every protected call sends `Authorization: Bearer <api.key>` with that exact prefix. There is no sign-in screen and no token refresh.

**Rationale**: `GET /api/expense` is the category-and-currency aggregate used for the month. `GET /api/expense/daily` is the per-day list of manual expenses plus one row per receipt. `GET /api/expenses` is the household name dictionary (34 names, no `Unknown`). Transcription reads photos and does not store a receipt. `POST /api/receipt` stores the edited receipt and lets the server create one expense per remaining line. `POST /api/expense` stores manual expenses only.

**Alternatives considered**: Using `GET /api/expense` for the day list would return category sums, not store names and receipt rows. Using `POST /api/expense` for receipt lines would skip the receipt record the day list groups on. Calling health before each screen adds a second source of “reachable” that the screens do not need.

## 3. Phone database: cache and outbox

**Decision**: One Room database with two roles.

- **Read cache** stores the last successful category list, month summary, and day list. The landing screen reads this cache. A successful fetch replaces only the period or list that was fetched. A failed fetch leaves the previous rows in place.
- **Outbox** stores photos waiting to be read, a transcript waiting for category edits, a confirmed receipt waiting to be accepted, and a manual expense list waiting to be accepted. Month and day figures never include outbox rows.

The landing screen shows the spec sentence “Showing the last saved copy” / “Pokazano ostatnią zapisaną kopię” when the rows on screen came from the cache because the latest fetch for that period failed. It does not show a fetch timestamp. A month or day that was never stored uses its empty state, even if another period is cached.

**Rationale**: The spec requires previously retrieved figures to survive a lost connection, and it forbids parked work from appearing in those figures. The server stays the authority for accepted records. The cache is a display copy. The outbox is work the server has not accepted.

**Alternatives considered**: `cache.md` merges the outbox into the day list and shows “Czeka” or “Błąd wysyłki” on those rows. That contradicts FR-007 and the offline acceptance scenarios, so the day list stays a pure server snapshot. `cache.md` also shows a timestamp in the banner; the spec already defines the banner text.

## 4. Month summary math

**Decision**: Request `from` = the first calendar day of the selected month and `to` = the last calendar day, inclusive. The month payload has one currency code. That code is the summary currency. Sum the amounts per category with scale 2. The month total is the sum of those category amounts, written with the symbol after the amount, a space, two fraction digits, and the active language's thousands separator, plus the currency code. A positive category row shows the name, a bar at the unrounded share, and the share times 100 rounded half up. The row stays when that integer is 0. A negative category row shows the signed amount, a signed half-up percent, and an empty bar. A zero amount is omitted. When the total is zero and a category amount is not zero, the row omits the percent. An empty array stores total `0.00`, currency `PLN`, and no category rows, shown as 0 zł and PLN. The client does not convert currencies.

**Rationale**: The author confirmed the month API returns one currency code. An empty payload has no code, and the chosen zero display is 0 zł and PLN. Negative category amounts are visible without a filled share bar.

**Alternatives considered**: Keeping an older snapshot when several currencies appear, showing only the largest currency, or showing a zero total with no currency code.

## 5. Currency on day rows

**Decision**: Every day-list amount is shown with `zł` after the amount, separated by a space, two fraction digits, and the active language's decimal mark. The daily payload has no currency, and the author chose zł for every line. Changing the day does not change the selected month and does not start an extra summary request. The client shows the amount from `listDailyExpenses` and does not replace it with a receipt header total. Elsewhere, PLN uses `zł`, EUR uses `€`, and USD uses `$`, in that same suffix form.

Day order is the server order: manual expenses first, then receipt rows. A receipt row shows `name` (seller trade name), the receipt label, and the amount. A manual row shows `name` (category), the expense label, and the amount. Descriptions are not in this payload and are not shown.

**Rationale**: `DailyExpense` has `type`, `date`, `id`, `name`, and `amount`. Currency and description are omitted. A single symbol keeps every day line measurable.

**Alternatives considered**: Using the month snapshot's symbol when that month has one currency, or showing the amount with no symbol.

## 6. Receipt document round-trip

**Decision**: Keep the transcription body as JSON. Review reads `seller.tradeName`, the calendar date of `receipt.issuedAt` as written (no zone shift), `receipt.currency`, `totals.amountDue`, and each item’s `description`, `total`, `discount.total`, and `category`. The person can change only an item’s `category`, or remove an item. Save posts the same JSON with those edits applied: omitted `id`, surviving items in their original order, every other field unchanged, including `totals`. The header total on screen stays at the original `amountDue` after deletions.

The displayed payment date is the date portion of `issuedAt` before the time, matching the server rule that the local date of that value becomes the generated expense’s payment date.

**Rationale**: `POST /api/receipt` validates the full fiscal receipt. Rebuilding a receipt from the few fields on screen would drop tax id, tax summary, or payments and produce `422 incomplete`. The server, not the phone, creates the per-line expenses (`item.total` plus a negative line discount, or zero when there is no line discount).

**Alternatives considered**: A typed receipt DTO with only the fields the UI edits would fail server validation. Recomputing the header total after line deletion contradicts the spec.

## 7. Categories, `Unknown`, and a receipt with no lines

**Decision**: Household names come only from the latest `GET /api/expenses` list. They are shown exactly as returned in both languages. `Unknown` is the transcription default and is not in that list. Save on review is refused locally while any remaining line’s category is absent from the cached list, with “Choose a listed category for every line”. Save on an expense form is refused until a category list has been retrieved at least once, with “Categories are unavailable until they can be retrieved”. Photos can still be parked without that list.

Removing every line blocks Save. Nothing is sent. The screen says “A receipt must keep at least one line” / “Paragon musi zachować co najmniej jedną pozycję”. A description made only of spaces blocks expense Save with “Description cannot be only spaces” / “Opis nie może składać się z samych spacji”.

**Rationale**: The receipt contract requires at least one item. The author chose a local block instead of a request the server rejects. The expense contract rejects a spaces-only description, and that case has its own sentence.

**Alternatives considered**: Posting an empty item list and treating HTTP 422 as a failed save. Reusing the 100-character sentence for a spaces-only description.

## 8. Outbox states and retries

**Decision**: User-visible receipt states are waiting to send, ready to edit, and error. User-visible expense states are waiting to send and error. An in-flight row is shown as waiting to send.

Receipt phases:

1. `TRANSCRIBE` + waiting. The worker, or the photo screen when the person is waiting, posts the photos.
2. HTTP 200 stores the JSON and moves the row to ready to edit. The photo screen opens review only for the save the person is still waiting on. A later success leaves the row ready on the waiting list.
3. Confirmed review moves the row to `CONFIRM` + waiting and keeps the edited JSON.
4. HTTP 201 deletes the outbox row and its photo files.

Expense rows store the whole valid list as one submission.

Retry classes:

| Outcome | Transcription, receipt confirm, and expense store |
| --- | --- |
| No network before the attempt | Stay waiting, retry when connected |
| No HTTP status (timeout, connection dropped) | Error |
| Any HTTP error, including 400, 401, 422, 500, 503, and 504 | Error |

Error rows are sent again only after Resubmit. Delete is unavailable while a row is `SENDING`. Delete of a waiting, ready, or error row removes it and, for a receipt, its files. The client does not send `Idempotency-Key`. A read that returns 401 keeps the last snapshot and shows the last-saved-copy sentence. A write that returns 401 is an error. The person is not asked to sign in.

**Rationale**: The author limited automatic retry to an attempt that never starts because the network is unavailable. Every HTTP error and every missing HTTP status waits for Resubmit, which also avoids repeating a write that may already have been stored. The contract has no idempotency key.

**Alternatives considered**: Retrying transcription timeouts and HTTP 500 automatically. Treating a rejected API key as unreachable for writes. `cache.md` sends `Idempotency-Key`, which the contract does not define.

## 9. Background sending

**Decision**: `OutboxSyncWorker` is a WorkManager worker constrained to a connected network. The app enqueues its unique work when a row is parked, when the process starts, and when Resubmit is tapped. The worker loops over waiting rows and, before finishing, looks again so a row parked during the run is included. It does not request a battery-optimization exemption. If the phone blocks the worker while the screen is off, the next open enqueues the work again.

OkHttp timeouts for transcription: connect 15 seconds, write 60 seconds, read 90 seconds, call 120 seconds. The server model budget is 60 seconds and a slow run answers 504.

**Rationale**: WorkManager survives process death and can run with the screen off, which is the spec’s background rule, while the edge case already accepts “next open” when the phone blocks that work.

**Alternatives considered**: A foreground service would ask for a notification the spec does not mention. A coroutine scoped to the composition stops when the person leaves the app.

## 10. Category share bars

**Decision**: The month summary is a vertical list of category rows: name, bar, whole-number percent. Each bar is a Vico 3.3.1 Material 3 cartesian chart with axes and legend hidden, height 8 dp, and one column whose value is the unrounded share. Column colors cycle through the Papier palette `#1D4ED8`, `#B45309`, `#0F766E`, `#6D28D9`, `#78716C`. Names and percents are Compose text. Day lists, receipt lines, expense rows, photo rows, and outbox rows are ordinary Compose lists.

**Rationale**: FR-003 requires the bar inside the category row. The requested chart library is Vico. A single column chart for every category would put long household names on a portrait axis and would separate the bar from the row.

**Alternatives considered**: A Material 3 linear progress indicator is the usual control for this shape. It stays available as the fallback only if a Vico host cannot draw an 8 dp axis-free column. A pie chart does not match the reference or FR-003.

## 11. Language, orientation, theme, configuration

**Decision**: Polish strings live in `res/values/`. English strings live in `res/values-en/`. Category names are not string resources. The activity is portrait-locked. The theme is the Papier light palette from the HTML reference (`background #F3EEE4`, `surface #FFFCF7`, `ink #1C1917`, `muted #57534E`, `primary #115E59`, `onPrimary #FFFFFF`, `primaryContainer #D7EDEB`, `track #E8E0D2`) and does not follow a dark system theme. The launcher icon is an adaptive icon using Papier primary `#115E59`. When `.external-resources/app_icon.png` is present, that PNG is the icon instead.

`api.baseUrl` and `api.key` are read from the machine’s `local.properties` into `BuildConfig`. `local.properties` is gitignored. `local.properties.example` documents `api.baseUrl=http://192.168.1.20:8080/` and `api.key=example-key`. The base URL must end with `/`. Cleartext HTTP is enabled because that endpoint is HTTP on the LAN. The menu entries Configuration and About are visible on the landing screen and perform no navigation and no data change.

**Rationale**: FR-030 is the platform locale fallback: English selects `values-en`, every other language selects the Polish default. Configuration is specified as a label only, so language cannot be an in-app setting. The user named `local.properties` as the place for the endpoint and the API key.

**Alternatives considered**: A settings screen for the endpoint would build Configuration, which this feature leaves closed. HTTPS-only would not reach the stated server.

## 12. Photo files

**Decision**: CameraX writes a JPEG. The gallery picker may add JPEG, PNG, or WebP, up to 5 files for the current selection. Each file is copied into app-private storage. A file over 10 MB is recompressed to JPEG under 10 MB before it is counted as selected. The person sees a generated name for a camera photo, the display name for a gallery photo, and the size. Save with zero photos is refused. A sixth photo is refused. Files remain until the server returns 201 for that receipt or the person deletes the parked receipt. Back from photo selection deletes only the current unsent selection. Back from a direct review deletes that session’s files. Back from a queued review keeps the files and drops the unsent edits.

**Rationale**: The transcription endpoint accepts 1–5 images of those types, at most 10 MB each, and stores nothing. FR-031 defines when the files leave the phone.

**Alternatives considered**: Leaving gallery content URIs uncopied loses the picture after the picker permission expires, so a parked receipt could not be sent later.
