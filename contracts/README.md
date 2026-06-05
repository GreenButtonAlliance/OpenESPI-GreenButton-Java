# Cross-service wire contracts (AS ↔ DC, DC → TP)

Canonical examples of the HTTP wire formats exchanged between the OpenESPI services:
the **Authorization Server ↔ Data Custodian** token/introspection format (JSON) and the
**Data Custodian → Third Party** notification format (XML). These files are the
**single source of truth** both services bind to in their own tests — a lightweight,
framework-free consumer-driven contract suited to standalone Spring Boot services
(deployed independently on EC2), **not** a Spring Cloud microservice mesh.

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

### AS → DC back-channel subscription provisioning (JSON)

At token-mint time the Authorization Server calls the Data Custodian to provision the
Authorization + Subscription(s) the customer approved on the DC-hosted Authorization Screen
(`POST {dc}/internal/backchannel/v1/subscriptions`, `201 Created` on success). This is an
**implementation** contract (not part of the ESPI standard). The introspection / token-response
contract above (#160) is the ESPI-standard companion; together they are "#150".

| File | Direction | Bound by |
|------|-----------|----------|
| `backchannel-subscription-request.json` | AS → DC request | AS consumer / DC provider |
| `backchannel-subscription-response.json` | DC → AS `201` response | AS consumer / DC provider |

- Request fields: `correlation_id`, `client_id`, `granted_scope` (ESPI `FB=...` grammar),
  `retail_customer_id`, `selected_usage_point_ids`.
- Response fields: `authorization_id`, `resource_subscription_id`, `customer_subscription_id`,
  `resource_uri`, `authorization_uri`, `customer_resource_uri`. The three `*_uri` fields are the
  canonical `Batch/...` / `Authorization/...` forms built by `EspiBatchUri`; the AS surfaces these
  (not the internal `*_id` fields) in the ESPI token/introspection body.
- **Provider** (DC `SubscriptionProvisioningController` / `SubscriptionProvisioningServiceImpl`) —
  `BackchannelWireContractTest` asserts DC consumes the request shape and produces the response
  shape (URIs via `EspiBatchUri`, scope via `EspiScope`).
- **Consumer** (AS `DataCustodianBackchannelClient`) — `DataCustodianBackchannelClientTest` asserts
  the client emits the request fixture and parses the response fixture.

> The AS keeps its own copy of these record DTOs (`BackchannelRequest`/`BackchannelResponse`) — the
> Authorization Server does not depend on `openespi-common`. The shared fixtures are what keep the
> two independent DTO copies in sync.

### DC → TP notification (BatchList, XML)

When new/updated data is available, the Data Custodian POSTs an ESPI `BatchList` document to
the Third Party's registered `thirdPartyNotifyUri` (`POST {tpBase}/espi/1_1/Notification`,
`Content-Type: application/atom+xml`, `200 OK` on success). The body lists the canonical
`Batch/...` resource URIs the TP should fetch back.

| File | Endpoint | Content-Type |
|------|----------|--------------|
| `notification-batchlist.xml` | `POST {tpBase}/espi/1_1/Notification` | `application/atom+xml` |

- **Producer** (DC `NotificationServiceImpl`) builds the resource URI via `EspiBatchUri`
  (`Batch/Subscription/{id}?published-min=…&published-max=…`) and serializes the `BatchListDto`
  through `BatchListXmlCodec` — the single canonical BatchList wire codec.
- **Consumer** (TP `NotificationController`) parses the document through the same
  `BatchListXmlCodec` and parses ids out of each resource URI via `EspiBatchUri`.

The wire type is the JAXB-annotated `BatchListDto` (root `<BatchList xmlns="http://naesb.org/espi">`,
repeating `<resources>` elements), never the JPA entity — per the project's JAXB/JPA separation rule.
