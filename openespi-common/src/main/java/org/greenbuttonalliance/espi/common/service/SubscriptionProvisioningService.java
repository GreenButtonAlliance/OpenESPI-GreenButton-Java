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

package org.greenbuttonalliance.espi.common.service;

import java.util.List;
import java.util.UUID;

/**
 * Provisions an {@link org.greenbuttonalliance.espi.common.domain.usage.AuthorizationEntity}
 * aggregate (Authorization + 1&ndash;2 Subscriptions) from an OAuth2 grant.
 *
 * <p>This is the data-custodian side of the AS&harr;DC back-channel: the AS calls into DC at
 * token-mint time with the granted scope, the customer identity, and the set of usage points the
 * customer chose to share. DC parses the scope (see {@link org.greenbuttonalliance.espi.common.scope.EspiScope}),
 * creates the Authorization aggregate (PR B1 model), and returns the canonical resource URIs that
 * the AS will include in the token response.</p>
 *
 * <p>Invariants (from PR B1):</p>
 * <ul>
 *   <li>Always creates exactly one <em>energy</em> Subscription (the {@code resource_uri} target).</li>
 *   <li>Creates a <em>customer/PII</em> Subscription only when the granted scope includes a
 *       Customer/PII Function Block (FB 54&ndash;62). DC builds the canonical
 *       {@code customerResourceURI} itself and stores it on the Authorization aggregate.</li>
 *   <li>Persistence happens through the Authorization aggregate; Subscriptions are never saved
 *       independently.</li>
 * </ul>
 *
 * <p>Not part of the ESPI standard &mdash; this is an implementation contract between the
 * GBA Authorization Server and a Data Custodian sandbox.</p>
 *
 * @see SubscriptionProvisionCommand
 * @see SubscriptionProvisionResult
 */
public interface SubscriptionProvisioningService {

	/**
	 * Provisions the Authorization aggregate for a completed OAuth2 grant.
	 *
	 * @param command the grant details from the AS
	 * @return the canonical URIs and IDs the AS should publish in the token response
	 * @throws IllegalArgumentException if the command is null, references unknown entities, or
	 *                                  carries an unparseable scope
	 */
	SubscriptionProvisionResult provisionFromGrant(SubscriptionProvisionCommand command);

	/**
	 * Command-side payload for {@link #provisionFromGrant(SubscriptionProvisionCommand)}.
	 *
	 * @param correlationId            opaque AS-supplied trace id; logged but not persisted
	 * @param clientId                 OAuth2 {@code client_id} of the granted TP (must match a
	 *                                 registered {@code ApplicationInformation})
	 * @param grantedScope             the ESPI scope string the customer approved (must parse via
	 *                                 {@link org.greenbuttonalliance.espi.common.scope.EspiScope#parse})
	 * @param retailCustomerId         primary key of the {@code RetailCustomer} that authenticated
	 *                                 (note: {@code RetailCustomerEntity} still uses {@code Long}
	 *                                 ids; tracked separately from the UUID5 migration)
	 * @param selectedUsagePointIds    usage points the customer chose to share with this TP; may be
	 *                                 empty for grants that include only customer/PII scope
	 */
	record SubscriptionProvisionCommand(
			String correlationId,
			String clientId,
			String grantedScope,
			Long retailCustomerId,
			List<UUID> selectedUsagePointIds
	) {}

	/**
	 * Result of a successful provisioning call.
	 *
	 * @param authorizationId          UUID of the persisted Authorization aggregate
	 * @param resourceSubscriptionId   UUID of the energy Subscription, or {@code null} for a
	 *                                 PII-only grant (no usage points selected)
	 * @param customerSubscriptionId   UUID of the customer/PII Subscription, or {@code null} if the
	 *                                 grant did not include Customer/PII scope
	 * @param resourceUri              absolute URI the TP polls for energy data
	 *                                 ({@code .../resource/Subscription/{id}}), or {@code null} for
	 *                                 a PII-only grant
	 * @param authorizationUri         absolute URI of the Authorization resource
	 *                                 ({@code .../resource/Authorization/{id}})
	 * @param customerResourceUri      absolute URI the TP polls for customer/PII data, or
	 *                                 {@code null} if no Customer/PII scope was granted
	 */
	record SubscriptionProvisionResult(
			UUID authorizationId,
			UUID resourceSubscriptionId,
			UUID customerSubscriptionId,
			String resourceUri,
			String authorizationUri,
			String customerResourceUri
	) {}
}
