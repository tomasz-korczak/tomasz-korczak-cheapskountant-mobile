# Consistency Checklist: Expense and Receipt Client

**Purpose**: Author review, before tasks, of whether the spec, the plan, and the API contract state the same rules with enough precision to implement
**Created**: 2026-10-15
**Feature**: [spec.md](../spec.md)

**Note**: This checklist tests the requirements, the plan, and `docs/api/openapi.yaml` as written. It does not test the app. Depth is standard. Mark an item done only when the written requirements already answer it, or after the spec or plan is updated so they do.

## Requirement Completeness

- [x] CHK001 - Are requirements defined for a day-list amount when the daily contract supplies no currency? [Gap, Spec §FR-006] — Every day-list amount uses zł.
- [x] CHK002 - Are requirements defined for a month summary whose payload contains more than one currency code? [Gap, Spec §Clarifications] — The month API returns one currency code, and the client shows that currency.
- [x] CHK003 - Are requirements defined for which receipt figure is "the total" when the contract has both an amount due and a gross amount? [Gap, Spec §FR-017] — Plan and the client binding use `totals.amountDue`. The spec's "receipt total" does not name `grossAmount`.
- [x] CHK004 - Are requirements defined for the fiscal groups that must be submitted unchanged with an edited receipt? [Completeness, Spec §FR-019] — The spec allows only a category change or a line removal. The plan submits every other receipt field unchanged.
- [x] CHK005 - Are image type and maximum size requirements defined for receipt photos, matching the transcription contract limits? [Gap, Spec §FR-011] — The contract allows JPEG, PNG, and WebP up to 10 MB each. The plan copies files into app storage and recompresses anything over 10 MB to JPEG.
- [x] CHK006 - Are requirements defined for a whitespace-only expense description, which the expense contract rejects? [Gap, Spec §FR-022] — Save is blocked with “Description cannot be only spaces” / “Opis nie może składać się z samych spacji”.
- [x] CHK007 - Are day-list ordering requirements defined? [Gap, Spec §FR-006] — The contract and the plan keep server order: manual expenses first, then receipt rows. The spec does not require the Papier picture's order.
- [x] CHK008 - Are requirements defined for a missing or rejected device API key, separate from the no-sign-in rule? [Gap, Spec §FR-029] — There is still no sign-in. A failed read shows the last saved copy. A failed write is an error.
- [x] CHK009 - Are destination requirements defined for receipt save in each combination of direct versus queued review and accepted versus unreachable server? [Completeness, Spec §FR-020, Spec §FR-021] — Accepted direct review returns to landing. Accepted queued review returns to photo selection. An unreachable save from either path returns to photo selection and parks the edit.

## Requirement Clarity

- [x] CHK010 - Is "the summary currency" defined for an empty month, when the payload contains no currency code? [Ambiguity, Spec §US1] — An empty month shows 0 zł and PLN.
- [x] CHK011 - Is "nearest whole number" given a tie-breaking rule for category percentages? [Ambiguity, Spec §Assumptions] — The plan rounds half up. That matches "nearest whole number" for a tie.
- [x] CHK012 - Is the calendar date shown for a receipt defined when the issued timestamp includes a zone offset? [Clarity, Spec §FR-017] — The contract and the plan use the calendar date written in `receipt.issuedAt`, with no zone shift. `2026-10-12T18:41:00+02:00` stays 12 October 2026.
- [x] CHK013 - Is the transcription category `Unknown` specified as outside the household category list? [Clarity, Spec §FR-019] — The category contract returns 34 household names and omits `Unknown`. Transcription sets `Unknown`. The spec blocks Save while a line's category is absent from that list.
- [x] CHK014 - Is "a send fails" distinguished from "the server is unreachable" by explicit failure classes? [Ambiguity, Spec §FR-027, Spec §US4] — No network before the attempt stays waiting. Any HTTP error, and no HTTP status, is an error until Resubmit.
- [x] CHK015 - Are amount grouping and the placement of `zł`, `€`, and `$` specified beyond the decimal mark and the symbol characters? [Clarity, Spec §FR-003, Spec §FR-006] — The symbol follows the amount, with a space. The month total also uses the language's thousands separator and two fraction digits.
- [x] CHK016 - Is it specified that the person reads the spec's send-failed sentence, including when the contract returns an English `explanation`? [Clarity, Spec §User-Facing Text] — That sentence is in User-Facing Text. The client binding logs `explanation` and shows the spec sentence.

## Requirement Consistency

