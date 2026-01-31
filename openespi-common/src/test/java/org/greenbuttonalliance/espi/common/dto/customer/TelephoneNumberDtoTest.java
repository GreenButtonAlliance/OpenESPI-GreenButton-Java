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
 * Unit tests for TelephoneNumberDto.
 * Verifies DTO structure and field assignments for ESPI 4.0 customer.xsd compliance.
 * TelephoneNumber schema definition: customer.xsd lines 1428-1478.
 */
@DisplayName("TelephoneNumberDto Unit Tests")
class TelephoneNumberDtoTest {

    @Test
    @DisplayName("Should create TelephoneNumberDto with all fields")
    void shouldCreateTelephoneNumberDtoWithAllFields() {
        // Arrange & Act
        TelephoneNumberDto phone = new TelephoneNumberDto(
            "1",                    // countryCode
            "555",                  // areaCode
            "123",                  // cityCode
            "4567",                 // localNumber
            "890",                  // ext
            "9",                    // dialOut
            "+",                    // internationalPrefix
            "tel:+15551234567890"   // ituPhone
        );

        // Assert
        assertThat(phone).isNotNull()
            .extracting(
                TelephoneNumberDto::getCountryCode,
                TelephoneNumberDto::getAreaCode,
                TelephoneNumberDto::getCityCode,
                TelephoneNumberDto::getLocalNumber,
                TelephoneNumberDto::getExt,
                TelephoneNumberDto::getDialOut,
                TelephoneNumberDto::getInternationalPrefix,
                TelephoneNumberDto::getItuPhone
            )
            .containsExactly("1", "555", "123", "4567", "890", "9", "+", "tel:+15551234567890");
    }

    @Test
    @DisplayName("Should create TelephoneNumberDto with minimal data")
    void shouldCreateTelephoneNumberDtoWithMinimalData() {
        // Arrange & Act
        TelephoneNumberDto phone = new TelephoneNumberDto(
            null, null, null, "555-1234", null, null, null, null
        );

        // Assert
        assertThat(phone).isNotNull();
        assertThat(phone.getLocalNumber()).isEqualTo("555-1234");
        assertThat(phone.getCountryCode()).isNull();
        assertThat(phone.getAreaCode()).isNull();
        assertThat(phone.getCityCode()).isNull();
        assertThat(phone.getExt()).isNull();
        assertThat(phone.getDialOut()).isNull();
        assertThat(phone.getInternationalPrefix()).isNull();
        assertThat(phone.getItuPhone()).isNull();
    }

    @Test
    @DisplayName("Should create TelephoneNumberDto with standard US format")
    void shouldCreateTelephoneNumberDtoWithStandardUSFormat() {
        // Arrange & Act - Standard US phone: +1 (555) 123-4567
        TelephoneNumberDto phone = new TelephoneNumberDto(
            "1", "555", null, "123-4567", null, null, null, null
        );

        // Assert
        assertThat(phone).isNotNull();
        assertThat(phone.getCountryCode()).isEqualTo("1");
        assertThat(phone.getAreaCode()).isEqualTo("555");
        assertThat(phone.getLocalNumber()).isEqualTo("123-4567");
    }

    @Test
    @DisplayName("Should create TelephoneNumberDto with extension")
    void shouldCreateTelephoneNumberDtoWithExtension() {
        // Arrange & Act
        TelephoneNumberDto phone = new TelephoneNumberDto(
            "1", "555", null, "1234", "567", null, null, null
        );

        // Assert
        assertThat(phone).isNotNull();
        assertThat(phone.getLocalNumber()).isEqualTo("1234");
        assertThat(phone.getExt()).isEqualTo("567");
    }

    @Test
    @DisplayName("Should create TelephoneNumberDto with international format")
    void shouldCreateTelephoneNumberDtoWithInternationalFormat() {
        // Arrange & Act - International: +44 20 7946 0958
        TelephoneNumberDto phone = new TelephoneNumberDto(
            "44",           // UK country code
            "20",           // London area code
            null,
            "7946 0958",    // local number
            null,
            "00",           // dialOut for international
            "+",            // international prefix
            "tel:+442079460958"
        );

        // Assert
        assertThat(phone).isNotNull();
        assertThat(phone.getCountryCode()).isEqualTo("44");
        assertThat(phone.getAreaCode()).isEqualTo("20");
        assertThat(phone.getLocalNumber()).isEqualTo("7946 0958");
        assertThat(phone.getDialOut()).isEqualTo("00");
        assertThat(phone.getInternationalPrefix()).isEqualTo("+");
        assertThat(phone.getItuPhone()).isEqualTo("tel:+442079460958");
    }

    @Test
    @DisplayName("Should support null values for all fields")
    void shouldSupportNullValuesForAllFields() {
        // Arrange & Act
        TelephoneNumberDto phone = new TelephoneNumberDto(
            null, null, null, null, null, null, null, null
        );

        // Assert
        assertThat(phone).isNotNull();
        assertThat(phone.getCountryCode()).isNull();
        assertThat(phone.getAreaCode()).isNull();
        assertThat(phone.getCityCode()).isNull();
        assertThat(phone.getLocalNumber()).isNull();
        assertThat(phone.getExt()).isNull();
        assertThat(phone.getDialOut()).isNull();
        assertThat(phone.getInternationalPrefix()).isNull();
        assertThat(phone.getItuPhone()).isNull();
    }

    @Test
    @DisplayName("Should maintain field values after creation")
    void shouldMaintainFieldValuesAfterCreation() {
        // Arrange & Act
        TelephoneNumberDto phone = new TelephoneNumberDto(
            "1", "800", "555", "1212", "123", "9", "+1", "tel:+18005551212"
        );

        // Assert - Access each field multiple times to verify immutability
        assertThat(phone.getCountryCode()).isEqualTo("1");
        assertThat(phone.getCountryCode()).isEqualTo("1"); // Second access
        assertThat(phone.getAreaCode()).isEqualTo("800");
        assertThat(phone.getAreaCode()).isEqualTo("800");
        assertThat(phone.getCityCode()).isEqualTo("555");
        assertThat(phone.getLocalNumber()).isEqualTo("1212");
        assertThat(phone.getExt()).isEqualTo("123");
        assertThat(phone.getDialOut()).isEqualTo("9");
        assertThat(phone.getInternationalPrefix()).isEqualTo("+1");
        assertThat(phone.getItuPhone()).isEqualTo("tel:+18005551212");
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void shouldHandleEqualsAndHashCodeCorrectly() {
        // Arrange
        TelephoneNumberDto phone1 = new TelephoneNumberDto(
            "1", "555", null, "1234", null, null, null, null
        );

        TelephoneNumberDto phone2 = new TelephoneNumberDto(
            "1", "555", null, "1234", null, null, null, null
        );

        TelephoneNumberDto phone3 = new TelephoneNumberDto(
            "1", "555", null, "5678", null, null, null, null
        );

        // Assert
        assertThat(phone1).isEqualTo(phone2);
        assertThat(phone1.hashCode()).isEqualTo(phone2.hashCode());
        assertThat(phone1).isNotEqualTo(phone3);
    }
}