# Option A Implementation Plan: Domain-Specific Export Services

## Executive Summary

Refactor `DtoExportServiceImpl` into two specialized export services (`UsageExportService` and `CustomerExportService`) to achieve proper namespace isolation and enable JAXB 3.x to reliably use Atom as the default namespace.

**Goal:** Each service manages only 2 namespaces (Atom + domain), allowing JAXB 3.x to predictably assign Atom as default.

---

## Current State Analysis

### Existing Architecture

```
DtoExportServiceImpl
├── JAXBContext with ALL classes (Atom + Usage + Customer)
├── determineRequiredNamespaces() - detects domain
├── createMarshaller() - configures JAXB
└── Uses EspiNamespacePrefixMapper

Problem: 3 namespaces confuse JAXB 3.x default selection
```

### Current Namespace Behavior

| Domain | Expected | Actual |
|--------|----------|--------|
| Customer | `xmlns="http://www.w3.org/2005/Atom"` | ✅ Works |
| Usage | `xmlns="http://www.w3.org/2005/Atom"` | ❌ `xmlns:ns3="..."` |

**Root Cause:** JAXB 3.x cannot predict default namespace with 3+ schemas in context.

---

## Target Architecture

### Service Structure

```
BaseExportService (abstract)
├── Common marshaller configuration
├── EspiNamespacePrefixMapper setup
└── XML header handling

UsageExportService extends BaseExportService
├── JAXBContext: Atom + Usage domain only
├── Exports: UsagePoint, MeterReading, IntervalBlock, etc.
└── Namespace: xmlns="Atom" xmlns:espi="..."

CustomerExportService extends BaseExportService
├── JAXBContext: Atom + Customer domain only
├── Exports: Customer, CustomerAccount, ServiceLocation, etc.
└── Namespace: xmlns="Atom" xmlns:cust="..."

DtoExportServiceFacade (new)
├── Delegates to UsageExportService or CustomerExportService
├── Domain detection logic
└── Backwards compatibility layer
```

---

## Implementation Phases

## Phase 1: Extract Common Logic (Foundation)

**Goal:** Create reusable base class with shared marshaller configuration.

### 1.1 Create `BaseExportService` Abstract Class

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/BaseExportService.java`

**Responsibilities:**
- Abstract JAXBContext creation (subclasses implement)
- Common marshaller configuration
- EspiNamespacePrefixMapper setup
- XML header constants
- Stream writing utilities

**Key Methods:**
```java
public abstract class BaseExportService {
    protected static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n...";

    // Subclasses must implement
    protected abstract JAXBContext getJAXBContext() throws JAXBException;
    protected abstract Set<String> getDomainNamespaces();

    // Common implementation
    protected Marshaller createMarshaller() throws JAXBException {
        JAXBContext context = getJAXBContext();
        Marshaller marshaller = context.createMarshaller();

        // Apply standard properties
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);

        // Apply namespace prefix mapper
        Set<String> namespaces = new HashSet<>(getDomainNamespaces());
        namespaces.add("http://www.w3.org/2005/Atom"); // Always include Atom

        EspiNamespacePrefixMapper prefixMapper = new EspiNamespacePrefixMapper(namespaces);
        try {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", prefixMapper);
        } catch (PropertyException e) {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", prefixMapper);
        }

        return marshaller;
    }

    protected void exportDto(Object dto, OutputStream stream) throws JAXBException {
        Marshaller marshaller = createMarshaller();
        marshaller.marshal(dto, stream);
    }
}
```

**Files to Create:**
- `BaseExportService.java`

**Dependencies:**
- `EspiNamespacePrefixMapper` (existing)
- Jakarta XML Binding APIs

---

## Phase 2: Create UsageExportService

**Goal:** Export service handling only Atom + Usage domain namespaces.

### 2.1 Create `UsageExportService`

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/UsageExportService.java`

**Responsibilities:**
- Initialize JAXBContext with ONLY Atom + Usage domain classes
- Export UsagePoint, MeterReading, IntervalBlock, etc.
- Ensure namespace isolation (no customer namespace)

