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

import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.mapper.usage.UsagePointMapper;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiAccessValidator;
import org.greenbuttonalliance.espi.datacustodian.web.api.support.ApiRequestValidator;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsagePointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsagePointRepository usagePointRepository;

    @MockitoBean
    private UsagePointMapper usagePointMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private ApiRequestValidator requestValidator;

    @MockitoBean
    private ApiAccessValidator accessValidator;

    private UUID usagePointId;
    private UUID subscriptionId;
    private UsagePointEntity usagePointEntity;
    private UsagePointDto usagePointDto;

    @BeforeEach
    void setUp() {
        usagePointId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();

        usagePointEntity = new UsagePointEntity();
        usagePointEntity.setId(usagePointId);
        usagePointDto = new UsagePointDto();

        Page<UsagePointEntity> page = new PageImpl<>(Collections.singletonList(usagePointEntity));

        when(accessValidator.isAdmin(any())).thenReturn(true);
        when(requestValidator.toPageable(eq(50), eq(0))).thenReturn(PageRequest.of(0, 50));
        when(requestValidator.toPageable(eq(0), eq(0)))
            .thenThrow(new ResponseStatusException(BAD_REQUEST, "'limit' must be greater than 0"));

        when(usagePointRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(usagePointRepository.findById(usagePointId)).thenReturn(Optional.of(usagePointEntity));
        when(usagePointMapper.toDto(any(UsagePointEntity.class))).thenReturn(usagePointDto);
    }

    @Nested
    @DisplayName("GET /UsagePoint")
    class GetAllTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        void getAll_AsAdmin_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsagePoint")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        void getAll_InvalidLimit_Returns400() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/UsagePoint")
                    .param("limit", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getAll_ThirdPartyWithoutToken_Returns403() throws Exception {
            when(accessValidator.isAdmin(any())).thenReturn(false);
            when(accessValidator.requireSubscriptionId(eq(null)))
                .thenThrow(new ResponseStatusException(FORBIDDEN, "Bearer token is required"));

            mockMvc.perform(get("/espi/1_1/resource/UsagePoint")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /Subscription/{subscriptionId}/UsagePoint")
    class SubscriptionTests {
        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getSubscriptionUsagePoints_AccessDenied_Returns403() throws Exception {
            doThrow(new ResponseStatusException(FORBIDDEN, "Token is not authorized for requested subscription"))
                .when(accessValidator).enforceSubscriptionPathAccess(any(), eq(null), eq(subscriptionId));

            mockMvc.perform(get("/espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint", subscriptionId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_FB_15_READ_3rd_party")
        void getSubscriptionUsagePoint_NotFound_Returns404() throws Exception {
            doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "UsagePoint not found in subscription"))
                .when(accessValidator).enforceUsagePointInSubscription(eq(subscriptionId), eq(usagePointId));

            mockMvc.perform(get("/espi/1_1/resource/Subscription/{subscriptionId}/UsagePoint/{usagePointId}",
                    subscriptionId, usagePointId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
        }
    }
}

