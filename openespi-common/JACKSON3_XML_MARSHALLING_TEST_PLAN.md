# Jackson 3 XML Marshalling Test Plan

**Branch:** `feature/jackson3-xml-marshalling-tests`
**Date:** 2026-01-04
**Purpose:** Update all XML marshalling tests to use Jackson 3 XmlMapper with JAXB annotations

---

## Executive Summary

This plan addresses the gap identified during Multi-Phase Schema Compliance review:
- **Current State**: XML marshalling tests use JAXB (`JAXBContext`, `Marshaller`, `Unmarshaller`)
- **Target State**: Tests should use Jackson 3 (`XmlMapper`) to match production code
- **Production Code**: Uses Jackson 3 XmlMapper with JAXB annotations (hybrid approach)
- **Test Code**: Still uses pure JAXB (misalignment)

---

## Issues Discovered During Review

### Issue #1: DtoExportServiceImpl Atom Metadata Extraction

**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImpl.java:189-195`

**Problem:** `createAtomEntry()` does NOT extract Atom metadata from entities

**Current Behavior:**
```java
public AtomEntryDto createAtomEntry(String title, Object resource) {
    return new AtomEntryDto(
        UUID.randomUUID().toString(),  // ❌ Generates NEW Version-4 UUID (random)
        title,                         // ❌ Uses parameter string
        resource                      // DTO resource
    );
}
```

**Issues:**
- ❌ Atom `<id>`: Generates new **Version-4** (random) UUID instead of using `entity.getId()` **(Version-5 UUID)**
- ❌ Atom `<title>`: Uses hardcoded parameter string instead of `entity.getDescription()`
- ❌ Atom `<link>` elements: Set to NULL instead of extracting from `entity.selfLink`, `upLink`, `relatedLinks`
- ✅ Atom `<published>`/`<updated>`: Uses NOW (certified approach - correct)

**CRITICAL:** ESPI requires **Version-5 UUIDs** (deterministic, based on namespace + name) NOT Version-4 (random)
- Version-4: `UUID.randomUUID()` - generates random UUID (version field = `4`)
- Version-5: `UUID.nameUUIDFromBytes()` - generates deterministic UUID from name (version field = `5`)

**Expected Behavior:**
```xml
<entry>
    <id>urn:uuid:48c2a019-5598-5e16-b0f9-49e4ff27f5fb</id>  <!-- entity.id (Version-5 UUID) -->
    <title>Front Electric Meter</title>  <!-- entity.description -->
    <published>2026-01-04T12:34:56Z</published>  <!-- NOW (correct) -->
    <updated>2026-01-04T12:34:56Z</updated>  <!-- NOW (correct) -->
    <link rel="self" href="/espi/1_1/resource/UsagePoint/48C2A019"/>  <!-- entity.selfLink -->
    <link rel="up" href="/espi/1_1/resource/UsagePoint"/>  <!-- entity.upLink -->
    <link rel="related" href="/espi/1_1/resource/UsagePoint/48C2A019/MeterReading"/>  <!-- entity.relatedLinks -->
    <content>
        <espi:UsagePoint>...</espi:UsagePoint>
    </content>
</entry>
```

**See:** `ATOM_METADATA_EXTRACTION_VERIFICATION.md` (verification document from review)

**Action:** Create separate GitHub issue for DtoExportServiceImpl enhancement

---

### Issue #2: Test Code Uses Version-4 UUIDs

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImplTest.java:118`

**Problem:** Test helper method creates entity with **Version-4 UUID** instead of **Version-5**

**Current Test Code:**
```java
UsagePointEntity usagePointEntity = new UsagePointEntity();
usagePointEntity.setId(UUID.fromString("48C2A019-5598-4E16-B0F9-49E4FF27F5FB"));
//                                               ↑
//                                    Version-4 (should be 5)
```

**UUID Analysis:**
- Format: `48c2a019-5598-4e16-b0f9-49e4ff27f5fb`
- Version field (3rd group, 1st char): `4e16` → **Version-4**
- Should be: `48c2a019-5598-5e16-b0f9-49e4ff27f5fb` → **Version-5**

**Impact:** Tests are not validating ESPI compliance requirement for Version-5 UUIDs

