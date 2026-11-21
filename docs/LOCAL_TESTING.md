# Local manual testing

Run the Authorization Server (AS), Data Custodian (DC) and Third Party (TP) together on your machine and test them by hand. Everything here uses the `local` Spring profile: in-memory H2 databases (all data resets on restart) and **DEV ONLY** credentials that are published in this repository.

> **Never use these credentials outside your machine.** The AS seeds the demo clients only when `espi.authserver.seed.demo-clients=true` (the local/dev/test profiles), and refuses to start with it on unless the issuer is `localhost`.

Prerequisites: Java 25, Maven 3.9+, `curl`. No Docker is needed.

## Start, check, stop

| | Bash (Linux, macOS, Git Bash) | PowerShell 7 (Windows) |
|---|---|---|
| Build and start all three, wait until healthy | `scripts/local/start-all.sh` | `scripts\local\start-all.ps1` |
| Start without rebuilding | `scripts/local/start-all.sh --no-build` | `scripts\local\start-all.ps1 -NoBuild` |
| Non-interactive check (tokens, introspection, feeds, actuator) | `scripts/local/smoke.sh` | `scripts\local\smoke.ps1` |
| Stop all three (by port) | `scripts/local/stop-all.sh` | `scripts\local\stop-all.ps1` |

Logs go to `.local-run/<app>.log`. Run the smoke check before testing by hand; it exits non-zero if anything fails.

## URLs

| App | URL | In-memory database |
|---|---|---|
| Authorization Server | http://localhost:9999 | `jdbc:h2:mem:oauth2_authserver` |
| Data Custodian | http://localhost:8081/DataCustodian | `jdbc:h2:mem:datacustodian` |
| Third Party | http://localhost:8082/ThirdParty | `jdbc:h2:mem:thirdparty` |

## Logins

| App | User | Notes |
|---|---|---|
| AS admin | `admin` / `admin` at http://localhost:9999/login | A passkey is enrolled at first login (`/webauthn/register`), by design. Override with `ESPI_BOOTSTRAP_ADMIN_USERNAME` / `ESPI_BOOTSTRAP_ADMIN_PASSWORD_HASH`. |
| DC admin | `admin` / `admin` | Sandbox custodian user (dev/test profiles only). |
| DC customer | `customer` / `customer` | Owns the sample UsagePoint (see "Sample data"). `local` profile only. |
| TP admin | `admin` / `admin` at http://localhost:8082/ThirdParty/login | TP administrator (`ROLE_CUSTODIAN`); obtains the TP's client access token on the admin page, and opens the H2 console at `/ThirdParty/h2-console/`. `local` profile only. |
| TP customer | `customer` / `customer` | TP customer (`ROLE_USER`); authorizes the TP at the DC from the Data Access Authorization cards (see "Customer flow"). `local` profile only. |

## OAuth clients (AS)

| client_id | Secret | Grant | Scope | Purpose |
|---|---|---|---|---|
| `third_party` | `tp-secret` | authorization_code, refresh_token | ESPI FB scopes (`FB=4_5_15_17;IntervalDuration=3600;…`) | The TP's customer flow (customer access token) |
| `third_party` | `tp-secret` | client_credentials | `FB=34_35` | The TP's client access token: the only TP token the ESPI Authorization (and later Bulk) APIs accept |
| `third_party` | `tp-secret` | client_credentials | `ThirdParty_Admin_Access` | TP actuator |
| `data_custodian_admin` | `dc-secret` | client_credentials | `DataCustodian_Admin_Access` | DC operator: every ESPI resource, DC actuator |
| `third_party_admin` | `tpadmin-secret` | client_credentials | `ThirdParty_Admin_Access` | Legacy separate TP admin client |
| `datacustodian` | `datacustodian-secret` | client_credentials | – | DC token introspection |
| `espi_admin` | `local-admin-secret` | client_credentials | `authserver.admin` | AS admin APIs, AS actuator |
| `espi_registrar` | `local-registrar-secret` | client_credentials | `client.create` | RFC 7591 initial access token |

