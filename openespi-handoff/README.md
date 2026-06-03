# OpenESPI-Handoff

Pure-infrastructure signed-handoff codec for the AS↔DC customer-flow redirect contract.

## Why this module exists

The GBA Authorization Server (`openespi-authserver`) and the Data Custodian Resource Server (`openespi-datacustodian`) are independently deployed Spring Boot applications. During the customer-facing OAuth 2.0 Authorization Code flow, control passes between them via HTTP redirects — the AS sends the customer to the DC for login + consent, and the DC sends them back with the result.

A **shared HTTP session** between AS and DC would force:
- Cross-app cookie-domain coupling (`SameSite=None` everywhere, even in dev)
- An ambiguous "whose authenticated principal lives in the shared session" answer
- New infrastructure to provision and reap (JDBC Spring Session schema or a Redis cluster)

Instead, this module implements a **signed-redirect-parameter** pattern: the cross-app state is encoded as a tamper-evident token carried in the redirect URL. Each app keeps its own session. The same idea OAuth 2.0 itself uses for the `state` parameter, just carrying more payload.

The codec is its own Maven module because:
1. Both `openespi-authserver` and `openespi-datacustodian` need the same canonical implementation.
2. `openespi-authserver` is deliberately independent of `openespi-common` (it must not depend on ESPI domain types like `RetailCustomerEntity`).
3. This module is **pure infrastructure** — HMAC + Jackson + JPA only, no ESPI domain types — so it can be safely shared without dissolving the AS↔domain boundary.

## Wire format

A handoff token is two dot-separated base64URL segments:

```
{base64URL(JSON(payload))} . {base64URL(HMAC-SHA256(key, base64URL(JSON(payload))))}
```

The JSON payload is a `SignedHandoff` polymorphic record. Two directions are defined:

### `SignedHandoff.Outbound` — AS → DC

```json
{
  "v": 1,
  "dir": "outbound",
  "cid": "opaque-trace-id",
  "iat": "2026-06-03T00:00:00Z",
  "exp": "2026-06-03T00:05:00Z",
  "nonce": "base64URL-128bits",
  "client_id": "third-party-client",
  "scope": "FB=4_5_15_54;IntervalDuration=3600",
  "return_url": "https://as.example.com/oauth2/authorize/continue"
}
```

### `SignedHandoff.Return` — DC → AS

```json
{
  "v": 1,
  "dir": "return",
  "cid": "opaque-trace-id",
  "iat": "2026-06-03T00:01:23Z",
  "exp": "2026-06-03T00:06:23Z",
  "nonce": "base64URL-128bits",
  "sub": "authenticated-customer-id",
  "up": ["uuid-1", "uuid-2"],
  "cust_uri": "https://dc.example.com/.../Customer/abc",
  "consent": "allow",
  "approved_scope": "FB=1_4_5_15_51_54;IntervalDuration=3600"
}
```

The `approved_scope` field carries the customer's **effective** scope after their checkbox decisions on the DC Authorization Screen — a (possibly narrowed) subset of the originally-requested scope. The AS mints the access token with `approved_scope`, NOT the original `scope`, so the third party receives exactly what the customer agreed to.

## Public API

| Class | Purpose |
|---|---|
| `SignedHandoff` | Sealed interface; `Outbound` and `Return` records carry direction-specific fields. Use `Outbound.of(...)` / `Return.of(...)` factories to fill in `version` and `direction` automatically. |
| `SignedHandoffCodec` | `@Component` exposing `encode(...)` and `decodeOutbound(token)` / `decodeReturn(token)`. Decode verifies signature (constant-time), version, direction, and expiry. Reads `espi.handoff.signing-key` from configuration. |
| `InvalidHandoffException` | Uniform rejection signal for malformed, tampered, expired, wrong-direction, replayed, or otherwise-rejected tokens. Callers should NOT reveal which sub-check failed to the user-agent. |
| `HandoffNonceEntity` | JPA entity for the single-use nonce table. Implements `Persistable<String>` with `isNew() == true` so `JpaRepository.save()` routes to `persist()` (INSERT-only), making duplicate-consume surface as a PK violation rather than silently UPDATE-ing. |
| `HandoffNonceRepository` | Spring Data JPA repository. Includes `deleteByExpiresAtBefore(cutoff)` for the periodic sweep. |
| `HandoffNonceService` | `@Service` exposing `generate()` for issuers and `consume(nonce, expiresAt)` for receivers. `consume` runs in `Propagation.REQUIRES_NEW` so the row commits independently of any surrounding business transaction — a partially-completed grant must not allow nonce reuse. |

## Security properties

