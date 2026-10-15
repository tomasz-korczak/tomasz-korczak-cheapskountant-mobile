# Feature Specification: Expense and Receipt Client

**Feature Branch**: `001-expense-receipt-client`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Native Android phone client, upright layout, for expenses and receipts. Landing screen shows a one-currency monthly category summary (month chosen from a calendar) and a daily list (day chosen from a calendar), plus Receipt photo and Register expense. Photo selection takes or picks up to 5 photos of one receipt, then save opens receipt review. Receipt review shows read-only store, date, currency, total, and lines (name, amount, discount, category) with checkbox selection, bulk category change, row delete, and save. Expense registration edits a list of description, amount, date, currency, and category. Categories come from the server. Currencies are PLN (default), EUR, and USD. No sign-in. Menu entries Configuration and About are visible and not built. Polish (default) and English. Full use needs the server. Without it, previously retrieved figures stay available, and new photos, expenses, and receipt category edits wait and are sent later, including while the phone screen is off."

## Clarifications

### Session 2026-10-13

- Q: When a month contains expenses in more than one currency, how should the month summary show totals and category percentages? → A: Assume the server returns only one currency. When the default currency is changed, the server will handle conversion. Conversion support is planned for the future.
- Q: Should each line in the day list include a currency code, or only the name and the amount? → A: Include the currency on each line, as on main screen 1: the name, a receipt or expense label, and the amount written with the currency (for example 44,87 zł).
- Q: After the server accepts a receipt, should the app delete that receipt's photos from the phone? → A: Delete the photos from the phone once the server accepts the receipt.
- Q: On the photo screen, should the button that submits the photos be labeled Send, or Save? → A: All three screens say Save (Zapisz).
- Q: If a receipt line shows a category that is not in the category list, can the person save the receipt without changing that line? → A: The person must change it to a listed category before saving.

### Session 2026-10-15

- Q: The daily list has no currency. Which symbol should each amount use? → A: Always zł.
- Q: How are currency symbols and thousands separators written? → A: The symbol follows the amount, with a space, for zł, €, and $. The month total also uses the active language's thousands separator and two fraction digits.
- Q: What if a month summary contains more than one currency? → A: The API returns one currency code for the month. The client shows that currency.
- Q: What currency is the zero total of an empty month? → A: 0 zł and PLN.
- Q: Can a receipt with no remaining lines be saved? → A: No. Save is blocked on the phone and tells the person the receipt must keep at least one line.
- Q: Which send failures stay waiting? → A: Only an attempt that cannot start because the network is unavailable. Any HTTP error, and any attempt that ends with no HTTP status, is an error until Resubmit.
- Q: What does a rejected API key look like? → A: There is still no sign-in. A failed read shows the last saved copy when one exists. A failed write is an error with the send-failed sentence.
- Q: Is a description of only spaces valid? → A: No. The form explains that the description cannot be only spaces.
- Q: Can a parked item be deleted while its send is in progress? → A: No. Delete is unavailable until that attempt finishes.
- Q: How does a negative category amount appear on the month summary? → A: The row shows the signed amount and a signed whole-number percent, and the bar is empty.
- Q: Are accessibility requirements in this feature? → A: No. They are out of scope.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Review a month and a day (Priority: P1)

A person opens the app and sees how much they spent in the current month, how that total splits across categories, and what was recorded for today. They can move to another month or another day without mixing those two choices.

**Why this priority**: Seeing spending is useful before any new expense or receipt is added.

**Independent Test**: With spending already stored for a known month and day, open the app, read the month total and category shares, then choose a different month and a different day and confirm each part changes on its own.

**Acceptance Scenarios**:

1. **Given** the server has spending for the current month in one currency, **When** the person opens the app, **Then** the landing screen shows that month and year, the month total, the currency, and one row per category that has spending, each with the category name, a bar, and a whole-number percentage.
2. **Given** the month summary is visible, **When** the person taps the month and year and chooses another month, **Then** the summary shows that month and the previously shown day stays selected.
3. **Given** today has one stored receipt and one stored manual expense, **When** the person views the day list, **Then** the receipt shows the store name, a receipt label, and the amount followed by zł, and the manual expense shows the category, an expense label, and the amount followed by zł.
4. **Given** a day is selected, **When** the person taps the date and chooses another day, **Then** the list shows that day and the selected month stays unchanged.
5. **Given** the selected month has no spending, **When** the summary is shown, **Then** the total is 0 zł, the currency code is PLN, and no category rows are shown.
6. **Given** the selected day has no records, **When** the day list is shown, **Then** the screen states that nothing is recorded for that day.
7. **Given** the person opens the app, **When** the landing screen appears, **Then** they are not asked to sign in.
8. **Given** the landing screen content is taller than the phone, **When** the person scrolls, **Then** they reach the month summary, the two actions, and the day list in that order.
9. **Given** the landing screen is visible, **When** the person opens the menu and chooses Configuration or About, **Then** they stay on the landing screen and no spending, language, or parked work changes.

