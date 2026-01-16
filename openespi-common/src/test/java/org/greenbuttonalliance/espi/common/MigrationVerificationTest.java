/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceLocationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.util.StdDateFormat;
import tools.jackson.dataformat.xml.XmlAnnotationIntrospector;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification tests for Spring Boot 3.5 migration.
 * Tests core functionality without dependencies on legacy support classes.
 */
@DisplayName("Spring Boot 3.5 Migration Verification")
class MigrationVerificationTest {

    @Test
    @DisplayName("Jakarta EE 9+ Validation API should work")
    void jakartaValidationShouldWork() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        
        UsagePointEntity entity = new UsagePointEntity();
        entity.setDescription("Test Usage Point");
        
        // Should not throw exceptions
        assertNotNull(validator);
        assertDoesNotThrow(() -> validator.validate(entity));
    }

    @Test
    @DisplayName("Jackson 3 XML with JAXB annotations should work for DTOs")
    void jackson3XmlWithJaxbAnnotationsShouldWork() throws Exception {
        // Production code uses Jackson 3 XmlMapper with JAXB annotation support
        AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance(
            new JakartaXmlBindAnnotationIntrospector(),
            new JacksonAnnotationIntrospector()
        );

        XmlMapper xmlMapper = XmlMapper.xmlBuilder()
            .annotationIntrospector(intr)
            .addModule(new JakartaXmlBindAnnotationModule()
                .setNonNillableInclusion(JsonInclude.Include.NON_EMPTY))
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
            .disable(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
            .defaultDateFormat(new StdDateFormat())
            .build();

        // Create a simple DTO with all nulls
        UsagePointDto dto = new UsagePointDto(
            null, // uuid
            null, // roleFlags
            null, // serviceCategory
            null, // status
            null, null, null, null, null, // serviceDeliveryPoint, amiBillingReady, checkBilling, connectionState, estimatedLoad
            null, null, null, null, // grounded, isSdp, isVirtual, minimalUsageExpected
            null, null, null, null, null, // nominalServiceVoltage, outageRegion, phaseCode, ratedCurrent, ratedPower
            null, null, null, null, // readCycle, readRoute, serviceDeliveryRemark, servicePriority
            null, null, null, null, null // pnodeRefs, aggregatedNodeRefs, meterReadings, usageSummaries, electricPowerQualitySummaries
        );

        // Wrap in Atom entry using full constructor (payload moved directly to AtomEntryDto, no AtomContentDto wrapper)
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        java.time.OffsetDateTime now = localDateTime.atOffset(java.time.ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();
        AtomEntryDto entry = new AtomEntryDto(
            "urn:uuid:test-entry",
            "Test Usage Point",
            now,
            now,
            null, // links
            dto   // content - passed directly (AtomEntryDto now includes AtomContentDto functionality)
        );

        // Marshal using Jackson 3
        String xml = assertDoesNotThrow(() -> xmlMapper.writeValueAsString(entry));

        // Debug: print XML
        System.out.println("Generated XML:");
        System.out.println(xml);

        // Verify XML structure
        assertTrue(xml.contains("entry"), "XML should contain 'entry' element"); // Now wrapping in Atom entry
        assertTrue(xml.contains("UsagePoint"), "XML should contain 'UsagePoint' element");
        assertTrue(xml.contains("http://naesb.org/espi"), "XML should contain ESPI namespace");
    }

    @Test
    @DisplayName("UUID primary key architecture should work")
    void uuidPrimaryKeyShouldWork() {
        // Test IdentifiedObject with UUID
        UsagePointEntity usagePoint = new UsagePointEntity();
        assertNotNull(usagePoint.getId()); // Should auto-generate UUID
        assertTrue(usagePoint.getId() instanceof UUID);
        
        CustomerEntity customer = new CustomerEntity();
        assertNotNull(customer.getId());
        assertTrue(customer.getId() instanceof UUID);
        
        MeterEntity meter = new MeterEntity();
        assertNotNull(meter.getId());
        assertTrue(meter.getId() instanceof UUID);
    }

    @Test
    @DisplayName("ESPI resource inheritance should work correctly")
    void espiResourceInheritanceShouldWork() {
        // Only actual ESPI resources should extend IdentifiedObject
        UsagePointEntity usagePoint = new UsagePointEntity();
        assertTrue(usagePoint instanceof IdentifiedObject);
        
        CustomerEntity customer = new CustomerEntity();
        assertTrue(customer instanceof IdentifiedObject);
        
        ServiceLocationEntity serviceLocation = new ServiceLocationEntity();
        assertTrue(serviceLocation instanceof IdentifiedObject);
        
        MeterEntity meter = new MeterEntity();
        assertTrue(meter instanceof IdentifiedObject);
    }

    @Test
    @DisplayName("Entity properties should work with Jakarta annotations")
    void entityPropertiesShouldWork() {
        UsagePointEntity entity = new UsagePointEntity();
        
        // Test basic property setting
        entity.setDescription("Test Description");
        assertEquals("Test Description", entity.getDescription());
        
        entity.setStatus((short) 5);
        assertEquals((short) 5, entity.getStatus());
        
        // Test role flags (byte array)
        byte[] roleFlags = {0x01, 0x02, 0x03};
        entity.setRoleFlags(roleFlags);
        assertArrayEquals(roleFlags, entity.getRoleFlags());
    }

    @Test
    @DisplayName("Customer domain entities should work independently")
    void customerDomainEntitiesShouldWork() {
        CustomerEntity customer = new CustomerEntity();
        customer.setSpecialNeed("NONE");
        
        assertEquals("NONE", customer.getSpecialNeed());
        assertNotNull(customer.getId());
        
        // Test ServiceLocation composition
        ServiceLocationEntity serviceLocation = new ServiceLocationEntity();
        serviceLocation.setAccessMethod("Key under mat");
        serviceLocation.setNeedsInspection(true);
        
        assertEquals("Key under mat", serviceLocation.getAccessMethod());
        assertTrue(serviceLocation.getNeedsInspection());
        assertNotNull(serviceLocation.getId());
    }

    @Test
    @DisplayName("SummaryMeasurement DTO should support ESPI business logic")
    void summaryMeasurementDtoShouldWork() {
        SummaryMeasurementDto dto = new SummaryMeasurementDto(
            "3",         // powerOfTenMultiplier as String
            1331784000L, // timeStamp
            "W",         // uom
            5000L,       // value
            "/espi/1_1/resource/ReadingType/07" // readingTypeRef
        );
        
        assertEquals("3", dto.powerOfTenMultiplier());
        assertEquals(1331784000L, dto.timeStamp());
        assertEquals("W", dto.uom());
        assertEquals(5000L, dto.value());
        assertEquals("/espi/1_1/resource/ReadingType/07", dto.readingTypeRef());
    }

    @Test
    @DisplayName("Basic compilation verification")
    void basicCompilationVerification() {
        // This test existing and passing means:
        // 1. Jakarta imports work
        // 2. Spring Boot 3.5 dependencies resolve
        // 3. UUID primary key architecture compiles
        // 4. Modern entity structure compiles
        // 5. DTO structure with JAXB annotations compiles
        
        assertTrue(true, "Compilation successful - Spring Boot 3.5 migration core features working");
    }
}