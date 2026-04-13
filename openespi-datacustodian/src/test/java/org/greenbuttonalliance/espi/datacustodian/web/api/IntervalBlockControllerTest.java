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

import org.greenbuttonalliance.espi.common.domain.usage.IntervalBlockEntity;
import org.greenbuttonalliance.espi.common.dto.usage.IntervalBlockDto;
import org.greenbuttonalliance.espi.common.mapper.usage.IntervalBlockMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.IntervalBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
public class IntervalBlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntervalBlockRepository intervalBlockRepository;

    @MockitoBean
    private IntervalBlockMapper intervalBlockMapper;

    private final UUID intervalBlockId = UUID.randomUUID();
    private IntervalBlockEntity intervalBlockEntity;
    private IntervalBlockDto intervalBlockDto;

    @BeforeEach
    void setUp() {
        intervalBlockEntity = new IntervalBlockEntity();
        intervalBlockEntity.setId(intervalBlockId);

        intervalBlockDto = new IntervalBlockDto();
        // Set some dummy data if needed for assertions

        when(intervalBlockRepository.findById(any(UUID.class))).thenReturn(Optional.of(intervalBlockEntity));
        when(intervalBlockRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(intervalBlockEntity)));
        when(intervalBlockMapper.toDto(any(IntervalBlockEntity.class))).thenReturn(intervalBlockDto);
    }

    @Nested
    @DisplayName("Get All Interval Blocks Tests")
    class GetAllIntervalBlocksTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /IntervalBlock - Should return 200 and List of IntervalBlocks")
        void getAllIntervalBlocks_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /IntervalBlock - Should handle pagination parameters")
        void getAllIntervalBlocks_WithPagination_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock")
                    .param("limit", "10")
                    .param("offset", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /IntervalBlock - Should return 401 for unauthenticated user")
        void getAllIntervalBlocks_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Interval Block By ID Tests")
    class GetIntervalBlockByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /IntervalBlock/{id} - Should return 200 and IntervalBlock")
        void getIntervalBlock_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock/" + intervalBlockId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /IntervalBlock/{id} - Should return 404 for unknown ID")
        void getIntervalBlock_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(intervalBlockRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock/" + unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }
}
