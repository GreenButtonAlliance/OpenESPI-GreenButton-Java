# Schema Compliance Remediation Plan

**Date**: 2025-12-30
**Issue**: Incorrect `related_links` tables for entities that don't extend `IdentifiedObject`
**Impact**: 11 incorrect related_links tables violating XSD schema compliance

---

## Executive Summary

The database schema currently includes 26 entity-specific related_links tables. According to ESPI 4.0 XSD analysis:
- ✅ **18 tables are correct** (15 exist + 3 missing: entities extend IdentifiedObject and use Atom rel="related")
- ❌ **11 tables are incorrect** (entities extend Object or use direct FK references instead of Atom links)
- 🎯 **Target**: Remove 11 incorrect tables and convert to proper JPA patterns
- 🎯 **Create**: 3 missing related_links tables (customer, meter, customer_account)

---

## Incorrect Related Links Tables to Remove

### Category 1: XSD Object-Based Entities (Should be @ElementCollection)

| # | Entity | Current Table | Parent Entity | XSD Reference |
|---|--------|--------------|---------------|---------------|
| 1 | IntervalReading | `interval_reading_related_links` | IntervalBlock | espi.xsd:1016 extends Object |
| 2 | ReadingQuality | `reading_quality_related_links` | IntervalReading | espi.xsd:1062 extends Object |
| 3 | PnodeRef | `pnode_ref_related_links` | UsagePoint | espi.xsd:1539 extends Object |
| 4 | AggregateNodeRef | `aggregated_node_ref_related_links` | UsagePoint | espi.xsd:1570 extends Object |
| 5 | LineItem | `line_item_related_links` | UsageSummary | espi.xsd:1444 extends Object |
| 6 | ServiceDeliveryPoint | `service_delivery_point_related_links` | UsagePoint | espi.xsd:1161 extends Object |
| 7 | StatementRef | `statement_ref_related_links` | Statement | customer.xsd:285 extends Object |

### Category 2: Custom Entities Using Direct FK References

| # | Entity | Current Table | Purpose | Rationale |
|---|--------|--------------|---------|-----------|
| 8 | RetailCustomer | `retail_customer_related_links` | Authentication/authorization bridge | Extends IdentifiedObject but uses direct FK references for marshalling, not Atom rel="related" |
| 9 | Subscription | `subscription_related_links` | OAuth2 access token authorization | Extends IdentifiedObject but uses direct FK references for marshalling, not Atom rel="related" |

**Special Note**: RetailCustomer and Subscription extend IdentifiedObject (base class) but don't use Atom feed patterns. They use direct foreign key relationships (applicationinformation_id, authorization_id, retail_customer_id) for data marshalling instead of related_links tables.

### Category 3: Element Wrappers (Not Entities)

| # | Entity | Current Table | XSD Type | Rationale |
|---|--------|--------------|----------|-----------|
| 10 | PhoneNumber | `phone_number_related_links` | NOT in XSD | Custom addition, polymorphic collection |
| 11 | BatchList | `batch_list_related_links` | BatchListType (sequence) | espi.xsd:1432 - wrapper type, not IdentifiedObject |

---

## Remediation Strategy

### Phase 1: IntervalReading (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `interval_readings` with UUID primary key
- Table: `interval_reading_related_links`
- Relationship: IntervalBlock @OneToMany IntervalReading

**Target State:**
- Remove `interval_reading_related_links` table
- Keep `interval_readings` table (needed for collection storage)
- Remove Atom link fields from IntervalReading entity (selfLink, upLink)
- Verify JPA mapping uses @OneToMany with proper cascade

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `interval_reading_related_links`)

**Java Files:**
- `IntervalReadingEntity.java` - Remove IdentifiedObject fields if present
- `IntervalBlockEntity.java` - Verify @OneToMany mapping
- `IntervalReadingDto.java` - Remove Atom link elements
- `IntervalReadingMapper.java` - Update mappings

**Rationale**: IntervalReading extends Object in espi.xsd:1016, not IdentifiedObject. It's a child element collection of IntervalBlock.

---

### Phase 2: ReadingQuality (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `reading_qualities` with UUID primary key
- Table: `reading_quality_related_links`
- Relationship: IntervalReading @OneToMany ReadingQuality

**Target State:**
- Remove `reading_quality_related_links` table
- Keep `reading_qualities` table (needed for collection storage)
- Remove Atom link fields from ReadingQuality entity
- Verify JPA mapping uses @OneToMany with proper cascade

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `reading_quality_related_links`)

