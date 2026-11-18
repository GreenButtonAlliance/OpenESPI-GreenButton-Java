/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
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
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.greenbuttonalliance.espi.datacustodian.config.EspiScopeOpaqueTokenIntrospector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves the {@link ResourceScope} for a resource-server request from the introspected token
 * (#119). This is the single place the Data Custodian decides "what may this caller see", so every
 * resource endpoint enforces the same rule and the subscription-scoping / data-leakage fix lives
 * in one tested component rather than per controller.
 *
 * <p>Resolution order (fail-closed):</p>
 * <ol>
 *   <li>DataCustodian admin authority → {@link ResourceScope#unfiltered()} (unfiltered).</li>
 *   <li>Token {@code resourceURI} = {@code …/Batch/Bulk/{id}} → admin (a Bulk token is a
 *       back-end/admin grant over a defined set; treated as unfiltered for the sandbox).</li>
 *   <li>Token {@code resourceURI} = {@code …/Batch/Subscription/{id}} → the subscription's granted
 *       UsagePoint ids (energy scope), parsed via {@link EspiBatchUri} and loaded from the DC's own
 *       store ({@link SubscriptionRepository#findUsagePointIdsBySubscriptionId}).</li>
 *   <li>Token {@code customerResourceURI} = {@code …/Batch/RetailCustomer/{id}} → the granted
 *       RetailCustomer id (PII scope; {@code Long} correlation key).</li>
 *   <li>Otherwise → {@link ResourceScope#denied()} (sees nothing).</li>
 * </ol>
 *
 * <p>Ids come from the trusted introspection response (the AS echoes the URIs the DC itself
 * minted), but the <em>granted set</em> is read from the DC's own database — never from the token
 * body — so a tampered/expanded URI cannot widen access.</p>
 */
@Component
public class ScopeResolver {

    /** Authority (from {@code EspiScopeOpaqueTokenIntrospector}) that grants unfiltered access. */
    static final String ADMIN_AUTHORITY = "SCOPE_DataCustodian_Admin_Access";

    private final SubscriptionRepository subscriptionRepository;

    public ScopeResolver(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Resolves the data-visibility scope for the current request.
     *
     * @param authentication the resource-server authentication (may be {@code null})
     * @return the resolved scope; {@link ResourceScope#denied()} when nothing is grantable
     */
    public ResourceScope resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResourceScope.denied();
        }
        if (hasAdminAuthority(authentication)) {
            return ResourceScope.unfiltered();
        }
        if (!(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
            return ResourceScope.denied();
        }

        String resourceUri = stringAttribute(principal, "resourceURI");
        // A Bulk token is a back-end/admin grant; treat as unfiltered for the sandbox.
        if (resourceUri != null && EspiBatchUri.bulkId(resourceUri).isPresent()) {
            return ResourceScope.unfiltered();
        }

        Set<UUID> usagePointIds = new LinkedHashSet<>();
        if (resourceUri != null) {
            EspiBatchUri.subscriptionId(resourceUri)
                    .flatMap(ScopeResolver::parseUuid)
                    .ifPresent(subscriptionId ->
                            usagePointIds.addAll(subscriptionRepository
                                    .findUsagePointIdsBySubscriptionId(subscriptionId)));
        }

        Set<Long> retailCustomerIds = new LinkedHashSet<>();
        String customerResourceUri = stringAttribute(principal, "customerResourceURI");
        if (customerResourceUri != null) {
            EspiBatchUri.retailCustomerId(customerResourceUri)
                    .flatMap(ScopeResolver::parseLong)
                    .ifPresent(retailCustomerIds::add);
        }

        if (usagePointIds.isEmpty() && retailCustomerIds.isEmpty()) {
            return ResourceScope.denied();
        }
        return ResourceScope.of(usagePointIds, retailCustomerIds);
    }

    /**
     * Whether the caller may read {@code Batch/Subscription/{subscriptionId}} (#279): the DataCustodian
     * admin, or a customer access token whose introspected {@code resourceURI} names exactly this
     * subscription. A Third Party's client access token ({@code FB=34_35}) never may, whatever its URIs: it
     * is for Authorization and Bulk. Unknown and foreign subscriptions are answered alike, so the caller
     * learns nothing about subscriptions it does not hold.
     */
    public boolean canReadSubscription(Authentication authentication, UUID subscriptionId) {
        if (subscriptionId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasAdminAuthority(authentication)) {
            return true;
        }
        if (hasAuthority(authentication, EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN)
                || !(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
            return false;
        }
        String resourceUri = stringAttribute(principal, "resourceURI");
        return resourceUri != null && EspiBatchUri.subscriptionId(resourceUri)
                .flatMap(ScopeResolver::parseUuid)
                .filter(subscriptionId::equals)
                .isPresent();
    }

    /**
     * The token's ESPI scope (the {@code FB=...} scope string, with its {@code HistoryLength} and other
     * terms), or empty for the admin, a token without one, or a scope that does not parse.
     */
    public Optional<EspiScope> espiScope(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
            return Optional.empty();
        }
        Object scope = principal.getAttribute("scope");
        Collection<?> scopes = scope instanceof Collection<?> collection ? collection
                : scope instanceof String s ? List.of(s.trim().split("\\s+")) : List.of();
        for (Object value : scopes) {
            String text = value == null ? "" : value.toString().trim();
            if (text.regionMatches(true, 0, "FB=", 0, 3)
                    && !EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN_SCOPE.equals(text)) {
                try {
                    return Optional.of(EspiScope.parse(text));
                } catch (IllegalArgumentException ex) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /** Whether the caller holds the DataCustodian admin authority. */
    public boolean isAdmin(Authentication authentication) {
        return authentication != null && hasAdminAuthority(authentication);
    }

    private static boolean hasAuthority(Authentication authentication, String name) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (name.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static String stringAttribute(OAuth2AuthenticatedPrincipal principal, String name) {
        Object value = principal.getAttribute(name);
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static java.util.Optional<UUID> parseUuid(String s) {
        try {
            return java.util.Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<Long> parseLong(String s) {
        try {
            return java.util.Optional.of(Long.valueOf(s));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
