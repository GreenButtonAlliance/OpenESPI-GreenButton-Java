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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CustomerAccountRESTController Mock MVC Tests")
public class CustomerAccountRESTControllerTest extends AbstractControllerMockTest {

    @Nested
    @DisplayName("GET /espi/1_1/resource/CustomerAccount")
    class GetAllCustomerAccounts {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK with list of customer accounts for admin")
        void shouldReturn200ForAdmin() throws Exception {
            CustomerAccountEntity entity = new CustomerAccountEntity();
            CustomerAccountDto dto = new CustomerAccountDto();

            when(customerAccountRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(customerAccountMapper.toDto(any(CustomerAccountEntity.class)))
                    .thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/CustomerAccount/{customerAccountId}")
    class GetCustomerAccount {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when customer account exists and user is admin")
        void shouldReturn200WhenExists() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerAccountEntity entity = new CustomerAccountEntity();
            CustomerAccountDto dto = new CustomerAccountDto();

            when(customerAccountRepository.findById(id)).thenReturn(Optional.of(entity));
            when(customerAccountMapper.toDto(entity)).thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount/" + id)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when customer account does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerAccountRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/CustomerAccount/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /espi/1_1/resource/CustomerAccount")
    class CreateCustomerAccount {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 201 Created when data is valid and user is admin")
        void shouldReturn201ForAdmin() throws Exception {
            CustomerAccountDto dto = new CustomerAccountDto();
            CustomerAccountEntity entity = new CustomerAccountEntity();
            entity.setId(UUID.randomUUID());

            when(customerAccountMapper.toEntity(any(CustomerAccountDto.class))).thenReturn(entity);
            when(customerAccountService.save(any(CustomerAccountEntity.class))).thenReturn(entity);
            when(customerAccountMapper.toDto(any(CustomerAccountEntity.class))).thenReturn(dto);

            mockMvc.perform(post("/espi/1_1/resource/CustomerAccount")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").doesNotExist()); // ID is in location header, DTO shouldn't have it per plan (if it's a creation DTO)
                    // Wait, CustomerAccountDto has id in some cases, but per guidelines creation DTO shouldn't have id.
        }
    }

    @Nested
    @DisplayName("PUT /espi/1_1/resource/CustomerAccount/{customerAccountId}")
    class UpdateCustomerAccount {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when update is successful")
        void shouldReturn200OnSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerAccountDto dto = new CustomerAccountDto();
            CustomerAccountEntity entity = new CustomerAccountEntity();

            when(customerAccountRepository.existsById(id)).thenReturn(true);
            when(customerAccountMapper.toEntity(any(CustomerAccountDto.class))).thenReturn(entity);
            when(customerAccountService.save(any(CustomerAccountEntity.class))).thenReturn(entity);
            when(customerAccountMapper.toDto(any(CustomerAccountEntity.class))).thenReturn(dto);

            mockMvc.perform(put("/espi/1_1/resource/CustomerAccount/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /espi/1_1/resource/CustomerAccount/{customerAccountId}")
    class DeleteCustomerAccount {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 204 No Content when deletion is successful")
        void shouldReturn204OnSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerAccountRepository.existsById(id)).thenReturn(true);

            mockMvc.perform(delete("/espi/1_1/resource/CustomerAccount/" + id))
                    .andExpect(status().isNoContent());
        }
    }
}
