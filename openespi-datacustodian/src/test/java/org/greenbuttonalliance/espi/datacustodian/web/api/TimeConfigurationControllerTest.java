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

import org.greenbuttonalliance.espi.common.domain.usage.TimeConfigurationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.TimeConfigurationDto;
import org.greenbuttonalliance.espi.common.mapper.usage.TimeConfigurationMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.TimeConfigurationRepository;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.TimeConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TimeConfigurationController Tests")
public class TimeConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeConfigurationRepository timeConfigurationRepository;

    @MockitoBean
    private TimeConfigurationMapper timeConfigurationMapper;

    @MockitoBean
    private TimeConfigurationService timeConfigurationService;

    @MockitoBean
    private DtoExportService exportService;

    private static final String BASE_URL = "/espi/1_1/resource/TimeConfiguration";
    private UUID timeConfigurationId;

    @BeforeEach
    void setUp() {
        timeConfigurationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("GET " + BASE_URL)
    class IndexTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("index returns 200 and Atom feed")
        void index_Returns200() throws Exception {
            AtomFeedDto feed = new AtomFeedDto(UUID.randomUUID().toString(), "Time Configurations", 
                    OffsetDateTime.now(), OffsetDateTime.now(), null, new ArrayList<>());
            when(exportService.createTimeConfigurationsFeed()).thenReturn(feed);

            mockMvc.perform(get(BASE_URL)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));
        }
    }

    @Nested
    @DisplayName("GET " + BASE_URL + "/{timeConfigurationId}")
    class ShowTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("show returns 200 and Atom entry")
        void show_Returns200() throws Exception {
            AtomEntryDto entry = new UsageAtomEntryDto(UUID.randomUUID().toString(), "Time Configuration", 
                    OffsetDateTime.now(), OffsetDateTime.now(), null, new TimeConfigurationDto());
            when(exportService.createTimeConfigurationEntry(timeConfigurationId)).thenReturn(entry);

            mockMvc.perform(get(BASE_URL + "/{id}", timeConfigurationId)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("show returns 404 when not found")
        void show_Returns404() throws Exception {
            when(exportService.createTimeConfigurationEntry(timeConfigurationId)).thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/{id}", timeConfigurationId)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST " + BASE_URL)
    class CreateTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("create returns 501 Not Implemented")
        void create_Returns501() throws Exception {
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_ATOM_XML_VALUE)
                    .content("<entry></entry>"))
                    .andExpect(status().isNotImplemented());
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_URL + "/{timeConfigurationId}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("delete returns 204 No Content when successful")
        void delete_Returns204() throws Exception {
            TimeConfigurationEntity entity = new TimeConfigurationEntity();
            when(timeConfigurationService.findById(timeConfigurationId)).thenReturn(entity);

            mockMvc.perform(delete(BASE_URL + "/{id}", timeConfigurationId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("delete returns 404 when not found")
        void delete_Returns404() throws Exception {
            when(timeConfigurationService.findById(timeConfigurationId)).thenReturn(null);

            mockMvc.perform(delete(BASE_URL + "/{id}", timeConfigurationId))
                    .andExpect(status().isNotFound());
        }
    }
}