Each grant yields only its own kind of scope (#251). `FB=34_35` (client access token) and `FB=36_40` (registration access token) are unique Function Block combinations that only `client_credentials` yields. Customer authorization yields only scopes naming customer data Function Blocks from the CMD Certification Application v2.14. A customer token whose scope includes FB_51 always carries `customerResourceURI`.

The TP's Spring OAuth2 registrations match these: `datacustodian-monthly` and `datacustodian-daily` (authorization_code, redirect `http://localhost:8082/ThirdParty/login/oauth2/code/<registration>`) and `datacustodian-admin` (client_credentials).

## curl recipes

```bash
AS=http://localhost:9999; DC=http://localhost:8081/DataCustodian; TP=http://localhost:8082/ThirdParty

# A client_credentials token (pick any client row above)
DC_ADMIN=$(curl -s -u data_custodian_admin:dc-secret -d grant_type=client_credentials \
  -d scope=DataCustodian_Admin_Access $AS/oauth2/token | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
TP_CLIENT=$(curl -s -u third_party:tp-secret -d grant_type=client_credentials \
  -d scope=FB=34_35 $AS/oauth2/token | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

# Introspection (as the DC does it)
curl -s -u datacustodian:datacustodian-secret -d "token=$DC_ADMIN" $AS/oauth2/introspect

# ESPI feeds (Atom XML)
curl -s -H "Authorization: Bearer $DC_ADMIN" $DC/espi/1_1/resource/UsagePoint
curl -s -H "Authorization: Bearer $TP_CLIENT" $DC/espi/1_1/resource/Authorization   # only this TP's rows

# Admin actuator endpoints
curl -s -H "Authorization: Bearer $DC_ADMIN" $DC/actuator/info
curl -s -H "Authorization: Bearer $TP_CLIENT" $TP/actuator/info

# RFC 7591 registration: initial access token, then register
IAT=$(curl -s -u espi_registrar:local-registrar-secret -d grant_type=client_credentials \
  -d scope=client.create $AS/oauth2/token | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
curl -s -H "Authorization: Bearer $IAT" -H 'Content-Type: application/json' $AS/oauth2/register -d '{
  "client_name": "My Third Party", "grant_types": ["authorization_code", "refresh_token"],
  "redirect_uris": ["https://tp.example/callback"],
  "scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000" }'
# HistoryLength is in seconds (REQ.21.6.1.2.1): 34128000 = 395 days. A month count such as 13 means 13 seconds.

# Start the customer flow as the TP does (follow the redirects in a browser)
open "$TP/oauth2/authorization/datacustodian-monthly"
```

## Captured ESPI XML

On the `local` profile the DC writes every ESPI resource response to `dev-xml-capture/run-<timestamp>/` at the repository root (`GET_UsagePoint.xml`, …), and an `.error.txt` with the status, body or exception chain for any 4xx/5xx or unhandled exception. The folder is git-ignored.

On the `local`, `dev`, `dev-mysql` and `dev-postgresql` profiles the TP captures each `Batch/Subscription` fetch to `dev-xml-capture/thirdparty-run-<yyyyMMdd-HHmmss>/GET_Batch_Subscription_<subscriptionId>_<n>.xml` (a non-200 response goes to `.error.txt` instead).

## Sample data (DC)

On the `local` profile the DC seeds a small, deterministic ESPI dataset at startup (`LocalSampleDataSeedRunner`, #253). The data is in-memory, so it is recreated on every restart, with the same ids.

| Resource | Sample |
|---|---|
| RetailCustomer | `customer` / `customer` (`ROLE_USER`), owner of the UsagePoint |
| UsagePoint | Electricity, LocalTimeParameters US Eastern (UTC-5, DST rules `360E2000` / `B40E2000`) |
| ReadingType | Energy delivered, hourly, Wh × 10⁻³, USD (`accumulationBehaviour 4`, `kind 12`, `uom 72`) |
| MeterReading / IntervalBlock | 2 daily blocks (2026-01-01 and 02) of 24 hourly readings, with cost |
| UsageSummary / ElectricPowerQualitySummary | January 2026 billing period |
| Customer, CustomerAccount, CustomerAgreement, ServiceLocation, Meter | One residential customer |
| ApplicationInformation | The `third_party` client, with the TP's local redirect and notification URIs |

The schema values follow the most common shapes in the Green Button CMD certification test feeds. Every identifying value is synthetic (`example.com`, `555-01xx`, `SAMPLE-…` numbers).

## Customer flow (TP → AS → DC → TP)

1. Sign in to the TP as `customer` / `customer` at http://localhost:8082/ThirdParty/login.
2. On the TP home page, choose **Authorize Monthly Access** (or Daily). The TP sends you to the AS, which hands the login and consent to the DC.
3. At the DC, sign in as `customer` / `customer` and approve the requested data on the Authorization Screen.
4. You return to the TP home page, still signed in to the TP, with "The Data Custodian authorized this Third Party". **Your authorizations** lists the scope, the access token's expiry, and the `resourceURI`, `authorizationURI` and `customerResourceURI` from the token response.

5. Under **Your authorizations**, choose **View data** (an ended authorization shows **Authorize again** instead). The TP fetches `Batch/Subscription` from the DC with the customer token (never shown or stored on the page), validates the feed against the ESPI XSDs, saves the fetch in its `subscription_feed_fetches` table and shows `/subscription/{authorizationId}`: the feed's UsagePoints with LocalTimeParameters, MeterReadings with ReadingType, IntervalBlocks and their readings, UsageSummaries and ElectricPowerQualitySummaries, the validation result, and the list of fetches; **Fetch now** adds a row. The sample data gives 1 UsagePoint, 1 MeterReading, 2 IntervalBlocks of 24 readings, 1 UsageSummary and 1 ElectricPowerQualitySummary. Local times use the LocalTimeParameters standard offset; DST is not applied (the rules are shown).

Both customer registrations (`datacustodian-monthly`, `datacustodian-daily`) request `FB=4_5_15_17` with the scope the AS registers: Monthly `FB=4_5_15_17;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=34128000`, Daily `FB=4_5_15_17;IntervalDuration=900;BlockDuration=monthly;HistoryLength=34128000`. If the AS still holds an older scope, restart it (the local databases are in memory). Both can be authorized one after the other in the same browser, and the same one again: each authorization goes through the DC's Authorization Screen with the TP's own `state` and callback (#299, #276).

`espi.thirdparty.xml-validation` is `off`, `report` (default: issues are listed, the data is still shown) or `enforce` (a feed with issues is rejected and its data is not shown). The DC currently writes enumeration values the XSD rejects (#278, e.g. ServiceCategory `ELECTRICITY`, `MILLI`, `WH`) and SummaryMeasurement/BillingChargeSource children without the ESPI namespace (#297). #278's `MILLI`/`WH` errors show only once #297 is fixed; today report mode lists #297's unqualified elements and the ServiceCategory content. So in `enforce` mode every real feed is rejected until both are fixed. To try it: restart only the TP with `ESPI_THIRDPARTY_XML_VALIDATION=enforce`.

The `SubscriptionClient` sends the customer token only to a `Batch/Subscription` URI on the configured DC (`espi.datacustodian.base-url`: same scheme, host and port, path under its base path), limits the body (`espi.thirdparty.subscription.max-body-bytes`, 16 MiB: on MySQL keep it at most half of `max_allowed_packet`, since escaping can double the INSERT; a save the database refuses is shown as a notice), uses connect/read timeouts of 10 s / 60 s and TLS 1.3 only. Outside the `local`, `dev`, `dev-mysql` and `dev-postgresql` profiles it requires https, so a deployment must set `espi.datacustodian.base-url` to an https URL or the TP fails at startup (#298).

The TP stores each token in its own ESPI `authorizations` table (Spring Security `oauth2Client`, #146), so the tokens survive a TP restart only on `dev-mysql` / `dev-postgresql`; on `local` the H2 database is in memory.

The TP administrator (`admin` / `admin`) can obtain the TP's own client access token (client_credentials, `FB=34_35`) from the **Client Access Token** card on the admin page; it shows the scope, expiry, the first characters of the token only, and the token response's `authorizationURI`, `resourceURI` (energy usage Bulk) and `customerResourceURI` (Customer PII Bulk, when applicable). The AS does not return these URIs for client_credentials tokens yet (#202).

## Known limitations

Open items (see the roadmap, #255):

- **#89:** the TP fetches, validates, saves and displays the Subscription feed; importing it into the usage tables is not done.
- **#278 / #297:** DC feed defects shown on the Subscription page in `report` mode (see "Customer flow" step 5).
- **#305:** authorize one registration at a time. Two authorizations running at once in one browser (two tabs) can mix up their approved scope and UsagePoints.
- The AS PostgreSQL and MySQL seed scripts still list the old `FB=4_5_15` scopes; only the local H2 seed matches the TP registrations.
- The sample data covers 2026-01-01 to 02 and is visible through the customer's `HistoryLength` (395 days) until about 2027-01-31; after that the feed still holds the UsagePoints and MeterReadings but no IntervalBlocks or summaries.
- **#202:** the AS's token-response URIs: `customerResourceURI` is gated on FB 54–62 instead of FB_51, a refresh re-provisions the subscription, and the `FB=34_35` client access token carries no `authorizationURI`, `resourceURI` (energy usage Bulk) or `customerResourceURI` (Customer PII Bulk).