- [x] CHK017 - Do the spec's per-line currency requirements, the plan's month-snapshot symbol rule, and the daily contract's omitted currency agree? [Conflict, Spec §FR-006] — Spec, plan, and contract now agree that day amounts use zł.
- [x] CHK018 - Do the spec's empty-receipt save, the contract's minimum of one item, and the plan's treatment of that rejection as a failed save agree? [Conflict, Spec §FR-019] — Save is blocked on the phone when no lines remain. The empty list is not sent.
- [x] CHK019 - Do the day-list receipt amount and the review header total refer to the same figure after lines are removed? [Conflict, Spec §FR-006, Spec §FR-017] — During review the header stays `totals.amountDue`. After acceptance the day list shows the daily contract amount. The plan states those figures can differ. The spec does not require them to stay equal after save.
- [x] CHK020 - Does the spec's failed-send outcome agree with the plan's split between HTTP 500 (still waiting) and a write that returns no HTTP status (error)? [Conflict, Spec §US4] — HTTP 500 and a missing HTTP status are both errors until Resubmit.
- [x] CHK021 - Do photo-retention requirements agree when a direct review is left without saving? [Conflict, Spec §FR-020, Spec §FR-031] — Photos stay only while the receipt is waiting, ready, or in error. Direct back discards the session, so the receipt is not parked and the plan deletes those files.
- [x] CHK022 - Do parked-work exclusion requirements agree between the spec and the plan, including against the cache note that would place the outbox on the day list? [Consistency, Spec §FR-007] — Spec and plan keep parked work off the month and day figures. The cache note does not override FR-007.
- [x] CHK023 - Do the last-saved-copy wording requirements agree between the spec and the plan? [Consistency, Spec §FR-009] — Both use "Showing the last saved copy" / "Pokazano ostatnią zapisaną kopię", with no fetch timestamp.
- [x] CHK024 - Do primary-action label requirements agree across the spec, the plan, and the Papier reference? [Consistency, Spec §Clarifications] — Spec and plan use Save / Zapisz on all three screens. The Papier "Wyślij" caption is not the requirement.
- [x] CHK025 - Do category-name requirements agree that server names are shown unchanged in both languages? [Consistency, Spec §FR-030] — Spec, plan, and the category contract agree.
- [x] CHK026 - Do the local-cache and outbox requirements stay consistent with the rule that the server is the only authority for accepted records? [Consistency, Constitution §II, Spec §FR-007] — The cache displays the last server payload. The outbox is unsent work and stays out of accepted figures.

## Acceptance Criteria Quality

- [x] CHK027 - Can the day-list currency outcome be judged objectively while the daily contract omits currency? [Measurability, Spec §US1, Spec §FR-006] — The required symbol is zł on every day line.
- [x] CHK028 - Can the empty-receipt success outcome be judged objectively against a contract that rejects an empty item list? [Measurability, Spec §US3] — Success requires at least one line. An empty list is a local refusal.
- [x] CHK029 - Are the offline success criteria specific enough to separate a waiting item from an error item? [Measurability, Spec §SC-006, Spec §SC-007] — Waiting work sends without another tap. An error stays until Resubmit or Delete. Which HTTP results land in which state is still CHK014.

## Scenario and Edge Coverage

- [x] CHK030 - Are requirements specified for one landing read failing while the other succeeds? [Coverage, Spec §FR-008, Spec §FR-009] — Month and day refresh separately. A failed period keeps its own snapshot or uses its empty state, and does not borrow the other period.
- [x] CHK031 - Are requirements specified for deleting a parked item while its send is in progress? [Gap, Spec §FR-015, Spec §FR-027] — Delete is unavailable until that attempt finishes.
- [x] CHK032 - Are requirements specified for whether the selected month and the selected day survive leaving the app? [Gap, Spec §FR-004, Spec §FR-005] — The day defaults to today. The spec does not require restoring the last selection. The plan returns to the current month and today on a cold start. Retrieved figures still remain.
- [x] CHK033 - Are requirements specified for a negative category amount in the month summary? [Gap, Spec §FR-003] — The row shows the signed amount and a signed whole-number percent, and the bar is empty.
- [x] CHK034 - Are requirements specified for a receipt or expense write that may already have been stored when no HTTP status returns, given the contract has no idempotency key? [Gap, Spec §FR-027] — A missing HTTP status is an error until Resubmit. The client does not send an idempotency key.
- [x] CHK035 - Are user-facing text requirements complete for a month total that has no currency code and for the other states the plan describes? [Completeness, Spec §User-Facing Text] — An empty month uses 0 zł, PLN, and the existing empty-month sentence. The new spaces and receipt-line sentences are in User-Facing Text.
- [x] CHK036 - Are accessibility requirements specified for the four screens, or is their absence recorded as an intentional boundary? [Gap, Spec §FR-001] — Accessibility is recorded as out of scope in the spec assumptions.
- [x] CHK037 - Are category-bar color requirements specified in the spec, or only by the plan's visual reference? [Gap, Spec §FR-003] — The spec requires a bar and a whole-number percent. Colors are the Papier palette in the plan, cycling by row.

## Notes

- All 37 items were closed on 2026-10-15. Twenty were already answered by the docs. The other seventeen were closed after the spec, plan, contract, and research were updated from the author's answers.
- `checklists/requirements.md` remains the general spec-quality list. This file is the cross-document list.
