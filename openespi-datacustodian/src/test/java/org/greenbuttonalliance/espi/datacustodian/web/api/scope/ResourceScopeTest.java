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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResourceScope} — the resolved per-request visibility scope (#119).
 */
@DisplayName("ResourceScope (#119)")
class ResourceScopeTest {

    private static final UUID UP = UUID.fromString("00000000-0000-5000-8000-000000000001");

    @Test
    @DisplayName("admin permits everything")
    void adminPermitsAll() {
        ResourceScope admin = ResourceScope.unfiltered();
        assertThat(admin.admin()).isTrue();
        assertThat(admin.isDenied()).isFalse();
        assertThat(admin.permitsUsagePoint(UUID.randomUUID())).isTrue();
        assertThat(admin.permitsRetailCustomer(1L)).isTrue();
    }

    @Test
    @DisplayName("of(...) permits only listed ids; denies others")
    void scopedPermitsOnlyListed() {
        ResourceScope scope = ResourceScope.of(Set.of(UP), Set.of(42L));
        assertThat(scope.admin()).isFalse();
        assertThat(scope.permitsUsagePoint(UP)).isTrue();
        assertThat(scope.permitsUsagePoint(UUID.randomUUID())).isFalse();
        assertThat(scope.permitsRetailCustomer(42L)).isTrue();
        assertThat(scope.permitsRetailCustomer(7L)).isFalse();
        assertThat(scope.isDenied()).isFalse();
    }

    @Test
    @DisplayName("denied permits nothing and is fail-closed")
    void deniedPermitsNothing() {
        ResourceScope denied = ResourceScope.denied();
        assertThat(denied.admin()).isFalse();
        assertThat(denied.isDenied()).isTrue();
        assertThat(denied.permitsUsagePoint(UP)).isFalse();
        assertThat(denied.permitsRetailCustomer(42L)).isFalse();
    }

    @Test
    @DisplayName("null id arguments are never permitted (except under admin)")
    void nullIdsNotPermitted() {
        ResourceScope scope = ResourceScope.of(Set.of(UP), Set.of(42L));
        assertThat(scope.permitsUsagePoint(null)).isFalse();
        assertThat(scope.permitsRetailCustomer(null)).isFalse();
        assertThat(ResourceScope.unfiltered().permitsUsagePoint(null)).isTrue();
    }

    @Test
    @DisplayName("null constructor sets normalize to empty immutable sets")
    void nullSetsNormalized() {
        ResourceScope scope = new ResourceScope(false, null, null);
        assertThat(scope.usagePointIds()).isEmpty();
        assertThat(scope.retailCustomerIds()).isEmpty();
        assertThat(scope.isDenied()).isTrue();
    }
}