**Java Files:**
- `ReadingQualityEntity.java` - Remove IdentifiedObject fields if present
- `IntervalReadingEntity.java` - Verify @OneToMany mapping
- `ReadingQualityDto.java` - Remove Atom link elements
- `ReadingQualityMapper.java` - Update mappings

**Rationale**: ReadingQuality extends Object in espi.xsd:1062, not IdentifiedObject. It's a child element collection of IntervalReading.

---

### Phase 3: PnodeRef (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `pnode_refs` with UUID primary key
- Table: `pnode_ref_related_links`
- Relationship: UsagePoint @OneToMany PnodeRef

**Target State:**
- Remove `pnode_ref_related_links` table
- Keep `pnode_refs` table (needed for collection storage)
- Remove Atom link fields from PnodeRef entity
- Verify JPA mapping uses @OneToMany with proper cascade

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `pnode_ref_related_links`)

**Java Files:**
- `PnodeRefEntity.java` - Remove IdentifiedObject fields if present
- `UsagePointEntity.java` - Verify @OneToMany mapping
- `PnodeRefDto.java` - Remove Atom link elements
- `PnodeRefMapper.java` - Update mappings

**Rationale**: PnodeRef extends Object in espi.xsd:1539, not IdentifiedObject. It's a reference element collection within UsagePoint.

---

### Phase 4: AggregateNodeRef (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `aggregated_node_refs` with UUID primary key
- Table: `aggregated_node_ref_related_links`
- Relationship: UsagePoint @OneToMany AggregateNodeRef

**Target State:**
- Remove `aggregated_node_ref_related_links` table
- Keep `aggregated_node_refs` table (needed for collection storage)
- Remove Atom link fields from AggregateNodeRef entity
- Verify JPA mapping uses @OneToMany with proper cascade

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `aggregated_node_ref_related_links`)

**Java Files:**
- `AggregateNodeRefEntity.java` - Remove IdentifiedObject fields if present
- `UsagePointEntity.java` - Verify @OneToMany mapping
- `AggregateNodeRefDto.java` - Remove Atom link elements
- `AggregateNodeRefMapper.java` - Update mappings

**Rationale**: AggregateNodeRef extends Object in espi.xsd:1570, not IdentifiedObject. It's a reference element collection within UsagePoint.

---

### Phase 5: LineItem (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `line_items` with UUID primary key
- Table: `line_item_related_links`
- Relationship: UsageSummary @OneToMany LineItem

**Target State:**
- Remove `line_item_related_links` table
- Keep `line_items` table (needed for collection storage)
- Remove Atom link fields from LineItem entity
- Verify JPA mapping uses @OneToMany with proper cascade

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `line_item_related_links`)

**Java Files:**
- `LineItemEntity.java` - Remove IdentifiedObject fields if present
- `UsageSummaryEntity.java` - Verify @OneToMany mapping
- `LineItemDto.java` - Remove Atom link elements
- `LineItemMapper.java` - Update mappings

**Rationale**: LineItem extends Object in espi.xsd:1444, not IdentifiedObject. It's a child element collection of UsageSummary (costAdditionalDetailLastPeriod).

---

### Phase 6: ServiceDeliveryPoint (Remove related_links, verify @OneToOne)

**Current State:**
- Table: `service_delivery_points` with UUID primary key
- Table: `service_delivery_point_related_links`
- Relationship: UsagePoint @OneToOne ServiceDeliveryPoint

**Target State:**
- Remove `service_delivery_point_related_links` table
- Keep `service_delivery_points` table (needed as separate entity)
- Remove Atom link fields from ServiceDeliveryPoint entity
- Verify JPA mapping uses @OneToOne (or embed as @Embedded if appropriate)

**Migration Files:**
- V1__Create_Base_Tables.sql (lines with `service_delivery_point_related_links`)

**Java Files:**
- `ServiceDeliveryPointEntity.java` - Remove IdentifiedObject fields if present
- `UsagePointEntity.java` - Verify @OneToOne mapping
- `ServiceDeliveryPointDto.java` - Remove Atom link elements
- `ServiceDeliveryPointMapper.java` - Update mappings

**Rationale**: ServiceDeliveryPoint extends Object in espi.xsd:1161, not IdentifiedObject. It's a non-navigable reference object.

