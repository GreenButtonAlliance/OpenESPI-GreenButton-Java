/*
 * Copyright 2026 Green Button Alliance, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.greenbuttonalliance.espi.authserver.controller;

import org.greenbuttonalliance.espi.authserver.dto.ClientManagementDTOs.CreateClientRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #274: PKCE is accepted, not required, for a confidential client; a public client must always send it.
 */
class OAuth2ClientManagementControllerProofKeyTest {

	static Stream<Arguments> cases() {
		return Stream.of(
				Arguments.of("confidential, default", List.of("client_secret_basic"), false, false),
				Arguments.of("confidential, asked for", List.of("client_secret_basic"), true, true),
				Arguments.of("no methods given (client_secret_basic)", null, false, false),
				Arguments.of("public, not asked for", List.of("none"), false, true),
				Arguments.of("public, asked for", List.of("none"), true, true));
	}

	@ParameterizedTest(name = "{0} -> requireProofKey {3}")
	@MethodSource("cases")
	@DisplayName("a new client requires PKCE when it is public or asks for it")
	void requiresProofKey(String label, List<String> methods, boolean asked, boolean expected) {
		CreateClientRequest request = new CreateClientRequest();
		request.setAuthenticationMethods(methods);
		request.setRequireProofKey(asked);

		assertThat(OAuth2ClientManagementController.requiresProofKey(request)).as(label).isEqualTo(expected);
	}
}
