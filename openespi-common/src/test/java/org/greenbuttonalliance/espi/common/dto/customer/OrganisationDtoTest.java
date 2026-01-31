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

package org.greenbuttonalliance.espi.common.dto.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrganisationDto.
 * Verifies DTO structure and field assignments for ESPI 4.0 customer.xsd compliance.
 * Organisation schema definition: customer.xsd lines 1096-1125.
 */
@DisplayName("OrganisationDto Unit Tests")
class OrganisationDtoTest {

    @Test
    @DisplayName("Should create OrganisationDto with all fields")
    void shouldCreateOrganisationDtoWithAllFields() {
        // Arrange
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "123 Main St", "Springfield", "IL", "62701", "USA"
        );

        CustomerDto.StreetAddressDto postalAddress = new CustomerDto.StreetAddressDto(
            "PO Box 456", "Springfield", "IL", "62702", "USA"
        );

        TelephoneNumberDto phone1 = new TelephoneNumberDto(
            "1", "217", null, "555-1234", null, null, null, null
        );

        TelephoneNumberDto phone2 = new TelephoneNumberDto(
            "1", "217", null, "555-5678", "101", null, null, null
        );

        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            null, null, "info@example.com", null, "https://www.example.com", null, null, null
        );

        // Act
        OrganisationDto organisation = new OrganisationDto(
            streetAddress, postalAddress, phone1, phone2, electronicAddress, "Test Organisation"
        );

        // Assert
        assertThat(organisation).isNotNull();
        assertThat(organisation.getStreetAddress()).isEqualTo(streetAddress);
        assertThat(organisation.getPostalAddress()).isEqualTo(postalAddress);
        assertThat(organisation.getPhone1()).isEqualTo(phone1);
        assertThat(organisation.getPhone2()).isEqualTo(phone2);
        assertThat(organisation.getElectronicAddress()).isEqualTo(electronicAddress);
        assertThat(organisation.getOrganisationName()).isEqualTo("Test Organisation");
    }

    @Test
    @DisplayName("Should create OrganisationDto with minimal data")
    void shouldCreateOrganisationDtoWithMinimalData() {
        // Arrange & Act
        OrganisationDto organisation = new OrganisationDto(
            null, null, null, null, null, "Minimal Org"
        );

        // Assert
        assertThat(organisation).isNotNull();
        assertThat(organisation.getStreetAddress()).isNull();
        assertThat(organisation.getPostalAddress()).isNull();
        assertThat(organisation.getPhone1()).isNull();
        assertThat(organisation.getPhone2()).isNull();
        assertThat(organisation.getElectronicAddress()).isNull();
        assertThat(organisation.getOrganisationName()).isEqualTo("Minimal Org");
    }

    @Test
    @DisplayName("Should create OrganisationDto with street address only")
    void shouldCreateOrganisationDtoWithStreetAddressOnly() {
        // Arrange
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "789 Business Blvd", "Capital City", "CA", "95814", "USA"
        );

        // Act
        OrganisationDto organisation = new OrganisationDto(
            streetAddress, null, null, null, null, "Business Org"
        );

        // Assert
        assertThat(organisation).isNotNull();
        assertThat(organisation.getStreetAddress()).isEqualTo(streetAddress);
        assertThat(organisation.getPostalAddress()).isNull();
        assertThat(organisation.getOrganisationName()).isEqualTo("Business Org");
    }

    @Test
    @DisplayName("Should create OrganisationDto with phone numbers only")
    void shouldCreateOrganisationDtoWithPhoneNumbersOnly() {
        // Arrange
        TelephoneNumberDto phone1 = new TelephoneNumberDto(
            "1", "800", null, "555-0000", null, null, null, null
        );

        TelephoneNumberDto phone2 = new TelephoneNumberDto(
            "1", "888", null, "555-1111", null, null, null, null
        );

        // Act
        OrganisationDto organisation = new OrganisationDto(
            null, null, phone1, phone2, null, "Phone Only Org"
        );

        // Assert
        assertThat(organisation).isNotNull();
        assertThat(organisation.getPhone1()).isEqualTo(phone1);
        assertThat(organisation.getPhone2()).isEqualTo(phone2);
        assertThat(organisation.getOrganisationName()).isEqualTo("Phone Only Org");
    }

    @Test
    @DisplayName("Should create OrganisationDto with electronic address only")
    void shouldCreateOrganisationDtoWithElectronicAddressOnly() {
        // Arrange
        ElectronicAddressDto electronicAddress = new ElectronicAddressDto(
            null, null, "contact@digital.org", null, "https://digital.org", null, null, null
        );

        // Act
        OrganisationDto organisation = new OrganisationDto(
            null, null, null, null, electronicAddress, "Digital Org"
        );

        // Assert
        assertThat(organisation).isNotNull();
        assertThat(organisation.getElectronicAddress()).isEqualTo(electronicAddress);
        assertThat(organisation.getOrganisationName()).isEqualTo("Digital Org");
    }

    @Test
    @DisplayName("Should support null organisation name")
    void shouldSupportNullOrganisationName() {
        // Arrange & Act
        OrganisationDto organisation = new OrganisationDto(
            null, null, null, null, null, null
        );

        // Assert
        assertThat(organisation).isNotNull();
        assertThat(organisation.getOrganisationName()).isNull();
    }

    @Test
    @DisplayName("Should maintain field values after creation")
    void shouldMaintainFieldValuesAfterCreation() {
        // Arrange
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "555 Test Ave", "TestCity", "TS", "55555", "USA"
        );

        TelephoneNumberDto phone = new TelephoneNumberDto(
            "1", "555", "123", "4567", "890", "9", "+", "tel:+15551234567890"
        );

        // Act
        OrganisationDto organisation = new OrganisationDto(
            streetAddress, null, phone, null, null, "Test Org"
        );

        // Assert - Verify all fields maintain their values
        assertThat(organisation.getStreetAddress().getStreetDetail()).isEqualTo("555 Test Ave");
        assertThat(organisation.getStreetAddress().getTownDetail()).isEqualTo("TestCity");
        assertThat(organisation.getStreetAddress().getStateOrProvince()).isEqualTo("TS");
        assertThat(organisation.getStreetAddress().getPostalCode()).isEqualTo("55555");
        assertThat(organisation.getStreetAddress().getCountry()).isEqualTo("USA");
        assertThat(organisation.getPhone1().getCountryCode()).isEqualTo("1");
        assertThat(organisation.getPhone1().getAreaCode()).isEqualTo("555");
        assertThat(organisation.getPhone1().getCityCode()).isEqualTo("123");
        assertThat(organisation.getPhone1().getLocalNumber()).isEqualTo("4567");
        assertThat(organisation.getPhone1().getExt()).isEqualTo("890");
        assertThat(organisation.getOrganisationName()).isEqualTo("Test Org");
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void shouldHandleEqualsAndHashCodeCorrectly() {
        // Arrange
        CustomerDto.StreetAddressDto streetAddress = new CustomerDto.StreetAddressDto(
            "123 Main St", "City", "ST", "12345", "USA"
        );

        OrganisationDto org1 = new OrganisationDto(
            streetAddress, null, null, null, null, "Test"
        );

        OrganisationDto org2 = new OrganisationDto(
            streetAddress, null, null, null, null, "Test"
        );

        // Assert
        assertThat(org1).isEqualTo(org2);
        assertThat(org1.hashCode()).isEqualTo(org2.hashCode());
    }
}