**JAXBContext Classes:**
```java
@Service
@Slf4j
public class UsageExportService extends BaseExportService {

    private JAXBContext jaxbContext;

    @PostConstruct
    public void init() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(
            // Atom protocol (usage-specific entry)
            org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto.class,

            // Usage domain classes ONLY (http://naesb.org/espi)
            org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AuthorizationDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.SubscriptionDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.BatchListDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.LineItemDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ServiceDeliveryPointDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.ReadingQualityDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.IntervalReadingDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.DateTimeIntervalDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.TariffRiderRefsDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.PnodeRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.PnodeRefsDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AggregatedNodeRefDto.class,
            org.greenbuttonalliance.espi.common.dto.usage.AggregatedNodeRefsDto.class

            // NO customer domain classes
        );
    }

    @Override
    protected JAXBContext getJAXBContext() {
        return jaxbContext;
    }

    @Override
    protected Set<String> getDomainNamespaces() {
        return Set.of("http://naesb.org/espi");
    }

    // Public API methods
    public void exportUsagePointEntry(UsagePointDto usagePoint, OutputStream stream)
            throws JAXBException {
        UsageAtomEntryDto entry = createUsageAtomEntry("Usage Point", usagePoint);
        exportDto(entry, stream);
    }

    public void exportUsagePointsFeed(List<UsagePointDto> usagePoints, OutputStream stream)
            throws JAXBException {
        List<AtomEntryDto> entries = usagePoints.stream()
            .map(dto -> createUsageAtomEntry("Usage Point", dto))
            .collect(Collectors.toList());

        AtomFeedDto feed = new AtomFeedDto(
            UUID.randomUUID().toString(),
            "Usage Points",
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            entries
        );

        exportDto(feed, stream);
    }

    private UsageAtomEntryDto createUsageAtomEntry(String title, Object resource) {
        OffsetDateTime now = OffsetDateTime.now();
        String entryId = "urn:uuid:" + UUID.randomUUID();
        return new UsageAtomEntryDto(entryId, title, now, now, null, resource);
    }
}
```

**Expected XML Output:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:espi="http://naesb.org/espi">
    <id>urn:uuid:...</id>
    <title>Usage Point</title>
    <published>2025-01-21T...</published>
    <updated>2025-01-21T...</updated>
    <espi:UsagePoint>
        <espi:roleFlags>01</espi:roleFlags>
        <espi:status>1</espi:status>
    </espi:UsagePoint>
</entry>
```

**Files to Create:**
- `UsageExportService.java`

---

## Phase 3: Create CustomerExportService

**Goal:** Export service handling only Atom + Customer domain namespaces.

### 3.1 Create `CustomerExportService`

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/CustomerExportService.java`

**Responsibilities:**
- Initialize JAXBContext with ONLY Atom + Customer domain classes
- Export Customer, CustomerAccount, ServiceLocation, etc.
- Ensure namespace isolation (no usage namespace)

**JAXBContext Classes:**
```java
@Service
@Slf4j
public class CustomerExportService extends BaseExportService {

    private JAXBContext jaxbContext;

    @PostConstruct
    public void init() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(
            // Atom protocol (customer-specific entry)
            org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.LinkDto.class,
            org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto.class,

            // Customer domain classes ONLY (http://naesb.org/espi/customer)
            org.greenbuttonalliance.espi.common.dto.customer.CustomerDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.CustomerAgreementDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.EndDeviceDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.MeterDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingsDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.ServiceLocationDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatementDto.class,
            org.greenbuttonalliance.espi.common.dto.customer.StatementRefDto.class

            // NO usage domain classes
        );
    }

    @Override
    protected JAXBContext getJAXBContext() {
        return jaxbContext;
    }

    @Override
    protected Set<String> getDomainNamespaces() {
        return Set.of("http://naesb.org/espi/customer");
    }

    // Public API methods
    public void exportCustomerEntry(CustomerDto customer, OutputStream stream)
            throws JAXBException {
        CustomerAtomEntryDto entry = createCustomerAtomEntry("Customer", customer);
        exportDto(entry, stream);
    }

    public void exportCustomersFeed(List<CustomerDto> customers, OutputStream stream)
            throws JAXBException {
        List<AtomEntryDto> entries = customers.stream()
            .map(dto -> createCustomerAtomEntry("Customer", dto))
            .collect(Collectors.toList());

        AtomFeedDto feed = new AtomFeedDto(
            UUID.randomUUID().toString(),
            "Customers",
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            entries
        );

        exportDto(feed, stream);
    }

    private CustomerAtomEntryDto createCustomerAtomEntry(String title, Object resource) {
        OffsetDateTime now = OffsetDateTime.now();
        String entryId = "urn:uuid:" + UUID.randomUUID();
        return new CustomerAtomEntryDto(entryId, title, now, now, null, resource);
    }
}
```

