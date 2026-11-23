/*
 *
 *        Copyright (c) 2025-2026 Green Button Alliance, Inc.
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.datacustodian.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The DC session cookie is SameSite=Lax in every profile (#271): the Authorization Server sends the customer to
 * {@code /oauth/authorize-screen}, a cross-site navigation that must carry the customer's DC sign-in, so Strict
 * would break the customer flow. Each profile's configuration is resolved the way startup resolves it, so the
 * profile files the DC takes from openespi-common are covered, and so is any new profile file.
 */
@DisplayName("DC session cookie is SameSite=Lax in every profile (#271)")
class SessionCookieSameSiteTest {

	static List<String> profiles() throws IOException {
		return ProfileConfig.profiles();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("profiles")
	@DisplayName("server.servlet.session.cookie.same-site resolves to lax")
	void laxInEveryProfile(String profile) {
		assertThat(ProfileConfig.load(profile).getProperty("server.servlet.session.cookie.same-site"))
				.isEqualToIgnoringCase("lax");
	}
}
