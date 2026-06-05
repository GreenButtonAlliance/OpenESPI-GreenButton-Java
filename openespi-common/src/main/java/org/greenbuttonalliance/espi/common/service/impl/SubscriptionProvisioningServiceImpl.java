/*
 * Copyright 2025 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.greenbuttonalliance.espi.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.usage.ApplicationInformationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity;
import org.greenbuttonalliance.espi.common.domain.usage.RetailCustomerEntity;
import org.greenbuttonalliance.espi.common.domain.usage.SubscriptionEntity;
import org.greenbuttonalliance.espi.common.domain.usage.UsagePointEntity;
import org.greenbuttonalliance.espi.common.repositories.usage.ApplicationInformationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.AuthorizationRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.RetailCustomerRepository;
import org.greenbuttonalliance.espi.common.repositories.usage.UsagePointRepository;
import org.greenbuttonalliance.espi.common.scope.EspiScope;
import org.greenbuttonalliance.espi.common.uri.EspiBatchUri;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Default implementation of {@link SubscriptionProvisioningService}.
 *
 * <p>Builds the {@link AuthorizationEntity} aggregate and persists it via the aggregate root:
 * {@code cascade = ALL} on {@code AuthorizationEntity.subscriptions} (PR B1) carries the
 * Subscriptions through with one {@code save}. URIs returned to the AS are absolute and use
 * {@code espi.resources.base-uri}.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionProvisioningServiceImpl implements SubscriptionProvisioningService {

	private final AuthorizationRepository authorizationRepository;
	private final ApplicationInformationRepository applicationInformationRepository;
	private final RetailCustomerRepository retailCustomerRepository;
	private final UsagePointRepository usagePointRepository;
	private final EspiIdGeneratorService idGeneratorService;

	/**
	 * Canonical absolute base URI for ESPI resources, used to build {@code resource_uri} and
	 * {@code authorization_uri} returned to the AS. Defaulted for unit tests; overridden in
	 * production by {@code application.yml}.
	 */
	@Value("${espi.resources.base-uri:http://localhost:8081/DataCustodian/espi/1_1/resource}")
	private String resourceBaseUri;

	@Override
	public SubscriptionProvisionResult provisionFromGrant(SubscriptionProvisionCommand command) {
		validate(command);

		EspiScope scope = EspiScope.parse(command.grantedScope());

		ApplicationInformationEntity application = applicationInformationRepository
				.findByClientId(command.clientId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Unknown client_id: " + command.clientId()));

		RetailCustomerEntity customer = retailCustomerRepository
				.findById(command.retailCustomerId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Unknown retail_customer_id: " + command.retailCustomerId()));

		List<UsagePointEntity> usagePoints = resolveUsagePoints(command.selectedUsagePointIds(), customer);
		boolean includesEnergy = !usagePoints.isEmpty();
		boolean includesPii = scope.includesCustomerPii();

		if (!includesEnergy && !includesPii) {
			throw new IllegalArgumentException(
					"Grant must include at least one selected usage point OR a Customer/PII FB scope");
		}

		AuthorizationEntity authorization = newAuthorization(command, application, customer, includesPii);

		SubscriptionEntity resourceSubscription = includesEnergy
				? newSubscription(command, application, customer, usagePoints, authorization)
				: null;
		SubscriptionEntity customerSubscription = includesPii
				? newSubscription(command, application, customer, List.of(), authorization)
				: null;

		String resourceUri = resourceSubscription != null ? subscriptionUri(resourceSubscription.getId()) : null;
		if (resourceUri != null) {
			authorization.setResourceURI(resourceUri);
		}
		String authorizationUri = authorizationUri(authorization.getId());
		authorization.setAuthorizationURI(authorizationUri);

		AuthorizationEntity persisted = authorizationRepository.save(authorization);

		log.info("Provisioned authorization {} for client {} customer {} (correlation_id={}, pii={}, usagePoints={})",
				persisted.getId(), command.clientId(), command.retailCustomerId(),
				command.correlationId(), includesPii, usagePoints.size());

		return new SubscriptionProvisionResult(
				persisted.getId(),
				resourceSubscription != null ? resourceSubscription.getId() : null,
				customerSubscription != null ? customerSubscription.getId() : null,
				resourceUri,
				authorizationUri,
				persisted.getCustomerResourceURI()
		);
	}

	private void validate(SubscriptionProvisionCommand command) {
		Objects.requireNonNull(command, "command");
		if (command.clientId() == null || command.clientId().isBlank()) {
			throw new IllegalArgumentException("client_id is required");
		}
		if (command.grantedScope() == null || command.grantedScope().isBlank()) {
			throw new IllegalArgumentException("granted_scope is required");
		}
		if (command.retailCustomerId() == null) {
			throw new IllegalArgumentException("retail_customer_id is required");
		}
	}

	private List<UsagePointEntity> resolveUsagePoints(List<UUID> ids, RetailCustomerEntity customer) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		List<UsagePointEntity> resolved = new ArrayList<>(ids.size());
		for (UUID id : ids) {
			UsagePointEntity up = usagePointRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("Unknown usage_point_id: " + id));
			if (up.getRetailCustomer() == null || !customer.getId().equals(up.getRetailCustomer().getId())) {
				throw new IllegalArgumentException(
						"usage_point_id " + id + " does not belong to retail_customer_id " + customer.getId());
			}
			resolved.add(up);
		}
		return resolved;
	}

	private AuthorizationEntity newAuthorization(SubscriptionProvisionCommand command,
												 ApplicationInformationEntity application,
												 RetailCustomerEntity customer,
												 boolean includesPii) {
		AuthorizationEntity authorization = new AuthorizationEntity(customer, application, command.grantedScope());
		authorization.setId(UUID.randomUUID());
		authorization.setThirdParty(command.clientId());
		authorization.setStatus(AuthorizationEntity.STATUS_ACTIVE);
		if (includesPii) {
			// Build the canonical ESPI Batch/RetailCustomer URI from the retail-customer id DC already
			// holds (#160). DC owns this value end-to-end; it is never round-tripped through the AS.
			authorization.setCustomerResourceURI(
					EspiBatchUri.batchRetailCustomer(resourceBaseUri, command.retailCustomerId()));
		}
		return authorization;
	}

	private SubscriptionEntity newSubscription(SubscriptionProvisionCommand command,
											   ApplicationInformationEntity application,
											   RetailCustomerEntity customer,
											   List<UsagePointEntity> usagePoints,
											   AuthorizationEntity authorization) {
		UUID id = idGeneratorService.generateSubscriptionId(command.clientId(), String.valueOf(customer.getId()));
		SubscriptionEntity subscription = new SubscriptionEntity(id);
		subscription.setRetailCustomer(customer);
		subscription.setApplicationInformation(application);
		subscription.setAuthorization(authorization);
		subscription.setUsagePoints(new ArrayList<>(usagePoints));
		authorization.getSubscriptions().add(subscription);
		return subscription;
	}

	// Canonical ESPI 4.0 Batch resource URIs via the single builder/parser shared with the consumers
	// (DC ResourceValidationFilter) — see EspiBatchUri / #160. The previous "/Subscription/{id}" form
	// omitted the required "/Batch/" segment and so failed DC's own resource validation.
	private String subscriptionUri(UUID subscriptionId) {
		return EspiBatchUri.batchSubscription(resourceBaseUri, subscriptionId);
	}

	private String authorizationUri(UUID authorizationId) {
		return EspiBatchUri.authorization(resourceBaseUri, authorizationId);
	}
}
