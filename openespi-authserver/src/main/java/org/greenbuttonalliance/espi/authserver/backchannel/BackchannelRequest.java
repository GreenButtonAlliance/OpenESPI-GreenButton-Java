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

package org.greenbuttonalliance.espi.authserver.backchannel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * AS&rarr;DC back-channel request body for {@code POST /internal/backchannel/v1/subscriptions}.
 *
 * <p>JSON wire format mirrors DC's {@code SubscriptionProvisionRequest} record. A local copy lives
 * here (rather than reusing the DC record) to preserve the long-standing
 * authserver-independent-of-common invariant &mdash; the AS does not depend on {@code
 * openespi-common} (which holds DC's domain).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackchannelRequest(
		@JsonProperty("correlation_id") String correlationId,
		@JsonProperty("client_id") String clientId,
		@JsonProperty("granted_scope") String grantedScope,
		@JsonProperty("retail_customer_id") Long retailCustomerId,
		@JsonProperty("selected_usage_point_ids") List<UUID> selectedUsagePointIds
) {
}
