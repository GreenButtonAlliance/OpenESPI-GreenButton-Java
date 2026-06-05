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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * JSON request body for {@code POST /internal/backchannel/v1/subscriptions}.
 *
 * <p>Sent by the GBA Authorization Server to a Data Custodian sandbox at token-mint time, after the
 * customer has authenticated against the DC and made selections on the DC-hosted Authorization
 * Screen. Implementation contract &mdash; not part of the ESPI standard.</p>
 *
 * @param correlationId         opaque AS-supplied trace id; echoed in DC logs for cross-system audit
 * @param clientId              OAuth2 {@code client_id} of the granted TP (must match a
 *                              registered {@code ApplicationInformation} on DC)
 * @param grantedScope          ESPI scope string the customer approved
 *                              (e.g. {@code "FB=4_5_15;IntervalDuration=3600"})
 * @param retailCustomerId      DC primary key of the authenticated customer
 * @param selectedUsagePointIds UUIDs of the usage points the customer chose to share with this TP;
 *                              may be empty for grants that include only customer/PII scope
 */
public record SubscriptionProvisionRequest(
		@JsonProperty("correlation_id")
		@NotBlank String correlationId,

		@JsonProperty("client_id")
		@NotBlank String clientId,

		@JsonProperty("granted_scope")
		@NotBlank String grantedScope,

		@JsonProperty("retail_customer_id")
		@NotNull Long retailCustomerId,

		@JsonProperty("selected_usage_point_ids")
		List<UUID> selectedUsagePointIds
) {}
