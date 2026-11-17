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
| TP admin | `admin` / `admin` at http://localhost:8082/ThirdParty/login | TP administrator (`ROLE_CUSTODIAN`); also opens the H2 console at `/ThirdParty/h2-console/`. `local` profile only. |
| TP customer | `customer` / `customer` | TP customer (`ROLE_USER`); sees the Data Access Authorization cards. `local` profile only. |

## OAuth clients (AS)

| client_id | Secret | Grant | Scope | Purpose |
|---|---|---|---|---|
| `third_party` | `tp-secret` | authorization_code, refresh_token | ESPI FB scopes (`FB=4_5_15;IntervalDuration=3600;…`) | The TP's customer flow (customer access token) |
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
  "scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13" }'

# Start the customer flow as the TP does (follow the redirects in a browser)
open "$TP/oauth2/authorization/datacustodian-monthly"
```

## Captured ESPI XML

On the `local` profile the DC writes every ESPI resource response to `dev-xml-capture/run-<timestamp>/` at the repository root (`GET_UsagePoint.xml`, …), and an `.error.txt` with the status, body or exception chain for any 4xx/5xx or unhandled exception. The folder is git-ignored.

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

## Known limitations

The customer flow does not complete yet. Open blockers, in the order they are hit (see the roadmap, #255):

- **#249:** the TP has no user login page.
- **#232:** the AS → DC delegation (`/authorize/delegate`) drops the authorization parameters and returns 400.
- **#146 / #202:** the TP's token handling and the resource URIs in the token response.
- **#197 / #89:** the TP cannot yet read and display the DC's feeds.
- **#211:** a customer narrowing the requested data at consent gets `invalid_scope`.
