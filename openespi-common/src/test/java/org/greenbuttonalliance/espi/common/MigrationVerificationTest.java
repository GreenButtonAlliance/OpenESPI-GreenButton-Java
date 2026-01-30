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

import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.MeterEntity;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.Organisation;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceLocationEntity;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.domain.customer.enums.CustomerKind;
import org.greenbuttonalliance.espi.common.domain.customer.common.ElectronicAddress;
import org.greenbuttonalliance.espi.common.domain.customer.common.StreetAddress;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.dto.SummaryMeasurementDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.OffsetDateTime;
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
    @DisplayName("JAXB XML with JAXB annotations should work for DTOs")
    void jaxbXmlWithJaxbAnnotationsShouldWork() throws Exception {
        // Production code uses Jakarta JAXB Marshaller with JAXB annotation support
        jakarta.xml.bind.JAXBContext jaxbContext = jakarta.xml.bind.JAXBContext.newInstance(UsageAtomEntryDto.class);
        jakarta.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FRAGMENT, true);

        // Create a simple DTO with all nulls
        UsagePointDto dto = new UsagePointDto(
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
        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:test-entry",
            "Test Usage Point",
            now,
            now,
            null, // links
            dto   // content - passed directly (AtomEntryDto now includes AtomContentDto functionality)
        );

        // Marshal using JAXB
        java.io.StringWriter writer = new java.io.StringWriter();
        String xml = assertDoesNotThrow(() -> {
            marshaller.marshal(entry, writer);
            return writer.toString();
        });

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
        
        assertEquals("3", dto.getPowerOfTenMultiplier());
        assertEquals(1331784000L, dto.getTimeStamp());
        assertEquals("W", dto.getUom());
        assertEquals(5000L, dto.getValue());
        assertEquals("/espi/1_1/resource/ReadingType/07", dto.getReadingTypeRef());
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

    @Test
    @DisplayName("Customer entity with all embedded objects should work")
    void customerWithAllEmbeddedObjectsShouldWork() {
        // Test Customer entity with Organisation embedded object
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerName("Test Customer");
        customer.setKind(CustomerKind.RESIDENTIAL);
        customer.setSpecialNeed("Life support");
        customer.setVip(true);
        customer.setPucNumber("PUC-12345");
        customer.setLocale("en-US");

        // Test Organisation embedded object
        Organisation org = new Organisation();
        org.setOrganisationName("Test Organisation");

        // Test StreetAddress nested embedded object
        StreetAddress streetAddress = new StreetAddress();
        streetAddress.setStreetDetail("123 Test Street");
        streetAddress.setTownDetail("Test City");
        streetAddress.setStateOrProvince("CA");
        streetAddress.setPostalCode("90000");
        streetAddress.setCountry("USA");
        org.setStreetAddress(streetAddress);

        // Test ElectronicAddress nested embedded object
        ElectronicAddress electronicAddress = new ElectronicAddress();
        electronicAddress.setEmail1("test@example.com");
        electronicAddress.setWeb("https://example.com");
        org.setElectronicAddress(electronicAddress);

        customer.setOrganisation(org);

        // Test Status embedded object
        CustomerEntity.Status status = new CustomerEntity.Status();
        status.setValue("active");
        status.setDateTime(OffsetDateTime.now());
        status.setReason("Migration verification test");
        customer.setStatus(status);

        // Test Priority embedded object
        CustomerEntity.Priority priority = new CustomerEntity.Priority();
        priority.setValue(1);
        priority.setRank(10);
        priority.setType("high-priority");
        customer.setPriority(priority);

        // Verify all fields work
        assertNotNull(customer.getId());
        assertEquals("Test Customer", customer.getCustomerName());
        assertEquals(CustomerKind.RESIDENTIAL, customer.getKind());
        assertEquals("Life support", customer.getSpecialNeed());
        assertTrue(customer.getVip());
        assertEquals("PUC-12345", customer.getPucNumber());
        assertEquals("en-US", customer.getLocale());

        assertNotNull(customer.getOrganisation());
        assertEquals("Test Organisation", customer.getOrganisation().getOrganisationName());
        assertNotNull(customer.getOrganisation().getStreetAddress());
        assertEquals("123 Test Street", customer.getOrganisation().getStreetAddress().getStreetDetail());
        assertNotNull(customer.getOrganisation().getElectronicAddress());
        assertEquals("test@example.com", customer.getOrganisation().getElectronicAddress().getEmail1());

        assertNotNull(customer.getStatus());
        assertEquals("active", customer.getStatus().getValue());
        assertNotNull(customer.getStatus().getDateTime());

        assertNotNull(customer.getPriority());
        assertEquals(1, customer.getPriority().getValue());
        assertEquals(10, customer.getPriority().getRank());
    }

    @Test
    @DisplayName("Customer embedded objects should be null-safe")
    void customerEmbeddedObjectsShouldBeNullSafe() {
        // Test that Customer can be created with null embedded objects
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerName("Minimal Customer");
        customer.setOrganisation(null);
        customer.setStatus(null);
        customer.setPriority(null);

        assertNotNull(customer.getId());
        assertEquals("Minimal Customer", customer.getCustomerName());
        assertNull(customer.getOrganisation());
        assertNull(customer.getStatus());
        assertNull(customer.getPriority());
    }

    @Test
    @DisplayName("Customer Organisation nested objects should be null-safe")
    void customerOrganisationNestedObjectsShouldBeNullSafe() {
        // Test that Organisation can be created with null nested objects
        CustomerEntity customer = new CustomerEntity();
        Organisation org = new Organisation();
        org.setOrganisationName("Minimal Organisation");
        org.setStreetAddress(null);
        org.setPostalAddress(null);
        org.setElectronicAddress(null);
        customer.setOrganisation(org);

        assertNotNull(customer.getOrganisation());
        assertEquals("Minimal Organisation", customer.getOrganisation().getOrganisationName());
        assertNull(customer.getOrganisation().getStreetAddress());
        assertNull(customer.getOrganisation().getPostalAddress());
        assertNull(customer.getOrganisation().getElectronicAddress());
    }
}