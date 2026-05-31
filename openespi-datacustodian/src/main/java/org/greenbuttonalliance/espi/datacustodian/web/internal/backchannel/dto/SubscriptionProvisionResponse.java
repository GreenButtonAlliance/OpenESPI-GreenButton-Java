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

package org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * JSON response body for {@code POST /internal/backchannel/v1/subscriptions}.
 *
 * <p>Carries the canonical URIs the AS includes in its token response so the TP can locate the
 * subscription, authorization, and (if granted) customer/PII resources. Null fields are omitted
 * from serialization.</p>
 *
 * @param authorizationId          UUID of the persisted Authorization aggregate
 * @param resourceSubscriptionId   UUID of the energy Subscription, or {@code null} for a PII-only
 *                                 grant
 * @param customerSubscriptionId   UUID of the customer/PII Subscription, or {@code null} if no
 *                                 Customer/PII scope was granted
 * @param resourceUri              absolute URI the TP polls for energy data, or {@code null} for a
 *                                 PII-only grant
 * @param authorizationUri         absolute URI of the Authorization resource
 * @param customerResourceUri      absolute URI the TP polls for customer/PII data, or {@code null}
 *                                 if no Customer/PII scope was granted
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionProvisionResponse(
		@JsonProperty("authorization_id")
		UUID authorizationId,

		@JsonProperty("resource_subscription_id")
		UUID resourceSubscriptionId,

		@JsonProperty("customer_subscription_id")
		UUID customerSubscriptionId,

		@JsonProperty("resource_uri")
		String resourceUri,

		@JsonProperty("authorization_uri")
		String authorizationUri,

		@JsonProperty("customer_resource_uri")
		String customerResourceUri
) {}
