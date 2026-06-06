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

import java.util.Set;
import java.util.UUID;

/**
 * The data-visibility scope resolved for a single resource-server request (#119).
 *
 * <p>Foundation for ESPI subscription scoping: a request is either {@code admin} (DataCustodian
 * admin / Bulk — sees everything) or constrained to a concrete set of granted ids derived from the
 * token's {@code resourceURI} / {@code customerResourceURI} (per #160):</p>
 * <ul>
 *   <li><b>energy</b> resources (UsagePoint → MeterReading → IntervalBlock, EPQS, UsageSummary) are
 *       visible iff their owning UsagePoint id is in {@link #usagePointIds()};</li>
 *   <li><b>PII</b> resources (Customer, CustomerAccount, …) are visible iff their owning
 *       RetailCustomer id is in {@link #retailCustomerIds()} (RetailCustomer is a {@code Long}
 *       local correlation key, not an ESPI resource);</li>
 *   <li><b>ReadingType</b> (shared metadata) is visible by reachability — i.e. referenced by a
 *       MeterReading whose UsagePoint is in {@link #usagePointIds()} — resolved by callers.</li>
 * </ul>
 *
 * <p>A non-admin request with no resolvable grant yields {@link #denied()} (empty sets): it sees
 * nothing, never everything — the fail-closed default.</p>
 *
 * @param admin            true for DataCustodian-admin / Bulk tokens (unfiltered access)
 * @param usagePointIds    the granted energy UsagePoint ids (ignored when {@code admin})
 * @param retailCustomerIds the granted PII RetailCustomer ids (ignored when {@code admin})
 */
public record ResourceScope(boolean admin, Set<UUID> usagePointIds, Set<Long> retailCustomerIds) {

    public ResourceScope {
        usagePointIds = usagePointIds == null ? Set.of() : Set.copyOf(usagePointIds);
        retailCustomerIds = retailCustomerIds == null ? Set.of() : Set.copyOf(retailCustomerIds);
    }

    /** Unfiltered access (DataCustodian admin / Bulk). */
    public static ResourceScope unfiltered() {
        return new ResourceScope(true, Set.of(), Set.of());
    }

    /** Fail-closed: a non-admin caller with no resolvable grant — sees nothing. */
    public static ResourceScope denied() {
        return new ResourceScope(false, Set.of(), Set.of());
    }

    /** A non-admin scope granting the given energy + PII id sets. */
    public static ResourceScope of(Set<UUID> usagePointIds, Set<Long> retailCustomerIds) {
        return new ResourceScope(false, usagePointIds, retailCustomerIds);
    }

    /** Whether an energy resource owned by {@code usagePointId} is visible to this scope. */
    public boolean permitsUsagePoint(UUID usagePointId) {
        return admin || (usagePointId != null && usagePointIds.contains(usagePointId));
    }

    /** Whether a PII resource owned by {@code retailCustomerId} is visible to this scope. */
    public boolean permitsRetailCustomer(Long retailCustomerId) {
        return admin || (retailCustomerId != null && retailCustomerIds.contains(retailCustomerId));
    }

    /** True when this scope can see no resource at all (non-admin with empty grants). */
    public boolean isDenied() {
        return !admin && usagePointIds.isEmpty() && retailCustomerIds.isEmpty();
    }
}