**Note**: ServiceDeliveryPoint is referenced by UsagePoint but doesn't have independent lifecycle - consider embedding if it's truly value-object behavior.

---

### Phase 7: StatementRef (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `statement_refs` with UUID primary key
- Table: `statement_ref_related_links`
- Relationship: Statement @OneToMany StatementRef

**Target State:**
- Remove `statement_ref_related_links` table
- Keep `statement_refs` table (needed for collection storage)
- Remove Atom link fields from StatementRef entity
- Verify JPA mapping uses @OneToMany with proper cascade

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `statement_ref_related_links`)

**Java Files:**
- `StatementRefEntity.java` - Remove IdentifiedObject fields if present
- `StatementEntity.java` - Verify @OneToMany mapping
- `StatementRefDto.java` - Remove Atom link elements
- `StatementRefMapper.java` - Update mappings

**Rationale**: StatementRef extends Object in customer.xsd:285, not IdentifiedObject. It's a document reference collection within Statement.

---

### Phase 8: PhoneNumber (Remove related_links, convert to @ElementCollection)

**Current State:**
- Table: `phone_numbers` with UUID primary key
- Table: `phone_number_related_links`
- Polymorphic relationship: Multiple parents (Customer, ServiceSupplier, etc.)

**Target State:**
- Remove `phone_number_related_links` table
- Keep `phone_numbers` table with polymorphic parent tracking
- Remove Atom link fields from PhoneNumber entity
- Maintain `parent_entity_uuid` and `parent_entity_type` for polymorphic association

**Migration Files:**
- V3__Create_additiional_Base_Tables.sql (lines with `phone_number_related_links`)

**Java Files:**
- `PhoneNumberEntity.java` - Remove IdentifiedObject fields if present
- Parent entities - Verify @ElementCollection or @OneToMany mappings
- `PhoneNumberDto.java` - Remove Atom link elements
- `PhoneNumberMapper.java` - Update mappings

**Rationale**: PhoneNumber is not in ESPI XSD - it's a custom addition. Polymorphic ownership suggests it's a value object collection.

---

### Phase 9: RetailCustomer (Remove related_links, verify API-only usage)

**Current State:**
- Table: `retail_customers` with UUID primary key
- Table: `retail_customer_related_links`
- Custom entity for API representation

**Target State:**
- Remove `retail_customer_related_links` table
- Keep `retail_customers` table (needed for API operations)
- Remove Atom link fields from RetailCustomer entity (if present)
- Verify entity is used only in API layer, not as ESPI resource

**Migration Files:**
- V1__Create_Base_Tables.sql (lines with `retail_customer_related_links`)

**Java Files:**
- `RetailCustomerEntity.java` - Verify it doesn't extend IdentifiedObject
- Services/Controllers - Verify API-only usage
- DTOs - Ensure no ESPI Atom link serialization

**Rationale**: RetailCustomer is a custom API entity representing utility customers, not an ESPI IdentifiedObject resource.

---

### Phase 10: Subscription (Remove related_links, verify API-only usage)

**Current State:**
- Table: `subscriptions` with UUID primary key
- Table: `subscription_related_links`
- Custom entity for OAuth2 authorization tracking

**Target State:**
- Remove `subscription_related_links` table
- Keep `subscriptions` table (needed for OAuth2 token tracking)
- Remove Atom link fields from Subscription entity (if present)
- Verify entity is used only for authorization, not as ESPI resource

**Migration Files:**
- V1__Create_Base_Tables.sql (lines with `subscription_related_links`)

**Java Files:**
- `SubscriptionEntity.java` - Verify it doesn't extend IdentifiedObject
- Services - Verify OAuth2/authorization usage only
- DTOs - Ensure no ESPI Atom link serialization

**Rationale**: Subscription is a custom API entity representing OAuth2 access grants, not an ESPI IdentifiedObject resource.

---

### Phase 11: BatchList (Remove related_links, verify wrapper usage)

**Current State:**
- Table: `batch_lists` with UUID primary key
- Table: `batch_list_related_links`
- Element wrapper for batch operations

**Target State:**
- Remove `batch_list_related_links` table
- Evaluate if `batch_lists` table is needed or if BatchList should be transient
- BatchListType in XSD is just a sequence of URIs - may not need persistence

**Migration Files:**
- V1__Create_Base_Tables.sql (lines with `batch_list_related_links`)

