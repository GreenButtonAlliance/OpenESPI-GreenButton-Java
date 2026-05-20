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
 *
 */

package org.greenbuttonalliance.espi.common.service.impl;

import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ApplicationInformationExportService Unit Tests")
public class ApplicationInformationExportServiceTest {

    private ApplicationInformationExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ApplicationInformationExportService();
        exportService.init();
    }

    @Test
    @DisplayName("Export ApplicationInformationDto to XML")
    void exportDto_success() {
        ApplicationInformationDto dto = new ApplicationInformationDto();
        dto.setClientId("test-client-id");
        dto.setClientName("Test Application");
        dto.setDataCustodianId("test-dc-id");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportService.exportDto(dto, baos);

        String xml = baos.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("<espi:ApplicationInformation"));
        assertTrue(xml.contains("<espi:clientId>test-client-id</espi:clientId>"));
        assertTrue(xml.contains("<espi:clientName>Test Application</espi:clientName>"));
        assertTrue(xml.contains("<espi:dataCustodianId>test-dc-id</espi:dataCustodianId>"));
    }
}