**Expected XML Output:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:cust="http://naesb.org/espi/customer">
    <id>urn:uuid:...</id>
    <title>Customer</title>
    <published>2025-01-21T...</published>
    <updated>2025-01-21T...</updated>
    <cust:Customer>
        <cust:customerName>John Doe</cust:customerName>
        <cust:vip>true</cust:vip>
    </cust:Customer>
</entry>
```

**Files to Create:**
- `CustomerExportService.java`

---

## Phase 4: Create Facade for Backwards Compatibility

**Goal:** Maintain existing `DtoExportService` interface while delegating to specialized services.

### 4.1 Create `DtoExportServiceFacade`

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceFacade.java`

**Purpose:** Backwards compatibility layer that auto-detects domain and delegates.

```java
@Service
@Primary  // This becomes the default DtoExportService implementation
@Slf4j
@RequiredArgsConstructor
public class DtoExportServiceFacade implements DtoExportService {

    private final UsageExportService usageExportService;
    private final CustomerExportService customerExportService;

    @Override
    public void exportDto(Object dto, OutputStream stream) {
        try {
            DomainType domain = detectDomain(dto);

            switch (domain) {
                case USAGE -> usageExportService.exportDto(dto, stream);
                case CUSTOMER -> customerExportService.exportDto(dto, stream);
                default -> throw new IllegalArgumentException("Unknown domain for DTO: " + dto.getClass());
            }

        } catch (JAXBException e) {
            log.error("Failed to export DTO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export DTO", e);
        }
    }

    @Override
    public void exportAtomFeed(AtomFeedDto atomFeedDto, OutputStream stream) {
        try {
            // Detect domain from first entry
            DomainType domain = detectDomainFromFeed(atomFeedDto);

            switch (domain) {
                case USAGE -> usageExportService.exportDto(atomFeedDto, stream);
                case CUSTOMER -> customerExportService.exportDto(atomFeedDto, stream);
                default -> throw new IllegalArgumentException("Cannot determine domain from feed");
            }

        } catch (JAXBException e) {
            log.error("Failed to export Atom feed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export Atom feed", e);
        }
    }

    // Existing interface methods delegate appropriately...

    private DomainType detectDomain(Object dto) {
        if (dto instanceof AtomEntryDto entry) {
            Object content = entry.getContent();
            if (content != null) {
                return detectDomainFromContent(content);
            }
        }

        return detectDomainFromContent(dto);
    }

    private DomainType detectDomainFromContent(Object content) {
        String packageName = content.getClass().getPackage().getName();

        if (packageName.contains(".dto.usage")) {
            return DomainType.USAGE;
        } else if (packageName.contains(".dto.customer")) {
            return DomainType.CUSTOMER;
        }

        return DomainType.UNKNOWN;
    }

    private DomainType detectDomainFromFeed(AtomFeedDto feed) {
        if (feed.getEntries() != null && !feed.getEntries().isEmpty()) {
            AtomEntryDto firstEntry = feed.getEntries().get(0);
            return detectDomain(firstEntry);
        }
        return DomainType.UNKNOWN;
    }

    private enum DomainType {
        USAGE, CUSTOMER, UNKNOWN
    }
}
```

**Files to Create:**
- `DtoExportServiceFacade.java`

**Files to Modify:**
- Mark `DtoExportServiceImpl` as `@Deprecated` (or delete after migration)

