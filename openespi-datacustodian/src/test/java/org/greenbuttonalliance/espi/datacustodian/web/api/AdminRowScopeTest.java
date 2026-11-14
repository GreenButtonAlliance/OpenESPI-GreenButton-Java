/*
 * Copyright (c) 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.datacustodian.web.api;

import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.dto.atom.AtomFeedDto;
import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Row-level scope on the ESPI admin resources (AdminCaller): a DataCustodian admin sees every record;
 * a ThirdParty admin sees only its own application's records (token client_id); a ThirdParty admin
 * token without client_id sees nothing; another application's record is a 404.
 */
@DisplayName("Admin resources: row-level scope by client_id")
class AdminRowScopeTest extends AbstractControllerMockTest {

    @MockitoBean
    private AuthorizationRepository authorizationRepository;

    private static final String TP_A = "third_party_a";
    private static final String TP_B = "third_party_b";

    private static RequestPostProcessor dcAdmin() {
        return opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_DataCustodian_Admin_Access"))
                .attributes(a -> a.put("client_id", "data_custodian_admin"));
    }

    private static RequestPostProcessor tpAdmin(String clientId) {
        return opaqueToken().authorities(new SimpleGrantedAuthority("SCOPE_ThirdParty_Admin_Access"))
                .attributes(a -> {
                    if (clientId != null) {
                        a.put("client_id", clientId);
                    }
                });
    }

    private static ApplicationInformationEntity application(String clientId) {
        ApplicationInformationEntity app = new ApplicationInformationEntity();
        app.setId(UUID.nameUUIDFromBytes(clientId.getBytes()));
        app.setClientId(clientId);
        return app;
    }

    @Nested
    @DisplayName("ApplicationInformation (carries client_secret and registration_access_token)")
    class ApplicationInformationScope {

        private final ApplicationInformationEntity appA = application(TP_A);
        private final ApplicationInformationEntity appB = application(TP_B);

        @SuppressWarnings("unchecked")
        private List<ApplicationInformationEntity> exported() {
            ArgumentCaptor<List<ApplicationInformationEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(applicationInformationService).export(anyString(), captor.capture(), any());
            return captor.getValue();
        }

        @Test
        @DisplayName("feed: a ThirdParty admin gets only its own application")
        void thirdPartyAdminFeedIsScoped() throws Exception {
            when(applicationInformationService.findAll()).thenReturn(List.of(appA, appB));

            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation").with(tpAdmin(TP_A)))
                    .andExpect(status().isOk());

            assertThat(exported()).containsExactly(appA);
        }

        @Test
        @DisplayName("feed: a DataCustodian admin gets every application")
        void dataCustodianAdminFeedIsUnscoped() throws Exception {
            when(applicationInformationService.findAll()).thenReturn(List.of(appA, appB));

            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation").with(dcAdmin()))
                    .andExpect(status().isOk());

            assertThat(exported()).containsExactly(appA, appB);
        }

        @Test
        @DisplayName("feed: a ThirdParty admin token without client_id gets nothing")
        void thirdPartyAdminWithoutClientIdGetsNothing() throws Exception {
            when(applicationInformationService.findAll()).thenReturn(List.of(appA, appB));

            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation").with(tpAdmin(null)))
                    .andExpect(status().isOk());

            assertThat(exported()).isEmpty();
        }

        @Test
        @DisplayName("entry: another application's record is 404; its own is 200")
        void thirdPartyAdminEntryIsScoped() throws Exception {
            when(applicationInformationService.findById(appB.getId())).thenReturn(appB);
            when(applicationInformationService.findById(appA.getId())).thenReturn(appA);

            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + appB.getId()).with(tpAdmin(TP_A)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/espi/1_1/resource/ApplicationInformation/" + appA.getId()).with(tpAdmin(TP_A)))
                    .andExpect(status().isOk());

            verify(applicationInformationService, never()).export(eq(appB), any());
        }
    }

    @Nested
    @DisplayName("Authorization")
    class AuthorizationScope {

        private AuthorizationEntity authorizationOf(String clientId) {
            AuthorizationEntity authorization = new AuthorizationEntity();
            authorization.setId(UUID.randomUUID());
            authorization.setApplicationInformation(application(clientId));
            return authorization;
        }

        @Test
        @DisplayName("feed: a ThirdParty admin is queried by its own client_id, never findAll")
        void thirdPartyAdminFeedQueriesByClientId() throws Exception {
            when(authorizationRepository.findAllByApplicationClientId(eq(TP_A), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(authorizationOf(TP_A))));

            mockMvc.perform(get("/espi/1_1/resource/Authorization").with(tpAdmin(TP_A)))
                    .andExpect(status().isOk());

            verify(authorizationRepository).findAllByApplicationClientId(eq(TP_A), any(Pageable.class));
            verify(authorizationRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("feed: a DataCustodian admin sees every Authorization")
        void dataCustodianAdminFeedUsesFindAll() throws Exception {
            when(authorizationRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(authorizationOf(TP_A), authorizationOf(TP_B))));

            mockMvc.perform(get("/espi/1_1/resource/Authorization").with(dcAdmin()))
                    .andExpect(status().isOk());

            verify(authorizationRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("feed: a ThirdParty admin token without client_id queries nothing")
        void thirdPartyAdminWithoutClientIdQueriesNothing() throws Exception {
            mockMvc.perform(get("/espi/1_1/resource/Authorization").with(tpAdmin(null)))
                    .andExpect(status().isOk());

            verify(authorizationRepository, never()).findAll(any(Pageable.class));
            verify(authorizationRepository, never()).findAllByApplicationClientId(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("entry: another application's Authorization is 404; its own is 200")
        void thirdPartyAdminEntryIsScoped() throws Exception {
            AuthorizationEntity foreign = authorizationOf(TP_B);
            AuthorizationEntity own = authorizationOf(TP_A);
            when(authorizationRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));
            when(authorizationRepository.findById(own.getId())).thenReturn(Optional.of(own));
            AtomFeedDto oneEntry = new AtomFeedDto();
            oneEntry.setEntries(List.of(new UsageAtomEntryDto()));
            when(usageExportService.buildFeed(anyString(), anyString(), any(), any())).thenReturn(oneEntry);

            mockMvc.perform(get("/espi/1_1/resource/Authorization/" + foreign.getId()).with(tpAdmin(TP_A)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/espi/1_1/resource/Authorization/" + own.getId()).with(tpAdmin(TP_A)))
                    .andExpect(status().isOk());
        }
    }
}
