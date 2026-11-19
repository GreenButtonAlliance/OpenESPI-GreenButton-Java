/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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

import org.greenbuttonalliance.espi.common.domain.usage.UsageSummaryEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsageSummaryDto;
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

@DisplayName("UsageSummaryRESTController Mock MVC Tests")
public class UsageSummaryRESTControllerTest extends AbstractControllerMockTest {

    @Nested
    @DisplayName("GET /espi/1_1/resource/UsageSummary")
    class GetAllUsageSummaries {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsageSummary"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK with list of usage summaries for admin")
        void shouldReturn200ForAdmin() throws Exception {
            UsageSummaryEntity entity = new UsageSummaryEntity();
            UsageSummaryDto dto = new UsageSummaryDto();

            when(usageSummaryRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(usageSummaryMapper.toDto(any(UsageSummaryEntity.class)))
                    .thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/UsageSummary")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "FB_15")
        @DisplayName("FB_15 passes the URL rule, but the root collection is admin only (#286)")
        void fbTokenIsRefusedOnRootCollection() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsageSummary")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isForbidden())
                    .andExpect(refusedByRowScope());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/UsageSummary/{usageSummaryId}")
    class GetUsageSummary {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when usage summary exists")
        void shouldReturn200WhenExists() throws Exception {
            UUID id = UUID.randomUUID();
            UsageSummaryEntity entity = new UsageSummaryEntity();
            UsageSummaryDto dto = new UsageSummaryDto();

            when(usageSummaryRepository.findById(id)).thenReturn(Optional.of(entity));
            when(usageSummaryMapper.toDto(entity)).thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/UsageSummary/" + id)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when usage summary does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(usageSummaryRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/UsageSummary/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/RetailCustomer/{retailCustomerId}/UsagePoint/{usagePointId}/UsageSummary")
    class RetailCustomerScopedTests {

        @Test
        @WithMockUser(authorities = "FB_15")
        @DisplayName("a token with no subscription gets 403 (#286)")
        void tokenWithoutSubscriptionIsRefused() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/RetailCustomer/1/UsagePoint/" + UUID.randomUUID() + "/UsageSummary")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isForbidden())
                    .andExpect(refusedByRowScope());
        }

        @Test
        @WithMockUser(authorities = "FB_15")
        @DisplayName("no summary is served under /Subscription/{id}/...: a Subscription is per grant, so the href would not be repeatable (#290)")
        void noSubscriptionScopedSummaries() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Subscription/" + UUID.randomUUID() + "/UsagePoint/" + UUID.randomUUID() + "/UsageSummary"))
                    .andExpect(status().isNotFound());
        }
    }
}
