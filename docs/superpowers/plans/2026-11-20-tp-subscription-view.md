# TP Subscription View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A signed-in Third Party customer fetches their `Batch/Subscription` feed from the Data Custodian, the TP validates and saves it, and a page shows every resource in it, linked into a tree.

**Architecture:** New package `org.greenbuttonalliance.espi.thirdparty.subscription` in openespi-thirdparty: an HTTP client, a schema-validation step, a dev-only disk capture, a TP-owned table of saved fetches (Flyway V7), a feed reader that links entries by their `related` links, a fetch service that orchestrates them, and a controller with a Thymeleaf page. Scopes gain FB_17 in the TP, AS and DC so the feed carries ElectricPowerQualitySummaries.

**Tech Stack:** Java 25, Spring Boot 4.0.1, Spring Security 7 `oauth2Client`, `RestClient`, JAXB (openespi-common DTOs), Spring Data JPA / Hibernate 7, Flyway, Thymeleaf, JUnit 5 + AssertJ + Mockito, Testcontainers (MySQL/PostgreSQL schema tests).

**Spec:** `docs/superpowers/specs/2026-11-20-tp-subscription-view-design.md`

## Global Constraints

- TP depends on openespi-common, never on openespi-datacustodian; the new entity and repository live in the TP, not in openespi-common (the DC must not get the table).
- HTTP with `RestClient` built from the existing `RestClient.Builder` bean (`SecurityConfiguration` ~L271); never `RestTemplate`.
- Constructor injection only; no field `@Autowired`; do not copy the legacy TP classes (`UsagePointRESTRepositoryImpl`, `MeterReadingController`, ...).
- Never log, capture, save or render an access/refresh token (the home page's existing 8-character prefix stays as is).
- Range query parameters are UTC timestamps ending in `Z` (`2026-01-01T00:00:00Z`); the DC rejects anything else with 400.
- `espi.thirdparty.xml-validation` values: `off`, `report`, `enforce`; default `report` in `application.yml`.
- Disk capture only under `@Profile({"dev-mysql", "dev-postgresql", "local", "dev"})`; properties `espi.dev.xml-capture.enabled` (default `true`) and `espi.dev.xml-capture.dir` (default `dev-xml-capture`, resolved against the repository root).
- Customer registrations' scopes: Monthly `FB=4_5_15_17;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000`; Daily `FB=4_5_15_17;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000` (amended during Task 1: Daily now asks for the scope the AS registers for it, instead of its earlier daily blocks and 7-day history).
- AssertJ chained assertions: one chain per object under test (CLAUDE.md).
- Use the Edit tool for source changes, never sed/awk (CLAUDE.md).
- Build/test: `mvn install -DskipTests -pl openespi-thirdparty -am` once, then `mvn test -pl openespi-thirdparty -Dtest=<Class>`. Testcontainers tests (MySQL/PostgreSQL schema tests) run in WSL with Docker (Java 25 at `$HOME/.sdkman/candidates/java/25-tem`).
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Never stage `.playwright-mcp/`.

## Review Focus

1. **A 200 response that is not an Atom feed** (an HTML error page from a proxy, an empty body) — the fetch is saved; the page shows "The feed could not be read: …", never a 500. Test in Task 7 (`showsReadErrorForNonFeedBody`).
2. **A fetch id that belongs to a different authorization** (the user's own other authorization, or another user's) — 404, never that fetch's data. Test in Task 7 (`fetchOfAnotherAuthorizationIs404`).
3. **A date range with `from` after `to`, or an unparseable date** — no fetch is made; the page shows "The start date must be on or before the end date." Test in Task 6 (`rejectsFromAfterTo`).
4. **An IntervalBlock whose MeterReading has no ReadingType in the feed** — readings show the raw value with "(no ReadingType)" instead of a scaled value; no exception. Test in Task 5 (`meterReadingWithoutReadingType`).
5. **A feed with no UsagePoints** (an empty range, or FBs without a commodity) — the page shows "The Data Custodian returned no UsagePoints for this range." with the feed facts. Test in Task 5 (`emptyFeedHasNoUsagePoints`) and Task 7 (`showsEmptyFeedMessage`).

---

### Task 1: FB_17 in both customer registrations

**Files:**
- Modify: `openespi-thirdparty/src/main/resources/application.yml:109,119`
- Modify: `openespi-thirdparty/src/main/resources/application-local.yml:80,82`
- Modify: `openespi-authserver/src/main/java/org/greenbuttonalliance/espi/authserver/config/AuthorizationServerConfig.java:267-269` (`ESPI_THIRD_PARTY_SCOPES`)
- Modify: `openespi-datacustodian/src/main/java/org/greenbuttonalliance/espi/datacustodian/sample/LocalSampleDataSeedRunner.java:309-311`
- Modify: `docs/LOCAL_TESTING.md:42` (and the sample-data table if it lists scopes)
- Test: `openespi-authserver/src/test/java/org/greenbuttonalliance/espi/authserver/AuthorizationServerApplicationTests.java:164-165`
- Test: `openespi-thirdparty/src/test/java/org/greenbuttonalliance/espi/thirdparty/web/DataCustodianAuthorizationTest.java:94` (`MONTHLY_SCOPE`)
- Test: any DC test asserting the seed's ApplicationInformation scopes (find with `git grep -n "HistoryLength=34128000" openespi-datacustodian/src/test`)

**Interfaces:**
- Produces: the two scope strings above, used by Task 8's manual check.

- [ ] **Step 1: Update the tests to the new scopes.** In `AuthorizationServerApplicationTests` the contained scopes become `"FB=4_5_15_17;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000"` and `"FB=4_5_15_17;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000"`; `DataCustodianAuthorizationTest.MONTHLY_SCOPE` becomes the Monthly string; any DC seed test likewise.
- [ ] **Step 2: Run them; expect FAIL** on the scope assertions.
  Run: `mvn test -pl openespi-authserver -Dtest=AuthorizationServerApplicationTests` and `mvn test -pl openespi-thirdparty -Dtest=DataCustodianAuthorizationTest`
- [ ] **Step 3: Change the scopes** in the five main files listed. Only the `third_party` customer scopes; leave generic AS documentation examples, `FB=34_35` and the PostgreSQL/MySQL AS seed scripts alone (applied migrations; record the latter in Task 8's known limitations).
- [ ] **Step 4: Check the DC Authorization Screen and the AS scope policy accept FB_17.** Add to `openespi-datacustodian/.../web/authorize/AuthorizeScreenServiceTest` a case `requestedFb17IsOfferedAndApproved`: requested `FB=4_5_15_17;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000` with the client's registered scopes containing it → the screen lists FB_17 and approving everything returns that scope unchanged. Add to `openespi-authserver/.../grant/EspiGrantScopePolicyTest` a case `acceptsFb17CustomerScope` (the same string is allowed for `third_party`). If either fails, fix the policy/allowlist in the class under test.
- [ ] **Step 5: Run the module suites; expect PASS.**
  Run: `mvn test -pl openespi-authserver -Dtest='AuthorizationServerApplicationTests,EspiGrantScopePolicyTest'`, `mvn test -pl openespi-datacustodian -Dtest='AuthorizeScreen*'`, `mvn test -pl openespi-thirdparty -Dtest=DataCustodianAuthorizationTest`
- [ ] **Step 6: Commit** `feat: FB_17 (ElectricPowerQualitySummary) in the TP's Monthly and Daily scopes (#89)`.

### Task 2: Validation mode

**Files:**
- Create: `.../thirdparty/subscription/XmlValidationMode.java`
- Create: `.../thirdparty/subscription/FeedValidation.java`
- Create: `.../thirdparty/subscription/SubscriptionFeedValidator.java`
- Modify: `openespi-thirdparty/src/main/resources/application.yml` (`espi.thirdparty.xml-validation: report`, with a comment explaining the three modes and #278)
- Test: `openespi-thirdparty/src/test/java/org/greenbuttonalliance/espi/thirdparty/subscription/SubscriptionFeedValidatorTest.java`

**Interfaces:**
- Produces:
  - `enum XmlValidationMode { OFF, REPORT, ENFORCE }`
  - `record FeedValidation(XmlValidationMode mode, Boolean valid, int errorCount, List<EspiXmlSchemaValidator.Issue> issues)` with `boolean blocksDisplay()` = `mode == ENFORCE && Boolean.FALSE.equals(valid)`.
  - `@Component SubscriptionFeedValidator(@Value("${espi.thirdparty.xml-validation:report}") XmlValidationMode mode)`; `FeedValidation validate(byte[] xml)` (uses `EspiXmlSchemaValidator.instance().validate(byte[])`; `OFF` → `valid null, errorCount 0, issues []`); `FeedValidation revalidate(String xml, XmlValidationMode savedMode)` for displaying a saved fetch with the mode it was saved under.

- [ ] **Step 1: Tests.** `offSkipsValidation` (malformed XML → `valid` null, no issues); `reportListsIssuesWithoutBlocking` (fixture `subscription/feed-with-278-errors.xml` → `valid` false, `errorCount > 0`, `blocksDisplay()` false); `enforceBlocksInvalidFeed` (same fixture → `blocksDisplay()` true); `enforceAllowsValidFeed` (fixture `subscription/valid-feed.xml` → `valid` true, `blocksDisplay()` false). Fixtures come from Task 5 Step 1; until then use a minimal valid Atom feed with one UsagePoint and a copy with `<espi:kind>ELECTRICITY</espi:kind>`.
- [ ] **Step 2: Run** `-Dtest=SubscriptionFeedValidatorTest`; expect FAIL (no classes).
- [ ] **Step 3: Implement** the three types; log `issueCount` at INFO and each issue at DEBUG.
- [ ] **Step 4: Run**; expect PASS.
- [ ] **Step 5: Commit** `feat(thirdparty): off/report/enforce XSD validation of fetched feeds (#89)`.

### Task 3: Saved-fetch table, entity and repository (Flyway V7)

**Files:**
- Create: `openespi-thirdparty/src/main/resources/db/thirdparty/h2/V7__Create_subscription_feed_fetches.sql`
- Create: `openespi-thirdparty/src/main/resources/db/thirdparty/mysql/V7__Create_subscription_feed_fetches.sql`
- Create: `openespi-thirdparty/src/main/resources/db/thirdparty/postgres/V7__Create_subscription_feed_fetches.sql`
- Create: `openespi-thirdparty/src/main/java/org/greenbuttonalliance/espi/thirdparty/subscription/SubscriptionFeedFetchEntity.java`
- Create: `openespi-thirdparty/src/main/java/org/greenbuttonalliance/espi/thirdparty/subscription/SubscriptionFeedFetchRepository.java`
- Modify: `openespi-thirdparty/src/main/java/org/greenbuttonalliance/espi/thirdparty/ThirdPartyApplication.java:46-51` (add `org.greenbuttonalliance.espi.thirdparty.subscription` to `@EntityScan` and `@EnableJpaRepositories`)
- Modify: Flyway `locations` in `application.yml` (`classpath:db/thirdparty/mysql`), `application-local.yml` (`.../h2`), `application-dev-mysql.yml` (`.../mysql`), `application-dev-postgresql.yml` (`.../postgres`), `src/test/resources/application-test.yml` (`.../h2`)
- Test: `openespi-thirdparty/src/test/java/org/greenbuttonalliance/espi/thirdparty/schema/AbstractThirdPartySchemaTest.java` (`EXPECTED_VERSIONS`, new round-trip test)

**Interfaces:**
- Produces:
  - `SubscriptionFeedFetchEntity` (JPA, table `subscription_feed_fetches`): `UUID id`; `UUID authorizationId`; `Instant fetchedAt`; `String resourceUri`; `Instant publishedMin`; `Instant publishedMax`; `int httpStatus`; `String body`; `XmlValidationMode validationMode` (Task 2's enum, `@Enumerated(STRING)`); `Boolean schemaValid` (null when `off`); `int errorCount`. Constructor `SubscriptionFeedFetchEntity(UUID authorizationId, Instant fetchedAt, String resourceUri, Instant publishedMin, Instant publishedMax, int httpStatus, String body, FeedValidation validation)` generating a random `id`; getters only.
  - `SubscriptionFeedFetchRepository extends JpaRepository<SubscriptionFeedFetchEntity, UUID>`: `List<SubscriptionFeedFetchEntity> findAllByAuthorizationIdOrderByFetchedAtDesc(UUID authorizationId)`, `Optional<SubscriptionFeedFetchEntity> findFirstByAuthorizationIdOrderByFetchedAtDesc(UUID authorizationId)`, `Optional<SubscriptionFeedFetchEntity> findByIdAndAuthorizationId(UUID id, UUID authorizationId)`.

- Consumes: `XmlValidationMode`, `FeedValidation` (Task 2).

- [ ] **Step 1: Write the failing schema test.** `EXPECTED_VERSIONS = {"1", "2", "3", "5", "6", "7"}` and add `subscription_feed_fetches_round_trip`: save a `RetailCustomerEntity` and an `AuthorizationEntity` owned by it, save two fetches 1 s apart (one 200 with a body of 100 000 characters, one 403), then assert `findAllByAuthorizationIdOrderByFetchedAtDesc` returns the 403 then the 200 with the full body length; delete the authorization and assert `count()` for that authorization is 0 (cascade).
- [ ] **Step 2: Run** `mvn test -pl openespi-thirdparty -Dtest=LocalProfileSchemaTest`; expect FAIL (no V7).
- [ ] **Step 3: Write the three V7 scripts.** Columns: `id CHAR(36) PRIMARY KEY`, `authorization_id CHAR(36) NOT NULL` with `FOREIGN KEY ... REFERENCES authorizations(id) ON DELETE CASCADE`, `fetched_at TIMESTAMP NOT NULL`, `resource_uri VARCHAR(1024) NOT NULL`, `published_min TIMESTAMP`, `published_max TIMESTAMP`, `http_status INT NOT NULL`, `body` (H2 `CLOB`, MySQL `LONGTEXT`, PostgreSQL `TEXT`), `validation_mode VARCHAR(16) NOT NULL`, `schema_valid BOOLEAN`, `error_count INT NOT NULL`; index `idx_sff_authorization_fetched (authorization_id, fetched_at)`. Header comment: TP-only table (#89), V7 is the next shared version, the DC never applies it (as the DC-only V4).
- [ ] **Step 4: Write the entity and repository, scan them, add the Flyway locations.** Map `body` so `ddl-auto: validate` accepts all three column types (e.g. `@Lob` or `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)`; the schema tests decide). `id`/`authorization_id` as `@JdbcTypeCode(SqlTypes.CHAR)` like `IdentifiedObject`.
- [ ] **Step 5: Run** `LocalProfileSchemaTest` (Windows) and `DevMysqlProfileSchemaTest`, `DevPostgresqlProfileSchemaTest` (WSL); expect PASS.
- [ ] **Step 6: Commit** `feat(thirdparty): subscription_feed_fetches table for saved Subscription feeds (#89)`.

### Task 4: Dev-only disk capture

**Files:**
- Create: `.../thirdparty/subscription/SubscriptionFeedCapture.java` (interface)
- Create: `.../thirdparty/subscription/DiskSubscriptionFeedCapture.java`
- Test: `openespi-thirdparty/src/test/java/org/greenbuttonalliance/espi/thirdparty/subscription/DiskSubscriptionFeedCaptureTest.java`

**Interfaces:**
- Produces: `interface SubscriptionFeedCapture { void capture(String subscriptionId, int httpStatus, byte[] body); }`. `DiskSubscriptionFeedCapture` is `@Component @Profile({"dev-mysql", "dev-postgresql", "local", "dev"})`, constructor `(@Value("${espi.dev.xml-capture.enabled:true}") boolean enabled, @Value("${espi.dev.xml-capture.dir:dev-xml-capture}") String dir)`. Consumers take `ObjectProvider<SubscriptionFeedCapture>` and call it only if present.
- Layout: `<dir>/thirdparty-run-<yyyyMMdd-HHmmss>/GET_Batch_Subscription_<subscriptionId>_<n>.xml` (`n` counts from 1 per run); a non-200 writes `..._<n>.error.txt` with `HTTP <status>` and the body. Relative `dir` resolves against the nearest ancestor containing `.git` (same rule as the DC's `EspiXmlCaptureFilter.resolveCaptureDir`; reimplement, do not depend on the DC). I/O failures are logged at WARN and never thrown.

- [ ] **Step 1: Tests** (with `@TempDir`, absolute dir): `writesFeedFile` (200 → one `.xml` with the exact bytes); `writesErrorFileForNon200` (403 → `.error.txt` containing `HTTP 403`); `numbersSuccessiveCaptures` (`_1`, `_2`); `disabledWritesNothing`; `notActiveOnTestProfile` (a `@SpringBootTest` with profile `test` has no `SubscriptionFeedCapture` bean).
- [ ] **Step 2: Run**; expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run**; expect PASS.
- [ ] **Step 5: Commit** `feat(thirdparty): dev-only capture of fetched Subscription feeds (#89)`.

### Task 5: Feed reader and Subscription tree

**Files:**
- Create: `.../thirdparty/subscription/SubscriptionTree.java` (the records below)
- Create: `.../thirdparty/subscription/SubscriptionFeedReader.java`
- Create: `.../thirdparty/subscription/SubscriptionFeedReadException.java`
- Create: `openespi-thirdparty/src/test/resources/subscription/feed-with-278-errors.xml`, `valid-feed.xml`, `empty-feed.xml`
- Test: `openespi-thirdparty/src/test/java/org/greenbuttonalliance/espi/thirdparty/subscription/SubscriptionFeedReaderTest.java`

**Interfaces:**
- Produces (records in `SubscriptionTree.java`, nested):
  - `record SubscriptionTree(String feedId, String title, OffsetDateTime updated, int entryCount, List<UsagePointNode> usagePoints, List<String> linkingProblems)`
  - `record Entry<T>(String id, String selfHref, OffsetDateTime published, OffsetDateTime updated, T resource)`
  - `record UsagePointNode(Entry<UsagePointDto> usagePoint, Entry<TimeConfigurationDto> localTimeParameters, List<MeterReadingNode> meterReadings, List<Entry<UsageSummaryDto>> usageSummaries, List<Entry<ElectricPowerQualitySummaryDto>> powerQualitySummaries)` (`localTimeParameters` may be null)
  - `record MeterReadingNode(Entry<MeterReadingDto> meterReading, Entry<ReadingTypeDto> readingType, List<Entry<IntervalBlockDto>> intervalBlocks)` (`readingType` may be null; blocks sorted by interval start)
  - `@Component SubscriptionFeedReader` with `SubscriptionTree read(byte[] xml)`; throws `SubscriptionFeedReadException(String message, Throwable cause)` (unchecked) when the bytes do not unmarshal to an `AtomFeedDto`.
- Linking rule (the algorithm the tests pin): index entries by `selfLink.href`. For each entry, look at its `rel="related"` links whose `type` starts with `espi-entry/`; resolve the href against the index. Parent of a MeterReading = the related `espi-entry/UsagePoint`; its ReadingType = related `espi-entry/ReadingType`; parent of an IntervalBlock = related `espi-entry/MeterReading`; parent of a UsageSummary / ElectricPowerQualitySummary = related `espi-entry/UsagePoint`; a UsagePoint's LocalTimeParameters = its related `espi-entry/LocalTimeParameters`. `espi-feed/...` links are ignored. A child with no resolvable parent link adds `"<Type> <selfHref>: parent <ParentType> not in the feed"` to `linkingProblems` and is left out of the tree; an entry with no known resource adds `"Entry <id>: unknown resource type"`. Unresolved forward links are not problems.

- [ ] **Step 1: Capture the fixture.** Start the local stack (`scripts/local/start-all`), sign in to the TP as `customer`, authorize **Monthly** (with Task 1's FB_17 scope), copy the `resourceURI` from the TP home page, then with `DC_ADMIN` from `docs/LOCAL_TESTING.md` "curl recipes": `curl -s -H "Authorization: Bearer $DC_ADMIN" "<resourceURI>?published-min=2025-10-01T00:00:00Z&published-max=2026-11-20T00:00:00Z" > openespi-thirdparty/src/test/resources/subscription/feed-with-278-errors.xml`. It must contain 1 UsagePoint, 1 MeterReading, 1 ReadingType, 2 IntervalBlocks of 24 readings, 1 UsageSummary, 1 ElectricPowerQualitySummary, 1 LocalTimeParameters. `valid-feed.xml` = the same with the ServiceCategory kind replaced by its numeric code so it passes the XSD; `empty-feed.xml` = the feed element with no entries.
- [ ] **Step 2: Tests.**
  - `readsSampleSubscription`: one UsagePoint; its LocalTimeParameters present; one MeterReading with a ReadingType and 2 IntervalBlocks in start order, 24 readings each; 1 UsageSummary; 1 ElectricPowerQualitySummary; `entryCount` 8; `linkingProblems` empty.
  - `childWithMissingParentIsReported`: the fixture with the MeterReading entry removed → no MeterReadings, and problems name both IntervalBlocks (`IntervalBlock ...: parent MeterReading not in the feed`).
  - `meterReadingWithoutReadingType`: ReadingType entry removed → MeterReadingNode with `readingType` null and its blocks still present; no problem for it (the ReadingType is a forward reference).
  - `emptyFeedHasNoUsagePoints`: `empty-feed.xml` → `usagePoints` empty, `entryCount` 0.
  - `malformedXmlThrows`: `"<html>oops</html>"` → `SubscriptionFeedReadException`.
- [ ] **Step 3: Run** `-Dtest=SubscriptionFeedReaderTest`; expect FAIL.
- [ ] **Step 4: Implement** with a `JAXBContext` of `AtomFeedDto.class, UsageAtomEntryDto.class`, created once; `entry.getResource()` gives the DTO.
- [ ] **Step 5: Run**; expect PASS. Point Task 2's validator test at the real fixtures and re-run it.
- [ ] **Step 6: Commit** `feat(thirdparty): read a Subscription feed into a linked tree (#89)`.

### Task 6: Fetch: HTTP client, range and fetch service

**Files:**
- Create: `.../thirdparty/subscription/FetchRange.java`
- Create: `.../thirdparty/subscription/SubscriptionClient.java`
- Create: `.../thirdparty/subscription/SubscriptionFetchService.java`
- Create: `.../thirdparty/subscription/AuthorizationNotFoundException.java`
- Test: `.../thirdparty/subscription/FetchRangeTest.java`, `SubscriptionClientTest.java`, `SubscriptionFetchServiceTest.java`

**Interfaces:**
- Consumes: `SubscriptionFeedValidator.validate(byte[])` (Task 2), `SubscriptionFeedCapture` (Task 4), `SubscriptionFeedFetchRepository` and the entity constructor (Task 3), `AuthorizationRepository`, `OAuth2AuthorizedClientManager`, `EspiScope.parse(String).historyLength()` (openespi-common), `EspiBatchUri.subscriptionId(String)`.
- Produces:
  - `record FetchRange(Instant publishedMin, Instant publishedMax)`; `static FetchRange of(LocalDate from, LocalDate to, String scope, Clock clock)`: both null → `publishedMin` = now − HistoryLength seconds (or `1970-01-01T00:00:00Z` when the scope has none or does not parse), `publishedMax` = now; `from` → its UTC midnight; `to` → the UTC midnight after it (inclusive day); `from` after `to` → `IllegalArgumentException("The start date must be on or before the end date.")`. Both instants truncated to seconds.
  - `@Component SubscriptionClient(RestClient.Builder builder)`; `record Response(int status, byte[] body)`; `Response get(String resourceUri, String accessToken, FetchRange range)`: `GET resourceUri?published-min=…&published-max=…` (ISO-8601 `Z`), headers `Authorization: Bearer …`, `Accept: application/atom+xml`; any status is returned, not thrown; I/O failure → `DataCustodianUnreachableException(String message, Throwable cause)` (unchecked, message without the token).
  - `@Service SubscriptionFetchService` with `UUID`-keyed methods:
    - `AuthorizationEntity ownedAuthorization(UUID authorizationId, String username)` → the row if `retailCustomer.username` equals `username` and it has a `clientRegistrationId` and `resourceURI`; otherwise `AuthorizationNotFoundException` (→ 404).
    - `FetchOutcome fetch(UUID authorizationId, Authentication user, LocalDate from, LocalDate to)` where `sealed interface FetchOutcome permits Saved, NotAuthorized, Unreachable, InvalidRange`; `record Saved(UUID fetchId)`, `record NotAuthorized()`, `record Unreachable(String message)`, `record InvalidRange(String message)`.
  - Order inside `fetch` (pinned by tests): ownership check → range (`InvalidRange` before any HTTP) → `authorizedClientManager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId(regId).principal(user).build())` (null → `NotAuthorized`) → **re-load the row** and use its current `resourceURI` → `client.get` (`Unreachable` on exception, nothing saved) → capture (if bean present) → `validator.validate` only for status 200 (non-200 saves `FeedValidation(mode, null, 0, [])`) → save → `Saved(id)`. Body saved as UTF-8 text.

- [ ] **Step 1: Tests.**
  - `FetchRangeTest`: `defaultsToHistoryLength` (scope with `HistoryLength=34128000`, clock 2026-11-20T12:00:00Z → min 2025-10-21T12:00:00Z, max 2026-11-20T12:00:00Z); `defaultsToEpochWithoutHistoryLength`; `datesAreUtcDaysInclusive` (2026-01-01..2026-01-02 → 2026-01-01T00:00:00Z..2026-01-03T00:00:00Z); `rejectsFromAfterTo` (message exactly as above).
  - `SubscriptionClientTest` (`MockRestServiceServer` bound to the builder): `sendsRangeBearerAndAccept` (query `published-min=2026-01-01T00:00:00Z&published-max=2026-01-03T00:00:00Z`, header `Bearer tok`, `Accept: application/atom+xml`); `returnsNon200WithoutThrowing` (403 body `denied` → `Response(403, "denied")`); `connectionFailureIsUnreachable`.
  - `SubscriptionFetchServiceTest` (mocks): `otherUsersAuthorizationIsNotFound`; `noResourceUriIsNotFound`; `noAuthorizedClientIsNotAuthorized` (nothing saved, no HTTP); `usesResourceUriReloadedAfterAuthorize` (row's URI changes during `authorize` → the new URI is requested); `savesNon200WithoutValidation`; `unreachableSavesNothing`; `invalidRangeMakesNoRequest`; `savesValidationResultFor200`.
- [ ] **Step 2: Run**; expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run**; expect PASS.
- [ ] **Step 5: Commit** `feat(thirdparty): fetch and save a customer's Batch/Subscription feed (#89)`.

### Task 7: Subscription page and home-page link

**Files:**
- Create: `.../thirdparty/subscription/SubscriptionController.java`
- Create: `.../thirdparty/subscription/SubscriptionPage.java` (view model)
- Create: `openespi-thirdparty/src/main/resources/templates/subscription.html`
- Modify: `.../thirdparty/web/AuthorizationView.java` (add `String id`, `boolean active`), `.../thirdparty/web/HomeController.java` (unchanged query), `templates/home.html:85-113` (per row: **View data** → `/subscription/{id}` when `active` and `resourceUri` present; **Authorize again** → that registration's authorize button when revoked)
- Test: `.../thirdparty/subscription/SubscriptionControllerTest.java`, `.../thirdparty/web/HomeControllerTests.java`

**Interfaces:**
- Consumes: `SubscriptionFetchService` (Task 6), `SubscriptionFeedFetchRepository` (Task 3), `SubscriptionFeedValidator.revalidate` (Task 2), `SubscriptionFeedReader.read` (Task 5), `HistoryPeriod` (openespi-common), `EspiScope`.
- Produces:
  - `GET /subscription/{authorizationId}` (optional `fetch` UUID): ownership via `ownedAuthorization` (404 otherwise). Revoked → page with "Authorize again" and the saved fetches list. No saved fetch yet and active → perform a fetch (as POST) then redirect. Otherwise show `fetch` (must be `findByIdAndAuthorizationId`, else 404) or the latest.
  - `POST /subscription/{authorizationId}/fetch` (form fields `from`, `to`, ISO dates, optional; CSRF on): `Saved` → redirect `/subscription/{id}?fetch={fetchId}`; `NotAuthorized` → redirect with flash `"The Data Custodian authorization has ended. Authorize again."`; `Unreachable` → flash `"The Data Custodian could not be reached: <message>"`; `InvalidRange` → flash its message.
  - `SubscriptionPage` fields the template uses: authorization (access name, scope, `historyPeriod` text from `HistoryPeriod`, `resourceUri`, active), fetches list (id, fetchedAt, status), the selected fetch (range, fetchedAt, status, error body excerpt ≤ 2000 chars for non-200), `FeedValidation`, `SubscriptionTree` or `readError` text, and `message` for the empty-tree case.
- Page content per the spec's section 6 (authorization, validation, linking problems, UsagePoints → MeterReadings with ReadingType → IntervalBlocks in `<details>` with a readings table: start in the UsagePoint's local time and UTC, duration, raw value, value × 10^powerOfTenMultiplier with uom, cost; summaries; feed facts). Uses `fragments/layout`. Fixed copy: "The Data Custodian returned no UsagePoints for this range.", "The feed could not be read: <reason>", "(no ReadingType)".

- [ ] **Step 1: Tests** (`@WebMvcTest(SubscriptionController.class)` with security, mocked services; a real `SubscriptionFeedReader` and the Task 5 fixtures):
  - `ownerSeesSubscription` (HTML contains the UsagePoint, 2 `<details>`, "ElectricPowerQualitySummary", the HistoryLength period "13 months (395 days)");
  - `otherUsersAuthorizationIs404`; `fetchOfAnotherAuthorizationIs404`;
  - `revokedShowsAuthorizeAgain`;
  - `enforceInvalidShowsIssuesNotData`; `reportInvalidShowsIssuesAndData`;
  - `showsReadErrorForNonFeedBody` (saved 200 with `<html>oops</html>` → "The feed could not be read:" and status 200);
  - `showsEmptyFeedMessage`;
  - `postRedirectsToSavedFetch`; `postNotAuthorizedFlashesAuthorizeAgain`; `postWithoutCsrfIs403`;
  - `HomeControllerTests`: an active row with `resourceUri` renders a `/subscription/<id>` link; a revoked row renders "Authorize again" and no link.
- [ ] **Step 2: Run**; expect FAIL.
- [ ] **Step 3: Implement** controller, view model, template, home changes.
- [ ] **Step 4: Run** `-Dtest='SubscriptionControllerTest,HomeControllerTests,GbaLayoutPagesTest'`; expect PASS.
- [ ] **Step 5: Commit** `feat(thirdparty): Subscription page showing the customer's whole feed (#89)`.

### Task 8: Docs, full suites and manual end-to-end check

**Files:**
- Modify: `docs/LOCAL_TESTING.md` ("Customer flow" step 5 **View data**; "Captured ESPI XML": the TP's `thirdparty-run-*` folder; "Known limitations": #89 now fetches/validates/saves/displays, import into the usage tables remains; the AS PostgreSQL/MySQL seed scripts still list the old `FB=4_5_15` scopes; sample data visible until about 2027-01-31)
- Modify: `CLAUDE.md` ("Still in progress": the TP fetches, validates, saves and displays the Subscription feed; import into the usage tables is #89's remainder)

- [ ] **Step 1: Update the two docs.**
- [ ] **Step 2: Full suites** in WSL: `mvn install -DskipTests` then `mvn verify -pl openespi-thirdparty`, `-pl openespi-authserver`, `-pl openespi-datacustodian`; expect 0 failures.
- [ ] **Step 3: Manual check** on `local`, once with Monthly and once with Daily: authorize, **View data** → the page shows 1 UsagePoint with LocalTimeParameters, 1 MeterReading with its ReadingType, 2 IntervalBlocks of 24 readings, 1 UsageSummary, 1 ElectricPowerQualitySummary; the validation section lists the #278 errors in `report` mode; a second **Fetch now** adds a row to the fetches list; `dev-xml-capture/thirdparty-run-*/` holds the XML. Restart the TP with `ESPI_THIRDPARTY_XML_VALIDATION=enforce` and confirm the issues show and the data does not.
- [ ] **Step 4: Commit** `docs: TP Subscription view in the local runbook and CLAUDE.md (#89)`.
