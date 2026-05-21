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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("IntervalBlockRESTController Mock MVC Tests")
public class IntervalBlockRESTControllerTest extends AbstractControllerMockTest {

    @Nested
    @DisplayName("GET /espi/1_1/resource/IntervalBlock")
    class GetAllIntervalBlocks {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK with list of interval blocks for admin")
        void shouldReturn200ForAdmin() throws Exception {
            IntervalBlockEntity entity = new IntervalBlockEntity();
            IntervalBlockDto dto = new IntervalBlockDto();

            when(intervalBlockRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(intervalBlockMapper.toDto(any(IntervalBlockEntity.class)))
                    .thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 200 OK for FB_15 scope")
        void shouldReturn200ForFB15() throws Exception {
            when(intervalBlockRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/IntervalBlock/{intervalBlockId}")
    class GetIntervalBlock {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when interval block exists and user is admin")
        void shouldReturn200WhenExists() throws Exception {
            UUID id = UUID.randomUUID();
            IntervalBlockEntity entity = new IntervalBlockEntity();
            IntervalBlockDto dto = new IntervalBlockDto();

            when(intervalBlockRepository.findById(id)).thenReturn(Optional.of(entity));
            when(intervalBlockMapper.toDto(entity)).thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock/" + id)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when interval block does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(intervalBlockRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/IntervalBlock/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/IntervalBlock")
    class GetSubscriptionIntervalBlocks {

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 200 OK with list of interval blocks")
        void shouldReturn200() throws Exception {
            UUID subId = UUID.randomUUID();
            UUID upId = UUID.randomUUID();
            UUID mrId = UUID.randomUUID();
            IntervalBlockEntity entity = new IntervalBlockEntity();
            IntervalBlockDto dto = new IntervalBlockDto();

            when(intervalBlockRepository.findAllByMeterReadingId(mrId))
                    .thenReturn(List.of(entity));
            when(intervalBlockMapper.toDto(any(IntervalBlockEntity.class)))
                    .thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + subId + "/UsagePoint/" + upId + "/MeterReading/" + mrId + "/IntervalBlock")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            UUID subId = UUID.randomUUID();
            UUID upId = UUID.randomUUID();
            UUID mrId = UUID.randomUUID();
            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + subId + "/UsagePoint/" + upId + "/MeterReading/" + mrId + "/IntervalBlock"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}/MeterReading/{meterReadingId}/IntervalBlock/{intervalBlockId}")
    class GetSubscriptionIntervalBlock {

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        @DisplayName("Should return 200 OK when interval block exists")
        void shouldReturn200WhenExists() throws Exception {
            UUID subId = UUID.randomUUID();
            UUID upId = UUID.randomUUID();
            UUID mrId = UUID.randomUUID();
            UUID ibId = UUID.randomUUID();
            IntervalBlockEntity entity = new IntervalBlockEntity();
            IntervalBlockDto dto = new IntervalBlockDto();

            when(intervalBlockRepository.findById(ibId)).thenReturn(Optional.of(entity));
            when(intervalBlockMapper.toDto(entity)).thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + subId + "/UsagePoint/" + upId + "/MeterReading/" + mrId + "/IntervalBlock/" + ibId)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }
    }
}
