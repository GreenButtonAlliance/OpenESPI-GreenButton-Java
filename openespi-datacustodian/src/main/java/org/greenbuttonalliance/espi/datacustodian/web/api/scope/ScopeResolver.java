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
import org.greenbuttonalliance.espi.datacustodian.config.UsageFunctionBlockAuthorities;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
        return subscriptionId(authentication).filter(subscriptionId::equals).isPresent();
    }

    /**
     * The subscription a customer access token is for, from its introspected {@code resourceURI}
     * ({@code …/Batch/Subscription/{id}}); empty for any other token, including a Third Party's client access
     * token ({@code FB=34_35}) whatever its URIs.
     */
    public Optional<UUID> subscriptionId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || hasAuthority(authentication, EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN)
                || !(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
            return Optional.empty();
        }
        String resourceUri = stringAttribute(principal, "resourceURI");
        return resourceUri == null ? Optional.empty()
                : EspiBatchUri.subscriptionId(resourceUri).flatMap(ScopeResolver::parseUuid);
    }

    /**
     * Every ESPI scope of the token (each {@code FB=...} scope string, with its {@code HistoryLength} and other
     * terms), except the client access token's {@code FB=34_35}; empty for a token without one.
     *
     * @throws IllegalArgumentException if an FB scope does not parse: the caller must refuse the request rather
     *                                  than read it as "no limits" (fail closed)
     */
    public List<EspiScope> espiScopes(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
            return List.of();
        }
        Object scope = principal.getAttribute("scope");
        Collection<?> scopes = scope instanceof Collection<?> collection ? collection
                : scope instanceof String s ? List.of(s.trim().split("\\s+")) : List.of();
        List<EspiScope> parsed = new ArrayList<>();
        for (Object value : scopes) {
            String text = value == null ? "" : value.toString().trim();
            if (text.regionMatches(true, 0, "FB=", 0, 3)
                    && !EspiScopeOpaqueTokenIntrospector.CLIENT_ACCESS_TOKEN_SCOPE.equalsIgnoreCase(text)) {
                parsed.add(EspiScope.parse(text));
            }
        }
        return parsed;
    }

    /**
     * The {@code HistoryLength} of the token's usage-data scopes, in seconds (REQ.21.6.1.2.1: "The amount of
     * historical information available from the Data Custodian in seconds"): the shortest among its FB scopes
     * that grant any usage-data Function Block (interval metering, a commodity, a usage summary or power
     * quality), or empty if none states one. Only a scope with no usage-data FB, such as a customer-data scope
     * ({@code FB=51_53_54;HistoryLength=...}), is left out: it does not limit usage data.
     *
     * @throws IllegalArgumentException if an FB scope does not parse (see {@link #espiScopes})
     */
    public Optional<Long> historyLengthSeconds(Authentication authentication) {
        return espiScopes(authentication).stream()
                .filter(scope -> scope.functionBlocks().stream()
                        .anyMatch(fb -> UsageFunctionBlockAuthorities.USAGE_DATA.contains("FB_" + fb)))
                .map(EspiScope::historyLength)
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .min(Long::compare);
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

    private static boolean hasAdminAuthority(Authentication authentication) {
        return hasAuthority(authentication, ADMIN_AUTHORITY);
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
