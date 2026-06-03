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

import java.util.UUID;

/**
 * DC&rarr;AS back-channel response body for {@code POST /internal/backchannel/v1/subscriptions}.
 *
 * <p>Mirrors DC's {@code SubscriptionProvisionResponse} record JSON. The {@code resource_uri},
 * {@code authorization_uri}, and {@code customer_resource_uri} fields are the canonical URIs the
 * AS surfaces in the OAuth2 token response (per ESPI 1.1/4.0) and {@code /oauth2/introspect}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackchannelResponse(
		@JsonProperty("authorization_id") UUID authorizationId,
		@JsonProperty("resource_subscription_id") UUID resourceSubscriptionId,
		@JsonProperty("customer_subscription_id") UUID customerSubscriptionId,
		@JsonProperty("resource_uri") String resourceUri,
		@JsonProperty("authorization_uri") String authorizationUri,
		@JsonProperty("customer_resource_uri") String customerResourceUri
) {
}
