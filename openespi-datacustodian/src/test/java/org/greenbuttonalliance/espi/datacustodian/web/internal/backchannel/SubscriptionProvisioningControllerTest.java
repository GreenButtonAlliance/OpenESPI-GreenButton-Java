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

import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService.SubscriptionProvisionCommand;
import org.greenbuttonalliance.espi.common.service.SubscriptionProvisioningService.SubscriptionProvisionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for the AS&harr;DC back-channel endpoint and its dedicated security chain.
 * Verifies auth gating (HTTP Basic with the dedicated back-channel credential), happy-path
 * serialization, validation errors, and the service-level {@code IllegalArgumentException}
 * exception handler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionProvisioningControllerTest {

	private static final String CLIENT_ID = "as-backchannel";
	private static final String CLIENT_SECRET = "change-me-in-production";

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;

	@MockitoBean private SubscriptionProvisioningService provisioningService;

	@Test
	void provision_withValidCredentialsAndBody_returns201AndCanonicalUris() throws Exception {
		UUID authId = UUID.randomUUID();
		UUID subId = UUID.randomUUID();
		UUID upId = UUID.randomUUID();
		String resourceUri = "https://dc.example.com/espi/1_1/resource/Subscription/" + subId;
		String authUri = "https://dc.example.com/espi/1_1/resource/Authorization/" + authId;

		when(provisioningService.provisionFromGrant(any(SubscriptionProvisionCommand.class)))
				.thenReturn(new SubscriptionProvisionResult(
						authId, subId, null, resourceUri, authUri, null));

		String body = """
				{
				  "correlation_id": "corr-1",
				  "client_id": "test-tp",
				  "granted_scope": "FB=4_5_15",
				  "retail_customer_id": 42,
				  "selected_usage_point_ids": ["%s"]
				}
				""".formatted(upId);

		mockMvc.perform(post("/internal/backchannel/v1/subscriptions")
						.with(httpBasic(CLIENT_ID, CLIENT_SECRET))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.authorization_id").value(authId.toString()))
				.andExpect(jsonPath("$.resource_subscription_id").value(subId.toString()))
				.andExpect(jsonPath("$.resource_uri").value(resourceUri))
				.andExpect(jsonPath("$.authorization_uri").value(authUri))
				.andExpect(jsonPath("$.customer_subscription_id").doesNotExist())
				.andExpect(jsonPath("$.customer_resource_uri").doesNotExist());

		verify(provisioningService).provisionFromGrant(any(SubscriptionProvisionCommand.class));
	}

	@Test
	void provision_withoutCredentials_returns401AndDoesNotCallService() throws Exception {
		mockMvc.perform(post("/internal/backchannel/v1/subscriptions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(provisioningService);
	}

	@Test
	void provision_withWrongPassword_returns401() throws Exception {
		mockMvc.perform(post("/internal/backchannel/v1/subscriptions")
						.with(httpBasic(CLIENT_ID, "nope"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(provisioningService);
	}

	@Test
	void provision_withBlankRequiredField_returns400() throws Exception {
		String body = """
				{
				  "correlation_id": "",
				  "client_id": "test-tp",
				  "granted_scope": "FB=4_5_15",
				  "retail_customer_id": 42,
				  "selected_usage_point_ids": []
				}
				""";

		mockMvc.perform(post("/internal/backchannel/v1/subscriptions")
						.with(httpBasic(CLIENT_ID, CLIENT_SECRET))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(provisioningService);
	}

	@Test
	void provision_withMissingRequiredField_returns400() throws Exception {
		String body = """
				{
				  "correlation_id": "corr-1",
				  "client_id": "test-tp",
				  "granted_scope": "FB=4_5_15",
				  "selected_usage_point_ids": []
				}
				""";

		mockMvc.perform(post("/internal/backchannel/v1/subscriptions")
						.with(httpBasic(CLIENT_ID, CLIENT_SECRET))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(provisioningService);
	}

	@Test
	void provision_whenServiceRejects_returns400WithErrorBody() throws Exception {
		when(provisioningService.provisionFromGrant(any(SubscriptionProvisionCommand.class)))
				.thenThrow(new IllegalArgumentException("Unknown client_id: test-tp"));

		String body = """
				{
				  "correlation_id": "corr-1",
				  "client_id": "test-tp",
				  "granted_scope": "FB=4_5_15",
				  "retail_customer_id": 42,
				  "selected_usage_point_ids": []
				}
				""";

		mockMvc.perform(post("/internal/backchannel/v1/subscriptions")
						.with(httpBasic(CLIENT_ID, CLIENT_SECRET))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_request"))
				.andExpect(jsonPath("$.error_description").value("Unknown client_id: test-tp"));
	}

	@Test
	void requestDto_serializesUnderscoreCaseRoundTrip() throws Exception {
		// Sanity: confirm DTO round-trip uses snake_case (the AS↔DC contract is snake_case).
		String json = """
				{
				  "correlation_id": "corr-1",
				  "client_id": "test-tp",
				  "granted_scope": "FB=4",
				  "retail_customer_id": 42,
				  "selected_usage_point_ids": []
				}
				""";
		var parsed = objectMapper.readValue(json,
				org.greenbuttonalliance.espi.datacustodian.web.internal.backchannel.dto.SubscriptionProvisionRequest.class);
		assertThat(parsed.correlationId()).isEqualTo("corr-1");
		assertThat(parsed.clientId()).isEqualTo("test-tp");
		assertThat(parsed.grantedScope()).isEqualTo("FB=4");
		assertThat(parsed.retailCustomerId()).isEqualTo(42L);
		assertThat(parsed.selectedUsagePointIds()).isEqualTo(List.of());
	}
}