**Java Files:**
- `BatchListEntity.java` - Verify usage pattern
- Evaluate if BatchList should be @Transient or removed entirely
- DTOs - BatchListType is just URI collection wrapper

**Rationale**: BatchListType extends Object in espi.xsd:1432 - it's a sequence wrapper, not an IdentifiedObject. May not require persistence.

---

## Implementation Phases

### Phase A: Analysis & Validation (1 branch, 1 PR)

**Branch**: `fix/schema-compliance-analysis`

**Tasks**:
1. Read all 11 entity Java files to document current IdentifiedObject usage
2. Read all 11 DTO files to document Atom link serialization
3. Identify all Flyway migration files requiring updates
4. Create detailed inventory of changes required
5. Update FLYWAY_SCHEMA_SUMMARY.md with findings
6. Create migration script templates for each phase

**Deliverable**: Comprehensive analysis document + migration templates

---

### Phase B: Remove Related Links Tables - Batch 1 (Collections)

**Branch**: `fix/schema-compliance-batch1-collections`

**Entities**: IntervalReading, ReadingQuality, PnodeRef, AggregateNodeRef, LineItem, StatementRef, PhoneNumber (7 entities)

**Tasks**:
1. Update Flyway scripts to DROP related_links tables
2. Remove IdentifiedObject inheritance from entities (if present)
3. Remove Atom link fields (selfLink, upLink, relatedLinks)
4. Update DTOs to remove Atom link elements
5. Update MapStruct mappers
6. Update tests to remove Atom link assertions
7. Verify parent entity @OneToMany/@ElementCollection mappings

**Migration Strategy**:
- Create V4__Remove_Collection_Related_Links.sql
- DROP TABLE statements for 7 related_links tables
- Add comments explaining XSD compliance rationale

**Testing**:
- Unit tests for entities (verify no Atom links)
- Integration tests with TestContainers
- Verify XML marshalling excludes Atom links for these entities

---

### Phase C: Remove Related Links Tables - Batch 2 (API Entities)

**Branch**: `fix/schema-compliance-batch2-api-entities`

**Entities**: RetailCustomer, Subscription (2 entities)

**Tasks**:
1. Update Flyway scripts to DROP related_links tables
2. Verify entities don't extend IdentifiedObject
3. Remove Atom link fields if present
4. Update services to ensure API-only usage
5. Update tests

**Migration Strategy**:
- Create V5__Remove_API_Entity_Related_Links.sql
- DROP TABLE statements for 2 related_links tables
- Add comments explaining custom entity rationale

**Testing**:
- Verify OAuth2 authorization flows still work
- Verify API endpoints for RetailCustomer operations
- Integration tests

---

### Phase D: Remove Related Links Tables - Batch 3 (Special Cases)

**Branch**: `fix/schema-compliance-batch3-special-cases`

**Entities**: ServiceDeliveryPoint, BatchList (2 entities)

**Tasks**:
1. ServiceDeliveryPoint: Evaluate @OneToOne vs @Embedded
2. BatchList: Evaluate if table needed at all (may be transient wrapper)
3. Update Flyway scripts accordingly
4. Update entities, DTOs, mappers
5. Update tests

**Migration Strategy**:
- Create V6__Remove_Special_Case_Related_Links.sql
- DROP TABLE statements for 2 related_links tables
- Add architectural notes for ServiceDeliveryPoint vs embedding decision
- Add notes for BatchList persistence necessity

**Testing**:
- ServiceDeliveryPoint relationship tests
- BatchList operations (if kept)
- XML marshalling tests

---

### Phase E: Update MULTI_PHASE Plan & Documentation

**Branch**: `docs/schema-compliance-updates`

**Tasks**:
1. Update MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md:
   - Remove IntervalReading, ReadingQuality phases (not IdentifiedObject)
   - Remove PnodeRef, AggregateNodeRef phases (not IdentifiedObject)
   - Remove LineItem phase (not IdentifiedObject)
   - Remove ServiceDeliveryPoint phase (not IdentifiedObject)
   - Update phase numbering accordingly
   - Add notes about entities being collections
2. Update FLYWAY_SCHEMA_SUMMARY.md:
   - Remove 11 incorrect related_links tables from documentation
   - Update table counts
   - Add section explaining Object vs IdentifiedObject distinction
3. Update SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md with completion status
4. Update Entity-to-Migration Quick Reference table

**Deliverable**: Updated documentation reflecting corrected schema

---

## Flyway Migration Updates

### Migration Files to Update