**Fix Required:**
1. Update test entity UUIDs to use Version-5 format
2. Add assertions to validate UUID version field
3. Document proper Version-5 UUID generation using `EspiIdGeneratorService`

---

### Issue #3: No Jackson 3 XML Marshalling Tests

**Current Test Inventory:**

| Test File | Technology | Status | Issue |
|-----------|-----------|---------|-------|
| **TimeConfigurationDtoTest.java** | JAXB | ✅ Active | Uses JAXBContext, not Jackson 3 |
| **SimpleXmlMarshallingTest.java** | JAXB | ❌ @Disabled | Comment: "refactor to use Jackson for marshalling" |
| **XmlDebugTest.java** | JAXB | ✅ Active | Uses JAXBContext, not Jackson 3 |
| **DtoExportServiceImplTest.java** | Jackson 3 (indirect) | ⚠️ No assertions | Just prints output, no validation |

**Gap:** No tests validate Jackson 3 XmlMapper XML marshalling/unmarshalling

**Production Code Uses:**
```java
// DtoExportServiceImpl.java:154-172
private XmlMapper createXmlMapper() {
    AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance(
        new JakartaXmlBindAnnotationIntrospector(),
        new JacksonAnnotationIntrospector()
    );

    return XmlMapper.xmlBuilder()
        .annotationIntrospector(intr)
        .addModule(new JakartaXmlBindAnnotationModule()
            .setNonNillableInclusion(JsonInclude.Include.NON_EMPTY))
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
        .disable(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
        .defaultDateFormat(new StdDateFormat())
        .build();
}
```

**Tests Should Use:** Same XmlMapper configuration to validate production code

---

## Test Output Location

### DtoExportServiceImplTest Output

**File:** `openespi-common/src/test/resources/sample-xml/testdata.xml`

**Current Output (excerpt):**
```xml
<entry xmlns:espi="http://naesb.org/espi">
    <id>urn:uuid:fa404785-1dc7-5aab-952d-14f8841df8de</id>  <!-- ❌ Version-4 random UUID -->
    <title/>  <!-- ❌ Empty - should have entity description -->
    <content>
        <espi:IntervalBlock>
            <espi:interval>
                <espi:duration>86400</espi:duration>
                <espi:start>1641099600</espi:start>
            </espi:interval>
            <espi:IntervalReading>
                <espi:timePeriod>
                    <espi:duration>86400</espi:duration>
                    <espi:start>1641099600</espi:start>
                </espi:timePeriod>
                <espi:value>3880</espi:value>
            </espi:IntervalReading>
        </espi:IntervalBlock>
    </content>
    <published>2022-01-02T06:00:00Z</published>
    <updated>2022-05-19T21:44:50Z</updated>
    <!-- ❌ No <link> elements - should have self, up, related -->
</entry>
```

**Issues Visible in Output:**
- Version-4 random UUID (should be Version-5 from entity)
- Empty `<title/>` element
- No `<link>` elements

---

## UUID Version Requirements

### ESPI UUID Specification

**NAESB ESPI Standard requires Version-5 UUIDs:**
- **Version-5**: Deterministic UUID based on namespace + name (SHA-1 hash)
- **Format**: `xxxxxxxx-xxxx-5xxx-xxxx-xxxxxxxxxxxx` (note the '5' in the version field)
- **Generation**: Based on resource's `self` link href

**Version Identification:**
```
UUID Format: xxxxxxxx-xxxx-Vxxx-xxxx-xxxxxxxxxxxx
                          ↑
                    Version field (first hex digit of 3rd group)

Version-4 example: 48c2a019-5598-4e16-b0f9-49e4ff27f5fb
                                 ↑
                        Version = 4 (random)

Version-5 example: 48c2a019-5598-5e16-b0f9-49e4ff27f5fb
                                 ↑
                        Version = 5 (deterministic)
```

**Version-4 (WRONG):**
```java
UUID.randomUUID()  // ❌ Generates random Version-4 UUID
// Example: fa404785-1dc7-4aab-952d-14f8841df8de
//                         ↑ version = 4
```

