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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Optional;

/**
 * Row-level scope for the ESPI admin resources (ApplicationInformation, Authorization).
 *
 * <p>{@code SCOPE_DataCustodian_Admin_Access} is the Data Custodian's own administrator and sees every
 * record. {@code SCOPE_ThirdParty_Admin_Access} is a Third Party's client_credentials token: it sees only
 * the records of its own application, matched on the token's {@code client_id}. Without that check any
 * Third Party admin could read every other Third Party's Authorizations and ApplicationInformation,
 * including their {@code client_secret} and {@code registration_access_token}.</p>
 *
 * <p>Fails closed: a caller that is not a DataCustodian admin and has no {@code client_id} sees nothing.</p>
 */
final class AdminCaller {

    static final String DATA_CUSTODIAN_ADMIN = "SCOPE_DataCustodian_Admin_Access";

    private AdminCaller() {
    }

    static boolean isDataCustodianAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(DATA_CUSTODIAN_ADMIN::equals);
    }

    /** The OAuth client the token was issued to, from the introspection response's {@code client_id}. */
    static Optional<String> clientId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal) {
            Object clientId = principal.getAttribute("client_id");
            if (clientId != null && !clientId.toString().isBlank()) {
                return Optional.of(clientId.toString());
            }
        }
        return Optional.empty();
    }

    /** Whether the caller may see a record owned by the application with {@code ownerClientId}. */
    static boolean canSee(Authentication authentication, String ownerClientId) {
        if (isDataCustodianAdmin(authentication)) {
            return true;
        }
        return ownerClientId != null && clientId(authentication).map(ownerClientId::equals).orElse(false);
    }
}
