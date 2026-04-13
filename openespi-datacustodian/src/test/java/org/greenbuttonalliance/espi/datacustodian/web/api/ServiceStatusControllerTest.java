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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.ServiceStatusDto;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ServiceStatusController Tests")
public class ServiceStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private DtoExportService exportService;

    private static final String SERVICE_STATUS_URL = "/espi/1_1/resource/ServiceStatus";

    @Test
    @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
    @DisplayName("index returns 200 and Atom entry for authorized user")
    void index_Returns200() throws Exception {
        String status = "1";
        
        AtomEntryDto entry = new UsageAtomEntryDto(UUID.randomUUID().toString(), "ServiceStatus", 
                new ServiceStatusDto(status));
        when(exportService.createServiceStatusEntry(anyString())).thenReturn(entry);

        mockMvc.perform(get(SERVICE_STATUS_URL)
                .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
    @DisplayName("index returns 200 with default status '0' when no token provided")
    void index_NoToken_ReturnsDefaultStatus() throws Exception {
        String defaultStatus = "0";
        
        AtomEntryDto entry = new UsageAtomEntryDto(UUID.randomUUID().toString(), "ServiceStatus", 
                new ServiceStatusDto(defaultStatus));
        when(exportService.createServiceStatusEntry(defaultStatus)).thenReturn(entry);

        mockMvc.perform(get(SERVICE_STATUS_URL)
                .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));
    }

    @Test
    @DisplayName("index returns 401 for unauthenticated user")
    void index_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get(SERVICE_STATUS_URL)
                .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                .andExpect(status().isUnauthorized());
    }
}
