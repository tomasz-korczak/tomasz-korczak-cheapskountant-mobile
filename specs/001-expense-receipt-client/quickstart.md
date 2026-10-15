# Quickstart: Expense and Receipt Client

Validation guide for the planned client. Implementation steps belong in `tasks.md`. Server fields are defined in [contracts/client-api.md](contracts/client-api.md) and [data-model.md](data-model.md).

## Prerequisites

- JDK 17, Android SDK 37, and a device or emulator running Android 8.0 (API 26) or newer.
- A reachable `cheapskountant-service` that implements `docs/api/openapi.yaml`.
- `local.properties` in the repository root (gitignored):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
api.baseUrl=http://192.168.1.20:8080/
api.key=example-key
```

`api.baseUrl` ends with `/`. `api.key` equals the service `API_KEY`. The sample host is the LAN address from the feature request; point it at the machine that runs the service.

Use the adaptive launcher icon in Papier primary `#115E59`. When `.external-resources/app_icon.png` is present, copy it over that icon, then install the app module.

## Build and unit checks

From the repository root, after the Gradle project exists:

```shell
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:connectedDebugAndroidTest
```

Unit tests cover month summing and percent rounding, expense validation, receipt JSON edits (category change and line removal without changing `totals`), and outbox transitions against MockWebServer. Connected tests cover the four screens’ main actions. `connectedDebugAndroidTest` needs a running emulator or device.

## Manual scenarios

Set the phone language to Polish for scenarios 1–6. Repeat the visible labels in English for scenario 7.

### 1. Landing reads the server

1. With the service up and at least one accepted expense in the current month, open the app.
2. Confirm there is no sign-in.
3. Confirm the month title, a total with `zł`, `€`, or `$` plus the currency code, and one row per spending category with a bar and a whole-number percent.
4. Confirm the day list for today uses store name + Paragon, or category + Wydatek, each with an amount followed by zł.
5. Open the menu and tap Konfiguracja, then O aplikacji. Confirm the landing figures and any parked rows are unchanged.

Expected: data matches `GET /api/expense` for the month and `GET /api/expense/daily` for today. Category names match `GET /api/expenses` exactly.

### 2. Month and day are independent

1. Note the selected day.
2. Open the month control and choose another month. Confirm the day text is unchanged.
3. Open the day control and choose another day. Confirm the month title is unchanged.
4. Choose a month and a day you know are empty. Confirm the empty-month and empty-day sentences, and a month total of 0 zł with PLN.

### 3. Register two expenses

1. Tap Wpisz wydatek. Confirm one empty row, today’s date, PLN, and no category selected.
2. Enter two valid rows (description at most 100 characters, amount such as `12,50`, a listed category).
3. Tap Zapisz.

Expected: `POST /api/expense` returns 201, the app opens the landing screen, and the new rows are absent from the day list until that day is fetched again. After the landing fetch, each manual expense shows its category, Wydatek, and amount.

Also confirm Save does nothing and explains the problem for a missing amount, a missing category, `0`, `1000000`, three fraction digits, a 101-character description, and a description made only of spaces.

### 4. Receipt photos and review

1. Tap Zdjęcie paragonu. Take or pick one to five photos of one receipt.
2. Confirm each row has a thumbnail, a name, and a size, and the count reads `n z 5`.
3. Tap Zapisz while the service is reachable. Confirm Odczytywanie paragonu, then the review screen.
4. Confirm the store, date, currency, and total are not editable. Confirm `Unknown` lines cannot be saved.
5. Set a listed category on the lines (Zmień zaznaczone or Zmień wszystkie), delete one line, and tap Zapisz.

Expected: `POST /api/transcription` runs before review. `POST /api/receipt` returns 201, the photos are gone from app storage, and the app returns to landing. The header total on review did not change when the line was deleted.

Also confirm a sixth photo is refused, Save with no photo is refused, removing every line blocks Save, and the header back arrow from this direct review returns to landing without saving.

### 5. Offline cache and outbox

1. Open the current month and today once while the service is reachable.
2. Turn the network off.
3. Open the app. Confirm those figures remain and the last-saved-copy sentence is visible. Choose a month that was never fetched and confirm the empty state.
4. Park a new expense. Confirm the form clears to one empty row, the submission appears under Oczekujące wydatki as Oczekuje na wysłanie, and the landing total does not include it.
5. Park receipt photos. Confirm review does not open, the receipt appears under Oczekujące paragony, and the landing total does not include it.
6. Restore the network and leave the app, or reopen it if the phone blocked background work.

Expected: waiting photos move to Gotowy do edycji without another Send tap. Editing that receipt and saving online removes it from the list and deletes its photos. A waiting expense disappears from the list after the server accepts it. The next landing fetch is what adds the accepted records to the month and the day.

### 6. Errors stay put

1. Cause a transcription `422` (for example a photo that is not a receipt) or a receipt save the server rejects.
2. Confirm the row is Błąd and offers only Usuń and Wyślij ponownie.
3. Leave it. Confirm it is not sent again until Wyślij ponownie.
4. Delete a parked receipt and confirm its photos are removed from app storage.

### 7. English

Set the phone language to English. Walk landing, photo, review, and expense registration. Confirm the English column of the spec’s User-Facing Text, and confirm category names are still the Polish server strings. Set the phone to a third language and confirm the UI returns to Polish.

## Layout check

Compare the four screens with `.external-resources/screens-papier.png` for stacking, the Papier colors, and the primary button. Prefer the spec where the picture differs:

- The primary button is Zapisz / Save on the photo screen as well.
- Photo selection lists parked receipts. Expense registration lists parked expenses.
- The landing menu contains Configuration and About and does not open a screen.
- Review has no extra header action beyond back. Line trash removes a line. The header total stays.
