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

import org.greenbuttonalliance.espi.common.domain.usage.MeterReadingEntity;
import org.greenbuttonalliance.espi.common.dto.usage.MeterReadingDto;
import org.greenbuttonalliance.espi.common.mapper.usage.MeterReadingMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.MeterReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeterReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterReadingRepository meterReadingRepository;

    @MockitoBean
    private MeterReadingMapper meterReadingMapper;

    @MockitoBean
    private org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator requestValidator;

    private UUID meterReadingId;
    private MeterReadingEntity meterReadingEntity;
    private MeterReadingDto meterReadingDto;

    @BeforeEach
    void setUp() {
        meterReadingId = UUID.randomUUID();
        meterReadingEntity = new MeterReadingEntity();
        meterReadingEntity.setId(meterReadingId);

        meterReadingDto = new MeterReadingDto();

        Page<MeterReadingEntity> page = new PageImpl<>(Collections.singletonList(meterReadingEntity));

        when(requestValidator.toPageable(eq(50), eq(0))).thenReturn(PageRequest.of(0, 50));
        when(requestValidator.toPageable(eq(50), eq(-1)))
            .thenThrow(new ResponseStatusException(BAD_REQUEST, "'offset' must be 0 or greater"));
        when(meterReadingRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(meterReadingRepository.findById(meterReadingId)).thenReturn(Optional.of(meterReadingEntity));
        when(meterReadingMapper.toDto(any(MeterReadingEntity.class))).thenReturn(meterReadingDto);
    }

    @Nested
    @DisplayName("GET /MeterReading")
    class GetAllTests {
        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getAll_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/MeterReading")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getAll_InvalidOffset_Returns400() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/MeterReading")
                    .param("offset", "-1")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
        }

        @Test
        void getAll_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/MeterReading")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /MeterReading/{id}")
    class GetByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getById_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/MeterReading/{id}", meterReadingId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getById_NotFound_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(meterReadingRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/MeterReading/{id}", unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
        }
    }
}

