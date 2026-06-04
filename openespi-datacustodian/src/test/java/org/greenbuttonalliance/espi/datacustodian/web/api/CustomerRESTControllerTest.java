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

@DisplayName("CustomerRESTController Mock MVC Tests")
public class CustomerRESTControllerTest extends AbstractControllerMockTest {

    @Nested
    @DisplayName("GET /espi/1_1/resource/Customer")
    class GetAllCustomers {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK with list of customers for admin")
        void shouldReturn200ForAdmin() throws Exception {
            CustomerEntity entity = new CustomerEntity();
            CustomerDto dto = new CustomerDto();

            when(customerRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(customerMapper.toDto(any(CustomerEntity.class)))
                    .thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/Customer")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer"))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * ESPI Customer-PII least-privilege enforcement (#157): a customer token may read /Customer only
     * with the base Connect-My-Data FB (FB_53) AND the Customer-specific FB (FB_54). FB_53 alone, the
     * specific FB without the base, or a different resource's FB (e.g. FB_56 CustomerAccount) are all
     * denied — proving the Customer-PII catalog is honored rather than collapsed to a single gate.
     */
    @Nested
    @DisplayName("ESPI Customer-PII FB authorization (#157)")
    class CustomerPiiScopeGating {

        @Test
        @WithMockUser(authorities = {"FB_53", "FB_54"})
        @DisplayName("FB_53 base + FB_54 (Customer) -> 200 OK")
        void baseAndCustomerFbReturns200() throws Exception {
            when(customerRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(new CustomerEntity())));
            when(customerMapper.toDto(any(CustomerEntity.class))).thenReturn(new CustomerDto());

            mockMvc.perform(get("/espi/1_1/resource/Customer").accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "FB_53")
        @DisplayName("FB_53 base alone (no FB_54) -> 403 (least privilege)")
        void baseWithoutCustomerFbForbidden() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "FB_54")
        @DisplayName("FB_54 without base FB_53 -> 403")
        void customerFbWithoutBaseForbidden() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {"FB_53", "FB_56"})
        @DisplayName("FB_53 + FB_56 (CustomerAccount FB) -> 403 on Customer (wrong resource FB)")
        void wrongResourceFbForbidden() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/Customer/{customerId}")
    class GetCustomer {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Customer/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when customer exists and user is admin")
        void shouldReturn200WhenExists() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerEntity entity = new CustomerEntity();
            CustomerDto dto = new CustomerDto();

            when(customerRepository.findById(id)).thenReturn(Optional.of(entity));
            when(customerMapper.toDto(entity)).thenReturn(dto);

            mockMvc.perform(get("/espi/1_1/resource/Customer/" + id)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when customer does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/espi/1_1/resource/Customer/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /espi/1_1/resource/Customer")
    class CreateCustomer {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 201 Created when data is valid and user is admin")
        void shouldReturn201ForAdmin() throws Exception {
            CustomerDto dto = new CustomerDto();
            CustomerEntity entity = new CustomerEntity();
            entity.setId(UUID.randomUUID());

            when(customerMapper.toEntity(any(CustomerDto.class))).thenReturn(entity);
            when(customerService.save(any(CustomerEntity.class))).thenReturn(entity);
            when(customerMapper.toDto(any(CustomerEntity.class))).thenReturn(dto);

            mockMvc.perform(post("/espi/1_1/resource/Customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }
    }

    @Nested
    @DisplayName("PUT /espi/1_1/resource/Customer/{customerId}")
    class UpdateCustomer {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when update is successful")
        void shouldReturn200OnSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerDto dto = new CustomerDto();
            CustomerEntity entity = new CustomerEntity();

            when(customerService.existsById(id)).thenReturn(true);
            when(customerMapper.toEntity(any(CustomerDto.class))).thenReturn(entity);
            when(customerService.save(any(CustomerEntity.class))).thenReturn(entity);
            when(customerMapper.toDto(any(CustomerEntity.class))).thenReturn(dto);

            mockMvc.perform(put("/espi/1_1/resource/Customer/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /espi/1_1/resource/Customer/{customerId}")
    class DeleteCustomer {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 204 No Content when deletion is successful")
        void shouldReturn204OnSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerService.existsById(id)).thenReturn(true);

            mockMvc.perform(delete("/espi/1_1/resource/Customer/" + id))
                    .andExpect(status().isNoContent());
        }
    }
}
