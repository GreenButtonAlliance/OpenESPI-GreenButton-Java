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
 * */

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.greenbuttonalliance.espi.common.service.UsagePointService;
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

import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RetailCustomerService retailCustomerService;

    @MockitoBean
    private UsagePointService usagePointService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private DtoExportService exportService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private final Long retailCustomerId = 1L;
    private final UUID usagePointId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RetailCustomerEntity customer = new RetailCustomerEntity();
        customer.setId(retailCustomerId);
        when(retailCustomerService.findById(retailCustomerId)).thenReturn(customer);

        UsagePointEntity usagePoint = new UsagePointEntity();
        usagePoint.setId(usagePointId);
        when(usagePointService.findAllByRetailCustomer(any(RetailCustomerEntity.class)))
                .thenReturn(Collections.singletonList(usagePoint));
        when(usagePointService.findById(retailCustomerId, usagePointId)).thenReturn(usagePoint);

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(subscriptionId);
        subscription.setRetailCustomer(customer);
        when(subscriptionService.findById(subscriptionId)).thenReturn(subscription);
        when(subscriptionService.findUsagePointIds(subscriptionId))
                .thenReturn(Collections.singletonList(usagePointId));
        when(subscriptionService.findRetailCustomerId(subscriptionId, usagePointId))
                .thenReturn(retailCustomerId);

        AuthorizationEntity authorization = new AuthorizationEntity();
        authorization.setSubscription(subscription);
        when(authorizationService.findByAccessToken("test-token")).thenReturn(authorization);
    }

    @Nested
    @DisplayName("Bulk Upload Tests")
    class UploadTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("POST /Batch/RetailCustomer/{id}/UsagePoint - Should return 501 Not Implemented")
        void upload_Returns501() throws Exception {
            mockMvc.perform(post("/espi/1_1/resource/Batch/RetailCustomer/" + retailCustomerId + "/UsagePoint")
                    .contentType(MediaType.APPLICATION_ATOM_XML_VALUE)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE)
                    .content("<feed xmlns=\"http://www.w3.org/2005/Atom\"></feed>"))
                    .andExpect(status().isNotImplemented());
        }
    }

    @Nested
    @DisplayName("Download Collection Tests")
    class DownloadCollectionTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Batch/RetailCustomer/{id}/UsagePoint - Should return 200 and XML")
        void downloadCollection_Returns200() throws Exception {
            when(exportService.createUsagePointsFeed(anyList())).thenReturn(new org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto());

            mockMvc.perform(get("/espi/1_1/resource/Batch/RetailCustomer/" + retailCustomerId + "/UsagePoint")
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=GreenButtonDownload.xml"));

            verify(exportService).createUsagePointsFeed(anyList());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Batch/RetailCustomer/{id}/UsagePoint - Should return 404 for unknown customer")
        void downloadCollection_Returns404() throws Exception {
            when(retailCustomerService.findById(99L)).thenReturn(null);
            mockMvc.perform(get("/espi/1_1/resource/Batch/RetailCustomer/99/UsagePoint")
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Download Member Tests")
    class DownloadMemberTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Batch/RetailCustomer/{id}/UsagePoint/{uid} - Should return 200 and XML")
        void downloadMember_Returns200() throws Exception {
            when(exportService.createUsagePointEntry(any(UsagePointEntity.class)))
                    .thenReturn(new org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto());

            mockMvc.perform(get("/espi/1_1/resource/Batch/RetailCustomer/" + retailCustomerId + "/UsagePoint/" + usagePointId)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));

            verify(exportService).createUsagePointEntry(any(UsagePointEntity.class));
        }
    }

    @Nested
    @DisplayName("Subscription Tests")
    class SubscriptionTests {
        @Test
        @WithMockUser(authorities = {"SCOPE_FB_15_READ_3rd_party", "SCOPE_DataCustodian_Admin_Access"})
        @DisplayName("GET /Batch/Subscription/{sid} - Should return 200 and XML")
        void subscription_Returns200() throws Exception {
            when(exportService.createUsagePointsFeedByIds(anyList())).thenReturn(new org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto());

            mockMvc.perform(get("/espi/1_1/resource/Batch/Subscription/" + subscriptionId)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));

            verify(exportService).createUsagePointsFeedByIds(anyList());
        }

        @Test
        @WithMockUser(authorities = {"SCOPE_FB_15_READ_3rd_party", "SCOPE_DataCustodian_Admin_Access"})
        @DisplayName("GET /Batch/Subscription/{sid}/UsagePoint/{uid} - Should return 200 and XML")
        void subscriptionMember_Returns200() throws Exception {
            when(exportService.createUsagePointEntry(eq(usagePointId)))
                    .thenReturn(new org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto());

            mockMvc.perform(get("/espi/1_1/resource/Batch/Subscription/" + subscriptionId + "/UsagePoint/" + usagePointId)
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));

            verify(exportService).createUsagePointEntry(eq(usagePointId));
        }
    }

    @Nested
    @DisplayName("Bulk Tests")
    class BulkTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Batch/Bulk/{bid} - Should return 501 Not Implemented")
        void bulk_Returns501() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Batch/Bulk/123")
                    .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                    .andExpect(status().isNotImplemented());
        }
    }
}
