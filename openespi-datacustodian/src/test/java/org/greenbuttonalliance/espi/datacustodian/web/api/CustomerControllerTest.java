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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerMapper;
import org.greenbuttonalliance.espi.common.service.customer.CustomerService;
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
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private CustomerMapper customerMapper;

    private final UUID customerId = UUID.randomUUID();
    private CustomerEntity customerEntity;
    private CustomerDto customerDto;

    @BeforeEach
    void setUp() {
        customerEntity = new CustomerEntity();
        customerEntity.setId(customerId);

        customerDto = new CustomerDto();
        customerDto.setCustomerName("John Doe");

        when(customerService.findAll()).thenReturn(Collections.singletonList(customerEntity));
        when(customerService.findById(customerId)).thenReturn(Optional.of(customerEntity));
        when(customerMapper.toDto(any(CustomerEntity.class))).thenReturn(customerDto);
    }

    @Nested
    @DisplayName("Get All Customers Tests")
    class GetAllCustomersTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Customer - Should return 200 and List of Customers")
        void getAllCustomers_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[0].customerName").value("John Doe"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Customer - Should handle pagination")
        void getAllCustomers_WithPagination_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer")
                    .param("limit", "10")
                    .param("offset", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /Customer - Should return 401 for unauthenticated")
        void getAllCustomers_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Customer By ID Tests")
    class GetCustomerByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Customer/{id} - Should return 200 and Customer")
        void getCustomer_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer/" + customerId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.customerName").value("John Doe"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /Customer/{id} - Should return 404 for unknown ID")
        void getCustomer_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(customerService.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/Customer/" + unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }
}
