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

import org.greenbuttonalliance.espi.common.domain.usage.ReadingTypeEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ReadingTypeDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ReadingTypeMapper;
import org.greenbuttonalliance.espi.common.service.ReadingTypeService;
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

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReadingTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingTypeService readingTypeService;

    @MockitoBean
    private ReadingTypeMapper readingTypeMapper;

    private final UUID readingTypeId = UUID.randomUUID();
    private ReadingTypeEntity readingTypeEntity;
    private ReadingTypeDto readingTypeDto;

    @BeforeEach
    void setUp() {
        readingTypeEntity = new ReadingTypeEntity();
        readingTypeEntity.setId(readingTypeId);

        readingTypeDto = new ReadingTypeDto();
        readingTypeDto.setAccumulationBehaviour("SUMMATION");

        when(readingTypeService.findById(any(UUID.class))).thenReturn(readingTypeEntity);
        when(readingTypeService.findAll()).thenReturn(Collections.singletonList(readingTypeEntity));
        when(readingTypeMapper.toDto(any(ReadingTypeEntity.class))).thenReturn(readingTypeDto);
    }

    @Nested
    @DisplayName("Get All Reading Types Tests")
    class GetAllReadingTypesTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ReadingType - Should return 200 and List of ReadingTypes")
        void getAllReadingTypes_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ReadingType")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[0].accumulationBehaviour").value("SUMMATION"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ReadingType - Should handle pagination parameters")
        void getAllReadingTypes_WithPagination_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ReadingType")
                    .param("limit", "10")
                    .param("offset", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /ReadingType - Should return 401 for unauthenticated user")
        void getAllReadingTypes_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ReadingType")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Reading Type By ID Tests")
    class GetReadingTypeByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ReadingType/{id} - Should return 200 and ReadingType")
        void getReadingType_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ReadingType/" + readingTypeId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.accumulationBehaviour").value("SUMMATION"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ReadingType/{id} - Should return 404 for unknown ID")
        void getReadingType_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(readingTypeService.findById(unknownId)).thenReturn(null);

            mockMvc.perform(get("/espi/1_1/resource/ReadingType/" + unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }
}
