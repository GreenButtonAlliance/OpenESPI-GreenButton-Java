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

package org.greenbuttonalliance.espi.authserver.config;

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
 * Every AS profile's configuration loads the way Spring Boot loads it at startup (#271). The YAML loader rejects a
 * duplicate key, and the config-data loader rejects {@code spring.profiles.active} or {@code include} in a
 * profile-specific file; either one stops the AS before any bean exists. Reads this module's own
 * {@code src/main/resources} and the Docker image's {@code docker/config} (surefire runs in the module directory),
 * not the test resources.
 */
@DisplayName("Every AS profile's configuration loads (#271)")
class ProfileConfigLoadsTest {

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"dev-mysql", "dev-postgresql", "local", "prod", "espi40", "docker"})
	@DisplayName("the profile loads as SPRING_PROFILES_ACTIVE would load it")
	void profileLoads(String profile) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("profile under test", Map.of(
				"spring.config.location", "file:src/main/resources/,file:docker/config/",
				"spring.profiles.active", profile)));

		ConfigDataEnvironmentPostProcessor.applyTo(environment, new DefaultResourceLoader(),
				new DefaultBootstrapContext());

		assertThat(environment.getActiveProfiles()).containsExactly(profile);
	}
}
