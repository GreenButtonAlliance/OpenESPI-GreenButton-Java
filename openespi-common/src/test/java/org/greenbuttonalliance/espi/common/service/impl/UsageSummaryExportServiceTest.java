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
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for UsageSummaryExportService namespace handling.
 */
@DisplayName("UsageSummaryExportService Namespace Tests")
class UsageSummaryExportServiceTest {

    private UsageSummaryExportService usageSummaryExportService;

    @BeforeEach
    void setUp() {
        usageSummaryExportService = new UsageSummaryExportService();
        usageSummaryExportService.init();
    }

    @Test
    @DisplayName("Should declare ONLY espi namespace")
    void shouldDeclareEspiNamespaceOnly() {
        // Arrange
        UsageSummaryDto usageSummary = new UsageSummaryDto();
        usageSummary.setCurrency("USD");
        
        UsageAtomEntryDto entry = new UsageAtomEntryDto(
            "urn:uuid:550e8400-e29b-51d4-a716-446655440011",
            "Usage Summary Test",
            usageSummary
        );

        // Act
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        usageSummaryExportService.exportDto(entry, stream);
        String xml = stream.toString();

        // Assert
        assertThat(xml).contains("xmlns:espi=\"http://naesb.org/espi\"");
        assertThat(xml).contains("xmlns:atom=\"http://www.w3.org/2005/Atom\"");
        assertThat(xml).contains("<espi:UsageSummary>");
        assertThat(xml).contains("<espi:currency>USD</espi:currency>");
    }
}
