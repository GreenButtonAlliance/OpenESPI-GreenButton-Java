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

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAccountEntity;
import org.greenbuttonalliance.espi.common.dto.customer.CustomerAccountDto;
import org.greenbuttonalliance.espi.common.mapper.customer.CustomerAccountMapper;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAccountService;
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
public class CustomerAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerAccountService customerAccountService;

    @MockitoBean
    private CustomerAccountMapper customerAccountMapper;

    private final UUID customerAccountId = UUID.randomUUID();
    private CustomerAccountEntity customerAccountEntity;
    private CustomerAccountDto customerAccountDto;

    @BeforeEach
    void setUp() {
        customerAccountEntity = new CustomerAccountEntity();
        customerAccountEntity.setId(customerAccountId);

        customerAccountDto = new CustomerAccountDto();
        customerAccountDto.setAccountId("ACCT-12345");

        when(customerAccountService.findAll()).thenReturn(Collections.singletonList(customerAccountEntity));
        when(customerAccountService.findById(customerAccountId)).thenReturn(Optional.of(customerAccountEntity));
        when(customerAccountMapper.toDto(any(CustomerAccountEntity.class))).thenReturn(customerAccountDto);
    }

    @Nested
    @DisplayName("Get All Customer Accounts Tests")
    class GetAllCustomerAccountsTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /CustomerAccount - Should return 200 and List of Customer Accounts")
        void getAllCustomerAccounts_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[0].accountId").value("ACCT-12345"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /CustomerAccount - Should handle pagination")
        void getAllCustomerAccounts_WithPagination_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount")
                    .param("limit", "10")
                    .param("offset", "0")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /CustomerAccount - Should return 401 for unauthenticated")
        void getAllCustomerAccounts_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Customer Account By ID Tests")
    class GetCustomerAccountByIdTests {
        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /CustomerAccount/{id} - Should return 200 and Customer Account")
        void getCustomerAccount_Returns200() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount/" + customerAccountId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.accountId").value("ACCT-12345"));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /CustomerAccount/{id} - Should return 404 for unknown ID")
        void getCustomerAccount_UnknownId_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(customerAccountService.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount/" + unknownId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }
}
