# Cross-service wire contracts (AS ↔ DC)

Canonical JSON examples of the HTTP wire formats exchanged between the Authorization
Server and the Data Custodian. These files are the **single source of truth** both
services bind to in their own tests — a lightweight, framework-free consumer-driven
contract suited to standalone Spring Boot services (deployed independently on EC2),
**not** a Spring Cloud microservice mesh.

## How the contract is enforced (no Spring Cloud, no Pact broker)

For each contract, two tests bind to the same file here:

- **Producer test** asserts the service's *actual* output matches this file.
- **Consumer test** asserts the service correctly *parses* this file.

If either side's wire format drifts from the file, that side's test fails. The fixture
is reviewed in PRs, so a deliberate wire change is a visible diff to this directory plus
a coordinated update on both sides.

What these contracts cover: the **wire format** (field names, types, the ESPI scope
grammar). What they deliberately do NOT cover: TLS, ingress, CORS, timeouts — those are
EC2 deployment concerns, not wire-format concerns.

## Contracts

The token-response / introspection wire format is defined by the **ESPI 4.0 standard** itself
(NAESB REQ.21 — its own example shows `"resourceURI":".../resource/Batch/Subscription/{id}"`,
`"scope":"FB=...;..."`, no `*_id` fields, `customerResourceURI` may be `""`). These fixtures
reproduce that standard shape for the two grant types:

| File | Grant type | `resourceURI` / `customerResourceURI` form |
|------|------------|--------------------------------------------|
| `token-response-subscription.json` | Authorization Code (Subscription) | `Batch/Subscription/{id}` / `Batch/RetailCustomer/{rc}` |
| `token-response-bulk.json` | Client Credentials (Bulk) | `Batch/Bulk/{bulkId}` / `Batch/Bulk/{bulkRcId}` |

- **Producer** (AS token endpoint / introspection, fed by DC `SubscriptionProvisioningServiceImpl`
  via `EspiBatchUri`) must emit these URI forms and only the three `*URI` fields.
- **Consumer** (DC `ResourceValidationFilter` / `EspiScopeOpaqueTokenIntrospector`) parses ids back
  out of the URIs via the same `EspiBatchUri`, and the `FB=...` scope via `EspiScope`.

UUIDs/ids/tokens in the fixtures are illustrative; the contract is the **shape** — field names,
the `Batch/{Subscription|Bulk|RetailCustomer}` URI forms, and the ESPI `FB=...` scope grammar.
