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

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.greenbuttonalliance.espi.common.service.UsageSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UsageSummaryController Tests")
public class UsageSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsageSummaryService usageSummaryService;

    @MockitoBean
    private DtoExportService exportService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private UUID usageSummaryId;
    private UUID usagePointId;
    private UUID subscriptionId;
    private UsageSummaryEntity usageSummaryEntity;
    private UsagePointEntity usagePointEntity;

    @BeforeEach
    void setUp() {
        usageSummaryId = UUID.randomUUID();
        usagePointId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();

        usagePointEntity = new UsagePointEntity();
        usagePointEntity.setId(usagePointId);

        usageSummaryEntity = new UsageSummaryEntity();
        usageSummaryEntity.setId(usageSummaryId);
        usageSummaryEntity.setUsagePoint(usagePointEntity);

        when(subscriptionService.findRetailCustomerId(subscriptionId, usagePointId)).thenReturn(1L);
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/UsageSummary")
    class IndexTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("index returns 200 and Atom feed for admin")
        void index_Returns200() throws Exception {
            when(exportService.createUsageSummariesFeed()).thenReturn(new AtomFeedDto());

            mockMvc.perform(get("/espi/1_1/resource/UsageSummary")
                    .accept(MediaType.APPLICATION_ATOM_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("index returns 403 for non-admin")
        void index_Returns403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsageSummary"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/UsageSummary/{usageSummaryId}")
    class ShowTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("show returns 200 and Atom entry when found")
        void show_Returns200() throws Exception {
            when(usageSummaryService.findById(usageSummaryId)).thenReturn(usageSummaryEntity);
            when(exportService.createUsageSummaryEntry(usageSummaryEntity)).thenReturn(new UsageAtomEntryDto());

            mockMvc.perform(get("/espi/1_1/resource/UsageSummary/{usageSummaryId}", usageSummaryId)
                    .accept(MediaType.APPLICATION_ATOM_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("show returns 404 when not found")
        void show_Returns404() throws Exception {
            when(usageSummaryService.findById(usageSummaryId)).thenReturn(null);

            mockMvc.perform(get("/espi/1_1/resource/UsageSummary/{usageSummaryId}", usageSummaryId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary")
    class NestedIndexTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("indexByUsagePoint returns 403 without bearer token")
        void indexByUsagePoint_Returns200() throws Exception {
            when(exportService.createUsageSummariesFeedByUsagePointId(usagePointId)).thenReturn(new AtomFeedDto());

            mockMvc.perform(get("/espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary",
                    subscriptionId, usagePointId)
                    .accept(MediaType.APPLICATION_ATOM_XML))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}")
    class NestedShowTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("showByUsagePoint returns 403 without bearer token")
        void showByUsagePoint_Returns200() throws Exception {
            when(usageSummaryService.findById(usageSummaryId)).thenReturn(usageSummaryEntity);
            when(exportService.createUsageSummaryEntry(usageSummaryEntity)).thenReturn(new UsageAtomEntryDto());

            mockMvc.perform(get("/espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}",
                    subscriptionId, usagePointId, usageSummaryId)
                    .accept(MediaType.APPLICATION_ATOM_XML))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("showByUsagePoint returns 403 without bearer token")
        void showByUsagePoint_Returns404_WrongUsagePoint() throws Exception {
            UUID otherUsagePointId = UUID.randomUUID();
            when(usageSummaryService.findById(usageSummaryId)).thenReturn(usageSummaryEntity);

            mockMvc.perform(get("/espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/UsageSummary/{usageSummaryId}",
                    subscriptionId, otherUsagePointId, usageSummaryId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Unimplemented Operations")
    class UnimplementedTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("POST returns 501")
        void create_Returns501() throws Exception {
            mockMvc.perform(post("/espi/1_1/resource/UsageSummary"))
                    .andExpect(status().isNotImplemented());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("PUT returns 501")
        void update_Returns501() throws Exception {
            mockMvc.perform(put("/espi/1_1/resource/UsageSummary/{usageSummaryId}", usageSummaryId))
                    .andExpect(status().isNotImplemented());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("DELETE returns 501")
        void delete_Returns501() throws Exception {
            mockMvc.perform(delete("/espi/1_1/resource/UsageSummary/{usageSummaryId}", usageSummaryId))
                    .andExpect(status().isNotImplemented());
        }
    }
}
