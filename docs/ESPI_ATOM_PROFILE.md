# ESPI Atom profile

ESPI resources are exchanged as Atom documents, "as described by Atom, IETF RFC 4287" (NAESB REQ.21 v4.1, REQ.21.6.2, p. 167). The HTTP method conventions come from the Atom Publishing Protocol, RFC 5023 (REQ.21.6.2.1, p. 169). RFC 5023 adds only its own `app:` namespace elements; Atom feeds and entries are as RFC 4287 defines them.

ESPI does not use RFC 4287 to the letter. It uses an **ESPI profile** of it, defined in practice by:

- the GBA `atom.xsd` used by the GBA DMD validator, copied unchanged to `openespi-common/src/main/resources/schema/ESPI_4.1/atom.xsd`;
- REQ.21's examples;
- the feeds of CMD-certified Data Custodians.

Every ESPI resource response, feed or entry, is served as `Content-Type: application/atom+xml`, and that is the only type the Data Custodian's resource endpoints produce: a request that accepts only `application/xml` or `application/json` gets 406, and a request without an `Accept` header gets Atom (#312).

This page lists the differences so that they are not "fixed" by mistake. **Do not change the code to follow RFC 4287 where this page says ESPI differs.** Doing so would either reveal which utility the data comes from, or make our feeds differ from every certified feed.

## Intentional differences

| # | ESPI (what we write and accept) | RFC 4287 | Why ESPI differs |
|---|---|---|---|
| 1 | No `atom:author` on feeds or entries | 4.1.1 / 4.1.2: a feed or each entry MUST have one or more `atom:author` | **Privacy.** An author would tie the data to a specific utility. espi.xsd, REQ.21's examples and certified feeds have none. |
| 2 | `<atom:content>` has no `type` attribute, and holds the ESPI resource (`espi:UsagePoint`, `cust:Customer`, ...) as its single child | 4.1.3.1: without `type`, content is treated as `type="text"`; 4.1.3.3 rule 1: text content MUST NOT contain child elements. An XML media type (`type="application/xml"`) would be needed. | REQ.21's examples, certified feeds and the DMD validator all use a bare `<content>`. Adding a `type` would make our feeds differ from all of them for no gain. |
| 3 | Every `atom:link` has `rel` and `type`; every entry has at least a `self` and an `up` link | 4.2.7.2 / 4.2.7.3: `rel` and `type` are optional; any number of links | Required by the GBA `atom.xsd`. ESPI navigation depends on `self`, `up` and `related` links, with types such as `espi-entry/UsagePoint` and `espi-feed/UsagePoint`. |

ESPI follows RFC 4287 where `atom.xsd` is looser. The code does not rely on that looseness:

- **No feed-level `atom:published`.** RFC 4287 4.1.1 defines none, and `atom.xsd` rejects it. Only entries have `published` (REQ.21.6.2: `published` = created, `updated` = last changed).
- **Dates carry a time zone** (RFC 4287 3.3). `OffsetDateTimeAdapter` always writes the offset or `Z`.

## What `atom.xsd` does not check

The schema uses `xs:choice maxOccurs="unbounded"` for feeds, entries and persons, so it **cannot** enforce these RFC 4287 rules:

- exactly one `id`, `title` and `updated` per feed and entry, and at most one `published` per entry;
- a time zone on dates (`xs:dateTime` allows a local time);
- entries after the feed's metadata.

`EspiXmlSchemaValidator` reports only what the schemas report, the same as the DMD validator. Checks for the rules above could be added as warnings. Never add checks for differences 1–3.

## Known issues in `atom.xsd`

These are GBA schema matters, not to be changed in our copy, which must stay identical to the DMD validator's:

- **Email pattern:** `atom:email` uses the pattern `\w+@(\w+\.)+\w+`, which rejects valid addresses such as `first.last@example.com` (RFC 4287 3.2.3). ESPI feeds carry no `author`, so this does not affect them.
- **`atom:source`:** an entry's `atom:source` is typed `textType`, while the `sourceType` the schema defines is unused, so a `source` holding feed metadata (RFC 4287 4.2.11) is rejected. ESPI does not use `source`.

## Open question: link `type` for related resources

REQ.21 p. 168 says related links "shall" use `type="application/atom+xml"`, for example `UsagePoint -> MeterReading`. Certified feeds, the DMD validator's samples and our Data Custodian use the ESPI form instead, `type="espi-entry/MeterReading"` or `espi-feed/...`.

`atom.xsd` accepts either, because `type` is any string, and our Third Party does not depend on the attribute. We follow certified practice until the GBA decides whether REQ.21 needs an erratum.

## Where this is implemented

| Concern | Code |
|---|---|
| Feed (no `author`, no feed `published`) | `openespi-common/.../dto/atom/AtomFeedDto.java` |
| Entry (resource inside a bare `atom:content`) | `UsageAtomEntryDto.java`, `CustomerAtomEntryDto.java` |
| Links (`type` on every link, also on an empty feed) | `openespi-common/.../service/BaseExportService.java` (`buildFeed`) |
| Validation against atom.xsd + espi.xsd + customer.xsd | `openespi-common/.../xml/EspiXmlSchemaValidator.java` |
| Media type (`application/atom+xml` only; the default without `Accept`) | `produces` on each `openespi-datacustodian/.../web/api/` controller; `WebConfiguration#configureContentNegotiation` |
| Tests | `AtomContentAndSchemaValidationTest`, `AtomFeedExportTest`, `EspiAtomMediaTypeIntegrationTest` |

History: #197 (the `atom:content` wrapper, the schema validator, the feed `published` and empty-feed link fixes), #247 (feed date adapters, entry types), #312 (the `application/atom+xml` media type).
