# TP Subscription view: fetch, validate and display a customer's ESPI usage data (minimal #89)

Date: 2026-11-20 · Issue: #89 (minimal slice) · Roadmap: #255, Phase 1 item 6

## Intent

The last Phase 1 exit criterion (#255): after a customer authorizes the Third Party (TP), the TP receives a
token **and displays that customer's usage data**. This proves the AS → DC → TP flow end to end when testing
by hand.

The roadmap originally said "display a UsagePoint and its readings". That was a development request to check
the output's formatting. Every usage resource is now fully marshalled by the Data Custodian (DC) and present in
the `Batch/Subscription` response, so showing only the UsagePoint is not enough: the TP must show **every
resource in the Subscription** and how they fit together. That checks that each resource the DC marshals
arrives, is read back by the TP, and links correctly.

## Goals

1. A signed-in TP customer can open a page for one of their authorizations and see the whole Subscription feed
   the DC returns for it.
2. The TP fetches the feed with the stored token (refreshing it when needed), from the stored `resourceURI`
   (`{dc}/espi/1_1/resource/Batch/Subscription/{subscriptionId}`).
3. The fetched XML is checked with `EspiXmlSchemaValidator`, in a configurable mode: `off`, `report` or
   `enforce`.
4. On development profiles only, the fetched XML is saved to disk for inspection.
5. The feed is read into the existing openespi-common DTOs and reassembled into a tree using the Atom links,
   and linking problems are reported.

6. **Every fetched feed is saved in the TP database, on every profile** (owner decision): one row per fetch
   with the XML as received. The page shows the saved feed. See "Saved feeds" below.

## Non-goals (deferred)

- **Importing into the TP's usage tables** (`usage_points`, `meter_readings`, `interval_blocks`, ...). Deferred
  to full #89 (Phase 3). Reasons:
  - the DC's `atom:id`s are not yet guaranteed permanent and repeatable (#281), so an update-by-id import could
    duplicate rows;
  - those tables come from openespi-common's Flyway scripts written for the DC; their ownership (TP customer
    and Authorization) and cleanup on revocation (#138) are a design of their own.
  The saved feeds and the tree this design builds are the input that later import walks.
- Wiring the notification receiver (`NotificationController.importResource`, `TODO(#89)`). It can call the
  same fetch and read later.
- Customer (PII) resources at `customerResourceURI`, the client_credentials Bulk URIs (#202, #256).
- The other #89 items: time-type adapters (none are needed on this feed path) and round-trip tests.
- Removing the legacy TP reader classes (`UsagePointRESTRepositoryImpl` and the controllers and services
  behind it). They are not used by the new code; deleting them is separate cleanup.

## Anti-goals (a failure even if it works)

- Showing data from another customer's authorization. The page must only open authorizations owned by the
  signed-in TP user.
- Writing an access or refresh token to a log, a capture file or the page (beyond the token prefix the home
  page already shows).
- Capturing XML on `test`, `prod` or `docker` profiles.
- An unhandled 500 when the DC refuses the request, the token is gone, or the XML is invalid: each of those is
  a message on the page.

## Constraints

- TP depends on openespi-common (DTOs, mappers, `EspiScope`, `HistoryPeriod`, `EspiXmlSchemaValidator`), never
  on openespi-datacustodian.
- `RestClient` (not `RestTemplate`; CLAUDE.md asks for migration away from it).
- Do not copy the legacy TP patterns (`Long` ids, field `@Autowired`, views without templates) (CLAUDE.md).
- AssertJ chained assertions in tests (CLAUDE.md).
- The DC still writes ServiceCategory as Java constant names (#278), so a real feed currently has XSD errors.
  The default mode must therefore be `report`, or `enforce` would reject every feed until #278 is fixed.
- `Batch/Subscription` with no range parameters returns only the last 24 hours of IntervalBlocks. The local
  sample data (#253) is two days in January 2026, so the TP must send a range.
- The DC caps every feed at now − HistoryLength. On `local` both TP registrations ask for
  `HistoryLength=34128000` (395 days), so the January 2026 sample data is returned until about
  2027-01-31; after that the manual check shows UsagePoints and MeterReadings with no IntervalBlocks or
  summaries. Dating the sample data relative to startup is a DC change outside this design: a follow-up issue.
  *(Builder's call, settled in review.)*

## Approaches

- **A. New, small components in the TP (chosen).** A fetch client, a validator step, a dev-only capture, a feed
  reader that links entries into a tree, a controller and a template. The tree is what persistence needs later,
  and it is the natural place to report linking problems.
- **B. Extend the legacy TP reader** (`UsagePointRESTRepositoryImpl` and its repository/service/controller
  chain) to keep every entry type. Less new code, but it builds on classes CLAUDE.md says to port or delete
  (`Long` ids, a placeholder `MeterReadingController`, views with no templates), tying the import to code
  slated for removal. Rejected.

The owner picked A.

How the data is saved ("data should always be saved"):

- **A. Save each fetched feed (XML as received) in a TP table, on every profile (chosen).** Access-controlled,
  deletable with the authorization, independent of stable ids.
- **B. Import into the usage tables now.** The full #89 import; blocked on #281 (ids) and on an ownership and
  revocation design for those tables. Deferred.
- **C. Write the XML file to disk on every profile.** Unprotected copies of customer data in production.
  Rejected; the disk capture stays development-only, as a debugging aid.

The owner picked A.

## Design

### Package

`org.greenbuttonalliance.espi.thirdparty.subscription` (new). Class names below are proposals.

### Flow

```
GET  /subscription/{authorizationId}[?fetch={fetchId}]
  → show the latest saved fetch (or the one named); if there is none yet, fetch first (as POST below)

POST /subscription/{authorizationId}/fetch   (from=YYYY-MM-DD, to=YYYY-MM-DD, optional; CSRF-protected)
  → SubscriptionController
      1. load the AuthorizationEntity by id; 404 unless it belongs to the signed-in user
         (retailCustomer.username = principal name) and has a clientRegistrationId and a resourceURI
      2. SubscriptionClient.fetch(authorization, principal, range)  → bytes + HTTP status
      3. capture (dev profiles only)
      4. validate per mode                                         → validation result
      5. save the fetch (every profile)                            → fetch id
  → redirect to GET /subscription/{authorizationId}?fetch={fetchId}

Displaying a saved fetch: SubscriptionFeedReader.read(saved XML) → SubscriptionTree + linking problems
  → templates/subscription.html
```

### 1. Page entry and ownership

- The home page's **Your authorizations** table gets a **View data** link per row that has a `resourceURI`
  (`AuthorizationView` gains the authorization id).
- The controller loads the authorization by its id and rejects (404) one that is not the signed-in user's or
  that has no `resourceURI`.
- A revoked authorization of the signed-in user is not a 404: the page shows "Authorize again" with the
  authorize button. This matters because a failed refresh revokes the row (Spring calls
  `removeAuthorizedClient` → `AuthorizationEntity.revoke()`, which keeps `resourceURI`). On the home page, an
  active row gets **View data**; a revoked row gets **Authorize again**. *(Builder's call, settled in review.)*

### 2. Fetch (`SubscriptionClient`)

- Token: `OAuth2AuthorizedClientManager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId(regId)
  .principal(authentication))`, so an expired access token is refreshed. If it returns no client (revoked, no
  token, refresh failed), the page says "Authorize again" with a link to the authorize button.
- Order: load the row (ownership), `authorize()` (which may refresh), then **re-load the row** and take
  `resourceURI` from it. Until #202 is fixed a refresh re-provisions the Subscription and
  `saveAuthorizedClient` overwrites `resourceURI`, so the URI read before `authorize()` can be stale.
  *(Builder's call, settled in review; tested.)*
- Request: `GET {resourceURI}?published-min=…&published-max=…` with `Authorization: Bearer …`,
  `Accept: application/atom+xml`, using a `RestClient` built from the existing `RestClient.Builder` bean.
- Range: timestamps in UTC ending in `Z` (the DC rejects anything else with 400).
  - Default `published-min` = now − the authorization's HistoryLength (parsed from its stored scope with
    `EspiScope`); if the scope has no HistoryLength, `1970-01-01T00:00:00Z`. Default `published-max` = now.
    The DC applies HistoryLength as a ceiling anyway, so the default asks for everything the grant allows.
  - The page has a from/to date form to narrow it (dates, interpreted as UTC midnight; `to` inclusive).
- Responses: keep the status and body. 401/403/404/5xx and connection failures become a page message with the
  status and a short body excerpt (no token). Only a 200 goes on to validation and reading.

### 3. Saved feeds (every profile)

Why the database and not files: the feed is a customer's energy data. In the database its access is checked
against the owning customer, and it can be deleted with the authorization; files on the server's disk have
neither. Saving the XML as received does not depend on stable `atom:id`s (#281), so it is safe now.

- New table `subscription_feed_fetches` (name is the builder's call), one row per fetch:
  - `id` (UUID), `authorization_id` (FK to `authorizations`, `ON DELETE CASCADE`);
  - `fetched_at` (UTC), `resource_uri` used, `published_min` / `published_max` sent;
  - `http_status`; the response body (the feed, or the error body for a non-200), as a large text/CLOB column;
  - `validation_mode`, `valid`, `error_count` (the issue list is recomputed from the saved XML when shown).
- Saved for every fetch attempt that reached the DC, including non-200 responses (so a failure is
  reviewable). A fetch that never reached the DC (no authorized client, connection refused) is not saved; it is
  a page message.
- Every row is kept; the page lists the authorization's fetches (newest first) so an earlier one can be
  opened. A retention limit is a follow-up. Deleting fetches on consent withdrawal belongs to #138; a token
  revocation after a failed refresh does **not** delete them.
- Read access only through the owning authorization's ownership check.
- **Flyway:** the TP's own migration, in a TP-only location (e.g. `db/thirdparty/<vendor>/`), at version
  **V7**, the next free version in the shared sequence, following the precedent of openespi-handoff's V4, which
  only the DC applies. Vendor variants for H2, MySQL and PostgreSQL (large text type differs). A schema test
  on all three, like the existing `*ProfileSchemaTest`s. *(Builder's call, settled when writing the doc.)*
- The entity and repository live in the TP (not openespi-common): the DC must not get this table.

### 3a. Capture (dev profiles only)

This is separate from saving: a debugging copy on disk, as the DC's `EspiXmlCaptureFilter` writes, so a
developer can open or diff exactly what was received. It is off in production because the files are
unprotected copies of customer data with no retention or revocation cleanup.


- A `@Profile({"dev-mysql", "dev-postgresql", "local", "dev"})` bean, following the DC's `EspiXmlCaptureFilter`:
  property `espi.dev.xml-capture.enabled` (default true) and `espi.dev.xml-capture.dir` (default
  `dev-xml-capture`, resolved against the repository root as the DC does).
- Writes `dev-xml-capture/thirdparty-run-<timestamp>/GET_Batch_Subscription_<subscriptionId>_<n>.xml`, and an
  `.error.txt` (status and body) for a non-200.
- Never writes request headers.

### 4. Validation

- Property `espi.thirdparty.xml-validation`: `off` | `report` | `enforce`. Default `report` in `application.yml`.
- What the modes are for: `report` shows schema errors without blocking (needed while #278 makes every real
  feed fail); `enforce` rejects a non-conforming feed, for certification-style testing once feeds pass; `off`
  skips the check.
- The validation result is saved with the fetch; in `enforce` an invalid feed is still saved (so it can be
  reviewed) but is not read or displayed.
- `off`: skip. `report`: validate with `EspiXmlSchemaValidator.instance().validate(byte[])`, show the issues
  (severity, line, column, message) above the data, and read the feed anyway. `enforce`: if not valid, show the
  issues and do not read or display the data.
- Log the issue count at INFO and each issue at DEBUG.

### 5. Read and link (`SubscriptionFeedReader`)

- Unmarshal with a JAXB context of `AtomFeedDto` + `UsageAtomEntryDto` (as the legacy reader does today);
  `entry.getResource()` gives the DTO. An unmarshalling failure is a page message with the parser's error.
- Index entries by their `self` href.
- Link children to parents through the `related` links the DC writes (DataSDK `REQ21_atom-link-matrix.md`;
  `up` is the root collection since #290 and does not identify the parent):
  - MeterReading → its UsagePoint and its ReadingType;
  - IntervalBlock → its MeterReading;
  - UsageSummary and ElectricPowerQualitySummary → their UsagePoint;
  - UsagePoint → its LocalTimeParameters.
  Links whose `type` is `espi-feed/...` (XPath collections) are not followed.
- **Linking problems** are reported, not fatal:
  - a child whose parent link is missing or points to an entry not in the feed;
  - an entry whose resource type the reader does not know;
  - a forward `espi-entry` link to a resource not in the feed is *not* a problem (children outside the range
    are expected).
- Result: an immutable `SubscriptionTree` (records):
  `UsagePoint` nodes, each with its LocalTimeParameters, MeterReadings (each with its ReadingType and
  IntervalBlocks in interval order), UsageSummaries and ElectricPowerQualitySummaries; plus the feed's id,
  title, updated time, entry count and the linking problems. Each node keeps its `atom:id`, `self` href and
  `published`/`updated`.

### 6. Page (`templates/subscription.html`)

Uses `fragments/layout`. Top to bottom:

1. Authorization: scope, HistoryLength in calendar units (`HistoryPeriod`), the `resourceURI`; for the fetch
   shown: the range used, fetch time, HTTP status (and the error body for a non-200). A **Fetch now** form
   (from/to dates) and the list of earlier fetches (newest first, each a link).
2. Validation: mode and a list of issues, or "valid".
3. Linking problems, if any.
4. Per UsagePoint: description, ServiceCategory, status, LocalTimeParameters (offset, DST rules).
   - Per MeterReading: description, ReadingType (commodity, kind, uom, powerOfTenMultiplier, intervalLength,
     accumulationBehaviour, flowDirection, currency).
     - Per IntervalBlock (collapsible `<details>`): interval start (in the UsagePoint's local time and in UTC)
       and duration, reading count, and a table of readings: start, duration, raw value, value scaled by
       `powerOfTenMultiplier` with its unit, cost.
   - UsageSummaries: billing period, overall consumption, bill amounts and currency.
   - ElectricPowerQualitySummaries: summary interval and the measured values present.
5. Feed facts: id, updated, entry count.

Customer-facing, so plain labels; the raw numeric codes are shown next to their names where the enum is known.

### 7. Scopes: every requested summary under both registrations (owner decision)

Both customer registrations, Monthly and Daily, request FB_17 (ElectricPowerQualitySummary) in addition to
FB_15 (UsageSummary), so every summary the DC marshals is in the Subscription under both frequency periods:
`FB=4_5_15_17` (with each registration's existing IntervalDuration, BlockDuration and HistoryLength). The DC
includes ElectricPowerQualitySummaries only for a token with FB_17
(`UsageFunctionBlockAuthorities.POWER_QUALITY`). Change it everywhere the two scopes are defined or allowed:

- TP: `application.yml` and `application-local.yml` (`datacustodian-monthly`, `datacustodian-daily`).
- AS: the `third_party` client's allowed scopes (`AuthorizationServerConfig`) and any seed, sample or doc that
  lists them.
- DC: the sample ApplicationInformation scopes (`LocalSampleDataSeedRunner`).
- `docs/LOCAL_TESTING.md` and any smoke script or contract that lists the scopes.

The builder confirms that the AS registration-time scope allowlist and the DC Authorization Screen accept
FB_17 for this client, and that a database already seeded with the old scope (PostgreSQL AS seeds) is handled
the same way #284 handled its scope change, or noted as a known limitation.

### 8. Configuration and docs

- `application.yml`: `espi.thirdparty.xml-validation: report`.
- `docs/LOCAL_TESTING.md`: a step 5 in "Customer flow" (**View data**), the TP capture folder under "Captured
  ESPI XML", and the "Known limitations" bullet for #197/#89 updated (no saving yet).
- #255 roadmap after merge.

### 9. Tests

- `SubscriptionFeedReaderTest`: a fixture feed in TP test resources (captured from a local DC run of the
  sample data) → tree shape and values; a child with a missing parent; an unknown entry type; malformed XML.
- Validation mode tests: `off` skips, `report` reads anyway, `enforce` stops on errors (use the fixture, which
  has the #278 errors, plus a valid variant).
- `SubscriptionClientTest`: query parameters and their `Z` format, the default range from HistoryLength and
  without it, Bearer header, error statuses, no authorized client.
- `SubscriptionControllerTest` (MockMvc): owner sees the page; another user's authorization is 404; no
  `resourceURI` is 404; revoked shows "Authorize again"; the `resourceURI` is re-read after `authorize()`;
  messages for DC errors.
- Saved feeds: repository test (save, latest per authorization, list newest first, cascade on authorization
  delete); the Flyway V7 migration in the schema tests on H2, MySQL and PostgreSQL; a non-200 is saved, a
  fetch that never reached the DC is not; another user cannot open a fetch id that is not theirs.
- Capture test with a temp directory; not active on `test`.
- Manual: the full local customer flow, once with Monthly and once with Daily, ends with the sample data on
  the page (2 IntervalBlocks of 24 readings, one UsageSummary, one ElectricPowerQualitySummary,
  LocalTimeParameters).

## Left to the builder

- Class and method names, and whether reader and tree live in one or two classes.
- Exact page styling within the existing layout.
- How enum codes are turned into names (existing openespi-common enums where they exist; otherwise the code).
