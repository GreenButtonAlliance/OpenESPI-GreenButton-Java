package org.greenbuttonalliance.espi.datacustodian.web.api.support;

import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.service.AuthorizationService;
import org.greenbuttonalliance.espi.common.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class ApiAccessValidator {

    private final AuthorizationService authorizationService;
    private final SubscriptionService subscriptionService;

    public ApiAccessValidator(AuthorizationService authorizationService, SubscriptionService subscriptionService) {
        this.authorizationService = authorizationService;
        this.subscriptionService = subscriptionService;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null
            && authentication.getAuthorities().stream()
            .anyMatch(a -> "SCOPE_DataCustodian_Admin_Access".equals(a.getAuthority()));
    }

    public UUID requireSubscriptionId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bearer token is required");
        }

        String token = authHeader.substring(7);
        AuthorizationEntity authorization = authorizationService.findByAccessToken(token);
        if (authorization == null || authorization.getSubscription() == null || authorization.getSubscription().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active subscription found for token");
        }

        return authorization.getSubscription().getId();
    }

    public void enforceSubscriptionPathAccess(Authentication authentication, String authHeader, UUID subscriptionId) {
        if (isAdmin(authentication)) {
            return;
        }

        UUID tokenSubscriptionId = requireSubscriptionId(authHeader);
        if (!tokenSubscriptionId.equals(subscriptionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is not authorized for requested subscription");
        }
    }

    public void enforceUsagePointInSubscription(UUID subscriptionId, UUID usagePointId) {
        Long retailCustomerId = subscriptionService.findRetailCustomerId(subscriptionId, usagePointId);
        if (retailCustomerId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UsagePoint not found in subscription");
        }
    }

    public void enforceRetailCustomerAccess(Authentication authentication, String authHeader, Long retailCustomerId) {
        if (isAdmin(authentication)) {
            return;
        }

        UUID tokenSubscriptionId = requireSubscriptionId(authHeader);
        SubscriptionEntity subscription = subscriptionService.findById(tokenSubscriptionId);
        if (subscription == null || subscription.getRetailCustomer() == null || subscription.getRetailCustomer().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No retail customer bound to subscription");
        }

        if (!subscription.getRetailCustomer().getId().equals(retailCustomerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RetailCustomer not found for token subscription");
        }
    }
}
