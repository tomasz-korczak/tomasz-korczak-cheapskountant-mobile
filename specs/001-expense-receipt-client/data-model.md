# Data Model: Expense and Receipt Client

The phone stores a read cache of server snapshots and an outbox of work the server has not accepted. Accepted expenses and receipts are not a second ledger. After HTTP 201 the outbox row is deleted; the landing figures change only when a later `GET` replaces the cache.

Money in the cache and the outbox is stored as scale-2 decimal strings (`0.00`), never as binary floating point. Receipt money fields inside the stored JSON stay strings with a dot and two fraction digits, as in the API.

## Read cache

### HouseholdCategory

| Field | Type | Rules |
| --- | --- | --- |
| position | int | Order from `GET /api/expenses`, starting at 0 |
| name | string | Primary key. Exact server spelling, 1–50 characters. Not translated |

A successful fetch replaces the whole table. The table is empty until the first success. Expense Save is refused while the table is empty. Receipt Save is refused while any remaining line’s category is missing from this table.

### MonthSnapshot

| Field | Type | Rules |
| --- | --- | --- |
| yearMonth | string | Primary key, `YYYY-MM` |
| currency | string | The one currency code from the month payload. `PLN` when the payload is empty |
| totalAmount | decimal string | Sum of category amounts. `0.00` for an empty month |
| fetchedAt | epoch millis | Last successful fetch of this month |
| showCachedCopy | boolean | True when this row is on screen because a later fetch failed |

### MonthCategory

| Field | Type | Rules |
| --- | --- | --- |
| yearMonth | string | Part of primary key |
| category | string | Part of primary key. Server spelling, including `Unknown` |
| amount | decimal string | Non-zero. A negative amount is shown with a signed percent and an empty bar |
| position | int | Stable order from the payload after per-category summing |

Rows for a month are replaced together with its snapshot. The bar value is `amount / totalAmount` as a ratio. The percent label is `amount * 100 / totalAmount`, half up, integer. A row with a zero amount is not stored.

### DaySnapshot

| Field | Type | Rules |
| --- | --- | --- |
| date | string | Primary key, `YYYY-MM-DD` |
| fetchedAt | epoch millis | Last successful fetch of this day |
| showCachedCopy | boolean | True when this row is on screen because a later fetch failed |

An empty day still has a snapshot and zero entries, so the client can tell “retrieved, nothing recorded” from “never retrieved”.

### DayEntry

| Field | Type | Rules |
| --- | --- | --- |
| date | string | Part of primary key |
| position | int | Part of primary key. Server order |
| type | `expense` or `receipt` | API `type` |
| serverId | long | Expense id or receipt id |
| name | string | Category for `expense`, seller trade name for `receipt` |
| amount | decimal string | API amount, unmodified |

Currency is not a column. Every day-list amount is displayed with `zł`.

## Outbox

### ReceiptOutbox

| Field | Type | Rules |
| --- | --- | --- |
| localId | UUID string | Primary key. Generated on the phone |
| phase | `TRANSCRIBE` or `CONFIRM` | Which call is next |
| status | `WAITING`, `SENDING`, `READY`, `ERROR` | See transitions |
| receiptJson | string or null | Full receipt JSON. Null until transcription returns 200 |
| baselineJson | string or null | Last JSON that review may revert to |
| errorReason | string or null | API `reason` when status is `ERROR`. The screen uses the spec send-failed sentence |
| createdAt | epoch millis | |
| updatedAt | epoch millis | |

User-facing status:

| Stored | Shown | Actions |
| --- | --- | --- |
| `TRANSCRIBE` + `WAITING` or `SENDING` | Waiting to send | Delete |
| `CONFIRM` + `WAITING` or `SENDING` | Waiting to send | Delete |
| `READY` | Ready to edit | Edit, Delete |
| `ERROR` | Error | Delete, Resubmit |

`SENDING` is internal. The list treats it as waiting to send.

### ReceiptPhoto

| Field | Type | Rules |
| --- | --- | --- |
| localId | UUID string | Part of primary key, references ReceiptOutbox |
| position | int | Part of primary key, 0–4. Page order sent to transcription |
| relativePath | string | File under app-private `receipt-queue/<localId>/` |
| displayName | string | Camera-generated name or gallery display name |
| byteSize | long | Size after the copy or recompress |

One to five photos while the receipt is queued, ready, or in error. Deleted with the receipt row on HTTP 201 or on user delete.

### ExpenseOutbox

| Field | Type | Rules |
| --- | --- | --- |
| localId | UUID string | Primary key |
| status | `WAITING`, `SENDING`, `ERROR` | No ready state |
| errorReason | string or null | API `reason` when `ERROR` |
| createdAt | epoch millis | |
| updatedAt | epoch millis | |

Shown as waiting to send (`WAITING`, `SENDING`) with Delete, or error with Delete and Resubmit. The list shows each line’s category, amount, and currency.

### ExpenseOutboxLine

