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

package org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService.SubscriptionProvisionCommand;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService.SubscriptionProvisionResult;
import org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel.dto.SubscriptionProvisionRequest;
import org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel.dto.SubscriptionProvisionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AS&rarr;DC back-channel: provisions an Authorization aggregate (Authorization + 1&ndash;2
 * Subscriptions) at OAuth2 token-mint time.
 *
 * <p><strong>Not part of the ESPI standard.</strong> Implementation contract between the GBA
 * Authorization Server and a Data Custodian sandbox. Mounted under {@code /internal/backchannel/}
 * with a dedicated security filter chain (HTTP Basic with a shared back-channel credential, no
 * OAuth2 introspection) so it is never reachable through the public ESPI resource-server chain.</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/backchannel/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionProvisioningController {

	private final SubscriptionProvisioningService provisioningService;

	@PostMapping
	public ResponseEntity<SubscriptionProvisionResponse> provision(
			@Valid @RequestBody SubscriptionProvisionRequest request) {

		SubscriptionProvisionResult result = provisioningService.provisionFromGrant(
				new SubscriptionProvisionCommand(
						request.correlationId(),
						request.clientId(),
						request.grantedScope(),
						request.retailCustomerId(),
						request.selectedUsagePointIds(),
						request.customerResourceUri()));

		SubscriptionProvisionResponse body = new SubscriptionProvisionResponse(
				result.authorizationId(),
				result.resourceSubscriptionId(),
				result.customerSubscriptionId(),
				result.resourceUri(),
				result.authorizationUri(),
				result.customerResourceUri());

		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
		log.warn("Back-channel provisioning rejected: {}", e.getMessage());
		return ResponseEntity.badRequest().body(Map.of(
				"error", "invalid_request",
				"error_description", e.getMessage()));
	}
}
