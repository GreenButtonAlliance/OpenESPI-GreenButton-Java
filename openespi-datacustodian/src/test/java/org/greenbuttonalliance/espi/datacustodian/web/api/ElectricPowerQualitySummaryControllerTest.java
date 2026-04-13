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

import org.greenbuttonalliance.espi.common.domain.usage.ElectricPowerQualitySummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ElectricPowerQualitySummaryDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ElectricPowerQualitySummaryMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ElectricPowerQualitySummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ElectricPowerQualitySummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElectricPowerQualitySummaryRepository electricPowerQualitySummaryRepository;

    @MockitoBean
    private ElectricPowerQualitySummaryMapper electricPowerQualitySummaryMapper;

    private final UUID summaryId = UUID.randomUUID();
    private ElectricPowerQualitySummaryEntity summaryEntity;
    private ElectricPowerQualitySummaryDto summaryDto;

    @BeforeEach
    void setUp() {
        summaryEntity = new ElectricPowerQualitySummaryEntity();
        summaryEntity.setId(summaryId);

        summaryDto = new ElectricPowerQualitySummaryDto();
        summaryDto.setFlickerPst(123L);

        Page<ElectricPowerQualitySummaryEntity> page = new PageImpl<>(Collections.singletonList(summaryEntity));

        when(electricPowerQualitySummaryRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(electricPowerQualitySummaryRepository.findById(summaryId)).thenReturn(Optional.of(summaryEntity));
        when(electricPowerQualitySummaryMapper.toDto(any(ElectricPowerQualitySummaryEntity.class))).thenReturn(summaryDto);
    }

    @Nested
    @DisplayName("Get All Electric Power Quality Summaries Tests")
    class GetAllSummariesTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ElectricPowerQualitySummary - Should return 200 and List of Summaries")
        void getAllSummaries_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ElectricPowerQualitySummary")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[0].flickerPst").value(123));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ElectricPowerQualitySummary - Should handle pagination")
        void getAllSummaries_WithPagination_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ElectricPowerQualitySummary")
                    .param("limit", "10")
                    .param("offset", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /ElectricPowerQualitySummary - Should return 401 for unauthenticated")
        void getAllSummaries_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ElectricPowerQualitySummary")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Electric Power Quality Summary By ID Tests")
    class GetSummaryByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ElectricPowerQualitySummary/{id} - Should return 200 and Summary")
        void getSummary_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ElectricPowerQualitySummary/" + summaryId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.flickerPst").value(123));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ElectricPowerQualitySummary/{id} - Should return 404 for unknown ID")
        void getSummary_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(electricPowerQualitySummaryRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/ElectricPowerQualitySummary/" + unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }
}