| File | Tables to Remove | Line Ranges |
|------|-----------------|-------------|
| `V1__Create_Base_Tables.sql` | `retail_customer_related_links`, `subscription_related_links`, `batch_list_related_links`, `service_delivery_point_related_links` | TBD after analysis |
| `V3__Create_additiional_Base_Tables.sql` | `interval_reading_related_links`, `reading_quality_related_links`, `pnode_ref_related_links`, `aggregated_node_ref_related_links`, `line_item_related_links`, `statement_ref_related_links`, `phone_number_related_links` | TBD after analysis |

**Strategy**:
- Since project is in development (not deployed), we can UPDATE existing migrations
- Remove CREATE TABLE statements for related_links tables
- Keep main entity tables (still needed for collections)
- Add comments explaining why related_links removed per XSD compliance

### New Migration Files

Create DROP TABLE migrations for production environments that have already run V1-V3:

```sql
-- V4__Remove_Collection_Related_Links.sql
-- Remove related_links tables for entities extending Object, not IdentifiedObject

DROP TABLE IF EXISTS interval_reading_related_links;
DROP TABLE IF EXISTS reading_quality_related_links;
DROP TABLE IF EXISTS pnode_ref_related_links;
DROP TABLE IF EXISTS aggregated_node_ref_related_links;
DROP TABLE IF EXISTS line_item_related_links;
DROP TABLE IF EXISTS statement_ref_related_links;
DROP TABLE IF EXISTS phone_number_related_links;

-- Rationale: These entities extend Object in ESPI XSD, not IdentifiedObject
-- Atom related links are only applicable to IdentifiedObject resources
```

---

## Testing Strategy

### Unit Tests
- Verify entities no longer have Atom link fields
- Verify DTOs don't include Atom link elements
- Verify MapStruct mappings compile and function

### Integration Tests (TestContainers)
- Verify parent-child relationships work (e.g., IntervalBlock → IntervalReading)
- Verify cascade operations
- Verify no orphaned related_links records

### XML Marshalling Tests
- Verify JAXB marshalling excludes Atom links for Object-based entities
- Verify JAXB unmarshalling handles missing links gracefully
- Validate against espi.xsd and customer.xsd

### API Tests
- RetailCustomer endpoints still functional
- Subscription/OAuth2 flows still functional
- BatchList operations (if applicable)

---

## Success Criteria

✅ All 11 incorrect related_links tables removed from database
✅ All entities properly mapped to @ElementCollection or appropriate pattern
✅ All Flyway migrations updated (existing V1/V3 + new V4-V6)
✅ All unit tests passing
✅ All integration tests passing (MySQL, PostgreSQL, H2)
✅ XML marshalling compliant with ESPI XSD schema
✅ MULTI_PHASE plan updated with corrected phase list
✅ Documentation updated (FLYWAY_SCHEMA_SUMMARY.md)

---

## Rollback Plan

If issues arise during implementation:

1. **Code Rollback**: Revert to previous Git commit
2. **Database Rollback**: Flyway supports repair and undo (if configured)
3. **Incremental Deployment**: Each phase is isolated in separate PR - can pause between phases
4. **Testing Gates**: No phase proceeds without passing integration tests

---

## Estimated Scope

| Phase | Entities | PRs | Estimated Complexity |
|-------|----------|-----|---------------------|
| Phase A | 11 | 1 | Medium - Analysis |
| Phase B | 7 | 1 | High - Bulk refactoring |
| Phase C | 2 | 1 | Low - Simple cleanup |
| Phase D | 2 | 1 | Medium - Special cases |
| Phase E | N/A | 1 | Low - Documentation |
| **Total** | **11** | **5** | **~3-5 days implementation** |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing functionality | High | Comprehensive testing before each PR |
| Data loss in production | High | Project is in development, not deployed yet |
| XML serialization breaks | Medium | XSD validation tests for all entities |
| Integration test failures | Medium | TestContainers for MySQL/PostgreSQL/H2 |
| Incomplete migration | Low | Phased approach with testing gates |

---

## Next Steps

1. **User Approval**: Review and approve this plan
2. **Phase A**: Create analysis branch and document current state
3. **Phase B-D**: Implement fixes in batches with PRs
4. **Phase E**: Update documentation
5. **Final Review**: Comprehensive testing and validation

---

**Document Version**: 1.0
**Last Updated**: 2025-12-30
**Status**: Awaiting Approval
