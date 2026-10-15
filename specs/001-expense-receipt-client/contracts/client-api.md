# Client API Binding

The server contract remains [`docs/api/openapi.yaml`](../../../docs/api/openapi.yaml) (Cheapskountant API 1.0.0). This file records how the Android client uses that contract. It does not add operations.

Base URL: `BuildConfig.API_BASE_URL` from `local.properties` `api.baseUrl`, ending with `/`.

Header on every call below:

```http
Authorization: Bearer <BuildConfig.API_KEY>
```

The prefix is the six characters `Bearer` plus one space. A missing or wrong key returns 401 with `reason=not authorized` for all of these routes. The client does not collect a user password.

Failure JSON, when present:

```json
{ "reason": "invalid submission", "explanation": "..." }
```

`reason` is one of `invalid submission`, `not authorized`, `unreadable`, `incomplete`, `transcription unavailable`, `storage failed`. The person sees the spec’s send-failed sentence. `explanation` is logged.

## Landing

### Month summary — `summarizeExpenses`

`GET /api/expense?from={YYYY-MM-DD}&to={YYYY-MM-DD}`

| Query | Value |
| --- | --- |
| `from` | First day of the selected month |
| `to` | Last day of the selected month |

200 body: array of `{ category, currency, amount }`. `amount` is a JSON number. The payload uses one currency code. An empty month is `[]` and is shown as 0 zł with currency code PLN. A negative category amount is shown with a signed percent and an empty bar.

400 means the dates were malformed. The client builds both dates, so this is a defect, not a user correction. The cached month stays.

### Day list — `listDailyExpenses`

`GET /api/expense/daily?payment_date={YYYY-MM-DD}`

200 body: array of `{ type, date, id, name, amount }` with `type` of `expense` or `receipt`. Currency is absent. An empty day is `[]` and the client stores a snapshot with no entries.

Display:

| `type` | Title | Label | Amount |
| --- | --- | --- | --- |
| `receipt` | `name` (seller trade name) | Receipt / Paragon | `amount` followed by `zł` |
| `expense` | `name` (category) | Expense / Wydatek | `amount` followed by `zł` |

### Categories — `listHouseholdCategories`

`GET /api/expenses`

200 body: 34 objects `{ "name": "<household name>" }` from `Jedzenie` through `Fermentacja alkoholowa`. `Unknown` is not included. The client replaces `HouseholdCategory` with this array in order. Names are shown as returned.

## Photo save — `transcribeReceipt`

`POST /api/transcription`

`multipart/form-data`. Repeat the part name `images` once per page, in selection order, 1 to 5 times. Content type is `image/jpeg`, `image/png`, or `image/webp`. Each part is at most 10 MB.

200 body: a `Receipt` JSON document with no `id`. Each item’s `category` is `Unknown`. Nothing is stored on the server. The client saves that document and opens review when the person is still waiting on this save.

| Status | Client |
| --- | --- |
| 200 | Phase becomes ready to edit |
| Any HTTP error, including 400, 401, 422, 503, and 504 | Error until Resubmit |
| No HTTP status | Error until Resubmit |
| No network before the attempt | Stay waiting |

The photo screen shows “Reading the receipt” / “Odczytywanie paragonu” only while this foreground call runs.

## Receipt save — `createReceipt`

`POST /api/receipt`

`Content-Type: application/json`. Body is the stored transcription JSON after review edits:

- `id` omitted
- `items` is the remaining lines, original order, `category` set to a household name
- `seller`, `receipt`, `taxSummary`, `totals`, `payments`, and any optional groups are unchanged
- line `discount` objects stay on the items that had them
- `totals.amountDue` stays the original header total even when lines were removed

The client does not post a receipt with no remaining lines. Save is blocked with “A receipt must keep at least one line” / “Paragon musi zachować co najmniej jedną pozycję”. API 1.0.0 would answer 422 `incomplete` for an empty `items` array.

201 body: the stored receipt including a new `id`. The client deletes the local photos and the outbox row. It does not copy this body into the day cache. The next daily fetch shows the server’s receipt row. That row’s amount is the sum of expenses the server created from the remaining lines, which can differ from the review header total.

| Status | Client |
| --- | --- |
| 201 | Delete photos and outbox row. Direct open returns to landing. Queued open returns to photo selection |
| Any HTTP error, including 400, 401, 422, and 500 | Error until Resubmit. Foreground save stays on review with the working list |
| No HTTP status | Error until Resubmit |
| No network before the attempt | Stay waiting |

Generated expense rule, applied by the server, not recomputed for the cache: line amount is `items[].total` plus `items[].discount.total` when a line discount exists. The receipt-level `discountSummary` does not become an expense. Payment date is the local calendar date of `receipt.issuedAt`. Currency is `receipt.currency`.

## Expense save — `storeExpenses`

`POST /api/expense`

`Content-Type: application/json`. Body is an array of 1 to 500 objects, in row order:

```json
[
  {
    "amount": 12.5,
    "paymentDate": "2026-10-12",
    "category": "Jedzenie",
    "currency": "PLN",
    "description": "Warzywa"
  }
]
```

| Field | Wire rule |
| --- | --- |
| `amount` | JSON number, scale 2, greater than 0 and at most 999999.99 |
| `paymentDate` | `YYYY-MM-DD` |
| `category` | Exact household name. `Unknown` is rejected by the server |
| `currency` | `PLN`, `EUR`, or `USD` |
| `description` | Omitted when the person left it blank. A description of only spaces is blocked on the phone |

201 returns the stored array. The client deletes the outbox row and opens the landing screen. It does not insert the new expenses into the day cache.

One invalid object rejects the whole array. The client validates the draft before posting so the common mistakes never leave the phone. Parked lists were valid at save time.

| Status | Client |
| --- | --- |
| 201 | Delete the outbox row. Foreground path opens landing |
| Any HTTP error, including 400, 401, and 500 | Error until Resubmit. Foreground path keeps the form |
| No HTTP status | Error until Resubmit |
| No network before the attempt | Stay waiting |

## Read-failure behavior

A failed `GET`, including HTTP 401, does not clear a snapshot that already exists for that month, that day, or the category list. The landing section that has such a snapshot shows the last-saved-copy sentence. A section with no snapshot shows its empty state. An empty month snapshot is total 0 and currency PLN. Outbox rows are not added to either section. A failed write, including HTTP 401, is an error until Resubmit. Delete is unavailable while that write is in progress.

## Calls the client does not make

| Call | Why |
| --- | --- |
| `GET /actuator/health` | Screen data and the worker use the result of the real call |
| Any update or delete of an accepted expense or receipt | Not in this API, and not in this feature |
| `Idempotency-Key` | Not in the contract |
| A user-token or refresh call | The only credential is the configured API key |