---

### User Story 2 - Register expenses by hand (Priority: P2)

A person enters one or more expenses, saves them once, and returns to the landing screen after the server accepts them.

**Why this priority**: Manual entry records spending that has no receipt photo.

**Independent Test**: Open expense registration, enter two valid expenses, save while the server is reachable, and confirm the submission is accepted and the landing screen returns.

**Acceptance Scenarios**:

1. **Given** the person opens Register expense, **When** the screen appears, **Then** it starts with one empty row whose payment date is today, whose currency is PLN, and whose category is not yet chosen.
2. **Given** a row is filled with an optional description of at most 100 characters, an amount greater than zero of at most 6 digits before the decimal mark and 2 after, a payment date, PLN, EUR, or USD, and one category, **When** the person saves while the server is reachable, **Then** the whole list is accepted, and the app returns to the landing screen.
3. **Given** the form has one row, **When** the person taps add, **Then** a new empty row appears below it with today's date and PLN.
4. **Given** a row is present, **When** the person taps that row's trash control, **Then** that row disappears from the working list.
5. **Given** the person has changed the form, **When** they tap the header back arrow, **Then** the app returns to the landing screen and none of those changes are sent.
6. **Given** a row has no amount, no category, an amount of zero or less, an amount above 999999.99, more than 2 decimal digits, a description longer than 100 characters, or a description made only of spaces, **When** the person saves, **Then** the list is not sent and the screen explains what to correct.

---

### User Story 3 - Submit a receipt from photos (Priority: P3)

A person photographs one receipt, or picks existing photos of it, reviews the read-back lines, adjusts categories, removes unwanted lines, and saves.

**Why this priority**: Receipt capture is the other way spending enters the app, and it depends on a review step the summary does not need.

**Independent Test**: Select two photos of one receipt, save while the server can read them, change one line's category, delete another line, save, and confirm the app returns to the landing screen only after the update is accepted.

**Acceptance Scenarios**:

1. **Given** the person taps Receipt photo, **When** photo selection opens, **Then** they can take a photo or choose one from the gallery, and the screen states that the photos belong to one receipt.
2. **Given** fewer than 5 photos are selected, **When** the person adds one, **Then** it appears with a miniature, a name, and a size, and the count of photos out of 5 is shown.
3. **Given** a photo is in the current selection, **When** the person removes it, **Then** it leaves the selection and is not submitted.
4. **Given** 5 photos are already selected, **When** the person tries to add another, **Then** it is not added and the screen states that the limit is 5.
5. **Given** at least one photo is selected and the server can be reached, **When** the person taps Save, **Then** the photo screen shows that the receipt is being read, and review opens only after the receipt details arrive.
6. **Given** the receipt details have arrived, **When** review opens, **Then** it shows the store name, payment date, currency, receipt total, and one row per line with a checkbox, name, total amount, discount, and category.
7. **Given** receipt review is open, **When** the person looks at the header and the line text, **Then** store name, payment date, currency, receipt total, line name, line amount, discount, and category cannot be typed over.
8. **Given** a line has a discount, **When** it is shown, **Then** the discount is a negative amount. **Given** a line has no discount, **When** it is shown, **Then** the discount is blank.
9. **Given** two lines are checked and a category is chosen, **When** the person taps Change selected, **Then** only those lines show the new category.
10. **Given** a category is chosen, **When** the person taps Change all, **Then** every remaining line shows that category, whether or not it is checked.
11. **Given** no line is checked or no category is chosen, **When** the person taps Change selected, **Then** no category changes and the screen explains why. **Given** no category is chosen, **When** the person taps Change all, **Then** no category changes and the screen explains why.
12. **Given** a line is shown, **When** the person taps its trash control, **Then** that line leaves the working list and the header total stays as first shown.
13. **Given** the person opened review from a successful photo save, **When** they tap Save and the server accepts the list, **Then** the app returns to the landing screen and those photos are deleted from the phone.
14. **Given** the person has changed categories or deleted lines, **When** they tap the header back arrow from the direct photo-save path, **Then** the app returns to the landing screen and those changes are not kept.
15. **Given** no photo is selected, **When** the person taps Save, **Then** nothing is sent and the screen asks for at least one photo.
16. **Given** a remaining line shows a category that is not in the category list, **When** the person taps Save, **Then** the receipt is not sent and the screen tells them to choose a listed category for every line.
17. **Given** every remaining line has a listed category, **When** the person taps Save and the server accepts it, **Then** the receipt is saved.
18. **Given** no lines remain, **When** the person taps Save, **Then** nothing is sent and the screen says the receipt must keep at least one line.

