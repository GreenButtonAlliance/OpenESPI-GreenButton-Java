# MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN Updates

**Date**: 2025-12-30
**Reason**: Remove phases for entities that don't extend IdentifiedObject per XSD analysis
**ESPI Version**: NAESB ESPI 4.0

---

## Phases to Remove (8 total)

### Category 1: XSD Object-Based Entities (6 phases)

These entities extend `Object` in ESPI XSD, not `IdentifiedObject`. They should be collections or embedded objects, NOT independent IdentifiedObject resources with Atom links.

| Phase # | Entity | XSD Reference | Reason for Removal |
|---------|--------|---------------|-------------------|
| **3** | IntervalReading | espi.xsd:1016 extends Object | Collection child of IntervalBlock, not independent resource |
| **4** | ReadingQuality | espi.xsd:1062 extends Object | Collection child of IntervalReading, not independent resource |
| **7** | ServiceDeliveryPoint | espi.xsd:1161 extends Object | Referenced by UsagePoint but not IdentifiedObject |
| **8** | PnodeRef | espi.xsd:1539 extends Object | Collection child of UsagePoint, not independent resource |
| **9** | AggregateNodeRef | espi.xsd:1570 extends Object | Collection child of UsagePoint, not independent resource |
| **10** | LineItem | espi.xsd:1444 extends Object | Collection child of UsageSummary, not independent resource |

### Category 2: Element Wrappers (1 phase)

| Phase # | Entity | Reason for Removal |
|---------|--------|-------------------|
| **15** | BatchList | BatchListType is sequence wrapper (espi.xsd:1432), not IdentifiedObject entity |

### Category 3: Abstract Base Classes (1 phase)

| Phase # | Entity | Reason for Removal |
|---------|--------|-------------------|
| **22** | Asset | Abstract base class (customer.xsd:643), only concrete subclasses (EndDevice, Meter) are instantiated |

**Note**: Asset extends IdentifiedObject but is never instantiated directly. EndDevice extends AssetContainer extends Asset. Only EndDevice and Meter are concrete entities.

---

## Phases to Keep (18 total)

All remaining phases represent **concrete** entities that properly extend `IdentifiedObject` per ESPI 4.0 XSD:

### Usage Domain (8 phases)

| New # | Old # | Entity | XSD Reference | Extends IdentifiedObject |
|-------|-------|--------|---------------|-------------------------|
| 1 | 1 | TimeConfiguration | espi.xsd:940 | ✅ Direct |
| 2 | 2 | ReadingType | espi.xsd:382 | ✅ Direct |
| 3 | 5 | IntervalBlock | espi.xsd:353 | ✅ Direct |
| 4 | 6 | MeterReading | espi.xsd:374 | ✅ Direct |
| 5 | 11 | UsageSummary | espi.xsd:806 | ✅ Direct (replaces deprecated ElectricPowerUsageSummary) |
| 6 | 12 | ElectricPowerQualitySummary | espi.xsd:614 | ✅ Direct |
| 7 | 16 | UsagePoint | espi.xsd:486 | ✅ Direct |
| 8 | 17 | ProgramDateIdMappings | customer.xsd:269 | ✅ Direct |

### Customer Domain (8 phases)

| New # | Old # | Entity | XSD Reference | Extends IdentifiedObject |
|-------|-------|--------|---------------|-------------------------|
| 9 | 18 | CustomerAccount | customer.xsd:118 → Document | ✅ Via Document |
| 10 | 19 | Statement | customer.xsd:373 | ✅ Direct |
| 11 | 20 | Customer | customer.xsd:67 → OrganisationRole | ✅ Via OrganisationRole |
| 12 | 21 | ServiceSupplier | customer.xsd:347 → OrganisationRole | ✅ Via OrganisationRole |
| 13 | 23 | ServiceLocation | customer.xsd:311 → WorkLocation → Location | ✅ Via Location |
| 14 | 24 | CustomerAgreement | customer.xsd:159 → Agreement → Document | ✅ Via Document |
| 15 | 25 | EndDevice | customer.xsd:210 → AssetContainer → Asset | ✅ Via Asset (concrete) |
| 16 | 26 | Meter | customer.xsd:243 → EndDevice | ✅ Via EndDevice (concrete) |

### Custom Domain (2 phases - No related_links tables)

| New # | Old # | Entity | Schema Columns | Extends IdentifiedObject |
|-------|-------|--------|----------------|-------------------------|
| 17 | 13 | RetailCustomer | first_name, last_name, username, password, role, enabled | ✅ Direct (custom) |
| 18 | 14 | Subscription | applicationinformation_id, authorization_id, retail_customer_id | ✅ Direct (custom) |

**Note**: RetailCustomer and Subscription are custom entities not defined in ESPI XSD. They extend IdentifiedObject but do NOT use Atom rel="related" links - they use direct foreign key references for data marshalling, so they do NOT require related_links tables.

---

## Renumbering Strategy

### Before (26 phases)
```
1. TimeConfiguration ✅
2. ReadingType ✅
3. IntervalReading ❌ REMOVE
4. ReadingQuality ❌ REMOVE
5. IntervalBlock ✅
6. MeterReading ✅
7. ServiceDeliveryPoint ❌ REMOVE
8. PnodeRef ❌ REMOVE
9. AggregateNodeRef ❌ REMOVE
10. LineItem ❌ REMOVE
11. UsageSummary ✅
12. ElectricPowerQualitySummary ✅
13. RetailCustomer ✅ (Custom)
14. Subscription ✅ (Custom)
15. BatchList ❌ REMOVE
16. UsagePoint ✅
17. ProgramDateIdMappings ✅
18. CustomerAccount ✅
19. Statement ✅
20. Customer ✅
21. ServiceSupplier ✅
22. Asset ❌ REMOVE (abstract)
23. ServiceLocation ✅
24. CustomerAgreement ✅
25. EndDevice ✅
26. Meter ✅
```

