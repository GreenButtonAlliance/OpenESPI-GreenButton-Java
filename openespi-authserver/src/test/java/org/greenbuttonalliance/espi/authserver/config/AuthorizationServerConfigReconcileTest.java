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

package org.greenbuttonalliance.espi.authserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AuthorizationServerConfig#reconcile} — the startup reconciliation that lets
 * default-client code changes reach an already-seeded database (#154).
 */
@DisplayName("Default-client reconciliation (#154)")
class AuthorizationServerConfigReconcileTest {

	private static RegisteredClient client(String id, String name, boolean requireProofKey) {
		return RegisteredClient.withId(id)
				.clientId("third_party")
				.clientName(name)
				.clientSecret("{noop}tp-secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("https://tp.example/cb")
				.scope("FB_1")
				.clientSettings(ClientSettings.builder()
						.requireAuthorizationConsent(true)
						.requireProofKey(requireProofKey)
						.build())
				.build();
	}

	@Test
	@DisplayName("no drift -> null (no write)")
	void inSyncReturnsNull() {
		RegisteredClient existing = client(UUID.randomUUID().toString(), "ThirdParty Application", false);
		RegisteredClient desired = client(UUID.randomUUID().toString(), "ThirdParty Application", false);

		assertThat(AuthorizationServerConfig.reconcile(existing, desired)).isNull();
	}

	@Test
	@DisplayName("requireProofKey drift -> reconciled with existing id and PKCE disabled")
	void proofKeyDriftReconciles() {
		String existingId = UUID.randomUUID().toString();
		RegisteredClient existing = client(existingId, "ThirdParty Application", true);  // stale: PKCE on
		RegisteredClient desired = client(UUID.randomUUID().toString(), "ThirdParty Application", false);

		RegisteredClient result = AuthorizationServerConfig.reconcile(existing, desired);

		assertThat(result).isNotNull();
		assertThat(result.getId())
				.as("must keep the existing primary-key id so the update lands on the same row")
				.isEqualTo(existingId);
		assertThat(result.getClientSettings().isRequireProofKey()).isFalse();
	}

	@Test
	@DisplayName("clientName drift -> reconciled with existing id and canonical name")
	void nameDriftReconciles() {
		String existingId = UUID.randomUUID().toString();
		RegisteredClient existing = client(existingId, existingId, false);  // stale: UUID as name
		RegisteredClient desired = client(UUID.randomUUID().toString(), "ThirdParty Application", false);

		RegisteredClient result = AuthorizationServerConfig.reconcile(existing, desired);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(existingId);
		assertThat(result.getClientName()).isEqualTo("ThirdParty Application");
	}
}
