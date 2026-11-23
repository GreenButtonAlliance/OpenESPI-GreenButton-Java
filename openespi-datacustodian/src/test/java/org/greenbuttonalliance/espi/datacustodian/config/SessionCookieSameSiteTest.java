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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The DC session cookie is SameSite=Lax in every profile (#271): the Authorization Server sends the customer to
 * {@code /oauth/authorize-screen}, a cross-site navigation that must carry the customer's DC sign-in, so Strict
 * would break the customer flow. Each profile's configuration is resolved the way Spring Boot resolves it at
 * startup, so the profile files the DC takes from openespi-common ({@code prod}, {@code docker},
 * {@code aws-sandbox}) are covered too.
 */
@DisplayName("DC session cookie is SameSite=Lax in every profile (#271)")
class SessionCookieSameSiteTest {

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"dev-mysql", "dev-postgresql", "local", "prod", "docker", "aws-sandbox"})
	@DisplayName("server.servlet.session.cookie.same-site resolves to lax")
	void laxInEveryProfile(String profile) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(
				new MapPropertySource("profile under test", Map.of("spring.profiles.active", profile)));

		ConfigDataEnvironmentPostProcessor.applyTo(environment, new DefaultResourceLoader(),
				new DefaultBootstrapContext());

		assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualToIgnoringCase("lax");
	}
}