---

### User Story 4 - Keep working when the server is unreachable (Priority: P4)

A person can still read spending retrieved earlier, and can park new expenses, receipt photos, and receipt edits until the server is reachable again. Parked work is sent even if the person leaves the app and the phone screen is off.

**Why this priority**: The app is only fully up to date while the server is reachable, but capture must not stop during a gap.

**Independent Test**: Retrieve a month while the server is reachable, then make it unreachable, confirm those figures remain, save a new expense and a receipt photo, and confirm both stay in their waiting lists and are not counted in the month total until the server accepts them.

**Acceptance Scenarios**:

1. **Given** a month and a day were retrieved earlier, **When** the person opens the app while the server is unreachable, **Then** that month and that day show the last retrieved figures and the screen states that they are the last saved copy.
2. **Given** the server is unreachable, **When** the person chooses a month or day that was never retrieved, **Then** that part shows its empty state and does not reuse another period's figures.
3. **Given** photos are selected and the server is unreachable, **When** the person taps Save, **Then** the receipt appears on the photo-selection waiting list as waiting to send, review does not open, and the month and day figures do not include it.
4. **Given** a receipt is waiting to send, **When** the server becomes reachable, including while the app is not on screen and the phone screen is off, **Then** the photos are sent without another tap.
5. **Given** a waiting receipt is read successfully, **When** the person next views the waiting list, **Then** that receipt is ready to edit and can be edited or deleted, and it is not yet part of the month or day figures.
6. **Given** the person opens a ready receipt from the waiting list, **When** they tap the header back arrow, **Then** they return to photo selection, the receipt stays on the waiting list, and the unsent review changes are dropped.
7. **Given** the person opens a ready receipt from the waiting list, **When** they save and the server accepts it, **Then** they return to photo selection, that receipt leaves the waiting list, and its photos are deleted from the phone.
8. **Given** the person confirms receipt edits while the server is unreachable, **When** the save is parked, **Then** they return to photo selection, the receipt is waiting to send, and it is still absent from the month and day figures.
9. **Given** an expense list is saved while the server is unreachable, **When** Save completes locally, **Then** that submission appears on the expense waiting list, the form is cleared, the landing screen is not opened, and the month and day figures do not include it.
10. **Given** an item is waiting to send and no send is in progress, **When** the person deletes it, **Then** it is removed and is not sent. **Given** a send is in progress, **When** the person looks for delete, **Then** delete is unavailable until that attempt finishes.
11. **Given** a send fails, **When** the person views that item, **Then** it is marked as an error and offers only delete and resubmit.
12. **Given** an item is marked as an error, **When** the person does nothing, **Then** it is not sent again until they tap Resubmit.
13. **Given** waiting items exist, **When** the person closes the app and opens it later, **Then** those items and the last retrieved figures are still there.
14. **Given** a parked receipt is waiting, ready to edit, or in error, **When** the person has not deleted it and the server has not accepted it, **Then** its photos remain on the phone.
15. **Given** the person deletes a parked receipt, **When** the deletion finishes, **Then** its photos are deleted from the phone.

---

### User Story 5 - Read the app in Polish or English (Priority: P5)

Every control is available in Polish and English. Polish is used unless the phone is set to English.

**Why this priority**: The first release has two languages, and the labels must exist before either language can be checked.

**Independent Test**: Set the phone to Polish and read the landing, photo, review, and expense screens, then set it to English and confirm the same controls use the English text.

**Acceptance Scenarios**:

1. **Given** the phone language is Polish, **When** any screen in this feature is shown, **Then** every control uses the Polish text in User-Facing Text.
2. **Given** the phone language is English, **When** any screen in this feature is shown, **Then** every control uses the English text in User-Facing Text.
3. **Given** the phone language is neither Polish nor English, **When** the app opens, **Then** the controls use the Polish text.
4. **Given** either language, **When** category names are shown, **Then** they appear exactly as supplied and are not replaced by a translation.

---

### Edge Cases

- A sixth photo is refused. Save with zero photos is refused.
- Removing every receipt line blocks Save. Nothing is sent, and the screen says the receipt must keep at least one line. The displayed receipt total stays the original total. A line whose category is not in the category list blocks Save until that line is given a listed category or removed.
- Change selected with no checked line, or either change action with no category chosen, changes nothing and explains why.
- Back from receipt review or expense registration discards unsent edits. Back from photo selection discards only the current unsent photo selection and keeps the waiting list.
- A failed save during receipt review leaves the person on that screen with the working list intact. A failed save during expense registration leaves the person on the form with the entered rows intact.
- The first launch, with nothing retrieved and the server unreachable, shows empty month and day states. Expense save is refused until a category list has been retrieved at least once. Photos can still be parked.
- Waiting photos, waiting receipt edits, and waiting expenses are excluded from month and day figures until the server accepts them.
- After the server accepts parked work, the next retrieval of that month and day includes it. Acceptance alone does not invent figures for a period that has not been retrieved.
- Rounded category percentages may not add up to exactly 100. A category with a positive amount still appears when its rounded percentage is 0. A category with a negative amount appears with that signed amount, a signed whole-number percent, and an empty bar. A category whose amount is zero does not appear. When the month total is zero and a category amount is not zero, the row has no percent.
- An amount of 999999.99 is accepted. 1000000.00, a third decimal digit, and a non-numeric amount are refused.
- Payment dates may be in the past or the future.
- Configuration and About do not open screens.
- Once the server accepts a receipt, its photos are deleted from the phone. Photos stay while that receipt is waiting, ready to edit, or in error, and they are deleted if the person deletes the parked receipt.
- If the phone blocks work while the screen is off, parked items that are still waiting to send are sent the next time the app is open and the server is reachable.
- An attempt that cannot start because the network is unavailable stays waiting to send. Any HTTP error, and any attempt that ends with no HTTP status, marks the item as an error until Resubmit or Delete.
- A failed read, including a rejected API key, shows the last saved copy when that period was retrieved. A failed write is an error. The app still does not ask the person to sign in.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST open on a landing screen laid out for an upright phone, with content stacked vertically and vertical scrolling when it does not fit.
- **FR-002**: The landing screen MUST show, from top to bottom, the month summary, the Receipt photo and Register expense actions, and the day list.
- **FR-003**: The month summary MUST show the selected month and year, the total written with the currency symbol after the amount and a space, the active language's thousands separator, two fraction digits, the currency code, and one row for each category with a non-zero amount. The month payload has one currency code, and the client MUST show that currency. The client MUST NOT convert amounts and MUST NOT show more than one currency in the summary. A positive category row MUST show the category name, a bar filled to that category's share of the total, and that share as a whole-number percentage. A negative category row MUST show the category name, the signed amount, a signed whole-number percentage, and an empty bar. An empty month MUST show 0 zł and the currency code PLN, and no category rows. When the month total is zero and a category amount is not zero, that row MUST omit the percentage.
- **FR-004**: Tapping the month and year MUST open a calendar limited to choosing a year and month, and choosing one MUST refresh only the month summary.
- **FR-005**: The day list MUST default to today. Tapping the date MUST open a calendar for a day, and choosing one MUST refresh only the day list.
- **FR-006**: The day list MUST show one entry per accepted receipt and one entry per accepted manual expense, in the order supplied for that day. A receipt entry MUST show the store name, a receipt label, and the amount followed by zł. A manual expense entry MUST show the category, an expense label, and the amount followed by zł. The daily record has no currency, so every day-list amount uses zł. On the month summary, receipt review, and expense rows, PLN amounts MUST use zł, EUR amounts MUST use €, and USD amounts MUST use $, after the amount, separated by a space, with two fraction digits.
- **FR-007**: Month and day figures MUST include only records the server has already accepted, and MUST NOT include parked photos, parked expenses, or unsent receipt edits.
- **FR-008**: While the server is reachable, the landing screen MUST refresh the selected month and the selected day from the server when it opens and when the person changes that period.
- **FR-009**: While the server is unreachable, or a read fails, including when the API key is rejected, a previously retrieved month or day MUST be shown from that retrieval, with a visible statement that it is the last saved copy. A period that was never retrieved MUST use its empty state. A failed read MUST NOT clear the other period.
- **FR-010**: Receipt photo MUST open photo selection. Register expense MUST open expense registration. The landing screen MUST offer a menu containing Configuration and About; choosing either MUST stay on the landing screen and MUST NOT change spending, language, or parked work.
- **FR-011**: Photo selection MUST let the person take a photo or choose one from the gallery, up to 5 photos, all for the same receipt, and MUST show the current count out of 5.
- **FR-012**: Each selected photo MUST show a miniature, a name, and a size, and MUST be removable before it is submitted. A camera photo MUST receive a visible name.
- **FR-013**: The photo screen, receipt review, and expense registration MUST label the primary button Save. Save on photo selection MUST require at least one photo. When the server reads the receipt, the app MUST open receipt review filled with the returned store name, payment date, currency, receipt total, and lines. When the server cannot be reached, the app MUST park the photos, stay on photo selection, and MUST NOT open review.
- **FR-014**: The photo-selection header back control MUST return to the landing screen, discard photos not yet submitted or parked, and leave parked receipts in place.
- **FR-015**: Photo selection MUST list parked receipts beneath the current selection whenever any exist. A receipt waiting to send photos or a confirmed edit MUST offer only delete. A receipt ready to edit MUST offer edit and delete. A receipt in error MUST offer only delete and resubmit. While a send attempt is in progress, delete MUST be unavailable.
- **FR-016**: Edit on a ready receipt MUST open receipt review. Leaving that review MUST return to photo selection. A save the server accepts MUST remove that receipt from the parked list and MUST delete its photos from the phone.
- **FR-017**: Receipt review MUST show a read-only header with store name, payment date, currency, and receipt total, and a line list with checkbox, name, total amount, discount, category, and trash. Name, amount, discount, and category MUST NOT be directly editable. A discount MUST be shown as a negative amount; a missing discount MUST be blank.
- **FR-018**: Change selected MUST apply the chosen category only to checked lines. Change all MUST apply it to every remaining line. Either action without a chosen category, and Change selected with no checked line, MUST leave lines unchanged and explain why.
- **FR-019**: Trash on a receipt line MUST remove that line from the working list immediately and MUST NOT change the displayed header total. Save MUST be refused while any remaining line has a category that is not in the category list, and the screen MUST tell the person to choose a listed category for every line. Save MUST be refused when no lines remain, nothing is sent, and the screen MUST say the receipt must keep at least one line. `Unknown` is not in the household category list. Save MUST send the remaining lines when every remaining line has a listed category.
- **FR-020**: Save on receipt review, when the server accepts it, MUST delete that receipt's photos from the phone and MUST return to the landing screen if review was opened directly after a photo save, or to photo selection if review was opened from the parked list. The header back control MUST discard unsent review changes and return to that same destination, and MUST keep the photos while the receipt is still parked.
- **FR-021**: If receipt review is saved while the server cannot be reached, the app MUST park the confirmed edits, return to photo selection, show the receipt as waiting to send, and keep it out of the month and day figures until the server accepts it.
- **FR-022**: Expense registration MUST edit a list of rows. Each row MUST have an optional description of at most 100 characters, a required amount greater than zero and at most 999999.99 with at most 2 decimal digits, a payment date defaulting to today, a currency defaulting to PLN, one required category, and trash. A description made only of spaces MUST be refused with the spaces sentence and MUST NOT be sent. Add MUST append an empty row with today's date and PLN.
- **FR-023**: The expense currency choices MUST be PLN, EUR, and USD. Category choices MUST be the list last supplied by the server. The person MUST choose a category; none is pre-selected.
- **FR-024**: Expense Save MUST send the whole current list when every row is valid and at least one row exists. On acceptance, the app MUST return to the landing screen. Invalid or empty lists MUST NOT be sent.
- **FR-025**: If expense Save cannot reach the server, the app MUST park that whole list, clear the form to one new empty row, stay on expense registration, and show the parked submission. The header back control MUST discard only the current unsent form and MUST NOT remove parked submissions.
- **FR-026**: The expense waiting list MUST show each parked submission's categories, amounts, and currencies. A submission waiting to send MUST offer only delete. A submission in error MUST offer only delete and resubmit. While a send attempt is in progress, delete MUST be unavailable.
- **FR-027**: An item that cannot be sent because the network is unavailable MUST stay waiting to send and MUST be sent when the server becomes reachable, including while the person is not using the app and the phone screen is off. An attempt that receives any HTTP error, or that ends with no HTTP status, MUST mark the item as an error. Items in error MUST stay in error until the person taps Resubmit or Delete. A write rejected for the API key is an error. The app MUST NOT ask the person to sign in.
- **FR-028**: Parked receipts, parked expenses, and the last retrieved month, day, and category list MUST remain available after the app is closed.
- **FR-029**: The app MUST NOT ask the person to sign in.
- **FR-030**: Every control in User-Facing Text MUST be shown in Polish when the phone language is Polish or is neither Polish nor English, and in English when the phone language is English. Category names MUST be shown as supplied.
- **FR-031**: Photos for a receipt MUST remain on the phone while that receipt is waiting to send, ready to edit, or in error. The app MUST delete those photos when the server accepts the receipt or when the person deletes the parked receipt.

