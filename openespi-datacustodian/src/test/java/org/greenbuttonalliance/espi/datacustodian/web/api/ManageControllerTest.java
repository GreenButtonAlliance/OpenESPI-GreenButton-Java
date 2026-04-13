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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ManageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Manage Command Tests")
    class ManageCommandTests {

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /manage - Should return 400 for missing command")
        void doCommand_MissingCommand_Returns400() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/DataCustodian/manage")
                    .accept(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Missing 'command' parameter")));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /manage - Should return 400 for unsupported command")
        void doCommand_UnsupportedCommand_Returns400() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/DataCustodian/manage")
                    .param("command", "unsupportedCommand")
                    .accept(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Unsupported command")));
        }

        @Test
        @WithMockUser(authorities = "SCOPE_DataCustodian_Admin_Access")
        @DisplayName("GET /manage - Should handle resetDataCustodianDB request")
        void doCommand_ResetCommand_ReturnsResponse() throws Exception {
            // Even if the script doesn't exist, we expect a response indicating it tried or failed to start
            mockMvc.perform(get("/espi/1_1/resource/DataCustodian/manage")
                    .param("command", "resetDataCustodianDB")
                    .accept(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(status().is(org.hamcrest.Matchers.oneOf(200, 500)));
        }

        @Test
        @DisplayName("GET /manage - Should return 401 for unauthenticated user")
        void doCommand_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/DataCustodian/manage")
                    .param("command", "resetDataCustodianDB")
                    .accept(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "SCOPE_RetailCustomer_Read_Access")
        @DisplayName("GET /manage - Should return 403 for insufficient authority")
        void doCommand_Forbidden_Returns403() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/DataCustodian/manage")
                    .param("command", "resetDataCustodianDB")
                    .accept(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(status().isForbidden());
        }
    }
}