---

## Phase 5: Update EspiNamespacePrefixMapper

**Goal:** Ensure prefix mapper returns empty string for Atom when it's the only default candidate.

### 5.1 Update `getPreferredPrefix()` Logic

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/utils/EspiNamespacePrefixMapper.java`

**Current:**
```java
if (ATOM_NAMESPACE.equals(namespaceUri)) {
    return "atom";  // Returns prefix
}
```

**Updated with Context Awareness:**
```java
@Override
public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
    if (namespaceUri == null) {
        return null;
    }

    // Atom namespace - return empty string if it should be default
    if (ATOM_NAMESPACE.equals(namespaceUri)) {
        // If only 2 namespaces total (Atom + one domain), make Atom default
        if (requiredNamespaces.size() == 2) {
            return "";  // Empty = default namespace
        }
        // Otherwise use atom: prefix for clarity
        return "atom";
    }

    if (ESPI_NAMESPACE.equals(namespaceUri)) {
        return "espi";
    }

    if (CUSTOMER_NAMESPACE.equals(namespaceUri)) {
        return "cust";
    }

    return null;
}
```

**Files to Modify:**
- `EspiNamespacePrefixMapper.java`

---

## Phase 6: Testing Strategy

### 6.1 Unit Tests

**Create `UsageExportServiceTest`**

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/service/impl/UsageExportServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class UsageExportServiceTest {

    private UsageExportService usageExportService;

    @BeforeEach
    void setUp() throws JAXBException {
        usageExportService = new UsageExportService();
        usageExportService.init();
    }

    @Test
    @DisplayName("Should export UsagePoint with Atom as default namespace")
    void shouldExportUsagePointWithAtomDefault() throws Exception {
        // Arrange
        UsagePointDto usagePoint = new UsagePointDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440001",
            new byte[]{0x01},
            null, (short) 1,
            // ... other fields
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Act
        usageExportService.exportUsagePointEntry(usagePoint, output);
        String xml = output.toString(StandardCharsets.UTF_8);

        // Assert
        System.out.println(xml);
        assertThat(xml).contains("xmlns=\"http://www.w3.org/2005/Atom\"");
        assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");
        assertThat(xml).doesNotContain("xmlns:cust");
        assertThat(xml).doesNotContain("http://naesb.org/espi/customer");
        assertThat(xml).contains("<entry");  // No prefix on Atom elements
        assertThat(xml).contains("<espi:UsagePoint>");
    }

    @Test
    @DisplayName("Should NOT declare customer namespace")
    void shouldNotDeclareCustomerNamespace() throws Exception {
        // Similar test verifying xmlns:cust absence
    }
}
```

**Create `CustomerExportServiceTest`**

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/service/impl/CustomerExportServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class CustomerExportServiceTest {

    private CustomerExportService customerExportService;

    @BeforeEach
    void setUp() throws JAXBException {
        customerExportService = new CustomerExportService();
        customerExportService.init();
    }

    @Test
    @DisplayName("Should export Customer with Atom as default namespace")
    void shouldExportCustomerWithAtomDefault() throws Exception {
        // Arrange
        CustomerDto customer = new CustomerDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440001",
            null, null, "Special needs", true, null, null, null, null, "John Doe"
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Act
        customerExportService.exportCustomerEntry(customer, output);
        String xml = output.toString(StandardCharsets.UTF_8);

        // Assert
        System.out.println(xml);
        assertThat(xml).contains("xmlns=\"http://www.w3.org/2005/Atom\"");
        assertThat(xml).contains("xmlns:cust=\"http://naesb.org/espi/customer\"");
        assertThat(xml).doesNotContain("xmlns:espi=\"http://naesb.org/espi\"");
        assertThat(xml).contains("<entry");  // No prefix on Atom elements
        assertThat(xml).contains("<cust:Customer>");
    }
}
```

### 6.2 Integration Tests

**Update Existing Integration Tests**

**Files to Update:**
- Any controller tests using `DtoExportService`
- REST API tests verifying XML output

---

## Phase 7: Update Package-info.java

### 7.1 Verify Atom Default Namespace Declaration

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/atom/package-info.java`

