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
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@SpringBootTest
//@ActiveProfiles("test")
@DisplayName("UsagePointController Mock MVC Tests")
public class UsagePointControllerTest extends AbstractControllerMockTest {

    @Nested
    @DisplayName("GET /espi/1_1/resource/UsagePoint")
    class GetAllUsagePoints {

        @BeforeEach
        void setUp() {
            when(usagePointRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsagePoint"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK with list of usage points for admin")
        void shouldReturn200ForAdmin() throws Exception {
            UsagePointEntity entity = new UsagePointEntity();
            UsagePointDto dto = new UsagePointDto();

            when(usagePointRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(usagePointMapper.toDto(any(UsagePointEntity.class)))
                    .thenReturn(dto);

            org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/espi/1_1/resource/UsagePoint")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 200 OK for FB_15 scope")
        void shouldReturn200ForFB15() throws Exception {
            when(usagePointRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/espi/1_1/resource/UsagePoint")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsagePoint"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/UsagePoint/{usagePointId}")
    class GetUsagePoint {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsagePoint/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when usage point exists and user is admin")
        void shouldReturn200WhenExists() throws Exception {
            UUID id = UUID.randomUUID();
            UsagePointEntity entity = new UsagePointEntity();
            UsagePointDto dto = new UsagePointDto();

            when(usagePointRepository.findById(id)).thenReturn(Optional.of(entity));
            when(usagePointMapper.toDto(entity)).thenReturn(dto);

            org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/espi/1_1/resource/UsagePoint/" + id)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when usage point does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(usagePointRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/UsagePoint/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint")
    class GetSubscriptionUsagePoints {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            when(usagePointRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + UUID.randomUUID() + "/UsagePoint"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 200 OK for FB_15 scope")
        void shouldReturn200ForFB15() throws Exception {
            when(usagePointRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            MvcResult result = mockMvc.perform(get("/espi/1_1/resource/Subscription/" + UUID.randomUUID() + "/UsagePoint")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + UUID.randomUUID() + "/UsagePoint"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}")
    class GetSubscriptionUsagePoint {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + UUID.randomUUID() + "/UsagePoint/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 200 OK when usage point exists and user has scope")
        void shouldReturn200WhenExists() throws Exception {
            UUID subId = UUID.randomUUID();
            UUID upId = UUID.randomUUID();
            UsagePointEntity entity = new UsagePointEntity();
            UsagePointDto dto = new UsagePointDto();

            when(usagePointRepository.findById(upId)).thenReturn(Optional.of(entity));
            when(usagePointMapper.toDto(entity)).thenReturn(dto);

            MvcResult result = mockMvc.perform(get("/espi/1_1/resource/Subscription/" + subId + "/UsagePoint/" + upId)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 404 Not Found when usage point does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID subId = UUID.randomUUID();
            UUID upId = UUID.randomUUID();
            when(usagePointRepository.findById(upId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + subId + "/UsagePoint/" + upId))
                    .andExpect(status().isNotFound());
        }
    }
}
