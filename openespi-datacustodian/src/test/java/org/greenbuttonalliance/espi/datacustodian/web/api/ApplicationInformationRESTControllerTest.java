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

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.dto.usage.ApplicationInformationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ApplicationInformationRESTController Mock MVC Tests")
public class ApplicationInformationRESTControllerTest extends AbstractControllerMockTest {

    @Nested
    @DisplayName("GET /espi/1_1/resource/ApplicationInformation")
    class GetAllApplicationInformation {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK with list of applications for admin")
        void shouldReturn200ForAdmin() throws Exception {
            ApplicationInformationEntity entity = new ApplicationInformationEntity();
            when(applicationInformationService.findAll()).thenReturn(List.of(entity));

            org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));

            verify(applicationInformationService).export(anyList(), any());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_ThirdParty_Admin_Access")
        @DisplayName("Should return 200 OK for third party admin")
        void shouldReturn200ForThirdPartyAdmin() throws Exception {
            when(applicationInformationService.findAll()).thenReturn(List.of());

            org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation")
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_Wrong_Authority")
        @DisplayName("Should return 403 Forbidden for user without proper authority")
        void shouldReturn403ForWrongAuthority() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /espi/1_1/resource/ApplicationInformation/{applicationInformationId}")
    class GetApplicationInformation {

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK when application exists and user is admin")
        void shouldReturn200WhenExists() throws Exception {
            UUID id = UUID.randomUUID();
            ApplicationInformationEntity entity = new ApplicationInformationEntity();
            when(applicationInformationService.findById(id)).thenReturn(entity);

            org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + id)
                            .accept(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));

            verify(applicationInformationService).export(any(ApplicationInformationEntity.class), any());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when application does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            when(applicationInformationService.findById(id)).thenReturn(null);

            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /espi/1_1/resource/ApplicationInformation")
    class CreateApplicationInformation {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 201 Created and the created application")
        void shouldReturn201Created() throws Exception {
            ApplicationInformationDto dto = new ApplicationInformationDto();
            dto.setClientId("test-client");
            ApplicationInformationEntity entity = new ApplicationInformationEntity();
            entity.setId(UUID.randomUUID());

            when(applicationInformationService.fromDto(any(ApplicationInformationDto.class))).thenReturn(entity);
            when(applicationInformationService.save(any(ApplicationInformationEntity.class))).thenReturn(entity);

            mockMvc.perform(post("/espi/1_1/resource/ApplicationInformation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }
    }

    @Nested
    @DisplayName("PUT /espi/1_1/resource/ApplicationInformation/{applicationInformationId}")
    class UpdateApplicationInformation {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 200 OK and the updated application")
        void shouldReturn200Ok() throws Exception {
            UUID id = UUID.randomUUID();
            ApplicationInformationDto dto = new ApplicationInformationDto();
            ApplicationInformationEntity entity = new ApplicationInformationEntity();
            entity.setId(id);

            when(applicationInformationService.findById(id)).thenReturn(entity);
            when(applicationInformationService.fromDto(any(ApplicationInformationDto.class))).thenReturn(entity);
            when(applicationInformationService.save(any(ApplicationInformationEntity.class))).thenReturn(entity);

            mockMvc.perform(put("/espi/1_1/resource/ApplicationInformation/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when application does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();
            ApplicationInformationDto dto = new ApplicationInformationDto();

            when(applicationInformationService.findById(id)).thenReturn(null);

            mockMvc.perform(put("/espi/1_1/resource/ApplicationInformation/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /espi/1_1/resource/ApplicationInformation/{applicationInformationId}")
    class DeleteApplicationInformation {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 204 No Content when application is deleted")
        void shouldReturn204NoContent() throws Exception {
            UUID id = UUID.randomUUID();
            ApplicationInformationEntity entity = new ApplicationInformationEntity();

            when(applicationInformationService.findById(id)).thenReturn(entity);

            mockMvc.perform(delete("/espi/1_1/resource/ApplicationInformation/" + id))
                    .andExpect(status().isNoContent());

            verify(applicationInformationService).deleteById(id);
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("Should return 404 Not Found when application does not exist")
        void shouldReturn404WhenNotExists() throws Exception {
            UUID id = UUID.randomUUID();

            when(applicationInformationService.findById(id)).thenReturn(null);

            mockMvc.perform(delete("/espi/1_1/resource/ApplicationInformation/" + id))
                    .andExpect(status().isNotFound());
        }
    }
}