**Current (line 30):**
```java
@XmlNs(prefix = "", namespaceURI = "http://www.w3.org/2005/Atom")
```

**Verification:** This is correct. The empty `prefix = ""` indicates Atom should be default namespace.

**No changes needed** - JAXB 3.x will respect this with 2-namespace contexts.

---

## Phase 8: Migration and Deprecation

### 8.1 Deprecate Old Service

**File:** `DtoExportServiceImpl.java`

```java
@Service("dtoExportServiceImpl")  // Give it a specific bean name
@Deprecated(since = "3.5.0-RC3", forRemoval = true)
@Slf4j
public class DtoExportServiceImpl implements DtoExportService {
    // Keep existing implementation for backwards compatibility
    // Add deprecation warnings in logs
}
```

### 8.2 Update Spring Configuration

**Ensure proper bean priority:**
- `@Primary` on `DtoExportServiceFacade` makes it default
- Controllers get facade by default
- Legacy code can still reference `dtoExportServiceImpl` bean name if needed

---

## Implementation Checklist

### Phase 1: Foundation
- [ ] Create `BaseExportService` abstract class
- [ ] Extract common marshaller configuration
- [ ] Extract XML header constants
- [ ] Add unit tests for BaseExportService

### Phase 2: Usage Service
- [ ] Create `UsageExportService`
- [ ] Initialize JAXBContext with usage domain classes only
- [ ] Implement export methods
- [ ] Create `UsageExportServiceTest`
- [ ] Verify Atom as default namespace in XML output
- [ ] Verify no customer namespace declared

### Phase 3: Customer Service
- [ ] Create `CustomerExportService`
- [ ] Initialize JAXBContext with customer domain classes only
- [ ] Implement export methods
- [ ] Create `CustomerExportServiceTest`
- [ ] Verify Atom as default namespace in XML output
- [ ] Verify no usage namespace declared

### Phase 4: Facade
- [ ] Create `DtoExportServiceFacade`
- [ ] Implement domain detection logic
- [ ] Add delegation to specialized services
- [ ] Mark facade as `@Primary`
- [ ] Create `DtoExportServiceFacadeTest`

### Phase 5: Namespace Mapper
- [ ] Update `EspiNamespacePrefixMapper.getPreferredPrefix()`
- [ ] Return `""` for Atom when 2 namespaces total
- [ ] Add unit tests for namespace mapper logic

### Phase 6: Testing
- [ ] Run all unit tests
- [ ] Run integration tests
- [ ] Verify XML output matches ESPI specification
- [ ] Test with real UsagePoint entities
- [ ] Test with real Customer entities

### Phase 7: Migration
- [ ] Mark `DtoExportServiceImpl` as deprecated
- [ ] Update controller injection (if needed)
- [ ] Update documentation
- [ ] Add migration guide for consumers

### Phase 8: Validation
- [ ] Compare XML output before/after
- [ ] Verify namespace isolation
- [ ] Verify Atom default namespace
- [ ] Performance testing (JAXBContext initialization)

---

## Files Summary

### New Files (7)
1. `service/impl/BaseExportService.java`
2. `service/impl/UsageExportService.java`
3. `service/impl/CustomerExportService.java`
4. `service/impl/DtoExportServiceFacade.java`
5. `test/.../UsageExportServiceTest.java`
6. `test/.../CustomerExportServiceTest.java`
7. `test/.../DtoExportServiceFacadeTest.java`

### Modified Files (2)
1. `utils/EspiNamespacePrefixMapper.java` - Update getPreferredPrefix() logic
2. `service/impl/DtoExportServiceImpl.java` - Add @Deprecated annotation

### No Changes Needed (1)
1. `dto/atom/package-info.java` - Already declares Atom as default

---

## Expected Outcomes

### Before Implementation
```xml
<!-- Usage Domain - INCORRECT -->
<ns3:entry xmlns:ns3="http://www.w3.org/2005/Atom" xmlns:espi="http://naesb.org/espi">
    <ns3:id>...</ns3:id>
    <espi:UsagePoint>...</espi:UsagePoint>
</ns3:entry>
```

