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

import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.CustomerAtomEntryDto;
import org.greenbuttonalliance.espi.common.service.DtoExportService;
import org.greenbuttonalliance.espi.common.service.RetailCustomerService;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RetailCustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RetailCustomerService retailCustomerService;

    @MockitoBean
    private DtoExportService exportService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private final Long retailCustomerId = 1L;
    private final java.util.UUID subscriptionId = java.util.UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RetailCustomerEntity customer = new RetailCustomerEntity();
        customer.setId(retailCustomerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        when(retailCustomerService.findById(retailCustomerId)).thenReturn(customer);

        when(exportService.createRetailCustomersFeed()).thenReturn(new AtomFeedDto());
        when(exportService.createRetailCustomerEntry(any(RetailCustomerEntity.class))).thenReturn(new CustomerAtomEntryDto());

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(subscriptionId);
        subscription.setRetailCustomer(customer);

        when(subscriptionService.findById(subscriptionId)).thenReturn(subscription);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
    @DisplayName("GET /RetailCustomer - Should return 200 and Atom XML")
    void index_Returns200() throws Exception {
        mockMvc.perform(get("/espi/1_1/resource/RetailCustomer")
                .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
    @DisplayName("GET /RetailCustomer/{id} - Should return 200 and Atom XML")
    void show_Returns200() throws Exception {
        mockMvc.perform(get("/espi/1_1/resource/RetailCustomer/" + retailCustomerId)
                .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML_VALUE));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
    @DisplayName("GET /RetailCustomer/{id} - Should return 404 when not found")
    void show_Returns404() throws Exception {
        when(retailCustomerService.findById(retailCustomerId + 1)).thenReturn(null);

        mockMvc.perform(get("/espi/1_1/resource/RetailCustomer/" + (retailCustomerId + 1))
                .accept(MediaType.APPLICATION_ATOM_XML_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
    @DisplayName("POST /RetailCustomer - Should return 501 Not Implemented")
    void create_Returns501() throws Exception {
        mockMvc.perform(post("/espi/1_1/resource/RetailCustomer")
                .contentType(MediaType.APPLICATION_ATOM_XML_VALUE)
                .content("<entry xmlns=\"http://www.w3.org/2005/Atom\"></entry>"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
    @DisplayName("PUT /RetailCustomer/{id} - Should return 501 Not Implemented")
    void update_Returns501() throws Exception {
        mockMvc.perform(put("/espi/1_1/resource/RetailCustomer/" + retailCustomerId)
                .contentType(MediaType.APPLICATION_ATOM_XML_VALUE)
                .content("<entry xmlns=\"http://www.w3.org/2005/Atom\"></entry>"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
    @DisplayName("DELETE /RetailCustomer/{id} - Should return 501 Not Implemented")
    void delete_Returns501() throws Exception {
        mockMvc.perform(delete("/espi/1_1/resource/RetailCustomer/" + retailCustomerId))
                .andExpect(status().isNotImplemented());
    }
}