| Field | Type | Rules |
| --- | --- | --- |
| localId | UUID string | Part of primary key |
| position | int | Part of primary key. Submit order |
| description | string or null | Omitted on the wire when null. At most 100 characters. Not whitespace-only |
| amount | decimal string | Greater than `0.00`, at most `999999.99`, scale 2 |
| paymentDate | string | `YYYY-MM-DD`, past or future |
| currency | `PLN`, `EUR`, or `USD` | Exact uppercase |
| category | string | Exact `HouseholdCategory.name` |

One outbox row is one `POST /api/expense` array. A single invalid line is not parked; the form explains the first problem and sends nothing.

## Screen models (not tables)

### Expense draft row

Starts as one empty row: description empty, amount empty, payment date today, currency PLN, category unset. Add appends the same defaults. Trash removes that row, including the last one. Save requires at least one row and every row valid.

### Receipt review draft

Holds `localId`, whether it was opened directly after a photo save or from the waiting list, and a working JSON document. Header fields are derived and are not writable. Checkbox state is in memory only. Change selected updates `items[].category` on checked lines. Change all updates every remaining line. Trash removes that item object. Neither action changes `totals`.

### Landing selection

Selected month and selected day live in composition state, defaulting to the current month and today. They are independent. They are not written to Room. Cold start returns to the current month and today; previously cached periods remain available when chosen again.

## Validation

| Check | Where | Result |
| --- | --- | --- |
| Zero photos | Photo Save | Stay, “Add at least one photo of the receipt” |
| Sixth photo | Photo add | Refuse, “A receipt can include at most 5 photos” |
| Image type outside jpeg, jpg, png, webp | Photo add | Refuse the file |
| File still over 10 MB after JPEG recompress | Photo add | Refuse the file |
| Expense amount missing, zero, or negative | Expense Save | “Enter an amount greater than zero” |
| More than 6 integer digits, more than 2 fraction digits, or above `999999.99` | Expense Save | “Use at most 6 digits before the decimal mark and 2 after” |
| Description longer than 100 | Expense Save | “Description can have at most 100 characters” |
| Description is only spaces | Expense Save | “Description cannot be only spaces”. Do not post it |
| Category unset | Expense Save | “Choose a category” |
| Category list never retrieved | Expense Save | “Categories are unavailable until they can be retrieved” |
| No complete row | Expense Save | “Add at least one complete expense” |
| Change selected with no checked line | Review | “Select at least one line” |
| Either change action with no category chosen | Review | “Choose a category” |
| Remaining line category not in `HouseholdCategory` | Receipt Save | “Choose a listed category for every line”. Do not post |
| Zero remaining lines | Receipt Save | “A receipt must keep at least one line”. Do not post |

Expense amounts are JSON numbers on the wire. Receipt amounts stay JSON strings.

## State transitions

### Receipt

```text
photos parked or online save started
        │
        v
TRANSCRIBE / WAITING  --no network before the attempt--> TRANSCRIBE / WAITING
        │
        ├--HTTP error or no HTTP status--> ERROR
        │
        └--200--> READY (JSON stored, photos kept)
                    │
        edit + Save online, 201 --> row and photos deleted
        edit + Save online, HTTP error or no status --> ERROR, stay on review if foreground
        edit + Save offline --> CONFIRM / WAITING, photo screen
        back from direct open --> row and photos deleted, landing
        back from queue --> revert to baselineJson, stay READY, photo screen

CONFIRM / WAITING --no network before the attempt--> CONFIRM / WAITING
              --201--> row and photos deleted
              --HTTP error or no HTTP status--> ERROR
ERROR --Resubmit--> previous phase / WAITING
WAITING, READY, or ERROR, and not SENDING --Delete--> row and photos deleted
SENDING --Delete--> unavailable until the attempt finishes
```

Resubmit from a transcription error returns to `TRANSCRIBE / WAITING`. Resubmit from a confirm error returns to `CONFIRM / WAITING`.

### Expense

```text
valid Save, server reachable --201--> row deleted, landing
valid Save, HTTP error or no status --> ERROR, form kept
valid Save, no network before the attempt --> WAITING, form reset to one empty row, stay
WAITING --201--> row deleted
WAITING --no network before the attempt--> WAITING
WAITING --HTTP error or no HTTP status--> ERROR
ERROR --Resubmit--> WAITING
WAITING or ERROR, and not SENDING --Delete--> row deleted, not sent
SENDING --Delete--> unavailable until the attempt finishes
back --> current draft discarded, outbox kept, landing
```

## Relationships

- A `MonthSnapshot` has many `MonthCategory` rows for one `yearMonth`.
- A `DaySnapshot` has many `DayEntry` rows for one `date`.
- A `ReceiptOutbox` has one to five `ReceiptPhoto` rows until it is deleted.
- A `ReceiptOutbox.receiptJson` item category should match a `HouseholdCategory.name` before confirm is attempted. `Unknown` does not match.
- An `ExpenseOutbox` has one or more `ExpenseOutboxLine` rows.
- `DayEntry.serverId` is not a foreign key to the outbox. Outbox rows have no server id. The daily payload’s id is stored only for stable ordering and display.
