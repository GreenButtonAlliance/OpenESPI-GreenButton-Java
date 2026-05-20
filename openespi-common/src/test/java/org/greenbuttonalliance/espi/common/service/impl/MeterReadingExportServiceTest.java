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

package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MeterReadingExportService Namespace Tests")
class MeterReadingExportServiceTest {

    private MeterReadingExportService meterReadingExportService;

    @BeforeEach
    void setUp() {
        meterReadingExportService = new MeterReadingExportService();
        meterReadingExportService.init();
    }

    @Test
    @DisplayName("Should declare ONLY espi namespace (NOT customer namespace)")
    void shouldDeclareEspiNamespaceOnly() {
        // Arrange
        MeterReadingDto meterReading = new MeterReadingDto();
        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440011",
            "Meter Reading Test",
            meterReading
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        meterReadingExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert - ESPI namespace PRESENT
        assertThat(xml)
            .as("XML should declare espi namespace")
            .contains("xmlns:espi=\"http://naesb.org/espi\"");

        // Assert - Customer namespace ABSENT
        assertThat(xml)
            .as("XML should NOT declare customer namespace")
            .doesNotContain("xmlns:cust")
            .doesNotContain("http://naesb.org/espi/customer");

        // Assert - Atom namespace is declared with atom prefix
        assertThat(xml)
            .as("XML should declare Atom namespace with atom prefix")
            .contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");

        // Assert - MeterReading content with espi prefix
        assertThat(xml)
            .as("MeterReading should use espi prefix")
            .contains("<espi:MeterReading");
    }
}