**Version-5 (CORRECT):**
```java
// Entity already has Version-5 UUID in id field
entity.getId()  // ✅ Returns Version-5 UUID from database
// Example: 48c2a019-5598-5e16-b0f9-49e4ff27f5fb
//                         ↑ version = 5
```

**Generation via EspiIdGeneratorService:**
```java
// IdentifiedObject.java:179-184
public void generateEspiCompliantId(EspiIdGeneratorService idGeneratorService) {
    if (selfLink != null && selfLink.getHref() != null) {
        UUID espiId = idGeneratorService.generateEspiId(selfLink.getHref());
        setId(espiId);  // ✅ Sets Version-5 UUID
    }
}
```

---

## Recommended Assertions for DtoExportServiceImplTest

### Current Test Code

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImplTest.java:48-69`

```java
@Test
void export_atom_feed_test() throws IOException {
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto usagePointEntryDto = getUsagePointEntry(now);
    AtomEntryDto meterReadingEntryDto = getMeeterReadingEntryDto(now);
    AtomEntryDto readingEntry = getReadingEntryDto(now);
    AtomEntryDto intervalBlockEntry = getIntervlBlockEntryDto(now);

    AtomFeedDto atomFeedDto = new AtomFeedDto("urn:uuid:15B0A4ED-CCF4-4521-A0A1-9FF650EC8A6B",
        "Green Button Subscription Feed", now, now, null,
        List.of(usagePointEntryDto, meterReadingEntryDto, readingEntry, intervalBlockEntry));

    try (OutputStream stream = new ByteArrayOutputStream()) {
        // Commented out due to conflict in IntervalReadingDto which cannot be fixed in this task
         dtoExportService.exportAtomFeed(atomFeedDto, stream);
         System.out.println(stream.toString());  // ❌ Only prints - no validation
    }
}
```

**Problem:** No assertions - just prints output

---

### Recommended Assertions

#### 1. XML Structure Assertions

```java
@Test
@DisplayName("Should export Atom feed with valid XML structure")
void shouldExportAtomFeedWithValidXmlStructure() throws IOException {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto usagePointEntry = getUsagePointEntry(now);
    AtomFeedDto atomFeedDto = new AtomFeedDto(
        "urn:uuid:15B0A4ED-CCF4-4521-A0A1-9FF650EC8A6B",
        "Green Button Subscription Feed",
        now, now, null,
        List.of(usagePointEntry)
    );

    // Act
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(atomFeedDto, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Assert - XML Declaration
    assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

    // Assert - Root element
    assertThat(xml).contains("<feed xmlns=\"http://www.w3.org/2005/Atom\">");

    // Assert - Feed metadata
    assertThat(xml).contains("<id>urn:uuid:15B0A4ED-CCF4-4521-A0A1-9FF650EC8A6B</id>");
    assertThat(xml).contains("<title>Green Button Subscription Feed</title>");
    assertThat(xml).contains("<updated>");
    assertThat(xml).contains("<published>");

    // Assert - Entry structure
    assertThat(xml).contains("<entry");
    assertThat(xml).contains("</entry>");

    // Assert - ESPI namespace in content
    assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");
    assertThat(xml).contains("<content>");
    assertThat(xml).contains("</content>");
}
```

---

#### 2. Atom Entry Metadata Assertions

```java
@Test
@DisplayName("Should export Atom entry with proper metadata elements")
void shouldExportAtomEntryWithProperMetadata() throws IOException {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto usagePointEntry = getUsagePointEntry(now);
    AtomFeedDto atomFeedDto = new AtomFeedDto(
        "urn:uuid:feed-id", "Test Feed", now, now, null,
        List.of(usagePointEntry)
    );

    // Act
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(atomFeedDto, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Assert - Entry has required Atom elements
    assertThat(xml).contains("<entry");
    assertThat(xml).containsPattern("<id>urn:uuid:[0-9a-f-]{36}</id>");  // UUID format
    assertThat(xml).contains("<title>");  // Should have title element
    assertThat(xml).contains("<published>");  // ISO 8601 timestamp
    assertThat(xml).contains("<updated>");  // ISO 8601 timestamp

    // Assert - Timestamps are ISO 8601 format
    assertThat(xml).containsPattern("<published>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    assertThat(xml).containsPattern("<updated>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
}
```

---

#### 3. UUID Version-5 Validation (Currently Failing - Expected)

```java
@Test
@DisplayName("Should use Version-5 UUIDs for Atom entry IDs")
@Disabled("Known issue: DtoExportServiceImpl generates Version-4 UUIDs instead of using entity Version-5 UUIDs - see GitHub issue #XX")
void shouldUseVersion5UuidsForAtomEntryIds() throws IOException {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    // NOTE: getUsagePointEntry() currently creates entity with Version-4 UUID (line 118)
    // This needs to be updated to use Version-5 UUID
    // Expected Version-5: 48c2a019-5598-5e16-b0f9-49e4ff27f5fb (note '5' in version field)
    AtomEntryDto usagePointEntry = getUsagePointEntry(now);
    AtomFeedDto atomFeedDto = new AtomFeedDto(
        "urn:uuid:feed-id", "Test Feed", now, now, null,
        List.of(usagePointEntry)
    );

    // Act
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(atomFeedDto, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Assert - Should use entity's Version-5 UUID (after test data is fixed)
    assertThat(xml).contains("<id>urn:uuid:48c2a019-5598-5e16-b0f9-49e4ff27f5fb</id>");

    // Assert - UUID version field should be '5' (4th group, 1st char)
    // Format: xxxxxxxx-xxxx-5xxx-xxxx-xxxxxxxxxxxx
    //                       ↑ version bit = 5
    assertThat(xml).containsPattern("<id>urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}</id>");

    // Assert - Should NOT contain Version-4 UUIDs (version bit = 4)
    assertThat(xml).doesNotContainPattern("<id>urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}</id>");
}
```

---

#### 4. Atom Link Assertions (Currently Failing - Expected)

```java
@Test
@DisplayName("Should export Atom entry with link elements")
@Disabled("Known issue: DtoExportServiceImpl does not extract links from entities - see GitHub issue #XX")
void shouldExportAtomEntryWithLinkElements() throws IOException {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto usagePointEntry = getUsagePointEntry(now);
    AtomFeedDto atomFeedDto = new AtomFeedDto(
        "urn:uuid:feed-id", "Test Feed", now, now, null,
        List.of(usagePointEntry)
    );

    // Act
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(atomFeedDto, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Assert - Entry should have links
    assertThat(xml).contains("<link rel=\"self\"");
    assertThat(xml).contains("<link rel=\"up\"");
    assertThat(xml).contains("<link rel=\"related\"");

    // Assert - Links should have href attributes
    assertThat(xml).containsPattern("<link rel=\"self\" href=\"[^\"]+\"");
    assertThat(xml).containsPattern("<link rel=\"up\" href=\"[^\"]+\"");
}
```

---

#### 5. ESPI Content Assertions

```java
@Test
@DisplayName("Should export ESPI UsagePoint content correctly")
void shouldExportEspiUsagePointContent() throws IOException {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto usagePointEntry = getUsagePointEntry(now);
    AtomFeedDto atomFeedDto = new AtomFeedDto(
        "urn:uuid:feed-id", "Test Feed", now, now, null,
        List.of(usagePointEntry)
    );

    // Act
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(atomFeedDto, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Assert - ESPI namespace
    assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");

    // Assert - UsagePoint element
    assertThat(xml).contains("<espi:UsagePoint>");
    assertThat(xml).contains("</espi:UsagePoint>");

    // Assert - UsagePoint fields
    assertThat(xml).contains("<espi:ServiceCategory>");
    assertThat(xml).contains("</espi:ServiceCategory>");
}
```

---

#### 6. Round-Trip Marshalling Assertion

```java
@Test
@DisplayName("Should support round-trip marshalling/unmarshalling")
void shouldSupportRoundTripMarshalling() throws IOException {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto original = getUsagePointEntry(now);
    AtomFeedDto originalFeed = new AtomFeedDto(
        "urn:uuid:feed-id", "Test Feed", now, now, null,
        List.of(original)
    );

    // Act - Marshal to XML
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(originalFeed, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Act - Unmarshal back from XML
    XmlMapper xmlMapper = createTestXmlMapper();
    AtomFeedDto roundTrip = xmlMapper.readValue(xml, AtomFeedDto.class);

    // Assert - Feed metadata preserved
    assertThat(roundTrip.id()).isEqualTo(originalFeed.id());
    assertThat(roundTrip.title()).isEqualTo(originalFeed.title());

    // Assert - Entry count preserved
    assertThat(roundTrip.entries()).hasSize(originalFeed.entries().size());
}

private XmlMapper createTestXmlMapper() {
    AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance(
        new JakartaXmlBindAnnotationIntrospector(),
        new JacksonAnnotationIntrospector()
    );

    return XmlMapper.xmlBuilder()
        .annotationIntrospector(intr)
        .addModule(new JakartaXmlBindAnnotationModule()
            .setNonNillableInclusion(JsonInclude.Include.NON_EMPTY))
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
        .disable(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
        .defaultDateFormat(new StdDateFormat())
        .build();
}
```

---

#### 7. XSD Schema Validation Assertion

```java
@Test
@DisplayName("Should generate XML that validates against ESPI 4.0 XSD schema")
void shouldGenerateXmlThatValidatesAgainstEspiXsd() throws Exception {
    // Arrange
    LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    OffsetDateTime now = localDateTime.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();

    AtomEntryDto usagePointEntry = getUsagePointEntry(now);
    AtomFeedDto atomFeedDto = new AtomFeedDto(
        "urn:uuid:feed-id", "Test Feed", now, now, null,
        List.of(usagePointEntry)
    );

    // Act - Generate XML
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    dtoExportService.exportAtomFeed(atomFeedDto, stream);
    String xml = stream.toString(StandardCharsets.UTF_8);

    // Assert - Validate against XSD
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    // Load ESPI 4.0 XSD schema
    URL xsdUrl = getClass().getClassLoader().getResource("schema/ESPI_4.0/espi.xsd");
    assertThat(xsdUrl).isNotNull();

    Schema schema = schemaFactory.newSchema(xsdUrl);
    Validator validator = schema.newValidator();

    // Validate XML against schema
    Source xmlSource = new StreamSource(new StringReader(xml));
    assertThatCode(() -> validator.validate(xmlSource))
        .doesNotThrowAnyException();
}
```

---

## Test Data Fix Required

### Update Test UUIDs to Version-5

**File:** `DtoExportServiceImplTest.java`

**Current (line 118):**
```java
UsagePointEntity usagePointEntity = new UsagePointEntity();
usagePointEntity.setId(UUID.fromString("48C2A019-5598-4E16-B0F9-49E4FF27F5FB"));
//                                               ↑ Version-4
```

**Fix Option 1: Manual Version-5 UUID**
```java
UsagePointEntity usagePointEntity = new UsagePointEntity();
usagePointEntity.setId(UUID.fromString("48C2A019-5598-5E16-B0F9-49E4FF27F5FB"));
//                                               ↑ Changed to Version-5
```

**Fix Option 2: Generate Using EspiIdGeneratorService (Preferred)**
```java
UsagePointEntity usagePointEntity = new UsagePointEntity();
usagePointEntity.setSelfLink(new LinkType("self", "/espi/1_1/resource/UsagePoint/48C2A019"));
usagePointEntity.generateEspiCompliantId(espiIdGeneratorService);
// Generates Version-5 UUID based on selfLink.href
```

**Apply to All Test Entities:**
- UsagePoint (line 118)
- MeterReading (line 106-112)
- ReadingType (line 92-94)
- IntervalBlock (line 80-81)

---

## Test Conversion Plan

### Step 1: Update TimeConfigurationDtoTest to Use Jackson 3

**Current:** Uses JAXB (`JAXBContext`, `Marshaller`, `Unmarshaller`)
**Target:** Use Jackson 3 (`XmlMapper`)

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/usage/TimeConfigurationDtoTest.java`

**Changes Required:**

#### Before (JAXB):
```java
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

@BeforeEach
void setUp() throws JAXBException {
    jaxbContext = JAXBContext.newInstance(TimeConfigurationDto.class);

    marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

    unmarshaller = jaxbContext.createUnmarshaller();
}
```

#### After (Jackson 3):
```java
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.dataformat.xml.XmlAnnotationIntrospector;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.util.StdDateFormat;
import static org.assertj.core.api.Assertions.assertThat;

private XmlMapper xmlMapper;

@BeforeEach
void setUp() {
    AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance(
        new JakartaXmlBindAnnotationIntrospector(),
        new JacksonAnnotationIntrospector()
    );

    xmlMapper = XmlMapper.xmlBuilder()
        .annotationIntrospector(intr)
        .addModule(new JakartaXmlBindAnnotationModule()
            .setNonNillableInclusion(JsonInclude.Include.NON_EMPTY))
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
        .disable(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
        .defaultDateFormat(new StdDateFormat())
        .build();
}

@Test
@DisplayName("Should marshal TimeConfigurationDto with realistic timezone data")
void shouldMarshalTimeConfigurationWithRealisticData() throws Exception {
    // Arrange
    TimeConfigurationDto timeConfig = new TimeConfigurationDto(
        null, // id (transient)
        "urn:uuid:550e8400-e29b-41d4-a716-446655440000", // uuid
        new byte[]{0x01, 0x0B, 0x05, 0x00, 0x02, 0x00}, // dstEndRule
        3600L, // dstOffset
        new byte[]{0x01, 0x03, 0x02, 0x00, 0x02, 0x00}, // dstStartRule
        -28800L  // tzOffset (UTC-8)
    );

    // Act
    String xml = xmlMapper.writeValueAsString(timeConfig);  // Jackson 3

    // Assert
    assertThat(xml).contains("TimeConfiguration");
    assertThat(xml).contains("http://naesb.org/espi");
    assertThat(xml).contains("mRID");
    assertThat(xml).contains("550e8400-e29b-41d4-a716-446655440000");
    assertThat(xml).contains("<espi:tzOffset>-28800</espi:tzOffset>");
    assertThat(xml).contains("<espi:dstOffset>3600</espi:dstOffset>");
}
```

---

### Step 2: Enable and Update SimpleXmlMarshallingTest

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/SimpleXmlMarshallingTest.java`

**Changes Required:**

1. **Remove @Disabled annotation** (line 41)
2. **Replace JAXB imports** with Jackson 3 imports
3. **Update setUp() method** to create XmlMapper
4. **Update all test methods** to use `xmlMapper.writeValueAsString()` and `xmlMapper.readValue()`
5. **Rename class** to `Jackson3XmlMarshallingTest`

---

### Step 3: Update XmlDebugTest

**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/XmlDebugTest.java`

**Changes Required:**
- Same as TimeConfigurationDtoTest (replace JAXB with Jackson 3)
- Keep debug output but add assertions

---

### Step 4: Fix Test Data UUIDs

**File:** `DtoExportServiceImplTest.java`

**Changes Required:**
1. Update all test entity UUIDs from Version-4 to Version-5
2. Use `EspiIdGeneratorService.generateEspiId()` where appropriate
3. Document UUID version requirements in test comments

---

### Step 5: Add Assertions to DtoExportServiceImplTest

**File:** `DtoExportServiceImplTest.java`

**Changes Required:**
1. Add all recommended assertions from above
2. Extract XmlMapper creation to helper method
3. Add round-trip tests
4. Add XSD validation tests
5. Add Version-5 UUID validation test (mark @Disabled until DtoExportServiceImpl fixed)
6. Mark link-related tests as @Disabled with reference to GitHub issue

---

## Implementation Checklist

- [ ] **Step 1**: Update TimeConfigurationDtoTest to use Jackson 3
  - [ ] Replace JAXB imports with Jackson 3 imports
  - [ ] Update setUp() to create XmlMapper
  - [ ] Update all test methods to use xmlMapper
  - [ ] Run tests and verify all pass

- [ ] **Step 2**: Enable and update SimpleXmlMarshallingTest
  - [ ] Remove @Disabled annotation
  - [ ] Rename to Jackson3XmlMarshallingTest
  - [ ] Replace JAXB with Jackson 3
  - [ ] Run tests and verify all pass

- [ ] **Step 3**: Update XmlDebugTest
  - [ ] Replace JAXB with Jackson 3
  - [ ] Add assertions to validate output
  - [ ] Run tests and verify passes

- [ ] **Step 4**: Fix test data UUIDs to Version-5
  - [ ] Update UsagePoint UUID (line 118): `4E16` → `5E16`
  - [ ] Update MeterReading UUID if present
  - [ ] Update ReadingType UUID if present
  - [ ] Update IntervalBlock UUID if present
  - [ ] Add UUID version validation helpers
  - [ ] Document UUID requirements in test comments

- [ ] **Step 5**: Add assertions to DtoExportServiceImplTest
  - [ ] Add XML structure assertions
  - [ ] Add Atom metadata assertions
  - [ ] Add ESPI content assertions
  - [ ] Add round-trip marshalling test
  - [ ] Add XSD validation test
  - [ ] Add Version-5 UUID validation test (mark @Disabled)
  - [ ] Mark link tests as @Disabled with GitHub issue reference
  - [ ] Run tests and verify all pass (except disabled)

- [ ] **Step 6**: Create GitHub issue for DtoExportServiceImpl enhancement
  - [ ] Document Atom metadata extraction issue (id, title, links)
  - [ ] Document Version-5 UUID requirement
  - [ ] Reference ATOM_METADATA_EXTRACTION_VERIFICATION.md
  - [ ] Link to disabled test cases

- [ ] **Step 7**: Update documentation
  - [ ] Update MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md if needed
  - [ ] Add this plan to openespi-common/docs/

- [ ] **Step 8**: Commit, push, create PR
  - [ ] Stage changes: `git add .`
  - [ ] Commit: `git commit -m "feat: update XML marshalling tests to use Jackson 3 and fix UUID versions"`
  - [ ] Push: `git push origin feature/jackson3-xml-marshalling-tests`
  - [ ] Create PR

---

## Required Dependencies

Verify these are in `openespi-common/pom.xml`:

```xml
<!-- Jackson 3 XML -->
<dependency>
    <groupId>tools.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- Jackson JAXB Module -->
<dependency>
    <groupId>tools.jackson.module</groupId>
    <artifactId>jackson-module-jakarta-xmlbind-annotations</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- AssertJ (for assertions) -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Expected Test Output Location

After running tests, XML output samples are stored in:

**Location:** `openespi-common/src/test/resources/sample-xml/testdata.xml`

**Current Content:** Atom feed with IntervalBlock, IntervalReading, UsagePoint entries

**Access in Tests:**
```java
URL xmlUrl = getClass().getClassLoader().getResource("sample-xml/testdata.xml");
```

**Note:** This file is generated by `DtoExportServiceImplTest` but not currently validated by assertions.

---

## Success Criteria

1. ✅ All XML marshalling tests use Jackson 3 XmlMapper (not JAXB)
2. ✅ TimeConfigurationDtoTest updated and passing
3. ✅ SimpleXmlMarshallingTest enabled, updated, and passing
4. ✅ XmlDebugTest updated and passing
5. ✅ All test entity UUIDs converted to Version-5
6. ✅ DtoExportServiceImplTest has comprehensive assertions
7. ✅ All tests validate Jackson 3 processes JAXB annotations correctly
8. ✅ XSD validation tests confirm ESPI 4.0 schema compliance
9. ✅ Round-trip marshalling tests confirm data integrity
10. ✅ Version-5 UUID validation test created (disabled until DtoExportServiceImpl fixed)
11. ✅ Test output location documented
12. ✅ GitHub issue created for DtoExportServiceImpl enhancement

---

**Author:** Claude Sonnet 4.5
**Date:** 2026-01-04
**Branch:** `feature/jackson3-xml-marshalling-tests`
**Related Documents:**
- MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md
- ATOM_METADATA_EXTRACTION_VERIFICATION.md (to be created)
- DTO_APPROACH_COMPARISON.md

**Test Data UUID Corrections:**
```
Current (Version-4): 48C2A019-5598-4E16-B0F9-49E4FF27F5FB
                                   ↑ version = 4
Fixed (Version-5):   48C2A019-5598-5E16-B0F9-49E4FF27F5FB
                                   ↑ version = 5
```