### After (18 phases)
```
1. TimeConfiguration
2. ReadingType
3. IntervalBlock (was 5)
4. MeterReading (was 6)
5. UsageSummary (was 11)
6. ElectricPowerQualitySummary (was 12)
7. UsagePoint (was 16)
8. ProgramDateIdMappings (was 17)
9. CustomerAccount (was 18)
10. Statement (was 19)
11. Customer (was 20)
12. ServiceSupplier (was 21)
13. ServiceLocation (was 23)
14. CustomerAgreement (was 24)
15. EndDevice (was 25)
16. Meter (was 26)
17. RetailCustomer (was 13) - Custom
18. Subscription (was 14) - Custom
```

---

## Impact on Entity-to-Migration Quick Reference Table

The Quick Reference table will grow from 26 rows to 18 rows, removing entries for 8 non-IdentifiedObject entities but retaining RetailCustomer and Subscription as custom IdentifiedObject entities:

**Removed Entries** (8 total):
- IntervalReading
- ReadingQuality
- ServiceDeliveryPoint
- PnodeRef
- AggregateNodeRef
- LineItem
- BatchList
- Asset (abstract base class)

**Retained Custom Entities** (2 total):
- RetailCustomer (extends IdentifiedObject)
- Subscription (extends IdentifiedObject)

---

## Impact on Flyway Migration Strategy

### Related Links Tables Count

**Before**: 26 entity-specific related_links tables (including 11 incorrect)

**After**: 18 entity-specific related_links tables (15 existing + 3 to create, removed 11 incorrect)

**Removed Tables**:
1. `interval_reading_related_links`
2. `reading_quality_related_links`
3. `service_delivery_point_related_links`
4. `pnode_ref_related_links`
5. `aggregated_node_ref_related_links`
6. `line_item_related_links`
7. `retail_customer_related_links`
8. `subscription_related_links`
9. `batch_list_related_links`
10. `statement_ref_related_links`
11. `phone_number_related_links`

**Notes**:
- Although Asset extends IdentifiedObject in XSD, it is abstract and never instantiated directly
- RetailCustomer and Subscription extend IdentifiedObject but use direct FK references instead of Atom rel="related" links
- Meter and EndDevice are separate ESPI resources requiring separate related_links tables

---

## Correct Related Links Tables (15 existing + 3 missing = 18 total)

| # | Table | Entity | Migration File | Status |
|---|-------|--------|----------------|--------|
| 1 | `application_information_related_links` | ApplicationInformation | V1 | ✅ Exists |
| 2 | `authorization_related_links` | Authorization | V1 | ✅ Exists |
| 3 | `reading_type_related_links` | ReadingType | V1 | ✅ Exists |
| 4 | `time_configuration_related_links` | TimeConfiguration | V2 (vendor-specific) | ✅ Exists |
| 5 | `usage_point_related_links` | UsagePoint | V2 (vendor-specific) | ✅ Exists |
| 6 | `meter_reading_related_links` | MeterReading | V3 | ✅ Exists |
| 7 | `interval_block_related_links` | IntervalBlock | V3 | ✅ Exists |
| 8 | `usage_summary_related_links` | UsageSummary | V3 | ✅ Exists |
| 9 | `electric_power_quality_summary_related_links` | ElectricPowerQualitySummary | V3 | ✅ Exists |
| 10 | `program_date_id_mapping_related_links` | ProgramDateIdMappings | V3 | ✅ Exists |
| 11 | `customer_agreement_related_links` | CustomerAgreement | V3 | ✅ Exists |
| 12 | `service_supplier_related_links` | ServiceSupplier | V3 | ✅ Exists |
| 13 | `service_location_related_links` | ServiceLocation | V3 | ✅ Exists |
| 14 | `end_device_related_links` | EndDevice | V3 | ✅ Exists |
| 15 | `statement_related_links` | Statement | V3 | ✅ Exists |
| 16 | `customer_related_links` | Customer | V3 | ⚠️ **MISSING** |
| 17 | `meter_related_links` | Meter | V3 | ⚠️ **MISSING** |
| 18 | `customer_account_related_links` | CustomerAccount | V3 | ⚠️ **MISSING** |

---

## Next Steps

1. ✅ Create SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md
2. ✅ Create MULTI_PHASE_PLAN_UPDATES.md
3. ⏳ Update MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md:
   - Remove 10 phases
   - Renumber remaining 16 phases
   - Update Entity-to-Migration Quick Reference table
   - Add note explaining Object vs IdentifiedObject distinction
4. ⏳ Update FLYWAY_SCHEMA_SUMMARY.md:
   - Update related_links table count (26 → 15)
   - Remove references to deleted tables
   - Add section explaining compliance corrections
   - Clarify ElectricPowerQualitySummary related_links status

---

**Document Version**: 1.2
**Status**: Approved for implementation
**Note**: ElectricPowerUsageSummary is DEPRECATED in ESPI 3.2+, replaced by UsageSummary in ESPI 4.0
