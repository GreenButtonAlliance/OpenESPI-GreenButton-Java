/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
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

package org.greenbuttonalliance.espi.datacustodian.web.api.scope;

import org.greenbuttonalliance.espi.common.repositories.usage.SubscriptionRepository;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScopeResolver} — the resource-server data-visibility scope core (#119).
 */
@DisplayName("ScopeResolver — token → ResourceScope (#119)")
class ScopeResolverTest {

    private static final String BASE = "https://utilityapi.com/DataCustodian/espi/1_1/resource";

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final ScopeResolver resolver = new ScopeResolver(subscriptionRepository);

    private static Authentication auth(Map<String, Object> attributes, String... authorities) {
        DefaultOAuth2AuthenticatedPrincipal principal =
                new DefaultOAuth2AuthenticatedPrincipal("tester", attributes, null);
        return new TestingAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("null / unauthenticated → denied")
    void nullAuthDenied() {
        assertThat(resolver.resolve(null).isDenied()).isTrue();
        TestingAuthenticationToken unauth = new TestingAuthenticationToken("x", null);
        unauth.setAuthenticated(false);
        assertThat(resolver.resolve(unauth).isDenied()).isTrue();
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("DataCustodian admin authority → admin (unfiltered)")
    void adminAuthority() {
        ResourceScope scope = resolver.resolve(
                auth(Map.of("active", true), "SCOPE_DataCustodian_Admin_Access"));

        assertThat(scope.admin()).isTrue();
        assertThat(scope.permitsUsagePoint(UUID.randomUUID())).isTrue();
        assertThat(scope.permitsRetailCustomer(999L)).isTrue();
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("Bulk resourceURI → admin (back-end/admin grant)")
    void bulkResourceUriIsAdmin() {
        ResourceScope scope = resolver.resolve(
                auth(Map.of("resourceURI", EspiBatchUri.batchBulk(BASE, "BULK_1")), "FB_4"));

        assertThat(scope.admin()).isTrue();
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("Subscription resourceURI → that subscription's granted UsagePoint ids (energy)")
    void subscriptionResourceUriResolvesUsagePoints() {
        UUID subscriptionId = UUID.fromString("11111111-2222-5333-8444-555555555555");
        UUID up1 = UUID.fromString("00000000-0000-5000-8000-000000000001");
        UUID up2 = UUID.fromString("00000000-0000-5000-8000-000000000002");
        when(subscriptionRepository.findUsagePointIdsBySubscriptionId(subscriptionId))
                .thenReturn(List.of(up1, up2));

        ResourceScope scope = resolver.resolve(auth(
                Map.of("resourceURI", EspiBatchUri.batchSubscription(BASE, subscriptionId)), "FB_4"));

        assertThat(scope.admin()).isFalse();
        assertThat(scope.usagePointIds()).containsExactlyInAnyOrder(up1, up2);
        assertThat(scope.permitsUsagePoint(up1)).isTrue();
        assertThat(scope.permitsUsagePoint(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("customerResourceURI → that RetailCustomer id (PII, Long key)")
    void customerResourceUriResolvesRetailCustomer() {
        ResourceScope scope = resolver.resolve(auth(
                Map.of("customerResourceURI", EspiBatchUri.batchRetailCustomer(BASE, "42")), "FB_53"));

        assertThat(scope.admin()).isFalse();
        assertThat(scope.retailCustomerIds()).containsExactly(42L);
        assertThat(scope.permitsRetailCustomer(42L)).isTrue();
        assertThat(scope.permitsRetailCustomer(7L)).isFalse();
    }

    @Test
    @DisplayName("non-admin token with no resolvable grant → denied (fail-closed)")
    void noGrantDenied() {
        ResourceScope scope = resolver.resolve(auth(Map.of("active", true), "FB_4"));
        assertThat(scope.isDenied()).isTrue();
        assertThat(scope.permitsUsagePoint(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("a subscription with no granted usage points → denied (not unfiltered)")
    void emptySubscriptionDenied() {
        UUID subscriptionId = UUID.fromString("22222222-3333-5444-8555-666666666666");
        when(subscriptionRepository.findUsagePointIdsBySubscriptionId(subscriptionId))
                .thenReturn(List.of());

        ResourceScope scope = resolver.resolve(auth(
                Map.of("resourceURI", EspiBatchUri.batchSubscription(BASE, subscriptionId)), "FB_4"));

        assertThat(scope.isDenied()).isTrue();
    }
}