### Key Entities *(include if feature involves data)*

- **Month summary**: The accepted spending for one calendar month in the single currency supplied by the server: a total and each spending category's share of that total. The client does not convert amounts.
- **Day entry**: One accepted receipt or one accepted manual expense for one calendar day. A receipt shows the store name, a receipt label, and the amount followed by zł. A manual expense shows the category, an expense label, and the amount followed by zł.
- **Manual expense**: A description, amount, payment date, currency, and category entered by the person.
- **Receipt**: One store purchase read from 1 to 5 photos, with store name, payment date, currency, total, and lines.
- **Receipt line**: A purchased item with name, total amount, optional discount, and category. The person may change only the category, or remove the line, before the receipt is accepted.
- **Category**: A spending label supplied by the server and chosen for a manual expense or a receipt line.
- **Parked submission**: Photos waiting to be read, a confirmed receipt edit waiting to be accepted, or a manual expense list waiting to be accepted. It is not part of month or day figures until accepted. Receipt photos are deleted from the phone when the server accepts the receipt or the person deletes the parked receipt.

### User-Facing Text

Currency codes PLN, EUR, and USD stay unchanged in both languages. Dates use the active language's month and day names. Amounts use the active language's decimal mark.

| Control | English | Polish |
| --- | --- | --- |
| Menu | Menu | Menu |
| Configuration | Configuration | Konfiguracja |
| About | About | O aplikacji |
| Month spending label | Expenses | Wydatki |
| Receipt photo action and photo screen title | Receipt photo | Zdjęcie paragonu |
| Register expense action and expense screen title | Register expense | Wpisz wydatek |
| Receipt review title | Receipt | Paragon |
| Day receipt label | Receipt | Paragon |
| Day expense label | Expense | Wydatek |
| Take photo | Take photo | Zrób zdjęcie |
| Choose from gallery | Choose from gallery | Wybierz z galerii |
| Photo count | {n} of 5 photos of this receipt | {n} z 5 zdjęć tego paragonu |
| Same receipt note | These photos are of one receipt | Te zdjęcia dotyczą jednego paragonu |
| Remove photo | Remove photo | Usuń zdjęcie |
| Save | Save | Zapisz |
| Back | Back | Wróć |
| Select line | Select | Zaznacz |
| Name | Name | Nazwa |
| Amount | Amount | Kwota |
| Discount | Discount | Rabat |
| Category | Category | Kategoria |
| Total | Total | Suma |
| Remove line | Remove line | Usuń pozycję |
| Change selected | Change selected | Zmień zaznaczone |
| Change all | Change all | Zmień wszystkie |
| Description | Description | Opis |
| Payment date | Payment date | Data płatności |
| Currency | Currency | Waluta |
| Add row | Add row | Dodaj wiersz |
| Waiting receipts | Waiting receipts | Oczekujące paragony |
| Waiting expenses | Waiting expenses | Oczekujące wydatki |
| Waiting to send | Waiting to send | Oczekuje na wysłanie |
| Ready to edit | Ready to edit | Gotowy do edycji |
| Error | Error | Błąd |
| Edit | Edit | Edytuj |
| Delete | Delete | Usuń |
| Resubmit | Resubmit | Wyślij ponownie |
| Last saved copy | Showing the last saved copy | Pokazano ostatnią zapisaną kopię |
| Empty month | No expenses this month | Brak wydatków w tym miesiącu |
| Empty day | Nothing recorded for this day | Brak wpisów tego dnia |
| No photos | Add at least one photo of the receipt | Dodaj co najmniej jedno zdjęcie paragonu |
| Photo limit | A receipt can include at most 5 photos | Paragon może mieć co najwyżej 5 zdjęć |
| No category chosen | Choose a category | Wybierz kategorię |
| Unlisted line category | Choose a listed category for every line | Wybierz kategorię z listy dla każdej pozycji |
| No line selected | Select at least one line | Zaznacz co najmniej jedną pozycję |
| Categories unavailable | Categories are unavailable until they can be retrieved | Kategorie będą dostępne po ich pobraniu |
| Amount missing | Enter an amount greater than zero | Podaj kwotę większą od zera |
| Amount rejected | Use at most 6 digits before the decimal mark and 2 after | Użyj co najwyżej 6 cyfr przed znakiem dziesiętnym i 2 po nim |
| Description rejected | Description can have at most 100 characters | Opis może mieć co najwyżej 100 znaków |
| Description spaces | Description cannot be only spaces | Opis nie może składać się z samych spacji |
| Receipt needs a line | A receipt must keep at least one line | Paragon musi zachować co najmniej jedną pozycję |
| Expense list empty | Add at least one complete expense | Dodaj co najmniej jeden kompletny wydatek |
| Send failed | Could not send. You can resubmit or delete this item. | Nie udało się wysłać. Możesz wysłać ponownie albo usunąć. |
| Reading receipt | Reading the receipt | Odczytywanie paragonu |

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A person who already has a retrieved current month can read its total, currency, and category shares within 5 seconds of opening the app.
- **SC-002**: Changing the month never changes the selected day, and changing the day never changes the selected month, in 100% of switches.
- **SC-003**: A person can enter two valid expenses and return to the landing screen in under 2 minutes while the server accepts the save.
- **SC-004**: Once receipt lines are on screen, a person can change categories, remove a line, and save in under 1 minute while the server accepts the save.
- **SC-005**: 100% of attempts to add a sixth photo, save with no photo, or save an invalid expense row are blocked and explained.
- **SC-006**: While the server is unreachable, 100% of previously retrieved month and day figures remain visible, and 100% of newly parked expenses and receipts stay out of those figures until accepted.
- **SC-007**: A parked item that is waiting to send leaves that state without a further send tap once the server is reachable, including when the app is not on screen, or on the next app opening if the phone blocked work while the screen was off.
- **SC-008**: Leaving receipt review or expense registration with the header back control leaves 100% of those unsent edits unsent.
- **SC-009**: Every control in User-Facing Text has both a Polish and an English label, and the language matches the phone rule in FR-030 on every screen of this feature.

