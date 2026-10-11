<!--
Sync Impact Report
- Version change: unversioned template → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Native Android Graphical Client
  - [PRINCIPLE_2_NAME] → II. Client of the Server
  - [PRINCIPLE_3_NAME] → III. Expense and Receipt Records
- Added sections:
  - Platform Constraints
  - Compliance
- Removed sections:
  - Principle IV template slot (unused)
  - Principle V template slot (unused)
- Follow-up TODOs: none
-->

# Cheapskountant Mobile Constitution

## Core Principles

### I. Native Android Graphical Client

The product MUST be a native Android application with a graphical user
interface. Every user-facing expense and receipt task MUST be completable
on screen. The product MUST NOT be delivered as a web application,
command-line tool, or non-Android client.

**Rationale**: The project exists to give people a phone client for
expenses and receipts.

### II. Client of the Server

This project is the client in a client-server system. Stored expense and
receipt records MUST be read from and written to the server. The server
MUST remain the authority for those records. A local copy MAY exist only
to display server data and MUST NOT override the server. The client MUST
NOT take on server duties.

**Rationale**: Two authorities would let the phone and the server disagree
about the same expense or receipt.

### III. Expense and Receipt Records

The client MUST let the user handle expenses and the receipts that belong
to them. A receipt MUST stay associated with its expense. Amount, date,
and receipt content shown or submitted MUST match the corresponding server
record. The client MUST NOT drop, change, or reassign that data unless the
user explicitly does so and the server accepts the change.

**Rationale**: The client's job is to present and submit these records
faithfully.

## Platform Constraints

- The client MUST run as a native Android application.
- The user interface MUST be graphical and usable on a phone.
- This repository MUST contain only the client.
- Server implementation, administration, and authoritative storage MUST
  stay outside this project.
- The client MUST send and receive expense and receipt changes through the
  server interface.

## Compliance

A change complies with this constitution only when all of the following
hold:

- Affected expense and receipt tasks still complete through the Android
  graphical interface.
- Stored records still come from and go to the server.
- Shown and submitted expense and receipt data still match the server
  record after an explicit user action.

Work that adds a second source of truth, moves server duties into the
client, or hides an expense or receipt task outside the graphical
interface is non-compliant.

## Governance

This constitution governs the Cheapskountant Mobile client. Plans, tasks,
and implementation MUST comply with it. If another practice conflicts with
this constitution, this constitution wins.

Amendments MUST be written into this file, MUST state what changed, and
MUST update the version and last-amended date. Versioning follows semantic
versioning:

- MAJOR: A principle is removed or redefined so that existing governance
  no longer applies.
- MINOR: A principle or section is added or materially expanded.
- PATCH: Wording is clarified without changing the rule.

Compliance is reviewed when a feature is specified, planned, and
implemented. A review MUST reject work that breaks a principle above.

**Version**: 1.0.0 | **Ratified**: 2026-10-11 | **Last Amended**: 2026-10-11