### After Implementation
```xml
<!-- Usage Domain - CORRECT -->
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:espi="http://naesb.org/espi">
    <id>...</id>
    <espi:UsagePoint>...</espi:UsagePoint>
</entry>

<!-- Customer Domain - CORRECT -->
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:cust="http://naesb.org/espi/customer">
    <id>...</id>
    <cust:Customer>...</cust:Customer>
</entry>
```

---

## Risks and Mitigations

### Risk 1: JAXBContext Initialization Performance
**Impact:** Two JAXBContexts instead of one
**Mitigation:**
- Initialize at `@PostConstruct` (one-time cost)
- Contexts are reused for all subsequent exports
- Smaller contexts = faster initialization

### Risk 2: Backwards Compatibility
**Impact:** Existing code depends on DtoExportServiceImpl
**Mitigation:**
- Keep DtoExportServiceImpl as deprecated
- Facade implements same interface
- Use `@Primary` for auto-wiring

### Risk 3: Missing Domain Classes
**Impact:** New DTO added but not registered in service
**Mitigation:**
- Comprehensive unit tests
- Clear documentation
- Consider annotation scanning (future enhancement)

---

## Future Enhancements

### Auto-Discovery of DTO Classes
Instead of manually listing all DTO classes, use classpath scanning:

```java
@PostConstruct
public void init() {
    Set<Class<?>> usageClasses = scanPackage("org.greenbuttonalliance.espi.common.dto.usage");
    this.jaxbContext = JAXBContext.newInstance(usageClasses.toArray(new Class[0]));
}
```

### Caching Strategy
Add caching for frequently exported entities to avoid repeated marshalling.

---

## Success Criteria

✅ **Phase 1 Complete When:**
- BaseExportService compiles
- Common marshaller logic extracted
- Unit tests pass

✅ **Phase 2 Complete When:**
- UsageExportService exports UsagePoint
- XML declares `xmlns="http://www.w3.org/2005/Atom"` (default)
- XML declares `xmlns:espi="http://naesb.org/espi"` (prefixed)
- NO `xmlns:cust` declared
- All tests pass

✅ **Phase 3 Complete When:**
- CustomerExportService exports Customer
- XML declares `xmlns="http://www.w3.org/2005/Atom"` (default)
- XML declares `xmlns:cust="http://naesb.org/espi/customer"` (prefixed)
- NO `xmlns:espi` declared
- All tests pass

✅ **Phase 4 Complete When:**
- Facade delegates correctly to usage/customer services
- Domain detection works for all DTO types
- Backwards compatibility maintained
- All integration tests pass

✅ **Final Success When:**
- All 8 phases complete
- All tests pass (unit + integration)
- XML output matches ESPI 4.0 specification
- Atom is default namespace in both domains
- No namespace pollution between domains

---

## Timeline Estimate

| Phase | Effort | Duration |
|-------|--------|----------|
| Phase 1: Foundation | Medium | 2-3 hours |
| Phase 2: Usage Service | Medium | 3-4 hours |
| Phase 3: Customer Service | Medium | 3-4 hours |
| Phase 4: Facade | Low | 1-2 hours |
| Phase 5: Namespace Mapper | Low | 1 hour |
| Phase 6: Testing | High | 4-6 hours |
| Phase 7: Migration | Low | 1-2 hours |
| Phase 8: Validation | Medium | 2-3 hours |
| **Total** | | **17-25 hours** |

---

## Next Steps

1. **Review and approve this plan**
2. **Create feature branch:** `feature/domain-specific-export-services`
3. **Begin Phase 1:** Create BaseExportService
4. **Iterate through phases** with testing at each step
5. **Create PR** when all phases complete

---

## Questions for Review

1. Should we keep `DtoExportServiceImpl` or delete it after migration?
2. Should the facade be in the `impl` package or elevated to the service level?
3. Do we need a third service for pure Atom exports (links, feeds without content)?
4. Should we add metrics/logging to track which service is being used?

---

**End of Implementation Plan**