## Assumptions

- This is the native Android phone client. It shows and submits expenses and receipts; the server remains the authority for accepted records.
- The month summary API returns one currency code. The client shows that currency, does not convert amounts, and does not offer a way to change the default currency. Changing that default, and converting existing amounts, is future work done by the server. An empty month shows 0 zł and PLN.
- Day-list amounts use zł because that payload has no currency. Elsewhere, PLN amounts use zł, EUR amounts use €, and USD amounts use $. The symbol follows the amount, with a space. The month total also uses the active language's thousands separator and shows the currency code.
- The displayed month percentage is that category's share of the month total, rounded to the nearest whole number. The bar uses the unrounded share.
- Receipt header values stay as first shown during review, including after lines are removed.
- Expense registration opens with one empty row so entry can start immediately.
- A camera photo's visible name is generated and does not have to match a gallery file name.
- The photo screen uses Save, the same label as receipt review and expense registration.
- Polish and English follow the phone language, with Polish for every other phone language, because Configuration is not available to hold a language choice.
- Category names are supplied ready to display.
- Configuration and About are labels only in this feature. Sign-in, accounts, editing or deleting an already accepted landing-screen entry, splitting photos across receipts, any third language, changing the default currency, converting amounts, and accessibility requirements are out of scope.
- Parked work is kept on the phone until it is deleted or the server accepts it. Accepted receipt photos are then deleted from the phone. If the phone prevents sending while the screen is off, sending resumes the next time the app is used and the server is reachable.