- **Tamper detection** — HMAC-SHA256 over the base64URL-encoded payload. Verification uses `MessageDigest.isEqual` (constant-time) to avoid timing attacks.
- **Replay protection** — every payload carries a 128-bit random nonce; receivers consume it through `HandoffNonceService` which inserts a row keyed by the nonce. The PK uniqueness constraint atomically detects replay.
- **Expiry** — every payload carries `iat` / `exp`. Default 5-minute lifetime. Receivers reject expired payloads before nonce-consume.
- **Direction tag** — every payload carries `dir` ("outbound" or "return"). Decoders verify direction matches the called method (`decodeOutbound` rejects a `return`-direction payload, and vice versa). Prevents replaying a token from one direction as the other.
- **Versioning** — every payload carries `v`. Additive changes don't bump the version; the codec rejects unknown versions with `InvalidHandoffException`.
- **Signing-key strength** — the codec rejects keys shorter than 32 characters at construction time. Production deployments MUST supply a high-entropy value via `ESPI_HANDOFF_SIGNING_KEY` environment variable.

## Configuration

| Property | Default | Description |
|---|---|---|
| `espi.handoff.signing-key` | `dev-only-handoff-signing-key-change-me` (≥32 chars) | HMAC-SHA256 signing key. **Must be identical on the AS and DC sides** — they are independent processes that must produce verifiable signatures over the same payload. Override via `ESPI_HANDOFF_SIGNING_KEY` env var in production. |

The signing key is constructor-injected into `SignedHandoffCodec` via `@Value`. The `@Component` annotation means the codec is auto-registered as a Spring bean wherever its package is component-scanned.

## How consumers wire it in

### `openespi-datacustodian`

Add `org.greenbuttonalliance.espi.handoff` to the application class's scan paths:

```java
@SpringBootApplication(scanBasePackages = {
    "org.greenbuttonalliance.espi.datacustodian",
    "org.greenbuttonalliance.espi.common",
    "org.greenbuttonalliance.espi.handoff"
})
@EntityScan(basePackages = {
    /* ...domain packages... */,
    "org.greenbuttonalliance.espi.handoff"
})
@EnableJpaRepositories(basePackages = {
    /* ...repository packages... */,
    "org.greenbuttonalliance.espi.handoff"
})
```

Spring Boot's default `MessageSource` and Flyway will pick up the bundled migration at `classpath:db/migration/V4__Create_Handoff_Nonces.sql` (vendor-neutral; works on H2, MySQL, PostgreSQL).

Then `@Autowired SignedHandoffCodec codec; @Autowired HandoffNonceService nonceService;` wherever you need to encode/decode handoffs and consume nonces.

### `openespi-authserver`

The AS uses the default `@SpringBootApplication` scan (`org.greenbuttonalliance.espi.authserver` package root). Since the handoff classes live in a different package (`org.greenbuttonalliance.espi.handoff`), you must explicitly add scan paths to the AS application class:

```java
@SpringBootApplication(scanBasePackages = {
    "org.greenbuttonalliance.espi.authserver",
    "org.greenbuttonalliance.espi.handoff"
})
@EntityScan(basePackages = {
    "org.greenbuttonalliance.espi.authserver",
    "org.greenbuttonalliance.espi.handoff"
})
@EnableJpaRepositories(basePackages = {
    "org.greenbuttonalliance.espi.authserver",
    "org.greenbuttonalliance.espi.handoff"
})
```

The AS's own Flyway configuration will pick up the bundled migration in the AS schema as well.

## Flyway migration

`src/main/resources/db/migration/V4__Create_Handoff_Nonces.sql` creates the `handoff_nonces` table. Vendor-neutral DDL — works as-is on H2, MySQL, and PostgreSQL.

```sql
CREATE TABLE handoff_nonces (
    nonce        VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMP   NOT NULL,
    consumed_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (nonce)
);
CREATE INDEX idx_handoff_nonces_expires_at ON handoff_nonces (expires_at);
```

Both consuming apps (AS and DC) end up with this table in their own database; receivers track the nonces they've consumed against replay.

## Testing

This module ships with two test classes:

- **`SignedHandoffCodecTest`** — 12 unit tests covering round-trip for both directions, tampered-payload / tampered-signature / wrong-key rejection (via constant-time compare), expiry, wrong-direction, wrong-version, malformed / empty token, and the short-signing-key construction rejection.
- **`HandoffNonceServiceTest`** — 6 `@DataJpaTest` cases (in-memory H2 + Flyway): first-consume success, replay rejection (PK violation), distinct-nonces independence, uniqueness over 10 000 generates, blank-nonce rejection, and the periodic-sweep reaper.

Consumers SHOULD also have their own round-trip integration tests that encode on one side and decode on the other to detect drift between the AS and DC implementations.

## References

- **PR #140 (C1)** — original introduction of the codec in `openespi-common`.
- **PR #143 (C2b)** — `approved_scope` field added to `Return`.
- **PR C3** — extraction to this standalone module, AS-side delegation.
- **Issue #122** — overall authorization-flow design.
- **RFC 6749** — OAuth 2.0 Authorization Framework (the `state` parameter design that inspired this approach).

## Maven coordinates

```xml
<dependency>
    <groupId>org.greenbuttonalliance.espi</groupId>
    <artifactId>OpenESPI-Handoff</artifactId>
    <version>3.5.0-RC2</version>
</dependency>
```

## License

Apache License 2.0 — see the parent project's `LICENSE` file.
