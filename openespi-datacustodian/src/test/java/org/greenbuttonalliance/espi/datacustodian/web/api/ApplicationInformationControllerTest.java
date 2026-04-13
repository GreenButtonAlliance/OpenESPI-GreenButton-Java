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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.greenbuttonalliance.espi.common.mapper.usage.ApplicationInformationMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApplicationInformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationInformationRepository applicationInformationRepository;

    @MockitoBean
    private ApplicationInformationMapper applicationInformationMapper;

    private final UUID applicationInformationId = UUID.randomUUID();
    private ApplicationInformationEntity applicationInformationEntity;
    private ApplicationInformationDto applicationInformationDto;

    @BeforeEach
    void setUp() {
        applicationInformationEntity = new ApplicationInformationEntity();
        applicationInformationEntity.setId(applicationInformationId);
        applicationInformationEntity.setClientId("test-client");

        applicationInformationDto = new ApplicationInformationDto();
        applicationInformationDto.setClientId("test-client");
        applicationInformationDto.setClientName("Test Application");

        Page<ApplicationInformationEntity> page = new PageImpl<>(Collections.singletonList(applicationInformationEntity));

        when(applicationInformationRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(applicationInformationRepository.findById(applicationInformationId)).thenReturn(Optional.of(applicationInformationEntity));
        when(applicationInformationRepository.existsById(applicationInformationId)).thenReturn(true);
        when(applicationInformationRepository.save(any(ApplicationInformationEntity.class))).thenReturn(applicationInformationEntity);
        
        when(applicationInformationMapper.toDto(any(ApplicationInformationEntity.class))).thenReturn(applicationInformationDto);
        when(applicationInformationMapper.toEntity(any(ApplicationInformationDto.class))).thenReturn(applicationInformationEntity);
    }

    @Nested
    @DisplayName("Get All ApplicationInformation Tests")
    class GetAllApplicationInformationTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ApplicationInformation - Should return 200 and List")
        void getAll_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[0].clientId").value("test-client"));
        }

        @Test
        @DisplayName("GET /ApplicationInformation - Should return 401 for unauthenticated")
        void getAll_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ApplicationInformation - Should return 400 for invalid limit")
        void getAll_InvalidLimit_Returns400() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation")
                    .param("limit", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ApplicationInformation - Should return 400 for invalid offset")
        void getAll_InvalidOffset_Returns400() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation")
                    .param("offset", "-1")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get ApplicationInformation By ID Tests")
    class GetByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ApplicationInformation/{id} - Should return 200 and Resource")
        void getById_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + applicationInformationId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.clientId").value("test-client"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /ApplicationInformation/{id} - Should return 404 for unknown ID")
        void getById_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(applicationInformationRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Create ApplicationInformation Tests")
    class CreateTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("POST /ApplicationInformation - Should return 201 and Created Resource")
        void create_Returns201() throws Exception {
            mockMvc.perform(post("/espi/1_1/resource/ApplicationInformation")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(applicationInformationDto))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.clientId").value("test-client"));
        }
    }

    @Nested
    @DisplayName("Update ApplicationInformation Tests")
    class UpdateTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("PUT /ApplicationInformation/{id} - Should return 200 and Updated Resource")
        void update_Returns200() throws Exception {
            mockMvc.perform(put("/espi/1_1/resource/ApplicationInformation/" + applicationInformationId)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(applicationInformationDto))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientId").value("test-client"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("PUT /ApplicationInformation/{id} - Should return 404 for unknown ID")
        void update_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(applicationInformationRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(put("/espi/1_1/resource/ApplicationInformation/" + unknownId)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(applicationInformationDto))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete ApplicationInformation Tests")
    class DeleteTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("DELETE /ApplicationInformation/{id} - Should return 200")
        void delete_Returns200() throws Exception {
            mockMvc.perform(delete("/espi/1_1/resource/ApplicationInformation/" + applicationInformationId))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("DELETE /ApplicationInformation/{id} - Should return 404 for unknown ID")
        void delete_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(applicationInformationRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(delete("/espi/1_1/resource/ApplicationInformation/" + unknownId))
                    .andExpect(status().isNotFound());
        }
    }
}
